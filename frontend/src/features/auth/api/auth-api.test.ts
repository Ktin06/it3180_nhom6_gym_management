import { beforeEach, describe, expect, it, vi } from 'vitest'

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))

vi.mock('../../../services/http', () => ({ http: { get, post } }))

import { getCurrentUser, getProblemDetail, login, register } from './auth-api'
import { ROLES } from '../../../auth/roles'
import type { RegisterRequest } from '../types/auth'

describe('auth-api', () => {
  beforeEach(() => {
    get.mockReset()
    post.mockReset()
  })

  it('gửi các trường login đúng endpoint và trả access token', async () => {
    post.mockResolvedValue({ data: { accessToken: 'jwt-memory' } })

    await expect(login({ email: 'hv@example.com', password: 'Secret123' })).resolves.toEqual({
      accessToken: 'jwt-memory',
    })
    expect(post).toHaveBeenCalledWith(
      '/auth/login',
      { email: 'hv@example.com', password: 'Secret123' },
      { skipBearer: true, skipAuthRefresh: true },
    )
  })

  it('gửi request đăng ký với DTO camelCase và không thêm trường giới tính', async () => {
    post.mockResolvedValue({ data: { accessToken: 'jwt-memory' } })
    const request: RegisterRequest = {
      fullName: 'Đặng Thị Ánh',
      email: 'anh@example.com',
      phone: '0912345678',
      password: 'Secret123',
    }

    await expect(register(request)).resolves.toEqual({ accessToken: 'jwt-memory' })

    expect(post).toHaveBeenCalledWith('/auth/register', request, {
      skipBearer: true,
      skipAuthRefresh: true,
    })
  })

  it.each(ROLES)('đọc DTO /me phẳng và ánh xạ vai trò %s', async (role) => {
    get.mockResolvedValueOnce({ data: { userId: 15, email: 'anh@example.com', role } })

    await expect(getCurrentUser()).resolves.toEqual({ email: 'anh@example.com', role })
    expect(get).toHaveBeenCalledWith('/auth/me')
  })

  it('từ chối role lạ trong phản hồi /me', async () => {
    get.mockResolvedValueOnce({ data: { userId: 15, email: 'anh@example.com', role: 'UNKNOWN' } })

    await expect(getCurrentUser()).rejects.toThrow('Thông tin tài khoản từ máy chủ không hợp lệ.')
  })

  it.each([undefined, null, '15', 0, 1.5])('từ chối userId không hợp lệ: %s', async (userId) => {
    get.mockResolvedValueOnce({ data: { userId, email: 'anh@example.com', role: 'MEMBER' } })

    await expect(getCurrentUser()).rejects.toThrow('Thông tin tài khoản từ máy chủ không hợp lệ.')
  })

  it('tách detail và lỗi theo trường từ ProblemDetail 409', () => {
    expect(
      getProblemDetail({
        response: {
          status: 409,
          data: {
            detail: 'Số điện thoại này đã có hồ sơ. Vui lòng liên hệ quầy lễ tân.',
            errors: { phone: 'Số điện thoại đã tồn tại' },
          },
        },
      }),
    ).toEqual({
      detail: 'Số điện thoại này đã có hồ sơ. Vui lòng liên hệ quầy lễ tân.',
      errors: { phone: 'Số điện thoại đã tồn tại' },
      status: 409,
    })
  })
})
