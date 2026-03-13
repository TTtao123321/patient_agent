# patient_agent

医疗智能 Agent 系统（患者侧），当前聚焦以下场景：
- 症状咨询
- 检查报告解读
- 医疗知识问答

## 项目现状

当前仓库已经落地并可运行的核心能力：
- Spring Boot 用户模块：注册、登录、`/me`
- Spring Boot 聊天模块：同步发送、异步发送、SSE 流式回答、历史分页、会话历史批量加载
- Java -> Python Agent HTTP 调用链
- Java -> RabbitMQ -> Python Agent 异步任务链路
- FastAPI 多 Agent 路由（Symptom/Report/Knowledge/Record）
- Router Agent：LLM 意图识别 + 关键词回退
- RAG 入库与检索接口（Milvus）
- Tool Calling 机制（4 个医疗工具）
- 聊天记忆机制：
  - Redis 短期记忆（会话上下文）
  - MySQL 历史记录（按 `session_id` 可查询）
- 流式回答机制：
  - FastAPI `StreamingResponse`
  - Java `SseEmitter` 代理转发
- 前端聊天体验增强（Vue3 + Pinia）：
  - 会话记忆自动加载：切换 Session 自动调用 `/api/v1/chat/history/{sessionNo}`
  - 会话级缓存：已加载会话不重复请求，支持失效重拉
  - 等待态动画：`AI 正在思考...` 动画气泡与流式状态提示
  - Agent 状态可视化：展示 Router/问诊/报告/知识库/病历阶段
- Report Agent 报告结构化解析：
  - 指标抽取（如白细胞、血红蛋白、血脂、血糖）
  - 异常判断（high/low/normal）
  - 医学解释与建议
  - 稳定 JSON 输出（Pydantic Schema）
- 可观测性（新增）：
  - 请求追踪：`X-Request-Id` 贯穿 Java <-> FastAPI
  - 系统日志：请求开始/结束、Agent 路由、Tool 调用、慢调用告警
  - 监控指标：FastAPI `/metrics` + `/metrics/prometheus`，Spring Boot Actuator
- 报告文本抽取增强（新增）：
  - PDF：Spring Boot 使用 PDFBox 自动抽取文本
  - 图片：Spring Boot 调用 Tesseract OCR 抽取文本（支持中文+英文）
  - 兜底解读：`rawText` 为空时会基于文件类型自动尝试提取，再进入 AI 解读

## 技术栈

- 前端：`Vue 3 + Vite + Pinia + Element Plus`
- 业务后端：`Java 17 + Spring Boot 3.3.2`
- AI 服务：`Python FastAPI`
- 模型：`Ollama + Qwen`
- RAG：`LangChain + Milvus + bge-m3`
- 基础设施：`MySQL`、`Redis`、`RabbitMQ`

## 仓库结构

```text
patient_agent/
├── README.md
├── docs/
│   ├── architecture.md
│   └── database-design.md
├── shared/
│   └── api-contracts/
│       └── rest-api-design.md
├── services/
│   ├── backend-springboot/            # Java 业务后端（已实现）
│   ├── ai-agent-fastapi/              # Python Agent 服务（已实现）
│   └── web-frontend/                  # 前端工程（已实现聊天/报告页面）
├── infra/
├── knowledge_base/
├── scripts/
└── tests/
```

## 关键接口

### Spring Boot（对前端）
- `POST /api/v1/users/register`
- `POST /api/v1/users/login`
- `GET /api/v1/users/me`
- `POST /api/v1/chat/messages/send`
- `POST /api/v1/chat/messages/stream`
- `GET /api/v1/chat/sessions/{sessionNo}/messages`
- `GET /api/v1/chat/history/{sessionNo}`
- `POST /api/v1/reports/upload`
- `POST /api/v1/reports/{reportNo}/interpret`

### FastAPI（对后端）
- `POST /agent/chat`
- `POST /agent/chat/stream`
- `GET /agent/sessions/{session_id}/history`
- `GET /metrics`
- `GET /metrics/prometheus`
- `POST /rag/ingest`
- `POST /rag/retrieve`
- `GET /tools/available`
- `POST /tools/execute`
- `POST /tools/batch`

### Spring Boot 可观测接口
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## 本地启动指南

### 1. 后端 Spring Boot

```bash
cd services/backend-springboot
mvn -q -DskipTests compile
mvn spring-boot:run
```

默认读取 `services/backend-springboot/src/main/resources/application.yml`，当前配置为：
- MySQL: `101.126.81.197:3307`
- Redis: `101.126.81.197:6389`
- RabbitMQ: `101.126.81.197:5672`
- Agent base-url: `http://localhost:8000`

监控相关配置：
- `app.monitoring.slow-request-ms`（默认 `1200`）
- `app.monitoring.slow-agent-call-ms`（默认 `1500`）

报告 OCR 配置（图片文本提取）：
- `app.report.ocr.tesseract-cmd`（默认 `tesseract`，当前示例为 `/opt/homebrew/bin/tesseract`）
- `app.report.ocr.lang`（默认 `eng`，当前示例为 `chi_sim+eng`）

### 2. AI Agent FastAPI

```bash
cd services/ai-agent-fastapi
/opt/homebrew/bin/python3.12 -m venv .venv
./.venv/bin/pip install -U pip
./.venv/bin/pip install \
  "fastapi>=0.111.0" "uvicorn[standard]>=0.30.0" \
  "langchain>=0.3.0,<0.4.0" "langchain-community>=0.3.0,<0.4.0" \
  "langchain-huggingface>=0.1.2,<0.2.0" "langchain-milvus>=0.1.6,<0.2.0" "langchain-ollama>=0.2.0,<0.3.0" \
  "pydantic>=2.8.0" "pymilvus>=2.4.0" "pypdf>=4.2.0" \
  "redis>=5.0.0" "pika>=1.3.0" "mysql-connector-python>=9.0.0"
./.venv/bin/python -m uvicorn app.main:app --app-dir . --host 127.0.0.1 --port 8000
```

健康检查：

```bash
curl http://127.0.0.1:8000/health
curl http://127.0.0.1:8000/metrics
curl http://127.0.0.1:8000/metrics/prometheus
```

### 3. 前端 Web（Vue）

```bash
cd services/web-frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

生产构建：

```bash
cd services/web-frontend
npm run build
```

## 多轮记忆验证示例

```bash
SESSION_ID=sess_demo_001

curl -X POST http://127.0.0.1:8000/agent/chat -H 'Content-Type: application/json' \
  -d '{"session_id":"'"$SESSION_ID"'","user_id":1,"query":"我咳嗽两周了"}'

curl -X POST http://127.0.0.1:8000/agent/chat -H 'Content-Type: application/json' \
  -d '{"session_id":"'"$SESSION_ID"'","user_id":1,"query":"晚上更严重"}'

curl "http://127.0.0.1:8000/agent/sessions/$SESSION_ID/history?limit=20"
```

## 异步任务示例

Spring Boot 发送用户消息后，先落用户消息，再把 AI 任务投递到 RabbitMQ，接口立即返回 `QUEUED`。

```bash
curl -X POST http://127.0.0.1:8080/api/v1/chat/messages/send \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"title":"异步问诊","sceneType":"mixed","content":"我最近一直咳嗽两周，还伴有低烧。"}'
```

随后可通过历史接口轮询 AI 回复：

```bash
curl "http://127.0.0.1:8080/api/v1/chat/sessions/{sessionNo}/messages?page=1&pageSize=20"
```

## 流式回答示例

FastAPI 直接流式输出：

```bash
curl -N -X POST http://127.0.0.1:8000/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"session_id":"stream_demo_001","user_id":1,"query":"我最近一直咳嗽，还伴有低烧，怎么办？"}'
```

Java 后端 SSE 代理输出：

```bash
curl -N -X POST http://127.0.0.1:8080/api/v1/chat/messages/stream \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"title":"流式问诊","sceneType":"mixed","content":"我最近一直咳嗽，还伴有低烧，怎么办？"}'
```

SSE 事件包含：
- `start`：返回 `session_id`、`intent`、`agent_used`
- `chunk`：逐步返回文本片段
- `done`：返回最终完整回答

## 前端会话记忆与状态展示

- 会话切换：前端调用 `GET /api/v1/chat/history/{sessionNo}` 一次性加载当前会话历史（默认最多 200 条）
- 缓存策略：`Pinia chat store` 维护 `sessions[sessionNo] -> { messages, loading, loaded }`，命中缓存不重复请求
- 流式状态：发送后先展示“AI 正在思考”，收到 `chunk` 后转为“AI 正在生成回复”
- Agent 进度：基于 SSE `start/chunk/done` 与意图映射显示 `Router Agent / 问诊 Agent / 报告解读 Agent / 知识库检索 Agent / 病历 Agent`

## 报告结构化解析说明

`report_analysis` 意图下，`ReportAgent` 会返回结构化 JSON，核心字段：
- `report_type`：`laboratory` / `imaging` / `general`
- `overall_status`：`normal` / `attention_needed` / `abnormal` / `insufficient_data`
- `indicators`：指标列表（名称、值、参考范围、状态、解释）
- `findings`：影像发现列表
- `medical_advice`：医学建议
- `recent_reports`：关联报告元数据

Schema 位于：`services/ai-agent-fastapi/app/schemas/report/structured_report.py`

### 文件文本提取策略（Spring Boot）

- 上传阶段（`/api/v1/reports/upload`）：
  - 优先使用请求体 `rawText`
  - 若 `rawText` 为空且上传了文件：
    - PDF：使用 PDFBox 抽取文本
    - 图片：调用 Tesseract OCR 抽取文本
    - 文本类文件（txt/md/csv/json）：按 UTF-8 读取
- 解读阶段（`/api/v1/reports/{reportNo}/interpret`）：
  - 若库中 `rawText` 为空，会根据 `fileUrl` 再次尝试 PDF/OCR/文本读取，成功后进入 AI 解读
  - 提取失败或无文本时返回业务错误，避免空内容解读

快速验证：

```bash
cd /Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi
/Users/taot/Desktop/projects/patient_agent/.venv/bin/python -m pytest tests/services/test_report_structured_parser.py -v
cd /Users/taot/Desktop/projects/patient_agent
./integration_test_report_analysis.sh
```

## 日志与追踪说明

- FastAPI：日志自动携带 `request_id`，并在响应头回传 `X-Request-Id`
- Spring Boot：MDC 记录 `traceId`，日志 pattern 输出 `[traceId=...]`
- Java -> FastAPI 调用会自动透传 `X-Request-Id`
- 关键日志事件：
  - `request_started` / `request_finished`
  - `agent_route_intent` / `agent_route_completed` / `agent_route_failed`
  - `agent_tool_call`
  - `slow_request_detected` / `slow_agent_call_detected` / `slow_tool_call_detected`

## 文档索引

- 总体架构：`docs/architecture.md`
- 数据库设计：`docs/database-design.md`
- REST API 设计：`shared/api-contracts/rest-api-design.md`
- FastAPI 模块说明：`services/ai-agent-fastapi/README.md`
- 前端模块说明：`services/web-frontend/README.md`
- Tool Calling 细节：`TOOL_CALLING_IMPLEMENTATION.md`
- Router 优化说明：`ROUTER_AGENT_OPTIMIZATION.md`

## 注意事项

- 仓库已忽略 `.venv` 与 `__pycache__`，避免提交本地环境文件。
- 图片 OCR 依赖系统 `tesseract` 二进制与语言包（示例：`chi_sim`、`eng`）。
- AI 依赖建议使用 `0.3.x` 的 LangChain 兼容线，避免导入路径不兼容问题。
