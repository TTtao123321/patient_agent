from collections.abc import Iterator
import logging
import time

from app.agents.router.router_agent import RouterAgent
from app.llm.llm_client import get_llm_client
from app.memory.session.session_manager import SessionManager


logger = logging.getLogger(__name__)


class ChatProcessor:
    """聊天处理主流程。

    - `process`：普通同步问答流程。
    - `stream`：流式问答流程，按 SSE 事件分块返回。
    """

    def __init__(self) -> None:
        self.router_agent = RouterAgent()
        self.session_manager = SessionManager()
        self.llm_client = get_llm_client()

    def process(self, session_id: str, user_id: int, message: str) -> tuple[str, str, str, int]:
        """同步处理聊天请求。"""
        started_at = time.perf_counter()
        # 构造短期上下文，提升多轮对话连贯性。
        context_text = self.session_manager.build_context_text(session_id=session_id)
        context_messages = self.session_manager.get_context_messages(session_id=session_id)
        query_with_context = message if not context_text else f"{context_text}\n当前问题: {message}"

        # 先持久化用户消息，再路由执行 Agent。
        self.session_manager.save_user_message(session_id=session_id, user_id=user_id, content=message)

        answer, intent, agent_used = self.router_agent.route(query_with_context, user_id)
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
        """流式处理聊天请求，按 start/chunk/done 事件输出。"""
        started_at = time.perf_counter()
        context_text = self.session_manager.build_context_text(session_id=session_id)
        context_messages = self.session_manager.get_context_messages(session_id=session_id)
        query_with_context = message if not context_text else f"{context_text}\n当前问题: {message}"

        self.session_manager.save_user_message(session_id=session_id, user_id=user_id, content=message)

        agent_result, intent, agent_used = self.router_agent.route(query_with_context, user_id)
        
        # 处理 Agent 返回的结果，支持字符串和字典两种格式
        draft_answer = ""
        structured_data = {}
        if isinstance(agent_result, dict):
            draft_answer = agent_result.get("answer", "")
            structured_data = {k: v for k, v in agent_result.items() if k != "answer"}
        else:
            draft_answer = agent_result
        
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
        # 报告结构化结果（JSON）不再经 LLM 润色，避免破坏结构。
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
                # 常规场景：对草稿做流式润色输出。
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
                # LLM 流式失败时走草稿兜底，不中断接口。
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

        # 在 done 事件中传递结构化数据
        done_data = {
            "session_id": session_id,
            "answer": final_answer,
            "intent": intent,
            "agent_used": agent_used,
        }
        if structured_data:
            done_data.update(structured_data)

        yield {
            "event": "done",
            "data": done_data,
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
        """构建给 LLM 的流式润色提示词。"""
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
        """将文本按固定长度切块，便于稳定推送 SSE chunk。"""
        if not text:
            return []
        return [text[index:index + chunk_size] for index in range(0, len(text), chunk_size)]

    def _should_bypass_stream_llm(self, intent: str, draft_answer: str) -> bool:
        """判断是否跳过 LLM 流式润色。"""
        return False
