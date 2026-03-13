<template>
  <div class="report-viewer">

    <!-- ── 列表区 ── -->
    <div class="list-panel" :class="{ 'is-collapsed': !!activeReport }">

      <!-- 工具栏 -->
      <div class="list-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索报告名称"
          clearable
          prefix-icon="Search"
          class="search-input"
          @input="filterList"
        />
        <el-select
          v-model="filterType"
          placeholder="全部类型"
          clearable
          class="type-filter"
          @change="filterList"
        >
          <el-option
            v-for="opt in REPORT_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button :icon="Refresh" circle :loading="loading" @click="loadList(true)" />
      </div>

      <!-- 列表 -->
      <el-scrollbar class="list-scroll">
        <div v-if="loading && !reports.length" class="list-placeholder">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else-if="!filteredReports.length" class="list-placeholder">
          <el-empty :image-size="64" :description="keyword || filterType ? '未找到匹配报告' : '暂无报告，请先上传'" />
        </div>

        <div
          v-for="item in filteredReports"
          v-else
          :key="item.reportNo"
          class="report-item"
          :class="{ 'is-active': activeReport?.reportNo === item.reportNo }"
          @click="handleSelect(item)"
        >
          <div class="item-top">
            <span class="item-title" :title="item.reportTitle">{{ item.reportTitle }}</span>
            <el-tag
              v-if="item.riskLevel"
              :type="RISK_TAG[item.riskLevel]?.type"
              size="small"
              class="item-risk"
            >
              {{ RISK_TAG[item.riskLevel]?.label ?? item.riskLevel }}
            </el-tag>
          </div>
          <div class="item-meta">
            <span class="meta-type">{{ REPORT_TYPE_LABEL[item.reportType] ?? item.reportType }}</span>
            <span v-if="item.hospitalName" class="meta-hospital">{{ item.hospitalName }}</span>
            <span class="meta-date">{{ formatDate(item.reportDate || item.createdAt) }}</span>
          </div>
          <div class="item-status">
            <el-tag :type="REVIEW_TAG[item.reviewStatus]?.type ?? 'info'" size="small" plain>
              {{ REVIEW_TAG[item.reviewStatus]?.label ?? item.reviewStatus ?? '未知' }}
            </el-tag>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more" @click="loadList(false)">
          <span v-if="!loadingMore">加载更多</span>
          <el-icon v-else class="is-loading"><Loading /></el-icon>
        </div>
      </el-scrollbar>
    </div>

    <!-- ── 详情区 ── -->
    <div v-if="activeReport" class="detail-panel">

      <!-- 详情头 -->
      <div class="detail-header">
        <el-button :icon="ArrowLeft" text @click="activeReport = null">返回列表</el-button>
        <div class="detail-title-row">
          <h3 class="detail-title">{{ activeReport.reportTitle }}</h3>
          <el-tag
            v-if="activeReport.riskLevel"
            :type="RISK_TAG[activeReport.riskLevel]?.type"
            size="large"
          >
            {{ RISK_TAG[activeReport.riskLevel]?.label ?? activeReport.riskLevel }}
          </el-tag>
        </div>
      </div>

      <!-- 元信息 -->
      <el-descriptions :column="2" border size="small" class="detail-meta">
        <el-descriptions-item label="报告类型">
          {{ REPORT_TYPE_LABEL[activeReport.reportType] ?? activeReport.reportType ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">
          {{ formatDateTime(activeReport.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="activeReport.reportDate" label="报告日期">
          {{ activeReport.reportDate }}
        </el-descriptions-item>
        <el-descriptions-item v-if="activeReport.hospitalName" label="医院">
          {{ activeReport.hospitalName }}
        </el-descriptions-item>
        <el-descriptions-item v-if="activeReport.departmentName" label="科室">
          {{ activeReport.departmentName }}
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="REVIEW_TAG[activeReport.reviewStatus]?.type ?? 'info'" size="small" plain>
            {{ REVIEW_TAG[activeReport.reviewStatus]?.label ?? activeReport.reviewStatus ?? '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报告编号" :span="2">
          <span class="mono">{{ activeReport.reportNo }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- AI 解读 -->
      <div class="interpret-section">
        <div class="section-header">
          <span class="section-title">AI 解读结果</span>
          <el-button
            v-if="!activeReport.interpretationSummary || activeReport.reviewStatus === 'FAILED'"
            size="small"
            type="primary"
            plain
            :loading="interpreting"
            @click="handleInterpret"
          >
            {{ interpreting ? '解读中…' : '立即解读' }}
          </el-button>
        </div>

        <!-- 解读中 -->
        <div v-if="interpreting" class="interpret-loading">
          <el-icon class="is-loading loading-spin"><Loading /></el-icon>
          <span>AI 正在分析报告内容，请稍候…</span>
        </div>

        <!-- 无解读内容 -->
        <div
          v-else-if="!activeReport.interpretationSummary"
          class="interpret-empty"
        >
          <el-empty :image-size="56" description="暂无 AI 解读，点击「立即解读」生成" />
        </div>

        <!-- 解读内容（Markdown） -->
        <div
          v-else
          class="interpret-body md-body"
          v-html="renderMarkdown(activeReport.interpretationSummary)"
        />
      </div>

      <!-- 原始文本（可选展示） -->
      <el-collapse v-if="activeReport.rawText" class="raw-collapse">
        <el-collapse-item title="查看原始报告文本" name="raw">
          <pre class="raw-text">{{ activeReport.rawText }}</pre>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 无选中时提示（宽屏） -->
    <div v-else class="detail-placeholder">
      <el-empty :image-size="80" description="选择左侧报告查看详情" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Loading, Refresh } from '@element-plus/icons-vue';
import MarkdownIt from 'markdown-it';
import { getReportDetail, interpretReport, listReports } from '../../api/report';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const md = new MarkdownIt({ html: false, linkify: true });

// ── 常量映射 ──
const REPORT_TYPE_OPTIONS = [
  { value: 'blood',      label: '血液检验' },
  { value: 'ct',         label: 'CT 检查' },
  { value: 'mri',        label: 'MRI 检查' },
  { value: 'ultrasound', label: '超声 / 彩超' },
  { value: 'ecg',        label: '心电图' },
  { value: 'pathology',  label: '病理报告' },
  { value: 'other',      label: '其他' },
];

const REPORT_TYPE_LABEL = Object.fromEntries(
  REPORT_TYPE_OPTIONS.map(o => [o.value, o.label])
);

const RISK_TAG = {
  LOW:      { label: '低风险',  type: 'success' },
  MEDIUM:   { label: '中等风险', type: 'warning' },
  HIGH:     { label: '高风险',  type: 'danger' },
  CRITICAL: { label: '危急值',  type: 'danger' },
};

const REVIEW_TAG = {
  PENDING:    { label: '待解读',  type: 'info' },
  PROCESSING: { label: '解读中',  type: 'warning' },
  DONE:       { label: '已完成',  type: 'success' },
  FAILED:     { label: '解读失败', type: 'danger' },
};

// ── 列表状态 ──
const reports      = ref([]);
const loading      = ref(false);
const loadingMore  = ref(false);
const page         = ref(1);
const pageSize     = 20;
const total        = ref(0);
const keyword      = ref('');
const filterType   = ref('');

const hasMore = computed(() => reports.value.length < total.value);

const filteredReports = computed(() => {
  let list = reports.value;
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase();
    list = list.filter(r => (r.reportTitle || '').toLowerCase().includes(kw));
  }
  if (filterType.value) {
    list = list.filter(r => r.reportType === filterType.value);
  }
  return list;
});

// ── 详情状态 ──
const activeReport = ref(null);
const interpreting = ref(false);

onMounted(() => {
  loadList(true);
});

// ── 列表加载 ──
async function loadList(reset = false) {
  const userId = getUserId();
  if (!userId) return;

  if (reset) {
    loading.value = true;
    page.value = 1;
  } else {
    loadingMore.value = true;
  }

  try {
    const res = await listReports(userId, page.value, pageSize);
    const data = res?.data?.data;
    if (data) {
      total.value = data.total ?? 0;
      if (reset) {
        reports.value = data.items ?? [];
      } else {
        reports.value.push(...(data.items ?? []));
        page.value += 1;
      }
    }
  } catch {
    ElMessage.error('加载报告列表失败');
  } finally {
    loading.value    = false;
    loadingMore.value = false;
  }
}

function filterList() {
  // 过滤是计算属性驱动的，这里无需额外逻辑；预留扩展入口
}

// ── 选择报告 → 加载详情 ──
async function handleSelect(item) {
  const userId = getUserId();
  if (!userId) return;

  // 先用列表数据快速展示，再加载完整详情
  activeReport.value = { ...item };

  try {
    const res = await getReportDetail(item.reportNo, userId);
    const detail = res?.data?.data;
    if (detail) {
      activeReport.value = detail;
    }
  } catch {
    ElMessage.error('加载报告详情失败');
  }
}

// ── 触发 AI 解读 ──
async function handleInterpret() {
  const userId = getUserId();
  if (!userId || !activeReport.value?.reportNo) return;

  interpreting.value = true;
  try {
    const res = await interpretReport(activeReport.value.reportNo, userId);
    const result = res?.data?.data;
    if (result) {
      activeReport.value.interpretationSummary = result.interpretationSummary;
      activeReport.value.riskLevel  = result.riskLevel;
      activeReport.value.reviewStatus = result.reviewStatus;
      // 同步更新列表中对应条目
      const idx = reports.value.findIndex(r => r.reportNo === activeReport.value.reportNo);
      if (idx !== -1) {
        reports.value[idx] = {
          ...reports.value[idx],
          riskLevel:    result.riskLevel,
          reviewStatus: result.reviewStatus,
        };
      }
    }
    ElMessage.success('AI 解读完成');
  } catch (err) {
    ElMessage.error(err?.message || 'AI 解读失败，请稍后重试');
  } finally {
    interpreting.value = false;
  }
}

function getUserId() {
  return userStore.userInfo?.id || userStore.userInfo?.userId || null;
}

// ── 时间格式化 ──
function formatDate(str) {
  if (!str) return '-';
  return str.slice(0, 10);
}

function formatDateTime(str) {
  if (!str) return '-';
  return str.replace('T', ' ').slice(0, 16);
}

// ── Markdown 渲染 ──
function renderMarkdown(text) {
  if (!text) return '';
  return md.render(text);
}
</script>

<style scoped>
/* ── 整体布局 ── */
.report-viewer {
  display: flex;
  gap: 0;
  height: calc(100vh - 100px);
  min-height: 480px;
  border: 1px solid #e4e8f0;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

/* ── 列表面板 ── */
.list-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #edf0f5;
  background: #f7f9fc;
  transition: width 0.25s;
}

.list-panel.is-collapsed {
  width: 280px;
}

@media (max-width: 760px) {
  .list-panel.is-collapsed {
    display: none;
  }
}

.list-toolbar {
  display: flex;
  gap: 8px;
  padding: 12px 12px 10px;
  flex-shrink: 0;
  border-bottom: 1px solid #edf0f5;
}

.search-input {
  flex: 1;
  min-width: 0;
}

.type-filter {
  width: 110px;
  flex-shrink: 0;
}

.list-scroll {
  flex: 1;
  min-height: 0;
}

.list-placeholder {
  padding: 24px 16px;
}

/* ── 报告列表项 ── */
.report-item {
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #eef1f6;
  transition: background 0.15s;
}

.report-item:hover {
  background: #edf3ff;
}

.report-item.is-active {
  background: #e4eeff;
  border-left: 3px solid #2d6de6;
}

.item-top {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
}

.item-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: #1a2438;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-risk {
  flex-shrink: 0;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.meta-type {
  background: #eef2f8;
  padding: 1px 6px;
  border-radius: 4px;
  color: #4a5d7e;
}

.item-status {
  display: flex;
  justify-content: flex-end;
}

.load-more {
  text-align: center;
  padding: 10px;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
}

/* ── 详情面板 ── */
.detail-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 20px 24px 32px;
}

.detail-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c7d3;
}

/* 详情头 */
.detail-header {
  margin-bottom: 16px;
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.detail-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a2438;
  margin: 0;
  flex: 1;
}

/* 元数据 */
.detail-meta {
  margin-bottom: 24px;
}

.mono {
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #5a6a84;
}

/* AI 解读区 */
.interpret-section {
  border: 1px solid #e4e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  background: #f8fafd;
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a2438;
}

.interpret-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #909399;
  font-size: 14px;
  padding: 16px 0;
}

.loading-spin {
  font-size: 18px;
  color: #409eff;
}

.interpret-empty {
  padding: 12px 0;
}

/* Markdown 解读正文 */
.interpret-body.md-body {
  font-size: 14px;
  line-height: 1.8;
  color: #2a3550;
}

.interpret-body :deep(> *:first-child) { margin-top: 0; }
.interpret-body :deep(> *:last-child)  { margin-bottom: 0; }

.interpret-body :deep(p) { margin: 0 0 0.6em; }
.interpret-body :deep(strong) { font-weight: 700; }

.interpret-body :deep(ul),
.interpret-body :deep(ol) {
  padding-left: 1.5em;
  margin: 0.4em 0 0.6em;
}

.interpret-body :deep(li) { margin-bottom: 0.25em; }

.interpret-body :deep(h3),
.interpret-body :deep(h4) {
  font-weight: 700;
  margin: 0.8em 0 0.4em;
  color: #1a2438;
}

.interpret-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.6em 0;
  font-size: 13px;
}

.interpret-body :deep(th),
.interpret-body :deep(td) {
  border: 1px solid #d0d7e3;
  padding: 6px 10px;
  text-align: left;
}

.interpret-body :deep(th) {
  background: #f0f4fa;
  font-weight: 600;
}

.interpret-body :deep(tr:nth-child(even) td) {
  background: #f7f9fc;
}

.interpret-body :deep(pre) {
  background: #1e2535;
  border-radius: 8px;
  padding: 12px 14px;
  overflow-x: auto;
  font-size: 13px;
  color: #e8eaf6;
}

.interpret-body :deep(code:not(pre > code)) {
  padding: 2px 5px;
  border-radius: 4px;
  background: #eef2f8;
  color: #d6336c;
  font-size: 0.88em;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
}

.interpret-body :deep(blockquote) {
  margin: 0.5em 0;
  padding: 6px 12px;
  border-left: 3px solid #2d8f6f;
  background: #f3fbf8;
  color: #47606d;
}

/* 原始文本 */
.raw-collapse {
  border: 1px solid #e4e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.raw-collapse :deep(.el-collapse-item__header) {
  padding: 0 16px;
  font-size: 13px;
  color: #606266;
  background: #f7f9fc;
}

.raw-collapse :deep(.el-collapse-item__wrap) {
  background: #f7f9fc;
}

.raw-text {
  font-size: 13px;
  line-height: 1.7;
  color: #3a4a6b;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  padding: 12px 16px;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  max-height: 400px;
  overflow-y: auto;
}
</style>
