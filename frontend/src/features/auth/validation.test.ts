import { describe, expect, it } from 'vitest'
import { isValidPassword, isValidPhone, normalizeEmail, normalizePhone } from './validation'

describe('validation F1', () => {
  it('chuẩn hóa email và số Việt Nam theo SRS', () => {
    expect(normalizeEmail('  HV@Example.COM ')).toBe('hv@example.com')
    expect(normalizePhone('+84 912 345 678')).toBe('0912345678')
    expect(isValidPhone('+84 (912) 345-678')).toBe(true)
    expect(isValidPhone('091234567')).toBe(false)
  })

  it('chấp nhận mật khẩu có chữ Unicode cùng chữ số', () => {
    expect(isValidPassword('ĐườngGym123')).toBe(true)
    expect(isValidPassword('duonggym')).toBe(false)
    expect(isValidPassword('Gym123')).toBe(false)
  })
})
