from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.core.config import Settings
from app.core.dependencies import get_chat_service
from app.main import app
from app.services.chat_service import ChatService
from app.services.intent_classifier import IntentName, IntentResult
from app.services.retrieval_service import BM25Index, RetrievalService
from app.services.vectorstore import VectorStoreService


# ---------------------------------------------------------------------------
# 공통 헬퍼 함수
# ---------------------------------------------------------------------------

def make_settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


def make_retrieval_service() -> RetrievalService:
    mock_vs = MagicMock(spec=VectorStoreService)
    mock_vs.similarity_search.return_value = []
    return RetrievalService(vectorstore_service=mock_vs, bm25_index=BM25Index())


def make_mock_classifier(
    intent: str = IntentName.GENERAL,
    rag_required: bool = False,
) -> AsyncMock:
    mock = AsyncMock()
    mock.classify.return_value = IntentResult(
        intent=intent,
        confidence=0.9,
        search_strategy="none",
        model_tier="mini",
        rag_required=rag_required,
        metadata_filter={},
    )
    return mock


def make_intent_result(
    intent: str,
    search_strategy: str,
    rag_required: bool,
    be_data_required: bool = False,
    metadata_filter: dict | None = None,
) -> IntentResult:
    return IntentResult(
        intent=intent,
        confidence=0.9,
        search_strategy=search_strategy,
        model_tier="mini",
        rag_required=rag_required,
        metadata_filter=metadata_filter or {},
        be_data_required=be_data_required,
    )


# ---------------------------------------------------------------------------
# 공통 Fixture
# ---------------------------------------------------------------------------

@pytest.fixture
def settings() -> Settings:
    return make_settings()


@pytest.fixture
def mock_settings(settings: Settings) -> Settings:
    return settings


@pytest.fixture
def retrieval_service() -> RetrievalService:
    return make_retrieval_service()


@pytest.fixture
def mock_retrieval_service(retrieval_service: RetrievalService) -> RetrievalService:
    return retrieval_service


@pytest.fixture
def mock_classifier() -> AsyncMock:
    return AsyncMock()


@pytest.fixture
def mock_intent_classifier() -> AsyncMock:
    return make_mock_classifier()


@pytest.fixture
def mock_chat_service(
    mock_settings: Settings,
    mock_retrieval_service: RetrievalService,
    mock_intent_classifier: AsyncMock,
) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "test response"
        service = ChatService(
            settings=mock_settings,
            retrieval_service=mock_retrieval_service,
            intent_classifier=mock_intent_classifier,
        )
        yield service


@pytest.fixture
async def client(mock_chat_service: ChatService):
    app.dependency_overrides[get_chat_service] = lambda: mock_chat_service
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()
