import { App } from 'antd'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemberInvoicePdfPage } from './MemberInvoicePdfPage'

afterEach(() => vi.restoreAllMocks())

function renderPage(pdf: (id: number) => Promise<Blob>, id = '88') {
  return render(
    <App>
      <MemoryRouter initialEntries={[`/member/invoices/${id}/pdf`]}>
        <Routes>
          <Route path="/member/invoices/:id/pdf" element={<MemberInvoicePdfPage api={{ pdf }} />} />
        </Routes>
      </MemoryRouter>
    </App>,
  )
}

describe('PDF hóa đơn của hội viên', () => {
  it('TC-F5.4.1-04d: gọi API PDF trực tiếp, không cần quyền API chi tiết hóa đơn nhân viên', async () => {
    const user = userEvent.setup()
    const pdf = vi.fn().mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:member-invoice')
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    const opened = { opener: window } as unknown as Window
    vi.spyOn(window, 'open').mockReturnValue(opened)
    const { unmount } = renderPage(pdf)
    await user.click(screen.getByRole('button', { name: 'In / tải PDF' }))
    expect(pdf).toHaveBeenCalledWith(88)
    expect(create).toHaveBeenCalledOnce()
    expect(opened.opener).toBeNull()
    unmount()
    expect(revoke).toHaveBeenCalledWith('blob:member-invoice')
  })

  describe('TC-F5.4.1-04e: hiển thị lỗi API %s, không tạo PDF từ dữ liệu giả', () => {
    it.each([404, 422])('trường hợp %s', async (status) => {
      const user = userEvent.setup()
      const detail =
        status === 404 ? 'Hóa đơn không tồn tại trên hệ thống.' : 'Hóa đơn chưa thanh toán.'
      const error = new AxiosError('Request failed')
      error.response = {
        status,
        statusText: 'Error',
        headers: {},
        config: { headers: new AxiosHeaders() },
        data: { detail },
      }
      const pdf = vi.fn().mockRejectedValue(error)
      const create = vi.spyOn(URL, 'createObjectURL')
      renderPage(pdf)
      await user.click(screen.getByRole('button', { name: 'In / tải PDF' }))
      expect(await screen.findByText(detail)).toBeInTheDocument()
      expect(create).not.toHaveBeenCalled()
    })
  })

  describe('TC-F5.4.1-07c: ID %s bị chặn trước khi gọi tải PDF', () => {
    it.each(['0', '-1', 'abc', '9223372036854775808'])('trường hợp %s', async (id) => {
      const pdf = vi.fn()
      renderPage(pdf, id)
      expect(screen.getByText('Mã hóa đơn không hợp lệ.')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'In / tải PDF' })).toBeDisabled()
      expect(pdf).not.toHaveBeenCalled()
    })
  })
})
