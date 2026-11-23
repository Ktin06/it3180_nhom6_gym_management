import { describe, expect, it } from 'vitest'
import { roleAwareReturnPath, safeReturnPath } from './redirect'
import { ROUTE_ACCESS } from '../routes/route-access'
import { HOME_PATH_BY_ROLE, MEMBER_MANAGEMENT_ROLES, type Role } from './roles'

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
  it.each(MEMBER_MANAGEMENT_ROLES)('giữ route F2/F3 với vai trò %s', (role) => {
    for (const pathname of ['/members', '/packages']) {
      expect(
        roleAwareReturnPath(
          { from: { pathname, search: '?page=2', hash: '#list' } },
          'https://gym.example',
          role,
        ),
      ).toBe(`${pathname}?page=2#list`)
    }
  })

  it.each<Role>(['MEMBER', 'TRAINER'])('không quay lại F2/F3 với vai trò %s', (role) => {
    for (const pathname of ['/members', '/packages']) {
      expect(roleAwareReturnPath({ from: { pathname } }, 'https://gym.example', role)).toBe(
        HOME_PATH_BY_ROLE[role],
      )
    }
  })

  it.each(['/members-other', '/packages/private', '/unknown'])(
    'không mở rộng quyền điều hướng sang %s',
    (pathname) => {
      expect(
        roleAwareReturnPath({ from: { pathname } }, 'https://gym.example', 'RECEPTIONIST'),
      ).toBe('/reception')
    },
  )

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

  describe('trang mới chỉ cần khai báo một dòng trong ROUTE_ACCESS', () => {
    const access = { ...ROUTE_ACCESS, '/checkins': { roles: ['RECEPTIONIST'] as const } }

    it('lễ tân quay lại đúng trang mới sau khi đăng nhập', () => {
      expect(
        roleAwareReturnPath(
          { from: { pathname: '/checkins', search: '?date=2026-11-19' } },
          'https://gym.example',
          'RECEPTIONIST',
          access,
        ),
      ).toBe('/checkins?date=2026-11-19')
    })

    it('hội viên mở trang mới của lễ tân thì về trang chủ hội viên', () => {
      expect(
        roleAwareReturnPath(
          { from: { pathname: '/checkins' } },
          'https://gym.example',
          'MEMBER',
          access,
        ),
      ).toBe('/member')
    })

    it('chưa khai báo thì lễ tân cũng về trang chủ của mình', () => {
      expect(
        roleAwareReturnPath(
          { from: { pathname: '/checkins' } },
          'https://gym.example',
          'RECEPTIONIST',
        ),
      ).toBe('/reception')
    })
  })
})
