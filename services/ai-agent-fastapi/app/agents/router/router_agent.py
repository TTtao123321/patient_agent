from app.agents.knowledge.knowledge_agent import KnowledgeAgent
from app.agents.report.report_agent import ReportAgent
from app.agents.router.intent_classifier import IntentClassifier
from app.agents.symptom.symptom_agent import SymptomAgent


class RouterAgent:
    def __init__(self) -> None:
        self.classifier = IntentClassifier()
        self.symptom_agent = SymptomAgent()
        self.report_agent = ReportAgent()
        self.knowledge_agent = KnowledgeAgent()

    def route(self, query: str) -> tuple[str, str, str]:
        intent = self.classifier.classify(query)

        if intent == "symptom_consult":
            answer = self.symptom_agent.handle(query)
            return answer, intent, "symptom_agent"

        if intent == "report_analysis":
            answer = self.report_agent.handle(query)
            return answer, intent, "report_agent"

        answer = self.knowledge_agent.handle(query)
        return answer, "medical_knowledge", "knowledge_agent"
