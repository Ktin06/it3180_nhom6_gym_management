import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders, type AxiosAdapter } from 'axios'
import { createMemoryRouter, MemoryRouter, Route, RouterProvider, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { routes } from '../../../app/router'
import { AuthContext } from '../../../auth/AuthContext'
import { beginSession, setAccessToken } from '../../../auth/session-store'
import { http } from '../../../services/http'
import { LoginPage } from './LoginPage'
import { ResetPasswordPage } from './ResetPasswordPage'

const originalAdapter = http.defaults.adapter

interface RecordedCall {
  url: string | undefined
  method: string | undefined
  data: unknown
  authorization: unknown
}

function setupResetPasswordHttp(
  onResetPassword: (body: unknown) => Promise<{ status: number; data?: unknown }>,
) {
  const calls: RecordedCall[] = []

  http.defaults.adapter = (async (config) => {
    const parsedData: unknown =
      typeof config.data === 'string' ? JSON.parse(config.data) : config.data
    calls.push({
      url: config.url,
      method: config.method,
      data: parsedData,
      authorization: config.headers.get('Authorization'),
    })

    const baseResponse = {
      config,
      headers: new AxiosHeaders(),
      status: 204,
      statusText: 'No Content',
      data: undefined,
    }

    if (config.url === '/auth/reset-password') {
      const result = await onResetPassword(parsedData)
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

function renderResetPasswordFlow(initialUrl: string) {
  return render(
    <AuthContext.Provider
      value={{
        status: 'anonymous',
        user: null,
        sessionError: null,
        login: vi.fn(),
        register: vi.fn(),
        logout: vi.fn(),
        retrySession: vi.fn(),
      }}
    >
      <MemoryRouter initialEntries={[initialUrl]}>
        <Routes>
          <Route path="/dat-lai-mat-khau" element={<ResetPasswordPage />} />
          <Route path="/quen-mat-khau" element={<p>Trang quên mật khẩu</p>} />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    const epoch = beginSession()
    setAccessToken('old-token-should-not-be-sent', epoch)
  })

  afterEach(() => {
    http.defaults.adapter = originalAdapter
  })

  it('TC-F1.5.1-03d: Trang Đặt mật khẩu gửi token và mật khẩu mới hợp lệ rồi chuyển về /login kèm thông báo', async () => {
    let resolveReset!: (value: { status: number; data?: unknown }) => void
    const calls = setupResetPasswordHttp(
      () =>
        new Promise((resolve) => {
          resolveReset = resolve
        }),
    )
    const user = userEvent.setup()
    renderResetPasswordFlow('/dat-lai-mat-khau?token=valid-token-abc')

    expect(screen.getByRole('heading', { name: 'Đặt mật khẩu' })).toBeInTheDocument()

    const newPasswordInput = screen.getByLabelText('Mật khẩu mới')
    const confirmPasswordInput = screen.getByLabelText('Nhập lại mật khẩu mới')
    fireEvent.change(newPasswordInput, { target: { value: 'NewPassword2' } })
    fireEvent.change(confirmPasswordInput, { target: { value: 'NewPassword2' } })

    const submitButton = screen.getByRole('button', { name: 'Đặt mật khẩu' })
    await user.click(submitButton)

    await waitFor(() => {
      expect(submitButton).toHaveAttribute('aria-busy', 'true')
      expect(newPasswordInput).toBeDisabled()
    })

    resolveReset({ status: 204 })

    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent(/đặt mật khẩu thành công/i)
    expect(calls).toEqual([
      {
        url: '/auth/reset-password',
        method: 'post',
        data: {
          token: 'valid-token-abc',
          newPassword: 'NewPassword2',
        },
        authorization: undefined,
      },
    ])
  })

  describe('TC-F1.5.1-05d: thiếu hoặc rỗng token trên URL báo lỗi ngay, hiện nút Gửi lại liên kết và không gọi API', () => {
    it.each([
      { label: 'không có tham số token', url: '/dat-lai-mat-khau' },
      { label: 'tham số token rỗng', url: '/dat-lai-mat-khau?token=%20%20' },
    ])(
      'Thiếu token trên URL ($label) báo lỗi ngay, hiện nút Gửi lại liên kết và không gọi API',
      async ({ url }) => {
        const onResetPassword = vi.fn(async () => ({ status: 204 }))
        const calls = setupResetPasswordHttp(onResetPassword)
        const user = userEvent.setup()
        renderResetPasswordFlow(url)

        expect(screen.getByRole('heading', { name: 'Đặt mật khẩu' })).toBeInTheDocument()
        expect(screen.getByRole('alert')).toHaveTextContent(/liên kết.*không hợp lệ|thiếu/i)
        expect(screen.queryByLabelText('Mật khẩu mới')).not.toBeInTheDocument()
        expect(onResetPassword).not.toHaveBeenCalled()
        expect(calls).toHaveLength(0)

        await user.click(screen.getByRole('button', { name: 'Gửi lại liên kết' }))
        expect(await screen.findByText('Trang quên mật khẩu')).toBeInTheDocument()
      },
    )
  })

  it('TC-F1.5.1-05e: kiểm tra chuẩn S1, hai mật khẩu không khớp không gọi API và gắn lỗi 400 vào trường', async () => {
    const onResetPassword = vi.fn(async () => ({
      status: 400,
      data: {
        detail: 'Dữ liệu đầu vào không hợp lệ.',
        errors: {
          newPassword: 'Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm cả chữ cái và chữ số',
        },
      },
    }))
    const calls = setupResetPasswordHttp(onResetPassword)
    const user = userEvent.setup()
    renderResetPasswordFlow('/dat-lai-mat-khau?token=valid-token-abc')

    const newPasswordInput = screen.getByLabelText('Mật khẩu mới')
    const confirmPasswordInput = screen.getByLabelText('Nhập lại mật khẩu mới')
    const submitButton = screen.getByRole('button', { name: 'Đặt mật khẩu' })

    // Dưới 8 ký tự
    fireEvent.change(newPasswordInput, { target: { value: 'Ab1' } })
    fireEvent.change(confirmPasswordInput, { target: { value: 'Ab1' } })
    await user.click(submitButton)
    expect(
      await screen.findByText(
        'Mật khẩu mới cần ít nhất 8 ký tự, có chữ A–Z/a–z và số, tối đa 72 byte UTF-8',
      ),
    ).toBeInTheDocument()
    expect(onResetPassword).not.toHaveBeenCalled()

    // Thiếu số
    fireEvent.change(newPasswordInput, { target: { value: 'abcdefgh' } })
    fireEvent.change(confirmPasswordInput, { target: { value: 'abcdefgh' } })
    await user.click(submitButton)
    expect(onResetPassword).not.toHaveBeenCalled()

    // Vượt quá 72 byte UTF-8
    const tooLong = `${'a'.repeat(72)}1`
    fireEvent.change(newPasswordInput, { target: { value: tooLong } })
    fireEvent.change(confirmPasswordInput, { target: { value: tooLong } })
    await user.click(submitButton)
    expect(onResetPassword).not.toHaveBeenCalled()

    // Hai mật khẩu không khớp
    fireEvent.change(newPasswordInput, { target: { value: 'NewPassword2' } })
    fireEvent.change(confirmPasswordInput, { target: { value: 'Different2' } })
    await user.click(submitButton)
    expect(await screen.findByText('Mật khẩu nhập lại chưa khớp')).toBeInTheDocument()
    expect(onResetPassword).not.toHaveBeenCalled()

    // Gửi hợp lệ, API trả 400 kèm errors.newPassword
    fireEvent.change(confirmPasswordInput, { target: { value: 'NewPassword2' } })
    await user.click(submitButton)
    expect(
      await screen.findByText('Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm cả chữ cái và chữ số'),
    ).toBeInTheDocument()
    expect(calls).toHaveLength(1)
  })

  it('TC-F1.5.1-04e: lỗi 422 hết hạn hoặc đã dùng hiển thị đúng câu detail của API kèm nút Gửi lại liên kết dẫn về /quen-mat-khau', async () => {
    const expiredDetail = 'Liên kết đã hết hạn hoặc đã được dùng.'
    setupResetPasswordHttp(async () => ({
      status: 422,
      data: { detail: expiredDetail },
    }))
    const user = userEvent.setup()
    renderResetPasswordFlow('/dat-lai-mat-khau?token=expired-token')

    fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: 'NewPassword2' } })
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), {
      target: { value: 'NewPassword2' },
    })
    await user.click(screen.getByRole('button', { name: 'Đặt mật khẩu' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(expiredDetail)
    const resendButton = screen.getByRole('button', { name: 'Gửi lại liên kết' })
    await user.click(resendButton)

    expect(await screen.findByText('Trang quên mật khẩu')).toBeInTheDocument()
  })

  describe('TC-F1.5.1-03e: /quen-mat-khau và /dat-lai-mat-khau là trang công khai trong AuthLayout', () => {
    it.each([
      { path: '/quen-mat-khau', heading: 'Quên mật khẩu' },
      { path: '/dat-lai-mat-khau?token=abc', heading: 'Đặt mật khẩu' },
    ])('$path là trang công khai trong AuthLayout', async ({ path, heading }) => {
      const memoryRouter = createMemoryRouter(routes, { initialEntries: [path] })
      render(
        <AuthContext.Provider
          value={{
            status: 'anonymous',
            user: null,
            sessionError: null,
            login: vi.fn(),
            register: vi.fn(),
            logout: vi.fn(),
            retrySession: vi.fn(),
          }}
        >
          <RouterProvider router={memoryRouter} />
        </AuthContext.Provider>,
      )

      expect(await screen.findByRole('heading', { name: heading })).toBeInTheDocument()
    })
  })
})
