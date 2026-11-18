## Liên kết

<!-- Closes cho issue task (tự đóng khi gộp). Refs cho issue user story (không tự đóng; đóng bằng tay sau nghiệm thu). Mỗi issue một dòng. -->
Closes #
Refs #

**Mã chức năng:** F
**AC đã làm:** <!-- ví dụ: AC-F3.3.1-01, AC-F3.3.1-02; PR không thuộc user story thì ghi "không có" -->

## Mô tả

<!-- Thay đổi gì, vì sao. Một việc một pull request. -->

## Cách kiểm thử

<!-- Các bước để người duyệt tự kiểm tra: lệnh chạy, request Postman, màn hình cần mở, dữ liệu mẫu. -->

1.
2.

## Checklist (định nghĩa hoàn thành)

- [ ] Các AC ghi ở mục "AC đã làm" có trong file SRS và thay đổi này đáp ứng đủ
- [ ] Có kiểm thử JUnit cho mỗi quy tắc nghiệp vụ mới hoặc thay đổi
- [ ] Kiểm thử mới mang mã TC của AC tương ứng ([docs/testing/README.md](https://github.com/Ktin06/it3180_nhom6_gym_management/blob/main/docs/testing/README.md)); hành vi chưa có AC thì đã thêm AC vào SRS
- [ ] Có request Postman cho mỗi API mới (trong `postman/`)
- [ ] CI xanh
- [ ] Tài liệu đã cập nhật (SRS, ADR, README, Swagger mô tả API) nếu cần
- [ ] Không sửa migration đã gộp vào `main`; schema mới nằm trong migration mới
- [ ] Không commit khóa bí mật, không có `.env`
- [ ] Không thêm tính năng ngoài bản chốt phạm vi
- [ ] Đã tự đọc lại toàn bộ thay đổi (kể cả phần do công cụ AI sinh) và giải thích được
- [ ] Sau khi gộp: kiểm tra chạy được trên staging
