# AI Agent FastAPI Service

## Tech Stack
- FastAPI
- LangChain
- Ollama
- Qwen

## Core Capabilities
- Multi-Agent architecture: Router, Symptom, Report, Knowledge
- Tool Calling: built-in tools and external tools registry
- RAG: ingestion, retrieval, reranking, vector store integration
- Chat memory: short-term session memory and long-term memory store

## Directory Design

```text
ai-agent-fastapi/
├── app/
│   ├── api/v1/endpoints/              # HTTP APIs: chat, report, knowledge, health
│   ├── agents/
│   │   ├── base/                      # Base agent interface and shared runtime
│   │   ├── orchestrator/              # Router-driven orchestration and multi-agent flows
│   │   ├── router/                    # Router Agent: intent classification and routing
│   │   ├── symptom/                   # Symptom Agent
│   │   ├── report/                    # Report Agent
│   │   └── knowledge/                 # Knowledge Agent
│   ├── tools/
│   │   ├── builtin/                   # Internal callable tools
│   │   ├── external/                  # External tool adapters
│   │   └── registry/                  # Tool registration and dispatch
│   ├── rag/
│   │   ├── ingestion/                 # Document cleaning/chunking/index pipeline
│   │   ├── retrieval/                 # Retriever and query expansion
│   │   ├── reranker/                  # Reranking strategy
│   │   ├── embeddings/                # Embedding providers
│   │   └── vectorstore/               # Milvus vector storage adapter
│   ├── memory/
│   │   ├── chat/                      # Conversation context memory
│   │   ├── session/                   # Session memory policy and state
│   │   └── store/                     # Redis/MySQL memory persistence adapters
│   ├── llm/
│   │   ├── ollama/                    # Ollama client and model runtime
│   │   ├── qwen/                      # Qwen model wrappers and prompts
│   │   └── prompts/                   # Prompt templates
│   ├── schemas/
│   │   ├── http/                      # Request/response DTO for APIs
│   │   └── agent/                     # Agent internal schema definitions
│   ├── services/
│   │   ├── chat/                      # Chat application service
│   │   ├── report/                    # Report interpretation service
│   │   └── knowledge/                 # Knowledge QA service
│   ├── integrations/
│   │   ├── milvus/                    # Milvus connector
│   │   ├── redis/                     # Redis connector
│   │   ├── mysql/                     # MySQL connector
│   │   └── rabbitmq/                  # RabbitMQ connector
│   ├── observability/
│   │   ├── logging/                   # Logging setup
│   │   ├── tracing/                   # Trace context utilities
│   │   └── metrics/                   # Metrics exporters
│   ├── workflows/                     # End-to-end workflow definitions
│   └── main.py                        # FastAPI startup entrypoint
├── configs/                           # env-specific configuration files
├── scripts/                           # dev and data pipeline scripts
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── pyproject.toml
└── .env.example
```
