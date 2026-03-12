import json

from app.agents.base.base_agent import BaseAgent
from app.services.report.report_structured_parser import ReportStructuredParser


class ReportAgent(BaseAgent):
    """Agent for handling medical report analysis queries."""

    def __init__(self) -> None:
        super().__init__()
        self.parser = ReportStructuredParser()
    
    def handle(self, query: str) -> str:
        """
        Handle medical report analysis query and return structured JSON.
        
        The result includes extracted indicators, abnormality judgement,
        medical explanations, and raw report context.
        """
        result = self.call_tool("get_medical_report", user_id=1, limit=3)
        reports = []
        if result["success"] and result["data"]:
            reports = result["data"].get("reports", [])

        structured_result = self.parser.parse(query=query, reports=reports)
        return structured_result.model_dump_json(ensure_ascii=False)
