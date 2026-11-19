import { createBrowserRouter, type RouteObject } from 'react-router-dom'
import { HOME_PATH_BY_ROLE, MEMBER_MANAGEMENT_ROLES, STAFF_ROLES } from '../auth/roles'
import { AuthLayout } from '../layouts/AuthLayout'
import { MemberLayout } from '../layouts/MemberLayout'
import { PublicLayout } from '../layouts/PublicLayout'
import { StaffLayout } from '../layouts/StaffLayout'
import { ForbiddenPage } from '../pages/ForbiddenPage'
import { GuestHomePage } from '../pages/home/GuestHomePage'
import { RoleHomePage } from '../pages/home/RoleHomePage'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { RegisterPage } from '../features/auth/pages/RegisterPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { MembersPage } from '../features/members/pages/MembersPage'
import { PackagesPage } from '../features/packages/pages/PackagesPage'
import { PublicPricingPage } from '../features/packages/pages/PublicPricingPage'

export const routes: RouteObject[] = [
  {
    element: <PublicLayout />,
    children: [
      { path: '/', element: <GuestHomePage /> },
      { path: '/bang-gia', element: <PublicPricingPage /> },
      { path: '/403', element: <ForbiddenPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
    ],
  },
  {
    element: (
      <ProtectedRoute allowedRoles={STAFF_ROLES}>
        <StaffLayout />
      </ProtectedRoute>
    ),
    children: [
      ...STAFF_ROLES.map((role) => ({
        path: HOME_PATH_BY_ROLE[role],
        element: (
          <ProtectedRoute allowedRoles={[role]}>
            <RoleHomePage role={role} />
          </ProtectedRoute>
        ),
      })),
      {
        path: '/members',
        element: (
          <ProtectedRoute allowedRoles={MEMBER_MANAGEMENT_ROLES}>
            <MembersPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/packages',
        element: (
          <ProtectedRoute allowedRoles={MEMBER_MANAGEMENT_ROLES}>
            <PackagesPage />
          </ProtectedRoute>
        ),
      },
    ],
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
