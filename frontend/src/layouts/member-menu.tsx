import { CalendarOutlined, HomeOutlined, IdcardOutlined, UserOutlined } from '@ant-design/icons'
import { HOME_PATH_BY_ROLE } from '../auth/roles'
import { menuItemsForRole, type MenuItem } from './menu'

/** Thanh điều hướng đáy màn hình của hội viên; mỗi trang một dòng. */
export const MEMBER_MENU: MenuItem[] = [
  { path: HOME_PATH_BY_ROLE.MEMBER, label: 'Trang chủ', icon: <HomeOutlined />, ready: true },
  {
    path: '/member/memberships',
    label: 'Gói tập',
    icon: <IdcardOutlined />,
    ready: false,
    roles: ['MEMBER'],
  }, // F7
  {
    path: '/member/classes',
    label: 'Lớp học',
    icon: <CalendarOutlined />,
    ready: false,
    roles: ['MEMBER'],
  }, // F8
  { path: '/member/pt', label: 'PT', icon: <UserOutlined />, ready: false, roles: ['MEMBER'] }, // F9
]

export function memberMenu(): MenuItem[] {
  return menuItemsForRole(MEMBER_MENU, 'MEMBER')
}
