import pytest
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient

from app.core.config import settings
from app.routers import health


@pytest.fixture
def client():
    app = FastAPI()
    app.include_router(health.router)
    return AsyncClient(transport=ASGITransport(app=app), base_url="http://test")


async def test_키가_없으면_ready는_503(client, monkeypatch):
    monkeypatch.setattr(settings, "anthropic_api_key", "")
    res = await client.get("/api/v1/health/ready")
    assert res.status_code == 503


async def test_키가_있으면_ready는_200(client, monkeypatch):
    monkeypatch.setattr(settings, "anthropic_api_key", "sk-test")
    res = await client.get("/api/v1/health/ready")
    assert res.status_code == 200


async def test_liveness는_키와_무관하게_200(client, monkeypatch):
    monkeypatch.setattr(settings, "anthropic_api_key", "")
    res = await client.get("/api/v1/health/")
    assert res.status_code == 200
