from app.agents.base.base_agent import BaseAgent
from app.rag.retrieval.retriever import RagRetriever


class KnowledgeAgent(BaseAgent):
    def __init__(self) -> None:
        self.retriever: RagRetriever | None = None

    def handle(self, query: str) -> str:
        if self.retriever is None:
            self.retriever = RagRetriever()

        docs = self.retriever.retrieve(query=query, top_k=3)
        if not docs:
            return (
                "[Knowledge Agent] 未在知识库中检索到高相关内容。"
                "请补充更具体的医学问题或关键词。"
            )

        snippets = [doc.page_content.strip().replace("\n", " ")[:120] for doc in docs]
        refs = " | ".join(snippets)
        return (
            "[Knowledge Agent] 已完成知识库检索。"
            f"相关片段: {refs}"
        )
