import { canOpen, ROUTE_ACCESS, type RouteAccessTable } from '../routes/route-access'
import { HOME_PATH_BY_ROLE, type Role } from './roles'

/** Chỉ dùng đường dẫn cùng origin được ProtectedRoute lưu vào history state. */
export function safeReturnPath(state: unknown, origin: string) {
  if (!state || typeof state !== 'object' || !('from' in state)) return null
  const from = state.from
  if (!from || typeof from !== 'object' || !('pathname' in from)) return null
  const location = from as { pathname?: unknown; search?: unknown; hash?: unknown }
  if (typeof location.pathname !== 'string') return null

  const path = `${location.pathname}${typeof location.search === 'string' ? location.search : ''}${typeof location.hash === 'string' ? location.hash : ''}`
  if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\')) return null
  try {
    const url = new URL(path, origin)
    return url.origin === origin ? `${url.pathname}${url.search}${url.hash}` : null
  } catch {
    return null
  }
}

/**
 * Giữ đường dẫn cũ nếu role mới đăng nhập được mở nó theo ROUTE_ACCESS; còn lại về trang chủ của role.
 * `access` chỉ truyền khi kiểm thử; mã thật dùng bảng mặc định.
 */
export function roleAwareReturnPath(
  state: unknown,
  origin: string,
  role: Role,
  access: RouteAccessTable = ROUTE_ACCESS,
) {
  const roleHome = HOME_PATH_BY_ROLE[role]
  const returnPath = safeReturnPath(state, origin)
  if (!returnPath) return roleHome

  const pathname = new URL(returnPath, origin).pathname
  return canOpen(role, pathname, access) ? returnPath : roleHome
}
