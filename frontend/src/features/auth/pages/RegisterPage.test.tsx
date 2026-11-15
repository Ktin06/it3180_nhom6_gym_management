import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthCompletionError, AuthContext } from '../../../auth/AuthContext'
import { RegisterPage } from './RegisterPage'

const register = vi.fn()
const retrySession = vi.fn()

function renderRegister() {
  return render(
    <AuthContext.Provider
      value={{
        status: 'anonymous',
        user: null,
        sessionError: null,
        login: vi.fn(),
        register,
        logout: vi.fn(),
        retrySession,
      }}
    >
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/member" element={<p>Trang hội viên</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('RegisterPage', () => {
  beforeEach(() => {
    register.mockReset()
    retrySession.mockReset()
  })

  it('chuẩn hóa dữ liệu, giữ họ tên tiếng Việt và tự đăng nhập hội viên', async () => {
    register.mockResolvedValue({ email: 'anh@example.com', role: 'MEMBER' })
    const user = userEvent.setup()
    renderRegister()

    await user.type(screen.getByLabelText('Họ và tên'), '  Đặng Thị Ánh  ')
    await user.type(screen.getByLabelText('Email'), 'ANH@EXAMPLE.COM')
    await user.type(screen.getByLabelText('Số điện thoại'), '+84 912 345 678')
    await user.type(screen.getByLabelText('Mật khẩu'), 'ĐườngGym123')
    await user.type(screen.getByLabelText('Nhập lại mật khẩu'), 'ĐườngGym123')
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(await screen.findByText('Trang hội viên')).toBeInTheDocument()
    expect(register).toHaveBeenCalledWith({
      fullName: 'Đặng Thị Ánh',
      email: 'anh@example.com',
      phone: '0912345678',
      password: 'ĐườngGym123',
    })
  })

  it('không gửi dữ liệu khi số điện thoại hoặc mật khẩu không đạt quy tắc', async () => {
    const user = userEvent.setup()
    renderRegister()

    await user.type(screen.getByLabelText('Họ và tên'), 'Nguyễn An')
    await user.type(screen.getByLabelText('Email'), 'an@example.com')
    await user.type(screen.getByLabelText('Số điện thoại'), '123')
    await user.type(screen.getByLabelText('Mật khẩu'), 'short')
    await user.type(screen.getByLabelText('Nhập lại mật khẩu'), 'short')
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(await screen.findByText('Nhập số di động 10 số, ví dụ 0912345678')).toBeInTheDocument()
    expect(
      await screen.findByText('Mật khẩu cần ít nhất 8 ký tự, gồm chữ và số'),
    ).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
  })

  it('nói rõ tài khoản đã tạo và retry /me thay vì gửi lại đăng ký', async () => {
    register.mockRejectedValue(new AuthCompletionError('register', 'Unable to load account.'))
    retrySession.mockResolvedValue({ email: 'anh@example.com', role: 'MEMBER' })
    const user = userEvent.setup()
    renderRegister()

    await user.type(screen.getByLabelText('Họ và tên'), 'Đặng Thị Ánh')
    await user.type(screen.getByLabelText('Email'), 'anh@example.com')
    await user.type(screen.getByLabelText('Số điện thoại'), '0912345678')
    await user.type(screen.getByLabelText('Mật khẩu'), 'DuongGym123')
    await user.type(screen.getByLabelText('Nhập lại mật khẩu'), 'DuongGym123')
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(await screen.findByText(/tài khoản đã được tạo/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(register).toHaveBeenCalledTimes(1)

    await user.click(screen.getByRole('button', { name: /thử tải thông tin tài khoản/i }))

    expect(await screen.findByText('Trang hội viên')).toBeInTheDocument()
    expect(retrySession).toHaveBeenCalledOnce()
    expect(register).toHaveBeenCalledTimes(1)
  })
})
