import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { checkinsApi } from './checkins'

vi.mock('../../../services/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const mockGet = vi.mocked(http.get)
const mockPost = vi.mocked(http.post)

describe('checkinsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('US-F4 API: gọi đúng endpoint tra cứu, check-in, check-out và lịch sử', async () => {
    mockGet.mockResolvedValueOnce({ data: { memberId: 12, memberCode: 'HV000012' } })
    const searched = await checkinsApi.searchMember('  HV000012  ')
    expect(mockGet).toHaveBeenCalledWith('/checkins/search-member', {
      params: { keyword: 'HV000012' },
    })
    expect(searched.memberId).toBe(12)

    mockPost.mockResolvedValueOnce({ data: { checkinId: 340 } })
    const checkedIn = await checkinsApi.checkin(12)
    expect(mockPost).toHaveBeenCalledWith('/checkins', { memberId: 12 })
    expect(checkedIn.checkinId).toBe(340)

    mockPost.mockResolvedValueOnce({ data: { checkinId: 340, durationMinutes: 95 } })
    const checkedOut = await checkinsApi.checkout(340)
    expect(mockPost).toHaveBeenCalledWith('/checkins/340/checkout')
    expect(checkedOut.durationMinutes).toBe(95)

    mockGet.mockResolvedValueOnce({
      data: { content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 },
    })
    await checkinsApi.history({
      date: '2026-11-21',
      status: 'CHECKED_IN',
      method: 'MANUAL',
      keyword: '  An  ',
      page: 1,
      size: 10,
    })
    expect(mockGet).toHaveBeenLastCalledWith('/checkins', {
      params: {
        date: '2026-11-21',
        status: 'CHECKED_IN',
        method: 'MANUAL',
        keyword: 'An',
        page: 1,
        size: 10,
      },
    })
  })
})
