import { Alert } from 'antd'

export function DemoNotice() {
  return import.meta.env.DEV && import.meta.env.VITE_F2_F3_MOCK === 'true' ? (
    <Alert
      type="warning"
      showIcon
      title="Dữ liệu minh họa"
      description="Thay đổi chỉ lưu trong phiên chạy thử, chưa được lưu trên máy chủ."
      style={{ marginBottom: 16 }}
    />
  ) : null
}
