from app.agents.router.router_agent import RouterAgent
from app.memory.session.session_manager import SessionManager


class ChatProcessor:
    def __init__(self) -> None:
        self.router_agent = RouterAgent()
        self.session_manager = SessionManager()

    def process(self, session_id: str, user_id: int, message: str) -> tuple[str, str, str, int]:
        context_text = self.session_manager.build_context_text(session_id=session_id)
        context_messages = self.session_manager.get_context_messages(session_id=session_id)
        query_with_context = message if not context_text else f"{context_text}\n当前问题: {message}"

        self.session_manager.save_user_message(session_id=session_id, user_id=user_id, content=message)

        answer, intent, agent_used = self.router_agent.route(query_with_context)
        self.session_manager.save_assistant_message(
            session_id=session_id,
            user_id=user_id,
            content=answer,
            intent=intent,
            agent_used=agent_used,
        )
        return answer, intent, agent_used, len(context_messages)
