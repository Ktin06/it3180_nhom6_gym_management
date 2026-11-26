package vn.edu.hust.nhom6.gym.feature.invoice.dto;

/** Kết quả xuất hóa đơn PDF gồm tên file gợi ý ({@code hoa-don-{code}.pdf}) và nội dung nhị phân PDF. */
public record InvoicePdfDocument(String filename, byte[] content) {
}
