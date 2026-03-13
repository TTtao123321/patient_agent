import axios from 'axios';

export const API_BASE_URL = 'http://localhost:8080/api';

export function getAuthHeaders() {
  const token = localStorage.getItem('token');
  return token
    ? {
        Authorization: `Bearer ${token}`,
      }
    : {};
}

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

request.interceptors.request.use(
  (config) => {
    const authHeaders = getAuthHeaders();
    if (Object.keys(authHeaders).length > 0) {
      config.headers = config.headers || {};
      Object.assign(config.headers, authHeaders);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user_info');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default request;
