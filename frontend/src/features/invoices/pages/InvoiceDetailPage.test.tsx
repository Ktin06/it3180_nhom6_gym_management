import { App } from 'antd'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../../auth/AuthContext'
import type { Role } from '../../../auth/roles'
import type { InvoiceDetail, InvoicesGateway, VietQrPayment } from '../types/invoice'
import { InvoiceDetailPage } from './InvoiceDetailPage'
import { http } from '../../../services/http'
import { liveInvoicesApi } from '../api/invoices'

afterEach(() => {
  vi.restoreAllMocks()
})

function axiosProblem(status: number, detail: string, errors?: Record<string, string>) {
  const error = new AxiosError('Request failed')
  error.response = {
    status,
    data: errors ? { detail, errors } : { detail },
    statusText: 'Error',
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

function makeInvoice(overrides: Partial<InvoiceDetail> = {}): InvoiceDetail {
  return {
    id: 88,
    code: 'HD000088',
    branchId: 1,
    memberId: 123,
    memberCode: 'HV000123',
    memberName: 'Nguyễn Văn A',
    memberPhone: '0912000123',
    createdBy: 2,
    source: 'COUNTER',
    subtotal: 3000000,
    discount: 0,
    total: 3000000,
    status: 'PENDING',
    paidAt: null,
    refundedBy: null,
    refundedAt: null,
    refundReason: null,
    createdAt: '2026-11-20T08:30:00Z',
    updatedAt: '2026-11-20T08:30:00Z',
    items: [
      {
        id: 10,
        itemType: 'PACKAGE',
        packageId: 5,
        productId: null,
        description: 'Gói PT 10 buổi',
        quantity: 1,
        unitPrice: 3000000,
        amount: 3000000,
        discountAmount: 0,
      },
    ],
    payments: [],
    ...overrides,
  }
}

const sampleVietQr: VietQrPayment = {
  invoiceId: 88,
  invoiceCode: 'HD000088',
  total: 3000000,
  bankId: 'MB',
  bankName: 'MBBank (NHTM CP Quân Đội)',
  accountNo: '0987654321',
  accountName: 'CONG TY GYM NHOM 6',
  transferContent: 'CN01 HD000088',
  qrImageUrl:
    'https://img.vietqr.io/image/MB-0987654321-compact.png?amount=3000000&addInfo=CN01%20HD000088',
}

function makeApi(overrides: Partial<InvoicesGateway> = {}): InvoicesGateway {
  return {
    branches: vi.fn().mockResolvedValue([]),
    list: vi.fn(),
    detail: vi.fn().mockResolvedValue(makeInvoice()),
    vietqr: vi.fn().mockResolvedValue(sampleVietQr),
    pay: vi.fn().mockResolvedValue({
      paymentId: 105,
      invoiceId: 88,
      invoiceCode: 'HD000088',
      method: 'CASH',
      amount: 3000000,
      status: 'PAID',
      paidAt: '2026-11-20T09:15:00Z',
      receivedBy: 2,
      receivedByName: 'Nguyễn Văn Lễ Tân',
      activatedMemberships: [
        {
          membershipId: 77,
          packageType: 'PT',
          status: 'ACTIVE',
          startDate: '2026-11-20',
          endDate: '2027-02-17',
          remainingSessions: 10,
        },
      ],
    }),
    cancel: vi.fn().mockResolvedValue({
      id: 88,
      code: 'HD000088',
      status: 'CANCELLED',
      updatedAt: '2026-11-20T10:00:00Z',
    }),
    pdf: vi.fn().mockResolvedValue(new Blob(['%PDF-1.4'], { type: 'application/pdf' })),
    refund: vi.fn().mockResolvedValue({
      invoiceId: 88,
      invoiceCode: 'HD000088',
      status: 'REFUNDED',
      refundedAt: '2026-11-20T02:00:00Z',
      refundedBy: 3,
      refundedByName: 'Trần Quản Lý',
      refundReason: 'Lễ tân bán nhầm gói TIME 3 tháng',
    }),
    ...overrides,
  }
}

function renderDetail(api: InvoicesGateway, role: Role = 'RECEPTIONIST', entry = '/hoa-don/88') {
  return render(
    <App>
      <AuthContext.Provider
        value={{
          status: 'authenticated',
          user: { email: `${role.toLowerCase()}@gym.local`, role },
          sessionError: null,
          login: vi.fn(),
          register: vi.fn(),
          logout: vi.fn(),
          retrySession: vi.fn(),
          completePasswordChange: vi.fn(),
        }}
      >
        <MemoryRouter initialEntries={[entry]}>
          <Link to="/hoa-don/89">Mở hóa đơn khác</Link>
          <Routes>
            <Route path="/hoa-don/:id" element={<InvoiceDetailPage api={api} />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    </App>,
  )
}

describe('chi tiết hóa đơn và thanh toán F5', () => {
  it('TC-F5.3.1-05c: đổi mã hóa đơn trong route phải tải dữ liệu mới và reset phương thức', async () => {
    const user = userEvent.setup()
    const api = makeApi({ detail: vi.fn(async (id) => makeInvoice({ id, code: `HD${id}` })) })
    renderDetail(api)
    expect(await screen.findByText('HD88')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Chuyển khoản'))
    expect(await screen.findByAltText(/Mã VietQR/)).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Mở hóa đơn khác' }))
    expect(await screen.findByText('HD89')).toBeInTheDocument()
    expect(api.detail).toHaveBeenLastCalledWith(89)
    expect(screen.getByLabelText('Tiền mặt')).toBeChecked()
    expect(screen.queryByAltText(/Mã VietQR/)).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
    expect(api.pay).toHaveBeenCalledWith(89, { method: 'CASH' })
  }, 10000)

  describe('TC-F5.3.1-09d: mã route không hợp lệ %s không gọi API', () => {
    it.each(['0', '-1', 'abc', '1.2', '1e2', '9223372036854775808'])(
      'trường hợp %s',
      async (id) => {
        const api = makeApi()
        renderDetail(api, 'RECEPTIONIST', `/hoa-don/${id}`)
        expect(await screen.findByText('Mã hóa đơn không hợp lệ.')).toBeInTheDocument()
        expect(api.detail).not.toHaveBeenCalled()
      },
    )
  })

  it('TC-F5.2.1-03c: cho phép xác nhận hóa đơn 0 đồng và hiển thị phiếu thu 0', async () => {
    const user = userEvent.setup()
    const zero = makeInvoice({ total: 0, subtotal: 0, items: [] })
    const api = makeApi({
      detail: vi
        .fn()
        .mockResolvedValueOnce(zero)
        .mockResolvedValue({
          ...zero,
          status: 'PAID',
          payments: [
            {
              id: 105,
              method: 'CASH',
              amount: 0,
              status: 'CONFIRMED',
              transactionRef: null,
              receivedBy: 2,
              receivedByName: 'Lễ tân',
              paidAt: '2026-11-20T09:15:00Z',
            },
          ],
        }),
      pay: vi.fn().mockResolvedValue({
        paymentId: 105,
        invoiceId: 88,
        invoiceCode: zero.code,
        method: 'CASH',
        amount: 0,
        status: 'PAID',
        activatedMemberships: [],
      }),
    })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    const confirm = screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ })
    expect(confirm).toBeEnabled()
    await user.click(confirm)
    expect(await screen.findByText(/Đã xác nhận thanh toán 0/)).toBeInTheDocument()
    expect(await screen.findByText('Lịch sử phiếu thu')).toBeInTheDocument()
    expect(api.pay).toHaveBeenCalledWith(88, { method: 'CASH' })
  })

  it('TC-F5.2.1-02d: hiển thị trạng thái UPCOMING và ngày hiệu lực đúng response server', async () => {
    const user = userEvent.setup()
    const api = makeApi({
      pay: vi.fn().mockResolvedValue({
        paymentId: 105,
        invoiceId: 88,
        invoiceCode: 'HD000088',
        method: 'CASH',
        amount: 3000000,
        status: 'PAID',
        activatedMemberships: [
          {
            membershipId: 77,
            packageType: 'PT',
            status: 'UPCOMING',
            startDate: '2027-01-01',
            endDate: '2027-01-30',
            remainingSessions: 10,
          },
        ],
      }),
    })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
    expect(await screen.findByText('Chờ kích hoạt (UPCOMING)')).toBeInTheDocument()
    expect(screen.getByText('01/01/2027 – 30/01/2027')).toBeInTheDocument()
    expect(screen.getByText('10')).toBeInTheDocument()
  })

  it('TC-F5.2.1-01e: hiển thị ACTIVE từ server sau thu tiền', async () => {
    const user = userEvent.setup()
    const api = makeApi()
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
    expect(await screen.findByText('Đang hiệu lực (ACTIVE)')).toBeInTheDocument()
    expect(screen.getByText('20/11/2026 – 17/02/2027')).toBeInTheDocument()
  })

  it('TC-F5.2.1-09d: 422 do có UPCOMING tải lại PENDING, không báo thanh toán thành công', async () => {
    const user = userEvent.setup()
    const detail = 'Hội viên đã có gói cùng loại đang chờ kích hoạt (UPCOMING).'
    const api = makeApi({ pay: vi.fn().mockRejectedValue(axiosProblem(422, detail)) })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
    expect(await screen.findByText(detail)).toBeInTheDocument()
    expect(api.detail).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ })).toBeEnabled()
    expect(screen.queryByText(/Đã xác nhận thanh toán/)).not.toBeInTheDocument()
  })

  it('TC-F5.2.1-12b: hóa đơn portal quá hạn hiện lỗi server và không báo thu tiền thành công', async () => {
    const user = userEvent.setup()
    const detail = 'Hóa đơn cổng hội viên đã quá hạn thanh toán.'
    const api = makeApi({
      detail: vi.fn().mockResolvedValue(makeInvoice({ source: 'MEMBER_PORTAL' })),
      pay: vi.fn().mockRejectedValue(axiosProblem(422, detail)),
    })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
    expect(await screen.findByText(detail)).toBeInTheDocument()
    await waitFor(() => expect(api.detail).toHaveBeenCalledTimes(2))
    expect(screen.queryByText(/Đã xác nhận thanh toán/)).not.toBeInTheDocument()
  })

  it('TC-F5.1.1-08b: hóa đơn portal quá hạn hiện lỗi VietQR và không hiển thị mã QR', async () => {
    const user = userEvent.setup()
    const detail = 'Hóa đơn cổng hội viên đã quá hạn thanh toán.'
    const api = makeApi({
      detail: vi.fn().mockResolvedValue(makeInvoice({ source: 'MEMBER_PORTAL' })),
      vietqr: vi.fn().mockRejectedValue(axiosProblem(422, detail)),
    })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Chuyển khoản'))
    expect(await screen.findByText(detail)).toBeInTheDocument()
    await waitFor(() => expect(api.detail).toHaveBeenCalledTimes(2))
    expect(screen.queryByAltText(/Mã VietQR/)).not.toBeInTheDocument()
  })

  it('TC-F5.2.1-07c: bấm xác nhận lặp khi đang gửi chỉ gọi pay một lần', async () => {
    const user = userEvent.setup()
    let finish!: (value: Awaited<ReturnType<InvoicesGateway['pay']>>) => void
    const api = makeApi({
      pay: vi.fn(
        () =>
          new Promise<Awaited<ReturnType<InvoicesGateway['pay']>>>((resolve) => {
            finish = resolve
          }),
      ),
    })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    const confirm = screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ })
    await user.dblClick(confirm)
    expect(api.pay).toHaveBeenCalledOnce()
    expect(screen.getByRole('button', { name: /Hủy hóa đơn/ })).toBeDisabled()
    await act(async () =>
      finish({
        paymentId: 1,
        invoiceId: 88,
        invoiceCode: 'HD000088',
        method: 'CASH',
        amount: 3000000,
        status: 'PAID',
        paidAt: '2026-11-20T09:15:00Z',
        receivedBy: 2,
        receivedByName: null,
        activatedMemberships: [],
      }),
    )
  })

  describe('TC-F5.3.1-07c: hủy gặp %s phải tải lại trạng thái mới', () => {
    it.each([409, 422])('trường hợp %s', async (status) => {
      const user = userEvent.setup()
      const detail = 'Hóa đơn vừa được xử lý bởi nhân viên khác.'
      const api = makeApi({
        detail: vi
          .fn()
          .mockResolvedValueOnce(makeInvoice())
          .mockResolvedValue(makeInvoice({ status: 'PAID' })),
        cancel: vi.fn().mockRejectedValue(axiosProblem(status, detail)),
      })
      renderDetail(api)
      expect(await screen.findByText('HD000088')).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: /Hủy hóa đơn/ }))
      await user.click(
        within(await screen.findByRole('dialog')).getByRole('button', { name: 'Xác nhận hủy' }),
      )
      expect(await screen.findByText(detail)).toBeInTheDocument()
      expect(api.detail).toHaveBeenCalledTimes(2)
      expect(
        screen.queryByRole('button', { name: /Xác nhận đã nhận tiền/ }),
      ).not.toBeInTheDocument()
    })
  })

  it('TC-F5.2.1-05d: lỗi transactionRef 400 được hiển thị ngay dưới ô nhập', async () => {
    const user = userEvent.setup()
    const error = axiosProblem(400, 'Vui lòng kiểm tra dữ liệu.')
    error.response!.data = {
      detail: 'Vui lòng kiểm tra dữ liệu.',
      errors: { transactionRef: 'Mã giao dịch tối đa 100 ký tự.' },
    }
    const api = makeApi({ pay: vi.fn().mockRejectedValue(error) })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Chuyển khoản'))
    expect(await screen.findByAltText(/Mã VietQR/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
    expect(await screen.findByText('Mã giao dịch tối đa 100 ký tự.')).toBeInTheDocument()
    expect(screen.getByLabelText(/Mã tham chiếu giao dịch/)).toHaveAttribute('aria-invalid', 'true')
  })

  describe('TC-F5.2.1-01f: hóa đơn nguồn %s vẫn thu được tại quầy', () => {
    it.each(['COUNTER', 'MEMBER_PORTAL'] as const)('trường hợp %s', async (source) => {
      const user = userEvent.setup()
      const api = makeApi({ detail: vi.fn().mockResolvedValue(makeInvoice({ source })) })
      renderDetail(api)
      expect(await screen.findByText('HD000088')).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
      expect(api.pay).toHaveBeenCalledWith(88, { method: 'CASH' })
    })
  })

  it('TC-F5.1.1-01d: ảnh QR lỗi vẫn giữ thông tin chuyển khoản và nút thử lại', async () => {
    const user = userEvent.setup()
    const api = makeApi()
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Chuyển khoản'))
    fireEvent.error(await screen.findByAltText(/Mã VietQR/))
    expect(screen.getByText(/Không tải được ảnh VietQR/)).toBeInTheDocument()
    expect(screen.getByText('CN01 HD000088')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Tải lại QR/ }))
    expect(api.vietqr).toHaveBeenCalledTimes(2)
  })

  it('TC-F5.1.1-01e: phản hồi QR cũ không ghi đè yêu cầu tải QR mới', async () => {
    const user = userEvent.setup()
    let finishOld!: (value: VietQrPayment) => void
    const api = makeApi({
      vietqr: vi
        .fn()
        .mockImplementationOnce(
          () =>
            new Promise((resolve) => {
              finishOld = resolve
            }),
        )
        .mockResolvedValue({ ...sampleVietQr, accountNo: 'TAI_KHOAN_MOI' }),
    })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Chuyển khoản'))
    await user.click(screen.getByLabelText('Tiền mặt'))
    await user.click(screen.getByLabelText('Chuyển khoản'))
    expect(await screen.findByText('TAI_KHOAN_MOI')).toBeInTheDocument()
    await act(async () => finishOld(sampleVietQr))
    expect(screen.getByText('TAI_KHOAN_MOI')).toBeInTheDocument()
    expect(screen.queryByText('0987654321')).not.toBeInTheDocument()
  })

  it('TC-F5.1.1-01f: thiếu Clipboard API không được báo sao chép thành công', async () => {
    const user = userEvent.setup()
    renderDetail(makeApi())
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Chuyển khoản'))
    expect(await screen.findByAltText(/Mã VietQR/)).toBeInTheDocument()
    const previous = Object.getOwnPropertyDescriptor(navigator, 'clipboard')
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: undefined })
    try {
      await user.click(screen.getByRole('button', { name: 'Sao chép nội dung chuyển khoản' }))
      expect(
        await screen.findByText('Vui lòng chọn và sao chép thủ công nội dung chuyển khoản.'),
      ).toBeInTheDocument()
      expect(screen.queryByText('Đã sao chép nội dung chuyển khoản.')).not.toBeInTheDocument()
    } finally {
      if (previous) Object.defineProperty(navigator, 'clipboard', previous)
    }
  })

  describe('TC-F5.2.1-04d: %s không thấy chức năng thu tiền hoặc VietQR', () => {
    it.each(['SUPER_ADMIN', 'TRAINER', 'MEMBER'] as const)('trường hợp %s', async (role) => {
      renderDetail(makeApi(), role)
      expect(await screen.findByText('HD000088')).toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: /Xác nhận đã nhận tiền/ }),
      ).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Chuyển khoản')).not.toBeInTheDocument()
      if (role !== 'SUPER_ADMIN')
        expect(screen.queryByRole('button', { name: /Hủy hóa đơn/ })).not.toBeInTheDocument()
    })
  })

  it('TC-F5.4.1-05b: Chủ chuỗi không thấy nút PDF dù hóa đơn PAID', async () => {
    renderDetail(
      makeApi({ detail: vi.fn().mockResolvedValue(makeInvoice({ status: 'PAID' })) }),
      'SUPER_ADMIN',
    )
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /tải PDF/ })).not.toBeInTheDocument()
  })

  it('TC-F5.4.1-04c: PDF 404 dạng Blob phải hiện detail server thay vì lỗi kết nối', async () => {
    const user = userEvent.setup()
    const detail = 'Hóa đơn không tồn tại trên hệ thống.'
    const error = axiosProblem(404, detail)
    error.response!.data = new Blob([JSON.stringify({ detail })], {
      type: 'application/problem+json',
    })
    vi.spyOn(http, 'get').mockRejectedValue(error)
    renderDetail(
      makeApi({
        detail: vi.fn().mockResolvedValue(makeInvoice({ status: 'PAID' })),
        pdf: liveInvoicesApi.pdf,
      }),
    )
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /tải PDF/ }))
    expect(await screen.findByText(detail)).toBeInTheDocument()
  })

  it('TC-F5.4.1-01f: rời trang khi PDF đang tải không tạo URL hoặc mở tab muộn', async () => {
    const user = userEvent.setup()
    let finish!: (blob: Blob) => void
    const create = vi.spyOn(URL, 'createObjectURL')
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    const api = makeApi({
      detail: vi.fn().mockResolvedValue(makeInvoice({ status: 'PAID' })),
      pdf: vi.fn(
        () =>
          new Promise<Blob>((resolve) => {
            finish = resolve
          }),
      ),
    })
    const { unmount } = renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /tải PDF/ }))
    unmount()
    await act(async () => finish(new Blob(['%PDF'])))
    expect(create).not.toHaveBeenCalled()
    expect(open).not.toHaveBeenCalled()
  })

  it('TC-F5.2.1-01d: xác nhận thu tiền mặt gửi method CASH và không gửi số tiền (S7)', async () => {
    const user = userEvent.setup()
    const paidInvoice = makeInvoice({
      status: 'PAID',
      paidAt: '2026-11-20T09:15:00Z',
      payments: [
        {
          id: 105,
          method: 'CASH',
          amount: 3000000,
          status: 'CONFIRMED',
          transactionRef: null,
          receivedBy: 2,
          receivedByName: 'Nguyễn Văn Lễ Tân',
          paidAt: '2026-11-20T09:15:00Z',
        },
      ],
    })
    const api = makeApi({
      detail: vi.fn().mockResolvedValueOnce(makeInvoice()).mockResolvedValueOnce(paidInvoice),
    })

    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))

    await waitFor(() => expect(api.pay).toHaveBeenCalledOnce())
    expect(api.pay).toHaveBeenCalledWith(88, { method: 'CASH' })
    const sentBody = (api.pay as ReturnType<typeof vi.fn>).mock.calls[0][1] as Record<
      string,
      unknown
    >
    expect(sentBody).not.toHaveProperty('amount')
    expect(sentBody).not.toHaveProperty('total')
    expect((await screen.findAllByText('Đã thanh toán')).length).toBeGreaterThan(0)
  })

  it('TC-F5.1.1-01c: chọn chuyển khoản gọi VietQR, hiện ảnh QR và nội dung chuyển khoản rồi xác nhận không gửi số tiền', async () => {
    const user = userEvent.setup()
    const api = makeApi()

    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Chuyển khoản'))

    await waitFor(() => expect(api.vietqr).toHaveBeenCalledWith(88))
    const qrImg = await screen.findByAltText(/Mã VietQR hóa đơn HD000088/i)
    expect(qrImg).toHaveAttribute('src', sampleVietQr.qrImageUrl)
    expect(screen.getByText('CN01 HD000088')).toBeInTheDocument()
    expect(screen.getByText(/0987654321/)).toBeInTheDocument()

    await user.type(screen.getByLabelText(/Mã tham chiếu giao dịch/i), 'FT263078912345')
    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))

    await waitFor(() =>
      expect(api.pay).toHaveBeenCalledWith(88, {
        method: 'BANK_TRANSFER',
        transactionRef: 'FT263078912345',
      }),
    )
    const sentBody = (api.pay as ReturnType<typeof vi.fn>).mock.calls[0][1] as Record<
      string,
      unknown
    >
    expect(sentBody).not.toHaveProperty('amount')
    expect(sentBody).not.toHaveProperty('total')
  })

  it('TC-F5.1.1-06c: lỗi 422 khi tải VietQR hiện đúng câu detail của API', async () => {
    const user = userEvent.setup()
    const configMsg = 'Chi nhánh chưa cấu hình tài khoản nhận chuyển khoản'
    const api = makeApi({
      vietqr: vi.fn().mockRejectedValueOnce(axiosProblem(422, configMsg)),
    })

    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Chuyển khoản'))
    expect(await screen.findByText(configMsg)).toBeInTheDocument()
  })

  it('TC-F5.2.1-07b: lỗi 409 hiện đúng câu detail của API và tải lại hóa đơn', async () => {
    const user = userEvent.setup()
    const conflictText = 'Hóa đơn này vừa được xác nhận thanh toán bởi một nhân viên khác.'
    const api = makeApi({
      detail: vi
        .fn()
        .mockResolvedValueOnce(makeInvoice())
        .mockResolvedValueOnce(makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' })),
      pay: vi.fn().mockRejectedValueOnce(axiosProblem(409, conflictText)),
    })

    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))

    expect(await screen.findByText(conflictText)).toBeInTheDocument()
    await waitFor(() => expect(api.detail).toHaveBeenCalledTimes(2))
  })

  it('TC-F5.2.1-08c: lỗi 422 hiện đúng câu detail của API', async () => {
    const user = userEvent.setup()
    const ruleText = 'Hóa đơn đã được thanh toán hoặc đã bị hủy trước đó.'
    const api = makeApi({
      pay: vi.fn().mockRejectedValueOnce(axiosProblem(422, ruleText)),
    })

    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Xác nhận đã nhận tiền/ }))
    expect(await screen.findByText(ruleText)).toBeInTheDocument()
  })

  it('TC-F5.3.1-06e: hủy hóa đơn PENDING thành công và cập nhật trạng thái', async () => {
    const user = userEvent.setup()
    const api = makeApi({
      detail: vi
        .fn()
        .mockResolvedValueOnce(makeInvoice())
        .mockResolvedValueOnce(makeInvoice({ status: 'CANCELLED' })),
    })

    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Hủy hóa đơn/ }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Xác nhận hủy' }))

    await waitFor(() => expect(api.cancel).toHaveBeenCalledWith(88))
    expect((await screen.findAllByText('Đã hủy')).length).toBeGreaterThan(0)
  })

  it('TC-F5.4.1-03b: hóa đơn PENDING không hiển thị nút In / tải PDF', async () => {
    const api = makeApi({ detail: vi.fn().mockResolvedValue(makeInvoice({ status: 'PENDING' })) })
    renderDetail(api)

    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /In \/ tải PDF/i })).not.toBeInTheDocument()
  })

  it('TC-F5.4.1-01d: hóa đơn PAID và REFUNDED tải PDF bằng Blob, mở bằng createObjectURL và thu hồi bằng revokeObjectURL', async () => {
    const user = userEvent.setup()
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:invoice-88')
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    const openSpy = vi.spyOn(window, 'open').mockReturnValue({} as Window)

    for (const status of ['PAID', 'REFUNDED'] as const) {
      const api = makeApi({
        detail: vi.fn().mockResolvedValue(
          makeInvoice({
            status,
            paidAt: '2026-11-20T09:15:00Z',
            refundedAt: status === 'REFUNDED' ? '2026-11-20T11:00:00Z' : null,
            refundReason: status === 'REFUNDED' ? 'Bán nhầm gói' : null,
          }),
        ),
      })
      const { unmount } = renderDetail(api)
      expect(await screen.findByText('HD000088')).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: /In \/ tải PDF/i }))
      await waitFor(() => expect(api.pdf).toHaveBeenCalledWith(88))
      expect(createObjectURL).toHaveBeenCalled()
      expect(openSpy).toHaveBeenCalledWith('blob:invoice-88', '_blank')

      unmount()
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:invoice-88')
    }
  })

  it('TC-F5.4.1-01e: mở được tab PDF thì không tải file lần nữa; bị chặn cửa sổ bật lên thì tải file về', async () => {
    const user = userEvent.setup()
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:invoice-88')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    const anchorClick = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const openedTab = { opener: window } as unknown as Window
    const openSpy = vi
      .spyOn(window, 'open')
      .mockReturnValueOnce(openedTab)
      .mockReturnValueOnce(null)
    const api = makeApi({
      detail: vi
        .fn()
        .mockResolvedValue(makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' })),
    })
    renderDetail(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    // Lần 1: tab mở được → chỉ mở tab, ngắt opener, không tải thêm file.
    await user.click(screen.getByRole('button', { name: /In \/ tải PDF/i }))
    await waitFor(() => expect(openSpy).toHaveBeenCalledTimes(1))
    expect(openedTab.opener).toBeNull()
    expect(anchorClick).not.toHaveBeenCalled()

    // Lần 2: trình duyệt chặn cửa sổ bật lên (window.open trả null) → tải file về.
    await user.click(screen.getByRole('button', { name: /In \/ tải PDF/i }))
    await waitFor(() => expect(anchorClick).toHaveBeenCalledTimes(1))
    expect(openSpy).toHaveBeenCalledTimes(2)
  })

  it('TC-F5.5.1-03d: nút Hoàn tiền chỉ hiện cho BRANCH_MANAGER với hóa đơn PAID, ẩn với lễ tân, chủ chuỗi và trạng thái khác', async () => {
    const paidInvoice = makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' })

    // Quản lý chi nhánh xem hóa đơn PAID → thấy nút Hoàn tiền.
    const managerRender = renderDetail(
      makeApi({ detail: vi.fn().mockResolvedValue(paidInvoice) }),
      'BRANCH_MANAGER',
    )
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Hoàn tiền/i })).toBeInTheDocument()
    managerRender.unmount()

    // Lễ tân và chủ chuỗi xem hóa đơn PAID → không thấy nút Hoàn tiền.
    for (const role of ['RECEPTIONIST', 'SUPER_ADMIN'] as const) {
      const rendered = renderDetail(
        makeApi({ detail: vi.fn().mockResolvedValue(paidInvoice) }),
        role,
      )
      expect(await screen.findByText('HD000088')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Hoàn tiền/i })).not.toBeInTheDocument()
      rendered.unmount()
    }

    // Quản lý chi nhánh xem hóa đơn không PAID (PENDING, CANCELLED, REFUNDED) → không thấy nút Hoàn tiền.
    for (const status of ['PENDING', 'CANCELLED', 'REFUNDED'] as const) {
      const rendered = renderDetail(
        makeApi({
          detail: vi.fn().mockResolvedValue(
            makeInvoice({
              status,
              refundedBy: status === 'REFUNDED' ? 3 : null,
              refundedAt: status === 'REFUNDED' ? '2026-11-20T02:00:00Z' : null,
              refundReason: status === 'REFUNDED' ? 'Bán nhầm gói' : null,
            }),
          ),
        }),
        'BRANCH_MANAGER',
      )
      expect(await screen.findByText('HD000088')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Hoàn tiền/i })).not.toBeInTheDocument()
      rendered.unmount()
    }
  })

  it('TC-F5.5.1-04d: để trống lý do hoàn tiền không gửi API và hiện lỗi bắt buộc', async () => {
    const user = userEvent.setup()
    const api = makeApi({
      detail: vi
        .fn()
        .mockResolvedValue(makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' })),
    })

    renderDetail(api, 'BRANCH_MANAGER')
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Hoàn tiền/i }))
    const dialog = await screen.findByRole('dialog')

    // Để trống lý do và bấm Tiếp tục → báo lỗi, không gọi API.
    await user.click(within(dialog).getByRole('button', { name: /Tiếp tục/i }))
    expect(await within(dialog).findByText('Vui lòng nhập lý do hoàn tiền.')).toBeInTheDocument()
    expect(api.refund).not.toHaveBeenCalled()

    // Chỉ nhập khoảng trắng → vẫn báo lỗi, không gọi API.
    await user.type(within(dialog).getByLabelText(/Lý do hoàn tiền/i), '   ')
    await user.click(within(dialog).getByRole('button', { name: /Tiếp tục/i }))
    expect(await within(dialog).findByText('Vui lòng nhập lý do hoàn tiền.')).toBeInTheDocument()
    expect(api.refund).not.toHaveBeenCalled()
  })

  it('TC-F5.5.1-01c: quản lý chi nhánh hoàn tiền hai bước thành công, tải lại chi tiết và hiện người hoàn, giờ Việt Nam, lý do', async () => {
    const user = userEvent.setup()
    const paidInvoice = makeInvoice({
      status: 'PAID',
      paidAt: '2026-11-20T01:00:00Z',
      payments: [
        {
          id: 105,
          method: 'CASH',
          amount: 3000000,
          status: 'CONFIRMED',
          transactionRef: null,
          receivedBy: 2,
          receivedByName: 'Nguyễn Văn Lễ Tân',
          paidAt: '2026-11-20T01:00:00Z',
        },
      ],
    })
    const refundedInvoice = makeInvoice({
      status: 'REFUNDED',
      paidAt: '2026-11-20T01:00:00Z',
      refundedBy: 3,
      refundedAt: '2026-11-20T02:00:00Z',
      refundReason: 'Lễ tân bán nhầm gói TIME 3 tháng',
      payments: [
        {
          id: 105,
          method: 'CASH',
          amount: 3000000,
          status: 'CANCELLED',
          transactionRef: null,
          receivedBy: 2,
          receivedByName: 'Nguyễn Văn Lễ Tân',
          paidAt: '2026-11-20T01:00:00Z',
        },
      ],
    })
    const api = makeApi({
      detail: vi.fn().mockResolvedValueOnce(paidInvoice).mockResolvedValueOnce(refundedInvoice),
    })

    renderDetail(api, 'BRANCH_MANAGER')
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    // Bước 1: mở hộp thoại, nhập lý do, bấm Tiếp tục (chưa gọi API ngay).
    await user.click(screen.getByRole('button', { name: /Hoàn tiền/i }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText(/Lý do hoàn tiền/i), {
      target: { value: '  Lễ tân bán nhầm gói TIME 3 tháng  ' },
    })
    await user.click(within(dialog).getByRole('button', { name: /Tiếp tục/i }))
    expect(api.refund).not.toHaveBeenCalled()

    // Bước 2: xác nhận hoàn tiền → gọi API và tải lại chi tiết từ server.
    await user.click(within(dialog).getByRole('button', { name: /Xác nhận hoàn tiền/i }))

    await waitFor(() =>
      expect(api.refund).toHaveBeenCalledWith(88, {
        refundReason: 'Lễ tân bán nhầm gói TIME 3 tháng',
      }),
    )
    await waitFor(() => expect(api.detail).toHaveBeenCalledTimes(2))

    const pageRegion = screen.getByRole('region', { name: /Chi tiết hóa đơn/i })
    expect((await within(pageRegion).findAllByText('Đã hoàn tiền')).length).toBeGreaterThan(0)
    expect(within(pageRegion).getByText('Trần Quản Lý')).toBeInTheDocument()
    // 2026-11-20T02:00:00Z đổi sang giờ Asia/Ho_Chi_Minh (+07:00) là 09:00 20/11/2026.
    expect(
      within(pageRegion).getByText(/09:00.*20\/11\/2026|20\/11\/2026.*09:00/),
    ).toBeInTheDocument()
    expect(within(pageRegion).getByText('Lễ tân bán nhầm gói TIME 3 tháng')).toBeInTheDocument()
    expect(within(pageRegion).queryByRole('button', { name: /Hoàn tiền/i })).not.toBeInTheDocument()
  })

  it('TC-F5.5.1-04e: lỗi 400 từ API hiện đúng câu errors.refundReason dưới ô lý do', async () => {
    const user = userEvent.setup()
    const api = makeApi({
      detail: vi
        .fn()
        .mockResolvedValue(makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' })),
      refund: vi.fn().mockRejectedValueOnce(
        axiosProblem(400, 'Vui lòng kiểm tra lại các trường dữ liệu.', {
          refundReason: 'Lý do hoàn tiền tối đa 500 ký tự.',
        }),
      ),
    })

    renderDetail(api, 'BRANCH_MANAGER')
    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Hoàn tiền/i }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText(/Lý do hoàn tiền/i), {
      target: { value: 'Lý do hợp lệ' },
    })
    await user.click(within(dialog).getByRole('button', { name: /Tiếp tục/i }))
    await user.click(within(dialog).getByRole('button', { name: /Xác nhận hoàn tiền/i }))

    expect(await within(dialog).findByText('Lý do hoàn tiền tối đa 500 ký tự.')).toBeInTheDocument()
  })

  it('TC-F5.5.1-05d: lỗi 403 và 404 khi hoàn tiền hiện đúng câu detail của API', async () => {
    const user = userEvent.setup()
    const cases = [
      { status: 403, detail: 'Chỉ quản lý chi nhánh mới có quyền hoàn tiền hóa đơn.' },
      { status: 404, detail: 'Hóa đơn không tồn tại trên hệ thống.' },
    ]

    for (const testCase of cases) {
      const api = makeApi({
        detail: vi
          .fn()
          .mockResolvedValue(makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' })),
        refund: vi.fn().mockRejectedValueOnce(axiosProblem(testCase.status, testCase.detail)),
      })

      const { unmount } = renderDetail(api, 'BRANCH_MANAGER')
      expect(await screen.findByText('HD000088')).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: /Hoàn tiền/i }))
      const dialog = await screen.findByRole('dialog')
      fireEvent.change(within(dialog).getByLabelText(/Lý do hoàn tiền/i), {
        target: { value: 'Bán nhầm gói' },
      })
      await user.click(within(dialog).getByRole('button', { name: /Tiếp tục/i }))
      await user.click(within(dialog).getByRole('button', { name: /Xác nhận hoàn tiền/i }))

      expect(await screen.findByText(testCase.detail)).toBeInTheDocument()
      unmount()
    }
  })

  it('TC-F5.5.1-06b: lỗi 409 và 422 hóa đơn không PAID hiện đúng câu detail và tải lại hóa đơn', async () => {
    const user = userEvent.setup()
    const cases = [
      {
        status: 409,
        detail: 'Dữ liệu hóa đơn vừa được cập nhật bởi thao tác khác. Vui lòng thử lại.',
      },
      {
        status: 422,
        detail: 'Chỉ có thể hoàn tiền cho hóa đơn đã thanh toán thành công.',
      },
    ]

    for (const testCase of cases) {
      const api = makeApi({
        detail: vi
          .fn()
          .mockResolvedValueOnce(makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' }))
          .mockResolvedValueOnce(
            makeInvoice({
              status: 'REFUNDED',
              paidAt: '2026-11-20T09:15:00Z',
              refundedBy: 3,
              refundedAt: '2026-11-20T10:00:00Z',
              refundReason: 'Đã hoàn trước đó',
            }),
          ),
        refund: vi.fn().mockRejectedValueOnce(axiosProblem(testCase.status, testCase.detail)),
      })

      const { unmount } = renderDetail(api, 'BRANCH_MANAGER')
      expect(await screen.findByText('HD000088')).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: /Hoàn tiền/i }))
      const dialog = await screen.findByRole('dialog')
      fireEvent.change(within(dialog).getByLabelText(/Lý do hoàn tiền/i), {
        target: { value: 'Bán nhầm gói' },
      })
      await user.click(within(dialog).getByRole('button', { name: /Tiếp tục/i }))
      await user.click(within(dialog).getByRole('button', { name: /Xác nhận hoàn tiền/i }))

      expect(await screen.findByText(testCase.detail)).toBeInTheDocument()
      await waitFor(() => expect(api.detail).toHaveBeenCalledTimes(2))
      unmount()
    }
  })

  it('TC-F5.5.1-11e: lỗi 422 gói đã dùng và còn chỗ đặt lớp hiện đúng từng câu detail theo BR-F5-14', async () => {
    const user = userEvent.setup()
    const brMessages = [
      'Gói tập của hóa đơn đã được sử dụng nên không thể hoàn tiền. Hội viên có thể bảo lưu hoặc chuyển nhượng gói.',
      'Hội viên còn chỗ đặt lớp nhóm chưa hủy. Hủy chỗ đặt trước khi hoàn tiền.',
    ]

    for (const detail of brMessages) {
      const api = makeApi({
        detail: vi
          .fn()
          .mockResolvedValue(makeInvoice({ status: 'PAID', paidAt: '2026-11-20T09:15:00Z' })),
        refund: vi.fn().mockRejectedValueOnce(axiosProblem(422, detail)),
      })

      const { unmount } = renderDetail(api, 'BRANCH_MANAGER')
      expect(await screen.findByText('HD000088')).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: /Hoàn tiền/i }))
      const dialog = await screen.findByRole('dialog')
      fireEvent.change(within(dialog).getByLabelText(/Lý do hoàn tiền/i), {
        target: { value: 'Bán nhầm gói' },
      })
      await user.click(within(dialog).getByRole('button', { name: /Tiếp tục/i }))
      await user.click(within(dialog).getByRole('button', { name: /Xác nhận hoàn tiền/i }))

      expect(await screen.findByText(detail)).toBeInTheDocument()
      unmount()
    }
  })
})
