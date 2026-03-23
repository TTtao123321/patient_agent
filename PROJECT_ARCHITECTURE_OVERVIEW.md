# patient_agent 项目功能与架构说明

## 1. 参与架构记忆设计：短期缓存 + 长期向量检索

### 设计目标
- 面向用户对话（短期记忆）和业务数据（病例/诊疗记录）的统一记忆能力
- 实现“短期缓存 + 长期向量检索”记忆架构
- 支持用户画像、关键事件抽取、定期刷新、个性化回答风格配置，并对外开放标准化记忆 API

### 本项目具体实现点
- **短期记忆**：前端会话切换时读取 `GET /api/v1/chat/history/{sessionNo}`，后端 Redis 缓存会话上下文（Spring Boot 与 Python Agent 共同使用 Redis）
- **长期记忆**：RAG 检索模块（FastAPI `rag/retrieve` + Milvus 向量索引）实现医疗知识和病例检索
- **关键事件抽取**：报告解析模块（`Report Agent`）可提取结构化指标（如白细胞、血糖），并判断异常；聊天记录中自动抽取用户病情信息
- **个性化回答风格**：Agent 路由层（Router Agent）根据意图与用户上下文选择不同 Agent，同时支持对话场景参数（`sceneType`）与默认提示策略
- **标准记忆 API**：`/agent/sessions/{session_id}/history`、`/rag/ingest`、`/rag/retrieve` 等接口为上层调用提供标准化记忆检索和存储能力

---

## 2. 模块化 MCP Server：业务域拆分 + 自动路由工具调用

### 设计目标
- 封装病例查询、就诊记录调取、身份验证等内部工具
- 按业务域拆分多模块 MCP Server
- 通过 Agent 调度实现工具自动路由调用，增强可扩展性和数据调用准确性

### 本项目具体实现点
- **多 Agent 模块**：FastAPI 目录 `app/agents/` 包含 `symptom`、`report`、`knowledge`、`record` 等业务模块
- **工具调用框架**：`tools` 目录内实现 Tool Calling（`tools/builtin` 与 `tools/external`），Agent 根据意图自动选择工具并执行
- **Router Agent 调度**：`agents/router` 实现意图识别及关键词回退机制，决定具体业务 Agent
- **身份验证与会话**：Spring Boot 端用户模块（注册/登录）+ Token 认证（`/api/v1/users/me`）与 AI Agent 会话信息整合
- **可扩展边界**：通过 FastAPI 路由与 Java 后端转发（`/api/v1/chat/messages/send`，异步队列）实现“业务域与工具独立，路由统一”

---

## 3. 多模态交互能力建设：语音播报 + 医学图像理解

### 设计目标
- 集成多模态模型（如 Doubao）实现语音播报，降低老年用户门槛
- 增强医学图像理解，支持影像辅助诊断
- 构建语音、视觉多场景交互体验

### 本项目对应功能
- **报告 OCR 与文本抽取**：Spring Boot 使用 PDFBox 与 Tesseract OCR 自动提取报告文本（图片/PDF），是多模态读取的核心能力
- **医学图像理解**：当前实现以 OCR 文本抽取为主，可扩展到图像模型分析（后续可接入 Doubao/其他视觉模型）
- **语音播报**：可在前端加入 TTS 组件，后端提供结构化解释输出（`/api/v1/reports/{reportNo}/interpret`）；本项目已具备输出标准化文本，可直接用于多模态播放

---

## 4. 智能规划与推理：意图识别 + ReAct/CoT + RAG 召回

### 设计目标
- 基于 GPT-4o 实现用户意图识别
- 融合 ReAct、思维链（CoT）和自我一致性策略
- 依托 RAG 召回真实诊疗数据完成多轮推理与行动规划
- 提升任务执行准确率与逻辑一致性

### 本项目实现点
- **意图识别**：Router Agent 使用 LLM 判断用户输入属于问诊、报告、知识、病历等场景
- **RAG 召回**：FastAPI RAG 模块（`rag/ingest`/`rag/retrieve`）从 Milvus 向量数据库检索真实医学知识和历史记录
- **思维链与规划**：Agent 在执行逻辑中嵌入工具调用与分步推理（`ReAct` 风格）；例如在报告解读流程先抽取指标再判断异常再给建议
- **自我一致性**：通过多轮对话上下文与历史消息复查，前端展示“已加载会话->无需重复请求”，提高回答一致性

---

## 5. 模块化架构设计：配置化与插拔式，高复用性与跨院迁移

### 设计目标
- 通过配置化与插件式设计，实现记忆模块与 MCP Server 高复用
- 支持跨院用户数据与智能体配置快速迁移
- 提升系统灵活性与多院区部署效率

### 本项目架构实现点
- **配置化**：Spring Boot `application.yml` 与 FastAPI `app/core/settings.py` 支持模型地址、数据库、RabbitMQ、Redis 等可配置参数
- **插拔式 Agent**：FastAPI 多 Agent 模块可按业务需求拆分或扩展（如新增药物问答、检验结果模块）
- **数据迁移**：RAG 向量检索与用户会话存储分层（Redis 会话 + Milvus 向量）易于跨环境导出导入
- **部署适配**：前端、Java 后端、Python Agent 三层解耦，支持独立扩展与横向伸缩。

---

## 结论
本项目已经落地了一个面向患者场景的智能医疗 Agent 平台：
- 包含问诊、报告解读、知识问答
- 集成流式回答、异步任务、工具调用、RAG 检索
- 实现了短期会话记忆与长期检索记忆
- 具备模块化 MCP Server 和 Agent 调度能力
- 支持多端（Web 前端）与多语言后端（Java/Python）协同

如需落地“Doubao 多模态语音 + 医学影像深度理解”可以在现有 OCR+RAG 架构基础上，增加多模态推理层与 TTS 播报层，继续复用当前 Agent 路由与工具调用框架。