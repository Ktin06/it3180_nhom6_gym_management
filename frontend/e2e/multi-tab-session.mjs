import assert from 'node:assert/strict'
import { statSync } from 'node:fs'
import test from 'node:test'
import { chromium } from 'playwright'

function deferred() {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { promise, resolve }
}

// Chạy với Vite và backend/CSDL thử thật; không giả lập phản hồi API.
test(
  'hai tab dùng chung cookie vẫn giữ phiên khi cùng refresh và tải lại',
  { timeout: 90000 },
  async (t) => {
    const baseURL = process.env.GYM_E2E_BASE_URL ?? 'http://localhost:5173'
    const email = process.env.GYM_E2E_EMAIL
    const password = process.env.GYM_E2E_PASSWORD
    assert(email && password, 'Cần GYM_E2E_EMAIL và GYM_E2E_PASSWORD của tài khoản kiểm thử riêng.')
    const backup = process.env.GYM_E2E_DB_BACKUP
    assert(
      backup && statSync(backup).isFile(),
      'Cần GYM_E2E_DB_BACKUP trỏ tới bản sao lưu CSDL trước khi đổi phiên.',
    )

    const browser = await chromium.launch({
      executablePath: process.env.GYM_BROWSER_EXECUTABLE || undefined,
    })
    t.after(() => browser.close())
    const context = await browser.newContext()
    const first = await context.newPage()
    const pageErrors = []
    context.on('page', (page) => page.on('pageerror', (error) => pageErrors.push(error)))
    first.on('pageerror', (error) => pageErrors.push(error))
    await first.goto(`${baseURL}/login`)
    await first.getByLabel('Email', { exact: true }).fill(email)
    await first.getByLabel('Mật khẩu', { exact: true }).fill(password)
    await first.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await first.waitForURL((url) =>
      /^\/(admin|manager|reception|trainer|member)(\/|$)/.test(url.pathname),
    )
    const protectedURL = first.url()
    const second = await context.newPage()
    await second.goto(protectedURL)
    const ready = (page) =>
      page.waitForFunction(async () => {
        const store = await import('/src/auth/session-store.ts')
        return store.getAccessToken() !== null
      })
    await Promise.all([ready(first), ready(second)])
    const previousCookie = (await context.cookies()).find((entry) => entry.name === 'refresh_token')
    assert(previousCookie?.httpOnly, 'Hai tab phải dùng cookie refresh HttpOnly thật.')

    const releaseFirst = deferred()
    let secondRequests = 0
    await first.route('**/api/v1/auth/refresh', async (route) => {
      await releaseFirst.promise
      await route.continue()
    })
    await second.route('**/api/v1/auth/refresh', async (route) => {
      secondRequests += 1
      await route.continue()
    })
    const refresh = (page) =>
      page
        .evaluate(async () => {
          const client = await import('/src/services/http.ts')
          await client.refreshAccessToken()
        })
        .then(
          () => null,
          (error) => error,
        )
    const firstStarted = first.waitForRequest(
      (request) => new URL(request.url()).pathname === '/api/v1/auth/refresh',
      { timeout: 10000 },
    )
    const firstResult = refresh(first)
    let secondResult
    try {
      await firstStarted
      secondResult = refresh(second)
      await second.waitForFunction(
        async () => {
          const locks = await navigator.locks.query()
          return (
            locks.held.some((lock) => lock.name === 'gym-auth-cookie') &&
            locks.pending.some((lock) => lock.name === 'gym-auth-cookie')
          )
        },
        undefined,
        { timeout: 5000 },
      )
      assert.equal(secondRequests, 0, 'Tab sau phải chờ khóa trước khi gửi cookie.')
    } finally {
      releaseFirst.resolve()
      await Promise.all([firstResult, secondResult])
    }
    assert.equal(await firstResult, null, 'Tab đầu refresh thành công.')
    assert.equal(await secondResult, null, 'Tab sau dùng cookie đã xoay và refresh thành công.')
    assert.equal(secondRequests, 1)
    await first.unroute('**/api/v1/auth/refresh')
    await second.unroute('**/api/v1/auth/refresh')

    const identity = async (page) => {
      const userId = await page.evaluate(async () => {
        const { http } = await import('/src/services/http.ts')
        return (await http.get('/auth/me')).data.userId
      })
      assert(Number.isSafeInteger(userId) && userId > 0, '/me phải trả userId số nguyên dương.')
      return userId
    }
    assert.equal(await identity(first), await identity(second))
    await Promise.all([first.reload(), second.reload()])
    await Promise.all([ready(first), ready(second)])
    assert.equal(await identity(first), await identity(second))
    assert.equal(pageErrors.length, 0, 'Không có lỗi JavaScript không được xử lý.')
    for (const page of [first, second]) {
      assert.equal(await page.evaluate(() => localStorage.length + sessionStorage.length), 0)
      assert.equal(await page.evaluate(() => document.cookie.includes('refresh_token=')), false)
    }
    const cookie = (await context.cookies()).find((entry) => entry.name === 'refresh_token')
    assert(cookie?.httpOnly, 'Refresh cookie phải là HttpOnly.')
    assert(cookie.value !== previousCookie.value, 'Refresh phải xoay cookie thật.')
    const replay = await context.request.post(`${baseURL}/api/v1/auth/refresh`, {
      headers: { Cookie: `refresh_token=${previousCookie.value}` },
    })
    assert.equal(replay.status(), 401, 'Token cũ đã dùng vẫn phải bị backend từ chối.')
  },
)
