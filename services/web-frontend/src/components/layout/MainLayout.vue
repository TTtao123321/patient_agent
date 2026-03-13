<template>
  <div class="main-layout">
    <!-- 固定顶栏 -->
    <Header />

    <!-- 三栏主体 -->
    <div class="layout-body">
      <!-- 左：会话历史 Sidebar -->
      <aside class="layout-sidebar">
        <slot name="sidebar" />
      </aside>

      <!-- 中：主内容区（ChatWindow 等） -->
      <main class="layout-main">
        <slot />
      </main>

      <!-- 右：报告/信息面板（可选） -->
      <section v-if="showAside" class="layout-aside">
        <slot name="aside" />
      </section>
    </div>
  </div>
</template>

<script setup>
import Header from './Header.vue';

defineProps({
  /** 控制右侧面板显隐 */
  showAside: {
    type: Boolean,
    default: true,
  },
});
</script>

<style scoped>
/* ── 整体容器：撑满视口，禁止 body 滚动 ── */
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f0f4f8;
}

/* ── 三栏主体 ── */
.layout-body {
  flex: 1;
  min-height: 0;         /* 让子元素可以正确计算 flex 高度 */
  display: flex;
  overflow: hidden;
}

/* ── 左侧 Sidebar ── */
.layout-sidebar {
  width: 260px;
  flex-shrink: 0;
  overflow: hidden;
  /* 背景色由 Sidebar.vue 自身控制 */
}

/* ── 中间主内容 ── */
.layout-main {
  flex: 1;
  min-width: 0;          /* 防止 flex 子项溢出 */
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 右侧面板 ── */
.layout-aside {
  width: 340px;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-left: 1px solid #e4e7ed;
}
</style>
