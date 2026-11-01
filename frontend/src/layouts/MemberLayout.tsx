import { CalendarOutlined, HomeOutlined, IdcardOutlined, UserOutlined } from '@ant-design/icons'
import { Layout, Typography } from 'antd'
import type { CSSProperties, ReactNode } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { UserMenu } from './UserMenu'

const { Header, Content } = Layout

interface NavItem {
  to: string
  label: string
  icon: ReactNode
  /** false khi chức năng chưa làm. */
  ready: boolean
}

const NAV_ITEMS: NavItem[] = [
  { to: '/member', label: 'Trang chủ', icon: <HomeOutlined />, ready: true },
  { to: '/member/memberships', label: 'Gói tập', icon: <IdcardOutlined />, ready: false }, // F7
  { to: '/member/classes', label: 'Lớp học', icon: <CalendarOutlined />, ready: false }, // F8
  { to: '/member/pt', label: 'PT', icon: <UserOutlined />, ready: false }, // F9
]

/** Giao diện hội viên, ưu tiên điện thoại: nội dung một cột, thanh điều hướng cố định ở đáy. */
export function MemberLayout() {
  return (
    <Layout style={{ minHeight: '100vh', paddingBottom: 64 }}>
      <Header
        style={{
          background: '#fff',
          padding: '0 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Typography.Text strong>Gym Nhóm 6</Typography.Text>
        <UserMenu />
      </Header>
      <Content style={{ padding: 16, width: '100%', maxWidth: 720, margin: '0 auto' }}>
        <Outlet />
      </Content>
      <nav aria-label="Điều hướng hội viên" style={bottomNavStyle}>
        {NAV_ITEMS.map((item) =>
          item.ready ? (
            <NavLink
              key={item.to}
              to={item.to}
              end
              style={({ isActive }) => ({
                ...navItemStyle,
                color: isActive ? '#1677ff' : 'rgba(0, 0, 0, 0.65)',
              })}
            >
              {item.icon}
              <span>{item.label}</span>
            </NavLink>
          ) : (
            <span
              key={item.to}
              title="Sẽ bổ sung"
              style={{ ...navItemStyle, color: 'rgba(0, 0, 0, 0.25)' }}
            >
              {item.icon}
              <span>{item.label}</span>
            </span>
          ),
        )}
      </nav>
    </Layout>
  )
}

const bottomNavStyle: CSSProperties = {
  position: 'fixed',
  insetInline: 0,
  bottom: 0,
  height: 56,
  display: 'flex',
  background: '#fff',
  borderTop: '1px solid #f0f0f0',
}

const navItemStyle: CSSProperties = {
  flex: 1,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: 12,
  gap: 2,
  textDecoration: 'none',
}
