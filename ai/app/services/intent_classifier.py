import json
import logging
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np

from app.services.embedding_service import EmbeddingService

logger = logging.getLogger(__name__)


@dataclass
class IntentResult:
    """인텐트 분류 결과.

    아키텍처 설계 문서 4.2절 스펙.
    """

    intent: str            # "EXPENSE_CLASSIFICATION"
    confidence: float      # 0.92 (코사인 유사도)
    search_strategy: str   # "hybrid_with_be_data"
    model_tier: str        # "standard" | "mini"
    rag_required: bool     # True
    metadata_filter: dict  # {"topic": ["경비", "필요경비"]}
    be_data_required: bool = False


class IntentName:
    """인텐트 유형 정의.

    시맨틱 라우터에서 지원하는 모든 인텐트 상수를 정의한다.
    """

    TAX_RATE_LOOKUP = "TAX_RATE_LOOKUP"
    EXPENSE_CLASSIFICATION = "EXPENSE_CLASSIFICATION"
    DEDUCTION_ELIGIBILITY = "DEDUCTION_ELIGIBILITY"
    PROCEDURE_GUIDE = "PROCEDURE_GUIDE"
    CONCEPT_EXPLANATION = "CONCEPT_EXPLANATION"
    CALCULATION = "CALCULATION"
    COMPARISON = "COMPARISON"
    GENERAL = "GENERAL"


class ModelTier:
    """LLM 모델 계층 정의.

    인텐트별 복잡도에 따라 사용할 모델을 선택한다.
    """

    MINI = "mini"         # GPT-4o-mini
    STANDARD = "standard" # GPT-4o


class IntentClassifier:
    """시맨틱 라우터 기반 인텐트 분류기.

    동작 방식 (아키텍처 설계 문서 4.2절):
    1. 각 인텐트의 예시 발화를 임베딩하여 벡터로 저장 (초기화 시 1회)
    2. 사용자 질문을 임베딩
    3. 코사인 유사도로 가장 가까운 인텐트 선택
    4. confidence threshold(0.7) 미만이면 GENERAL로 분류

    구현: numpy 기반 직접 구현 (의존성 최소화)
    """

    CONFIDENCE_THRESHOLD = 0.5

    def __init__(self, intents_path: str, embedding_service: EmbeddingService) -> None:
        self.intents_path = intents_path
        self.embedding_service = embedding_service
        self.intents: list[dict] = []
        self._intent_embeddings: np.ndarray | None = None  # (N, D) 배열
        self._intent_labels: list[str] = []
        self._intent_meta: dict[str, dict] = {}  # name -> intent dict

    async def initialize(self) -> None:
        """비동기 초기화. 예시 발화를 임베딩한다.

        처리:
        1. intents_path에서 JSON 로드
        2. 모든 인텐트의 examples를 수집
        3. 배치로 임베딩 생성 (API 호출 최소화)
        4. 각 임베딩에 인텐트 이름 라벨 부여
        5. numpy 배열로 변환하여 저장

        호출 시점: init_services()에서 앱 기동 시 1회
        """
        path = Path(self.intents_path)
        try:
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
        except FileNotFoundError:
            logger.error("인텐트 파일을 찾을 수 없습니다: %s", path)
            raise
        except json.JSONDecodeError as e:
            logger.error("인텐트 파일 JSON 파싱 실패: %s", e)
            raise

        self.intents = data["intents"]

        # 인텐트 메타정보 저장
        for intent in self.intents:
            self._intent_meta[intent["name"]] = intent

        # 모든 예시 발화 수집
        all_examples: list[str] = []
        all_labels: list[str] = []
        for intent in self.intents:
            for example in intent["examples"]:
                all_examples.append(example)
                all_labels.append(intent["name"])

        # 배치 임베딩 (API 호출 최소화)
        embeddings = await self.embedding_service.embed_texts(all_examples)

        self._intent_labels = all_labels
        self._intent_embeddings = np.array(embeddings, dtype=np.float32)

    async def classify(self, query: str) -> IntentResult:
        """사용자 질문을 인텐트로 분류한다.

        입력: query (str) - 사용자 질문
        출력: IntentResult

        처리:
        1. 질문을 임베딩 (embed_text)
        2. 저장된 모든 예시 임베딩과 코사인 유사도 계산
        3. 가장 높은 유사도의 인텐트 선택
        4. confidence가 CONFIDENCE_THRESHOLD(0.7) 미만이면 GENERAL 반환
        5. IntentResult 생성 (인텐트 메타정보 포함)
        """
        if self._intent_embeddings is None:
            return self._build_intent_result(IntentName.GENERAL, 0.0)

        query_vec = await self.embedding_service.embed_text(query)
        q = np.array(query_vec, dtype=np.float32)

        # 코사인 유사도 배치 계산
        q_norm = np.linalg.norm(q)
        if q_norm == 0:
            return self._build_intent_result(IntentName.GENERAL, 0.0)

        norms = np.linalg.norm(self._intent_embeddings, axis=1)
        # 0 나눔 방지
        norms = np.where(norms == 0, 1e-10, norms)
        scores = (self._intent_embeddings @ q) / (norms * q_norm)

        best_idx = int(np.argmax(scores))
        best_score = float(scores[best_idx])
        best_intent = self._intent_labels[best_idx]

        if best_score < self.CONFIDENCE_THRESHOLD:
            return self._build_intent_result(IntentName.GENERAL, best_score)

        return self._build_intent_result(best_intent, best_score)

    def _build_intent_result(self, intent_name: str, confidence: float) -> IntentResult:
        """인텐트 이름으로 IntentResult를 생성한다."""
        meta = self._intent_meta.get(intent_name)
        if meta is None:
            # GENERAL 폴백
            meta = self._intent_meta.get(IntentName.GENERAL, {})

        return IntentResult(
            intent=intent_name,
            confidence=confidence,
            search_strategy=meta.get("search_strategy", "none"),
            model_tier=meta.get("model_tier", ModelTier.MINI),
            rag_required=meta.get("rag_required", False),
            metadata_filter=meta.get("metadata_filter", {}),
            be_data_required=meta.get("be_data_required", False),
        )
