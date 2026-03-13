from pydantic import BaseModel, Field, model_validator


class AgentChatRequest(BaseModel):
    """聊天请求体。"""

    session_id: str | None = Field(default=None, description="Chat session id")
    session_no: str | None = Field(default=None, description="Backward-compatible chat session number")
    user_id: int = Field(..., description="User id")
    query: str = Field(..., description="User input text")

    @model_validator(mode="after")
    def validate_session_field(self) -> "AgentChatRequest":
        """兼容 session_id/session_no 两种字段。"""
        if not self.session_id and not self.session_no:
            raise ValueError("session_id is required")
        return self

    def get_session_id(self) -> str:
        """获取统一会话 ID。"""
        if self.session_id:
            return self.session_id
        if self.session_no:
            return self.session_no
        raise ValueError("session_id is required")


class AgentChatResponse(BaseModel):
    """聊天响应体。"""

    session_id: str
    answer: str
    intent: str
    agent_used: str
    used_context_messages: int = 0
