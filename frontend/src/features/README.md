# features/

Mỗi chức năng một thư mục, đặt tên theo nghiệp vụ, chữ thường nối gạch ngang (ví dụ `auth` cho F1, `member` cho F2, `checkin` cho F4, `payment` cho F5). Xem [ADR 0007](../../../docs/decisions/0007-cau-truc-ma-nguon.md).

```
features/member/
├── api/          hàm gọi API của chức năng, dùng services/http.ts (ví dụ member-api.ts)
├── components/   component chỉ dùng trong chức năng này
├── pages/        trang (màn hình) – được khai báo route trong app/router.tsx
└── types/        kiểu dữ liệu request/response, khớp DTO của backend
```

Quy tắc:

- Chức năng khác cần dữ liệu của chức năng này thì gọi qua hàm trong `api/`, không nhân bản kiểu hay lời gọi API.
- Component dùng chung từ hai chức năng trở lên thì chuyển lên `src/components/`.
- Khi chức năng xong, bật mục menu tương ứng (`ready: true` trong `layouts/staff-menu.tsx` hoặc `layouts/MemberLayout.tsx`).
