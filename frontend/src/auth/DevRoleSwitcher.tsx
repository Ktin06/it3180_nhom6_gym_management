import { Select, Space, Tag } from 'antd'
import { useNavigate } from 'react-router-dom'
import { HOME_PATH_BY_ROLE, ROLE_LABELS, ROLES, type Role } from './roles'
import { useAuth } from './useAuth'

/**
 * Nút chọn vai trò giả lập, CHỈ hiện ở chế độ phát triển (npm run dev).
 * Sẽ bỏ khi có đăng nhập thật ở F1.
 */
export function DevRoleSwitcher() {
  const { user, loginAs } = useAuth()
  const navigate = useNavigate()

  if (!import.meta.env.DEV) {
    return null
  }

  const handleChange = (role: Role) => {
    loginAs(role)
    navigate(HOME_PATH_BY_ROLE[role])
  }

  return (
    <Space size={4}>
      <Tag color="orange">DEV</Tag>
      <Select<Role>
        size="small"
        style={{ minWidth: 160 }}
        placeholder="Giả lập vai trò"
        value={user?.role}
        onChange={handleChange}
        options={ROLES.map((role) => ({ value: role, label: ROLE_LABELS[role] }))}
      />
    </Space>
  )
}
