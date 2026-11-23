import { LockOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../../auth/useAuth'
import { errorMessage } from '../../shared/api-error'
import { changePassword, getProblemDetail } from '../api/auth-api'
import { isValidPassword } from '../api/validation'

interface ChangePasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const VALIDATED_FIELDS = ['currentPassword', 'newPassword'] as const

/** Màn hình đổi mật khẩu cho mọi vai trò đã đăng nhập (UC-F1.4). */
export function ChangePasswordPage() {
  const [form] = Form.useForm<ChangePasswordForm>()
  const [pending, setPending] = useState(false)
  const [errorDetail, setErrorDetail] = useState<string | null>(null)
  const { completePasswordChange } = useAuth()
  const navigate = useNavigate()

  const handleFinish = (values: ChangePasswordForm) => {
    setPending(true)
    setErrorDetail(null)
    void (async () => {
      try {
        await changePassword({
          currentPassword: values.currentPassword,
          newPassword: values.newPassword,
        })
        completePasswordChange?.()
        navigate('/login', { replace: true })
      } catch (error) {
        const problem = getProblemDetail(error)
        const fieldErrors = Object.entries(problem.errors).flatMap(([name, message]) => {
          const field = VALIDATED_FIELDS.find((candidate) => candidate === name)
          return field ? [{ name: field, errors: [message] }] : []
        })
        if (fieldErrors.length > 0) {
          form.setFields(fieldErrors)
        }
        setErrorDetail(problem.detail ?? (fieldErrors.length > 0 ? null : errorMessage(error)))
      } finally {
        setPending(false)
      }
    })()
  }

  return (
    <Card style={{ width: '100%', maxWidth: 480 }}>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Đổi mật khẩu
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Sau khi đổi mật khẩu thành công, toàn bộ phiên đăng nhập hiện có sẽ bị thu hồi và bạn cần
        đăng nhập lại.
      </Typography.Paragraph>
      {errorDetail && (
        <Alert type="error" showIcon title={errorDetail} style={{ marginBottom: 16 }} />
      )}
      <Form<ChangePasswordForm>
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        requiredMark={false}
        disabled={pending}
      >
        <Form.Item
          label="Mật khẩu hiện tại"
          name="currentPassword"
          rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }]}
        >
          <Input.Password
            prefix={<LockOutlined />}
            placeholder="Nhập mật khẩu hiện tại"
            autoComplete="current-password"
          />
        </Form.Item>
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
          Đổi mật khẩu
        </Button>
      </Form>
    </Card>
  )
}
