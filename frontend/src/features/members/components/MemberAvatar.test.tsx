import { act, render, screen, waitFor } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { MemberAvatar } from './MemberAvatar'

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

function makeAxiosError(status: number, detail = 'Không tìm thấy ảnh.') {
  const error = new AxiosError('Request failed')
  error.response = {
    status,
    data: { detail },
    statusText: String(status),
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllEnvs()
})

describe('MemberAvatar', () => {
  it('US-F2.4.2 KB1: tải Blob qua services/http.ts, tạo ObjectURL và gọi URL.revokeObjectURL khi đổi memberId hoặc unmount', async () => {
    vi.stubEnv('VITE_F2_F3_MOCK', 'false')
    const blob1 = new Blob(['img-1'], { type: 'image/jpeg' })
    const blob2 = new Blob(['img-2'], { type: 'image/jpeg' })
    const get = vi
      .spyOn(http, 'get')
      .mockResolvedValueOnce({ data: blob1 })
      .mockResolvedValueOnce({ data: blob2 })
    const createObjectURL = vi
      .spyOn(URL, 'createObjectURL')
      .mockReturnValueOnce('blob:avatar-1')
      .mockReturnValueOnce('blob:avatar-2')
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    const { rerender, unmount } = render(<MemberAvatar memberId={1} fullName="Nguyễn Minh An" />)

    const img1 = await screen.findByRole('img', { name: 'Ảnh đại diện Nguyễn Minh An' })
    expect(img1).toHaveAttribute('src', 'blob:avatar-1')
    expect(get).toHaveBeenCalledWith('/members/1/avatar', { responseType: 'blob' })
    expect(createObjectURL).toHaveBeenCalledWith(blob1)

    rerender(<MemberAvatar memberId={2} fullName="Trần Hoài Bình" />)

    await waitFor(() => expect(revokeObjectURL).toHaveBeenCalledWith('blob:avatar-1'))
    const img2 = await screen.findByRole('img', { name: 'Ảnh đại diện Trần Hoài Bình' })
    expect(img2).toHaveAttribute('src', 'blob:avatar-2')
    expect(get).toHaveBeenLastCalledWith('/members/2/avatar', { responseType: 'blob' })
    expect(createObjectURL).toHaveBeenCalledWith(blob2)

    unmount()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:avatar-2')
  })

  it('US-F2.4.2 KB1: bỏ qua phản hồi đến muộn khi memberId đã đổi', async () => {
    vi.stubEnv('VITE_F2_F3_MOCK', 'false')
    const first = createDeferred<{ data: Blob }>()
    const second = createDeferred<{ data: Blob }>()
    vi.spyOn(http, 'get').mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:avatar-2')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    const { rerender } = render(<MemberAvatar memberId={1} fullName="Nguyễn Minh An" />)
    rerender(<MemberAvatar memberId={2} fullName="Trần Hoài Bình" />)

    await act(async () => {
      second.resolve({ data: new Blob(['img-2'], { type: 'image/jpeg' }) })
    })
    const img2 = await screen.findByRole('img', { name: 'Ảnh đại diện Trần Hoài Bình' })
    expect(img2).toHaveAttribute('src', 'blob:avatar-2')

    await act(async () => {
      first.resolve({ data: new Blob(['img-1-late'], { type: 'image/jpeg' }) })
    })

    expect(screen.getByRole('img', { name: 'Ảnh đại diện Trần Hoài Bình' })).toHaveAttribute(
      'src',
      'blob:avatar-2',
    )
    expect(createObjectURL).toHaveBeenCalledTimes(1)
  })

  it('TC-F2.4.2-08a: hiện chữ cái đầu của tên khi hasAvatar là false (không gọi API), API trả 404 hoặc lỗi mạng', async () => {
    vi.stubEnv('VITE_F2_F3_MOCK', 'false')
    const get = vi
      .spyOn(http, 'get')
      .mockRejectedValueOnce(makeAxiosError(404))
      .mockRejectedValueOnce(new Error('Network Error'))

    const { rerender } = render(
      <MemberAvatar memberId={1} fullName="Nguyễn Minh An" hasAvatar={false} />,
    )
    expect(screen.getByText('A')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(get).not.toHaveBeenCalled()

    rerender(<MemberAvatar memberId={2} fullName="Trần Hoài Bình" />)
    await waitFor(() =>
      expect(get).toHaveBeenCalledWith('/members/2/avatar', { responseType: 'blob' }),
    )
    expect(screen.getByText('B')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()

    rerender(<MemberAvatar memberId={3} fullName="Lê Phương Chi" />)
    await waitFor(() =>
      expect(get).toHaveBeenCalledWith('/members/3/avatar', { responseType: 'blob' }),
    )
    expect(screen.getByText('C')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })
})
