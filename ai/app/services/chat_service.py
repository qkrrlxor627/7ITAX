import logging
import uuid
from cachetools import TTLCache

from anthropic import APITimeoutError, AuthenticationError, RateLimitError
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

from app.core.config import Settings
from app.core.exceptions import AIServiceError, LLMAuthError, LLMRateLimitError, LLMTimeoutError

logger = logging.getLogger(__name__)
from app.core.prompts import build_intent_prompt
from app.services.intent_classifier import IntentClassifier, IntentResult
from app.services.retrieval_service import RetrievalService
from app.utils.query_rewriter import QueryRewriter
from app.utils.text_utils import format_search_results

MAX_HISTORY_LENGTH = 20


class ChatService:
    """채팅 서비스.

    인텐트 분류, RAG 검색, LLM 호출을 조합하여 사용자 메시지에 응답한다.
    """

    def __init__(
        self,
        settings: Settings,
        retrieval_service: RetrievalService,
        intent_classifier: IntentClassifier,
        backend_client=None,  # Phase 3: 선택적
        cache_service=None,   # Phase 4: 선택적
    ) -> None:
        self.settings = settings
        # mini=Haiku (쿼리 재작성 등 가벼운 작업), standard=Opus (메인 챗봇 답변)
        self.llm_mini = ChatAnthropic(
            api_key=settings.anthropic_api_key,
            model=settings.claude_model_haiku,
            temperature=0.7,
            timeout=30,
            max_tokens=2048,
        )
        self.llm_standard = ChatAnthropic(
            api_key=settings.anthropic_api_key,
            model=settings.claude_model_opus,
            temperature=0.7,
            timeout=30,
            max_tokens=2048,
        )
        self.retrieval_service = retrieval_service
        self.intent_classifier = intent_classifier
        self.backend_client = backend_client
        self.cache_service = cache_service
        self._histories: TTLCache = TTLCache(maxsize=1000, ttl=3600)
        self.query_rewriter = QueryRewriter(llm=self.llm_mini)

    async def get_response(
        self,
        message: str,
        session_id: str | None = None,
        user_id: str | None = None,
    ) -> tuple[str, str, str]:
        if session_id is None:
            session_id = uuid.uuid4().hex

        if session_id not in self._histories:
            self._histories[session_id] = []
        history = self._histories[session_id]

        # 1. 인텐트 분류
        intent_result: IntentResult = await self.intent_classifier.classify(message)

        # 2. 시맨틱 캐시 확인 (Phase 4에서 활성화)
        if self.cache_service:
            try:
                cached = await self.cache_service.get(message)
                if cached:
                    return cached, session_id, "cached"
            except Exception as e:
                logger.warning("캐시 조회 실패 (무시): %s", e)

        # 3. 인텐트별 검색 (rag_enabled=False 시 건너뜀)
        context_text = ""
        if self.settings.rag_enabled and intent_result.rag_required:
            try:
                search_query = await self.query_rewriter.rewrite(message)
                results = await self.retrieval_service.retrieve(
                    query=search_query,
                    metadata_filter=intent_result.metadata_filter or None,
                    search_strategy=intent_result.search_strategy,
                )
                context_text = format_search_results(results)
            except AIServiceError:
                raise
            except Exception as e:
                logger.error("검색 서비스 오류: %s", e, exc_info=True)
                raise AIServiceError("검색 중 오류가 발생했습니다.") from e

        # 4. (필요시) 백엔드 데이터 보강
        user_transactions_text = ""
        user_data_text = ""
        if intent_result.be_data_required and self.backend_client and user_id:
            from app.utils.text_utils import format_business_info, format_transactions

            try:
                transactions = await self.backend_client.get_transactions(user_id)
                business_info = await self.backend_client.get_business_info(user_id)
                user_transactions_text = format_transactions(transactions)
                user_data_text = format_business_info(business_info)
            except Exception as e:
                # 백엔드 데이터 보강 실패는 치명적이지 않다: 개인화만 생략하고 일반 답변을 이어간다.
                # (예: BE에 해당 사용자 데이터 엔드포인트가 없거나 네트워크/인증 오류)
                logger.warning(
                    "백엔드 사용자 데이터 조회 실패 → 개인화 생략하고 답변 계속: %s", e
                )

        # 5. 인텐트별 프롬프트 생성
        system_prompt = build_intent_prompt(
            intent_name=intent_result.intent,
            context=context_text,
            user_transactions=user_transactions_text,
            user_data=user_data_text,
        )

        # 6. 메시지 구성 + LLM 호출
        messages: list[BaseMessage] = [SystemMessage(content=system_prompt)]
        messages.extend(history)
        messages.append(HumanMessage(content=message))

        llm = self._select_llm(intent_result.model_tier)
        answer = await self._call_llm(messages, llm=llm)

        # 7. 히스토리 관리
        history.append(HumanMessage(content=message))
        history.append(AIMessage(content=answer))

        if len(history) > MAX_HISTORY_LENGTH:
            self._histories[session_id] = history[-MAX_HISTORY_LENGTH:]

        # 8. 캐시 저장 (Phase 4에서 활성화)
        if self.cache_service:
            try:
                await self.cache_service.put(message, answer, intent_result.intent)
            except Exception as e:
                logger.warning("캐시 저장 실패 (무시): %s", e)

        return answer, session_id, llm.model

    def get_history(self, session_id: str) -> list[dict[str, str]]:
        """대화 세션의 히스토리를 반환한다.

        Args:
            session_id: 대화 세션 ID.

        Returns:
            list: 메시지 리스트. 각 원소는 {"role": "user" | "assistant", "content": str}.
        """
        history = self._histories.get(session_id, [])
        return [
            {
                "role": "user" if isinstance(msg, HumanMessage) else "assistant",
                "content": msg.content,
            }
            for msg in history
        ]

    def _select_llm(self, model_tier: str) -> ChatAnthropic:
        """모델 티어에 따라 LLM 인스턴스를 선택한다."""
        if model_tier == "standard":
            return self.llm_standard
        return self.llm_mini

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type(LLMTimeoutError),
    )
    async def _call_llm(
        self,
        messages: list[BaseMessage],
        llm: ChatAnthropic | None = None,
    ) -> str:
        """LLM에 메시지를 전달하고 응답을 받는다.

        타임아웃 발생 시 최대 3회 재시도한다.

        Args:
            messages: 대화 메시지 리스트.
            llm: LLM 인스턴스. None이면 self.llm_mini 사용.

        Returns:
            str: LLM 응답 텍스트.

        Raises:
            LLMTimeoutError: 타임아웃 발생 시 (3회 재시도 후).
            LLMAuthError: 인증 실패 시.
            LLMRateLimitError: API 레이트 제한 시.
        """
        target_llm = llm or self.llm_mini
        try:
            response = await target_llm.ainvoke(messages)
            return response.content
        except APITimeoutError as e:
            raise LLMTimeoutError() from e
        except AuthenticationError as e:
            raise LLMAuthError() from e
        except RateLimitError as e:
            raise LLMRateLimitError() from e
