import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import type { CreatePackageInvoiceInput } from '../types/sale'
import { livePackageSalesApi } from './package-sales'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('kết nối API bán và gia hạn gói F3', () => {
  it('TC-F3.5.1-01d: livePackageSalesApi gọi đúng GET /memberships?memberId={id} và POST /invoices không kèm giá hoặc tổng tiền', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({
      data: {
        invoiceId: 88,
        invoiceCode: 'HD000088',
        membershipId: 77,
        memberId: 123,
        packageId: 5,
        packageName: 'Gói PT 10 buổi',
        subtotal: 3000000,
        discount: 0,
        total: 3000000,
        status: 'PENDING',
        membershipStatus: 'PENDING',
        startDate: '2026-11-20',
        endDate: '2027-02-17',
      },
    })

    await livePackageSalesApi.listMemberships(123)
    expect(get).toHaveBeenCalledWith('/memberships', { params: { memberId: 123 } })

    const payload: CreatePackageInvoiceInput = {
      memberId: 123,
      items: [{ itemType: 'PACKAGE', packageId: 5 }],
    }
    await livePackageSalesApi.createInvoice(payload)
    expect(post).toHaveBeenCalledWith('/invoices', payload)
    const sentBody = post.mock.calls[0][1] as Record<string, unknown>
    expect(Object.keys(sentBody)).toEqual(['memberId', 'items'])
    expect(Object.keys((sentBody.items as Record<string, unknown>[])[0])).toEqual([
      'itemType',
      'packageId',
    ])
  })
})
