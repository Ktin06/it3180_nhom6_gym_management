import { describe, expect, it } from 'vitest'
import { HOME_PATH_BY_ROLE, ROLES } from '../auth/roles'
import { canOpen, ROUTE_ACCESS, rolesFor } from './route-access'

describe('ROUTE_ACCESS', () => {
  describe('TC-F5.3.1-08j: %s mở được danh sách và chi tiết hóa đơn', () => {
    it.each(['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST'] as const)('trường hợp %s', (role) => {
      expect(canOpen(role, '/hoa-don')).toBe(true)
      expect(canOpen(role, '/hoa-don/88')).toBe(true)
      expect(canOpen(role, '/member/invoices/88/pdf')).toBe(false)
    })
  })
  describe('TC-F5.3.1-08k: %s không mở được trang hóa đơn của nhân viên', () => {
    it.each(['TRAINER', 'MEMBER'] as const)('trường hợp %s', (role) => {
      expect(canOpen(role, '/hoa-don')).toBe(false)
      expect(canOpen(role, '/hoa-don/88')).toBe(false)
    })
  })
  it('TC-F5.4.1-04f: chỉ MEMBER mở điểm vào PDF hội viên', () => {
    expect(canOpen('MEMBER', '/member/invoices/88/pdf')).toBe(true)
    expect(canOpen('TRAINER', '/member/invoices/88/pdf')).toBe(false)
  })
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
