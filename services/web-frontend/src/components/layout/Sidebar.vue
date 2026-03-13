<template>
  <div class="sidebar">
    <!-- 顶部：新建对话 -->
    <div class="sidebar-header">
      <el-button
        class="new-chat-btn"
        :loading="creating"
        @click="handleNewChat"
      >
        <el-icon class="btn-icon"><Plus /></el-icon>
        新建对话
      </el-button>
    </div>

    <!-- 搜索框 -->
    <div class="sidebar-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索对话"
        clearable
        size="small"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 分组标签 -->
    <div class="sidebar-section-label">历史对话</div>

    <!-- 会话列表 -->
    <el-scrollbar class="sidebar-scroll">
      <!-- 加载中 -->
      <div v-if="loading" class="sidebar-loading">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <span>加载中…</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!filteredSessions.length" class="empty-state">
        <p class="empty-main">{{ searchKeyword ? '未找到相关对话' : '暂无历史对话' }}</p>
        <p v-if="!searchKeyword" class="empty-hint">点击「新建对话」开始问诊</p>
      </div>

      <!-- 会话卡片列表 -->
      <template v-else>
        <div
          v-for="session in filteredSessions"
          :key="session.sessionNo"
          class="session-item"
          :class="{ 'is-active': session.sessionNo === activeSessionNo }"
          @click="handleSelect(session.sessionNo)"
        >
          <div class="session-main">
            <span v-if="session.sceneType" class="scene-tag">
              {{ SCENE_LABELS[session.sceneType] || session.sceneType }}
            </span>
            <div class="session-title">{{ session.title || '新对话' }}</div>
          </div>
          <div class="session-meta">
            <span class="session-time">{{ formatTime(session.lastMessageAt || session.createdAt) }}</span>
            <span
              v-if="session.sessionStatus === 'PROCESSING'"
              class="status-badge processing"
            >处理中</span>
          </div>
        </div>

        <!-- 加载更多 -->
        <div
          v-if="hasMore"
          class="load-more"
          @click="loadMore"
        >
          <span v-if="!loadingMore">加载更多</span>
          <el-icon v-else class="is-loading"><Loading /></el-icon>
        </div>
      </template>
    </el-scrollbar>

    <!-- 底部状态栏 -->
    <div class="sidebar-footer">
      <span class="status-dot" :class="onlineStatusClass"></span>
      <span class="status-text">{{ onlineStatusText }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { Plus, Search, Loading } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getSessions, createSession } from '../../api/chat';
import { useUserStore } from '../../stores/user';

const props = defineProps({
  /** 当前激活的会话编号 */
  activeSessionNo: {
    type: String,
    default: null,
  },
});

const emit = defineEmits(['select', 'new-chat']);

const SCENE_LABELS = {
  mixed:           '综合问诊',
  report_analysis: '报告分析',
  symptom:         '症状咨询',
  knowledge:       '知识查询',
};

const userStore = useUserStore();

// ── 会话列表状态 ──
const sessions    = ref([]);
const loading     = ref(false);
const loadingMore = ref(false);
const creating    = ref(false);
const currentPage = ref(1);
const pageSize    = 20;
const totalCount  = ref(0);
const searchKeyword = ref('');

const hasMore = computed(() => sessions.value.length < totalCount.value);

const filteredSessions = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return sessions.value;
  return sessions.value.filter(s => (s.title || '').toLowerCase().includes(kw));
});

// ── 初始化加载 ──
onMounted(() => {
  fetchSessions(true);
});

async function fetchSessions(reset = false) {
  const userId = userStore.userInfo?.id || userStore.userInfo?.userId;
  if (!userId) return;

  if (reset) {
    loading.value = true;
    currentPage.value = 1;
  } else {
    loadingMore.value = true;
  }

  try {
    const res = await getSessions(userId, currentPage.value, pageSize);
    const data = res?.data?.data;
    if (data) {
      totalCount.value = data.total ?? 0;
      if (reset) {
        sessions.value = data.items ?? [];
      } else {
        sessions.value.push(...(data.items ?? []));
      }
    }
  } catch (e) {
    ElMessage.error('加载会话列表失败');
  } finally {
    loading.value    = false;
    loadingMore.value = false;
  }
}

function loadMore() {
  if (loadingMore.value || !hasMore.value) return;
  currentPage.value++;
  fetchSessions(false);
}

// ── 新建会话 ──
async function handleNewChat() {
  const userId = userStore.userInfo?.id || userStore.userInfo?.userId;
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }
  creating.value = true;
  try {
    const res = await createSession(userId, '新对话', 'mixed');
    const newSession = res?.data?.data;
    if (newSession?.sessionNo) {
      sessions.value.unshift(newSession);
      totalCount.value++;
      emit('new-chat', newSession.sessionNo);
    }
  } catch (e) {
    ElMessage.error('创建会话失败');
  } finally {
    creating.value = false;
  }
}

// ── 切换会话 ──
function handleSelect(sessionNo) {
  emit('select', sessionNo);
}

// ── 对外暴露刷新方法（Chat.vue 发送消息后可调用） ──
function refresh() {
  fetchSessions(true);
}
defineExpose({ refresh });

// ── 时间格式化 ──
function formatTime(timeStr) {
  if (!timeStr) return '';
  const date = new Date(timeStr.replace(' ', 'T'));
  const diff  = Date.now() - date.getTime();
  if (diff < 60_000)         return '刚刚';
  if (diff < 3_600_000)      return `${Math.floor(diff / 60_000)} 分钟前`;
  if (diff < 86_400_000)     return `${Math.floor(diff / 3_600_000)} 小时前`;
  if (diff < 2_592_000_000)  return `${Math.floor(diff / 86_400_000)} 天前`;
  return `${date.getMonth() + 1}月${date.getDate()}日`;
}

// ── 在线状态（可扩展为真实健康检查） ──
const onlineStatusClass = ref('online');
const onlineStatusText  = ref('AI 服务在线');
</script>

<style scoped>
/* ── 容器 ── */
.sidebar {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #1a2438;
  overflow: hidden;
  user-select: none;
}

/* ── 顶部：新建按钮 ── */
.sidebar-header {
  padding: 14px 12px 8px;
  flex-shrink: 0;
}
.new-chat-btn {
  width: 100%;
  height: 38px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  background: #2d6de6;
  border-color: #2d6de6;
  color: #fff;
}
.new-chat-btn:hover {
  background: #3d7ef7;
  border-color: #3d7ef7;
}
.btn-icon {
  margin-right: 4px;
}

/* ── 搜索框 ── */
.sidebar-search {
  padding: 4px 12px 6px;
  flex-shrink: 0;
}
.search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: none;
  border-radius: 6px;
}
.search-input :deep(.el-input__inner) {
  color: #c8d0e0;
  font-size: 13px;
}
.search-input :deep(.el-input__inner::placeholder) {
  color: #4a5a78;
}
.search-input :deep(.el-input__prefix) {
  color: #4a5a78;
}

/* ── 分组标签 ── */
.sidebar-section-label {
  padding: 8px 16px 4px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.8px;
  color: #3a4a60;
  text-transform: uppercase;
  flex-shrink: 0;
}

/* ── 滚动区 ── */
.sidebar-scroll {
  flex: 1;
  min-height: 0;
}

/* ── 加载中 ── */
.sidebar-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 0;
  color: #4a5a78;
  font-size: 13px;
}
.loading-icon {
  font-size: 18px;
}

/* ── 空状态 ── */
.empty-state {
  padding: 36px 16px;
  text-align: center;
}
.empty-main {
  margin: 0 0 6px;
  font-size: 13px;
  color: #5a6a88;
}
.empty-hint {
  margin: 0;
  font-size: 12px;
  color: #3a4a60;
}

/* ── 会话卡片 ── */
.session-item {
  padding: 9px 14px;
  margin: 1px 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.session-item:hover {
  background: rgba(255, 255, 255, 0.07);
}
.session-item.is-active {
  background: rgba(64, 158, 255, 0.18);
}
.session-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.scene-tag {
  display: inline-block;
  align-self: flex-start;
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 3px;
  background: rgba(64, 158, 255, 0.15);
  color: #79b8ff;
  line-height: 1.6;
}
.session-title {
  font-size: 13px;
  color: #c8d0e0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}
.session-item.is-active .session-title {
  color: #79b8ff;
}
.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 3px;
}
.session-time {
  font-size: 11px;
  color: #4a5a78;
}
.status-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  line-height: 1.6;
}
.status-badge.processing {
  background: rgba(230, 162, 60, 0.15);
  color: #e6a23c;
}

/* ── 加载更多 ── */
.load-more {
  text-align: center;
  padding: 10px 0 14px;
  font-size: 12px;
  color: #4a5a78;
  cursor: pointer;
  transition: color 0.15s;
}
.load-more:hover {
  color: #79b8ff;
}

/* ── 底部状态栏 ── */
.sidebar-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-dot.online {
  background: #48bb78;
  box-shadow: 0 0 5px #48bb78;
}
.status-dot.offline {
  background: #fc8181;
  box-shadow: 0 0 5px #fc8181;
}
.status-text {
  font-size: 12px;
  color: #4a5a78;
}
</style>

