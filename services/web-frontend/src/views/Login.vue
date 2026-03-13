<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">医疗 AI Agent 登录</h1>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        label-position="top"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model.trim="loginForm.username"
            placeholder="请输入用户名"
            clearable
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { useUserStore } from '../stores/user';
import { login } from '../api/auth';

interface LoginForm {
  username: string;
  password: string;
}

interface LoginApiResponse {
  code?: number;
  message?: string;
  data?: {
    accessToken?: string;
    user?: Record<string, unknown>;
  };
}

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const loginFormRef = ref<FormInstance>();

const loginForm = reactive<LoginForm>({
  username: '',
  password: '',
});

const rules: FormRules<LoginForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度应为 2-50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度应为 6-50 个字符', trigger: 'blur' },
  ],
};

async function handleLogin() {
  if (!loginFormRef.value) return;

  try {
    await loginFormRef.value.validate();
  } catch {
    ElMessage.error('请先修正表单校验错误');
    return;
  }

  loading.value = true;
  try {
    const response = await login(loginForm.username, loginForm.password);
    const result = response?.data as LoginApiResponse;

    if (result?.code !== 0) {
      const errorMsg = result?.message || '登录失败，请稍后重试';
      throw new Error(errorMsg);
    }

    const token = result?.data?.accessToken;
    if (!token) {
      throw new Error('登录成功但未返回 token');
    }

    userStore.setToken(token);
    userStore.setUserInfo(result?.data?.user || null);
    ElMessage.success('登录成功');
    await router.push('/chat');
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败，请稍后重试';
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #f2f7ff 0%, #eefaf3 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 32px 28px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(20, 40, 80, 0.08);
}

.login-title {
  margin: 0 0 24px;
  font-size: 24px;
  font-weight: 600;
  color: #1f2a44;
  text-align: center;
}

.login-form {
  margin-top: 8px;
}

.login-button {
  width: 100%;
}
</style>
