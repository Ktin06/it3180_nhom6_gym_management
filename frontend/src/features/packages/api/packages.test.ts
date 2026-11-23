import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { livePackagesApi, packagesApi } from './packages'
import type { CreatePackageInput } from '../types/package'

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllEnvs()
})

describe('kết nối danh mục gói F3', () => {
  it('TC-F3.2.1-01: tạo gói gọi POST /packages', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue({ data: { id: 1 } })
    const input: CreatePackageInput = { code: 'TIME-1M', name: 'Gói tháng', type: 'TIME', durationDays: 30, sessionCount: null, price: 500000, description: null }
    await livePackagesApi.create(input)
    expect(post).toHaveBeenCalledWith('/packages', input)
  })

  it('TC-F3.3.1-01: cập nhật gói gọi PUT /packages/{id}', async () => {
    const put = vi.spyOn(http, 'put').mockResolvedValue({ data: { id: 7 } })
    await livePackagesApi.update(7, { name: 'Tên mới', price: 900000 })
    expect(put).toHaveBeenCalledWith('/packages/7', { name: 'Tên mới', price: 900000 })
  })

  it('TC-F3.4.1-01: ngừng bán gọi PUT endpoint không có body', async () => {
    const put = vi.spyOn(http, 'put').mockResolvedValue({ data: {} })
    await livePackagesApi.deactivate(7)
    expect(put).toHaveBeenCalledWith('/packages/7/deactivate')
  })

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
})
