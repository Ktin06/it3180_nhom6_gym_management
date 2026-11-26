import { http } from '../../services/http'
import { publicRequest } from '../../services/request-options'

export interface CalendarFilter {
  from: string
  to: string
  branchId?: number
}
export interface BranchOption {
  id: number
  name: string
}
export interface CalendarSession {
  id: number
  branchId: number
  branchName: string
  className: string
  roomName: string
  trainerName: string
  startAt: string
  endAt: string
  status: 'SCHEDULED' | 'RESCHEDULED' | 'CANCELLED' | 'COMPLETED'
}
export type BookingStatus = 'BOOKED' | 'CANCELLED' | 'ATTENDED' | 'NO_SHOW'
export interface MemberSession extends CalendarSession {
  capacity: number
  remaining: number
  bookingId: number | null
  bookingStatus: BookingStatus | null
  originalStartAt: string | null
}
export interface MemberCalendar {
  memberId: number
  sessions: MemberSession[]
}
export type AttendanceStatus = 'ATTENDED' | 'NO_SHOW'
export interface AttendanceRow {
  bookingId: number
  memberId: number
  memberCode: string
  fullName: string
  status: BookingStatus
  arrivedAt: string | null
  attendanceMarkedBy: number | null
  updatedAt: string
}
export interface AttendanceRoster {
  session: Pick<CalendarSession, 'id' | 'startAt' | 'endAt' | 'status'>
  bookings: AttendanceRow[]
}

export const classApi = {
  branches: async () =>
    (await http.get<BranchOption[]>('/public/class-branches', publicRequest)).data,
  publicCalendar: async (params: CalendarFilter) =>
    (await http.get<CalendarSession[]>('/public/class-sessions', { ...publicRequest, params }))
      .data,
  memberCalendar: async (params: CalendarFilter) =>
    (await http.get<MemberCalendar>('/members/me/class-sessions', { params })).data,
  trainerCalendar: async (params: CalendarFilter) =>
    (await http.get<CalendarSession[]>('/trainers/me/class-sessions', { params })).data,
  book: async (sessionId: number, memberId: number) => {
    await http.post(`/class-sessions/${sessionId}/bookings`, { memberId })
  },
  cancel: async (bookingId: number) => {
    await http.put(`/class-bookings/${bookingId}/cancel`)
  },
  roster: async (sessionId: number) =>
    (await http.get<AttendanceRoster>(`/class-sessions/${sessionId}/attendance`)).data,
  mark: async (sessionId: number, row: AttendanceRow, status: AttendanceStatus) =>
    (
      await http.put<AttendanceRow>(`/class-sessions/${sessionId}/attendance`, {
        bookingId: row.bookingId,
        status,
        expectedUpdatedAt: row.updatedAt,
      })
    ).data,
}
