import type {
  CheckinMembershipStatus,
  CheckinMethod,
  CheckinPackageType,
  CheckinStatus,
} from './types/checkin'

const VN_TIME_ZONE = 'Asia/Ho_Chi_Minh'

const vnTimeFormatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VN_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

const vnDateTimeFormatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VN_TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export function formatVnTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return vnTimeFormatter.format(date)
}

export function formatVnDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return vnDateTimeFormatter.format(date)
}

export function formatIsoDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '—'
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  if (!match) return isoDate
  return `${match[3]}/${match[2]}/${match[1]}`
}

export function formatDurationMinutes(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined) return '—'
  if (minutes < 60) return `${minutes} phút`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins === 0
    ? `${minutes} phút (${hours} giờ)`
    : `${minutes} phút (${hours} giờ ${mins} phút)`
}

export const PACKAGE_TYPE_META: Record<CheckinPackageType, { label: string; color: string }> = {
  TIME: { label: 'Theo thời gian (TIME)', color: 'blue' },
  SESSION: { label: 'Theo buổi (SESSION)', color: 'purple' },
  PT: { label: 'HLV cá nhân (PT)', color: 'gold' },
}

export const MEMBERSHIP_STATUS_META: Record<
  CheckinMembershipStatus,
  { label: string; color: string }
> = {
  ACTIVE: { label: 'Đang hoạt động', color: 'green' },
  UPCOMING: { label: 'Chờ kích hoạt', color: 'geekblue' },
  FROZEN: { label: 'Đang bảo lưu', color: 'orange' },
  EXPIRED: { label: 'Đã hết hạn', color: 'default' },
  CANCELLED: { label: 'Đã hủy', color: 'red' },
  PENDING: { label: 'Chờ thanh toán', color: 'gold' },
}

export const CHECKIN_STATUS_META: Record<CheckinStatus, { label: string; color: string }> = {
  CHECKED_IN: { label: 'Đang tập', color: 'green' },
  CHECKED_OUT: { label: 'Đã ra về', color: 'default' },
}

export const CHECKIN_METHOD_LABELS: Record<CheckinMethod, string> = {
  MANUAL: 'Thủ công',
  FACE: 'Khuôn mặt',
}

export function formatGender(gender: string | null | undefined): string {
  if (gender === 'MALE') return 'Nam'
  if (gender === 'FEMALE') return 'Nữ'
  if (gender === 'OTHER') return 'Khác'
  return 'Chưa cập nhật'
}
