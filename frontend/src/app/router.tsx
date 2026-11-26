import type { ReactNode } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router-dom'
import { HOME_PATH_BY_ROLE, STAFF_ROLES } from '../auth/roles'
import { AuthLayout } from '../layouts/AuthLayout'
import { MemberLayout } from '../layouts/MemberLayout'
import { PublicLayout } from '../layouts/PublicLayout'
import { StaffLayout } from '../layouts/StaffLayout'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { GuestHomePage } from '../pages/home/GuestHomePage'
import { RoleHomePage } from '../pages/home/RoleHomePage'
import { ForgotPasswordPage } from '../features/auth/pages/ForgotPasswordPage'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { RegisterPage } from '../features/auth/pages/RegisterPage'
import { ResetPasswordPage } from '../features/auth/pages/ResetPasswordPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { rolesFor, type ProtectedPath } from '../routes/route-access'
import { MembersPage } from '../features/members/pages/MembersPage'
import { PackagesPage } from '../features/packages/pages/PackagesPage'
import {
  MemberClassCalendarPage,
  PublicClassCalendarPage,
  TrainerClassCalendarPage,
} from '../features/classes/ClassCalendarPages'
import { TrainerAttendancePage } from '../features/classes/TrainerAttendancePage'
import { PublicPricingPage } from '../features/packages/pages/PublicPricingPage'
import { SellPackagePage } from '../features/packages/pages/SellPackagePage'

/** Route cần đăng nhập; vai trò được vào đọc từ ROUTE_ACCESS (routes/route-access.ts). */
function guarded(path: ProtectedPath, element: ReactNode): RouteObject {
  return {
    path,
    element: <ProtectedRoute allowedRoles={rolesFor(path)}>{element}</ProtectedRoute>,
  }
}

export const routes: RouteObject[] = [
  {
    element: <PublicLayout />,
    children: [
      { path: '/', element: <GuestHomePage /> },
      { path: '/bang-gia', element: <PublicPricingPage /> },
      { path: '/lich-lop', element: <PublicClassCalendarPage /> },
      { path: '/403', element: <ForbiddenPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/quen-mat-khau', element: <ForgotPasswordPage /> },
      { path: '/dat-lai-mat-khau', element: <ResetPasswordPage /> },
    ],
  },
  {
    element: (
      <ProtectedRoute allowedRoles={STAFF_ROLES}>
        <StaffLayout />
      </ProtectedRoute>
    ),
    children: [
      ...STAFF_ROLES.map((role) => guarded(HOME_PATH_BY_ROLE[role], <RoleHomePage role={role} />)),
      guarded('/members', <MembersPage />),
      guarded('/packages', <PackagesPage />),
      guarded('/trainer/classes', <TrainerClassCalendarPage />),
      guarded('/trainer/classes/:sessionId/attendance', <TrainerAttendancePage />),
      guarded('/ban-goi', <SellPackagePage />),
      // Trang mới của nhân viên: thêm một dòng guarded(...) ở đây.
    ],
  },
  {
    element: (
      <ProtectedRoute allowedRoles={['MEMBER']}>
        <MemberLayout />
      </ProtectedRoute>
    ),
    children: [
      guarded(HOME_PATH_BY_ROLE.MEMBER, <RoleHomePage role="MEMBER" />),
      guarded('/member/classes', <MemberClassCalendarPage />),
    ],
  },
]

export const router = createBrowserRouter(routes)
