import { Typography } from 'antd'

/** Trang chủ tạm cho khách. Bảng giá (F3) và lịch lớp (F8) sẽ bổ sung. */
export function GuestHomePage() {
  return (
    <>
      <Typography.Title level={2}>Web quản lý chuỗi phòng tập gym</Typography.Title>
      <Typography.Paragraph>
        Trang dành cho khách: xem bảng giá, lịch lớp và đăng ký tài khoản (sẽ bổ sung).
      </Typography.Paragraph>
    </>
  )
}
