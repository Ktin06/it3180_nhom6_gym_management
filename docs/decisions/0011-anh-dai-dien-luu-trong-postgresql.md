# ADR 0011: Ảnh đại diện hội viên lưu trong PostgreSQL

- **Ngày:** 14/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm), chốt phương án ngày 12/11/2026 khi lập kế hoạch Sprint 2 (việc S2-01)
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

F2 có ảnh đại diện hội viên. F4 (check-in tại quầy) phải luôn hiện ảnh để lễ tân đối chiếu, chống mượn thẻ ([bản chốt phạm vi](../requirements/ban-chot-pham-vi.md), mục F4). Câu hỏi "lưu ảnh ở đâu" được để mở từ 01/11 ([thiết kế dữ liệu](../design/erd/thiet-ke-du-lieu.md), mục câu hỏi còn mở) và phải chốt trước khi làm F4 ở Sprint 2.

Ràng buộc:

- [ADR 0001](0001-cong-nghe.md) chỉ có Vercel (frontend), Render (backend) và Neon (PostgreSQL), không có dịch vụ lưu file.
- Ổ đĩa của Render bản miễn phí bị xóa mỗi lần triển khai lại, nên không lưu file trên Render.
- Ảnh hội viên là dữ liệu cá nhân: chỉ người có quyền xem hồ sơ mới được xem ảnh (S3).
- Dung lượng: database staging trên Neon dùng khoảng 34 MB (12/11). Ảnh đã thu nhỏ khoảng 20–40 KB; 1.000 hội viên thêm khoảng 30 MB, vẫn trong hạn mức gói miễn phí.

## Quyết định

1. **Lưu ảnh trong PostgreSQL**, bảng riêng `member_avatars`, mỗi hội viên tối đa một ảnh (`member_id` duy nhất). Bảng con của `members`, lấy chi nhánh qua `members`, không có `branch_id` riêng (quy tắc mã số 9).
2. **Bỏ cột `members.avatar_url`.** Đường dẫn ảnh suy ra được từ mã hội viên, lưu thêm là lưu một thông tin hai nơi (quy tắc mã số 12).
3. **Server thu nhỏ và nén trước khi lưu.** Nhận JPEG hoặc PNG, tối đa 5 MB, mỗi cạnh tối đa 8.000 px (đọc kích thước trước khi giải nén để chặn ảnh quá lớn). Thu nhỏ để cạnh dài nhất 256 px, lưu JPEG chất lượng khoảng 0,8. Ảnh sau nén không quá 100 KB (`CHECK`). Không tin loại file do trình duyệt khai báo; kiểm tra bằng nội dung file.
4. **API có kiểm tra quyền, dùng chung quy tắc với hồ sơ hội viên (F2):**
   - `GET /api/v1/members/{id}/avatar`: ai xem được `GET /api/v1/members/{id}` thì xem được ảnh (chủ chuỗi, quản lý chi nhánh, lễ tân; hội viên chỉ xem ảnh của chính mình). Trả `image/jpeg`; chưa có ảnh thì 404.
   - `PUT /api/v1/members/{id}/avatar` (multipart): ai sửa được hồ sơ thì đổi được ảnh (chủ chuỗi, quản lý chi nhánh, lễ tân). Hội viên **không** tự đổi ảnh: ảnh dùng để lễ tân đối chiếu, để hội viên tự đổi thì mất tác dụng chống mượn thẻ.
   - Đổi ảnh là ghi đè dòng cũ, ghi `uploaded_by` là người tải lên. Không có API xóa ảnh.
5. **Migration `V9_02`** (đã giữ số ở issue #110) tạo bảng và bỏ cột, làm cùng API ở việc S2-06 (Thái Hùng Lân).

Bảng `member_avatars` (bản chuẩn trong [erd-v2.dbml](../design/erd/erd-v2.dbml)):

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `id` | `bigint` | khóa chính |
| `member_id` | `bigint not null unique` | → `members.id` |
| `content` | `bytea not null` | ảnh JPEG đã thu nhỏ |
| `content_type` | `varchar(50) not null` | `CHECK content_type = 'image/jpeg'` |
| `size_bytes` | `integer not null` | `CHECK size_bytes > 0 AND size_bytes <= 102400` |
| `uploaded_by` | `bigint not null` | → `users.id`, người tải lên lần cuối |
| `created_at`, `updated_at` | `timestamptz not null` | |

## Phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Bảng riêng trong PostgreSQL (đã chọn) | Không thêm dịch vụ, không thêm khóa bí mật; dùng chung phân quyền, sao lưu với dữ liệu khác; lấy hồ sơ hội viên không phải đọc kèm ảnh | Tăng dung lượng database; phải giới hạn kích thước; ảnh đi qua backend |
| Cột `bytea` ngay trong `members` | Ít bảng hơn | Mọi truy vấn hồ sơ, danh sách hội viên dễ kéo theo ảnh; entity `Member` nặng |
| Dịch vụ lưu file bên ngoài có gói miễn phí | Không tốn dung lượng database | Thêm dịch vụ ngoài ADR 0001 và khóa bí mật; phải tự làm đường dẫn có hạn để chặn người ngoài xem ảnh; phụ thuộc điều khoản, hạn mức của bên thứ ba |
| Lưu file trên ổ đĩa Render | Đơn giản | Mất hết ảnh mỗi lần triển khai lại |

## Hệ quả

- **Dễ hơn:** không có cấu hình hay khóa mới trên Render; staging và máy cá nhân chạy giống nhau; kiểm thử bằng Testcontainers như các bảng khác.
- **Khó hơn:** frontend không gắn thẳng `<img src="/api/v1/members/{id}/avatar">` được, vì token chỉ nằm trong bộ nhớ, không tự gửi kèm. Frontend gọi API qua `services/http.ts`, nhận `Blob`, tạo `URL.createObjectURL` để hiển thị và thu hồi khi rời trang. Màn hình check-in (F4) và form hồ sơ (F2) dùng chung một component.
- **Cần theo dõi:** dung lượng Neon sau mỗi đợt dữ liệu mẫu; nếu vượt hạn mức thì viết ADR mới, không sửa ADR này.
- **Cần cập nhật theo:**
  - `erd-v2.dbml`, `thiet-ke-du-lieu.md`: thêm `member_avatars`, bỏ `members.avatar_url`, đóng câu hỏi còn mở (cùng PR với ADR này).
  - S2-06 (Thái Hùng Lân): migration `V9_02`; bỏ trường `avatarUrl` trong entity `Member` và kiểm thử `MemberApiIntegrationTest` đang đọc cột `avatar_url`; tăng `spring.servlet.multipart.max-file-size` lên 5 MB (mặc định của Spring là 1 MB); kiểm thử JUnit cho quyền xem, quyền đổi, file sai loại, file quá lớn; request Postman.
  - S2-06g (Nguyễn Anh Tú): ô tải ảnh trong form hồ sơ. S2-11 (Lê Phước Minh Quân): hiện ảnh ở màn hình check-in.
