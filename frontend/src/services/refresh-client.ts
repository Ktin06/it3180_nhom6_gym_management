import axios from 'axios'
import type { AccessTokenResponse } from '../features/auth/types/auth'
import { HTTP_BASE_URL, HTTP_TIMEOUT } from './http-config'

/** Client riêng, không có bearer/interceptor; cookie HttpOnly cùng domain đi tự động. */
const refreshClient = axios.create({
  baseURL: HTTP_BASE_URL,
  timeout: HTTP_TIMEOUT,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

export async function requestRefresh() {
  const response = await refreshClient.post<AccessTokenResponse>('/auth/refresh')
  return response.data
}
