# patient_agent 系统架构设计

## 1. 目标与范围
本系统用于为患者提供三类核心能力：
- 症状咨询（初步分诊建议）
- 检查报告解读（结构化解释与风险提示）
- 医疗知识问答（基于医疗知识库的可追溯回答）

系统采用多 Agent 协同模式，核心 Agent 包括：
- Router Agent
- Symptom Agent
- Report Agent
- Knowledge Agent

## 1.1 当前实现状态（2026-03）
已落地模块：
- Spring Boot 用户与聊天模块（注册、登录、发送消息、流式回答、历史查询）
- Java -> FastAPI 的 Agent HTTP / SSE 调用
- Java -> RabbitMQ -> FastAPI 异步任务链路
- FastAPI 路由 Agent、流式回答与 RAG 检索接口
- Tool Calling（4 个医疗工具）
- 聊天记忆机制：Redis 短期记忆 + MySQL 历史记录（支持 `session_id`）
- Router Agent：LLM 意图识别 + 关键词回退，支持 `symptom_consult`、`report_analysis`、`medical_knowledge`、`record_query`

## 2. 总体架构（逻辑视图）

### 2.1 前端层（Web）
- 技术：React（可替换为 Vue）
- 职责：
  - 患者会话 UI、历史会话展示
  - 报告上传（PDF/图片/文本）
  - 回答溯源展示（引用知识片段）
  - 风险提示与就医建议展示

### 2.2 业务后端层（Java Spring Boot）
- 职责：
  - 用户、会话、权限、审计日志
  - 业务编排（会话状态、任务提交、结果归档）
  - 统一 API 网关（对前端暴露）
  - 调用 Python Agent 服务并做熔断、限流、重试
  - 代理流式输出（SSE）
  - 发送 RabbitMQ 异步 AI 任务
- 数据存储：
  - MySQL：用户、会话、问答记录、报告元数据
  - Redis：会话缓存、限流计数、热点问答缓存

### 2.3 AI Agent 层（Python FastAPI）
- Router Agent：
  - 识别用户意图（症状咨询/报告解读/知识问答/病历查询）
  - 选择下游 Agent，必要时多 Agent 并行
- Symptom Agent：
  - 收集症状要素（部位、时长、伴随症状、危险信号）
  - 输出分诊建议与下一步检查建议
- Report Agent：
  - 解析检查报告内容（OCR/文本提取后的结构化）
  - 结合参考区间给出解释与注意事项
- Knowledge Agent：
  - 对接 RAG 检索，生成带来源引用的医学知识回答
- RecordQuery Agent：
  - 处理病历历史和既往记录查询
- Stream Processor：
  - 先生成 Agent 草稿
  - 再调用 LLM 逐步输出最终文本
  - 流结束后统一写回记忆与聊天历史

### 2.4 模型与知识层
- Ollama + Qwen 本地模型：负责推理生成
- RAG 知识库：
  - 文档清洗、切片、Embedding
  - Milvus 向量检索
  - 检索结果重排（可选）

### 2.5 基础设施层
- RabbitMQ：异步任务（报告解析、知识入库、长任务回调）
- Milvus：向量存储与相似检索
- MySQL：结构化业务数据
- Redis：缓存与会话状态
- Ollama：本地模型服务

## 3. 核心调用链（主路径）
1. 前端提交用户问题到 Spring Boot。
2. Spring Boot 记录会话并调用 FastAPI `/agent/chat`。
3. Router Agent 判断意图并分发到 Symptom/Report/Knowledge Agent。
4. 需要知识检索时，Knowledge Agent 调用 RAG Pipeline（Milvus 检索）。
5. Agent 汇总结果，返回结构化响应（answer、risk_level、citations、next_steps）。
6. Spring Boot 持久化结果并返回前端展示。

### 3.1 异步任务链路
1. 前端调用 Spring Boot `POST /api/v1/chat/messages/send`。
2. Spring Boot 落用户消息，并把 `session_id`、`user_id`、`message` 投递到 RabbitMQ。
3. Python FastAPI 消费 `chat.task.queue`。
4. Consumer 复用 `ChatProcessor` 完成 Router、Agent、记忆写入。
5. Python 将 AI 回复回写到 `chat_message` / `chat_session`。
6. 前端通过历史接口轮询获得最终回复。

### 3.2 流式回答链路
1. 前端调用 Spring Boot `POST /api/v1/chat/messages/stream`。
2. Spring Boot 创建 `SseEmitter` 并转发到 FastAPI `POST /agent/chat/stream`。
3. FastAPI 先输出 `start` 事件，再按 `chunk` 持续返回文本片段，最后输出 `done`。
4. Java 后端将 SSE 原样转发给前端。
5. 流结束后，Spring Boot 保存完整 AI 消息到聊天表。

## 4. 非功能设计
- 安全：
  - 医疗数据脱敏、传输加密、操作审计
  - 敏感提示词与违规输出拦截
- 可靠性：
  - Spring Boot 对 FastAPI 调用增加超时、重试、熔断
  - RabbitMQ 解耦耗时任务
  - 流式与异步两条链路分离，降低互相影响
- 可观测：
  - 日志（trace_id 贯穿）
  - 指标（QPS、延迟、Agent 命中率、检索召回）
- 合规：
  - 回答附免责声明，不替代医生诊断

## 5. 接口边界（建议）
- Spring Boot 对前端：
  - `POST /api/v1/chat`
  - `POST /api/v1/report/upload`
  - `GET /api/v1/session/{id}`
- Spring Boot 对 FastAPI：
  - `POST /agent/chat`
  - `POST /agent/report/interpret`
  - `POST /agent/knowledge/ask`
- FastAPI 对 RAG：
  - `POST /rag/retrieve`
  - `POST /rag/ingest`

## 6. 部署建议
- 本地开发：`docker-compose` 一键拉起 MySQL/Redis/RabbitMQ/Milvus/Ollama。
- 生产建议：
  - Spring Boot 与 FastAPI 独立伸缩
  - Milvus 与 MySQL 使用持久卷和备份策略
  - 关键接口接入 API 网关与鉴权
