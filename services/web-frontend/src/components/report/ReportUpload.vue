<template>
  <div class="report-upload">
    <!-- ── 上传表单卡片 ── -->
    <el-card class="upload-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">上传医疗报告</span>
          <span class="card-sub">支持 PDF 和图片格式，上传后自动 AI 解读</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="88px"
        label-position="left"
        class="upload-form"
        @submit.prevent
      >
        <!-- 报告标题 -->
        <el-form-item label="报告标题" prop="reportTitle">
          <el-input
            v-model="form.reportTitle"
            placeholder="例：2024-03 血常规检查"
            maxlength="80"
            show-word-limit
          />
        </el-form-item>

        <!-- 报告类型 -->
        <el-form-item label="报告类型" prop="reportType">
          <el-select v-model="form.reportType" placeholder="请选择" style="width:100%">
            <el-option
              v-for="opt in REPORT_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <!-- 文件上传区 -->
        <el-form-item label="报告文件" prop="file">
          <el-upload
            ref="uploadRef"
            class="file-upload-area"
            drag
            action=""
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-exceed="handleExceed"
            accept=".pdf,.jpg,.jpeg,.png,.webp,.bmp,.tiff,.tif"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-text">将文件拖拽到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="upload-tip">支持 PDF、JPG、PNG、WEBP 等格式，单文件不超过 20MB</div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 可选信息（折叠） -->
        <el-collapse v-model="expandOptional" class="optional-collapse">
          <el-collapse-item title="填写更多信息（可选）" name="optional">
            <el-form-item label="医院名称">
              <el-input v-model="form.hospitalName" placeholder="例：北京协和医院" maxlength="60" />
            </el-form-item>
            <el-form-item label="科室">
              <el-input v-model="form.departmentName" placeholder="例：检验科" maxlength="40" />
            </el-form-item>
            <el-form-item label="报告日期">
              <el-date-picker
                v-model="form.reportDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width:100%"
              />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>

        <div class="form-actions">
          <el-button
            type="primary"
            :loading="phase === 'uploading'"
            :disabled="phase === 'analyzing'"
            @click="handleSubmit"
          >
            <template v-if="phase === 'uploading'">上传中…</template>
            <template v-else>上传并解读</template>
          </el-button>
          <el-button :disabled="phase !== 'idle'" @click="resetForm">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <!-- ── 进度指示 ── -->
    <el-card v-if="phase !== 'idle'" class="progress-card" shadow="never">
      <el-steps :active="stepActive" class="steps" finish-status="success" align-center>
        <el-step title="上传文件" :status="stepStatus(0)" />
        <el-step title="AI 解读" :status="stepStatus(1)" />
        <el-step title="解读完成" :status="stepStatus(2)" />
      </el-steps>
    </el-card>

    <!-- ── AI 解读结果 ── -->
    <el-card v-if="result" class="result-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">AI 解读结果</span>
          <el-tag :type="riskTagType" size="large" class="risk-tag">{{ riskLabel }}</el-tag>
        </div>
      </template>

      <div class="result-body">
        <div class="result-meta">
          <span class="meta-item"><span class="meta-key">报告编号</span>{{ result.reportNo }}</span>
          <span class="meta-item"><span class="meta-key">审核状态</span>{{ reviewStatusLabel }}</span>
        </div>

        <el-divider />

        <div class="result-summary">
          <h4 class="summary-title">解读摘要</h4>
          <div
            class="summary-content md-body"
            v-html="renderMarkdown(result.interpretationSummary)"
          />
        </div>

        <div class="result-actions">
          <el-button type="primary" plain @click="resetForNext">继续上传</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import MarkdownIt from 'markdown-it';
import { interpretReport, uploadReport } from '../../api/report';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const md = new MarkdownIt({ html: false, linkify: true });

// ── 报告类型选项 ──
const REPORT_TYPE_OPTIONS = [
  { value: 'blood',      label: '血液检验' },
  { value: 'ct',         label: 'CT 检查' },
  { value: 'mri',        label: 'MRI 检查' },
  { value: 'ultrasound', label: '超声 / 彩超' },
  { value: 'ecg',        label: '心电图' },
  { value: 'pathology',  label: '病理报告' },
  { value: 'other',      label: '其他' },
];

// ── 表单状态 ──
const formRef     = ref(null);
const uploadRef   = ref(null);
const expandOptional = ref([]);

const form = reactive({
  reportTitle:    '',
  reportType:     '',
  hospitalName:   '',
  departmentName: '',
  reportDate:     null,
  file:           null,
});

const rules = {
  reportTitle: [{ required: true, message: '请填写报告标题', trigger: 'blur' }],
  reportType:  [{ required: true, message: '请选择报告类型', trigger: 'change' }],
  file:        [{ required: true, message: '请选择要上传的文件', trigger: 'change' }],
};

// ── 流程状态 ── idle / uploading / analyzing / done
const phase  = ref('idle');
const result = ref(null);

// ── 步骤条 ──
const stepActive = computed(() => {
  if (phase.value === 'uploading')  return 0;
  if (phase.value === 'analyzing')  return 1;
  if (phase.value === 'done')       return 2;
  return 0;
});

function stepStatus(step) {
  if (phase.value === 'uploading'  && step === 0) return 'process';
  if (phase.value === 'analyzing'  && step === 0) return 'success';
  if (phase.value === 'analyzing'  && step === 1) return 'process';
  if (phase.value === 'done'       && step <= 1)  return 'success';
  if (phase.value === 'done'       && step === 2) return 'success';
  return 'wait';
}

// ── 风险标签 ──
const RISK_MAP = {
  LOW:      { label: '低风险',  type: 'success' },
  MEDIUM:   { label: '中等风险', type: 'warning' },
  HIGH:     { label: '高风险',  type: 'danger' },
  CRITICAL: { label: '危急值',  type: 'danger' },
};

const riskLabel   = computed(() => RISK_MAP[result.value?.riskLevel]?.label ?? '未评级');
const riskTagType = computed(() => RISK_MAP[result.value?.riskLevel]?.type  ?? 'info');

const REVIEW_STATUS_MAP = {
  PENDING:    '待审核',
  PROCESSING: '解读中',
  DONE:       '已完成',
  FAILED:     '解读失败',
};
const reviewStatusLabel = computed(() => REVIEW_STATUS_MAP[result.value?.reviewStatus] ?? result.value?.reviewStatus ?? '-');

// ── 文件处理 ──
function handleFileChange(file) {
  const raw  = file.raw;
  const maxMB = 20;
  if (raw.size > maxMB * 1024 * 1024) {
    ElMessage.warning(`文件大小不能超过 ${maxMB}MB`);
    uploadRef.value?.clearFiles();
    return;
  }
  form.file = raw;
}

function handleFileRemove() {
  form.file = null;
}

function handleExceed() {
  ElMessage.warning('每次只能上传一个文件，请先移除已选文件');
}

// ── 提交 ──
async function handleSubmit() {
  const userId = userStore.userInfo?.id || userStore.userInfo?.userId;
  if (!userId) {
    ElMessage.warning('请先登录后再上传');
    return;
  }

  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  result.value = null;
  phase.value  = 'uploading';

  let reportNo;
  try {
    const uploadRes = await uploadReport({
      userId,
      reportType:     form.reportType,
      reportTitle:    form.reportTitle,
      hospitalName:   form.hospitalName   || undefined,
      departmentName: form.departmentName || undefined,
      reportDate:     form.reportDate     || undefined,
      file:           form.file,
    });

    reportNo = uploadRes?.data?.data?.reportNo;
    if (!reportNo) throw new Error('上传成功但未返回报告编号');
  } catch (err) {
    phase.value = 'idle';
    ElMessage.error(err?.message || '上传失败，请稍后重试');
    return;
  }

  phase.value = 'analyzing';

  try {
    const interpretRes = await interpretReport(reportNo, userId);
    result.value = interpretRes?.data?.data;
    phase.value  = 'done';
    ElMessage.success('AI 解读完成');
  } catch (err) {
    phase.value = 'idle';
    ElMessage.error(err?.message || 'AI 解读失败，请稍后重试');
  }
}

// ── 重置 ──
function resetForm() {
  formRef.value?.resetFields();
  uploadRef.value?.clearFiles();
  form.file         = null;
  form.hospitalName   = '';
  form.departmentName = '';
  form.reportDate     = null;
  expandOptional.value = [];
}

function resetForNext() {
  resetForm();
  result.value = null;
  phase.value  = 'idle';
}

// ── Markdown 渲染 ──
function renderMarkdown(text) {
  if (!text) return '';
  return md.render(text);
}
</script>

<style scoped>
.report-upload {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 780px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

/* ── 卡片通用 ── */
.upload-card,
.progress-card,
.result-card {
  border-radius: 14px;
  border: 1px solid #e4e8f0;
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a2438;
}

.card-sub {
  font-size: 13px;
  color: #909399;
}

/* ── 上传区 ── */
.upload-form {
  margin-top: 4px;
}

.file-upload-area {
  width: 100%;
}

.file-upload-area :deep(.el-upload-dragger) {
  border-radius: 10px;
  border: 1.5px dashed #c0cce4;
  background: #f7f9fc;
  transition: border-color 0.2s;
  padding: 22px 0;
}

.file-upload-area :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background: #ecf5ff;
}

.upload-icon {
  font-size: 44px;
  color: #9aadc8;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.upload-text em {
  color: #409eff;
  font-style: normal;
}

.upload-tip {
  font-size: 12px;
  color: #aab4c4;
  text-align: center;
  margin-top: 6px;
}

/* ── 可选折叠 ── */
.optional-collapse {
  margin-bottom: 20px;
  border: none;
}

.optional-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  color: #606266;
  background: none;
  border: none;
  padding-left: 0;
}

.optional-collapse :deep(.el-collapse-item__wrap) {
  border: none;
  background: none;
}

.optional-collapse :deep(.el-collapse-item__content) {
  padding: 12px 0 0;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

/* ── 步骤条 ── */
.progress-card :deep(.el-card__body) {
  padding: 16px 24px;
}

.steps {
  padding: 4px 0;
}

/* ── 解读结果 ── */
.result-body {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 2px;
}

.meta-key {
  font-weight: 600;
  color: #1a2438;
  margin-right: 6px;
}

.risk-tag {
  font-size: 13px;
}

.summary-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a2438;
  margin: 0 0 10px;
}

/* Markdown 渲染样式（v-html 不受 scoped 影响，需 :deep） */
.result-body :deep(.md-body) {
  font-size: 14px;
  line-height: 1.8;
  color: #2a3550;
}

.result-body :deep(.md-body > *:first-child) { margin-top: 0; }
.result-body :deep(.md-body > *:last-child)  { margin-bottom: 0; }

.result-body :deep(.md-body p) { margin: 0 0 0.6em; }
.result-body :deep(.md-body strong) { font-weight: 700; }
.result-body :deep(.md-body ul),
.result-body :deep(.md-body ol) {
  padding-left: 1.5em;
  margin: 0.4em 0 0.6em;
}
.result-body :deep(.md-body li) { margin-bottom: 0.2em; }
.result-body :deep(.md-body h3),
.result-body :deep(.md-body h4) {
  font-weight: 700;
  margin: 0.8em 0 0.4em;
}

.result-body :deep(.md-body pre) {
  background: #1e2535;
  border-radius: 8px;
  padding: 12px 14px;
  overflow-x: auto;
  font-size: 13px;
}

.result-body :deep(.md-body code:not(pre > code)) {
  padding: 2px 5px;
  border-radius: 4px;
  background: #eef2f8;
  color: #d6336c;
  font-size: 0.88em;
}

.result-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
