import type { CheckinMethod, MembershipStatus } from './types/member-portal'

/** Nhãn tiếng Việt cho trạng thái gói tập theo SRS F7 BR-F7.2. */
export const MEMBERSHIP_STATUS_META: Record<MembershipStatus, { label: string; color: string }> = {
  PENDING: { label: 'Chờ thanh toán', color: 'gold' },
  UPCOMING: { label: 'Chưa đến ngày bắt đầu', color: 'cyan' },
  ACTIVE: { label: 'Đang hoạt động', color: 'green' },
  FROZEN: { label: 'Đang bảo lưu', color: 'blue' },
  EXPIRED: { label: 'Hết hạn', color: 'default' },
  TRANSFERRED: { label: 'Đã chuyển nhượng', color: 'purple' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
}

export const CHECKIN_METHOD_LABELS: Record<CheckinMethod, string> = {
  MANUAL: 'Thủ công',
  FACE: 'Nhận diện khuôn mặt',
}

const vnTimeFormat = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

/** Định dạng giờ HH:mm theo múi giờ Việt Nam (Asia/Ho_Chi_Minh). */
export function formatVnTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const parsed = new Date(iso)
  if (Number.isNaN(parsed.getTime())) return iso
  return vnTimeFormat.format(parsed)
}
