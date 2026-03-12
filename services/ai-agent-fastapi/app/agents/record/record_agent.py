"""Agent for handling medical record queries."""

from app.agents.base.base_agent import BaseAgent


class RecordQueryAgent(BaseAgent):
    """Agent for handling medical record queries.
    
    Handles queries about patient's medical history, past diagnoses, 
    previous treatments, and medical background.
    """
    
    def handle(self, query: str) -> str:
        """
        Handle medical record query.
        
        Retrieves and summarizes patient's medical history and records.
        """
        response = "[Record Query Agent] "
        
        # Try to get medical records to provide medical history
        # In a real scenario, we would extract user_id from context
        result = self.call_tool("get_medical_record", user_id=1, limit=5)
        
        if result["success"] and result["data"]:
            records = result["data"].get("records", [])
            if records:
                response += "已检索到您的医疗记录。医疗历史摘要：\n"
                for idx, record in enumerate(records, 1):
                    visit_date = record.get("visit_date", "未知日期")
                    chief_complaint = record.get("chief_complaint", "未记录")
                    diagnosis = record.get("diagnosis", "未记录")
                    response += f"\n{idx}. [{visit_date}] 主诉：{chief_complaint}"
                    if diagnosis:
                        response += f"，诊断：{diagnosis}"
                response += "\n"
            else:
                response += "暂未找到相关医疗记录。"
        else:
            response += "未能检索到医疗记录。"

        response += "\n如需查询具体记录详情或对比病程发展，请提供更具体的时间范围或症状关键词。"
        return response
