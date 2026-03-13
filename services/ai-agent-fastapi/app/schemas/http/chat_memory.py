from datetime import datetime

from pydantic import BaseModel, Field


class ChatHistoryItem(BaseModel):
    """单条历史消息结构。"""

    session_id: str
    user_id: int
    role: str
    content: str
    intent: str | None = None
    agent_used: str | None = None
    created_at: datetime | None = None


class ChatHistoryResponse(BaseModel):
    """历史消息查询响应。"""

    session_id: str
    total: int
    messages: list[ChatHistoryItem]
