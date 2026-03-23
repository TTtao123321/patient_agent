#!/usr/bin/env python3
"""
为测试用户添加测试问诊记录的脚本
"""

import mysql.connector
from mysql.connector import Error
from datetime import datetime, timedelta
import uuid


def generate_record_no():
    """生成病历编号"""
    return 'MR' + uuid.uuid4().hex[:30].upper()


def generate_session_no():
    """生成会话编号"""
    return 'S' + uuid.uuid4().hex[:31].upper()


def generate_message_no():
    """生成消息编号"""
    return 'M' + uuid.uuid4().hex[:31].upper()


def add_test_medical_records():
    """添加测试问诊记录"""
    connection = None
    try:
        # 连接数据库
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

            # 1. 获取测试用户的ID
            cursor.execute("SELECT id, real_name FROM patient_user WHERE username = 'test'")
            user = cursor.fetchone()
            if not user:
                print("错误：找不到用户名为 'test' 的用户")
                return

            user_id, real_name = user
            print(f"找到测试用户: ID={user_id}, 姓名={real_name}")

            # 2. 先删除该用户已有的旧测试数据（如果有的话）
            print("清理该用户的旧数据...")
            cursor.execute("DELETE FROM chat_message WHERE user_id = %s", (user_id,))
            cursor.execute("DELETE FROM chat_session WHERE user_id = %s", (user_id,))
            cursor.execute("DELETE FROM medical_report WHERE user_id = %s", (user_id,))
            cursor.execute("DELETE FROM medical_record WHERE user_id = %s", (user_id,))
            print(f"已删除 {cursor.rowcount} 条旧记录")

            # 3. 添加3条测试病历记录
            test_records = [
                {
                    'record_no': generate_record_no(),
                    'patient_name': real_name,
                    'age': 35,
                    'chief_complaint': '发热、咳嗽3天',
                    'present_illness': '患者3天前受凉后出现发热，体温最高38.5℃，伴咳嗽、咳少量白色黏痰，无胸闷、胸痛，无恶心、呕吐，自服感冒药症状无明显缓解。',
                    'past_history': '既往体健，否认高血压、糖尿病病史，否认肝炎、结核等传染病史。',
                    'allergy_history': '否认药物、食物过敏史。',
                    'family_history': '父母体健，否认家族性遗传病史。',
                    'surgery_history': '否认手术、外伤史。',
                    'medication_history': '近期自服复方氨酚烷胺片，每日2次，每次1片。',
                    'diagnosis_summary': '初步诊断：上呼吸道感染。建议：多饮水，注意休息，继续观察体温变化，如症状加重及时就医。',
                    'attending_doctor': '张医生',
                    'record_date': datetime.now() - timedelta(days=30)
                },
                {
                    'record_no': generate_record_no(),
                    'patient_name': real_name,
                    'age': 35,
                    'chief_complaint': '腹痛、腹泻1天',
                    'present_illness': '患者1天前进食不洁食物后出现脐周阵发性腹痛，伴腹泻，每日5-6次，为黄色稀水样便，无脓血便，无恶心、呕吐，无发热。',
                    'past_history': '既往体健，去年曾有急性胃肠炎病史，经治疗后好转。',
                    'allergy_history': '对青霉素过敏。',
                    'family_history': '父亲有高血压病史，母亲体健。',
                    'surgery_history': '否认手术史。',
                    'medication_history': '昨日自服黄连素片，每日3次，每次2片。',
                    'diagnosis_summary': '初步诊断：急性胃肠炎。建议：清淡饮食，多喝淡盐水，如症状持续或加重请就医。',
                    'attending_doctor': '李医生',
                    'record_date': datetime.now() - timedelta(days=15)
                },
                {
                    'record_no': generate_record_no(),
                    'patient_name': real_name,
                    'age': 35,
                    'chief_complaint': '头痛、头晕2天',
                    'present_illness': '患者2天前熬夜后出现双侧颞部搏动性头痛，伴轻度头晕，无恶心、呕吐，无视物模糊，无肢体活动障碍。',
                    'past_history': '既往有偏头痛病史5年，每年发作3-4次，休息后可缓解。',
                    'allergy_history': '否认过敏史。',
                    'family_history': '母亲有偏头痛病史。',
                    'surgery_history': '否认手术史。',
                    'medication_history': '昨日自服布洛芬缓释胶囊1粒，头痛有所缓解。',
                    'diagnosis_summary': '初步诊断：偏头痛发作。建议：保证充足睡眠，避免劳累和精神紧张，必要时就医。',
                    'attending_doctor': '王医生',
                    'record_date': datetime.now() - timedelta(days=5)
                }
            ]

            medical_record_ids = []
            for record in test_records:
                insert_sql = """
                INSERT INTO medical_record 
                (record_no, user_id, patient_name, age, chief_complaint, present_illness, 
                 past_history, allergy_history, family_history, surgery_history, 
                 medication_history, diagnosis_summary, attending_doctor, record_date)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """
                cursor.execute(insert_sql, (
                    record['record_no'], user_id, record['patient_name'], record['age'],
                    record['chief_complaint'], record['present_illness'], record['past_history'],
                    record['allergy_history'], record['family_history'], record['surgery_history'],
                    record['medication_history'], record['diagnosis_summary'], record['attending_doctor'],
                    record['record_date']
                ))
                medical_record_ids.append(cursor.lastrowid)
                print(f"已添加病历记录: {record['record_no']} - {record['chief_complaint']}")

            # 4. 为每个病历添加对应的聊天会话和消息
            session_data = [
                {
                    'title': '发热咳嗽咨询',
                    'scene_type': 'symptom',
                    'record_id': medical_record_ids[0],
                    'messages': [
                        ('USER', '医生，我发烧咳嗽3天了，怎么办？'),
                        ('AGENT', '您好！请问您体温最高多少度？有没有咳痰？'),
                        ('USER', '最高38.5度，有少量白色黏痰'),
                        ('AGENT', '根据您的症状，初步考虑是上呼吸道感染。建议您多饮水，注意休息。如果症状持续或加重，请及时就医。')
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
                        ('AGENT', '考虑是急性胃肠炎。建议您清淡饮食，多喝淡盐水补充水分。可以服用一些益生菌调理肠道。')
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
                        ('AGENT', '那应该是偏头痛发作了。建议您好好休息，保证睡眠。如果头痛严重可以服用止痛药，但不要频繁使用。')
                    ]
                }
            ]

            for session in session_data:
                # 插入会话
                session_no = generate_session_no()
                insert_session_sql = """
                INSERT INTO chat_session 
                (session_no, user_id, medical_record_id, title, scene_type, session_status)
                VALUES (%s, %s, %s, %s, %s, 'ACTIVE')
                """
                cursor.execute(insert_session_sql, (
                    session_no, user_id, session['record_id'], 
                    session['title'], session['scene_type']
                ))
                session_id = cursor.lastrowid
                print(f"已添加聊天会话: {session['title']}")

                # 插入消息
                for seq, (sender_type, content) in enumerate(session['messages'], 1):
                    message_no = generate_message_no()
                    agent_type = 'symptom' if sender_type == 'AGENT' else None
                    insert_message_sql = """
                    INSERT INTO chat_message 
                    (message_no, session_id, user_id, sender_type, agent_type, message_type, sequence_no, content)
                    VALUES (%s, %s, %s, %s, %s, 'TEXT', %s, %s)
                    """
                    cursor.execute(insert_message_sql, (
                        message_no, session_id, user_id, sender_type, agent_type, seq, content
                    ))
                print(f"  已添加 {len(session['messages'])} 条消息")

            # 提交事务
            connection.commit()
            print("\n所有测试数据添加成功！")

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
    add_test_medical_records()
