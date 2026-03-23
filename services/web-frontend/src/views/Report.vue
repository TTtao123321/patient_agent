<template>
  <div class="report-page">
    <Header />
    <div class="page-content">
      <el-tabs v-model="activeTab" class="report-tabs">
        <el-tab-pane label="上传 &amp; 解读" name="upload">
          <ReportUpload />
        </el-tab-pane>
        <el-tab-pane label="我的报告" name="list">
          <ReportViewer />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import Header from '../components/layout/Header.vue';
import ReportUpload from '../components/report/ReportUpload.vue';
import ReportViewer from '../components/report/ReportViewer.vue';

const router    = useRouter();
const userStore = useUserStore();
const activeTab = ref('upload');
</script>

<style scoped>
.report-page {
  min-height: 100vh;
  background: #f4f6fa;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.page-content {
  flex: 1;
  overflow: auto;
}

.report-tabs {
  padding: 20px 24px 0;
}

.report-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.report-tabs :deep(.el-tabs__content) {
  padding-top: 4px;
}

</style>
