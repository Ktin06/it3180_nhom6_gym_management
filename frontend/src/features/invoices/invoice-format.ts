import type { InvoiceSource, InvoiceStatus, PaymentMethod, PaymentStatus } from './types/invoice'

export const INVOICE_STATUS_META: Record<InvoiceStatus, { label: string; color: string }> = {
  PENDING: { label: 'Chờ thanh toán', color: 'gold' },
  PAID: { label: 'Đã thanh toán', color: 'green' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
  REFUNDED: { label: 'Đã hoàn tiền', color: 'purple' },
}

export const INVOICE_SOURCE_LABELS: Record<InvoiceSource, string> = {
  COUNTER: 'Tại quầy',
  MEMBER_PORTAL: 'Cổng hội viên',
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
}

export const PAYMENT_STATUS_META: Record<PaymentStatus, { label: string; color: string }> = {
  CONFIRMED: { label: 'Đã xác nhận', color: 'green' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
}

/** Định dạng tiền VND số nguyên, ví dụ 3.000.000 ₫. */
export const vndFormat = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const vnDateTimeFormat = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

/** Định dạng mốc thời gian UTC ISO sang ngày giờ Việt Nam (Asia/Ho_Chi_Minh). */
export function formatVnDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const parsed = new Date(iso)
  if (Number.isNaN(parsed.getTime())) return iso
  return vnDateTimeFormat.format(parsed)
}

/** Ngày hiệu lực là LocalDate, không chuyển qua múi giờ của trình duyệt. */
export function formatInvoiceDate(date: string): string {
  return date.split('-').reverse().join('/')
}

export function parseInvoiceId(value: string | undefined): number | undefined {
  if (!value || !/^[1-9]\d*$/.test(value)) return undefined
  const id = Number(value)
  return Number.isSafeInteger(id) ? id : undefined
}
