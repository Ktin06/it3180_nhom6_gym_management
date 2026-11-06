import { ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Select, Table, Tag } from 'antd'
import type { Status } from '../../shared/types'
import { errorMessage } from '../../shared/api-error'
import { DemoNotice } from '../../shared/DemoNotice'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { packagesApi } from '../api/packages'
import type { PackageQuery, PackagesGateway, PackageType } from '../types/package'
import '../../shared/feature-pages.css'

const initialQuery: PackageQuery = { page: 0, size: 20, status: 'ACTIVE' }
const typeLabels: Record<PackageType, string> = {
  TIME: 'Theo thời gian',
  SESSION: 'Theo buổi',
  PT: 'Huấn luyện cá nhân',
}
const money = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

export function PackagesPage({ api = packagesApi }: { api?: PackagesGateway }) {
  const list = useRemoteQuery(api.list, initialQuery)
  return (
    <section className="feature-page" aria-labelledby="packages-heading">
      <div className="feature-heading">
        <div>
          <h2 id="packages-heading">Danh mục gói tập</h2>
          <p>Tra cứu loại gói, thời hạn, số buổi và giá bán.</p>
        </div>
      </div>
      <DemoNotice />
      <div className="feature-content">
        <div className="feature-filters">
          <div className="feature-filter">
            <label htmlFor="package-type">Loại gói</label>
            <Select
              id="package-type"
              value={list.query.type ?? 'ALL'}
              options={[
                { value: 'ALL', label: 'Tất cả loại gói' },
                ...Object.entries(typeLabels).map(([value, label]) => ({ value, label })),
              ]}
              onChange={(value) =>
                list.changeQuery({
                  ...list.query,
                  type: value === 'ALL' ? undefined : (value as PackageType),
                  page: 0,
                })
              }
            />
          </div>
          <div className="feature-filter">
            <label htmlFor="package-status">Trạng thái</label>
            <Select<Status>
              id="package-status"
              value={list.query.status ?? 'ACTIVE'}
              options={[
                { value: 'ACTIVE', label: 'Đang bán' },
                { value: 'INACTIVE', label: 'Ngừng bán' },
              ]}
              onChange={(value) =>
                list.changeQuery({
                  ...list.query,
                  status: value,
                  page: 0,
                })
              }
            />
          </div>
          <Button icon={<ReloadOutlined />} onClick={list.reload}>
            Tải lại
          </Button>
        </div>
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
          <Table
            rowKey="id"
            loading={list.loading}
            dataSource={list.data?.content ?? []}
            scroll={{ x: 900 }}
            locale={{
              emptyText: list.error ? 'Chưa tải được dữ liệu.' : 'Không có gói tập phù hợp.',
            }}
            columns={[
              { title: 'Mã gói', dataIndex: 'code', width: 140 },
              { title: 'Tên gói', dataIndex: 'name', width: 200 },
              {
                title: 'Loại gói',
                dataIndex: 'type',
                width: 180,
                render: (type: PackageType) => (
                  <Tag color={type === 'PT' ? 'purple' : type === 'TIME' ? 'blue' : 'cyan'}>
                    {typeLabels[type]}
                  </Tag>
                ),
              },
              {
                title: 'Thời hạn',
                dataIndex: 'durationDays',
                width: 100,
                render: (days: number) => `${days} ngày`,
              },
              {
                title: 'Số buổi',
                dataIndex: 'sessionCount',
                width: 100,
                render: (count: number | null) => count ?? 'Không giới hạn',
              },
              {
                title: 'Giá bán',
                dataIndex: 'price',
                width: 150,
                render: (price: number) => money.format(price),
              },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 120,
                render: (status: string) => (
                  <Tag color={status === 'ACTIVE' ? 'green' : 'default'}>
                    {status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}
                  </Tag>
                ),
              },
            ]}
            expandable={{
              rowExpandable: (item) => Boolean(item.description),
              expandedRowRender: (item) => <p style={{ margin: 0 }}>{item.description}</p>,
              columnTitle: 'Mô tả',
            }}
            pagination={{
              current: list.query.page + 1,
              pageSize: list.query.size,
              total: list.data?.totalElements ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `${total} gói tập`,
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
