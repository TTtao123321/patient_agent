import request, { API_BASE_URL, getAuthHeaders } from '../utils/request';

/**
 * 查询用户会话列表（按最新消息时间倒序）
 * @param {number} userId
 * @param {number} [page=1]
 * @param {number} [pageSize=20]
 */
export function getSessions(userId, page = 1, pageSize = 20) {
  return request({
    url: '/v1/chat/sessions',
    method: 'get',
    params: { userId, page, pageSize },
  });
}

/**
 * 创建新会话（不发送第一条消息）
 * @param {number}  userId
 * @param {string}  [title='新对话']
 * @param {string}  [sceneType='mixed']
 */
export function createSession(userId, title = '新对话', sceneType = 'mixed') {
  return request({
    url: '/v1/chat/sessions',
    method: 'post',
    data: { userId, title, sceneType },
  });
}

/**
 * 异步发送消息
 * @param {object} params
 * @param {string|null} params.sessionNo   null = 创建新会话
 * @param {number}      params.userId
 * @param {string}      params.content
 * @param {string}      [params.sceneType='mixed']
 * @param {string}      [params.title]
 */
export function sendMessage({ sessionNo, userId, content, sceneType = 'mixed', title }) {
  return request({
    url: '/v1/chat/messages/send',
    method: 'post',
    data: { sessionNo, userId, content, sceneType, title },
  });
}

/**
 * 流式发送消息（SSE over fetch）
 * @param {object} params
 * @param {string} params.sessionNo
 * @param {number} params.userId
 * @param {string} params.content
 * @param {string} [params.sceneType='mixed']
 * @param {string} [params.title]
 * @param {(event: { event: string, data: any }) => void} [params.onEvent]
 * @param {AbortSignal} [params.signal]
 */
export async function streamMessage({
  sessionNo,
  userId,
  content,
  sceneType = 'mixed',
  title,
  onEvent,
  signal,
}) {
  const response = await fetch(`${API_BASE_URL}/v1/chat/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...getAuthHeaders(),
    },
    body: JSON.stringify({ sessionNo, userId, content, sceneType, title }),
    signal,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || '流式请求失败');
  }

  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const frames = buffer.split('\n\n');
    buffer = frames.pop() || '';

    for (const frame of frames) {
      const parsedEvent = parseSseEvent(frame);
      if (!parsedEvent) continue;
      onEvent?.(parsedEvent);
    }
  }

  const tail = decoder.decode();
  if (tail) {
    buffer += tail;
  }
  if (buffer.trim()) {
    const parsedEvent = parseSseEvent(buffer);
    if (parsedEvent) {
      onEvent?.(parsedEvent);
    }
  }
}

function parseSseEvent(frame) {
  const lines = frame
    .split('\n')
    .map(line => line.trimEnd())
    .filter(Boolean);

  if (!lines.length) {
    return null;
  }

  let eventName = 'message';
  const dataLines = [];

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
      continue;
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim());
    }
  }

  const rawData = dataLines.join('\n');
  if (!rawData) {
    return { event: eventName, data: {} };
  }

  try {
    return {
      event: eventName,
      data: JSON.parse(rawData),
    };
  } catch {
    return {
      event: eventName,
      data: rawData,
    };
  }
}

/**
 * 查询会话历史消息（分页）
 * @param {string} sessionNo
 * @param {number} [page=1]
 * @param {number} [pageSize=50]
 */
export function getSessionMessages(sessionNo, page = 1, pageSize = 50) {
  return request({
    url: `/v1/chat/sessions/${sessionNo}/messages`,
    method: 'get',
    params: { page, pageSize },
  });
}

/**
 * 一次性获取会话完整历史（最多 200 条），切换 Session 时使用
 * @param {string} sessionNo
 */
export function getHistory(sessionNo) {
  return request({
    url: `/v1/chat/history/${sessionNo}`,
    method: 'get',
    params: { page: 1, pageSize: 200 },
  });
}
