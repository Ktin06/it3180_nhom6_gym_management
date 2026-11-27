import { App } from 'antd'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../../auth/AuthContext'
import type { Role } from '../../../auth/roles'
import { createMockPackagesApi, mockPackagesApi } from '../api/mock-packages'
import type { PackagesGateway } from '../types/package'
import { PackagesPage } from './PackagesPage'

function renderWithRole(role: Role, api: PackagesGateway) {
  return render(
    <App>
      <AuthContext.Provider
        value={{
          status: 'authenticated',
          user: { email: `${role.toLowerCase()}@gym.local`, role },
          sessionError: null,
          login: vi.fn(),
          register: vi.fn(),
          logout: vi.fn(),
          retrySession: vi.fn(),
          completePasswordChange: vi.fn(),
        }}
      >
        <PackagesPage api={api} />
      </AuthContext.Provider>
    </App>,
  )
}

function axiosProblem(status: number, data: Record<string, unknown>) {
  const error = new AxiosError('Request failed')
  error.response = {
    status,
    data,
    statusText: 'Error',
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

describe('danh mục gói', () => {
  it('TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ nguyên lọc trạng thái', async () => {
    const user = userEvent.setup()
    const api = { ...mockPackagesApi, list: vi.fn(mockPackagesApi.list) }
    render(<PackagesPage api={api} />)
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

  it('TC-F3.1.1-03a: Chỉ quản lý chi nhánh thấy nút thêm, sửa, ngừng bán với gói chi nhánh mình; ẩn với gói toàn chuỗi và vai trò khác', async () => {
    for (const role of ['RECEPTIONIST', 'SUPER_ADMIN'] as const) {
      const api = createMockPackagesApi()
      const { unmount } = renderWithRole(role, api)
      expect(await screen.findByText('TIME-1M')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Thêm gói/ })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Sửa/ })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Ngừng bán/ })).not.toBeInTheDocument()
      unmount()
    }

    const managerApi = createMockPackagesApi()
    renderWithRole('BRANCH_MANAGER', managerApi)
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()
    expect(screen.getByText('TIME-6M')).toBeInTheDocument()

    expect(screen.getByRole('button', { name: /Thêm gói/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sửa TIME-1M' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ngừng bán TIME-1M' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sửa TIME-6M' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ngừng bán TIME-6M' })).not.toBeInTheDocument()
  })

  it('TC-F3.2.1-01c: Tạo gói mới từ trang danh mục đóng hộp thoại và tải lại danh sách', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    const create = vi.spyOn(api, 'create')
    const list = vi.spyOn(api, 'list')

    renderWithRole('BRANCH_MANAGER', api)
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Thêm gói/ }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Mã gói'), 'TIME-NEW')
    await user.type(within(dialog).getByLabelText('Tên gói'), 'Gói mới tạo')
    await user.type(within(dialog).getByLabelText('Thời hạn (ngày)'), '45')
    await user.type(within(dialog).getByLabelText('Giá bán (VND)'), '600000')
    await user.click(within(dialog).getByRole('button', { name: 'Lưu gói' }))

    await waitFor(() => expect(create).toHaveBeenCalledOnce())
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await waitFor(() => expect(list).toHaveBeenCalledTimes(2))
    expect(await screen.findByText('TIME-NEW')).toBeInTheDocument()
  })

  it('TC-F3.3.1-06b: Gói ngừng bán vẫn hiện nút Sửa cho quản lý chi nhánh và sửa không tự bật lại', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    const update = vi.spyOn(api, 'update')

    renderWithRole('BRANCH_MANAGER', api)
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Trạng thái'))
    await user.click(await screen.findByTitle('Ngừng bán'))
    expect(await screen.findByText('SESSION-20')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sửa SESSION-20' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ngừng bán SESSION-20' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Sửa SESSION-20' }))
    const dialog = await screen.findByRole('dialog')
    await user.clear(within(dialog).getByLabelText('Giá bán (VND)'))
    await user.type(within(dialog).getByLabelText('Giá bán (VND)'), '950000')
    await user.click(within(dialog).getByRole('button', { name: 'Lưu gói' }))

    await waitFor(() => expect(update).toHaveBeenCalledOnce())
    expect('status' in update.mock.calls[0][1]).toBe(false)
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(await screen.findByText('SESSION-20')).toBeInTheDocument()
    expect(screen.getByText(/950\.000/)).toBeInTheDocument()
  })

  it('TC-F3.4.1-01b, TC-F3.4.1-03b: Xác nhận ngừng bán gói ACTIVE và ẩn nút Ngừng bán với gói INACTIVE', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    const deactivate = vi.spyOn(api, 'deactivate')

    renderWithRole('BRANCH_MANAGER', api)
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Ngừng bán TIME-1M' }))
    const confirmCancel = await screen.findByRole('dialog')
    expect(within(confirmCancel).getByText('Ngừng bán gói này?')).toBeInTheDocument()
    await user.click(within(confirmCancel).getByRole('button', { name: 'Hủy' }))
    expect(deactivate).not.toHaveBeenCalled()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: 'Ngừng bán TIME-1M' }))
    const confirmOk = await screen.findByRole('dialog')
    await user.click(within(confirmOk).getByRole('button', { name: 'Ngừng bán' }))

    await waitFor(() => expect(deactivate).toHaveBeenCalledWith(1))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await waitFor(() => expect(screen.queryByText('TIME-1M')).not.toBeInTheDocument())

    await user.click(screen.getByLabelText('Trạng thái'))
    await user.click(await screen.findByTitle('Ngừng bán'))
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ngừng bán TIME-1M' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sửa TIME-1M' })).toBeInTheDocument()
  })

  it('TC-F3.4.1-04c, TC-F3.4.1-06h: Lỗi 422, 403, 404 khi ngừng bán hiển thị thông báo tiếng Việt', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    vi.spyOn(api, 'deactivate')
      .mockRejectedValueOnce(
        axiosProblem(422, { detail: 'Gói tập này đã ở trạng thái ngừng bán.' }),
      )
      .mockRejectedValueOnce(
        axiosProblem(403, { detail: 'Bạn không có quyền thực hiện thao tác này.' }),
      )
      .mockRejectedValueOnce(axiosProblem(404, { detail: 'Gói tập không tồn tại trên hệ thống.' }))

    renderWithRole('BRANCH_MANAGER', api)
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()

    for (const expectedText of [
      'Gói tập này đã ở trạng thái ngừng bán.',
      'Bạn không có quyền thực hiện thao tác này.',
      'Gói tập không tồn tại trên hệ thống.',
    ]) {
      await user.click(screen.getByRole('button', { name: 'Ngừng bán TIME-1M' }))
      const dialog = await screen.findByRole('dialog')
      await user.click(within(dialog).getByRole('button', { name: 'Ngừng bán' }))
      expect(await screen.findByText(expectedText)).toBeInTheDocument()
    }
  })
})
