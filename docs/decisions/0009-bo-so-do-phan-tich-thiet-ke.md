# ADR 0009: Bộ sơ đồ phân tích và thiết kế

- **Ngày:** 02/11/2026
- **Người quyết định:** cả nhóm tại buổi họp kick-off 02/11 ([biên bản](../meetings/2026-11-02-kick-off.md))
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

Bản chốt phạm vi còn để mở câu hỏi bài tập lớn dùng sơ đồ UML, DFD hay cả hai. Kế hoạch Sprint 1 mới có sơ đồ use case, sơ đồ hoạt động và ERD. Nhóm cần một danh sách sơ đồ cố định, để chia việc cho các sprint và để báo cáo LaTeX có đủ phần khảo sát, phân tích, thiết kế.

## Quyết định

Bài tập lớn dùng 8 loại sơ đồ, chia 3 nhóm. Hệ thống viết theo hướng đối tượng (Spring Boot), nên dùng sơ đồ UML, không dùng DFD.

| Nhóm | Sơ đồ | Phạm vi | Thư mục |
| --- | --- | --- | --- |
| Khảo sát và phân tích nghiệp vụ | Biểu đồ phân cấp chức năng | F1–F20 | `docs/design/function/` |
| | Sơ đồ use case | Tổng quan và phân rã một số chức năng chính | `docs/design/use-case/` |
| | Sơ đồ hoạt động | 2–3 nghiệp vụ quan trọng: check-in, danh sách chờ, đặt lịch PT | `docs/design/activity/` |
| Phân tích hệ thống | Sơ đồ tuần tự | 2–3 luồng quan trọng | `docs/design/sequence/` |
| | Sơ đồ lớp phân tích | Toàn hệ thống | `docs/design/class/` |
| Thiết kế hệ thống | Sơ đồ kiến trúc | Frontend, backend, cơ sở dữ liệu, triển khai | `docs/design/architecture/` |
| | ERD | 39 bảng | `docs/design/erd/` (đã có) |
| | Sơ đồ lớp thiết kế | Theo gói chức năng của backend | `docs/design/class/` |

Mọi sơ đồ vẽ bằng draw.io, lưu dạng `.drawio.svg` ([docs/README.md](../README.md)). Riêng ERD dùng `erd-v2.dbml` làm bản chuẩn.

## Phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Bộ UML 8 loại sơ đồ (đã chọn) | Khớp cách viết mã hướng đối tượng; sơ đồ lớp, tuần tự đối chiếu được với mã | Nhiều loại sơ đồ phải giữ khớp với mã |
| DFD (sơ đồ luồng dữ liệu) | Quen thuộc với phân tích có cấu trúc | Không khớp với kiến trúc hướng đối tượng; phải vẽ thêm UML cho phần thiết kế |
| Cả UML và DFD | Đầy đủ nhất | Gấp đôi công vẽ, không thêm thông tin |

## Hệ quả

- Sprint 1 làm sơ đồ use case, sơ đồ hoạt động và ERD như kế hoạch.
- Biểu đồ phân cấp chức năng, sơ đồ tuần tự, hai sơ đồ lớp và sơ đồ kiến trúc đưa vào kế hoạch Sprint 2–3.
- Sơ đồ lớp thiết kế và sơ đồ tuần tự phải khớp với mã. Khi đổi tên lớp hoặc luồng gọi, cập nhật sơ đồ trong cùng pull request.
