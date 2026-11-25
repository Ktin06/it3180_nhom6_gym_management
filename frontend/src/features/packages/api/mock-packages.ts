import type { GymPackageDetail, PackagesGateway } from '../types/package'

function initialPackages(): GymPackageDetail[] {
  const timestamp = '2026-11-01T08:00:00Z'
  return [
    {
      id: 1,
      code: 'TIME-1M',
      name: 'Gói 1 tháng',
      type: 'TIME',
      durationDays: 30,
      sessionCount: null,
      price: 500000,
      description: 'Tập luyện trong 30 ngày.',
      status: 'ACTIVE',
      branchId: 1,
      createdAt: timestamp,
      updatedAt: timestamp,
    },
    {
      id: 2,
      code: 'TIME-3M',
      name: 'Gói 3 tháng',
      type: 'TIME',
      durationDays: 90,
      sessionCount: null,
      price: 1350000,
      description: null,
      status: 'ACTIVE',
      branchId: 1,
      createdAt: timestamp,
      updatedAt: timestamp,
    },
    {
      id: 3,
      code: 'SESSION-10',
      name: 'Gói 10 buổi',
      type: 'SESSION',
      durationDays: 60,
      sessionCount: 10,
      price: 650000,
      description: null,
      status: 'ACTIVE',
      branchId: 1,
      createdAt: timestamp,
      updatedAt: timestamp,
    },
    {
      id: 4,
      code: 'PT-10',
      name: 'PT 10 buổi',
      type: 'PT',
      durationDays: 90,
      sessionCount: 10,
      price: 3000000,
      description: 'Tập 1–1 với huấn luyện viên; không dùng để check-in phòng tập.',
      status: 'ACTIVE',
      branchId: 1,
      createdAt: timestamp,
      updatedAt: timestamp,
    },
    {
      id: 5,
      code: 'TIME-6M',
      name: 'Gói 6 tháng',
      type: 'TIME',
      durationDays: 180,
      sessionCount: null,
      price: 2500000,
      description: null,
      status: 'ACTIVE',
      branchId: null,
      createdAt: timestamp,
      updatedAt: timestamp,
    },
    {
      id: 6,
      code: 'SESSION-20',
      name: 'Gói 20 buổi cũ',
      type: 'SESSION',
      durationDays: 90,
      sessionCount: 20,
      price: 1000000,
      description: null,
      status: 'INACTIVE',
      branchId: 1,
      createdAt: timestamp,
      updatedAt: timestamp,
    },
  ]
}

/** Dữ liệu hư cấu trong bộ nhớ để chạy thử giao diện; không thay thế kiểm tra của backend. */
export function createMockPackagesApi(): PackagesGateway {
  let sequence = 6
  const packages = initialPackages()

  function findOwned(id: number) {
    const item = packages.find((pkg) => pkg.id === id)
    if (!item) throw new Error('Gói tập không tồn tại trên hệ thống.')
    if (item.branchId === null) throw new Error('Bạn không có quyền thực hiện thao tác này.')
    return item
  }

  return {
    async list(query) {
      const filtered = packages
        .filter(
          (item) =>
            (!query.type || item.type === query.type) &&
            (!query.status || item.status === query.status),
        )
        .sort((a, b) => a.type.localeCompare(b.type) || a.name.localeCompare(b.name, 'vi'))
      return structuredClone({
        content: filtered.slice(query.page * query.size, (query.page + 1) * query.size),
        page: query.page,
        size: query.size,
        totalElements: filtered.length,
        totalPages: Math.ceil(filtered.length / query.size),
      })
    },
    async create(input) {
      const code = input.code.trim()
      if (packages.some((item) => item.code === code)) {
        throw new Error(`Mã gói '${code}' đã tồn tại trên hệ thống.`)
      }
      const now = new Date().toISOString()
      const created: GymPackageDetail = {
        id: ++sequence,
        code,
        name: input.name.trim(),
        type: input.type,
        durationDays: input.durationDays,
        sessionCount: input.type === 'TIME' ? null : (input.sessionCount ?? null),
        price: input.price,
        description: input.description ?? null,
        status: 'ACTIVE',
        branchId: 1,
        createdAt: now,
        updatedAt: now,
      }
      packages.push(created)
      return structuredClone(created)
    },
    async update(id, input) {
      const item = findOwned(id)
      item.name = input.name !== undefined ? input.name.trim() : item.name
      item.durationDays = input.durationDays ?? item.durationDays
      if (item.type !== 'TIME' && input.sessionCount !== undefined) {
        item.sessionCount = input.sessionCount
      }
      item.price = input.price ?? item.price
      if (input.description !== undefined) {
        item.description = input.description
      }
      item.updatedAt = new Date().toISOString()
      return structuredClone(item)
    },
    async deactivate(id) {
      const item = findOwned(id)
      if (item.status === 'INACTIVE') {
        throw new Error('Gói tập này đã ở trạng thái ngừng bán.')
      }
      item.status = 'INACTIVE'
      item.updatedAt = new Date().toISOString()
      return {
        id: item.id,
        code: item.code,
        status: item.status,
        updatedAt: item.updatedAt,
      }
    },
  }
}

export const mockPackagesApi = createMockPackagesApi()
