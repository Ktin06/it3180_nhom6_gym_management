import { LogoutOutlined } from '@ant-design/icons'
import { App, Button, Space, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROLE_LABELS } from '../auth/roles'
import { useAuth } from '../auth/useAuth'

/** Góc phải thanh tiêu đề: tên vai trò và nút đăng xuất. */
export function UserMenu() {
  const { user, logout } = useAuth()
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [logoutFailed, setLogoutFailed] = useState(false)
  const [pending, setPending] = useState(false)

  const handleLogout = async () => {
    setPending(true)
    try {
      await logout()
      setLogoutFailed(false)
      navigate('/login', { replace: true })
    } catch {
      setLogoutFailed(true)
      message.warning('Đăng xuất chưa hoàn tất; phiên có thể vẫn hoạt động. Hãy thử lại.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Space size="small" wrap>
      {user && (
        <>
          <Typography.Text type="secondary" className="hide-on-mobile">
            {ROLE_LABELS[user.role]}
          </Typography.Text>
          <Button
            size="small"
            icon={<LogoutOutlined />}
            loading={pending}
            aria-label={logoutFailed ? 'Thử đăng xuất lại' : 'Đăng xuất'}
            onClick={() => void handleLogout()}
          >
            {logoutFailed ? 'Thử lại' : 'Đăng xuất'}
          </Button>
        </>
      )}
    </Space>
  )
}
