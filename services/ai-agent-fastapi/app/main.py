from fastapi import FastAPI
from fastapi.responses import PlainTextResponse

from app.api.v1.endpoints.agent import router as agent_router
from app.api.v1.endpoints.rag import router as rag_router
from app.api.v1.endpoints.tool import router as tool_router
from app.integrations.rabbitmq.chat_task_consumer import chat_task_consumer
from app.observability.logging.logging_config import configure_logging
from app.observability.metrics.registry import metrics_registry
from app.observability.tracing.request_tracing_middleware import RequestTracingMiddleware

configure_logging()

app = FastAPI(title="Patient Agent AI Service", version="0.1.0")
app.add_middleware(RequestTracingMiddleware)

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


@app.get("/metrics")
def metrics() -> dict:
    return metrics_registry.snapshot()


@app.get("/metrics/prometheus", response_class=PlainTextResponse)
def metrics_prometheus() -> str:
    return metrics_registry.to_prometheus_text()
