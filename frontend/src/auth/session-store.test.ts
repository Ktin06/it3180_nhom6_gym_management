import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  beginSession,
  getAccessToken,
  getSessionEpoch,
  setAccessToken,
  subscribeToSessionExpiry,
  suspendSession,
} from './session-store'

const jwt = (sub: string, rotation = 0) =>
  `e30.${btoa(JSON.stringify({ sub, rotation })).replace(/=/g, '')}.signature`

describe('session subject', () => {
  beforeEach(() => beginSession())

  it('cho phép xoay token cùng chủ thể', () => {
    const epoch = getSessionEpoch()
    expect(setAccessToken(jwt('15'), epoch)).toBe(true)
    expect(setAccessToken(jwt('15', 1), epoch)).toBe(true)
    expect(getAccessToken()).toBe(jwt('15', 1))
  })

  it.each([jwt('26'), 'invalid-token', 'e30.e30.signature'])(
    'kết thúc phiên khi token mới không giữ chủ thể: %s',
    (token) => {
      const epoch = getSessionEpoch()
      setAccessToken(jwt('15'), epoch)
      const onExpiry = vi.fn()
      const unsubscribe = subscribeToSessionExpiry(onExpiry)
      try {
        expect(setAccessToken(token, epoch)).toBe(false)
        expect(getAccessToken()).toBeNull()
        expect(onExpiry).toHaveBeenCalledWith(
          'Tài khoản đã đổi ở tab khác. Vui lòng đăng nhập lại.',
        )
      } finally {
        unsubscribe()
      }
    },
  )

  it('lỗi mạng không bỏ chủ thể đã ghim khi thử phục hồi', () => {
    setAccessToken(jwt('15'), getSessionEpoch())
    suspendSession(getSessionEpoch())
    expect(setAccessToken(jwt('26'), getSessionEpoch())).toBe(false)
    expect(getAccessToken()).toBeNull()
  })

  it('đăng nhập mới có thể đổi tài khoản, response cũ không ảnh hưởng phiên mới', () => {
    const oldEpoch = getSessionEpoch()
    setAccessToken(jwt('15'), oldEpoch)
    const newEpoch = beginSession()
    setAccessToken(jwt('26'), newEpoch)
    expect(setAccessToken(jwt('15'), oldEpoch)).toBe(false)
    expect(getAccessToken()).toBe(jwt('26'))
  })
})
