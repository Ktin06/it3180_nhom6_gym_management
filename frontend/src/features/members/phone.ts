import { normalizePhone, stripPhoneSeparators } from '../../services/phone'

/** Tiền tố ngắn giữ nguyên như MemberPhoneSupport, chỉ chuẩn hóa số đầy đủ hợp lệ. */
export function normalizePhoneSearch(raw: string) {
  const cleaned = stripPhoneSeparators(raw)
  const fullLength = cleaned.startsWith('+84') ? 12 : cleaned.startsWith('84') ? 11 : 10
  if (cleaned.length < fullLength) return cleaned
  const phone = normalizePhone(cleaned)
  return /^0\d{9}$/.test(phone) ? phone : cleaned
}
