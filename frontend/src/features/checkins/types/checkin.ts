import type { PageResponse } from '../../shared/types'

export type CheckinPackageType = 'TIME' | 'SESSION' | 'PT'

export type CheckinMembershipStatus =
  | 'ACTIVE'
  | 'UPCOMING'
  | 'EXPIRED'
  | 'FROZEN'
  | 'CANCELLED'
  | 'PENDING'

export type CheckinMemberStatus = 'ACTIVE' | 'INACTIVE'

export type CheckinMethod = 'MANUAL' | 'FACE'

export type CheckinStatus = 'CHECKED_IN' | 'CHECKED_OUT'

/** Lớp học nhóm sắp diễn ra / vừa được đánh dấu đã đến (ClassArrivalDto). */
export interface ClassArrivalInfo {
  bookingId: number
  sessionId: number
  className: string
  roomName: string
  startTime: string
}

/** Một gói tập của hội viên trả về khi tra cứu tại quầy (MemberSearchDto.HeldPackage). */
export interface HeldPackage {
  id: number
  packageName: string
  packageType: CheckinPackageType
  branchId: number | null
  status: CheckinMembershipStatus
  startDate: string
  endDate: string
  totalSessions: number | null
  remainingSessions: number | null
}

/** Kết quả GET /api/v1/checkins/search-member?keyword= (MemberSearchDto). */
export interface MemberSearchResult {
  memberId: number
  memberCode: string
  fullName: string
  phone: string
  gender?: string | null
  hasAvatar: boolean
  status: CheckinMemberStatus
  isCurrentlyCheckedIn: boolean
  memberships: HeldPackage[]
  upcomingClassSession?: ClassArrivalInfo | null
}

/** Gói tập được hệ thống áp dụng khi check-in (CheckinResultDto.AppliedMembership). */
export interface AppliedMembership {
  id: number
  packageName?: string | null
  packageType: CheckinPackageType
  sessionDeducted: boolean
  remainingSessions?: number | null
}

/** Kết quả POST /api/v1/checkins (CheckinResultDto). */
export interface CheckinResult {
  checkinId: number
  memberId: number
  memberCode: string
  fullName: string
  appliedMembership: AppliedMembership
  checkinDate: string
  checkedInAt: string
  classArrivalMarked: boolean
  classArrivals: ClassArrivalInfo[]
  message: string
}

/** Kết quả POST /api/v1/checkins/{id}/checkout (CheckoutResultDto). */
export interface CheckoutResult {
  checkinId: number
  checkedOutAt: string
  durationMinutes: number
  message: string
}

/** Một dòng trong GET /api/v1/checkins (CheckinHistoryItemDto). */
export interface CheckinHistoryItem {
  checkinId: number
  memberId: number
  memberCode: string
  fullName: string
  hasAvatar: boolean
  membershipId: number
  packageName: string
  packageType: CheckinPackageType
  method: CheckinMethod
  sessionDeducted: boolean
  checkinDate: string
  checkedInAt: string
  checkedOutAt: string | null
  durationMinutes: number | null
  status: CheckinStatus
  createdBy: number
  createdByName: string | null
}

export interface CheckinHistoryQuery {
  date?: string
  status?: CheckinStatus
  method?: CheckinMethod
  keyword?: string
  page: number
  size: number
}

export interface CheckinGateway {
  searchMember(keyword: string): Promise<MemberSearchResult>
  checkin(memberId: number): Promise<CheckinResult>
  checkout(checkinId: number): Promise<CheckoutResult>
  history(query: CheckinHistoryQuery): Promise<PageResponse<CheckinHistoryItem>>
}
