import logging
from functools import lru_cache

from langchain_huggingface import HuggingFaceEmbeddings

logger = logging.getLogger(__name__)


@lru_cache(maxsize=None)
def get_embeddings(model_name: str, device: str) -> HuggingFaceEmbeddings:
    """로컬 임베딩 모델을 프로세스당 한 번만 로드해 공유한다.

    VectorStoreService와 EmbeddingService가 같은 (model_name, device)로 호출하면
    동일 인스턴스를 재사용하므로 bge-m3(~2GB)를 중복 로드하지 않는다.
    """
    logger.info("임베딩 모델 로드: %s (device=%s)", model_name, device)
    return HuggingFaceEmbeddings(
        model_name=model_name,
        model_kwargs={"device": device},
        encode_kwargs={"normalize_embeddings": True},
    )
