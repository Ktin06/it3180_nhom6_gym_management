# ADR 0007: Cấu trúc mã nguồn – đóng gói theo chức năng

- **Ngày:** 01/11/2026
- **Người quyết định:** Hoàng Kim Tiến (trưởng nhóm), cần cả nhóm xác nhận
- **Trạng thái:** Đã chấp nhận

## Bối cảnh

Mỗi thành viên phụ trách trọn một số chức năng (F1–F20) từ cơ sở dữ liệu đến giao diện. Báo cáo cần sơ đồ lớp phân tầng (controller – service – repository) khớp với mã nguồn, vì đó là lý do chọn Spring Boot ([ADR 0001](0001-cong-nghe.md)).

## Quyết định

**Backend:** đóng gói theo chức năng, bên trong chia theo tầng.

```
vn.edu.hust.nhom6.gym
├── config/      cấu hình chung (bảo mật, OpenAPI, auditing)
├── common/      BaseEntity, xử lý lỗi, PageResponse
└── feature/
    └── <chức năng>/   ví dụ member (F2), checkin (F4)
        ├── controller/
        ├── service/
        ├── repository/
        ├── entity/
        ├── dto/
        └── mapper/
```

**Frontend:** tương tự, `src/features/<chức năng>/` gồm `api/`, `components/`, `pages/`, `types/`; phần dùng chung nằm ở `layouts/`, `routes/`, `services/`, `auth/`, `components/`.

Quy tắc: một chức năng chỉ gọi chức năng khác qua service (backend) hoặc qua hàm trong `api/` (frontend), không gọi thẳng repository của chức năng khác.

## Phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Theo chức năng, trong chia tầng (đã chọn) | Mỗi người sở hữu trọn một thư mục, ít xung đột khi gộp; vẫn khớp sơ đồ lớp phân tầng | Phải giữ kỷ luật không gọi chéo repository |
| Theo tầng toàn cục (`controller/`, `service/`, `repository/` ở gốc) | Quen thuộc, giống ví dụ trên mạng | Mọi người cùng sửa một thư mục, xung đột nhiều; khó thấy ranh giới chức năng |
| Mô-đun Maven riêng cho từng chức năng | Ranh giới cứng | Quá phức tạp cho dự án học phần |

## Hệ quả

- Sơ đồ lớp trong báo cáo vẽ theo từng chức năng, mỗi sơ đồ có đủ ba tầng.
- Người duyệt pull request dễ thấy thay đổi nằm ngoài phạm vi chức năng.
- File `feature/package-info.java` và `frontend/src/features/README.md` mô tả quy ước này cho người mới và cho công cụ AI hỗ trợ lập trình.
