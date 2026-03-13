import json
import threading
import time
import uuid

import pika

from app.core.settings import settings
from app.integrations.mysql.mysql_client import get_mysql_connection
from app.services.chat.chat_processor import ChatProcessor


class ChatTaskConsumer:
    """RabbitMQ 聊天任务消费者。

    消费 `chat.task.queue` 中的异步任务，调用 ChatProcessor 生成回答，
    再将 Agent 消息写回 MySQL 聊天表。
    """

    def __init__(self) -> None:
        self._stop_event = threading.Event()
        self._thread: threading.Thread | None = None
        self._connection: pika.BlockingConnection | None = None
        self._channel: pika.adapters.blocking_connection.BlockingChannel | None = None
        self.processor = ChatProcessor()

    def start(self) -> None:
        """启动消费者线程（幂等）。"""
        if not settings.rabbitmq_consumer_enabled:
            return
        if self._thread and self._thread.is_alive():
            return

        self._thread = threading.Thread(target=self._consume_loop, name="chat-task-consumer", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        """停止消费并关闭连接。"""
        self._stop_event.set()
        try:
            if self._channel and self._channel.is_open:
                self._channel.stop_consuming()
        except Exception:
            pass
        try:
            if self._connection and self._connection.is_open:
                self._connection.close()
        except Exception:
            pass
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=5)

    def _consume_loop(self) -> None:
        """消费主循环：异常自动重连，避免线程退出。"""
        while not self._stop_event.is_set():
            try:
                self._open_connection()
                self._channel.basic_qos(prefetch_count=1)
                self._channel.basic_consume(
                    queue=settings.rabbitmq_chat_task_queue,
                    on_message_callback=self._handle_message,
                )
                self._channel.start_consuming()
            except Exception as exc:
                print(f"RabbitMQ consumer stopped unexpectedly: {exc}")
                self._close_connection()
                if not self._stop_event.is_set():
                    time.sleep(3)

    def _open_connection(self) -> None:
        """建立 RabbitMQ 连接并声明任务队列。"""
        credentials = pika.PlainCredentials(settings.rabbitmq_username, settings.rabbitmq_password)
        parameters = pika.ConnectionParameters(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            virtual_host=settings.rabbitmq_virtual_host,
            credentials=credentials,
            heartbeat=30,
        )
        self._connection = pika.BlockingConnection(parameters)
        self._channel = self._connection.channel()
        self._channel.queue_declare(queue=settings.rabbitmq_chat_task_queue, durable=True)

    def _close_connection(self) -> None:
        """关闭 channel/connection 并清空本地引用。"""
        try:
            if self._channel and self._channel.is_open:
                self._channel.close()
        except Exception:
            pass
        try:
            if self._connection and self._connection.is_open:
                self._connection.close()
        except Exception:
            pass
        self._channel = None
        self._connection = None

    def _handle_message(self, channel, method, properties, body: bytes) -> None:
        """处理单条任务消息。

        业务失败时会写入一条系统错误消息到会话中，避免前端长时间无反馈。
        """
        try:
            payload = json.loads(body.decode("utf-8"))
            session_id = payload["session_id"]
            user_id = int(payload["user_id"])
            message = payload["message"]

            answer, intent, agent_used, _ = self.processor.process(
                session_id=session_id,
                user_id=user_id,
                message=message,
            )
            self._save_agent_message(
                session_id=session_id,
                user_id=user_id,
                content=answer,
                agent_used=agent_used,
            )
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as exc:
            print(f"Failed to process chat task: {exc}")
            try:
                payload = json.loads(body.decode("utf-8"))
                self._save_error_message(
                    session_id=payload.get("session_id", ""),
                    user_id=int(payload.get("user_id", 0)),
                    error_message="AI 任务处理失败，请稍后重试。",
                )
            except Exception:
                pass
            channel.basic_ack(delivery_tag=method.delivery_tag)

    def _save_agent_message(self, session_id: str, user_id: int, content: str, agent_used: str) -> None:
        """保存 Agent 正常回复。"""
        self._save_chat_message(
            session_id=session_id,
            user_id=user_id,
            content=content,
            agent_type=agent_used,
            session_status="ACTIVE",
        )

    def _save_error_message(self, session_id: str, user_id: int, error_message: str) -> None:
        """保存系统错误提示消息。"""
        if not session_id or user_id <= 0:
            return
        self._save_chat_message(
            session_id=session_id,
            user_id=user_id,
            content=error_message,
            agent_type="system",
            session_status="FAILED",
        )

    def _save_chat_message(self, session_id: str, user_id: int, content: str, agent_type: str, session_status: str) -> None:
        """写入 chat_message，并同步更新 chat_session 状态。"""
        conn = get_mysql_connection()
        try:
            cursor = conn.cursor(dictionary=True)
            cursor.execute(
                "SELECT id, user_id FROM chat_session WHERE session_no = %s AND is_deleted = 0 LIMIT 1",
                (session_id,),
            )
            session_row = cursor.fetchone()
            if not session_row:
                return

            chat_session_id = session_row["id"]
            message_user_id = session_row["user_id"] if session_row.get("user_id") is not None else user_id

            cursor.execute(
                "SELECT COALESCE(MAX(sequence_no), 0) AS max_sequence_no FROM chat_message WHERE session_id = %s AND is_deleted = 0",
                (chat_session_id,),
            )
            sequence_row = cursor.fetchone()
            next_sequence_no = int(sequence_row["max_sequence_no"]) + 1

            insert_sql = """
            INSERT INTO chat_message (
                message_no,
                session_id,
                user_id,
                sender_type,
                agent_type,
                message_type,
                sequence_no,
                content,
                sent_at,
                created_at,
                updated_at,
                is_deleted
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW(), NOW(), 0)
            """
            cursor.execute(
                insert_sql,
                (
                    self._generate_message_no(),
                    chat_session_id,
                    message_user_id,
                    "AGENT",
                    agent_type,
                    "TEXT",
                    next_sequence_no,
                    content,
                ),
            )
            cursor.execute(
                """
                UPDATE chat_session
                SET current_agent = %s, session_status = %s, last_message_at = NOW(), updated_at = NOW()
                WHERE id = %s
                """,
                (agent_type, session_status, chat_session_id),
            )
            cursor.close()
        finally:
            conn.close()

    def _generate_message_no(self) -> str:
        """生成消息编号（M + 31位十六进制）。"""
        return "M" + uuid.uuid4().hex[:31]


chat_task_consumer = ChatTaskConsumer()
