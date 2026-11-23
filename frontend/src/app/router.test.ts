import type { RouteObject } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { MEMBER_MENU } from '../layouts/member-menu'
import { STAFF_MENU } from '../layouts/staff-menu'
import { ROUTE_ACCESS } from '../routes/route-access'
import { routes } from './router'

function allPaths(list: RouteObject[]): string[] {
  return list.flatMap((route) => [
    ...(route.path ? [route.path] : []),
    ...(route.children ? allPaths(route.children) : []),
  ])
}

describe('router khớp với ROUTE_ACCESS và menu', () => {
  const paths = allPaths(routes)

  it('mọi mục menu đã bật (ready) đều có route', () => {
    for (const item of [...STAFF_MENU, ...MEMBER_MENU].filter((i) => i.ready)) {
      expect(paths, item.path).toContain(item.path)
    }
  })

  it('mọi trang khai báo trong ROUTE_ACCESS đều có route', () => {
    for (const path of Object.keys(ROUTE_ACCESS)) expect(paths, path).toContain(path)
  })

  it('trang dưới /member chỉ dành cho hội viên (MemberLayout chỉ cho MEMBER vào)', () => {
    for (const [path, rule] of Object.entries(ROUTE_ACCESS)) {
      if (path === '/member' || path.startsWith('/member/')) {
        expect(rule.roles, path).toEqual(['MEMBER'])
      }
    }
  })
})
