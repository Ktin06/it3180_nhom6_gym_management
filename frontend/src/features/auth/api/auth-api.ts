import { queueCookieOperation } from '../../../auth/cookie-operation-queue'
import { ROLES, type Role } from '../../../auth/roles'
import type { AuthUser } from '../../../auth/AuthContext'
import { http } from '../../../services/http'
import type {
  AccessTokenResponse,
  AuthMeResponse,
  LoginRequest,
  RegisterRequest,
} from '../types/auth'

const publicAuthRequest = { skipBearer: true, skipAuthRefresh: true }

export function login(request: LoginRequest) {
  return queueCookieOperation(async () => {
    const response = await http.post<AccessTokenResponse>('/auth/login', request, {
      ...publicAuthRequest,
    })
    return response.data
  })
}

export function register(request: RegisterRequest) {
  return queueCookieOperation(async () => {
    const response = await http.post<AccessTokenResponse>('/auth/register', request, {
      ...publicAuthRequest,
    })
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

export function logout() {
  return queueCookieOperation(async () => {
    await http.post('/auth/logout', undefined, { ...publicAuthRequest })
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

function isRole(value: unknown): value is Role {
  return typeof value === 'string' && ROLES.some((role) => role === value)
}
