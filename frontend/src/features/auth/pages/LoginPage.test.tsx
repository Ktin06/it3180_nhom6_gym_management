import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../../auth/AuthContext'
import { LoginPage } from './LoginPage'

const login = vi.fn()

function renderLogin() {
  return render(
    <AuthContext.Provider
      value={{
        status: 'anonymous',
        user: null,
        login,
        register: vi.fn(),
        logout: vi.fn(),
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
  beforeEach(() => login.mockReset())

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

  it('mở nội dung route state nội bộ sau khi xác thực', async () => {
    login.mockResolvedValue({ email: 'lt@example.com', role: 'RECEPTIONIST' })
    const user = userEvent.setup()
    render(
      <AuthContext.Provider
        value={{ status: 'anonymous', user: null, login, register: vi.fn(), logout: vi.fn() }}
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
        value={{ status: 'anonymous', user: null, login, register: vi.fn(), logout: vi.fn() }}
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
