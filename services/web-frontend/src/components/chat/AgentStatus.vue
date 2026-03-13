<template>
  <transition name="agent-fade">
    <div v-if="step" class="agent-status">
      <!-- Agent 标签 -->
      <span class="agent-badge" :class="`agent-badge--${step.type}`">
        {{ AGENT_LABELS[step.type] ?? 'AI Agent' }}
      </span>

      <!-- 步骤行：spinner + 文字 + 进度点 -->
      <span class="agent-step-row">
        <span class="agent-spinner">
          <span class="spinner-ring" />
        </span>
        <span class="agent-step-text">{{ step.text }}</span>
        <span class="step-ellipsis">
          <span class="step-dot" />
          <span class="step-dot" />
          <span class="step-dot" />
        </span>
      </span>
    </div>
  </transition>
</template>

<script setup>
defineProps({
  /** { type: string, text: string } | null */
  step: {
    type: Object,
    default: null,
  },
});

const AGENT_LABELS = {
  router:          'Router Agent',
  symptom:         '问诊 Agent',
  report_analysis: '报告解读 Agent',
  knowledge:       '知识库检索 Agent',
  record:          '病历 Agent',
  generating:      'AI Agent',
};
</script>

<style scoped>
/* ── 整体容器 ── */
.agent-status {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 14px;
  margin-bottom: 10px;
  border-radius: 20px;
  background: #f0f6ff;
  border: 1px solid #d6e8ff;
  width: fit-content;
  max-width: 100%;
  font-size: 13px;
}

/* ── Agent 类型标签 ── */
.agent-badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: #ddeeff;
  color: #2558c5;
}

.agent-badge--symptom {
  background: #e6f9f0;
  color: #1a8a5a;
}

.agent-badge--report_analysis {
  background: #fff4e6;
  color: #b85c00;
}

.agent-badge--knowledge {
  background: #f3edff;
  color: #6b3fc7;
}

.agent-badge--record {
  background: #e8f6fe;
  color: #0b6ea8;
}

/* ── 步骤行 ── */
.agent-step-row {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #3a5080;
  min-width: 0;
  flex: 1;
}

.agent-step-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 旋转 spinner ── */
.agent-spinner {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner-ring {
  display: block;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: #3a7ef5;
  border-right-color: #3a7ef5;
  animation: spinRing 0.75s linear infinite;
}

/* ── 进度点（3 小点省略号） ── */
.step-ellipsis {
  display: flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
}

.step-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #6b8ccc;
  animation: dotBounce 1.2s ease-in-out infinite;
}

.step-dot:nth-child(2) { animation-delay: 0.2s; }
.step-dot:nth-child(3) { animation-delay: 0.4s; }

/* ── 动画 ── */
@keyframes spinRing {
  to { transform: rotate(360deg); }
}

@keyframes dotBounce {
  0%, 80%, 100% { transform: scaleY(0.6); opacity: 0.5; }
  40%           { transform: scaleY(1.0); opacity: 1;   }
}

/* ── 出入场过渡 ── */
.agent-fade-enter-active,
.agent-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.agent-fade-enter-from,
.agent-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
