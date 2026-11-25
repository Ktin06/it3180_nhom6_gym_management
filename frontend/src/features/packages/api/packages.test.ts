import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { livePackagesApi, packagesApi } from './packages'

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllEnvs()
})

describe('kết nối danh mục gói F3', () => {
  it('TC-F3.1.1-02f: gửi bộ lọc và phân trang đúng endpoint đã đặc tả', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data: { content: [] } })
    await livePackagesApi.list({ page: 0, size: 20, type: 'SESSION', status: 'INACTIVE' })
    expect(get).toHaveBeenCalledWith('/packages', {
      params: { page: 0, size: 20, type: 'SESSION', status: 'INACTIVE' },
    })
  })

  it('không hiển thị giá giả khi API thật lỗi', async () => {
    vi.stubEnv('VITE_F2_F3_MOCK', 'false')
    vi.spyOn(http, 'get').mockRejectedValue(new Error('API gói chưa sẵn sàng'))
    await expect(packagesApi.list({ page: 0, size: 20 })).rejects.toThrow('API gói chưa sẵn sàng')
  })

  it('S5: livePackagesApi gửi đúng POST /packages, PUT /packages/{id}, PUT /packages/{id}/deactivate', async () => {
    const createPayload = {
      code: 'TIME-1M',
      name: 'Gói 1 tháng',
      type: 'TIME' as const,
      durationDays: 30,
      price: 500000,
      description: null,
    }
    const updatePayload = {
      name: 'Gói 1 tháng mới',
      durationDays: 30,
      price: 450000,
      description: null,
    }
    const post = vi.spyOn(http, 'post').mockResolvedValue({ data: { id: 1, ...createPayload } })
    const put = vi.spyOn(http, 'put').mockResolvedValue({ data: { id: 1, status: 'INACTIVE' } })

    await livePackagesApi.create(createPayload)
    expect(post).toHaveBeenCalledWith('/packages', createPayload)

    await livePackagesApi.update(12, updatePayload)
    expect(put).toHaveBeenNthCalledWith(1, '/packages/12', updatePayload)

    await livePackagesApi.deactivate(12)
    expect(put).toHaveBeenNthCalledWith(2, '/packages/12/deactivate')
  })
})
