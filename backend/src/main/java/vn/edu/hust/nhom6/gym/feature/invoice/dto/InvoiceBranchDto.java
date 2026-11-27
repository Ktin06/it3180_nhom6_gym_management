package vn.edu.hust.nhom6.gym.feature.invoice.dto;

/** Danh mục chỉ đọc cho bộ lọc hóa đơn của Chủ chuỗi; gồm cả chi nhánh ngừng hoạt động. */
public record InvoiceBranchDto(Long id, String code, String name) { }
