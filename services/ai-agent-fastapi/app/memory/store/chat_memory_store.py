import json
from datetime import datetime
from typing import Any

from app.core.settings import settings
from app.integrations.mysql.mysql_client import get_mysql_connection
from app.integrations.redis.redis_client import get_redis_client


class ChatMemoryStore:
    def __init__(self) -> None:
        self.redis_prefix = settings.redis_chat_key_prefix
        self.redis_ttl = settings.redis_chat_ttl_seconds
        self.short_term_limit = settings.short_term_message_limit
        self._ensure_mysql_table()

    def _redis_key(self, session_id: str) -> str:
        return f"{self.redis_prefix}:{session_id}:messages"

    def _ensure_mysql_table(self) -> None:
        create_sql = """
        CREATE TABLE IF NOT EXISTS agent_chat_history (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            session_id VARCHAR(64) NOT NULL,
            user_id BIGINT NOT NULL,
            role VARCHAR(16) NOT NULL,
            content TEXT NOT NULL,
            intent VARCHAR(64) NULL,
            agent_used VARCHAR(64) NULL,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            KEY idx_agent_chat_history_session_created (session_id, created_at),
            KEY idx_agent_chat_history_user_created (user_id, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
        """
        try:
            conn = get_mysql_connection()
            cursor = conn.cursor()
            cursor.execute(create_sql)
            cursor.close()
            conn.close()
        except Exception:
            # MySQL is optional at runtime; failures should not block chatting.
            pass

    def append_short_term_message(self, session_id: str, user_id: int, role: str, content: str) -> None:
        payload = {
            "session_id": session_id,
            "user_id": user_id,
            "role": role,
            "content": content,
            "created_at": datetime.utcnow().isoformat(),
        }
        key = self._redis_key(session_id)
        try:
            redis_client = get_redis_client()
            redis_client.rpush(key, json.dumps(payload, ensure_ascii=False))
            redis_client.ltrim(key, -self.short_term_limit, -1)
            redis_client.expire(key, self.redis_ttl)
        except Exception:
            pass

    def get_short_term_messages(self, session_id: str, limit: int) -> list[dict[str, Any]]:
        key = self._redis_key(session_id)
        try:
            redis_client = get_redis_client()
            raw_messages = redis_client.lrange(key, -limit, -1)
            return [json.loads(item) for item in raw_messages]
        except Exception:
            return []

    def append_history_message(
        self,
        session_id: str,
        user_id: int,
        role: str,
        content: str,
        intent: str | None = None,
        agent_used: str | None = None,
    ) -> None:
        insert_sql = """
        INSERT INTO agent_chat_history (session_id, user_id, role, content, intent, agent_used)
        VALUES (%s, %s, %s, %s, %s, %s)
        """
        try:
            conn = get_mysql_connection()
            cursor = conn.cursor()
            cursor.execute(insert_sql, (session_id, user_id, role, content, intent, agent_used))
            cursor.close()
            conn.close()
        except Exception:
            pass

    def get_history_messages(self, session_id: str, limit: int) -> list[dict[str, Any]]:
        query_sql = """
        SELECT session_id, user_id, role, content, intent, agent_used, created_at
        FROM agent_chat_history
        WHERE session_id = %s
        ORDER BY created_at DESC, id DESC
        LIMIT %s
        """
        try:
            conn = get_mysql_connection()
            cursor = conn.cursor(dictionary=True)
            cursor.execute(query_sql, (session_id, limit))
            rows = cursor.fetchall()
            cursor.close()
            conn.close()
            rows.reverse()
            return rows
        except Exception:
            return []
