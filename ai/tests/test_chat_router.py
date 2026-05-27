from httpx import AsyncClient


class TestChatEndpoint:
    async def test_chat_success(self, client: AsyncClient):
        response = await client.post(
            "/api/v1/chat/", json={"message": "세금 질문입니다"}
        )
        assert response.status_code == 200
        data = response.json()
        assert "answer" in data
        assert "session_id" in data
        assert isinstance(data["answer"], str)

    async def test_chat_with_session_id(self, client: AsyncClient):
        response = await client.post(
            "/api/v1/chat/",
            json={"message": "안녕", "session_id": "test-session"},
        )
        assert response.status_code == 200
        assert response.json()["session_id"] == "test-session"

    async def test_chat_empty_message_returns_422(self, client: AsyncClient):
        response = await client.post("/api/v1/chat/", json={"message": ""})
        assert response.status_code == 422

    async def test_chat_missing_message_returns_422(self, client: AsyncClient):
        response = await client.post("/api/v1/chat/", json={})
        assert response.status_code == 422


class TestHistoryEndpoint:
    async def test_get_history_success(self, client: AsyncClient):
        # 먼저 대화 생성
        chat_resp = await client.post(
            "/api/v1/chat/", json={"message": "안녕"}
        )
        session_id = chat_resp.json()["session_id"]

        # 히스토리 조회
        response = await client.get(f"/api/v1/chat/history/{session_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["session_id"] == session_id
        assert data["message_count"] >= 1

    async def test_get_history_unknown_session(self, client: AsyncClient):
        response = await client.get("/api/v1/chat/history/nonexistent")
        assert response.status_code == 200
        assert response.json()["message_count"] == 0
