import { computed, reactive, ref } from 'vue';
import { defineStore } from 'pinia';
import { getHistory } from '../api/chat';

/**
 * 将后端 ChatMessageItemResponse 数组映射为前端 message 对象
 * @param {Array} items
 */
function mapItems(items = []) {
  return items.map(item => ({
    role: item.senderType === 'AGENT' ? 'assistant' : 'user',
    content: item.content || '',
    messageNo: item.messageNo,
    sentAt: item.sentAt,
  }));
}

/**
 * 聊天会话 Pinia Store
 *
 * 职责：
 * 1. 记录当前激活的 sessionNo
 * 2. 缓存每个 session 的消息历史（避免重复请求）
 * 3. 提供切换 session / 推消息 / 流式更新 等操作
 */
export const useChatStore = defineStore('chat', () => {
  /** 每个 sessionNo 对应一条记录：{ messages[], loading, loaded } */
  const sessions = reactive({});

  /** 当前激活的 sessionNo，null 表示空白新对话 */
  const activeSessionNo = ref(null);

  // ── 计算属性 ───────────────────────────────────────────────────────────────

  /** 当前会话的消息列表（响应式） */
  const currentMessages = computed(() => {
    if (!activeSessionNo.value) return [];
    return sessions[activeSessionNo.value]?.messages ?? [];
  });

  /** 当前会话是否正在加载历史记录 */
  const historyLoading = computed(() => {
    if (!activeSessionNo.value) return false;
    return sessions[activeSessionNo.value]?.loading ?? false;
  });

  // ── 内部工具 ───────────────────────────────────────────────────────────────

  function ensureSession(sessionNo) {
    if (!sessions[sessionNo]) {
      sessions[sessionNo] = { messages: [], loading: false, loaded: false };
    }
    return sessions[sessionNo];
  }

  // ── 公开 Actions ───────────────────────────────────────────────────────────

  /**
   * 切换到指定会话：设置 activeSessionNo 并按需拉取历史（缓存命中则跳过）
   */
  async function switchSession(sessionNo) {
    activeSessionNo.value = sessionNo;
    if (!sessionNo) return;

    const s = ensureSession(sessionNo);
    if (s.loaded || s.loading) return; // cache hit

    s.loading = true;
    try {
      const res = await getHistory(sessionNo);
      s.messages = mapItems(res?.data?.data?.items ?? []);
      s.loaded = true;
    } catch (e) {
      console.error('[ChatStore] 加载会话历史失败:', e);
      // 保持 loaded=false，下次切换时会重试
    } finally {
      s.loading = false;
    }
  }

  /**
   * 进入空白新对话（不切换到任何现有 session）
   */
  function startNewChat() {
    activeSessionNo.value = null;
  }

  /**
   * 注册一个刚创建的 session（发送消息时新建），标记为已加载、没有历史
   */
  function initSession(sessionNo) {
    sessions[sessionNo] = { messages: [], loading: false, loaded: true };
    activeSessionNo.value = sessionNo;
  }

  /**
   * 向指定 session 追加一条消息
   * @returns 追加后的响应式消息对象（可直接 mutate 触发渲染）
   */
  function pushMessage(sessionNo, message) {
    const s = ensureSession(sessionNo);
    s.messages.push(message);
    // 通过 reactive 代理链访问，返回的是响应式引用
    return s.messages[s.messages.length - 1];
  }

  /**
   * 使指定 session 的历史缓存失效，下次切换时重新拉取
   */
  function invalidateSession(sessionNo) {
    if (sessions[sessionNo]) {
      sessions[sessionNo].loaded = false;
    }
  }

  return {
    sessions,
    activeSessionNo,
    currentMessages,
    historyLoading,
    switchSession,
    startNewChat,
    initSession,
    pushMessage,
    invalidateSession,
  };
});
