import { EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, App, Button, Input, Select, Space, Table, Tag } from 'antd'
import { useEffect, useRef, useState } from 'react'
import type { Status } from '../../shared/types'
import { errorMessage } from '../../shared/api-error'
import { DemoNotice } from '../../shared/DemoNotice'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { membersApi } from '../api/members'
import { MemberFormModal } from '../components/MemberFormModal'
import type { MemberDetail, MemberQuery, MembersGateway } from '../types/member'
import '../../shared/feature-pages.css'

const initialQuery: MemberQuery = { page: 0, size: 20 }

export function MembersPage({ api = membersApi }: { api?: MembersGateway }) {
  const { message } = App.useApp()
  const list = useRemoteQuery(api.list, initialQuery)
  const [keyword, setKeyword] = useState('')
  const [editing, setEditing] = useState<MemberDetail | null>()
  const [loadingId, setLoadingId] = useState<number>()
  const [editError, setEditError] = useState<{ id: number; message: string }>()
  const editRequest = useRef(0)

  useEffect(
    () => () => {
      editRequest.current += 1
    },
    [],
  )

  function closeEditor() {
    editRequest.current += 1
    setLoadingId(undefined)
    setEditing(undefined)
  }

  function addMember() {
    editRequest.current += 1
    setLoadingId(undefined)
    setEditError(undefined)
    setEditing(null)
  }

  async function edit(id: number) {
    const request = ++editRequest.current
    setLoadingId(id)
    setEditError(undefined)
    try {
      const member = await api.detail(id)
      if (request === editRequest.current) setEditing(member)
    } catch (cause) {
      if (request === editRequest.current) setEditError({ id, message: errorMessage(cause) })
    } finally {
      if (request === editRequest.current) setLoadingId(undefined)
    }
  }

  return (
    <section className="feature-page" aria-labelledby="members-heading">
      <div className="feature-heading">
        <div>
          <h2 id="members-heading">Hồ sơ hội viên</h2>
          <p>Tra cứu và quản lý thông tin hội viên.</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={addMember}>
          Thêm hội viên
        </Button>
      </div>
      <DemoNotice />
      <div className="feature-content">
        <div className="feature-filters">
          <div className="feature-filter feature-search">
            <label htmlFor="member-search">Tìm hội viên</label>
            <Input.Search
              id="member-search"
              placeholder="Mã hội viên, số điện thoại hoặc đầu họ tên"
              allowClear
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onSearch={(value) =>
                list.changeQuery({ ...list.query, keyword: value.trim() || undefined, page: 0 })
              }
              enterButton="Tìm"
            />
          </div>
          <div className="feature-filter">
            <label htmlFor="member-status">Trạng thái</label>
            <Select<Status | 'ALL'>
              id="member-status"
              value={list.query.status ?? 'ALL'}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Đang hoạt động' },
                { value: 'INACTIVE', label: 'Ngừng hoạt động' },
              ]}
              onChange={(value) =>
                list.changeQuery({
                  ...list.query,
                  status: value === 'ALL' ? undefined : value,
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
            style={{ marginBottom: 16 }}
            action={
              <Button size="small" onClick={list.reload}>
                Thử lại
              </Button>
            }
          />
        )}
        {editError && (
          <Alert
            type="error"
            showIcon
            title={editError.message}
            style={{ marginBottom: 16 }}
            action={
              <Button size="small" onClick={() => void edit(editError.id)}>
                Thử lại
              </Button>
            }
          />
        )}
        <div aria-busy={list.loading}>
          <Table
            rowKey="id"
            loading={list.loading}
            dataSource={list.data?.content ?? []}
            locale={{
              emptyText: list.error ? 'Chưa tải được dữ liệu.' : 'Không có hội viên phù hợp.',
            }}
            scroll={{ x: 850 }}
            columns={[
              { title: 'Mã hội viên', dataIndex: 'memberCode', width: 130 },
              { title: 'Họ và tên', dataIndex: 'fullName', width: 200 },
              { title: 'Số điện thoại', dataIndex: 'phone', width: 140 },
              {
                title: 'Email',
                dataIndex: 'email',
                width: 200,
                render: (value: string | null) => value ?? 'Chưa có tài khoản',
              },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 170,
                render: (status: string) => (
                  <Tag color={status === 'ACTIVE' ? 'green' : 'default'}>
                    {status === 'ACTIVE' ? 'Đang hoạt động' : 'Ngừng hoạt động'}
                  </Tag>
                ),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                width: 90,
                fixed: 'right',
                render: (_, member) => (
                  <Space>
                    <Button
                      type="link"
                      icon={<EditOutlined />}
                      aria-label={`Sửa ${member.memberCode}`}
                      disabled={loadingId !== undefined}
                      loading={loadingId === member.id}
                      onClick={() => void edit(member.id)}
                    >
                      Sửa
                    </Button>
                  </Space>
                ),
              },
            ]}
            pagination={{
              current: list.query.page + 1,
              pageSize: list.query.size,
              total: list.data?.totalElements ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50, 100],
              showTotal: (total) => `${total} hội viên`,
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
        <MemberFormModal
          key={editing?.id ?? 'new'}
          member={editing}
          api={api}
          onClose={closeEditor}
          onSaved={() => {
            closeEditor()
            list.changeQuery({ ...list.query, page: 0 })
            void message.success('Đã lưu hồ sơ hội viên.')
          }}
        />
      )}
    </section>
  )
}
