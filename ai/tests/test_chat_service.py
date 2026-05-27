from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from openai import APITimeoutError, AuthenticationError, RateLimitError
from tenacity import RetryError

from app.core.config import Settings
from app.core.exceptions import LLMAuthError, LLMRateLimitError, LLMTimeoutError
from app.services.chat_service import MAX_HISTORY_LENGTH, ChatService
from app.services.intent_classifier import IntentName, IntentResult

from .conftest import make_mock_classifier, make_retrieval_service


@pytest.fixture
def service(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock:
        mock.return_value = "test response"
        svc = ChatService(
            settings=settings,
            retrieval_service=make_retrieval_service(),
            intent_classifier=make_mock_classifier(),
        )
        svc._mock_llm = mock
        yield svc


class TestGetResponse:
    async def test_new_session_creates_uuid(self, service: ChatService):
        _, session_id, _ = await service.get_response("hello")
        assert session_id is not None
        assert len(session_id) == 32  # UUID hex

    async def test_existing_session_reused(self, service: ChatService):
        _, session_id, _ = await service.get_response("first message", session_id="my-session")
        assert session_id == "my-session"

    async def test_history_preserved_across_calls(self, service: ChatService):
        _, sid, _ = await service.get_response("first")
        await service.get_response("second", session_id=sid)

        history = service.get_history(sid)
        assert len(history) == 4  # user+ai * 2

    async def test_history_trimmed_at_max_length(self, service: ChatService):
        sid = "trim-test"
        for i in range(MAX_HISTORY_LENGTH):
            await service.get_response(f"message {i}", session_id=sid)

        history = service.get_history(sid)
        assert len(history) <= MAX_HISTORY_LENGTH


class TestGetHistory:
    async def test_returns_formatted_messages(self, service: ChatService):
        await service.get_response("hello", session_id="hist-test")
        history = service.get_history("hist-test")

        assert history[0] == {"role": "user", "content": "hello"}
        assert history[1]["role"] == "assistant"

    async def test_unknown_session_returns_empty(self, service: ChatService):
        history = service.get_history("nonexistent")
        assert history == []


@pytest.fixture
def service_with_cache(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "pipeline answer"
        cache_svc = AsyncMock()
        cache_svc.get = AsyncMock(return_value=None)
        cache_svc.put = AsyncMock()
        svc = ChatService(
            settings=settings,
            retrieval_service=make_retrieval_service(),
            intent_classifier=make_mock_classifier(),
            cache_service=cache_svc,
        )
        svc._mock_llm = mock_llm
        yield svc


@pytest.fixture
def service_with_backend(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "answer"
        mock_backend = AsyncMock()
        mock_backend.get_transactions = AsyncMock(return_value=[])
        mock_backend.get_business_info = AsyncMock(return_value=None)
        clf = AsyncMock()
        clf.classify.return_value = IntentResult(
            intent=IntentName.EXPENSE_CLASSIFICATION,
            confidence=0.9,
            search_strategy="none",
            model_tier="standard",
            rag_required=False,
            metadata_filter={},
            be_data_required=True,
        )
        svc = ChatService(
            settings=settings,
            retrieval_service=make_retrieval_service(),
            intent_classifier=clf,
            backend_client=mock_backend,
        )
        svc._mock_llm = mock_llm
        yield svc


@pytest.fixture
def service_without_backend(settings: Settings) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "answer"
        clf = AsyncMock()
        clf.classify.return_value = IntentResult(
            intent=IntentName.EXPENSE_CLASSIFICATION,
            confidence=0.9,
            search_strategy="none",
            model_tier="mini",
            rag_required=False,
            metadata_filter={},
            be_data_required=True,
        )
        svc = ChatService(
            settings=settings,
            retrieval_service=make_retrieval_service(),
            intent_classifier=clf,
            backend_client=None,
        )
        svc._mock_llm = mock_llm
        yield svc


# ---------------------------------------------------------------------------
# Cache Integration Tests
# ---------------------------------------------------------------------------

class TestCacheIntegration:
    async def test_cache_hit_returns_early_without_pipeline(
        self, service_with_cache: ChatService
    ):
        service_with_cache.cache_service.get.return_value = "cached answer"

        answer, _, _ = await service_with_cache.get_response(
            "How do I get a tax refund?", session_id="s1"
        )

        assert answer == "cached answer"
        service_with_cache._mock_llm.assert_not_called()

    async def test_cache_miss_proceeds_to_pipeline(
        self, service_with_cache: ChatService
    ):
        service_with_cache.cache_service.get.return_value = None

        answer, _, _ = await service_with_cache.get_response(
            "How to file VAT?", session_id="s2"
        )

        service_with_cache._mock_llm.assert_called_once()
        assert answer == "pipeline answer"

    async def test_response_is_stored_in_cache_after_pipeline(
        self, service_with_cache: ChatService
    ):
        service_with_cache.cache_service.get.return_value = None
        question = "When is the income tax deadline?"

        await service_with_cache.get_response(question, session_id="s3")

        service_with_cache.cache_service.put.assert_called_once()
        call_args = service_with_cache.cache_service.put.call_args
        assert call_args[0][0] == question


# ---------------------------------------------------------------------------
# be_data_required Branch Tests
# ---------------------------------------------------------------------------

class TestBeDataBranch:
    async def test_be_data_required_true_calls_backend_apis(
        self, service_with_backend: ChatService
    ):
        await service_with_backend.get_response(
            "Show my expense deductions", session_id="s1", user_id="user-42"
        )

        service_with_backend.backend_client.get_transactions.assert_called_once_with(
            "user-42"
        )
        service_with_backend.backend_client.get_business_info.assert_called_once_with(
            "user-42"
        )

    async def test_no_user_id_skips_backend_call(
        self, service_with_backend: ChatService
    ):
        await service_with_backend.get_response(
            "Show my expense deductions", session_id="s1"
        )

        service_with_backend.backend_client.get_transactions.assert_not_called()
        service_with_backend.backend_client.get_business_info.assert_not_called()

    async def test_no_backend_client_proceeds_with_empty_data(
        self, service_without_backend: ChatService
    ):
        answer, _, _ = await service_without_backend.get_response(
            "Show expenses", session_id="s1", user_id="user-1"
        )

        assert answer == "answer"
        service_without_backend._mock_llm.assert_called_once()


# ---------------------------------------------------------------------------
# _select_llm Tier Selection Tests
# ---------------------------------------------------------------------------

class TestSelectLlm:
    def test_standard_tier_returns_llm_standard(self, service: ChatService):
        result = service._select_llm("standard")
        assert result is service.llm_standard

    def test_mini_tier_returns_llm_mini(self, service: ChatService):
        result = service._select_llm("mini")
        assert result is service.llm_mini

    def test_undefined_tier_falls_back_to_mini(self, service: ChatService):
        result = service._select_llm("ultra")
        assert result is service.llm_mini

    def test_empty_string_tier_falls_back_to_mini(self, service: ChatService):
        result = service._select_llm("")
        assert result is service.llm_mini


class TestCallLlm:
    async def test_timeout_retries_then_raises(self, settings: Settings):
        service = ChatService(
            settings=settings,
            retrieval_service=make_retrieval_service(),
            intent_classifier=make_mock_classifier(),
        )
        mock_ainvoke = AsyncMock(side_effect=APITimeoutError(request=None))
        with patch("langchain_openai.ChatOpenAI.ainvoke", mock_ainvoke):
            with pytest.raises(RetryError):
                await service._call_llm([])
            assert mock_ainvoke.call_count == 3

    async def test_auth_error_raises_llm_auth_error(self, settings: Settings):
        service = ChatService(
            settings=settings,
            retrieval_service=make_retrieval_service(),
            intent_classifier=make_mock_classifier(),
        )
        mock_response = AsyncMock()
        mock_response.status_code = 401
        mock_response.json.return_value = {"error": {"message": "invalid key"}}
        with patch(
            "langchain_openai.ChatOpenAI.ainvoke",
            new_callable=AsyncMock,
            side_effect=AuthenticationError(
                message="auth failed", response=mock_response, body=None
            ),
        ):
            with pytest.raises(LLMAuthError):
                await service._call_llm([])

    async def test_rate_limit_raises_llm_rate_limit_error(self, settings: Settings):
        service = ChatService(
            settings=settings,
            retrieval_service=make_retrieval_service(),
            intent_classifier=make_mock_classifier(),
        )
        mock_response = AsyncMock()
        mock_response.status_code = 429
        mock_response.json.return_value = {"error": {"message": "rate limit"}}
        with patch(
            "langchain_openai.ChatOpenAI.ainvoke",
            new_callable=AsyncMock,
            side_effect=RateLimitError(
                message="rate limit", response=mock_response, body=None
            ),
        ):
            with pytest.raises(LLMRateLimitError):
                await service._call_llm([])
