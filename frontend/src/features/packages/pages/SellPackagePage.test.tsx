import { App } from 'antd'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import { ROLES } from '../../../auth/roles'
import { menuForRole } from '../../../layouts/staff-menu'
import { canOpen } from '../../../routes/route-access'
import { createMockMembersApi } from '../../members/api/mock-members'
import { mockPackagesApi } from '../api/mock-packages'
import type { HeldMembership, PackageInvoice, PackageSalesGateway } from '../types/sale'
import { SellPackagePage } from './SellPackagePage'

function axiosProblem(status: number, data: Record<string, unknown>) {
  const error = new AxiosError('Request failed')
  error.response = {
    status,
    data,
    statusText: 'Error',
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

function createSalesStub(
  initialMemberships: Record<number, HeldMembership[]> = {},
): PackageSalesGateway {
  return {
    async listMemberships(memberId) {
      return structuredClone(initialMemberships[memberId] ?? [])
    },
    async createInvoice(input) {
      const response: PackageInvoice = {
        invoiceId: 88,
        invoiceCode: 'HD000088',
        membershipId: 77,
        memberId: input.memberId,
        packageId: input.items[0].packageId,
        packageName: 'Gói 1 tháng',
        subtotal: 500000,
        discount: 0,
        total: 500000,
        status: 'PENDING',
        membershipStatus: 'PENDING',
        startDate: '2026-11-20',
        endDate: '2026-12-19',
      }
      return response
    },
  }
}

describe('màn hình bán và gia hạn gói tại quầy F3', () => {
  it('TC-F3.5.1-01e: Chọn hội viên và gói, chỉ gửi memberId và items lên POST /invoices, hiển thị mã hóa đơn, tổng tiền VND, ngày dự kiến và trạng thái Chờ thanh toán', async () => {
    const user = userEvent.setup()
    const membersApi = createMockMembersApi()
    const packagesApi = mockPackagesApi
    const salesApi = createSalesStub()
    const listMemberships = vi.spyOn(salesApi, 'listMemberships')
    const createInvoice = vi.spyOn(salesApi, 'createInvoice')

    render(
      <App>
        <SellPackagePage members={membersApi} packages={packagesApi} sales={salesApi} />
      </App>,
    )

    await user.click(await screen.findByRole('button', { name: 'Chọn HV000030' }))
    await waitFor(() => expect(listMemberships).toHaveBeenCalledWith(30))
    expect(await screen.findByText(/chưa có gói nào/i)).toBeInTheDocument()

    await user.click(await screen.findByRole('button', { name: 'Chọn gói TIME-1M' }))
    expect(screen.getByText('Mua mới')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Tạo hóa đơn/ }))
    await waitFor(() => expect(createInvoice).toHaveBeenCalledOnce())

    const sentPayload = createInvoice.mock.calls[0][0]
    expect(sentPayload).toEqual({
      memberId: 30,
      items: [{ itemType: 'PACKAGE', packageId: 1 }],
    })
    expect('price' in sentPayload).toBe(false)
    expect('subtotal' in sentPayload).toBe(false)
    expect('total' in sentPayload).toBe(false)

    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument()
    expect(screen.getAllByText(/500\.000/).length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('20/11/2026')).toBeInTheDocument()
    expect(screen.getByText('19/12/2026')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Đã thanh toán/i })).not.toBeInTheDocument()
  })

  it('TC-F3.5.1-03d: Hội viên có gói TIME ACTIVE cùng phạm vi thì hiện nhãn "Gia hạn nối tiếp" và ngày bắt đầu dự kiến từ renewalStartDate của server', async () => {
    const user = userEvent.setup()
    const membersApi = createMockMembersApi()
    const packagesApi = mockPackagesApi
    const salesApi = createSalesStub({
      30: [
        {
          id: 11,
          packageId: 1,
          packageName: 'Gói 1 tháng',
          packageType: 'TIME',
          branchId: 1,
          status: 'ACTIVE',
          startDate: '2026-12-01',
          endDate: '2026-12-31',
          totalSessions: null,
          remainingSessions: null,
          renewalStartDate: '2027-01-01',
        },
      ],
    })

    render(
      <App>
        <SellPackagePage members={membersApi} packages={packagesApi} sales={salesApi} />
      </App>,
    )

    await user.click(await screen.findByRole('button', { name: 'Chọn HV000030' }))
    expect(await screen.findByText('Đang hiệu lực')).toBeInTheDocument()

    await user.click(await screen.findByRole('button', { name: 'Chọn gói TIME-1M' }))
    expect(screen.getByText('Gia hạn nối tiếp')).toBeInTheDocument()
    expect(screen.getAllByText(/01\/01\/2027/).length).toBeGreaterThanOrEqual(1)

    // Gói toàn chuỗi (branchId = null) là phạm vi khác với gói chi nhánh (S2-07.1).
    await user.click(screen.getByRole('button', { name: 'Chọn gói TIME-6M' }))
    expect(screen.getByText('Mua mới')).toBeInTheDocument()
    expect(screen.queryByText('Gia hạn nối tiếp')).not.toBeInTheDocument()
  })

  it('TC-F3.5.1-04e: Hội viên có gói SESSION đã hết buổi hiển thị ngày gia hạn dự kiến lấy trực tiếp từ renewalStartDate của server', async () => {
    const user = userEvent.setup()
    const membersApi = createMockMembersApi()
    const packagesApi = mockPackagesApi
    const salesApi = createSalesStub({
      30: [
        {
          id: 12,
          packageId: 3,
          packageName: 'Gói 10 buổi',
          packageType: 'SESSION',
          branchId: 1,
          status: 'ACTIVE',
          startDate: '2026-11-01',
          endDate: '2026-11-30',
          totalSessions: 10,
          remainingSessions: 0,
          renewalStartDate: '2026-11-21',
        },
      ],
    })

    render(
      <App>
        <SellPackagePage members={membersApi} packages={packagesApi} sales={salesApi} />
      </App>,
    )

    await user.click(await screen.findByRole('button', { name: 'Chọn HV000030' }))
    expect(await screen.findByText(/Còn 0\/10 buổi/)).toBeInTheDocument()

    await user.click(await screen.findByRole('button', { name: 'Chọn gói SESSION-10' }))
    expect(screen.getByText('Gia hạn nối tiếp')).toBeInTheDocument()
    expect(screen.getAllByText(/21\/11\/2026/).length).toBeGreaterThanOrEqual(1)
    expect(screen.queryByText(/01\/12\/2026/)).not.toBeInTheDocument()
  })

  it.each(['PENDING', 'UPCOMING'] as const)(
    'TC-F3.5.1-06d: chặn lập hóa đơn khi đã có gói %s cùng phạm vi',
    async (status) => {
      const user = userEvent.setup()
      const membersApi = createMockMembersApi()
      const packagesApi = mockPackagesApi
      const salesApi = createSalesStub({
        30: [
          {
            id: 13,
            packageId: 1,
            packageName: 'Gói 1 tháng',
            packageType: 'TIME',
            branchId: 1,
            status,
            startDate: '2026-11-20',
            endDate: '2026-12-19',
            totalSessions: null,
            remainingSessions: null,
            renewalStartDate: null,
          },
        ],
      })
      const create = vi.spyOn(salesApi, 'createInvoice')

      render(
        <App>
          <SellPackagePage members={membersApi} packages={packagesApi} sales={salesApi} />
        </App>,
      )

      await user.click(await screen.findByRole('button', { name: 'Chọn HV000030' }))
      await user.click(await screen.findByRole('button', { name: 'Chọn gói TIME-1M' }))
      expect(await screen.findByText('Đang có gói chờ cùng loại')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Tạo hóa đơn/ })).toBeDisabled()
      await user.click(screen.getByRole('button', { name: /Tạo hóa đơn/ }))
      expect(create).not.toHaveBeenCalled()
    },
  )

  it('TC-F3.5.1-01: giữ lựa chọn khi tạo hóa đơn và yêu cầu tải lại nếu kiểm tra gói thất bại', async () => {
    const user = userEvent.setup()
    const sales = createSalesStub()
    const load = vi.spyOn(sales, 'listMemberships')
    let finish!: () => void
    const pending = new Promise<void>((resolve) => {
      finish = resolve
    })
    const original = sales.createInvoice
    const create = vi.spyOn(sales, 'createInvoice').mockImplementation(async (input) => {
      await pending
      return original(input)
    })
    render(
      <App>
        <SellPackagePage
          members={createMockMembersApi()}
          packages={mockPackagesApi}
          sales={sales}
        />
      </App>,
    )
    await user.click(await screen.findByRole('button', { name: 'Chọn HV000030' }))
    await user.click(await screen.findByRole('button', { name: 'Chọn gói TIME-1M' }))
    const submit = screen.getByRole('button', { name: /Tạo hóa đơn/ })
    await user.click(submit)
    const other = screen.getByRole('button', { name: 'Chọn HV000028' })
    expect(other).toBeDisabled()
    for (const button of screen.getAllByRole('button', { name: /^Chọn gói / })) {
      expect(button).toBeDisabled()
    }
    await user.click(other)
    expect(load).not.toHaveBeenCalledWith(28)
    load.mockRejectedValueOnce(new Error('Chưa tải được gói'))
    await act(async () => finish())
    expect(await screen.findByText('HD000088')).toBeInTheDocument()
    expect(await screen.findByText('Chưa tải được gói')).toBeInTheDocument()
    expect(submit).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Thử lại' }))
    await waitFor(() => expect(submit).toBeEnabled())
    await user.click(other)
    expect(load).toHaveBeenLastCalledWith(28)
    expect(screen.queryByText('HD000088')).not.toBeInTheDocument()
    expect(create).toHaveBeenCalledTimes(1)
  })

  it('TC-F3.5.1-08g: Lỗi 422 hoặc 404 khi hội viên ngừng hoạt động, gói ngừng bán hoặc ngoài chi nhánh hiển thị đúng thông báo từ API', async () => {
    const user = userEvent.setup()
    const membersApi = createMockMembersApi()
    const packagesApi = mockPackagesApi
    const salesApi = createSalesStub()
    vi.spyOn(salesApi, 'createInvoice')
      .mockRejectedValueOnce(
        axiosProblem(422, { detail: 'Hội viên đã ngừng hoạt động, không thể bán gói tập.' }),
      )
      .mockRejectedValueOnce(axiosProblem(422, { detail: 'Gói tập này đã ngừng bán.' }))
      .mockRejectedValueOnce(axiosProblem(404, { detail: 'Gói tập không tồn tại trên hệ thống.' }))

    render(
      <App>
        <SellPackagePage members={membersApi} packages={packagesApi} sales={salesApi} />
      </App>,
    )

    await user.click(await screen.findByRole('button', { name: 'Chọn HV000030' }))
    expect(await screen.findByText(/chưa có gói nào/i)).toBeInTheDocument()
    await user.click(await screen.findByRole('button', { name: 'Chọn gói TIME-1M' }))

    for (const expectedError of [
      'Hội viên đã ngừng hoạt động, không thể bán gói tập.',
      'Gói tập này đã ngừng bán.',
      'Gói tập không tồn tại trên hệ thống.',
    ]) {
      await user.click(screen.getByRole('button', { name: /Tạo hóa đơn/ }))
      expect(await screen.findByText(expectedError)).toBeInTheDocument()
    }
  })

  it('S3: Đổi hội viên xóa kết quả hóa đơn cũ và bỏ qua phản hồi danh sách gói hiện có đến muộn', async () => {
    const user = userEvent.setup()
    const membersApi = createMockMembersApi()
    const packagesApi = mockPackagesApi
    const salesApi = createSalesStub()

    let resolveFirst!: (rows: HeldMembership[]) => void
    const listMemberships = vi
      .spyOn(salesApi, 'listMemberships')
      .mockImplementationOnce(
        () =>
          new Promise<HeldMembership[]>((resolve) => {
            resolveFirst = resolve
          }),
      )
      .mockResolvedValueOnce([
        {
          id: 22,
          packageId: 3,
          packageName: 'Gói 10 buổi',
          packageType: 'SESSION',
          branchId: 1,
          status: 'ACTIVE',
          startDate: '2026-11-01',
          endDate: '2026-11-30',
          totalSessions: 10,
          remainingSessions: 4,
          renewalStartDate: '2026-12-01',
        },
      ])

    render(
      <App>
        <SellPackagePage members={membersApi} packages={packagesApi} sales={salesApi} />
      </App>,
    )

    await user.click(await screen.findByRole('button', { name: 'Chọn HV000030' }))
    await user.click(screen.getByRole('button', { name: 'Chọn HV000028' }))

    await waitFor(() => expect(listMemberships).toHaveBeenCalledTimes(2))
    expect(await screen.findByText(/Còn 4\/10 buổi/)).toBeInTheDocument()

    await act(async () =>
      resolveFirst([
        {
          id: 99,
          packageId: 1,
          packageName: 'Gói cũ đến muộn',
          packageType: 'TIME',
          branchId: 1,
          status: 'ACTIVE',
          startDate: '2026-11-01',
          endDate: '2026-11-30',
          totalSessions: null,
          remainingSessions: null,
          renewalStartDate: '2026-12-01',
        },
      ]),
    )

    expect(screen.queryByText('Gói cũ đến muộn')).not.toBeInTheDocument()
    expect(screen.getByText(/Còn 4\/10 buổi/)).toBeInTheDocument()
  })

  it('N0.3 và F2: tìm hội viên theo mã hoạt động đúng và /ban-goi chỉ mở cho RECEPTIONIST', async () => {
    const user = userEvent.setup()
    const membersApi = createMockMembersApi()
    const listMembers = vi.spyOn(membersApi, 'list')

    render(
      <App>
        <SellPackagePage
          members={membersApi}
          packages={mockPackagesApi}
          sales={createSalesStub()}
        />
      </App>,
    )

    await screen.findByRole('button', { name: 'Chọn HV000030' })
    await user.click(screen.getByLabelText('Tìm hội viên'))
    await user.paste('HV000002')
    await user.click(screen.getByRole('button', { name: 'Tìm' }))

    await waitFor(() =>
      expect(listMembers).toHaveBeenLastCalledWith({
        page: 0,
        size: 10,
        status: 'ACTIVE',
        keyword: 'HV000002',
      }),
    )
    expect(await screen.findByRole('button', { name: 'Chọn HV000002' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Chọn HV000030' })).not.toBeInTheDocument()

    expect(canOpen('RECEPTIONIST', '/ban-goi')).toBe(true)
    expect(menuForRole('RECEPTIONIST').some((item) => item.path === '/ban-goi' && item.ready)).toBe(
      true,
    )
    for (const role of ROLES.filter((r) => r !== 'RECEPTIONIST')) {
      expect(canOpen(role, '/ban-goi')).toBe(false)
      expect(menuForRole(role).some((item) => item.path === '/ban-goi')).toBe(false)
    }
  })
})
