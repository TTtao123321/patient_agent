#!/usr/bin/env python3
"""给测试用户添加测试病历记录。
"""

import pymysql
from pymysql.cursors import DictCursor
from datetime import datetime, timedelta
import uuid

# 数据库配置 - 使用 root 用户
DB_CONFIG = {
    'host': '101.126.81.197',
    'port': 3307,
    'user': 'root',
    'password': '123456',
    'database': 'patient_agent',
    'cursorclass': DictCursor,
    'charset': 'utf8mb4'
}

def generate_record_no():
    """生成病历编号。"""
    now = datetime.now()
    return f"MR{now.strftime('%Y%m%d')}{uuid.uuid4().hex[:6].upper()}"

def add_test_records():
    """添加测试病历记录。"""
    conn = None
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        # 先查询一下表结构，确保字段正确
        cursor.execute("DESCRIBE medical_record")
        print("表结构:")
        for col in cursor.fetchall():
            print(f"  {col['Field']} - {col['Type']}")
        print()
        
        # 测试数据 - 使用实际表中的字段
        test_records = [
            {
                'record_no': generate_record_no(),
                'user_id': 1,
                'patient_name': '测试用户',
                'age': 35,
                'chief_complaint': '发热、咳嗽3天',
                'diagnosis_summary': '考虑为急性上呼吸道感染',
                'attending_doctor': '王医生',
                'record_date': datetime.now() - timedelta(days=10),
                'is_deleted': 0
            },
            {
                'record_no': generate_record_no(),
                'user_id': 1,
                'patient_name': '测试用户',
                'age': 35,
                'chief_complaint': '头痛、头晕1周',
                'diagnosis_summary': '考虑为紧张性头痛，血压偏高',
                'attending_doctor': '李医生',
                'record_date': datetime.now() - timedelta(days=30),
                'is_deleted': 0
            },
            {
                'record_no': generate_record_no(),
                'user_id': 1,
                'patient_name': '测试用户',
                'age': 34,
                'chief_complaint': '体检发现血脂异常',
                'diagnosis_summary': '血脂异常（高胆固醇血症）',
                'attending_doctor': '张医生',
                'record_date': datetime.now() - timedelta(days=90),
                'is_deleted': 0
            }
        ]
        
        # 插入数据 - 使用实际字段
        sql = """
        INSERT INTO medical_record (
            record_no, user_id, patient_name, age, chief_complaint,
            diagnosis_summary, attending_doctor, record_date, is_deleted
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, 0
        )
        """
        
        for record in test_records:
            cursor.execute(sql, (
                record['record_no'],
                record['user_id'],
                record['patient_name'],
                record['age'],
                record['chief_complaint'],
                record['diagnosis_summary'],
                record['attending_doctor'],
                record['record_date']
            ))
        
        conn.commit()
        print(f"✅ 成功添加 {len(test_records)} 条测试病历记录！")
        print("\n添加的记录：")
        for i, record in enumerate(test_records, 1):
            print(f"{i}. {record['record_no']} - {record['chief_complaint']} ({record['record_date'].strftime('%Y-%m-%d')})")
            
        # 查询验证
        print("\n验证查询结果：")
        cursor.execute("SELECT record_no, patient_name, chief_complaint, record_date FROM medical_record WHERE user_id = 1 ORDER BY record_date DESC")
        results = cursor.fetchall()
        for row in results:
            print(f"  {row['record_no']}: {row['chief_complaint']} ({row['record_date']})")
            
    except Exception as e:
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()
        if conn:
            conn.rollback()
    finally:
        if conn:
            conn.close()

if __name__ == "__main__":
    add_test_records()
