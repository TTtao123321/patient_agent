from fastapi import FastAPI

from app.api.v1.endpoints.agent import router as agent_router
from app.api.v1.endpoints.rag import router as rag_router
from app.api.v1.endpoints.tool import router as tool_router
from app.integrations.rabbitmq.chat_task_consumer import chat_task_consumer

app = FastAPI(title="Patient Agent AI Service", version="0.1.0")

app.include_router(agent_router)
app.include_router(rag_router)
app.include_router(tool_router)


@app.on_event("startup")
def on_startup() -> None:
    chat_task_consumer.start()


@app.on_event("shutdown")
def on_shutdown() -> None:
    chat_task_consumer.stop()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
