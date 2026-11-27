import { AxiosError, AxiosHeaders } from 'axios'
import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App as AntdApp } from 'antd'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { menuForRole } from '../../../layouts/staff-menu'
import { canOpen } from '../../../routes/route-access'
import type { PageResponse } from '../../../services/list-types'
import type {
  CheckinGateway,
  CheckinHistoryItem,
  CheckinResult,
  CheckoutResult,
  MemberSearchResult,
} from '../types/checkin'
import { CheckinPage } from './CheckinPage'

function apiError(status: number, detail: string): AxiosError {
  return new AxiosError(
    detail,
    'ERR_BAD_RESPONSE',
    undefined,
    undefined,
    {
      status,
      statusText: String(status),
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: { status, detail },
    },
  )
}

function emptyPage(): PageResponse<CheckinHistoryItem> {
  return { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
}

const activeMember: MemberSearchResult = {
  memberId: 12,
  memberCode: 'HV000012',
  fullName: 'Nguyễn Văn An',
  phone: '0912345678',
  gender: 'MALE',
  hasAvatar: true,
  status: 'ACTIVE',
  isCurrentlyCheckedIn: false,
  memberships: [
    {
      id: 45,
      packageName: 'Gói 10 Buổi Gym',
      packageType: 'SESSION',
      branchId: 1,
      status: 'ACTIVE',
      startDate: '2026-11-01',
      endDate: '2026-12-31',
      totalSessions: 10,
      remainingSessions: 4,
    },
    {
      id: 46,
      packageName: 'Gói PT 1-1',
      packageType: 'PT',
      branchId: 1,
      status: 'UPCOMING',
      startDate: '2026-12-01',
      endDate: '2027-01-30',
      totalSessions: 12,
      remainingSessions: 12,
    },
    {
      id: 47,
      packageName: 'Gói Tiêu Chuẩn 1 Tháng',
      packageType: 'TIME',
      branchId: null,
      status: 'EXPIRED',
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      totalSessions: null,
      remainingSessions: null,
    },
  ],
  upcomingClassSession: {
    bookingId: 105,
    sessionId: 205,
    className: 'Hatha Yoga',
    roomName: 'Phòng 201',
    startTime: '2026-11-21T11:00:00Z', // 18:00 giờ Việt Nam (Asia/Ho_Chi_Minh)
  },
}

const activeCheckinRow: CheckinHistoryItem = {
  checkinId: 340,
  memberId: 15,
  memberCode: 'HV000015',
  fullName: 'Trần Thị Bích',
  hasAvatar: false,
  membershipId: 50,
  packageName: 'Gói Tiêu Chuẩn 1 Tháng',
  packageType: 'TIME',
  method: 'MANUAL',
  sessionDeducted: false,
  checkinDate: '2026-11-21',
  checkedInAt: '2026-11-21T02:00:00Z', // 09:00 VN
  checkedOutAt: null,
  durationMinutes: null,
  status: 'CHECKED_IN',
  createdBy: 3,
  createdByName: 'Lễ tân CN01',
}

function createGateway(overrides: Partial<CheckinGateway> = {}): CheckinGateway {
  return {
    searchMember: vi.fn().mockResolvedValue(activeMember),
    checkin: vi.fn(),
    checkout: vi.fn(),
    history: vi.fn().mockResolvedValue(emptyPage()),
    ...overrides,
  }
}

function renderPage(api: CheckinGateway, loadAvatar?: (memberId: number) => Promise<Blob>) {
  return render(
    <AntdApp>
      <CheckinPage api={api} loadAvatar={loadAvatar} />
    </AntdApp>,
  )
}

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('CheckinPage (F4 – S2-11.3)', () => {
  it('US-F4.1.1 KB1: tìm hội viên hiện ảnh lớn khi hasAvatar=true, mọi gói và lớp sắp diễn ra theo giờ VN', async () => {
    const user = userEvent.setup()
    const api = createGateway()
    const loadAvatar = vi.fn().mockResolvedValue(new Blob(['img'], { type: 'image/png' }))
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:avatar-12')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    renderPage(api, loadAvatar)

    await user.type(screen.getByLabelText(/Tìm hội viên/i), 'HV000012')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))

    expect(api.searchMember).toHaveBeenCalledWith('HV000012')
    expect(await screen.findByText('Nguyễn Văn An')).toBeInTheDocument()
    expect(screen.getByText('HV000012')).toBeInTheDocument()

    // Ảnh đại diện được tải vì hasAvatar = true
    await waitFor(() => expect(loadAvatar).toHaveBeenCalledWith(12))
    expect(createObjectURL).toHaveBeenCalled()

    // Hiện MỌI gói kèm trạng thái (ACTIVE, UPCOMING, EXPIRED)
    expect(screen.getByText('Gói 10 Buổi Gym')).toBeInTheDocument()
    expect(screen.getByText('Gói PT 1-1')).toBeInTheDocument()
    expect(screen.getByText('Gói Tiêu Chuẩn 1 Tháng')).toBeInTheDocument()
    expect(screen.getAllByText('Đang hoạt động').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('Chờ kích hoạt')).toBeInTheDocument()
    expect(screen.getByText('Đã hết hạn')).toBeInTheDocument()

    // Lớp sắp diễn ra đổi từ 11:00Z sang 18:00 giờ Việt Nam
    expect(screen.getByText(/Hatha Yoga/)).toBeInTheDocument()
    expect(screen.getByText(/Phòng 201/)).toBeInTheDocument()
    expect(screen.getByText(/18:00/)).toBeInTheDocument()
  })

  it('US-F4.1.1 KB1: không gọi tải ảnh khi hasAvatar=false', async () => {
    const user = userEvent.setup()
    const api = createGateway({
      searchMember: vi.fn().mockResolvedValue({ ...activeMember, hasAvatar: false }),
    })
    const loadAvatar = vi.fn().mockResolvedValue(new Blob(['img'], { type: 'image/png' }))

    renderPage(api, loadAvatar)

    await user.type(screen.getByLabelText(/Tìm hội viên/i), '0912345678')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))

    expect(await screen.findByText('Nguyễn Văn An')).toBeInTheDocument()
    expect(loadAvatar).not.toHaveBeenCalled()
  })

  it('US-F4.1.1 E1-E2: hiển thị đúng câu lỗi 400 và 404 khi tra cứu', async () => {
    const user = userEvent.setup()
    const searchMember = vi
      .fn()
      .mockRejectedValueOnce(
        apiError(
          400,
          'Vui lòng nhập mã hội viên hợp lệ (ví dụ: HV000123) hoặc số điện thoại 10 chữ số.',
        ),
      )
      .mockRejectedValueOnce(
        apiError(
          404,
          'Không tìm thấy thông tin hội viên trong hệ thống. Vui lòng kiểm tra lại mã hoặc số điện thoại.',
        ),
      )
    const api = createGateway({ searchMember })

    renderPage(api)

    const input = screen.getByLabelText(/Tìm hội viên/i)
    await user.type(input, 'ABC')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))
    expect(
      await screen.findByText(
        'Vui lòng nhập mã hội viên hợp lệ (ví dụ: HV000123) hoặc số điện thoại 10 chữ số.',
      ),
    ).toBeInTheDocument()

    await user.clear(input)
    await user.type(input, 'HV999999')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))
    expect(
      await screen.findByText(
        'Không tìm thấy thông tin hội viên trong hệ thống. Vui lòng kiểm tra lại mã hoặc số điện thoại.',
      ),
    ).toBeInTheDocument()
  })

  it('US-F4.1.1 E3: hồ sơ ngừng hoạt động hiện cảnh báo đỏ và vô hiệu hóa nút check-in', async () => {
    const user = userEvent.setup()
    const api = createGateway({
      searchMember: vi.fn().mockResolvedValue({
        ...activeMember,
        hasAvatar: false,
        status: 'INACTIVE',
      }),
    })

    renderPage(api)

    await user.type(screen.getByLabelText(/Tìm hội viên/i), 'HV000009')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))

    expect(
      await screen.findByText(/Hồ sơ hội viên này đã ngừng hoạt động/i),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Xác nhận Check-in/i })).toBeDisabled()
  })

  it('US-F4.2.1 KB1: check-in thành công hiện thông báo xanh, gói áp dụng, trừ buổi, buổi còn lại và nhắc lớp', async () => {
    const user = userEvent.setup()
    const result: CheckinResult = {
      checkinId: 350,
      memberId: 12,
      memberCode: 'HV000012',
      fullName: 'Nguyễn Văn An',
      appliedMembership: {
        id: 45,
        packageName: 'Gói 10 Buổi Gym',
        packageType: 'SESSION',
        sessionDeducted: true,
        remainingSessions: 3,
      },
      checkinDate: '2026-11-21',
      checkedInAt: '2026-11-21T10:30:00Z', // 17:30 VN
      classArrivalMarked: true,
      classArrivals: [
        {
          bookingId: 105,
          sessionId: 205,
          className: 'Hatha Yoga',
          roomName: 'Phòng 201',
          startTime: '2026-11-21T11:00:00Z', // 18:00 VN
        },
      ],
      message: 'Check-in thành công. Chúc quý hội viên buổi tập hiệu quả!',
    }
    const api = createGateway({
      searchMember: vi.fn().mockResolvedValue({ ...activeMember, hasAvatar: false }),
      checkin: vi.fn().mockResolvedValue(result),
    })

    renderPage(api)

    await user.type(screen.getByLabelText(/Tìm hội viên/i), 'HV000012')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))
    await screen.findByText('Nguyễn Văn An')

    await user.click(screen.getByRole('button', { name: /Xác nhận Check-in/i }))

    expect(api.checkin).toHaveBeenCalledWith(12)
    expect(
      await screen.findByText('Check-in thành công. Chúc quý hội viên buổi tập hiệu quả!'),
    ).toBeInTheDocument()
    const successBox = screen.getByTestId('checkin-success-alert')
    expect(within(successBox).getByText(/Gói 10 Buổi Gym/)).toBeInTheDocument()
    expect(within(successBox).getByText(/Đã trừ 1 buổi/)).toBeInTheDocument()
    expect(within(successBox).getByText(/Còn lại: 3 buổi/)).toBeInTheDocument()
    expect(within(successBox).getByText(/Hatha Yoga/)).toBeInTheDocument()
    expect(within(successBox).getByText(/Phòng 201/)).toBeInTheDocument()
    expect(within(successBox).getByText(/18:00/)).toBeInTheDocument()
  })

  it('US-F4.2.1 E1-E5: lỗi 422 hiển thị đúng nguyên văn câu detail từ máy chủ', async () => {
    const user = userEvent.setup()
    const detail =
      'Gói huấn luyện viên cá nhân (PT) chỉ dùng cho các buổi tập 1-1 có lịch hẹn, không dùng để check-in tự do vào phòng tập. Hội viên cần có thêm gói theo thời gian hoặc theo lượt.'
    const api = createGateway({
      searchMember: vi.fn().mockResolvedValue({ ...activeMember, hasAvatar: false }),
      checkin: vi.fn().mockRejectedValue(apiError(422, detail)),
    })

    renderPage(api)

    await user.type(screen.getByLabelText(/Tìm hội viên/i), 'HV000006')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))
    await screen.findByText('Nguyễn Văn An')

    await user.click(screen.getByRole('button', { name: /Xác nhận Check-in/i }))

    expect(await screen.findByText(detail)).toBeInTheDocument()
  })

  it('US-F4.2.1 KB7: lỗi 409 hiện nút Thử lại và bấm lại gọi đúng API ra 201', async () => {
    const user = userEvent.setup()
    const conflictDetail =
      'Dữ liệu lượt check-in đang được xử lý đồng thời bởi một thao tác khác. Vui lòng thử lại.'
    const retryResult: CheckinResult = {
      checkinId: 351,
      memberId: 12,
      memberCode: 'HV000012',
      fullName: 'Nguyễn Văn An',
      appliedMembership: {
        id: 45,
        packageName: 'Gói 10 Buổi Gym',
        packageType: 'SESSION',
        sessionDeducted: false,
        remainingSessions: 4,
      },
      checkinDate: '2026-11-21',
      checkedInAt: '2026-11-21T10:31:00Z',
      classArrivalMarked: false,
      classArrivals: [],
      message: 'Check-in thành công. Chúc quý hội viên buổi tập hiệu quả!',
    }
    const checkin = vi
      .fn()
      .mockRejectedValueOnce(apiError(409, conflictDetail))
      .mockResolvedValueOnce(retryResult)

    const api = createGateway({
      searchMember: vi.fn().mockResolvedValue({ ...activeMember, hasAvatar: false }),
      checkin,
    })

    renderPage(api)

    await user.type(screen.getByLabelText(/Tìm hội viên/i), 'HV000012')
    await user.click(screen.getByRole('button', { name: /Tra cứu/i }))
    await screen.findByText('Nguyễn Văn An')

    await user.click(screen.getByRole('button', { name: /Xác nhận Check-in/i }))
    expect(await screen.findByText(conflictDetail)).toBeInTheDocument()

    const retryButton = screen.getByRole('button', { name: /Thử lại/i })
    await user.click(retryButton)

    expect(checkin).toHaveBeenNthCalledWith(1, 12)
    expect(checkin).toHaveBeenNthCalledWith(2, 12)
    const successBox = await screen.findByTestId('checkin-success-alert')
    expect(within(successBox).getByText(/Không trừ buổi/)).toBeInTheDocument()
  })

  it('US-F4.4.1 KB1: danh sách đang tập hôm nay gọi check-out, hiện thời lượng phút và xử lý lỗi 404', async () => {
    const user = userEvent.setup()
    const checkoutResult: CheckoutResult = {
      checkinId: 340,
      checkedOutAt: '2026-11-21T03:45:00Z',
      durationMinutes: 105,
      message: 'Check-out thành công. Hẹn gặp lại quý hội viên!',
    }
    const history = vi
      .fn()
      .mockResolvedValueOnce({
        content: [activeCheckinRow],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        content: [activeCheckinRow],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValue(emptyPage())
    const checkout = vi
      .fn()
      .mockResolvedValueOnce(checkoutResult)
      .mockRejectedValueOnce(
        apiError(404, 'Hội viên không có lượt tập nào đang diễn ra cần check-out.'),
      )

    const api = createGateway({ history, checkout })

    renderPage(api)

    expect(await screen.findByText('Trần Thị Bích')).toBeInTheDocument()
    expect(history).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'CHECKED_IN', page: 0, size: 20 }),
    )
    expect(screen.getByText(/09:00/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Check-out/i }))

    expect(checkout).toHaveBeenCalledWith(340)
    expect(
      await screen.findByText(/Check-out thành công\. Hẹn gặp lại quý hội viên!/),
    ).toBeInTheDocument()
    expect(screen.getByText(/105 phút/)).toBeInTheDocument()

    // Bấm lần nữa gặp lỗi 404 từ backend -> hiển thị đúng câu detail
    await user.click(screen.getByRole('button', { name: /Check-out/i }))
    expect(
      await screen.findByText('Hội viên không có lượt tập nào đang diễn ra cần check-out.'),
    ).toBeInTheDocument()
  })

  it('US-F4.4.1 E1 (M3-L1): check-out 404 vì lượt đã được tab khác check-out thì tải lại danh sách, dòng cũ biến mất', async () => {
    const user = userEvent.setup()
    const history = vi
      .fn()
      .mockResolvedValueOnce({
        content: [activeCheckinRow],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValue(emptyPage())
    const checkout = vi
      .fn()
      .mockRejectedValueOnce(
        apiError(404, 'Hội viên không có lượt tập nào đang diễn ra cần check-out.'),
      )
    renderPage(createGateway({ history, checkout }))

    expect(await screen.findByText('Trần Thị Bích')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Check-out/i }))

    expect(
      await screen.findByText('Hội viên không có lượt tập nào đang diễn ra cần check-out.'),
    ).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByText('Trần Thị Bích')).not.toBeInTheDocument())
    expect(history).toHaveBeenCalledTimes(2)
    expect(
      screen.getByText('Hội viên không có lượt tập nào đang diễn ra cần check-out.'),
    ).toBeInTheDocument()
  })

  it('US-F4.4.1 KB2: khung Đang tập hôm nay hỗ trợ chuyển trang khi có hơn 20 hội viên', async () => {
    const user = userEvent.setup()
    const page1Item: CheckinHistoryItem = {
      ...activeCheckinRow,
      checkinId: 501,
      memberCode: 'HV000101',
      fullName: 'Hội viên trang 1',
    }
    const page2Item: CheckinHistoryItem = {
      ...activeCheckinRow,
      checkinId: 521,
      memberCode: 'HV000121',
      fullName: 'Hội viên trang 2',
    }
    const history = vi.fn().mockImplementation(async (query) => ({
      content: query.page === 0 ? [page1Item] : [page2Item],
      page: query.page,
      size: query.size,
      totalElements: 25,
      totalPages: 2,
    }))

    renderPage(createGateway({ history }))

    expect(await screen.findByText('Hội viên trang 1')).toBeInTheDocument()
    expect(screen.getByText('Đang tập hôm nay (25)')).toBeInTheDocument()

    await user.click(screen.getByTitle('2'))

    await waitFor(() =>
      expect(history).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'CHECKED_IN', page: 1, size: 20 }),
      ),
    )
    expect(await screen.findByText('Hội viên trang 2')).toBeInTheDocument()
  })

  it('US-F4.5.1 KB1: tab lịch sử lọc theo ngày, trạng thái, phương thức, từ khóa và phân trang', async () => {
    const user = userEvent.setup()
    const historyRow: CheckinHistoryItem = {
      ...activeCheckinRow,
      checkinId: 401,
      fullName: 'Lê Hoàng Nam',
      memberCode: 'HV000108',
      checkedInAt: '2026-11-20T17:10:00Z', // 00:10 ngày 21/11 giờ VN
      checkedOutAt: '2026-11-20T18:30:00Z', // 01:30 ngày 21/11 giờ VN
      durationMinutes: 80,
      status: 'CHECKED_OUT',
    }
    const history = vi.fn().mockImplementation(async (query) => {
      if (query.status === 'CHECKED_IN' && !query.date && !query.keyword) {
        return emptyPage()
      }
      return {
        content: [historyRow],
        page: query.page,
        size: query.size,
        totalElements: 25,
        totalPages: 2,
      }
    })

    const api = createGateway({ history })
    renderPage(api)

    await user.click(screen.getByRole('tab', { name: /Lịch sử check-in/i }))

    expect(await screen.findByText('Lê Hoàng Nam')).toBeInTheDocument()
    expect(screen.getByText(/00:10/)).toBeInTheDocument()
    expect(screen.getByText(/01:30/)).toBeInTheDocument()
    expect(screen.getByText(/80 phút/)).toBeInTheDocument()

    // Lọc theo ngày
    const dateInput = screen.getByLabelText(/Ngày check-in/i)
    await user.clear(dateInput)
    await user.type(dateInput, '2026-11-20')

    await waitFor(() =>
      expect(history).toHaveBeenCalledWith(
        expect.objectContaining({ date: '2026-11-20', page: 0 }),
      ),
    )

    // Lọc theo từ khóa
    const keywordInput = screen.getByLabelText(/Tìm theo tên hoặc mã/i)
    await user.type(keywordInput, 'Nam')
    await user.click(screen.getByRole('button', { name: /Lọc/i }))

    await waitFor(() =>
      expect(history).toHaveBeenCalledWith(
        expect.objectContaining({ date: '2026-11-20', keyword: 'Nam', page: 0 }),
      ),
    )

    // Chuyển trang 2
    await user.click(screen.getByTitle('2'))
    await waitFor(() =>
      expect(history).toHaveBeenCalledWith(expect.objectContaining({ page: 1 })),
    )
  })

  it('US-F4.1.1 S3: route /checkins và menu Check-in (F4) chỉ mở cho RECEPTIONIST và BRANCH_MANAGER', () => {
    expect(canOpen('RECEPTIONIST', '/checkins')).toBe(true)
    expect(canOpen('BRANCH_MANAGER', '/checkins')).toBe(true)
    expect(canOpen('SUPER_ADMIN', '/checkins')).toBe(false)
    expect(canOpen('TRAINER', '/checkins')).toBe(false)
    expect(canOpen('MEMBER', '/checkins')).toBe(false)

    for (const role of ['RECEPTIONIST', 'BRANCH_MANAGER'] as const) {
      const item = menuForRole(role).find((m) => m.path === '/checkins')
      expect(item?.ready, role).toBe(true)
    }
    for (const role of ['SUPER_ADMIN', 'TRAINER', 'MEMBER'] as const) {
      expect(menuForRole(role).find((m) => m.path === '/checkins'), role).toBeUndefined()
    }
  })
})
