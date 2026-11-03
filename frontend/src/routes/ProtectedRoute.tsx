import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Flex, Spin } from 'antd'
import type { Role } from '../auth/roles'
import { useAuth } from '../auth/useAuth'

interface ProtectedRouteProps {
  /** Danh sách vai trò được vào. */
  allowedRoles: Role[]
  /** Nội dung cần bảo vệ; bỏ trống thì hiển thị route con qua Outlet. */
  children?: ReactNode
}

/**
 * Chặn ở giao diện: chưa đăng nhập thì chuyển /login, sai vai trò thì chuyển /403.
 * Đây chỉ là trải nghiệm người dùng; backend vẫn phải kiểm tra vai trò và quyền sở hữu (S3).
 */
export function ProtectedRoute({ allowedRoles, children }: ProtectedRouteProps) {
  const { user, status } = useAuth()
  const location = useLocation()

  if (status === 'initializing') {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100vh' }} role="status">
        <Spin tip="Đang kiểm tra phiên đăng nhập…" />
      </Flex>
    )
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/403" replace />
  }
  return children ?? <Outlet />
}
