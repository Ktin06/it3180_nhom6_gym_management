import { render, screen, waitFor } from '@testing-library/react'
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

function makeAvatarError(status: number, data: Record<string, unknown>) {
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
    const file = new File(['valid-png'], 'chan-dung.png', { type: 'image/png' })
    const input = screen.getByLabelText('Chọn file ảnh đại diện')
    await user.upload(input, file)

    await waitFor(() => expect(uploadAvatar).toHaveBeenCalledWith(member.id, file))
    const img = await screen.findByRole('img', { name: `Ảnh đại diện ${member.fullName}` })
    expect(img).toHaveAttribute('src', 'blob:uploaded-avatar')
    expect(getAvatar).toHaveBeenCalledTimes(2)
    expect(screen.getByLabelText('Họ và tên')).toHaveValue(member.fullName)
  })

  it('TC-F2.4.2-03c: báo lỗi sớm với file sai định dạng hoặc vượt quá 5 MB mà không gọi API', async () => {
    const user = userEvent.setup({ applyAccept: false })
    const api = createMockMembersApi()
    const member = await api.detail(1)
    const uploadAvatar = vi.spyOn(api, 'uploadAvatar')
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:exact-5mb')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    render(<MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />)

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
        makeAvatarError(400, {
          detail: 'Vui lòng kiểm tra lại các trường dữ liệu.',
          errors: { file: 'Dung lượng file tải lên vượt quá giới hạn cho phép (tối đa 5 MB).' },
        }),
      )
      .mockRejectedValueOnce(
        makeAvatarError(400, {
          detail: 'Kích thước ảnh không được vượt quá 8000x8000 điểm ảnh.',
        }),
      )

    render(<MemberFormModal member={member} api={api} onClose={vi.fn()} onSaved={vi.fn()} />)

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
