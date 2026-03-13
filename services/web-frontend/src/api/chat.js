import request from '../utils/request';

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
