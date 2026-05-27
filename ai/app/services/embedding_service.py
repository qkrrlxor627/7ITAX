import logging

import numpy as np
from langchain_huggingface import HuggingFaceEmbeddings

from app.core.config import Settings
from app.core.exceptions import EmbeddingError

logger = logging.getLogger(__name__)


class EmbeddingService:
    """텍스트 임베딩 생성 서비스.

    로컬 HuggingFace 모델 사용 (기본: BAAI/bge-m3, 멀티링궐).
    외부 API 키가 필요 없으며 첫 실행 시 모델을 다운로드해 캐시한다.
    """

    def __init__(self, settings: Settings) -> None:
        self.embeddings = HuggingFaceEmbeddings(
            model_name=settings.embedding_model,
            model_kwargs={"device": settings.embedding_device},
            encode_kwargs={"normalize_embeddings": True},
        )

    async def embed_text(self, text: str) -> list[float]:
        """단일 텍스트의 임베딩 벡터를 생성한다.

        출력: list[float] — 모델 차원 벡터 (bge-m3 기준 1024차원)
        """
        try:
            # HuggingFaceEmbeddings는 동기 구현이며, base 클래스가
            # aembed_query를 executor로 비동기 래핑한다.
            return await self.embeddings.aembed_query(text)
        except Exception as e:
            logger.error("임베딩 생성 실패: %s", e, exc_info=True)
            raise EmbeddingError() from e

    async def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """여러 텍스트의 임베딩 벡터를 배치로 생성한다."""
        try:
            return await self.embeddings.aembed_documents(texts)
        except Exception as e:
            logger.error("배치 임베딩 생성 실패: %s", e, exc_info=True)
            raise EmbeddingError() from e

    @staticmethod
    def cosine_similarity(vec_a: list[float], vec_b: list[float]) -> float:
        """두 벡터 간 코사인 유사도를 계산한다.

        출력: float (0.0 ~ 1.0)
        """
        a = np.array(vec_a)
        b = np.array(vec_b)
        return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b)))
