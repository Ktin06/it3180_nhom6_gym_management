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

Frontend gọi API theo đường dẫn cùng domain `/api/v1` ([ADR 0008](../docs/decisions/0008-goi-api-cung-domain.md)): khi chạy `npm run dev`, Vite chuyển tiếp `/api` sang `http://localhost:8080`; khi deploy, `vercel.json` chuyển tiếp sang `https://it3180-nhom6-gym-management.onrender.com`. Không cần khai báo biến môi trường trên Vercel; Root Directory đặt là `frontend`. Cấu hình đã có địa chỉ thật, nhưng cần kiểm tra kết nối trên môi trường triển khai.

Biến môi trường (nếu có) đọc từ file `.env` ở **thư mục gốc repo** (`envDir: '..'`). `VITE_API_URL` chỉ dùng khi muốn gọi thẳng một backend khác.

## Xác thực F1

Frontend có trang đăng ký và đăng nhập; access token chỉ giữ trong bộ nhớ tab, refresh token do backend đặt vào cookie `HttpOnly`. Adapter dùng hợp đồng JSON F1 mục 6.1 (`/me` gồm `userId`, `email`, `role`). Nếu refresh trả token của tài khoản khác, tab kết thúc phiên và yêu cầu đăng nhập lại để tránh hiển thị người A nhưng gửi thao tác bằng người B. Xem [ghi chú tích hợp](../docs/guides/tich-hop-frontend-f1.md) cho hợp đồng, cách chạy lại và bằng chứng kiểm chứng. Vitest không thay thế kiểm tra backend hoặc staging.

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

## Giao diện hội viên và danh mục gói (#27)

- `/members`: danh sách, tìm theo mã/SĐT/đầu họ tên, lọc trạng thái, phân trang, thêm/sửa hồ sơ và trạng thái; không có ảnh, cấp tài khoản hoặc chỉ số cơ thể.
- `/packages`: danh mục, lọc TIME/SESSION/PT và đang bán/ngừng bán, phân trang, giá VND và mô tả. Phần này là tra cứu danh mục; chưa triển khai form quản lý gói hay bán/gia hạn. Wireframe #18 có phạm vi rộng hơn đầu việc màn hình danh mục của #27.
- Hai route và menu dành cho `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`, khớp API F2 đã merge ở #97. TRAINER không được tra cứu chung; quyền MEMBER xem hồ sơ của chính mình qua API không mở quyền vào màn hình quản lý này. Backend kiểm tra quyền/chi nhánh.

Để xem dữ liệu minh họa F2/F3, chạy trong PowerShell tại `frontend/`:

```powershell
$env:VITE_F2_F3_MOCK = 'true'
npm run dev
```

Đăng nhập bằng tài khoản thật có vai trò Lễ tân, Quản lý hoặc Chủ chuỗi, rồi mở Hội viên/Gói tập từ menu. Backend auth vẫn phải chạy: cờ này chỉ thay dữ liệu F2/F3, không giả lập phiên đăng nhập hoặc cấp JWT. Dữ liệu minh họa chỉ ở bộ nhớ của trang; tải lại trang thì mất thay đổi. Banner thông báo rõ chế độ này. Cờ trên chỉ có hiệu lực trong dev, bị bỏ qua trong build production; lỗi API thật không tự chuyển sang dữ liệu minh họa.

API client trong `src/features/members/api/members.ts` và `packages/api/packages.ts` dùng `services/http.ts` của F1 để gắn access token và phục hồi phiên. Trang frontend đánh số từ 1, request API đánh số từ 0. DTO F2 khớp API #97: form tạo chỉ gửi `fullName`, `phone`, `gender`, `dob`; form sửa thêm `status`, đọc chi tiết trước khi sửa. Mã hội viên/email/chi nhánh do server quản lý. DTO danh mục F3 và bộ lọc `type`, `status`, `page`, `size` khớp `PackageSummaryDto`/`PackageController` của #107. Không truyền branchId từ bộ lọc UI.

Schema #101, API F2 #97, API F3 #107 và frontend auth #80 đã merge và được đồng bộ từ main `2262010`. Để dùng API thật, bỏ cờ mock (hoặc đặt `VITE_F2_F3_MOCK=false`), khởi động lại dev server, đăng nhập bằng tài khoản thật rồi kiểm tra thêm/sửa/tìm kiếm/phân quyền với backend. Xem [kết quả tích hợp F2/F3 và cách chạy lại](../docs/guides/tich-hop-frontend-f2-f3.md). Kiểm tra cục bộ không thay thế nghiệm thu HTTPS staging.

### Đối chiếu hợp đồng F2 ngày 10/11/2026

Nguồn: `MemberController`, bốn DTO trong `feature/member/dto`, `PageResponse`, các lớp xử lý lỗi và `PhoneUtils` tại commit `872386d`. Truy vết yêu cầu: [US-F2.1 #52](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/52), task #27.

| Phần          | Kết quả                                                                                                                                                                     |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Endpoint      | Khớp GET/POST `/api/v1/members`, GET/PUT `/api/v1/members/{id}`; tạo trả 201, sửa trả 200, cùng trả chi tiết hồ sơ.                                                         |
| Phân trang    | Khớp `keyword`, `status`, `page` từ 0, `size` 1–100; phản hồi trực tiếp `content`, `page`, `size`, `totalElements`, `totalPages`.                                           |
| DTO           | Tên trường, enum và các trường nullable khớp. `gender`/`dob` trống gửi `null`, kể cả khi xóa giá trị cũ bằng PUT.                                                           |
| Số điện thoại | Form đã bổ sung dạng `84…` đầy đủ theo backend, bên cạnh `0…` và `+84…`. Mock hỗ trợ tìm tiền tố `0900`; tiền tố quốc tế chưa đầy đủ như `84900` không tự đổi thành `0900`. |
| Ngày sinh     | Tùy chọn, nếu có phải trong quá khứ (`@Past`); backend kiểm tra và form hiển thị `errors.dob`.                                                                              |
| Lỗi           | Khớp `ProblemDetail.detail` và `errors` dạng tên trường → thông báo cho 400; 409 giữ dữ liệu form và hiển thị lỗi; hồ sơ ngoài phạm vi trả 404.                             |

Đây là đối chiếu mã nguồn và kiểm thử frontend; chưa chứng minh HTTP/cookie, quyền chi nhánh hoặc lưu bền trên backend thật.

Kiểm tra: `npm run lint`, `npm test -- --run`, `npm run build`. Vitest kiểm tra DTO, tìm kiếm/phân trang, lỗi trùng SĐT/validation, retry, phản hồi cũ và lọc gói. Khi duyệt giao diện, xem thêm ở 360px: bảng cuộn ngang trong vùng riêng và form cuộn theo chiều dọc.

### Đề xuất bổ sung: gói tập của từng hội viên

Ghi nhận ngày 06/11/2026 khi xem giao diện #27: cần cân nhắc phần **“Gói tập của hội viên”** trong trang chi tiết hồ sơ, để nhân viên biết hội viên đã mua và đang dùng gói nào. Đây là đề xuất cần nhóm xác nhận phạm vi và API; chưa nằm trong bản giao diện hiện tại.

- Dữ liệu nên có: tên và loại gói (TIME/SESSION/PT), ngày bắt đầu/hết hạn, trạng thái gói, tổng/số buổi còn lại với SESSION/PT. Nếu có nhiều gói thì hiển thị riêng từng gói; không mặc định mỗi hội viên chỉ có một gói.
- Phân biệt `packages` (danh mục và giá đang bán) với `memberships` (gói cụ thể hội viên đã mua). Không suy ra gói đã mua từ danh mục hoặc dùng trạng thái hồ sơ ACTIVE để kết luận gói còn hiệu lực. Phần xem này tách khỏi thao tác bán/gia hạn và xác nhận thanh toán.
- Backend cần cung cấp dữ liệu gắn đúng `memberId`, kiểm tra quyền và chi nhánh, trả trạng thái/ngày/số buổi từ server. Hợp đồng F2 đã merge ở commit `872386d` chưa chứa danh sách gói; nhóm cần chốt API tra cứu gói phù hợp, không tự đặt endpoint hoặc thêm trường vào DTO của Lân.
- Khi triển khai: phối hợp Lân và người phụ trách nghiệp vụ gói để thống nhất hợp đồng, cập nhật đặc tả/wireframe, rồi thêm phần xem và kiểm thử. Cần kiểm tra các trường hợp chưa mua gói, có TIME cùng PT, gói UPCOMING/hết hạn, và truy cập hồ sơ ngoài quyền.

Ghi chú này chưa được gửi thành issue/comment trên GitHub và chưa xác nhận mốc triển khai; dùng làm đầu việc cần bàn giao khi nhóm chốt phần bổ sung.
