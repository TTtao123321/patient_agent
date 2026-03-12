import mysql.connector
from mysql.connector.pooling import MySQLConnectionPool

from app.core.settings import settings


_mysql_pool: MySQLConnectionPool | None = None


def get_mysql_pool() -> MySQLConnectionPool:
    global _mysql_pool
    if _mysql_pool is None:
        _mysql_pool = MySQLConnectionPool(
            pool_name="agent_memory_pool",
            pool_size=settings.mysql_pool_size,
            host=settings.mysql_host,
            port=settings.mysql_port,
            user=settings.mysql_user,
            password=settings.mysql_password,
            database=settings.mysql_database,
            charset="utf8mb4",
            collation="utf8mb4_general_ci",
            autocommit=True,
        )
    return _mysql_pool


def get_mysql_connection() -> mysql.connector.MySQLConnection:
    pool = get_mysql_pool()
    return pool.get_connection()
