import { EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Input, Select, Table, Tag, Typography } from 'antd'
import { useCallback, useContext, useState } from 'react'
import { AuthContext } from '../../../auth/AuthContext'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../services/api-error'
import { useRemoteQuery } from '../../../services/use-remote-query'
import { invoicesApi } from '../api/invoices'
import {
  formatVnDateTime,
  INVOICE_SOURCE_LABELS,
  INVOICE_STATUS_META,
  vndFormat,
} from '../invoice-format'
import type {
  InvoiceQuery,
  InvoicesGateway,
  InvoiceSource,
  InvoiceStatus,
  InvoiceSummary,
} from '../types/invoice'
import '../../../components/feature-pages.css'

const initialQuery: InvoiceQuery = { page: 0, size: 20 }

export interface InvoicesPageProps {
  api?: Pick<InvoicesGateway, 'list' | 'branches'>
}

/** Màn hình danh sách và tra cứu hóa đơn tại quầy (UC-F5.3, S2-10.1). */
export function InvoicesPage({ api = invoicesApi }: InvoicesPageProps) {
  const list = useRemoteQuery(api.list, initialQuery)
  const role = useContext(AuthContext)?.user?.role
  const isOwner = role === 'SUPER_ADMIN'
  const loadBranches = useCallback(
    () => (isOwner ? api.branches() : Promise.resolve([])),
    [api, isOwner],
  )
  const branches = useRemoteQuery(loadBranches, undefined)
  const [keyword, setKeyword] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [dateError, setDateError] = useState<string>()

  function applyDateFilter(nextFrom: string, nextTo: string) {
    setFromDate(nextFrom)
    setToDate(nextTo)
    if (nextFrom && nextTo && nextFrom > nextTo) {
      setDateError('Ngày bắt đầu không được lớn hơn ngày kết thúc.')
      return
    }
    setDateError(undefined)
    list.changeQuery({
      ...list.query,
      fromDate: nextFrom || undefined,
      toDate: nextTo || undefined,
      page: 0,
    })
  }

  function applyKeyword(value: string) {
    list.changeQuery({
      ...list.query,
      keyword: value.trim() || undefined,
      page: 0,
    })
  }

  return (
    <section className="feature-page" aria-labelledby="invoices-heading">
      <div className="feature-heading">
        <div>
          <h2 id="invoices-heading">Hóa đơn và thanh toán</h2>
          <p>Tra cứu hóa đơn theo trạng thái, nguồn, ngày Việt Nam và thu tiền tại quầy.</p>
        </div>
      </div>

      <div className="feature-content">
        <div className="feature-filters">
          <div className="feature-filter feature-search">
            <label htmlFor="invoice-keyword">Tìm kiếm</label>
            <Input.Search
              id="invoice-keyword"
              placeholder="Mã hóa đơn, tên hoặc SĐT hội viên"
              allowClear
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onSearch={applyKeyword}
              enterButton="Tìm"
            />
          </div>

          <div className="feature-filter">
            <label htmlFor="invoice-status">Trạng thái</label>
            <Select
              id="invoice-status"
              value={list.query.status ?? 'ALL'}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                ...Object.entries(INVOICE_STATUS_META).map(([value, meta]) => ({
                  value,
                  label: meta.label,
                })),
              ]}
              onChange={(value) =>
                list.changeQuery({
                  ...list.query,
                  status: value === 'ALL' ? undefined : (value as InvoiceStatus),
                  page: 0,
                })
              }
            />
          </div>

          <div className="feature-filter">
            <label htmlFor="invoice-source">Nguồn</label>
            <Select
              id="invoice-source"
              value={list.query.source ?? 'ALL'}
              options={[
                { value: 'ALL', label: 'Tất cả nguồn' },
                ...Object.entries(INVOICE_SOURCE_LABELS).map(([value, label]) => ({
                  value,
                  label,
                })),
              ]}
              onChange={(value) =>
                list.changeQuery({
                  ...list.query,
                  source: value === 'ALL' ? undefined : (value as InvoiceSource),
                  page: 0,
                })
              }
            />
          </div>

          <div className="feature-filter">
            <label htmlFor="invoice-from-date">Từ ngày lập</label>
            <Input
              id="invoice-from-date"
              type="date"
              value={fromDate}
              onChange={(event) => applyDateFilter(event.target.value, toDate)}
            />
          </div>

          <div className="feature-filter">
            <label htmlFor="invoice-to-date">Đến ngày lập</label>
            <Input
              id="invoice-to-date"
              type="date"
              value={toDate}
              onChange={(event) => applyDateFilter(fromDate, event.target.value)}
            />
          </div>

          {isOwner && (
            <div className="feature-filter">
              <label htmlFor="invoice-branch">Chi nhánh</label>
              <Select
                id="invoice-branch"
                allowClear
                placeholder="Toàn chuỗi"
                value={list.query.branchId}
                loading={branches.loading}
                disabled={Boolean(branches.error)}
                options={branches.data?.map((branch) => ({
                  value: branch.id,
                  label: `${branch.code} – ${branch.name}`,
                }))}
                onChange={(branchId) => list.changeQuery({ ...list.query, branchId, page: 0 })}
              />
            </div>
          )}

          <Button icon={<ReloadOutlined />} onClick={list.reload} style={{ minHeight: 40 }}>
            Tải lại
          </Button>
        </div>

        {dateError && (
          <Alert type="warning" showIcon title={dateError} style={{ marginBottom: 16 }} />
        )}
        {isOwner && Boolean(branches.error) && (
          <Alert
            type="error"
            showIcon
            title={errorMessage(branches.error)}
            action={
              <Button size="small" onClick={branches.reload}>
                Tải lại chi nhánh
              </Button>
            }
            style={{ marginBottom: 16 }}
          />
        )}

        {Boolean(list.error) && (
          <Alert
            type="error"
            showIcon
            title={errorMessage(list.error)}
            action={
              <Button size="small" onClick={list.reload}>
                Thử lại
              </Button>
            }
            style={{ marginBottom: 16 }}
          />
        )}

        <div aria-busy={list.loading}>
          <Table<InvoiceSummary>
            rowKey="id"
            loading={list.loading}
            dataSource={list.data?.content ?? []}
            scroll={{ x: 960 }}
            locale={{
              emptyText: list.error ? 'Chưa tải được dữ liệu.' : 'Không có hóa đơn phù hợp.',
            }}
            columns={[
              ...(isOwner
                ? [
                    {
                      title: 'Chi nhánh',
                      dataIndex: 'branchId',
                      width: 160,
                      render: (id: number) =>
                        branches.data?.find((branch) => branch.id === id)?.code ??
                        `Chi nhánh ${id}`,
                    },
                  ]
                : []),
              {
                title: 'Mã hóa đơn',
                dataIndex: 'code',
                width: 140,
                render: (code: string, row: InvoiceSummary) => (
                  <Link to={`/hoa-don/${row.id}`}>
                    <strong>{code}</strong>
                  </Link>
                ),
              },
              {
                title: 'Khách hàng',
                key: 'customer',
                width: 220,
                render: (_: unknown, row: InvoiceSummary) =>
                  row.memberId === null ? (
                    <Typography.Text type="secondary">Khách lẻ</Typography.Text>
                  ) : (
                    <div>
                      <div>
                        <strong>{row.memberName ?? '—'}</strong>
                      </div>
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {[row.memberCode, row.memberPhone].filter(Boolean).join(' · ') || '—'}
                      </Typography.Text>
                    </div>
                  ),
              },
              {
                title: 'Nguồn',
                dataIndex: 'source',
                width: 130,
                render: (source: InvoiceSource) => INVOICE_SOURCE_LABELS[source] ?? source,
              },
              {
                title: 'Tổng tiền',
                dataIndex: 'total',
                width: 150,
                align: 'right',
                render: (total: number) => <strong>{vndFormat.format(total)}</strong>,
              },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 140,
                render: (status: InvoiceStatus) => {
                  const meta = INVOICE_STATUS_META[status]
                  return <Tag color={meta.color}>{meta.label}</Tag>
                },
              },
              {
                title: 'Thời gian tạo',
                dataIndex: 'createdAt',
                width: 150,
                render: (value: string) => formatVnDateTime(value),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                width: 130,
                fixed: 'right',
                render: (_: unknown, row: InvoiceSummary) => (
                  <Link to={`/hoa-don/${row.id}`}>
                    <Button
                      type={row.status === 'PENDING' ? 'primary' : 'link'}
                      size="small"
                      icon={<EyeOutlined />}
                      style={{ minHeight: 36 }}
                    >
                      {row.status === 'PENDING' && !isOwner ? 'Thu tiền' : 'Chi tiết'}
                    </Button>
                  </Link>
                ),
              },
            ]}
            pagination={{
              current: list.query.page + 1,
              pageSize: list.query.size,
              total: list.data?.totalElements ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `${total} hóa đơn`,
              onChange: (page, size) =>
                list.changeQuery({
                  ...list.query,
                  page: size !== list.query.size ? 0 : page - 1,
                  size,
                }),
            }}
          />
        </div>
      </div>
    </section>
  )
}
