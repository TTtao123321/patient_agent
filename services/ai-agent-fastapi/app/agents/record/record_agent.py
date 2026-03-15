"""病历查询 Agent。"""

from app.agents.base.base_agent import BaseAgent


class RecordQueryAgent(BaseAgent):
    """处理用户病历/既往史相关问题。"""
    
    def handle(self, query: str) -> str:
        """查询并摘要历史病历信息。"""
        lines: list[str] = ["【病历查询 Agent】"]

        # 通过工具读取病历（当前用示例 user_id，后续可从上下文透传）。
        result = self.call_tool("get_medical_record", user_id=1, limit=5)

        if result["success"] and result["data"]:
            records = result["data"].get("records", [])
            lines.append(f"[工具调用] get_medical_record → 查询到 {len(records)} 条病历记录")
            lines.append("")
            if records:
                lines.append("医疗历史摘要：")
                for idx, record in enumerate(records, 1):
                    visit_date = record.get("visit_date", "未知日期")
                    chief_complaint = record.get("chief_complaint", "未记录")
                    diagnosis = record.get("diagnosis", "未记录")
                    entry = f"{idx}. [{visit_date}] 主诉：{chief_complaint}，诊断：{diagnosis}"
                    lines.append(entry)
            else:
                lines.append("暂未找到相关医疗记录。")
        else:
            lines.append("[工具调用] get_medical_record → 查询失败")
            lines.append("")
            lines.append("未能检索到医疗记录，请稍后再试。")

        lines.append("")
        lines.append("如需查询具体记录详情或对比病程发展，请提供更具体的时间范围或症状关键词。")
        return "\n".join(lines)
