from datetime import timedelta
from unittest.mock import AsyncMock

import numpy as np
import pytest

from app.services.cache_service import CacheEntry, SemanticCache


@pytest.fixture
def mock_embedding_service() -> AsyncMock:
    """embed_text 호출별로 고정 벡터를 반환하는 Mock EmbeddingService."""
    service = AsyncMock()

    async def embed_text_side_effect(text: str) -> list[float]:
        if "쿼리A" in text and "거의 동일" in text:
            # 정규화 시 쿼리A와 높은 유사도를 가지는 벡터
            return [0.99, 0.1, 0.0]
        elif "쿼리A" in text:
            return [1.0, 0.0, 0.0]
        elif "쿼리B" in text:
            return [0.0, 1.0, 0.0]
        else:
            return [0.0, 0.0, 1.0]

    service.embed_text.side_effect = embed_text_side_effect
    return service


@pytest.fixture
def cache(mock_embedding_service: AsyncMock) -> SemanticCache:
    return SemanticCache(
        embedding_service=mock_embedding_service,
        threshold=0.95,
        max_entries=10000,
        ttl_hours=24,
    )


class TestSemanticCacheGet:
    async def test_cache_miss_returns_none(self, cache: SemanticCache):
        """빈 캐시에서 get()은 None을 반환한다."""
        result = await cache.get("쿼리A")
        assert result is None

    async def test_cache_hit_returns_response(
        self,
        cache: SemanticCache,
        mock_embedding_service: AsyncMock,
    ):
        """put 후 동일 벡터로 get() 시 저장된 응답을 반환한다."""
        await cache.put("쿼리A", "응답 내용", "general")

        # 동일한 쿼리로 조회 (동일 벡터 반환 → 유사도 1.0)
        result = await cache.get("쿼리A")
        assert result == "응답 내용"

    async def test_low_similarity_returns_none(
        self,
        cache: SemanticCache,
    ):
        """유사도가 threshold 미만이면 None을 반환한다."""
        await cache.put("쿼리A", "응답 내용", "general")

        # 쿼리B는 쿼리A와 직교 → 유사도 0.0
        result = await cache.get("쿼리B")
        assert result is None

    async def test_hit_count_incremented(
        self,
        cache: SemanticCache,
    ):
        """캐시 히트 시 해당 entry의 hit_count가 1 증가한다."""
        await cache.put("쿼리A", "응답 내용", "general")
        assert cache.entries[0].hit_count == 0

        await cache.get("쿼리A")
        assert cache.entries[0].hit_count == 1

        await cache.get("쿼리A")
        assert cache.entries[0].hit_count == 2


class TestSemanticCachePut:
    async def test_put_stores_entry(self, cache: SemanticCache):
        """put() 후 entries 길이가 1이 된다."""
        assert len(cache.entries) == 0
        await cache.put("쿼리A", "응답", "general")
        assert len(cache.entries) == 1

    async def test_put_builds_matrix(self, cache: SemanticCache):
        """put() 후 _embeddings_matrix가 생성된다."""
        assert cache._embeddings_matrix is None
        await cache.put("쿼리A", "응답", "general")
        assert cache._embeddings_matrix is not None
        assert cache._embeddings_matrix.shape == (1, 3)

    async def test_max_entries_eviction(self, mock_embedding_service: AsyncMock):
        """max_entries 초과 시 오래된 항목이 먼저 제거된다."""
        cache = SemanticCache(
            embedding_service=mock_embedding_service,
            threshold=0.95,
            max_entries=2,
            ttl_hours=24,
        )

        # 서로 다른 쿼리 3개 추가
        mock_embedding_service.embed_text.side_effect = [
            [1.0, 0.0, 0.0],
            [0.0, 1.0, 0.0],
            [0.0, 0.0, 1.0],
        ]
        await cache.put("첫번째", "응답1", "general")
        await cache.put("두번째", "응답2", "general")
        await cache.put("세번째", "응답3", "general")

        # max_entries=2이므로 가장 오래된 "첫번째" 응답이 제거됨
        assert len(cache.entries) == 2
        responses = [e.response for e in cache.entries]
        assert "응답1" not in responses
        assert "응답2" in responses
        assert "응답3" in responses

    async def test_put_stores_intent(self, cache: SemanticCache):
        """put() 시 인텐트 정보가 entry에 저장된다."""
        await cache.put("쿼리A", "응답", "tax_query")
        assert cache.entries[0].intent == "tax_query"


class TestSemanticCacheExpiry:
    async def test_expired_entries_evicted(
        self,
        cache: SemanticCache,
        mock_embedding_service: AsyncMock,
    ):
        """TTL이 지난 항목은 get() 호출 시 제거된다."""
        await cache.put("쿼리A", "응답 내용", "general")
        assert len(cache.entries) == 1

        # created_at을 과거로 조작하여 TTL 초과 시뮬레이션
        cache.entries[0].created_at = cache.entries[0].created_at - timedelta(hours=25)

        # get() 내부에서 _evict_expired() 호출 → 만료 항목 제거
        result = await cache.get("쿼리A")

        assert result is None
        assert len(cache.entries) == 0

    async def test_non_expired_entries_retained(
        self,
        cache: SemanticCache,
    ):
        """TTL이 지나지 않은 항목은 유지된다."""
        await cache.put("쿼리A", "응답 내용", "general")
        # created_at을 TTL 내로 유지 (조작 없음)

        result = await cache.get("쿼리A")
        assert result == "응답 내용"


class TestSemanticCacheStats:
    def test_get_stats_empty(self, cache: SemanticCache):
        """빈 캐시의 통계: total_entries=0, total_hits=0, intent_distribution={}."""
        stats = cache.get_stats()

        assert stats["total_entries"] == 0
        assert stats["total_hits"] == 0
        assert stats["intent_distribution"] == {}

    async def test_get_stats_with_entries(
        self,
        cache: SemanticCache,
        mock_embedding_service: AsyncMock,
    ):
        """항목 추가 후 통계가 올바르게 집계된다."""
        mock_embedding_service.embed_text.side_effect = [
            [1.0, 0.0, 0.0],
            [0.0, 1.0, 0.0],
            [0.0, 0.0, 1.0],
        ]
        await cache.put("질문1", "응답1", "tax_query")
        await cache.put("질문2", "응답2", "tax_query")
        await cache.put("질문3", "응답3", "general")

        # hit_count 수동 설정
        cache.entries[0].hit_count = 3
        cache.entries[1].hit_count = 1

        stats = cache.get_stats()

        assert stats["total_entries"] == 3
        assert stats["total_hits"] == 4  # 3 + 1 + 0
        assert stats["intent_distribution"] == {"tax_query": 2, "general": 1}

    async def test_get_stats_intent_distribution(self, cache: SemanticCache):
        """인텐트별 항목 수가 정확히 집계된다."""
        await cache.put("쿼리A", "응답1", "tax_query")

        stats = cache.get_stats()
        assert stats["intent_distribution"]["tax_query"] == 1
        assert "general" not in stats["intent_distribution"]
