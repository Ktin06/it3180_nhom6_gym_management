import type { DashboardPackageType, ExpiringWarningReason } from './types/dashboard'

const VN_TIME_ZONE = 'Asia/Ho_Chi_Minh'

export const vndFormat = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
})

const vnTimeFormatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VN_TIME_ZONE,
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
  return mins === 0 ? `${hours} giờ` : `${hours} giờ ${mins} phút`
}

export const WARNING_REASON_META: Record<ExpiringWarningReason, { label: string; color: string }> =
  {
    EXPIRING_DATE: { label: 'Sắp hết hạn', color: 'orange' },
    LOW_SESSIONS: { label: 'Sắp hết buổi', color: 'volcano' },
  }

export const PACKAGE_TYPE_LABELS: Record<DashboardPackageType, string> = {
  TIME: 'Theo thời gian',
  SESSION: 'Theo buổi',
  PT: 'HLV cá nhân',
}
