import { App } from 'antd'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { createMockMembersApi } from '../api/mock-members'
import { MembersPage } from './MembersPage'
import type { MemberDetail } from '../types/member'

describe('danh sách hội viên', () => {
  it('giữ form thêm mới khi phản hồi tải hồ sơ sửa cũ đến muộn', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const oldMember = await api.detail(30)
    let completeDetail!: (member: MemberDetail) => void
    const detail = vi.spyOn(api, 'detail').mockImplementationOnce(
      () =>
        new Promise<MemberDetail>((resolve) => {
          completeDetail = resolve
        }),
    )
    const create = vi.spyOn(api, 'create')
    const update = vi.spyOn(api, 'update')
    render(
      <App>
        <MembersPage api={api} />
      </App>,
    )
    await screen.findByText('HV000030')
    await user.click(screen.getByRole('button', { name: 'Sửa HV000030' }))
    expect(detail).toHaveBeenCalledWith(30)
    await user.click(screen.getByRole('button', { name: /Thêm hội viên/ }))
    await user.type(screen.getByLabelText('Họ và tên'), 'Hồ sơ mới')
    await user.type(screen.getByLabelText('Số điện thoại'), '0912009999')

    await act(async () => completeDetail(oldMember))

    expect(screen.getByRole('dialog').querySelector('.ant-modal-title')).toHaveTextContent(
      'Thêm hội viên',
    )
    expect(screen.getByLabelText('Họ và tên')).toHaveValue('Hồ sơ mới')
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        fullName: 'Hồ sơ mới',
        phone: '0912009999',
        gender: null,
        dob: null,
      }),
    )
    expect(update).not.toHaveBeenCalled()
  })

  it('thử lại đúng hồ sơ cần sửa sau lỗi tải chi tiết, không chỉ tải lại danh sách', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(30)
    const detail = vi
      .spyOn(api, 'detail')
      .mockRejectedValueOnce(new Error('Không tải được hồ sơ cần sửa'))
      .mockResolvedValueOnce(member)
    const list = vi.spyOn(api, 'list')
    render(
      <App>
        <MembersPage api={api} />
      </App>,
    )
    await screen.findByText('HV000030')
    await user.click(screen.getByRole('button', { name: 'Sửa HV000030' }))
    await screen.findByText('Không tải được hồ sơ cần sửa')

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    await waitFor(() => expect(detail).toHaveBeenCalledTimes(2))
    expect(detail).toHaveBeenLastCalledWith(30)
    expect(list).toHaveBeenCalledTimes(1)
    const dialog = await screen.findByRole('dialog')
    expect(dialog.querySelector('.ant-modal-title')).toHaveTextContent('Sửa hồ sơ HV000030')
    expect(screen.getByLabelText('Họ và tên')).toHaveValue(member.fullName)
    expect(screen.queryByText('Không tải được hồ sơ cần sửa')).not.toBeInTheDocument()
  })

  it('đổi trang gọi API từ 0, tìm kiếm mới trở về trang đầu và có thể xóa tìm kiếm', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const list = vi.spyOn(api, 'list')
    render(
      <App>
        <MembersPage api={api} />
      </App>,
    )
    expect(await screen.findByText('HV000030')).toBeInTheDocument()
    await user.click(screen.getByTitle('2'))
    await waitFor(() => expect(list).toHaveBeenLastCalledWith({ page: 1, size: 20 }))
    await user.type(screen.getByLabelText('Tìm hội viên'), 'HV000001')
    await user.click(screen.getByRole('button', { name: /Tìm$/ }))
    await waitFor(() =>
      expect(list).toHaveBeenLastCalledWith({ page: 0, size: 20, keyword: 'HV000001' }),
    )
    expect(await screen.findByText('HV000001')).toBeInTheDocument()
    expect(screen.queryByText('HV000030')).not.toBeInTheDocument()
    await user.click(screen.getByLabelText('Tìm hội viên'))
    await user.keyboard('{Control>}a{/Control}{Backspace}{Enter}')
    await waitFor(() =>
      expect(list).toHaveBeenLastCalledWith({ page: 0, size: 20, keyword: undefined }),
    )
    expect(await screen.findByText('HV000030')).toBeInTheDocument()
  })

  it('hiển thị lỗi tải dữ liệu và cho thử lại', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    vi.spyOn(api, 'list').mockRejectedValueOnce(new Error('Mất kết nối máy chủ'))
    render(
      <App>
        <MembersPage api={api} />
      </App>,
    )
    expect(await screen.findByText('Mất kết nối máy chủ')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('HV000030')).toBeInTheDocument()
    expect(screen.queryByText('Mất kết nối máy chủ')).not.toBeInTheDocument()
  })
})
