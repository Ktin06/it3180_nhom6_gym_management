import { afterEach, describe, expect, it, vi } from 'vitest'
import { beginSession, clearSession, setAccessToken } from '../../../auth/session-store'
import { subscribeToNotifications } from './notification-stream'

class TestSocket {
  static readonly OPEN = 1
  static readonly CLOSED = 3
  static readonly instances: TestSocket[] = []

  readyState = TestSocket.OPEN
  sent: string[] = []
  onopen?: () => void
  onmessage?: (event: { data: string }) => void
  onclose?: (event: { code: number }) => void

  constructor() {
    TestSocket.instances.push(this)
  }

  send(frame: string) {
    this.sent.push(frame)
  }

  close() {
    this.readyState = TestSocket.CLOSED
    this.onclose?.({ code: 1000 })
  }

  receive(frame: string) {
    this.onmessage?.({ data: frame + '\0' })
  }
}

afterEach(() => {
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
  TestSocket.instances.length = 0
  clearSession()
})

describe('kênh STOMP thông báo F10', () => {
  it('TC-F10.2.1-05f: dùng access token trong CONNECT, đăng ký kênh riêng và bỏ qua sự kiện của phiên cũ', async () => {
    vi.stubGlobal('WebSocket', TestSocket)
    vi.stubEnv('VITE_WS_URL', 'wss://gym.test/ws')
    const epoch = beginSession()
    setAccessToken('member-test-token', epoch)
    const invalidate = vi.fn()
    const unsubscribe = subscribeToNotifications(epoch, invalidate)
    try {
      await vi.waitFor(() => expect(TestSocket.instances).toHaveLength(1))
      const socket = TestSocket.instances[0]
      socket.onopen?.()
      expect(socket.sent[0]).toMatch(/^CONNECT\n/)
      expect(socket.sent[0]).toContain('Authorization:Bearer member-test-token\n')

      socket.receive('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n')
      const subscription = socket.sent.find((frame) => frame.startsWith('SUBSCRIBE\n'))
      expect(subscription).toContain('destination:/user/queue/notifications\n')
      const subscriptionId = subscription?.match(/\nid:([^\n]+)/)?.[1]
      const notification = `MESSAGE\nsubscription:${subscriptionId}\nmessage-id:1\n\n{}`
      expect(invalidate).toHaveBeenCalledOnce()
      socket.receive(notification)
      expect(invalidate).toHaveBeenCalledTimes(2)

      beginSession()
      socket.receive(notification)
      expect(invalidate).toHaveBeenCalledTimes(2)
    } finally {
      unsubscribe()
    }
    expect(TestSocket.instances[0].readyState).toBe(TestSocket.CLOSED)
  })
})
