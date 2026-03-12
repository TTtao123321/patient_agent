# patient_agent

医疗智能 Agent 系统（患者侧）项目，用于：
- 症状咨询
- 检查报告解读
- 医疗知识问答

技术栈：
- 前端：React（可替换 Vue）
- 后端：Java Spring Boot
- AI Agent：Python FastAPI
- AI：Ollama + Qwen（本地模型）
- RAG：Milvus 向量库 + 医疗知识库
- 基础设施：MySQL、Redis、RabbitMQ

## 系统架构

系统采用多 Agent 架构：
- `Router Agent`：意图识别与任务路由
- `Symptom Agent`：症状咨询与初步分诊
- `Report Agent`：检查报告结构化解读
- `Knowledge Agent`：RAG 检索增强问答

核心调用链：
1. 前端发起问题/上传报告到 Spring Boot。
2. Spring Boot 管理会话和鉴权，调用 FastAPI Agent 服务。
3. Router Agent 路由到对应 Agent（可并行）。
4. Knowledge Agent 通过 RAG（Milvus）检索知识并生成回答。
5. 结果回写 MySQL/Redis，并返回前端展示。

详细架构说明见：`docs/architecture.md`

## 项目目录结构

```text
patient_agent/
├── README.md
├── docs/
│   └── architecture.md
├── services/
│   ├── web-frontend/                  # React/Vue 前端工程
│   ├── backend-springboot/            # Java 业务后端
│   │   ├── src/main/java/com/patientagent/
│   │   ├── src/main/resources/
│   │   └── src/test/java/com/patientagent/
│   └── ai-agent-fastapi/              # Python 多 Agent 服务
│       ├── app/
│       │   ├── api/                   # FastAPI 路由层
│       │   ├── agents/                # Router/Symptom/Report/Knowledge Agent
│       │   ├── core/                  # 配置、日志、安全、中间件
│       │   ├── rag/                   # 检索、重排、知识入库
│       │   └── models/                # 请求/响应模型
│       └── tests/
├── infra/
│   ├── docker/                        # compose 与部署脚本
│   ├── mysql/init/                    # MySQL 初始化脚本
│   ├── redis/                         # Redis 配置
│   ├── rabbitmq/                      # RabbitMQ 配置
│   ├── milvus/                        # Milvus 配置
│   └── ollama/                        # Ollama 模型配置
├── knowledge_base/
│   ├── raw/                           # 原始医疗文档
│   ├── processed/                     # 清洗和切片后的文档
│   └── vector_indexes/                # 向量索引导出/备份
├── shared/
│   └── api-contracts/                 # 前后端/服务间接口契约
├── scripts/                           # 开发与运维脚本
└── tests/
	├── integration/                   # 跨服务集成测试
	└── e2e/                           # 端到端测试
```

## 下一步建议

1. 先确定 `shared/api-contracts` 中的统一请求响应格式（含 `trace_id`、`citations`、`risk_level`）。
2. 优先落地 `Router Agent` 和 `Knowledge Agent`，尽快跑通最小可用 RAG 闭环。
3. 用 `docker-compose` 拉起 MySQL/Redis/RabbitMQ/Milvus/Ollama 后再接入前后端。
