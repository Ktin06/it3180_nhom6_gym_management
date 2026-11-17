import { http } from '../../../services/http'
import type { MembersGateway } from '../types/member'

export const liveMembersApi: MembersGateway = {
  list: async (params) => (await http.get('/members', { params })).data,
  detail: async (id) => (await http.get(`/members/${id}`)).data,
  create: async (input) => (await http.post('/members', input)).data,
  update: async (id, input) => (await http.put(`/members/${id}`, input)).data,
}

async function gateway() {
  if (import.meta.env.DEV && import.meta.env.VITE_F2_F3_MOCK === 'true') {
    return (await import('./mock-members')).mockMembersApi
  }
  return liveMembersApi
}

export const membersApi: MembersGateway = {
  list: async (query) => (await gateway()).list(query),
  detail: async (id) => (await gateway()).detail(id),
  create: async (input) => (await gateway()).create(input),
  update: async (id, input) => (await gateway()).update(id, input),
}
