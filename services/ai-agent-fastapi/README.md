# AI Agent FastAPI Service

## Tech Stack
- FastAPI
- LangChain
- Ollama
- Qwen

## Core Capabilities
- Multi-Agent architecture: Router, Symptom, Report, Knowledge
- Tool Calling: built-in tools and external tools registry (get_medical_report, get_medical_record, search_drug, search_department)
- RAG: ingestion, retrieval, reranking, vector store integration
- Chat memory: short-term session memory and long-term memory store

## Tool Calling Features

The system now supports comprehensive Tool Calling mechanism enabling agents to access medical data and knowledge:

### Available Tools
1. **get_medical_report** - Retrieve user's medical reports (blood test, CT, MRI, etc.)
2. **get_medical_record** - Retrieve patient's medical records and history
3. **search_drug** - Search for drug information (usage, dosage, contraindications)
4. **search_department** - Search for medical department information

### Agent Integration
Each agent (Symptom, Report, Knowledge) can invoke tools within their response generation:
- **SymptomAgent**: Calls get_medical_record and search_drug for context-aware advice
- **ReportAgent**: Calls get_medical_report to retrieve and analyze reports
- **KnowledgeAgent**: Calls search_department and search_drug alongside RAG retrieval

### Tool APIs
- `GET /tools/available` - List all available tools
- `POST /tools/execute` - Execute a single tool
- `POST /tools/batch` - Execute multiple tools in batch

## Directory Design

```text
ai-agent-fastapi/
├── app/
│   ├── api/v1/endpoints/              # HTTP APIs: chat, report, knowledge, health, tools
│   ├── agents/
│   │   ├── base/                      # Base agent interface with tool calling capability
│   │   ├── orchestrator/              # Router-driven orchestration and multi-agent flows
│   │   ├── router/                    # Router Agent: intent classification and routing
│   │   ├── symptom/                   # Symptom Agent (with tool integration)
│   │   ├── report/                    # Report Agent (with tool integration)
│   │   └── knowledge/                 # Knowledge Agent (with tool integration and RAG)
│   ├── tools/
│   │   ├── base_tool.py               # Tool base class and definitions
│   │   ├── builtin/                   # Internal callable tools
│   │   ├── external/                  # External tool adapters
│   │   ├── medical/
│   │   │   ├── medical_tools.py       # 4 medical tools implementation
│   │   │   └── __init__.py
│   │   ├── registry/
│   │   │   └── tool_registry.py       # Tool registration and discovery
│   │   ├── executor/
│   │   │   └── tool_executor.py       # Tool execution engine
│   │   └── TOOL_CALLING_DOC.py        # Tool calling documentation
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
│   │   ├── http/
│   │   │   ├── agent_chat.py          # Agent chat request/response
│   │   │   ├── tool_call.py           # Tool calling request/response
│   │   │   └── rag.py                 # RAG request/response
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
│   └── main.py                        # FastAPI startup entrypoint with tool router
├── configs/                           # env-specific configuration files
├── scripts/                           # dev and data pipeline scripts
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── pyproject.toml
└── .env.example
```
