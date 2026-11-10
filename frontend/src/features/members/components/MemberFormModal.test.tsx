import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { createMockMembersApi } from '../api/mock-members'
import { MemberFormModal } from './MemberFormModal'

describe('form hồ sơ hội viên', () => {
  it('hiển thị lỗi validation do backend trả về ngay dưới trường tương ứng', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const error = new AxiosError('Bad request')
    error.response = {
      status: 400,
      data: {
        detail: 'Dữ liệu không hợp lệ.',
        errors: { phone: 'Số điện thoại bị từ chối từ server.' },
      },
      statusText: 'Bad Request',
      headers: {},
      config: { headers: new AxiosHeaders() },
    }
    vi.spyOn(api, 'create').mockRejectedValue(error)
    render(<MemberFormModal member={null} api={api} onClose={vi.fn()} onSaved={vi.fn()} />)
    await user.type(screen.getByLabelText('Họ và tên'), 'Hội viên thử')
    await user.type(screen.getByLabelText('Số điện thoại'), '0912345678')
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
    expect(await screen.findByText('Số điện thoại bị từ chối từ server.')).toBeInTheDocument()
  })
  it.each(['+84 912 345 678', '84 912 345 678', '0912345678'])(
    'kiểm tra thông tin bắt buộc và gửi đúng DTO với số %s',
    async (phone) => {
      const user = userEvent.setup()
      const api = createMockMembersApi()
      const create = vi.spyOn(api, 'create')
      const onSaved = vi.fn()
      render(<MemberFormModal member={null} api={api} onClose={vi.fn()} onSaved={onSaved} />)
      await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
      expect(await screen.findByText('Vui lòng nhập họ và tên.')).toBeInTheDocument()
      expect(create).not.toHaveBeenCalled()
      await user.type(screen.getByLabelText('Họ và tên'), '  Hội viên thử  ')
      await user.type(screen.getByLabelText('Số điện thoại'), phone)
      await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
      await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
      expect(create).toHaveBeenCalledWith({
        fullName: 'Hội viên thử',
        phone,
        gender: null,
        dob: null,
      })
    },
  )

  it('giữ form và dữ liệu đã nhập khi server từ chối số điện thoại trùng', async () => {
    const user = userEvent.setup()
    const onSaved = vi.fn()
    render(
      <MemberFormModal
        member={null}
        api={createMockMembersApi()}
        onClose={vi.fn()}
        onSaved={onSaved}
      />,
    )
    await user.type(screen.getByLabelText('Họ và tên'), 'Hội viên thử')
    await user.type(screen.getByLabelText('Số điện thoại'), '0900000001')
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
    expect(await screen.findByText(/đã được đăng ký cho hội viên khác/)).toBeInTheDocument()
    expect(screen.getByLabelText('Họ và tên')).toHaveValue('Hội viên thử')
    expect(onSaved).not.toHaveBeenCalled()
  })

  it('sửa hồ sơ gửi đầy đủ trạng thái và giữ giá trị tùy chọn đang có', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const update = vi.spyOn(api, 'update')
    const member = { ...(await api.detail(1)), dob: '2000-02-20', gender: 'OTHER' as const }
    render(<MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />)
    expect(screen.getByText(/đồng thời vô hiệu hóa/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(1, {
        fullName: member.fullName,
        phone: member.phone,
        dob: '2000-02-20',
        gender: 'OTHER',
        status: 'INACTIVE',
      }),
    )
  })
})
