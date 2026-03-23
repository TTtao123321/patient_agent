"""意图识别器。

优先关键词快速分类，必要时回退到 LLM 提升复杂场景识别准确性。
"""

from app.llm.llm_client import get_llm_client


class IntentClassifier:
    """将用户问题分类到既定医疗意图。"""
    
    # LLM 不可用时的兜底关键词集合。
    SYMPTOM_KEYWORDS = {
        "症状", "不舒服", "疼痛", "痛", 
        "发烧", "发热", "咳嗽", "腹痛", "头痛", "喉咙", "咽痛", "鼻塞", "流鼻涕",
        "fever", "cough", "pain", "headache", "ache", "itch",
    }

    REPORT_KEYWORDS = {
        "报告", "化验", "检查单", "指标", "检验", "影像", "检查结果", "血液", "数值", "参考范围", "阳性", "阴性",
        "report", "lab", "ct", "mri", "ultrasound", "blood test", "result", "values", "index",
    }
    
    RECORD_KEYWORDS = {
        "病历", "记录", "历史", "既往", "以前", "病史", "医疗历史", "过去", "之前的",
        "history", "record", "background", "past", "previous", "medical history", "medical records",
    }
    
    KNOWLEDGE_KEYWORDS = {
        "疾病", "治疗", "药物", "用药", "科室", "医生", "诊疗", "怎样", "怎么", "如何", "建议",
        "disease", "treatment", "medicine", "drug", "department", "doctor", "how to", "advice",
    }

    INTENT_SYSTEM_PROMPT = """You are a medical domain expert responsible for classifying patient inquiries into specific intent categories.

Your task is to analyze the user query and determine which category it belongs to:

1. **symptom_consult**: The user is describing their current symptoms or asking about symptoms they are experiencing (e.g., "I have a cough and fever", "What does this pain mean?")

2. **report_analysis**: The user is asking about medical test results, lab reports, imaging reports, or numerical indicators from examinations (e.g., "My blood test shows high white blood cells", "What does the CT scan shadow mean?")

3. **record_query**: The user is asking about their medical history, past medical records, previous diagnoses, or medical background (e.g., "I want to check my medical records", "Did I have pneumonia before?")

4. **medical_knowledge**: The user is asking general medical questions, about diseases, treatments, medication, or seeking medical advice/education (e.g., "How to treat a cold?", "What are the symptoms of flu?")

Important guidelines:
- If the query mentions specific test numbers or results (血液检查, 化验指标, CT/MRI results), classify as "report_analysis"
- If the query asks about past medical events or history, classify as "record_query"  
- If the query describes current symptoms, classify as "symptom_consult"
- If the query asks general medical questions, classify as "medical_knowledge"

User query: "{query}"

Respond with ONLY the intent name (one of: symptom_consult, report_analysis, record_query, medical_knowledge). No other text."""

    def __init__(self):
        """初始化意图分类器。"""
        self.llm_client = get_llm_client()

    def classify(self, query: str) -> str:
        """识别问题意图。

        策略：先做关键词分类（快且稳定），仅在不确定时调用 LLM。
        """
        if not query or not query.strip():
            return "medical_knowledge"

        # 先走关键词分类，保证响应速度和稳定性。
        keyword_intent = self._classify_by_keywords(query)
        
        # 关键词结果已明确时，直接返回。
        if keyword_intent != "medical_knowledge":
            return keyword_intent
        
        try:
            # 关键词不明显时，调用 LLM 进行细粒度判别。
            prompt = self.INTENT_SYSTEM_PROMPT.format(query=query.strip())
            response = self.llm_client.invoke(prompt).strip().lower()
            
            # 从 LLM 输出中抽取合法意图。
            valid_intents = {"symptom_consult", "report_analysis", "medical_knowledge", "record_query"}
            
            # 允许模型输出包含额外内容，只要命中合法意图即可。
            for intent in valid_intents:
                if intent in response:
                    return intent
            
            # 模型结果不规范时回退关键词结果。
            return keyword_intent
        except Exception as e:
            # LLM 调用异常时兜底，保证流程不中断。
            print(f"LLM classification failed: {e}, falling back to keywords")
            return keyword_intent

    def _classify_by_keywords(self, query: str) -> str:
        """关键词意图分类（基线规则）。"""
        text = (query or "").strip().lower()
        
        # 按优先级依次匹配，避免类别歧义。
        # 1) 报告解读（优先，因为报告查询中经常包含"记录"等词）
        if any(word in text for word in self.REPORT_KEYWORDS):
            return "report_analysis"
        
        # 2) 病历查询
        if any(word in text for word in self.RECORD_KEYWORDS):
            return "record_query"
        
        # 3) 常见问法词需与症状词区分，避免把症状咨询误判为知识问答。
        has_knowledge_kw = any(word in text for word in {"怎样", "怎么", "如何", "治疗", "建议"})
        has_symptom_kw = any(word in text for word in self.SYMPTOM_KEYWORDS)
        
        if has_knowledge_kw and not has_symptom_kw:
            return "medical_knowledge"

        # 4) 症状咨询
        if any(word in text for word in self.SYMPTOM_KEYWORDS):
            return "symptom_consult"
        
        # 5) 医学常识
        if any(word in text for word in self.KNOWLEDGE_KEYWORDS):
            return "medical_knowledge"

        return "medical_knowledge"  # Default intent



