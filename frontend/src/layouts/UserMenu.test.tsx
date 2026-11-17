import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App } from 'antd'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../auth/AuthContext'
import { UserMenu } from './UserMenu'

const logout = vi.fn()

function renderUserMenu() {
  return render(
    <App>
      <AuthContext.Provider
        value={{
          status: 'authenticated',
          user: { email: 'lt@example.com', role: 'RECEPTIONIST' },
          sessionError: null,
          login: vi.fn(),
          register: vi.fn(),
          logout,
          retrySession: vi.fn(),
        }}
      >
        <MemoryRouter initialEntries={['/reception']}>
          <Routes>
            <Route
              path="/reception"
              element={
                <>
                  <header style={{ width: 360, height: 64 }}>
                    <UserMenu />
                  </header>
                  <p>Nội dung lễ tân</p>
                </>
              }
            />
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
    expect(screen.getByRole('button', { name: 'Thử đăng xuất lại' })).toHaveTextContent('Thử lại')
    expect(screen.queryByText('Trang đăng nhập')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Thử đăng xuất lại' }))

    expect(await screen.findByText('Trang đăng nhập')).toBeInTheDocument()
    expect(logout).toHaveBeenCalledTimes(2)
  })
})
