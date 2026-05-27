from unittest.mock import AsyncMock, patch

import pytest

from app.core.config import Settings
from app.services.embedding_service import EmbeddingService


@pytest.fixture
def settings() -> Settings:
    return Settings(
        anthropic_api_key="test-key",
    )


@pytest.fixture
def service(settings: Settings) -> EmbeddingService:
    with patch("app.services.embedding_service.HuggingFaceEmbeddings"):
        return EmbeddingService(settings=settings)


class TestEmbedText:
    async def test_returns_list_of_float(self, service: EmbeddingService):
        """embed_text()가 list[float]를 반환한다."""
        fake_vector = [0.1] * 1536
        service.embeddings.aembed_query = AsyncMock(return_value=fake_vector)

        result = await service.embed_text("세금 환급 방법")

        assert isinstance(result, list)
        assert len(result) == 1536
        assert all(isinstance(v, float) for v in result)

    async def test_calls_aembed_query_with_text(self, service: EmbeddingService):
        """embed_text()가 aembed_query()를 올바른 인자로 호출한다."""
        fake_vector = [0.0] * 1536
        service.embeddings.aembed_query = AsyncMock(return_value=fake_vector)

        await service.embed_text("종합소득세")

        service.embeddings.aembed_query.assert_called_once_with("종합소득세")


class TestEmbedTexts:
    async def test_returns_list_of_vectors(self, service: EmbeddingService):
        """embed_texts()가 텍스트 수만큼 벡터 리스트를 반환한다."""
        texts = ["세금", "환급", "신고"]
        fake_vectors = [[0.1] * 1536] * len(texts)
        service.embeddings.aembed_documents = AsyncMock(return_value=fake_vectors)

        result = await service.embed_texts(texts)

        assert len(result) == len(texts)

    async def test_each_element_is_list_of_float(self, service: EmbeddingService):
        """embed_texts() 결과의 각 원소가 list[float]이다."""
        texts = ["부가가치세", "원천징수"]
        fake_vectors = [[0.2] * 1536, [0.3] * 1536]
        service.embeddings.aembed_documents = AsyncMock(return_value=fake_vectors)

        result = await service.embed_texts(texts)

        for vec in result:
            assert isinstance(vec, list)
            assert all(isinstance(v, float) for v in vec)

    async def test_calls_aembed_documents_with_texts(self, service: EmbeddingService):
        """embed_texts()가 aembed_documents()를 올바른 인자로 호출한다."""
        texts = ["세금 A", "세금 B"]
        service.embeddings.aembed_documents = AsyncMock(return_value=[[0.0] * 1536] * 2)

        await service.embed_texts(texts)

        service.embeddings.aembed_documents.assert_called_once_with(texts)


class TestCosineSimilarity:
    def test_identical_vectors_return_one(self):
        """동일한 벡터의 코사인 유사도는 1.0이다."""
        vec = [1.0, 0.0, 0.0]
        result = EmbeddingService.cosine_similarity(vec, vec)
        assert abs(result - 1.0) < 1e-6

    def test_orthogonal_vectors_return_zero(self):
        """직교 벡터의 코사인 유사도는 0.0이다."""
        vec_a = [1.0, 0.0, 0.0]
        vec_b = [0.0, 1.0, 0.0]
        result = EmbeddingService.cosine_similarity(vec_a, vec_b)
        assert abs(result - 0.0) < 1e-6

    def test_opposite_vectors_return_minus_one(self):
        """반대 방향 벡터의 코사인 유사도는 -1.0이다."""
        vec_a = [1.0, 0.0, 0.0]
        vec_b = [-1.0, 0.0, 0.0]
        result = EmbeddingService.cosine_similarity(vec_a, vec_b)
        assert abs(result - (-1.0)) < 1e-6

    def test_returns_float(self):
        """반환 타입이 float이다."""
        vec = [0.5, 0.5, 0.5]
        result = EmbeddingService.cosine_similarity(vec, vec)
        assert isinstance(result, float)
