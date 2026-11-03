import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
import { beginSession, getAccessToken, setAccessToken } from './session-store'

function AuthProbe() {
  const auth = useAuth()
  return (
    <>
      <p>{auth.status}</p>
      <p>{auth.user?.role ?? 'Chưa có người dùng'}</p>
      <button
        onClick={() =>
          void auth.login({ email: 'hv@example.com', password: 'Secret123' }).catch(() => {})
        }
      >
        Đăng nhập thử
      </button>
      <button onClick={() => void auth.logout().catch(() => {})}>Đăng xuất thử</button>
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

describe('AuthProvider', () => {
  beforeEach(() => {
    beginSession()
    authApi.getCurrentUser.mockReset()
    authApi.login.mockReset()
    authApi.logout.mockReset().mockResolvedValue(undefined)
    authApi.register.mockReset()
    refresh.mockReset().mockRejectedValue(new Error('Chưa có refresh cookie'))
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
