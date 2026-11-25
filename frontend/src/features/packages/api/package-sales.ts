import { http } from '../../../services/http'
import type { PackageSalesGateway } from '../types/sale'

export const livePackageSalesApi: PackageSalesGateway = {
  listMemberships: async (memberId) =>
    (await http.get('/memberships', { params: { memberId } })).data,
  createInvoice: async (input) => (await http.post('/invoices', input)).data,
}

export const packageSalesApi: PackageSalesGateway = livePackageSalesApi
