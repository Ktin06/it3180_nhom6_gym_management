import { EditOutlined, PlusOutlined, ReloadOutlined, StopOutlined } from '@ant-design/icons'
import { Alert, App, Button, Modal, Select, Space, Table, Tag, Typography } from 'antd'
import { useContext, useState } from 'react'
import { AuthContext } from '../../../auth/AuthContext'
import type { Status } from '../../shared/types'
import { errorMessage } from '../../shared/api-error'
import { DemoNotice } from '../../shared/DemoNotice'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { packagesApi } from '../api/packages'
import { PackageFormModal } from '../components/PackageFormModal'
import { packageTypeLabels as typeLabels, vndFormat as money } from '../package-format'
import type { GymPackage, PackageQuery, PackagesGateway, PackageType } from '../types/package'
import '../../shared/feature-pages.css'

const initialQuery: PackageQuery = { page: 0, size: 20, status: 'ACTIVE' }

export function PackagesPage({ api = packagesApi }: { api?: PackagesGateway }) {
  const { message } = App.useApp()
  const auth = useContext(AuthContext)
  const isManager = auth?.user?.role === 'BRANCH_MANAGER'
  const list = useRemoteQuery(api.list, initialQuery)
  const [editing, setEditing] = useState<GymPackage | null>()
  const [deactivating, setDeactivating] = useState<GymPackage>()
  const [deactivatingPending, setDeactivatingPending] = useState(false)
  const [actionError, setActionError] = useState<string>()

  function openCreate() {
    setActionError(undefined)
    setEditing(null)
  }

  function openEdit(item: GymPackage) {
    setActionError(undefined)
    setEditing(item)
  }

  function openDeactivate(item: GymPackage) {
    setActionError(undefined)
    setDeactivating(item)
  }

  async function confirmDeactivate(target: GymPackage) {
    setDeactivatingPending(true)
    setActionError(undefined)
    try {
      await api.deactivate(target.id)
      setDeactivating(undefined)
      list.reload()
      void message?.success?.(`Đã ngừng bán gói ${target.code}.`)
    } catch (cause) {
      setDeactivating(undefined)
      setActionError(errorMessage(cause))
    } finally {
      setDeactivatingPending(false)
    }
  }

  return (
    <section className="feature-page" aria-labelledby="packages-heading">
      <div className="feature-heading">
        <div>
          <h2 id="packages-heading">Danh mục gói tập</h2>
          <p>Tra cứu loại gói, thời hạn, số buổi và giá bán.</p>
        </div>
        {isManager && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openCreate}
            style={{ minHeight: 40 }}
          >
            Thêm gói
          </Button>
        )}
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
        {actionError && (
          <Alert
            type="error"
            showIcon
            title={actionError}
            action={
              <Button size="small" onClick={() => setActionError(undefined)}>
                Đóng
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
            scroll={{ x: isManager ? 1090 : 900 }}
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
              ...(isManager
                ? [
                    {
                      title: 'Thao tác',
                      key: 'actions',
                      width: 200,
                      fixed: 'right' as const,
                      render: (_: unknown, item: GymPackage) =>
                        item.branchId === null ? (
                          <Typography.Text type="secondary">Toàn chuỗi</Typography.Text>
                        ) : (
                          <Space size={0}>
                            <Button
                              type="link"
                              icon={<EditOutlined />}
                              aria-label={`Sửa ${item.code}`}
                              style={{ minHeight: 40 }}
                              onClick={() => openEdit(item)}
                            >
                              Sửa
                            </Button>
                            {item.status === 'ACTIVE' && (
                              <Button
                                type="link"
                                danger
                                icon={<StopOutlined />}
                                aria-label={`Ngừng bán ${item.code}`}
                                style={{ minHeight: 40 }}
                                onClick={() => openDeactivate(item)}
                              >
                                Ngừng bán
                              </Button>
                            )}
                          </Space>
                        ),
                    },
                  ]
                : []),
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
      {editing !== undefined && (
        <PackageFormModal
          key={editing?.id ?? 'new'}
          pkg={editing}
          api={api}
          onClose={() => setEditing(undefined)}
          onSaved={() => {
            const wasEditing = Boolean(editing)
            setEditing(undefined)
            if (wasEditing) {
              list.reload()
            } else {
              list.changeQuery({ ...list.query, status: 'ACTIVE', page: 0 })
            }
            void message?.success?.(wasEditing ? 'Đã cập nhật gói tập.' : 'Đã thêm gói tập mới.')
          }}
        />
      )}
      {deactivating && (
        <Modal
          open
          title="Ngừng bán gói này?"
          onCancel={deactivatingPending ? undefined : () => setDeactivating(undefined)}
          closable={!deactivatingPending}
          mask={{ closable: !deactivatingPending }}
          footer={
            <div className="feature-form-actions" style={{ marginTop: 0 }}>
              <Button
                disabled={deactivatingPending}
                onClick={() => setDeactivating(undefined)}
                style={{ minHeight: 40 }}
              >
                Hủy
              </Button>
              <Button
                type="primary"
                danger
                loading={deactivatingPending}
                onClick={() => void confirmDeactivate(deactivating)}
                style={{ minHeight: 40 }}
              >
                Ngừng bán
              </Button>
            </div>
          }
        >
          <p style={{ margin: 0 }}>
            Gói <strong>{deactivating.name}</strong> ({deactivating.code}) sẽ chuyển sang trạng thái
            ngừng bán. Các gói hội viên đã mua trước đó vẫn sử dụng bình thường.
          </p>
        </Modal>
      )}
    </section>
  )
}
