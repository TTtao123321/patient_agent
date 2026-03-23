"""直接测试 GetMedicalReportTool"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'services', 'ai-agent-fastapi'))

import httpx
import os

BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://127.0.0.1:8080")
user_id = 1

print("=== 步骤1: 获取报告列表 ===")
params = {
    "userId": user_id,
    "page": 1,
    "pageSize": 10
}
with httpx.Client(timeout=30.0) as client:
    response = client.get(
        f"{BACKEND_BASE_URL}/api/v1/reports",
        params=params
    )
    result = response.json()
    print(f"列表响应: {result}")
    print()
    
    data = result.get("data", {})
    items = data.get("items", [])
    
    print(f"找到 {len(items)} 条报告:")
    for i, item in enumerate(items):
        print(f"\n{i+1}. report_no: {item.get('reportNo')}")
        print(f"   标题: {item.get('reportTitle')}")
        print(f"   类型: {item.get('reportType')}")
        print(f"   日期: {item.get('reportDate')}")
        print(f"   rawText: {item.get('rawText', '<空>')}")
        print(f"   interpretationSummary: {item.get('interpretationSummary', '<空>')}")
    
    print("\n=== 步骤2: 获取每个报告的详情 ===")
    reports = []
    for item in items:
        report_no = item.get("reportNo")
        if not report_no:
            continue
        
        print(f"\n获取详情 report_no: {report_no}")
        try:
            detail_response = client.get(
                f"{BACKEND_BASE_URL}/api/v1/reports/{report_no}",
                params={"userId": user_id}
            )
            detail_result = detail_response.json()
            
            if detail_result.get("code") == 0:
                detail_data = detail_result.get("data", {})
                print(f"  详情:")
                print(f"    rawText长度: {len(str(detail_data.get('rawText', '')))}")
                print(f"    interpretationSummary: {detail_data.get('interpretationSummary', '<空>')}")
                reports.append({
                    "report_no": detail_data.get("reportNo"),
                    "report_type": detail_data.get("reportType"),
                    "report_title": detail_data.get("reportTitle"),
                    "report_date": detail_data.get("reportDate"),
                    "risk_level": detail_data.get("riskLevel"),
                    "raw_text": detail_data.get("rawText", ""),
                    "interpretation_summary": detail_data.get("interpretationSummary", ""),
                    "hospital_name": detail_data.get("hospitalName"),
                    "department_name": detail_data.get("departmentName"),
                })
        except Exception as e:
            print(f"  错误: {e}")
    
    print(f"\n=== 步骤3: 格式化输出 ===")
    if not reports:
        print("暂未查询到您的医疗报告记录。")
    else:
        lines = []
        lines.append("根据查询到您的医疗报告如下：\n")
        
        for report in reports:
            report_date = report.get("report_date", "")
            report_title = report.get("report_title", "")
            report_type = report.get("report_type", "")
            risk_level = report.get("risk_level", "")
            hospital = report.get("hospital_name", "")
            summary = report.get("interpretation_summary", "")
            raw_text = report.get("raw_text", "")
            
            lines.append(f"【{report_date}】{report_title}")
            if hospital:
                lines.append(f"🏥 医院：{hospital}")
            if risk_level:
                lines.append(f"⚠️ 风险等级：{risk_level}")
            if summary:
                lines.append(f"📝 摘要：{summary}")
            if raw_text:
                lines.append(f"\n📋 报告内容：\n{raw_text}")
            lines.append("\n" + "=" * 50 + "\n")
        
        final_text = "\n".join(lines)
        print(final_text)
