import { Alert, Button, Space } from 'antd'
import { Link, useParams } from 'react-router-dom'
import { invoicesApi } from '../api/invoices'
import { parseInvoiceId } from '../invoice-format'
import type { InvoicesGateway } from '../types/invoice'
import { useInvoicePdf } from '../use-invoice-pdf'
import '../../shared/feature-pages.css'

export function MemberInvoicePdfPage({
  api = invoicesApi,
}: {
  api?: Pick<InvoicesGateway, 'pdf'>
}) {
  const { id } = useParams()
  return <MemberInvoicePdfContent key={id} id={id} api={api} />
}

/** F7 dẫn sang đây bằng invoiceId; không gọi API danh sách/chi tiết dành cho nhân viên. */
function MemberInvoicePdfContent({
  id,
  api,
}: {
  id: string | undefined
  api: Pick<InvoicesGateway, 'pdf'>
}) {
  const invoiceId = parseInvoiceId(id)
  const pdf = useInvoicePdf(api.pdf)
  return (
    <section className="feature-page" aria-labelledby="member-invoice-heading">
      <div className="feature-heading">
        <h2 id="member-invoice-heading">Hóa đơn thanh toán</h2>
      </div>
      <div className="feature-content">
        <p>Tải hóa đơn đã thanh toán của bạn để xem, lưu hoặc in.</p>
        {!invoiceId && <Alert type="error" showIcon title="Mã hóa đơn không hợp lệ." />}
        {pdf.error && <Alert type="error" showIcon title={pdf.error} />}
        <Space wrap style={{ marginTop: 16 }}>
          <Link to="/member">
            <Button>Về trang hội viên</Button>
          </Link>
          <Button
            type="primary"
            disabled={!invoiceId}
            loading={pdf.exporting}
            onClick={() => invoiceId && void pdf.exportPdf(invoiceId, `hoa-don-${invoiceId}.pdf`)}
          >
            In / tải PDF
          </Button>
        </Space>
      </div>
    </section>
  )
}
