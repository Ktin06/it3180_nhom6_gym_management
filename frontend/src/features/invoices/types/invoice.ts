import type { PageQuery, PageResponse } from '../../shared/types'

export type InvoiceStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED'
export type InvoiceSource = 'COUNTER' | 'MEMBER_PORTAL'
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER'
export type PaymentStatus = 'CONFIRMED' | 'CANCELLED'

export interface InvoiceBranch {
  id: number
  code: string
  name: string
}

export interface InvoiceSummary {
  id: number
  code: string
  branchId: number
  memberId: number | null
  memberCode: string | null
  memberName: string | null
  memberPhone: string | null
  total: number
  source: InvoiceSource
  status: InvoiceStatus
  paidAt: string | null
  createdAt: string
}

export interface InvoiceItem {
  id: number
  itemType: string
  packageId: number | null
  productId: number | null
  description: string
  quantity: number
  unitPrice: number
  amount: number
  discountAmount: number
}

export interface InvoicePayment {
  id: number
  method: PaymentMethod
  amount: number
  status: PaymentStatus
  transactionRef: string | null
  receivedBy: number
  receivedByName: string | null
  paidAt: string
}

export interface InvoiceDetail {
  id: number
  code: string
  branchId: number
  memberId: number | null
  memberCode: string | null
  memberName: string | null
  memberPhone: string | null
  createdBy: number | null
  source: InvoiceSource
  subtotal: number
  discount: number
  total: number
  status: InvoiceStatus
  paidAt: string | null
  refundedBy: number | null
  refundedAt: string | null
  refundReason: string | null
  createdAt: string
  updatedAt: string
  items: InvoiceItem[]
  payments: InvoicePayment[]
}

export interface VietQrPayment {
  invoiceId: number
  invoiceCode: string
  total: number
  bankId: string
  bankName: string
  accountNo: string
  accountName: string
  transferContent: string
  qrImageUrl: string
}

export interface PayInvoiceInput {
  method: PaymentMethod
  transactionRef?: string
}

export interface ActivatedMembership {
  membershipId: number
  packageType: 'TIME' | 'SESSION' | 'PT'
  status: 'ACTIVE' | 'UPCOMING'
  startDate: string
  endDate: string
  remainingSessions: number | null
}

export interface PaymentConfirmation {
  paymentId: number
  invoiceId: number
  invoiceCode: string
  method: PaymentMethod
  amount: number
  status: InvoiceStatus
  paidAt: string
  receivedBy: number
  receivedByName: string | null
  activatedMemberships: ActivatedMembership[]
}

export interface CancelledInvoice {
  id: number
  code: string
  status: InvoiceStatus
  updatedAt: string
}

export interface RefundInvoiceInput {
  refundReason: string
}

export interface RefundResult {
  invoiceId: number
  invoiceCode: string
  status: InvoiceStatus
  refundedAt: string
  refundedBy: number
  refundedByName: string | null
  refundReason: string
}

export interface InvoiceQuery extends PageQuery {
  status?: InvoiceStatus
  source?: InvoiceSource
  fromDate?: string
  toDate?: string
  keyword?: string
  branchId?: number
}

export interface InvoicesGateway {
  branches(): Promise<InvoiceBranch[]>
  list(query: InvoiceQuery): Promise<PageResponse<InvoiceSummary>>
  detail(id: number): Promise<InvoiceDetail>
  vietqr(id: number): Promise<VietQrPayment>
  pay(id: number, input: PayInvoiceInput): Promise<PaymentConfirmation>
  cancel(id: number): Promise<CancelledInvoice>
  pdf(id: number): Promise<Blob>
  refund(id: number, input: RefundInvoiceInput): Promise<RefundResult>
}
