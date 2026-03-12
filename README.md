# patient_agent

医疗智能 Agent 系统（患者侧），当前聚焦以下场景：
- 症状咨询
- 检查报告解读
- 医疗知识问答

## 项目现状

当前仓库已经落地并可运行的核心能力：
- Spring Boot 用户模块：注册、登录、`/me`
- Spring Boot 聊天模块：发送消息、历史分页
- Java -> Python Agent HTTP 调用链
- FastAPI 多 Agent 路由（Symptom/Report/Knowledge）
- RAG 入库与检索接口（Milvus）
- Tool Calling 机制（4 个医疗工具）
- 聊天记忆机制：
  - Redis 短期记忆（会话上下文）
  - MySQL 历史记录（按 `session_id` 可查询）

## 技术栈

- 前端：`React`（目录已预留，当前未落地页面代码）
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
│   └── web-frontend/                  # 前端目录（预留）
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
- `GET /api/v1/chat/sessions/{sessionNo}/messages`

### FastAPI（对后端）
- `POST /agent/chat`
- `GET /agent/sessions/{session_id}/history`
- `POST /rag/ingest`
- `POST /rag/retrieve`
- `GET /tools/available`
- `POST /tools/execute`
- `POST /tools/batch`

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

## 文档索引

- 总体架构：`docs/architecture.md`
- 数据库设计：`docs/database-design.md`
- REST API 设计：`shared/api-contracts/rest-api-design.md`
- FastAPI 模块说明：`services/ai-agent-fastapi/README.md`
- Tool Calling 细节：`TOOL_CALLING_IMPLEMENTATION.md`

## 注意事项

- 仓库已忽略 `.venv` 与 `__pycache__`，避免提交本地环境文件。
- `services/web-frontend` 当前为空目录，前端尚未实现。
- AI 依赖建议使用 `0.3.x` 的 LangChain 兼容线，避免导入路径不兼容问题。
