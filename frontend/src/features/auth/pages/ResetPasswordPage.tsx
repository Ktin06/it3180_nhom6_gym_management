import { LockOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { errorMessage } from '../../../services/api-error'
import { getProblemDetail, resetPassword } from '../api/auth-api'
import { isValidPassword } from '../api/validation'

interface ResetPasswordForm {
  newPassword: string
  confirmPassword: string
}

const DEFAULT_TOKEN_EXPIRED_MESSAGE = 'Liên kết đã hết hạn hoặc đã được dùng.'
const RESET_SUCCESS_NOTICE = 'Đặt mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.'

/**
 * Trang đặt mật khẩu bằng token một lần qua liên kết email (công khai, UC-F1.5).
 * Dùng chung cho cả luồng quên mật khẩu (S2-05) và đặt mật khẩu lần đầu khi lễ tân tạo tài khoản (S2-36).
 */
export function ResetPasswordPage() {
  const [form] = Form.useForm<ResetPasswordForm>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [pending, setPending] = useState(false)
  const [errorDetail, setErrorDetail] = useState<string | null>(null)
  const [tokenExpiredError, setTokenExpiredError] = useState<string | null>(null)

  const token = searchParams.get('token')?.trim() ?? ''

  const handleFinish = (values: ResetPasswordForm) => {
    if (!token) return
    setPending(true)
    setErrorDetail(null)
    setTokenExpiredError(null)
    void (async () => {
      try {
        await resetPassword({ token, newPassword: values.newPassword })
        navigate('/login', {
          replace: true,
          state: { notice: RESET_SUCCESS_NOTICE },
        })
      } catch (error) {
        const problem = getProblemDetail(error)
        if (problem.status === 422) {
          setTokenExpiredError(problem.detail ?? DEFAULT_TOKEN_EXPIRED_MESSAGE)
          return
        }
        const newPasswordError = problem.errors.newPassword
        if (newPasswordError) {
          form.setFields([{ name: 'newPassword', errors: [newPasswordError] }])
        }
        setErrorDetail(
          problem.errors.token ?? problem.detail ?? (newPasswordError ? null : errorMessage(error)),
        )
      } finally {
        setPending(false)
      }
    })()
  }

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Đặt mật khẩu
      </Typography.Title>
      {!token ? (
        <>
          <Alert
            type="error"
            showIcon
            title="Liên kết đặt mật khẩu không hợp lệ hoặc thiếu mã xác thực."
            style={{ marginBottom: 16 }}
          />
          <Button
            type="primary"
            block
            style={{ minHeight: 40 }}
            onClick={() => navigate('/quen-mat-khau')}
          >
            Gửi lại liên kết
          </Button>
          <Typography.Paragraph style={{ margin: '16px 0 0', textAlign: 'center' }}>
            <Link to="/login">Quay lại đăng nhập</Link>
          </Typography.Paragraph>
        </>
      ) : (
        <>
          <Typography.Paragraph type="secondary">
            Nhập mật khẩu mới cho tài khoản của bạn. Mật khẩu cần ít nhất 8 ký tự, gồm chữ cái và
            chữ số.
          </Typography.Paragraph>
          {tokenExpiredError && (
            <>
              <Alert type="error" showIcon title={tokenExpiredError} style={{ marginBottom: 12 }} />
              <Button
                type="primary"
                block
                style={{ minHeight: 40, marginBottom: 16 }}
                onClick={() => navigate('/quen-mat-khau')}
              >
                Gửi lại liên kết
              </Button>
            </>
          )}
          {errorDetail && (
            <Alert type="error" showIcon title={errorDetail} style={{ marginBottom: 16 }} />
          )}
          <Form<ResetPasswordForm>
            form={form}
            layout="vertical"
            onFinish={handleFinish}
            requiredMark={false}
            disabled={pending}
          >
            <Form.Item
              label="Mật khẩu mới"
              name="newPassword"
              rules={[
                { required: true, message: 'Vui lòng nhập mật khẩu mới' },
                {
                  validator: (_, value: string | undefined) =>
                    !value || isValidPassword(value)
                      ? Promise.resolve()
                      : Promise.reject(
                          new Error(
                            'Mật khẩu mới cần ít nhất 8 ký tự, có chữ A–Z/a–z và số, tối đa 72 byte UTF-8',
                          ),
                        ),
                },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Tối thiểu 8 ký tự, có chữ và số"
                autoComplete="new-password"
              />
            </Form.Item>
            <Form.Item
              label="Nhập lại mật khẩu mới"
              name="confirmPassword"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: 'Vui lòng nhập lại mật khẩu mới' },
                ({ getFieldValue }) => ({
                  validator(_, value: string | undefined) {
                    return !value || getFieldValue('newPassword') === value
                      ? Promise.resolve()
                      : Promise.reject(new Error('Mật khẩu nhập lại chưa khớp'))
                  },
                }),
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Nhập lại mật khẩu mới"
                autoComplete="new-password"
              />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={pending}
              aria-busy={pending}
              style={{ minHeight: 40 }}
            >
              Đặt mật khẩu
            </Button>
          </Form>
          <Typography.Paragraph style={{ margin: '16px 0 0', textAlign: 'center' }}>
            <Link to="/login">Quay lại đăng nhập</Link>
          </Typography.Paragraph>
        </>
      )}
    </>
  )
}
