export function normalizeEmail(value: string) {
  return value.trim().toLowerCase()
}

export function normalizePhone(value: string) {
  const compact = value.trim().replace(/[\s().-]/g, '')
  if (compact.startsWith('+84')) return `0${compact.slice(3)}`
  if (compact.startsWith('84') && compact.length === 11) return `0${compact.slice(2)}`
  return compact
}

export function isValidPhone(value: string) {
  return /^0[0-9]{9}$/.test(normalizePhone(value))
}

export function isValidPassword(value: string) {
  return (
    /^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$/u.test(value) &&
    !/[\r\n\u0085\u2028\u2029]/u.test(value) &&
    new TextEncoder().encode(value).byteLength <= 72
  )
}
