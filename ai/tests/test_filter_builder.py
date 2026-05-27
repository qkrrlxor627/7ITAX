import pytest
from app.utils.filter_builder import INTENT_FILTER_MAP, build_filter_for_intent


class TestIntentFilterMap:
    def test_has_8_intents(self):
        """INTENT_FILTER_MAP에 8개 인텐트가 정의된다."""
        assert len(INTENT_FILTER_MAP) == 8

    def test_required_intents_present(self):
        """8개 인텐트가 모두 포함된다."""
        required = {
            "TAX_RATE_LOOKUP", "EXPENSE_CLASSIFICATION", "DEDUCTION_ELIGIBILITY",
            "PROCEDURE_GUIDE", "CONCEPT_EXPLANATION", "CALCULATION",
            "COMPARISON", "GENERAL",
        }
        assert set(INTENT_FILTER_MAP.keys()) == required

    def test_empty_filter_intents(self):
        """CONCEPT_EXPLANATION, COMPARISON, GENERAL은 빈 필터({})를 가진다."""
        assert INTENT_FILTER_MAP["CONCEPT_EXPLANATION"] == {}
        assert INTENT_FILTER_MAP["COMPARISON"] == {}
        assert INTENT_FILTER_MAP["GENERAL"] == {}


class TestBuildFilterForIntent:
    def test_tax_rate_lookup_returns_filter(self):
        """TAX_RATE_LOOKUP은 세율 관련 필터를 반환한다."""
        filt = build_filter_for_intent("TAX_RATE_LOOKUP")
        assert filt is not None
        assert "topics" in filt
        assert "세율" in filt["topics"]["$in"]

    def test_expense_classification_returns_filter(self):
        """EXPENSE_CLASSIFICATION은 경비 관련 필터를 반환한다."""
        filt = build_filter_for_intent("EXPENSE_CLASSIFICATION")
        assert filt is not None
        assert "경비" in filt["topics"]["$in"]

    def test_concept_explanation_returns_none(self):
        """빈 필터 인텐트(CONCEPT_EXPLANATION)는 None을 반환한다."""
        filt = build_filter_for_intent("CONCEPT_EXPLANATION")
        assert filt is None

    def test_comparison_returns_none(self):
        """COMPARISON은 None을 반환한다."""
        filt = build_filter_for_intent("COMPARISON")
        assert filt is None

    def test_general_returns_none(self):
        """GENERAL은 None을 반환한다."""
        filt = build_filter_for_intent("GENERAL")
        assert filt is None

    def test_unknown_intent_returns_none(self):
        """알 수 없는 인텐트는 None을 반환한다."""
        filt = build_filter_for_intent("UNKNOWN_INTENT")
        assert filt is None

    def test_additional_filter_merged(self):
        """additional_filter가 기본 필터에 병합된다."""
        additional = {"law_name": {"$in": ["소득세법"]}}
        filt = build_filter_for_intent("TAX_RATE_LOOKUP", additional_filter=additional)
        assert "topics" in filt
        assert "law_name" in filt
        assert filt["law_name"] == {"$in": ["소득세법"]}

    def test_additional_filter_on_empty_intent(self):
        """빈 필터 인텐트에 additional_filter를 추가하면 해당 필터가 반환된다."""
        additional = {"law_name": {"$in": ["소득세법"]}}
        filt = build_filter_for_intent("GENERAL", additional_filter=additional)
        assert filt is not None
        assert "law_name" in filt

    def test_all_non_empty_intents_return_filter(self):
        """빈 필터가 아닌 모든 인텐트는 None이 아닌 필터를 반환한다."""
        non_empty_intents = [
            "TAX_RATE_LOOKUP", "EXPENSE_CLASSIFICATION", "DEDUCTION_ELIGIBILITY",
            "PROCEDURE_GUIDE", "CALCULATION",
        ]
        for intent in non_empty_intents:
            filt = build_filter_for_intent(intent)
            assert filt is not None, f"{intent}는 None이 아닌 필터를 반환해야 한다"
