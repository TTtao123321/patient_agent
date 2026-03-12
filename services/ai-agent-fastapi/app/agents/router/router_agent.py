from app.agents.knowledge.knowledge_agent import KnowledgeAgent
from app.agents.record.record_agent import RecordQueryAgent
from app.agents.report.report_agent import ReportAgent
from app.agents.router.intent_classifier import IntentClassifier
from app.agents.symptom.symptom_agent import SymptomAgent


class RouterAgent:
    """Router agent that classifies intents and routes to appropriate agents.
    
    Supports four intent types:
    - symptom_consult: Queries about symptoms
    - report_analysis: Queries about medical reports and test results
    - medical_knowledge: General medical knowledge questions
    - record_query: Queries about medical history and records
    """
    
    def __init__(self) -> None:
        self.classifier = IntentClassifier()
        self.symptom_agent = SymptomAgent()
        self.report_agent = ReportAgent()
        self.knowledge_agent = KnowledgeAgent()
        self.record_agent = RecordQueryAgent()

    def route(self, query: str) -> tuple[str, str, str]:
        """
        Route query to appropriate agent based on intent.
        
        Args:
            query: User query
        
        Returns:
            Tuple of (answer, intent, agent_used)
        """
        intent = self.classifier.classify(query)

        if intent == "symptom_consult":
            answer = self.symptom_agent.handle(query)
            return answer, intent, "symptom_agent"

        if intent == "report_analysis":
            answer = self.report_agent.handle(query)
            return answer, intent, "report_agent"
        
        if intent == "record_query":
            answer = self.record_agent.handle(query)
            return answer, intent, "record_agent"

        # Default to knowledge agent for medical_knowledge intent
        answer = self.knowledge_agent.handle(query)
        return answer, "medical_knowledge", "knowledge_agent"

