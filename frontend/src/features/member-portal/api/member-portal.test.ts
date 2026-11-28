import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { publicPackagesApi } from '../../packages/api/public-packages'
import { invoicesApi } from '../../invoices/api/invoices'
import { memberPortalApi } from './member-portal'

describe('memberPortalApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('TC-F7.1.1-01b, TC-F7.1.1-02b: listMemberships gọi GET /me/memberships không truyền memberId', async () => {
    const getSpy = vi.spyOn(http, 'get').mockResolvedValue({ data: [] })
    await memberPortalApi.listMemberships()
    expect(getSpy).toHaveBeenCalledWith('/me/memberships')
  })

  it('TC-F7.2.1-01b, TC-F7.2.1-03b: listCheckins gọi GET /me/checkins kèm tham số phân trang và không truyền memberId', async () => {
    const getSpy = vi.spyOn(http, 'get').mockResolvedValue({
      data: { content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 },
    })
    await memberPortalApi.listCheckins({ page: 1, size: 10 })
    expect(getSpy).toHaveBeenCalledWith('/me/checkins', { params: { page: 1, size: 10 } })
  })

  it('TC-F7.3.1-01b, TC-F7.3.1-03b: listInvoices gọi GET /me/invoices kèm tham số phân trang và không truyền memberId', async () => {
    const getSpy = vi.spyOn(http, 'get').mockResolvedValue({
      data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    })
    await memberPortalApi.listInvoices({ page: 0, size: 20 })
    expect(getSpy).toHaveBeenCalledWith('/me/invoices', { params: { page: 0, size: 20 } })
  })

  it('TC-F7.4.1-01b: requestPurchase chỉ gửi packageId lên POST /me/purchase-requests, không gửi số tiền', async () => {
    const postSpy = vi.spyOn(http, 'post').mockResolvedValue({ data: { invoiceCode: 'HD000010' } })
    await memberPortalApi.requestPurchase(5)
    expect(postSpy).toHaveBeenCalledWith('/me/purchase-requests', { packageId: 5 })
  })

  it('TC-F7.5.1-01b: requestRenewal chỉ gửi membershipId lên POST /me/renewal-requests, không gửi số tiền', async () => {
    const postSpy = vi.spyOn(http, 'post').mockResolvedValue({ data: { invoiceCode: 'HD000011' } })
    await memberPortalApi.requestRenewal(12)
    expect(postSpy).toHaveBeenCalledWith('/me/renewal-requests', { membershipId: 12 })
  })

  it('TC-F7.4.1-01c: listPublicPackages dùng lại publicPackagesApi.list và downloadInvoicePdf dùng lại invoicesApi.pdf', async () => {
    const pkgSpy = vi.spyOn(publicPackagesApi, 'list').mockResolvedValue([])
    const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    const pdfSpy = vi.spyOn(invoicesApi, 'pdf').mockResolvedValue(pdfBlob)

    await memberPortalApi.listPublicPackages()
    const blob = await memberPortalApi.downloadInvoicePdf(88)

    expect(pkgSpy).toHaveBeenCalledTimes(1)
    expect(pdfSpy).toHaveBeenCalledWith(88)
    expect(blob).toBe(pdfBlob)
  })
})
