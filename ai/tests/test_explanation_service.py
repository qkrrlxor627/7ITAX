"""ExplanationService 단위 테스트."""

import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.services.explanation_service import ExplanationService
from app.services.retrieval_service import SearchResult


# --- Fixture ---

SAMPLE_MAPPINGS = [
    {
        "category": "소모품비",
        "search_keywords": ["소모품", "소모성 물품", "사무용품", "필요경비"],
        "law_filter": ["소득세법"],
        "description_hint": "사무용품 등 소모성 자재 구입",
    },
    {
        "category": "접대비",
        "search_keywords": ["접대비", "접대비 한도", "손금불산입"],
        "law_filter": ["소득세법"],
        "description_hint": "거래처 접대 관련 비용",
    },
]


@pytest.fixture
def mock_retrieval():
    return AsyncMock()


@pytest.fixture
def mock_llm():
    return AsyncMock()


@pytest.fixture
def explanation_service(mock_retrieval, mock_llm):
    return ExplanationService(
        retrieval_service=mock_retrieval,
        llm=mock_llm,
        category_mappings=SAMPLE_MAPPINGS,
    )


# --- 매핑 조회 테스트 ---

class TestGetMapping:
    def test_existing_category(self, explanation_service):
        """존재하는 세목의 매핑 반환."""
        mapping = explanation_service._get_mapping("소모품비")
        assert mapping is not None
        assert mapping["category"] == "소모품비"
        assert "소모품" in mapping["search_keywords"]

    def test_unknown_category_returns_none(self, explanation_service):
        """미존재 세목은 None 반환."""
        mapping = explanation_service._get_mapping("알수없는세목")
        assert mapping is None


# --- 검색 쿼리 구성 테스트 ---

class TestBuildSearchQuery:
    def test_combines_description_and_keywords(self, explanation_service):
        """거래 설명 + 전체 키워드를 결합."""
        mapping = {"search_keywords": ["소모품", "소모성 물품", "사무용품", "필요경비"]}
        query = explanation_service._build_search_query("볼펜 구입", mapping)
        assert "볼펜 구입" in query
        assert "소모품" in query
        assert "필요경비" in query


# --- 필터 구성 테스트 ---

class TestBuildFilter:
    def test_creates_in_filter(self, explanation_service):
        """$in 필터 구성."""
        mapping = {"law_filter": ["소득세법", "소득세법 시행령"]}
        f = explanation_service._build_filter(mapping)
        assert f == {"law_name": {"$in": ["소득세법", "소득세법 시행령"]}}


# --- 컨텍스트 포맷팅 테스트 ---

class TestFormatContext:
    def test_formats_with_source(self, explanation_service):
        """출처 포함 포맷팅."""
        result = SearchResult(
            content="제33조 필요경비 관련 내용",
            metadata={"law_name": "소득세법"},
            score=0.9,
        )
        context = explanation_service._format_context([result])
        assert "[출처: 소득세법]" in context
        assert "제33조" in context

    def test_truncates_long_content(self, explanation_service):
        """500자 초과 컨텐츠 절단."""
        result = SearchResult(
            content="가" * 1000,
            metadata={"law_name": "소득세법"},
            score=0.9,
        )
        context = explanation_service._format_context([result])
        # content[:500] 이므로 500자 이하로 절단
        assert "가" * 500 in context
        assert "가" * 501 not in context

    def test_empty_results(self, explanation_service):
        """검색 결과 0건 시 안내 메시지."""
        context = explanation_service._format_context([])
        assert "관련 법률 자료 없음" in context


# --- LLM 응답 파싱 테스트 ---

class TestParseResponse:
    def test_valid_json(self, explanation_service):
        """정상 JSON 파싱."""
        content = '{"reason": "소모품비입니다", "legal_basis": "소득세법 제33조"}'
        result = explanation_service._parse_response(content)
        assert result["reason"] == "소모품비입니다"
        assert result["legal_basis"] == "소득세법 제33조"

    def test_json_in_code_block(self, explanation_service):
        """코드블록으로 감싼 JSON 처리."""
        content = '```json\n{"reason": "테스트", "legal_basis": "법률"}\n```'
        result = explanation_service._parse_response(content)
        assert result["reason"] == "테스트"

    def test_invalid_json_returns_default(self, explanation_service):
        """유효하지 않은 JSON -> 기본값."""
        content = "이것은 JSON이 아닙니다"
        result = explanation_service._parse_response(content)
        assert result["reason"] != ""
        assert result["legal_basis"] == ""


# --- 통합 generate_explanation 테스트 ---

class TestGenerateExplanation:
    @pytest.mark.asyncio
    async def test_success_flow(self, explanation_service, mock_retrieval, mock_llm):
        """정상 흐름: 검색 -> LLM -> 결과."""
        mock_result = SearchResult(
            content="제33조 필요경비",
            metadata={"law_name": "소득세법"},
            score=0.9,
        )
        mock_retrieval.retrieve.return_value = [mock_result]

        mock_response = MagicMock()
        mock_response.content = '{"reason": "사무용품", "legal_basis": "소득세법 제33조"}'
        mock_llm.ainvoke.return_value = mock_response

        result = await explanation_service.generate_explanation(
            description="볼펜 구입", category="소모품비", confidence=0.92
        )
        assert result["reason"] == "사무용품"
        assert result["legal_basis"] == "소득세법 제33조"

    @pytest.mark.asyncio
    async def test_llm_error_returns_default(self, explanation_service, mock_retrieval, mock_llm):
        """LLM 에러 시 기본 설명."""
        mock_retrieval.retrieve.return_value = []
        mock_llm.ainvoke.side_effect = Exception("Timeout")

        result = await explanation_service.generate_explanation(
            description="볼펜 구입", category="소모품비", confidence=0.92
        )
        assert "소모품" in result["reason"] or "소모성" in result["reason"]

    @pytest.mark.asyncio
    async def test_unknown_category_returns_default(self, explanation_service):
        """미존재 세목은 기본 설명 반환."""
        result = await explanation_service.generate_explanation(
            description="기타", category="알수없는세목", confidence=0.3
        )
        assert "알수없는세목" in result["reason"]

    @pytest.mark.asyncio
    async def test_rag_empty_still_calls_llm(self, explanation_service, mock_retrieval, mock_llm):
        """RAG 0건이어도 LLM 호출."""
        mock_retrieval.retrieve.return_value = []
        mock_response = MagicMock()
        mock_response.content = '{"reason": "일반 설명", "legal_basis": "해당 없음"}'
        mock_llm.ainvoke.return_value = mock_response

        result = await explanation_service.generate_explanation(
            description="카드결제", category="소모품비", confidence=0.45
        )
        assert result["reason"] == "일반 설명"
        mock_llm.ainvoke.assert_called_once()
