#!/usr/bin/env python3
"""
专门检查medical_record表
"""

import mysql.connector
from mysql.connector import Error


def check_medical_record():
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

            print("medical_record 表的详细结构:")
            print("=" * 80)
            cursor.execute("SHOW CREATE TABLE medical_record")
            result = cursor.fetchone()
            print(result[1])

    except Error as e:
        print(f"数据库错误: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()
            print("\n数据库连接已关闭")


if __name__ == "__main__":
    check_medical_record()
