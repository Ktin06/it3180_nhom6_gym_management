import axios from 'axios'
import {
  expireSession,
  getAccessToken,
  getSessionEpoch,
  setAccessToken,
  suspendSession,
} from '../auth/session-store'
import { queueCookieOperation } from '../auth/cookie-operation-queue'
import { requestRefresh } from './refresh-client'

const baseURL = import.meta.env.VITE_API_URL ?? '/api/v1'
const timeout = 15000
let refreshFlight: { epoch: number; promise: Promise<string> } | null = null

/**
 * Client HTTP dùng chung; mọi API nghiệp vụ có tiền tố /api/v1.
 * Gọi cùng domain (ADR 0008): Vite (dev) hoặc Vercel (deploy) chuyển tiếp /api sang backend,
 * nhờ đó cookie refresh token là cookie cùng domain.
 */
export const http = axios.create({
  baseURL,
  timeout,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

export function refreshAccessToken(expectedEpoch = getSessionEpoch()): Promise<string> {
  if (getSessionEpoch() !== expectedEpoch)
    return Promise.reject(new Error('Phiên đăng nhập đã thay đổi.'))
  if (refreshFlight?.epoch === expectedEpoch) return refreshFlight.promise

  const operation = queueCookieOperation(async () => {
    if (getSessionEpoch() !== expectedEpoch) throw new Error('Phiên đăng nhập đã thay đổi.')
    try {
      const response = await requestRefresh()
      if (typeof response.accessToken !== 'string' || !response.accessToken) {
        throw new Error('Máy chủ không trả access token hợp lệ.')
      }
      if (!setAccessToken(response.accessToken, expectedEpoch)) {
        throw new Error('Phiên đăng nhập đã thay đổi.')
      }
      return response.accessToken
    } catch (error) {
      if (getSessionEpoch() === expectedEpoch) {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          expireSession(expectedEpoch)
        } else {
          suspendSession(expectedEpoch)
        }
      }
      throw error
    }
  })
  const promise = operation.finally(() => {
    if (refreshFlight?.promise === promise) refreshFlight = null
  })
  refreshFlight = { epoch: expectedEpoch, promise }
  return promise
}

http.interceptors.request.use((config) => {
  const currentEpoch = getSessionEpoch()
  if (config.authEpoch !== undefined && config.authEpoch !== currentEpoch) {
    throw new Error('Phiên đăng nhập đã thay đổi.')
  }
  config.authEpoch ??= currentEpoch
  if (config.skipBearer) {
    config.headers.delete('Authorization')
    return config
  }
  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
    config.authSentToken = token
  } else {
    config.headers.delete('Authorization')
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.config.authSentToken && response.config.authEpoch !== getSessionEpoch()) {
      throw new Error('Phiên đăng nhập đã thay đổi.')
    }
    return response
  },
  async (error) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401) throw error

    const config = error.config
    if (!config || config.skipAuthRefresh || !config.authSentToken) throw error
    if (config.authEpoch === undefined || config.authEpoch !== getSessionEpoch()) throw error
    if (config.authRetry) {
      expireSession(config.authEpoch)
      throw error
    }

    const currentToken = getAccessToken()
    if (currentToken && currentToken !== config.authSentToken) {
      config.authRetry = true
      return http.request(config)
    }

    await refreshAccessToken(config.authEpoch)
    if (config.authEpoch !== getSessionEpoch()) throw error
    config.authRetry = true
    return http.request(config)
  },
)
