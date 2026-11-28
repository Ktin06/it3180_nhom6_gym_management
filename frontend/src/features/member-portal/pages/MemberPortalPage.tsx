import { DownloadOutlined, ReloadOutlined } from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Empty,
  Pagination,
  Progress,
  Spin,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  formatVnDateTime,
  INVOICE_SOURCE_LABELS,
  INVOICE_STATUS_META,
} from '../../invoices/invoice-format'
import { formatVnDate, packageTypeLabels, vndFormat } from '../../packages/package-format'
import type { PublicPackage } from '../../packages/types/package'
import { errorMessage } from '../../../services/api-error'
import type { PageQuery, PageResponse } from '../../../services/list-types'
import { useRemoteQuery } from '../../../services/use-remote-query'
import { memberPortalApi } from '../api/member-portal'
import {
  CHECKIN_METHOD_LABELS,
  formatVnTime,
  MEMBERSHIP_STATUS_META,
} from '../member-portal-format'
import type {
  MemberPortalGateway,
  MyCheckin,
  MyInvoice,
  MyMembership,
  PortalInvoiceResult,
} from '../types/member-portal'
import '../../../components/feature-pages.css'

type PortalTabKey = 'memberships' | 'buy' | 'checkins' | 'invoices'

export interface MemberPortalPageProps {
  api?: MemberPortalGateway
}

/**
 * Cổng hội viên – "Gói của tôi" (F7, S2-15):
 * gồm 4 tab Gói của tôi, Mua gói, Lịch sử check-in, Hóa đơn.
 * Thanh toán tại quầy, không thanh toán online, không tự tính tiền ở trình duyệt.
 */
export function MemberPortalPage({ api = memberPortalApi }: MemberPortalPageProps) {
  const { message } = App.useApp()

  const loadMemberships = useCallback(() => api.listMemberships(), [api])
  const loadPublicPackages = useCallback(() => api.listPublicPackages(), [api])
  const loadCheckins = useCallback((query: PageQuery) => api.listCheckins(query), [api])
  const loadInvoices = useCallback((query: PageQuery) => api.listInvoices(query), [api])

  const membershipsQuery = useRemoteQuery(loadMemberships, undefined)

  const [activeTab, setActiveTab] = useState<PortalTabKey>('memberships')
  const [loadedTabs, setLoadedTabs] = useState<Record<PortalTabKey, boolean>>({
    memberships: true,
    buy: false,
    checkins: false,
    invoices: false,
  })
  const [invoicesReloadKey, setInvoicesReloadKey] = useState(0)

  const [submittingId, setSubmittingId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string>()
  const [portalResult, setPortalResult] = useState<PortalInvoiceResult>()
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<number | null>(null)

  const blobCleanupTimers = useRef<Map<string, number>>(new Map())

  const revokeAllBlobUrls = useCallback(() => {
    for (const [url, timerId] of blobCleanupTimers.current.entries()) {
      window.clearTimeout(timerId)
      URL.revokeObjectURL(url)
    }
    blobCleanupTimers.current.clear()
  }, [])

  useEffect(() => () => revokeAllBlobUrls(), [revokeAllBlobUrls])

  function openTab(key: PortalTabKey) {
    setActiveTab(key)
    setLoadedTabs((prev) => (prev[key] ? prev : { ...prev, [key]: true }))
  }

  async function handleRenew(membership: MyMembership) {
    const key = `renew-${membership.id}`
    setSubmittingId(key)
    setActionError(undefined)
    try {
      const result = await api.requestRenewal(membership.id)
      setPortalResult(result)
      void message?.success?.(`Đã tạo hóa đơn chờ ${result.invoiceCode}.`)
      membershipsQuery.reload()
      setInvoicesReloadKey((k) => k + 1)
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setSubmittingId(null)
    }
  }

  async function handlePurchase(pkg: PublicPackage) {
    const key = `buy-${pkg.id}`
    setSubmittingId(key)
    setActionError(undefined)
    try {
      const result = await api.requestPurchase(pkg.id)
      setPortalResult(result)
      void message?.success?.(`Đã tạo hóa đơn chờ ${result.invoiceCode}.`)
      membershipsQuery.reload()
      setInvoicesReloadKey((k) => k + 1)
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setSubmittingId(null)
    }
  }

  async function handleDownloadPdf(invoice: MyInvoice) {
    setDownloadingInvoiceId(invoice.id)
    setActionError(undefined)
    try {
      const blob = await api.downloadInvoicePdf(invoice.id)
      const url = URL.createObjectURL(blob)
      const timerId = window.setTimeout(() => {
        URL.revokeObjectURL(url)
        blobCleanupTimers.current.delete(url)
      }, 60_000)
      blobCleanupTimers.current.set(url, timerId)

      const opened = window.open(url, '_blank')
      if (opened) {
        opened.opener = null
      } else {
        const link = document.createElement('a')
        link.href = url
        link.download = `hoa-don-${invoice.code}.pdf`
        document.body.appendChild(link)
        link.click()
        link.remove()
      }
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setDownloadingInvoiceId(null)
    }
  }

  const membershipsContent = (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Typography.Text type="secondary">
          Danh sách các gói tập của bạn. Thanh toán được thực hiện tại quầy lễ tân.
        </Typography.Text>
        <Button
          icon={<ReloadOutlined />}
          onClick={membershipsQuery.reload}
          style={{ minHeight: 36 }}
        >
          Tải lại
        </Button>
      </div>

      {Boolean(membershipsQuery.error) && (
        <Alert
          type="error"
          showIcon
          title={errorMessage(membershipsQuery.error)}
          action={
            <Button size="small" onClick={membershipsQuery.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      <div aria-busy={membershipsQuery.loading}>
        {membershipsQuery.loading ? (
          <Spin description="Đang tải danh sách gói tập…" />
        ) : (membershipsQuery.data ?? []).length === 0 ? (
          <Empty description="Bạn chưa có gói tập">
            <Button type="primary" onClick={() => openTab('buy')}>
              Xem bảng giá &amp; Mua gói
            </Button>
          </Empty>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {(membershipsQuery.data ?? []).map((item) => {
              const statusMeta = MEMBERSHIP_STATUS_META[item.status]
              const hasSessions =
                item.packageType !== 'TIME' &&
                item.totalSessions !== null &&
                item.remainingSessions !== null
              const percent =
                hasSessions && item.totalSessions && item.totalSessions > 0
                  ? Math.round(((item.remainingSessions ?? 0) / item.totalSessions) * 100)
                  : 0
              // BR-F7.9: gói Đã hủy không gia hạn được (backend trả 422); hội viên mua lại ở tab Mua gói.
              const canRenew = item.status !== 'PENDING' && item.status !== 'CANCELLED'

              return (
                <Card key={item.id} size="small">
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <div
                      style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 8,
                      }}
                    >
                      <Typography.Text strong style={{ fontSize: 15 }}>
                        {item.packageName}
                      </Typography.Text>
                      <Tag color={statusMeta.color}>{statusMeta.label}</Tag>
                    </div>

                    <Typography.Text type="secondary">
                      Loại: {packageTypeLabels[item.packageType]} ·{' '}
                      {item.branchName ?? 'Toàn chuỗi'}
                    </Typography.Text>

                    <Typography.Text>
                      Hiệu lực: <strong>{formatVnDate(item.startDate)}</strong> –{' '}
                      <strong>{formatVnDate(item.endDate)}</strong>
                    </Typography.Text>

                    {hasSessions && (
                      <div>
                        <Typography.Text strong>
                          Còn {item.remainingSessions}/{item.totalSessions} buổi
                        </Typography.Text>
                        <Progress
                          percent={percent}
                          showInfo={false}
                          size="small"
                          style={{ marginBottom: 0, marginTop: 4 }}
                        />
                      </div>
                    )}

                    {item.status === 'PENDING' && (
                      <div
                        style={{
                          display: 'flex',
                          flexWrap: 'wrap',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          gap: 8,
                        }}
                      >
                        <Typography.Text type="warning">
                          Vui lòng thanh toán tại quầy để kích hoạt gói.
                        </Typography.Text>
                        <Button type="link" size="small" onClick={() => openTab('invoices')}>
                          Xem hóa đơn
                        </Button>
                      </div>
                    )}

                    {canRenew && (
                      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
                        <Button
                          type="default"
                          loading={submittingId === `renew-${item.id}`}
                          disabled={submittingId !== null && submittingId !== `renew-${item.id}`}
                          onClick={() => void handleRenew(item)}
                          style={{ minHeight: 36 }}
                        >
                          Gia hạn
                        </Button>
                      </div>
                    )}
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )

  return (
    <section className="feature-page" aria-labelledby="member-portal-heading">
      <div className="feature-heading">
        <div>
          <h2 id="member-portal-heading">Gói của tôi</h2>
          <p>
            Theo dõi gói tập, gửi yêu cầu mua hoặc gia hạn (thanh toán tại quầy), xem lịch sử
            check-in và hóa đơn.
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {portalResult && (
          <Alert
            type="success"
            showIcon
            closable
            onClose={() => setPortalResult(undefined)}
            title={`Đã tạo hóa đơn chờ ${portalResult.invoiceCode} — Tổng tiền: ${vndFormat.format(portalResult.total)}`}
            description={
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <span>
                  Gói: <strong>{portalResult.packageName}</strong>
                </span>
                <span>
                  Hạn tự hủy (giờ Việt Nam):{' '}
                  <strong>{formatVnDateTime(portalResult.expiresAt)}</strong>
                </span>
                <span>Vui lòng thanh toán tại quầy trước hạn trên để kích hoạt gói.</span>
                <div>
                  <Button size="small" onClick={() => openTab('invoices')}>
                    Xem danh sách hóa đơn
                  </Button>
                </div>
              </div>
            }
          />
        )}

        {actionError && (
          <Alert
            type="error"
            showIcon
            closable
            onClose={() => setActionError(undefined)}
            title={actionError}
          />
        )}

        <Tabs
          activeKey={activeTab}
          onChange={(key) => openTab(key as PortalTabKey)}
          items={[
            {
              key: 'memberships',
              label: 'Gói của tôi',
              children: membershipsContent,
            },
            {
              key: 'buy',
              label: 'Mua gói',
              children: loadedTabs.buy ? (
                <BuyPackageTab
                  load={loadPublicPackages}
                  submittingId={submittingId}
                  onPurchase={(pkg) => void handlePurchase(pkg)}
                />
              ) : null,
            },
            {
              key: 'checkins',
              label: 'Lịch sử check-in',
              children: loadedTabs.checkins ? <CheckinsTab load={loadCheckins} /> : null,
            },
            {
              key: 'invoices',
              label: 'Hóa đơn',
              children: loadedTabs.invoices ? (
                <InvoicesTab
                  key={invoicesReloadKey}
                  load={loadInvoices}
                  downloadingInvoiceId={downloadingInvoiceId}
                  onDownloadPdf={(invoice) => void handleDownloadPdf(invoice)}
                />
              ) : null,
            },
          ]}
        />
      </div>
    </section>
  )
}

interface BuyPackageTabProps {
  load: () => Promise<PublicPackage[]>
  submittingId: string | null
  onPurchase: (pkg: PublicPackage) => void
}

function BuyPackageTab({ load, submittingId, onPurchase }: BuyPackageTabProps) {
  const packagesQuery = useRemoteQuery(load, undefined)
  const items = packagesQuery.data ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Typography.Text type="secondary">
          Chọn gói cần mua để tạo hóa đơn chờ. <strong>Thanh toán tại quầy</strong>, không có thanh
          toán online.
        </Typography.Text>
        <Button icon={<ReloadOutlined />} onClick={packagesQuery.reload} style={{ minHeight: 36 }}>
          Tải lại
        </Button>
      </div>

      {Boolean(packagesQuery.error) && (
        <Alert
          type="error"
          showIcon
          title={errorMessage(packagesQuery.error)}
          action={
            <Button size="small" onClick={packagesQuery.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      <div aria-busy={packagesQuery.loading}>
        {packagesQuery.loading ? (
          <Spin description="Đang tải danh sách gói đang bán…" />
        ) : items.length === 0 ? (
          <Empty description="Chưa có gói đang bán." />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {items.map((pkg) => (
              <Card key={pkg.id} size="small">
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 8,
                    }}
                  >
                    <Typography.Text strong style={{ fontSize: 15 }}>
                      {pkg.name}
                    </Typography.Text>
                    <Tag color="blue">{packageTypeLabels[pkg.type]}</Tag>
                  </div>

                  <Typography.Title level={4} style={{ margin: 0, color: '#1677ff' }}>
                    {vndFormat.format(pkg.price)}
                  </Typography.Title>

                  <Typography.Text>
                    {pkg.type === 'TIME'
                      ? `Thời hạn ${pkg.durationDays} ngày`
                      : `${pkg.sessionCount ?? 0} buổi · Thời hạn ${pkg.durationDays} ngày`}
                  </Typography.Text>

                  {pkg.description && (
                    <Typography.Text type="secondary">{pkg.description}</Typography.Text>
                  )}

                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
                    <Button
                      type="primary"
                      loading={submittingId === `buy-${pkg.id}`}
                      disabled={submittingId !== null && submittingId !== `buy-${pkg.id}`}
                      onClick={() => onPurchase(pkg)}
                      style={{ minHeight: 36 }}
                    >
                      Mua
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

interface CheckinsTabProps {
  load: (query: PageQuery) => Promise<PageResponse<MyCheckin>>
}

function CheckinsTab({ load }: CheckinsTabProps) {
  const initialQuery = useMemo<PageQuery>(() => ({ page: 0, size: 10 }), [])
  const checkinsQuery = useRemoteQuery(load, initialQuery)
  const pageData = checkinsQuery.data
  const items = pageData?.content ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Typography.Text type="secondary">
          Các lượt check-in gần nhất của bạn (giờ Việt Nam).
        </Typography.Text>
        <Button icon={<ReloadOutlined />} onClick={checkinsQuery.reload} style={{ minHeight: 36 }}>
          Tải lại
        </Button>
      </div>

      {Boolean(checkinsQuery.error) && (
        <Alert
          type="error"
          showIcon
          title={errorMessage(checkinsQuery.error)}
          action={
            <Button size="small" onClick={checkinsQuery.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      <div aria-busy={checkinsQuery.loading}>
        {checkinsQuery.loading ? (
          <Spin description="Đang tải lịch sử check-in…" />
        ) : items.length === 0 ? (
          <Empty description="Chưa có lịch sử check-in" />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {items.map((item) => (
              <Card key={item.id} size="small">
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <div
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 8,
                    }}
                  >
                    <Typography.Text strong>{item.branchName}</Typography.Text>
                    <Typography.Text type="secondary">
                      {formatVnDate(item.checkinDate)}
                    </Typography.Text>
                  </div>

                  <Typography.Text>
                    Vào: <strong>{formatVnTime(item.checkedInAt)}</strong> · Ra:{' '}
                    <strong>
                      {item.checkedOutAt ? formatVnTime(item.checkedOutAt) : 'Chưa check-out'}
                    </strong>
                  </Typography.Text>

                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center' }}>
                    <Typography.Text type="secondary">
                      {item.packageName} ({packageTypeLabels[item.packageType]}) ·{' '}
                      {CHECKIN_METHOD_LABELS[item.method] ?? item.method}
                    </Typography.Text>
                    {item.sessionDeducted && <Tag color="orange">Trừ 1 buổi</Tag>}
                  </div>
                </div>
              </Card>
            ))}

            {pageData && pageData.totalElements > pageData.size && (
              <div style={{ display: 'flex', justifyContent: 'center', marginTop: 4 }}>
                <Pagination
                  current={pageData.page + 1}
                  pageSize={pageData.size}
                  total={pageData.totalElements}
                  showSizeChanger={false}
                  onChange={(nextPage, nextSize) =>
                    checkinsQuery.changeQuery({ page: nextPage - 1, size: nextSize })
                  }
                />
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

interface InvoicesTabProps {
  load: (query: PageQuery) => Promise<PageResponse<MyInvoice>>
  downloadingInvoiceId: number | null
  onDownloadPdf: (invoice: MyInvoice) => void
}

function InvoicesTab({ load, downloadingInvoiceId, onDownloadPdf }: InvoicesTabProps) {
  const initialQuery = useMemo<PageQuery>(() => ({ page: 0, size: 10 }), [])
  const invoicesQuery = useRemoteQuery(load, initialQuery)
  const pageData = invoicesQuery.data
  const items = pageData?.content ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Typography.Text type="secondary">
          Danh sách hóa đơn của bạn (giờ Việt Nam). Hóa đơn chờ tự hủy sau thời hạn nếu chưa thanh
          toán tại quầy.
        </Typography.Text>
        <Button icon={<ReloadOutlined />} onClick={invoicesQuery.reload} style={{ minHeight: 36 }}>
          Tải lại
        </Button>
      </div>

      {Boolean(invoicesQuery.error) && (
        <Alert
          type="error"
          showIcon
          title={errorMessage(invoicesQuery.error)}
          action={
            <Button size="small" onClick={invoicesQuery.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      <div aria-busy={invoicesQuery.loading}>
        {invoicesQuery.loading ? (
          <Spin description="Đang tải danh sách hóa đơn…" />
        ) : items.length === 0 ? (
          <Empty description="Bạn chưa có hóa đơn" />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {items.map((invoice) => {
              const statusMeta = INVOICE_STATUS_META[invoice.status]
              const canDownloadPdf = invoice.status === 'PAID' || invoice.status === 'REFUNDED'

              return (
                <Card key={invoice.id} size="small">
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <div
                      style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 8,
                      }}
                    >
                      <Typography.Text strong style={{ fontSize: 15 }}>
                        {invoice.code}
                      </Typography.Text>
                      <Tag color={statusMeta.color}>{statusMeta.label}</Tag>
                    </div>

                    <Typography.Text strong style={{ fontSize: 16, color: '#1677ff' }}>
                      {vndFormat.format(invoice.total)}
                    </Typography.Text>

                    <Typography.Text type="secondary">
                      {invoice.branchName} ·{' '}
                      {INVOICE_SOURCE_LABELS[invoice.source] ?? invoice.source}
                    </Typography.Text>

                    <Typography.Text type="secondary">
                      Tạo lúc: {formatVnDateTime(invoice.createdAt)}
                      {invoice.paidAt ? ` · Thanh toán: ${formatVnDateTime(invoice.paidAt)}` : ''}
                    </Typography.Text>

                    {invoice.items.length > 0 && (
                      <ul style={{ margin: '4px 0', paddingLeft: 18 }}>
                        {invoice.items.map((line) => (
                          <li key={line.id}>
                            {line.description} × {line.quantity} — {vndFormat.format(line.amount)}
                          </li>
                        ))}
                      </ul>
                    )}

                    {invoice.status === 'PENDING' && invoice.expiresAt && (
                      <Alert
                        type="warning"
                        showIcon
                        title={`Hạn tự hủy: ${formatVnDateTime(invoice.expiresAt)}`}
                        description="Vui lòng thanh toán tại quầy trước thời điểm này."
                      />
                    )}

                    {canDownloadPdf && (
                      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
                        <Button
                          icon={<DownloadOutlined />}
                          loading={downloadingInvoiceId === invoice.id}
                          onClick={() => onDownloadPdf(invoice)}
                          style={{ minHeight: 36 }}
                        >
                          Tải PDF
                        </Button>
                      </div>
                    )}
                  </div>
                </Card>
              )
            })}

            {pageData && pageData.totalElements > pageData.size && (
              <div style={{ display: 'flex', justifyContent: 'center', marginTop: 4 }}>
                <Pagination
                  current={pageData.page + 1}
                  pageSize={pageData.size}
                  total={pageData.totalElements}
                  showSizeChanger={false}
                  onChange={(nextPage, nextSize) =>
                    invoicesQuery.changeQuery({ page: nextPage - 1, size: nextSize })
                  }
                />
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
