from app.core.settings import settings
from app.memory.store.chat_memory_store import ChatMemoryStore


class SessionManager:
    """会话记忆管理器。

    封装短期上下文获取、历史拼接和消息持久化逻辑。
    """

    def __init__(self) -> None:
        self.store = ChatMemoryStore()

    def get_context_messages(self, session_id: str) -> list[dict]:
        """读取用于上下文拼接的短期消息。"""
        return self.store.get_short_term_messages(
            session_id=session_id,
            limit=settings.context_message_limit,
        )

    def build_context_text(self, session_id: str) -> str:
        """把短期消息拼成可注入 Prompt 的上下文文本。"""
        messages = self.get_context_messages(session_id)
        if not messages:
            return ""

        lines: list[str] = []
        for message in messages:
            role = message.get("role", "user")
            content = message.get("content", "")
            if not content:
                continue
            label = "用户" if role == "user" else "助手"
            lines.append(f"{label}: {content}")

        if not lines:
            return ""

        history = "\n".join(lines)
        return f"历史对话上下文:\n{history}\n"

    def save_user_message(self, session_id: str, user_id: int, content: str) -> None:
        """保存用户消息到 Redis 短期记忆和 MySQL 历史表。"""
        self.store.append_short_term_message(session_id=session_id, user_id=user_id, role="user", content=content)
        self.store.append_history_message(session_id=session_id, user_id=user_id, role="user", content=content)

    def save_assistant_message(
        self,
        session_id: str,
        user_id: int,
        content: str,
        intent: str,
        agent_used: str,
    ) -> None:
        """保存助手消息，并记录 intent/agent_used 元数据。"""
        self.store.append_short_term_message(session_id=session_id, user_id=user_id, role="assistant", content=content)
        self.store.append_history_message(
            session_id=session_id,
            user_id=user_id,
            role="assistant",
            content=content,
            intent=intent,
            agent_used=agent_used,
        )

    def get_history(self, session_id: str, limit: int) -> list[dict]:
        """读取会话历史消息。"""
        return self.store.get_history_messages(session_id=session_id, limit=limit)
