import logging

from app.core.settings import settings
from app.observability.tracing.request_context import get_request_id


class RequestIdFilter(logging.Filter):
    """日志过滤器：把 request_id 注入到每条日志记录。"""

    def filter(self, record: logging.LogRecord) -> bool:
        record.request_id = get_request_id()
        return True


def configure_logging() -> None:
    """配置全局日志格式与 request_id 过滤器。"""
    root_logger = logging.getLogger()
    if root_logger.handlers:
        # 若已有 handler，仅补充过滤器，避免重复添加 handler。
        for handler in root_logger.handlers:
            handler.addFilter(RequestIdFilter())
        return

    formatter = logging.Formatter(
        "%(asctime)s | %(levelname)s | request_id=%(request_id)s | %(name)s | %(message)s"
    )
    handler = logging.StreamHandler()
    handler.setFormatter(formatter)
    handler.addFilter(RequestIdFilter())

    root_logger.setLevel(getattr(logging, settings.log_level.upper(), logging.INFO))
    root_logger.addHandler(handler)
