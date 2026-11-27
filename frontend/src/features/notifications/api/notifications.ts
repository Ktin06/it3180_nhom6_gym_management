import { http } from '../../../services/http'

export interface MemberNotification {
  id: number
  type: string
  title: string
  content: string | null
  relatedEntity: string | null
  relatedId: number | null
  read: boolean
  readAt: string | null
  createdAt: string
}

interface NotificationPage {
  content: MemberNotification[]
  totalElements: number
}

const path = '/me/notifications'

export const notificationsApi = {
  async list(page: number, size: number, authEpoch: number, signal: AbortSignal) {
    return (await http.get<NotificationPage>(path, { params: { page, size }, authEpoch, signal }))
      .data
  },

  async unreadCount(authEpoch: number, signal?: AbortSignal) {
    return (await http.get<{ unreadCount: number }>(`${path}/unread-count`, { authEpoch, signal }))
      .data.unreadCount
  },

  async markRead(id: number, authEpoch: number) {
    return (await http.patch<MemberNotification>(`${path}/${id}/read`, undefined, { authEpoch }))
      .data
  },
}
