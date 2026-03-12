from app.agents.base.base_agent import BaseAgent
from app.rag.retrieval.retriever import RagRetriever


class KnowledgeAgent(BaseAgent):
    """Agent for handling medical knowledge queries."""
    
    def __init__(self) -> None:
        super().__init__()
        self.retriever: RagRetriever | None = None

    def handle(self, query: str) -> str:
        """
        Handle medical knowledge query.
        
        Uses RAG retrieval and tool calling to provide comprehensive answers.
        """
        response = "[Knowledge Agent] "
        
        # First, try RAG retrieval for knowledge base information
        if self.retriever is None:
            self.retriever = RagRetriever()

        docs = self.retriever.retrieve(query=query, top_k=3)
        
        # Then, try to search for relevant departments and drugs
        # Extract potential keywords from query
        department_keywords = ["科", "科室", "医生", "诊疗"]
        drug_keywords = ["药", "用药", "治疗"]
        
        tool_results = []
        
        # Search for relevant departments if query contains department keyword
        if any(kw in query for kw in department_keywords):
            dept_result = self.call_tool("search_department", department_name="呼吸科", limit=1)
            if dept_result["success"] and dept_result["data"]:
                tool_results.append(f"推荐科室: {dept_result['data']}")
        
        # Search for relevant drugs if query contains drug keyword
        if any(kw in query for kw in drug_keywords):
            drug_result = self.call_tool("search_drug", drug_name="氨溴索", limit=1)
            if drug_result["success"] and drug_result["data"]:
                tool_results.append(f"相关用药: {drug_result['data']}")
        
        # Compile response from RAG results
        if not docs:
            response += "未在知识库中检索到高相关内容。"
        else:
            snippets = [doc.page_content.strip().replace("\n", " ")[:120] for doc in docs]
            refs = " | ".join(snippets)
            response += f"知识库检索结果: {refs}。"
        
        # Add tool results
        if tool_results:
            response += " " + " ".join(tool_results)
        
        response += " 如需更多专业医疗建议，请咨询医疗专业人士。"
        return response
