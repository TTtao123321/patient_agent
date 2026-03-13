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
# 全局请求追踪中间件：注入 request_id、记录请求指标。
app.add_middleware(RequestTracingMiddleware)

app.include_router(agent_router)
app.include_router(rag_router)
app.include_router(tool_router)


@app.on_event("startup")
def on_startup() -> None:
    # 启动 RabbitMQ 消费线程，处理异步 AI 任务。
    chat_task_consumer.start()


@app.on_event("shutdown")
def on_shutdown() -> None:
    # 优雅停止消费者线程和连接。
    chat_task_consumer.stop()


@app.get("/health")
def health() -> dict[str, str]:
    """健康检查接口。"""
    return {"status": "ok"}


@app.get("/metrics")
def metrics() -> dict:
    """JSON 格式监控快照。"""
    return metrics_registry.snapshot()


@app.get("/metrics/prometheus", response_class=PlainTextResponse)
def metrics_prometheus() -> str:
    """Prometheus 文本格式监控指标。"""
    return metrics_registry.to_prometheus_text()
