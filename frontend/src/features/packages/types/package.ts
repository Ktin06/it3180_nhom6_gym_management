import type { PageQuery, PageResponse, Status } from '../../shared/types'

export type PackageType = 'TIME' | 'SESSION' | 'PT'

/** Hợp đồng dự kiến từ đặc tả F3; cần đối chiếu lại với backend #29 khi bàn giao. */
export interface GymPackage {
  id: number
  code: string
  name: string
  type: PackageType
  durationDays: number
  sessionCount: number | null
  price: number
  description: string | null
  status: Status
  branchId: number | null
}

export interface PackageQuery extends PageQuery {
  type?: PackageType
  status?: Status
}

export interface PackagesGateway {
  list(query: PackageQuery): Promise<PageResponse<GymPackage>>
}
