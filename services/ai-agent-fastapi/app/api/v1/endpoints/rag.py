from fastapi import APIRouter

from app.rag.retrieval.retriever import RagRetriever
from app.schemas.http.rag import (
    RagIngestRequest,
    RagIngestResponse,
    RagRetrieveItem,
    RagRetrieveRequest,
    RagRetrieveResponse,
)

router = APIRouter(prefix="/rag", tags=["rag"])
retriever: RagRetriever | None = None


def get_retriever() -> RagRetriever:
    """延迟初始化检索器，避免应用启动时加载重资源。"""
    global retriever
    if retriever is None:
        retriever = RagRetriever()
    return retriever


@router.post("/ingest", response_model=RagIngestResponse)
def ingest(request: RagIngestRequest) -> RagIngestResponse:
    """导入文档到向量库。"""
    result = get_retriever().ingest_path(request.input_path)
    return RagIngestResponse(**result)


@router.post("/retrieve", response_model=RagRetrieveResponse)
def retrieve(request: RagRetrieveRequest) -> RagRetrieveResponse:
    """按 query 检索相关知识片段。"""
    docs = get_retriever().retrieve(query=request.query, top_k=request.top_k)
    items = [
        RagRetrieveItem(content=doc.page_content, metadata=doc.metadata or {})
        for doc in docs
    ]
    return RagRetrieveResponse(items=items)
