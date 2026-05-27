import pytest

from app.core.prompts import CONTEXT_INSTRUCTIONS, INTENT_PROMPTS, build_intent_prompt


# 인텐트별 기대 키워드 매핑
INTENT_KEYWORDS = {
    "TAX_RATE_LOOKUP": ["세율", "법률 조항"],
    "EXPENSE_CLASSIFICATION": ["경비 분류", "필요경비"],
    "DEDUCTION_ELIGIBILITY": ["공제", "감면"],
    "PROCEDURE_GUIDE": ["절차", "단계"],
    "CONCEPT_EXPLANATION": ["개념", "쉽고 명확"],
    "CALCULATION": ["계산", "세액"],
    "COMPARISON": ["비교", "차이점"],
    "GENERAL": ["세금", "친절"],
}


class TestBuildIntentPromptPerIntent:
    """T01: 8개 인텐트별 프롬프트 반환 검증."""

    @pytest.mark.parametrize(
        "intent_name",
        list(INTENT_KEYWORDS.keys()),
        ids=list(INTENT_KEYWORDS.keys()),
    )
    def test_returns_non_empty_prompt(self, intent_name):
        result = build_intent_prompt(intent_name)
        assert isinstance(result, str)
        assert len(result) > 0

    @pytest.mark.parametrize(
        "intent_name,keywords",
        list(INTENT_KEYWORDS.items()),
        ids=list(INTENT_KEYWORDS.keys()),
    )
    def test_contains_intent_specific_keywords(self, intent_name, keywords):
        result = build_intent_prompt(intent_name)
        for keyword in keywords:
            assert keyword in result, f"인텐트 '{intent_name}' 프롬프트에 '{keyword}' 미포함"

    def test_unknown_intent_falls_back_to_general(self):
        # 정의되지 않은 인텐트는 GENERAL 템플릿 + without_context 지시 추가
        result = build_intent_prompt("UNKNOWN")
        assert result.startswith(INTENT_PROMPTS["GENERAL"])
        assert CONTEXT_INSTRUCTIONS["without_context"] in result

    def test_all_defined_intents_have_templates(self):
        # INTENT_PROMPTS 딕셔너리에 8개 인텐트가 모두 정의되어 있는지 확인
        expected_intents = {
            "TAX_RATE_LOOKUP",
            "EXPENSE_CLASSIFICATION",
            "DEDUCTION_ELIGIBILITY",
            "PROCEDURE_GUIDE",
            "CONCEPT_EXPLANATION",
            "CALCULATION",
            "COMPARISON",
            "GENERAL",
        }
        assert set(INTENT_PROMPTS.keys()) == expected_intents


class TestBuildIntentPromptContext:
    """T02: context/user_data 포함 여부 검증."""

    def test_context_included_in_prompt(self):
        context_text = "소득세법 제55조 세율표"
        result = build_intent_prompt("TAX_RATE_LOOKUP", context=context_text)
        assert context_text in result

    def test_user_transactions_included_in_prompt(self):
        transactions = "2025-01-15 카페 업무미팅 35,000원"
        result = build_intent_prompt(
            "EXPENSE_CLASSIFICATION", user_transactions=transactions
        )
        assert transactions in result

    def test_user_data_included_in_prompt(self):
        user_data = "사업자번호: 123-45-67890, 업종: 소매업"
        result = build_intent_prompt("CALCULATION", user_data=user_data)
        assert user_data in result

    def test_empty_context_uses_fallback(self):
        # context=""이면 "(관련 자료 없음)" 으로 대체
        result = build_intent_prompt("TAX_RATE_LOOKUP", context="")
        assert "(관련 자료 없음)" in result

    def test_none_context_uses_fallback(self):
        # context 기본값("")도 falsy이므로 동일하게 폴백 처리
        result = build_intent_prompt("TAX_RATE_LOOKUP")
        assert "(관련 자료 없음)" in result

    def test_empty_context_no_exception(self):
        # 빈 context 전달 시 예외 미발생
        result = build_intent_prompt("COMPARISON", context="")
        assert isinstance(result, str)

    def test_general_intent_ignores_context_param(self):
        # GENERAL 템플릿에는 {context} 플레이스홀더가 없음
        result = build_intent_prompt("GENERAL", context="무시될 내용")
        assert "무시될 내용" not in result

    def test_multiple_params_combined(self):
        # context + user_transactions + user_data 동시 전달
        result = build_intent_prompt(
            "CALCULATION",
            context="종합소득세법",
            user_transactions="거래내역",
            user_data="사업자정보",
        )
        assert "종합소득세법" in result
        assert "사업자정보" in result


class TestContextInstructions:
    """T19: 프롬프트 Fallback 전략 (CONTEXT_INSTRUCTIONS) 검증."""

    def test_with_context_adds_citation_rules(self):
        """context 존재 시 참고 자료 근거 답변 지시 추가."""
        result = build_intent_prompt("TAX_RATE_LOOKUP", context="소득세법 제55조")
        assert CONTEXT_INSTRUCTIONS["with_context"] in result
        assert "반드시 위 참고 자료에 근거하여 답변" in result

    def test_without_context_adds_fallback_rules(self):
        """context 없을 시 일반 지식 답변 지시 추가."""
        result = build_intent_prompt("TAX_RATE_LOOKUP", context="")
        assert CONTEXT_INSTRUCTIONS["without_context"] in result
        assert "세무사 상담을 권고" in result

    def test_no_context_param_adds_fallback_rules(self):
        """context 미전달 시 without_context 지시."""
        result = build_intent_prompt("DEDUCTION_ELIGIBILITY")
        assert CONTEXT_INSTRUCTIONS["without_context"] in result

    def test_context_instructions_appended_after_template(self):
        """CONTEXT_INSTRUCTIONS는 템플릿 뒤에 추가됨."""
        context_text = "테스트 자료"
        result = build_intent_prompt("TAX_RATE_LOOKUP", context=context_text)
        template_end = result.index(context_text) + len(context_text)
        instructions_start = result.index("답변 규칙:")
        assert instructions_start > template_end
