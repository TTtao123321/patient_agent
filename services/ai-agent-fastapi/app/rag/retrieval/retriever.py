from langchain_core.documents import Document

from app.rag.ingestion.document_loader import load_documents
from app.rag.ingestion.text_splitter import split_documents
from app.rag.vectorstore.milvus_store import MilvusVectorStore


class RagRetriever:
    def __init__(self) -> None:
        self.vector_store = MilvusVectorStore()

    def ingest_path(self, input_path: str) -> dict[str, int]:
        raw_docs = load_documents(input_path)
        chunks = split_documents(raw_docs)
        stored = self.vector_store.add_documents(chunks)
        return {
            "raw_documents": len(raw_docs),
            "chunks": len(chunks),
            "stored": stored,
        }

    def retrieve(self, query: str, top_k: int = 5) -> list[Document]:
        return self.vector_store.similarity_search(query=query, top_k=top_k)
