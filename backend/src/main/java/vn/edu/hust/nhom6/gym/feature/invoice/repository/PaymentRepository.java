package vn.edu.hust.nhom6.gym.feature.invoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hust.nhom6.gym.feature.invoice.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Danh sách phiếu thu của một hóa đơn, mới nhất trước (chi tiết hóa đơn S2-09.3). */
    List<Payment> findByInvoiceIdOrderByPaidAtDescIdDesc(Long invoiceId);
}
