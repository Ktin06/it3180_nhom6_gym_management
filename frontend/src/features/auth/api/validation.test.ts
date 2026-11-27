import { describe, expect, it } from 'vitest'
import { isValidPhone, normalizePhone } from '../../../services/phone'
import { isValidPassword, normalizeEmail } from './validation'

describe('validation F1', () => {
  it('chuẩn hóa email và số Việt Nam theo SRS', () => {
    expect(normalizeEmail('  HV@Example.COM ')).toBe('hv@example.com')
    expect(normalizePhone('+84 912 345 678')).toBe('0912345678')
    expect(isValidPhone('+84 912.345-678')).toBe(true)
    expect(isValidPhone('+84 (912) 345-678')).toBe(false)
    expect(isValidPhone('091234567')).toBe(false)
    expect(normalizePhone('84912345678')).toBe('0912345678')
    expect(isValidPhone('84 912 345 678')).toBe(true)
    expect(isValidPhone('849123456789')).toBe(false)
  })

  it('TC-F1.1.1-04k: khớp quy tắc chữ ASCII và số của backend, vẫn cho Unicode cùng chữ ASCII', () => {
    expect(isValidPassword('ĐườngGym123')).toBe(true)
    expect(isValidPassword('duonggym')).toBe(false)
    expect(isValidPassword('Gym123')).toBe(false)
    expect(isValidPassword('ầầầầầầầ1')).toBe(false)
    expect(isValidPassword('a123456\n')).toBe(false)
    expect(isValidPassword('a123456\u0085')).toBe(false)
    expect(isValidPassword('😀😀😀😀a1')).toBe(false)
  })

  it('TC-F1.1.1-04l: giới hạn đúng 72 byte UTF-8 thay vì đếm ký tự', () => {
    expect(isValidPassword(`${'a'.repeat(71)}1`)).toBe(true)
    expect(isValidPassword(`${'a'.repeat(72)}1`)).toBe(false)
    expect(isValidPassword(`${'ầ'.repeat(22)}abc123`)).toBe(true)
    expect(isValidPassword(`${'ầ'.repeat(23)}abc123`)).toBe(false)
  })
})
