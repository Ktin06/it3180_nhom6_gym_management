import { http } from '../../../services/http'
import type { InvoicesGateway, PayInvoiceInput, RefundInvoiceInput } from '../types/invoice'
import { decodePdfProblem } from '../invoice-errors'

export const liveInvoicesApi: InvoicesGateway = {
  branches: async () => (await http.get('/invoices/branches')).data,
  list: async (params) => (await http.get('/invoices', { params })).data,
  detail: async (id) => (await http.get(`/invoices/${id}`)).data,
  vietqr: async (id) => (await http.get(`/invoices/${id}/vietqr`)).data,
  pay: async (id, input) => {
    // S7: chỉ gửi phương thức và mã tham chiếu tùy chọn, không bao giờ gửi số tiền lên server.
    const body: PayInvoiceInput = { method: input.method }
    const ref = input.transactionRef?.trim()
    if (ref) body.transactionRef = ref
    return (await http.post(`/invoices/${id}/payments`, body)).data
  },
  cancel: async (id) => (await http.put(`/invoices/${id}/cancel`)).data,
  refund: async (id, input) => {
    // S7: chỉ gửi lý do hoàn tiền, không gửi số tiền (server luôn hoàn toàn bộ invoices.total).
    const body: RefundInvoiceInput = { refundReason: input.refundReason.trim() }
    return (await http.post(`/invoices/${id}/refund`, body)).data
  },
  pdf: async (id) => {
    try {
      return (await http.get<Blob>(`/invoices/${id}/pdf`, { responseType: 'blob' })).data
    } catch (error) {
      await decodePdfProblem(error)
      throw error
    }
  },
}

export const invoicesApi: InvoicesGateway = liveInvoicesApi
