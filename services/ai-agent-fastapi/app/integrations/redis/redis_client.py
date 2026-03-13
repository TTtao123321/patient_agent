import redis

from app.core.settings import settings


_redis_client: redis.Redis | None = None


def get_redis_client() -> redis.Redis:
    """获取（或初始化）Redis 客户端单例。"""
    global _redis_client
    if _redis_client is None:
        _redis_client = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password,
            db=settings.redis_db,
            decode_responses=True,
            socket_connect_timeout=settings.redis_connect_timeout_seconds,
            socket_timeout=settings.redis_read_timeout_seconds,
        )
    return _redis_client
