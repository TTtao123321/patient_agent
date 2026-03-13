from langchain_huggingface import HuggingFaceEmbeddings

from app.core.settings import settings


def create_bge_m3_embeddings() -> HuggingFaceEmbeddings:
    """创建 BGE-M3 向量嵌入模型。"""
    return HuggingFaceEmbeddings(
        model_name=settings.bge_m3_model_name,
        model_kwargs={"device": "cpu"},
        encode_kwargs={"normalize_embeddings": True},
    )
