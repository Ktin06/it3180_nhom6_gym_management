import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  PrinterOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Input,
  Modal,
  Radio,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import axios from 'axios'
import { useCallback, useContext, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AuthContext } from '../../../auth/AuthContext'
import { errorMessage } from '../../shared/api-error'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { invoicesApi } from '../api/invoices'
import { ActivatedMemberships } from '../components/ActivatedMemberships'
import { invoiceFieldErrors } from '../invoice-errors'
import { useInvoicePdf } from '../use-invoice-pdf'
import {
  formatVnDateTime,
  INVOICE_SOURCE_LABELS,
  INVOICE_STATUS_META,
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_META,
  vndFormat,
  parseInvoiceId,
} from '../invoice-format'
import type {
  InvoiceItem,
  InvoicePayment,
  InvoicesGateway,
  PaymentConfirmation,
  PaymentMethod,
  VietQrPayment,
} from '../types/invoice'
import '../../shared/feature-pages.css'

export interface InvoiceDetailPageProps {
  api?: InvoicesGateway
}

/** Màn hình chi tiết hóa đơn, thu tiền tại quầy (tiền mặt / VietQR), hủy hóa đơn chờ và in PDF (S2-10.1). */
export function InvoiceDetailPage(props: InvoiceDetailPageProps) {
  const { id } = useParams()
  return <InvoiceDetailContent key={id} {...props} />
}

function InvoiceDetailContent({ api = invoicesApi }: InvoiceDetailPageProps) {
  const { id } = useParams()
  const invoiceId = parseInvoiceId(id) ?? 0
  const validId = invoiceId > 0
  const { message } = App.useApp()
  const auth = useContext(AuthContext)
  const role = auth?.user?.role

  // Chủ chuỗi chỉ tra cứu và hủy hóa đơn chờ; thu tiền, VietQR và in PDF dành cho lễ tân / quản lý chi nhánh.
  const canCollectPayment = role === 'RECEPTIONIST' || role === 'BRANCH_MANAGER'
  const canExportPdf = canCollectPayment
  const canCancel = canCollectPayment || role === 'SUPER_ADMIN'

  const fetchDetail = useCallback(
    (targetId: number) => (targetId > 0 ? api.detail(targetId) : Promise.resolve(undefined)),
    [api],
  )
  const detailQuery = useRemoteQuery(fetchDetail, validId ? invoiceId : 0)
  const invoice = validId ? detailQuery.data : undefined
  const loading = validId ? detailQuery.loading : false
  const loadError = validId ? detailQuery.error : undefined

  const [method, setMethod] = useState<PaymentMethod>('CASH')
  const [transactionRef, setTransactionRef] = useState('')
  const [vietQr, setVietQr] = useState<VietQrPayment>()
  const [loadingQr, setLoadingQr] = useState(false)
  const [qrError, setQrError] = useState<string>()
  const [qrImageFailed, setQrImageFailed] = useState(false)
  const [transactionRefError, setTransactionRefError] = useState<string>()

  const [paying, setPaying] = useState(false)
  const [actionError, setActionError] = useState<string>()
  const [paymentResult, setPaymentResult] = useState<PaymentConfirmation>()

  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const pdf = useInvoicePdf(api.pdf)
  const active = useRef(false)
  const mutationInFlight = useRef(false)
  const qrRequest = useRef(0)
  useEffect(() => {
    active.current = true
    return () => {
      active.current = false
    }
  }, [])

  const loadDetail = useCallback(() => {
    if (!validId) return
    detailQuery.reload()
  }, [detailQuery, validId])

  async function loadVietQr() {
    if (!validId || !canCollectPayment || invoice?.status !== 'PENDING') return
    const request = ++qrRequest.current
    setLoadingQr(true)
    setQrError(undefined)
    setVietQr(undefined)
    setQrImageFailed(false)
    try {
      const data = await api.vietqr(invoiceId)
      if (active.current && request === qrRequest.current) setVietQr(data)
    } catch (cause) {
      if (!active.current || request !== qrRequest.current) return
      setVietQr(undefined)
      setQrError(errorMessage(cause))
      if (axios.isAxiosError(cause) && cause.response?.status === 422) loadDetail()
    } finally {
      if (active.current && request === qrRequest.current) setLoadingQr(false)
    }
  }

  function handleMethodChange(nextMethod: PaymentMethod) {
    setMethod(nextMethod)
    setActionError(undefined)
    setTransactionRefError(undefined)
    if (nextMethod === 'BANK_TRANSFER' && invoice?.status === 'PENDING' && canCollectPayment) {
      void loadVietQr()
    }
  }

  async function handleConfirmPayment() {
    if (!invoice || !canCollectPayment || invoice.status !== 'PENDING' || mutationInFlight.current)
      return
    mutationInFlight.current = true
    setPaying(true)
    setActionError(undefined)
    setTransactionRefError(undefined)
    try {
      const result = await api.pay(invoice.id, {
        method,
        ...(method === 'BANK_TRANSFER' && transactionRef.trim()
          ? { transactionRef: transactionRef.trim() }
          : {}),
      })
      if (!active.current) return
      setPaymentResult(result)
      void message?.success?.(`Đã xác nhận thanh toán hóa đơn ${result.invoiceCode}.`)
      loadDetail()
    } catch (cause) {
      if (!active.current) return
      const fields = invoiceFieldErrors(cause)
      setTransactionRefError(fields.transactionRef)
      setActionError(fields.transactionRef ? undefined : errorMessage(cause))
      if (
        axios.isAxiosError(cause) &&
        (cause.response?.status === 409 || cause.response?.status === 422)
      ) {
        loadDetail()
      }
    } finally {
      mutationInFlight.current = false
      if (active.current) setPaying(false)
    }
  }

  async function handleCancelInvoice() {
    if (!invoice || !canCancel || invoice.status !== 'PENDING' || mutationInFlight.current) return
    mutationInFlight.current = true
    setCancelling(true)
    setActionError(undefined)
    try {
      await api.cancel(invoice.id)
      if (!active.current) return
      setCancelModalOpen(false)
      void message?.success?.(`Đã hủy hóa đơn ${invoice.code}.`)
      loadDetail()
    } catch (cause) {
      if (!active.current) return
      setCancelModalOpen(false)
      setActionError(errorMessage(cause))
      if (
        axios.isAxiosError(cause) &&
        (cause.response?.status === 409 || cause.response?.status === 422)
      ) {
        loadDetail()
      }
    } finally {
      mutationInFlight.current = false
      if (active.current) setCancelling(false)
    }
  }

  async function handleExportPdf() {
    if (!invoice) return
    setActionError(undefined)
    await pdf.exportPdf(invoice.id, `hoa-don-${invoice.code}.pdf`)
  }

  async function copyTransferContent(text: string, label = 'nội dung chuyển khoản') {
    try {
      if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable')
      await navigator.clipboard.writeText(text)
      void message?.success?.(`Đã sao chép ${label}.`)
    } catch {
      void message?.info?.(`Vui lòng chọn và sao chép thủ công ${label}.`)
    }
  }

  if (!validId) {
    return (
      <section className="feature-page">
        <Alert
          type="error"
          showIcon
          title="Mã hóa đơn không hợp lệ."
          action={
            <Link to="/hoa-don">
              <Button size="small">Quay lại danh sách</Button>
            </Link>
          }
        />
      </section>
    )
  }

  const statusMeta = invoice ? INVOICE_STATUS_META[invoice.status] : undefined
  const canShowPdfButton =
    Boolean(invoice) &&
    canExportPdf &&
    (invoice?.status === 'PAID' || invoice?.status === 'REFUNDED')

  return (
    <section className="feature-page" aria-labelledby="invoice-detail-heading">
      <div className="feature-heading">
        <div>
          <Space wrap style={{ marginBottom: 8 }}>
            <Link to="/hoa-don">
              <Button icon={<ArrowLeftOutlined />} style={{ minHeight: 40 }}>
                Quay lại danh sách
              </Button>
            </Link>
          </Space>
          <h2 id="invoice-detail-heading">
            Chi tiết hóa đơn {invoice ? invoice.code : `#${invoiceId}`}{' '}
            {statusMeta && <Tag color={statusMeta.color}>{statusMeta.label}</Tag>}
          </h2>
          <p>Kiểm tra dịch vụ, xác nhận thanh toán tại quầy hoặc xuất hóa đơn PDF.</p>
        </div>

        <Space wrap>
          {canShowPdfButton && (
            <Button
              type="primary"
              icon={<PrinterOutlined />}
              loading={pdf.exporting}
              onClick={() => void handleExportPdf()}
              style={{ minHeight: 40 }}
            >
              In / tải PDF
            </Button>
          )}
          {/* S2-38.2: gắn nút Hoàn tiền cho BRANCH_MANAGER tại đây khi invoice?.status === 'PAID'. */}
          <Button
            icon={<ReloadOutlined />}
            onClick={() => void loadDetail()}
            style={{ minHeight: 40 }}
          >
            Tải lại
          </Button>
        </Space>
      </div>

      {loading && !invoice ? (
        <div className="feature-content">
          <Spin description="Đang tải chi tiết hóa đơn…" />
        </div>
      ) : loadError !== undefined && !invoice ? (
        <Alert
          type="error"
          showIcon
          title={errorMessage(loadError)}
          action={
            <Button size="small" onClick={() => void loadDetail()}>
              Thử lại
            </Button>
          }
        />
      ) : invoice ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {(actionError || pdf.error) && (
            <Alert
              type="error"
              showIcon
              title={actionError ?? pdf.error}
              action={
                <Button
                  size="small"
                  onClick={() => {
                    setActionError(undefined)
                    pdf.clearError()
                  }}
                >
                  Đóng
                </Button>
              }
            />
          )}

          {paymentResult && (
            <Alert
              type="success"
              showIcon
              title={`Đã xác nhận thanh toán ${vndFormat.format(paymentResult.amount)} (${PAYMENT_METHOD_LABELS[paymentResult.method]}).`}
              description={
                <ActivatedMemberships memberships={paymentResult.activatedMemberships} />
              }
            />
          )}

          <Card title="Thông tin hóa đơn">
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="Mã hóa đơn">
                <strong>{invoice.code}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={INVOICE_STATUS_META[invoice.status].color}>
                  {INVOICE_STATUS_META[invoice.status].label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Khách hàng">
                {invoice.memberId === null ? (
                  'Khách lẻ'
                ) : (
                  <>
                    <strong>{invoice.memberName ?? '—'}</strong>
                    {invoice.memberCode ? ` (${invoice.memberCode})` : ''}
                    {invoice.memberPhone ? ` · ${invoice.memberPhone}` : ''}
                  </>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Nguồn">
                {INVOICE_SOURCE_LABELS[invoice.source] ?? invoice.source}
              </Descriptions.Item>
              <Descriptions.Item label="Thời gian tạo">
                {formatVnDateTime(invoice.createdAt)}
              </Descriptions.Item>
              <Descriptions.Item label="Thời gian thanh toán">
                {formatVnDateTime(invoice.paidAt)}
              </Descriptions.Item>
              <Descriptions.Item label="Tổng cần thanh toán">
                <Typography.Text strong style={{ fontSize: 16, color: '#1677ff' }}>
                  {vndFormat.format(invoice.total)}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Tạm tính">
                {vndFormat.format(invoice.subtotal)}
              </Descriptions.Item>
              <Descriptions.Item label="Giảm giá">
                {vndFormat.format(invoice.discount)}
              </Descriptions.Item>
              {invoice.status === 'REFUNDED' && (
                <>
                  <Descriptions.Item label="Thời gian hoàn tiền">
                    {formatVnDateTime(invoice.refundedAt)}
                  </Descriptions.Item>
                  <Descriptions.Item label="Lý do hoàn tiền">
                    {invoice.refundReason ?? '—'}
                  </Descriptions.Item>
                </>
              )}
            </Descriptions>
          </Card>

          <Card title="Chi tiết dịch vụ">
            <Table<InvoiceItem>
              rowKey="id"
              pagination={false}
              scroll={{ x: 640 }}
              dataSource={invoice.items}
              locale={{ emptyText: 'Không có dòng dịch vụ.' }}
              columns={[
                { title: 'Mô tả', dataIndex: 'description' },
                { title: 'SL', dataIndex: 'quantity', width: 70, align: 'right' },
                {
                  title: 'Đơn giá',
                  dataIndex: 'unitPrice',
                  width: 140,
                  align: 'right',
                  render: (value: number) => vndFormat.format(value),
                },
                {
                  title: 'Giảm giá',
                  dataIndex: 'discountAmount',
                  width: 130,
                  align: 'right',
                  render: (value: number) => vndFormat.format(value),
                },
                {
                  title: 'Thành tiền',
                  dataIndex: 'amount',
                  width: 150,
                  align: 'right',
                  render: (value: number) => <strong>{vndFormat.format(value)}</strong>,
                },
              ]}
            />
          </Card>

          {invoice.status === 'PENDING' && (
            <Card title="Thu tiền và xử lý hóa đơn chờ">
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {canCollectPayment ? (
                  <>
                    <div>
                      <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
                        Phương thức thanh toán tại quầy
                      </Typography.Text>
                      <Radio.Group
                        value={method}
                        disabled={paying || cancelling}
                        onChange={(event) =>
                          handleMethodChange(event.target.value as PaymentMethod)
                        }
                      >
                        <Space wrap>
                          <Radio value="CASH">Tiền mặt</Radio>
                          <Radio value="BANK_TRANSFER">Chuyển khoản</Radio>
                        </Space>
                      </Radio.Group>
                    </div>

                    {method === 'BANK_TRANSFER' && (
                      <Card
                        size="small"
                        title="Mã chuyển khoản VietQR"
                        extra={
                          <Button
                            size="small"
                            icon={<ReloadOutlined />}
                            loading={loadingQr}
                            onClick={() => void loadVietQr()}
                          >
                            Tải lại QR
                          </Button>
                        }
                      >
                        {loadingQr ? (
                          <Spin description="Đang tạo mã VietQR…" />
                        ) : qrError ? (
                          <Alert type="error" showIcon title={qrError} />
                        ) : vietQr ? (
                          <div
                            style={{
                              display: 'flex',
                              flexWrap: 'wrap',
                              gap: 20,
                              alignItems: 'flex-start',
                            }}
                          >
                            <img
                              src={vietQr.qrImageUrl}
                              hidden={qrImageFailed}
                              onError={() => setQrImageFailed(true)}
                              alt={`Mã VietQR hóa đơn ${vietQr.invoiceCode}`}
                              style={{
                                width: 220,
                                maxWidth: '100%',
                                height: 'auto',
                                borderRadius: 8,
                                border: '1px solid #e8ecf2',
                                background: '#fff',
                              }}
                            />
                            {qrImageFailed && (
                              <Alert
                                type="warning"
                                showIcon
                                title="Không tải được ảnh VietQR. Hãy thử tải lại QR hoặc dùng thông tin chuyển khoản bên cạnh."
                              />
                            )}
                            <div style={{ flex: 1, minWidth: 220 }}>
                              <Descriptions column={1} bordered size="small">
                                <Descriptions.Item label="Ngân hàng">
                                  {vietQr.bankName} ({vietQr.bankId})
                                </Descriptions.Item>
                                <Descriptions.Item label="Số tài khoản">
                                  <strong>{vietQr.accountNo}</strong>
                                  <Button
                                    size="small"
                                    icon={<CopyOutlined />}
                                    style={{ marginLeft: 8 }}
                                    onClick={() =>
                                      void copyTransferContent(vietQr.accountNo, 'số tài khoản')
                                    }
                                  >
                                    Sao chép tài khoản
                                  </Button>
                                </Descriptions.Item>
                                <Descriptions.Item label="Chủ tài khoản">
                                  {vietQr.accountName}
                                </Descriptions.Item>
                                <Descriptions.Item label="Số tiền">
                                  <strong>{vndFormat.format(vietQr.total)}</strong>
                                </Descriptions.Item>
                                <Descriptions.Item label="Nội dung chuyển khoản">
                                  <Space wrap>
                                    <Typography.Text strong code>
                                      {vietQr.transferContent}
                                    </Typography.Text>
                                    <Button
                                      size="small"
                                      icon={<CopyOutlined />}
                                      aria-label="Sao chép nội dung chuyển khoản"
                                      onClick={() =>
                                        void copyTransferContent(vietQr.transferContent)
                                      }
                                    >
                                      Sao chép
                                    </Button>
                                  </Space>
                                </Descriptions.Item>
                              </Descriptions>
                            </div>
                          </div>
                        ) : null}

                        <div style={{ marginTop: 16, maxWidth: 420 }}>
                          <label
                            htmlFor="invoice-transaction-ref"
                            style={{
                              display: 'block',
                              marginBottom: 6,
                              fontSize: 13,
                              fontWeight: 500,
                            }}
                          >
                            Mã tham chiếu giao dịch (tùy chọn)
                          </label>
                          <Input
                            id="invoice-transaction-ref"
                            placeholder="Ví dụ: FT263078912345"
                            maxLength={100}
                            disabled={paying || cancelling}
                            status={transactionRefError ? 'error' : undefined}
                            aria-invalid={Boolean(transactionRefError)}
                            aria-describedby={
                              transactionRefError ? 'invoice-transaction-ref-error' : undefined
                            }
                            value={transactionRef}
                            onChange={(event) => {
                              setTransactionRef(event.target.value)
                              setTransactionRefError(undefined)
                            }}
                          />
                          {transactionRefError && (
                            <Typography.Text
                              id="invoice-transaction-ref-error"
                              type="danger"
                              role="alert"
                            >
                              {transactionRefError}
                            </Typography.Text>
                          )}
                        </div>
                      </Card>
                    )}

                    <Alert
                      type="warning"
                      showIcon
                      title="Chỉ bấm xác nhận sau khi đã nhận đủ tiền mặt hoặc đối chiếu tin nhắn báo có từ ngân hàng."
                    />
                  </>
                ) : (
                  <Alert
                    type="info"
                    showIcon
                    title="Chỉ lễ tân hoặc quản lý chi nhánh mới được xác nhận thu tiền tại quầy."
                  />
                )}

                <div
                  className="feature-form-actions"
                  style={{ marginTop: 0, justifyContent: 'space-between' }}
                >
                  {canCancel && (
                    <Button
                      danger
                      icon={<CloseCircleOutlined />}
                      disabled={paying || cancelling}
                      onClick={() => setCancelModalOpen(true)}
                      style={{ minHeight: 40 }}
                    >
                      Hủy hóa đơn
                    </Button>
                  )}
                  {canCollectPayment && (
                    <Button
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      loading={paying}
                      disabled={cancelling || (method === 'BANK_TRANSFER' && loadingQr)}
                      onClick={() => void handleConfirmPayment()}
                      style={{ minHeight: 40 }}
                    >
                      Xác nhận đã nhận tiền
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          )}

          {invoice.payments.length > 0 && (
            <Card title="Lịch sử phiếu thu">
              <Table<InvoicePayment>
                rowKey="id"
                pagination={false}
                scroll={{ x: 640 }}
                dataSource={invoice.payments}
                columns={[
                  {
                    title: 'Phương thức',
                    dataIndex: 'method',
                    width: 140,
                    render: (value: PaymentMethod) => PAYMENT_METHOD_LABELS[value] ?? value,
                  },
                  {
                    title: 'Số tiền',
                    dataIndex: 'amount',
                    width: 140,
                    align: 'right',
                    render: (value: number) => <strong>{vndFormat.format(value)}</strong>,
                  },
                  {
                    title: 'Trạng thái',
                    dataIndex: 'status',
                    width: 130,
                    render: (value: InvoicePayment['status']) => {
                      const meta = PAYMENT_STATUS_META[value]
                      return <Tag color={meta.color}>{meta.label}</Tag>
                    },
                  },
                  {
                    title: 'Mã giao dịch',
                    dataIndex: 'transactionRef',
                    render: (value: string | null) => value ?? '—',
                  },
                  {
                    title: 'Người thu',
                    dataIndex: 'receivedByName',
                    render: (value: string | null, row: InvoicePayment) =>
                      value ?? `Người dùng #${row.receivedBy}`,
                  },
                  {
                    title: 'Thời gian thu',
                    dataIndex: 'paidAt',
                    width: 150,
                    render: (value: string) => formatVnDateTime(value),
                  },
                ]}
              />
            </Card>
          )}
        </div>
      ) : null}

      <Modal
        open={cancelModalOpen}
        title="Hủy hóa đơn chờ?"
        onCancel={cancelling ? undefined : () => setCancelModalOpen(false)}
        closable={!cancelling}
        mask={{ closable: !cancelling }}
        footer={
          <div className="feature-form-actions" style={{ marginTop: 0 }}>
            <Button
              disabled={cancelling}
              onClick={() => setCancelModalOpen(false)}
              style={{ minHeight: 40 }}
            >
              Đóng
            </Button>
            <Button
              type="primary"
              danger
              loading={cancelling}
              onClick={() => void handleCancelInvoice()}
              style={{ minHeight: 40 }}
            >
              Xác nhận hủy
            </Button>
          </div>
        }
      >
        <p style={{ margin: 0 }}>
          Hóa đơn <strong>{invoice?.code}</strong> và gói tập chờ thanh toán đi kèm (nếu có) sẽ
          chuyển sang trạng thái đã hủy. Thao tác này không thể hoàn tác.
        </p>
      </Modal>
    </section>
  )
}
