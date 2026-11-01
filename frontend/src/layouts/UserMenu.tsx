import { LogoutOutlined } from '@ant-design/icons'
import { Button, Space, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { DevRoleSwitcher } from '../auth/DevRoleSwitcher'
import { ROLE_LABELS } from '../auth/roles'
import { useAuth } from '../auth/useAuth'

/** Góc phải thanh tiêu đề: tên vai trò, nút giả lập (chỉ ở chế độ phát triển), đăng xuất. */
export function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <Space size="small" wrap>
      <DevRoleSwitcher />
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
