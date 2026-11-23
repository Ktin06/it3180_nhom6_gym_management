import type { CreatePackageInput, GymPackage, PackagesGateway, UpdatePackageInput } from '../types/package'

const packages: GymPackage[] = [
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
  },
]

export const mockPackagesApi: PackagesGateway = {
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
  async create(input: CreatePackageInput) {
    const item: GymPackage = { ...input, id: Math.max(...packages.map((x) => x.id)) + 1, status: 'ACTIVE', branchId: 1 }
    packages.push(item)
    return structuredClone(item)
  },
  async update(id: number, input: UpdatePackageInput) {
    const index = packages.findIndex((item) => item.id === id)
    if (index < 0) throw new Error('Không tìm thấy gói tập.')
    packages[index] = { ...packages[index], ...input }
    return structuredClone(packages[index])
  },
  async deactivate(id: number) {
    const item = packages.find((candidate) => candidate.id === id)
    if (!item) throw new Error('Không tìm thấy gói tập.')
    if (item.status !== 'ACTIVE') throw new Error('Gói tập đã ngừng bán.')
    item.status = 'INACTIVE'
  },
}
