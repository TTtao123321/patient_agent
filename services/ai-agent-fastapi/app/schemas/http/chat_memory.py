from datetime import datetime

from pydantic import BaseModel, Field


class ChatHistoryItem(BaseModel):
    session_id: str
    user_id: int
    role: str
    content: str
    intent: str | None = None
    agent_used: str | None = None
    created_at: datetime | None = None


class ChatHistoryResponse(BaseModel):
    session_id: str
    total: int
    messages: list[ChatHistoryItem]
