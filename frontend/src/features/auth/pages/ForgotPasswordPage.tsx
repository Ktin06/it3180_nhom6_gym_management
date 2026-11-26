import { MailOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../shared/api-error'
import { forgotPassword, getProblemDetail } from '../api/auth-api'
import { normalizeEmail } from '../api/validation'

interface ForgotPasswordForm {
  email: string
}

const DEFAULT_FORGOT_PASSWORD_MESSAGE =
  'Nếu email có trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu.'

/** Trang yêu cầu gửi liên kết đặt lại mật khẩu qua email (công khai, UC-F1.5). */
export function ForgotPasswordPage() {
  const [form] = Form.useForm<ForgotPasswordForm>()
  const [pending, setPending] = useState(false)
  const [submittedMessage, setSubmittedMessage] = useState<string | null>(null)
  const [errorDetail, setErrorDetail] = useState<string | null>(null)

  const handleFinish = (values: ForgotPasswordForm) => {
    setPending(true)
    setErrorDetail(null)
    void (async () => {
      try {
        const result = await forgotPassword({ email: normalizeEmail(values.email) })
        const message =
          typeof result?.message === 'string' && result.message.trim()
            ? result.message
            : DEFAULT_FORGOT_PASSWORD_MESSAGE
        setSubmittedMessage(message)
      } catch (error) {
        const problem = getProblemDetail(error)
        const emailError = problem.errors.email
        if (emailError) {
          form.setFields([{ name: 'email', errors: [emailError] }])
        }
        setErrorDetail(problem.detail ?? (emailError ? null : errorMessage(error)))
      } finally {
        setPending(false)
      }
    })()
  }

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Quên mật khẩu
      </Typography.Title>
      {submittedMessage ? (
        <>
          <Alert type="success" showIcon title={submittedMessage} style={{ marginBottom: 16 }} />
          <Typography.Paragraph style={{ margin: '16px 0 0', textAlign: 'center' }}>
            <Link to="/login">Quay lại đăng nhập</Link>
          </Typography.Paragraph>
        </>
      ) : (
        <>
          <Typography.Paragraph type="secondary">
            Nhập email tài khoản để nhận liên kết đặt lại mật khẩu.
          </Typography.Paragraph>
          {errorDetail && (
            <Alert type="error" showIcon title={errorDetail} style={{ marginBottom: 16 }} />
          )}
          <Form<ForgotPasswordForm>
            form={form}
            layout="vertical"
            onFinish={handleFinish}
            requiredMark={false}
            disabled={pending}
          >
            <Form.Item
              label="Email"
              name="email"
              normalize={normalizeEmail}
              rules={[
                { required: true, message: 'Vui lòng nhập email' },
                { type: 'email', message: 'Email không hợp lệ' },
              ]}
            >
              <Input prefix={<MailOutlined />} placeholder="ban@vidu.com" autoComplete="email" />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={pending}
              aria-busy={pending}
              style={{ minHeight: 40 }}
            >
              Gửi liên kết đặt lại mật khẩu
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
