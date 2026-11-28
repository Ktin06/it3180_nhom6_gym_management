import { Client } from '@stomp/stompjs'
import deployment from '../../../../vercel.json'
import { getAccessToken, getSessionEpoch } from '../../../auth/session-store'
import { HTTP_BASE_URL } from '../../../services/http-config'
import { notificationsApi } from './notifications'

const destination = '/user/queue/notifications'

function brokerUrl() {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL

  const deployedApi = deployment.rewrites.find((rule) => rule.source === '/api/:path*')
  const backend = /^https?:\/\//.test(HTTP_BASE_URL)
    ? HTTP_BASE_URL
    : import.meta.env.DEV
      ? window.location.origin
      : (deployedApi?.destination ?? window.location.origin)
  const url = new URL('/ws', backend)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  return url.href
}

/** Chỉ dùng token phiên hiện tại và gửi trong STOMP CONNECT (ADR 0008). */
export function subscribeToNotifications(authEpoch: number, invalidate: () => void) {
  const client = new Client({
    brokerURL: brokerUrl(),
    reconnectDelay: 5000,
    connectionTimeout: 8000,
  })
  const controller = new AbortController()
  const active = () => !controller.signal.aborted && getSessionEpoch() === authEpoch
  let rejectedToken: string | null = null
  let sentToken: string | null = null

  client.beforeConnect = async () => {
    const token = getAccessToken()
    if (active() && token && token === rejectedToken) {
      try {
        // REST dùng chung luồng refresh khi broker từ chối access token đã hết hạn.
        await notificationsApi.unreadCount(authEpoch, controller.signal)
      } catch {
        // Lần kết nối tiếp theo đọc lại token nếu phiên vẫn còn hiệu lực.
      }
    }
    sentToken = active() ? getAccessToken() : null
    if (!sentToken) {
      await client.deactivate()
      return
    }
    client.connectHeaders = { Authorization: `Bearer ${sentToken}` }
  }
  client.onConnect = () => {
    if (!active()) return
    rejectedToken = null
    client.subscribe(destination, () => {
      if (active()) invalidate()
    })
    // Đồng bộ các thông báo phát sinh trong lúc mất kết nối.
    invalidate()
  }
  client.onStompError = () => {
    if (!client.connected) rejectedToken = sentToken
  }

  client.activate()
  return () => {
    controller.abort()
    void client.deactivate({ force: true })
  }
}
