/** Cùng dấu phân cách và quy tắc đầu số với PhoneUtils của backend. */
export function stripPhoneSeparators(value: string) {
  return value.replace(/[ \t\n\v\f\r.-]/g, '')
}

export function normalizePhone(value: string) {
  const compact = stripPhoneSeparators(value)
  if (compact.startsWith('+84')) return '0' + compact.slice(3)
  if (compact.startsWith('84') && compact.length === 11) return '0' + compact.slice(2)
  return compact
}

export function isValidPhone(value: string) {
  return /^0[0-9]{9}$/.test(normalizePhone(value))
}
