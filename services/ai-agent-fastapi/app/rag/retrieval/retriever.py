from langchain_core.documents import Document

from app.rag.ingestion.document_loader import load_documents
from app.rag.ingestion.text_splitter import split_documents
from app.rag.vectorstore.milvus_store import MilvusVectorStore


class RagRetriever:
    """RAG 检索器。

    提供文档导入（ingest）和相似度检索（retrieve）两种能力。
    """

    def __init__(self) -> None:
        self.vector_store = MilvusVectorStore()

    def ingest_path(self, input_path: str) -> dict[str, int]:
        """导入路径下文档到向量库并返回统计信息。"""
        raw_docs = load_documents(input_path)
        chunks = split_documents(raw_docs)
        stored = self.vector_store.add_documents(chunks)
        return {
            "raw_documents": len(raw_docs),
            "chunks": len(chunks),
            "stored": stored,
        }

    def retrieve(self, query: str, top_k: int = 5) -> list[Document]:
        """按 query 检索 top_k 条相关文档。"""
        return self.vector_store.similarity_search(query=query, top_k=top_k)
