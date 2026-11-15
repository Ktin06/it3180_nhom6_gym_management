/** Cùng cách chuẩn hóa với PhoneUtils của backend F2; server vẫn kiểm tra đầu vào. */
export function normalizePhone(raw: string) {
  let phone = raw.replace(/[\s.-]/g, '')
  if (phone.startsWith('+84')) phone = `0${phone.slice(3)}`
  else if (phone.startsWith('84') && phone.length === 11) phone = `0${phone.slice(2)}`
  return phone
}

/** Tiền tố ngắn giữ nguyên như MemberPhoneSupport, chỉ chuẩn hóa số đầy đủ hợp lệ. */
export function normalizePhoneSearch(raw: string) {
  const cleaned = raw.replace(/[\s.-]/g, '')
  const fullLength = cleaned.startsWith('+84') ? 12 : cleaned.startsWith('84') ? 11 : 10
  if (cleaned.length < fullLength) return cleaned
  const phone = normalizePhone(cleaned)
  return /^0\d{9}$/.test(phone) ? phone : cleaned
}
