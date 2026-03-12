from app.agents.base.base_agent import BaseAgent
from app.tools.executor.tool_executor import ToolCall


class SymptomAgent(BaseAgent):
    """Agent for handling symptom consultation queries."""
    
    def handle(self, query: str) -> str:
        """
        Handle symptom consultation query.
        
        May call medical record and drug search tools to gather information.
        """
        # For symptom consultation, try to get recent medical records and search for relevant drugs
        response = "[Symptom Agent] "
        
        # Try to get medical records to understand patient history
        # In a real scenario, we would extract user_id from context
        result = self.call_tool("get_medical_record", user_id=1, limit=2)
        if result["success"] and result["data"]:
            records = result["data"].get("records", [])
            if records:
                recent_record = records[0]
                response += f"Based on your medical history (最近病历: {recent_record.get('chief_complaint')}), "
        
        # Extract potential drugs from query to search
        drug_keywords = ["阿莫西林", "氨溴索", "布洛芬"]
        mentioned_drugs = [drug for drug in drug_keywords if drug in query]
        if mentioned_drugs:
            drug_info_list = []
            for drug in mentioned_drugs[:2]:
                drug_result = self.call_tool("search_drug", drug_name=drug, limit=1)
                if drug_result["success"] and drug_result["data"]:
                    drugs = drug_result["data"].get("drugs", [])
                    if drugs:
                        drug_info_list.append(f"{drugs[0].get('name')}: {drugs[0].get('usage')}")
            if drug_info_list:
                response += f"使用建议: {';'.join(drug_info_list)}。"
        
        response += "请补充症状持续时间、伴随症状和既往病史，以便给出更准确建议。"
        return response
