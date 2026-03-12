from pydantic import BaseModel, Field


class AgentChatRequest(BaseModel):
    session_no: str = Field(..., description="Chat session number")
    user_id: int = Field(..., description="User id")
    query: str = Field(..., description="User input text")


class AgentChatResponse(BaseModel):
    answer: str
    intent: str
    agent_used: str
