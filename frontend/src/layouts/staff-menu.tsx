import {
  BarChartOutlined,
  CalendarOutlined,
  DollarOutlined,
  HomeOutlined,
  IdcardOutlined,
  LoginOutlined,
  ShopOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'
import { HOME_PATH_BY_ROLE, type Role } from '../auth/roles'

export interface StaffMenuItem {
  path: string
  label: string
  icon: ReactNode
  roles: Role[]
  /** false khi chức năng chưa làm; đổi thành true khi gộp chức năng tương ứng. */
  ready: boolean
}

const home = (role: Role): StaffMenuItem => ({
  path: HOME_PATH_BY_ROLE[role],
  label: 'Trang chủ',
  icon: <HomeOutlined />,
  roles: [role],
  ready: true,
})

/** Menu bên trái của nhân viên; mỗi vai trò chỉ thấy mục có trong `roles`. */
export const STAFF_MENU: StaffMenuItem[] = [
  home('SUPER_ADMIN'),
  home('BRANCH_MANAGER'),
  home('RECEPTIONIST'),
  home('TRAINER'),
  {
    path: '/members',
    label: 'Hội viên (F2)',
    icon: <TeamOutlined />,
    roles: ['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST'],
    ready: true,
  },
  {
    path: '/packages',
    label: 'Gói tập (F3)',
    icon: <IdcardOutlined />,
    roles: ['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST'],
    ready: true,
  },
  {
    path: '/checkins',
    label: 'Check-in (F4)',
    icon: <LoginOutlined />,
    roles: ['RECEPTIONIST'],
    ready: false,
  },
  {
    path: '/invoices',
    label: 'Thanh toán (F5)',
    icon: <DollarOutlined />,
    roles: ['BRANCH_MANAGER', 'RECEPTIONIST'],
    ready: false,
  },
  {
    path: '/dashboard',
    label: 'Dashboard (F6)',
    icon: <BarChartOutlined />,
    roles: ['SUPER_ADMIN', 'BRANCH_MANAGER'],
    ready: false,
  },
  {
    path: '/classes',
    label: 'Lớp học (F8)',
    icon: <CalendarOutlined />,
    roles: ['BRANCH_MANAGER', 'RECEPTIONIST', 'TRAINER'],
    ready: false,
  },
  {
    path: '/branches',
    label: 'Chi nhánh (F18)',
    icon: <ShopOutlined />,
    roles: ['SUPER_ADMIN'],
    ready: false,
  },
]

export function menuForRole(role: Role): StaffMenuItem[] {
  return STAFF_MENU.filter((item) => item.roles.includes(role))
}
