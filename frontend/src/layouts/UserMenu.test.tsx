import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App } from 'antd'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Role } from '../auth/roles'
import { AuthContext } from '../auth/AuthContext'
import { UserMenu } from './UserMenu'

const logout = vi.fn()

function renderUserMenu(role: Role = 'RECEPTIONIST', initialPath = '/reception') {
  return render(
    <App>
      <AuthContext.Provider
        value={{
          status: 'authenticated',
          user: { email: 'user@example.com', role },
          sessionError: null,
          login: vi.fn(),
          register: vi.fn(),
          logout,
          retrySession: vi.fn(),
          completePasswordChange: vi.fn(),
        }}
      >
        <MemoryRouter initialEntries={[initialPath]}>
          <Routes>
            <Route
              path={initialPath}
              element={
                <>
                  <header style={{ width: 360, height: 64 }}>
                    <UserMenu />
                  </header>
                  <p>Nội dung lễ tân</p>
                </>
              }
            />
            <Route path="/doi-mat-khau" element={<p>Trang đổi mật khẩu</p>} />
            <Route path="/login" element={<p>Trang đăng nhập</p>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    </App>,
  )
}

describe('UserMenu', () => {
  beforeEach(() => logout.mockReset())

  it('giữ người dùng ở lại sau logout lỗi và chỉ điều hướng khi retry thành công', async () => {
    logout.mockRejectedValueOnce(new Error('Network Error')).mockResolvedValueOnce(undefined)
    const user = userEvent.setup()
    renderUserMenu()

    await user.click(screen.getByRole('button', { name: 'Đăng xuất' }))

    const warning = await screen.findByText(/đăng xuất chưa hoàn tất/i)
    expect(warning).toBeInTheDocument()
    expect(screen.getByRole('banner')).not.toContainElement(warning)
    expect(screen.getByText('Nội dung lễ tân')).toBeInTheDocument()
    const retryButton = screen.getByRole('button', { name: 'Thử đăng xuất lại' })
    expect(retryButton).toHaveTextContent('Thử lại')
    // Màn 360px vẫn thấy chữ "Thử lại": nhãn không bị ẩn như lúc bình thường
    const retryLabel = Array.from(retryButton.querySelectorAll('span')).find((el) => el.textContent === 'Thử lại')
    expect(retryLabel).not.toHaveClass('hide-on-mobile')
    expect(screen.queryByText('Trang đăng nhập')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Thử đăng xuất lại' }))

    expect(await screen.findByText('Trang đăng nhập')).toBeInTheDocument()
    expect(logout).toHaveBeenCalledTimes(2)
  })

  describe('TC-F1.4.1-04c: UserMenu có mục Đổi mật khẩu cho mọi vai trò, mở /doi-mat-khau', () => {
    it.each([
      { role: 'RECEPTIONIST' as const, path: '/reception' },
      { role: 'MEMBER' as const, path: '/member' },
    ])(
      'UserMenu có mục Đổi mật khẩu cho $role, mở /doi-mat-khau',
      async ({ role, path }) => {
        const user = userEvent.setup()
        renderUserMenu(role, path)

        const changePasswordButton = screen.getByRole('button', { name: 'Đổi mật khẩu' })
        expect(changePasswordButton).toBeInTheDocument()

        await user.click(changePasswordButton)

        expect(await screen.findByText('Trang đổi mật khẩu')).toBeInTheDocument()
      },
    )
  })

  describe('TC-F1.4.1-04d: ẩn chữ của nút trên màn hẹp 360px, giữ tên nút qua aria-label', () => {
    it.each(['Đổi mật khẩu', 'Đăng xuất'])('ẩn chữ của nút "%s" trên màn hẹp, giữ tên nút', (name) => {
      renderUserMenu('MEMBER', '/member')

      const button = screen.getByRole('button', { name })
      const label = Array.from(button.querySelectorAll('span')).find((el) => el.textContent === name)
      expect(label).toBeDefined()
      expect(label).toHaveClass('hide-on-mobile')
    })
  })
})
