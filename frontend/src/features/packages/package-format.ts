import type { PackageType } from './types/package'

/** Tên loại gói dùng chung cho trang nội bộ và bảng giá công khai, để một loại gói chỉ có một tên. */
export const packageTypeLabels: Record<PackageType, string> = {
  TIME: 'Theo thời gian',
  SESSION: 'Theo buổi',
  PT: 'Huấn luyện cá nhân',
}

/** Định dạng tiền VND, số nguyên, ví dụ 500.000 ₫. */
export const vndFormat = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

/** Định dạng ngày ISO (YYYY-MM-DD) sang DD/MM/YYYY mà không lệch múi giờ. */
export function formatVnDate(isoDate: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : isoDate
}
