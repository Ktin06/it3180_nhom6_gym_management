package vn.edu.hust.nhom6.gym.feature.invoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Số tiếp theo của mã hóa đơn (V9_05). Sequence không quay lui khi giao dịch rollback nên mã có thể nhảy số. */
    @Query(value = "SELECT nextval('invoice_code_seq')", nativeQuery = true)
    long nextInvoiceCode();
}
