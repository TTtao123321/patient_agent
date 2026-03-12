# 医疗智能 Agent 系统 REST API 设计

版本：v1
网关前缀：`/api/v1`

## 1. 统一返回结构

### 1.1 成功返回
```json
{
  "code": 0,
  "message": "OK",
  "trace_id": "4f6d0f2f4a9e4cbe9b8e6f5a7d3c11aa",
  "data": {}
}
```

### 1.2 失败返回
```json
{
  "code": 40001,
  "message": "Invalid parameters",
  "trace_id": "4f6d0f2f4a9e4cbe9b8e6f5a7d3c11aa",
  "data": null
}
```

### 1.3 分页结构
```json
{
  "code": 0,
  "message": "OK",
  "trace_id": "4f6d0f2f4a9e4cbe9b8e6f5a7d3c11aa",
  "data": {
    "items": [],
    "page": 1,
    "page_size": 20,
    "total": 120
  }
}
```

## 2. 用户系统

### 2.1 用户注册
- 接口路径：`POST /api/v1/users/register`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 登录名，3-64 位 |
| password | string | 是 | 明文密码，后端哈希存储 |
| real_name | string | 是 | 真实姓名 |
| gender | int | 否 | 0未知 1男 2女 |
| birth_date | string | 否 | `YYYY-MM-DD` |
| phone | string | 是 | 手机号 |
| email | string | 否 | 邮箱 |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| user_id | long | 用户ID |
| user_no | string | 用户编号 |
| created_at | string | 注册时间 |

### 2.2 用户登录
- 接口路径：`POST /api/v1/users/login`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| access_token | string | 访问令牌 |
| expires_in | int | 过期时间（秒） |
| user | object | 用户信息 |

### 2.3 获取当前用户信息
- 接口路径：`GET /api/v1/users/me`
- 请求方式：`GET`
- 请求参数：无（Header 带 `Authorization: Bearer <token>`）
- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| user_id | long | 用户ID |
| username | string | 用户名 |
| real_name | string | 真实姓名 |
| phone | string | 手机号 |
| status | int | 用户状态 |

## 3. 报告管理

### 3.1 上传医疗报告
- 接口路径：`POST /api/v1/reports/upload`
- 请求方式：`POST`（`multipart/form-data`）
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | PDF/图片/文本文件 |
| report_type | string | 是 | `blood`/`ct`/`mri`/`pathology`/`ultrasound` |
| report_title | string | 是 | 报告标题 |
| report_date | string | 是 | `YYYY-MM-DD HH:mm:ss` |
| hospital_name | string | 否 | 医院名称 |
| department_name | string | 否 | 科室名称 |
| medical_record_id | long | 否 | 关联病历ID |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| report_id | long | 报告ID |
| report_no | string | 报告编号 |
| review_status | string | `PENDING` |
| created_at | string | 创建时间 |

### 3.2 获取报告详情
- 接口路径：`GET /api/v1/reports/{report_id}`
- 请求方式：`GET`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| report_id | long | 是 | 路径参数 |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| report_id | long | 报告ID |
| report_no | string | 报告编号 |
| report_type | string | 报告类型 |
| report_title | string | 报告标题 |
| report_date | string | 报告时间 |
| raw_text | string | 原始文本 |
| parsed_json | object | 结构化结果 |
| interpretation_summary | string | 解读摘要 |
| risk_level | string | 风险等级 |
| review_status | string | 处理状态 |

### 3.3 报告列表
- 接口路径：`GET /api/v1/reports`
- 请求方式：`GET`
- 请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | 否 | 默认 1 |
| page_size | int | 否 | 默认 20 |
| report_type | string | 否 | 报告类型过滤 |
| review_status | string | 否 | 状态过滤 |
| start_date | string | 否 | 开始时间 |
| end_date | string | 否 | 结束时间 |

- 返回结构：分页结构，`items` 每项包含 `report_id`、`report_title`、`report_type`、`report_date`、`risk_level`、`review_status`。

### 3.4 触发报告解读（Agent）
- 接口路径：`POST /api/v1/reports/{report_id}/interpret`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| report_id | long | 是 | 路径参数 |
| mode | string | 否 | `sync` 或 `async`，默认 `async` |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| task_id | string | 异步任务ID |
| report_id | long | 报告ID |
| status | string | `QUEUED` / `RUNNING` / `DONE` |

## 4. 聊天系统

### 4.1 创建会话
- 接口路径：`POST /api/v1/chat/sessions`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| title | string | 否 | 会话标题 |
| scene_type | string | 是 | `symptom`/`report`/`knowledge`/`mixed` |
| medical_record_id | long | 否 | 关联病历ID |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| session_id | long | 会话ID |
| session_no | string | 会话编号 |
| scene_type | string | 会话场景 |
| started_at | string | 创建时间 |

### 4.2 发送消息
- 接口路径：`POST /api/v1/chat/sessions/{session_id}/messages`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| session_id | long | 是 | 路径参数 |
| content | string | 是 | 用户输入内容 |
| message_type | string | 否 | 默认 `TEXT` |
| attachments | array | 否 | 附件列表 |
| context | object | 否 | 额外上下文，如报告ID |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| message_id | long | 用户消息ID |
| agent_message_id | long | Agent 回答消息ID |
| answer | string | 回复内容 |
| intent | string | 路由意图 |
| agent_used | string | 实际处理 Agent |
| risk_level | string | 风险等级 |
| citations | array | 引用来源 |
| next_steps | array | 下一步建议 |

### 4.3 会话消息列表
- 接口路径：`GET /api/v1/chat/sessions/{session_id}/messages`
- 请求方式：`GET`
- 请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| session_id | long | 是 | 路径参数 |
| page | int | 否 | 默认 1 |
| page_size | int | 否 | 默认 50 |

- 返回结构：分页结构，`items` 每项包含 `message_id`、`sender_type`、`content`、`agent_type`、`citations`、`sent_at`。

### 4.4 会话列表
- 接口路径：`GET /api/v1/chat/sessions`
- 请求方式：`GET`
- 请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | 否 | 默认 1 |
| page_size | int | 否 | 默认 20 |
| scene_type | string | 否 | 场景过滤 |
| session_status | string | 否 | 状态过滤 |

- 返回结构：分页结构，`items` 每项包含 `session_id`、`title`、`scene_type`、`last_message_at`、`session_status`。

## 5. Agent 调用（后端对 AI Agent 服务）

说明：本模块是 Spring Boot 调用 FastAPI 的内部 REST 接口，通常不直接暴露给前端。

### 5.1 统一聊天入口（Router）
- 接口路径：`POST /agent/chat`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| trace_id | string | 是 | 链路追踪ID |
| session_no | string | 是 | 会话编号 |
| user_id | long | 是 | 用户ID |
| query | string | 是 | 用户问题 |
| context | object | 否 | 会话上下文 |
| history | array | 否 | 对话历史 |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| answer | string | 回答内容 |
| intent | string | 路由意图 |
| agent_used | string | 命中的 Agent |
| risk_level | string | 风险等级 |
| citations | array | 引用来源 |
| next_steps | array | 下一步建议 |
| memory_patch | object | 记忆更新片段 |

### 5.2 报告解读
- 接口路径：`POST /agent/report/interpret`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| trace_id | string | 是 | 链路ID |
| report_id | long | 是 | 报告ID |
| report_text | string | 否 | 报告文本 |
| parsed_json | object | 否 | 结构化报告 |
| user_profile | object | 否 | 用户画像 |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| interpretation_summary | string | 报告解读 |
| abnormal_items | array | 异常项列表 |
| risk_level | string | 风险等级 |
| suggestions | array | 建议 |

### 5.3 知识问答（RAG）
- 接口路径：`POST /agent/knowledge/ask`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| trace_id | string | 是 | 链路ID |
| query | string | 是 | 问题 |
| top_k | int | 否 | 默认 5 |
| filters | object | 否 | 检索过滤条件 |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| answer | string | 回答 |
| citations | array | 引用片段 |
| retrieval_docs | array | 召回文档元信息 |

### 5.4 Tool Calling 执行
- 接口路径：`POST /agent/tools/execute`
- 请求方式：`POST`
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| trace_id | string | 是 | 链路ID |
| tool_name | string | 是 | 工具名 |
| arguments | object | 是 | 工具参数 |
| timeout_ms | int | 否 | 默认 3000 |

- 返回结构（data）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tool_name | string | 工具名 |
| success | boolean | 是否成功 |
| result | object | 工具执行结果 |
| error | string | 失败时错误信息 |

## 6. 错误码建议

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 40001 | 参数错误 |
| 40101 | 未认证 |
| 40301 | 无权限 |
| 40401 | 资源不存在 |
| 40901 | 资源冲突 |
| 42901 | 请求过载 |
| 50001 | 系统内部错误 |
| 50201 | Agent 服务不可用 |
| 50401 | Agent 服务超时 |
