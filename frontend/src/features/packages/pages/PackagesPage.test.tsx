import { App } from 'antd'
import type { ReactNode } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { mockPackagesApi } from '../api/mock-packages'
import { PackagesPage } from './PackagesPage'
import type { PackagesGateway } from '../types/package'
import { AuthContext, type AuthContextValue } from '../../../auth/AuthContext'

const manager: AuthContextValue = { status: 'authenticated', user: { email: 'manager@example.com', role: 'BRANCH_MANAGER' }, sessionError: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), retrySession: vi.fn() }
const renderWithManager = (ui: ReactNode) => render(<App><AuthContext.Provider value={manager}>{ui}</AuthContext.Provider></App>)

describe('danh mục gói', () => {
  it('TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ nguyên lọc trạng thái', async () => {
    const user = userEvent.setup()
    const api: PackagesGateway = { list: vi.fn(mockPackagesApi.list), create: vi.fn(), update: vi.fn(), deactivate: vi.fn() }
    renderWithManager(<PackagesPage api={api} />)
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()
    expect(screen.queryByText('SESSION-20')).not.toBeInTheDocument()
    await user.click(screen.getByLabelText('Loại gói'))
    await user.click(await screen.findByTitle('Huấn luyện cá nhân'))
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith({
        page: 0,
        size: 20,
        type: 'PT',
        status: 'ACTIVE',
      }),
    )
    expect(await screen.findByText('PT-10')).toBeInTheDocument()
    expect(screen.queryByText('TIME-1M')).not.toBeInTheDocument()
  })

  it('TC-F3.2.1-01: quản lý thêm gói và gửi đúng dữ liệu tới API', async () => {
    const user = userEvent.setup()
    const api: PackagesGateway = { list: vi.fn(mockPackagesApi.list), create: vi.fn().mockResolvedValue({}), update: vi.fn(), deactivate: vi.fn() }
    renderWithManager(<PackagesPage api={api} />)
    await user.click(await screen.findByRole('button', { name: /Thêm gói/ }))
    await user.type(screen.getByLabelText('Mã gói'), 'TIME-TEST')
    await user.type(screen.getByLabelText('Tên gói'), 'Gói thử')
    await user.clear(screen.getByLabelText('Giá bán (VND)'))
    await user.type(screen.getByLabelText('Giá bán (VND)'), '100000')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    await waitFor(() => expect(api.create).toHaveBeenCalledWith(expect.objectContaining({ code: 'TIME-TEST', name: 'Gói thử', type: 'TIME', sessionCount: null, price: 100000 })))
  })

  it('TC-F3.3.1-08: ẩn thao tác quản lý với người không phải quản lý chi nhánh', async () => {
    const api: PackagesGateway = { list: vi.fn(mockPackagesApi.list), create: vi.fn(), update: vi.fn(), deactivate: vi.fn() }
    render(<App><PackagesPage api={api} /></App>)
    await screen.findByText('TIME-1M')
    expect(screen.queryByRole('button', { name: 'Thêm gói' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sửa TIME-1M' })).not.toBeInTheDocument()
  })
})
