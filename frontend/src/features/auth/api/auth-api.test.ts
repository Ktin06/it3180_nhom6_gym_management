import { beforeEach, describe, expect, it, vi } from 'vitest'

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))

vi.mock('../../../services/http', () => ({ http: { get, post } }))

import { getCurrentUser, getProblemDetail, login, register } from './auth-api'

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
    const request = {
      fullName: 'Đặng Thị Ánh',
      email: 'anh@example.com',
      phone: '0912345678',
      password: 'Secret123',
    }

    await register(request)

    expect(post).toHaveBeenCalledWith('/auth/register', request, {
      skipBearer: true,
      skipAuthRefresh: true,
    })
  })

  it('đọc AuthUser tạm từ /me và từ chối role lạ', async () => {
    get.mockResolvedValueOnce({ data: { email: 'anh@example.com', role: 'MEMBER' } })
    await expect(getCurrentUser()).resolves.toEqual({ email: 'anh@example.com', role: 'MEMBER' })

    get.mockResolvedValueOnce({ data: { email: 'anh@example.com', role: 'UNKNOWN' } })
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
