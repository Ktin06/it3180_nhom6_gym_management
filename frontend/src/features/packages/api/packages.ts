import { http } from '../../../services/http'
import type { PackagesGateway } from '../types/package'

export const livePackagesApi: PackagesGateway = {
  list: async (params) => (await http.get('/packages', { params })).data,
  create: async (input) => (await http.post('/packages', input)).data,
  update: async (id, input) => (await http.put(`/packages/${id}`, input)).data,
  deactivate: async (id) => (await http.put(`/packages/${id}/deactivate`)).data,
}

async function gateway() {
  if (import.meta.env.DEV && import.meta.env.VITE_F2_F3_MOCK === 'true') {
    return (await import('./mock-packages')).mockPackagesApi
  }
  return livePackagesApi
}

export const packagesApi: PackagesGateway = {
  list: async (query) => (await gateway()).list(query),
  create: async (input) => (await gateway()).create(input),
  update: async (id, input) => (await gateway()).update(id, input),
  deactivate: async (id) => (await gateway()).deactivate(id),
}
