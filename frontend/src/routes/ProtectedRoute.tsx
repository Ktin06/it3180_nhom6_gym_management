import type { ReactNode } from 'react'
import { useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Alert, Button, Flex, Spin } from 'antd'
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
  const { user, status, sessionError, retrySession } = useAuth()
  const location = useLocation()
  const [retrying, setRetrying] = useState(false)

  const handleRetry = async () => {
    setRetrying(true)
    try {
      await retrySession()
    } catch {
      // Provider giữ thông báo lỗi để người dùng có thể thử lại.
    } finally {
      setRetrying(false)
    }
  }

  if (status === 'initializing') {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100vh' }} role="status">
        <Spin tip="Đang kiểm tra phiên đăng nhập…" />
      </Flex>
    )
  }
  if (status === 'recoverable-error') {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100vh', padding: 24 }}>
        <Alert
          type="warning"
          showIcon
          title={sessionError ?? 'Chưa thể xác minh phiên đăng nhập. Hãy thử lại.'}
          action={
            <Button loading={retrying} onClick={() => void handleRetry()}>
              Thử lại
            </Button>
          }
        />
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
