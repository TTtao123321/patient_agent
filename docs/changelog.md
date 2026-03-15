# 项目变更日志

---

## 2026-03-15

### 1. 修复 Ollama 模型名称配置 (`settings.py`)

**文件**: `services/ai-agent-fastapi/app/core/settings.py`

**问题**: `ollama_model` 默认值为 `"qwen"`，但本地 Ollama 服务中不存在该模型，
导致 LLM 调用返回 404，意图分类器无法走 LLM 分支，始终回退到关键词匹配。

**修改**: 将默认值从 `"qwen"` 改为 `"Qwen2.5-3B:latest"`，与本地已拉取的模型名对齐。

---

### 2. KnowledgeAgent 增加 Milvus 不可用时的降级容错 (`knowledge_agent.py`)

**文件**: `services/ai-agent-fastapi/app/agents/knowledge/knowledge_agent.py`

**问题**: 当 Milvus 向量库（`192.168.44.50:19530`）不可达时，`RagRetriever()` 初始化
抛出 `ConnectionNotExistException`，异常未被捕获，导致整条消费链路中断，
用户收到"AI 任务处理失败，请稍后重试"的系统错误。

**修改**:
- 新增 `_rag_disabled: bool` 标志位，初始化失败时置 `True`。
- 在 `RagRetriever()` 初始化和 `.retrieve()` 调用处各加 `try/except`，连接失败时平滑降级。
- 新增 `_build_fallback_response(query)` 方法，内置问候词检测
  (`{"你好","您好","hi","hello","嗨","在吗","在么"}`)，对问候语返回友好回复，
  对其他查询返回"知识库暂不可用"提示，而非系统报错。

---

### 3. 所有 Agent 回复增加 Agent 名称与工具调用可视化

**涉及文件**:
- `services/ai-agent-fastapi/app/agents/symptom/symptom_agent.py`
- `services/ai-agent-fastapi/app/agents/record/record_agent.py`
- `services/ai-agent-fastapi/app/agents/report/report_agent.py`
- `services/ai-agent-fastapi/app/agents/knowledge/knowledge_agent.py`

**问题**: 各 Agent 返回的回复文本中没有任何标识，用户和开发者无法直观看到
是哪个 Agent 在响应、调用了哪些工具以及工具的返回摘要。

**修改**: 统一将回复内容改为带结构头部的格式：

```
【Agent 名称】
[工具调用] tool_name(参数) → 结果摘要

正文内容...
```

各 Agent 具体格式：

| Agent | 头部 | 工具调用行示例 |
|---|---|---|
| SymptomAgent | `【症状咨询 Agent】` | `[工具调用] get_medical_record → 近期主诉：xxx` |
| RecordQueryAgent | `【病历查询 Agent】` | `[工具调用] get_medical_record → 查询到 N 条病历记录` |
| ReportAgent | `【报告解读 Agent】` | `[工具调用] get_medical_report → 查询到 N 份报告` |
| KnowledgeAgent | `【医学知识 Agent】` | `[RAG 检索] similarity_search → 命中 N 篇相关文档` |

**降级时格式**（Milvus 不可用）：
```
【医学知识 Agent】
[RAG 检索] similarity_search → 知识库服务当前不可用，已降级

你好，我在。虽然知识库暂时不可用，我仍可进行基础分诊和健康建议。
```

---

## 历史提交记录

| 提交 SHA | 说明 |
|---|---|
| `adb4ac9` | feat: complete chat memory UX and report OCR docs |
| `6577103` | feat: add medical chat layout and session sidebar |
| `1683888` | feat(report): implement medical report management module |
| `f08a277` | docs: add Chinese comments across ai-agent-fastapi business code |
| `36f6d39` | docs: add Chinese comments to all Spring Boot business code |
| `4fe4d99` | feat: add observability and structured report docs |
| `86903fc` | Implement Report Agent structured parsing with schema and tests |
| `962c02a` | feat: add streaming chat support |
| `d00838a` | feat: add RabbitMQ async AI task pipeline |
| `12724fd` | docs: add router agent testing notes |
