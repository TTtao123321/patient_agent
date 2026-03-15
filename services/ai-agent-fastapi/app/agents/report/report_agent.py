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
        lines: list[str] = ["【报告解读 Agent】"]

        result = self.call_tool("get_medical_report", user_id=1, limit=3)
        reports = []
        if result["success"] and result["data"]:
            reports = result["data"].get("reports", [])
            lines.append(f"[工具调用] get_medical_report → 查询到 {len(reports)} 份报告")
        else:
            lines.append("[工具调用] get_medical_report → 查询失败或暂无报告")

        lines.append("")
        structured_result = self.parser.parse(query=query, reports=reports)
        lines.append(structured_result.model_dump_json(ensure_ascii=False))
        return "\n".join(lines)
