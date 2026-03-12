# patient_agent 数据库设计

数据库：MySQL
目标库：patient_agent
字符集：utf8mb4
存储引擎：InnoDB

## 1. 用户表 patient_user

### 表用途
存储系统用户基础信息、登录信息和状态信息。

### 主键
- `id`

### 主要索引
- `uk_patient_user_user_no`：用户编号唯一索引
- `uk_patient_user_username`：用户名唯一索引
- `uk_patient_user_phone`：手机号唯一索引
- `idx_patient_user_status`：状态索引
- `idx_patient_user_created_at`：创建时间索引

### 字段说明
| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 用户主键 |
| user_no | VARCHAR(32) | 用户编号 |
| username | VARCHAR(64) | 登录账号 |
| password_hash | VARCHAR(255) | 密码哈希 |
| real_name | VARCHAR(64) | 真实姓名 |
| gender | TINYINT | 性别 |
| birth_date | DATE | 出生日期 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(128) | 邮箱 |
| id_card_no | VARCHAR(32) | 身份证号 |
| status | TINYINT | 账号状态 |
| last_login_at | DATETIME | 最后登录时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除标记 |
| deleted_at | DATETIME | 删除时间 |

## 2. 病历表 medical_record

### 表用途
存储患者病历快照，包括主诉、现病史、既往史等结构化摘要。

### 主键
- `id`

### 外键
- `user_id -> patient_user.id`

### 主要索引
- `uk_medical_record_record_no`：病历编号唯一索引
- `idx_medical_record_user_id`：用户索引
- `idx_medical_record_record_date`：病历时间索引
- `idx_medical_record_user_date`：用户 + 病历时间复合索引

### 字段说明
| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 病历主键 |
| record_no | VARCHAR(32) | 病历编号 |
| user_id | BIGINT | 用户ID |
| patient_name | VARCHAR(64) | 患者姓名快照 |
| age | INT | 年龄 |
| chief_complaint | VARCHAR(512) | 主诉 |
| present_illness | TEXT | 现病史 |
| past_history | TEXT | 既往史 |
| allergy_history | TEXT | 过敏史 |
| family_history | TEXT | 家族史 |
| surgery_history | TEXT | 手术史 |
| medication_history | TEXT | 用药史 |
| diagnosis_summary | TEXT | 诊断摘要 |
| attending_doctor | VARCHAR(64) | 接诊医生 |
| record_date | DATETIME | 病历时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除标记 |
| deleted_at | DATETIME | 删除时间 |

## 3. 医疗报告表 medical_report

### 表用途
存储上传或解析后的检查报告、结构化解析结果和解读结果。

### 主键
- `id`

### 外键
- `user_id -> patient_user.id`
- `medical_record_id -> medical_record.id`

### 主要索引
- `uk_medical_report_report_no`：报告编号唯一索引
- `idx_medical_report_user_id`：用户索引
- `idx_medical_report_record_id`：病历索引
- `idx_medical_report_report_date`：报告时间索引
- `idx_medical_report_status`：报告状态索引
- `idx_medical_report_user_date`：用户 + 报告时间复合索引

### 字段说明
| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 报告主键 |
| report_no | VARCHAR(32) | 报告编号 |
| user_id | BIGINT | 用户ID |
| medical_record_id | BIGINT | 关联病历ID |
| report_type | VARCHAR(32) | 报告类型 |
| report_title | VARCHAR(128) | 报告标题 |
| hospital_name | VARCHAR(128) | 医院名称 |
| department_name | VARCHAR(64) | 科室名称 |
| report_date | DATETIME | 报告时间 |
| source_type | VARCHAR(16) | 来源类型 |
| file_url | VARCHAR(255) | 原文件地址 |
| raw_text | LONGTEXT | 原始文本 |
| parsed_json | JSON | 结构化解析结果 |
| interpretation_summary | TEXT | 解读摘要 |
| risk_level | VARCHAR(16) | 风险等级 |
| review_status | VARCHAR(16) | 报告状态 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除标记 |
| deleted_at | DATETIME | 删除时间 |

## 4. 聊天会话表 chat_session

### 表用途
存储用户一次对话会话的上下文，包括会话场景、当前 Agent 和摘要。

### 主键
- `id`

### 外键
- `user_id -> patient_user.id`
- `medical_record_id -> medical_record.id`

### 主要索引
- `uk_chat_session_session_no`：会话编号唯一索引
- `idx_chat_session_user_id`：用户索引
- `idx_chat_session_record_id`：病历索引
- `idx_chat_session_status`：会话状态索引
- `idx_chat_session_user_last_msg`：用户 + 最后消息时间复合索引

### 字段说明
| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 会话主键 |
| session_no | VARCHAR(32) | 会话编号 |
| user_id | BIGINT | 用户ID |
| medical_record_id | BIGINT | 关联病历ID |
| title | VARCHAR(128) | 会话标题 |
| scene_type | VARCHAR(32) | 会话场景 |
| current_agent | VARCHAR(32) | 当前 Agent |
| session_status | VARCHAR(16) | 会话状态 |
| summary | TEXT | 会话摘要 |
| started_at | DATETIME | 开始时间 |
| last_message_at | DATETIME | 最后消息时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除标记 |
| deleted_at | DATETIME | 删除时间 |

## 5. 聊天消息表 chat_message

### 表用途
存储会话中的每一条消息，包括用户输入、Agent 输出、引用和结构化扩展内容。

### 主键
- `id`

### 外键
- `session_id -> chat_session.id`
- `user_id -> patient_user.id`
- `report_id -> medical_report.id`

### 主要索引
- `uk_chat_message_message_no`：消息编号唯一索引
- `uk_chat_message_session_seq`：会话 + 序号唯一索引
- `idx_chat_message_user_id`：用户索引
- `idx_chat_message_report_id`：报告索引
- `idx_chat_message_session_sent_at`：会话 + 发送时间复合索引

### 字段说明
| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 消息主键 |
| message_no | VARCHAR(32) | 消息编号 |
| session_id | BIGINT | 会话ID |
| user_id | BIGINT | 用户ID |
| report_id | BIGINT | 关联报告ID |
| sender_type | VARCHAR(16) | 发送方类型 |
| agent_type | VARCHAR(32) | Agent 类型 |
| message_type | VARCHAR(16) | 消息类型 |
| sequence_no | INT | 会话内序号 |
| content | LONGTEXT | 消息内容 |
| structured_payload | JSON | 结构化扩展内容 |
| citations | JSON | 引用信息 |
| tokens_in | INT | 输入 token 数 |
| tokens_out | INT | 输出 token 数 |
| sent_at | DATETIME | 发送时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除标记 |
| deleted_at | DATETIME | 删除时间 |

## 6. Agent 会话历史表 agent_chat_history

### 表用途
FastAPI 聊天记忆模块使用的历史消息表，用于按 `session_id` 查询多轮会话历史。

### 主键
- `id`

### 主要索引
- `idx_agent_chat_history_session_created`：会话 + 时间索引
- `idx_agent_chat_history_user_created`：用户 + 时间索引

### 字段说明
| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| session_id | VARCHAR(64) | 会话 ID（FastAPI 侧） |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(16) | 角色：`user`/`assistant` |
| content | TEXT | 消息内容 |
| intent | VARCHAR(64) | 路由意图（assistant 消息可用） |
| agent_used | VARCHAR(64) | 实际处理 Agent（assistant 消息可用） |
| created_at | DATETIME | 创建时间 |

### 备注
- 该表由 FastAPI 运行时自动执行 `CREATE TABLE IF NOT EXISTS` 创建。
- 短期上下文存在 Redis，长期历史落在该表。
