export const VN_TIME_ZONE = 'Asia/Ho_Chi_Minh'

export const DAY_OF_WEEK_LABELS: Record<number, string> = {
  1: 'Thứ Hai',
  2: 'Thứ Ba',
  3: 'Thứ Tư',
  4: 'Thứ Năm',
  5: 'Thứ Sáu',
  6: 'Thứ Bảy',
  7: 'Chủ nhật',
}

export const DAY_OF_WEEK_OPTIONS = Object.entries(DAY_OF_WEEK_LABELS).map(([value, label]) => ({
  value: Number(value),
  label,
}))

const vnDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: VN_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const vnTimeFormatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VN_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

const vnDateDisplayFormatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VN_TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

/** Trả ngày YYYY-MM-DD theo múi giờ Việt Nam (Asia/Ho_Chi_Minh). */
export function vnDateIso(date: Date = new Date()): string {
  return vnDateFormatter.format(date)
}

/** Cộng thêm số ngày vào chuỗi YYYY-MM-DD (không phụ thuộc múi giờ máy). */
export function addIsoDays(isoDate: string, days: number): string {
  const [y, m, d] = isoDate.split('-').map(Number)
  const utc = new Date(Date.UTC(y, m - 1, d + days))
  const year = utc.getUTCFullYear()
  const month = String(utc.getUTCMonth() + 1).padStart(2, '0')
  const day = String(utc.getUTCDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** Khoảng 7 ngày từ hôm nay (giờ Việt Nam): khớp mặc định của GET /api/v1/pt/schedule. */
export function defaultSevenDayRange(now: Date = new Date()): { from: string; to: string } {
  const from = vnDateIso(now)
  return { from, to: addIsoDays(from, 6) }
}

/** Định dạng chuỗi YYYY-MM-DD thành DD/MM/YYYY. */
export function formatVnDate(isoDate: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : isoDate
}

/** Định dạng thời điểm ISO UTC thành HH:mm theo giờ Việt Nam. */
export function formatVnTime(isoUtc: string): string {
  const date = new Date(isoUtc)
  if (Number.isNaN(date.getTime())) return isoUtc
  return vnTimeFormatter.format(date)
}

/** Định dạng thời điểm ISO UTC thành DD/MM/YYYY theo giờ Việt Nam. */
export function formatVnDateFromUtc(isoUtc: string): string {
  const date = new Date(isoUtc)
  if (Number.isNaN(date.getTime())) return isoUtc
  return vnDateDisplayFormatter.format(date)
}

/** Định dạng thời điểm ISO UTC thành "HH:mm DD/MM/YYYY" theo giờ Việt Nam. */
export function formatVnDateTime(isoUtc: string): string {
  const date = new Date(isoUtc)
  if (Number.isNaN(date.getTime())) return isoUtc
  return `${vnTimeFormatter.format(date)} ${vnDateDisplayFormatter.format(date)}`
}

/** Rút gọn "HH:mm:ss" thành "HH:mm" cho ô input time và hiển thị. */
export function formatClockTime(timeValue: string): string {
  const match = /^(\d{2}:\d{2})/.exec(timeValue)
  return match ? match[1] : timeValue
}

export type PtSessionActionState = 'BEFORE_START' | 'ACTIONABLE' | 'EXPIRED'

/**
 * Xác định trạng thái thao tác xác nhận / ghi vắng của huấn luyện viên theo giờ Việt Nam:
 * - Chưa tới giờ bắt đầu (`now < startAt`): vô hiệu hóa nút (Q11).
 * - Đã tới giờ và vẫn trong cùng ngày Việt Nam (tới 23:59:59): được phép thao tác (BR-F9.18).
 * - Quá 23:59 cùng ngày Việt Nam (`vnDate(now) > vnDate(startAt)`): ẩn nút, báo quá hạn (BR-F9.19, Q16).
 */
export function ptSessionActionState(
  startAtIso: string,
  now: Date = new Date(),
): PtSessionActionState {
  const startDate = new Date(startAtIso)
  if (Number.isNaN(startDate.getTime())) return 'BEFORE_START'
  if (now.getTime() < startDate.getTime()) return 'BEFORE_START'
  if (vnDateIso(now) > vnDateIso(startDate)) return 'EXPIRED'
  return 'ACTIONABLE'
}
