class IntentClassifier:
    SYMPTOM_KEYWORDS = {
        "symptom",
        "fever",
        "cough",
        "pain",
        "headache",
        "腹痛",
        "头痛",
        "发烧",
        "咳嗽",
        "症状",
        "不舒服",
    }

    REPORT_KEYWORDS = {
        "report",
        "lab",
        "ct",
        "mri",
        "ultrasound",
        "blood test",
        "报告",
        "化验",
        "检查单",
        "指标",
        "检验",
        "影像",
    }

    def classify(self, query: str) -> str:
        text = (query or "").strip().lower()
        if not text:
            return "medical_knowledge"

        if any(word in text for word in self.REPORT_KEYWORDS):
            return "report_analysis"

        if any(word in text for word in self.SYMPTOM_KEYWORDS):
            return "symptom_consult"

        return "medical_knowledge"
