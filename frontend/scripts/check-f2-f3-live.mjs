import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'

// Optional external Playwright installation keeps the app's dependencies unchanged.
const { chromium, request } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright')
const baseURL = process.env.GYM_TEST_URL ?? 'http://localhost:5173'
const password = process.env.GYM_TEST_PASSWORD
assert(password, 'Set GYM_TEST_PASSWORD to the disposable local seed password')
const checks = []
const clients = []
let browser
async function check(name, fn) {
  await fn()
  checks.push(name)
  console.log(`PASS ${name}`)
}
async function api(email) {
  const client = await request.newContext({ baseURL, extraHTTPHeaders: { Origin: baseURL } })
  clients.push(client)
  const login = await client.post('/api/v1/auth/login', { data: { email, password } })
  assert.equal(login.status(), 200)
  const token = (await login.json()).accessToken
  return { client, headers: { Authorization: `Bearer ${token}` } }
}
async function waitFor(fn) {
  for (let i = 0; i < 100; i++) {
    if (await fn()) return
    await new Promise((resolve) => setTimeout(resolve, 100))
  }
  throw new Error('Condition did not become true within 10 seconds')
}
async function uiLogin(page, email, path) {
  await page.goto(path)
  await page.waitForURL('**/login')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
}
try {
  const anonymous = await request.newContext({ baseURL })
  clients.push(anonymous)
  await check('API denies anonymous members and packages', async () => {
    for (const path of ['/members', '/packages']) {
      assert.equal((await anonymous.get(`/api/v1${path}`)).status(), 401)
    }
  })
  const actors = {}
  for (const [email, role, expected] of [
    ['admin@example.com', 'SUPER_ADMIN', 200],
    ['manager@example.com', 'BRANCH_MANAGER', 200],
    ['receptionist@example.com', 'RECEPTIONIST', 200],
    ['trainer1@example.com', 'TRAINER', 403],
    ['member1@example.com', 'MEMBER', 403],
  ]) {
    await check(`API role ${role}`, async () => {
      const actor = await api(email)
      actors[role] = actor
      for (const path of ['/members', '/packages']) {
        assert.equal(
          (await actor.client.get(`/api/v1${path}`, { headers: actor.headers })).status(),
          expected,
        )
      }
    })
  }
  const admin = actors.SUPER_ADMIN
  const reception = actors.RECEPTIONIST
  const list = async (actor, path) => {
    const response = await actor.client.get(`/api/v1${path}`, { headers: actor.headers })
    assert.equal(response.status(), 200)
    return response.json()
  }
  await check('API enforces member branch scope for list/detail/update', async () => {
    const all = await list(admin, '/members?keyword=HV888888')
    assert.equal(all.content.length, 1)
    assert.equal((await list(reception, '/members?keyword=HV888888')).totalElements, 0)
    const foreign = all.content[0]
    assert.equal(
      (
        await reception.client.get(`/api/v1/members/${foreign.id}`, { headers: reception.headers })
      ).status(),
      404,
    )
    assert.equal(
      (
        await reception.client.put(`/api/v1/members/${foreign.id}`, {
          headers: reception.headers,
          data: {
            fullName: foreign.fullName,
            phone: foreign.phone,
            gender: null,
            dob: null,
            status: 'ACTIVE',
          },
        })
      ).status(),
      404,
    )
  })
  await check('API enforces package branch scope and filters', async () => {
    assert((await list(admin, '/packages')).content.some((p) => p.code === 'CN02_ONLY'))
    assert(!(await list(reception, '/packages')).content.some((p) => p.code === 'CN02_ONLY'))
    const filtered = await list(reception, '/packages?type=SESSION&status=INACTIVE')
    assert(filtered.content.some((p) => p.code === 'CN01_INACTIVE'))
    assert(filtered.content.every((p) => p.type === 'SESSION' && p.status === 'INACTIVE'))
  })
  const run = String(Date.now()).slice(-7)
  const phone = `092${run}`
  const name = `Live integration ${run}`
  browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({ baseURL })
  const page = await context.newPage()
  const jsErrors = []
  page.on('pageerror', (error) => jsErrors.push(error.message))
  let created
  await check('Browser returns to /members after real login', async () => {
    await uiLogin(page, 'receptionist@example.com', '/members')
    await page.waitForURL('**/members')
    await page.getByRole('heading', { name: 'Hồ sơ hội viên' }).waitFor()
    assert.equal(await page.getByText('Dữ liệu minh họa', { exact: true }).count(), 0)
  })
  await check('Browser creates a member through real API', async () => {
    await page.getByRole('button', { name: 'Thêm hội viên' }).click()
    await page.getByLabel('Họ và tên', { exact: true }).fill(name)
    await page.getByLabel('Số điện thoại', { exact: true }).fill(phone)
    const responsePromise = page.waitForResponse(
      (r) => r.url().endsWith('/api/v1/members') && r.request().method() === 'POST',
    )
    await page.getByRole('button', { name: 'Lưu hồ sơ' }).click()
    const response = await responsePromise
    assert.equal(response.status(), 201)
    created = await response.json()
    await page.getByRole('dialog').waitFor({ state: 'hidden' })
    await page.getByText(created.memberCode, { exact: true }).waitFor()
  })
  await check('Browser reload restores session and persisted member', async () => {
    const refresh = page.waitForResponse((r) => r.url().endsWith('/auth/refresh'))
    await page.reload()
    assert.equal((await refresh).status(), 200)
    await page.getByText(created.memberCode, { exact: true }).waitFor()
    const detail = await reception.client.get(`/api/v1/members/${created.id}`, {
      headers: reception.headers,
    })
    assert.equal((await detail.json()).phone, phone)
  })
  await check('Browser pagination requests page 1 and returns to first page', async () => {
    const next = page.waitForResponse(
      (r) => r.url().includes('/members?') && new URL(r.url()).searchParams.get('page') === '1',
    )
    await page.getByTitle('2', { exact: true }).click()
    const response = await next
    assert.equal(response.status(), 200)
    assert.equal((await response.json()).page, 1)
    await waitFor(async () => !(await page.getByText(created.memberCode, { exact: true }).count()))
    await page.getByTitle('1', { exact: true }).click()
    await page.getByText(created.memberCode, { exact: true }).waitFor()
  })
  await check('Browser searches by phone and member code', async () => {
    for (const keyword of [phone, created.memberCode, name]) {
      await page.getByLabel('Tìm hội viên', { exact: true }).fill(keyword)
      const response = page.waitForResponse(
        (r) => r.url().includes('/members?') && r.url().includes('keyword='),
      )
      await page.getByRole('button', { name: 'Tìm', exact: true }).click()
      assert.equal((await response).status(), 200)
      await waitFor(async () => (await page.locator('tbody tr[data-row-key]').count()) === 1)
      await page.getByText(created.memberCode, { exact: true }).waitFor()
    }
  })
  await check('Browser edits member and persists INACTIVE status', async () => {
    await page.getByRole('button', { name: `Sửa ${created.memberCode}` }).click()
    await page.getByLabel('Họ và tên', { exact: true }).fill(`${name} updated`)
    await page.getByLabel('Trạng thái', { exact: true }).last().click()
    await page.getByTitle('Ngừng hoạt động', { exact: true }).click()
    const response = page.waitForResponse(
      (r) => r.url().endsWith(`/members/${created.id}`) && r.request().method() === 'PUT',
    )
    await page.getByRole('button', { name: 'Lưu hồ sơ' }).click()
    assert.equal((await response).status(), 200)
    await page.getByRole('dialog').waitFor({ state: 'hidden' })
    await page.reload()
    const persisted = await (
      await reception.client.get(`/api/v1/members/${created.id}`, { headers: reception.headers })
    ).json()
    assert.equal(persisted.status, 'INACTIVE')
    assert.equal(persisted.fullName, `${name} updated`)
    await page.getByText(`${name} updated`, { exact: true }).waitFor()
  })
  await check('Browser preserves form on duplicate phone 409', async () => {
    await page.getByRole('button', { name: 'Thêm hội viên' }).click()
    await page.getByLabel('Họ và tên', { exact: true }).fill('Duplicate phone integration')
    await page.getByLabel('Số điện thoại', { exact: true }).fill(phone)
    const response = page.waitForResponse(
      (r) => r.url().endsWith('/members') && r.request().method() === 'POST',
    )
    await page.getByRole('button', { name: 'Lưu hồ sơ' }).click()
    assert.equal((await response).status(), 409)
    await page.getByRole('dialog').getByRole('alert').waitFor()
    assert.equal(await page.getByLabel('Số điện thoại', { exact: true }).inputValue(), phone)
    await page.getByRole('button', { name: 'Hủy', exact: true }).click()
  })
  await check('Browser shows real backend date validation at dob field', async () => {
    await page.getByRole('button', { name: 'Thêm hội viên' }).click()
    await page.getByLabel('Họ và tên', { exact: true }).fill('Future birthday integration')
    await page.getByLabel('Số điện thoại', { exact: true }).fill(`093${run}`)
    await page.getByLabel('Ngày sinh (tùy chọn)', { exact: true }).fill('2099-01-01')
    const response = page.waitForResponse(
      (r) => r.url().endsWith('/members') && r.request().method() === 'POST',
    )
    await page.getByRole('button', { name: 'Lưu hồ sơ' }).click()
    const result = await response
    assert.equal(result.status(), 400)
    const problem = await result.json()
    assert(problem.errors.dob)
    await page.getByText(problem.errors.dob, { exact: true }).waitFor()
    await page.getByRole('button', { name: 'Hủy', exact: true }).click()
  })
  await check('Browser filters inactive members using real API', async () => {
    await page.getByLabel('Trạng thái', { exact: true }).click()
    const response = page.waitForResponse(
      (r) => r.url().includes('/members?') && r.url().includes('status=INACTIVE'),
    )
    await page.getByTitle('Ngừng hoạt động', { exact: true }).click()
    const data = await (await response).json()
    assert(data.content.length > 0)
    assert(data.content.every((m) => m.status === 'INACTIVE'))
    await page.getByText(created.memberCode, { exact: true }).waitFor()
  })
  await check('Browser filters live packages and hides foreign branch', async () => {
    await page.goto('/packages')
    await page.getByText('TIME_1M', { exact: true }).waitFor()
    assert.equal(await page.getByText('CN02_ONLY', { exact: true }).count(), 0)
    await page.getByLabel('Loại gói', { exact: true }).click()
    await page.getByTitle('Theo buổi', { exact: true }).click()
    await page.getByText('SES_10', { exact: true }).waitFor()
    await page.getByLabel('Trạng thái', { exact: true }).click()
    await page.getByTitle('Ngừng bán', { exact: true }).click()
    await page.getByText('CN01_INACTIVE', { exact: true }).waitFor()
    assert.equal(await page.getByText('SES_10', { exact: true }).count(), 0)
  })
  await check('Browser refreshes rejected bearer and retries business request', async () => {
    // Make the next outgoing bearer invalid; backend returns a real 401.
    // No response, refresh cookie or API data is mocked.
    await page.route(
      '**/api/v1/members?*',
      async (route) => {
        await route.continue({
          headers: {
            ...route.request().headers(),
            authorization: 'Bearer expired-integration-token',
          },
        })
      },
      { times: 1 },
    )
    const refresh = page.waitForResponse((r) => r.url().endsWith('/auth/refresh'))
    const success = page.waitForResponse((r) => r.url().includes('/members?') && r.status() === 200)
    await page.getByRole('menuitem', { name: /Hội viên/ }).click()
    assert.equal((await refresh).status(), 200)
    await success
    await page.getByText(created.memberCode, { exact: true }).waitFor()
  })
  await check('Browser cookies and Web Storage', async () => {
    const cookie = (await context.cookies()).find((c) => c.name === 'refresh_token')
    assert(cookie?.httpOnly)
    assert.equal(cookie.path, '/api/v1/auth')
    assert.equal(cookie.sameSite, 'Strict')
    assert.equal(await page.evaluate(() => document.cookie.includes('refresh_token')), false)
    assert.deepEqual(
      await page.evaluate(() => [Object.keys(localStorage), Object.keys(sessionStorage)]),
      [[], []],
    )
  })
  await check('Browser 360px members/packages fit viewport', async () => {
    await page.setViewportSize({ width: 360, height: 800 })
    for (const path of ['/members', '/packages']) {
      await page.goto(path)
      await page.locator('tbody tr[data-row-key]').first().waitFor()
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    }
  })
  await check('Browser logout invalidates cookie and reload stays anonymous', async () => {
    const response = page.waitForResponse((r) => r.url().endsWith('/auth/logout'))
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
    assert.equal((await response).status(), 204)
    await page.waitForURL('**/login')
    await page.goto('/members')
    await page.waitForURL('**/login')
  })
  await context.close()
  for (const [email, role, target] of [
    ['manager@example.com', 'BRANCH_MANAGER', '/packages'],
    ['admin@example.com', 'SUPER_ADMIN', '/members'],
    ['trainer1@example.com', 'TRAINER', '/trainer'],
    ['member1@example.com', 'MEMBER', '/member'],
  ]) {
    await check(`Browser route permissions ${role}`, async () => {
      const isolated = await browser.newContext({ baseURL })
      const tab = await isolated.newPage()
      await uiLogin(tab, email, target === '/packages' ? '/packages' : '/members')
      await tab.waitForURL(`**${target}`)
      if (role === 'TRAINER' || role === 'MEMBER') {
        await tab.goto('/members')
        await tab.waitForURL('**/403')
        await tab.goto('/packages')
        await tab.waitForURL('**/403')
      }
      await isolated.close()
    })
  }
  await check('Browser rejects a different account refreshed from a shared cookie', async () => {
    const shared = await browser.newContext({ baseURL })
    const first = await shared.newPage()
    await uiLogin(first, 'receptionist@example.com', '/members')
    await first.waitForURL('**/members')
    await first.getByText(created.memberCode, { exact: true }).waitFor()
    const second = await shared.newPage()
    const identity = second.waitForResponse(
      (r) => r.url().endsWith('/auth/me') && r.status() === 200,
    )
    await second.goto('/login')
    // Let bootstrap finish before explicitly logging into account B.
    await identity
    await second.getByLabel('Email', { exact: true }).fill('admin@example.com')
    await second.getByLabel('Mật khẩu', { exact: true }).fill(password)
    await second.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await second.waitForURL('**/admin')
    await first.bringToFront()
    assert.equal(new URL(first.url()).pathname, '/members')
    const operations = []
    first.on('request', (r) => {
      if (r.url().includes('/api/v1/members?') || r.url().endsWith('/auth/logout')) {
        operations.push(r.url())
      }
    })
    await first.route(
      '**/api/v1/members?*',
      (route) =>
        route.continue({
          headers: {
            ...route.request().headers(),
            authorization: 'Bearer expired-integration-token',
          },
        }),
      { times: 1 },
    )
    const refresh = first.waitForResponse((r) => r.url().endsWith('/auth/refresh'))
    await first.getByRole('button', { name: /Tải lại/ }).click()
    assert.equal((await refresh).status(), 200)
    await first.waitForURL('**/login')
    await first
      .getByText('Tài khoản đã đổi ở tab khác. Vui lòng đăng nhập lại.', { exact: true })
      .waitFor()
    assert.equal(operations.length, 1, 'No retry with B and no logout of the shared cookie')
    assert.equal((await second.request.post('/api/v1/auth/refresh')).status(), 200)
    await shared.close()
  })
  await check('No uncaught browser JavaScript errors', async () => assert.deepEqual(jsErrors, []))
  const report = {
    baseURL,
    timestamp: new Date().toISOString(),
    passed: checks.length,
    browser: browser.version(),
    checks,
    memberCode: created.memberCode,
  }
  if (process.env.GYM_TEST_OUTPUT) {
    await mkdir(process.env.GYM_TEST_OUTPUT, { recursive: true })
    await writeFile(`${process.env.GYM_TEST_OUTPUT}/results.json`, JSON.stringify(report, null, 2))
  }
  console.log(JSON.stringify(report, null, 2))
} finally {
  await browser?.close()
  for (const client of clients) await client.dispose()
}
