from unittest.mock import AsyncMock, patch

import pytest

from app.services.chat_service import ChatService
from app.services.intent_classifier import IntentName
from app.services.retrieval_service import RetrievalService, SearchResult

from .conftest import make_intent_result


@pytest.fixture
def chat_service(settings, retrieval_service, mock_classifier) -> ChatService:
    with patch.object(ChatService, "_call_llm", new_callable=AsyncMock) as mock_llm:
        mock_llm.return_value = "test response"
        svc = ChatService(
            settings=settings,
            retrieval_service=retrieval_service,
            intent_classifier=mock_classifier,
        )
        svc._mock_llm = mock_llm
        svc.query_rewriter.rewrite = AsyncMock(side_effect=lambda q: q)
        yield svc


class TestPipelineIntegration:

    async def test_general_intent_skips_rag(self, chat_service, mock_classifier, retrieval_service):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            await chat_service.get_response("hello")
            mock_retrieve.assert_not_called()

    async def test_expense_intent_calls_retrieve_with_hybrid_be(
        self, chat_service, mock_classifier, retrieval_service
    ):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.EXPENSE_CLASSIFICATION,
            search_strategy="hybrid_with_be_data",
            rag_required=True,
            metadata_filter={"topic": ["expense"]},
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("expense processing for dinner")
            mock_retrieve.assert_called_once()
            call_kwargs = mock_retrieve.call_args.kwargs
            assert call_kwargs["search_strategy"] == "hybrid_with_be_data"

    async def test_concept_intent_uses_vector_search(
        self, chat_service, mock_classifier, retrieval_service
    ):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.CONCEPT_EXPLANATION,
            search_strategy="vector",
            rag_required=True,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("What is withholding tax?")
            mock_retrieve.assert_called_once()
            assert mock_retrieve.call_args.kwargs["search_strategy"] == "vector"

    async def test_tax_rate_intent_uses_metadata_filter(
        self, chat_service, mock_classifier, retrieval_service
    ):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.TAX_RATE_LOOKUP,
            search_strategy="metadata_filter",
            rag_required=True,
            metadata_filter={"topic": ["tax_rate"]},
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("Tell me the income tax rate")
            mock_retrieve.assert_called_once()
            call_kwargs = mock_retrieve.call_args.kwargs
            assert call_kwargs["search_strategy"] == "metadata_filter"
            assert call_kwargs["metadata_filter"] == {"topic": ["tax_rate"]}

    async def test_comparison_intent_uses_multi_query(
        self, chat_service, mock_classifier, retrieval_service
    ):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.COMPARISON,
            search_strategy="multi_query",
            rag_required=True,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            mock_retrieve.return_value = []
            await chat_service.get_response("simplified vs general taxation comparison")
            assert mock_retrieve.call_args.kwargs["search_strategy"] == "multi_query"

    async def test_intent_prompt_applied_in_system_message(
        self, chat_service, mock_classifier
    ):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.TAX_RATE_LOOKUP,
            search_strategy="metadata_filter",
            rag_required=False,
        )
        await chat_service.get_response("Tell me the tax rate")
        call_args = chat_service._mock_llm.call_args[0][0]
        system_messages = [
            msg for msg in call_args
            if hasattr(msg, "type") and msg.type == "system"
        ]
        assert len(system_messages) >= 1
        system_content = system_messages[0].content
        assert "세율" in system_content or "한국 세금 전문가" in system_content

    async def test_search_results_included_in_context(
        self, chat_service, mock_classifier, retrieval_service
    ):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.TAX_RATE_LOOKUP,
            search_strategy="metadata_filter",
            rag_required=True,
        )
        mock_results = [
            SearchResult(
                content="Income Tax Act Article 55 tax rate provisions",
                metadata={"law_name": "Income Tax Act", "law_type": "law", "chunk_id": "c1"},
                score=0.9,
            )
        ]
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock, return_value=mock_results
        ):
            await chat_service.get_response("Tell me the income tax rate")
            call_args = chat_service._mock_llm.call_args[0][0]
            system_msg = next(
                msg for msg in call_args
                if hasattr(msg, "type") and msg.type == "system"
            )
            assert "Income Tax Act Article 55" in system_msg.content

    async def test_rag_not_required_skips_retrieve(
        self, chat_service, mock_classifier, retrieval_service
    ):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock
        ) as mock_retrieve:
            await chat_service.get_response("Thank you")
            mock_retrieve.assert_not_called()

    async def test_session_id_preserved(self, chat_service, mock_classifier):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        _, sid, _ = await chat_service.get_response("hello", session_id="my-session")
        assert sid == "my-session"

    async def test_history_preserved_across_calls(self, chat_service, mock_classifier):
        mock_classifier.classify.return_value = make_intent_result(
            intent=IntentName.GENERAL,
            search_strategy="none",
            rag_required=False,
        )
        _, sid, _ = await chat_service.get_response("first", session_id="hist-session")
        await chat_service.get_response("second", session_id=sid)

        history = chat_service.get_history(sid)
        assert len(history) == 4  # (user+ai) * 2

    async def test_all_intents_rag_routing(self, chat_service, mock_classifier, retrieval_service):
        rag_intents = [
            (IntentName.TAX_RATE_LOOKUP, "metadata_filter", True),
            (IntentName.EXPENSE_CLASSIFICATION, "hybrid_with_be_data", True),
            (IntentName.DEDUCTION_ELIGIBILITY, "hybrid", True),
            (IntentName.PROCEDURE_GUIDE, "hybrid", True),
            (IntentName.CONCEPT_EXPLANATION, "vector", True),
            (IntentName.CALCULATION, "hybrid_with_be_data", True),
            (IntentName.COMPARISON, "multi_query", True),
            (IntentName.GENERAL, "none", False),
        ]
        with patch.object(
            retrieval_service, "retrieve", new_callable=AsyncMock, return_value=[]
        ) as mock_retrieve:
            for intent_name, strategy, rag_required in rag_intents:
                mock_retrieve.reset_mock()
                mock_classifier.classify.return_value = make_intent_result(
                    intent=intent_name,
                    search_strategy=strategy,
                    rag_required=rag_required,
                )
                await chat_service.get_response(f"test question for {intent_name}")

                if rag_required:
                    mock_retrieve.assert_called_once()
                    assert mock_retrieve.call_args.kwargs["search_strategy"] == strategy
                else:
                    mock_retrieve.assert_not_called()
