import { App } from 'antd'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { PageResponse } from '../../shared/types'
import type { InvoicesGateway, InvoiceSummary } from '../types/invoice'
import { InvoicesPage } from './InvoicesPage'
import { AuthContext } from '../../../auth/AuthContext'
import type { Role } from '../../../auth/roles'

const sampleRows: InvoiceSummary[] = [
  {
    id: 88,
    code: 'HD000088',
    branchId: 1,
    memberId: 123,
    memberCode: 'HV000123',
    memberName: 'Nguyễn Văn A',
    memberPhone: '0912000123',
    total: 3000000,
    source: 'COUNTER',
    status: 'PENDING',
    paidAt: null,
    createdAt: '2026-11-20T08:30:00Z',
  },
  {
    id: 89,
    code: 'HD000089',
    branchId: 1,
    memberId: null,
    memberCode: null,
    memberName: null,
    memberPhone: null,
    total: 50000,
    source: 'MEMBER_PORTAL',
    status: 'PAID',
    paidAt: '2026-11-20T09:00:00Z',
    createdAt: '2026-11-20T08:45:00Z',
  },
]

function makeGateway(
  rows: InvoiceSummary[] = sampleRows,
  totalElements = rows.length,
): Pick<InvoicesGateway, 'list' | 'branches'> {
  return {
    branches: vi.fn().mockResolvedValue([
      { id: 1, code: 'CN01', name: 'Chi nhánh 1' },
      { id: 2, code: 'CN02', name: 'Chi nhánh 2' },
    ]),
    list: vi.fn(async (query): Promise<PageResponse<InvoiceSummary>> => ({
      content: rows,
      page: query.page,
      size: query.size,
      totalElements,
      totalPages: Math.max(1, Math.ceil(totalElements / query.size)),
    })),
  }
}

function renderPage(api: Pick<InvoicesGateway, 'list' | 'branches'>, role: Role = 'RECEPTIONIST') {
  return render(
    <App>
      <AuthContext.Provider
        value={{
          status: 'authenticated',
          user: { email: 'staff@example.com', role },
          sessionError: null,
          login: vi.fn(),
          register: vi.fn(),
          logout: vi.fn(),
          retrySession: vi.fn(),
          completePasswordChange: vi.fn(),
        }}
      >
        <MemoryRouter>
          <InvoicesPage api={api} />
        </MemoryRouter>
      </AuthContext.Provider>
    </App>,
  )
}

describe('danh sách hóa đơn F5', () => {
  it('TC-F5.3.1-04d: Chủ chuỗi lọc chi nhánh bằng danh mục toàn chuỗi', async () => {
    const user = userEvent.setup()
    const api = makeGateway(sampleRows, 25)
    renderPage(api, 'SUPER_ADMIN')
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    await waitFor(() => expect(api.branches).toHaveBeenCalledOnce())
    await user.click(screen.getByTitle('2'))
    await user.click(screen.getByLabelText('Chi nhánh'))
    await user.click(await screen.findByTitle('CN02 – Chi nhánh 2'))
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith({ page: 0, size: 20, branchId: 2 }),
    )
    expect(screen.queryByRole('button', { name: 'Thu tiền' })).not.toBeInTheDocument()
  }, 10000)

  describe('TC-F5.3.1-01c: %s không tải danh mục hoặc chọn chi nhánh khác', () => {
    it.each(['RECEPTIONIST', 'BRANCH_MANAGER'] as const)('trường hợp %s', async (role) => {
      const api = makeGateway()
      renderPage(api, role)
      expect(await screen.findByText('HD000088')).toBeInTheDocument()
      expect(api.branches).not.toHaveBeenCalled()
      expect(screen.queryByLabelText('Chi nhánh')).not.toBeInTheDocument()
    })
  })

  it('TC-F5.3.1-09c: khoảng ngày ngược báo lỗi và không gửi bộ lọc sai', async () => {
    const api = makeGateway()
    renderPage(api)
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Từ ngày lập'), { target: { value: '2026-11-25' } })
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ fromDate: '2026-11-25' }),
      ),
    )
    const calls = vi.mocked(api.list).mock.calls.length
    fireEvent.change(screen.getByLabelText('Đến ngày lập'), { target: { value: '2026-11-24' } })
    expect(
      await screen.findByText('Ngày bắt đầu không được lớn hơn ngày kết thúc.'),
    ).toBeInTheDocument()
    expect(api.list).toHaveBeenCalledTimes(calls)
  })

  it('TC-F5.3.1-04e: lỗi danh mục chi nhánh có thể thử lại, danh sách hóa đơn vẫn tải', async () => {
    const user = userEvent.setup()
    const api = makeGateway()
    api.branches = vi
      .fn()
      .mockRejectedValueOnce(new Error('Chưa tải được danh mục chi nhánh.'))
      .mockResolvedValue([{ id: 1, code: 'CN01', name: 'Chi nhánh 1' }])
    renderPage(api, 'SUPER_ADMIN')
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    expect(await screen.findByText('Chưa tải được danh mục chi nhánh.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Tải lại chi nhánh' }))
    await waitFor(() => expect(api.branches).toHaveBeenCalledTimes(2))
    await waitFor(() =>
      expect(screen.queryByText('Chưa tải được danh mục chi nhánh.')).not.toBeInTheDocument(),
    )
    expect(screen.getByLabelText('Chi nhánh')).toBeEnabled()
  })

  it('TC-F5.3.1-02d: lỗi danh sách có thể tải lại và hiển thị trạng thái rỗng', async () => {
    const user = userEvent.setup()
    const api = makeGateway([])
    api.list = vi
      .fn()
      .mockRejectedValueOnce(new Error('Mất kết nối danh sách hóa đơn.'))
      .mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    renderPage(api)
    expect(await screen.findByText('Mất kết nối danh sách hóa đơn.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Không có hóa đơn phù hợp.')).toBeInTheDocument()
  })

  it('TC-F5.3.1-02c: lọc danh sách hóa đơn theo trạng thái, nguồn, khoảng ngày, từ khóa và phân trang', async () => {
    const user = userEvent.setup()
    const api = makeGateway(sampleRows, 25)
    renderPage(api)

    expect(await screen.findByText('HD000088')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Trạng thái'))
    await user.click(await screen.findByTitle('Chờ thanh toán'))
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'PENDING', page: 0, size: 20 }),
      ),
    )

    await user.click(screen.getByLabelText('Nguồn'))
    await user.click(await screen.findByTitle('Cổng hội viên'))
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'PENDING', source: 'MEMBER_PORTAL', page: 0 }),
      ),
    )

    fireEvent.change(screen.getByLabelText('Từ ngày lập'), { target: { value: '2026-11-01' } })
    fireEvent.change(screen.getByLabelText('Đến ngày lập'), { target: { value: '2026-11-20' } })
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fromDate: '2026-11-01',
          toDate: '2026-11-20',
          page: 0,
        }),
      ),
    )

    await user.type(screen.getByLabelText('Tìm kiếm'), 'HD000088{Enter}')
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ keyword: 'HD000088', page: 0 }),
      ),
    )

    await user.click(screen.getByTitle('2'))
    await waitFor(() =>
      expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 20 })),
    )
  }, 10000)

  it('TC-F5.3.1-03b: hiển thị hóa đơn khách lẻ và tiền định dạng VND số nguyên', async () => {
    const api = makeGateway()
    renderPage(api)

    expect(await screen.findByText('HD000089')).toBeInTheDocument()
    expect(screen.getByText('Khách lẻ')).toBeInTheDocument()
    expect(screen.getByText(/3\.000\.000\s*₫/)).toBeInTheDocument()
    expect(screen.getByText(/50\.000\s*₫/)).toBeInTheDocument()
  })
})
