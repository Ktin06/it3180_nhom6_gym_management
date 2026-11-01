import { Typography } from 'antd'
import { ROLE_LABELS, type Role } from '../../auth/roles'

/** Trang chủ tạm cho từng vai trò; nội dung thật do các chức năng bổ sung sau. */
export function RoleHomePage({ role }: { role: Role }) {
  return (
    <>
      <Typography.Title level={3}>Trang chủ – {ROLE_LABELS[role]}</Typography.Title>
      <Typography.Paragraph type="secondary">Nội dung sẽ bổ sung.</Typography.Paragraph>
    </>
  )
}
