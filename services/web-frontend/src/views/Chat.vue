<template>
  <MainLayout :show-aside="showAside">
    <!-- ── 左：会话历史 ── -->
    <template #sidebar>
      <Sidebar
        ref="sidebarRef"
        :active-session-no="chatStore.activeSessionNo"
        @select="handleSelectSession"
        @new-chat="handleNewChat"
      />
    </template>

    <!-- ── 中：聊天窗口 ── -->
    <div class="chat-main">
      <div v-if="!chatStore.currentMessages.length && !chatStore.activeSessionNo && !chatStore.historyLoading" class="chat-welcome">
        <div class="welcome-content">
          <div class="welcome-icon">⚕️</div>
          <h2 class="welcome-title">你好，我是您的医疗 AI 助手</h2>
          <p class="welcome-sub">请描述您的症状或问题，我将为您提供专业医疗建议</p>
          <div class="quick-tips">
            <div
              v-for="tip in quickTips"
              :key="tip"
              class="quick-tip-chip"
              @click="fillInput(tip)"
            >
              {{ tip }}
            </div>
          </div>
        </div>
      </div>
      <ChatWindow
        :messages="chatStore.currentMessages"
        :show-empty-state="false"
        :awaiting-reply="awaitingReply"
        :stream-hint="streamHint"
        :history-loading="chatStore.historyLoading"
        :agent-step="agentStep"
        @send="handleSend"
        @stop="handleStop"
      />
    </div>

    <!-- ── 右：报告信息面板 ── -->
    <template #aside>
      <div class="report-panel">
        <div class="panel-header">
          <span class="panel-title">报告信息</span>
          <el-button text size="small" @click="showAside = false">收起</el-button>
        </div>
        <div class="panel-body">
          <div v-if="!currentReports.length" class="empty-state">
            <el-empty description="本次对话暂无关联报告" :image-size="72" />
          </div>
          <div v-else class="reports-list">
            <div v-for="(report, index) in currentReports" :key="index" class="report-card">
              <div class="report-header">
                <div class="report-title">
                  <span class="report-date">{{ report.report_date }}</span>
                  <span class="report-name">{{ report.report_title }}</span>
                </div>
                <el-tag v-if="report.risk_level" :type="getRiskLevelType(report.risk_level)" size="small">
                  {{ report.risk_level }}
                </el-tag>
              </div>
              <div class="report-info">
                <div v-if="report.hospital_name" class="info-item">
                  <span class="label">🏥 医院：</span>
                  <span class="value">{{ report.hospital_name }}</span>
                </div>
                <div v-if="report.department_name" class="info-item">
                  <span class="label">🏢 科室：</span>
                  <span class="value">{{ report.department_name }}</span>
                </div>
                <div v-if="report.report_type" class="info-item">
                  <span class="label">📋 类型：</span>
                  <span class="value">{{ report.report_type }}</span>
                </div>
              </div>
              <div v-if="report.interpretation_summary" class="report-summary">
                <div class="summary-title">📝 摘要</div>
                <div class="summary-content">{{ report.interpretation_summary }}</div>
              </div>
              <el-collapse v-if="report.raw_text">
                <el-collapse-item title="查看完整报告">
                  <div class="raw-text">{{ report.raw_text }}</div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </div>
      </div>
    </template>
  </MainLayout>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue';
import { ElMessage } from 'element-plus';
import MainLayout from '../components/layout/MainLayout.vue';
import Sidebar from '../components/layout/Sidebar.vue';
import ChatWindow from '../components/chat/ChatWindow.vue';
import { createSession, streamMessage } from '../api/chat';

import { useUserStore } from '../stores/user';
import { useChatStore } from '../stores/chat';

const showAside = ref(true);
const userStore = useUserStore();
const chatStore = useChatStore();
const sidebarRef = ref(null);
const awaitingReply = ref(false);
const streamHint = ref(null);
/** 当前 Agent 步骤 { type, text } | null */
const agentStep = ref(null);
/** 当前会话关联的报告列表 */
const currentReports = ref([]);
let latestSessionRequestId = 0;
let activeStreamController = null;

const quickTips = [
  '我最近持续咳嗽，偶有低烧',
  '头痛头晕，血压偏高',
  '帮我解读一下血常规报告',
  '最近睡眠不好，容易疲劳',
];

onBeforeUnmount(() => {
  abortActiveStream();
});

function fillInput(text) {
  handleSend({ role: 'user', content: text });
}

async function handleSelectSession(sessionNo) {
  const requestId = nextSessionRequestId();
  abortActiveStream();
  awaitingReply.value = false;
  // switchSession 会设置 activeSessionNo 并按缓存策略拉取历史
  await chatStore.switchSession(sessionNo);
  // 若期间用户又切换了其他会话，本次切换已过期，不做额外处理（store 状态由最新操作覆盖）
  if (requestId !== latestSessionRequestId) return;
}

function handleNewChat(sessionNo) {
  nextSessionRequestId();
  abortActiveStream();
  awaitingReply.value = false;
  if (sessionNo) {
    // 侧边栏直接传来了新建好的 sessionNo
    chatStore.initSession(sessionNo);
  } else {
    chatStore.startNewChat();
  }
}

async function handleSend(message) {
  const content = message?.content?.trim();
  if (!content) return;

  const userId = getCurrentUserId();
  if (!userId) {
    ElMessage.warning('请先登录后再发送消息');
    return;
  }

  const requestId = nextSessionRequestId();
  abortActiveStream();

  let controller = null;
  try {
    const sessionNo = await ensureSessionNo(userId, content, requestId);
    if (!sessionNo || requestId !== latestSessionRequestId) {
      return;
    }

    // 向 store 推入本次对话的两条消息，liveAssistant 是响应式引用
    chatStore.pushMessage(sessionNo, { role: 'user', content });
    const liveAssistant = chatStore.pushMessage(sessionNo, {
      role: 'assistant',
      content: '',
      streaming: true,
    });
    awaitingReply.value = true;
    streamHint.value = 'AI 正在思考您的问题…';
    agentStep.value = { type: 'router', text: '路由中，分析问题类型' };

    controller = new AbortController();
    activeStreamController = controller;

    await streamMessage({
      sessionNo,
      userId,
      content,
      sceneType: 'mixed',
      title: buildSessionTitle(content),
      signal: controller.signal,
      onEvent: ({ event, data }) => {
        if (requestId !== latestSessionRequestId) {
          return;
        }

        if (event === 'start') {
          streamHint.value = getIntentHint(data?.intent);
          agentStep.value = getAgentStep(data?.intent, 'analyzing');
          return;
        }

        if (event === 'chunk') {
          if (!liveAssistant.content) {
            streamHint.value = 'AI 正在生成回复…';
            agentStep.value = getAgentStep(agentStep.value?.type, 'generating');
          }
          liveAssistant.content += getChunkContent(data);
          return;
        }

        if (event === 'done') {
          const finalAnswer = getDoneContent(data);
          if (finalAnswer) {
            liveAssistant.content = finalAnswer;
          }
          liveAssistant.streaming = false;
          awaitingReply.value = false;
          streamHint.value = null;
          agentStep.value = null;
          sidebarRef.value?.refresh?.();
          // 保存报告数据
          if (data?.reports && Array.isArray(data.reports)) {
            currentReports.value = data.reports;
          }
          return;
        }

        if (event === 'error') {
          throw new Error(getErrorMessage(data));
        }
      },
    });

    if (requestId === latestSessionRequestId) {
      liveAssistant.streaming = false;
      awaitingReply.value = false;
      streamHint.value = null;
      agentStep.value = null;
      sidebarRef.value?.refresh?.();
    }
  } catch (error) {
    if (error?.name === 'AbortError' || requestId !== latestSessionRequestId) {
      return;
    }

    const msgs = chatStore.currentMessages;
    const lastMessage = msgs[msgs.length - 1];
    if (lastMessage?.role === 'assistant' && lastMessage.streaming) {
      lastMessage.streaming = false;
      lastMessage.content = lastMessage.content || 'AI 回复中断，请稍后重试。';
    } else {
      const sessionNo = chatStore.activeSessionNo;
      if (sessionNo) {
        chatStore.pushMessage(sessionNo, { role: 'assistant', content: 'AI 回复中断，请稍后重试。' });
      }
    }
    awaitingReply.value = false;
    streamHint.value = null;
    agentStep.value = null;
    ElMessage.error(error?.message || '发送消息失败，请稍后重试');
  } finally {
    if (controller && activeStreamController === controller) {
      activeStreamController = null;
    }
  }
}

function nextSessionRequestId() {
  latestSessionRequestId += 1;
  return latestSessionRequestId;
}

function abortActiveStream() {
  if (activeStreamController) {
    activeStreamController.abort();
    activeStreamController = null;
  }
}

function getCurrentUserId() {
  return userStore.userInfo?.id || userStore.userInfo?.userId || null;
}

function buildSessionTitle(content) {
  return content.trim().slice(0, 18) || '新对话';
}

async function ensureSessionNo(userId, content, requestId) {
  if (chatStore.activeSessionNo) {
    return chatStore.activeSessionNo;
  }

  const response = await createSession(userId, '新对话', 'mixed');
  if (requestId !== latestSessionRequestId) {
    return null;
  }

  const sessionNo = response?.data?.data?.sessionNo;
  if (!sessionNo) {
    throw new Error('创建会话失败');
  }

  // 注册到 store（标记已加载、激活）
  chatStore.initSession(sessionNo);
  sidebarRef.value?.refresh?.();
  return sessionNo;
}

// loadSessionMessages 已由 chatStore.switchSession 取代，此处删除

function handleStop() {
  abortActiveStream();
  const msgs = chatStore.currentMessages;
  const lastMessage = msgs[msgs.length - 1];
  if (lastMessage?.role === 'assistant' && lastMessage.streaming) {
    lastMessage.streaming = false;
    if (!lastMessage.content) {
      lastMessage.content = '（已停止生成）';
    }
  }
  awaitingReply.value = false;
  streamHint.value = null;
  agentStep.value = null;
}

function getIntentHint(intent) {
  const INTENT_HINTS = {
    symptom: 'AI 正在分析症状描述…',
    report_analysis: 'AI 正在解读医疗报告…',
    knowledge: 'AI 正在查询医学知识库…',
    record: 'AI 正在查阅病历数据…',
  };
  return INTENT_HINTS[intent] ?? 'AI 正在思考您的问题…';
}

/** 根据 intent + 阶段关键字返回 AgentStep 对象 */
function getAgentStep(intent, phase) {
  const STEP_MAP = {
    symptom: {
      analyzing:  { type: 'symptom',         text: '正在分析症状描述' },
      generating: { type: 'symptom',         text: '生成问诊建议' },
    },
    report_analysis: {
      analyzing:  { type: 'report_analysis', text: '正在解读检验报告' },
      generating: { type: 'report_analysis', text: '生成解读结果' },
    },
    knowledge: {
      analyzing:  { type: 'knowledge',       text: '查询医学指南数据库' },
      generating: { type: 'knowledge',       text: '整理关联知识点' },
    },
    record: {
      analyzing:  { type: 'record',          text: '检索玩家病历数据' },
      generating: { type: 'record',          text: '就病历生成建议' },
    },
  };
  const steps = STEP_MAP[intent];
  if (steps?.[phase]) return steps[phase];
  if (phase === 'generating') return { type: intent ?? 'generating', text: '生成回答' };
  return { type: intent ?? 'router', text: '分析您的问题' };
}

function getChunkContent(data) {
  return data?.content || '';
}

function getDoneContent(data) {
  return data?.answer || data?.content || '';
}

function getErrorMessage(data) {
  return data?.message || data?.error || 'AI 回复失败';
}

function getRiskLevelType(riskLevel) {
  const level = riskLevel?.toLowerCase() || '';
  if (level.includes('high') || level.includes('高')) {
    return 'danger';
  }
  if (level.includes('medium') || level.includes('中')) {
    return 'warning';
  }
  if (level.includes('low') || level.includes('低')) {
    return 'success';
  }
  return 'info';
}
</script>

<style scoped>
.chat-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* ── 欢迎页 ── */
.chat-welcome {
  position: absolute;
  inset: 0 0 132px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
  pointer-events: none;
  z-index: 2;
}
.welcome-content {
  text-align: center;
  max-width: 520px;
  padding: 40px 24px;
  pointer-events: auto;
}
.welcome-icon {
  font-size: 54px;
  margin-bottom: 18px;
  line-height: 1;
}
.welcome-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2438;
  margin: 0 0 10px;
}
.welcome-sub {
  font-size: 15px;
  color: #909399;
  margin: 0 0 28px;
  line-height: 1.7;
}
.quick-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}
.quick-tip-chip {
  padding: 8px 18px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid #dce3ed;
  font-size: 13px;
  color: #3a4a6b;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.quick-tip-chip:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
  border-radius: 8px;
}

/* ── 右侧报告面板 ── */
.report-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.panel-header {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f0f2f5;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a2438;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.reports-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.report-card {
  background: #ffffff;
  border: 1px solid #e8ecf4;
  border-radius: 12px;
  padding: 16px;
}
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.report-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  margin-right: 12px;
}
.report-date {
  font-size: 12px;
  color: #909399;
}
.report-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a2438;
  line-height: 1.4;
}
.report-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.info-item {
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
}
.info-item .label {
  color: #909399;
  flex-shrink: 0;
}
.info-item .value {
  color: #303133;
}
.report-summary {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.summary-title {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 8px;
}
.summary-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.raw-text {
  font-size: 12px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fafafa;
  padding: 12px;
  border-radius: 6px;
  max-height: 300px;
  overflow-y: auto;
}
</style>
