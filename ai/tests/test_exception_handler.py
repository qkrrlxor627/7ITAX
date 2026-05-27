from unittest.mock import AsyncMock, patch

from httpx import ASGITransport, AsyncClient

from app.core.dependencies import get_chat_service
from app.core.exceptions import AIServiceError, LLMTimeoutError
from app.main import app
from app.services.chat_service import ChatService


class TestExceptionHandler:
    async def test_ai_service_error_returns_json(self):
        mock_service = AsyncMock(spec=ChatService)
        mock_service.get_response.side_effect = AIServiceError("테스트 에러", status_code=500)

        app.dependency_overrides[get_chat_service] = lambda: mock_service
        try:
            transport = ASGITransport(app=app)
            async with AsyncClient(transport=transport, base_url="http://test") as ac:
                response = await ac.post(
                    "/api/v1/chat/", json={"message": "안녕"}
                )
            assert response.status_code == 500
            assert response.json() == {"detail": "테스트 에러"}
        finally:
            app.dependency_overrides.clear()

    async def test_llm_timeout_returns_504(self):
        mock_service = AsyncMock(spec=ChatService)
        mock_service.get_response.side_effect = LLMTimeoutError()

        app.dependency_overrides[get_chat_service] = lambda: mock_service
        try:
            transport = ASGITransport(app=app)
            async with AsyncClient(transport=transport, base_url="http://test") as ac:
                response = await ac.post(
                    "/api/v1/chat/", json={"message": "안녕"}
                )
            assert response.status_code == 504
            assert "시간 초과" in response.json()["detail"]
        finally:
            app.dependency_overrides.clear()
