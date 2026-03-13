<template>
  <div class="report-page">
    <div class="page-header">
      <h2 class="page-title">医疗报告</h2>
      <el-button text @click="handleLogout">退出登录</el-button>
    </div>

    <el-tabs v-model="activeTab" class="report-tabs">
      <el-tab-pane label="上传 &amp; 解读" name="upload">
        <ReportUpload />
      </el-tab-pane>
      <el-tab-pane label="我的报告" name="list">
        <ReportViewer />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import ReportUpload from '../components/report/ReportUpload.vue';
import ReportViewer from '../components/report/ReportViewer.vue';

const router    = useRouter();
const userStore = useUserStore();
const activeTab = ref('upload');

function handleLogout() {
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.report-page {
  min-height: 100vh;
  background: #f4f6fa;
  padding: 0;
}

.page-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: #fff;
  border-bottom: 1px solid #e4e8f0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a2438;
  margin: 0;
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
