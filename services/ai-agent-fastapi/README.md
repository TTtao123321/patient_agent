# AI Agent FastAPI Service

Python 多 Agent 服务，负责意图路由、工具调用、RAG 检索、会话记忆、RabbitMQ 消费和流式回答。

## Core Capabilities

- Multi-Agent: `RouterAgent` -> `SymptomAgent` / `ReportAgent` / `KnowledgeAgent` / `RecordQueryAgent`
- Intent Classification: LLM 识别 + 关键词回退
- Tool Calling: 医疗工具统一注册与执行
- RAG: 文档入库与检索（Milvus）
- Chat Memory:
  - Redis: 会话短期上下文
  - MySQL: 历史消息持久化
- Async Tasks:
  - Java 生产 `chat.task.queue`
  - Python 消费任务并回写聊天结果
- Streaming Response:
  - `StreamingResponse` 输出 SSE
  - 流结束后写入会话记忆与聊天历史
- Structured Report Parsing:
  - 报告指标识别（血常规/血脂/血糖/影像）
  - 异常判断与医学解释
  - `ReportAgent` 输出结构化 JSON（Pydantic）
- Observability:
  - 请求追踪（`X-Request-Id`）
  - 请求/Agent/Tool 调用日志
  - 监控指标（JSON + Prometheus）

## Implemented APIs

- `GET /health`
- `GET /metrics`
- `GET /metrics/prometheus`
- `POST /agent/chat`
- `POST /agent/chat/stream`
- `GET /agent/sessions/{session_id}/history`
- `POST /rag/ingest`
- `POST /rag/retrieve`
- `GET /tools/available`
- `POST /tools/execute`
- `POST /tools/batch`

## Tool Calling

已实现 4 个医疗工具：
- `get_medical_report`
- `get_medical_record`
- `search_drug`
- `search_department`

详细说明见：`app/tools/TOOL_CALLING_DOC.py`。

## Chat Memory

聊天记忆链路：
1. `POST /agent/chat` 接收 `session_id`
2. 从 Redis 读取短期上下文并拼接到当前问题
3. 调用 Router/下游 Agent 生成回答
4. 用户消息与助手消息写入：
   - Redis 列表（短期）
   - MySQL `agent_chat_history`（长期）
5. 可通过 `GET /agent/sessions/{session_id}/history` 查询历史

## Async Task Consumer

异步任务链路：
1. Java 后端把 `session_id`、`user_id`、`message` 投递到 RabbitMQ `chat.task.queue`
2. FastAPI 启动时自动拉起消费者线程
3. 消费者复用 `ChatProcessor` 走 Router Agent 和记忆逻辑
4. 处理完成后：
  - 写入 Redis/MySQL 记忆
  - 回写 Spring Boot 使用的 `chat_message` / `chat_session` 表

### RabbitMQ 关键配置

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `RABBITMQ_VHOST`
- `RABBITMQ_CHAT_TASK_QUEUE`
- `RABBITMQ_CONSUMER_ENABLED`

## Streaming Chat

流式聊天链路：
1. `POST /agent/chat/stream` 接收 `session_id`、`user_id`、`query`
2. 先完成意图识别和 Router Agent 草稿生成
3. 再使用 Ollama/Qwen 以流式方式逐步生成最终文本
4. 返回 SSE：`start`、`chunk`、`done`
5. 流结束后把最终回答写回记忆存储

说明：`report_analysis` 场景中若草稿为 JSON，流式链路会绕过二次 LLM 润色，以保证结构化 JSON 不被破坏。

## Structured Report Parsing

核心实现：
- 解析服务：`app/services/report/report_structured_parser.py`
- Schema：`app/schemas/report/structured_report.py`
- Agent：`app/agents/report/report_agent.py`

结构化字段：
- `report_type`
- `overall_status`
- `indicators`
- `findings`
- `medical_advice`
- `recent_reports`

支持示例指标：
- 血常规：白细胞、血红蛋白、血小板、中性粒细胞百分比
- 血脂：总胆固醇、低密度脂蛋白、高密度脂蛋白、甘油三酯
- 血糖：空腹血糖

测试：
- 单元测试：`tests/services/test_report_structured_parser.py`（9 个用例）
- 集成测试脚本：`integration_test_report_analysis.sh`

## Observability

### 请求追踪
- 中间件：`app/observability/tracing/request_tracing_middleware.py`
- 每个请求自动生成或接收 `X-Request-Id`
- 响应头回传 `X-Request-Id`

### 日志
- 配置：`app/observability/logging/logging_config.py`
- 日志格式包含 `request_id`
- 关键日志事件：
  - `request_started` / `request_finished`
  - `agent_route_intent` / `agent_route_completed` / `agent_route_failed`
  - `agent_tool_call`
  - 慢调用告警日志

### 监控指标
- JSON 指标：`GET /metrics`
- Prometheus 指标：`GET /metrics/prometheus`
- 指标包括：
  - HTTP 请求总量/错误量/按路径统计
  - Agent 调用总量/失败量
  - Tool 调用总量/失败量

### SSE 事件格式

- `start`：会话信息、命中意图、命中 Agent、上下文条数
- `chunk`：当前文本分片
- `done`：完整回答、意图、Agent

### 关键配置（`app/core/settings.py`）

- Redis
  - `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD` `REDIS_DB`
  - `REDIS_CHAT_KEY_PREFIX` `REDIS_CHAT_TTL_SECONDS`
- MySQL
  - `MYSQL_HOST` `MYSQL_PORT` `MYSQL_USER` `MYSQL_PASSWORD` `MYSQL_DATABASE`
- Context
  - `SHORT_TERM_MESSAGE_LIMIT`
  - `CONTEXT_MESSAGE_LIMIT`
- Logging & Monitoring
  - `LOG_LEVEL`
  - `SLOW_REQUEST_THRESHOLD_MS`
  - `SLOW_AGENT_CALL_THRESHOLD_MS`
  - `SLOW_TOOL_CALL_THRESHOLD_MS`

## Local Run

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

## Quick Verify

```bash
curl -s http://127.0.0.1:8000/health
curl -s http://127.0.0.1:8000/metrics
curl -s http://127.0.0.1:8000/metrics/prometheus | head -40

curl -s -X POST http://127.0.0.1:8000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"session_id":"sess_demo_001","user_id":1,"query":"我咳嗽两周"}'

curl -N -X POST http://127.0.0.1:8000/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"session_id":"stream_demo_001","user_id":1,"query":"我最近一直咳嗽，还伴有低烧，怎么办？"}'

curl -s "http://127.0.0.1:8000/agent/sessions/sess_demo_001/history?limit=20"

/Users/taot/Desktop/projects/patient_agent/.venv/bin/python -m pytest tests/services/test_report_structured_parser.py -v
```

## Directory Overview

```text
ai-agent-fastapi/
├── app/
│   ├── api/v1/endpoints/              # agent/rag/tools endpoints
│   ├── agents/                        # router/symptom/report/knowledge
│   ├── tools/                         # tool definition/registry/executor/medical tools
│   ├── rag/                           # ingest/retrieve/vectorstore/embeddings
│   ├── memory/                        # session manager + store
│   ├── integrations/                  # redis/mysql/rabbitmq connectors
│   ├── services/chat/                 # chat processor / stream logic
│   ├── services/report/               # structured report parser
│   ├── schemas/http/                  # request/response models
│   ├── schemas/report/                # structured report schemas
│   ├── observability/                 # logging/tracing/metrics
│   ├── core/                          # settings
│   └── main.py
├── pyproject.toml
└── README.md
```
