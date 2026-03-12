from app.agents.base.base_agent import BaseAgent


class ReportAgent(BaseAgent):
    """Agent for handling medical report analysis queries."""
    
    def handle(self, query: str) -> str:
        """
        Handle medical report analysis query.
        
        May call medical report tool to retrieve user's reports.
        """
        response = "[Report Agent] "
        
        # Try to get medical reports
        # In a real scenario, we would extract user_id from context
        result = self.call_tool("get_medical_report", user_id=1, limit=3)
        if result["success"] and result["data"]:
            reports = result["data"].get("reports", [])
            if reports:
                response += "已检索到您的医疗报告列表。"
                for report in reports[:2]:
                    response += (
                        f"\n- {report.get('report_date')} {report.get('report_title')}"
                        f"({report.get('risk_level')}): {report.get('interpretation_summary', '暂无解读')}"
                    )
                response += "\n"
            else:
                response += "暂未找到相关医疗报告。"
        else:
            response += "未能检索到医疗报告。"
        
        response += "请提供具体的报告数据、关键指标和参考范围，以便进行更准确的解读。"
        return response
