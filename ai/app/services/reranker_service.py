"""Cross-Encoder Reranker 서비스.

BAAI/bge-reranker-v2-m3 모델을 사용하여 검색 결과를 재정렬한다.
query-document 쌍의 관련성을 Cross-Encoder로 정밀 판단한다.
"""

import logging

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

logger = logging.getLogger(__name__)


class RerankerService:
    """Cross-Encoder 기반 Reranker.

    모델: BAAI/bge-reranker-v2-m3
    - 다국어 지원 (한국어 포함)
    - 크기: ~560MB
    - CPU 추론 시 ~130ms (7개 문서 기준)
    """

    MODEL_NAME = "BAAI/bge-reranker-v2-m3"

    def __init__(self, device: str | None = None) -> None:
        self.device = device or ("cuda" if torch.cuda.is_available() else "cpu")
        logger.info("Reranker 모델 로딩: %s (device=%s)", self.MODEL_NAME, self.device)

        self.tokenizer = AutoTokenizer.from_pretrained(self.MODEL_NAME)
        self.model = AutoModelForSequenceClassification.from_pretrained(self.MODEL_NAME)
        self.model.to(self.device)
        self.model.eval()

        logger.info("Reranker 모델 로딩 완료")

    def rerank(
        self,
        query: str,
        documents: list[dict],
        top_k: int = 5,
    ) -> list[dict]:
        """검색 결과를 Cross-Encoder 점수로 재정렬한다.

        Args:
            query: 사용자 쿼리 문자열.
            documents: 검색 결과 리스트.
                       각 항목은 {"content": str, "metadata": dict, "score": float}.
            top_k: 반환할 상위 문서 수.

        Returns:
            Cross-Encoder 점수 기준 상위 top_k개 문서.
            각 문서에 "rerank_score" 필드가 추가됨.
        """
        if not documents:
            return []

        pairs = [[query, doc["content"]] for doc in documents]

        inputs = self.tokenizer(
            pairs,
            padding=True,
            truncation=True,
            max_length=512,
            return_tensors="pt",
        ).to(self.device)

        with torch.no_grad():
            scores = self.model(**inputs).logits.squeeze(-1).cpu().tolist()

        if isinstance(scores, float):
            scores = [scores]

        scored_docs = list(zip(documents, scores))
        scored_docs.sort(key=lambda x: x[1], reverse=True)

        results = []
        for doc, score in scored_docs[:top_k]:
            result = dict(doc)
            result["rerank_score"] = score
            results.append(result)

        return results
