# Backend-Springboot 模块详细说明

## 整体概述

`backend-springboot` 是患者 AI 助手系统的 Java 业务后端，基于 Spring Boot 3.3.2 构建，主要职责包括：

- 用户管理（注册、登录、认证）
- 聊天会话与消息持久化
- 医疗报告管理与文本抽取（PDF/OCR）
- 与 FastAPI Agent 服务通信（HTTP + RabbitMQ）
- SSE 流式响应代理转发

---

## 项目结构

```
backend-springboot/
├── src/main/java/com/patientagent/
│   ├── client/agent/              # FastAPI Agent 客户端
│   ├── common/                    # 通用组件
│   ├── config/                    # 配置类
│   ├── modules/                   # 业务模块
│   │   ├── chat/                  # 聊天模块
│   │   ├── medicalrecord/         # 病历模块
│   │   ├── report/                # 报告模块
│   │   └── user/                  # 用户模块
│   ├── tools/                     # 工具类
│   └── PatientAgentApplication.java
├── src/main/resources/
│   ├── db/migration/              # 数据库迁移脚本
│   └── application.yml            # 配置文件
├── http/                          # API 测试文件
└── uploads/reports/               # 报告文件上传目录
```

---

## 核心模块详解

### 1. 用户模块

**文件位置：** `modules/user/`

**主要功能：**
- 用户注册
- 用户登录
- 获取当前用户信息

**关键文件：**
- `UserController.java` - HTTP 入口
- `UserService.java` - 业务逻辑
- `UserEntity.java` - 用户实体
- `UserRepository.java` - JPA Repository

---

### 2. 聊天模块

**文件位置：** `modules/chat/`

**主要功能：**
- 会话创建与管理
- 消息发送（异步 + 流式）
- 历史消息查询

**关键文件：**
- `ChatController.java:72-75` - 流式聊天接口
- `ChatServiceImpl.java:108-133` - 流式聊天实现
- `ChatServiceImpl.java:71-104` - 异步聊天实现
- `AiTaskPublisher.java` - RabbitMQ 任务发布

**核心流程：**

1. **异步模式** (`/messages/send`):
   - 保存用户消息到 MySQL
   - 将任务发布到 RabbitMQ 队列 `chat.task.queue`
   - 立即返回 `QUEUED` 状态给前端

2. **流式模式** (`/messages/stream`):
   - 保存用户消息到 MySQL
   - 在工作线程中调用 FastAPI `/agent/chat/stream`
   - 逐帧转发 SSE 事件给前端
   - 流结束后持久化 Agent 回答

---

### 3. 报告模块

**文件位置：** `modules/report/`

**主要功能：**
- 报告上传（支持图片/PDF）
- 文本抽取（PDFBox + Tesseract OCR）
- 报告列表与详情查询
- AI 解读触发

**关键文件：**
- `ReportController.java:40-60` - 报告上传接口
- `ReportService.java` - 报告业务逻辑

**文本抽取策略：**
- **PDF**: 使用 Apache PDFBox 提取文本
- **图片**: 使用 Tesseract OCR 提取（支持中英文 `chi_sim+eng`）
- **兜底**: 优先使用请求中的 `rawText`，为空时再尝试提取

---

### 4. Agent 客户端

**文件位置：** `client/agent/`

**主要功能：**
- 同步调用 FastAPI Agent
- 流式调用 FastAPI Agent（SSE）
- 请求追踪（透传 `X-Request-Id`）
- 慢调用告警

**关键文件：**
- `AgentHttpClient.java:71-130` - 同步聊天调用
- `AgentHttpClient.java:133-201` - 流式聊天调用
- `AgentHttpClient.java:221-255` - SSE 流解析

**技术选型：**
- 同步调用：Spring `RestTemplate`
- 流式调用：JDK 11+ `HttpClient`（RestTemplate 不支持流式语义）

---

## Agent 调用链详解

### 1. chat() 同步调用链

**整体流程：**
```
前端 → ChatController.sendMessage() 
    ↓
ChatServiceImpl.sendMessage()
    ↓
AgentHttpClient.chat()  ← 核心实现
    ↓
RestTemplate.exchange()
    ↓
FastAPI /agent/chat
    ↓
返回 JSON 响应
    ↓
parseAnswer() 解析 answer 字段
    ↓
返回完整回答字符串
```

**详细步骤：**

1. **URL 构造与时间记录**
   - 构造 FastAPI Agent 的完整 URL：`{baseUrl}/agent/chat`
   - 记录调用开始时间 `startedAt`，用于计算延迟

2. **请求对象构造**
   - 创建 `AgentChatRequest` 对象
   - 设置 `sessionNo`（会话编号，关联上下文）
   - 设置 `userId`（用户ID，权限验证）
   - 设置 `query`（用户提问内容）

3. **请求头设置**
   - `Content-Type: application/json`
   - 从 MDC 获取 `traceId`，通过 `X-Request-Id` 头传递给 FastAPI

4. **HTTP 请求发送**
   - 使用 `RestTemplate.exchange()` 发送 POST 请求
   - 响应类型为 `JsonNode`（灵活的 JSON 解析）

5. **响应验证**
   - 检查状态码是否为 2xx 成功
   - 检查响应体是否不为 null

6. **耗时计算与监控**
   - 计算总耗时 `latencyMs`
   - 记录完成日志
   - 超过 `slowAgentCallThresholdMs`（默认 1500ms）输出警告日志

7. **响应解析**
   - 调用 `parseAnswer()` 从 JSON 中提取 `answer` 字段
   - 兼容两种格式：`{"answer": "..."}` 或 `{"data": {"answer": "..."}}`

8. **异常处理**
   - 捕获 `RestClientException`（连接超时、4xx/5xx 错误等）
   - 包装为 `IllegalStateException` 向上抛出

---

### 2. streamChat() 流式调用链

**整体流程：**
```
前端 → ChatController.streamMessage()
    ↓
ChatServiceImpl.streamMessage()
    ↓
AgentHttpClient.streamChat()  ← 核心实现
    ↓
JDK HttpClient.send()
    ↓
FastAPI /agent/chat/stream
    ↓
返回 SSE 流
    ↓
parseSseStream() 逐帧解析
    ↓
dispatchSseEvent() 分发事件
    ↓
eventConsumer 回调上层
    ↓
ChatServiceImpl 转发给前端 SseEmitter
```

**详细步骤：**

1. **URL 构造与时间记录**
   - 构造 FastAPI Agent 流式接口 URL：`{baseUrl}/agent/chat/stream`
   - 记录调用开始时间 `startedAt`

2. **请求对象构造**
   - 与同步调用相同，创建 `AgentChatRequest` 对象

3. **JSON 序列化**
   - 使用 `ObjectMapper` 将请求对象手动序列化为 JSON 字符串
   - JDK HttpClient 不支持自动对象转换

4. **请求头设置**
   - `Content-Type: application/json`
   - `Accept: text/event-stream`（声明接受 SSE 流式响应）
   - 超时设置：5 分钟（流式响应可能较长）
   - 透传 `X-Request-Id` 链路追踪

5. **HTTP 请求发送**
   - 使用 JDK 11+ `HttpClient.send()` 发送请求
   - 响应体处理器：`HttpResponse.BodyHandlers.ofInputStream()`
   - 这样可以逐块读取 SSE 流，无需等待完整响应

6. **响应验证**
   - 仅验证状态码是否为 2xx
   - 不检查 body 为 null（InputStream 在响应开始时就可用）

7. **SSE 流解析**
   - 调用 `parseSseStream()` 按行解析 SSE 协议
   - SSE 协议规则：
     - 每一行以字段名开头：`event:`、`data:`
     - 空行表示一个事件帧结束
     - 同一事件可以有多个 `data:` 行
     - 默认事件名为 `message`

8. **事件分发**
   - 每解析完一个事件帧，调用 `dispatchSseEvent()`
   - 将 JSON 数据解析为 `JsonNode`
   - 创建 `AgentStreamEvent` 对象
   - 通过 `eventConsumer` 回调转发给上层

9. **完成处理**
   - 流结束后计算总耗时
   - 记录完成日志
   - 检查慢调用并告警

10. **异常处理**
    - `IOException`：连接失败、网络中断、JSON 序列化失败
    - `InterruptedException`：线程被中断，恢复中断状态后抛出异常

---

## 配置与基础设施

### application.yml 配置要点

```yaml
spring:
  datasource:    # MySQL 配置
  data.redis:    # Redis 配置
  rabbitmq:      # RabbitMQ 配置

agent:
  base-url: http://localhost:8000  # FastAPI 地址

app:
  report.ocr:
    tesseract-cmd: /opt/homebrew/bin/tesseract
    lang: chi_sim+eng
  monitoring:
    slow-request-ms: 1200
    slow-agent-call-ms: 1500

management:
  endpoints:  # Actuator 监控端点
```

### RabbitMQ 配置

**文件位置：** `config/rabbitmq/RabbitMQConfig.java:20-68`

- 队列名：`chat.task.queue`
- 持久化：`durable=true`
- 消息序列化：Jackson JSON
- 确认模式：`AUTO_ACK`

---

## 数据库设计

**文件位置：** `resources/db/migration/V1__create_medical_agent_core_tables.sql`

| 表名 | 说明 |
|------|------|
| `patient_user` | 用户表 |
| `medical_record` | 病历表 |
| `medical_report` | 医疗报告表 |
| `chat_session` | 聊天会话表 |
| `chat_message` | 聊天消息表 |

**核心关联：**
- `chat_session` ↔ `patient_user` (用户会话)
- `chat_message` ↔ `chat_session` (会话消息)
- `medical_report` ↔ `medical_record` (报告关联病历)

---

## 可观测性

### 请求追踪

- **Spring Boot**: `RequestTraceFilter` + MDC `traceId`
- **FastAPI**: 接收 `X-Request-Id` 头并回传
- **日志格式**: `[traceId=xxx]`

### 监控

- **Actuator 端点**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- **慢调用告警**: 超过配置阈值的请求/Agent 调用会输出警告日志

---

## 关键接口列表

### 用户接口
- `POST /api/v1/users/register` - 用户注册
- `POST /api/v1/users/login` - 用户登录
- `GET /api/v1/users/me` - 获取当前用户

### 聊天接口
- `GET /api/v1/chat/sessions` - 查询会话列表
- `POST /api/v1/chat/sessions` - 创建会话
- `POST /api/v1/chat/messages/send` - 异步发送消息
- `POST /api/v1/chat/messages/stream` - 流式发送消息（SSE）
- `GET /api/v1/chat/history/{sessionNo}` - 获取会话完整历史
- `GET /api/v1/chat/sessions/{sessionNo}/messages` - 分页查询消息

### 报告接口
- `POST /api/v1/reports/upload` - 上传报告
- `GET /api/v1/reports` - 查询报告列表
- `GET /api/v1/reports/{reportNo}` - 获取报告详情
- `POST /api/v1/reports/{reportNo}/interpret` - 触发 AI 解读

---

## 本地启动

```bash
cd services/backend-springboot
mvn spring-boot:run
```

默认配置：
- 端口：8080
- MySQL：`101.126.81.197:3307`
- Redis：`101.126.81.197:6389`
- RabbitMQ：`101.126.81.197:5672`
- FastAPI：`http://localhost:8000`
