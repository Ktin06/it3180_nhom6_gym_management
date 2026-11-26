import {
  BarChartOutlined,
  CalendarOutlined,
  DollarOutlined,
  HomeOutlined,
  IdcardOutlined,
  LoginOutlined,
  ShopOutlined,
  ShoppingCartOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { HOME_PATH_BY_ROLE, type Role } from '../auth/roles'
import { menuItemsForRole, type MenuItem } from './menu'

export type StaffMenuItem = MenuItem

const home = (role: Role): StaffMenuItem => ({
  path: HOME_PATH_BY_ROLE[role],
  label: 'Trang chủ',
  icon: <HomeOutlined />,
  ready: true,
})

/**
 * Menu bên trái của nhân viên. Mục `ready: true` hiện cho vai trò được mở trang trong
 * ROUTE_ACCESS; mục chưa làm hiện mờ cho các vai trò ghi tạm trong `roles`.
 */
export const STAFF_MENU: StaffMenuItem[] = [
  home('SUPER_ADMIN'),
  home('BRANCH_MANAGER'),
  home('RECEPTIONIST'),
  home('TRAINER'),
  {
    path: '/members',
    label: 'Hội viên (F2)',
    icon: <TeamOutlined />,
    ready: true,
  },
  {
    path: '/packages',
    label: 'Gói tập (F3)',
    icon: <IdcardOutlined />,
    ready: true,
  },
  {
    path: '/ban-goi',
    label: 'Bán gói (F3)',
    icon: <ShoppingCartOutlined />,
    ready: true,
  },
  {
    path: '/checkins',
    label: 'Check-in (F4)',
    icon: <LoginOutlined />,
    ready: true,
  },
  {
    path: '/hoa-don',
    label: 'Thanh toán (F5)',
    icon: <DollarOutlined />,
    ready: true,
  },
  // S2-12.2: giao diện dashboard cơ bản (F6)
  {
    path: '/dashboard',
    label: 'Dashboard (F6)',
    icon: <BarChartOutlined />,
    ready: true,
  },
  {
    path: '/dat-cho-lop',
    label: 'Đặt chỗ lớp (F8)',
    icon: <CalendarOutlined />,
    ready: true,
  },
  {
    path: '/classes',
    label: 'Lớp học (F8)',
    icon: <CalendarOutlined />,
    ready: true,
  },
  {
    path: '/branches',
    label: 'Chi nhánh (F18)',
    icon: <ShopOutlined />,
    roles: ['SUPER_ADMIN'],
    ready: false,
  },
  // S2-20.2: giao diện huấn luyện viên (F9)
  {
    path: '/pt/lich',
    label: 'Lịch của tôi (F9)',
    icon: <CalendarOutlined />,
    ready: true,
  },
  {
    path: '/pt/hoc-vien',
    label: 'Học viên (F9)',
    icon: <TeamOutlined />,
    ready: true,
  },
]

export function menuForRole(role: Role): StaffMenuItem[] {
  return menuItemsForRole(STAFF_MENU, role)
}
