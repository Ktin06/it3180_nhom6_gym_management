import { App } from 'antd'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import type { PublicPackage } from '../../packages/types/package'
import type {
  MemberPortalGateway,
  MyCheckin,
  MyInvoice,
  MyMembership,
  PortalInvoiceResult,
} from '../types/member-portal'
import { MemberPortalPage } from './MemberPortalPage'

function axiosProblem(status: number, detail: string) {
  const error = new AxiosError('Request failed')
  error.response = {
    status,
    data: { detail },
    statusText: 'Error',
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

function makeMembership(overrides: Partial<MyMembership> = {}): MyMembership {
  return {
    id: 12,
    packageId: 3,
    packageName: 'Gói 1 tháng không giới hạn',
    packageType: 'TIME',
    branchId: 1,
    branchName: 'Chi nhánh 1',
    status: 'ACTIVE',
    startDate: '2026-11-01',
    endDate: '2026-11-30',
    totalSessions: null,
    remainingSessions: null,
    ...overrides,
  }
}

function makePublicPackage(overrides: Partial<PublicPackage> = {}): PublicPackage {
  return {
    id: 3,
    name: 'Gói 3 tháng không giới hạn',
    type: 'TIME',
    durationDays: 90,
    sessionCount: null,
    price: 1200000,
    description: 'Tập không giới hạn tại chi nhánh.',
    ...overrides,
  }
}

function makeCheckin(overrides: Partial<MyCheckin> = {}): MyCheckin {
  return {
    id: 45,
    branchId: 1,
    branchName: 'Chi nhánh 1',
    membershipId: 12,
    packageName: 'Gói 10 buổi toàn chuỗi',
    packageType: 'SESSION',
    method: 'MANUAL',
    sessionDeducted: true,
    checkinDate: '2026-11-20',
    // 08:30 và 09:45 giờ Việt Nam (UTC+7)
    checkedInAt: '2026-11-20T01:30:00Z',
    checkedOutAt: '2026-11-20T02:45:00Z',
    ...overrides,
  }
}

function makeInvoice(overrides: Partial<MyInvoice> = {}): MyInvoice {
  return {
    id: 88,
    code: 'HD000088',
    branchId: 1,
    branchName: 'Chi nhánh 1',
    source: 'MEMBER_PORTAL',
    subtotal: 500000,
    discount: 0,
    total: 500000,
    status: 'PENDING',
    paidAt: null,
    createdAt: '2026-11-20T03:00:00Z',
    // 10:00 23/11/2026 giờ Việt Nam
    expiresAt: '2026-11-23T03:00:00Z',
    items: [
      {
        id: 101,
        itemType: 'PACKAGE',
        packageId: 3,
        description: 'Gói 1 tháng không giới hạn',
        quantity: 1,
        unitPrice: 500000,
        amount: 500000,
        discountAmount: 0,
      },
    ],
    ...overrides,
  }
}

function makePortalResult(overrides: Partial<PortalInvoiceResult> = {}): PortalInvoiceResult {
  return {
    invoiceId: 99,
    invoiceCode: 'HD000099',
    membershipId: 50,
    packageId: 3,
    packageName: 'Gói 1 tháng không giới hạn',
    subtotal: 500000,
    discount: 0,
    total: 500000,
    status: 'PENDING',
    membershipStatus: 'PENDING',
    startDate: '2026-12-01',
    endDate: '2026-12-30',
    // 10:00 24/11/2026 giờ Việt Nam
    expiresAt: '2026-11-24T03:00:00Z',
    ...overrides,
  }
}

function createApiStub(overrides: Partial<MemberPortalGateway> = {}): MemberPortalGateway {
  return {
    listMemberships: vi.fn(async () => []),
    listCheckins: vi.fn(async (query) => ({
      content: [],
      page: query.page ?? 0,
      size: query.size ?? 10,
      totalElements: 0,
      totalPages: 0,
    })),
    listInvoices: vi.fn(async (query) => ({
      content: [],
      page: query.page ?? 0,
      size: query.size ?? 10,
      totalElements: 0,
      totalPages: 0,
    })),
    listPublicPackages: vi.fn(async () => []),
    requestPurchase: vi.fn(async () => makePortalResult()),
    requestRenewal: vi.fn(async () => makePortalResult()),
    downloadInvoicePdf: vi.fn(async () => new Blob(['%PDF-1.4'], { type: 'application/pdf' })),
    ...overrides,
  }
}

function renderPage(api: MemberPortalGateway) {
  return render(
    <App>
      <MemberPortalPage api={api} />
    </App>,
  )
}

describe('MemberPortalPage (F7 – Cổng hội viên)', () => {
  it('TC-F7.1.1-01c: hiển thị nhãn trạng thái tiếng Việt, số buổi còn lại và không có thanh toán online', async () => {
    const api = createApiStub({
      listMemberships: vi.fn(async () => [
        makeMembership({
          id: 1,
          packageName: 'Gói TIME đang dùng',
          status: 'ACTIVE',
          branchId: null,
          branchName: null,
        }),
        makeMembership({
          id: 2,
          packageName: 'Gói PT 10 buổi',
          packageType: 'PT',
          status: 'PENDING',
          totalSessions: 10,
          remainingSessions: 10,
        }),
        makeMembership({
          id: 3,
          packageName: 'Gói buổi nối tiếp',
          packageType: 'SESSION',
          status: 'UPCOMING',
          totalSessions: 12,
          remainingSessions: 12,
        }),
        makeMembership({
          id: 4,
          packageName: 'Gói đang bảo lưu',
          packageType: 'TIME',
          status: 'FROZEN',
        }),
      ]),
    })

    renderPage(api)

    expect(await screen.findByText('Gói TIME đang dùng')).toBeInTheDocument()
    expect(screen.getByText('Đang hoạt động')).toBeInTheDocument()
    expect(screen.getByText(/Toàn chuỗi/)).toBeInTheDocument()
    expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument()
    expect(screen.getByText('Chưa đến ngày bắt đầu')).toBeInTheDocument()
    expect(screen.getByText('Đang bảo lưu')).toBeInTheDocument()
    expect(screen.getByText(/10\/10 buổi/)).toBeInTheDocument()
    expect(screen.getByText(/12\/12 buổi/)).toBeInTheDocument()
    // Không có thanh toán online
    expect(
      screen.queryByRole('button', { name: /Thanh toán ngay|Thanh toán online/i }),
    ).not.toBeInTheDocument()
  })

  it('TC-F7.1.1-03b: chưa có gói tập hiện đúng câu "Bạn chưa có gói tập"', async () => {
    const api = createApiStub()
    renderPage(api)
    expect(await screen.findByText('Bạn chưa có gói tập')).toBeInTheDocument()
  })

  it('TC-F7.1.1-04a, TC-F7.5.1-03b: gói Đã hủy không có nút Gia hạn, gói Hết hạn vẫn có', async () => {
    const api = createApiStub({
      listMemberships: vi.fn(async () => [
        makeMembership({ id: 1, packageName: 'Gói đã hủy', status: 'CANCELLED' }),
        makeMembership({ id: 2, packageName: 'Gói đã hết hạn', status: 'EXPIRED' }),
      ]),
    })
    renderPage(api)

    const cancelledCard = (await screen.findByText('Gói đã hủy')).closest(
      '.ant-card',
    ) as HTMLElement
    const expiredCard = screen.getByText('Gói đã hết hạn').closest('.ant-card') as HTMLElement
    expect(within(cancelledCard).getByText('Đã hủy')).toBeInTheDocument()
    expect(within(cancelledCard).queryByRole('button', { name: /Gia hạn/ })).not.toBeInTheDocument()
    expect(within(expiredCard).getByRole('button', { name: /Gia hạn/ })).toBeInTheDocument()
  })

  it('TC-F7.5.1-01c, TC-F7.5.1-02b: bấm Gia hạn gọi renewal-requests và hiện mã hóa đơn, tổng tiền, hạn tự hủy, hướng dẫn thanh toán tại quầy', async () => {
    const user = userEvent.setup()
    const requestRenewal = vi.fn(async () =>
      makePortalResult({
        invoiceCode: 'HD000150',
        total: 750000,
        expiresAt: '2026-11-24T03:00:00Z',
      }),
    )
    const listMemberships = vi.fn(async () => [
      makeMembership({ id: 42, packageName: 'Gói 1 tháng', status: 'ACTIVE' }),
    ])
    const api = createApiStub({ listMemberships, requestRenewal })

    renderPage(api)
    await screen.findByText('Gói 1 tháng')

    await user.click(screen.getByRole('button', { name: /Gia hạn/ }))

    await waitFor(() => {
      expect(requestRenewal).toHaveBeenCalledWith(42)
    })
    const alertTitle = await screen.findByText(/Đã tạo hóa đơn chờ HD000150 — Tổng tiền/)
    const alert = alertTitle.closest('.ant-alert') as HTMLElement
    expect(within(alert).getByText(/750\.000/)).toBeInTheDocument()
    expect(within(alert).getByText(/10:00.*24\/11\/2026|24\/11\/2026.*10:00/)).toBeInTheDocument()
    expect(within(alert).getByText(/Vui lòng thanh toán tại quầy/i)).toBeInTheDocument()
    expect(listMemberships).toHaveBeenCalledTimes(2)
  })

  it('TC-F7.5.1-05b, TC-F7.5.1-06b: gia hạn lỗi 409 và 422 hiện nguyên câu detail từ server', async () => {
    const user = userEvent.setup()
    const requestRenewal = vi
      .fn()
      .mockRejectedValueOnce(
        axiosProblem(
          422,
          'Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.',
        ),
      )
      .mockRejectedValueOnce(axiosProblem(422, 'Gói tập này đã ngừng bán.'))
    const api = createApiStub({
      listMemberships: vi.fn(async () => [
        makeMembership({ id: 42, packageName: 'Gói 1 tháng', status: 'ACTIVE' }),
      ]),
      requestRenewal,
    })

    renderPage(api)
    await screen.findByText('Gói 1 tháng')

    await user.click(screen.getByRole('button', { name: /Gia hạn/ }))
    expect(
      await screen.findByText(
        'Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.',
      ),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Gia hạn/ }))
    expect(await screen.findByText('Gói tập này đã ngừng bán.')).toBeInTheDocument()
  })

  it('TC-F7.4.1-01d: tab Mua gói tải bảng giá công khai, bấm Mua gọi purchase-requests và hiện hướng dẫn thanh toán tại quầy', async () => {
    const user = userEvent.setup()
    const requestPurchase = vi.fn(async () =>
      makePortalResult({
        invoiceCode: 'HD000201',
        total: 1200000,
        expiresAt: '2026-11-25T05:30:00Z',
      }),
    )
    const api = createApiStub({
      listPublicPackages: vi.fn(async () => [
        makePublicPackage({ id: 7, name: 'Gói 3 tháng VIP', price: 1200000 }),
      ]),
      requestPurchase,
    })

    renderPage(api)
    await screen.findByText('Bạn chưa có gói tập')

    await user.click(screen.getByRole('tab', { name: 'Mua gói' }))
    expect(await screen.findByText('Gói 3 tháng VIP')).toBeInTheDocument()
    expect(screen.getByText(/1\.200\.000/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Mua/ }))

    await waitFor(() => {
      expect(requestPurchase).toHaveBeenCalledWith(7)
    })
    const alertTitle = await screen.findByText(/Đã tạo hóa đơn chờ HD000201 — Tổng tiền/)
    const alert = alertTitle.closest('.ant-alert') as HTMLElement
    expect(within(alert).getByText(/1\.200\.000/)).toBeInTheDocument()
    expect(within(alert).getByText(/12:30.*25\/11\/2026|25\/11\/2026.*12:30/)).toBeInTheDocument()
    expect(within(alert).getByText(/Vui lòng thanh toán tại quầy/i)).toBeInTheDocument()
  })

  it('TC-F7.4.1-05f, TC-F7.4.1-07e: mua gói lỗi 409 và 422 hiện nguyên câu detail từ server', async () => {
    const user = userEvent.setup()
    const requestPurchase = vi
      .fn()
      .mockRejectedValueOnce(
        axiosProblem(
          422,
          'Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.',
        ),
      )
      .mockRejectedValueOnce(axiosProblem(422, 'Gói tập này đã ngừng bán.'))
    const api = createApiStub({
      listPublicPackages: vi.fn(async () => [
        makePublicPackage({ id: 9, name: 'Gói 1 tháng', price: 500000 }),
      ]),
      requestPurchase,
    })

    renderPage(api)
    await screen.findByText('Bạn chưa có gói tập')

    await user.click(screen.getByRole('tab', { name: 'Mua gói' }))
    await screen.findByText('Gói 1 tháng')

    await user.click(screen.getByRole('button', { name: /Mua/ }))
    expect(
      await screen.findByText(
        'Hội viên đã có gói cùng loại đang chờ thanh toán hoặc chờ kích hoạt. Không thể tạo thêm gói mới.',
      ),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Mua/ }))
    expect(await screen.findByText('Gói tập này đã ngừng bán.')).toBeInTheDocument()
  })

  it('TC-F7.2.1-01c: tab Lịch sử check-in hiển thị giờ Việt Nam và phân trang', async () => {
    const user = userEvent.setup()
    const listCheckins = vi.fn(async (query) => ({
      content:
        query.page === 0
          ? [makeCheckin({ id: 1, branchName: 'Chi nhánh 1', checkedInAt: '2026-11-20T01:30:00Z' })]
          : [
              makeCheckin({
                id: 2,
                branchName: 'Chi nhánh 2',
                checkedInAt: '2026-11-19T11:00:00Z',
              }),
            ],
      page: query.page ?? 0,
      size: query.size ?? 10,
      totalElements: 15,
      totalPages: 2,
    }))
    const api = createApiStub({ listCheckins })

    renderPage(api)
    await screen.findByText('Bạn chưa có gói tập')

    await user.click(screen.getByRole('tab', { name: 'Lịch sử check-in' }))
    expect(await screen.findByText('Chi nhánh 1')).toBeInTheDocument()
    // 01:30Z = 08:30 Asia/Ho_Chi_Minh
    expect(screen.getByText(/08:30/)).toBeInTheDocument()

    const page2Button = screen.getByTitle('2')
    await user.click(page2Button)

    await waitFor(() => {
      expect(listCheckins).toHaveBeenCalledWith(expect.objectContaining({ page: 1 }))
    })
    expect(await screen.findByText('Chi nhánh 2')).toBeInTheDocument()
    // 11:00Z = 18:00 Asia/Ho_Chi_Minh
    expect(screen.getByText(/18:00/)).toBeInTheDocument()
  })

  it('TC-F7.2.1-02b: chưa có lượt check-in nào hiện đúng câu "Chưa có lịch sử check-in"', async () => {
    const user = userEvent.setup()
    const api = createApiStub()

    renderPage(api)
    await screen.findByText('Bạn chưa có gói tập')

    await user.click(screen.getByRole('tab', { name: 'Lịch sử check-in' }))
    expect(await screen.findByText('Chưa có lịch sử check-in')).toBeInTheDocument()
  })

  it('TC-F7.3.1-01c, TC-F7.3.1-02c: tab Hóa đơn hiện hạn tự hủy cho hóa đơn chờ và nút tải PDF cho hóa đơn PAID', async () => {
    const user = userEvent.setup()
    const downloadInvoicePdf = vi.fn(async () => new Blob(['%PDF'], { type: 'application/pdf' }))
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null)
    const createUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test-pdf')
    const revokeUrlSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    const api = createApiStub({
      listInvoices: vi.fn(async () => ({
        content: [
          makeInvoice({
            id: 88,
            code: 'HD000088',
            status: 'PENDING',
            source: 'MEMBER_PORTAL',
            total: 500000,
            expiresAt: '2026-11-23T03:00:00Z',
          }),
          makeInvoice({
            id: 89,
            code: 'HD000089',
            status: 'PAID',
            source: 'COUNTER',
            total: 1200000,
            paidAt: '2026-11-18T04:00:00Z',
            expiresAt: null,
          }),
        ],
        page: 0,
        size: 10,
        totalElements: 2,
        totalPages: 1,
      })),
      downloadInvoicePdf,
    })

    renderPage(api)
    await screen.findByText('Bạn chưa có gói tập')

    await user.click(screen.getByRole('tab', { name: 'Hóa đơn' }))
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    expect(screen.getByText('HD000089')).toBeInTheDocument()
    // Hóa đơn chờ hiện hạn tự hủy (10:00 23/11/2026 VN)
    expect(screen.getByText(/Hạn tự hủy/i)).toBeInTheDocument()
    expect(screen.getByText(/10:00.*23\/11\/2026|23\/11\/2026.*10:00/)).toBeInTheDocument()

    // Chỉ có đúng 1 nút Tải PDF (cho hóa đơn PAID HD000089, không có cho PENDING HD000088)
    const pdfButtons = screen.getAllByRole('button', { name: /Tải PDF/i })
    expect(pdfButtons).toHaveLength(1)
    const paidCard = screen.getByText('HD000089').closest('.ant-card') as HTMLElement
    await user.click(within(paidCard).getByRole('button', { name: /Tải PDF/i }))

    await waitFor(() => {
      expect(downloadInvoicePdf).toHaveBeenCalledWith(89)
    })

    openSpy.mockRestore()
    createUrlSpy.mockRestore()
    revokeUrlSpy.mockRestore()
  })

  it('TC-F7.3.1-04b: chưa có hóa đơn nào hiện đúng câu "Bạn chưa có hóa đơn"', async () => {
    const user = userEvent.setup()
    const api = createApiStub()

    renderPage(api)
    await screen.findByText('Bạn chưa có gói tập')

    await user.click(screen.getByRole('tab', { name: 'Hóa đơn' }))
    expect(await screen.findByText('Bạn chưa có hóa đơn')).toBeInTheDocument()
  })
})
