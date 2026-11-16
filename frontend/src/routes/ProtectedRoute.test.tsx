import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthUser } from '../auth/AuthContext'
import type { Role } from '../auth/roles'
import { ProtectedRoute } from './ProtectedRoute'

function renderAt(
  path: string,
  user: AuthUser | null,
  allowedRoles: Role[],
  status:
    | 'initializing'
    | 'authenticated'
    | 'anonymous'
    | 'recoverable-error' = user ? 'authenticated' : 'anonymous',
  retrySession = vi.fn(async () => user!),
) {
  return render(
    <AuthContext.Provider
      value={{
        status,
        user,
        sessionError:
          status === 'recoverable-error'
            ? 'Chưa thể xác minh phiên đăng nhập. Hãy kiểm tra kết nối rồi thử lại.'
            : null,
        login: async () => user!,
        register: async () => user!,
        logout: async () => {},
        retrySession,
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

  it('giữ nội dung được bảo vệ đóng khi lỗi phiên tạm thời và cung cấp retry', async () => {
    const retrySession = vi.fn(async () => ({ email: 'lt@demo.local', role: 'RECEPTIONIST' as const }))
    const user = userEvent.setup()
    renderAt('/reception', null, ['RECEPTIONIST'], 'recoverable-error', retrySession)

    expect(screen.getByRole('alert')).toHaveTextContent(/chưa thể xác minh phiên/i)
    expect(screen.getByRole('button', { name: /thử lại/i })).toBeInTheDocument()
    expect(screen.queryByText('Nội dung lễ tân')).not.toBeInTheDocument()
    expect(screen.queryByText('Trang đăng nhập')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /thử lại/i }))
    expect(retrySession).toHaveBeenCalledOnce()
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
