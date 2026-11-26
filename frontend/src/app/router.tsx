import type { ReactNode } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router-dom'
import { HOME_PATH_BY_ROLE, STAFF_ROLES } from '../auth/roles'
import { AuthLayout } from '../layouts/AuthLayout'
import { AuthenticatedShell } from '../layouts/AuthenticatedShell'
import { MemberLayout } from '../layouts/MemberLayout'
import { PublicLayout } from '../layouts/PublicLayout'
import { StaffLayout } from '../layouts/StaffLayout'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { GuestHomePage } from '../pages/home/GuestHomePage'
import { RoleHomePage } from '../pages/home/RoleHomePage'
import { ChangePasswordPage } from '../features/auth/pages/ChangePasswordPage'
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
import { CheckinPage } from '../features/checkins/pages/CheckinPage'
import { SellPackagePage } from '../features/packages/pages/SellPackagePage'
import { InvoicesPage } from '../features/invoices/pages/InvoicesPage'
import { InvoiceDetailPage } from '../features/invoices/pages/InvoiceDetailPage'
import { MemberClassesPage } from '../features/group-classes/pages/MemberClassesPage'
import { PublicClassesPage } from '../features/group-classes/pages/PublicClassesPage'
import { ReceptionClassBookingPage } from '../features/group-classes/pages/ReceptionClassBookingPage'
import { StaffClassesPage } from '../features/group-classes/pages/StaffClassesPage'
import { CheckinPage } from '../features/checkins/pages/CheckinPage'
// S2-20.2: giao diện huấn luyện viên (F9)
import { TrainerSchedulePage } from '../features/trainer/pages/TrainerSchedulePage'
import { TrainerStudentsPage } from '../features/trainer/pages/TrainerStudentsPage'
// S2-15.1: giao diện cổng hội viên (F7)
import { MemberPortalPage } from '../features/member-portal/pages/MemberPortalPage'
// S2-12.2: giao diện dashboard cơ bản (F6)
import { DashboardPage } from '../features/dashboard/pages/DashboardPage'

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
      { path: '/lich-lop', element: <PublicClassesPage /> },
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
      guarded('/hoa-don', <InvoicesPage />),
      guarded('/hoa-don/:id', <InvoiceDetailPage />),
      guarded('/dat-cho-lop', <ReceptionClassBookingPage />),
      guarded('/classes', <StaffClassesPage />),
      guarded('/checkins', <CheckinPage />),
      // S2-20.2: giao diện huấn luyện viên (F9)
      guarded('/pt/lich', <TrainerSchedulePage />),
      guarded('/pt/hoc-vien', <TrainerStudentsPage />),
      // S2-12.2: giao diện dashboard cơ bản (F6)
      guarded('/checkins', <CheckinPage />),
      guarded('/dashboard', <DashboardPage />),
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
      guarded('/member/classes', <MemberClassesPage />),
      // S2-15.1: giao diện cổng hội viên (F7)
      guarded('/member/memberships', <MemberPortalPage />),
      // Trang mới của hội viên: thêm một dòng guarded(...) ở đây.
      guarded('/member/classes', <MemberClassCalendarPage />),
    ],
  },
  {
    element: (
      <ProtectedRoute allowedRoles={rolesFor('/doi-mat-khau')}>
        <AuthenticatedShell />
      </ProtectedRoute>
    ),
    children: [guarded('/doi-mat-khau', <ChangePasswordPage />)],
  },
]

export const router = createBrowserRouter(routes)
