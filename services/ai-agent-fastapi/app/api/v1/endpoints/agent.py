import json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.memory.session.session_manager import SessionManager
from app.schemas.http.chat_memory import ChatHistoryItem, ChatHistoryResponse
from app.schemas.http.agent_chat import AgentChatRequest, AgentChatResponse
from app.services.chat.chat_processor import ChatProcessor

router = APIRouter(prefix="/agent", tags=["agent"])
session_manager = SessionManager()
chat_processor = ChatProcessor()


def _format_sse(event_name: str, payload: dict) -> str:
    return f"event: {event_name}\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


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


@router.post("/chat/stream")
def stream_chat(request: AgentChatRequest) -> StreamingResponse:
    session_id = request.get_session_id()

    def event_generator():
        for item in chat_processor.stream(
            session_id=session_id,
            user_id=request.user_id,
            message=request.query,
        ):
            yield _format_sse(item["event"], item["data"])

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.get("/sessions/{session_id}/history", response_model=ChatHistoryResponse)
def get_session_history(session_id: str, limit: int = 50) -> ChatHistoryResponse:
    messages = session_manager.get_history(session_id=session_id, limit=limit)
    items = [ChatHistoryItem(**item) for item in messages]
    return ChatHistoryResponse(session_id=session_id, total=len(items), messages=items)
