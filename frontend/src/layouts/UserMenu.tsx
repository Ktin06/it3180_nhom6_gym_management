import { LogoutOutlined } from '@ant-design/icons'
import { App, Button, Space, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { ROLE_LABELS } from '../auth/roles'
import { useAuth } from '../auth/useAuth'

/** Góc phải thanh tiêu đề: tên vai trò và nút đăng xuất. */
export function UserMenu() {
  const { user, logout } = useAuth()
  const { message } = App.useApp()
  const navigate = useNavigate()

  const handleLogout = async () => {
    navigate('/login')
    try {
      await logout()
    } catch {
      message.warning('Đã đăng xuất trên thiết bị này nhưng chưa thể thu hồi phiên trên máy chủ.')
    }
  }

  return (
    <Space size="small" wrap>
      {user && (
        <>
          <Typography.Text type="secondary" className="hide-on-mobile">
            {ROLE_LABELS[user.role]}
          </Typography.Text>
          <Button size="small" icon={<LogoutOutlined />} onClick={handleLogout}>
            Đăng xuất
          </Button>
        </>
      )}
    </Space>
  )
}
