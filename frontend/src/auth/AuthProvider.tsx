import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { AuthContext, type AuthUser } from './AuthContext'
import { ROLES, type Role } from './roles'

// TẠM THỜI: trạng thái đăng nhập giả lập để thử menu và phân quyền giao diện.
// Sẽ thay bằng đăng nhập thật (gọi API /api/v1/auth, lưu access token) ở F1.
const STORAGE_KEY = 'gym.dev-role'

function readStoredRole(): Role | null {
  try {
    const value = sessionStorage.getItem(STORAGE_KEY)
    return ROLES.find((role) => role === value) ?? null
  } catch {
    return null
  }
}

function toUser(role: Role | null): AuthUser | null {
  return role ? { email: `${role.toLowerCase()}@demo.local`, role } : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() =>
    import.meta.env.DEV ? toUser(readStoredRole()) : null,
  )

  const loginAs = useCallback((role: Role) => {
    try {
      sessionStorage.setItem(STORAGE_KEY, role)
    } catch {
      // Trình duyệt chặn bộ nhớ phiên thì chỉ giữ trong bộ nhớ.
    }
    setUser(toUser(role))
  }, [])

  const logout = useCallback(() => {
    try {
      sessionStorage.removeItem(STORAGE_KEY)
    } catch {
      // Bỏ qua.
    }
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, loginAs, logout }), [user, loginAs, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
