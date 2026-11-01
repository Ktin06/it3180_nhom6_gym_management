import { createBrowserRouter, type RouteObject } from 'react-router-dom'
import { HOME_PATH_BY_ROLE, STAFF_ROLES } from '../auth/roles'
import { AuthLayout } from '../layouts/AuthLayout'
import { MemberLayout } from '../layouts/MemberLayout'
import { PublicLayout } from '../layouts/PublicLayout'
import { StaffLayout } from '../layouts/StaffLayout'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { GuestHomePage } from '../pages/home/GuestHomePage'
import { RoleHomePage } from '../pages/home/RoleHomePage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProtectedRoute } from '../routes/ProtectedRoute'

export const routes: RouteObject[] = [
  {
    element: <PublicLayout />,
    children: [
      { path: '/', element: <GuestHomePage /> },
      { path: '/403', element: <ForbiddenPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    element: <AuthLayout />,
    children: [{ path: '/login', element: <LoginPage /> }],
  },
  {
    element: (
      <ProtectedRoute allowedRoles={STAFF_ROLES}>
        <StaffLayout />
      </ProtectedRoute>
    ),
    children: STAFF_ROLES.map((role) => ({
      path: HOME_PATH_BY_ROLE[role],
      element: (
        <ProtectedRoute allowedRoles={[role]}>
          <RoleHomePage role={role} />
        </ProtectedRoute>
      ),
    })),
  },
  {
    element: (
      <ProtectedRoute allowedRoles={['MEMBER']}>
        <MemberLayout />
      </ProtectedRoute>
    ),
    children: [{ path: HOME_PATH_BY_ROLE.MEMBER, element: <RoleHomePage role="MEMBER" /> }],
  },
]

export const router = createBrowserRouter(routes)
