"""검색 품질 회귀 테스트.

경비율표(테이블) 청크가 법조문 검색을 오염시키는 문제의 재발을 방지한다.
retrieval_service의 필터링 로직을 단위 테스트한다.

사용법:
    pytest tests/test_retrieval_quality.py -v
"""

from unittest.mock import AsyncMock, MagicMock

import pytest

from app.services.retrieval_service import BM25Index, RetrievalService, SearchResult
from app.services.vectorstore import VectorStoreService


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

def _make_mock_vectorstore(results: list[dict] | None = None):
    """모의 VectorStoreService를 생성한다."""
    mock = MagicMock(spec=VectorStoreService)
    mock.similarity_search.return_value = results or []
    return mock


def _make_service(
    vector_results: list[dict] | None = None,
    bm25_docs: list[dict] | None = None,
) -> RetrievalService:
    """RetrievalService를 테스트용으로 생성한다."""
    vs = _make_mock_vectorstore(vector_results)
    bm25 = BM25Index()
    if bm25_docs:
        bm25.build(bm25_docs)
    return RetrievalService(vectorstore_service=vs, bm25_index=bm25)


# ---------------------------------------------------------------------------
# 테이블 제외 필터 테스트
# ---------------------------------------------------------------------------

class TestBuildEffectiveFilter:
    """_build_effective_filter 메서드 테스트."""

    def test_no_filter_adds_table_exclusion(self):
        """메타데이터 필터 없을 때 테이블 제외가 자동 추가된다."""
        svc = _make_service()
        result = svc._build_effective_filter(None)
        assert result == {"law_type": {"$ne": "테이블"}}

    def test_existing_filter_merges_table_exclusion(self):
        """기존 필터에 테이블 제외가 병합된다."""
        svc = _make_service()
        result = svc._build_effective_filter({"topics": {"$contains": "경비"}})
        assert result == {
            "law_type": {"$ne": "테이블"},
            "topics": {"$contains": "경비"},
        }

    def test_explicit_law_type_not_overridden(self):
        """사용자가 law_type을 명시하면 기본 필터가 적용되지 않는다."""
        svc = _make_service()
        user_filter = {"law_type": "법률"}
        result = svc._build_effective_filter(user_filter)
        assert result == {"law_type": "법률"}

    def test_empty_dict_filter_adds_table_exclusion(self):
        """빈 dict 필터에도 테이블 제외가 추가된다."""
        svc = _make_service()
        result = svc._build_effective_filter({})
        assert result == {"law_type": {"$ne": "테이블"}}


# ---------------------------------------------------------------------------
# 점수 임계치 테스트
# ---------------------------------------------------------------------------

class TestScoreThreshold:
    """_apply_score_threshold 메서드 테스트."""

    def test_high_score_results_pass(self):
        """임계치 이상 점수는 통과한다."""
        svc = _make_service()
        results = [
            SearchResult(content="소득세법 제35조", metadata={}, score=0.85),
            SearchResult(content="소득세법 시행령", metadata={}, score=0.70),
        ]
        filtered = svc._apply_score_threshold(results)
        assert len(filtered) == 2

    def test_low_score_results_filtered(self):
        """임계치 미만 점수는 필터링된다."""
        svc = _make_service()
        results = [
            SearchResult(content="경비율표 업종코드", metadata={}, score=0.15),
            SearchResult(content="관련 없는 문서", metadata={}, score=0.10),
        ]
        filtered = svc._apply_score_threshold(results)
        assert len(filtered) == 0

    def test_mixed_scores_partial_filter(self):
        """혼합 점수에서 임계치 이상만 남는다."""
        svc = _make_service()
        results = [
            SearchResult(content="소득세법 제35조", metadata={}, score=0.50),
            SearchResult(content="경비율표 업종코드", metadata={}, score=0.15),
            SearchResult(content="소득세법 시행령", metadata={}, score=0.30),
        ]
        filtered = svc._apply_score_threshold(results)
        assert len(filtered) == 2
        assert all(r.score >= 0.25 for r in filtered)

    def test_exact_threshold_passes(self):
        """정확히 임계치인 점수는 통과한다."""
        svc = _make_service()
        results = [
            SearchResult(content="test", metadata={}, score=0.25),
        ]
        filtered = svc._apply_score_threshold(results)
        assert len(filtered) == 1


# ---------------------------------------------------------------------------
# $ne/$nin 메타데이터 필터 테스트
# ---------------------------------------------------------------------------

class TestMetadataFilterOperators:
    """_apply_metadata_filter의 $ne, $nin 연산자 테스트."""

    def _make_docs(self) -> list[tuple[dict, float]]:
        """테스트용 문서 목록."""
        return [
            ({"content": "소득세법 제35조", "metadata": {"law_type": "법률", "law_name": "소득세법"}}, 0.9),
            ({"content": "경비율표 업종코드", "metadata": {"law_type": "테이블", "law_name": "기준경비율"}}, 0.8),
            ({"content": "소득세법 시행령", "metadata": {"law_type": "시행령", "law_name": "소득세법 시행령"}}, 0.7),
            ({"content": "부가가치세법", "metadata": {"law_type": "법률", "law_name": "부가가치세법"}}, 0.6),
        ]

    def test_ne_excludes_matching(self):
        """$ne 연산자가 일치하는 문서를 제외한다."""
        svc = _make_service()
        docs = self._make_docs()
        filtered = svc._apply_metadata_filter(docs, {"law_type": {"$ne": "테이블"}})
        assert len(filtered) == 3
        assert all(d["metadata"]["law_type"] != "테이블" for d, _ in filtered)

    def test_nin_excludes_multiple(self):
        """$nin 연산자가 목록에 포함된 문서를 제외한다."""
        svc = _make_service()
        docs = self._make_docs()
        filtered = svc._apply_metadata_filter(
            docs, {"law_type": {"$nin": ["테이블", "시행령"]}}
        )
        assert len(filtered) == 2
        for d, _ in filtered:
            assert d["metadata"]["law_type"] in ("법률",)

    def test_ne_combined_with_contains(self):
        """$ne와 $contains를 결합할 수 있다."""
        svc = _make_service()
        docs = self._make_docs()
        filtered = svc._apply_metadata_filter(docs, {
            "law_type": {"$ne": "테이블"},
            "law_name": {"$contains": "소득세법"},
        })
        assert len(filtered) == 2
        for d, _ in filtered:
            assert "소득세법" in d["metadata"]["law_name"]
            assert d["metadata"]["law_type"] != "테이블"

    def test_ne_no_match_keeps_all(self):
        """$ne 조건에 해당하는 문서가 없으면 전부 유지된다."""
        svc = _make_service()
        docs = self._make_docs()
        filtered = svc._apply_metadata_filter(docs, {"law_type": {"$ne": "시행규칙"}})
        assert len(filtered) == 4


# ---------------------------------------------------------------------------
# 검색 전략별 필터 적용 테스트
# ---------------------------------------------------------------------------

class TestRetrieveAppliesFilter:
    """retrieve() 호출 시 테이블 제외 필터가 적용되는지 검증."""

    @pytest.fixture
    def mock_vectorstore(self):
        mock = MagicMock(spec=VectorStoreService)
        mock.similarity_search.return_value = [
            {"content": "법조문", "metadata": {"law_type": "법률"}, "score": 0.5},
        ]
        return mock

    @pytest.fixture
    def service(self, mock_vectorstore):
        return RetrievalService(vectorstore_service=mock_vectorstore)

    async def test_vector_strategy_passes_filter(self, service, mock_vectorstore):
        """vector 전략에서 테이블 제외 필터가 전달된다."""
        await service.retrieve("접대비 한도", search_strategy="vector")
        call_args = mock_vectorstore.similarity_search.call_args
        assert call_args.kwargs.get("filter") is not None or call_args[1].get("filter") is not None
        passed_filter = call_args.kwargs.get("filter") or call_args[1].get("filter")
        assert "law_type" in passed_filter

    async def test_metadata_filter_strategy_merges_filter(self, service, mock_vectorstore):
        """metadata_filter 전략에서 기존 필터와 테이블 제외가 병합된다."""
        await service.retrieve(
            "접대비 한도",
            search_strategy="metadata_filter",
            metadata_filter={"topics": {"$contains": "경비"}},
        )
        call_args = mock_vectorstore.similarity_search.call_args
        passed_filter = call_args.kwargs.get("filter") or call_args[1].get("filter")
        assert "law_type" in passed_filter
        assert "topics" in passed_filter

    async def test_none_strategy_returns_empty(self, service, mock_vectorstore):
        """none 전략은 검색을 수행하지 않는다."""
        results = await service.retrieve("안녕", search_strategy="none")
        assert results == []
        mock_vectorstore.similarity_search.assert_not_called()
