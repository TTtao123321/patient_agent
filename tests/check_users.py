#!/usr/bin/env python3
"""
查看数据库中的用户
"""

import mysql.connector
from mysql.connector import Error


def check_users():
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

            cursor.execute("SELECT id, user_no, username, real_name, phone FROM patient_user")
            users = cursor.fetchall()

            if users:
                print("数据库中的用户:")
                print("=" * 80)
                for user in users:
                    print(f"ID: {user[0]}, 用户编号: {user[1]}, 用户名: {user[2]}, 姓名: {user[3]}, 手机: {user[4]}")
            else:
                print("数据库中没有用户")

    except Error as e:
        print(f"数据库错误: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()
            print("\n数据库连接已关闭")


if __name__ == "__main__":
    check_users()
