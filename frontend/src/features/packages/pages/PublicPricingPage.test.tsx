import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { http } from '../../../services/http'
import { publicPackagesApi } from '../api/public-packages'
import type { PublicPackage } from '../types/package'
import { PublicPricingPage } from './PublicPricingPage'

const packages: PublicPackage[] = [
  { id: 1, name: 'Gói 1 tháng', type: 'TIME', durationDays: 30, price: 500000 },
  { id: 2, name: '12 buổi', type: 'SESSION', durationDays: 60, sessionCount: 12, price: 600000 },
  { id: 3, name: 'PT 10 buổi', type: 'PT', durationDays: 60, sessionCount: 10, price: 3000000 },
]

function renderPage(list: () => Promise<PublicPackage[]>) {
  const api = { list: vi.fn(list) }
  render(
    <MemoryRouter>
      <PublicPricingPage api={api} />
    </MemoryRouter>,
  )
  return api
}

afterEach(() => vi.restoreAllMocks())

describe('bảng giá công khai', () => {
  it('TC-F3.1.2-01b: hiện giá từ server, thời hạn hoặc số buổi, quyền lợi theo loại và thanh toán tại quầy', async () => {
    renderPage(async () => packages)

    expect(await screen.findByText('Gói 1 tháng')).toBeInTheDocument()
    expect(screen.getByText(/500\.000/)).toBeInTheDocument()
    expect(screen.getByText('Thời hạn 30 ngày')).toBeInTheDocument()
    expect(screen.getByText('12 buổi, dùng trong 60 ngày')).toBeInTheDocument()
    expect(screen.getByText('Được đặt lớp nhóm')).toBeInTheDocument()
    expect(screen.getByText('Không đặt được lớp nhóm')).toBeInTheDocument()
    expect(screen.getByText('Không dùng để vào phòng tập hay đặt lớp nhóm')).toBeInTheDocument()
    expect(screen.getByText('Thanh toán tại quầy')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /thanh toán|mua ngay|giỏ hàng/i }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Đăng ký tài khoản' })).toHaveAttribute(
      'href',
      '/register',
    )
  })

  it('TC-F3.1.2-01c: gọi API công khai không kèm access token và không tự làm mới phiên', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data: packages })

    await expect(publicPackagesApi.list()).resolves.toEqual(packages)
    expect(get).toHaveBeenCalledWith('/public/packages', {
      skipBearer: true,
      skipAuthRefresh: true,
    })
  })

  it('TC-F3.1.2-04b: chưa có gói đang bán thì báo trống, không hiện giá nào', async () => {
    renderPage(async () => [])

    expect(await screen.findByText('Chưa có gói đang bán.')).toBeInTheDocument()
    expect(screen.queryByText(/₫/)).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Đăng ký tài khoản' })).toBeInTheDocument()
  })

  it('TC-F3.1.2-05a: lỗi tải thì báo lỗi, không hiện giá; bấm Thử lại thì gọi lại API', async () => {
    const user = userEvent.setup()
    let calls = 0
    const api = renderPage(async () => {
      calls += 1
      if (calls === 1) throw new Error('mạng lỗi')
      return packages
    })

    expect(
      await screen.findByText('Không tải được bảng giá. Vui lòng thử lại.'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/₫/)).not.toBeInTheDocument()
    expect(screen.queryByText('Chưa có gói đang bán.')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Gói 1 tháng')).toBeInTheDocument()
    await waitFor(() => expect(api.list).toHaveBeenCalledTimes(2))
  })
})
