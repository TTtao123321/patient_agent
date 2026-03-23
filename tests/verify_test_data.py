#!/usr/bin/env python3
"""
验证测试数据
"""

import mysql.connector
from mysql.connector import Error


def verify_test_data():
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

            # 查询测试用户
            print("测试用户信息:")
            print("=" * 80)
            cursor.execute("SELECT id, username, real_name, phone FROM patient_user WHERE username = 'test'")
            user = cursor.fetchone()
            if user:
                print(f"ID: {user[0]}, 用户名: {user[1]}, 姓名: {user[2]}, 手机: {user[3]}")
                user_id = user[0]
            else:
                print("未找到测试用户")
                return

            print("\n\n病历记录:")
            print("=" * 80)
            cursor.execute("SELECT id, record_no, patient_name, chief_complaint, diagnosis_summary, attending_doctor, record_date FROM medical_record WHERE user_id = %s ORDER BY record_date DESC", (user_id,))
            records = cursor.fetchall()
            print(f"共找到 {len(records)} 条病历记录:")
            for record in records:
                print(f"\nID: {record[0]}")
                print(f"病历编号: {record[1]}")
                print(f"患者姓名: {record[2]}")
                print(f"主诉: {record[3]}")
                print(f"诊断摘要: {record[4]}")
                print(f"接诊医生: {record[5]}")
                print(f"病历时间: {record[6]}")

            print("\n\n聊天会话:")
            print("=" * 80)
            cursor.execute("SELECT id, title, scene_type, started_at FROM chat_session WHERE user_id = %s ORDER BY started_at DESC", (user_id,))
            sessions = cursor.fetchall()
            print(f"共找到 {len(sessions)} 个聊天会话:")
            for session in sessions:
                print(f"\n会话ID: {session[0]}, 标题: {session[1]}, 类型: {session[2]}, 开始时间: {session[3]}")
                
                cursor.execute("SELECT sender_type, content, sent_at FROM chat_message WHERE session_id = %s ORDER BY sequence_no", (session[0],))
                messages = cursor.fetchall()
                print(f"  消息数: {len(messages)}")
                for msg in messages:
                    print(f"    [{msg[0]}] {msg[1]}")

    except Error as e:
        print(f"数据库错误: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()
            print("\n数据库连接已关闭")


if __name__ == "__main__":
    verify_test_data()
