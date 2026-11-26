import { queueCookieOperation } from '../../../auth/cookie-operation-queue'
import {
  captureCookieRevision,
  currentTabCookieRevision,
  publishSessionChange,
} from '../../../auth/session-coordination'
import { getSessionEpoch } from '../../../auth/session-store'
import { ROLES, type Role } from '../../../auth/roles'
import type { AuthUser } from '../../../auth/AuthContext'
import { http } from '../../../services/http'
import { publicRequest } from '../../../services/request-options'
import type {
  AccessTokenResponse,
  AuthMeResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from '../types/auth'

const publicAuthRequest = publicRequest

export function login(request: LoginRequest) {
  return queueCookieOperation(async () => {
    await captureCookieRevision()
    const response = await http.post<AccessTokenResponse>('/auth/login', request, {
      ...publicAuthRequest,
    })
    await publishSessionChange(response.data.accessToken)
    return response.data
  })
}

export function register(request: RegisterRequest) {
  return queueCookieOperation(async () => {
    await captureCookieRevision()
    const response = await http.post<AccessTokenResponse>('/auth/register', request, {
      ...publicAuthRequest,
    })
    await publishSessionChange(response.data.accessToken)
    return response.data
  })
}

export async function getCurrentUser(): Promise<AuthUser> {
  const response = await http.get<AuthMeResponse>('/auth/me')
  const { userId, email, role } = response.data
  if (
    typeof userId !== 'number' ||
    !Number.isInteger(userId) ||
    userId <= 0 ||
    typeof email !== 'string' ||
    !isRole(role)
  ) {
    throw new Error('Thông tin tài khoản từ máy chủ không hợp lệ.')
  }
  return { email, role }
}

export async function logout() {
  const authEpoch = getSessionEpoch()
  await captureCookieRevision()
  return queueCookieOperation(async () => {
    if (authEpoch !== getSessionEpoch()) throw new Error('Phiên đăng nhập đã thay đổi.')
    if (!(await currentTabCookieRevision(authEpoch))) return
    // Bearer cho backend đối chiếu chủ cookie; không refresh sang tài khoản ở tab khác.
    await http.post('/auth/logout', undefined, { skipAuthRefresh: true, authEpoch })
    await publishSessionChange(null)
  })
}

export async function forgotPassword(
  request: ForgotPasswordRequest,
): Promise<ForgotPasswordResponse> {
  const response = await http.post<ForgotPasswordResponse>('/auth/forgot-password', request, {
    ...publicAuthRequest,
  })
  return response.data
}

export async function resetPassword(request: ResetPasswordRequest): Promise<void> {
  await http.post('/auth/reset-password', request, {
    ...publicAuthRequest,
  })
}

export function getProblemDetail(error: unknown) {
  const response = (error as { response?: { data?: unknown; status?: unknown } } | null)?.response
  const data = response?.data as { detail?: unknown; errors?: unknown } | undefined
  const detail = typeof data?.detail === 'string' ? data.detail : null
  const errors: Record<string, string> = {}
  if (data?.errors && typeof data.errors === 'object') {
    Object.entries(data.errors).forEach(([name, value]) => {
      if (typeof value === 'string') errors[name] = value
    })
  }
  return { detail, errors, status: typeof response?.status === 'number' ? response.status : null }
}

export async function changePassword(request: ChangePasswordRequest, authEpoch: number): Promise<void> {
  await http.post('/auth/change-password', request, { authEpoch })
}

function isRole(value: unknown): value is Role {
  return typeof value === 'string' && ROLES.some((role) => role === value)
}
