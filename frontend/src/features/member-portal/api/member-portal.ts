import { http } from '../../../services/http'
import { invoicesApi } from '../../invoices/api/invoices'
import { publicPackagesApi } from '../../packages/api/public-packages'
import type { MemberPortalGateway } from '../types/member-portal'

/**
 * Client gọi các API cổng hội viên (F7: UC-F7.1–UC-F7.5), tái sử dụng bảng giá công khai (F3) và xuất PDF (F5).
 * Không gửi memberId hay số tiền từ trình duyệt (BR-F7.1, S7).
 */
export const memberPortalApi: MemberPortalGateway = {
  listMemberships: async () => (await http.get('/me/memberships')).data,
  listCheckins: async (params) => (await http.get('/me/checkins', { params })).data,
  listInvoices: async (params) => (await http.get('/me/invoices', { params })).data,
  listPublicPackages: () => publicPackagesApi.list(),
  requestPurchase: async (packageId) =>
    (await http.post('/me/purchase-requests', { packageId })).data,
  requestRenewal: async (membershipId) =>
    (await http.post('/me/renewal-requests', { membershipId })).data,
  downloadInvoicePdf: (invoiceId) => invoicesApi.pdf(invoiceId),
}
