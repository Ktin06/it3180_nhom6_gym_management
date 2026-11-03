# Frontend – React + Vite

React 19, TypeScript 6, Vite 8, Ant Design 6 (giao diện tiếng Việt), React Router 7, axios, dayjs. Phiên bản đầy đủ: [docs/decisions/0001-cong-nghe.md](../docs/decisions/0001-cong-nghe.md).

## Chạy

Yêu cầu Node 22 (`.nvmrc`; chạy `nvm use` hoặc `fnm use` trong thư mục này).

```bash
npm ci            # cài đúng phiên bản trong package-lock.json
npm run dev       # http://localhost:5173
```

| Lệnh             | Việc                                                                |
| ---------------- | ------------------------------------------------------------------- |
| `npm run dev`    | Chạy chế độ phát triển                                              |
| `npm run build`  | Kiểm tra kiểu (tsc) và build vào `dist/`                            |
| `npm run lint`   | ESLint                                                              |
| `npm run format` | Định dạng bằng Prettier (`format:check` chỉ kiểm tra)               |
| `npm test`       | Vitest ở chế độ theo dõi; `npm test -- --run` chạy một lần (như CI) |

Frontend gọi API theo đường dẫn cùng domain `/api/v1` ([ADR 0008](../docs/decisions/0008-goi-api-cung-domain.md)): khi chạy `npm run dev`, Vite chuyển tiếp `/api` sang `http://localhost:8080`; khi deploy, `vercel.json` chuyển tiếp sang backend trên Render. Không cần khai báo biến môi trường trên Vercel; Root Directory đặt là `frontend`. Sau khi tạo dịch vụ Render, thay địa chỉ giả `https://thay-bang-dia-chi-render.invalid` trong `vercel.json` bằng địa chỉ thật.

Biến môi trường (nếu có) đọc từ file `.env` ở **thư mục gốc repo** (`envDir: '..'`). `VITE_API_URL` chỉ dùng khi muốn gọi thẳng một backend khác.

## Xác thực F1

Frontend có trang đăng ký và đăng nhập; access token chỉ giữ trong bộ nhớ tab, refresh token do backend đặt vào cookie `HttpOnly`. Adapter hiện dùng DTO tạm vì backend F1 chưa có trên `main`; xem [ghi chú tích hợp](../docs/guides/tich-hop-frontend-f1.md) trước khi nối API thật. Vitest mock không thay thế kiểm tra backend hoặc staging.

## Quy ước thư mục

```
src/
├── app/          App.tsx, router.tsx (khai báo route), providers.tsx (ConfigProvider antd vi_VN, dayjs vi)
├── layouts/      PublicLayout (khách), AuthLayout (đăng nhập), StaffLayout (menu trái theo vai trò,
│                 ngăn kéo trên màn hình hẹp), MemberLayout (ưu tiên điện thoại)
├── routes/       ProtectedRoute: chưa đăng nhập → /login, sai vai trò → /403
├── features/     mỗi chức năng một thư mục (xem features/README.md)
├── pages/        trang không thuộc chức năng nào: 403, 404, trang chủ tạm theo vai trò
├── services/     http.ts – axios dùng chung
├── auth/         vai trò (Role), AuthContext/AuthProvider, useAuth, session-store
├── components/   component dùng chung nhiều chức năng
├── hooks/        hook dùng chung
├── utils/        hàm tiện ích (định dạng tiền VND, ngày giờ...)
├── types/        kiểu dùng chung, khai báo biến môi trường
└── test/         cấu hình chung cho Vitest
```

- Component và trang: `PascalCase.tsx`. Hook: `useTenHook.ts`. Module khác: chữ thường nối gạch ngang (`staff-menu.tsx`, `http.ts`).
- File kiểm thử đặt cạnh file được kiểm thử: `ProtectedRoute.test.tsx`.
- Giao diện chỉ tiếng Việt; dùng tốt từ màn hình rộng 360px.
- Phân quyền ở giao diện chỉ để trải nghiệm; backend vẫn kiểm tra vai trò và quyền sở hữu dữ liệu.
- Không tự tính giá hay số tiền ở frontend để gửi lên; luôn hiển thị số liệu server trả về.
