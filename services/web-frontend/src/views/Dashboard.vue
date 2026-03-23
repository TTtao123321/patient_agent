<template>
  <div class="page-wrap">
    <Header />
    <div class="page-content">
      <h2>Dashboard Page</h2>
    </div>
  </div>
</template>

<script setup>
import Header from '../components/layout/Header.vue';
</script>

<style scoped>
.page-wrap {
  min-height: 100vh;
  background: #f4f6fa;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.page-content {
  flex: 1;
  overflow: auto;
  padding: 24px;
}
</style>
