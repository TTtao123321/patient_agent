<template>
  <div class="page-wrap">
    <h2>Report Page</h2>
    <el-button type="danger" @click="handleLogout">退出登录</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();

function handleLogout() {
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.page-wrap {
  padding: 24px;
}
</style>
