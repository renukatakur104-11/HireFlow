/// <reference types="vite/client" />
import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

let accessToken: string | null = null;
let refreshToken: string | null = null;

export const setTokens = (at: string, rt: string) => {
  accessToken = at;
  refreshToken = rt;
  localStorage.setItem('hireflow_refresh_token', rt);
};

export const clearTokens = () => {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('hireflow_refresh_token');
};

export const getStoredRefreshToken = () =>
  localStorage.getItem('hireflow_refresh_token');

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = [];

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token!)));
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && !original._retry) {
      const rt = refreshToken || getStoredRefreshToken();
      if (!rt) { clearTokens(); window.location.href = '/login'; return Promise.reject(error); }

      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => { failedQueue.push({ resolve, reject }); })
          .then((token) => { original.headers['Authorization'] = `Bearer ${token}`; return apiClient(original); });
      }

      original._retry = true;
      isRefreshing = true;
      try {
        const resp = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken: rt });
        const { accessToken: newAt, refreshToken: newRt } = resp.data.data;
        setTokens(newAt, newRt);
        processQueue(null, newAt);
        original.headers['Authorization'] = `Bearer ${newAt}`;
        return apiClient(original);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
