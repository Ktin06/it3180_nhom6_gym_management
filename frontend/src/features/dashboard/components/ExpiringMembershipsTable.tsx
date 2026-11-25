import { Button, Card, Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { MemberAvatar } from '../../members/components/MemberAvatar'
import { formatIsoDate, PACKAGE_TYPE_LABELS, WARNING_REASON_META } from '../dashboard-format'
import type { ExpiringMembership } from '../types/dashboard'

export interface ExpiringMembershipsTableProps {
  items: ExpiringMembership[]
  loading?: boolean
  canRenew: boolean
  onRenew: (item: ExpiringMembership) => void
  loadAvatar?: (memberId: number) => Promise<Blob>
}

/** Bảng cảnh báo gói sắp hết hạn / sắp hết buổi (UC-F6.4). */
export function ExpiringMembershipsTable({
  items,
  loading = false,
  canRenew,
  onRenew,
  loadAvatar,
}: ExpiringMembershipsTableProps) {
  const columns: ColumnsType<ExpiringMembership> = [
    {
      title: 'Hội viên',
      key: 'member',
      render: (_, record) => (
        <div className="dashboard-member-cell">
          <MemberAvatar
            memberId={record.memberId}
            fullName={record.fullName}
            hasAvatar={record.hasAvatar}
            size={36}
            loadAvatar={loadAvatar}
          />
          <div className="dashboard-member-meta">
            <Typography.Text strong>{record.fullName}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {record.memberCode} · {record.phone}
            </Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: 'Gói hiện tại',
      key: 'package',
      render: (_, record) => (
        <div className="dashboard-member-meta">
          <Typography.Text>{record.packageName}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {PACKAGE_TYPE_LABELS[record.packageType]} · Hết hạn {formatIsoDate(record.endDate)}
          </Typography.Text>
        </div>
      ),
    },
    {
      title: 'Cảnh báo',
      key: 'warning',
      render: (_, record) => {
        const meta = WARNING_REASON_META[record.warningReason]
        return (
          <div className="dashboard-member-meta">
            <div>
              <Tag color={meta.color}>{meta.label}</Tag>
            </div>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              Còn {record.daysRemaining} ngày
              {record.packageType === 'SESSION' && record.remainingSessions !== null
                ? ` · Còn ${record.remainingSessions} buổi`
                : ''}
            </Typography.Text>
          </div>
        )
      },
    },
    ...(canRenew
      ? [
          {
            title: 'Thao tác',
            key: 'action',
            width: 110,
            render: (_: unknown, record: ExpiringMembership) => (
              <Button
                type="primary"
                size="small"
                aria-label={`Gia hạn cho ${record.fullName}`}
                onClick={() => onRenew(record)}
                style={{ minHeight: 34 }}
              >
                Gia hạn
              </Button>
            ),
          },
        ]
      : []),
  ]

  return (
    <Card
      title="Gói sắp hết hạn / sắp hết buổi"
      data-testid="expiring-memberships-section"
      className="dashboard-table-card"
    >
      <Table<ExpiringMembership>
        rowKey="membershipId"
        size="small"
        loading={loading}
        dataSource={items}
        columns={columns}
        pagination={false}
        scroll={{ x: 'max-content' }}
        locale={{
          emptyText: <Empty description="Chưa có gói nào sắp hết hạn hoặc sắp hết buổi." />,
        }}
      />
    </Card>
  )
}
