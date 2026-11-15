import { describe, expect, it } from 'vitest'
import { roleAwareReturnPath, safeReturnPath } from './redirect'

describe('safeReturnPath', () => {
  it('giữ lại đường dẫn nội bộ do route bảo vệ ghi vào state', () => {
    expect(
      safeReturnPath(
        { from: { pathname: '/reception', search: '?tab=members', hash: '#top' } },
        'https://gym.example',
      ),
    ).toBe('/reception?tab=members#top')
  })

  it.each([
    { from: { pathname: 'https://evil.example/login' } },
    { from: { pathname: '//evil.example/login' } },
    { from: { pathname: '/\\evil.example/login' } },
    { redirect: 'https://evil.example/login' },
  ])('bỏ qua state không an toàn: %o', (state) => {
    expect(safeReturnPath(state, 'https://gym.example')).toBeNull()
  })
})

describe('roleAwareReturnPath', () => {
  it('dùng trang chủ của role mới nếu return path thuộc role khác', () => {
    expect(
      roleAwareReturnPath({ from: { pathname: '/member' } }, 'https://gym.example', 'RECEPTIONIST'),
    ).toBe('/reception')
  })

  it('giữ route con, query và hash khi route thuộc role mới đăng nhập', () => {
    expect(
      roleAwareReturnPath(
        { from: { pathname: '/reception/members', search: '?tab=active', hash: '#top' } },
        'https://gym.example',
        'RECEPTIONIST',
      ),
    ).toBe('/reception/members?tab=active#top')
  })

  it('dùng home của role mới khi return path không an toàn', () => {
    expect(
      roleAwareReturnPath(
        { from: { pathname: '//evil.example' } },
        'https://gym.example',
        'MEMBER',
      ),
    ).toBe('/member')
  })
})
