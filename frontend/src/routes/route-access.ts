import { matchPath } from 'react-router-dom'
import { MEMBER_MANAGEMENT_ROLES, type Role } from '../auth/roles'

export interface RouteAccessRule {
  /** Vai trò được mở trang. */
  roles: readonly Role[]
  /** true: mọi đường dẫn con (ví dụ `/reception/...`) cũng thuộc các vai trò này. */
  includeChildren?: boolean
}

export type RouteAccessTable = Readonly<Record<string, RouteAccessRule>>

/**
 * Danh sách duy nhất: trang cần đăng nhập → vai trò được mở.
 * Router (bọc ProtectedRoute), điều hướng sau đăng nhập và menu đều đọc bảng này.
 * Thêm trang mới: thêm một dòng ở đây trước, rồi mới thêm route và mục menu.
 * Đường dẫn viết theo cú pháp React Router, có thể có tham số, ví dụ `/members/:id`.
 *
 * Đây chỉ là trải nghiệm người dùng; backend vẫn kiểm tra vai trò và quyền sở hữu (S3).
 */
export const ROUTE_ACCESS = {
  // Trang chủ từng vai trò (khớp HOME_PATH_BY_ROLE), gồm cả trang con.
  '/admin': { roles: ['SUPER_ADMIN'], includeChildren: true },
  '/manager': { roles: ['BRANCH_MANAGER'], includeChildren: true },
  '/reception': { roles: ['RECEPTIONIST'], includeChildren: true },
  '/trainer': { roles: ['TRAINER'], includeChildren: true },
  '/member': { roles: ['MEMBER'], includeChildren: true },
  // Trang chức năng của nhân viên.
  '/members': { roles: MEMBER_MANAGEMENT_ROLES }, // F2
  '/packages': { roles: MEMBER_MANAGEMENT_ROLES }, // F3
  '/ban-goi': { roles: ['RECEPTIONIST'] }, // F3
  '/hoa-don': { roles: MEMBER_MANAGEMENT_ROLES }, // F5
  '/hoa-don/:id': { roles: MEMBER_MANAGEMENT_ROLES }, // F5
  '/member/invoices/:id/pdf': { roles: ['MEMBER'] }, // F5, điểm vào từ lịch sử F7
} satisfies RouteAccessTable

/** Đường dẫn đã khai báo quyền; viết sai tên thì TypeScript báo lỗi. */
export type ProtectedPath = keyof typeof ROUTE_ACCESS

/** Vai trò được mở một trang đã khai báo; router dùng để bọc ProtectedRoute. */
export function rolesFor(path: ProtectedPath): Role[] {
  return [...ROUTE_ACCESS[path].roles]
}

/** Vai trò có được mở đường dẫn thật (pathname, không gồm query) này không. */
export function canOpen(
  role: Role,
  pathname: string,
  access: RouteAccessTable = ROUTE_ACCESS,
): boolean {
  return Object.entries(access).some(
    ([path, rule]) =>
      rule.roles.includes(role) &&
      matchPath({ path, end: !rule.includeChildren, caseSensitive: true }, pathname) !== null,
  )
}
