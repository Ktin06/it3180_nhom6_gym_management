import { Button, Layout, Typography } from 'antd'
import { Link, Outlet } from 'react-router-dom'

const { Header, Content, Footer } = Layout

/** Giao diện cho khách chưa đăng nhập: bảng giá, lịch lớp (sẽ bổ sung), đăng nhập. */
export function PublicLayout() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          background: '#fff',
          padding: '0 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Link to="/">
          <Typography.Text strong>Gym Nhóm 6</Typography.Text>
        </Link>
        <Link to="/login">
          <Button type="primary">Đăng nhập</Button>
        </Link>
      </Header>
      <Content style={{ padding: 16, width: '100%', maxWidth: 1080, margin: '0 auto' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>IT3180 – Nhóm 6</Footer>
    </Layout>
  )
}
