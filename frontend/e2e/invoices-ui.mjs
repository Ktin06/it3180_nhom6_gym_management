import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { chromium } from 'playwright'

// Kiểm tra trình duyệt với API giả lập, độc lập backend/CSDL và không thu tiền thật.
// Bật Vite trước; GYM_E2E_BASE_URL mặc định http://localhost:5173.
const baseURL = process.env.GYM_E2E_BASE_URL ?? 'http://localhost:5173'
const artifacts = process.env.GYM_E2E_ARTIFACT_DIR ?? path.join(os.tmpdir(), 's210-ui-evidence')
await mkdir(artifacts, { recursive: true })
const browser = await chromium.launch({
  executablePath: process.env.GYM_BROWSER_EXECUTABLE || undefined,
  headless: true,
})
const contexts = []
const pageErrors = []

function invoice(id, overrides = {}) {
  return {
    id,
    code: `HD${String(id).padStart(6, '0')}`,
    branchId: 1,
    memberId: 123,
    memberCode: 'HV000123',
    memberName: 'Nguyễn Văn An',
    memberPhone: '0912000123',
    createdBy: 2,
    source: 'COUNTER',
    subtotal: 500000,
    discount: 0,
    total: 500000,
    status: 'PENDING',
    paidAt: null,
    refundedBy: null,
    refundedAt: null,
    refundReason: null,
    createdAt: '2026-11-25T02:00:00Z',
    updatedAt: '2026-11-25T02:00:00Z',
    items: [
      {
        id: id * 10,
        itemType: 'PACKAGE',
        packageId: 5,
        productId: null,
        description: 'Gói tập 30 ngày',
        quantity: 1,
        unitPrice: 500000,
        amount: 500000,
        discountAmount: 0,
      },
    ],
    payments: [],
    ...overrides,
  }
}

async function session(role, viewport = { width: 1365, height: 1000 }) {
  const context = await browser.newContext({ viewport })
  contexts.push(context)
  const requests = []
  const invoices = new Map([
    [88, invoice(88)],
    [89, invoice(89, { total: 0, subtotal: 0, items: [] })],
    [90, invoice(90, { source: 'MEMBER_PORTAL' })],
    [91, invoice(91, { branchId: 2, memberId: 999, memberName: 'Trần Thị Bình' })],
    [92, invoice(92, { status: 'PAID' })],
  ])
  const token = `${Buffer.from('{}').toString('base64url')}.${Buffer.from(
    JSON.stringify({
      sub: '2',
      role,
      exp: Math.floor(Date.now() / 1000) + 3600,
    }),
  ).toString('base64url')}.browser-fixture`
  await context.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const endpoint = url.pathname.replace('/api/v1', '')
    const send = (body, status = 200) => route.fulfill({ status, json: body })
    if (endpoint === '/auth/refresh') return send({ accessToken: token })
    if (endpoint === '/auth/me') return send({ userId: 2, email: `${role}@example.test`, role })
    const data = request.postDataJSON()
    requests.push({
      endpoint,
      method: request.method(),
      query: Object.fromEntries(url.searchParams),
      data,
    })
    assert.ok(
      request.headers().authorization?.startsWith('Bearer '),
      'API nghiệp vụ phải có Bearer token',
    )
    if (endpoint === '/invoices/branches')
      return send(
        [
          { id: 1, code: 'CN01', name: 'Bách Khoa' },
          { id: 2, code: 'CN02', name: 'Cầu Giấy' },
        ],
        role === 'SUPER_ADMIN' ? 200 : 403,
      )
    if (endpoint === '/invoices') {
      const rows = [...invoices.values()].filter(
        (row) =>
          !url.searchParams.has('branchId') ||
          row.branchId === Number(url.searchParams.get('branchId')),
      )
      return send({ content: rows, page: 0, size: 20, totalElements: rows.length, totalPages: 1 })
    }
    const match = /^\/invoices\/(\d+)(?:\/(\w+))?$/.exec(endpoint)
    const target = match && invoices.get(Number(match[1]))
    const action = match?.[2]
    if (!target || (role === 'MEMBER' && target.memberId !== 123))
      return send({ detail: 'Hóa đơn không tồn tại trên hệ thống.' }, 404)
    if (!action) return send(target)
    if (action === 'vietqr')
      return send({
        invoiceId: target.id,
        invoiceCode: target.code,
        total: target.total,
        bankId: 'MB',
        bankName: 'MBBank',
        accountNo: '0987654321',
        accountName: 'CONG TY GYM NHOM 6',
        transferContent: `CN01 ${target.code}`,
        qrImageUrl: 'https://img.vietqr.io/image/browser-fixture.png',
      })
    if (action === 'payments') {
      assert.deepEqual(
        Object.keys(data).sort(),
        data.transactionRef ? ['method', 'transactionRef'] : ['method'],
      )
      target.status = 'PAID'
      target.paidAt = '2026-11-25T03:00:00Z'
      target.payments = [
        {
          id: 105,
          method: data.method,
          amount: target.total,
          status: 'CONFIRMED',
          transactionRef: data.transactionRef ?? null,
          receivedBy: 2,
          receivedByName: 'Lễ tân kiểm thử',
          paidAt: target.paidAt,
        },
      ]
      return send({
        paymentId: 105,
        invoiceId: target.id,
        invoiceCode: target.code,
        method: data.method,
        amount: target.total,
        status: 'PAID',
        paidAt: target.paidAt,
        receivedBy: 2,
        receivedByName: 'Lễ tân kiểm thử',
        activatedMemberships: [
          {
            membershipId: 77,
            packageType: 'TIME',
            status: 'UPCOMING',
            startDate: '2026-12-01',
            endDate: '2026-12-30',
            remainingSessions: null,
          },
        ],
      })
    }
    if (action === 'cancel') {
      target.status = 'CANCELLED'
      return send({
        id: target.id,
        code: target.code,
        status: target.status,
        updatedAt: target.updatedAt,
      })
    }
    if (action === 'pdf')
      return route.fulfill({
        contentType: 'application/pdf',
        body: '%PDF-1.4\n% browser fixture\n%%EOF',
      })
    throw new Error(`API chưa được định nghĩa trong fixture: ${endpoint}`)
  })
  await context.route('https://img.vietqr.io/**', (route) =>
    route.fulfill({
      contentType: 'image/svg+xml',
      body: '<svg xmlns="http://www.w3.org/2000/svg" width="220" height="220"><rect width="220" height="220" fill="white"/><rect x="25" y="25" width="170" height="170" fill="#111"/><text x="110" y="115" text-anchor="middle" fill="white">QR TEST</text></svg>',
    }),
  )
  const page = await context.newPage()
  page.on('pageerror', (error) => pageErrors.push(error.message))
  return { context, page, requests }
}

try {
  const staff = await session('RECEPTIONIST')
  await staff.page.goto(`${baseURL}/hoa-don`)
  await staff.page.getByRole('link', { name: 'HD000088', exact: true }).click()
  await staff.page.getByLabel('Chuyển khoản', { exact: true }).check()
  await staff.page.getByAltText('Mã VietQR hóa đơn HD000088').waitFor()
  await staff.page.screenshot({
    path: path.join(artifacts, 'checkout-desktop.png'),
    fullPage: true,
  })
  await staff.page.getByLabel('Mã tham chiếu giao dịch (tùy chọn)').fill(' FT-TEST ')
  await staff.page.getByRole('button', { name: /Xác nhận đã nhận tiền/ }).click()
  await staff.page.getByText('Chờ kích hoạt (UPCOMING)', { exact: true }).waitFor()
  await staff.page.getByText('01/12/2026 – 30/12/2026', { exact: true }).waitFor()
  assert.deepEqual(staff.requests.find((r) => r.endpoint === '/invoices/88/payments')?.data, {
    method: 'BANK_TRANSFER',
    transactionRef: 'FT-TEST',
  })
  await staff.page.evaluate(() => {
    window.open = () => null
  })
  const downloaded = staff.page.waitForEvent('download')
  await staff.page.getByRole('button', { name: /In \/ tải PDF/ }).click()
  assert.equal((await downloaded).suggestedFilename(), 'hoa-don-HD000088.pdf')

  await staff.page.goto(`${baseURL}/hoa-don/89`)
  await staff.page.getByRole('button', { name: /Xác nhận đã nhận tiền/ }).click()
  await staff.page.getByText(/Đã xác nhận thanh toán 0/).waitFor()
  await staff.page.goto(`${baseURL}/hoa-don/90`)
  await staff.page.getByRole('button', { name: /Hủy hóa đơn/ }).click()
  await staff.page.getByRole('dialog').getByRole('button', { name: 'Xác nhận hủy' }).click()
  await staff.page.getByText('Đã hủy', { exact: true }).first().waitFor()
  assert.equal(staff.requests.filter((r) => r.endpoint === '/invoices/branches').length, 0)

  const owner = await session('SUPER_ADMIN')
  await owner.page.goto(`${baseURL}/hoa-don`)
  await owner.page.getByLabel('Chi nhánh', { exact: true }).click()
  await owner.page.getByTitle('CN02 – Cầu Giấy', { exact: true }).click()
  await owner.page.getByRole('link', { name: 'HD000091', exact: true }).waitFor()
  assert.ok(owner.requests.some((r) => r.endpoint === '/invoices' && r.query.branchId === '2'))
  await owner.page.goto(`${baseURL}/hoa-don/92`)
  await owner.page.getByRole('heading', { name: /Chi tiết hóa đơn HD000092/ }).waitFor()
  assert.equal(await owner.page.getByRole('button', { name: /In \/ tải PDF/ }).count(), 0)

  const member = await session('MEMBER')
  await member.page.goto(`${baseURL}/member/invoices/91/pdf`)
  await member.page.getByRole('button', { name: 'In / tải PDF' }).click()
  await member.page.getByText('Hóa đơn không tồn tại trên hệ thống.', { exact: true }).waitFor()
  assert.equal(member.requests.filter((r) => /^\/invoices\/\d+$/.test(r.endpoint)).length, 0)
  await member.page.goto(`${baseURL}/member/invoices/88/pdf`)
  await member.page.getByRole('heading', { name: 'Hóa đơn thanh toán', exact: true }).waitFor()
  await member.page.evaluate(() => {
    window.open = () => null
  })
  const memberDownload = member.page.waitForEvent('download')
  await member.page.getByRole('button', { name: 'In / tải PDF' }).click()
  assert.equal((await memberDownload).suggestedFilename(), 'hoa-don-88.pdf')

  const mobile = await session('RECEPTIONIST', { width: 390, height: 844 })
  await mobile.page.goto(`${baseURL}/hoa-don`)
  await mobile.page.getByRole('link', { name: 'HD000088', exact: true }).waitFor()
  assert.ok(
    await mobile.page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1,
    ),
    'Giao diện mobile không được tràn ngang toàn trang',
  )
  await mobile.page.screenshot({
    path: path.join(artifacts, 'invoices-mobile.png'),
    fullPage: true,
  })
  await mobile.page.goto(`${baseURL}/hoa-don/88`)
  await mobile.page.getByLabel('Chuyển khoản', { exact: true }).check()
  await mobile.page.getByAltText('Mã VietQR hóa đơn HD000088').waitFor()
  assert.ok(
    await mobile.page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1,
    ),
  )
  await mobile.page.screenshot({
    path: path.join(artifacts, 'checkout-mobile.png'),
    fullPage: true,
  })

  const trainer = await session('TRAINER')
  await trainer.page.goto(`${baseURL}/hoa-don`)
  await trainer.page.waitForURL('**/403')
  assert.deepEqual(pageErrors, [])
  console.log(
    JSON.stringify(
      {
        result: 'PASS',
        api: 'simulated',
        roles: ['RECEPTIONIST', 'SUPER_ADMIN', 'MEMBER', 'TRAINER'],
        checks: [
          'BANK_TRANSFER + UPCOMING',
          'PDF fallback download',
          'zero invoice',
          'cancel portal invoice',
          'owner branch filter',
          'member PDF ownership error',
          'member PDF download',
          'mobile layout',
          'trainer denied',
        ],
        artifacts,
      },
      null,
      2,
    ),
  )
} finally {
  await Promise.all(contexts.map((context) => context.close()))
  await browser.close()
}
