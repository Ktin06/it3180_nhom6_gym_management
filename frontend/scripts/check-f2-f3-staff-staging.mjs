import assert from 'node:assert/strict'
import { randomInt } from 'node:crypto'
import { mkdir, readFile, writeFile } from 'node:fs/promises'

const { chromium, request } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright')
const baseURL = process.env.GYM_TEST_URL
assert(baseURL?.startsWith('https://'), 'Set GYM_TEST_URL to the HTTPS preview')
assert(
  process.env.GYM_STAFF_CREDENTIALS,
  'Set GYM_STAFF_CREDENTIALS to a private JSON file outside the repository',
)
const credentials = JSON.parse(await readFile(process.env.GYM_STAFF_CREDENTIALS, 'utf8'))
const checks = []
const members = []
const limitations = []
const errors = []
let browser
let failure
const contexts = []
let foreignMember
let foreignOwner
let cn01Member
let knownFixture
function redact(value) {
  let text = String(value)
  for (const account of Object.values(credentials)) {
    for (const secret of [account.email, account.password]) {
      if (secret) text = text.replaceAll(secret, '[redacted]')
    }
  }
  return text
    .replace(/Bearer\s+[A-Za-z0-9._~-]+/gi, 'Bearer [redacted]')
    .replace(/(refresh_token=)[^;\s]+/gi, '$1[redacted]')
}
async function check(name, fn) {
  await fn()
  checks.push(name)
  console.log(`PASS ${name}`)
}
try {
  browser = await chromium.launch({ channel: 'msedge', headless: true })
  for (const [key, role] of [
    ...(credentials.cn02 ? [['cn02', 'RECEPTIONIST']] : []),
    ['manager', 'BRANCH_MANAGER'],
    ['receptionist', 'RECEPTIONIST'],
  ]) {
    if (process.env.GYM_STAFF_ROLE && process.env.GYM_STAFF_ROLE !== key) continue
    const account = credentials[key]
    const label = `${role} ${key === 'cn02' ? 'CN02' : 'CN01'}`
    assert(account?.email && account.password, `Missing credentials for ${role}`)
    const api = await request.newContext({ baseURL, timeout: 45000 })
    contexts.push(api)
    let headers
    await check(`${label}: real login and server role`, async () => {
      const response = await api.post('/api/v1/auth/login', { data: account })
      assert.equal(response.status(), 200, `${role} login status`)
      headers = { Authorization: `Bearer ${(await response.json()).accessToken}` }
      const me = await api.get('/api/v1/auth/me', { headers })
      assert.equal(me.status(), 200)
      assert.equal((await me.json()).role, role)
    })
    if (key === 'cn02') {
      await check('CN02: fixture HV888888 really exists in another branch', async () => {
        const response = await api.get('/api/v1/members?keyword=HV888888', { headers })
        assert.equal(response.status(), 200)
        const data = await response.json()
        assert.equal(data.totalElements, 1)
        assert.equal(data.content[0].memberCode, 'HV888888')
        assert.equal(data.content[0].phone, '0988000001')
        knownFixture = data.content[0]
      })
    }
    const context = await browser.newContext({ baseURL })
    contexts.push(context)
    const page = await context.newPage()
    page.on('pageerror', (error) => errors.push(redact(error.message)))
    const phone = `095${String(randomInt(10000000)).padStart(7, '0')}`
    const name = `PR103 Staff ${key} ${Date.now()}`
    let created
    await check(`${label}: HTTPS login returns to members`, async () => {
      await page.goto('/members')
      await page.waitForURL('**/login')
      await page.getByLabel('Email', { exact: true }).fill(account.email)
      await page.getByLabel('Mật khẩu', { exact: true }).fill(account.password)
      await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
      await page.waitForURL('**/members')
      await page.getByRole('heading', { name: 'Hồ sơ hội viên' }).waitFor()
      assert.equal(await page.getByText('Dữ liệu minh họa', { exact: true }).count(), 0)
    })
    await check(`${label}: create member via UI and real API`, async () => {
      await page.getByRole('button', { name: 'Thêm hội viên' }).click()
      await page.getByLabel('Họ và tên', { exact: true }).fill(name)
      await page.getByLabel('Số điện thoại', { exact: true }).fill(phone)
      const response = page.waitForResponse(
        (r) => r.url().endsWith('/members') && r.request().method() === 'POST',
      )
      await page.getByRole('button', { name: 'Lưu hồ sơ' }).click()
      const result = await response
      assert.equal(result.status(), 201)
      created = await result.json()
      assert(created.homeBranchId > 0)
      if (key === 'cn02') {
        assert.equal(created.homeBranchId, knownFixture.homeBranchId)
        foreignMember = created
        foreignOwner = { api, headers, name: `${name} edited` }
      } else if (foreignMember) {
        assert.notEqual(created.homeBranchId, foreignMember.homeBranchId)
        cn01Member ??= created
      }
      members.push({
        role,
        branchAccount: key,
        id: created.id,
        memberCode: created.memberCode,
        homeBranchId: created.homeBranchId,
      })
      await page.getByRole('dialog').waitFor({ state: 'hidden' })
      await page.getByText(created.memberCode, { exact: true }).waitFor()
    })
    await check(`${label}: reload restores session and persisted record`, async () => {
      const refresh = page.waitForResponse((r) => r.url().endsWith('/auth/refresh'))
      await page.reload()
      assert.equal((await refresh).status(), 200)
      await page.getByText(created.memberCode, { exact: true }).waitFor()
      const detail = await api.get(`/api/v1/members/${created.id}`, { headers })
      assert.equal(detail.status(), 200)
      assert.equal((await detail.json()).phone, phone)
    })
    await check(`${label}: pending keyword is applied when changing status`, async () => {
      await page.getByLabel('Tìm hội viên', { exact: true }).fill(phone)
      await page.getByLabel('Trạng thái', { exact: true }).click()
      const result = page.waitForResponse(
        (r) =>
          r.url().includes('/members?') && new URL(r.url()).searchParams.get('keyword') === phone,
      )
      await page
        .locator('.ant-select-dropdown:visible')
        .getByTitle('Đang hoạt động', { exact: true })
        .click()
      const response = await result
      assert.equal(response.status(), 200)
      const data = await response.json()
      assert.equal(data.totalElements, 1)
      assert.equal(data.content[0].id, created.id)
      assert.equal(data.content[0].status, 'ACTIVE')
    })
    await check(`${label}: edit name/status and persist after reload`, async () => {
      await page.getByRole('button', { name: `Sửa ${created.memberCode}` }).click()
      await page.getByLabel('Họ và tên', { exact: true }).fill(`${name} edited`)
      await page.getByRole('dialog').getByLabel('Trạng thái', { exact: true }).click()
      await page
        .locator('.ant-select-dropdown:visible')
        .getByTitle('Ngừng hoạt động', { exact: true })
        .click()
      const result = page.waitForResponse(
        (r) => r.url().endsWith(`/members/${created.id}`) && r.request().method() === 'PUT',
      )
      await page.getByRole('button', { name: 'Lưu hồ sơ' }).click()
      assert.equal((await result).status(), 200)
      await page.getByRole('dialog').waitFor({ state: 'hidden' })
      await page.reload()
      await page.getByText(`${name} edited`, { exact: true }).waitFor()
      const detail = await (await api.get(`/api/v1/members/${created.id}`, { headers })).json()
      assert.equal(detail.fullName, `${name} edited`)
      assert.equal(detail.status, 'INACTIVE')
    })
    await check(`${label}: search by code/name and filter INACTIVE`, async () => {
      for (const keyword of [created.memberCode, name]) {
        await page.getByLabel('Tìm hội viên', { exact: true }).fill(keyword)
        const result = page.waitForResponse(
          (r) =>
            r.url().includes('/members?') &&
            new URL(r.url()).searchParams.get('keyword') === keyword,
        )
        await page.getByRole('button', { name: 'Tìm', exact: true }).click()
        assert.equal((await (await result).json()).content[0].id, created.id)
      }
      await page.getByLabel('Trạng thái', { exact: true }).click()
      const result = page.waitForResponse(
        (r) => r.url().includes('/members?') && r.url().includes('status=INACTIVE'),
      )
      await page
        .locator('.ant-select-dropdown:visible')
        .getByTitle('Ngừng hoạt động', { exact: true })
        .click()
      const data = await (await result).json()
      assert.equal(data.totalElements, 1)
      assert(data.content.every((m) => m.status === 'INACTIVE'))
    })
    await check(`${label}: duplicate phone is rejected without losing form`, async () => {
      await page.getByRole('button', { name: 'Thêm hội viên' }).click()
      await page.getByLabel('Họ và tên', { exact: true }).fill('PR103 duplicate test')
      await page.getByLabel('Số điện thoại', { exact: true }).fill(phone)
      const result = page.waitForResponse(
        (r) => r.url().endsWith('/members') && r.request().method() === 'POST',
      )
      await page.getByRole('button', { name: 'Lưu hồ sơ' }).click()
      assert.equal((await result).status(), 409)
      await page.getByRole('dialog').getByRole('alert').waitFor()
      assert.equal(await page.getByLabel('Số điện thoại', { exact: true }).inputValue(), phone)
      await page.getByRole('button', { name: 'Hủy', exact: true }).click()
    })
    await check(`${label}: scoped member list stays in account branch`, async () => {
      const first = await api.get('/api/v1/members?size=100', { headers })
      assert.equal(first.status(), 200)
      const data = await first.json()
      assert(data.content.every((m) => m.homeBranchId === created.homeBranchId))
      for (let i = 1; i < data.totalPages; i++) {
        const next = await (await api.get(`/api/v1/members?size=100&page=${i}`, { headers })).json()
        assert(next.content.every((m) => m.homeBranchId === created.homeBranchId))
      }
      const override = await api.get('/api/v1/members?size=100&branchId=999999', { headers })
      assert.equal(override.status(), 200)
      assert.equal((await override.json()).totalElements, data.totalElements)
    })
    await check(
      `${label}: catalog loads with branch/global scope and type/status filters`,
      async () => {
        const response = page.waitForResponse(
          (r) => r.url().includes('/packages?') && r.status() === 200,
        )
        await page.goto('/packages')
        const data = await (await response).json()
        assert(data.content.length > 0)
        assert(
          data.content.every((p) => p.branchId === null || p.branchId === created.homeBranchId),
        )
        await page.getByLabel('Loại gói', { exact: true }).click()
        const typed = page.waitForResponse(
          (r) => r.url().includes('/packages?') && r.url().includes('type=SESSION'),
        )
        await page
          .locator('.ant-select-dropdown:visible')
          .getByTitle('Theo buổi', { exact: true })
          .click()
        const filtered = await (await typed).json()
        assert(filtered.content.every((p) => p.type === 'SESSION' && p.status === 'ACTIVE'))
        await page.getByLabel('Trạng thái', { exact: true }).click()
        const inactive = page.waitForResponse(
          (r) => r.url().includes('/packages?') && r.url().includes('status=INACTIVE'),
        )
        await page
          .locator('.ant-select-dropdown:visible')
          .getByTitle('Ngừng bán', { exact: true })
          .click()
        assert(
          (await (await inactive).json()).content.every(
            (p) => p.type === 'SESSION' && p.status === 'INACTIVE',
          ),
        )
      },
    )
    if (key !== 'cn02' && (foreignMember || process.env.GYM_FOREIGN_MEMBER_ID)) {
      await check(`${label}: known foreign member cannot be read or updated`, async () => {
        const path = `/api/v1/members/${foreignMember?.id ?? Number(process.env.GYM_FOREIGN_MEMBER_ID)}`
        assert.equal((await api.get(path, { headers })).status(), 404)
        assert.equal(
          (
            await api.put(path, {
              headers,
              data: {
                fullName: 'PR103 denied cross branch',
                phone,
                gender: null,
                dob: null,
                status: 'ACTIVE',
              },
            })
          ).status(),
          404,
        )
      })
      if (foreignMember) {
        await check(
          `${label}: cannot search CN02 fixture or new member, even overriding branchId`,
          async () => {
            for (const keyword of [
              knownFixture.memberCode,
              foreignMember.memberCode,
              foreignMember.phone,
            ]) {
              const response = await api.get(
                `/api/v1/members?keyword=${encodeURIComponent(keyword)}&branchId=${foreignMember.homeBranchId}`,
                { headers },
              )
              assert.equal(response.status(), 200)
              assert.equal((await response.json()).totalElements, 0)
            }
            assert.equal(
              (await api.get(`/api/v1/members/${knownFixture.id}`, { headers })).status(),
              404,
            )
          },
        )
        await check(`${label}: browser search hides new CN02 record`, async () => {
          await page.goto('/members')
          await page.getByLabel('Tìm hội viên', { exact: true }).fill(foreignMember.memberCode)
          const result = page.waitForResponse(
            (r) =>
              r.url().includes('/members?') &&
              new URL(r.url()).searchParams.get('keyword') === foreignMember.memberCode,
          )
          await page.getByRole('button', { name: 'Tìm', exact: true }).click()
          assert.equal((await (await result).json()).totalElements, 0)
          await page.getByText('Không có hội viên phù hợp.', { exact: true }).waitFor()
          assert.equal(await page.getByText(foreignMember.memberCode, { exact: true }).count(), 0)
        })
      }
    }
    await check(`${label}: Secure cookie and logout`, async () => {
      const cookie = (await context.cookies()).find((c) => c.name === 'refresh_token')
      assert(cookie?.secure && cookie.httpOnly)
      assert.equal(cookie.sameSite, 'Strict')
      assert.equal(cookie.path, '/api/v1/auth')
      const result = page.waitForResponse((r) => r.url().endsWith('/auth/logout'))
      await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click()
      assert.equal((await result).status(), 204)
      await page.waitForURL('**/login')
      await page.goto('/members')
      await page.waitForURL('**/login')
    })
    await api.post('/api/v1/auth/logout')
    await context.close()
  }
  if (foreignMember && cn01Member) {
    await check('CN02: denied CN01 attempts did not modify the owned member', async () => {
      const response = await foreignOwner.api.get(`/api/v1/members/${foreignMember.id}`, {
        headers: foreignOwner.headers,
      })
      assert.equal(response.status(), 200)
      const data = await response.json()
      assert.equal(data.fullName, foreignOwner.name)
      assert.equal(data.phone, foreignMember.phone)
      assert.equal(data.status, 'INACTIVE')
      assert.equal(data.homeBranchId, foreignMember.homeBranchId)
    })
    await check('CN02: reciprocal access to an existing CN01 member is denied', async () => {
      const response = await foreignOwner.api.get(`/api/v1/members/${cn01Member.id}`, {
        headers: foreignOwner.headers,
      })
      assert.equal(response.status(), 404)
      const list = await foreignOwner.api.get(`/api/v1/members?keyword=${cn01Member.memberCode}`, {
        headers: foreignOwner.headers,
      })
      assert.equal((await list.json()).totalElements, 0)
    })
  }
  await check('No uncaught browser errors', async () => assert.deepEqual(errors, []))
  if (!foreignMember && !process.env.GYM_FOREIGN_MEMBER_ID)
    limitations.push(
      'No independently identified foreign-branch member was provided: direct cross-branch read/update denial is not yet demonstrated',
    )
  if (process.env.GYM_STAFF_ROLE) limitations.push('This run covered only the selected staff role')
  limitations.push('Backend deployment revision is not independently exposed')
} catch (error) {
  failure = redact(error.message)
  console.error(`FAIL ${failure}`)
  process.exitCode = 1
} finally {
  const report = {
    baseURL,
    timestamp: new Date().toISOString(),
    frontendCommit: process.env.GYM_TEST_COMMIT,
    browser: browser?.version(),
    passed: checks.length,
    checks,
    members,
    ...(knownFixture
      ? {
          foreignFixture: {
            id: knownFixture.id,
            memberCode: knownFixture.memberCode,
            homeBranchId: knownFixture.homeBranchId,
          },
        }
      : {}),
    limitations,
    ...(failure ? { failure } : {}),
  }
  if (process.env.GYM_TEST_OUTPUT) {
    await mkdir(process.env.GYM_TEST_OUTPUT, { recursive: true })
    await writeFile(
      `${process.env.GYM_TEST_OUTPUT}/staff-staging-results.json`,
      JSON.stringify(report, null, 2),
    )
  }
  console.log(JSON.stringify(report, null, 2))
  for (const context of contexts) await context.dispose?.()
  await browser?.close()
}
