import logging
import time
import uuid

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

from app.core.settings import settings
from app.observability.metrics.registry import metrics_registry
from app.observability.tracing.request_context import set_request_id


logger = logging.getLogger(__name__)


class RequestTracingMiddleware(BaseHTTPMiddleware):
    """请求追踪中间件。

    负责生成/透传 X-Request-Id、记录请求日志与指标、输出慢请求告警。
    """

    async def dispatch(self, request: Request, call_next):
        # 优先复用上游 request id，缺失则本地生成。
        request_id = request.headers.get("X-Request-Id") or request.headers.get("x-request-id")
        if not request_id:
            request_id = str(uuid.uuid4())

        set_request_id(request_id)
        started_at = time.perf_counter()

        logger.info("request_started method=%s path=%s", request.method, request.url.path)
        response = None
        status_code = 500
        try:
            response = await call_next(request)
            status_code = response.status_code
            return response
        finally:
            latency_ms = (time.perf_counter() - started_at) * 1000
            metrics_registry.record_request(
                path=request.url.path,
                latency_ms=latency_ms,
                is_error=status_code >= 500,
            )
            logger.info(
                "request_finished method=%s path=%s status=%s latency_ms=%.2f",
                request.method,
                request.url.path,
                status_code,
                latency_ms,
            )
            if latency_ms >= settings.slow_request_threshold_ms:
                logger.warning(
                    "slow_request_detected method=%s path=%s status=%s latency_ms=%.2f threshold_ms=%s",
                    request.method,
                    request.url.path,
                    status_code,
                    latency_ms,
                    settings.slow_request_threshold_ms,
                )
            if response is not None:
                # 将 request_id 透传回调用方，便于链路排障。
                response.headers["X-Request-Id"] = request_id
