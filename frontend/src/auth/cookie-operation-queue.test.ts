import { describe, expect, it, vi } from 'vitest'
import { queueCookieOperation } from './cookie-operation-queue'

function deferred() {
  let resolve!: () => void
  const promise = new Promise<void>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

describe('cookie operation queue', () => {
  it('hai tab làm mới bằng cookie mới nhất sau khi đợi khóa chung', async () => {
    const lockRequests = vi.spyOn(navigator.locks, 'request')
    vi.resetModules()
    const firstTab = await import('./cookie-operation-queue')
    vi.resetModules()
    const secondTab = await import('./cookie-operation-queue')
    let cookie = 'R1'
    let revoked = false
    const firstStarted = deferred()
    const releaseFirst = deferred()
    const seen: string[] = []
    const first = firstTab.queueCookieOperation(async () => {
      const sent = cookie
      seen.push(sent)
      firstStarted.resolve()
      await releaseFirst.promise
      cookie = 'R2'
    })
    await firstStarted.promise
    const second = secondTab.queueCookieOperation(async () => {
      const sent = cookie
      seen.push(sent)
      if (sent === 'R1') revoked = true
      cookie = 'R3'
    })

    try {
      await vi.waitFor(() => expect(lockRequests).toHaveBeenCalledTimes(2))
      expect(seen).toEqual(['R1'])
    } finally {
      releaseFirst.resolve()
      await Promise.all([first, second])
    }
    expect(seen).toEqual(['R1', 'R2'])
    expect(revoked).toBe(false)
    expect(cookie).toBe('R3')
  })

  it('đợi refresh đang chạy trước logout và login tiếp theo', async () => {
    const refreshGate = deferred()
    const order: string[] = []
    const refresh = queueCookieOperation(async () => {
      order.push('refresh:start')
      await refreshGate.promise
      order.push('refresh:end')
    })
    const logout = queueCookieOperation(async () => {
      order.push('logout')
    })
    const login = queueCookieOperation(async () => {
      order.push('login')
    })

    await vi.waitFor(() => expect(order).toEqual(['refresh:start']))
    expect(order).toEqual(['refresh:start'])
    refreshGate.resolve()
    await Promise.all([refresh, logout, login])

    expect(order).toEqual(['refresh:start', 'refresh:end', 'logout', 'login'])
  })

  it('một tab logout vẫn chờ refresh của tab khác', async () => {
    const lockRequests = vi.spyOn(navigator.locks, 'request')
    vi.resetModules()
    const firstTab = await import('./cookie-operation-queue')
    vi.resetModules()
    const secondTab = await import('./cookie-operation-queue')
    const release = deferred()
    const started = deferred()
    const order: string[] = []
    const refresh = firstTab.queueCookieOperation(async () => {
      order.push('refresh:start')
      started.resolve()
      await release.promise
      order.push('refresh:end')
    })
    await started.promise
    const logout = secondTab.queueCookieOperation(async () => {
      order.push('logout')
    })
    try {
      await vi.waitFor(() => expect(lockRequests).toHaveBeenCalledTimes(2))
      expect(order).toEqual(['refresh:start'])
    } finally {
      release.resolve()
      await Promise.all([refresh, logout])
    }
    expect(order).toEqual(['refresh:start', 'refresh:end', 'logout'])
  })

  it('nhả khóa sau lỗi và không làm hỏng thao tác tiếp theo', async () => {
    await expect(
      queueCookieOperation(async () => {
        throw new Error('Network Error')
      }),
    ).rejects.toThrow('Network Error')
    await expect(queueCookieOperation(async () => 'ok')).resolves.toBe('ok')
  })

  it('không gửi thao tác cookie khi không có Web Locks', async () => {
    Object.defineProperty(navigator, 'locks', { configurable: true, value: undefined })
    const operation = vi.fn()
    await expect(queueCookieOperation(operation)).rejects.toThrow('Vui lòng cập nhật trình duyệt.')
    expect(operation).not.toHaveBeenCalled()
  })
})
