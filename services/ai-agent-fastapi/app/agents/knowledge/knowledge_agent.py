from app.agents.base.base_agent import BaseAgent
from app.rag.retrieval.retriever import RagRetriever


class KnowledgeAgent(BaseAgent):
    """医学知识问答 Agent。

    主要用于通用医学问题，先走 RAG 检索，再按关键词补充工具查询结果。
    """
    
    def __init__(self) -> None:
        super().__init__()
        self.retriever: RagRetriever | None = None

    def handle(self, query: str) -> str:
        """处理医学知识类问题并返回文本回答。"""
        response = "[Knowledge Agent] "
        
        # 先从知识库检索相关片段。
        if self.retriever is None:
            self.retriever = RagRetriever()

        docs = self.retriever.retrieve(query=query, top_k=3)
        
        # 再基于关键词尝试调用科室/药物工具，补充可执行建议。
        department_keywords = ["科", "科室", "医生", "诊疗"]
        drug_keywords = ["药", "用药", "治疗"]
        
        tool_results = []
        
        # 命中科室关键词时，补充推荐科室。
        if any(kw in query for kw in department_keywords):
            dept_result = self.call_tool("search_department", department_name="呼吸科", limit=1)
            if dept_result["success"] and dept_result["data"]:
                tool_results.append(f"推荐科室: {dept_result['data']}")
        
        # 命中药物关键词时，补充用药信息。
        if any(kw in query for kw in drug_keywords):
            drug_result = self.call_tool("search_drug", drug_name="氨溴索", limit=1)
            if drug_result["success"] and drug_result["data"]:
                tool_results.append(f"相关用药: {drug_result['data']}")
        
        # 汇总 RAG 结果。
        if not docs:
            response += "未在知识库中检索到高相关内容。"
        else:
            snippets = [doc.page_content.strip().replace("\n", " ")[:120] for doc in docs]
            refs = " | ".join(snippets)
            response += f"知识库检索结果: {refs}。"
        
        # 拼接工具结果。
        if tool_results:
            response += " " + " ".join(tool_results)
        
        response += " 如需更多专业医疗建议，请咨询医疗专业人士。"
        return response
