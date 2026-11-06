import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { mockPackagesApi } from '../api/mock-packages'
import { PackagesPage } from './PackagesPage'

describe('danh mục gói', () => {
  it('mặc định lấy gói đang bán; chọn loại PT giữ nguyên lọc trạng thái', async () => {
    const user = userEvent.setup()
    const api = { list: vi.fn(mockPackagesApi.list) }
    render(<PackagesPage api={api} />)
    expect(await screen.findByText('TIME-1M')).toBeInTheDocument()
    expect(screen.queryByText('SESSION-20')).not.toBeInTheDocument()
    await user.click(screen.getByLabelText('Loại gói'))
    await user.click(await screen.findByTitle('Huấn luyện cá nhân'))
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith({
        page: 0,
        size: 20,
        type: 'PT',
        status: 'ACTIVE',
      }),
    )
    expect(await screen.findByText('PT-10')).toBeInTheDocument()
    expect(screen.queryByText('TIME-1M')).not.toBeInTheDocument()
  })
})
