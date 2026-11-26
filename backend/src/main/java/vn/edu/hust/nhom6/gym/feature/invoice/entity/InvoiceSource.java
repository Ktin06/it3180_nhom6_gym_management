package vn.edu.hust.nhom6.gym.feature.invoice.entity;

/**
 * Khớp {@code ck_invoices_source}. COUNTER: lễ tân lập, không tự hủy. MEMBER_PORTAL: hội viên tự gửi (F7),
 * tự hủy sau {@code PENDING_INVOICE_TTL_DAYS} (BR-F3-14).
 */
public enum InvoiceSource { COUNTER, MEMBER_PORTAL }
