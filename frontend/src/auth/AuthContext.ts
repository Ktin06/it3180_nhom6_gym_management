import { createContext } from 'react'
import type { Role } from './roles'
import type { LoginRequest, RegisterRequest } from '../features/auth/types/auth'

export interface AuthUser {
  email: string
  role: Role
}

export type AuthStatus =
  | 'initializing'
  | 'authenticated'
  | 'anonymous'
  | 'recoverable-error'

/** Yêu cầu đăng nhập/đăng ký thành công nhưng client chưa tải được danh tính tài khoản. */
export class AuthCompletionError extends Error {
  readonly action: 'login' | 'register'

  constructor(
    action: 'login' | 'register',
    cause: unknown,
  ) {
    super(
      action === 'register'
        ? 'Tài khoản đã được tạo nhưng chưa tải được thông tin tài khoản.'
        : 'Đăng nhập đã được tiếp nhận nhưng chưa tải được thông tin tài khoản.',
      { cause },
    )
    this.name = 'AuthCompletionError'
    this.action = action
  }
}

export interface AuthContextValue {
  status: AuthStatus
  user: AuthUser | null
  sessionError: string | null
  login: (request: LoginRequest) => Promise<AuthUser>
  register: (request: RegisterRequest) => Promise<AuthUser>
  logout: () => Promise<void>
  retrySession: () => Promise<AuthUser>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
