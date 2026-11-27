import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthCompletionError, AuthContext } from '../../../auth/AuthContext'
import { LoginPage } from './LoginPage'

const login = vi.fn()
const retrySession = vi.fn()

function renderLogin(sessionError: string | null = null) {
  return render(
    <AuthContext.Provider
      value={{
        status: 'anonymous',
        user: null,
        sessionError,
        login,
        register: vi.fn(),
        logout: vi.fn(),
        retrySession,
        completePasswordChange: vi.fn(),
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

  it('hiện thông báo tài khoản đổi ở tab khác tại trang đăng nhập', () => {
    renderLogin('Tài khoản đã đổi ở tab khác. Vui lòng đăng nhập lại.')
    expect(screen.getByRole('alert')).toHaveTextContent('Tài khoản đã đổi ở tab khác')
  })

  it('TC-F1.2.1-04g: đăng nhập bằng email đã chuẩn hóa rồi mở trang theo role', async () => {
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
          completePasswordChange: vi.fn(),
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

  it('ẩn retry identity cũ khi bắt đầu lần đăng nhập mới bị sai mật khẩu', async () => {
    login
      .mockRejectedValueOnce(new AuthCompletionError('login', 'Unable to load account.'))
      .mockRejectedValueOnce({ response: { status: 401, data: { detail: 'Sai mật khẩu.' } } })
    const user = userEvent.setup()
    renderLogin()
    await user.type(screen.getByLabelText('Email'), 'hv@example.com')
    await user.type(screen.getByLabelText('Mật khẩu'), 'Secret123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))
    expect(
      await screen.findByRole('button', { name: 'Thử tải thông tin tài khoản' }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))
    expect(await screen.findByText('Sai mật khẩu.')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Thử tải thông tin tài khoản' }),
    ).not.toBeInTheDocument()
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
          completePasswordChange: vi.fn(),
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

  it('TC-F1.5.1-01d: LoginPage có liên kết Quên mật khẩu? tới /quen-mat-khau và hiện thông báo đặt mật khẩu thành công', async () => {
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
          completePasswordChange: vi.fn(),
        }}
      >
        <MemoryRouter
          initialEntries={[
            {
              pathname: '/login',
              state: { notice: 'Đặt mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.' },
            },
          ]}
        >
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/quen-mat-khau" element={<p>Trang quên mật khẩu</p>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Đặt mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.',
    )

    const forgotLink = screen.getByRole('link', { name: 'Quên mật khẩu?' })
    expect(forgotLink).toHaveAttribute('href', '/quen-mat-khau')

    await user.click(forgotLink)
    expect(await screen.findByText('Trang quên mật khẩu')).toBeInTheDocument()
  })
})
