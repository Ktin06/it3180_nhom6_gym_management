import { Card, Layout } from 'antd'
import { Outlet } from 'react-router-dom'

/** Khung cho trang đăng nhập, đăng ký: một thẻ ở giữa màn hình. */
export function AuthLayout() {
  return (
    <Layout
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
      }}
    >
      <Card style={{ width: '100%', maxWidth: 400 }}>
        <Outlet />
      </Card>
    </Layout>
  )
}
