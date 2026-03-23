from app.agents.base.base_agent import BaseAgent
from app.tools.executor.tool_executor import ToolCall


class SymptomAgent(BaseAgent):
    """症状咨询 Agent。"""
    
    def handle(self, query: str, user_id: int) -> str:
        """处理症状类问题，结合病历与药物信息给出初步建议。"""
        lines: list[str] = ["【症状咨询 Agent】"]

        # 先读取近期病历用于上下文补充。
        record_result = self.call_tool("get_medical_record", user_id=user_id, limit=2)
        recent_complaint: str | None = None
        if record_result["success"] and record_result["data"]:
            records = record_result["data"].get("records", [])
            if records:
                recent_complaint = records[0].get("chief_complaint", "")
                lines.append(f"[工具调用] get_medical_record → 近期主诉：{recent_complaint}")
            else:
                lines.append("[工具调用] get_medical_record → 暂无近期病历记录")
        else:
            lines.append("[工具调用] get_medical_record → 查询失败，跳过病历参考")

        # 提取问题中提及的药名并查询说明。
        drug_keywords = ["阿莫西林", "氨溴索", "布洛芬"]
        mentioned_drugs = [drug for drug in drug_keywords if drug in query]
        drug_info_lines: list[str] = []
        for drug in mentioned_drugs[:2]:
            drug_result = self.call_tool("search_drug", drug_name=drug, limit=1)
            if drug_result["success"] and drug_result["data"]:
                drugs = drug_result["data"].get("drugs", [])
                if drugs:
                    usage = drugs[0].get("usage", "暂无说明")
                    lines.append(f"[工具调用] search_drug({drug}) → 用法：{usage}")
                    drug_info_lines.append(f"{drug}：{usage}")
                else:
                    lines.append(f"[工具调用] search_drug({drug}) → 未找到药物信息")
            else:
                lines.append(f"[工具调用] search_drug({drug}) → 查询失败")

        # 生成回答正文。
        lines.append("")
        if recent_complaint:
            lines.append(f"根据您的近期病历（主诉：{recent_complaint}），结合您当前描述：")
        if drug_info_lines:
            lines.append("用药参考：" + "；".join(drug_info_lines) + "。")
        lines.append("请补充症状持续时间、伴随症状和既往病史，以便给出更准确建议。")

        return "\n".join(lines)
