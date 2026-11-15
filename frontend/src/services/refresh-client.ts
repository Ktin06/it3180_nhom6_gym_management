import axios from 'axios'
import type { AccessTokenResponse } from '../features/auth/types/auth'

/** Client riêng, không có bearer/interceptor; cookie HttpOnly cùng domain đi tự động. */
const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

export async function requestRefresh() {
  const response = await refreshClient.post<AccessTokenResponse>('/auth/refresh')
  return response.data
}
