import { describe, expect, it } from 'vitest'
import { canOpen } from '../routes/route-access'
import { MEMBER_MENU, memberMenu } from './member-menu'

describe('menu hội viên', () => {
  it('hội viên thấy đủ các mục, mục chưa làm vẫn hiện (mờ)', () => {
    expect(memberMenu().map((item) => item.path)).toEqual(MEMBER_MENU.map((item) => item.path))
  })

  it('mục đã bật (ready) nào cũng đã khai báo quyền cho hội viên trong ROUTE_ACCESS', () => {
    for (const item of MEMBER_MENU.filter((i) => i.ready)) {
      expect(canOpen('MEMBER', item.path), item.path).toBe(true)
    }
  })
})
