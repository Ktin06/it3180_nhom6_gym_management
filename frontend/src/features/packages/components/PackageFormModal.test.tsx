import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import { createMockPackagesApi } from '../api/mock-packages'
import type { GymPackage } from '../types/package'
import { PackageFormModal } from './PackageFormModal'

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

async function fill(user: ReturnType<typeof userEvent.setup>, label: string, text: string) {
  const input = screen.getByLabelText(label)
  await user.clear(input)
  if (text) {
    await user.click(input)
    await user.paste(text)
  }
}

describe('form thêm và sửa gói tập F3', () => {
  it('TC-F3.2.1-01b: Thêm gói TIME hợp lệ gửi đúng DTO không chứa sessionCount và gọi onSaved', async () => {
    const user = userEvent.setup()
    const timeApi = createMockPackagesApi()
    const createTime = vi.spyOn(timeApi, 'create')
    const onSavedTime = vi.fn()
    render(<PackageFormModal pkg={null} api={timeApi} onClose={vi.fn()} onSaved={onSavedTime} />)
    await fill(user, 'Mã gói', '  TIME-NEW  ')
    await fill(user, 'Tên gói', '  Gói 1 tháng mới  ')
    await fill(user, 'Thời hạn (ngày)', '30')
    await fill(user, 'Giá bán (VND)', '500000')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    await waitFor(() => expect(onSavedTime).toHaveBeenCalledOnce())
    expect(createTime).toHaveBeenCalledWith({
      code: 'TIME-NEW',
      name: 'Gói 1 tháng mới',
      type: 'TIME',
      durationDays: 30,
      price: 500000,
      description: null,
    })
    expect('sessionCount' in createTime.mock.calls[0][0]).toBe(false)
  })

  it('TC-F3.2.1-01d: Thêm gói SESSION hợp lệ gửi đúng DTO có số buổi và mô tả', async () => {
    const user = userEvent.setup()
    const sessionApi = createMockPackagesApi()
    const createSession = vi.spyOn(sessionApi, 'create')
    const onSavedSession = vi.fn()
    render(
      <PackageFormModal pkg={null} api={sessionApi} onClose={vi.fn()} onSaved={onSavedSession} />,
    )
    await user.click(screen.getByLabelText('Loại gói'))
    await user.click(await screen.findByTitle('Theo buổi'))
    await fill(user, 'Mã gói', 'SES-10')
    await fill(user, 'Tên gói', 'Gói 10 buổi')
    await fill(user, 'Thời hạn (ngày)', '60')
    await fill(user, 'Số buổi', '10')
    await fill(user, 'Giá bán (VND)', '650000')
    await fill(user, 'Mô tả (tùy chọn)', '  Tập theo buổi  ')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    await waitFor(() => expect(onSavedSession).toHaveBeenCalledOnce())
    expect(createSession).toHaveBeenCalledWith({
      code: 'SES-10',
      name: 'Gói 10 buổi',
      type: 'SESSION',
      durationDays: 60,
      sessionCount: 10,
      price: 650000,
      description: 'Tập theo buổi',
    })
  })

  it('TC-F3.2.1-01e: Thêm gói PT hợp lệ gửi đúng DTO có số buổi và gọi onSaved', async () => {
    const user = userEvent.setup()
    const ptApi = createMockPackagesApi()
    const createPt = vi.spyOn(ptApi, 'create')
    const onSavedPt = vi.fn()
    render(<PackageFormModal pkg={null} api={ptApi} onClose={vi.fn()} onSaved={onSavedPt} />)
    await user.click(screen.getByLabelText('Loại gói'))
    await user.click(await screen.findByTitle('Huấn luyện cá nhân'))
    await fill(user, 'Mã gói', 'PT-10B')
    await fill(user, 'Tên gói', 'Gói PT 10 buổi')
    await fill(user, 'Thời hạn (ngày)', '90')
    await fill(user, 'Số buổi', '10')
    await fill(user, 'Giá bán (VND)', '3000000')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    await waitFor(() => expect(onSavedPt).toHaveBeenCalledOnce())
    expect(createPt).toHaveBeenCalledWith({
      code: 'PT-10B',
      name: 'Gói PT 10 buổi',
      type: 'PT',
      durationDays: 90,
      sessionCount: 10,
      price: 3000000,
      description: null,
    })
  })

  it('TC-F3.2.1-02b: Gói TIME ẩn ô số buổi và không gửi sessionCount; SESSION và PT bắt buộc số buổi dương', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    const create = vi.spyOn(api, 'create')
    render(<PackageFormModal pkg={null} api={api} onClose={vi.fn()} onSaved={vi.fn()} />)

    expect(screen.queryByLabelText('Số buổi')).not.toBeInTheDocument()

    await user.click(screen.getByLabelText('Loại gói'))
    await user.click(await screen.findByTitle('Theo buổi'))
    expect(screen.getByLabelText('Số buổi')).toBeInTheDocument()

    await fill(user, 'Mã gói', 'SES-CHECK')
    await fill(user, 'Tên gói', 'Gói kiểm tra số buổi')
    await fill(user, 'Thời hạn (ngày)', '30')
    await fill(user, 'Giá bán (VND)', '500000')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    expect(await screen.findByText('Vui lòng nhập số buổi.')).toBeInTheDocument()
    expect(create).not.toHaveBeenCalled()

    await fill(user, 'Số buổi', '0')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    expect(await screen.findByText('Số buổi phải là số nguyên lớn hơn 0.')).toBeInTheDocument()
    expect(create).not.toHaveBeenCalled()

    await user.click(screen.getByLabelText('Loại gói'))
    await user.click(await screen.findByTitle('Huấn luyện cá nhân'))
    expect(screen.getByLabelText('Số buổi')).toBeInTheDocument()
    await fill(user, 'Số buổi', '-2')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    expect(await screen.findByText('Số buổi phải là số nguyên lớn hơn 0.')).toBeInTheDocument()
    expect(create).not.toHaveBeenCalled()

    await user.click(screen.getByLabelText('Loại gói'))
    await user.click(await screen.findByTitle('Theo thời gian'))
    expect(screen.queryByLabelText('Số buổi')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    await waitFor(() => expect(create).toHaveBeenCalledOnce())
    expect('sessionCount' in create.mock.calls[0][0]).toBe(false)
  })

  it('TC-F3.2.1-03d: Kiểm tra đầu vào, cho phép giá 0 đồng và gán lỗi 400 theo trường từ API', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    const create = vi.spyOn(api, 'create').mockRejectedValueOnce(
      axiosProblem(400, {
        detail: 'Vui lòng kiểm tra lại các trường dữ liệu.',
        errors: { code: 'Mã gói bị từ chối từ server.' },
      }),
    )
    const onSaved = vi.fn()
    render(<PackageFormModal pkg={null} api={api} onClose={vi.fn()} onSaved={onSaved} />)

    await fill(user, 'Mã gói', 'A'.repeat(31))
    await fill(user, 'Tên gói', 'B'.repeat(256))
    await fill(user, 'Thời hạn (ngày)', '0')
    await fill(user, 'Giá bán (VND)', '1.5')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))

    expect(await screen.findByText('Mã gói tối đa 30 ký tự.')).toBeInTheDocument()
    expect(screen.getByText('Tên gói tối đa 255 ký tự.')).toBeInTheDocument()
    expect(screen.getByText('Thời hạn phải là số nguyên lớn hơn 0.')).toBeInTheDocument()
    expect(screen.getByText('Giá bán phải là số nguyên từ 0 trở lên.')).toBeInTheDocument()
    expect(create).not.toHaveBeenCalled()

    await fill(user, 'Mã gói', 'FREE-1D')
    await fill(user, 'Tên gói', 'Gói trải nghiệm 0 đồng')
    await fill(user, 'Thời hạn (ngày)', '1')
    await fill(user, 'Giá bán (VND)', '0')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))

    expect(await screen.findByText('Mã gói bị từ chối từ server.')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng kiểm tra lại các trường dữ liệu.')).toBeInTheDocument()
    expect(screen.getByLabelText('Tên gói')).toHaveValue('Gói trải nghiệm 0 đồng')
    expect(onSaved).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    expect(create).toHaveBeenLastCalledWith(
      expect.objectContaining({ code: 'FREE-1D', durationDays: 1, price: 0 }),
    )
  })

  it('TC-F3.2.1-05c: Lỗi 409 trùng mã gói hiển thị thông báo tiếng Việt và giữ dữ liệu form', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    vi.spyOn(api, 'create').mockRejectedValueOnce(
      axiosProblem(409, { detail: "Mã gói 'TIME-1M' đã tồn tại trên hệ thống." }),
    )
    const onSaved = vi.fn()
    render(<PackageFormModal pkg={null} api={api} onClose={vi.fn()} onSaved={onSaved} />)

    await fill(user, 'Mã gói', 'TIME-1M')
    await fill(user, 'Tên gói', 'Gói trùng mã')
    await fill(user, 'Thời hạn (ngày)', '30')
    await fill(user, 'Giá bán (VND)', '500000')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))

    expect(
      await screen.findByText("Mã gói 'TIME-1M' đã tồn tại trên hệ thống."),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Mã gói')).toHaveValue('TIME-1M')
    expect(screen.getByLabelText('Tên gói')).toHaveValue('Gói trùng mã')
    expect(onSaved).not.toHaveBeenCalled()
  })

  it('TC-F3.2.1-06d: Lỗi 403 khi tạo gói hiển thị thông báo tiếng Việt từ API', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    vi.spyOn(api, 'create').mockRejectedValueOnce(
      axiosProblem(403, { detail: 'Bạn không có quyền thực hiện thao tác này.' }),
    )
    const onSaved = vi.fn()
    render(<PackageFormModal pkg={null} api={api} onClose={vi.fn()} onSaved={onSaved} />)

    await fill(user, 'Mã gói', 'TIME-NEW')
    await fill(user, 'Tên gói', 'Gói mới')
    await fill(user, 'Thời hạn (ngày)', '30')
    await fill(user, 'Giá bán (VND)', '500000')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))

    expect(
      await screen.findByText('Bạn không có quyền thực hiện thao tác này.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Tên gói')).toHaveValue('Gói mới')
    expect(onSaved).not.toHaveBeenCalled()
  })

  it('TC-F3.3.1-01c, TC-F3.3.1-04c: Sửa gói giữ nguyên và không gửi code/type, cập nhật đúng trường', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    const update = vi.spyOn(api, 'update')
    const onSaved = vi.fn()
    const pkg: GymPackage = {
      id: 3,
      code: 'SESSION-10',
      name: 'Gói 10 buổi',
      type: 'SESSION',
      durationDays: 60,
      sessionCount: 10,
      price: 650000,
      description: 'Mô tả ban đầu',
      status: 'ACTIVE',
      branchId: 1,
    }
    render(<PackageFormModal pkg={pkg} api={api} onClose={vi.fn()} onSaved={onSaved} />)

    expect(screen.getByLabelText('Mã gói')).toBeDisabled()
    expect(screen.getByLabelText('Loại gói')).toBeDisabled()

    await fill(user, 'Tên gói', 'Gói 10 buổi ưu đãi')
    await fill(user, 'Giá bán (VND)', '550000')
    await fill(user, 'Số buổi', '12')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    const sent = update.mock.calls[0][1]
    expect(update).toHaveBeenCalledWith(3, {
      name: 'Gói 10 buổi ưu đãi',
      durationDays: 60,
      sessionCount: 12,
      price: 550000,
      description: 'Mô tả ban đầu',
    })
    expect('code' in sent).toBe(false)
    expect('type' in sent).toBe(false)
  })

  it('TC-F3.3.1-03b: Xóa trống mô tả khi sửa gửi description là null', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    const update = vi.spyOn(api, 'update')
    const onSaved = vi.fn()
    const pkg: GymPackage = {
      id: 1,
      code: 'TIME-1M',
      name: 'Gói 1 tháng',
      type: 'TIME',
      durationDays: 30,
      sessionCount: null,
      price: 500000,
      description: 'Mô tả cũ cần xóa',
      status: 'ACTIVE',
      branchId: 1,
    }
    render(<PackageFormModal pkg={pkg} api={api} onClose={vi.fn()} onSaved={onSaved} />)

    await fill(user, 'Mô tả (tùy chọn)', '')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    const sent = update.mock.calls[0][1]
    expect(sent).toEqual({
      name: 'Gói 1 tháng',
      durationDays: 30,
      price: 500000,
      description: null,
    })
    expect('sessionCount' in sent).toBe(false)
  })

  it('TC-F3.3.1-05d, TC-F3.3.1-07b: Lỗi 400 theo trường hoặc 404 khi sửa giữ form và hiện thông báo', async () => {
    const user = userEvent.setup()
    const api = createMockPackagesApi()
    vi.spyOn(api, 'update')
      .mockRejectedValueOnce(
        axiosProblem(400, {
          detail: 'Vui lòng kiểm tra lại các trường dữ liệu.',
          errors: { price: 'Giá từ server không hợp lệ.' },
        }),
      )
      .mockRejectedValueOnce(axiosProblem(404, { detail: 'Gói tập không tồn tại trên hệ thống.' }))
    const onSaved = vi.fn()
    const pkg: GymPackage = {
      id: 1,
      code: 'TIME-1M',
      name: 'Gói 1 tháng',
      type: 'TIME',
      durationDays: 30,
      sessionCount: null,
      price: 500000,
      description: null,
      status: 'ACTIVE',
      branchId: 1,
    }
    render(<PackageFormModal pkg={pkg} api={api} onClose={vi.fn()} onSaved={onSaved} />)

    await fill(user, 'Tên gói', 'Gói giữ nguyên form')
    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))

    expect(await screen.findByText('Giá từ server không hợp lệ.')).toBeInTheDocument()
    expect(screen.getByLabelText('Tên gói')).toHaveValue('Gói giữ nguyên form')

    await user.click(screen.getByRole('button', { name: 'Lưu gói' }))
    expect(await screen.findByText('Gói tập không tồn tại trên hệ thống.')).toBeInTheDocument()
    expect(screen.getByLabelText('Tên gói')).toHaveValue('Gói giữ nguyên form')
    expect(onSaved).not.toHaveBeenCalled()
  })
})
