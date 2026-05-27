from dataclasses import dataclass

from rank_bm25 import BM25Okapi

from app.services.reranker_service import RerankerService
from app.services.vectorstore import VectorStoreService
from app.utils.korean_tokenizer import KoreanTokenizer


@dataclass
class SearchResult:
    """검색 결과 단위.

    아키텍처 설계 문서 6.2절의 SearchResult 스펙을 따른다.
    """

    content: str    # 청크 텍스트
    metadata: dict  # {law_name, law_type, tax_type, chunk_id, ...}
    score: float    # 유사도 점수 (0.0 ~ 1.0)


class BM25Index:
    """BM25 인메모리 인덱스.

    앱 기동 시 ChromaDB에서 전체 청크를 로드하여 구축한다.
    법률 용어의 키워드 정확 매칭에 사용된다.
    """

    def __init__(self) -> None:
        self.bm25: BM25Okapi | None = None
        self.documents: list[dict] = []
        self._tokenizer = KoreanTokenizer()

    def build(self, documents: list[dict]) -> None:
        """BM25 인덱스를 구축한다.

        documents: [{"content": str, "metadata": dict}, ...]
        """
        self.documents = documents
        tokenized = [self._tokenizer.tokenize_for_bm25(doc["content"]) for doc in documents]
        self.bm25 = BM25Okapi(tokenized)

    def search(self, query: str, top_k: int = 20) -> list[tuple[dict, float]]:
        """BM25 키워드 검색을 수행한다.

        출력: [(document_dict, score), ...] 상위 top_k개
        """
        if self.bm25 is None:
            return []

        tokenized_query = self._tokenizer.tokenize_for_bm25(query)
        scores = self.bm25.get_scores(tokenized_query)
        top_indices = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)[:top_k]
        return [(self.documents[i], float(scores[i])) for i in top_indices if scores[i] > 0]

    @property
    def is_built(self) -> bool:
        """BM25 인덱스 구축 여부를 확인한다.

        Returns:
            bool: 인덱스가 구축되었으면 True, 아니면 False.
        """
        return self.bm25 is not None


class RetrievalService:
    """검색 서비스.

    Phase 1: 벡터 검색만 (ChromaDB similarity_search)
    Phase 2: 하이브리드 검색 추가 (BM25 + RRF)
    """

    RRF_K = 60
    MIN_VECTOR_RELEVANCE = 0.25  # 벡터 검색 코사인 유사도 최소 임계치

    def __init__(
        self,
        vectorstore_service: VectorStoreService,
        bm25_index: BM25Index | None = None,
        reranker: RerankerService | None = None,
    ) -> None:
        self.vectorstore = vectorstore_service
        self.bm25_index = bm25_index or BM25Index()
        self.reranker = reranker

    async def retrieve(
        self,
        query: str,
        top_k: int = 5,
        metadata_filter: dict | None = None,
        search_strategy: str = "hybrid",
        be_data: dict | None = None,
    ) -> list[SearchResult]:
        """인텐트에 따른 검색 전략 분기.

        search_strategy 옵션:
        - "hybrid": BM25 + 벡터 + RRF (기본)
        - "hybrid_with_be_data": hybrid + 백엔드 데이터 컨텍스트 포함
        - "vector": 벡터 검색만
        - "metadata_filter": 메타데이터 필터 중심
        - "multi_query": 비교 대상별 다중 쿼리
        - "none": 검색 생략
        """
        if search_strategy == "none":
            return []

        # 기본: 테이블 데이터 제외 (경비율표 청크가 법조문 검색을 방해하지 않도록)
        effective_filter = self._build_effective_filter(metadata_filter)

        if search_strategy == "vector":
            results = await self._vector_search(query, top_k, effective_filter)
            return self._apply_score_threshold(results)

        if search_strategy == "metadata_filter":
            results = await self._filtered_search(query, top_k, effective_filter)
            return self._apply_score_threshold(results)

        if search_strategy == "multi_query":
            return await self._multi_query_search(query, top_k, effective_filter)

        if search_strategy == "hybrid":
            return await self._hybrid_search(query, top_k, effective_filter)

        if search_strategy == "hybrid_with_be_data":
            results = await self._hybrid_search(query, top_k, effective_filter)
            return self._enrich_with_be_data(results, be_data or {})

        raise ValueError(f"알 수 없는 검색 전략: {search_strategy}")

    def _build_effective_filter(self, metadata_filter: dict | None) -> dict | None:
        """기본 필터를 적용한다.

        테이블 타입(경비율표 등)을 기본 제외하여
        법조문 검색 시 경비율표 청크가 상위를 점유하는 문제를 방지한다.
        사용자가 명시적으로 law_type 필터를 지정한 경우 그대로 사용한다.
        """
        default_exclude = {"law_type": {"$ne": "테이블"}}
        if metadata_filter is None:
            return default_exclude
        if "law_type" in metadata_filter:
            return metadata_filter
        return {**default_exclude, **metadata_filter}

    def _apply_score_threshold(self, results: list[SearchResult]) -> list[SearchResult]:
        """벡터 검색 결과에서 최소 관련성 점수 미만 결과를 필터링한다."""
        return [r for r in results if r.score >= self.MIN_VECTOR_RELEVANCE]

    def _enrich_with_be_data(
        self,
        results: list[SearchResult],
        be_data: dict,
    ) -> list[SearchResult]:
        """BE 데이터를 검색 결과 컨텍스트에 prepend한다.

        be_data가 비어있으면 results를 그대로 반환한다.
        """
        if not be_data:
            return results

        be_parts = []
        if be_data.get("transactions"):
            be_parts.append(f"거래내역: {be_data['transactions']}")
        if be_data.get("business_info"):
            be_parts.append(f"사업자정보: {be_data['business_info']}")

        if not be_parts:
            return results

        be_result = SearchResult(
            content="\n".join(be_parts),
            metadata={"source": "backend_data"},
            score=1.0,
        )
        return [be_result] + results

    async def _hybrid_search(
        self,
        query: str,
        top_k: int,
        metadata_filter: dict | None,
    ) -> list[SearchResult]:
        """BM25 + 벡터 + RRF 하이브리드 검색 (+ Reranker)."""
        bm25_results = self._bm25_search(query, top_k=20, metadata_filter=metadata_filter)
        vector_results = await self._vector_search(query, top_k=20, metadata_filter=metadata_filter)
        rrf_results = self._rrf_fusion(bm25_results, vector_results, top_k=top_k * 2)

        if self.reranker is not None:
            return self._rerank(query, rrf_results, top_k)

        return rrf_results[:top_k]

    def _rerank(
        self,
        query: str,
        results: list[SearchResult],
        top_k: int,
    ) -> list[SearchResult]:
        """Cross-Encoder로 검색 결과를 재정렬한다."""
        if not results:
            return []

        docs = [
            {"content": r.content, "metadata": r.metadata, "score": r.score}
            for r in results
        ]

        reranked = self.reranker.rerank(query=query, documents=docs, top_k=top_k)

        return [
            SearchResult(
                content=doc["content"],
                metadata=doc["metadata"],
                score=doc.get("rerank_score", doc["score"]),
            )
            for doc in reranked
        ]

    async def _filtered_search(
        self,
        query: str,
        top_k: int,
        metadata_filter: dict | None,
    ) -> list[SearchResult]:
        """메타데이터 필터 중심 검색 (TAX_RATE_LOOKUP)."""
        return await self._vector_search(query, top_k, metadata_filter)

    async def _multi_query_search(
        self,
        query: str,
        top_k: int,
        metadata_filter: dict | None = None,
    ) -> list[SearchResult]:
        """다중 쿼리 검색 (COMPARISON). MVP: 단일 쿼리로 검색."""
        return await self._vector_search(query, top_k, metadata_filter)

    def _bm25_search(
        self,
        query: str,
        top_k: int = 20,
        metadata_filter: dict | None = None,
    ) -> list[SearchResult]:
        """BM25 키워드 검색 + 메타데이터 필터링."""
        raw = self.bm25_index.search(query, top_k=top_k * 2)
        if metadata_filter:
            raw = self._apply_metadata_filter(raw, metadata_filter)
        raw = raw[:top_k]
        return [
            SearchResult(
                content=doc["content"],
                metadata=doc["metadata"],
                score=score,
            )
            for doc, score in raw
        ]

    def _apply_metadata_filter(
        self,
        results: list[tuple[dict, float]],
        metadata_filter: dict | None,
    ) -> list[tuple[dict, float]]:
        """메타데이터 필터를 적용한다.

        지원 연산자:
        - {"$in": [v1, v2]}: metadata[key] in values
        - {"$contains": v}: v in metadata[key]
        - 단일 값: metadata[key] == value
        """
        if metadata_filter is None:
            return results

        filtered = []
        for doc, score in results:
            meta = doc.get("metadata", {})
            match = True
            for key, condition in metadata_filter.items():
                if isinstance(condition, dict):
                    if "$in" in condition:
                        if meta.get(key) not in condition["$in"]:
                            match = False
                            break
                    elif "$nin" in condition:
                        if meta.get(key) in condition["$nin"]:
                            match = False
                            break
                    elif "$ne" in condition:
                        if meta.get(key) == condition["$ne"]:
                            match = False
                            break
                    elif "$contains" in condition:
                        if condition["$contains"] not in str(meta.get(key, "")):
                            match = False
                            break
                else:
                    if meta.get(key) != condition:
                        match = False
                        break
            if match:
                filtered.append((doc, score))

        return filtered

    def _rrf_fusion(
        self,
        results_a: list[SearchResult],
        results_b: list[SearchResult],
        top_k: int,
    ) -> list[SearchResult]:
        """Reciprocal Rank Fusion.

        공식: score(d) = sum(1 / (k + rank_i))
        """
        scores: dict[str, float] = {}
        docs: dict[str, SearchResult] = {}

        for rank, result in enumerate(results_a):
            doc_id = result.metadata.get("chunk_id", result.content[:50])
            scores[doc_id] = scores.get(doc_id, 0) + 1 / (self.RRF_K + rank + 1)
            docs[doc_id] = result

        for rank, result in enumerate(results_b):
            doc_id = result.metadata.get("chunk_id", result.content[:50])
            scores[doc_id] = scores.get(doc_id, 0) + 1 / (self.RRF_K + rank + 1)
            docs[doc_id] = result

        sorted_ids = sorted(scores, key=scores.__getitem__, reverse=True)[:top_k]
        return [
            SearchResult(
                content=docs[doc_id].content,
                metadata=docs[doc_id].metadata,
                score=scores[doc_id],
            )
            for doc_id in sorted_ids
        ]

    async def _vector_search(
        self,
        query: str,
        top_k: int,
        metadata_filter: dict | None,
    ) -> list[SearchResult]:
        """ChromaDB 벡터 유사도 검색."""
        raw_results = self.vectorstore.similarity_search(
            query=query,
            k=top_k,
            filter=metadata_filter,
        )
        return [
            SearchResult(
                content=r["content"],
                metadata=r["metadata"],
                score=r["score"],
            )
            for r in raw_results
        ]
