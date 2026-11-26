import { App } from 'antd'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders, type AxiosAdapter } from 'axios'
import { createMemoryRouter, MemoryRouter, Route, RouterProvider, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const requestRefresh = vi.hoisted(() => vi.fn())
vi.mock('../../../services/refresh-client', () => ({ requestRefresh }))

import { routes } from '../../../app/router'
import { AuthProvider } from '../../../auth/AuthProvider'
import type { Role } from '../../../auth/roles'
import { publishSessionChange } from '../../../auth/session-coordination'
import { beginSession, getAccessToken } from '../../../auth/session-store'
import { readCookieRevision, writeCookieRevision } from '../../../auth/session-revision'
import { ProtectedRoute } from '../../../routes/ProtectedRoute'
import { rolesFor } from '../../../routes/route-access'
import { http } from '../../../services/http'
import { ChangePasswordPage } from './ChangePasswordPage'
import { LoginPage } from './LoginPage'

const originalAdapter = http.defaults.adapter
const jwt = (sub: string) => `e30.${btoa(JSON.stringify({ sub }))}.signature`

interface RecordedCall {
  url: string | undefined
  method: string | undefined
  data: unknown
  authorization: unknown
}

function setupAuthenticatedHttp(
  role: Role,
  onChangePassword: () => Promise<{ status: number; data?: unknown }>,
) {
  requestRefresh.mockReset().mockResolvedValueOnce({ accessToken: jwt('15') })
  const calls: RecordedCall[] = []

  http.defaults.adapter = (async (config) => {
    const parsedData: unknown = typeof config.data === 'string' ? JSON.parse(config.data) : config.data
    calls.push({
      url: config.url,
      method: config.method,
      data: parsedData,
      authorization: config.headers.get('Authorization'),
    })

    const baseResponse = {
      config,
      headers: new AxiosHeaders(),
      status: 200,
      statusText: 'OK',
      data: {},
    }

    if (config.url === '/auth/me') {
      return {
        ...baseResponse,
        data: { userId: 15, email: `${role.toLowerCase()}@example.com`, role },
      }
    }

    if (config.url === '/auth/change-password') {
      const result = await onChangePassword()
      if (result.status >= 400) {
        throw new AxiosError('Request failed', AxiosError.ERR_BAD_REQUEST, config, undefined, {
          ...baseResponse,
          status: result.status,
          statusText: 'Error',
          data: result.data ?? {},
        })
      }
      return {
        ...baseResponse,
        status: result.status,
        data: result.data,
      }
    }

    return baseResponse
  }) as AxiosAdapter

  return calls
}

function renderChangePasswordFlow(
  role: Role,
  onChangePassword: () => Promise<{ status: number; data?: unknown }>,
) {
  const calls = setupAuthenticatedHttp(role, onChangePassword)
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/doi-mat-khau']}>
        <Routes>
          <Route
            path="/doi-mat-khau"
            element={
              <ProtectedRoute allowedRoles={rolesFor('/doi-mat-khau')}>
                <ChangePasswordPage />
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
  return calls
}

describe('ChangePasswordPage', () => {
  beforeEach(async () => {
    beginSession()
    await publishSessionChange(null)
  })

  afterEach(() => {
    http.defaults.adapter = originalAdapter
  })

  it('TC-F1.4.1-01f: Đổi mật khẩu thành công gọi API, xóa phiên trình duyệt và chuyển về /login kèm thông báo', async () => {
    let resolveChangePassword!: (value: { status: number; data?: unknown }) => void
    const calls = renderChangePasswordFlow(
      'RECEPTIONIST',
      () =>
        new Promise((resolve) => {
          resolveChangePassword = resolve
        }),
    )
    const user = userEvent.setup()

    const currentInput = await screen.findByLabelText('Mật khẩu hiện tại')
    fireEvent.change(currentInput, { target: { value: 'OldPassword1' } })
    fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: 'NewPassword2' } })
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), {
      target: { value: 'NewPassword2' },
    })
    const submitButton = screen.getByRole('button', { name: 'Đổi mật khẩu' })
    await user.click(submitButton)

    await waitFor(() => {
      expect(submitButton).toHaveAttribute('aria-busy', 'true')
      expect(currentInput).toBeDisabled()
    })

    resolveChangePassword({ status: 204 })

    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('Đã đổi mật khẩu, vui lòng đăng nhập lại')
    expect(getAccessToken()).toBeNull()

    const apiCall = calls.find((call) => call.url === '/auth/change-password')
    expect(apiCall).toEqual({
      url: '/auth/change-password',
      method: 'post',
      data: {
        currentPassword: 'OldPassword1',
        newPassword: 'NewPassword2',
      },
      authorization: `Bearer ${jwt('15')}`,
    })
  })

  it('TC-F1.4.1-01j: Phản hồi đổi mật khẩu đến muộn giữ phiên mới của tab khác', async () => {
    let finish!: (response: { status: number }) => void
    const onChange = vi.fn(() => new Promise<{ status: number }>((resolve) => { finish = resolve }))
    renderChangePasswordFlow('MEMBER', onChange)
    const user = userEvent.setup()
    fireEvent.change(await screen.findByLabelText('Mật khẩu hiện tại'), { target: { value: 'OldPassword1' } })
    fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: 'NewPassword2' } })
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), { target: { value: 'NewPassword2' } })
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))
    await waitFor(() => expect(onChange).toHaveBeenCalledOnce())

    const newSession = { session: 'tab-khac-vua-dang-nhap', refresh: null }
    await writeCookieRevision(newSession)
    finish({ status: 204 })
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Đổi mật khẩu', busy: true })).not.toBeInTheDocument()
    })
    expect(await readCookieRevision()).toEqual(newSession)
  })

  it('TC-F1.4.1-03b: Kiểm tra quy tắc mật khẩu S1, khớp mật khẩu nhập lại và hiển thị lỗi 400 theo trường', async () => {
    const onChangePassword = vi.fn(async () => ({
      status: 400,
      data: {
        detail: 'Vui lòng kiểm tra lại các trường dữ liệu.',
        errors: {
          currentPassword: 'Mật khẩu hiện tại không được để trống',
          newPassword: 'Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm cả chữ cái và chữ số',
        },
      },
    }))
    const calls = renderChangePasswordFlow('MEMBER', onChangePassword)
    const user = userEvent.setup()

    const currentInput = await screen.findByLabelText('Mật khẩu hiện tại')
    const newInput = screen.getByLabelText('Mật khẩu mới')
    const confirmInput = screen.getByLabelText('Nhập lại mật khẩu mới')

    // Dưới 8 ký tự
    await user.type(currentInput, 'OldPassword1')
    await user.type(newInput, 'Ab1')
    await user.type(confirmInput, 'Ab1')
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(
      await screen.findByText(
        'Mật khẩu mới cần ít nhất 8 ký tự, có chữ A–Z/a–z và số, tối đa 72 byte UTF-8',
      ),
    ).toBeInTheDocument()
    expect(onChangePassword).not.toHaveBeenCalled()

    // Thiếu chữ hoặc số
    fireEvent.change(newInput, { target: { value: 'abcdefgh' } })
    fireEvent.change(confirmInput, { target: { value: 'abcdefgh' } })
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))
    expect(
      await screen.findByText(
        'Mật khẩu mới cần ít nhất 8 ký tự, có chữ A–Z/a–z và số, tối đa 72 byte UTF-8',
      ),
    ).toBeInTheDocument()
    expect(onChangePassword).not.toHaveBeenCalled()

    // Hai ô mật khẩu mới không khớp
    fireEvent.change(newInput, { target: { value: 'NewPassword2' } })
    fireEvent.change(confirmInput, { target: { value: 'Different2' } })
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))
    expect(await screen.findByText('Mật khẩu nhập lại chưa khớp')).toBeInTheDocument()
    expect(onChangePassword).not.toHaveBeenCalled()

    // Vượt quá 72 byte UTF-8
    const tooLong = `${'a'.repeat(72)}1`
    fireEvent.change(newInput, { target: { value: tooLong } })
    fireEvent.change(confirmInput, { target: { value: tooLong } })
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))
    expect(
      await screen.findByText(
        'Mật khẩu mới cần ít nhất 8 ký tự, có chữ A–Z/a–z và số, tối đa 72 byte UTF-8',
      ),
    ).toBeInTheDocument()
    expect(onChangePassword).not.toHaveBeenCalled()

    // API trả 400 kèm errors theo trường
    fireEvent.change(newInput, { target: { value: 'NewPassword2' } })
    fireEvent.change(confirmInput, { target: { value: 'NewPassword2' } })
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(await screen.findByText('Mật khẩu hiện tại không được để trống')).toBeInTheDocument()
    expect(
      await screen.findByText(
        'Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm cả chữ cái và chữ số',
      ),
    ).toBeInTheDocument()
    expect(calls.filter((call) => call.url === '/auth/change-password')).toHaveLength(1)
  })

  it('TC-F1.4.1-02c: Lỗi 422 từ API hiển thị đúng thông báo và không xóa phiên', async () => {
    const onChangePassword = vi
      .fn()
      .mockResolvedValueOnce({
        status: 422,
        data: { detail: 'Mật khẩu hiện tại không đúng.' },
      })
      .mockResolvedValueOnce({
        status: 422,
        data: { detail: 'Mật khẩu mới phải khác mật khẩu hiện tại.' },
      })
    renderChangePasswordFlow('TRAINER', onChangePassword)
    const user = userEvent.setup()

    const currentInput = await screen.findByLabelText('Mật khẩu hiện tại')
    fireEvent.change(currentInput, { target: { value: 'WrongPass1' } })
    fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: 'NewPassword2' } })
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), {
      target: { value: 'NewPassword2' },
    })
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Mật khẩu hiện tại không đúng.')
    expect(screen.getByRole('heading', { name: 'Đổi mật khẩu' })).toBeInTheDocument()
    expect(getAccessToken()).toBe(jwt('15'))

    // Không chặn trước newPassword === currentPassword ở trình duyệt để backend trả 422
    fireEvent.change(currentInput, { target: { value: 'NewPassword2' } })
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Mật khẩu mới phải khác mật khẩu hiện tại.',
      )
    })
    expect(screen.getByRole('heading', { name: 'Đổi mật khẩu' })).toBeInTheDocument()
    expect(getAccessToken()).toBe(jwt('15'))
  })

  describe('TC-F1.4.1-04b: mở /doi-mat-khau đúng layout theo vai trò và không bị /403', () => {
    it.each([
      { role: 'RECEPTIONIST' as const },
      { role: 'MEMBER' as const },
    ])(
      '$role mở /doi-mat-khau đúng layout của mình và không bị /403',
      async ({ role }) => {
        setupAuthenticatedHttp(role, async () => ({ status: 204 }))
        const memoryRouter = createMemoryRouter(routes, { initialEntries: ['/doi-mat-khau'] })

        render(
          <App>
            <AuthProvider>
              <RouterProvider router={memoryRouter} />
            </AuthProvider>
          </App>,
        )

        expect(await screen.findByRole('heading', { name: 'Đổi mật khẩu' })).toBeInTheDocument()
        if (role === 'MEMBER') {
          expect(screen.getByRole('navigation', { name: 'Điều hướng hội viên' })).toBeInTheDocument()
        } else {
          expect(screen.getByRole('button', { name: 'Mở menu' })).toBeInTheDocument()
        }
        expect(screen.queryByText(/không có quyền/i)).not.toBeInTheDocument()
      },
    )
  })
})
