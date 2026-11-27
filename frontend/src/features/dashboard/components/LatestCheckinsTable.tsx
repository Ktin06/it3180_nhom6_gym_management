import { LogoutOutlined } from '@ant-design/icons'
import { Button, Card, Empty, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { Link } from 'react-router-dom'
import { MemberAvatar } from '../../members/components/MemberAvatar'
import { formatDurationMinutes, formatVnTime } from '../dashboard-format'
import type { LatestCheckin } from '../types/dashboard'

export interface LatestCheckinsTableProps {
  items: LatestCheckin[]
  loading?: boolean
  canCheckout: boolean
  checkoutBusyId: number | null
  onQuickCheckout: (item: LatestCheckin) => void
  loadAvatar?: (memberId: number) => Promise<Blob>
}

/** Bảng 10 lượt check-in mới nhất trong ngày kèm nút Check-out nhanh (UC-F6.5). */
export function LatestCheckinsTable({
  items,
  loading = false,
  canCheckout,
  checkoutBusyId,
  onQuickCheckout,
  loadAvatar,
}: LatestCheckinsTableProps) {
  const columns: ColumnsType<LatestCheckin> = [
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
              {record.memberCode}
            </Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: 'Gói tập',
      dataIndex: 'packageName',
      key: 'packageName',
    },
    {
      title: 'Giờ vào',
      key: 'checkedInAt',
      width: 85,
      render: (_, record) => <span>{formatVnTime(record.checkedInAt)}</span>,
    },
    {
      title: 'Trạng thái',
      key: 'status',
      render: (_, record) =>
        record.status === 'CHECKED_IN' ? (
          <div className="dashboard-status-cell">
            <Tag color="green">Đang tập</Tag>
            {canCheckout && (
              <Button
                size="small"
                icon={<LogoutOutlined />}
                loading={checkoutBusyId === record.checkinId}
                disabled={checkoutBusyId !== null && checkoutBusyId !== record.checkinId}
                aria-label={`Check-out nhanh cho ${record.fullName}`}
                onClick={() => onQuickCheckout(record)}
                style={{ minHeight: 34 }}
              >
                Check-out
              </Button>
            )}
          </div>
        ) : (
          <div className="dashboard-status-cell">
            <Tag color="default">Đã về</Tag>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {formatDurationMinutes(record.durationMinutes)}
            </Typography.Text>
          </div>
        ),
    },
  ]

  return (
    <Card
      title="Lượt check-in mới nhất hôm nay"
      extra={canCheckout ? <Link to="/checkins">Mở màn hình check-in</Link> : undefined}
      data-testid="latest-checkins-section"
      className="dashboard-table-card"
    >
      <Table<LatestCheckin>
        rowKey="checkinId"
        size="small"
        loading={loading}
        dataSource={items}
        columns={columns}
        pagination={false}
        scroll={{ x: 'max-content' }}
        locale={{
          emptyText: <Empty description="Hôm nay chưa có lượt check-in nào." />,
        }}
      />
    </Card>
  )
}
