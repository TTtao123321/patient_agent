from pydantic import BaseModel, Field


class RagIngestRequest(BaseModel):
    input_path: str = Field(..., description="Document file or directory path")


class RagIngestResponse(BaseModel):
    raw_documents: int
    chunks: int
    stored: int


class RagRetrieveRequest(BaseModel):
    query: str = Field(..., description="User query")
    top_k: int = Field(default=5, ge=1, le=20)


class RagRetrieveItem(BaseModel):
    content: str
    metadata: dict


class RagRetrieveResponse(BaseModel):
    items: list[RagRetrieveItem]
