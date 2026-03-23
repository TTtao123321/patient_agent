#!/usr/bin/env python3
"""
创建测试用户并添加测试问诊记录的脚本
"""

import mysql.connector
from mysql.connector import Error
from datetime import datetime, timedelta
import uuid
import hashlib


def generate_user_no():
    """生成用户编号"""
    return 'U' + uuid.uuid4().hex[:31].upper()


def generate_record_no():
    """生成病历编号"""
    return 'MR' + uuid.uuid4().hex[:30].upper()


def generate_session_no():
    """生成会话编号"""
    return 'S' + uuid.uuid4().hex[:31].upper()


def generate_message_no():
    """生成消息编号"""
    return 'M' + uuid.uuid4().hex[:31].upper()


def hash_password(password):
    """简单的密码哈希（与Java后端保持一致）"""
    return hashlib.sha256(password.encode()).hexdigest()


def add_test_user_and_records():
    """添加测试用户和问诊记录"""
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
            print("成功连接到数据库")

            now = datetime.now()
            
            # 1. 创建测试用户
            user_no = generate_user_no()
            username = 'test'
            password_hash = hash_password('123456')
            real_name = '测试用户'
            phone = '13800138000'

            insert_user_sql = """
            INSERT INTO patient_user 
            (user_no, username, password_hash, real_name, gender, phone, status, is_deleted, created_at, updated_at)
            VALUES (%s, %s, %s, %s, 0, %s, 1, 0, %s, %s)
            """
            cursor.execute(insert_user_sql, (user_no, username, password_hash, real_name, phone, now, now))
            user_id = cursor.lastrowid
            print(f"已创建测试用户: ID={user_id}, 用户名={username}, 姓名={real_name}")

            # 2. 添加3条测试病历记录
            test_records = [
                {
                    'record_no': generate_record_no(),
                    'patient_name': real_name,
                    'age': 35,
                    'chief_complaint': '发热、咳嗽3天',
                    'diagnosis_summary': '初步诊断：上呼吸道感染。建议：多饮水，注意休息，继续观察体温变化，如症状加重及时就医。',
                    'attending_doctor': '张医生',
                    'record_date': datetime.now() - timedelta(days=30)
                },
                {
                    'record_no': generate_record_no(),
                    'patient_name': real_name,
                    'age': 35,
                    'chief_complaint': '腹痛、腹泻1天',
                    'diagnosis_summary': '初步诊断：急性胃肠炎。建议：清淡饮食，多喝淡盐水，如症状持续或加重请就医。',
                    'attending_doctor': '李医生',
                    'record_date': datetime.now() - timedelta(days=15)
                },
                {
                    'record_no': generate_record_no(),
                    'patient_name': real_name,
                    'age': 35,
                    'chief_complaint': '头痛、头晕2天',
                    'diagnosis_summary': '初步诊断：偏头痛发作。建议：保证充足睡眠，避免劳累和精神紧张，必要时就医。',
                    'attending_doctor': '王医生',
                    'record_date': datetime.now() - timedelta(days=5)
                }
            ]

            medical_record_ids = []
            for record in test_records:
                insert_sql = """
                INSERT INTO medical_record 
                (record_no, user_id, patient_name, age, chief_complaint, diagnosis_summary, 
                 attending_doctor, record_date, is_deleted)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 0)
                """
                cursor.execute(insert_sql, (
                    record['record_no'], user_id, record['patient_name'], record['age'],
                    record['chief_complaint'], record['diagnosis_summary'], record['attending_doctor'],
                    record['record_date']
                ))
                medical_record_ids.append(cursor.lastrowid)
                print(f"已添加病历记录: {record['record_no']} - {record['chief_complaint']}")

            # 3. 为每个病历添加对应的聊天会话和消息
            session_data = [
                {
                    'title': '发热咳嗽咨询',
                    'scene_type': 'symptom',
                    'record_id': medical_record_ids[0],
                    'messages': [
                        ('USER', '医生，我发烧咳嗽3天了，怎么办？'),
                        ('AGENT', '您好！请问您体温最高多少度？有没有咳痰？'),
                        ('USER', '最高38.5度，有少量白色黏痰'),
                        ('AGENT', '根据您的症状，初步考虑是上呼吸道感染。建议您多饮水，注意休息。')
                    ]
                },
                {
                    'title': '腹痛腹泻咨询',
                    'scene_type': 'symptom',
                    'record_id': medical_record_ids[1],
                    'messages': [
                        ('USER', '医生，我昨天吃坏东西了，现在肚子疼还拉肚子'),
                        ('AGENT', '您好！请问您腹泻几次了？有没有呕吐或发热？'),
                        ('USER', '大概5-6次，没有呕吐，也不发烧'),
                        ('AGENT', '考虑是急性胃肠炎。建议您清淡饮食，多喝淡盐水补充水分。')
                    ]
                },
                {
                    'title': '头痛咨询',
                    'scene_type': 'symptom',
                    'record_id': medical_record_ids[2],
                    'messages': [
                        ('USER', '医生，我这两天头痛得厉害，特别是太阳穴的位置'),
                        ('AGENT', '您好！请问您以前有过类似的头痛吗？最近有没有熬夜或压力大？'),
                        ('USER', '我有偏头痛很多年了，这两天确实熬夜了'),
                        ('AGENT', '那应该是偏头痛发作了。建议您好好休息，保证睡眠。')
                    ]
                }
            ]

            for session in session_data:
                # 插入会话
                session_no = generate_session_no()
                insert_session_sql = """
                INSERT INTO chat_session 
                (session_no, user_id, medical_record_id, title, scene_type, session_status, is_deleted, 
                 started_at, last_message_at, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, 'ACTIVE', 0, %s, %s, %s, %s)
                """
                cursor.execute(insert_session_sql, (
                    session_no, user_id, session['record_id'], 
                    session['title'], session['scene_type'],
                    now, now, now, now
                ))
                session_id = cursor.lastrowid
                print(f"已添加聊天会话: {session['title']}")

                # 插入消息
                for seq, (sender_type, content) in enumerate(session['messages'], 1):
                    message_no = generate_message_no()
                    agent_type = 'symptom' if sender_type == 'AGENT' else None
                    message_time = now + timedelta(minutes=seq)
                    insert_message_sql = """
                    INSERT INTO chat_message 
                    (message_no, session_id, user_id, sender_type, agent_type, message_type, sequence_no, content, is_deleted,
                     sent_at, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, 'TEXT', %s, %s, 0, %s, %s, %s)
                    """
                    cursor.execute(insert_message_sql, (
                        message_no, session_id, user_id, sender_type, agent_type, seq, content,
                        message_time, message_time, message_time
                    ))
                print(f"  已添加 {len(session['messages'])} 条消息")

            # 提交事务
            connection.commit()
            print("\n所有测试数据添加成功！")
            print(f"测试用户登录信息: 用户名=test, 密码=123456")

    except Error as e:
        print(f"数据库错误: {e}")
        if connection:
            connection.rollback()
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()
            print("数据库连接已关闭")


if __name__ == "__main__":
    add_test_user_and_records()
