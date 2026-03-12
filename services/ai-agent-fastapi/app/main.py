from fastapi import FastAPI

from app.api.v1.endpoints.agent import router as agent_router
from app.api.v1.endpoints.rag import router as rag_router

app = FastAPI(title="Patient Agent AI Service", version="0.1.0")

app.include_router(agent_router)
app.include_router(rag_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
