import { describe, expect, it } from 'vitest'
import { menuForRole } from './staff-menu'

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
