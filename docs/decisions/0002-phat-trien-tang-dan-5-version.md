# ADR 0002: Phát triển tăng dần theo 5 version

- **Ngày:** 27/10/2026 (cập nhật lịch 4 sprint ngày 01/11/2026)
- **Người quyết định:** cả nhóm 6
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

20 chức năng (F1–F20) phải làm trong 4 sprint, mỗi sprint 2 tuần, từ 27/10/2026 đến 21/12/2026; bàn giao Sprint 1 ngày 10/11/2026, bàn giao cuối và bảo vệ ở tuần 16. Rủi ro trễ tiến độ cao vì nhóm 8 người lần đầu làm dự án cỡ này. Nguồn: [bản chốt phạm vi](../requirements/ban-chot-pham-vi.md).

## Quyết định

Phát triển tăng dần theo 5 version; mỗi version là một bản chạy được trên môi trường staging.

| Version | Chủ đề | Chức năng | Mức cam kết |
| --- | --- | --- | --- |
| v1 | Vận hành cơ bản tại quầy (một chi nhánh mặc định) | F1–F6 | Bắt buộc |
| v2 | Hội viên, lớp học và huấn luyện viên | F7–F10 | Bắt buộc |
| v3 | Vận hành nâng cao | F11–F17 | Mục tiêu |
| v4 | Chuỗi phòng tập | F18–F19 | Mục tiêu; cắt thứ hai nếu trễ |
| v5 | Nhận diện khuôn mặt (tính năng AI duy nhất) | F20 | Cắt đầu tiên nếu trễ |

- Nếu trễ thì cắt v5 trước, rồi đến v4.
- Từ v1 đến v3 hệ thống chạy với một chi nhánh mặc định, nhưng cơ sở dữ liệu có `branch_id` ngay từ v1 để v4 không phải sửa schema.
- `main` luôn chạy được và là nguồn triển khai staging (xem [ADR 0006](0006-git-workflow.md)).

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Làm đủ 20 chức năng rồi mới tích hợp | Không có bản demo giữa kỳ; rủi ro "không có gì chạy được" ở cuối kỳ |
| Chia theo tầng (một người làm toàn bộ cơ sở dữ liệu, một người làm toàn bộ giao diện) | Khó chia đều, phụ thuộc chéo nhiều; không khớp với cách chấm theo chức năng |

## Hệ quả

- Mỗi version có danh sách chức năng và tiêu chí chấp nhận rõ ràng; nhãn GitHub `v1`…`v5` đánh dấu issue.
- Thứ tự cắt khi trễ đã thống nhất trước, không phải tranh luận lại.
- Thiết kế dữ liệu phải tính trước cho cả 20 chức năng (39 bảng), kể cả phần làm ở v3–v5.
