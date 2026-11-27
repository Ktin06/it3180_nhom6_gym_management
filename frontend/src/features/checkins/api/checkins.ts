import axios from 'axios'
import { http } from '../../../services/http'
import type { PageResponse } from '../../shared/types'
import type {
  CheckinGateway,
  CheckinHistoryItem,
  CheckinHistoryQuery,
  CheckinResult,
  CheckoutResult,
  MemberSearchResult,
} from '../types/checkin'

/** Gọi API F4 (check-in tại quầy, check-out, lịch sử) qua services/http.ts. */
export const checkinsApi: CheckinGateway = {
  async searchMember(keyword: string): Promise<MemberSearchResult> {
    const { data } = await http.get<MemberSearchResult>('/checkins/search-member', {
      params: { keyword: keyword.trim() },
    })
    return data
  },

  async checkin(memberId: number): Promise<CheckinResult> {
    const { data } = await http.post<CheckinResult>('/checkins', { memberId })
    return data
  },

  async checkout(checkinId: number): Promise<CheckoutResult> {
    const { data } = await http.post<CheckoutResult>(`/checkins/${checkinId}/checkout`)
    return data
  },

  async history(query: CheckinHistoryQuery): Promise<PageResponse<CheckinHistoryItem>> {
    const params: Record<string, string | number> = {
      page: query.page,
      size: query.size,
    }
    if (query.date) params.date = query.date
    if (query.status) params.status = query.status
    if (query.method) params.method = query.method
    const trimmedKeyword = query.keyword?.trim()
    if (trimmedKeyword) params.keyword = trimmedKeyword

    const { data } = await http.get<PageResponse<CheckinHistoryItem>>('/checkins', { params })
    return data
  },
}

/** Kiểm tra lỗi 409 Conflict (VD-20 Scenario 7: hai lễ tân check-in cùng lúc). */
export function isConflictError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 409
}
