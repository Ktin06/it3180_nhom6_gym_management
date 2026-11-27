import { LogoutOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Input, Select, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useRef, useState } from 'react'
import { MemberAvatar } from '../../members/components/MemberAvatar'
import { errorMessage } from '../../shared/api-error'
import { useRemoteQuery } from '../../shared/use-remote-query'
import {
  CHECKIN_METHOD_LABELS,
  CHECKIN_STATUS_META,
  formatDurationMinutes,
  formatVnTime,
  PACKAGE_TYPE_META,
} from '../checkin-format'
import type {
  CheckinGateway,
  CheckinHistoryItem,
  CheckinHistoryQuery,
  CheckinMethod,
  CheckinStatus,
} from '../types/checkin'

const initialHistoryQuery: CheckinHistoryQuery = { page: 0, size: 20 }

export interface CheckinHistoryTabProps {
  api: Pick<CheckinGateway, 'history'>
  refreshKey: number
  checkoutBusyId: number | null
  onCheckout: (checkinId: number) => void
  loadAvatar?: (memberId: number) => Promise<Blob>
}

/**
 * Tab lịch sử check-in trong ngày tại chi nhánh (UC-F4.5):
 * lọc ngày, trạng thái, phương thức, từ khóa, phân trang, giờ hiển thị theo Asia/Ho_Chi_Minh.
 */
export function CheckinHistoryTab({
  api,
  refreshKey,
  checkoutBusyId,
  onCheckout,
  loadAvatar,
}: CheckinHistoryTabProps) {
  const loadHistory = useCallback((query: CheckinHistoryQuery) => api.history(query), [api])
  const list = useRemoteQuery(loadHistory, initialHistoryQuery)
  const [keyword, setKeyword] = useState('')
  const prevRefreshKey = useRef(refreshKey)

  useEffect(() => {
    if (prevRefreshKey.current !== refreshKey) {
      prevRefreshKey.current = refreshKey
      list.reload()
    }
  }, [list, refreshKey])

  const columns: ColumnsType<CheckinHistoryItem> = [
    {
      title: 'Hội viên',
      key: 'member',
      render: (_, row) => (
        <div className="checkin-history-member">
          <MemberAvatar
            memberId={row.memberId}
            fullName={row.fullName}
            hasAvatar={row.hasAvatar}
            size={36}
            loadAvatar={loadAvatar}
          />
          <div>
            <Typography.Text strong>{row.fullName}</Typography.Text>
            <div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {row.memberCode}
              </Typography.Text>
            </div>
          </div>
        </div>
      ),
    },
    {
      title: 'Gói áp dụng',
      key: 'package',
      render: (_, row) => (
        <div>
          <div>{row.packageName}</div>
          <Tag color={PACKAGE_TYPE_META[row.packageType].color} style={{ marginTop: 2 }}>
            {row.packageType}
          </Tag>
          {row.sessionDeducted && <Tag color="volcano">-1 buổi</Tag>}
        </div>
      ),
    },
    {
      title: 'Giờ vào',
      dataIndex: 'checkedInAt',
      key: 'checkedInAt',
      render: (value: string) => formatVnTime(value),
    },
    {
      title: 'Giờ ra',
      dataIndex: 'checkedOutAt',
      key: 'checkedOutAt',
      render: (value: string | null) => (value ? formatVnTime(value) : '—'),
    },
    {
      title: 'Thời lượng',
      dataIndex: 'durationMinutes',
      key: 'durationMinutes',
      render: (value: number | null) => formatDurationMinutes(value),
    },
    {
      title: 'Phương thức',
      dataIndex: 'method',
      key: 'method',
      render: (value: CheckinMethod) => CHECKIN_METHOD_LABELS[value] ?? value,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (value: CheckinStatus) => {
        const meta = CHECKIN_STATUS_META[value]
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: 'Nhân viên',
      dataIndex: 'createdByName',
      key: 'createdByName',
      render: (value: string | null) => value ?? '—',
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, row) =>
        row.status === 'CHECKED_IN' ? (
          <Button
            size="small"
            icon={<LogoutOutlined />}
            loading={checkoutBusyId === row.checkinId}
            onClick={() => onCheckout(row.checkinId)}
          >
            Check-out
          </Button>
        ) : (
          <Typography.Text type="secondary">Đã ra về</Typography.Text>
        ),
    },
  ]

  return (
    <div>
      <div className="feature-filters">
        <div className="feature-filter">
          <label htmlFor="checkin-history-date">Ngày check-in</label>
          <Input
            id="checkin-history-date"
            type="date"
            value={list.query.date ?? ''}
            onChange={(e) =>
              list.changeQuery({
                ...list.query,
                date: e.target.value || undefined,
                page: 0,
              })
            }
          />
        </div>

        <div className="feature-filter">
          <label htmlFor="checkin-history-status">Trạng thái</label>
          <Select
            id="checkin-history-status"
            value={list.query.status ?? 'ALL'}
            options={[
              { value: 'ALL', label: 'Tất cả trạng thái' },
              { value: 'CHECKED_IN', label: 'Đang tập' },
              { value: 'CHECKED_OUT', label: 'Đã ra về' },
            ]}
            onChange={(value) =>
              list.changeQuery({
                ...list.query,
                status: value === 'ALL' ? undefined : (value as CheckinStatus),
                page: 0,
              })
            }
          />
        </div>

        <div className="feature-filter">
          <label htmlFor="checkin-history-method">Phương thức</label>
          <Select
            id="checkin-history-method"
            value={list.query.method ?? 'ALL'}
            options={[
              { value: 'ALL', label: 'Tất cả phương thức' },
              { value: 'MANUAL', label: 'Thủ công' },
              { value: 'FACE', label: 'Khuôn mặt' },
            ]}
            onChange={(value) =>
              list.changeQuery({
                ...list.query,
                method: value === 'ALL' ? undefined : (value as CheckinMethod),
                page: 0,
              })
            }
          />
        </div>

        <div className="feature-filter feature-search">
          <label htmlFor="checkin-history-keyword">Tìm theo tên hoặc mã</label>
          <Input.Search
            id="checkin-history-keyword"
            placeholder="Nhập tên hoặc mã HV..."
            allowClear
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value)
              if (!e.target.value) {
                list.changeQuery({ ...list.query, keyword: undefined, page: 0 })
              }
            }}
            onSearch={(val) =>
              list.changeQuery({
                ...list.query,
                keyword: val.trim() || undefined,
                page: 0,
              })
            }
            enterButton="Lọc"
          />
        </div>

        <Button icon={<ReloadOutlined />} onClick={list.reload} loading={list.loading}>
          Làm mới
        </Button>
      </div>

      {list.error != null && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          title="Không tải được lịch sử check-in"
          description={errorMessage(list.error)}
        />
      )}

      <Table<CheckinHistoryItem>
        rowKey="checkinId"
        loading={list.loading}
        columns={columns}
        dataSource={list.data?.content ?? []}
        scroll={{ x: 'max-content' }}
        locale={{ emptyText: 'Chưa có lượt check-in nào phù hợp bộ lọc.' }}
        pagination={{
          current: (list.data?.page ?? list.query.page) + 1,
          pageSize: list.data?.size ?? list.query.size,
          total: list.data?.totalElements ?? 0,
          showSizeChanger: true,
          onChange: (page, pageSize) =>
            list.changeQuery({
              ...list.query,
              page: page - 1,
              size: pageSize,
            }),
        }}
      />
    </div>
  )
}
