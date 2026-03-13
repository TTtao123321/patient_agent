import logging
import time

from app.core.settings import settings
from app.agents.knowledge.knowledge_agent import KnowledgeAgent
from app.observability.metrics.registry import metrics_registry
from app.agents.record.record_agent import RecordQueryAgent
from app.agents.report.report_agent import ReportAgent
from app.agents.router.intent_classifier import IntentClassifier
from app.agents.symptom.symptom_agent import SymptomAgent


logger = logging.getLogger(__name__)


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
        started_at = time.perf_counter()
        intent = self.classifier.classify(query)
        logger.info("agent_route_intent intent=%s query_len=%s", intent, len(query))

        if intent == "symptom_consult":
            return self._execute_agent(
                handler=self.symptom_agent.handle,
                query=query,
                intent=intent,
                agent_name="symptom_agent",
                started_at=started_at,
            )

        if intent == "report_analysis":
            return self._execute_agent(
                handler=self.report_agent.handle,
                query=query,
                intent=intent,
                agent_name="report_agent",
                started_at=started_at,
            )

        if intent == "record_query":
            return self._execute_agent(
                handler=self.record_agent.handle,
                query=query,
                intent=intent,
                agent_name="record_agent",
                started_at=started_at,
            )

        # Default to knowledge agent for medical_knowledge intent
        return self._execute_agent(
            handler=self.knowledge_agent.handle,
            query=query,
            intent="medical_knowledge",
            agent_name="knowledge_agent",
            started_at=started_at,
        )

    def _execute_agent(
        self,
        handler,
        query: str,
        intent: str,
        agent_name: str,
        started_at: float,
    ) -> tuple[str, str, str]:
        try:
            answer = handler(query)
            latency_ms = (time.perf_counter() - started_at) * 1000
            metrics_registry.record_agent_call(agent_name=agent_name, success=True)
            logger.info(
                "agent_route_completed intent=%s agent=%s latency_ms=%.2f",
                intent,
                agent_name,
                latency_ms,
            )
            if latency_ms >= settings.slow_agent_call_threshold_ms:
                logger.warning(
                    "slow_agent_call_detected intent=%s agent=%s latency_ms=%.2f threshold_ms=%s",
                    intent,
                    agent_name,
                    latency_ms,
                    settings.slow_agent_call_threshold_ms,
                )
            return answer, intent, agent_name
        except Exception:
            metrics_registry.record_agent_call(agent_name=agent_name, success=False)
            logger.exception("agent_route_failed intent=%s agent=%s", intent, agent_name)
            raise

