import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { liveInvoicesApi } from './invoices'
import { AxiosError, AxiosHeaders } from 'axios'
import { errorMessage } from '../../shared/api-error'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('kết nối API hóa đơn và thanh toán F5', () => {
  it('TC-F5.3.1-04c: danh mục chi nhánh dùng endpoint hóa đơn dành cho Chủ chuỗi', async () => {
    const data = [{ id: 1, code: 'CN01', name: 'Bách Khoa' }]
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data })
    expect(await liveInvoicesApi.branches()).toEqual(data)
    expect(get).toHaveBeenCalledWith('/invoices/branches')
  })

  it('TC-F5.2.1-10c: loại bỏ giá trị tiền gửi thêm và chuẩn hóa mã tham chiếu', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue({ data: {} })
    const input = {
      method: 'BANK_TRANSFER' as const,
      transactionRef: '  FT123  ',
      amount: 1,
      total: 1,
      unitPrice: 1,
    }
    await liveInvoicesApi.pay(88, input)
    expect(post).toHaveBeenCalledWith('/invoices/88/payments', {
      method: 'BANK_TRANSFER',
      transactionRef: 'FT123',
    })
    await liveInvoicesApi.pay(88, { method: 'CASH', transactionRef: '   ' })
    expect(post).toHaveBeenLastCalledWith('/invoices/88/payments', { method: 'CASH' })
  })

  describe('TC-F5.4.1-04b: giữ status và detail của lỗi PDF Blob %s', () => {
    it.each([404, 422])('trường hợp %s', async (status) => {
      const detail =
        status === 404
          ? 'Hóa đơn không tồn tại trên hệ thống.'
          : 'Chỉ có thể xuất hóa đơn đã thanh toán.'
      const error = new AxiosError('Request failed')
      error.response = {
        status,
        statusText: 'Error',
        headers: {},
        config: { headers: new AxiosHeaders() },
        data: new Blob([JSON.stringify({ detail })], { type: 'application/problem+json' }),
      }
      vi.spyOn(http, 'get').mockRejectedValue(error)
      await expect(liveInvoicesApi.pdf(88)).rejects.toBe(error)
      expect(error.response.status).toBe(status)
      expect(errorMessage(error)).toBe(detail)
    })
  })

  it('TC-F5.4.1-01c: lỗi PDF không phải JSON vẫn giữ được lỗi HTTP gốc', async () => {
    const error = new AxiosError('Proxy unavailable')
    error.response = {
      status: 502,
      statusText: 'Bad Gateway',
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: new Blob(['<html>Bad Gateway</html>'], { type: 'text/html' }),
    }
    vi.spyOn(http, 'get').mockRejectedValue(error)
    await expect(liveInvoicesApi.pdf(88)).rejects.toBe(error)
    expect(error.response.status).toBe(502)
  })

  it('TC-F5.3.1-05b: list và detail gọi đúng endpoint và tham số lọc', async () => {
    const get = vi
      .spyOn(http, 'get')
      .mockResolvedValueOnce({
        data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
      })
      .mockResolvedValueOnce({ data: { id: 88, code: 'HD000088' } })

    await liveInvoicesApi.list({
      status: 'PENDING',
      source: 'COUNTER',
      fromDate: '2026-11-01',
      toDate: '2026-11-20',
      keyword: 'HD000088',
      page: 1,
      size: 10,
    })
    expect(get).toHaveBeenNthCalledWith(1, '/invoices', {
      params: {
        status: 'PENDING',
        source: 'COUNTER',
        fromDate: '2026-11-01',
        toDate: '2026-11-20',
        keyword: 'HD000088',
        page: 1,
        size: 10,
      },
    })

    await liveInvoicesApi.detail(88)
    expect(get).toHaveBeenNthCalledWith(2, '/invoices/88')
  })

  it('TC-F5.1.1-01b: vietqr gọi GET /invoices/{id}/vietqr', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({
      data: {
        invoiceId: 88,
        invoiceCode: 'HD000088',
        total: 3000000,
        bankId: 'MB',
        bankName: 'MBBank',
        accountNo: '0987654321',
        accountName: 'CONG TY GYM NHOM 6',
        transferContent: 'CN01 HD000088',
        qrImageUrl: 'https://img.vietqr.io/image/MB-0987654321-compact.png?amount=3000000',
      },
    })

    await liveInvoicesApi.vietqr(88)
    expect(get).toHaveBeenCalledWith('/invoices/88/vietqr')
  })

  it('TC-F5.2.1-10b: pay gửi đúng method và không gửi số tiền lên server (S7)', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue({
      data: {
        paymentId: 105,
        invoiceId: 88,
        invoiceCode: 'HD000088',
        method: 'BANK_TRANSFER',
        amount: 3000000,
        status: 'PAID',
        paidAt: '2026-11-20T09:15:00Z',
        receivedBy: 2,
        receivedByName: 'Lễ tân A',
        activatedMemberships: [],
      },
    })

    await liveInvoicesApi.pay(88, { method: 'BANK_TRANSFER', transactionRef: 'FT123456' })
    expect(post).toHaveBeenCalledWith('/invoices/88/payments', {
      method: 'BANK_TRANSFER',
      transactionRef: 'FT123456',
    })
    const sentBody = post.mock.calls[0][1] as Record<string, unknown>
    expect(sentBody).not.toHaveProperty('amount')
    expect(sentBody).not.toHaveProperty('total')
    expect(Object.keys(sentBody)).toEqual(['method', 'transactionRef'])
  })

  it('TC-F5.3.1-06d: cancel gọi PUT /invoices/{id}/cancel', async () => {
    const put = vi.spyOn(http, 'put').mockResolvedValue({
      data: { id: 88, code: 'HD000088', status: 'CANCELLED', updatedAt: '2026-11-20T10:00:00Z' },
    })

    await liveInvoicesApi.cancel(88)
    expect(put).toHaveBeenCalledWith('/invoices/88/cancel')
  })

  it('TC-F5.4.1-01b: pdf gọi GET /invoices/{id}/pdf với responseType blob', async () => {
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data: blob })

    const result = await liveInvoicesApi.pdf(88)
    expect(get).toHaveBeenCalledWith('/invoices/88/pdf', { responseType: 'blob' })
    expect(result).toBe(blob)
  })

  it('TC-F5.5.1-01d: refund gửi POST /invoices/{id}/refund với refundReason đã cắt khoảng trắng và không gửi số tiền', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue({
      data: {
        invoiceId: 88,
        invoiceCode: 'HD000088',
        status: 'REFUNDED',
        refundedAt: '2026-11-20T02:00:00Z',
        refundedBy: 3,
        refundedByName: 'Trần Quản Lý',
        refundReason: 'Lễ tân bán nhầm gói 3 tháng',
      },
    })

    const result = await liveInvoicesApi.refund(88, {
      refundReason: '  Lễ tân bán nhầm gói 3 tháng  ',
    })

    expect(post).toHaveBeenCalledWith('/invoices/88/refund', {
      refundReason: 'Lễ tân bán nhầm gói 3 tháng',
    })
    const sentBody = post.mock.calls[0][1] as Record<string, unknown>
    expect(sentBody).not.toHaveProperty('amount')
    expect(sentBody).not.toHaveProperty('total')
    expect(Object.keys(sentBody)).toEqual(['refundReason'])
    expect(result.status).toBe('REFUNDED')
    expect(result.refundedByName).toBe('Trần Quản Lý')
  })
})
