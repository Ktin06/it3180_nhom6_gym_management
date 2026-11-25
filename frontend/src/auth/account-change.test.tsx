import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders, type AxiosAdapter } from 'axios'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'

const requestRefresh = vi.hoisted(() => vi.fn())
vi.mock('../services/refresh-client', () => ({ requestRefresh }))

import { AuthProvider } from './AuthProvider'
import { beginSession, getAccessToken } from './session-store'
import { useAuth } from './useAuth'
import { http } from '../services/http'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { LoginPage } from '../features/auth/pages/LoginPage'

const originalAdapter = http.defaults.adapter
const jwt = (sub: string) => `e30.${btoa(JSON.stringify({ sub }))}.signature`

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

function AccountPage() {
  const { user } = useAuth()
  return (
    <>
      <p>{user?.email}</p>
      <button onClick={() => void http.post('/private-operation').catch(() => {})}>
        Gửi thao tác
      </button>
    </>
  )
}

it('TC-F1.3.1-04f: refresh khác tài khoản đóng trang bảo vệ và đưa về login, không gửi thao tác bằng B', async () => {
  beginSession()
  requestRefresh.mockReset().mockResolvedValueOnce({ accessToken: jwt('15') })
  const requests: { url: string | undefined; bearer: unknown }[] = []
  http.defaults.adapter = (async (config) => {
    requests.push({ url: config.url, bearer: config.headers.get('Authorization') })
    const response = {
      config,
      headers: new AxiosHeaders(),
      status: 200,
      statusText: 'OK',
      data: {},
    }
    if (config.url === '/auth/me') {
      return { ...response, data: { userId: 15, email: 'a@example.com', role: 'MEMBER' } }
    }
    throw new AxiosError('Unauthorized', AxiosError.ERR_BAD_REQUEST, config, undefined, {
      ...response,
      status: 401,
      statusText: 'Unauthorized',
    })
  }) as AxiosAdapter
  const user = userEvent.setup()
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/member']}>
        <Routes>
          <Route
            path="/member"
            element={
              <ProtectedRoute allowedRoles={['MEMBER']}>
                <AccountPage />
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
  expect(await screen.findByText('a@example.com')).toBeInTheDocument()
  requestRefresh.mockResolvedValueOnce({ accessToken: jwt('26') })

  await user.click(screen.getByRole('button', { name: 'Gửi thao tác' }))

  expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
  expect(screen.getByRole('alert')).toHaveTextContent('Tài khoản đã đổi ở tab khác')
  expect(screen.queryByText('a@example.com')).not.toBeInTheDocument()
  expect(getAccessToken()).toBeNull()
  expect(requestRefresh).toHaveBeenCalledTimes(2)
  expect(requests.filter((request) => request.url === '/private-operation')).toHaveLength(1)
  expect(requests.some((request) => request.bearer === `Bearer ${jwt('26')}`)).toBe(false)
  expect(requests.some((request) => request.url === '/auth/logout')).toBe(false)
})
