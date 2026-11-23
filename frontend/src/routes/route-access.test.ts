import { describe, expect, it } from 'vitest'
import { HOME_PATH_BY_ROLE, ROLES } from '../auth/roles'
import { canOpen, ROUTE_ACCESS, rolesFor } from './route-access'

describe('ROUTE_ACCESS', () => {
  it.each(ROLES)('trang chủ của %s chỉ vai trò đó mở được, gồm cả trang con', (role) => {
    const home = HOME_PATH_BY_ROLE[role]
    expect(rolesFor(home)).toEqual([role])
    expect(canOpen(role, home)).toBe(true)
    expect(canOpen(role, `${home}/con`)).toBe(true)
    for (const other of ROLES.filter((r) => r !== role)) {
      expect(canOpen(other, home)).toBe(false)
    }
  })

  it('không coi đường dẫn chỉ trùng tiền tố chữ là trang con', () => {
    expect(canOpen('MEMBER', '/members')).toBe(false)
    expect(canOpen('RECEPTIONIST', '/receptionist')).toBe(false)
  })

  it('trang không khai báo includeChildren thì không mở rộng sang trang con', () => {
    expect(canOpen('RECEPTIONIST', '/members')).toBe(true)
    expect(canOpen('RECEPTIONIST', '/members/123')).toBe(false)
  })

  it('đường dẫn có tham số khớp theo cú pháp React Router', () => {
    const access = { '/members/:id': { roles: ['RECEPTIONIST'] as const } }
    expect(canOpen('RECEPTIONIST', '/members/42', access)).toBe(true)
    expect(canOpen('RECEPTIONIST', '/members/42/edit', access)).toBe(false)
    expect(canOpen('MEMBER', '/members/42', access)).toBe(false)
  })

  it('đường dẫn chưa khai báo thì không vai trò nào mở được', () => {
    for (const role of ROLES) expect(canOpen(role, '/unknown')).toBe(false)
  })

  it('mọi khóa trong bảng là đường dẫn tuyệt đối', () => {
    for (const path of Object.keys(ROUTE_ACCESS)) expect(path.startsWith('/')).toBe(true)
  })
})
