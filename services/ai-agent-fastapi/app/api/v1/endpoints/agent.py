from fastapi import APIRouter

from app.agents.router.router_agent import RouterAgent
from app.schemas.http.agent_chat import AgentChatRequest, AgentChatResponse

router = APIRouter(prefix="/agent", tags=["agent"])
router_agent = RouterAgent()


@router.post("/chat", response_model=AgentChatResponse)
def chat(request: AgentChatRequest) -> AgentChatResponse:
    answer, intent, agent_used = router_agent.route(request.query)
    return AgentChatResponse(answer=answer, intent=intent, agent_used=agent_used)
