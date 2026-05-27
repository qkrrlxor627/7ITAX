import json
import logging
import re

from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage, SystemMessage

from app.core.prompts import CLASSIFY_EXPLANATION_PROMPT
from app.services.retrieval_service import RetrievalService, SearchResult

logger = logging.getLogger(__name__)


class ExplanationService:
    """거래 세목 분류 결과에 대한 법적 근거와 이유를 생성하는 서비스."""

    def __init__(
        self,
        retrieval_service: RetrievalService,
        llm: ChatAnthropic,
        category_mappings: list[dict],
    ) -> None:
        self.retrieval_service = retrieval_service
        self.llm = llm
        self._mapping_dict: dict[str, dict] = {
            m["category"]: m for m in category_mappings
        }

    async def generate_explanation(
        self,
        description: str,
        category: str,
        confidence: float,
    ) -> dict:
        try:
            mapping = self._get_mapping(category)
            if mapping is None:
                return self._default_explanation(category)

            search_query = self._build_search_query(description, mapping)
            metadata_filter = self._build_filter(mapping)
            search_results = await self.retrieval_service.retrieve(
                query=search_query,
                top_k=3,
                metadata_filter=metadata_filter,
                search_strategy="hybrid",
            )
            context = self._format_context(search_results)
            raw_response = await self._call_llm(category, description, context)
            return self._parse_response(raw_response)
        except Exception as e:
            logger.warning("설명 생성 실패 (fallback 사용): %s", e)
            return self._default_explanation(category)

    def _get_mapping(self, category: str) -> dict | None:
        return self._mapping_dict.get(category)

    def _build_search_query(self, description: str, mapping: dict) -> str:
        keywords = " ".join(mapping.get("search_keywords", []))
        return f"{description} {keywords}"

    def _build_filter(self, mapping: dict) -> dict:
        return {"law_name": {"$in": mapping["law_filter"]}}

    def _format_context(self, search_results: list[SearchResult]) -> str:
        if not search_results:
            return "(관련 법률 자료 없음)"

        parts = []
        for result in search_results:
            content = result.content[:500]
            source = result.metadata.get("law_name", "알 수 없음")
            parts.append(f"[출처: {source}]\n{content}")
        return "\n\n---\n\n".join(parts)

    async def _call_llm(self, category: str, description: str, context: str) -> str:
        prompt = CLASSIFY_EXPLANATION_PROMPT.format(
            category=category,
            description=description,
            context=context,
        )
        messages = [
            SystemMessage(content="당신은 한국 세금 전문가입니다."),
            HumanMessage(content=prompt),
        ]
        response = await self.llm.ainvoke(messages)
        return response.content

    def _parse_response(self, response: str) -> dict:
        cleaned = re.sub(r"```(?:json)?\s*", "", response).strip()
        cleaned = cleaned.rstrip("`").strip()

        try:
            parsed = json.loads(cleaned)
            return {
                "reason": parsed.get("reason", ""),
                "legal_basis": parsed.get("legal_basis", ""),
            }
        except json.JSONDecodeError:
            logger.warning("LLM 응답 JSON 파싱 실패: %s", response[:200])
            return {"reason": response.strip()[:200], "legal_basis": ""}

    def _default_explanation(self, category: str) -> dict:
        mapping = self._get_mapping(category)
        hint = mapping["description_hint"] if mapping else f"'{category}' 세목으로 분류되었습니다."
        return {"reason": hint, "legal_basis": ""}
