<template>
  <div class="chat-window">
    <el-scrollbar ref="scrollbarRef" class="messages-scroll">
      <div class="messages-list">
        <!-- 切换 Session 时的历史加载骨架屏 -->
        <div v-if="historyLoading" class="history-loading">
          <div v-for="i in 5" :key="i" class="skeleton-row" :class="i % 2 === 0 ? 'is-right' : 'is-left'">
            <div class="skeleton-avatar" />
            <el-skeleton :rows="1" animated class="skeleton-body" />
          </div>
        </div>

        <div v-else-if="showEmptyState && !messages.length" class="empty-state">
          <div class="empty-icon">⚕️</div>
          <h3 class="empty-title">开始一段新的医疗对话</h3>
          <p class="empty-text">请描述您的症状、检查结果或健康问题，我会基于当前会话为您整理回答。</p>
        </div>

        <!-- 流式回复时，在消息列表末尾显示 Agent 状态 -->
        <div v-if="agentStep" class="agent-status-row">
          <AgentStatus :step="agentStep" />
        </div>

        <div
          v-for="(message, index) in messages"
          :key="`${message.role}-${index}`"
          class="message-row"
          :class="message.role === 'user' ? 'is-user' : 'is-assistant'"
        >
          <div class="message-avatar">
            {{ message.role === 'user' ? '我' : 'AI' }}
          </div>
          <div class="message-bubble-wrap">
            <div class="message-label">
              {{ message.role === 'user' ? '用户' : '医疗 AI 助手' }}
            </div>
            <div class="message-bubble" :class="{ 'is-thinking': message.streaming && !message.content }">
              <template v-if="message.streaming && !message.content">
                <div class="thinking-indicator">
                  <div class="thinking-brain">
                    <span class="thinking-dot"></span>
                    <span class="thinking-dot"></span>
                    <span class="thinking-dot"></span>
                  </div>
                  <span class="thinking-label">AI 正在思考…</span>
                </div>
              </template>
              <template v-else>
                <div
                  v-if="message.role === 'assistant'"
                  class="md-body"
                  :class="{ 'is-streaming': message.streaming }"
                  v-html="renderMarkdown(message.content)"
                />
                <span v-else>{{ message.content }}</span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </el-scrollbar>

    <ChatInput :loading="awaitingReply" :stream-hint="streamHint" @submit="handleSubmit" @stop="emit('stop')" />
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js/lib/core';
import javascript from 'highlight.js/lib/languages/javascript';
import python from 'highlight.js/lib/languages/python';
import java from 'highlight.js/lib/languages/java';
import sql from 'highlight.js/lib/languages/sql';
import json from 'highlight.js/lib/languages/json';
import bash from 'highlight.js/lib/languages/bash';
import xml from 'highlight.js/lib/languages/xml';
import ChatInput from './ChatInput.vue';
import AgentStatus from './AgentStatus.vue';

hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('js', javascript);
hljs.registerLanguage('python', python);
hljs.registerLanguage('java', java);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('json', json);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('sh', bash);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('html', xml);

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: false,
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(code, { language: lang, ignoreIllegals: true }).value}</code></pre>`;
      } catch {/* ignore */}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(code)}</code></pre>`;
  },
});

function renderMarkdown(text) {
  if (!text) return '';
  return md.render(text);
}

const props = defineProps({
  messages: {
    type: Array,
    default: () => [],
  },
  showEmptyState: {
    type: Boolean,
    default: true,
  },
  awaitingReply: {
    type: Boolean,
    default: false,
  },
  streamHint: {
    type: String,
    default: null,
  },
  historyLoading: {
    type: Boolean,
    default: false,
  },
  /** { type: string, text: string } | null — 当前 Agent 执行步骤 */
  agentStep: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(['send', 'stop']);

const scrollbarRef = ref(null);

watch(
  () => [
    props.historyLoading,
    props.awaitingReply,
    props.agentStep?.text,
    props.messages.length,
    props.messages.map(message => `${message.role}:${message.content?.length || 0}:${message.streaming ? '1' : '0'}`).join('|'),
  ],
  async () => {
    await nextTick();
    const wrap = scrollbarRef.value?.wrapRef;
    if (wrap) {
      wrap.scrollTop = wrap.scrollHeight;
    }
  },
  { immediate: true }
);

function handleSubmit(content) {
  emit('send', { role: 'user', content });
}
</script>

<style scoped>
.chat-window {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f7f9fc 0%, #eff3f8 100%);
}

.messages-scroll {
  flex: 1;
  min-height: 0;
}

.messages-list {
  min-height: 100%;
  padding: 28px 24px 20px;
}

.empty-state {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #5f6f86;
}

/* ── Agent 状态行 ── */
.agent-status-row {
  padding: 0 0 4px 48px; /* 与 assistant 气泡左边缘对齐：avatar(36) + gap(12) */
}

/* ── 历史加载骨架屏 ── */
.history-loading {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.skeleton-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.skeleton-row.is-right {
  flex-direction: row-reverse;
}

.skeleton-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e4e8f0;
  flex-shrink: 0;
}

.skeleton-body {
  flex: 1;
  max-width: 60%;
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  font-size: 34px;
  background: linear-gradient(135deg, #e8f3ff 0%, #f3fbf2 100%);
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.14);
}

.empty-title {
  margin: 0 0 10px;
  font-size: 22px;
  color: #1a2438;
}

.empty-text {
  max-width: 520px;
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #2d6de6;
}

.message-row.is-assistant .message-avatar {
  background: #2d8f6f;
}

.message-bubble-wrap {
  max-width: min(78%, 720px);
}

.message-row.is-user .message-bubble-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-label {
  margin-bottom: 6px;
  font-size: 12px;
  color: #7a889d;
}

.message-bubble {
  padding: 14px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.75;
  word-break: break-word;
  overflow-wrap: anywhere;
  color: #1f2a44;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(31, 42, 68, 0.06);
}

.message-row.is-user .message-bubble {
  color: #ffffff;
  background: linear-gradient(135deg, #2d6de6 0%, #4b89ff 100%);
}

.message-bubble.is-thinking {
  display: inline-block;
  background: linear-gradient(135deg, #f0f7ff, #f7fbff);
  border: 1.5px solid transparent;
  background-clip: padding-box;
  box-shadow: 0 0 0 1.5px rgba(45,110,230,0.18), 0 8px 24px rgba(31, 42, 68, 0.06);
  animation: thinkingBubblePulse 2s ease-in-out infinite;
}

/* 思考指示器整体行 */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0;
}

/* 三点容器 */
.thinking-brain {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}

/* AI 正在思考文字 */
.thinking-label {
  font-size: 13px;
  font-weight: 500;
  color: #4a6fa5;
  letter-spacing: 0.02em;
  animation: thinkingFade 2s ease-in-out infinite;
}

.thinking-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4a8cdb;
  animation: thinkingPulse 1.4s infinite ease-in-out;
}

.thinking-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0.4s;
}

.typing-cursor {
  display: inline-block;
  width: 1px;
  height: 1.1em;
  margin-left: 2px;
  vertical-align: -0.15em;
  background: currentColor;
  animation: cursorBlink 1s steps(1) infinite;
}

/* 流式时在 md-body 末尾追加光标 */
.md-body.is-streaming::after {
  content: '';
  display: inline-block;
  width: 1px;
  height: 1.1em;
  margin-left: 2px;
  vertical-align: -0.15em;
  background: currentColor;
  animation: cursorBlink 1s steps(1) infinite;
}

@keyframes thinkingPulse {
  0%, 80%, 100% {
    transform: scale(0.75) translateY(0);
    opacity: 0.4;
  }
  40% {
    transform: scale(1) translateY(-3px);
    opacity: 1;
  }
}

@keyframes thinkingBubblePulse {
  0%, 100% {
    box-shadow: 0 0 0 1.5px rgba(45,110,230,0.18), 0 8px 24px rgba(31,42,68,0.06);
  }
  50% {
    box-shadow: 0 0 0 3px rgba(45,110,230,0.28), 0 8px 28px rgba(31,42,68,0.10);
  }
}

@keyframes thinkingFade {
  0%, 100% { opacity: 0.7; }
  50%       { opacity: 1;   }
}

@keyframes cursorBlink {
  0%, 49% {
    opacity: 1;
  }
  50%, 100% {
    opacity: 0;
  }
}

/* ── Markdown 正文样式（不加 scoped 选择器，因为 v-html 渲染的内容不受 scoped 影响） */
.message-bubble :deep(.md-body) {
  line-height: 1.75;
}

.message-bubble :deep(.md-body > *:first-child) {
  margin-top: 0;
}

.message-bubble :deep(.md-body > *:last-child) {
  margin-bottom: 0;
}

.message-bubble :deep(.md-body p) {
  margin: 0 0 0.6em;
}

.message-bubble :deep(.md-body strong) {
  font-weight: 700;
}

.message-bubble :deep(.md-body em) {
  font-style: italic;
}

.message-bubble :deep(.md-body ul),
.message-bubble :deep(.md-body ol) {
  padding-left: 1.5em;
  margin: 0.4em 0 0.6em;
}

.message-bubble :deep(.md-body li) {
  margin-bottom: 0.2em;
}

.message-bubble :deep(.md-body h1),
.message-bubble :deep(.md-body h2),
.message-bubble :deep(.md-body h3),
.message-bubble :deep(.md-body h4) {
  margin: 0.8em 0 0.4em;
  font-weight: 700;
  line-height: 1.4;
}

.message-bubble :deep(.md-body h1) { font-size: 1.2em; }
.message-bubble :deep(.md-body h2) { font-size: 1.1em; }
.message-bubble :deep(.md-body h3) { font-size: 1.0em; }

.message-bubble :deep(.md-body table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.6em 0;
  font-size: 13px;
}

.message-bubble :deep(.md-body th),
.message-bubble :deep(.md-body td) {
  border: 1px solid #d0d7e3;
  padding: 6px 10px;
  text-align: left;
}

.message-bubble :deep(.md-body th) {
  background: #f0f4fa;
  font-weight: 600;
}

.message-bubble :deep(.md-body tr:nth-child(even) td) {
  background: #f7f9fc;
}

.message-bubble :deep(.md-body pre.hljs) {
  margin: 0.6em 0;
  border-radius: 8px;
  overflow-x: auto;
  padding: 12px 14px;
  background: #1e2535;
  font-size: 13px;
  line-height: 1.55;
}

.message-bubble :deep(.md-body pre.hljs code) {
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  color: #e8eaf6;
  background: none;
  padding: 0;
}

.message-bubble :deep(.md-body code:not(pre > code)) {
  padding: 2px 5px;
  border-radius: 4px;
  font-size: 0.88em;
  background: #eef2f8;
  color: #d6336c;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
}

.message-bubble :deep(.md-body blockquote) {
  margin: 0.5em 0;
  padding: 6px 12px;
  border-left: 3px solid #2d8f6f;
  background: #f3fbf8;
  color: #47606d;
}

.message-bubble :deep(.md-body a) {
  color: #2d6de6;
  text-decoration: underline;
}

.message-bubble :deep(.md-body hr) {
  border: none;
  border-top: 1px solid #e3e8f0;
  margin: 0.8em 0;
}
</style>