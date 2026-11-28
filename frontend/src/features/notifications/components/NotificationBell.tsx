import { BellOutlined } from '@ant-design/icons'
import {
  Alert,
  Badge,
  Button,
  Drawer,
  Empty,
  Pagination,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTE_ACCESS } from '../../../routes/route-access'
import type { MemberNotification } from '../api/notifications'
import { notificationPageSize, useNotificationInbox } from '../use-notification-inbox'

const typeLabels: Record<string, string> = {
  MEMBERSHIP_EXPIRING: 'Gói tập sắp hết hạn hoặc hết buổi',
  CLASS_REMINDER: 'Nhắc lịch lớp',
  CLASS_RESCHEDULED: 'Lớp đổi giờ',
  CLASS_ROOM_CHANGED: 'Lớp đổi phòng',
  CLASS_CANCELLED: 'Lớp bị hủy',
  PT_SESSION_CANCELLED: 'Buổi PT bị hủy',
}

const relations: Record<string, { label: string; path: string }> = {
  memberships: { label: 'Gói tập', path: '/member/memberships' },
  class_bookings: { label: 'Lượt đặt lớp', path: '/member/classes' },
  class_sessions: { label: 'Buổi lớp', path: '/member/classes' },
  pt_sessions: { label: 'Buổi PT', path: '/member/pt' },
}

const dateFormat = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: 'Asia/Ho_Chi_Minh',
})

function RelatedItem({ item, onNavigate }: { item: MemberNotification; onNavigate: () => void }) {
  if (!item.relatedEntity || item.relatedId == null) return null
  const relation = relations[item.relatedEntity]
  if (!relation) return null
  const label = `${relation.label} #${item.relatedId}`
  return Object.hasOwn(ROUTE_ACCESS, relation.path) ? (
    <Link to={relation.path} onClick={onNavigate}>
      {label}
    </Link>
  ) : (
    <Typography.Text type="secondary">{label}</Typography.Text>
  )
}

export function NotificationBell({ authEpoch }: { authEpoch: number }) {
  const [open, setOpen] = useState(false)
  const [page, setPage] = useState(0)
  const [activeId, setActiveId] = useState<number>()
  const inbox = useNotificationInbox(authEpoch, open, page)

  function openInbox() {
    setPage(0)
    setActiveId(undefined)
    setOpen(true)
  }

  function openItem(item: MemberNotification) {
    setActiveId(item.id)
    if (!item.read) void inbox.markRead(item.id)
  }

  return (
    <>
      <Badge count={inbox.unreadCount ?? 0} size="small">
        <Button
          aria-label={inbox.unreadCount ? `Thông báo, ${inbox.unreadCount} chưa đọc` : 'Thông báo'}
          aria-haspopup="dialog"
          icon={<BellOutlined />}
          onClick={openInbox}
        />
      </Badge>
      <Drawer title="Thông báo" open={open} onClose={() => setOpen(false)} size="min(100vw, 440px)">
        <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
          {inbox.loadError && (
            <Alert
              type="error"
              showIcon
              title="Chưa tải được thông báo."
              action={
                <Button size="small" onClick={inbox.refresh}>
                  Thử lại
                </Button>
              }
            />
          )}
          {inbox.items == null && !inbox.loadError && <Skeleton active />}
          {inbox.items?.length === 0 && !inbox.loadError && (
            <Empty description="Bạn chưa có thông báo" />
          )}
          {inbox.items && inbox.items.length > 0 && (
            <ul
              aria-label="Danh sách thông báo"
              style={{ listStyle: 'none', margin: 0, padding: 0 }}
            >
              {inbox.items.map((item) => (
                <li
                  key={item.id}
                  style={{
                    borderBottom: '1px solid #eee',
                    padding: 12,
                    background: item.read ? undefined : '#f0f7ff',
                    overflowWrap: 'anywhere',
                  }}
                >
                  <Space orientation="vertical" size={6} style={{ width: '100%' }}>
                    <Space wrap size={[6, 4]}>
                      <Tag color={item.read ? undefined : 'blue'}>
                        {item.read ? 'Đã đọc' : 'Chưa đọc'}
                      </Tag>
                      <Typography.Text type="secondary">
                        {typeLabels[item.type] ?? 'Thông báo'}
                      </Typography.Text>
                    </Space>
                    <Button
                      type="link"
                      style={{
                        padding: 0,
                        height: 'auto',
                        whiteSpace: 'normal',
                        textAlign: 'start',
                      }}
                      aria-expanded={activeId === item.id}
                      loading={inbox.pendingId === item.id}
                      disabled={
                        inbox.pendingId !== undefined && inbox.pendingId !== item.id && !item.read
                      }
                      onClick={() => openItem(item)}
                    >
                      {item.title}
                    </Button>
                    <Typography.Text type="secondary">
                      <time dateTime={item.createdAt}>
                        {dateFormat.format(new Date(item.createdAt))}
                      </time>
                    </Typography.Text>
                    {activeId === item.id && (
                      <>
                        {item.content && (
                          <Typography.Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                            {item.content}
                          </Typography.Paragraph>
                        )}
                        <RelatedItem item={item} onNavigate={() => setOpen(false)} />
                      </>
                    )}
                    {inbox.readErrorId === item.id && (
                      <Alert
                        type="error"
                        title="Chưa đánh dấu được thông báo đã đọc."
                        action={
                          <Button size="small" onClick={() => openItem(item)}>
                            Thử lại
                          </Button>
                        }
                      />
                    )}
                  </Space>
                </li>
              ))}
            </ul>
          )}
          {inbox.total > notificationPageSize && (
            <Pagination
              simple
              size="small"
              aria-label="Phân trang thông báo"
              current={page + 1}
              pageSize={notificationPageSize}
              total={inbox.total}
              onChange={(current) => {
                setActiveId(undefined)
                setPage(current - 1)
              }}
            />
          )}
        </Space>
      </Drawer>
    </>
  )
}
