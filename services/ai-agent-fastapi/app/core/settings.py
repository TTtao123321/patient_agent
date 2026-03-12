import os


class Settings:
    rag_collection_name: str = os.getenv("RAG_COLLECTION_NAME", "medical_knowledge")
    milvus_host: str = os.getenv("MILVUS_HOST", "localhost")
    milvus_port: str = os.getenv("MILVUS_PORT", "19530")
    bge_m3_model_name: str = os.getenv("BGE_M3_MODEL_NAME", "BAAI/bge-m3")
    chunk_size: int = int(os.getenv("RAG_CHUNK_SIZE", "800"))
    chunk_overlap: int = int(os.getenv("RAG_CHUNK_OVERLAP", "120"))

    @property
    def milvus_uri(self) -> str:
        return f"http://{self.milvus_host}:{self.milvus_port}"


settings = Settings()
