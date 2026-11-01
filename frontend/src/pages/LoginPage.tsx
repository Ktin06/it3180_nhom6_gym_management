import { LockOutlined, MailOutlined } from '@ant-design/icons'
import { Alert, App, Button, Divider, Form, Input, Typography } from 'antd'
import { DevRoleSwitcher } from '../auth/DevRoleSwitcher'

interface LoginForm {
  email: string
  password: string
}

/**
 * Trang đăng nhập – hiện là giao diện tĩnh, CHƯA gọi API.
 * F1 sẽ gọi POST /api/v1/auth/login. Chỉ đăng nhập bằng email (bản chốt phạm vi, F1).
 */
export function LoginPage() {
  const { message } = App.useApp()

  const handleFinish = () => {
    message.info('Đăng nhập sẽ được bổ sung ở chức năng F1.')
  }

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Đăng nhập
      </Typography.Title>
      <Form<LoginForm> layout="vertical" onFinish={handleFinish} requiredMark={false}>
        <Form.Item
          label="Email"
          name="email"
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
        <Button type="primary" htmlType="submit" block>
          Đăng nhập
        </Button>
      </Form>
      {import.meta.env.DEV && (
        <>
          <Divider plain>Chế độ phát triển</Divider>
          <Alert
            type="warning"
            showIcon
            title="Chọn vai trò để giả lập đăng nhập (sẽ thay bằng đăng nhập thật ở F1)."
            style={{ marginBottom: 12 }}
          />
          <DevRoleSwitcher />
        </>
      )}
    </>
  )
}
