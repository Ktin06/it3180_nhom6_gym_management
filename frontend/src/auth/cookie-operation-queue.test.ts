import { describe, expect, it } from 'vitest'
import { queueCookieOperation } from './cookie-operation-queue'

function deferred() {
  let resolve!: () => void
  const promise = new Promise<void>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

describe('cookie operation queue', () => {
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

    await Promise.resolve()
    expect(order).toEqual(['refresh:start'])
    refreshGate.resolve()
    await Promise.all([refresh, logout, login])

    expect(order).toEqual(['refresh:start', 'refresh:end', 'logout', 'login'])
  })
})
