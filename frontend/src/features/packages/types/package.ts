import type { PageQuery, PageResponse, Status } from '../../shared/types'

export type PackageType = 'TIME' | 'SESSION' | 'PT'

/** Khớp PackageSummaryDto của API F3 đã merge qua #107. */
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

/** Một gói trên bảng giá công khai; khớp PublicPackageDto (US-F3.1.2). */
export interface PublicPackage {
  id: number
  name: string
  type: PackageType
  durationDays: number
  sessionCount?: number | null
  price: number
  description?: string | null
}

export interface PublicPackagesGateway {
  list(): Promise<PublicPackage[]>
}
