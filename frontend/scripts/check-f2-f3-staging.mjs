import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'

const { chromium, request } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright')
const baseURL = process.env.GYM_TEST_URL
assert(baseURL?.startsWith('https://'), 'Set GYM_TEST_URL to the deployed HTTPS preview')
const checks = []
const api = await request.newContext({ baseURL, timeout: 45000 })
let browser
async function check(name, fn) {
  await fn()
  checks.push(name)
  console.log(`PASS ${name}`)
}
try {
  await check('HTTPS preview includes current auth and F2/F3 code', async () => {
    const html = await api.get('/login')
    assert.equal(html.status(), 200)
    assert.equal(new URL(html.url()).origin, new URL(baseURL).origin)
    const src = (await html.text()).match(/src="([^"]+\.js)"/)?.[1]
    assert(src, 'Expected built application module')
    const bundle = await api.get(src)
    assert.equal(bundle.status(), 200)
    const code = await bundle.text()
    for (const marker of ['Tài khoản đã đổi ở tab khác', 'Hồ sơ hội viên', 'Nhập lại mật khẩu']) {
      assert(code.includes(marker), `Missing deployed source marker: ${marker}`)
    }
  })
  await check('HTTPS proxy forwards anonymous auth/F2/F3 requests to backend', async () => {
    for (const path of ['/auth/me', '/members', '/packages']) {
      assert.equal((await api.get(`/api/v1${path}`)).status(), 401)
    }
  })
  browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({ baseURL })
  const page = await context.newPage()
  page.setDefaultTimeout(30000)
  const errors = []
  page.on('pageerror', (error) => errors.push(error.message))
  const suffix = String(Date.now())
  const email = `pr103-https-${suffix}@example.com`
  const phone = `094${suffix.slice(-7)}`
  const password = `Staging9!${suffix}`
  let bearer
  await check('HTTPS browser registration creates member and loads real identity', async () => {
    await page.goto('/register')
    await page.getByLabel('Họ và tên', { exact: true }).fill('Kiem thu HTTPS PR103')
    await page.getByLabel('Email', { exact: true }).fill(email)
    await page.getByLabel('Số điện thoại', { exact: true }).fill(phone)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
    await page.getByLabel('Nhập lại mật khẩu', { exact: true }).fill(password)
    const created = page.waitForResponse((r) => r.url().endsWith('/auth/register'))
    const identity = page.waitForResponse((r) => r.url().endsWith('/auth/me') && r.status() === 200)
    await page.getByRole('button', { name: 'Đăng ký', exact: true }).click()
    assert.equal((await created).status(), 201)
    const me = await identity
    assert.equal((await me.json()).role, 'MEMBER')
    bearer = (await me.request().allHeaders()).authorization
    assert(bearer)
    await page.waitForURL('**/member')
  })
  await check('HTTPS cookie has Secure/HttpOnly/SameSite/Path and is hidden from JS', async () => {
    const cookie = (await context.cookies()).find((c) => c.name === 'refresh_token')
    assert(cookie?.secure && cookie.httpOnly)
    assert.equal(cookie.sameSite, 'Strict')
    assert.equal(cookie.path, '/api/v1/auth')
    assert.equal(cookie.domain, new URL(baseURL).hostname)
    assert.equal(await page.evaluate(() => document.cookie.includes('refresh_token')), false)
    assert.deepEqual(
      await page.evaluate(() => [Object.keys(localStorage), Object.keys(sessionStorage)]),
      [[], []],
    )
  })
  await check('HTTPS reload refreshes cookie and retains member session', async () => {
    const refresh = page.waitForResponse((r) => r.url().endsWith('/auth/refresh'))
    const identity = page.waitForResponse((r) => r.url().endsWith('/auth/me') && r.status() === 200)
    await page.reload()
    assert.equal((await refresh).status(), 200)
    await identity
    await page.waitForURL('**/member')
  })
  await check('HTTPS new tab restores session', async () => {
    const tab = await context.newPage()
    const refresh = tab.waitForResponse((r) => r.url().endsWith('/auth/refresh'))
    await tab.goto('/member')
    assert.equal((await refresh).status(), 200)
    await tab.waitForURL('**/member')
    await tab.close()
  })
  await check('HTTPS F2/F3 APIs reject authenticated MEMBER', async () => {
    for (const path of ['/members', '/packages']) {
      assert.equal(
        (await api.get(`/api/v1${path}`, { headers: { Authorization: bearer } })).status(),
        403,
      )
    }
  })
  await check('HTTPS frontend F2/F3 routes reject MEMBER', async () => {
    for (const path of ['/members', '/packages']) {
      await page.goto(path)
      await page.waitForURL('**/403')
    }
  })
  await check('HTTPS logout invalidates session and protected route stays anonymous', async () => {
    await page.goto('/member')
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).waitFor()
    const logout = page.waitForResponse((r) => r.url().endsWith('/auth/logout'))
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
    assert.equal((await logout).status(), 204)
    await page.waitForURL('**/login')
    const refresh = page.waitForResponse((r) => r.url().endsWith('/auth/refresh'))
    await page.goto('/member')
    assert.equal((await refresh).status(), 401)
    await page.waitForURL('**/login')
  })
  await check('HTTPS wrong password is rejected and correct login works', async () => {
    await page.getByLabel('Email', { exact: true }).fill(email)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(`${password}wrong`)
    let response = page.waitForResponse((r) => r.url().endsWith('/auth/login'))
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    assert.equal((await response).status(), 401)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
    response = page.waitForResponse((r) => r.url().endsWith('/auth/login'))
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    assert.equal((await response).status(), 200)
    await page.waitForURL('**/member')
  })
  await check('HTTPS 360px auth pages fit viewport', async () => {
    await page.setViewportSize({ width: 360, height: 800 })
    for (const path of ['/register', '/login']) {
      await page.goto(path)
      await page.getByLabel('Email', { exact: true }).waitFor()
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    }
  })
  await check('HTTPS browser has no uncaught JavaScript errors', async () =>
    assert.deepEqual(errors, []),
  )
  await context.request.post('/api/v1/auth/logout')
  const report = {
    baseURL,
    timestamp: new Date().toISOString(),
    browser: browser.version(),
    passed: checks.length,
    frontendCommit: process.env.GYM_TEST_COMMIT ?? 'check GitHub deployment status',
    checks,
    testEmail: email,
    limitations: [
      'No authorized staff staging account: positive F2/F3 CRUD and branch scope remain locally verified only',
      'Backend deployment commit is not exposed publicly',
    ],
  }
  if (process.env.GYM_TEST_OUTPUT) {
    await mkdir(process.env.GYM_TEST_OUTPUT, { recursive: true })
    await writeFile(
      `${process.env.GYM_TEST_OUTPUT}/staging-results.json`,
      JSON.stringify(report, null, 2),
    )
  }
  console.log(JSON.stringify(report, null, 2))
} finally {
  await browser?.close()
  await api.dispose()
}
