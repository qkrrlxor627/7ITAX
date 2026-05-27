from app.core.exceptions import (
    AIServiceError,
    LLMAuthError,
    LLMRateLimitError,
    LLMTimeoutError,
)


class TestExceptionAttributes:
    def test_ai_service_error_defaults(self):
        exc = AIServiceError("서버 오류")
        assert exc.message == "서버 오류"
        assert exc.status_code == 500

    def test_llm_timeout_error(self):
        exc = LLMTimeoutError()
        assert exc.status_code == 504
        assert "시간 초과" in exc.message

    def test_llm_rate_limit_error(self):
        exc = LLMRateLimitError()
        assert exc.status_code == 429

    def test_llm_auth_error(self):
        exc = LLMAuthError()
        assert exc.status_code == 401


class TestExceptionHierarchy:
    def test_all_inherit_from_ai_service_error(self):
        assert issubclass(LLMTimeoutError, AIServiceError)
        assert issubclass(LLMRateLimitError, AIServiceError)
        assert issubclass(LLMAuthError, AIServiceError)
