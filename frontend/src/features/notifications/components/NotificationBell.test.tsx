import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getSessionEpoch } from '../../../auth/session-store'
import { subscribeToNotifications } from '../api/notification-stream'
import { notificationsApi, type MemberNotification } from '../api/notifications'
import { NotificationBell } from './NotificationBell'

vi.mock('../api/notification-stream')
vi.mock('../api/notifications')

const unread: MemberNotification = {
  id: 7,
  type: 'CLASS_RESCHEDULED',
  title: 'Lớp Yoga đổi lịch',
  content: 'Giờ cũ 18:00, giờ mới 19:00.',
  relatedEntity: 'class_sessions',
  relatedId: 12,
  read: false,
  readAt: null,
  createdAt: '2026-11-27T04:00:00Z',
}

function renderBell() {
  return render(
    <MemoryRouter>
      <NotificationBell authEpoch={getSessionEpoch()} />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(subscribeToNotifications).mockReturnValue(() => {})
  vi.mocked(notificationsApi.unreadCount).mockResolvedValue(1)
  vi.mocked(notificationsApi.list).mockResolvedValue({ content: [unread], totalElements: 1 })
})

describe('chuông thông báo hội viên', () => {
  it('TC-F10.5.1-03f, TC-F10.5.1-04f: mở nội dung, chuyển trạng thái và cập nhật số chưa đọc', async () => {
    const user = userEvent.setup()
    const read = { ...unread, read: true, readAt: unread.createdAt }
    vi.mocked(notificationsApi.markRead).mockImplementation(async () => {
      vi.mocked(notificationsApi.unreadCount).mockResolvedValue(0)
      vi.mocked(notificationsApi.list).mockResolvedValue({ content: [read], totalElements: 1 })
      return read
    })
    renderBell()

    await user.click(await screen.findByRole('button', { name: 'Thông báo, 1 chưa đọc' }))
    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByText('Chưa đọc')).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: unread.title }))
    expect(within(dialog).getByText(unread.content!)).toBeInTheDocument()
    expect(within(dialog).getByRole('link', { name: 'Buổi lớp #12' })).toHaveAttribute(
      'href',
      '/member/classes',
    )
    expect(await within(dialog).findByText('Đã đọc')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Thông báo', hidden: true })).toBeInTheDocument(),
    )
    expect(notificationsApi.markRead).toHaveBeenCalledExactlyOnceWith(7, getSessionEpoch())
  })

  it('TC-F10.5.1-02f: dùng trang bắt đầu từ 0 và phân trang 20 bản ghi', async () => {
    const user = userEvent.setup()
    vi.mocked(notificationsApi.list).mockResolvedValue({ content: [unread], totalElements: 21 })
    renderBell()
    await user.click(await screen.findByRole('button', { name: 'Thông báo, 1 chưa đọc' }))
    const dialog = await screen.findByRole('dialog')
    await within(dialog).findByText(unread.title)
    await user.click(within(dialog).getByTitle('Next Page'))
    await waitFor(() =>
      expect(notificationsApi.list).toHaveBeenLastCalledWith(
        1,
        20,
        getSessionEpoch(),
        expect.any(AbortSignal),
      ),
    )
  })

  it('TC-F10.5.1-06f: cho tải lại sau lỗi và chỉ hiện danh sách rỗng khi tải thành công', async () => {
    const user = userEvent.setup()
    vi.mocked(notificationsApi.list)
      .mockRejectedValueOnce(new Error('Máy chủ chưa sẵn sàng'))
      .mockResolvedValue({ content: [], totalElements: 0 })
    renderBell()
    await user.click(await screen.findByRole('button', { name: 'Thông báo, 1 chưa đọc' }))
    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByText('Chưa tải được thông báo.')).toBeInTheDocument()
    expect(within(dialog).queryByText('Bạn chưa có thông báo')).not.toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Thử lại' }))
    expect(await within(dialog).findByText('Bạn chưa có thông báo')).toBeInTheDocument()
  })

  it('TC-F10.5.1-04g: giữ trạng thái chưa đọc khi máy chủ từ chối và cho thử lại', async () => {
    const user = userEvent.setup()
    vi.mocked(notificationsApi.markRead).mockRejectedValue(new Error('Máy chủ chưa sẵn sàng'))
    renderBell()
    await user.click(await screen.findByRole('button', { name: 'Thông báo, 1 chưa đọc' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(await within(dialog).findByRole('button', { name: unread.title }))
    expect(
      await within(dialog).findByText('Chưa đánh dấu được thông báo đã đọc.'),
    ).toBeInTheDocument()
    expect(within(dialog).getByText('Chưa đọc')).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Thử lại' }))
    expect(notificationsApi.markRead).toHaveBeenCalledTimes(2)
  })

  it('TC-F10.4.1-06f: cập nhật chuông và danh sách đang mở khi nhận thông báo mới', async () => {
    const user = userEvent.setup()
    const unsubscribe = vi.fn()
    vi.mocked(subscribeToNotifications).mockReturnValue(unsubscribe)
    const view = renderBell()
    await user.click(await screen.findByRole('button', { name: 'Thông báo, 1 chưa đọc' }))
    const dialog = await screen.findByRole('dialog')
    await within(dialog).findByText(unread.title)
    const latest = { ...unread, id: 8, title: 'Lớp Pilates đổi lịch' }
    vi.mocked(notificationsApi.unreadCount).mockResolvedValue(2)
    vi.mocked(notificationsApi.list).mockResolvedValue({
      content: [latest, unread],
      totalElements: 2,
    })
    act(() => vi.mocked(subscribeToNotifications).mock.calls[0][1]())
    expect(await within(dialog).findByText(latest.title)).toBeInTheDocument()
    expect(
      await screen.findByRole('button', { name: 'Thông báo, 2 chưa đọc', hidden: true }),
    ).toBeInTheDocument()
    view.unmount()
    expect(unsubscribe).toHaveBeenCalledOnce()
  })
})
