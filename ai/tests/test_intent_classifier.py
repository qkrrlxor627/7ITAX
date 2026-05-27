from unittest.mock import AsyncMock, patch

import numpy as np
import pytest

from app.services.intent_classifier import IntentClassifier, IntentName, IntentResult, ModelTier


# 테스트용 질문-인텐트 쌍 (예시 발화에 없는 새로운 질문)
TEST_QUERIES = [
    # TAX_RATE_LOOKUP
    ("소득세 세율표 좀 보여주세요", IntentName.TAX_RATE_LOOKUP),
    ("부가세 10%가 맞나요?", IntentName.TAX_RATE_LOOKUP),
    # EXPENSE_CLASSIFICATION
    ("회사 회식비 경비 처리 가능한가요?", IntentName.EXPENSE_CLASSIFICATION),
    ("카페에서 업무 미팅 비용 처리", IntentName.EXPENSE_CLASSIFICATION),
    # DEDUCTION_ELIGIBILITY
    ("교육비 세액공제 받을 수 있나요?", IntentName.DEDUCTION_ELIGIBILITY),
    ("소기업 소상공인 공제 대상인가요?", IntentName.DEDUCTION_ELIGIBILITY),
    # PROCEDURE_GUIDE
    ("종소세 신고 방법이 궁금해요", IntentName.PROCEDURE_GUIDE),
    ("사업자 등록은 어디서 하나요?", IntentName.PROCEDURE_GUIDE),
    # CONCEPT_EXPLANATION
    ("원천징수가 무슨 뜻이에요?", IntentName.CONCEPT_EXPLANATION),
    ("간이과세자란 뭔가요?", IntentName.CONCEPT_EXPLANATION),
    # CALCULATION
    ("매출 5000만원이면 세금 얼마인가요?", IntentName.CALCULATION),
    ("부가세 신고할 금액 계산해줘", IntentName.CALCULATION),
    # COMPARISON
    ("간이과세 vs 일반과세 뭐가 유리?", IntentName.COMPARISON),
    ("기준경비율 단순경비율 차이", IntentName.COMPARISON),
    # GENERAL
    ("안녕", IntentName.GENERAL),
    ("감사합니다", IntentName.GENERAL),
]

VALID_SEARCH_STRATEGIES = {
    "hybrid", "vector", "metadata_filter", "multi_query", "none", "hybrid_with_be_data"
}


def _make_mock_embedding_service(dim: int = 4):
    """결정론적 임베딩을 반환하는 EmbeddingService mock."""
    call_count = {"n": 0}

    async def embed_text(text: str) -> list[float]:
        vec = [hash(text + str(i)) % 100 / 100.0 for i in range(dim)]
        norm = sum(x**2 for x in vec) ** 0.5 or 1.0
        return [x / norm for x in vec]

    async def embed_texts(texts: list[str]) -> list[list[float]]:
        return [await embed_text(t) for t in texts]

    svc = AsyncMock()
    svc.embed_text.side_effect = embed_text
    svc.embed_texts.side_effect = embed_texts
    return svc


class TestIntentResult:
    def test_instantiation(self):
        result = IntentResult(
            intent=IntentName.TAX_RATE_LOOKUP,
            confidence=0.92,
            search_strategy="metadata_filter",
            model_tier=ModelTier.MINI,
            rag_required=True,
            metadata_filter={"topic": ["세율"]},
        )
        assert result.intent == IntentName.TAX_RATE_LOOKUP
        assert result.confidence == 0.92
        assert result.search_strategy == "metadata_filter"
        assert result.model_tier == ModelTier.MINI
        assert result.rag_required is True
        assert result.be_data_required is False

    def test_be_data_required_default(self):
        result = IntentResult(
            intent=IntentName.GENERAL,
            confidence=0.5,
            search_strategy="none",
            model_tier=ModelTier.MINI,
            rag_required=False,
            metadata_filter={},
        )
        assert result.be_data_required is False


class TestIntentName:
    def test_all_intents_defined(self):
        assert IntentName.TAX_RATE_LOOKUP == "TAX_RATE_LOOKUP"
        assert IntentName.EXPENSE_CLASSIFICATION == "EXPENSE_CLASSIFICATION"
        assert IntentName.DEDUCTION_ELIGIBILITY == "DEDUCTION_ELIGIBILITY"
        assert IntentName.PROCEDURE_GUIDE == "PROCEDURE_GUIDE"
        assert IntentName.CONCEPT_EXPLANATION == "CONCEPT_EXPLANATION"
        assert IntentName.CALCULATION == "CALCULATION"
        assert IntentName.COMPARISON == "COMPARISON"
        assert IntentName.GENERAL == "GENERAL"


class TestModelTier:
    def test_values(self):
        assert ModelTier.MINI == "mini"
        assert ModelTier.STANDARD == "standard"


class TestIntentClassifierInit:
    async def test_initialize_loads_intents(self, tmp_path):
        import json

        intent_data = {
            "intents": [
                {
                    "name": "GENERAL",
                    "description": "일반",
                    "examples": ["안녕", "감사"],
                    "search_strategy": "none",
                    "metadata_filter": {},
                    "model_tier": "mini",
                    "rag_required": False,
                    "be_data_required": False,
                }
            ]
        }
        json_file = tmp_path / "test_intents.json"
        json_file.write_text(json.dumps(intent_data), encoding="utf-8")

        mock_svc = _make_mock_embedding_service()
        classifier = IntentClassifier(str(json_file), mock_svc)
        await classifier.initialize()

        assert len(classifier.intents) == 1
        assert classifier._intent_embeddings is not None
        assert classifier._intent_embeddings.shape[0] == 2  # 2개 예시

    async def test_classify_before_init_returns_general(self, tmp_path):
        import json

        json_file = tmp_path / "empty_intents.json"
        json_file.write_text(json.dumps({"intents": []}), encoding="utf-8")

        mock_svc = _make_mock_embedding_service()
        classifier = IntentClassifier(str(json_file), mock_svc)
        # initialize() 없이 classify 호출
        result = await classifier.classify("세율이 얼마인가요?")
        assert result.intent == IntentName.GENERAL


class TestIntentClassifierClassify:
    @pytest.fixture
    async def classifier(self):
        """실제 tax_intents.json을 사용하는 IntentClassifier (임베딩만 mock)."""
        mock_svc = _make_mock_embedding_service(dim=8)
        c = IntentClassifier("app/data/intents/tax_intents.json", mock_svc)
        await c.initialize()
        return c

    async def test_classify_returns_intent_result(self, classifier):
        result = await classifier.classify("세율이 어떻게 되나요?")
        assert isinstance(result, IntentResult)
        assert result.intent is not None
        assert 0.0 <= result.confidence <= 1.0

    async def test_classify_result_has_valid_strategy(self, classifier):
        result = await classifier.classify("소득세 세율")
        assert result.search_strategy in VALID_SEARCH_STRATEGIES

    async def test_classify_result_has_valid_model_tier(self, classifier):
        result = await classifier.classify("부가세 신고")
        assert result.model_tier in (ModelTier.MINI, ModelTier.STANDARD)

    async def test_classify_result_has_bool_fields(self, classifier):
        result = await classifier.classify("경비 처리")
        assert isinstance(result.rag_required, bool)
        assert isinstance(result.be_data_required, bool)
        assert isinstance(result.metadata_filter, dict)

    async def test_low_confidence_returns_general(self, tmp_path):
        """모든 예시와 유사도가 낮으면 GENERAL 반환."""
        import json

        import numpy as np

        intent_data = {
            "intents": [
                {
                    "name": "TAX_RATE_LOOKUP",
                    "description": "세율 조회",
                    "examples": ["세율 알려줘"],
                    "search_strategy": "metadata_filter",
                    "metadata_filter": {},
                    "model_tier": "mini",
                    "rag_required": True,
                    "be_data_required": False,
                },
                {
                    "name": "GENERAL",
                    "description": "일반",
                    "examples": ["안녕"],
                    "search_strategy": "none",
                    "metadata_filter": {},
                    "model_tier": "mini",
                    "rag_required": False,
                    "be_data_required": False,
                },
            ]
        }
        json_file = tmp_path / "intents.json"
        json_file.write_text(json.dumps(intent_data), encoding="utf-8")

        # 항상 낮은 유사도를 반환하도록 설정
        mock_svc = AsyncMock()
        mock_svc.embed_texts.return_value = [
            [1.0, 0.0],  # TAX_RATE_LOOKUP 예시
            [0.0, 1.0],  # GENERAL 예시
        ]
        # 쿼리 임베딩을 두 벡터 모두와 유사도 낮게 (직교)
        mock_svc.embed_text.return_value = [0.0, 0.0]  # 제로 벡터

        c = IntentClassifier(str(json_file), mock_svc)
        await c.initialize()

        result = await c.classify("전혀 다른 질문")
        assert result.intent == IntentName.GENERAL


class TestIntentClassifierWithRealJson:
    """실제 tax_intents.json 파일 구조 검증."""

    async def test_all_intents_loaded(self):
        import json

        with open("app/data/intents/tax_intents.json", encoding="utf-8") as f:
            data = json.load(f)

        intent_names = {i["name"] for i in data["intents"]}
        expected = {
            IntentName.TAX_RATE_LOOKUP,
            IntentName.EXPENSE_CLASSIFICATION,
            IntentName.DEDUCTION_ELIGIBILITY,
            IntentName.PROCEDURE_GUIDE,
            IntentName.CONCEPT_EXPLANATION,
            IntentName.CALCULATION,
            IntentName.COMPARISON,
            IntentName.GENERAL,
        }
        assert intent_names == expected

    async def test_all_intents_have_required_fields(self):
        import json

        with open("app/data/intents/tax_intents.json", encoding="utf-8") as f:
            data = json.load(f)

        for intent in data["intents"]:
            name = intent["name"]
            assert "name" in intent, f"{name}: name 필드 없음"
            assert "description" in intent, f"{name}: description 필드 없음"
            assert "examples" in intent, f"{name}: examples 필드 없음"
            assert "search_strategy" in intent, f"{name}: search_strategy 필드 없음"
            assert "metadata_filter" in intent, f"{name}: metadata_filter 필드 없음"
            assert "model_tier" in intent, f"{name}: model_tier 필드 없음"
            assert "rag_required" in intent, f"{name}: rag_required 필드 없음"
            assert "be_data_required" in intent, f"{name}: be_data_required 필드 없음"
            assert len(intent["examples"]) >= 1, f"{name}: examples가 비어있음"
