from dataclasses import dataclass, field
from datetime import datetime, timedelta

import numpy as np

from app.services.embedding_service import EmbeddingService


@dataclass
class CacheEntry:
    """시맨틱 캐시의 단일 항목.

    query_embedding과 response를 저장하여 임베딩 유사도 기반 캐시 히트 판정에 사용된다.

    Attributes:
        query_embedding: 질문의 임베딩 벡터 (numpy array).
        response: 캐시된 응답 텍스트.
        intent: 사용자 인텐트.
        created_at: 캐시 생성 시간.
        hit_count: 캐시 히트 횟수.
    """

    query_embedding: np.ndarray
    response: str
    intent: str
    created_at: datetime = field(default_factory=datetime.now)
    hit_count: int = 0


class SemanticCache:
    """시맨틱 유사도 기반 응답 캐시.

    질문 임베딩 간 코사인 유사도가 threshold 이상이면 캐시 히트로 판단하여
    LLM 호출 없이 저장된 응답을 반환한다.
    """

    DEFAULT_THRESHOLD = 0.95
    DEFAULT_MAX_ENTRIES = 10000
    DEFAULT_TTL_HOURS = 24

    def __init__(
        self,
        embedding_service: EmbeddingService,
        threshold: float = DEFAULT_THRESHOLD,
        max_entries: int = DEFAULT_MAX_ENTRIES,
        ttl_hours: int = DEFAULT_TTL_HOURS,
    ) -> None:
        self.embedding_service = embedding_service
        self.threshold = threshold
        self.max_entries = max_entries
        self.ttl = timedelta(hours=ttl_hours)
        self.entries: list[CacheEntry] = []
        self._embeddings_matrix: np.ndarray | None = None

    async def get(self, query: str) -> str | None:
        """캐시에서 유사 질문의 응답을 조회한다.

        1. 만료 항목 제거
        2. 엔트리 없으면 None 반환
        3. 질문 임베딩 생성
        4. 배치 코사인 유사도 계산
        5. 최고 유사도 >= threshold 이면 응답 반환 (hit_count 증가)
        6. 아니면 None 반환
        """
        self._evict_expired()

        if not self.entries or self._embeddings_matrix is None:
            return None

        query_embedding = np.array(await self.embedding_service.embed_text(query))
        q_norm = np.linalg.norm(query_embedding)
        if q_norm == 0:
            return None

        # 배치 코사인 유사도: (matrix @ q) / (row_norms * q_norm)
        norms = np.linalg.norm(self._embeddings_matrix, axis=1)
        # 0 나눗셈 방지
        safe_norms = np.where(norms == 0, 1e-10, norms)
        similarities = (self._embeddings_matrix @ query_embedding) / (safe_norms * q_norm)

        best_idx = int(np.argmax(similarities))
        if similarities[best_idx] >= self.threshold:
            self.entries[best_idx].hit_count += 1
            return self.entries[best_idx].response

        return None

    async def put(self, query: str, response: str, intent: str) -> None:
        """질문-응답 쌍을 캐시에 저장한다.

        1. 질문 임베딩 생성
        2. CacheEntry 생성 후 리스트에 추가
        3. max_entries 초과 시 오래된 항목부터 제거
        4. 임베딩 행렬 재구축
        """
        query_embedding = np.array(await self.embedding_service.embed_text(query))
        entry = CacheEntry(
            query_embedding=query_embedding,
            response=response,
            intent=intent,
        )
        self.entries.append(entry)

        if len(self.entries) > self.max_entries:
            excess = len(self.entries) - self.max_entries
            self.entries = self.entries[excess:]

        self._rebuild_matrix()

    def _evict_expired(self) -> None:
        """TTL이 지난 항목을 제거하고 행렬을 재구축한다."""
        now = datetime.now()
        self.entries = [e for e in self.entries if now - e.created_at < self.ttl]
        self._rebuild_matrix()

    def _rebuild_matrix(self) -> None:
        """모든 entry의 임베딩을 2D 행렬로 쌓는다."""
        if self.entries:
            self._embeddings_matrix = np.vstack([e.query_embedding for e in self.entries])
        else:
            self._embeddings_matrix = None

    def get_stats(self) -> dict:
        """캐시 통계를 반환한다.

        반환값:
            total_entries: 현재 캐시 항목 수
            total_hits: 전체 히트 횟수 합산
            intent_distribution: 인텐트별 항목 수
        """
        total_hits = sum(e.hit_count for e in self.entries)

        intent_distribution: dict[str, int] = {}
        for entry in self.entries:
            intent_distribution[entry.intent] = intent_distribution.get(entry.intent, 0) + 1

        return {
            "total_entries": len(self.entries),
            "total_hits": total_hits,
            "intent_distribution": intent_distribution,
        }
