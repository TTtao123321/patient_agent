"""Test script for Router Agent intent classification."""

import sys
import uuid

# Add FastAPI project to path
sys.path.insert(0, '/Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi')

# Test the router agent directly
from app.agents.router.router_agent import RouterAgent

router = RouterAgent()

# Test cases for different intents
test_cases = [
    ("我最近一直咳嗽，还有发烧。请问这是什么症状？", "symptom_consult"),
    ("我的血液检查报告显示白细胞偏高，这是什么意思？", "report_analysis"),
    ("我想了解一下我过去的医疗记录。", "record_query"),
    ("感冒一般怎样治疗？", "medical_knowledge"),
    ("我的CT影像显示有阴影。", "report_analysis"),
    ("我之前得过肺炎，现在又开始咳嗽了。", "symptom_consult"),
]

print("=" * 80)
print("Router Agent 意图分类和路由测试")
print("=" * 80)

for query, expected_intent in test_cases:
    print(f"\n查询: {query}")
    print(f"预期意图: {expected_intent}")
    
    try:
        answer, intent, agent_used = router.route(query)
        
        # Check if intent matches
        status = "✓" if intent == expected_intent else "✗"
        print(f"{status} 识别意图: {intent}")
        print(f"  意图匹配: {intent == expected_intent}")
        print(f"  使用Agent: {agent_used}")
        print(f"  回复摘要: {answer[:100]}...")
    except Exception as e:
        print(f"✗ 错误: {e}")

print("\n" + "=" * 80)
print("测试完成")
print("=" * 80)
