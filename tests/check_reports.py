import pymysql
import json

# 数据库连接配置
db_config = {
    "host": "101.126.81.197",
    "port": 3306,
    "user": "root",
    "password": "root123456",
    "database": "patient_agent",
    "charset": "utf8mb4"
}

try:
    connection = pymysql.connect(**db_config)
    cursor = connection.cursor(pymysql.cursors.DictCursor)
    
    print("=== 查询 medical_report 表数据 ===")
    cursor.execute("SELECT * FROM medical_report WHERE user_id = 1")
    reports = cursor.fetchall()
    print(f"找到 {len(reports)} 条报告记录：")
    for report in reports:
        print(f"\n报告ID: {report['id']}")
        print(f"报告编号: {report['report_no']}")
        print(f"报告类型: {report['report_type']}")
        print(f"报告标题: {report['report_title']}")
        print(f"报告日期: {report['report_date']}")
        print(f"风险等级: {report['risk_level']}")
        print(f"interpretation_summary: {report.get('interpretation_summary', '')}")
        print(f"raw_text: {report.get('raw_text', '')}")
        print(f"parsed_json: {report.get('parsed_json', '')}")
    
    cursor.close()
    connection.close()
    
except Exception as e:
    print(f"查询失败: {e}")
