import type { Role } from '../../../auth/roles'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  phone: string
  password: string
  dob?: string
  /** Chưa gửi từ giao diện đến khi backend chốt tập giá trị giới tính. */
  gender?: string
}

/** DTO tạm: SRS xác nhận trường accessToken, backend chưa công bố JSON response cụ thể. */
export interface AccessTokenResponse {
  accessToken: string
}

/** DTO tạm cho GET /auth/me; chỉ lấy email/role đang cần cho AuthUser và menu. */
export interface AuthMeResponse {
  email: string
  role: Role
}

export interface ProblemDetailResponse {
  title?: string
  detail?: string
  errors?: Record<string, string>
}
