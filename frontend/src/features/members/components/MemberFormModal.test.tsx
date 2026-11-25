import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { AuthContext, type AuthContextValue } from '../../../auth/AuthContext'
import type { Role } from '../../../auth/roles'
import { createMockMembersApi } from '../api/mock-members'
import { MemberFormModal } from './MemberFormModal'

function authValue(role: Role): AuthContextValue {
  return {
    status: 'authenticated',
    user: { email: `${role.toLowerCase()}@gym.local`, role },
    sessionError: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    retrySession: vi.fn(),
  }
}

function makeAccountError(status: number, data: Record<string, unknown>) {
  const error = new AxiosError('Bad Request')
  error.response = {
    status,
    data,
    statusText: String(status),
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

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

  it('TC-F2.2.1-01a: tạo tài khoản thành công báo đã gửi email, tải lại hồ sơ và ẩn nút tạo tài khoản', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(2)
    const createAccount = vi.spyOn(api, 'createAccount')
    const detail = vi.spyOn(api, 'detail')
    const onAccountCreated = vi.fn()

    render(
      <AuthContext.Provider value={authValue('RECEPTIONIST')}>
        <MemberFormModal
          member={member}
          api={api}
          onClose={vi.fn()}
          onSaved={vi.fn()}
          onAccountCreated={onAccountCreated}
        />
      </AuthContext.Provider>,
    )

    await user.click(screen.getByRole('button', { name: /Tạo tài khoản/ }))
    await user.type(screen.getByLabelText('Email đăng nhập'), '  HoiVien@Example.COM ')
    await user.click(screen.getByRole('button', { name: 'Xác nhận tạo tài khoản' }))

    await waitFor(() =>
      expect(createAccount).toHaveBeenCalledWith(member.id, { email: 'HoiVien@Example.COM' }),
    )
    expect(
      await screen.findByText('Đã gửi email đặt mật khẩu tới hoivien@example.com.'),
    ).toBeInTheDocument()
    expect(screen.getByText('hoivien@example.com')).toBeInTheDocument()
    expect(detail).toHaveBeenCalledWith(member.id)
    expect(onAccountCreated).toHaveBeenCalledOnce()
    expect(screen.queryByRole('button', { name: /Tạo tài khoản/ })).not.toBeInTheDocument()
  })

  it('TC-F2.2.1-03a, TC-F2.2.1-05a: hồ sơ đã có tài khoản hiện email và không có nút tạo tài khoản; hồ sơ ngừng hoạt động không có nút', async () => {
    const api = createMockMembersApi()
    const linkedMember = {
      ...(await api.detail(2)),
      userId: 99,
      email: 'da-co@example.com',
    }
    const inactiveMember = await api.detail(1)

    const { rerender } = render(
      <AuthContext.Provider value={authValue('BRANCH_MANAGER')}>
        <MemberFormModal member={linkedMember} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    expect(screen.getByText('da-co@example.com')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Tạo tài khoản/ })).not.toBeInTheDocument()

    rerender(
      <AuthContext.Provider value={authValue('SUPER_ADMIN')}>
        <MemberFormModal
          key={inactiveMember.id}
          member={inactiveMember}
          api={api}
          onClose={vi.fn()}
          onSaved={vi.fn()}
        />
      </AuthContext.Provider>,
    )

    expect(screen.queryByRole('button', { name: /Tạo tài khoản/ })).not.toBeInTheDocument()
  })

  it('TC-F2.2.1-07a: hội viên không thấy nút tạo tài khoản', async () => {
    const api = createMockMembersApi()
    const member = await api.detail(2)

    render(
      <AuthContext.Provider value={authValue('MEMBER')}>
        <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    expect(screen.queryByRole('button', { name: /Tạo tài khoản/ })).not.toBeInTheDocument()
  })

  it('TC-F2.2.1-07b: kiểm tra định dạng email ở trình duyệt trước khi gọi API và hiển thị lỗi 400', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(2)
    const createAccount = vi.spyOn(api, 'createAccount').mockRejectedValueOnce(
      makeAccountError(400, {
        detail: 'Vui lòng kiểm tra lại các trường dữ liệu.',
        errors: { email: 'Email không đúng định dạng' },
      }),
    )

    render(
      <AuthContext.Provider value={authValue('RECEPTIONIST')}>
        <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    await user.click(screen.getByRole('button', { name: /Tạo tài khoản/ }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận tạo tài khoản' }))
    expect(await screen.findByText('Vui lòng nhập email.')).toBeInTheDocument()
    expect(createAccount).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('Email đăng nhập'), 'khong-phai-email')
    await user.click(screen.getByRole('button', { name: 'Xác nhận tạo tài khoản' }))
    expect(await screen.findByText('Email không đúng định dạng.')).toBeInTheDocument()
    expect(createAccount).not.toHaveBeenCalled()

    await user.clear(screen.getByLabelText('Email đăng nhập'))
    await user.type(screen.getByLabelText('Email đăng nhập'), 'hop-le@example.com')
    await user.click(screen.getByRole('button', { name: 'Xác nhận tạo tài khoản' }))
    expect(await screen.findByText('Email không đúng định dạng')).toBeInTheDocument()
    expect(createAccount).toHaveBeenCalledOnce()
  })

  it('TC-F2.2.1-03b, TC-F2.2.1-04a, TC-F2.2.1-05b, TC-F2.2.1-07c: lỗi 409, 422 và 404 hiện đúng câu detail của API', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(2)
    const createAccount = vi
      .spyOn(api, 'createAccount')
      .mockRejectedValueOnce(
        makeAccountError(409, { detail: 'Hội viên này đã có tài khoản đăng nhập.' }),
      )
      .mockRejectedValueOnce(
        makeAccountError(409, { detail: 'Email này đã được sử dụng bởi một tài khoản khác.' }),
      )
      .mockRejectedValueOnce(
        makeAccountError(422, {
          detail: 'Chỉ có thể tạo tài khoản cho hồ sơ hội viên đang hoạt động.',
        }),
      )
      .mockRejectedValueOnce(makeAccountError(404, { detail: 'Không tìm thấy hội viên.' }))

    render(
      <AuthContext.Provider value={authValue('RECEPTIONIST')}>
        <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    await user.click(screen.getByRole('button', { name: /Tạo tài khoản/ }))
    await user.type(screen.getByLabelText('Email đăng nhập'), 'hoivien@example.com')

    await user.click(screen.getByRole('button', { name: /Xác nhận tạo tài khoản/ }))
    expect(await screen.findByText('Hội viên này đã có tài khoản đăng nhập.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Xác nhận tạo tài khoản/ }))
    expect(
      await screen.findByText('Email này đã được sử dụng bởi một tài khoản khác.'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Xác nhận tạo tài khoản/ }))
    expect(
      await screen.findByText('Chỉ có thể tạo tài khoản cho hồ sơ hội viên đang hoạt động.'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Xác nhận tạo tài khoản/ }))
    expect(await screen.findByText('Không tìm thấy hội viên.')).toBeInTheDocument()
    expect(createAccount).toHaveBeenCalledTimes(4)
  })
})
