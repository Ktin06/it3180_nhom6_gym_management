import { createContext } from 'react'
import type { Role } from './roles'
import type { LoginRequest, RegisterRequest } from '../features/auth/types/auth'

export interface AuthUser {
  email: string
  role: Role
}

export interface AuthContextValue {
  status: 'initializing' | 'authenticated' | 'anonymous'
  user: AuthUser | null
  login: (request: LoginRequest) => Promise<AuthUser>
  register: (request: RegisterRequest) => Promise<AuthUser>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
