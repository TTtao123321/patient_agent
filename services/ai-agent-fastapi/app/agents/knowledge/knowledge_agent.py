import logging

from app.agents.base.base_agent import BaseAgent
from app.rag.retrieval.retriever import RagRetriever


logger = logging.getLogger(__name__)


class KnowledgeAgent(BaseAgent):
    """医学知识问答 Agent。

    主要用于通用医学问题，先走 RAG 检索，再按关键词补充工具查询结果。
    """
    
    def __init__(self) -> None:
        super().__init__()
        self.retriever: RagRetriever | None = None
        self._rag_disabled = False

    def handle(self, query: str) -> str:
        """处理医学知识类问题并返回文本回答。"""
        lines: list[str] = ["【医学知识 Agent】"]

        docs = []
        rag_error: Exception | None = None

        # 先从知识库检索相关片段；异常时降级，避免整条链路报错。
        if not self._rag_disabled:
            if self.retriever is None:
                try:
                    self.retriever = RagRetriever()
                except Exception as exc:  # pragma: no cover - depends on external infra
                    rag_error = exc
                    self._rag_disabled = True
            if self.retriever is not None and rag_error is None:
                try:
                    docs = self.retriever.retrieve(query=query, top_k=3)
                except Exception as exc:  # pragma: no cover - depends on external infra
                    rag_error = exc
                    self._rag_disabled = True

        if rag_error is not None:
            logger.warning("knowledge_rag_unavailable_fallback error=%s", rag_error)
            return self._build_fallback_response(query=query)

        lines.append(f"[RAG 检索] similarity_search → 命中 {len(docs)} 篇相关文档")

        # 再基于关键词尝试调用科室/药物工具，补充可执行建议。
        department_keywords = ["科", "科室", "医生", "诊疗"]
        drug_keywords = ["药", "用药", "治疗"]

        dept_info: str | None = None
        drug_info: str | None = None

        # 命中科室关键词时，补充推荐科室。
        if any(kw in query for kw in department_keywords):
            dept_result = self.call_tool("search_department", department_name="呼吸科", limit=1)
            if dept_result["success"] and dept_result["data"]:
                dept_info = str(dept_result["data"])
                lines.append(f"[工具调用] search_department(呼吸科) → {dept_info[:80]}")
            else:
                lines.append("[工具调用] search_department(呼吸科) → 未找到科室信息")

        # 命中药物关键词时，补充用药信息。
        if any(kw in query for kw in drug_keywords):
            drug_result = self.call_tool("search_drug", drug_name="氨溴索", limit=1)
            if drug_result["success"] and drug_result["data"]:
                drug_info = str(drug_result["data"])
                lines.append(f"[工具调用] search_drug(氨溴索) → {drug_info[:80]}")
            else:
                lines.append("[工具调用] search_drug(氨溴索) → 未找到药物信息")

        lines.append("")

        # 汇总 RAG 结果。
        if not docs:
            lines.append("知识库中未检索到高相关内容。")
        else:
            snippets = [doc.page_content.strip().replace("\n", " ")[:120] for doc in docs]
            lines.append("知识库参考：")
            for i, snippet in enumerate(snippets, 1):
                lines.append(f"  {i}. {snippet}")

        # 拼接工具结果。
        if dept_info:
            lines.append(f"推荐科室：{dept_info}")
        if drug_info:
            lines.append(f"相关用药：{drug_info}")

        lines.append("")
        lines.append("如需更多专业医疗建议，请咨询医疗专业人士。")
        return "\n".join(lines)

    def _build_fallback_response(self, query: str) -> str:
        """RAG 不可用时的文本降级回复。"""
        text = (query or "").strip().lower()
        greetings = {"你好", "您好", "hi", "hello", "嗨", "在吗", "在么"}

        header = "【医学知识 Agent】\n[RAG 检索] similarity_search → 知识库服务当前不可用，已降级\n"

        if text in greetings:
            return (
                header
                + "\n你好，我在。"
                + "虽然知识库暂时不可用，我仍可进行基础分诊和健康建议。"
                + "请直接告诉我具体症状、持续时间和体温等信息。"
            )

        return (
            header
            + "\n当前知识库服务暂时不可用，已自动切换为基础问答模式。"
            + "请补充更具体的信息（如症状、持续时间、体温、是否有基础病/用药），"
            + "我会先给你通用建议；如症状加重请及时线下就医。"
        )
