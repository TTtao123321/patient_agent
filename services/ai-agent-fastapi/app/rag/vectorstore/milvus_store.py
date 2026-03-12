from langchain_core.documents import Document
from langchain_milvus import Milvus

from app.core.settings import settings
from app.rag.embeddings.bge_m3_embeddings import create_bge_m3_embeddings


class MilvusVectorStore:
    def __init__(self) -> None:
        self._embeddings = create_bge_m3_embeddings()
        self._store = Milvus(
            embedding_function=self._embeddings,
            collection_name=settings.rag_collection_name,
            connection_args={"uri": settings.milvus_uri},
            auto_id=True,
        )

    def add_documents(self, documents: list[Document]) -> int:
        ids = self._store.add_documents(documents)
        return len(ids)

    def similarity_search(self, query: str, top_k: int = 5) -> list[Document]:
        return self._store.similarity_search(query=query, k=top_k)
