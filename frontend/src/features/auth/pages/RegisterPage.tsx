import { Alert, Button, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { HOME_PATH_BY_ROLE } from '../../../auth/roles'
import { useAuth } from '../../../auth/useAuth'
import { getProblemDetail } from '../api/auth-api'
import type { RegisterRequest } from '../types/auth'
import { isValidPassword, isValidPhone, normalizeEmail, normalizePhone } from '../validation'

interface RegisterForm extends RegisterRequest {
  confirmPassword: string
}

export function RegisterPage() {
  const [form] = Form.useForm<RegisterForm>()
  const [pending, setPending] = useState(false)
  const [errorDetail, setErrorDetail] = useState<string | null>(null)
  const { register } = useAuth()
  const navigate = useNavigate()

  const handleFinish = (values: RegisterForm) => {
    setPending(true)
    setErrorDetail(null)
    void (async () => {
      try {
        const user = await register({
          fullName: values.fullName.trim(),
          email: normalizeEmail(values.email),
          phone: normalizePhone(values.phone),
          password: values.password,
          ...(values.dob ? { dob: values.dob } : {}),
        })
        navigate(HOME_PATH_BY_ROLE[user.role], { replace: true })
      } catch (error) {
        const problem = getProblemDetail(error)
        const fieldErrors = { ...problem.errors }
        const conflictField = problem.detail?.toLowerCase().includes('email')
          ? 'email'
          : problem.detail?.toLowerCase().includes('số điện thoại')
            ? 'phone'
            : null
        if (problem.status === 409 && problem.detail && conflictField) {
          fieldErrors[conflictField] = problem.detail
        }
        form.setFields(
          Object.entries(fieldErrors)
            .filter(([name]) =>
              ['fullName', 'email', 'phone', 'password', 'confirmPassword', 'dob'].includes(name),
            )
            .map(([name, errors]) => ({ name: name as keyof RegisterForm, errors: [errors] })),
        )
        setErrorDetail(
          problem.status === 409 && conflictField
            ? null
            : (problem.detail ?? 'Đăng ký chưa thành công. Vui lòng thử lại.'),
        )
      } finally {
        setPending(false)
      }
    })()
  }

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Tạo tài khoản hội viên
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Tạo hồ sơ tại chi nhánh mặc định.
        <br />
        Chưa bao gồm mua gói tập.
      </Typography.Paragraph>
      {errorDetail && (
        <Alert type="error" showIcon title={errorDetail} style={{ marginBottom: 16 }} />
      )}
      <Form<RegisterForm> form={form} layout="vertical" onFinish={handleFinish} disabled={pending}>
        <Form.Item
          label="Họ và tên"
          name="fullName"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập họ và tên' },
            { max: 255, message: 'Họ và tên tối đa 255 ký tự' },
          ]}
        >
          <Input placeholder="Nhập họ và tên" autoComplete="name" maxLength={255} />
        </Form.Item>
        <Form.Item
          label="Email"
          name="email"
          normalize={normalizeEmail}
          rules={[
            { required: true, message: 'Vui lòng nhập email' },
            { type: 'email', message: 'Email không hợp lệ' },
          ]}
        >
          <Input placeholder="ban@example.com" autoComplete="email" />
        </Form.Item>
        <Form.Item
          label="Số điện thoại"
          name="phone"
          rules={[
            { required: true, message: 'Vui lòng nhập số điện thoại' },
            {
              validator: (_, value: string | undefined) =>
                !value || isValidPhone(value)
                  ? Promise.resolve()
                  : Promise.reject(new Error('Nhập số di động 10 số, ví dụ 0912345678')),
            },
          ]}
        >
          <Input type="tel" inputMode="tel" placeholder="0xxxxxxxxx hoặc +84…" autoComplete="tel" />
        </Form.Item>
        <Form.Item
          label="Mật khẩu"
          name="password"
          rules={[
            { required: true, message: 'Vui lòng nhập mật khẩu' },
            {
              validator: (_, value: string | undefined) =>
                !value || isValidPassword(value)
                  ? Promise.resolve()
                  : Promise.reject(new Error('Mật khẩu cần ít nhất 8 ký tự, gồm chữ và số')),
            },
          ]}
        >
          <Input.Password
            placeholder="Tối thiểu 8 ký tự, có chữ và số"
            autoComplete="new-password"
          />
        </Form.Item>
        <Form.Item
          label="Nhập lại mật khẩu"
          name="confirmPassword"
          dependencies={['password']}
          rules={[
            { required: true, message: 'Vui lòng nhập lại mật khẩu' },
            ({ getFieldValue }) => ({
              validator(_, value: string | undefined) {
                return !value || getFieldValue('password') === value
                  ? Promise.resolve()
                  : Promise.reject(new Error('Mật khẩu nhập lại chưa khớp'))
              },
            }),
          ]}
        >
          <Input.Password placeholder="Nhập lại mật khẩu" autoComplete="new-password" />
        </Form.Item>
        <Form.Item label="Ngày sinh (không bắt buộc)" name="dob">
          <Input type="date" />
        </Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          block
          loading={pending}
          aria-label="Đăng ký"
          aria-busy={pending}
        >
          Đăng ký
        </Button>
      </Form>
      <Typography.Paragraph style={{ margin: '16px 0 0', textAlign: 'center' }}>
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </Typography.Paragraph>
      <Typography.Paragraph style={{ margin: '8px 0 0', textAlign: 'center' }}>
        <Link to="/">Về trang khách</Link>
      </Typography.Paragraph>
    </>
  )
}
