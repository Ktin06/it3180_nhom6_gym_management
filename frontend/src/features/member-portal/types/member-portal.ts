import type { InvoiceSource, InvoiceStatus } from '../../invoices/types/invoice'
import type { PackageType, PublicPackage } from '../../packages/types/package'
import type { PageQuery, PageResponse } from '../../shared/types'

export type MembershipStatus =
  'PENDING' | 'UPCOMING' | 'ACTIVE' | 'FROZEN' | 'EXPIRED' | 'TRANSFERRED' | 'CANCELLED'

/** Khớp MyMembershipDto từ GET /api/v1/me/memberships (UC-F7.1). */
export interface MyMembership {
  id: number
  packageId: number
  packageName: string
  packageType: PackageType
  branchId: number | null
  branchName: string | null
  status: MembershipStatus
  startDate: string
  endDate: string
  totalSessions: number | null
  remainingSessions: number | null
}

export type CheckinMethod = 'MANUAL' | 'FACE'

/** Khớp MyCheckinDto từ GET /api/v1/me/checkins (UC-F7.2). */
export interface MyCheckin {
  id: number
  branchId: number
  branchName: string
  membershipId: number
  packageName: string
  packageType: PackageType
  method: CheckinMethod
  sessionDeducted: boolean
  checkinDate: string
  checkedInAt: string
  checkedOutAt: string | null
}

/** Khớp MyInvoiceItemDto trong MyInvoiceDto (UC-F7.3). */
export interface MyInvoiceItem {
  id: number
  itemType: string
  packageId: number | null
  description: string
  quantity: number
  unitPrice: number
  amount: number
  discountAmount: number
}

/** Khớp MyInvoiceDto từ GET /api/v1/me/invoices (UC-F7.3). */
export interface MyInvoice {
  id: number
  code: string
  branchId: number
  branchName: string
  source: InvoiceSource
  subtotal: number
  discount: number
  total: number
  status: InvoiceStatus
  paidAt: string | null
  createdAt: string
  expiresAt: string | null
  items: MyInvoiceItem[]
}

/** Khớp PortalInvoiceDto từ POST /api/v1/me/purchase-requests và /renewal-requests (UC-F7.4, UC-F7.5). */
export interface PortalInvoiceResult {
  invoiceId: number
  invoiceCode: string
  membershipId: number
  packageId: number
  packageName: string
  subtotal: number
  discount: number
  total: number
  status: InvoiceStatus
  membershipStatus: MembershipStatus
  startDate: string
  endDate: string
  expiresAt: string
}

export interface MemberPortalGateway {
  listMemberships(): Promise<MyMembership[]>
  listCheckins(query: PageQuery): Promise<PageResponse<MyCheckin>>
  listInvoices(query: PageQuery): Promise<PageResponse<MyInvoice>>
  listPublicPackages(): Promise<PublicPackage[]>
  requestPurchase(packageId: number): Promise<PortalInvoiceResult>
  requestRenewal(membershipId: number): Promise<PortalInvoiceResult>
  downloadInvoicePdf(invoiceId: number): Promise<Blob>
}
