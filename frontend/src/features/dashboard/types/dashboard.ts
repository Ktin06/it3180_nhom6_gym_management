export type DashboardPackageType = 'TIME' | 'SESSION' | 'PT'
export type ExpiringWarningReason = 'EXPIRING_DATE' | 'LOW_SESSIONS'
export type DashboardCheckinMethod = 'MANUAL' | 'FACE'
export type DashboardCheckinStatus = 'CHECKED_IN' | 'CHECKED_OUT'

/** Hợp đồng JSON của GET /api/v1/dashboard/stats (S2-12.1). */
export interface DashboardStats {
  currentlyInGym: number
  todayCheckinCount: number
  todayRevenue: number
  cashRevenue: number
  transferRevenue: number
  expiringPackagesCount: number
}

/** Hợp đồng JSON của GET /api/v1/dashboard/hourly-checkins (S2-12.1). */
export interface HourlyCheckin {
  hour: string
  count: number
}

/** Hợp đồng JSON của GET /api/v1/dashboard/expiring-memberships (S2-12.1). */
export interface ExpiringMembership {
  membershipId: number
  memberId: number
  memberCode: string
  fullName: string
  phone: string
  hasAvatar: boolean
  packageName: string
  packageType: DashboardPackageType
  endDate: string
  daysRemaining: number
  remainingSessions: number | null
  warningReason: ExpiringWarningReason
}

/** Hợp đồng JSON của GET /api/v1/dashboard/latest-checkins (S2-12.1). */
export interface LatestCheckin {
  checkinId: number
  memberId: number
  memberCode: string
  fullName: string
  hasAvatar: boolean
  packageName: string
  packageType: DashboardPackageType
  method: DashboardCheckinMethod
  checkedInAt: string
  checkedOutAt: string | null
  durationMinutes: number | null
  status: DashboardCheckinStatus
}

export interface DashboardListParams {
  branchId?: number
  limit?: number
}

export interface BranchOption {
  id: number
  label: string
}

export interface DashboardGateway {
  stats(branchId?: number): Promise<DashboardStats>
  hourlyCheckins(branchId?: number): Promise<HourlyCheckin[]>
  expiringMemberships(params?: DashboardListParams): Promise<ExpiringMembership[]>
  latestCheckins(params?: DashboardListParams): Promise<LatestCheckin[]>
}
