from contextvars import ContextVar


_request_id_ctx: ContextVar[str] = ContextVar("request_id", default="-")


def set_request_id(request_id: str) -> None:
    """将 request_id 写入当前协程上下文。"""
    _request_id_ctx.set(request_id)


def get_request_id() -> str:
    """读取当前协程上下文中的 request_id。"""
    return _request_id_ctx.get()
