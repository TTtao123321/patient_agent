"""Tool calling documentation and examples."""

"""
# Tool Calling Mechanism for Medical Agent System

## Overview

The Tool Calling mechanism enables agents to access external tools and data sources
to provide more accurate and comprehensive responses. This document describes the
architecture and usage.

## Available Tools

### 1. get_medical_report
Retrieves medical reports for a user.

**Parameters:**
- `user_id` (int, required): User ID
- `report_type` (str, optional): Report type filter (blood, ct, mri, pathology, ultrasound)
- `limit` (int, optional): Number of reports to return (default: 5)

**Response:**
```json
{
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
  }
}
```

### 2. get_medical_record
Retrieves medical records for a user.

**Parameters:**
- `user_id` (int, required): User ID
- `limit` (int, optional): Number of records to return (default: 3)

**Response:**
```json
{
  "success": true,
  "data": {
    "user_id": 1,
    "records_count": 1,
    "records": [
      {
        "record_no": "MR20260310001",
        "patient_name": "李明",
        "age": 45,
        "record_date": "2026-03-10",
        "chief_complaint": "咳嗽2周",
        "present_illness": "患者2周前因受凉出现干咳...",
        "past_history": "高血压病史10年...",
        "allergy_history": "青霉素过敏",
        "attending_doctor": "王医生"
      }
    ]
  }
}
```

### 3. search_drug
Searches for drug information.

**Parameters:**
- `drug_name` (str, required): Drug name or ingredient
- `limit` (int, optional): Number of results to return (default: 3)

**Response:**
```json
{
  "success": true,
  "data": {
    "query": "阿莫西林",
    "results_count": 1,
    "drugs": [
      {
        "name": "阿莫西林",
        "generic_name": "Amoxicillin",
        "category": "β-内酰胺类抗生素",
        "usage": "用于治疗由敏感菌引起的感染...",
        "dosage": "成人：250-500mg，每8小时一次...",
        "contraindications": "对青霉素及β-内酰胺类抗生素过敏者禁用",
        "side_effects": "胃肠道反应、皮疹、过敏反应"
      }
    ]
  }
}
```

### 4. search_department
Searches for medical department information.

**Parameters:**
- `department_name` (str, required): Department name or keyword
- `limit` (int, optional): Number of results to return (default: 3)

**Response:**
```json
{
  "success": true,
  "data": {
    "query": "呼吸科",
    "results_count": 1,
    "departments": [
      {
        "name": "呼吸科",
        "description": "呼吸科专业诊治呼吸系统疾病",
        "main_diseases": "肺炎、支气管炎、哮喘...",
        "common_procedures": "胸部X线、胸部CT、肺功能检查...",
        "specialist": "肺部及呼吸系统疾病诊疗专家"
      }
    ]
  }
}
```

## API Endpoints

### List Available Tools
```
GET /tools/available
```

Response:
```json
{
  "total_tools": 4,
  "tools": [
    {
      "name": "get_medical_report",
      "description": "获取用户的医疗报告...",
      "parameters": [...]
    }
  ]
}
```

### Execute Single Tool
```
POST /tools/execute
```

Request:
```json
{
  "tool_name": "get_medical_report",
  "parameters": {
    "user_id": 1,
    "limit": 3
  }
}
```

Response:
```json
{
  "result": {
    "tool_name": "get_medical_report",
    "success": true,
    "data": {...},
    "error": null
  }
}
```

### Execute Multiple Tools (Batch)
```
POST /tools/batch
```

Request:
```json
{
  "tool_calls": [
    {
      "tool_name": "get_medical_report",
      "parameters": {"user_id": 1}
    },
    {
      "tool_name": "search_drug",
      "parameters": {"drug_name": "阿莫西林"}
    }
  ]
}
```

Response:
```json
{
  "total_calls": 2,
  "successful_calls": 2,
  "results": [...]
}
```

## Agent Integration

Each agent has access to the tool executor and can call tools within its `handle()` method.

### Example: SymptomAgent with Tool Calling

The SymptomAgent can now:
1. Call `get_medical_record` to understand patient history
2. Call `search_drug` to find relevant medication information
3. Integrate this information into its response

### Example: ReportAgent with Tool Calling

The ReportAgent can now:
1. Call `get_medical_report` to retrieve user's reports
2. Analyze the reports and provide interpretation
3. Recommend follow-up actions based on risk levels

### Example: KnowledgeAgent with Tool Calling

The KnowledgeAgent can now:
1. Use RAG for knowledge base retrieval
2. Call `search_department` for department recommendations
3. Call `search_drug` for medication information
4. Provide comprehensive medical knowledge responses

## Architecture

```
User Request
    ↓
RouterAgent (Intent Classification)
    ↓
Specific Agent (Symptom/Report/Knowledge)
    ↓
ToolExecutor (Tool Calling)
    ↓
Tool (GetMedicalReport/GetMedicalRecord/SearchDrug/SearchDepartment)
    ↓
Tool Result
    ↓
Agent Response
```

## Extension Points

### Adding New Tools

1. Create a new class inheriting from `BaseTool`
2. Implement required properties: `name`, `description`
3. Implement `execute(**kwargs)` method
4. Register in `ToolRegistry._register_default_tools()`

Example:
```python
class CustomTool(BaseTool):
    @property
    def name(self) -> str:
        return "custom_tool"
    
    @property
    def description(self) -> str:
        return "Custom tool description"
    
    @property
    def parameters(self) -> list[ToolParameter]:
        return [...]
    
    def execute(self, **kwargs) -> dict[str, Any]:
        # Tool implementation
        return {"success": True, "data": {...}}
```

### Future Enhancements

1. **Integration with Java Backend**: Tools can make HTTP calls to Java API endpoints for real data
2. **LLM-based Tool Selection**: Use LLM to intelligently choose which tools to call
3. **Tool Result Caching**: Cache tool results for common queries
4. **Parallel Tool Execution**: Execute multiple tools concurrently
5. **Tool Output Formatting**: Format tool results for better agent consumption
"""

# This module serves as documentation for the Tool Calling system
# Implementation details are in separate modules
