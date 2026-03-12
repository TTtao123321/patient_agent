from fastapi import APIRouter

from app.memory.session.session_manager import SessionManager
from app.schemas.http.chat_memory import ChatHistoryItem, ChatHistoryResponse
from app.schemas.http.agent_chat import AgentChatRequest, AgentChatResponse
from app.services.chat.chat_processor import ChatProcessor

router = APIRouter(prefix="/agent", tags=["agent"])
session_manager = SessionManager()
chat_processor = ChatProcessor()


@router.post("/chat", response_model=AgentChatResponse)
def chat(request: AgentChatRequest) -> AgentChatResponse:
    session_id = request.get_session_id()
    answer, intent, agent_used, used_context_messages = chat_processor.process(
        session_id=session_id,
        user_id=request.user_id,
        message=request.query,
    )

    return AgentChatResponse(
        session_id=session_id,
        answer=answer,
        intent=intent,
        agent_used=agent_used,
        used_context_messages=used_context_messages,
    )


@router.get("/sessions/{session_id}/history", response_model=ChatHistoryResponse)
def get_session_history(session_id: str, limit: int = 50) -> ChatHistoryResponse:
    messages = session_manager.get_history(session_id=session_id, limit=limit)
    items = [ChatHistoryItem(**item) for item in messages]
    return ChatHistoryResponse(session_id=session_id, total=len(items), messages=items)
