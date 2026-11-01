import { createContext } from 'react'
import type { Role } from './roles'

export interface AuthUser {
  email: string
  role: Role
}

export interface AuthContextValue {
  user: AuthUser | null
  /** Chỉ dùng ở chế độ phát triển để giả lập đăng nhập. Sẽ thay bằng đăng nhập thật ở F1. */
  loginAs: (role: Role) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
