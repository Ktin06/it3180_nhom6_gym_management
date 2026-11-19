import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const revisionKey = 'gym-auth-cookie-revision'
const cookieSession = 'current-session'

class TestChannel {
  static current: TestChannel
  onmessage: ((event: MessageEvent) => void) | null = null
  postMessage = vi.fn()

  constructor() {
    TestChannel.current = this
  }

  receive(data: object) {
    this.onmessage?.(new MessageEvent('message', { data }))
  }
}

function setRevision(session: string, refresh: string | null = null) {
  localStorage.setItem(revisionKey, JSON.stringify({ session, refresh }))
}

function tokenFor(subject: string) {
  return `header.${btoa(JSON.stringify({ sub: subject }))}.signature`
}

describe('đồng bộ phiên giữa các tab', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubGlobal('BroadcastChannel', TestChannel)
    setRevision(cookieSession)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('tab chờ lấy token của lần refresh mới và bỏ qua kết quả cũ', async () => {
    const sync = await import('./session-coordination')
    const before = sync.captureCookieRevision()
    setRevision(cookieSession, 'new-refresh')
    const revision = sync.currentTabCookieRevision(0)!
    const waiting = sync.reuseConcurrentRefresh(before, revision)
    const channel = TestChannel.current
    expect(channel.postMessage).toHaveBeenCalledWith({ type: 'request', refresh: 'new-refresh' })

    channel.receive({
      type: 'refreshed',
      refresh: 'other-refresh',
      token: 'old-token',
      issuedAt: Date.now(),
    })
    channel.receive({
      type: 'refreshed',
      refresh: 'new-refresh',
      token: 'fresh-token',
      issuedAt: Date.now(),
    })
    await expect(waiting).resolves.toBe('fresh-token')
    expect(localStorage.getItem(revisionKey)).not.toContain('fresh-token')
  })

  it('khi tab refresh đã đóng, cho lần chờ gửi request tuần tự với cookie hiện tại', async () => {
    vi.useFakeTimers()
    const sync = await import('./session-coordination')
    const before = sync.captureCookieRevision()
    setRevision(cookieSession, 'new-refresh')
    const waiting = sync.reuseConcurrentRefresh(before, sync.currentTabCookieRevision(0)!)
    await vi.runAllTimersAsync()
    await expect(waiting).resolves.toBeNull()
  })

  it.each([null, '29'])(
    'đăng xuất hoặc đổi tài khoản ở tab khác xóa phiên cũ (%s)',
    async (subject) => {
      const sync = await import('./session-coordination')
      const store = await import('./session-store')
      sync.captureCookieRevision()
      store.setAccessToken(tokenFor('18'), 0)
      const expired = vi.fn()
      store.subscribeToSessionExpiry(expired)
      setRevision('new-session')

      TestChannel.current.receive({ type: 'session', session: 'new-session', subject })
      expect(store.getAccessToken()).toBeNull()
      expect(expired).toHaveBeenCalledOnce()
      expect(sync.currentTabCookieRevision(0)).toBeNull()
    },
  )

  it('áp dụng logout dù broadcast đến trước khi tab thấy dấu phiên mới', async () => {
    const sync = await import('./session-coordination')
    const store = await import('./session-store')
    sync.captureCookieRevision()
    store.setAccessToken(tokenFor('18'), 0)

    TestChannel.current.receive({ type: 'session', session: 'new-session', subject: null })
    expect(store.getAccessToken()).not.toBeNull()
    setRevision('new-session')
    window.dispatchEvent(new StorageEvent('storage', { key: revisionKey }))
    expect(store.getAccessToken()).toBeNull()
  })

  it('đăng nhập cùng tài khoản ở tab khác không làm mất phiên', async () => {
    const sync = await import('./session-coordination')
    const store = await import('./session-store')
    sync.captureCookieRevision()
    const token = tokenFor('18')
    store.setAccessToken(token, 0)
    setRevision('new-session')

    TestChannel.current.receive({ type: 'session', session: 'new-session', subject: '18' })
    expect(store.getAccessToken()).toBe(token)
    expect(sync.currentTabCookieRevision(0)?.session).toBe('new-session')
  })

  it('chặn thao tác cookie cũ ngay cả khi thông báo đổi phiên chưa đến', async () => {
    const sync = await import('./session-coordination')
    const store = await import('./session-store')
    sync.captureCookieRevision()
    store.setAccessToken(tokenFor('18'), 0)
    setRevision('new-session')

    expect(sync.currentTabCookieRevision(0)).toBeNull()
    expect(store.getAccessToken()).toBeNull()
  })
})
