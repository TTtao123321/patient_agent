"""测试 ReportStructuredParser 的筛选逻辑"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'services', 'ai-agent-fastapi'))

from app.services.report.report_structured_parser import ReportStructuredParser

# 测试数据（模拟从 API 获取的报告）
test_reports = [
    {
        "report_no": "R202603222218122201",
        "report_type": "blood",
        "report_title": "2026年3月血常规检查",
        "report_date": "2026-03-22 22:18:12",
        "risk_level": "HIGH",
        "raw_text": "",
        "parsed_json": "",
        "interpretation_summary": "",
        "hospital_name": None,
        "department_name": None,
    },
    {
        "report_no": "RA77060AA49964602AC4B0A8780EB6EC",
        "report_type": "ultrasound",
        "report_title": "腹部超声检查报告",
        "report_date": "2026-03-20 06:22:32",
        "risk_level": "low",
        "raw_text": "",
        "parsed_json": "",
        "interpretation_summary": "",
        "hospital_name": "市第一人民医院",
        "department_name": "超声科",
    },
    {
        "report_no": "RFB06F9502349493486E75F3744E9A3D",
        "report_type": "blood",
        "report_title": "血常规检查报告",
        "report_date": "2026-03-13 06:22:32",
        "risk_level": "medium",
        "raw_text": """血常规检查报告
姓名：测试用户
性别：男
年龄：35岁
检查日期：2026-03-12

白细胞计数（WBC）: 10.5×10^9/L 【偏高】
红细胞计数（RBC）: 4.8×10^12/L 【正常】
血红蛋白（HGB）: 148g/L 【正常】
血小板计数（PLT）: 210×10^9/L 【正常】

结论：白细胞计数略高，可能存在轻度感染，建议结合临床症状进一步检查。""",
        "parsed_json": "{\"HGB\": \"148\", \"PLT\": \"210\", \"RBC\": \"4.8\", \"WBC\": \"10.5\"}",
        "interpretation_summary": "白细胞计数偏高，提示可能存在轻度炎症或感染。",
        "hospital_name": "市第一人民医院",
        "department_name": "检验科",
    },
    {
        "report_no": "R424A35E6859440ECB6766D9ED08AA07",
        "report_type": "blood",
        "report_title": "血常规检查报告",
        "report_date": "2026-02-26 06:22:32",
        "risk_level": "low",
        "raw_text": "血常规检查报告\n姓名：测试用户\n性别：男\n年龄：35岁\n检查日期：2026-02-25\n\n白细胞计数（WBC）: 6.5×10^9/L 【正常】\n红细胞计数（RBC）: 4.9×10^12/L 【正常】\n血红蛋白（HGB）: 150g/L 【正常】\n血小板计数（PLT）: 220×10^9/L 【正常】\n\n结论：各项指标正常，建议定期体检。",
        "parsed_json": "{\"HGB\": \"150\", \"PLT\": \"220\", \"RBC\": \"4.9\", \"WBC\": \"6.5\"}",
        "interpretation_summary": "各项检查指标均在正常范围内，提示当前身体状况良好。",
        "hospital_name": "市第一人民医院",
        "department_name": "检验科",
    }
]

# 用户查询
query = "我想知道我的医疗报告，在2026年3月份的血常规检查"

print("=== 测试 ReportStructuredParser ===")
print(f"用户查询: {query}\n")

parser = ReportStructuredParser()

# 测试 _infer_report_type
inferred_type = parser._infer_report_type(query=query, reports=[])
print(f"推断的报告类型: {inferred_type}")

# 测试 _select_relevant_reports
selected = parser._select_relevant_reports(query=query, reports=test_reports)
print(f"筛选出的报告数量: {len(selected)}")
for report in selected:
    print(f"  - {report['report_date']} {report['report_title']} ({report['report_type']})")

print("\n=== 完整解析结果 ===")
result = parser.parse(query=query, reports=test_reports)
print(f"报告类型: {result.report_type}")
print(f"整体状态: {result.overall_status}")
print(f"最近报告数量: {len(result.recent_reports)}")
for report in result.recent_reports:
    print(f"  - {report.report_date} {report.report_title}")

print(f"\n指标数量: {len(result.indicators)}")
for indicator in result.indicators:
    print(f"  - {indicator.name}: {indicator.value}{indicator.unit} ({indicator.status})")
