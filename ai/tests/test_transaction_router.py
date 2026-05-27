"""세목 분류 API 통합 테스트.

POST /api/v1/transaction/classify 엔드포인트에 대해
정상 분류(confidence >= 0.7), fallback 동작(< 0.7), 빈 입력 에러 응답을 검증한다.
"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.core.dependencies import get_tax_classifier_service
from app.main import app
from app.services.tax_classifier_service import TaxClassifierService


@pytest.fixture
def mock_classifier_high_confidence():
    """confidence >= 0.7 인 로컬 모델 분류 결과를 반환하는 Mock."""
    mock = MagicMock(spec=TaxClassifierService)
    mock.classify.return_value = {
        "category": "접대비",
        "confidence": 0.92,
        "method": "local_model",
        "needs_fallback": False,
    }
    return mock


@pytest.fixture
def mock_classifier_low_confidence():
    """confidence < 0.7 인 fallback 필요 결과를 반환하는 Mock."""
    mock = MagicMock(spec=TaxClassifierService)
    mock.classify.return_value = {
        "category": "기타경비",
        "confidence": 0.45,
        "method": "fallback_needed",
        "needs_fallback": True,
    }
    return mock


@pytest.fixture
async def client_high(mock_classifier_high_confidence):
    app.dependency_overrides[get_tax_classifier_service] = (
        lambda: mock_classifier_high_confidence
    )
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()


@pytest.fixture
async def client_low(mock_classifier_low_confidence):
    app.dependency_overrides[get_tax_classifier_service] = (
        lambda: mock_classifier_low_confidence
    )
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()


class TestClassifyHighConfidence:
    """confidence >= 0.7: 로컬 모델 결과를 즉시 반환."""

    async def test_returns_200(self, client_high: AsyncClient):
        resp = await client_high.post(
            "/api/v1/transaction/classify",
            json={"description": "거래처 접대 저녁 식사비"},
        )
        assert resp.status_code == 200

    async def test_response_schema(self, client_high: AsyncClient):
        resp = await client_high.post(
            "/api/v1/transaction/classify",
            json={"description": "거래처 접대 저녁 식사비"},
        )
        data = resp.json()
        assert "category" in data
        assert "confidence" in data
        assert "method" in data

    async def test_method_is_local_model(self, client_high: AsyncClient):
        resp = await client_high.post(
            "/api/v1/transaction/classify",
            json={"description": "거래처 접대 저녁 식사비"},
        )
        data = resp.json()
        assert data["method"] == "local_model"
        assert data["category"] == "접대비"
        assert data["confidence"] >= 0.7


class TestClassifyLowConfidence:
    """confidence < 0.7: LLM fallback 경로 검증."""

    async def test_fallback_success(self, client_low: AsyncClient):
        with patch(
            "app.routers.transaction._llm_fallback",
            new_callable=AsyncMock,
            return_value="소모품비",
        ):
            resp = await client_low.post(
                "/api/v1/transaction/classify",
                json={"description": "애매한 거래 내역"},
            )
            data = resp.json()
            assert resp.status_code == 200
            assert data["method"] == "llm_fallback"
            assert data["category"] == "소모품비"

    async def test_fallback_failure_uses_local(self, client_low: AsyncClient):
        with patch(
            "app.routers.transaction._llm_fallback",
            new_callable=AsyncMock,
            side_effect=Exception("LLM 호출 실패"),
        ):
            resp = await client_low.post(
                "/api/v1/transaction/classify",
                json={"description": "애매한 거래 내역"},
            )
            data = resp.json()
            assert resp.status_code == 200
            assert data["method"] == "local_model"
            assert data["category"] == "기타경비"


class TestClassifyValidation:
    """입력 유효성 검증."""

    async def test_empty_description_returns_422(self, client_high: AsyncClient):
        resp = await client_high.post(
            "/api/v1/transaction/classify",
            json={"description": ""},
        )
        assert resp.status_code == 422

    async def test_missing_description_returns_422(self, client_high: AsyncClient):
        resp = await client_high.post(
            "/api/v1/transaction/classify",
            json={},
        )
        assert resp.status_code == 422

    async def test_no_body_returns_422(self, client_high: AsyncClient):
        resp = await client_high.post("/api/v1/transaction/classify")
        assert resp.status_code == 422

    async def test_description_too_long_returns_422(self, client_high: AsyncClient):
        resp = await client_high.post(
            "/api/v1/transaction/classify",
            json={"description": "a" * 501},
        )
        assert resp.status_code == 422
