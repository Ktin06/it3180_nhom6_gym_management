import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { readCookieRevision, writeCookieRevision } from './session-revision'

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
  return writeCookieRevision({ session, refresh })
}

function tokenFor(subject: string) {
  return `header.${btoa(JSON.stringify({ sub: subject }))}.signature`
}

describe('đồng bộ phiên giữa các tab', () => {
  beforeEach(async () => {
    vi.resetModules()
    vi.stubGlobal('BroadcastChannel', TestChannel)
    await setRevision(cookieSession)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('tab chờ lấy token của lần refresh mới và bỏ qua kết quả cũ', async () => {
    const sync = await import('./session-coordination')
    const before = await sync.captureCookieRevision()
    await setRevision(cookieSession, 'new-refresh')
    const revision = (await sync.currentTabCookieRevision(0))!
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
    expect(await readCookieRevision()).toEqual({ session: cookieSession, refresh: 'new-refresh' })
  })

  it('khi tab refresh đã đóng, cho lần chờ gửi request tuần tự với cookie hiện tại', async () => {
    vi.useFakeTimers()
    const sync = await import('./session-coordination')
    const before = await sync.captureCookieRevision()
    await setRevision(cookieSession, 'new-refresh')
    const waiting = sync.reuseConcurrentRefresh(before, (await sync.currentTabCookieRevision(0))!)
    await vi.runAllTimersAsync()
    await expect(waiting).resolves.toBeNull()
  })

  it.each([null, '29'])(
    'đăng xuất hoặc đổi tài khoản ở tab khác xóa phiên cũ (%s)',
    async (subject) => {
      const sync = await import('./session-coordination')
      const store = await import('./session-store')
      await sync.captureCookieRevision()
      store.setAccessToken(tokenFor('18'), 0)
      const expired = vi.fn()
      store.subscribeToSessionExpiry(expired)
      await setRevision('new-session')

      TestChannel.current.receive({ type: 'session', session: 'new-session', subject })
      await vi.waitFor(() => expect(store.getAccessToken()).toBeNull())
      expect(expired).toHaveBeenCalledOnce()
      await expect(sync.currentTabCookieRevision(0)).resolves.toBeNull()
    },
  )

  it('áp dụng logout đã commit dù bản localStorage ở tab hiện tại còn cũ', async () => {
    const sync = await import('./session-coordination')
    const store = await import('./session-store')
    await sync.captureCookieRevision()
    store.setAccessToken(tokenFor('18'), 0)
    localStorage.setItem(revisionKey, JSON.stringify({ session: cookieSession, refresh: null }))

    await setRevision('new-session')
    TestChannel.current.receive({ type: 'session', session: 'new-session', subject: null })
    await vi.waitFor(() => expect(store.getAccessToken()).toBeNull())
  })

  it('đăng nhập cùng tài khoản ở tab khác không làm mất phiên', async () => {
    const sync = await import('./session-coordination')
    const store = await import('./session-store')
    await sync.captureCookieRevision()
    const token = tokenFor('18')
    store.setAccessToken(token, 0)
    await setRevision('new-session')

    TestChannel.current.receive({ type: 'session', session: 'new-session', subject: '18' })
    expect(store.getAccessToken()).toBe(token)
    await vi.waitFor(() => expect(store.getAccessToken()).toBe(token))
    await expect(sync.currentTabCookieRevision(0)).resolves.toMatchObject({
      session: 'new-session',
    })
  })

  it('chặn thao tác cookie cũ ngay cả khi thông báo đổi phiên chưa đến', async () => {
    const sync = await import('./session-coordination')
    const store = await import('./session-store')
    await sync.captureCookieRevision()
    store.setAccessToken(tokenFor('18'), 0)
    localStorage.setItem(revisionKey, JSON.stringify({ session: cookieSession, refresh: null }))
    await setRevision('new-session')

    await expect(sync.currentTabCookieRevision(0)).resolves.toBeNull()
    expect(store.getAccessToken()).toBeNull()
  })
})
