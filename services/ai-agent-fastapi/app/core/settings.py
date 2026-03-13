import os


class Settings:
    log_level: str = os.getenv("LOG_LEVEL", "INFO")
    slow_request_threshold_ms: int = int(os.getenv("SLOW_REQUEST_THRESHOLD_MS", "1200"))
    slow_agent_call_threshold_ms: int = int(os.getenv("SLOW_AGENT_CALL_THRESHOLD_MS", "1500"))
    slow_tool_call_threshold_ms: int = int(os.getenv("SLOW_TOOL_CALL_THRESHOLD_MS", "1000"))

    rag_collection_name: str = os.getenv("RAG_COLLECTION_NAME", "medical_knowledge")
    milvus_host: str = os.getenv("MILVUS_HOST", "localhost")
    milvus_port: str = os.getenv("MILVUS_PORT", "19530")
    bge_m3_model_name: str = os.getenv("BGE_M3_MODEL_NAME", "BAAI/bge-m3")
    chunk_size: int = int(os.getenv("RAG_CHUNK_SIZE", "800"))
    chunk_overlap: int = int(os.getenv("RAG_CHUNK_OVERLAP", "120"))

    redis_host: str = os.getenv("REDIS_HOST", "101.126.81.197")
    redis_port: int = int(os.getenv("REDIS_PORT", "6389"))
    redis_password: str | None = os.getenv("REDIS_PASSWORD", "123456")
    redis_db: int = int(os.getenv("REDIS_DB", "0"))
    redis_connect_timeout_seconds: int = int(os.getenv("REDIS_CONNECT_TIMEOUT_SECONDS", "2"))
    redis_read_timeout_seconds: int = int(os.getenv("REDIS_READ_TIMEOUT_SECONDS", "2"))
    redis_chat_key_prefix: str = os.getenv("REDIS_CHAT_KEY_PREFIX", "patient_agent:chat")
    redis_chat_ttl_seconds: int = int(os.getenv("REDIS_CHAT_TTL_SECONDS", "86400"))

    mysql_host: str = os.getenv("MYSQL_HOST", "101.126.81.197")
    mysql_port: int = int(os.getenv("MYSQL_PORT", "3307"))
    mysql_user: str = os.getenv("MYSQL_USER", "root")
    mysql_password: str = os.getenv("MYSQL_PASSWORD", "123456")
    mysql_database: str = os.getenv("MYSQL_DATABASE", "patient_agent")
    mysql_pool_size: int = int(os.getenv("MYSQL_POOL_SIZE", "5"))

    rabbitmq_host: str = os.getenv("RABBITMQ_HOST", "101.126.81.197")
    rabbitmq_port: int = int(os.getenv("RABBITMQ_PORT", "5672"))
    rabbitmq_username: str = os.getenv("RABBITMQ_USERNAME", "admin")
    rabbitmq_password: str = os.getenv("RABBITMQ_PASSWORD", "admin")
    rabbitmq_virtual_host: str = os.getenv("RABBITMQ_VHOST", "/")
    rabbitmq_chat_task_queue: str = os.getenv("RABBITMQ_CHAT_TASK_QUEUE", "chat.task.queue")
    rabbitmq_consumer_enabled: bool = os.getenv("RABBITMQ_CONSUMER_ENABLED", "true").lower() == "true"

    short_term_message_limit: int = int(os.getenv("SHORT_TERM_MESSAGE_LIMIT", "40"))
    context_message_limit: int = int(os.getenv("CONTEXT_MESSAGE_LIMIT", "12"))

    ollama_base_url: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    ollama_model: str = os.getenv("OLLAMA_MODEL", "qwen")

    @property
    def milvus_uri(self) -> str:
        return f"http://{self.milvus_host}:{self.milvus_port}"


settings = Settings()
