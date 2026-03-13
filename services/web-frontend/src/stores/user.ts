import { ref } from 'vue';
import { defineStore } from 'pinia';

export interface UserInfo {
  id?: number | string;
  username?: string;
  nickname?: string;
  [key: string]: unknown;
}

const TOKEN_KEY = 'token';
const USER_INFO_KEY = 'user_info';

function parseUserInfo() {
  const raw = localStorage.getItem(USER_INFO_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UserInfo;
  } catch {
    localStorage.removeItem(USER_INFO_KEY);
    return null;
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '');
  const userInfo = ref<UserInfo | null>(parseUserInfo());

  function setToken(newToken: string) {
    token.value = newToken;
    localStorage.setItem(TOKEN_KEY, newToken);
  }

  function getToken() {
    if (!token.value) {
      token.value = localStorage.getItem(TOKEN_KEY) || '';
    }
    return token.value;
  }

  function setUserInfo(newUserInfo: UserInfo | null) {
    userInfo.value = newUserInfo;
    if (newUserInfo) {
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(newUserInfo));
    } else {
      localStorage.removeItem(USER_INFO_KEY);
    }
  }

  function logout() {
    token.value = '';
    userInfo.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_INFO_KEY);
  }

  return {
    token,
    userInfo,
    setToken,
    getToken,
    setUserInfo,
    logout,
  };
});
