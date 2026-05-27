"""인텐트 분류 커버리지 테스트.

user-question-examples.md의 질문들이 올바른 인텐트로 분류되는지 검증한다.
"접대비 한도" 같은 엣지 케이스 재발을 방지하는 회귀 테스트.

사용법:
    # 단위 테스트만 (mock, 비용 0)
    pytest tests/test_intent_coverage.py -v -k "not integration"

    # 통합 테스트 포함 (실제 임베딩 API 호출)
    pytest tests/test_intent_coverage.py -v -m integration
"""

from unittest.mock import AsyncMock

import numpy as np
import pytest

from app.services.intent_classifier import IntentClassifier, IntentName

# ---------------------------------------------------------------------------
# Tier 1: RAG 활성화 필수 — GENERAL로 분류되면 안 되는 세금 질문
# 인텐트가 정확하지 않아도 됨, RAG만 작동하면 OK
# ---------------------------------------------------------------------------

MUST_NOT_BE_GENERAL: list[str] = [
    # A. 사업자등록·업종코드
    "프리랜서 개발자인데 사업자등록을 해야 하나요?",
    "1인 개발자 업종코드는 722000으로 해야 하나요?",
    "프리랜서에서 사업자로 전환하면 기존 3.3% 원천징수 소득은 어떻게 처리되나요?",
    "사업자등록은 홈택스에서 바로 할 수 있나요?",
    "자택 주소로 사업자등록이 가능한가요?",
    # B. 종합소득세 기본
    "종합소득세가 뭔가요? 프리랜서도 내야 하나요?",
    "종소세 세율 구간이 어떻게 되나요?",
    "종소세 신고 방식 중 단순경비율과 기준경비율의 차이가 뭔가요?",
    "추계신고와 장부신고 중 어떤 게 유리한가요?",
    "근로소득과 사업소득이 같이 있으면 어떻게 신고하나요?",
    # C. 부가가치세
    "부가가치세가 뭔가요? 개발 용역비에도 부가세가 붙나요?",
    "부가세 매입세액 공제는 어떤 항목이 가능한가요?",
    "부가세 환급은 어떤 경우에 받을 수 있나요?",
    "사업용 신용카드로 결제한 것들은 자동으로 매입세액 공제가 되나요?",
    # D. 경비 처리·분류 (엣지 케이스 집중)
    "맥북 프로 구매비용을 경비 처리할 수 있나요?",
    "AWS, GCP 같은 클라우드 서버 비용을 경비로 잡을 수 있나요?",
    "JetBrains IDE, Figma, GitHub 같은 SaaS 구독료도 경비 인정되나요?",
    "카페에서 작업할 때 쓰는 커피값도 경비 처리가 가능한가요?",
    "1인 사업자인데 접대비 한도가 얼마야?",
    "개인사업자 접대비 인정 범위",
    "100만 원 넘는 장비는 한 번에 경비 처리하나요, 감가상각 해야 하나요?",
    "자택에서 일하는 1인 개발자인데 월세 일부를 경비로 잡을 수 있나요?",
    "ChatGPT Plus, Copilot 같은 AI 도구 구독료는 경비 처리가 되나요?",
    "경비를 장부에 기록할 때 계정과목은 어떻게 분류해야 하나요?",
    # E. 절세·공제·감면
    "1인 개발자가 활용할 수 있는 절세 방법에는 어떤 게 있나요?",
    "청년 창업 세액감면이 뭔가요? IT 업종도 해당되나요?",
    "노란우산공제 가입하면 절세 효과가 얼마나 되나요?",
    "IRP에 가입하면 세금을 얼마나 줄일 수 있나요?",
    "기장세액공제가 뭔가요? 간편장부 작성하면 받을 수 있나요?",
    # F. 장부·기장·회계
    "간편장부와 복식부기의 차이가 뭔가요?",
    "연 매출이 얼마 이상이면 복식부기 의무자가 되나요?",
    "증빙 자료는 몇 년간 보관해야 하나요?",
    "세무사 없이 혼자 장부 기장하고 신고하는 게 가능한가요?",
    # G. 세금계산서·영수증
    "클라이언트가 세금계산서를 요청하는데 어떻게 발급하나요?",
    "전자세금계산서는 홈택스에서 발급할 수 있나요?",
    "적격증빙이 뭔가요? 간이영수증도 적격증빙에 해당되나요?",
    "세금계산서 발급을 잊어버리고 기한이 지났는데 어떻게 해야 하나요?",
    # H. 원천징수·프리랜서 소득
    "3.3% 원천징수가 정확히 뭔가요?",
    "다른 프리랜서한테 외주를 맡기면 3.3% 원천징수를 해야 하나요?",
    "프리랜서 소득이 사업소득인가요, 기타소득인가요?",
    # I. 신고 일정·절차
    "부가세 신고 기한이 언제인가요?",
    "종합소득세 신고 기한은 언제인가요?",
    "수정신고와 경정청구의 차이가 뭔가요?",
    # J. 간이과세·전환
    "간이과세자와 일반과세자의 차이가 뭔가요?",
    "간이과세자 기준 매출이 얼마인가요?",
    "1인 개발자는 간이과세가 유리한가요, 일반과세가 유리한가요?",
    # K. 해외매출·영세율
    "앱스토어에서 해외 매출이 발생하면 세금 처리는 어떻게 하나요?",
    "영세율이 뭔가요? 해외 매출에 적용되나요?",
    "페이팔로 받은 해외 수입도 매출 신고해야 하나요?",
    # L. 가산세·불이익
    "부가세 신고를 깜빡하고 기한이 지났는데 가산세가 얼마나 나오나요?",
    "종소세를 과다 납부했는데 돌려받을 수 있나요?",
    "무신고 가산세와 과소신고 가산세의 차이가 뭔가요?",
]

# ---------------------------------------------------------------------------
# Tier 2: 특정 인텐트 매핑 (높은 확신도)
# ---------------------------------------------------------------------------

EXPECTED_INTENT_MAP: list[tuple[str, str]] = [
    # EXPENSE_CLASSIFICATION
    ("식대 경비 처리 가능한가요?", IntentName.EXPENSE_CLASSIFICATION),
    ("1인 사업자인데 접대비 한도가 얼마야?", IntentName.EXPENSE_CLASSIFICATION),
    ("거래처 선물 접대비 한도가 얼마인가요?", IntentName.EXPENSE_CLASSIFICATION),
    ("맥북 프로 구매비용을 경비 처리할 수 있나요?", IntentName.EXPENSE_CLASSIFICATION),
    ("AWS 클라우드 서버 비용을 경비로 잡을 수 있나요?", IntentName.EXPENSE_CLASSIFICATION),
    ("직원 회식비 필요경비 인정되나요?", IntentName.EXPENSE_CLASSIFICATION),
    # TAX_RATE_LOOKUP
    ("종합소득세 세율이 어떻게 되나요?", IntentName.TAX_RATE_LOOKUP),
    ("부가가치세율은 몇 퍼센트인가요?", IntentName.TAX_RATE_LOOKUP),
    ("과세표준 5000만원일 때 세율은?", IntentName.TAX_RATE_LOOKUP),
    # DEDUCTION_ELIGIBILITY
    ("중소기업 특별세액감면 받을 수 있나요?", IntentName.DEDUCTION_ELIGIBILITY),
    ("청년 창업 세액감면 조건이 뭔가요?", IntentName.DEDUCTION_ELIGIBILITY),
    ("노란우산공제 가입 대상인가요?", IntentName.DEDUCTION_ELIGIBILITY),
    # PROCEDURE_GUIDE
    ("종합소득세 신고 기간이 언제인가요?", IntentName.PROCEDURE_GUIDE),
    ("홈택스에서 소득세 신고하는 방법", IntentName.PROCEDURE_GUIDE),
    ("사업자 등록 어떻게 하나요?", IntentName.PROCEDURE_GUIDE),
    # CONCEPT_EXPLANATION
    ("부가가치세가 뭔가요?", IntentName.CONCEPT_EXPLANATION),
    ("원천징수 뜻이 뭔가요?", IntentName.CONCEPT_EXPLANATION),
    ("필요경비란 무엇인가요?", IntentName.CONCEPT_EXPLANATION),
    # COMPARISON
    ("간이과세자 vs 일반과세자 어떤 게 유리?", IntentName.COMPARISON),
    ("기준경비율 단순경비율 비교", IntentName.COMPARISON),
    ("법인사업자 개인사업자 세금 차이", IntentName.COMPARISON),
    # CALCULATION
    ("매출 1억이면 세금 얼마 내야 하나요?", IntentName.CALCULATION),
    ("부가세 납부 금액 계산해줘", IntentName.CALCULATION),
    # GENERAL (올바르게 GENERAL로 분류되어야 하는 질문)
    ("안녕하세요", IntentName.GENERAL),
    ("감사합니다", IntentName.GENERAL),
    ("도움이 됐어요", IntentName.GENERAL),
]


# ---------------------------------------------------------------------------
# 단위 테스트: 테스트 데이터 무결성
# ---------------------------------------------------------------------------

class TestIntentCoverageData:
    """테스트 데이터 구조 및 커버리지 검증."""

    def test_must_not_be_general_has_sufficient_cases(self):
        """RAG 필수 질문이 충분히 많은지 확인."""
        assert len(MUST_NOT_BE_GENERAL) >= 50, (
            f"RAG 필수 질문이 50개 미만: {len(MUST_NOT_BE_GENERAL)}"
        )

    def test_expected_intent_covers_all_intents(self):
        """모든 인텐트 유형이 테스트 케이스에 포함되는지 확인."""
        tested_intents = {intent for _, intent in EXPECTED_INTENT_MAP}
        all_intents = {
            IntentName.TAX_RATE_LOOKUP,
            IntentName.EXPENSE_CLASSIFICATION,
            IntentName.DEDUCTION_ELIGIBILITY,
            IntentName.PROCEDURE_GUIDE,
            IntentName.CONCEPT_EXPLANATION,
            IntentName.CALCULATION,
            IntentName.COMPARISON,
            IntentName.GENERAL,
        }
        missing = all_intents - tested_intents
        assert not missing, f"테스트에 누락된 인텐트: {missing}"

    def test_no_duplicate_questions_within_tier(self):
        """각 티어 내에서 중복 질문이 없는지 확인."""
        # Tier 1 내 중복
        seen = set()
        dups = [q for q in MUST_NOT_BE_GENERAL if q in seen or seen.add(q)]
        assert not dups, f"Tier 1 중복: {dups}"

        # Tier 2 내 중복
        seen = set()
        dups = [q for q, _ in EXPECTED_INTENT_MAP if q in seen or seen.add(q)]
        assert not dups, f"Tier 2 중복: {dups}"

    def test_edge_case_questions_included(self):
        """과거 실패했던 엣지 케이스가 테스트에 포함되어 있는지 확인."""
        edge_cases = [
            "1인 사업자인데 접대비 한도가 얼마야?",
        ]
        for case in edge_cases:
            assert case in MUST_NOT_BE_GENERAL, (
                f"엣지 케이스 누락: {case}"
            )


# ---------------------------------------------------------------------------
# 통합 테스트: 실제 임베딩 기반 인텐트 분류 검증
# ---------------------------------------------------------------------------

def _make_deterministic_embedding_service(dim: int = 8):
    """결정론적 해시 기반 임베딩 서비스 (단위 테스트용)."""
    async def embed_text(text: str) -> list[float]:
        vec = [hash(text + str(i)) % 1000 / 1000.0 for i in range(dim)]
        norm = sum(x**2 for x in vec) ** 0.5 or 1.0
        return [x / norm for x in vec]

    async def embed_texts(texts: list[str]) -> list[list[float]]:
        return [await embed_text(t) for t in texts]

    svc = AsyncMock()
    svc.embed_text.side_effect = embed_text
    svc.embed_texts.side_effect = embed_texts
    return svc


@pytest.fixture
async def real_intent_classifier():
    """실제 인텐트 분류기 (실제 임베딩 API 호출).

    환경 변수에서 GMS API 키를 로드한다.
    """
    from app.core.config import Settings
    from app.services.embedding_service import EmbeddingService

    settings = Settings()
    embedding_svc = EmbeddingService(settings)
    classifier = IntentClassifier(
        intents_path="app/data/intents/tax_intents.json",
        embedding_service=embedding_svc,
    )
    await classifier.initialize()
    return classifier


class TestIntentClassificationIntegration:
    """실제 임베딩 기반 인텐트 분류 통합 테스트.

    이 테스트는 실제 OpenAI API를 호출하므로 비용이 발생한다.
    수동 실행: pytest tests/test_intent_coverage.py -m integration -v
    """

    @pytest.mark.integration
    @pytest.mark.parametrize("question", MUST_NOT_BE_GENERAL)
    async def test_tax_question_not_classified_as_general(
        self, real_intent_classifier, question,
    ):
        """세금 관련 질문은 GENERAL이 아닌 인텐트로 분류되어야 한다."""
        result = await real_intent_classifier.classify(question)
        assert result.intent != IntentName.GENERAL, (
            f"'{question}' → {result.intent} (confidence={result.confidence:.3f}). "
            f"GENERAL로 분류됨 — RAG가 비활성화되어 답변 품질 저하 위험"
        )

    @pytest.mark.integration
    @pytest.mark.parametrize("question", MUST_NOT_BE_GENERAL)
    async def test_tax_question_requires_rag(
        self, real_intent_classifier, question,
    ):
        """세금 관련 질문은 RAG가 활성화되어야 한다."""
        result = await real_intent_classifier.classify(question)
        assert result.rag_required, (
            f"'{question}' → {result.intent}, rag_required=False. "
            f"RAG 없이 일반 지식으로만 응답할 위험"
        )

    @pytest.mark.integration
    @pytest.mark.parametrize("question,expected_intent", EXPECTED_INTENT_MAP)
    async def test_specific_intent_classification(
        self, real_intent_classifier, question, expected_intent,
    ):
        """특정 질문이 기대한 인텐트로 분류되는지 검증."""
        result = await real_intent_classifier.classify(question)
        assert result.intent == expected_intent, (
            f"'{question}' → {result.intent} (expected: {expected_intent}, "
            f"confidence={result.confidence:.3f})"
        )
