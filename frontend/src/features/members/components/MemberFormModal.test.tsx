import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
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

function makeFormError(status: number, data: Record<string, unknown>) {
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

afterEach(() => {
  vi.restoreAllMocks()
})

describe('form hồ sơ hội viên', () => {
  it('TC-F2.1.1-02h: hiển thị lỗi validation do backend trả về ngay dưới trường tương ứng', async () => {
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

  it('TC-F2.1.1-02i: giữ form và dữ liệu đã nhập khi server từ chối số điện thoại trùng', async () => {
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

  it('TC-F2.4.1-01e: sửa hồ sơ gửi đầy đủ trạng thái và giữ giá trị tùy chọn đang có', async () => {
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

  it('Enter chỉ tạo tài khoản một lần và vẫn cho lưu hồ sơ sau khi tạo xong', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(2)
    const create = api.createAccount
    let finishAccount!: () => void
    const createAccount = vi
      .spyOn(api, 'createAccount')
      .mockImplementationOnce(async (id, input) => {
        await new Promise<void>((resolve) => {
          finishAccount = resolve
        })
        return create(id, input)
      })
    const update = vi.spyOn(api, 'update')
    const onSaved = vi.fn()
    const onClose = vi.fn()

    render(
      <AuthContext.Provider value={authValue('RECEPTIONIST')}>
        <MemberFormModal
          member={member}
          api={api}
          onClose={onClose}
          onSaved={onSaved}
          initialAccountFormOpen
        />
      </AuthContext.Provider>,
    )

    const email = screen.getByLabelText('Email đăng nhập')
    await user.type(email, 'hoivien@example.com{Enter}{Enter}')
    expect(createAccount).toHaveBeenCalledExactlyOnceWith(member.id, {
      email: 'hoivien@example.com',
    })
    expect(screen.getByRole('button', { name: 'Lưu hồ sơ' })).toBeDisabled()
    expect(update).not.toHaveBeenCalled()
    expect(onSaved).not.toHaveBeenCalled()

    await act(async () => finishAccount())
    expect(await screen.findByText('hoivien@example.com')).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
    expect(update).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    expect(update).toHaveBeenCalledOnce()
    expect(createAccount).toHaveBeenCalledOnce()
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

  it.each(['MEMBER', null] as const)(
    'TC-F2.2.1-07a: vai trò %s không thấy nút tạo tài khoản',
    async (role) => {
      const api = createMockMembersApi()
      const member = await api.detail(2)

      render(
        <AuthContext.Provider value={role ? authValue(role) : null}>
          <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
        </AuthContext.Provider>,
      )

      expect(screen.queryByRole('button', { name: /Tạo tài khoản/ })).not.toBeInTheDocument()
    },
  )

  it('TC-F2.2.1-07b: kiểm tra định dạng email ở trình duyệt trước khi gọi API và hiển thị lỗi 400', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(2)
    const createAccount = vi.spyOn(api, 'createAccount').mockRejectedValueOnce(
      makeFormError(400, {
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
        makeFormError(409, { detail: 'Hội viên này đã có tài khoản đăng nhập.' }),
      )
      .mockRejectedValueOnce(
        makeFormError(409, { detail: 'Email này đã được sử dụng bởi một tài khoản khác.' }),
      )
      .mockRejectedValueOnce(
        makeFormError(422, {
          detail: 'Chỉ có thể tạo tài khoản cho hồ sơ hội viên đang hoạt động.',
        }),
      )
      .mockRejectedValueOnce(makeFormError(404, { detail: 'Không tìm thấy hội viên.' }))

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

  it('TC-F2.4.2-08b: mở hồ sơ chưa có ảnh thì hiện chữ cái đầu và không gọi API ảnh', async () => {
    const api = createMockMembersApi()
    const member = { ...(await api.detail(1)), hasAvatar: false }
    const getAvatar = vi.spyOn(api, 'getAvatar')

    render(<MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />)

    expect(screen.getByText('A')).toBeInTheDocument()
    expect(getAvatar).not.toHaveBeenCalled()
  })

  it('TC-F2.4.2-01e: chọn ảnh JPEG/PNG hợp lệ gọi API tải lên và tải lại hiển thị ảnh mới', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(1)
    const uploadAvatar = vi.spyOn(api, 'uploadAvatar')
    const getAvatar = vi.spyOn(api, 'getAvatar')
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:uploaded-avatar')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    render(
      <AuthContext.Provider value={authValue('RECEPTIONIST')}>
        <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    expect(screen.getByText('A')).toBeInTheDocument()
    expect(getAvatar).not.toHaveBeenCalled()
    const file = new File(['valid-png'], 'chan-dung.png', { type: 'image/png' })
    const input = screen.getByLabelText('Chọn file ảnh đại diện')
    await user.upload(input, file)

    await waitFor(() => expect(uploadAvatar).toHaveBeenCalledWith(member.id, file))
    const img = await screen.findByRole('img', { name: `Ảnh đại diện ${member.fullName}` })
    expect(img).toHaveAttribute('src', 'blob:uploaded-avatar')
    expect(getAvatar).toHaveBeenCalledExactlyOnceWith(member.id)
    expect((await api.detail(member.id)).hasAvatar).toBe(true)
    expect(screen.getByLabelText('Họ và tên')).toHaveValue(member.fullName)
  })

  it('TC-F2.4.2-03c: báo lỗi sớm với file sai định dạng hoặc vượt quá 5 MB mà không gọi API', async () => {
    const user = userEvent.setup({ applyAccept: false })
    const api = createMockMembersApi()
    const member = await api.detail(1)
    const uploadAvatar = vi.spyOn(api, 'uploadAvatar')
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:exact-5mb')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    render(
      <AuthContext.Provider value={authValue('RECEPTIONIST')}>
        <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    const input = screen.getByLabelText('Chọn file ảnh đại diện')

    const pdfFile = new File(['%PDF-1.4'], 'ho-so.pdf', { type: 'application/pdf' })
    await user.upload(input, pdfFile)
    expect(
      await screen.findByText('Chỉ chấp nhận ảnh định dạng JPEG hoặc PNG.'),
    ).toBeInTheDocument()
    expect(uploadAvatar).not.toHaveBeenCalled()

    const oversizedFile = new File(['x'], 'anh-lon.jpg', { type: 'image/jpeg' })
    Object.defineProperty(oversizedFile, 'size', { value: 5 * 1024 * 1024 + 1 })
    await user.upload(input, oversizedFile)
    expect(await screen.findByText('Dung lượng ảnh không được vượt quá 5 MB.')).toBeInTheDocument()
    expect(uploadAvatar).not.toHaveBeenCalled()

    const boundaryFile = new File(['x'], 'anh-5mb.jpg', { type: 'image/jpeg' })
    Object.defineProperty(boundaryFile, 'size', { value: 5 * 1024 * 1024 })
    await user.upload(input, boundaryFile)
    await waitFor(() => expect(uploadAvatar).toHaveBeenCalledWith(member.id, boundaryFile))
  })

  it('TC-F2.4.2-03d: lỗi 400 từ API hiển thị đúng câu errors.file hoặc detail', async () => {
    const user = userEvent.setup()
    const api = createMockMembersApi()
    const member = await api.detail(1)
    const uploadAvatar = vi
      .spyOn(api, 'uploadAvatar')
      .mockRejectedValueOnce(
        makeFormError(400, {
          detail: 'Vui lòng kiểm tra lại các trường dữ liệu.',
          errors: { file: 'Dung lượng file tải lên vượt quá giới hạn cho phép (tối đa 5 MB).' },
        }),
      )
      .mockRejectedValueOnce(
        makeFormError(400, {
          detail: 'Kích thước ảnh không được vượt quá 8000x8000 điểm ảnh.',
        }),
      )

    render(
      <AuthContext.Provider value={authValue('RECEPTIONIST')}>
        <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    const input = screen.getByLabelText('Chọn file ảnh đại diện')
    await user.upload(input, new File(['a'], 'a.jpg', { type: 'image/jpeg' }))
    expect(
      await screen.findByText('Dung lượng file tải lên vượt quá giới hạn cho phép (tối đa 5 MB).'),
    ).toBeInTheDocument()

    await user.upload(input, new File(['b'], 'b.png', { type: 'image/png' }))
    expect(
      await screen.findByText('Kích thước ảnh không được vượt quá 8000x8000 điểm ảnh.'),
    ).toBeInTheDocument()
    expect(uploadAvatar).toHaveBeenCalledTimes(2)
  })

  it('TC-F2.4.2-05d: hội viên không thấy nút đổi ảnh', async () => {
    const api = createMockMembersApi()
    const member = await api.detail(1)

    render(
      <AuthContext.Provider value={authValue('MEMBER')}>
        <MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />
      </AuthContext.Provider>,
    )

    expect(screen.getByText('A')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /ảnh đại diện/i })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Chọn file ảnh đại diện')).not.toBeInTheDocument()
  })
})
