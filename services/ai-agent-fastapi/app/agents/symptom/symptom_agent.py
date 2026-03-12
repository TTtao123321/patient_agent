from app.agents.base.base_agent import BaseAgent


class SymptomAgent(BaseAgent):
    def handle(self, query: str) -> str:
        return (
            "[Symptom Agent] 已识别为症状咨询。"
            "请补充症状持续时间、伴随症状和既往病史，以便给出更准确建议。"
        )
