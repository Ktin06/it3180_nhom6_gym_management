import { Button, Space, Typography } from 'antd'
import { Link } from 'react-router-dom'

/** Trang chủ tạm cho khách. Lịch lớp (F8) sẽ bổ sung. */
export function GuestHomePage() {
  return (
    <>
      <Typography.Title level={2}>Web quản lý chuỗi phòng tập gym</Typography.Title>
      <Typography.Paragraph>
        Trang dành cho khách: xem bảng giá, lịch lớp và tạo tài khoản hội viên.
      </Typography.Paragraph>
      <Space wrap>
        <Link to="/bang-gia">
          <Button>Xem bảng giá</Button>
        </Link>
        <Link to="/register">
          <Button type="primary">Đăng ký hội viên</Button>
        </Link>
        <Link to="/login">
          <Button>Đăng nhập</Button>
        </Link>
      </Space>
    </>
  )
}
