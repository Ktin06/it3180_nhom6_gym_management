import { describe, expect, it } from 'vitest'
import { HOME_PATH_BY_ROLE, ROLES, STAFF_ROLES } from '../auth/roles'
import { canOpen } from '../routes/route-access'
import { menuForRole, STAFF_MENU } from './staff-menu'

describe('menu quản lý F2/F3', () => {
  it.each(['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST'] as const)(
    '%s có thể mở hội viên và gói tập',
    (role) => {
      const paths = menuForRole(role)
        .filter((item) => item.ready)
        .map((item) => item.path)
      expect(paths).toContain('/members')
      expect(paths).toContain('/packages')
    },
  )

  it.each(['TRAINER', 'MEMBER'] as const)('%s không có menu tra cứu nội bộ', (role) => {
    expect(menuForRole(role).map((item) => item.path)).not.toContain('/members')
    expect(menuForRole(role).map((item) => item.path)).not.toContain('/packages')
  })
})

describe('menu nhân viên đọc quyền từ ROUTE_ACCESS', () => {
  it('mục đã bật (ready) nào cũng có ít nhất một vai trò được mở trong ROUTE_ACCESS', () => {
    for (const item of STAFF_MENU.filter((i) => i.ready)) {
      expect(
        ROLES.some((role) => canOpen(role, item.path)),
        item.path,
      ).toBe(true)
    }
  })

  it('mỗi vai trò nhân viên thấy đúng một mục Trang chủ, là trang chủ của mình', () => {
    for (const role of STAFF_ROLES) {
      const homes = menuForRole(role).filter((item) => item.label === 'Trang chủ')
      expect(homes.map((item) => item.path)).toEqual([HOME_PATH_BY_ROLE[role]])
    }
  })
})
