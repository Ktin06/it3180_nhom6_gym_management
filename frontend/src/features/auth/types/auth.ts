import type { Role } from '../../../auth/roles'

export interface LoginRequest {
  email: string
  password: string
}

export type Gender = 'MALE' | 'FEMALE' | 'OTHER'

export interface RegisterRequest {
  fullName: string
  email: string
  phone: string
  password: string
  dob?: string
  gender?: Gender | null
}

/** Hợp đồng F1 mục 6.1: login/register/refresh trả JSON phẳng. */
export interface AccessTokenResponse {
  accessToken: string
}

/** GET /api/v1/auth/me; AuthUser/menu chỉ sử dụng email và role. */
export interface AuthMeResponse {
  userId: number
  email: string
  role: Role
}

export interface ProblemDetailResponse {
  title?: string
  detail?: string
  errors?: Record<string, string>
}
