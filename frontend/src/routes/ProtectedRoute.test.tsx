import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AuthContext, type AuthUser } from '../auth/AuthContext'
import type { Role } from '../auth/roles'
import { ProtectedRoute } from './ProtectedRoute'

function renderAt(
  path: string,
  user: AuthUser | null,
  allowedRoles: Role[],
  status: 'initializing' | 'authenticated' | 'anonymous' = user ? 'authenticated' : 'anonymous',
) {
  return render(
    <AuthContext.Provider
      value={{
        status,
        user,
        login: async () => user!,
        register: async () => user!,
        logout: async () => {},
      }}
    >
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<p>Trang đăng nhập</p>} />
          <Route path="/403" element={<p>Không có quyền</p>} />
          <Route
            path="/reception"
            element={
              <ProtectedRoute allowedRoles={allowedRoles}>
                <p>Nội dung lễ tân</p>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('ProtectedRoute', () => {
  it('chuyển về /login khi chưa đăng nhập', () => {
    renderAt('/reception', null, ['RECEPTIONIST'])

    expect(screen.getByText('Trang đăng nhập')).toBeInTheDocument()
    expect(screen.queryByText('Nội dung lễ tân')).not.toBeInTheDocument()
  })

  it('đợi bootstrap phiên trước khi chuyển hướng', () => {
    renderAt('/reception', null, ['RECEPTIONIST'], 'initializing')

    expect(screen.getByRole('status')).toHaveTextContent('Đang kiểm tra phiên đăng nhập')
    expect(screen.queryByText('Trang đăng nhập')).not.toBeInTheDocument()
  })

  it('chuyển về /403 khi sai vai trò', () => {
    renderAt('/reception', { email: 'hv@demo.local', role: 'MEMBER' }, ['RECEPTIONIST'])

    expect(screen.getByText('Không có quyền')).toBeInTheDocument()
    expect(screen.queryByText('Nội dung lễ tân')).not.toBeInTheDocument()
  })

  it('hiển thị nội dung khi đúng vai trò', () => {
    renderAt('/reception', { email: 'lt@demo.local', role: 'RECEPTIONIST' }, ['RECEPTIONIST'])

    expect(screen.getByText('Nội dung lễ tân')).toBeInTheDocument()
  })
})
