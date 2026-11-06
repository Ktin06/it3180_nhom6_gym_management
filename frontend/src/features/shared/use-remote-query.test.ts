import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useRemoteQuery } from './use-remote-query'

describe('tải dữ liệu khi đổi điều kiện tìm kiếm', () => {
  it('bỏ kết quả cũ nếu phản hồi đến sau yêu cầu mới', async () => {
    let completeOld!: (value: string) => void
    const load = vi.fn((query: string) =>
      query === 'cũ'
        ? new Promise<string>((resolve) => {
            completeOld = resolve
          })
        : Promise.resolve('kết quả mới'),
    )
    const { result } = renderHook(() => useRemoteQuery<string, string>(load, 'cũ'))
    act(() => result.current.changeQuery('mới'))
    await waitFor(() => expect(result.current.data).toBe('kết quả mới'))
    await act(async () => completeOld('kết quả cũ'))
    expect(result.current.data).toBe('kết quả mới')
  })

  it('ẩn dữ liệu cũ lúc tải lại và phục hồi được sau lỗi kết nối', async () => {
    const load = vi
      .fn<() => Promise<string>>()
      .mockRejectedValueOnce(new Error('Mất kết nối'))
      .mockResolvedValueOnce('Đã tải lại')
    const { result } = renderHook(() => useRemoteQuery(load, {}))
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBeInstanceOf(Error)
    act(() => result.current.reload())
    expect(result.current.loading).toBe(true)
    expect(result.current.error).toBeUndefined()
    await waitFor(() => expect(result.current.data).toBe('Đã tải lại'))
  })
})
