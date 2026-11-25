import { CheckOutlined, ReloadOutlined, ShoppingCartOutlined } from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Empty,
  Input,
  Select,
  Spin,
  Tag,
  Typography,
} from 'antd'
import { useEffect, useRef, useState } from 'react'
import { membersApi } from '../../members/api/members'
import type { MemberQuery, MembersGateway, MemberSummary } from '../../members/types/member'
import { errorMessage } from '../../shared/api-error'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { packagesApi } from '../api/packages'
import { packageSalesApi } from '../api/package-sales'
import { formatVnDate, packageTypeLabels, vndFormat } from '../package-format'
import type { GymPackage, PackageQuery, PackagesGateway, PackageType } from '../types/package'
import type {
  CreatePackageInvoiceInput,
  HeldMembership,
  InvoiceStatus,
  MembershipStatus,
  PackageInvoice,
  PackageSalesGateway,
} from '../types/sale'
import '../../shared/feature-pages.css'

const initialMemberQuery: MemberQuery = { page: 0, size: 10, status: 'ACTIVE' }
const initialPackageQuery: PackageQuery = { page: 0, size: 50, status: 'ACTIVE' }

const MEMBERSHIP_STATUS_LABELS: Record<MembershipStatus, { label: string; color: string }> = {
  ACTIVE: { label: 'Đang hiệu lực', color: 'green' },
  FROZEN: { label: 'Đang bảo lưu', color: 'cyan' },
  UPCOMING: { label: 'Chờ kích hoạt', color: 'blue' },
  PENDING: { label: 'Chờ thanh toán', color: 'gold' },
}

const INVOICE_STATUS_LABELS: Record<InvoiceStatus, { label: string; color: string }> = {
  PENDING: { label: 'Chờ thanh toán', color: 'gold' },
  PAID: { label: 'Đã thanh toán', color: 'green' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
  REFUNDED: { label: 'Đã hoàn tiền', color: 'red' },
}

type SaleMode =
  | { kind: 'waiting'; membership: HeldMembership }
  | { kind: 'renewal'; membership: HeldMembership; renewalStartDate: string }
  | { kind: 'new' }

function resolveSaleMode(memberships: HeldMembership[], pkg: GymPackage): SaleMode {
  // Gói toàn chuỗi (branchId = null) và gói chi nhánh là hai phạm vi khác nhau (BR-F3-10).
  const sameScope = memberships.filter(
    (item) => item.packageType === pkg.type && item.branchId === pkg.branchId,
  )
  const waiting = sameScope.find((item) => item.status === 'PENDING' || item.status === 'UPCOMING')
  if (waiting) return { kind: 'waiting', membership: waiting }

  const activeOrFrozen = sameScope.find(
    (item) =>
      (item.status === 'ACTIVE' || item.status === 'FROZEN') && item.renewalStartDate !== null,
  )
  if (activeOrFrozen?.renewalStartDate) {
    return {
      kind: 'renewal',
      membership: activeOrFrozen,
      renewalStartDate: activeOrFrozen.renewalStartDate,
    }
  }
  return { kind: 'new' }
}

export interface SellPackagePageProps {
  members?: Pick<MembersGateway, 'list'>
  packages?: Pick<PackagesGateway, 'list'>
  sales?: PackageSalesGateway
}

/** Màn hình lễ tân bán mới hoặc gia hạn gói tại quầy (UC-F3.5, S2-08.1). */
export function SellPackagePage({
  members = membersApi,
  packages = packagesApi,
  sales = packageSalesApi,
}: SellPackagePageProps) {
  const { message } = App.useApp()
  const memberList = useRemoteQuery(members.list, initialMemberQuery)
  const packageList = useRemoteQuery(packages.list, initialPackageQuery)
  const [memberKeyword, setMemberKeyword] = useState('')
  const [selectedMember, setSelectedMember] = useState<MemberSummary>()
  const [selectedPackage, setSelectedPackage] = useState<GymPackage>()
  const [memberships, setMemberships] = useState<HeldMembership[]>([])
  const [loadingMemberships, setLoadingMemberships] = useState(false)
  const [membershipsError, setMembershipsError] = useState<unknown>()
  const [creating, setCreating] = useState(false)
  const [invoiceError, setInvoiceError] = useState<unknown>()
  const [createdInvoice, setCreatedInvoice] = useState<PackageInvoice>()
  const membershipRequest = useRef(0)

  useEffect(
    () => () => {
      membershipRequest.current += 1
    },
    [],
  )

  async function loadMemberships(memberId: number) {
    const requestId = ++membershipRequest.current
    setLoadingMemberships(true)
    setMembershipsError(undefined)
    try {
      const rows = await sales.listMemberships(memberId)
      if (requestId === membershipRequest.current) {
        setMemberships(rows)
      }
    } catch (cause) {
      if (requestId === membershipRequest.current) {
        setMemberships([])
        setMembershipsError(cause)
      }
    } finally {
      if (requestId === membershipRequest.current) {
        setLoadingMemberships(false)
      }
    }
  }

  function selectMember(member: MemberSummary) {
    setSelectedMember(member)
    setCreatedInvoice(undefined)
    setInvoiceError(undefined)
    setMemberships([])
    void loadMemberships(member.id)
  }

  function selectPackage(pkg: GymPackage) {
    setSelectedPackage(pkg)
    setCreatedInvoice(undefined)
    setInvoiceError(undefined)
  }

  function searchMembers(value: string) {
    const normalized = value.trim() || undefined
    memberList.changeQuery({
      ...memberList.query,
      keyword: normalized,
      page: 0,
    })
  }

  async function submitInvoice() {
    if (!selectedMember || !selectedPackage || creating || !saleMode || saleMode.kind === 'waiting')
      return
    setCreating(true)
    setInvoiceError(undefined)
    const payload: CreatePackageInvoiceInput = {
      memberId: selectedMember.id,
      items: [{ itemType: 'PACKAGE', packageId: selectedPackage.id }],
    }
    try {
      const invoice = await sales.createInvoice(payload)
      setCreatedInvoice(invoice)
      void message.success(`Đã tạo hóa đơn ${invoice.invoiceCode}.`)
      void loadMemberships(selectedMember.id)
    } catch (cause) {
      setInvoiceError(cause)
    } finally {
      setCreating(false)
    }
  }

  const saleMode =
    selectedMember && selectedPackage && !loadingMemberships && !membershipsError
      ? resolveSaleMode(memberships, selectedPackage)
      : null

  return (
    <section className="feature-page" aria-labelledby="sell-package-heading">
      <div className="feature-heading">
        <div>
          <h2 id="sell-package-heading">Bán và gia hạn gói tập</h2>
          <p>Chọn hội viên, kiểm tra gói đang giữ và tạo hóa đơn chờ thanh toán tại quầy.</p>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <Card title="1. Tìm và chọn hội viên">
          <div className="feature-filters">
            <div className="feature-filter feature-search">
              <label htmlFor="sell-member-search">Tìm hội viên</label>
              <Input.Search
                id="sell-member-search"
                placeholder="Mã hội viên, số điện thoại hoặc đầu họ tên"
                allowClear
                value={memberKeyword}
                onChange={(event) => setMemberKeyword(event.target.value)}
                onSearch={searchMembers}
                enterButton="Tìm"
              />
            </div>
            <Button icon={<ReloadOutlined />} onClick={memberList.reload} style={{ minHeight: 40 }}>
              Tải lại
            </Button>
          </div>
          {Boolean(memberList.error) && (
            <Alert
              type="error"
              showIcon
              title={errorMessage(memberList.error)}
              action={
                <Button size="small" onClick={memberList.reload}>
                  Thử lại
                </Button>
              }
              style={{ marginBottom: 16 }}
            />
          )}
          <div aria-busy={memberList.loading}>
            {memberList.loading ? (
              <Spin description="Đang tải hội viên…" />
            ) : (memberList.data?.content ?? []).length === 0 ? (
              <Empty description="Không tìm thấy hội viên phù hợp." />
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {(memberList.data?.content ?? []).map((member) => {
                  const isSelected = selectedMember?.id === member.id
                  return (
                    <div
                      key={member.id}
                      style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 12,
                        padding: '10px 12px',
                        borderRadius: 8,
                        border: isSelected ? '1px solid #1677ff' : '1px solid #e8ecf2',
                        background: isSelected ? '#f0f7ff' : '#fff',
                      }}
                    >
                      <div>
                        <strong>{member.memberCode}</strong> · {member.fullName} ·{' '}
                        {member.phone}{' '}
                      </div>
                      <Button
                        type={isSelected ? 'primary' : 'default'}
                        icon={isSelected ? <CheckOutlined /> : undefined}
                        aria-label={`Chọn ${member.memberCode}`}
                        onClick={() => selectMember(member)}
                        disabled={creating}
                        style={{ minHeight: 40 }}
                      >
                        {isSelected ? 'Đang chọn' : 'Chọn'}
                      </Button>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </Card>

        <Card title="2. Gói tập hiện có của hội viên">
          {!selectedMember ? (
            <Typography.Text type="secondary">
              Chọn một hội viên ở bước 1 để xem các gói đang giữ hoặc chờ xử lý.
            </Typography.Text>
          ) : loadingMemberships ? (
            <Spin description="Đang tải gói hiện có…" />
          ) : membershipsError !== undefined ? (
            <Alert
              type="error"
              showIcon
              title={errorMessage(membershipsError)}
              action={
                <Button size="small" onClick={() => void loadMemberships(selectedMember.id)}>
                  Thử lại
                </Button>
              }
            />
          ) : memberships.length === 0 ? (
            <Typography.Text type="secondary">
              Hội viên chưa có gói nào đang hiệu lực hoặc chờ xử lý.
            </Typography.Text>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {memberships.map((item) => {
                const statusInfo = MEMBERSHIP_STATUS_LABELS[item.status]
                return (
                  <div
                    key={item.id}
                    style={{
                      padding: '10px 12px',
                      borderRadius: 8,
                      border: '1px solid #e8ecf2',
                      display: 'flex',
                      flexWrap: 'wrap',
                      justifyContent: 'space-between',
                      gap: 8,
                    }}
                  >
                    <div>
                      <div>
                        <strong>{item.packageName}</strong>{' '}
                        <Tag>{packageTypeLabels[item.packageType]}</Tag>
                        <Tag>{item.branchId === null ? 'Toàn chuỗi' : 'Chi nhánh'}</Tag>
                        <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
                      </div>
                      <Typography.Text type="secondary">
                        Thời hạn: {formatVnDate(item.startDate)} – {formatVnDate(item.endDate)}
                        {item.remainingSessions !== null &&
                          ` · Còn ${item.remainingSessions}/${item.totalSessions ?? item.remainingSessions} buổi`}
                        {item.renewalStartDate &&
                          ` · Gia hạn nối tiếp từ ${formatVnDate(item.renewalStartDate)}`}
                      </Typography.Text>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </Card>

        <Card title="3. Chọn gói tập đang bán">
          <div className="feature-filters">
            <div className="feature-filter">
              <label htmlFor="sell-package-type">Loại gói</label>
              <Select
                id="sell-package-type"
                value={packageList.query.type ?? 'ALL'}
                options={[
                  { value: 'ALL', label: 'Tất cả loại gói' },
                  ...Object.entries(packageTypeLabels).map(([value, label]) => ({ value, label })),
                ]}
                onChange={(value) =>
                  packageList.changeQuery({
                    ...packageList.query,
                    type: value === 'ALL' ? undefined : (value as PackageType),
                    page: 0,
                  })
                }
              />
            </div>
            <Button
              icon={<ReloadOutlined />}
              onClick={packageList.reload}
              style={{ minHeight: 40 }}
            >
              Tải lại
            </Button>
          </div>
          {Boolean(packageList.error) && (
            <Alert
              type="error"
              showIcon
              title={errorMessage(packageList.error)}
              action={
                <Button size="small" onClick={packageList.reload}>
                  Thử lại
                </Button>
              }
              style={{ marginBottom: 16 }}
            />
          )}
          <div aria-busy={packageList.loading}>
            {packageList.loading ? (
              <Spin description="Đang tải danh mục gói…" />
            ) : (packageList.data?.content ?? []).length === 0 ? (
              <Empty description="Chưa có gói đang bán." />
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {(packageList.data?.content ?? []).map((pkg) => {
                  const isSelected = selectedPackage?.id === pkg.id
                  return (
                    <div
                      key={pkg.id}
                      style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 12,
                        padding: '10px 12px',
                        borderRadius: 8,
                        border: isSelected ? '1px solid #1677ff' : '1px solid #e8ecf2',
                        background: isSelected ? '#f0f7ff' : '#fff',
                      }}
                    >
                      <div>
                        <div>
                          <strong>{pkg.name}</strong> ({pkg.code}){' '}
                          <Tag>{packageTypeLabels[pkg.type]}</Tag>
                          <Tag>{pkg.branchId === null ? 'Toàn chuỗi' : 'Chi nhánh'}</Tag>
                        </div>
                        <Typography.Text type="secondary">
                          {pkg.durationDays} ngày
                          {pkg.sessionCount !== null ? ` · ${pkg.sessionCount} buổi` : ''} ·{' '}
                          <strong>{vndFormat.format(pkg.price)}</strong>
                        </Typography.Text>
                      </div>
                      <Button
                        type={isSelected ? 'primary' : 'default'}
                        icon={isSelected ? <CheckOutlined /> : undefined}
                        aria-label={`Chọn gói ${pkg.code}`}
                        onClick={() => selectPackage(pkg)}
                        disabled={creating}
                        style={{ minHeight: 40 }}
                      >
                        {isSelected ? 'Đã chọn' : 'Chọn gói'}
                      </Button>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </Card>

        <Card title="4. Kiểm tra và lập hóa đơn">
          {!selectedMember || !selectedPackage ? (
            <Typography.Text type="secondary">
              Chọn hội viên và gói tập để xem tóm tắt và lập hóa đơn chờ thanh toán.
            </Typography.Text>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <Descriptions column={1} bordered size="small">
                <Descriptions.Item label="Hội viên">
                  {selectedMember.memberCode} · {selectedMember.fullName}
                </Descriptions.Item>
                <Descriptions.Item label="Gói tập">
                  {selectedPackage.name} ({selectedPackage.code}) ·{' '}
                  {packageTypeLabels[selectedPackage.type]}
                </Descriptions.Item>
                <Descriptions.Item label="Giá niêm yết">
                  {vndFormat.format(selectedPackage.price)}
                </Descriptions.Item>
                {saleMode && (
                  <Descriptions.Item label="Hình thức">
                    {saleMode.kind === 'renewal' ? (
                      <>
                        <Tag color="blue">Gia hạn nối tiếp</Tag>{' '}
                        <span>Ngày bắt đầu dự kiến: {formatVnDate(saleMode.renewalStartDate)}</span>
                      </>
                    ) : saleMode.kind === 'waiting' ? (
                      <>
                        <Tag color="orange">Đang có gói chờ cùng loại</Tag>{' '}
                        <span>
                          Gói {saleMode.membership.packageName} đang ở trạng thái{' '}
                          {MEMBERSHIP_STATUS_LABELS[saleMode.membership.status].label}.
                        </span>
                      </>
                    ) : (
                      <Tag color="green">Mua mới</Tag>
                    )}
                  </Descriptions.Item>
                )}
              </Descriptions>

              {Boolean(invoiceError) && (
                <Alert type="error" showIcon title={errorMessage(invoiceError)} />
              )}

              <div>
                <Button
                  type="primary"
                  icon={<ShoppingCartOutlined />}
                  loading={creating}
                  disabled={!saleMode || saleMode.kind === 'waiting'}
                  onClick={() => void submitInvoice()}
                  style={{ minHeight: 40 }}
                >
                  Tạo hóa đơn
                </Button>
              </div>
            </div>
          )}
        </Card>

        {createdInvoice && (
          <Card title="Hóa đơn vừa tạo">
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Mã hóa đơn">
                <strong>{createdInvoice.invoiceCode}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={INVOICE_STATUS_LABELS[createdInvoice.status].color}>
                  {INVOICE_STATUS_LABELS[createdInvoice.status].label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Gói tập">{createdInvoice.packageName}</Descriptions.Item>
              <Descriptions.Item label="Tổng tiền">
                <strong>{vndFormat.format(createdInvoice.total)}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Ngày bắt đầu dự kiến">
                {formatVnDate(createdInvoice.startDate)}
              </Descriptions.Item>
              <Descriptions.Item label="Ngày kết thúc dự kiến">
                {formatVnDate(createdInvoice.endDate)}
              </Descriptions.Item>
            </Descriptions>
            <Typography.Paragraph type="secondary" style={{ margin: '12px 0 0' }}>
              Vui lòng thu tiền tại quầy và xác nhận thanh toán ở màn hình Hóa đơn (F5) để kích hoạt
              gói.
            </Typography.Paragraph>
          </Card>
        )}
      </div>
    </section>
  )
}
