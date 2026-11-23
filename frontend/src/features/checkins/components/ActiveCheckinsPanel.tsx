import { LogoutOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Input, Spin, Tag, Typography } from 'antd'
import { useState } from 'react'
import { MemberAvatar } from '../../members/components/MemberAvatar'
import { errorMessage } from '../../shared/api-error'
import type { PageResponse } from '../../shared/types'
import { formatDurationMinutes, formatVnTime } from '../checkin-format'
import type { CheckinHistoryItem, CheckoutResult } from '../types/checkin'

export interface ActiveCheckinsPanelProps {
  data?: PageResponse<CheckinHistoryItem>
  loading: boolean
  error?: unknown
  checkoutBusyId: number | null
  checkoutSuccess: CheckoutResult | null
  checkoutError: unknown
  onSearchKeyword: (keyword: string) => void
  onReload: () => void
  onCheckout: (checkinId: number) => void
  loadAvatar?: (memberId: number) => Promise<Blob>
}

/**
 * Danh sách "Đang tập hôm nay" (GET /api/v1/checkins?status=CHECKED_IN) với nút Check-out nhanh (UC-F4.4).
 */
export function ActiveCheckinsPanel({
  data,
  loading,
  error,
  checkoutBusyId,
  checkoutSuccess,
  checkoutError,
  onSearchKeyword,
  onReload,
  onCheckout,
  loadAvatar,
}: ActiveCheckinsPanelProps) {
  const [quickKeyword, setQuickKeyword] = useState('')
  const items = data?.content ?? []
  const total = data?.totalElements ?? 0

  return (
    <Card
      className="checkin-active-panel"
      title={
        <div className="checkin-card-header">
          <span>Đang tập hôm nay ({total})</span>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={onReload}
            loading={loading}
            aria-label="Tải lại danh sách đang tập"
          >
            Làm mới
          </Button>
        </div>
      }
    >
      <div className="checkin-active-search">
        <Input.Search
          placeholder="Tìm tên hoặc mã HV để check-out..."
          aria-label="Tìm nhanh hội viên đang tập"
          allowClear
          value={quickKeyword}
          onChange={(e) => {
            setQuickKeyword(e.target.value)
            if (!e.target.value) onSearchKeyword('')
          }}
          onSearch={onSearchKeyword}
          enterButton="Lọc"
        />
      </div>

      {checkoutSuccess && (
        <Alert
          type="success"
          showIcon
          style={{ marginBottom: 12 }}
          title={checkoutSuccess.message}
          description={`Giờ ra: ${formatVnTime(checkoutSuccess.checkedOutAt)} · Thời lượng tập: ${formatDurationMinutes(checkoutSuccess.durationMinutes)}`}
        />
      )}

      {checkoutError != null && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 12 }}
          title="Không thể check-out"
          description={errorMessage(checkoutError)}
        />
      )}

      {error != null && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 12 }}
          title="Không tải được danh sách đang tập"
          description={errorMessage(error)}
        />
      )}

      {loading && items.length === 0 ? (
        <div className="checkin-loading-box">
          <Spin />
        </div>
      ) : items.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="Hiện chưa có hội viên nào đang tập."
        />
      ) : (
        <div className="checkin-active-list">
          {items.map((item) => (
            <div key={item.checkinId} className="checkin-active-item">
              <div className="checkin-active-info">
                <MemberAvatar
                  memberId={item.memberId}
                  fullName={item.fullName}
                  hasAvatar={item.hasAvatar}
                  size={40}
                  loadAvatar={loadAvatar}
                />
                <div className="checkin-active-text">
                  <div className="checkin-active-name">
                    <Typography.Text strong>{item.fullName}</Typography.Text>{' '}
                    <Tag>{item.memberCode}</Tag>
                  </div>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    Vào lúc {formatVnTime(item.checkedInAt)} · {item.packageName}
                  </Typography.Text>
                </div>
              </div>
              <Button
                size="middle"
                icon={<LogoutOutlined />}
                loading={checkoutBusyId === item.checkinId}
                onClick={() => onCheckout(item.checkinId)}
                style={{ minHeight: 36 }}
              >
                Check-out
              </Button>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}
