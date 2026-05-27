import logging

from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings

from app.core.config import Settings
from app.core.exceptions import VectorStoreError

logger = logging.getLogger(__name__)


class VectorStoreService:
    """ChromaDB 기반 벡터 저장소 서비스."""

    COLLECTION_NAME = "tax_laws"

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.embeddings = OpenAIEmbeddings(
            model=settings.embedding_model,
            openai_api_key=settings.gms_api_key,
            openai_api_base=settings.gms_base_url,
            chunk_size=10,
            check_embedding_ctx_length=False,
        )
        self.vectorstore = Chroma(
            collection_name=self.COLLECTION_NAME,
            embedding_function=self.embeddings,
            persist_directory=settings.chroma_persist_directory,
        )

    def add_documents(
        self,
        texts: list[str],
        metadatas: list[dict],
        ids: list[str] | None = None,
    ) -> None:
        """청크 텍스트와 메타데이터를 벡터 저장소에 추가한다."""
        self.vectorstore.add_texts(
            texts=texts,
            metadatas=metadatas,
            ids=ids,
        )

    @staticmethod
    def convert_filter(metadata_filter: dict | None) -> dict | None:
        """범용 메타데이터 필터를 ChromaDB where 필터로 변환한다.

        입력 형식:
            {"law_name": {"$in": ["소득세법"]}, "topics": {"$contains": "세율"}}

        ChromaDB 출력 형식:
            단일 조건: {"law_name": {"$in": [...]}}
            복수 조건: {"$and": [{...}, {...}]}
        """
        if metadata_filter is None:
            return None

        conditions = []
        for key, value in metadata_filter.items():
            if isinstance(value, dict):
                conditions.append({key: value})
            elif isinstance(value, list):
                conditions.append({key: {"$in": value}})
            else:
                conditions.append({key: {"$eq": value}})

        if len(conditions) == 1:
            return conditions[0]
        return {"$and": conditions}

    def similarity_search(
        self,
        query: str,
        k: int = 5,
        filter: dict | None = None,
    ) -> list[dict]:
        """쿼리와 유사한 문서를 검색한다.

        filter를 ChromaDB 형식으로 변환 후 검색한다.

        Returns:
            [{"content": str, "metadata": dict, "score": float}, ...]
        """
        chroma_filter = self.convert_filter(filter)
        try:
            results = self.vectorstore.similarity_search_with_relevance_scores(
                query=query,
                k=k,
                filter=chroma_filter,
            )
        except Exception as e:
            logger.error("ChromaDB 유사도 검색 실패: %s", e, exc_info=True)
            raise VectorStoreError("벡터 저장소 검색 중 오류가 발생했습니다.") from e
        return [
            {
                "content": doc.page_content,
                "metadata": doc.metadata,
                "score": score,
            }
            for doc, score in results
        ]

    def get_all_documents(self) -> list[dict]:
        """BM25 인덱스 구축용으로 전체 문서를 반환한다.

        Returns:
            [{"content": str, "metadata": dict}, ...]
        """
        try:
            collection = self.vectorstore._collection
            result = collection.get(include=["documents", "metadatas"])
        except Exception as e:
            logger.error("ChromaDB 전체 문서 조회 실패: %s", e, exc_info=True)
            raise VectorStoreError("벡터 저장소 문서 조회 중 오류가 발생했습니다.") from e
        documents = result.get("documents") or []
        metadatas = result.get("metadatas") or []
        return [
            {"content": doc, "metadata": meta}
            for doc, meta in zip(documents, metadatas)
        ]

    def get_collection_stats(self) -> dict:
        """컬렉션 통계를 반환한다.

        Returns:
            {"total_documents": int, "collection_name": str}
        """
        collection = self.vectorstore._collection
        return {
            "total_documents": collection.count(),
            "collection_name": self.COLLECTION_NAME,
        }

    def delete_collection(self) -> None:
        """컬렉션을 삭제한다 (재인덱싱 시 사용)."""
        self.vectorstore.delete_collection()
