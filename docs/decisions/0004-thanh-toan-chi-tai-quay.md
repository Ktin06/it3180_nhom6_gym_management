# ADR 0004: Thanh toán chỉ tại quầy

- **Ngày:** 27/10/2026 (bổ sung hóa đơn chờ F7 ngày 01/11/2026)
- **Người quyết định:** cả nhóm 6
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

Bản phạm vi ngày 10/10 có chức năng thanh toán online (VNPay, ví điện tử). Tích hợp cổng thanh toán cần tài khoản doanh nghiệp, môi trường sandbox, xử lý callback và đối soát; khó demo và khó kiểm thử trong 4 sprint. Giảng viên ưu tiên chức năng quản lý dễ kiểm thử. Nguồn: mục "Thanh toán" và "Phạm vi không làm" trong [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md).

## Quyết định

- Chỉ thanh toán tại quầy: tiền mặt hoặc chuyển khoản qua mã VietQR có sẵn số tiền; lễ tân hoặc quản lý bấm xác nhận đã nhận tiền (S7).
- Hóa đơn do lễ tân lập tại quầy, hoặc do hội viên tạo ở trạng thái chờ (PENDING) khi gửi yêu cầu mua hoặc gia hạn gói từ cổng hội viên (F7); hóa đơn chờ quá 3 ngày\* tự hủy.
- Giá luôn lấy từ server; dòng hóa đơn lưu giá tại thời điểm bán.
- Gói chỉ kích hoạt khi hóa đơn ở trạng thái đã thanh toán (PAID).
- Chỉ quản lý chi nhánh được hoàn tiền; mọi lần hoàn tiền ghi nhật ký hoạt động.

(\*) Tham số `PENDING_INVOICE_TTL_DAYS` trong `system_configs`.

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Cổng thanh toán online (VNPay, ví điện tử) | Cần tài khoản doanh nghiệp, callback, đối soát; khó demo, khó kiểm thử; đưa vào hướng phát triển |
| VietQR kèm đối soát tự động qua API ngân hàng | Cần API ngân hàng trả phí; lễ tân xác nhận thủ công là đủ cho demo |

## Hệ quả

- Bỏ chức năng thanh toán online (mang mã F10 ở bản phạm vi 10/10; F10 hiện nay là Thông báo); không có bảng giao dịch cổng thanh toán.
- Bảng `payments` có `received_by` (người xác nhận) và `transaction_ref` (nội dung chuyển khoản).
- Cần tác vụ nền hủy hóa đơn chờ quá hạn và gói PENDING đi kèm.
