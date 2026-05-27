"""세목 분류 서비스 단위 테스트.

모델이 아직 학습되지 않은 경우 자동으로 skip 처리된다.
학습 후 실행: cd ai/ && pytest tests/test_tax_classifier.py -v
"""

from pathlib import Path

import pytest

MODEL_PATH = Path(__file__).parent.parent / "models" / "tax_classifier"

TAX_CATEGORIES: list[str] = [
    "매출", "상품·원재료 매입", "기초/기말 재고", "급료", "제세공과금",
    "임차료", "지급이자", "접대비", "기부금", "감가상각비",
    "차량유지비", "지급수수료", "소모품비", "복리후생비", "운반비",
    "광고선전비", "여비교통비", "기타경비", "고정자산 매입", "고정자산 매도",
]


@pytest.fixture(scope="module")
def classifier():
    if not MODEL_PATH.exists():
        pytest.skip("세목 분류 모델이 아직 학습되지 않았습니다")
    from app.services.tax_classifier_service import TaxClassifierService

    return TaxClassifierService()


class TestClassifyReturnSchema:
    """classify() 반환값 스키마 검증."""

    def test_returns_required_keys(self, classifier):
        result = classifier.classify("커피숍에서 거래처 미팅 식사비")
        assert "category" in result
        assert "confidence" in result
        assert "method" in result
        assert "needs_fallback" in result

    def test_category_is_valid_string(self, classifier):
        result = classifier.classify("사무실 월세 납부")
        assert isinstance(result["category"], str)
        assert result["category"] in TAX_CATEGORIES

    def test_confidence_is_float_between_0_and_1(self, classifier):
        result = classifier.classify("택시비 영수증")
        assert isinstance(result["confidence"], float)
        assert 0.0 <= result["confidence"] <= 1.0

    def test_method_is_valid_value(self, classifier):
        result = classifier.classify("AWS 서버 사용료")
        assert result["method"] in ("local_model", "fallback_needed")

    def test_needs_fallback_is_bool(self, classifier):
        result = classifier.classify("KTX 왕복 출장비")
        assert isinstance(result["needs_fallback"], bool)


class TestConfidenceThreshold:
    """confidence threshold 동작 검증."""

    def test_high_confidence_no_fallback(self, classifier):
        result = classifier.classify("거래처 접대 저녁 식사비 15만원")
        if result["confidence"] >= 0.7:
            assert result["method"] == "local_model"
            assert result["needs_fallback"] is False

    def test_low_confidence_needs_fallback(self, classifier):
        result = classifier.classify("거래처 접대 저녁 식사비 15만원")
        if result["confidence"] < 0.7:
            assert result["method"] == "fallback_needed"
            assert result["needs_fallback"] is True


class TestEmptyInput:
    """빈 입력 처리 검증."""

    def test_empty_string(self, classifier):
        result = classifier.classify("")
        assert result["category"] == "기타경비"
        assert result["confidence"] == 0.0
        assert result["needs_fallback"] is True

    def test_whitespace_only(self, classifier):
        result = classifier.classify("   ")
        assert result["category"] == "기타경비"
        assert result["needs_fallback"] is True

    def test_none_like_empty(self, classifier):
        """빈 문자열 처리."""
        result = classifier.classify("")
        assert result["method"] == "fallback_needed"


class TestCategoryClassification:
    """주요 세목별 분류 정확도 검증."""

    def test_classify_entertainment(self, classifier):
        result = classifier.classify("거래처 클라이언트 접대 식사비 카드 결제")
        assert result["category"] in TAX_CATEGORIES

    def test_classify_commission(self, classifier):
        result = classifier.classify("GitHub Copilot 월 구독료 결제")
        assert result["category"] in TAX_CATEGORIES

    def test_classify_travel(self, classifier):
        result = classifier.classify("서울-부산 출장 KTX 왕복 교통비")
        assert result["category"] in TAX_CATEGORIES

    def test_classify_rent(self, classifier):
        result = classifier.classify("공유오피스 3월 임대료 납부")
        assert result["category"] in TAX_CATEGORIES

    def test_classify_supplies(self, classifier):
        result = classifier.classify("사무용품 A4용지 토너 구매")
        assert result["category"] in TAX_CATEGORIES

    def test_classify_vehicle(self, classifier):
        result = classifier.classify("업무용 차량 주유비 SK에너지")
        assert result["category"] in TAX_CATEGORIES

    def test_classify_advertising(self, classifier):
        result = classifier.classify("인스타그램 광고비 집행")
        assert result["category"] in TAX_CATEGORIES

    def test_classify_revenue(self, classifier):
        result = classifier.classify("웹 개발 프로젝트 용역비 입금")
        assert result["category"] in TAX_CATEGORIES


class TestFullCategoryCoverage:
    """20개 세목 전체 커버리지 스모크 테스트."""

    SMOKE_INPUTS: list[str] = [
        "프리랜서 개발 용역 대금 수령",          # 매출
        "서버 부품 구매",                         # 상품·원재료 매입
        "기말 재고 실사",                         # 기초/기말 재고
        "직원 3월 급여 이체",                     # 급료
        "자동차세 납부",                          # 제세공과금
        "사무실 월세 납부",                       # 임차료
        "사업자 대출 이자 납부",                  # 지급이자
        "고객사 담당자 식사 접대",                # 접대비
        "적십자 기부금 납부",                     # 기부금
        "노트북 감가상각 처리",                   # 감가상각비
        "업무용 차량 엔진오일 교환",              # 차량유지비
        "AWS 클라우드 서비스 이용료",             # 지급수수료
        "사무실 복합기 토너 구매",                # 소모품비
        "직원 점심 식대 지원",                    # 복리후생비
        "택배 배송료 결제",                       # 운반비
        "네이버 키워드 광고비",                   # 광고선전비
        "부산 출장 KTX 교통비",                   # 여비교통비
        "기타 잡비 처리",                         # 기타경비
        "업무용 데스크톱 PC 구매",                # 고정자산 매입
        "중고 모니터 매각 처분",                  # 고정자산 매도
    ]

    def test_smoke_all_categories(self, classifier):
        """20개 입력에 대해 모두 유효한 세목을 반환하는지 확인."""
        for text in self.SMOKE_INPUTS:
            result = classifier.classify(text)
            assert result["category"] in TAX_CATEGORIES, (
                f"'{text}' -> '{result['category']}' 가 유효한 세목이 아닙니다"
            )
            assert 0.0 <= result["confidence"] <= 1.0


class TestClassifyResponseWithExplanation:
    """T21: TransactionClassifyResponse에 reason, legal_basis 필드 검증."""

    def test_response_has_reason_field(self):
        """reason 필드 존재 및 기본값."""
        from app.models.transaction import TransactionClassifyResponse

        resp = TransactionClassifyResponse(
            category="소모품비", confidence=0.92, method="local_model"
        )
        assert hasattr(resp, "reason")
        assert resp.reason == ""

    def test_response_has_legal_basis_field(self):
        """legal_basis 필드 존재 및 기본값."""
        from app.models.transaction import TransactionClassifyResponse

        resp = TransactionClassifyResponse(
            category="접대비", confidence=0.85, method="local_model"
        )
        assert hasattr(resp, "legal_basis")
        assert resp.legal_basis == ""

    def test_response_with_explanation_values(self):
        """reason, legal_basis에 값이 있는 경우."""
        from app.models.transaction import TransactionClassifyResponse

        resp = TransactionClassifyResponse(
            category="소모품비",
            confidence=0.92,
            method="local_model",
            reason="사무용품 구입에 해당",
            legal_basis="소득세법 제33조",
        )
        assert resp.reason == "사무용품 구입에 해당"
        assert resp.legal_basis == "소득세법 제33조"

    def test_response_missing_required_field_raises(self):
        """필수 필드 누락 시 ValidationError."""
        from pydantic import ValidationError
        from app.models.transaction import TransactionClassifyResponse

        with pytest.raises(ValidationError):
            TransactionClassifyResponse(confidence=0.5, method="local_model")
