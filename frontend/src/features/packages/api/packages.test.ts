import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { livePackagesApi, packagesApi } from './packages'

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllEnvs()
})

describe('kết nối danh mục gói F3', () => {
  it('gửi bộ lọc và phân trang đúng endpoint đã đặc tả', async () => {
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
