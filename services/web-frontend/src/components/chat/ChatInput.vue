<template>
  <div class="chat-input-area">
    <div class="input-wrapper" :class="{ 'is-focused': inputFocused, 'is-disabled': disabled }">
      <el-input
        v-model="draft"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 6 }"
        :placeholder="placeholder"
        resize="none"
        maxlength="1000"
        show-word-limit
        :disabled="disabled"
        @keydown="handleKeydown"
        @focus="inputFocused = true"
        @blur="inputFocused = false"
      />
      <div class="input-footer">
        <div class="input-hint">
          <span v-if="loading">{{ streamHint || 'AI 正在生成回复，请稍候…' }}</span>
          <span v-else>Enter 发送，Shift+Enter 换行</span>
        </div>
        <el-button
          v-if="loading"
          type="danger"
          plain
          class="stop-btn"
          @click="emitStop"
        >
          停止生成
        </el-button>
        <el-button
          v-else
          type="primary"
          class="send-btn"
          :disabled="disabled || !canSend"
          @click="emitSubmit"
        >
          发送消息
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  streamHint: {
    type: String,
    default: null,
  },
  placeholder: {
    type: String,
    default: '请输入消息内容（Enter 发送，Shift+Enter 换行）',
  },
});

const emit = defineEmits(['submit', 'stop']);

const draft = ref('');
const inputFocused = ref(false);

const canSend = computed(() => draft.value.trim().length > 0);

function emitSubmit() {
  const content = draft.value.trim();
  if (!content || props.disabled || props.loading) return;
  emit('submit', content);
  draft.value = '';
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    emitSubmit();
  }
}

function emitStop() {
  emit('stop');
}
</script>

<style scoped>
.chat-input-area {
  flex-shrink: 0;
  padding: 14px 20px 18px;
  border-top: 1px solid #e4e7ed;
  background: rgba(244, 246, 249, 0.96);
  backdrop-filter: blur(10px);
}

.input-wrapper {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #dce3ed;
  padding: 12px 12px 10px;
  box-shadow: 0 8px 24px rgba(20, 40, 80, 0.05);
  transition: border-color 0.2s, box-shadow 0.2s, opacity 0.2s;
}

.input-wrapper.is-focused {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}

.input-wrapper.is-disabled {
  opacity: 0.88;
}

.input-wrapper :deep(.el-textarea__inner) {
  border: none;
  padding: 0;
  resize: none;
  font-size: 14px;
  line-height: 1.7;
  box-shadow: none;
  background: transparent;
}

.input-wrapper :deep(.el-input__count) {
  right: 0;
}

.input-footer {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f3f6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.input-hint {
  font-size: 12px;
  color: #97a3b6;
}

.send-btn,
.stop-btn {
  min-width: 110px;
  border-radius: 10px;
}
</style>