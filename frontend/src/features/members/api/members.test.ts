import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { liveMembersApi, membersApi } from './members'
import { createMockMembersApi } from './mock-members'

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllEnvs()
})

describe('kết nối API F2', () => {
  it('gửi trang từ 0 cùng các điều kiện lọc qua client HTTP dùng chung', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data: { content: [] } })
    await liveMembersApi.list({ page: 1, size: 10, keyword: '+84 900 000 001', status: 'ACTIVE' })
    expect(get).toHaveBeenCalledWith('/members', {
      params: { page: 1, size: 10, keyword: '+84 900 000 001', status: 'ACTIVE' },
    })
  })

  it('giữ lỗi API thật, không tự thay bằng danh sách giả', async () => {
    vi.stubEnv('VITE_F2_F3_MOCK', 'false')
    vi.spyOn(http, 'get').mockRejectedValue(new Error('Backend chưa sẵn sàng'))
    await expect(membersApi.list({ page: 0, size: 20 })).rejects.toThrow('Backend chưa sẵn sàng')
  })
})

describe('dữ liệu minh họa hội viên', () => {
  it('tìm cùng một số điện thoại ở dạng +84 và từ chối tạo trùng mà không thêm dòng', async () => {
    const api = createMockMembersApi()
    const before = await api.list({ page: 0, size: 100 })
    const input = {
      fullName: 'Người thử nghiệm',
      phone: '+84 900 000 001',
      gender: null,
      dob: null,
    }
    const found = await api.list({ page: 0, size: 20, keyword: input.phone })
    expect(found.content[0].memberCode).toBe('HV000001')
    await expect(api.create(input)).rejects.toThrow('đã được đăng ký')
    expect((await api.list({ page: 0, size: 100 })).totalElements).toBe(before.totalElements)
  })

  it('sửa chính hồ sơ không báo trùng SĐT và giữ thông tin khi sửa trùng người khác thất bại', async () => {
    const api = createMockMembersApi()
    const input = {
      fullName: 'Tên đã sửa',
      phone: '0900000001',
      gender: null,
      dob: null,
      status: 'INACTIVE' as const,
    }
    expect((await api.update(1, input)).fullName).toBe('Tên đã sửa')
    await expect(api.update(1, { ...input, phone: '0900000002' })).rejects.toThrow(
      'đã được đăng ký',
    )
    expect((await api.detail(1)).phone).toBe('0900000001')
  })
})
