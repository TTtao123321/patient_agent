from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_core.documents import Document

from app.core.settings import settings


def split_documents(documents: list[Document]) -> list[Document]:
    """按配置的 chunk 大小与重叠率切分文档。"""
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=settings.chunk_size,
        chunk_overlap=settings.chunk_overlap,
        separators=["\n\n", "\n", "。", "；", " ", ""],
    )
    return splitter.split_documents(documents)
