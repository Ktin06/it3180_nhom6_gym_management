import { useCallback, useEffect, useState } from 'react'
import { getSessionEpoch } from '../../auth/session-store'
import { subscribeToNotifications } from './api/notification-stream'
import { notificationsApi, type MemberNotification } from './api/notifications'

export const notificationPageSize = 20

export function useNotificationInbox(authEpoch: number, open: boolean, page: number) {
  const [revision, setRevision] = useState(0)
  const [unreadCount, setUnreadCount] = useState<number>()
  const [result, setResult] = useState<{
    page: number
    items: MemberNotification[]
    total: number
  }>()
  const [failedPage, setFailedPage] = useState<number>()
  const [pendingId, setPendingId] = useState<number>()
  const [readErrorId, setReadErrorId] = useState<number>()

  const refresh = useCallback(() => setRevision((current) => current + 1), [])

  useEffect(() => {
    const unsubscribe = subscribeToNotifications(authEpoch, refresh)
    function refreshOnReturn() {
      if (document.visibilityState === 'visible') refresh()
    }
    document.addEventListener('visibilitychange', refreshOnReturn)
    return () => {
      unsubscribe()
      document.removeEventListener('visibilitychange', refreshOnReturn)
    }
  }, [authEpoch, refresh])

  useEffect(() => {
    const controller = new AbortController()
    void notificationsApi.unreadCount(authEpoch, controller.signal).then(
      (count) => {
        if (!controller.signal.aborted) setUnreadCount(count)
      },
      () => {
        if (!controller.signal.aborted) setUnreadCount(undefined)
      },
    )
    return () => controller.abort()
  }, [authEpoch, open, revision])

  useEffect(() => {
    if (!open) return
    const controller = new AbortController()
    void notificationsApi.list(page, notificationPageSize, authEpoch, controller.signal).then(
      (data) => {
        if (controller.signal.aborted) return
        setResult({ page, items: data.content, total: data.totalElements })
        setFailedPage(undefined)
      },
      () => {
        if (!controller.signal.aborted) setFailedPage(page)
      },
    )
    return () => controller.abort()
  }, [authEpoch, open, page, revision])

  async function markRead(id: number) {
    if (pendingId !== undefined) return
    setPendingId(id)
    setReadErrorId(undefined)
    try {
      const updated = await notificationsApi.markRead(id, authEpoch)
      if (getSessionEpoch() !== authEpoch) return
      setResult(
        (current) =>
          current && {
            ...current,
            items: current.items.map((item) => (item.id === updated.id ? updated : item)),
          },
      )
      refresh()
    } catch {
      if (getSessionEpoch() === authEpoch) setReadErrorId(id)
    } finally {
      if (getSessionEpoch() === authEpoch) setPendingId(undefined)
    }
  }

  const currentPage = result?.page === page ? result : undefined
  return {
    unreadCount,
    items: currentPage?.items,
    total: result?.total ?? 0,
    loadError: failedPage === page,
    pendingId,
    readErrorId,
    refresh,
    markRead,
  }
}
