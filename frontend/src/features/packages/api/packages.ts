import { http } from '../../../services/http'
import type { CreatePackageInput, PackagesGateway, UpdatePackageInput } from '../types/package'

export const livePackagesApi: PackagesGateway = {
  list: async (params) => (await http.get('/packages', { params })).data,
  create: async (input: CreatePackageInput) => (await http.post('/packages', input)).data,
  update: async (id: number, input: UpdatePackageInput) => (await http.put(`/packages/${id}`, input)).data,
  deactivate: async (id: number) => { await http.put(`/packages/${id}/deactivate`) },
}

export const packagesApi: PackagesGateway = {
  async list(query) {
    if (import.meta.env.DEV && import.meta.env.VITE_F2_F3_MOCK === 'true') {
      return (await import('./mock-packages')).mockPackagesApi.list(query)
    }
    return livePackagesApi.list(query)
  },
  create: (input) => livePackagesApi.create(input),
  update: (id, input) => livePackagesApi.update(id, input),
  deactivate: (id) => livePackagesApi.deactivate(id),
}
