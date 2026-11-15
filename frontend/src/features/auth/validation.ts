export function normalizeEmail(value: string) {
  return value.trim().toLowerCase()
}

export function normalizePhone(value: string) {
  const compact = value.trim().replace(/[\s().-]/g, '')
  return compact.startsWith('+84') ? `0${compact.slice(3)}` : compact
}

export function isValidPhone(value: string) {
  return /^0[0-9]{9}$/.test(normalizePhone(value))
}

export function isValidPassword(value: string) {
  return value.length >= 8 && /\p{L}/u.test(value) && /[0-9]/.test(value)
}
