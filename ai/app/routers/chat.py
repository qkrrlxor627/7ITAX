from fastapi import APIRouter, Depends

from app.core.dependencies import get_chat_service
from app.models.chat import ChatHistoryResponse, ChatRequest, ChatResponse
from app.services.chat_service import ChatService

router = APIRouter(prefix="/api/v1/chat", tags=["chat"])


@router.post("/", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    service: ChatService = Depends(get_chat_service),
) -> ChatResponse:
    answer, session_id, model_used = await service.get_response(
        request.message, request.session_id, request.user_id
    )
    return ChatResponse(
        answer=answer, model=model_used, session_id=session_id
    )


@router.get("/history/{session_id}", response_model=ChatHistoryResponse)
async def get_history(
    session_id: str,
    service: ChatService = Depends(get_chat_service),
) -> ChatHistoryResponse:
    """특정 세션의 채팅 히스토리를 조회한다.

    세션 ID에 해당하는 모든 메시지 기록을 반환한다.
    \f

    Args:
        session_id: 조회할 세션 ID.
        service: ChatService 의존성 주입.

    Returns:
        ChatHistoryResponse: 세션의 채팅 메시지 목록과 메시지 수.
    """
    messages = service.get_history(session_id)
    return ChatHistoryResponse(session_id=session_id, messages=messages, message_count=len(messages),)
