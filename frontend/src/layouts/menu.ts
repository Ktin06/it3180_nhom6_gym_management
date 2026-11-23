import type { ReactNode } from 'react'
import type { Role } from '../auth/roles'
import { canOpen } from '../routes/route-access'

interface MenuItemBase {
  path: string
  label: string
  icon: ReactNode
}

/**
 * Mục menu. Khi `ready: true`, vai trò thấy mục được đọc từ ROUTE_ACCESS (routes/route-access.ts).
 * Khi chưa làm (`ready: false`), mục hiện mờ cho các vai trò ghi tạm trong `roles`;
 * lúc bật `ready: true` thì xóa `roles` (TypeScript sẽ báo nếu còn để).
 */
export type MenuItem = MenuItemBase & ({ ready: true } | { ready: false; roles: Role[] })

/** Các mục một vai trò được thấy. */
export function menuItemsForRole(items: MenuItem[], role: Role): MenuItem[] {
  return items.filter((item) => (item.ready ? canOpen(role, item.path) : item.roles.includes(role)))
}
