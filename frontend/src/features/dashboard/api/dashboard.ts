import { http } from '../../../services/http'
import type {
  DashboardGateway,
  DashboardListParams,
  DashboardStats,
  ExpiringMembership,
  HourlyCheckin,
  LatestCheckin,
} from '../types/dashboard'

function buildParams(branchId?: number, limit?: number): Record<string, number> {
  const params: Record<string, number> = {}
  if (branchId !== undefined) params.branchId = branchId
  if (limit !== undefined) params.limit = limit
  return params
}

/** Gọi 4 API chỉ đọc của F6 (Dashboard cơ bản) qua services/http.ts. */
export const dashboardApi: DashboardGateway = {
  async stats(branchId?: number): Promise<DashboardStats> {
    const { data } = await http.get<DashboardStats>('/dashboard/stats', {
      params: buildParams(branchId),
    })
    return data
  },

  async hourlyCheckins(branchId?: number): Promise<HourlyCheckin[]> {
    const { data } = await http.get<HourlyCheckin[]>('/dashboard/hourly-checkins', {
      params: buildParams(branchId),
    })
    return data
  },

  async expiringMemberships(params?: DashboardListParams): Promise<ExpiringMembership[]> {
    const { data } = await http.get<ExpiringMembership[]>('/dashboard/expiring-memberships', {
      params: buildParams(params?.branchId, params?.limit),
    })
    return data
  },

  async latestCheckins(params?: DashboardListParams): Promise<LatestCheckin[]> {
    const { data } = await http.get<LatestCheckin[]>('/dashboard/latest-checkins', {
      params: buildParams(params?.branchId, params?.limit),
    })
    return data
  },
}
