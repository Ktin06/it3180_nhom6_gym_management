import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { dashboardApi } from './dashboard'

vi.mock('../../../services/http', () => ({
  http: {
    get: vi.fn(),
  },
}))

const mockedGet = vi.mocked(http.get)

describe('dashboardApi (F6)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('US-F6 API: gọi đúng 4 endpoint /dashboard/* qua http.ts, kèm branchId và limit khi có', async () => {
    mockedGet
      .mockResolvedValueOnce({
        data: {
          currentlyInGym: 3,
          todayCheckinCount: 10,
          todayRevenue: 500000,
          cashRevenue: 200000,
          transferRevenue: 300000,
          expiringPackagesCount: 2,
        },
      })
      .mockResolvedValueOnce({
        data: [{ hour: '06:00', count: 1 }],
      })
      .mockResolvedValueOnce({
        data: [],
      })
      .mockResolvedValueOnce({
        data: [],
      })

    await dashboardApi.stats()
    await dashboardApi.hourlyCheckins(2)
    await dashboardApi.expiringMemberships({ branchId: 2, limit: 15 })
    await dashboardApi.latestCheckins({ limit: 8 })

    expect(mockedGet).toHaveBeenNthCalledWith(1, '/dashboard/stats', { params: {} })
    expect(mockedGet).toHaveBeenNthCalledWith(2, '/dashboard/hourly-checkins', {
      params: { branchId: 2 },
    })
    expect(mockedGet).toHaveBeenNthCalledWith(3, '/dashboard/expiring-memberships', {
      params: { branchId: 2, limit: 15 },
    })
    expect(mockedGet).toHaveBeenNthCalledWith(4, '/dashboard/latest-checkins', {
      params: { limit: 8 },
    })
  })
})
