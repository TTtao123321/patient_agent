from fastapi import APIRouter

from app.agents.router.router_agent import RouterAgent
from app.memory.session.session_manager import SessionManager
from app.schemas.http.chat_memory import ChatHistoryItem, ChatHistoryResponse
from app.schemas.http.agent_chat import AgentChatRequest, AgentChatResponse

router = APIRouter(prefix="/agent", tags=["agent"])
router_agent = RouterAgent()
session_manager = SessionManager()


@router.post("/chat", response_model=AgentChatResponse)
def chat(request: AgentChatRequest) -> AgentChatResponse:
    session_id = request.get_session_id()
    context_text = session_manager.build_context_text(session_id=session_id)
    context_messages = session_manager.get_context_messages(session_id=session_id)
    query_with_context = request.query if not context_text else f"{context_text}\n当前问题: {request.query}"

    session_manager.save_user_message(session_id=session_id, user_id=request.user_id, content=request.query)

    answer, intent, agent_used = router_agent.route(query_with_context)
    session_manager.save_assistant_message(
        session_id=session_id,
        user_id=request.user_id,
        content=answer,
        intent=intent,
        agent_used=agent_used,
    )

    return AgentChatResponse(
        session_id=session_id,
        answer=answer,
        intent=intent,
        agent_used=agent_used,
        used_context_messages=len(context_messages),
    )


@router.get("/sessions/{session_id}/history", response_model=ChatHistoryResponse)
def get_session_history(session_id: str, limit: int = 50) -> ChatHistoryResponse:
    messages = session_manager.get_history(session_id=session_id, limit=limit)
    items = [ChatHistoryItem(**item) for item in messages]
    return ChatHistoryResponse(session_id=session_id, total=len(items), messages=items)
