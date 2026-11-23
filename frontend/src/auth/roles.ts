/** Vai trò có tài khoản đăng nhập; khớp giá trị của cột users.role trong ERD. Khách không có vai trò. */
export const ROLES = ['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST', 'TRAINER', 'MEMBER'] as const

export type Role = (typeof ROLES)[number]

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: 'Chủ chuỗi',
  BRANCH_MANAGER: 'Quản lý chi nhánh',
  RECEPTIONIST: 'Lễ tân',
  TRAINER: 'Huấn luyện viên',
  MEMBER: 'Hội viên',
}

/** Các vai trò nhân viên dùng giao diện StaffLayout. */
export const STAFF_ROLES: Role[] = ['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST', 'TRAINER']

/** Vai trò được tra cứu và quản lý hồ sơ hội viên, danh mục gói. */
export const MEMBER_MANAGEMENT_ROLES: Role[] = ['SUPER_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST']

/** Trang chủ của từng vai trò sau khi đăng nhập; quyền mở khai báo ở routes/route-access.ts. */
export const HOME_PATH_BY_ROLE = {
  SUPER_ADMIN: '/admin',
  BRANCH_MANAGER: '/manager',
  RECEPTIONIST: '/reception',
  TRAINER: '/trainer',
  MEMBER: '/member',
} as const satisfies Record<Role, string>
