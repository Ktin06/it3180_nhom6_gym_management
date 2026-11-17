import { http } from '../../../services/http'
import type { PackagesGateway } from '../types/package'

export const livePackagesApi: PackagesGateway = {
  list: async (params) => (await http.get('/packages', { params })).data,
}

export const packagesApi: PackagesGateway = {
  async list(query) {
    if (import.meta.env.DEV && import.meta.env.VITE_F2_F3_MOCK === 'true') {
      return (await import('./mock-packages')).mockPackagesApi.list(query)
    }
    return livePackagesApi.list(query)
  },
}
