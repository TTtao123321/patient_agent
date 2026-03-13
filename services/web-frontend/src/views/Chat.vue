<template>
  <MainLayout :show-aside="showAside">
    <!-- ── 左：会话历史 ── -->
    <template #sidebar>
      <Sidebar
        ref="sidebarRef"
        :active-session-no="activeSessionNo"
        @select="handleSelectSession"
        @new-chat="handleNewChat"
      />
    </template>

    <!-- ── 中：聊天窗口 ── -->
    <div class="chat-window">
      <!-- 欢迎页（无活跃会话时） -->
      <div v-if="!activeSessionNo" class="chat-welcome">
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

      <!-- 消息列表区（有活跃会话时） -->
      <el-scrollbar v-else ref="messagesScrollRef" class="messages-scroll">
        <div class="messages-list">
          <!-- TODO: 消息列表将在下一阶段实现 -->
          <p class="messages-placeholder">消息将显示在这里</p>
        </div>
      </el-scrollbar>

      <!-- 输入区（始终显示） -->
      <div class="chat-input-area">
        <div class="input-wrapper" :class="{ 'is-focused': inputFocused }">
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="请描述您的症状或问题（Enter 发送，Shift+Enter 换行）"
            resize="none"
            @keydown="handleKeydown"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
          />
          <div class="input-footer">
            <span class="char-count" :class="{ 'is-warning': inputText.length > 900 }">
              {{ inputText.length }} / 1000
            </span>
            <el-button
              type="primary"
              class="send-btn"
              :disabled="!inputText.trim()"
              @click="handleSend"
            >
              发 送
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 右：报告信息面板 ── -->
    <template #aside>
      <div class="report-panel">
        <div class="panel-header">
          <span class="panel-title">报告信息</span>
          <el-button text size="small" @click="showAside = false">收起</el-button>
        </div>
        <div class="panel-body">
          <el-empty description="本次对话暂无关联报告" :image-size="72" />
        </div>
      </div>
    </template>
  </MainLayout>
</template>

<script setup>
import { ref } from 'vue';
import MainLayout from '../components/layout/MainLayout.vue';
import Sidebar from '../components/layout/Sidebar.vue';

// 布局状态
const showAside = ref(true);

// 会话状态（后续连接 API）
const sidebarRef = ref(null);
const activeSessionNo = ref(null);

// 输入框状态
const inputText = ref('');
const inputFocused = ref(false);
const messagesScrollRef = ref(null);

const quickTips = [
  '我最近持续咳嗽，偶有低烧',
  '头痛头晕，血压偏高',
  '帮我解读一下血常规报告',
  '最近睡眠不好，容易疲劳',
];

function fillInput(text) {
  inputText.value = text;
}

function handleSelectSession(sessionNo) {
  activeSessionNo.value = sessionNo;
}

function handleNewChat(sessionNo) {
  activeSessionNo.value = sessionNo ?? null;
  inputText.value = '';
}

function handleSend() {
  if (!inputText.value.trim()) return;
  // TODO: 调用发送消息 API
  console.log('[Chat] send:', inputText.value);
  inputText.value = '';
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
}
</script>

<style scoped>
/* ── 聊天窗口容器 ── */
.chat-window {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f4f6f9;
}

/* ── 欢迎页 ── */
.chat-welcome {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
}
.welcome-content {
  text-align: center;
  max-width: 520px;
  padding: 40px 24px;
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
}

/* ── 消息区 ── */
.messages-scroll {
  flex: 1;
  min-height: 0;
}
.messages-list {
  padding: 24px;
  min-height: 100%;
}
.messages-placeholder {
  text-align: center;
  color: #c0c4cc;
  font-size: 14px;
  padding: 40px 0;
}

/* ── 输入区 ── */
.chat-input-area {
  flex-shrink: 0;
  padding: 14px 20px 18px;
  background: #f4f6f9;
  border-top: 1px solid #e4e7ed;
}
.input-wrapper {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #dce3ed;
  padding: 12px 12px 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input-wrapper.is-focused {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}
.input-wrapper :deep(.el-textarea__inner) {
  border: none;
  padding: 0;
  resize: none;
  font-size: 14px;
  line-height: 1.65;
  box-shadow: none;
  background: transparent;
}
.input-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f4f4f4;
}
.char-count {
  font-size: 12px;
  color: #c0c4cc;
}
.char-count.is-warning {
  color: #e6a23c;
}
.send-btn {
  min-width: 88px;
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
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
