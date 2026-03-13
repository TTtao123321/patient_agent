from collections.abc import Iterator
import logging
import time

from app.agents.router.router_agent import RouterAgent
from app.llm.llm_client import get_llm_client
from app.memory.session.session_manager import SessionManager


logger = logging.getLogger(__name__)


class ChatProcessor:
    def __init__(self) -> None:
        self.router_agent = RouterAgent()
        self.session_manager = SessionManager()
        self.llm_client = get_llm_client()

    def process(self, session_id: str, user_id: int, message: str) -> tuple[str, str, str, int]:
        started_at = time.perf_counter()
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
        logger.info(
            "chat_process_completed session_id=%s user_id=%s intent=%s agent=%s latency_ms=%.2f",
            session_id,
            user_id,
            intent,
            agent_used,
            (time.perf_counter() - started_at) * 1000,
        )
        return answer, intent, agent_used, len(context_messages)

    def stream(self, session_id: str, user_id: int, message: str) -> Iterator[dict]:
        started_at = time.perf_counter()
        context_text = self.session_manager.build_context_text(session_id=session_id)
        context_messages = self.session_manager.get_context_messages(session_id=session_id)
        query_with_context = message if not context_text else f"{context_text}\n当前问题: {message}"

        self.session_manager.save_user_message(session_id=session_id, user_id=user_id, content=message)

        draft_answer, intent, agent_used = self.router_agent.route(query_with_context)
        prompt = self._build_stream_prompt(message=message, draft_answer=draft_answer, intent=intent)

        yield {
            "event": "start",
            "data": {
                "session_id": session_id,
                "intent": intent,
                "agent_used": agent_used,
                "used_context_messages": len(context_messages),
            },
        }

        logger.info(
            "chat_stream_started session_id=%s user_id=%s intent=%s agent=%s",
            session_id,
            user_id,
            intent,
            agent_used,
        )

        chunks: list[str] = []
        if self._should_bypass_stream_llm(intent=intent, draft_answer=draft_answer):
            final_answer = draft_answer
            for segment in self._segment_text(final_answer):
                chunks.append(segment)
                yield {
                    "event": "chunk",
                    "data": {
                        "content": segment,
                    },
                }
        else:
            try:
                for chunk in self.llm_client.stream(prompt):
                    if not chunk:
                        continue
                    for segment in self._segment_text(chunk):
                        chunks.append(segment)
                        yield {
                            "event": "chunk",
                            "data": {
                                "content": segment,
                            },
                        }
            except Exception:
                pass

            final_answer = "".join(chunks).strip()
            if not final_answer:
                final_answer = draft_answer
                for segment in self._segment_text(final_answer):
                    yield {
                        "event": "chunk",
                        "data": {
                            "content": segment,
                        },
                    }

        self.session_manager.save_assistant_message(
            session_id=session_id,
            user_id=user_id,
            content=final_answer,
            intent=intent,
            agent_used=agent_used,
        )

        yield {
            "event": "done",
            "data": {
                "session_id": session_id,
                "answer": final_answer,
                "intent": intent,
                "agent_used": agent_used,
            },
        }
        logger.info(
            "chat_stream_completed session_id=%s user_id=%s intent=%s agent=%s latency_ms=%.2f",
            session_id,
            user_id,
            intent,
            agent_used,
            (time.perf_counter() - started_at) * 1000,
        )

    def _build_stream_prompt(self, message: str, draft_answer: str, intent: str) -> str:
        return f"""
你是医疗问答助手。请基于用户问题和已有专业草稿，生成一段自然、清晰、适合直接展示给用户的中文回答。

要求：
1. 保持医学表达谨慎，不要编造诊断结论。
2. 内容要简洁清楚，优先直接回答用户问题。
3. 如果草稿里已有建议，请保留有效信息并润色。
4. 不要输出标题，不要使用 Markdown。
5. 当前意图类型：{intent}

用户问题：
{message}

已有草稿：
{draft_answer}

请直接输出最终回答：
""".strip()

    def _segment_text(self, text: str, chunk_size: int = 24) -> list[str]:
        if not text:
            return []
        return [text[index:index + chunk_size] for index in range(0, len(text), chunk_size)]

    def _should_bypass_stream_llm(self, intent: str, draft_answer: str) -> bool:
        if intent != "report_analysis":
            return False
        text = draft_answer.strip()
        return text.startswith("{") or text.startswith("[")
