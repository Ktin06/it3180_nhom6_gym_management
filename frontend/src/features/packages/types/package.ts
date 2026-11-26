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

/** Khớp PackageDetailDto trả về khi thêm hoặc sửa gói (UC-F3.2, UC-F3.3). */
export interface GymPackageDetail extends GymPackage {
  createdAt: string
  updatedAt: string
}

/** Dữ liệu gửi POST /api/v1/packages; gói TIME không gửi sessionCount. */
export interface PackageCreateInput {
  code: string
  name: string
  type: PackageType
  durationDays: number
  sessionCount?: number | null
  price: number
  description?: string | null
}

/** Dữ liệu gửi PUT /api/v1/packages/{id}; không gửi code hoặc type (BR-F3-05). */
export interface PackageUpdateInput {
  name?: string
  durationDays?: number
  sessionCount?: number | null
  price?: number
  description?: string | null
}

/** Khớp PackageDeactivationDto trả về khi ngừng bán gói (UC-F3.4). */
export interface PackageDeactivation {
  id: number
  code: string
  status: Status
  updatedAt: string
}

export interface PackageQuery extends PageQuery {
  type?: PackageType
  status?: Status
}

export interface PackagesGateway {
  list(query: PackageQuery): Promise<PageResponse<GymPackage>>
  create(input: PackageCreateInput): Promise<GymPackageDetail>
  update(id: number, input: PackageUpdateInput): Promise<GymPackageDetail>
  deactivate(id: number): Promise<PackageDeactivation>
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
