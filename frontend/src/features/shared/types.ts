export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type Status = 'ACTIVE' | 'INACTIVE'

export interface PageQuery {
  page: number
  size: number
}
