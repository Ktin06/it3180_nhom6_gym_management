import { App } from 'antd'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../../../auth/AuthContext'
import { ROLES, type Role } from '../../../auth/roles'
import { menuForRole } from '../../../layouts/staff-menu'
import { canOpen } from '../../../routes/route-access'
import { checkinsApi } from '../../checkins/api/checkins'
import type {
  DashboardGateway,
  DashboardStats,
  ExpiringMembership,
  HourlyCheckin,
  LatestCheckin,
} from '../types/dashboard'
import { DashboardPage } from './DashboardPage'

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

function makeAuthValue(role: Role): AuthContextValue {
  return {
    status: 'authenticated',
    user: { email: `${role.toLowerCase()}@gym.local`, role },
    sessionError: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    retrySession: vi.fn(),
  }
}

const sampleStats: DashboardStats = {
  currentlyInGym: 7,
  todayCheckinCount: 15,
  todayRevenue: 2170000,
  cashRevenue: 50000,
  transferRevenue: 2120000,
  expiringPackagesCount: 4,
}

const sampleHourly: HourlyCheckin[] = [
  { hour: '06:00', count: 2 },
  { hour: '07:00', count: 0 },
  { hour: '18:00', count: 5 },
  { hour: '21:00', count: 0 },
]

const sampleExpiring: ExpiringMembership[] = [
  {
    membershipId: 78,
    memberId: 15,
    memberCode: 'HV000015',
    fullName: 'Trần Thị Mai',
    phone: '0987654321',
    hasAvatar: false,
    packageName: 'Gói Tập 10 Buổi',
    packageType: 'SESSION',
    endDate: '2026-12-10',
    daysRemaining: 19,
    remainingSessions: 0,
    warningReason: 'LOW_SESSIONS',
  },
  {
    membershipId: 79,
    memberId: 16,
    memberCode: 'HV000016',
    fullName: 'Nguyễn Văn An',
    phone: '0912345678',
    hasAvatar: false,
    packageName: 'Gói Hội Viên 1 Tháng',
    packageType: 'TIME',
    endDate: '2026-11-24',
    daysRemaining: 3,
    remainingSessions: null,
    warningReason: 'EXPIRING_DATE',
  },
]

const sampleLatestCheckins: LatestCheckin[] = [
  {
    checkinId: 345,
    memberId: 15,
    memberCode: 'HV000015',
    fullName: 'Trần Thị Mai',
    hasAvatar: true,
    packageName: 'Gói Tiêu Chuẩn 1 Tháng',
    packageType: 'TIME',
    method: 'MANUAL',
    checkedInAt: '2026-11-21T02:50:00Z', // 09:50 Asia/Ho_Chi_Minh
    checkedOutAt: null,
    durationMinutes: null,
    status: 'CHECKED_IN',
  },
  {
    checkinId: 344,
    memberId: 16,
    memberCode: 'HV000016',
    fullName: 'Nguyễn Văn An',
    hasAvatar: false,
    packageName: 'Gói 10 Buổi',
    packageType: 'SESSION',
    method: 'FACE',
    checkedInAt: '2026-11-21T02:00:00Z', // 09:00 Asia/Ho_Chi_Minh
    checkedOutAt: '2026-11-21T02:45:00Z',
    durationMinutes: 45,
    status: 'CHECKED_OUT',
  },
]

function createDashboardStub(overrides: Partial<DashboardGateway> = {}): DashboardGateway {
  return {
    stats: vi.fn().mockResolvedValue(sampleStats),
    hourlyCheckins: vi.fn().mockResolvedValue(sampleHourly),
    expiringMemberships: vi.fn().mockResolvedValue(sampleExpiring),
    latestCheckins: vi.fn().mockResolvedValue(sampleLatestCheckins),
    ...overrides,
  }
}

function SellPackageProbe() {
  const location = useLocation()
  return (
    <div data-testid="sell-package-probe">
      <span data-testid="probe-search">{location.search}</span>
      <span data-testid="probe-state">{JSON.stringify(location.state ?? null)}</span>
    </div>
  )
}

function renderDashboard({
  role = 'RECEPTIONIST',
  api = createDashboardStub(),
  loadAvatar = vi.fn().mockResolvedValue(new Blob(['img'], { type: 'image/png' })),
}: {
  role?: Role
  api?: DashboardGateway
  loadAvatar?: (memberId: number) => Promise<Blob>
} = {}) {
  return {
    api,
    loadAvatar,
    ...render(
      <AuthContext.Provider value={makeAuthValue(role)}>
        <MemoryRouter initialEntries={['/dashboard']}>
          <App>
            <Routes>
              <Route
                path="/dashboard"
                element={<DashboardPage api={api} loadAvatar={loadAvatar} />}
              />
              <Route path="/ban-goi" element={<SellPackageProbe />} />
            </Routes>
          </App>
        </MemoryRouter>
      </AuthContext.Provider>,
    ),
  }
}

describe('DashboardPage (F6 – S2-12.2)', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('US-F6.1.1 KB1: lễ tân mở dashboard không thấy ô chọn chi nhánh, tự gọi 4 API không kèm branchId và hiển thị nguyên 4 chỉ số KPI gồm doanh thu tách tiền mặt/chuyển khoản', async () => {
    const api = createDashboardStub()
    renderDashboard({ role: 'RECEPTIONIST', api })

    await waitFor(() => expect(api.stats).toHaveBeenCalledWith(undefined))
    expect(api.hourlyCheckins).toHaveBeenCalledWith(undefined)
    expect(api.expiringMemberships).toHaveBeenCalledWith({ branchId: undefined, limit: 10 })
    expect(api.latestCheckins).toHaveBeenCalledWith({ branchId: undefined, limit: 10 })

    expect(screen.queryByLabelText(/Chọn chi nhánh/i)).not.toBeInTheDocument()

    const currentlyCard = screen.getByTestId('kpi-currently-in-gym')
    expect(within(currentlyCard).getByText('7')).toBeInTheDocument()

    const checkinsCard = screen.getByTestId('kpi-today-checkins')
    expect(within(checkinsCard).getByText('15')).toBeInTheDocument()

    const revenueCard = screen.getByTestId('kpi-today-revenue')
    expect(revenueCard).toHaveTextContent(/2\.170\.000/)
    expect(revenueCard).toHaveTextContent(/Tiền mặt/i)
    expect(revenueCard).toHaveTextContent(/50\.000/)
    expect(revenueCard).toHaveTextContent(/Chuyển khoản/i)
    expect(revenueCard).toHaveTextContent(/2\.120\.000/)

    const expiringCard = screen.getByTestId('kpi-expiring-packages')
    expect(within(expiringCard).getByText('4')).toBeInTheDocument()
  })

  it('US-F6.1.1 KB2: chủ chuỗi chưa chọn chi nhánh thì không gọi API nào và hiện câu nhắc; sau khi chọn chi nhánh mới gọi đủ 4 API kèm branchId', async () => {
    const user = userEvent.setup()
    const api = createDashboardStub()
    renderDashboard({ role: 'SUPER_ADMIN', api })

    expect(
      await screen.findByText(/chọn chi nhánh để xem Dashboard/i),
    ).toBeInTheDocument()
    expect(api.stats).not.toHaveBeenCalled()
    expect(api.hourlyCheckins).not.toHaveBeenCalled()
    expect(api.expiringMemberships).not.toHaveBeenCalled()
    expect(api.latestCheckins).not.toHaveBeenCalled()

    const branchSelect = screen.getByRole('combobox', { name: /Chọn chi nhánh/i })
    await user.click(branchSelect)
    const option = await screen.findByText(/Chi nhánh Bách Khoa/i)
    await user.click(option)

    await waitFor(() => expect(api.stats).toHaveBeenCalledWith(1))
    expect(api.hourlyCheckins).toHaveBeenCalledWith(1)
    expect(api.expiringMemberships).toHaveBeenCalledWith({ branchId: 1, limit: 10 })
    expect(api.latestCheckins).toHaveBeenCalledWith({ branchId: 1, limit: 10 })
  })

  it('US-F6.2.1 KB1: biểu đồ cột theo giờ bằng div/CSS hiển thị đủ mọi khung API trả kể cả khung 0 lượt', async () => {
    renderDashboard({ role: 'BRANCH_MANAGER' })

    const chart = await screen.findByTestId('hourly-checkin-chart')
    expect(within(chart).getByTestId('hour-bar-06:00')).toHaveTextContent('06:00')
    expect(within(chart).getByTestId('hour-bar-06:00')).toHaveTextContent('2')

    const zeroBar07 = within(chart).getByTestId('hour-bar-07:00')
    expect(zeroBar07).toHaveTextContent('07:00')
    expect(zeroBar07).toHaveTextContent('0')

    expect(within(chart).getByTestId('hour-bar-18:00')).toHaveTextContent('5')
    expect(within(chart).getByTestId('hour-bar-21:00')).toHaveTextContent('0')
  })

  it('US-F6.4.1 KB1: bảng gói sắp hết hiển thị đúng lý do, số ngày, số buổi gói SESSION và nút Gia hạn chuyển sang /ban-goi kèm hội viên điền sẵn', async () => {
    const user = userEvent.setup()
    renderDashboard({ role: 'RECEPTIONIST' })

    const expiringSection = await screen.findByTestId('expiring-memberships-section')
    expect(within(expiringSection).getByText('Sắp hết buổi')).toBeInTheDocument()
    expect(within(expiringSection).getByText('Sắp hết hạn')).toBeInTheDocument()
    expect(within(expiringSection).getByText(/Còn 19 ngày/i)).toBeInTheDocument()
    expect(within(expiringSection).getByText(/Còn 0 buổi/i)).toBeInTheDocument()
    expect(within(expiringSection).getByText(/Còn 3 ngày/i)).toBeInTheDocument()

    const renewBtn = within(expiringSection).getByRole('button', {
      name: /Gia hạn cho Trần Thị Mai/i,
    })
    await user.click(renewBtn)

    expect(await screen.findByTestId('sell-package-probe')).toBeInTheDocument()
    expect(screen.getByTestId('probe-search')).toBeEmptyDOMElement()
    expect(screen.getByTestId('probe-state')).toHaveTextContent('"memberCode":"HV000015"')
  })

  it('US-F6.5.1 KB1: bảng lượt check-in mới nhất hiển thị giờ VN, ảnh chỉ tải khi hasAvatar=true, tag Đang tập / Đã về và check-out nhanh gọi checkinsApi.checkout rồi tải lại dashboard', async () => {
    const user = userEvent.setup()
    const api = createDashboardStub()
    const loadAvatar = vi.fn().mockResolvedValue(new Blob(['img'], { type: 'image/png' }))
    const checkoutSpy = vi.spyOn(checkinsApi, 'checkout').mockResolvedValue({
      checkinId: 345,
      checkedOutAt: '2026-11-21T03:35:00Z',
      durationMinutes: 45,
      message: 'Check-out thành công. Hẹn gặp lại quý hội viên!',
    })

    renderDashboard({ role: 'RECEPTIONIST', api, loadAvatar })

    const latestSection = await screen.findByTestId('latest-checkins-section')
    expect(within(latestSection).getByText('09:50')).toBeInTheDocument()
    expect(within(latestSection).getByText('09:00')).toBeInTheDocument()
    expect(within(latestSection).getByText('Đang tập')).toBeInTheDocument()
    expect(within(latestSection).getByText('Đã về')).toBeInTheDocument()
    expect(within(latestSection).getByText(/45 phút/i)).toBeInTheDocument()

    await waitFor(() => expect(loadAvatar).toHaveBeenCalledWith(15))
    expect(loadAvatar).not.toHaveBeenCalledWith(16)

    const checkoutBtn = within(latestSection).getByRole('button', {
      name: /Check-out nhanh cho Trần Thị Mai/i,
    })
    await user.click(checkoutBtn)

    await waitFor(() => expect(checkoutSpy).toHaveBeenCalledWith(345))
    await waitFor(() => expect(api.stats).toHaveBeenCalledTimes(2))
    expect(api.latestCheckins).toHaveBeenCalledTimes(2)
    expect(await screen.findByTestId('checkout-notice')).toHaveTextContent(/Check-out thành công/i)
  })

  it('US-F6.5.1 E1: check-out nhanh lỗi hiện đúng câu detail của API; chủ chuỗi không thấy nút check-out nhanh và nút gia hạn', async () => {
    const user = userEvent.setup()
    vi.spyOn(checkinsApi, 'checkout').mockRejectedValue(
      axiosProblem(404, 'Hội viên không có lượt tập nào đang diễn ra cần check-out.'),
    )

    const { unmount } = renderDashboard({ role: 'BRANCH_MANAGER' })
    const latestSection = await screen.findByTestId('latest-checkins-section')
    const checkoutBtn = await within(latestSection).findByRole('button', {
      name: /Check-out nhanh cho Trần Thị Mai/i,
    })
    await user.click(checkoutBtn)
    expect(await screen.findByTestId('checkout-notice')).toHaveTextContent(
      'Hội viên không có lượt tập nào đang diễn ra cần check-out.',
    )
    unmount()

    const adminApi = createDashboardStub()
    renderDashboard({ role: 'SUPER_ADMIN', api: adminApi })
    const branchSelect = screen.getByRole('combobox', { name: /Chọn chi nhánh/i })
    await user.click(branchSelect)
    await user.click(await screen.findByText(/Chi nhánh Bách Khoa/i))

    const adminLatest = await screen.findByTestId('latest-checkins-section')
    await waitFor(() => expect(within(adminLatest).getByText('Đang tập')).toBeInTheDocument())
    expect(
      within(adminLatest).queryByRole('button', { name: /Check-out/i }),
    ).not.toBeInTheDocument()

    const adminExpiring = screen.getByTestId('expiring-memberships-section')
    expect(within(adminExpiring).queryByRole('button', { name: /Gia hạn/i })).not.toBeInTheDocument()
  })

  it('US-F6.1.1 E1: lỗi tải API dashboard hiện đúng câu detail và cho phép thử lại', async () => {
    const user = userEvent.setup()
    const statsMock = vi
      .fn()
      .mockRejectedValueOnce(axiosProblem(404, 'Không tìm thấy chi nhánh.'))
      .mockResolvedValueOnce(sampleStats)
    const api = createDashboardStub({ stats: statsMock })

    renderDashboard({ role: 'RECEPTIONIST', api })

    expect(await screen.findByText('Không tìm thấy chi nhánh.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Thử lại/i }))

    await waitFor(() => expect(statsMock).toHaveBeenCalledTimes(2))
    expect(await screen.findByTestId('kpi-currently-in-gym')).toHaveTextContent('7')
  })

  it('US-F6.1.1 S3: route /dashboard và menu Dashboard (F6) mở đúng cho RECEPTIONIST, BRANCH_MANAGER, SUPER_ADMIN', () => {
    const allowed: Role[] = ['RECEPTIONIST', 'BRANCH_MANAGER', 'SUPER_ADMIN']
    for (const role of ROLES) {
      expect(canOpen(role, '/dashboard')).toBe(allowed.includes(role))
    }

    for (const role of allowed) {
      const item = menuForRole(role).find((entry) => entry.path === '/dashboard')
      expect(item?.ready).toBe(true)
    }

    expect(menuForRole('TRAINER').some((entry) => entry.path === '/dashboard')).toBe(false)
    expect(menuForRole('MEMBER').some((entry) => entry.path === '/dashboard')).toBe(false)
  })
})
