from pydantic import BaseModel, Field


class RagIngestRequest(BaseModel):
    """RAG 导入请求。"""

    input_path: str = Field(..., description="Document file or directory path")


class RagIngestResponse(BaseModel):
    """RAG 导入响应。"""

    raw_documents: int
    chunks: int
    stored: int


class RagRetrieveRequest(BaseModel):
    """RAG 检索请求。"""

    query: str = Field(..., description="User query")
    top_k: int = Field(default=5, ge=1, le=20)


class RagRetrieveItem(BaseModel):
    """RAG 命中文档项。"""

    content: str
    metadata: dict


class RagRetrieveResponse(BaseModel):
    """RAG 检索响应。"""

    items: list[RagRetrieveItem]
