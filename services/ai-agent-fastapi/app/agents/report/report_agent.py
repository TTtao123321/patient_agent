from app.agents.base.base_agent import BaseAgent


class ReportAgent(BaseAgent):
    """报告解读 Agent。

    先通过工具读取报告，返回包含文本回答和结构化报告数据的字典。
    """

    def __init__(self) -> None:
        super().__init__()
    
    def handle(self, query: str, user_id: int) -> dict:
        """处理报告分析请求，返回包含文本回答和结构化报告数据的字典。"""
        result = self.call_tool("get_medical_report", user_id=user_id, limit=10)
        reports = []
        if result["success"] and result["data"]:
            reports = result["data"].get("reports", [])
        
        if not reports:
            return {
                "answer": "暂未查询到您的医疗报告记录。",
                "reports": []
            }
        
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
        
        return {
            "answer": "\n".join(lines),
            "reports": reports
        }
