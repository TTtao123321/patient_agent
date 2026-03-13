# 医疗报告管理模块

## 概述

医疗报告管理模块负责检查报告的**上传、查询、修改、删除**以及 **AI 智能解读**。
报告可与病历记录建立关联，支持双向查询：通过病历查关联报告列表，也可通过报告反向查询所属病历摘要。

模块位置：`services/backend-springboot/src/main/java/com/patientagent/modules/report/`

---

## 接口总览

所有接口统一前缀 `/api/v1/reports`，响应格式参见 [rest-api-design.md](../shared/api-contracts/rest-api-design.md)。

| 方法   | 路径                                 | 功能描述                                   |
|--------|--------------------------------------|--------------------------------------------|
| POST   | `/upload`                            | 上传检查报告（支持纯文本 / 文件）          |
| GET    | `/`                                  | 分页查询报告列表（支持多维筛选）           |
| GET    | `/{reportNo}`                        | 查询报告详情                               |
| PATCH  | `/{reportNo}`                        | 部分更新报告元信息                         |
| DELETE | `/{reportNo}`                        | 软删除报告                                 |
| POST   | `/{reportNo}/interpret`              | 触发 AI 解读                               |
| GET    | `/{reportNo}/medical-record`         | 查询报告关联的病历摘要                     |

---

## 接口详情

### 1. 上传报告

```
POST /api/v1/reports/upload
Content-Type: multipart/form-data
```

**必填参数**

| 参数          | 类型   | 说明                                         |
|---------------|--------|----------------------------------------------|
| userId        | Long   | 用户 ID                                      |
| reportType    | String | 报告类型（建议规范值：blood / ct / mri / ecg / pathology / other） |
| reportTitle   | String | 报告标题                                     |

至少提供 `rawText`（报告原文文本）或 `file`（上传文件）之一。

**可选参数**

| 参数             | 类型   | 说明                                          |
|------------------|--------|-----------------------------------------------|
| medicalRecordId  | Long   | 关联病历 ID（用于报告与病历双向查询）         |
| hospitalName     | String | 医院名称                                      |
| departmentName   | String | 科室名称                                      |
| reportDate       | String | 报告日期，格式 `yyyy-MM-dd` 或 `yyyy-MM-ddTHH:mm:ss`，缺省取当前时间 |
| rawText          | String | 报告原始文本内容                              |
| file             | File   | 报告文件（支持 .txt .md .csv .json 自动提取文本；其他格式仅保存路径） |

**文件处理逻辑**：若同时传入 `rawText` 和 `file`，优先使用 `rawText`。若只传入文本类型文件，系统自动读取文件内容填充 `rawText` 字段。

**响应示例**

```json
{
  "code": 200,
  "traceId": "...",
  "data": {
    "reportId": 1,
    "reportNo": "R202603131045261234",
    "reportType": "blood",
    "reportTitle": "血常规复查报告",
    "reportDate": "2026-03-13 00:00:00",
    "reviewStatus": "PENDING"
  }
}
```

---

### 2. 分页查询报告列表

```
GET /api/v1/reports?userId=1&page=1&pageSize=20
```

**查询参数**

| 参数             | 必填 | 说明                                               |
|------------------|------|----------------------------------------------------|
| userId           | 是   | 用户 ID（只返回该用户的报告）                      |
| reportType       | 否   | 报告类型过滤（如 `blood`、`ct`）                   |
| riskLevel        | 否   | 风险等级过滤：`LOW` / `MEDIUM` / `HIGH` / `CRITICAL` |
| dateFrom         | 否   | 报告日期起始（闭区间），格式 `yyyy-MM-dd`          |
| dateTo           | 否   | 报告日期截止（闭区间），格式 `yyyy-MM-dd`          |
| medicalRecordId  | 否   | 关联病历 ID（支持从病历侧查所有关联报告）          |
| page             | 否   | 页码，默认 1                                       |
| pageSize         | 否   | 每页条数，默认 20，最大 100                        |

所有筛选参数可自由组合，不传则不过滤，结果按 `reportDate` 倒序排列。

---

### 3. 查询报告详情

```
GET /api/v1/reports/{reportNo}?userId=1
```

返回完整字段，包含 `rawText`、`parsedJson`（AI 解读的结构化 JSON）、`interpretationSummary`。

---

### 4. 部分更新报告

```
PATCH /api/v1/reports/{reportNo}?userId=1
Content-Type: application/json
```

**请求体（所有字段均可选，null 字段不覆盖原值）**

```json
{
  "reportTitle": "新标题",
  "reportType": "mri",
  "hospitalName": "省人民医院",
  "departmentName": "放射科",
  "reportDate": "2026-03-14",
  "rawText": "更新后的报告文本"
}
```

- `hospitalName` / `departmentName` / `rawText` 传入空字符串时，字段会被清空（置为 null）。
- 返回更新后的完整报告详情。

---

### 5. 软删除报告

```
DELETE /api/v1/reports/{reportNo}?userId=1
```

将 `is_deleted` 标记置 1，不物理删除数据库记录。删除后该报告在所有查询接口中不可见。响应 `data` 为 `null`。

---

### 6. AI 解读

```
POST /api/v1/reports/{reportNo}/interpret?userId=1
```

**执行流程**

1. 读取报告的 `rawText`（优先）或从文件路径读取文本内容。
2. 构造提示词，要求模型输出：核心异常点 / 风险分级 / 建议复查项 / 建议就诊科室。
3. 调用 FastAPI `AgentClient.chat()` 获取 AI 回复。
4. 从回复中自动推断风险等级（关键词匹配）。
5. 将解读结果写入 `interpretation_summary` / `risk_level` / `parsed_json`，状态置为 `REVIEWED`。

**风险等级推断规则**

| 等级     | 触发关键词（中英文）                               |
|----------|----------------------------------------------------|
| CRITICAL | critical / 危急 / 紧急 / 立即就医                  |
| HIGH     | high / 高风险 / 明显异常 / 高度怀疑                |
| MEDIUM   | medium / 中风险 / 需复查 / 建议复诊                |
| LOW      | 其他                                               |

**前提**：报告必须存在 `rawText` 或可读取的文本文件，否则返回业务错误。FastAPI AI 服务须已启动（默认 `http://127.0.0.1:8000`）。

---

### 7. 查询关联病历摘要

```
GET /api/v1/reports/{reportNo}/medical-record?userId=1
```

**前提**：报告上传时必须传入 `medicalRecordId`，且对应病历存在且未删除。

返回病历的摘要信息（不含现病史 / 既往史等完整文本字段）：

```json
{
  "code": 200,
  "data": {
    "recordId": 1,
    "recordNo": "MR202603130001",
    "userId": 1,
    "patientName": "张三",
    "age": 45,
    "chiefComplaint": "咳嗽伴低烧两周",
    "diagnosisSummary": "上呼吸道感染",
    "attendingDoctor": "李主任",
    "recordDate": "2026-03-10 09:30:00"
  }
}
```

---

## 数据模型

核心表 `medical_report`，关键字段说明：

| 字段                   | 类型       | 说明                                              |
|------------------------|------------|---------------------------------------------------|
| report_no              | VARCHAR(32)| 报告唯一编号，格式 `R{yyyyMMddHHmmss}{4位随机数}` |
| user_id                | BIGINT     | 归属用户                                          |
| medical_record_id      | BIGINT     | 关联病历 ID（可为 null）                          |
| report_type            | VARCHAR(32)| 报告类型（小写存储）                              |
| source_type            | VARCHAR(16)| `UPLOAD`（文件上传） / `MANUAL`（文本直传）       |
| raw_text               | LONGTEXT   | 报告原始文本                                      |
| parsed_json            | JSON       | AI 解读后的结构化数据                             |
| interpretation_summary | TEXT       | AI 解读摘要全文                                   |
| risk_level             | VARCHAR(16)| `LOW` / `MEDIUM` / `HIGH` / `CRITICAL`           |
| review_status          | VARCHAR(16)| `PENDING`（待解读） / `REVIEWED`（已解读）       |
| is_deleted             | TINYINT    | 逻辑删除标记：0 正常，1 已删除                    |

---

## 代码结构

```
modules/report/
├── controller/
│   └── ReportController.java          # HTTP 端点，路由分发
├── service/
│   ├── ReportService.java             # 服务接口定义
│   └── ReportServiceImpl.java         # 业务逻辑实现
├── repository/
│   └── MedicalReportRepository.java   # JPA + JpaSpecificationExecutor（支持动态筛选）
├── entity/
│   └── MedicalReportEntity.java       # medical_report 表映射
└── dto/
    ├── ReportUploadResponse.java      # 上传响应
    ├── ReportListResponse.java        # 列表响应（含分页信息）
    ├── ReportItemResponse.java        # 列表项（摘要字段）
    ├── ReportDetailResponse.java      # 详情响应（全量字段）
    ├── ReportUpdateRequest.java       # PATCH 请求体
    ├── ReportInterpretResponse.java   # AI 解读响应
    └── MedicalRecordRefResponse.java  # 关联病历摘要响应

modules/medicalrecord/
├── entity/
│   └── MedicalRecordEntity.java       # medical_record 表映射（跨模块只读）
└── repository/
    └── MedicalRecordRepository.java   # 病历只读查询
```

---

## 权限模型

所有接口均通过 `userId` 参数进行用户隔离：

- 查询/修改/删除操作会校验 `report.userId == userId`，不一致时返回业务错误 `No permission`。
- 当前为简单参数鉴权，生产环境建议改为 JWT Token 提取 userId，避免客户端伪造。

---

## 配置项

| 配置键                     | 默认值           | 说明                     |
|---------------------------|-----------------|--------------------------|
| `app.report.upload-dir`   | `uploads/reports` | 文件上传本地存储目录      |

在 `application.yml` 中可覆写：

```yaml
app:
  report:
    upload-dir: /data/patient_agent/reports
```

---

## HTTP 测试文件

完整的回归测试用例见 [report-api.http](../services/backend-springboot/http/report-api.http)，涵盖：

- 正向场景：上传（文本/文件）、筛选查询（7 种组合）、PATCH 更新、DELETE、AI 解读、病历关联查询
- 负向场景：越权查询 / 越权修改 / 越权删除、删除后验证不可见
