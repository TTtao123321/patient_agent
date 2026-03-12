from app.core.settings import settings
from app.memory.store.chat_memory_store import ChatMemoryStore


class SessionManager:
    def __init__(self) -> None:
        self.store = ChatMemoryStore()

    def get_context_messages(self, session_id: str) -> list[dict]:
        return self.store.get_short_term_messages(
            session_id=session_id,
            limit=settings.context_message_limit,
        )

    def build_context_text(self, session_id: str) -> str:
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
        return self.store.get_history_messages(session_id=session_id, limit=limit)
