#!/usr/bin/env python3
"""
检查表结构
"""

import mysql.connector
from mysql.connector import Error


def check_table_structure():
    connection = None
    try:
        connection = mysql.connector.connect(
            host='101.126.81.197',
            port=3307,
            database='patient_agent',
            user='root',
            password='123456'
        )

        if connection.is_connected():
            cursor = connection.cursor()
            print("成功连接到数据库\n")

            tables = ['patient_user', 'medical_record', 'chat_session', 'chat_message']
            for table in tables:
                print(f"表 {table} 的结构:")
                print("=" * 80)
                cursor.execute(f"DESCRIBE {table}")
                columns = cursor.fetchall()
                for col in columns:
                    print(f"{col[0]:<30} {col[1]:<20}")
                print("\n")

    except Error as e:
        print(f"数据库错误: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()
            print("数据库连接已关闭")


if __name__ == "__main__":
    check_table_structure()
