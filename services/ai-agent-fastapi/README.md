# AI Agent FastAPI Service

Python 多 Agent 服务，负责意图路由、工具调用、RAG 检索和会话记忆。

## Core Capabilities

- Multi-Agent: `RouterAgent` -> `SymptomAgent` / `ReportAgent` / `KnowledgeAgent`
- Tool Calling: 医疗工具统一注册与执行
- RAG: 文档入库与检索（Milvus）
- Chat Memory:
  - Redis: 会话短期上下文
  - MySQL: 历史消息持久化

## Implemented APIs

- `GET /health`
- `POST /agent/chat`
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

### 关键配置（`app/core/settings.py`）

- Redis
  - `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD` `REDIS_DB`
  - `REDIS_CHAT_KEY_PREFIX` `REDIS_CHAT_TTL_SECONDS`
- MySQL
  - `MYSQL_HOST` `MYSQL_PORT` `MYSQL_USER` `MYSQL_PASSWORD` `MYSQL_DATABASE`
- Context
  - `SHORT_TERM_MESSAGE_LIMIT`
  - `CONTEXT_MESSAGE_LIMIT`

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

curl -s -X POST http://127.0.0.1:8000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"session_id":"sess_demo_001","user_id":1,"query":"我咳嗽两周"}'

curl -s "http://127.0.0.1:8000/agent/sessions/sess_demo_001/history?limit=20"
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
│   ├── integrations/                  # redis/mysql connectors
│   ├── schemas/http/                  # request/response models
│   ├── core/                          # settings
│   └── main.py
├── pyproject.toml
└── README.md
```
