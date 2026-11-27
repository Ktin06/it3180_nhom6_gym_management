import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { classApi, type CalendarSession, type MemberSession } from './class-api'
import {
  MemberClassCalendarPage,
  PublicClassCalendarPage,
  TrainerClassCalendarPage,
} from './ClassCalendarPages'
import { classDateKey, currentWeek } from './class-time'
import { TrainerAttendancePage } from './TrainerAttendancePage'

const session: CalendarSession = {
  id: 11,
  branchId: 1,
  branchName: 'Chi nhánh Cầu Giấy',
  className: 'Yoga',
  roomName: 'Studio',
  trainerName: 'Mai Anh',
  startAt: '2099-11-23T10:00:00Z',
  endAt: '2099-11-23T11:00:00Z',
  status: 'SCHEDULED',
}
const available: MemberSession = {
  ...session,
  capacity: 10,
  remaining: 1,
  bookingId: null,
  bookingStatus: null,
  originalStartAt: null,
}

afterEach(() => vi.restoreAllMocks())

describe('lịch nhóm F8', () => {
  it('TC-F8.0.1-01b, TC-F8.0.1-04b: khách xem giờ Việt Nam và đăng nhập để đặt', async () => {
    vi.spyOn(classApi, 'branches').mockResolvedValue([
      { id: session.branchId, name: session.branchName },
    ])
    const list = vi.spyOn(classApi, 'publicCalendar').mockResolvedValue([session])
    render(
      <MemoryRouter>
        <PublicClassCalendarPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Yoga')).toBeInTheDocument()
    expect(screen.getByText('17:00–18:00')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Đăng nhập để đặt' })).toHaveAttribute('href', '/login')
    expect(list).toHaveBeenCalledWith(currentWeek())
    expect(classDateKey('2026-11-22T18:00:00Z')).toBe('2026-11-23')
  })

  it('TC-F8.0.1-02b, TC-F8.0.1-03b: lịch rỗng và bộ lọc từ chối khoảng ngày ngược', async () => {
    vi.spyOn(classApi, 'branches').mockResolvedValue([])
    const list = vi.spyOn(classApi, 'publicCalendar').mockResolvedValue([])
    render(
      <MemoryRouter>
        <PublicClassCalendarPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Không có lớp trong khoảng ngày đã chọn.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2099-11-24' } })
    fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2099-11-23' } })
    await userEvent.click(screen.getByRole('button', { name: 'Xem lịch' }))
    expect(screen.getByText(/Chọn khoảng từ 1 đến 31 ngày/)).toBeInTheDocument()
    expect(list).toHaveBeenCalledTimes(1)
  })

  it('TC-F8.1.1-01d, TC-F8.1.1-03d: đặt đúng hội viên, làm mới sĩ số và vô hiệu lớp đầy', async () => {
    vi.spyOn(classApi, 'branches').mockResolvedValue([])
    const full = { ...available, id: 12, className: 'Boxing', remaining: 0 }
    vi.spyOn(classApi, 'memberCalendar')
      .mockResolvedValueOnce({ memberId: 47, sessions: [available, full] })
      .mockResolvedValue({
        memberId: 47,
        sessions: [{ ...available, remaining: 0, bookingId: 91, bookingStatus: 'BOOKED' }, full],
      })
    const book = vi.spyOn(classApi, 'book').mockResolvedValue()
    render(
      <MemoryRouter>
        <MemberClassCalendarPage />
      </MemoryRouter>,
    )
    await screen.findByText('Yoga')
    const [openButton, fullButton] = screen.getAllByRole('button', { name: 'Đặt chỗ' })
    expect(fullButton).toBeDisabled()
    await userEvent.click(openButton)
    expect(book).toHaveBeenCalledWith(11, 47)
    expect(await screen.findByText('Đặt chỗ thành công.')).toBeInTheDocument()
    expect(await screen.findByText('Đã đặt')).toBeInTheDocument()
  })

  it('TC-F8.1.1-02b: hiển thị nguyên nhân server từ chối đặt chỗ', async () => {
    vi.spyOn(classApi, 'branches').mockResolvedValue([])
    vi.spyOn(classApi, 'memberCalendar').mockResolvedValue({ memberId: 47, sessions: [available] })
    vi.spyOn(classApi, 'book').mockRejectedValue(new Error('Hội viên đang bị khóa đặt lớp.'))
    render(
      <MemoryRouter>
        <MemberClassCalendarPage />
      </MemoryRouter>,
    )
    await userEvent.click(await screen.findByRole('button', { name: 'Đặt chỗ' }))
    expect(await screen.findByText('Hội viên đang bị khóa đặt lớp.')).toBeInTheDocument()
  })

  it('TC-F8.3.1-03b: xác nhận hủy chỗ đã đổi giờ và hiển thị trạng thái sau hủy', async () => {
    vi.spyOn(classApi, 'branches').mockResolvedValue([])
    const booked: MemberSession = {
      ...available,
      status: 'RESCHEDULED',
      bookingId: 91,
      bookingStatus: 'BOOKED',
      originalStartAt: '2099-11-23T08:00:00Z',
    }
    vi.spyOn(classApi, 'memberCalendar')
      .mockResolvedValueOnce({ memberId: 47, sessions: [booked] })
      .mockResolvedValue({ memberId: 47, sessions: [{ ...booked, bookingStatus: 'CANCELLED' }] })
    const cancel = vi.spyOn(classApi, 'cancel').mockResolvedValue()
    render(
      <MemoryRouter>
        <MemberClassCalendarPage />
      </MemoryRouter>,
    )
    await userEvent.click(await screen.findByRole('button', { name: 'Hủy chỗ' }))
    expect(await screen.findByText(/không bị tính vi phạm/)).toBeInTheDocument()
    const buttons = screen.getAllByRole('button', { name: 'Hủy chỗ' })
    await userEvent.click(buttons[buttons.length - 1])
    expect(cancel).toHaveBeenCalledWith(91)
    expect(await screen.findByText('Đã hủy chỗ của bạn.')).toBeInTheDocument()
    expect(await screen.findByText('Đã hủy chỗ')).toBeInTheDocument()
  })

  it('TC-F8.5.1-01b, TC-F8.6.1-01b, TC-F8.6.1-03b: đi vào buổi được phân công, xem check-in và ghi điểm danh', async () => {
    const now = new Date()
    const slot = {
      ...session,
      startAt: `${classDateKey(now)}T00:00:00+07:00`,
      endAt: `${classDateKey(now)}T01:00:00+07:00`,
    }
    vi.spyOn(classApi, 'trainerCalendar').mockResolvedValue([slot])
    const row = {
      bookingId: 91,
      memberId: 47,
      memberCode: 'HV000047',
      fullName: 'Lê Hoa',
      status: 'BOOKED' as const,
      arrivedAt: slot.startAt,
      updatedAt: slot.startAt,
      attendanceMarkedBy: null,
    }
    const marked = { ...row, status: 'ATTENDED' as const, attendanceMarkedBy: 7 }
    vi.spyOn(classApi, 'roster')
      .mockResolvedValueOnce({ session: slot, bookings: [row] })
      .mockResolvedValue({ session: slot, bookings: [marked] })
    const update = vi.spyOn(classApi, 'mark').mockResolvedValue(marked)
    render(
      <MemoryRouter initialEntries={['/trainer/classes']}>
        <Routes>
          <Route path="/trainer/classes" element={<TrainerClassCalendarPage />} />
          <Route
            path="/trainer/classes/:sessionId/attendance"
            element={<TrainerAttendancePage />}
          />
        </Routes>
      </MemoryRouter>,
    )
    await userEvent.click(await screen.findByRole('link', { name: 'Điểm danh' }))
    expect(await screen.findByText('Check-in 00:00')).toBeInTheDocument()
    expect(screen.getByText('Chưa điểm danh')).toBeInTheDocument()
    const actions = screen.getByRole('group', { name: 'Điểm danh Lê Hoa' })
    await userEvent.click(within(actions).getByRole('button', { name: 'Có mặt' }))
    expect(update).toHaveBeenCalledWith(11, row, 'ATTENDED')
    expect(await screen.findByText('Đã lưu điểm danh của Lê Hoa.')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Có mặt' })).toBeDisabled())
  })
})
