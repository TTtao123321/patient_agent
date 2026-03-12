"""Debug script to test intent classification locally."""

import sys
sys.path.insert(0, '/Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi')

from app.agents.router.intent_classifier import IntentClassifier

classifier = IntentClassifier()

test_cases = [
    "我最近一直咳嗽，还有发烧。请问这是什么症状？",
    "我的血液检查报告显示白细胞偏高，这是什么意思？",
    "我想了解一下我过去的医疗记录。",
    "感冒一般怎样治疗？",
    "我的CT影像显示有阴影。",
    "我之前得过肺炎，现在又开始咳嗽了。",
]

for query in test_cases:
    print(f"\n查询: {query}")
    
    # Test keyword classification
    keyword_intent = classifier._classify_by_keywords(query)
    print(f"  关键词分类: {keyword_intent}")
    
    # Check which keywords match
    text = query.lower()
    matches = {
        "SYMPTOM": [k for k in classifier.SYMPTOM_KEYWORDS if k in text],
        "REPORT": [k for k in classifier.REPORT_KEYWORDS if k in text],
        "RECORD": [k for k in classifier.RECORD_KEYWORDS if k in text],
        "KNOWLEDGE": [k for k in classifier.KNOWLEDGE_KEYWORDS if k in text],
    }
    
    for kw_type, matched_keywords in matches.items():
        if matched_keywords:
            print(f"    {kw_type}匹配: {matched_keywords}")
    
    # Test full classification
    intent = classifier.classify(query)
    print(f"  最终分类: {intent}")
