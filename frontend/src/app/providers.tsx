import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import type { ReactNode } from 'react'
import { AuthProvider } from '../auth/AuthProvider'

dayjs.locale('vi')

/** Bọc toàn ứng dụng: giao diện tiếng Việt cho Ant Design và dayjs, trạng thái đăng nhập. */
export function Providers({ children }: { children: ReactNode }) {
  return (
    <ConfigProvider locale={viVN}>
      <AntApp>
        <AuthProvider>{children}</AuthProvider>
      </AntApp>
    </ConfigProvider>
  )
}
