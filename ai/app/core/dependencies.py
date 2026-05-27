import json
import logging
from functools import lru_cache
from pathlib import Path

from app.core.config import Settings
from app.core.exceptions import AIServiceError
from app.services.backend_client import BackendClient
from app.services.cache_service import SemanticCache
from app.services.chat_service import ChatService
from app.services.embedding_service import EmbeddingService
from app.services.explanation_service import ExplanationService
from app.services.intent_classifier import IntentClassifier
from app.services.reranker_service import RerankerService
from app.services.retrieval_service import BM25Index, RetrievalService
from app.services.tax_classifier_service import TaxClassifierService
from app.services.vectorstore import VectorStoreService

logger = logging.getLogger(__name__)

_vectorstore_service: VectorStoreService | None = None
_retrieval_service: RetrievalService | None = None
_chat_service: ChatService | None = None
_tax_classifier_service: TaxClassifierService | None = None
_explanation_service: ExplanationService | None = None

INTENTS_PATH = "app/data/intents/tax_intents.json"


@lru_cache
def get_settings() -> Settings:
    """애플리케이션 설정을 로드하고 반환한다.

    캐시되어 있으므로 동일한 Settings 인스턴스를 반환한다.

    Returns:
        Settings: 애플리케이션 설정 객체.
    """
    return Settings()


async def init_services() -> None:
    """모든 서비스를 초기화한다. 앱 lifespan에서 호출.

    초기화 순서 (의존성 순):
    1. VectorStoreService (ChromaDB 연결)
    2. BM25Index 구축 (ChromaDB 전체 문서 로드)
    3. RetrievalService (VectorStoreService + BM25Index 주입)
    4. EmbeddingService + IntentClassifier (예시 발화 임베딩)
    5. ChatService (Settings + RetrievalService + IntentClassifier 주입)
    """
    global _vectorstore_service, _retrieval_service, _chat_service

    settings = get_settings()

    try:
        _vectorstore_service = VectorStoreService(settings)
    except Exception as e:
        logger.error("VectorStoreService 초기화 실패: %s", e, exc_info=True)
        raise

    bm25_index = BM25Index()
    all_docs = _vectorstore_service.get_all_documents()
    if all_docs:
        bm25_index.build(all_docs)
        logger.info("BM25 인덱스 구축 완료: %d개 문서", len(all_docs))
    else:
        logger.warning("BM25 인덱스: 문서 없음 (ChromaDB 비어있음)")

    reranker = None
    try:
        reranker = RerankerService()
        logger.info("RerankerService 초기화 완료")
    except Exception as e:
        logger.warning("RerankerService 초기화 실패 (Reranker 비활성): %s", e)

    _retrieval_service = RetrievalService(
        vectorstore_service=_vectorstore_service,
        bm25_index=bm25_index,
        reranker=reranker,
    )

    embedding_service = EmbeddingService(settings)
    intent_classifier = IntentClassifier(INTENTS_PATH, embedding_service)
    try:
        await intent_classifier.initialize()
        logger.info("IntentClassifier 초기화 완료")
    except Exception as e:
        logger.error("IntentClassifier 초기화 실패: %s", e, exc_info=True)
        raise

    cache_service = (
        SemanticCache(
            embedding_service=embedding_service,
            threshold=settings.cache_threshold,
            max_entries=settings.cache_max_entries,
            ttl_hours=settings.cache_ttl_hours,
        )
        if settings.cache_enabled
        else None
    )

    backend_client = BackendClient(settings)

    _chat_service = ChatService(
        settings=settings,
        retrieval_service=_retrieval_service,
        intent_classifier=intent_classifier,
        backend_client=backend_client,
        cache_service=cache_service,
    )


    # 세목 분류 서비스 초기화 (모델 미존재 시 경고만 출력)
    global _tax_classifier_service
    try:
        _tax_classifier_service = TaxClassifierService()
        logger.info("TaxClassifierService 초기화 완료")
    except FileNotFoundError as e:
        logger.warning("TaxClassifierService 모델 없음 (파인튜닝 필요): %s", e)

    # 세목 분류 설명 서비스 초기화 (실패 시 경고만 출력, 앱 중단 없음)
    global _explanation_service
    try:
        mapping_path = Path("app/data/category_legal_mapping.json")
        with open(mapping_path, encoding="utf-8") as f:
            category_mappings = json.load(f)

        from langchain_anthropic import ChatAnthropic

        explanation_llm = ChatAnthropic(
            api_key=settings.anthropic_api_key,
            model=settings.claude_model_haiku,
            temperature=0.3,
            timeout=10,
            max_tokens=1024,
        )
        _explanation_service = ExplanationService(
            retrieval_service=_retrieval_service,
            llm=explanation_llm,
            category_mappings=category_mappings,
        )
        logger.info("ExplanationService 초기화 완료")
    except Exception as e:
        logger.warning("ExplanationService 초기화 실패 (설명 생성 비활성): %s", e)


def get_tax_classifier_service() -> TaxClassifierService:
    """TaxClassifierService 인스턴스를 반환한다. FastAPI Depends()에서 사용."""
    if _tax_classifier_service is None:
        raise AIServiceError(
            "세목 분류 서비스가 초기화되지 않았습니다. 모델 파인튜닝이 필요합니다."
        )
    return _tax_classifier_service


def get_explanation_service() -> ExplanationService | None:
    """ExplanationService 인스턴스를 반환한다. None일 수 있음 (초기화 실패 시)."""
    return _explanation_service


def get_chat_service() -> ChatService:
    """ChatService 인스턴스를 반환한다. FastAPI Depends()에서 사용."""
    if _chat_service is None:
        raise AIServiceError("서비스가 초기화되지 않았습니다. 잠시 후 다시 시도해주세요.")
    return _chat_service
