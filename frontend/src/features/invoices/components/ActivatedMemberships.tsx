import { Descriptions, Space, Tag } from 'antd'
import { formatInvoiceDate } from '../invoice-format'
import type { ActivatedMembership } from '../types/invoice'

export function ActivatedMemberships({ memberships }: { memberships: ActivatedMembership[] }) {
  return (
    <Space orientation="vertical" style={{ width: '100%' }}>
      {memberships.map((membership) => (
        <Descriptions key={membership.membershipId} size="small" column={1}>
          <Descriptions.Item label={`Gói ${membership.packageType}`}>
            <Tag color={membership.status === 'ACTIVE' ? 'green' : 'blue'}>
              {membership.status === 'ACTIVE'
                ? 'Đang hiệu lực (ACTIVE)'
                : 'Chờ kích hoạt (UPCOMING)'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Hiệu lực">
            {formatInvoiceDate(membership.startDate)} – {formatInvoiceDate(membership.endDate)}
          </Descriptions.Item>
          {membership.remainingSessions !== null && (
            <Descriptions.Item label="Số buổi còn lại">
              {membership.remainingSessions}
            </Descriptions.Item>
          )}
        </Descriptions>
      ))}
    </Space>
  )
}
