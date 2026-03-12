"""
Tool Calling Usage Examples

This module demonstrates how to use the Tool Calling API.
"""

import json

# Example 1: List Available Tools
print("=" * 60)
print("Example 1: List Available Tools")
print("=" * 60)
print("""
GET /tools/available

Response:
{
  "total_tools": 4,
  "tools": [
    {
      "name": "get_medical_report",
      "description": "获取用户的医疗报告，包含报告类型、时间、解读摘要和风险等级",
      "parameters": [
        {
          "name": "user_id",
          "type": "integer",
          "description": "用户ID",
          "required": true
        },
        {
          "name": "report_type",
          "type": "string",
          "description": "报告类型: blood(血液检查), ct(CT)...",
          "required": false
        },
        {
          "name": "limit",
          "type": "integer",
          "description": "返回最多报告数，默认值5",
          "required": false
        }
      ]
    },
    {
      "name": "get_medical_record",
      "description": "获取用户的病历信息，包含主诉、现病史、既往史等",
      "parameters": [...]
    },
    {
      "name": "search_drug",
      "description": "搜索药物信息，包含用途、用法用量、禁忌和副作用",
      "parameters": [...]
    },
    {
      "name": "search_department",
      "description": "搜索医疗科室信息，包含科室名称、主治疾病、常见检查项目",
      "parameters": [...]
    }
  ]
}
""")

# Example 2: Execute Single Tool - Get Medical Report
print("\n" + "=" * 60)
print("Example 2: Execute Single Tool - Get Medical Report")
print("=" * 60)
print("""
POST /tools/execute
Content-Type: application/json

{
  "tool_name": "get_medical_report",
  "parameters": {
    "user_id": 1,
    "report_type": "blood",
    "limit": 3
  }
}

Response:
{
  "result": {
    "tool_name": "get_medical_report",
    "success": true,
    "data": {
      "user_id": 1,
      "reports_count": 2,
      "reports": [
        {
          "report_no": "RPT20260310001",
          "report_type": "blood",
          "report_title": "血液检查报告",
          "report_date": "2026-03-10",
          "risk_level": "LOW",
          "interpretation_summary": "血红蛋白和白细胞计数正常，无异常发现",
          "hospital_name": "协和医院",
          "department_name": "检验科"
        }
      ]
    },
    "error": null
  }
}
""")

# Example 3: Execute Single Tool - Search Drug
print("\n" + "=" * 60)
print("Example 3: Execute Single Tool - Search Drug")
print("=" * 60)
print("""
POST /tools/execute
Content-Type: application/json

{
  "tool_name": "search_drug",
  "parameters": {
    "drug_name": "阿莫西林",
    "limit": 2
  }
}

Response:
{
  "result": {
    "tool_name": "search_drug",
    "success": true,
    "data": {
      "query": "阿莫西林",
      "results_count": 1,
      "drugs": [
        {
          "name": "阿莫西林",
          "generic_name": "Amoxicillin",
          "category": "β-内酰胺类抗生素",
          "usage": "用于治疗由敏感菌引起的感染，如呼吸道感染、泌尿道感染等",
          "dosage": "成人：250-500mg，每8小时一次；儿童：按体重25-45mg/kg/日分次服用",
          "contraindications": "对青霉素及β-内酰胺类抗生素过敏者禁用",
          "side_effects": "胃肠道反应、皮疹、过敏反应"
        }
      ]
    },
    "error": null
  }
}
""")

# Example 4: Execute Single Tool - Get Medical Record
print("\n" + "=" * 60)
print("Example 4: Execute Single Tool - Get Medical Record")
print("=" * 60)
print("""
POST /tools/execute
Content-Type: application/json

{
  "tool_name": "get_medical_record",
  "parameters": {
    "user_id": 1,
    "limit": 2
  }
}

Response:
{
  "result": {
    "tool_name": "get_medical_record",
    "success": true,
    "data": {
      "user_id": 1,
      "records_count": 2,
      "records": [
        {
          "record_no": "MR20260310001",
          "patient_name": "李明",
          "age": 45,
          "record_date": "2026-03-10",
          "chief_complaint": "咳嗽2周",
          "present_illness": "患者2周前因受凉出现干咳，伴喉咙痒，无发热，无浓痰",
          "past_history": "高血压病史10年，目前控制良好；2年前得过肺炎",
          "allergy_history": "青霉素过敏",
          "attending_doctor": "王医生"
        }
      ]
    },
    "error": null
  }
}
""")

# Example 5: Execute Single Tool - Search Department
print("\n" + "=" * 60)
print("Example 5: Execute Single Tool - Search Department")
print("=" * 60)
print("""
POST /tools/execute
Content-Type: application/json

{
  "tool_name": "search_department",
  "parameters": {
    "department_name": "呼吸科",
    "limit": 1
  }
}

Response:
{
  "result": {
    "tool_name": "search_department",
    "success": true,
    "data": {
      "query": "呼吸科",
      "results_count": 1,
      "departments": [
        {
          "name": "呼吸科",
          "description": "呼吸科专业诊治呼吸系统疾病",
          "main_diseases": "肺炎、支气管炎、哮喘、肺结核、肺癌、慢性阻塞性肺病等",
          "common_procedures": "胸部X线、胸部CT、肺功能检查、支气管镜检查、痰培养",
          "specialist": "肺部及呼吸系统疾病诊疗专家"
        }
      ]
    },
    "error": null
  }
}
""")

# Example 6: Batch Tool Execution
print("\n" + "=" * 60)
print("Example 6: Batch Tool Execution - Multiple Tools")
print("=" * 60)
print("""
POST /tools/batch
Content-Type: application/json

{
  "tool_calls": [
    {
      "tool_name": "get_medical_report",
      "parameters": {"user_id": 1, "limit": 2}
    },
    {
      "tool_name": "search_drug",
      "parameters": {"drug_name": "氨溴索"}
    },
    {
      "tool_name": "search_department",
      "parameters": {"department_name": "呼吸科"}
    }
  ]
}

Response:
{
  "total_calls": 3,
  "successful_calls": 3,
  "results": [
    {
      "tool_name": "get_medical_report",
      "success": true,
      "data": {...},
      "error": null
    },
    {
      "tool_name": "search_drug",
      "success": true,
      "data": {...},
      "error": null
    },
    {
      "tool_name": "search_department",
      "success": true,
      "data": {...},
      "error": null
    }
  ]
}
""")

# Example 7: Agent with Tool Integration - Symptom Query
print("\n" + "=" * 60)
print("Example 7: Agent Chat with Tool Integration")
print("=" * 60)
print("""
When a user sends a symptom-related query to the agent:

POST /agent/chat
Content-Type: application/json

{
  "session_no": "sess_001",
  "user_id": 1,
  "query": "最近一直咳嗽，有什么好的建议吗？"
}

The SymptomAgent will:
1. Call get_medical_record(user_id=1) to understand patient history
2. Search for relevant drugs if mentioned in the query
3. Return a comprehensive response incorporating this information

Response:
{
  "answer": "[Symptom Agent] Based on your medical history (最近病历: 咳嗽2周), 
    相关用药: 氨溴索: 用于治疗各种呼吸道疾病引起的咳嗽...
    请补充症状持续时间、伴随症状和既往病史，以便给出更准确建议。",
  "intent": "symptom_consult",
  "agent_used": "symptom_agent"
}
""")

# Example 8: Error Handling
print("\n" + "=" * 60)
print("Example 8: Error Handling - Invalid Tool")
print("=" * 60)
print("""
POST /tools/execute
Content-Type: application/json

{
  "tool_name": "nonexistent_tool",
  "parameters": {}
}

Response (Error):
{
  "result": {
    "tool_name": "nonexistent_tool",
    "success": false,
    "data": null,
    "error": "Tool 'nonexistent_tool' not found"
  }
}
""")

# Example 9: Error Handling - Missing Required Parameters
print("\n" + "=" * 60)
print("Example 9: Error Handling - Missing Required Parameters")
print("=" * 60)
print("""
POST /tools/execute
Content-Type: application/json

{
  "tool_name": "get_medical_report",
  "parameters": {}
}

Response (Error):
{
  "result": {
    "tool_name": "get_medical_report",
    "success": false,
    "data": null,
    "error": "缺少必需参数: user_id"
  }
}
""")

print("\n" + "=" * 60)
print("Tool Calling Examples Complete")
print("=" * 60)
print("""
Key Features:
1. Four medical tools available for agent use
2. Agents automatically integrate tool calls into responses
3. Batch execution for multiple tools in parallel
4. Error handling for missing tools or invalid parameters
5. Extensible architecture for adding new tools

For more details, see app/tools/TOOL_CALLING_DOC.py
""")
