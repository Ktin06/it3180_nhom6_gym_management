import { App } from 'antd'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { canOpen } from '../../../routes/route-access'
import { ptSessionActionState } from '../trainer-time'
import type {
  PtSessionCompletionResult,
  PtSessionScheduleItem,
  TrainerAvailability,
  TrainerScheduleGateway,
  TrainerScheduleQuery,
  TrainerScheduleResponse,
} from '../types/trainer'
import { TrainerSchedulePage } from './TrainerSchedulePage'

function axiosProblem(status: number, detail: string, errors?: Record<string, string>) {
  const error = new AxiosError('Request failed')
  error.response = {
    status,
    data: errors ? { status, detail, errors } : { status, detail },
    statusText: 'Error',
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

function makeAvailability(overrides: Partial<TrainerAvailability> = {}): TrainerAvailability {
  return {
    id: 1,
    trainerId: 3,
    branchId: 1,
    dayOfWeek: 2,
    startTime: '08:00:00',
    endTime: '12:00:00',
    createdAt: '2026-11-20T02:00:00Z',
    updatedAt: '2026-11-20T02:00:00Z',
    ...overrides,
  }
}

function makePtSession(overrides: Partial<PtSessionScheduleItem> = {}): PtSessionScheduleItem {
  return {
    id: 10,
    memberId: 5,
    memberCode: 'HV000005',
    memberName: 'Nguyễn Văn An',
    membershipId: 2,
    branchId: 1,
    // 14:00 - 15:00 giờ Việt Nam ngày 21/11/2026
    startAt: '2026-11-21T07:00:00Z',
    endAt: '2026-11-21T08:00:00Z',
    status: 'BOOKED',
    note: null,
    ...overrides,
  }
}

function makeSchedule(overrides: Partial<TrainerScheduleResponse> = {}): TrainerScheduleResponse {
  return {
    trainerId: 3,
    branchId: 1,
    from: '2026-11-21',
    to: '2026-11-27',
    availabilities: [makeAvailability()],
    ptSessions: [makePtSession()],
    classSessions: [
      {
        id: 7,
        classScheduleId: 1,
        branchId: 1,
        classTypeId: 1,
        classTypeName: 'Yoga Cơ bản',
        roomId: 1,
        roomName: 'Studio Yoga',
        occurrenceDate: '2026-11-21',
        // 18:00 - 19:00 giờ Việt Nam
        startAt: '2026-11-21T11:00:00Z',
        endAt: '2026-11-21T12:00:00Z',
        originalStartAt: null,
        capacity: 25,
        status: 'SCHEDULED',
      },
    ],
    ...overrides,
  }
}

function makeCompletion(
  overrides: Partial<PtSessionCompletionResult> = {},
): PtSessionCompletionResult {
  return {
    id: 10,
    trainerId: 3,
    memberId: 5,
    membershipId: 2,
    branchId: 1,
    startAt: '2026-11-21T07:00:00Z',
    endAt: '2026-11-21T08:00:00Z',
    status: 'DONE',
    confirmedAt: '2026-11-21T07:30:00Z',
    commissionAmount: 45000,
    remainingSessions: 4,
    membershipStatus: 'ACTIVE',
    activatedMembershipId: null,
    movedSessionIds: [],
    cancelledSessionIds: [],
    ...overrides,
  }
}

function makeGateway(overrides: Partial<TrainerScheduleGateway> = {}): TrainerScheduleGateway {
  return {
    getSchedule: vi.fn(async () => makeSchedule()),
    createAvailability: vi.fn(async (input) =>
      makeAvailability({
        id: 2,
        dayOfWeek: input.dayOfWeek,
        startTime: `${input.startTime}:00`,
        endTime: `${input.endTime}:00`,
      }),
    ),
    updateAvailability: vi.fn(async (id, input) =>
      makeAvailability({
        id,
        dayOfWeek: input.dayOfWeek,
        startTime: `${input.startTime}:00`,
        endTime: `${input.endTime}:00`,
      }),
    ),
    deleteAvailability: vi.fn(async () => undefined),
    confirmPtSession: vi.fn(async () => makeCompletion()),
    markPtSessionNoShow: vi.fn(async () =>
      makeCompletion({ status: 'NO_SHOW', commissionAmount: null }),
    ),
    ...overrides,
  }
}

function renderSchedulePage(
  api: TrainerScheduleGateway,
  now: () => Date = () => new Date('2026-11-21T07:30:00Z'), // 14:30 giờ VN
) {
  return render(
    <MemoryRouter>
      <App>
        <TrainerSchedulePage api={api} now={now} />
      </App>
    </MemoryRouter>,
  )
}

describe('TrainerSchedulePage (Lịch của tôi – F9)', () => {
  it('US-F9.1.1 KB1: xem lịch dạy tuần, thêm sửa xóa khung giờ rảnh có hộp xác nhận và liên kết lịch lớp', async () => {
    const user = userEvent.setup()
    const queries: (TrainerScheduleQuery | undefined)[] = []
    const createMock = vi.fn(async () => makeAvailability({ id: 9, dayOfWeek: 4 }))
    const updateMock = vi.fn(async () => makeAvailability({ id: 1, endTime: '13:00:00' }))
    const deleteMock = vi.fn(async () => undefined)
    const api = makeGateway({
      getSchedule: vi.fn(async (query) => {
        queries.push(query)
        return makeSchedule()
      }),
      createAvailability: createMock,
      updateAvailability: updateMock,
      deleteAvailability: deleteMock,
    })

    renderSchedulePage(api)

    expect(await screen.findByText('Nguyễn Văn An')).toBeInTheDocument()
    expect(screen.getByText(/14:00\s*–\s*15:00/)).toBeInTheDocument()
    expect(screen.getByText('Yoga Cơ bản')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Mở lịch dạy lớp/i })).toHaveAttribute('href', '/classes')
    expect(screen.getByText(/Thứ Ba/i)).toBeInTheDocument()
    expect(screen.getByText(/08:00\s*–\s*12:00/)).toBeInTheDocument()

    // Đổi tuần sau
    await user.click(screen.getByRole('button', { name: /Tuần sau/i }))
    await waitFor(() => {
      expect(queries.length).toBeGreaterThanOrEqual(2)
    })

    // Thêm khung giờ rảnh
    await user.click(screen.getByRole('button', { name: /Thêm khung giờ rảnh/i }))
    const addDialog = await screen.findByRole('dialog')
    const startInput = within(addDialog).getByLabelText('Giờ bắt đầu')
    const endInput = within(addDialog).getByLabelText('Giờ kết thúc')
    await user.clear(startInput)
    await user.type(startInput, '14:00')
    await user.clear(endInput)
    await user.type(endInput, '18:00')
    await user.click(within(addDialog).getByRole('button', { name: /^Lưu$/ }))

    await waitFor(() => {
      expect(createMock).toHaveBeenCalledWith(
        expect.objectContaining({
          startTime: '14:00',
          endTime: '18:00',
        }),
      )
    })

    // Sửa khung giờ rảnh
    const slotCard = screen.getByTestId('availability-item-1')
    await user.click(within(slotCard).getByRole('button', { name: /^Sửa$/ }))
    const editDialog = await screen.findByRole('dialog')
    const editEnd = within(editDialog).getByLabelText('Giờ kết thúc')
    await user.clear(editEnd)
    await user.type(editEnd, '13:00')
    await user.click(within(editDialog).getByRole('button', { name: /^Lưu$/ }))

    await waitFor(() => {
      expect(updateMock).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          dayOfWeek: 2,
          startTime: '08:00',
          endTime: '13:00',
        }),
      )
    })

    // Xóa khung giờ rảnh có hộp xác nhận
    await user.click(within(slotCard).getByRole('button', { name: /^Xóa$/ }))
    const confirmDialog = await screen.findByRole('dialog')
    expect(within(confirmDialog).getByText(/xác nhận xóa khung giờ rảnh/i)).toBeInTheDocument()
    await user.click(within(confirmDialog).getByRole('button', { name: /Xác nhận xóa/i }))

    await waitFor(() => {
      expect(deleteMock).toHaveBeenCalledWith(1)
    })
  })

  it('US-F9.1.1 (M2-L2): buổi lớp đổi giờ sang ngày khác hiện ngày mới theo startAt, không hiện ngày gốc', async () => {
    const base = makeSchedule().classSessions[0]
    const api = makeGateway({
      getSchedule: vi.fn(async () =>
        makeSchedule({
          classSessions: [
            {
              ...base,
              occurrenceDate: '2026-11-24',
              originalStartAt: '2026-11-24T11:00:00Z', // 18:00 24/11 VN
              startAt: '2026-11-22T02:30:00Z', // 09:30 22/11 VN
              endAt: '2026-11-22T03:15:00Z',
              status: 'RESCHEDULED',
            },
          ],
        }),
      ),
    })

    renderSchedulePage(api)

    expect(await screen.findByText(/22\/11\/2026 · 09:30\s*–\s*10:15/)).toBeInTheDocument()
    expect(screen.queryByText(/24\/11\/2026 · 09:30/)).not.toBeInTheDocument()
  })

  it('US-F9.1.1 E1-E5: lỗi 400 hiện dưới đúng ô và lỗi 422 chồng khung hoặc đã có hội viên đặt hiện đúng câu API', async () => {
    const user = userEvent.setup()
    const api = makeGateway({
      createAvailability: vi
        .fn()
        .mockRejectedValueOnce(
          axiosProblem(400, 'Dữ liệu không hợp lệ.', {
            endTime: 'Giờ kết thúc phải sau giờ bắt đầu.',
          }),
        )
        .mockRejectedValueOnce(
          axiosProblem(422, 'Khung giờ bị trùng với khung rảnh đã khai báo.'),
        ),
      deleteAvailability: vi
        .fn()
        .mockRejectedValueOnce(
          axiosProblem(422, 'Khung giờ đã có hội viên đặt, không thể xóa hoặc thu hẹp.'),
        ),
    })

    renderSchedulePage(api)

    await screen.findByTestId('availability-item-1')

    // 400 dưới đúng ô endTime
    await user.click(screen.getByRole('button', { name: /Thêm khung giờ rảnh/i }))
    const dialog = await screen.findByRole('dialog')
    const startInput = within(dialog).getByLabelText('Giờ bắt đầu')
    const endInput = within(dialog).getByLabelText('Giờ kết thúc')
    await user.clear(startInput)
    await user.type(startInput, '18:00')
    await user.clear(endInput)
    await user.type(endInput, '17:00')
    await user.click(within(dialog).getByRole('button', { name: /Lưu$/ }))

    expect(
      await within(dialog).findByText('Giờ kết thúc phải sau giờ bắt đầu.'),
    ).toBeInTheDocument()

    // 422 chồng khung
    await user.clear(endInput)
    await user.type(endInput, '20:00')
    await user.click(within(dialog).getByRole('button', { name: /Lưu$/ }))

    expect(
      await within(dialog).findByText('Khung giờ bị trùng với khung rảnh đã khai báo.'),
    ).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: /^Hủy$/ }))

    // 422 xóa khung đã có hội viên đặt
    const slotCard = screen.getByTestId('availability-item-1')
    await user.click(within(slotCard).getByRole('button', { name: /^Xóa$/ }))
    const deleteDialog = await screen.findByRole('dialog')
    await user.click(within(deleteDialog).getByRole('button', { name: /Xác nhận xóa/i }))

    expect(
      await screen.findByText('Khung giờ đã có hội viên đặt, không thể xóa hoặc thu hẹp.'),
    ).toBeInTheDocument()
  })

  it('US-F9.3.1 Q11-Q16-Q13: nút xác nhận vô hiệu trước giờ, ẩn khi quá hạn 23:59 giờ VN và không có nút hủy buổi PT', async () => {
    // Hiện tại là 14:30 giờ VN ngày 21/11/2026 (2026-11-21T07:30:00Z)
    const schedule = makeSchedule({
      ptSessions: [
        // 1. Chưa tới giờ: 16:00 giờ VN ngày 21/11/2026
        makePtSession({
          id: 101,
          memberName: 'Học viên Chưa Tới Giờ',
          startAt: '2026-11-21T09:00:00Z',
          endAt: '2026-11-21T10:00:00Z',
          status: 'BOOKED',
        }),
        // 2. Đã tới giờ và trong ngày: 14:00 giờ VN ngày 21/11/2026
        makePtSession({
          id: 102,
          memberName: 'Học viên Đã Tới Giờ',
          startAt: '2026-11-21T07:00:00Z',
          endAt: '2026-11-21T08:00:00Z',
          status: 'BOOKED',
        }),
        // 3. Quá 23:59 ngày hôm qua: 22:00 giờ VN ngày 20/11/2026 (2026-11-20T15:00:00Z)
        makePtSession({
          id: 103,
          memberName: 'Học viên Quá Hạn',
          startAt: '2026-11-20T15:00:00Z',
          endAt: '2026-11-20T16:00:00Z',
          status: 'BOOKED',
        }),
      ],
    })
    const api = makeGateway({
      getSchedule: vi.fn(async () => schedule),
    })

    renderSchedulePage(api, () => new Date('2026-11-21T07:30:00Z'))

    const futureCard = await screen.findByTestId('pt-session-101')
    expect(within(futureCard).getByRole('button', { name: 'Xác nhận buổi' })).toBeDisabled()
    expect(within(futureCard).getByRole('button', { name: 'Ghi vắng mặt' })).toBeDisabled()

    const activeCard = screen.getByTestId('pt-session-102')
    expect(within(activeCard).getByRole('button', { name: 'Xác nhận buổi' })).toBeEnabled()
    expect(within(activeCard).getByRole('button', { name: 'Ghi vắng mặt' })).toBeEnabled()

    const expiredCard = screen.getByTestId('pt-session-103')
    expect(within(expiredCard).queryByRole('button', { name: 'Xác nhận buổi' })).not.toBeInTheDocument()
    expect(within(expiredCard).queryByRole('button', { name: 'Ghi vắng mặt' })).not.toBeInTheDocument()
    expect(
      within(expiredCard).getByText('Quá hạn, liên hệ quản lý chi nhánh'),
    ).toBeInTheDocument()

    // BR-F9.17 / Q13: PT không có nút hủy buổi PT
    expect(screen.queryByRole('button', { name: /Hủy buổi/i })).not.toBeInTheDocument()

    // Biên 23:59:59 và 00:00:00 theo giờ Asia/Ho_Chi_Minh (buổi 21/11/2026 14:00 VN = 07:00Z)
    expect(
      ptSessionActionState('2026-11-21T07:00:00Z', new Date('2026-11-21T16:59:59Z')),
    ).toBe('ACTIONABLE')
    expect(
      ptSessionActionState('2026-11-21T07:00:00Z', new Date('2026-11-21T17:00:00Z')),
    ).toBe('EXPIRED')

    // Chỉ vai trò TRAINER được mở /pt/lich và /pt/hoc-vien
    expect(canOpen('TRAINER', '/pt/lich')).toBe(true)
    expect(canOpen('TRAINER', '/pt/hoc-vien')).toBe(true)
    for (const role of ['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST', 'MEMBER'] as const) {
      expect(canOpen(role, '/pt/lich')).toBe(false)
      expect(canOpen(role, '/pt/hoc-vien')).toBe(false)
    }
  })

  it('US-F9.3.1 BR-F9.18: xác nhận buổi và ghi vắng mặt gọi đúng API, báo buổi tương lai bị hủy và xử lý 409/422', async () => {
    const user = userEvent.setup()
    const confirmMock = vi.fn(async () =>
      makeCompletion({
        id: 10,
        remainingSessions: 0,
        membershipStatus: 'EXPIRED',
        cancelledSessionIds: [21, 22],
      }),
    )
    const noShowMock = vi
      .fn()
      .mockRejectedValueOnce(
        axiosProblem(
          409,
          'Buổi PT vừa được một thao tác khác cập nhật. Vui lòng tải lại danh sách buổi.',
        ),
      )
      .mockResolvedValueOnce(makeCompletion({ id: 11, status: 'NO_SHOW', commissionAmount: null }))
    const getScheduleMock = vi.fn(async () =>
      makeSchedule({
        ptSessions: [
          makePtSession({ id: 10, memberName: 'Nguyễn Văn An' }),
          makePtSession({ id: 11, memberName: 'Trần Thị Bình' }),
        ],
      }),
    )
    const api = makeGateway({
      getSchedule: getScheduleMock,
      confirmPtSession: confirmMock,
      markPtSessionNoShow: noShowMock,
    })

    renderSchedulePage(api, () => new Date('2026-11-21T07:30:00Z'))

    const firstCard = await screen.findByTestId('pt-session-10')
    await user.click(within(firstCard).getByRole('button', { name: 'Xác nhận buổi' }))

    await waitFor(() => {
      expect(confirmMock).toHaveBeenCalledWith(10)
    })
    expect(
      await screen.findByText(/Đã hủy 2 buổi tương lai vì gói của hội viên hết buổi/i),
    ).toBeInTheDocument()

    const secondCard = screen.getByTestId('pt-session-11')
    await user.click(within(secondCard).getByRole('button', { name: /Ghi vắng mặt/ }))

    expect(
      await screen.findByText(
        'Buổi PT vừa được một thao tác khác cập nhật. Vui lòng tải lại danh sách buổi.',
      ),
    ).toBeInTheDocument()

    await user.click(within(secondCard).getByRole('button', { name: /Ghi vắng mặt/ }))
    await waitFor(() => {
      expect(noShowMock).toHaveBeenCalledTimes(2)
    })
  })
  it('US-F9.3.1 E5 (M5): tab cũ xác nhận buổi đã được xác nhận ở tab khác (422) thì tải lại, thẻ hiện "Đã diễn ra"', async () => {
    const user = userEvent.setup()
    const confirmMock = vi.fn(async () => {
      throw axiosProblem(422, 'Buổi PT đã được xác nhận hoặc ghi vắng, không trừ buổi lần hai.')
    })
    const getScheduleMock = vi
      .fn()
      .mockResolvedValueOnce(makeSchedule({ ptSessions: [makePtSession({ id: 10 })] }))
      .mockResolvedValue(makeSchedule({ ptSessions: [makePtSession({ id: 10, status: 'DONE' })] }))
    const api = makeGateway({ getSchedule: getScheduleMock, confirmPtSession: confirmMock })

    renderSchedulePage(api, () => new Date('2026-11-21T07:30:00Z'))

    const card = await screen.findByTestId('pt-session-10')
    await user.click(within(card).getByRole('button', { name: 'Xác nhận buổi' }))

    expect(
      await screen.findByText('Buổi PT đã được xác nhận hoặc ghi vắng, không trừ buổi lần hai.'),
    ).toBeInTheDocument()
    await waitFor(() => {
      expect(getScheduleMock).toHaveBeenCalledTimes(2)
    })
    const reloaded = await screen.findByTestId('pt-session-10')
    expect(await within(reloaded).findByText('Đã diễn ra')).toBeInTheDocument()
    expect(within(reloaded).queryByRole('button', { name: 'Xác nhận buổi' })).not.toBeInTheDocument()
  })
})
