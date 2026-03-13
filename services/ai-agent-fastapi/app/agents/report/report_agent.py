import json

from app.agents.base.base_agent import BaseAgent
from app.services.report.report_structured_parser import ReportStructuredParser


class ReportAgent(BaseAgent):
    """报告解读 Agent。

    先通过工具读取报告，再用结构化解析器输出标准 JSON。
    """

    def __init__(self) -> None:
        super().__init__()
        self.parser = ReportStructuredParser()
    
    def handle(self, query: str) -> str:
        """处理报告分析请求并返回结构化 JSON 字符串。"""
        result = self.call_tool("get_medical_report", user_id=1, limit=3)
        reports = []
        if result["success"] and result["data"]:
            reports = result["data"].get("reports", [])

        structured_result = self.parser.parse(query=query, reports=reports)
        return structured_result.model_dump_json(ensure_ascii=False)
