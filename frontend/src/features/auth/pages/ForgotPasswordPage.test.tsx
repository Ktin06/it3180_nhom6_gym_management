import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders, type AxiosAdapter } from 'axios'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { beginSession, setAccessToken } from '../../../auth/session-store'
import { http } from '../../../services/http'
import { ForgotPasswordPage } from './ForgotPasswordPage'

const originalAdapter = http.defaults.adapter
const GENERIC_MESSAGE = 'Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu.'

interface RecordedCall {
  url: string | undefined
  method: string | undefined
  data: unknown
  authorization: unknown
}

function setupForgotPasswordHttp(
  onForgotPassword: (body: unknown) => Promise<{ status: number; data?: unknown }>,
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
      status: 200,
      statusText: 'OK',
      data: {},
    }

    if (config.url === '/auth/forgot-password') {
      const result = await onForgotPassword(parsedData)
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

function renderForgotPasswordPage() {
  return render(
    <MemoryRouter initialEntries={['/quen-mat-khau']}>
      <Routes>
        <Route path="/quen-mat-khau" element={<ForgotPasswordPage />} />
        <Route path="/login" element={<p>Trang đăng nhập</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    const epoch = beginSession()
    setAccessToken('old-token-should-not-be-sent', epoch)
  })

  afterEach(() => {
    http.defaults.adapter = originalAdapter
  })

  describe('TC-F1.5.1-01c: nhập email ở /quen-mat-khau gửi email chuẩn hóa và luôn hiện câu chung từ API', () => {
    it.each([
      {
        label: 'email có trong hệ thống',
        inputEmail: '  HV@EXAMPLE.COM  ',
        normalized: 'hv@example.com',
      },
      {
        label: 'email không tồn tại trong hệ thống',
        inputEmail: 'khongtontai@example.com',
        normalized: 'khongtontai@example.com',
      },
    ])(
      'Nhập $label ở /quen-mat-khau gửi email chuẩn hóa và luôn hiện câu chung từ API',
      async ({ inputEmail, normalized }) => {
        let resolveRequest!: (value: { status: number; data?: unknown }) => void
        const calls = setupForgotPasswordHttp(
          () =>
            new Promise((resolve) => {
              resolveRequest = resolve
            }),
        )
        const user = userEvent.setup()
        renderForgotPasswordPage()

        const emailInput = screen.getByLabelText('Email')
        fireEvent.change(emailInput, { target: { value: inputEmail } })
        const submitButton = screen.getByRole('button', { name: 'Gửi liên kết đặt lại mật khẩu' })
        await user.click(submitButton)

        await waitFor(() => {
          expect(submitButton).toHaveAttribute('aria-busy', 'true')
          expect(emailInput).toBeDisabled()
        })

        resolveRequest({ status: 200, data: { message: GENERIC_MESSAGE } })

        expect(await screen.findByRole('alert')).toHaveTextContent(GENERIC_MESSAGE)
        expect(calls).toEqual([
          {
            url: '/auth/forgot-password',
            method: 'post',
            data: { email: normalized },
            authorization: undefined,
          },
        ])

        const backLink = screen.getByRole('link', { name: 'Quay lại đăng nhập' })
        expect(backLink).toHaveAttribute('href', '/login')
      },
    )
  })

  it('TC-F1.5.1-05c: email sai định dạng ở trình duyệt không gọi API và lỗi 400 từ API gắn vào trường email', async () => {
    const onForgotPassword = vi.fn(async () => ({
      status: 400,
      data: {
        detail: 'Dữ liệu đầu vào không hợp lệ.',
        errors: {
          email: 'Email không đúng định dạng',
        },
      },
    }))
    setupForgotPasswordHttp(onForgotPassword)
    const user = userEvent.setup()
    renderForgotPasswordPage()

    const emailInput = screen.getByLabelText('Email')
    fireEvent.change(emailInput, { target: { value: 'khong-phai-email' } })
    await user.click(screen.getByRole('button', { name: 'Gửi liên kết đặt lại mật khẩu' }))

    expect(await screen.findByText('Email không hợp lệ')).toBeInTheDocument()
    expect(onForgotPassword).not.toHaveBeenCalled()

    fireEvent.change(emailInput, { target: { value: 'bad@example.com' } })
    await user.click(screen.getByRole('button', { name: 'Gửi liên kết đặt lại mật khẩu' }))

    expect(await screen.findByText('Email không đúng định dạng')).toBeInTheDocument()
    expect(onForgotPassword).toHaveBeenCalledOnce()
  })
})
