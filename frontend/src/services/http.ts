import axios from 'axios'

/**
 * Client HTTP dùng chung; mọi API nghiệp vụ có tiền tố /api/v1.
 * Gọi cùng domain (ADR 0008): Vite (dev) hoặc Vercel (deploy) chuyển tiếp /api sang backend,
 * nhờ đó cookie refresh token là cookie cùng domain.
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  // TODO(F1): gắn access token: config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      // TODO(F1): gọi API làm mới token (refresh token trong cookie httpOnly, S2);
      // thất bại thì xóa phiên và chuyển về /login.
    }
    return Promise.reject(error)
  },
)
