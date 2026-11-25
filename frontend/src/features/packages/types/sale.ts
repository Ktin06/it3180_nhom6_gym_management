import type { PackageType } from './package'

export type MembershipStatus = 'PENDING' | 'UPCOMING' | 'ACTIVE' | 'FROZEN'
export type InvoiceStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED'

/** Khớp HeldMembershipDto của GET /api/v1/memberships?memberId={id} (S2-07.1). */
export interface HeldMembership {
  id: number
  packageId: number
  packageName: string
  packageType: PackageType
  branchId: number | null
  status: MembershipStatus
  startDate: string
  endDate: string
  totalSessions: number | null
  remainingSessions: number | null
  renewalStartDate: string | null
}

/** Chỉ gửi ID hội viên và ID gói; tuyệt đối không gửi giá hoặc tổng tiền (S7). */
export interface CreatePackageInvoiceInput {
  memberId: number
  items: [{ itemType: 'PACKAGE'; packageId: number }]
}

/** Khớp PackageInvoiceDto trả về 201 từ POST /api/v1/invoices (S2-07.1). */
export interface PackageInvoice {
  invoiceId: number
  invoiceCode: string
  membershipId: number
  memberId: number
  packageId: number
  packageName: string
  subtotal: number
  discount: number
  total: number
  status: InvoiceStatus
  membershipStatus: MembershipStatus
  startDate: string
  endDate: string
}

export interface PackageSalesGateway {
  listMemberships(memberId: number): Promise<HeldMembership[]>
  createInvoice(input: CreatePackageInvoiceInput): Promise<PackageInvoice>
}
