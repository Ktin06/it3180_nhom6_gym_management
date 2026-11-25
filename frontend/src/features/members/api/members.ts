import { http } from '../../../services/http'
import type { MembersGateway } from '../types/member'

export const liveMembersApi: MembersGateway = {
  list: async (params) => (await http.get('/members', { params })).data,
  detail: async (id) => (await http.get(`/members/${id}`)).data,
  create: async (input) => (await http.post('/members', input)).data,
  update: async (id, input) => (await http.put(`/members/${id}`, input)).data,
  getAvatar: async (id) =>
    (await http.get<Blob>(`/members/${id}/avatar`, { responseType: 'blob' })).data,
  uploadAvatar: async (id, file) => {
    const formData = new FormData()
    formData.append('file', file)
    await http.put(`/members/${id}/avatar`, formData, {
      headers: { 'Content-Type': undefined },
    })
  },
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
  getAvatar: async (id) => (await gateway()).getAvatar(id),
  uploadAvatar: async (id, file) => (await gateway()).uploadAvatar(id, file),
}
