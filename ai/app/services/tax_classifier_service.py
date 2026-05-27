"""
세목 분류 서비스.

Fine-tuned klue/roberta-base 모델로 거래 내역을 20개 세목 중 하나로 분류한다.
싱글턴 패턴으로 모델 중복 로딩을 방지한다.
"""

import logging
from pathlib import Path

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

logger = logging.getLogger(__name__)

MODEL_DIR = Path(__file__).parent.parent.parent / "models" / "tax_classifier"
MAX_LENGTH = 128
CONFIDENCE_THRESHOLD = 0.7

TAX_CATEGORIES: list[str] = [
    "매출",              # 0
    "상품·원재료 매입",   # 1
    "기초/기말 재고",     # 2
    "급료",              # 3
    "제세공과금",         # 4
    "임차료",            # 5
    "지급이자",           # 6
    "접대비",            # 7
    "기부금",            # 8
    "감가상각비",         # 9
    "차량유지비",         # 10
    "지급수수료",         # 11
    "소모품비",           # 12
    "복리후생비",         # 13
    "운반비",            # 14
    "광고선전비",         # 15
    "여비교통비",         # 16
    "기타경비",           # 17
    "고정자산 매입",      # 18
    "고정자산 매도",      # 19
]


class TaxClassifierService:
    """Fine-tuned 모델 기반 세목 분류 서비스. 싱글턴 패턴."""

    _instance: "TaxClassifierService | None" = None

    def __new__(cls) -> "TaxClassifierService":
        if cls._instance is not None:
            return cls._instance
        instance = super().__new__(cls)
        cls._instance = instance
        return instance

    def __init__(self) -> None:
        if hasattr(self, "_initialized") and self._initialized:
            return

        if not MODEL_DIR.exists():
            raise FileNotFoundError(
                f"세목 분류 모델이 존재하지 않습니다: {MODEL_DIR}. "
                "먼저 python -m scripts.train_tax_classifier 를 실행하세요."
            )

        self._device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        logger.info("세목 분류 모델 로드 시작 (device=%s)", self._device)

        self._tokenizer = AutoTokenizer.from_pretrained(str(MODEL_DIR))
        self._model = AutoModelForSequenceClassification.from_pretrained(str(MODEL_DIR))
        self._model.to(self._device)
        self._model.eval()

        self._initialized = True
        logger.info("세목 분류 모델 로드 완료: %d개 레이블", len(TAX_CATEGORIES))

    def classify(self, user_input: str) -> dict:
        """거래 내역 텍스트를 세목으로 분류한다.

        Args:
            user_input: 거래 내역 텍스트.

        Returns:
            dict with keys:
                - category (str): 분류된 세목명
                - confidence (float): 예측 확률 (0~1)
                - method (str): "local_model" | "fallback_needed"
                - needs_fallback (bool): LLM fallback 필요 여부
        """
        if not user_input or not user_input.strip():
            return {
                "category": "기타경비",
                "confidence": 0.0,
                "method": "fallback_needed",
                "needs_fallback": True,
            }

        encodings = self._tokenizer(
            user_input,
            truncation=True,
            padding="max_length",
            max_length=MAX_LENGTH,
            return_tensors="pt",
        )
        encodings = {k: v.to(self._device) for k, v in encodings.items()}

        with torch.no_grad():
            outputs = self._model(**encodings)
            probs = torch.softmax(outputs.logits, dim=-1)
            confidence, predicted_idx = torch.max(probs, dim=-1)

        confidence_val = confidence.item()
        predicted_label = predicted_idx.item()
        category = TAX_CATEGORIES[predicted_label]

        needs_fallback = confidence_val < CONFIDENCE_THRESHOLD
        method = "fallback_needed" if needs_fallback else "local_model"

        logger.debug(
            "세목 분류 결과: '%s' -> %s (confidence=%.4f, method=%s)",
            user_input[:50],
            category,
            confidence_val,
            method,
        )

        return {
            "category": category,
            "confidence": round(confidence_val, 4),
            "method": method,
            "needs_fallback": needs_fallback,
        }
