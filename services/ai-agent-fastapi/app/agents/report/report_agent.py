from app.agents.base.base_agent import BaseAgent


class ReportAgent(BaseAgent):
    def handle(self, query: str) -> str:
        return (
            "[Report Agent] 已识别为报告解读请求。"
            "请上传或粘贴关键指标、参考区间和检查结论，我将进行结构化分析。"
        )
