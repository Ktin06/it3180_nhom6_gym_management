import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const authApi = vi.hoisted(() => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
}))
const refresh = vi.hoisted(() => vi.fn())

vi.mock('../features/auth/api/auth-api', () => authApi)
vi.mock('../services/http', () => ({ refreshAccessToken: refresh }))

import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'
import {
  beginSession,
  clearSession,
  getAccessToken,
  getSessionEpoch,
  setAccessToken,
} from './session-store'

function AuthProbe() {
  const auth = useAuth()
  const [lastError, setLastError] = useState('')
  return (
    <>
      <p>{auth.status}</p>
      <p>{auth.user?.role ?? 'Chưa có người dùng'}</p>
      <p>{lastError}</p>
      <button
        onClick={() =>
          void auth.login({ email: 'hv@example.com', password: 'Secret123' }).catch(() => {})
        }
      >
        Đăng nhập thử
      </button>
      <button
        onClick={() =>
          void auth
            .register({
              fullName: 'Học viên',
              email: 'hv@example.com',
              phone: '0912345678',
              password: 'Secret123',
            })
            .catch((error: Error) => setLastError(error.name))
        }
      >
        Đăng ký thử
      </button>
      <button onClick={() => void auth.retrySession().catch(() => {})}>Thử khôi phục phiên</button>
      <button onClick={() => void auth.logout().catch(() => setLastError('logout failed'))}>
        Đăng xuất thử
      </button>
    </>
  )
}

function renderProvider() {
  return render(
    <AuthProvider>
      <AuthProbe />
    </AuthProvider>,
  )
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

function unauthorizedError() {
  return Object.assign(new Error('Unauthorized'), { response: { status: 401 } })
}

describe('AuthProvider', () => {
  beforeEach(() => {
    beginSession()
    authApi.getCurrentUser.mockReset()
    authApi.login.mockReset()
    authApi.logout.mockReset().mockResolvedValue(undefined)
    authApi.register.mockReset()
    refresh.mockReset().mockRejectedValue(unauthorizedError())
  })

  it('bootstrap làm mới cookie rồi lấy user từ /me', async () => {
    refresh.mockImplementation(async (epoch: number) => {
      setAccessToken('bootstrap-jwt', epoch)
      return 'bootstrap-jwt'
    })
    authApi.getCurrentUser.mockResolvedValue({ email: 'hv@example.com', role: 'MEMBER' })

    renderProvider()

    expect(await screen.findByText('authenticated')).toBeInTheDocument()
    expect(screen.getByText('MEMBER')).toBeInTheDocument()
    expect(refresh).toHaveBeenCalledTimes(1)
    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(1)
  })

  it('bootstrap không có cookie kết thúc anonymous', async () => {
    renderProvider()

    expect(await screen.findByText('anonymous')).toBeInTheDocument()
    expect(authApi.getCurrentUser).not.toHaveBeenCalled()
  })

  it('bootstrap lỗi tạm thời giữ trang được bảo vệ đóng và cho thử lại', async () => {
    refresh
      .mockRejectedValueOnce(new Error('Network Error'))
      .mockImplementationOnce(async (epoch: number) => {
        setAccessToken('recovered-jwt', epoch)
        return 'recovered-jwt'
      })
    authApi.getCurrentUser.mockResolvedValue({ email: 'hv@example.com', role: 'MEMBER' })
    const user = userEvent.setup()
    renderProvider()

    expect(await screen.findByText('recoverable-error')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Thử khôi phục phiên' }))

    expect(await screen.findByText('authenticated')).toBeInTheDocument()
    expect(refresh).toHaveBeenCalledTimes(2)
    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(1)
  })

  it('login chỉ giữ access token trong memory và lấy identity từ /me', async () => {
    authApi.login.mockResolvedValue({ accessToken: 'login-jwt' })
    authApi.getCurrentUser.mockResolvedValue({ email: 'hv@example.com', role: 'MEMBER' })
    const setItem = vi.spyOn(Storage.prototype, 'setItem')
    const user = userEvent.setup()
    renderProvider()
    await screen.findByText('anonymous')

    await user.click(screen.getByRole('button', { name: 'Đăng nhập thử' }))

    expect(await screen.findByText('authenticated')).toBeInTheDocument()
    expect(getAccessToken()).toBe('login-jwt')
    expect(setItem).not.toHaveBeenCalled()
    setItem.mockRestore()
  })

  it('sau khi register thành công, lỗi /me cho phép retry identity mà không đăng ký lần nữa', async () => {
    authApi.register.mockResolvedValue({ accessToken: 'created-jwt' })
    authApi.getCurrentUser
      .mockRejectedValueOnce(new Error('Service Unavailable'))
      .mockResolvedValueOnce({ email: 'hv@example.com', role: 'MEMBER' })
    const user = userEvent.setup()
    renderProvider()
    await screen.findByText('anonymous')

    await user.click(screen.getByRole('button', { name: 'Đăng ký thử' }))

    expect(await screen.findByText('AuthCompletionError')).toBeInTheDocument()
    expect(screen.getByText('recoverable-error')).toBeInTheDocument()
    expect(getAccessToken()).toBe('created-jwt')
    expect(authApi.register).toHaveBeenCalledTimes(1)

    await user.click(screen.getByRole('button', { name: 'Thử khôi phục phiên' }))

    expect(await screen.findByText('authenticated')).toBeInTheDocument()
    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(2)
    expect(authApi.register).toHaveBeenCalledTimes(1)
  })

  it('logout chỉ xóa phiên sau khi server xác nhận; reload sau retry kiểm tra cookie lại', async () => {
    refresh.mockImplementation(async (epoch: number) => {
      setAccessToken('bootstrap-jwt', epoch)
      return 'bootstrap-jwt'
    })
    authApi.getCurrentUser.mockResolvedValue({ email: 'hv@example.com', role: 'MEMBER' })
    authApi.logout
      .mockRejectedValueOnce(new Error('Network Error'))
      .mockResolvedValueOnce(undefined)
    const user = userEvent.setup()
    const view = renderProvider()
    await screen.findByText('authenticated')

    await user.click(screen.getByRole('button', { name: 'Đăng xuất thử' }))
    expect(await screen.findByText('logout failed')).toBeInTheDocument()
    expect(screen.getByText('authenticated')).toBeInTheDocument()
    expect(getAccessToken()).toBe('bootstrap-jwt')

    await user.click(screen.getByRole('button', { name: 'Đăng xuất thử' }))
    expect(await screen.findByText('anonymous')).toBeInTheDocument()
    expect(getAccessToken()).toBeNull()

    view.unmount()
    refresh.mockRejectedValue(unauthorizedError())
    renderProvider()
    expect(await screen.findByText('anonymous')).toBeInTheDocument()
    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(1)
  })

  it('reload sau lỗi mạng logout khôi phục phiên nếu cookie vẫn còn hiệu lực', async () => {
    refresh.mockImplementation(async (epoch: number) => {
      setAccessToken(`bootstrap-${epoch}`, epoch)
      return `bootstrap-${epoch}`
    })
    authApi.getCurrentUser.mockResolvedValue({ email: 'hv@example.com', role: 'MEMBER' })
    authApi.logout.mockRejectedValueOnce(new Error('Network Error'))
    const user = userEvent.setup()
    const view = renderProvider()
    await screen.findByText('authenticated')

    await user.click(screen.getByRole('button', { name: 'Đăng xuất thử' }))
    expect(await screen.findByText('logout failed')).toBeInTheDocument()
    view.unmount()

    clearSession(getSessionEpoch())
    renderProvider()

    expect(await screen.findByText('authenticated')).toBeInTheDocument()
    expect(refresh).toHaveBeenCalledTimes(2)
    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(2)
  })

  it('không để /me cũ sau logout khôi phục phiên đăng nhập', async () => {
    const pendingMe = deferred<{ email: string; role: 'MEMBER' }>()
    authApi.login.mockResolvedValue({ accessToken: 'login-jwt' })
    authApi.getCurrentUser.mockReturnValue(pendingMe.promise)
    const user = userEvent.setup()
    renderProvider()
    await screen.findByText('anonymous')

    await user.click(screen.getByRole('button', { name: 'Đăng nhập thử' }))
    await waitFor(() => expect(authApi.getCurrentUser).toHaveBeenCalledTimes(1))
    await user.click(screen.getByRole('button', { name: 'Đăng xuất thử' }))
    expect(screen.getByText('anonymous')).toBeInTheDocument()

    pendingMe.resolve({ email: 'cu@example.com', role: 'MEMBER' })
    await waitFor(() => expect(getAccessToken()).toBeNull())
    expect(screen.queryByText('authenticated')).not.toBeInTheDocument()
  })
})
