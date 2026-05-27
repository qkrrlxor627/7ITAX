import pytest
from pydantic import ValidationError

from app.models.chat import ChatHistoryResponse, ChatRequest, ChatResponse


class TestChatRequest:
    def test_valid_message(self):
        req = ChatRequest(message="세금 질문입니다")
        assert req.message == "세금 질문입니다"
        assert req.session_id is None

    def test_empty_message_rejected(self):
        with pytest.raises(ValidationError):
            ChatRequest(message="")

    def test_too_long_message_rejected(self):
        with pytest.raises(ValidationError):
            ChatRequest(message="가" * 2001)


class TestChatHistoryResponse:
    def test_valid_response(self):
        resp = ChatHistoryResponse(
            session_id="abc123",
            messages=[{"role": "user", "content": "안녕"}],
            message_count=1,
        )
        assert resp.session_id == "abc123"
        assert resp.message_count == 1
