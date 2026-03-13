# Web Frontend

`services/web-frontend` 为患者侧前端工程，基于 Vue 3 + Vite + Pinia + Element Plus 实现。

## 已实现功能

### 1. 聊天主流程

- 创建会话、切换会话、发送消息、停止流式回复。
- 支持调用 Spring Boot SSE 接口并实时渲染 `start/chunk/done/error` 事件。
- 支持 Markdown 渲染与代码高亮（`markdown-it` + `highlight.js`）。

主要文件：
- `src/views/Chat.vue`
- `src/components/chat/ChatWindow.vue`
- `src/components/chat/ChatInput.vue`

### 2. 会话记忆自动加载（新增）

- 新增后端接口：`GET /api/v1/chat/history/{sessionNo}`。
- 切换 Session 时自动拉取历史并缓存到 Pinia。
- 缓存命中时不重复请求，降低接口压力与页面等待时间。

主要文件：
- `src/stores/chat.js`
- `src/api/chat.js`

核心状态模型：

```js
sessions[sessionNo] = {
	messages: [],
	loading: false,
	loaded: false
}
```

### 3. 等待态与 Agent 状态展示（新增）

- AI 未返回正文时显示“AI 正在思考...”动画。
- 根据 SSE 意图和阶段显示 Agent 处理状态：
	- `Router Agent`
	- `问诊 Agent`
	- `报告解读 Agent`
	- `知识库检索 Agent`
	- `病历 Agent`

主要文件：
- `src/components/chat/AgentStatus.vue`
- `src/components/chat/ChatWindow.vue`

### 4. 报告管理页面

- 报告上传、报告列表、报告详情、触发解读。
- 对接后端报告接口，支持查看解读结果与风险等级。

主要文件：
- `src/views/Report.vue`
- `src/api/report.js`
- `src/components/report/*`

## 对接接口

前端主要调用 Spring Boot `/api/v1`：

- 用户：
	- `POST /users/register`
	- `POST /users/login`
	- `GET /users/me`
- 聊天：
	- `POST /chat/sessions`
	- `GET /chat/sessions`
	- `GET /chat/history/{sessionNo}`
	- `GET /chat/sessions/{sessionNo}/messages`
	- `POST /chat/messages/send`
	- `POST /chat/messages/stream`
- 报告：
	- `POST /reports/upload`
	- `GET /reports`
	- `GET /reports/{reportNo}`
	- `POST /reports/{reportNo}/interpret`

## 本地运行

```bash
cd services/web-frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

生产构建：

```bash
cd services/web-frontend
npm run build
```

## 联调前置

- Spring Boot: `http://127.0.0.1:8080`
- FastAPI: `http://127.0.0.1:8000`
- 前端通过 `vite.config.js` 或请求封装中的 base URL 与后端联调。
