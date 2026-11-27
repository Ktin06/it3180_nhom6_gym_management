export function normalizeEmail(value: string) {
  return value.trim().toLowerCase()
}

export function isValidPassword(value: string) {
  return (
    /^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$/u.test(value) &&
    !/[\r\n\u0085\u2028\u2029]/u.test(value) &&
    new TextEncoder().encode(value).byteLength <= 72
  )
}
