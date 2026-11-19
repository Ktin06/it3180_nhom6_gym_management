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
    context.on('response', (response) => {
      const path = new URL(response.url()).pathname
      if (path.startsWith('/api/v1/auth/')) {
        t.diagnostic(
          `${response.request().method()} ${path}: ${response.status()}, bearer=${Boolean(response.request().headers().authorization)}`,
        )
      }
    })
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
    // Chờ bootstrap hoàn tất và route bảo vệ thực sự mở trước khi gọi API nghiệp vụ.
    const ready = (page) => page.getByRole('button', { name: 'Đăng xuất', exact: true }).waitFor()
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
    assert.equal(await secondResult, null, 'Tab sau dùng access token được chia sẻ.')
    assert.equal(secondRequests, 0, 'Tab chờ dùng kết quả của tab đầu, không gửi request thứ hai.')
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
      const storage = await page.evaluate(() => ({
        local: Object.entries(localStorage),
        session: Object.keys(sessionStorage),
        cookieVisible: document.cookie.includes('refresh_token='),
      }))
      assert.deepEqual(storage.session, [])
      assert.equal(storage.cookieVisible, false)
      assert.equal(storage.local.length, 1)
      assert.equal(storage.local[0][0], 'gym-auth-cookie-revision')
      const marker = JSON.parse(storage.local[0][1])
      assert.deepEqual(Object.keys(marker).sort(), ['refresh', 'session'])
      for (const revision of Object.values(marker)) {
        assert.match(revision, /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/)
      }
    }
    const cookie = (await context.cookies()).find((entry) => entry.name === 'refresh_token')
    assert(cookie?.httpOnly, 'Refresh cookie phải là HttpOnly.')
    assert(cookie.value !== previousCookie.value, 'Refresh phải xoay cookie thật.')

    const otherEmail = process.env.GYM_E2E_OTHER_EMAIL
    assert(otherEmail, 'Cần GYM_E2E_OTHER_EMAIL của tài khoản kiểm thử thứ hai.')
    const userA = await identity(first)
    const releaseLogin = deferred()
    let staleLogoutRequests = 0
    first.on('request', (request) => {
      if (new URL(request.url()).pathname === '/api/v1/auth/logout') staleLogoutRequests += 1
    })
    await second.goto(`${baseURL}/login`)
    await second.route('**/api/v1/auth/login', async (route) => {
      await releaseLogin.promise
      await route.continue()
    })
    await second.getByLabel('Email', { exact: true }).fill(otherEmail)
    await second.getByLabel('Mật khẩu', { exact: true }).fill(password)
    const loginStarted = second.waitForRequest('**/api/v1/auth/login')
    await second.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    try {
      await loginStarted
      await first.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
      await first.waitForFunction(async () =>
        (await navigator.locks.query()).pending.some((lock) => lock.name === 'gym-auth-cookie'),
      )
    } finally {
      releaseLogin.resolve()
    }

    await second.waitForURL((url) => url.pathname === new URL(protectedURL).pathname)
    await ready(second)
    await first.waitForURL('**/login')
    const userB = await identity(second)
    assert.notEqual(userA, userB, 'Hai tab phải thực sự mang hai chủ thể khác nhau.')
    const cookieB = (await context.cookies()).find((entry) => entry.name === 'refresh_token')
    assert(cookieB?.httpOnly, 'Đăng xuất chờ từ A không được xóa cookie của B.')
    assert.equal(staleLogoutRequests, 0, 'Yêu cầu đăng xuất cũ không được gửi với cookie mới.')
    assert.equal(await refresh(second), null, 'B vẫn refresh được sau khi A thoát phiên.')
    assert.equal(await identity(second), userB)

    await first.goto(second.url())
    await ready(first)
    assert.equal(await identity(first), userB)
    const logoutResponse = second.waitForResponse('**/api/v1/auth/logout')
    await second.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
    const response = await logoutResponse
    assert.equal(response.status(), 204)
    assert.match(await response.request().headerValue('authorization'), /^Bearer /)
    try {
      await Promise.all([
        first.waitForURL('**/login', { timeout: 5000 }),
        second.waitForURL('**/login', { timeout: 5000 }),
      ])
    } catch (error) {
      for (const [name, page] of [
        ['first', first],
        ['second', second],
      ]) {
        const result = await page.evaluate(async () => {
          const store = await import('/src/auth/session-store.ts')
          return {
            path: location.pathname,
            tokenInMemory: Boolean(store.getAccessToken()),
          }
        })
        t.diagnostic(`${name}: ${JSON.stringify(result)}`)
      }
      t.diagnostic(
        `cookiePresent: ${(await context.cookies()).some((entry) => entry.name === 'refresh_token')}`,
      )
      t.diagnostic(`pageErrors: ${pageErrors.map((error) => error.name).join(', ')}`)
      throw error
    }
    assert.equal(
      (await context.cookies()).some((entry) => entry.name === 'refresh_token'),
      false,
    )
    assert.equal(
      pageErrors.length,
      0,
      'Không có lỗi JavaScript khi đổi tài khoản hoặc đăng xuất các tab.',
    )
  },
)
