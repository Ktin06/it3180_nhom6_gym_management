import { AxiosError, AxiosHeaders } from 'axios'
import type { MemberDetail, MemberInput, MembersGateway } from '../types/member'
import { normalizePhone } from '../../../services/phone'
import { normalizePhoneSearch } from '../phone'

/** Dữ liệu hư cấu, trong bộ nhớ; không dùng làm xác thực hoặc thay backend. */
export function createMockMembersApi(): MembersGateway {
  let sequence = 30
  let userSequence = 100
  const avatars = new Map<number, Blob>()

  function mockHttpError(status: number, data: Record<string, unknown>) {
    const error = new AxiosError(String(status))
    error.response = {
      status,
      data,
      statusText: String(status),
      headers: {},
      config: { headers: new AxiosHeaders() },
    }
    return error
  }
  const names = ['Nguyễn Minh An', 'Trần Hoài Bình', 'Lê Phương Chi', 'Phạm Gia Duy', 'Vũ Ngọc Hà']
  const members: MemberDetail[] = Array.from({ length: 30 }, (_, index) => ({
    id: index + 1,
    memberCode: `HV${String(index + 1).padStart(6, '0')}`,
    fullName: `${names[index % names.length]}${index < 5 ? '' : ` ${index + 1}`}`,
    phone: `090000${String(index + 1).padStart(4, '0')}`,
    email: null,
    status: index % 7 === 0 ? 'INACTIVE' : 'ACTIVE',
    homeBranchId: 1,
    hasAvatar: false,
    userId: null,
    gender: null,
    dob: null,
    createdAt: '2026-11-01T08:00:00Z',
    updatedAt: '2026-11-01T08:00:00Z',
  }))

  function find(id: number) {
    const member = members.find((item) => item.id === id)
    if (!member) throw new Error('Không tìm thấy hội viên.')
    return member
  }

  function validate(input: MemberInput, id?: number) {
    if (!input.fullName.trim()) throw new Error('Họ tên không được để trống.')
    const phone = normalizePhone(input.phone)
    if (!/^0\d{9}$/.test(phone)) throw new Error('Số điện thoại phải có 10 chữ số, bắt đầu bằng 0.')
    if (members.some((item) => item.phone === phone && item.id !== id)) {
      throw new Error('Số điện thoại này đã được đăng ký cho hội viên khác.')
    }
    return phone
  }

  return {
    async list(query) {
      const keyword = query.keyword?.trim() ?? ''
      const isPhone = /^[+0-9\s.-]+$/.test(keyword) && /[0-9]/.test(keyword)
      const phoneKeyword = normalizePhoneSearch(keyword)
      const filtered = members
        .filter((item) => !query.status || item.status === query.status)
        .filter(
          (item) =>
            !keyword ||
            item.memberCode.toLowerCase() === keyword.toLowerCase() ||
            (isPhone && item.phone.startsWith(phoneKeyword)) ||
            item.fullName.toLocaleLowerCase('vi').startsWith(keyword.toLocaleLowerCase('vi')),
        )
        .sort((a, b) => b.id - a.id)
      return structuredClone({
        content: filtered.slice(query.page * query.size, (query.page + 1) * query.size),
        page: query.page,
        size: query.size,
        totalElements: filtered.length,
        totalPages: Math.ceil(filtered.length / query.size),
      })
    },
    async detail(id) {
      return structuredClone(find(id))
    },
    async create(input) {
      const phone = validate(input)
      const id = ++sequence
      const now = new Date().toISOString()
      const member: MemberDetail = {
        ...input,
        fullName: input.fullName.trim(),
        phone,
        id,
        memberCode: `HV${String(id).padStart(6, '0')}`,
        status: 'ACTIVE',
        email: null,
        homeBranchId: 1,
        hasAvatar: false,
        userId: null,
        createdAt: now,
        updatedAt: now,
      }
      members.push(member)
      return structuredClone(member)
    },
    async update(id, input) {
      const member = find(id)
      const phone = validate(input, id)
      Object.assign(member, input, {
        fullName: input.fullName.trim(),
        phone,
        updatedAt: new Date().toISOString(),
      })
      return structuredClone(member)
    },
    async getAvatar(id) {
      find(id)
      const blob = avatars.get(id)
      if (!blob) {
        throw mockHttpError(404, { detail: 'Hội viên chưa có ảnh đại diện.' })
      }
      return blob
    },
    async uploadAvatar(id, file) {
      const member = find(id)
      avatars.set(id, new Blob([file], { type: 'image/jpeg' }))
      member.hasAvatar = true
    },
    async createAccount(id, input) {
      const member = members.find((item) => item.id === id)
      if (!member) {
        throw mockHttpError(404, { detail: 'Không tìm thấy hội viên.' })
      }
      if (member.status !== 'ACTIVE') {
        throw mockHttpError(422, {
          detail: 'Chỉ có thể tạo tài khoản cho hồ sơ hội viên đang hoạt động.',
        })
      }
      if (member.userId !== null) {
        throw mockHttpError(409, { detail: 'Hội viên này đã có tài khoản đăng nhập.' })
      }
      const normalizedEmail = input.email.trim().toLowerCase()
      if (!normalizedEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
        throw mockHttpError(400, {
          detail: 'Vui lòng kiểm tra lại các trường dữ liệu.',
          errors: { email: 'Email không đúng định dạng' },
        })
      }
      if (members.some((item) => item.email === normalizedEmail)) {
        throw mockHttpError(409, {
          detail: 'Email này đã được sử dụng bởi một tài khoản khác.',
        })
      }
      const userId = ++userSequence
      member.userId = userId
      member.email = normalizedEmail
      member.updatedAt = new Date().toISOString()
      return { memberId: member.id, userId, email: normalizedEmail }
    },
  }
}

export const mockMembersApi = createMockMembersApi()
