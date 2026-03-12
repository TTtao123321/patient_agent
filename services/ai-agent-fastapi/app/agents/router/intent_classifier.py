"""Intent classifier using LLM for accurate intent recognition."""

from app.llm.llm_client import get_llm_client


class IntentClassifier:
    """Classify user queries into specific intents using LLM."""
    
    # Fallback keywords for quick classification if LLM is unavailable
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
        """Initialize intent classifier."""
        self.llm_client = get_llm_client()

    def classify(self, query: str) -> str:
        """
        Classify query intent using LLM with fallback to keywords.
        
        Args:
            query: User query
        
        Returns:
            Intent classification (symptom_consult, report_analysis, medical_knowledge, record_query)
        """
        if not query or not query.strip():
            return "medical_knowledge"

        # Try keyword-based classification first for speed and reliability
        keyword_intent = self._classify_by_keywords(query)
        
        # If keyword classification is confident, use it
        if keyword_intent != "medical_knowledge":
            return keyword_intent
        
        try:
            # Use LLM for more nuanced classification or when keywords are uncertain
            prompt = self.INTENT_SYSTEM_PROMPT.format(query=query.strip())
            response = self.llm_client.invoke(prompt).strip().lower()
            
            # Validate and extract intent from response
            valid_intents = {"symptom_consult", "report_analysis", "medical_knowledge", "record_query"}
            
            # Look for exact intent match in response
            for intent in valid_intents:
                if intent in response:
                    return intent
            
            # If LLM response doesn't contain a valid intent, fall back to keywords
            return keyword_intent
        except Exception as e:
            # Fallback to keyword-based classification if LLM fails
            print(f"LLM classification failed: {e}, falling back to keywords")
            return keyword_intent

    def _classify_by_keywords(self, query: str) -> str:
        """
        Keyword-based intent classification for reliable baseline.
        
        Args:
            query: User query
        
        Returns:
            Intent classification
        """
        text = (query or "").strip().lower()
        
        # Check keywords in order of priority (more specific first)
        # Priority 1: Record keywords for history/past queries
        if any(word in text for word in self.RECORD_KEYWORDS):
            return "record_query"
        
        # Priority 2: Report keywords for test results
        if any(word in text for word in self.REPORT_KEYWORDS):
            return "report_analysis"
        
        # Priority 3: Knowledge keywords often accompany symptoms - need differentiation
        # If query has "怎样", "怎么", "如何", "治疗" without symptom keywords, it's medical knowledge
        has_knowledge_kw = any(word in text for word in {"怎样", "怎么", "如何", "治疗", "建议"})
        has_symptom_kw = any(word in text for word in self.SYMPTOM_KEYWORDS)
        
        if has_knowledge_kw and not has_symptom_kw:
            return "medical_knowledge"

        # Priority 4: Symptom keywords
        if any(word in text for word in self.SYMPTOM_KEYWORDS):
            return "symptom_consult"
        
        # Priority 5: General knowledge keywords
        if any(word in text for word in self.KNOWLEDGE_KEYWORDS):
            return "medical_knowledge"

        return "medical_knowledge"  # Default intent



