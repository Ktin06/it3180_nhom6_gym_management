import { Button, Layout, Space, Typography } from 'antd'
import { Link, Outlet } from 'react-router-dom'

const { Header, Content, Footer } = Layout

/** Giao diện cho khách chưa đăng nhập: bảng giá, lịch lớp, đăng ký, đăng nhập. */
export function PublicLayout() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          background: '#fff',
          // Header của Ant Design cao cố định 64px; màn 360px cần giãn cao để nút xuống hàng không đè nhau.
          height: 'auto',
          minHeight: 64,
          lineHeight: 'normal',
          padding: '8px 16px',
          display: 'flex',
          flexWrap: 'wrap',
          gap: 8,
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Link to="/">
          <Typography.Text strong style={{ whiteSpace: 'nowrap' }}>
            Gym Nhóm 6
          </Typography.Text>
        </Link>
        <Space size={4} wrap>
          <Link to="/bang-gia">
            <Button type="text">Bảng giá</Button>
          </Link>
          <Link to="/lich-lop">
            <Button type="text">Lịch lớp</Button>
          </Link>
          <Link to="/register">
            <Button>Đăng ký</Button>
          </Link>
          <Link to="/login">
            <Button type="primary">Đăng nhập</Button>
          </Link>
        </Space>
      </Header>
      <Content style={{ padding: 16, width: '100%', maxWidth: 1080, margin: '0 auto' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>IT3180 – Nhóm 6</Footer>
    </Layout>
  )
}
