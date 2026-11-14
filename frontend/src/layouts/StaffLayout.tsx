import { MenuOutlined } from '@ant-design/icons'
import { Button, Drawer, Grid, Layout, Menu, Typography, type MenuProps } from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { menuForRole } from './staff-menu'
import { UserMenu } from './UserMenu'

const { Header, Sider, Content } = Layout

/** Giao diện nhân viên: menu bên trái theo vai trò; màn hình hẹp thì menu thu vào ngăn kéo. */
export function StaffLayout() {
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const screens = Grid.useBreakpoint()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const isWide = screens.lg ?? false

  const items: MenuProps['items'] = (user ? menuForRole(user.role) : []).map((item) => ({
    key: item.path,
    icon: item.icon,
    label: item.ready ? item.label : `${item.label} – sẽ bổ sung`,
    disabled: !item.ready,
  }))

  const menu = (
    <Menu
      mode="inline"
      selectedKeys={[location.pathname]}
      items={items}
      onClick={({ key }) => {
        navigate(key)
        setDrawerOpen(false)
      }}
      style={{ borderInlineEnd: 0 }}
    />
  )

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {isWide && (
        <Sider width={240} theme="light">
          <div style={{ padding: 16 }}>
            <Typography.Title level={5} style={{ margin: 0 }}>
              Gym Nhóm 6
            </Typography.Title>
          </div>
          {menu}
        </Sider>
      )}
      <Layout>
        <Header style={headerStyle}>
          {isWide ? (
            <span />
          ) : (
            <Button
              type="text"
              icon={<MenuOutlined />}
              aria-label="Mở menu"
              onClick={() => setDrawerOpen(true)}
            />
          )}
          <UserMenu />
        </Header>
        <Content style={{ padding: 16 }}>
          <Outlet />
        </Content>
      </Layout>
      <Drawer
        title="Gym Nhóm 6"
        placement="left"
        size={260}
        open={!isWide && drawerOpen}
        onClose={() => setDrawerOpen(false)}
        styles={{ body: { padding: 0 } }}
      >
        {menu}
      </Drawer>
    </Layout>
  )
}

const headerStyle = {
  background: '#fff',
  padding: '12px 16px',
  height: 'auto',
  minHeight: 64,
  lineHeight: 'normal',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 8,
}
