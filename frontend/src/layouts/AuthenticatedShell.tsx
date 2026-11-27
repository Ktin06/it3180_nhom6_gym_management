import { useAuth } from '../auth/useAuth'
import { MemberLayout } from './MemberLayout'
import { StaffLayout } from './StaffLayout'

/** Chọn khung giao diện nhân viên hoặc hội viên cho trang dùng chung mọi vai trò. */
export function AuthenticatedShell() {
  const { user } = useAuth()
  return user?.role === 'MEMBER' ? <MemberLayout /> : <StaffLayout />
}
