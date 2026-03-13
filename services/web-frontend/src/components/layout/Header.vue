<template>
  <header class="app-header">
    <!-- 左：品牌 -->
    <div class="header-brand">
      <span class="brand-icon">⚕</span>
      <span class="brand-name">医疗 AI 助手</span>
    </div>

    <!-- 中：导航 -->
    <nav class="header-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-link"
        :class="{ 'is-active': route.path === item.path }"
      >
        {{ item.label }}
      </router-link>
    </nav>

    <!-- 右：用户信息 -->
    <div class="header-user">
      <el-avatar :size="30" :style="{ backgroundColor: '#409eff', fontSize: '13px', flexShrink: 0 }">
        {{ avatarLetter }}
      </el-avatar>
      <span class="user-name">{{ displayName }}</span>
      <el-divider direction="vertical" style="margin: 0 4px;" />
      <el-button text size="small" @click="handleLogout">退出登录</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '../../stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const navItems = [
  { path: '/chat',      label: '问诊对话' },
  { path: '/report',    label: '报告分析' },
  { path: '/dashboard', label: '健康档案' },
];

const displayName = computed(() => {
  const info = userStore.userInfo;
  return info?.nickname || info?.username || info?.userNo || '用户';
});

const avatarLetter = computed(() => displayName.value.charAt(0).toUpperCase());

function handleLogout() {
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.app-header {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 100;
}

/* 品牌 */
.header-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 200px;
}
.brand-icon {
  font-size: 22px;
  line-height: 1;
}
.brand-name {
  font-size: 17px;
  font-weight: 700;
  color: #1a2438;
  letter-spacing: 0.3px;
}

/* 导航 */
.header-nav {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.nav-link {
  padding: 6px 18px;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
}
.nav-link:hover {
  background: #f5f7fa;
  color: #409eff;
}
.nav-link.is-active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

/* 用户区 */
.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 200px;
  justify-content: flex-end;
}
.user-name {
  font-size: 14px;
  color: #606266;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
