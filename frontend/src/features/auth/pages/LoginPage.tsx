import { LockOutlined, MailOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../../auth/useAuth'
import { roleAwareReturnPath } from '../../../auth/redirect'
import { getProblemDetail } from '../api/auth-api'
import { normalizeEmail } from '../validation'

interface LoginForm {
  email: string
  password: string
}

export function LoginPage() {
  const [form] = Form.useForm<LoginForm>()
  const [pending, setPending] = useState(false)
  const [errorDetail, setErrorDetail] = useState<string | null>(null)
  const { login } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const handleFinish = (values: LoginForm) => {
    setPending(true)
    setErrorDetail(null)
    void (async () => {
      try {
        const user = await login({ email: normalizeEmail(values.email), password: values.password })
        navigate(roleAwareReturnPath(location.state, window.location.origin, user.role), {
          replace: true,
        })
      } catch (error) {
        const problem = getProblemDetail(error)
        form.setFields(
          Object.entries(problem.errors)
            .filter(([name]) => name === 'email' || name === 'password')
            .map(([name, errors]) => ({ name: name as keyof LoginForm, errors: [errors] })),
        )
        setErrorDetail(problem.detail ?? 'Đăng nhập chưa thành công. Vui lòng thử lại.')
      } finally {
        setPending(false)
      }
    })()
  }

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Đăng nhập
      </Typography.Title>
      {errorDetail && (
        <Alert type="error" showIcon title={errorDetail} style={{ marginBottom: 16 }} />
      )}
      <Form<LoginForm>
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
        <Form.Item
          label="Mật khẩu"
          name="password"
          rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
        >
          <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
        </Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          block
          loading={pending}
          aria-label="Đăng nhập"
          aria-busy={pending}
        >
          Đăng nhập
        </Button>
      </Form>
      <Typography.Paragraph style={{ margin: '16px 0 0', textAlign: 'center' }}>
        Chưa có tài khoản? <Link to="/register">Đăng ký hội viên</Link>
      </Typography.Paragraph>
    </>
  )
}
