import pytest
from unittest.mock import AsyncMock, MagicMock

from app.services.retrieval_service import BM25Index, RetrievalService, SearchResult
from app.services.vectorstore import VectorStoreService


# ---------------------------------------------------------------------------
# BM25Index
# ---------------------------------------------------------------------------

class TestBM25Index:
    def test_is_not_built_initially(self):
        """초기 상태에서 is_built는 False이다."""
        idx = BM25Index()
        assert idx.is_built is False

    def test_build_marks_as_built(self):
        """build() 호출 후 is_built는 True이다."""
        idx = BM25Index()
        idx.build([{"content": "소득세 과세표준", "metadata": {}}])
        assert idx.is_built is True

    def test_search_returns_empty_when_not_built(self):
        """인덱스가 구축되지 않으면 search()는 빈 리스트를 반환한다."""
        idx = BM25Index()
        result = idx.search("소득세")
        assert result == []

    def test_search_returns_matching_documents(self):
        """알려진 문서에서 키워드 검색 결과가 반환된다."""
        idx = BM25Index()
        docs = [
            {"content": "종합소득세 과세표준 계산", "metadata": {"chunk_id": "doc1"}},
            {"content": "부가가치세 신고 절차", "metadata": {"chunk_id": "doc2"}},
            {"content": "종합소득세 세율표 적용", "metadata": {"chunk_id": "doc3"}},
        ]
        idx.build(docs)
        results = idx.search("종합소득세")
        assert len(results) > 0
        # 종합소득세 관련 문서가 상위에 있어야 함
        top_doc, top_score = results[0]
        assert "종합소득세" in top_doc["content"]

    def test_search_returns_score_greater_than_zero(self):
        """매칭된 문서의 점수는 0보다 크다.

        BM25Okapi는 IDF = log((N - n + 0.5) / (n + 0.5)) 공식을 사용하므로,
        검색어가 전체 문서 중 절반 미만에만 등장할 때 양의 점수를 얻는다.
        쿼리 단어가 3개 문서 중 1개에만 등장하면 IDF > 0 이 보장된다.
        """
        idx = BM25Index()
        idx.build([
            {"content": "세액공제 계산 방법", "metadata": {}},
            {"content": "부가가치세 신고 절차", "metadata": {}},
            {"content": "종합소득세 세율표", "metadata": {}},
        ])
        results = idx.search("세액공제")
        assert len(results) >= 1
        _, score = results[0]
        assert score > 0.0

    def test_search_excludes_zero_score_documents(self):
        """점수가 0인 문서는 결과에 포함되지 않는다."""
        idx = BM25Index()
        idx.build([
            {"content": "부가가치세 내용", "metadata": {}},
            {"content": "전혀 관련 없는 내용 xyz", "metadata": {}},
        ])
        results = idx.search("부가가치세")
        contents = [doc["content"] for doc, _ in results]
        assert "전혀 관련 없는 내용 xyz" not in contents

    def test_build_with_empty_documents(self):
        """빈 문서 리스트로 build하면 BM25Okapi 라이브러리 제약으로 ZeroDivisionError가 발생한다."""
        idx = BM25Index()
        with pytest.raises(ZeroDivisionError):
            idx.build([])

    def test_search_top_k_limits_results(self):
        """top_k 파라미터가 결과 수를 제한한다."""
        idx = BM25Index()
        docs = [{"content": f"소득세 문서 {i}", "metadata": {}} for i in range(10)]
        idx.build(docs)
        results = idx.search("소득세", top_k=3)
        assert len(results) <= 3


# ---------------------------------------------------------------------------
# RetrievalService._apply_metadata_filter
# ---------------------------------------------------------------------------

class TestApplyMetadataFilter:
    def setup_method(self):
        mock_vs = MagicMock(spec=VectorStoreService)
        self.service = RetrievalService(vectorstore_service=mock_vs)

    def _make_results(self, items: list[dict]) -> list[tuple[dict, float]]:
        return [(item, 1.0) for item in items]

    def test_none_filter_returns_all(self):
        """filter가 None이면 모든 결과를 반환한다."""
        results = self._make_results([
            {"content": "a", "metadata": {"law_name": "소득세법"}},
            {"content": "b", "metadata": {"law_name": "부가가치세법"}},
        ])
        filtered = self.service._apply_metadata_filter(results, None)
        assert len(filtered) == 2

    def test_in_operator_filters_correctly(self):
        """$in 연산자로 특정 값 목록에 속하는 문서만 반환한다."""
        results = self._make_results([
            {"content": "a", "metadata": {"law_name": "소득세법"}},
            {"content": "b", "metadata": {"law_name": "부가가치세법"}},
            {"content": "c", "metadata": {"law_name": "조세특례제한법"}},
        ])
        filt = {"law_name": {"$in": ["소득세법", "부가가치세법"]}}
        filtered = self.service._apply_metadata_filter(results, filt)
        assert len(filtered) == 2
        contents = [doc["content"] for doc, _ in filtered]
        assert "a" in contents
        assert "b" in contents

    def test_contains_operator_filters_correctly(self):
        """$contains 연산자로 특정 문자열을 포함하는 문서만 반환한다."""
        results = self._make_results([
            {"content": "a", "metadata": {"topics": "세율,과세표준"}},
            {"content": "b", "metadata": {"topics": "신고,절차"}},
        ])
        filt = {"topics": {"$contains": "세율"}}
        filtered = self.service._apply_metadata_filter(results, filt)
        assert len(filtered) == 1
        assert filtered[0][0]["content"] == "a"

    def test_equality_filter(self):
        """단일 값 조건으로 정확히 일치하는 문서만 반환한다."""
        results = self._make_results([
            {"content": "a", "metadata": {"tax_type": "소득세"}},
            {"content": "b", "metadata": {"tax_type": "부가가치세"}},
        ])
        filt = {"tax_type": "소득세"}
        filtered = self.service._apply_metadata_filter(results, filt)
        assert len(filtered) == 1
        assert filtered[0][0]["content"] == "a"

    def test_missing_key_excluded(self):
        """필터 키가 메타데이터에 없으면 해당 문서는 제외된다."""
        results = self._make_results([
            {"content": "a", "metadata": {}},
            {"content": "b", "metadata": {"law_name": "소득세법"}},
        ])
        filt = {"law_name": {"$in": ["소득세법"]}}
        filtered = self.service._apply_metadata_filter(results, filt)
        assert len(filtered) == 1
        assert filtered[0][0]["content"] == "b"


# ---------------------------------------------------------------------------
# RetrievalService._rrf_fusion
# ---------------------------------------------------------------------------

class TestRRFFusion:
    def setup_method(self):
        mock_vs = MagicMock(spec=VectorStoreService)
        self.service = RetrievalService(vectorstore_service=mock_vs)

    def _make_result(self, chunk_id: str, content: str = "", score: float = 1.0) -> SearchResult:
        return SearchResult(content=content, metadata={"chunk_id": chunk_id}, score=score)

    def test_empty_lists_returns_empty(self):
        """두 리스트가 모두 비어있으면 빈 리스트를 반환한다."""
        result = self.service._rrf_fusion([], [], top_k=5)
        assert result == []

    def test_single_list_a(self):
        """results_b가 비어있으면 results_a의 점수로만 RRF 점수를 계산한다."""
        a = [self._make_result("doc1"), self._make_result("doc2")]
        result = self.service._rrf_fusion(a, [], top_k=5)
        assert len(result) == 2

    def test_shared_document_scores_sum(self):
        """양쪽에 존재하는 문서의 RRF 점수는 두 점수의 합이다."""
        a = [self._make_result("shared_doc")]
        b = [self._make_result("shared_doc")]
        result = self.service._rrf_fusion(a, b, top_k=5)
        assert len(result) == 1
        # rank=0이지만 구현에서 rank+1 사용: score = 1/(60+1) + 1/(60+1) = 2/61
        expected_score = 2 / (60 + 1)
        assert abs(result[0].score - expected_score) < 1e-9

    def test_top_k_limits_results(self):
        """top_k 파라미터가 반환 결과 수를 제한한다."""
        a = [self._make_result(f"a{i}") for i in range(10)]
        b = [self._make_result(f"b{i}") for i in range(10)]
        result = self.service._rrf_fusion(a, b, top_k=3)
        assert len(result) == 3

    def test_higher_rank_gets_higher_score(self):
        """순위가 높을수록(앞에 있을수록) RRF 점수가 높다."""
        a = [self._make_result("top_doc"), self._make_result("bottom_doc")]
        result = self.service._rrf_fusion(a, [], top_k=2)
        assert result[0].metadata["chunk_id"] == "top_doc"
        assert result[0].score > result[1].score

    def test_result_score_is_rrf_score(self):
        """반환된 SearchResult의 score는 원본 score가 아닌 RRF 점수이다."""
        a = [SearchResult(content="내용", metadata={"chunk_id": "doc1"}, score=0.99)]
        result = self.service._rrf_fusion(a, [], top_k=5)
        # RRF score = 1/(60+1) ≈ 0.0164, 0.99가 아님
        assert result[0].score < 0.1


# ---------------------------------------------------------------------------
# RetrievalService.retrieve (검색 전략 분기)
# ---------------------------------------------------------------------------

class TestRetrieveStrategies:
    def setup_method(self):
        self.mock_vs = MagicMock(spec=VectorStoreService)
        self.mock_vs.similarity_search.return_value = []
        self.bm25 = BM25Index()
        self.service = RetrievalService(
            vectorstore_service=self.mock_vs,
            bm25_index=self.bm25,
        )

    async def test_strategy_none_returns_empty(self):
        """search_strategy='none'이면 빈 리스트를 반환한다."""
        result = await self.service.retrieve("질문", search_strategy="none")
        assert result == []
        self.mock_vs.similarity_search.assert_not_called()

    async def test_strategy_vector_calls_vector_search(self):
        """search_strategy='vector'이면 벡터 검색만 수행한다."""
        result = await self.service.retrieve("종합소득세", search_strategy="vector")
        self.mock_vs.similarity_search.assert_called_once()

    async def test_strategy_metadata_filter_calls_vector_search(self):
        """search_strategy='metadata_filter'이면 벡터 검색을 수행한다."""
        result = await self.service.retrieve("세율", search_strategy="metadata_filter")
        self.mock_vs.similarity_search.assert_called_once()

    async def test_strategy_multi_query_calls_vector_search(self):
        """search_strategy='multi_query'이면 벡터 검색을 수행한다."""
        result = await self.service.retrieve("간이과세자 일반과세자", search_strategy="multi_query")
        self.mock_vs.similarity_search.assert_called_once()

    async def test_strategy_hybrid_is_default(self):
        """기본 search_strategy는 'hybrid'이다."""
        # hybrid는 vector search를 호출한다
        result = await self.service.retrieve("종합소득세")
        self.mock_vs.similarity_search.assert_called()

    async def test_strategy_hybrid_merges_bm25_and_vector(self):
        """hybrid 전략: BM25와 벡터 검색 결과가 RRF로 융합된다."""
        # BM25 인덱스에 문서 구축
        docs = [{"content": "종합소득세 과세표준", "metadata": {"chunk_id": "bm25_doc"}}]
        self.bm25.build(docs)

        # 벡터 검색 결과 mock
        self.mock_vs.similarity_search.return_value = [
            {"content": "벡터 검색 문서", "metadata": {"chunk_id": "vector_doc"}, "score": 0.9}
        ]

        result = await self.service.retrieve("종합소득세", search_strategy="hybrid")
        # BM25 + 벡터 결과가 융합되어 반환됨
        assert isinstance(result, list)


# ---------------------------------------------------------------------------
# RetrievalService.retrieve — hybrid_with_be_data 전략
# ---------------------------------------------------------------------------

class TestRetrieveHybridWithBeData:
    def setup_method(self):
        self.mock_vs = MagicMock(spec=VectorStoreService)
        self.mock_vs.similarity_search.return_value = []
        self.bm25 = BM25Index()
        self.service = RetrievalService(
            vectorstore_service=self.mock_vs,
            bm25_index=self.bm25,
        )

    async def test_hybrid_with_be_data_includes_be_context(self):
        """hybrid_with_be_data 전략은 BE 데이터를 결과 앞에 포함해야 한다."""
        be_data = {
            "transactions": [{"date": "2025-01", "amount": 100000, "merchant": "주유소"}],
            "business_info": {"type": "개인사업자", "industry": "운수업"},
        }

        results = await self.service.retrieve(
            query="경비 공제 받을 수 있는 항목은?",
            search_strategy="hybrid_with_be_data",
            be_data=be_data,
        )

        assert results is not None
        assert len(results) >= 1
        assert results[0].metadata.get("source") == "backend_data"

    async def test_hybrid_with_be_data_empty_be_data_equals_hybrid(self):
        """be_data가 비어있으면 일반 hybrid와 동일한 결과를 반환한다."""
        self.mock_vs.similarity_search.return_value = [
            {"content": "세금 신고 방법", "metadata": {"chunk_id": "doc1"}, "score": 0.9}
        ]

        results_hybrid = await self.service.retrieve(
            query="세금 신고 방법", search_strategy="hybrid"
        )
        results_be = await self.service.retrieve(
            query="세금 신고 방법", search_strategy="hybrid_with_be_data", be_data={}
        )

        assert results_hybrid == results_be

    async def test_unknown_strategy_raises_value_error(self):
        """정의되지 않은 strategy는 ValueError를 발생시켜야 한다."""
        with pytest.raises(ValueError, match="알 수 없는 검색 전략"):
            await self.service.retrieve(
                query="세금 신고",
                search_strategy="invalid_strategy_xyz",
            )


# ---------------------------------------------------------------------------
# RetrievalService.retrieve — 기존 전략 회귀 확인
# ---------------------------------------------------------------------------

class TestRetrieveStrategyRegression:
    """hybrid_with_be_data 추가 및 ValueError 도입 후 기존 전략 회귀 확인."""

    def setup_method(self):
        self.mock_vs = MagicMock(spec=VectorStoreService)
        self.mock_vs.similarity_search.return_value = []
        self.bm25 = BM25Index()
        self.service = RetrievalService(
            vectorstore_service=self.mock_vs,
            bm25_index=self.bm25,
        )

    async def test_vector_strategy_calls_vector_search(self):
        """strategy='vector' 변경 없이 벡터 검색을 수행한다."""
        await self.service.retrieve(query="세금", search_strategy="vector")
        self.mock_vs.similarity_search.assert_called_once()

    async def test_hybrid_strategy_explicit_still_works(self):
        """strategy='hybrid'가 명시적 분기로 변경된 후에도 정상 동작한다."""
        result = await self.service.retrieve(query="세금", search_strategy="hybrid")
        assert isinstance(result, list)

    async def test_hybrid_default_still_works(self):
        """search_strategy 기본값(hybrid)이 변경 없이 동작한다."""
        result = await self.service.retrieve(query="세금")
        assert isinstance(result, list)
