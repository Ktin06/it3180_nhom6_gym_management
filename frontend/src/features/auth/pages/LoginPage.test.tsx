import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthCompletionError, AuthContext } from '../../../auth/AuthContext'
import { LoginPage } from './LoginPage'

const login = vi.fn()
const retrySession = vi.fn()

function renderLogin() {
  return render(
    <AuthContext.Provider
      value={{
        status: 'anonymous',
        user: null,
        sessionError: null,
        login,
        register: vi.fn(),
        logout: vi.fn(),
        retrySession,
      }}
    >
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/member" element={<p>Trang hội viên</p>} />
          <Route path="/reception" element={<p>Trang lễ tân</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    login.mockReset()
    retrySession.mockReset()
  })

  it('đăng nhập bằng email đã chuẩn hóa rồi mở trang theo role', async () => {
    login.mockResolvedValue({ email: 'hv@example.com', role: 'MEMBER' })
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('Email'), 'HV@EXAMPLE.COM')
    await user.type(screen.getByLabelText('Mật khẩu'), 'Secret123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Trang hội viên')).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith({ email: 'hv@example.com', password: 'Secret123' })
  })

  it('sau khi đăng nhập thành công nhưng /me lỗi, retry identity mà không gửi lại mật khẩu', async () => {
    login.mockRejectedValue(new AuthCompletionError('login', 'Unable to load account.'))
    retrySession.mockResolvedValue({ email: 'hv@example.com', role: 'MEMBER' })
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('Email'), 'hv@example.com')
    await user.type(screen.getByLabelText('Mật khẩu'), 'Secret123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText(/đăng nhập đã được tiếp nhận/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Thử tải thông tin tài khoản' }))

    expect(await screen.findByText('Trang hội viên')).toBeInTheDocument()
    expect(retrySession).toHaveBeenCalledOnce()
    expect(login).toHaveBeenCalledOnce()
  })

  it('mở nội dung route state nội bộ sau khi xác thực', async () => {
    login.mockResolvedValue({ email: 'lt@example.com', role: 'RECEPTIONIST' })
    const user = userEvent.setup()
    render(
      <AuthContext.Provider
        value={{
          status: 'anonymous',
          user: null,
          sessionError: null,
          login,
          register: vi.fn(),
          logout: vi.fn(),
          retrySession: vi.fn(),
        }}
      >
        <MemoryRouter
          initialEntries={[{ pathname: '/login', state: { from: { pathname: '/reception' } } }]}
        >
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/reception" element={<p>Trang lễ tân</p>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    await user.type(screen.getByLabelText('Email'), 'lt@example.com')
    await user.type(screen.getByLabelText('Mật khẩu'), 'Secret123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Trang lễ tân')).toBeInTheDocument()
  })

  it('mở home role mới nếu return path trước đó thuộc hội viên', async () => {
    login.mockResolvedValue({ email: 'lt@example.com', role: 'RECEPTIONIST' })
    const user = userEvent.setup()
    render(
      <AuthContext.Provider
        value={{
          status: 'anonymous',
          user: null,
          sessionError: null,
          login,
          register: vi.fn(),
          logout: vi.fn(),
          retrySession: vi.fn(),
        }}
      >
        <MemoryRouter
          initialEntries={[{ pathname: '/login', state: { from: { pathname: '/member' } } }]}
        >
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/member" element={<p>Trang hội viên</p>} />
            <Route path="/reception" element={<p>Trang lễ tân</p>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    await user.type(screen.getByLabelText('Email'), 'lt@example.com')
    await user.type(screen.getByLabelText('Mật khẩu'), 'Secret123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Trang lễ tân')).toBeInTheDocument()
    expect(screen.queryByText('Trang hội viên')).not.toBeInTheDocument()
  })
})
