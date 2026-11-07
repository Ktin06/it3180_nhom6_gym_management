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

## Đăng nhập giả lập

Ở chế độ phát triển (`npm run dev`) có ô **"Giả lập vai trò"** trên trang đăng nhập và thanh tiêu đề, để thử menu và phân quyền giao diện theo 5 vai trò. Ô này không có trong bản build. Sẽ thay bằng đăng nhập thật ở F1.

## Quy ước thư mục

```
src/
├── app/          App.tsx, router.tsx (khai báo route), providers.tsx (ConfigProvider antd vi_VN, dayjs vi)
├── layouts/      PublicLayout (khách), AuthLayout (đăng nhập), StaffLayout (menu trái theo vai trò,
│                 ngăn kéo trên màn hình hẹp), MemberLayout (ưu tiên điện thoại)
├── routes/       ProtectedRoute: chưa đăng nhập → /login, sai vai trò → /403
├── features/     mỗi chức năng một thư mục (xem features/README.md)
├── pages/        trang không thuộc chức năng nào: đăng nhập, 403, 404, trang chủ tạm theo vai trò
├── services/     http.ts – axios dùng chung
├── auth/         vai trò (Role), AuthContext, useAuth, DevRoleSwitcher
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
- Hai route và menu dành cho `SUPER_ADMIN`, `BRANCH_MANAGER`, `RECEPTIONIST`. F2 tạm chưa mở cho TRAINER theo API đang review của Lân; backend vẫn phải kiểm tra quyền/chi nhánh.

Để xem thử khi backend chưa sẵn sàng, chạy trong PowerShell tại `frontend/`:

```powershell
$env:VITE_F2_F3_MOCK = 'true'
npm run dev
```

Ở trang đăng nhập chọn vai trò giả lập Lễ tân hoặc Quản lý, rồi mở Hội viên/Gói tập từ menu. Dữ liệu minh họa chỉ ở bộ nhớ của trang; tải lại trang thì mất thay đổi. Banner thông báo rõ chế độ này. Cờ trên chỉ có hiệu lực trong dev, bị bỏ qua trong build production; lỗi API thật không tự chuyển sang dữ liệu minh họa.

API client trong `src/features/members/api/members.ts` và `packages/api/packages.ts` dùng `services/http.ts`. Trang frontend đánh số từ 1, request API đánh số từ 0. DTO F2 đối chiếu [PR #97, commit 37bc98a](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97): form tạo chỉ gửi `fullName`, `phone`, `gender`, `dob`; form sửa thêm `status`, đọc chi tiết trước khi sửa. Mã hội viên/email/chi nhánh do server quản lý. F3 theo [đặc tả](../docs/requirements/srs/dac-ta-use-case-f3-goi-tap-va-gia-han.md), còn cần Lân xác nhận hợp đồng #29. Không truyền branchId từ bộ lọc UI.

**Chưa nghiệm thu nối API thật của #27:** cần phần frontend đăng nhập và gắn token vào HTTP của F1 (#80), API F2 hoàn thiện phần kết nối tài khoản/chi nhánh và được merge/triển khai, API F3 #29 có sẵn, proxy Vercel trỏ đúng backend. Bản main hiện vẫn có AuthProvider giả lập và HTTP chưa gắn token; chọn vai trò giả lập không cấp JWT thật. Để dùng API thật, bỏ cờ mock và khởi động lại dev server sau khi các phụ thuộc được hoàn tất, rồi kiểm tra lại thêm/sửa/tìm kiếm/phân quyền với backend.

Kiểm tra: `npm run lint`, `npm test -- --run`, `npm run build`. Vitest kiểm tra DTO, tìm kiếm/phân trang, lỗi trùng SĐT/validation, retry, phản hồi cũ và lọc gói. Khi duyệt giao diện, xem thêm ở 360px: bảng cuộn ngang trong vùng riêng và form cuộn theo chiều dọc.

### Đề xuất bổ sung: gói tập của từng hội viên

Ghi nhận ngày 06/11/2026 khi xem giao diện #27: cần cân nhắc phần **“Gói tập của hội viên”** trong trang chi tiết hồ sơ, để nhân viên biết hội viên đã mua và đang dùng gói nào. Đây là đề xuất cần nhóm xác nhận phạm vi và API; chưa nằm trong bản giao diện hiện tại.

- Dữ liệu nên có: tên và loại gói (TIME/SESSION/PT), ngày bắt đầu/hết hạn, trạng thái gói, tổng/số buổi còn lại với SESSION/PT. Nếu có nhiều gói thì hiển thị riêng từng gói; không mặc định mỗi hội viên chỉ có một gói.
- Phân biệt `packages` (danh mục và giá đang bán) với `memberships` (gói cụ thể hội viên đã mua). Không suy ra gói đã mua từ danh mục hoặc dùng trạng thái hồ sơ ACTIVE để kết luận gói còn hiệu lực. Phần xem này tách khỏi thao tác bán/gia hạn và xác nhận thanh toán.
- Backend cần cung cấp dữ liệu gắn đúng `memberId`, kiểm tra quyền và chi nhánh, trả trạng thái/ngày/số buổi từ server. Hợp đồng F2 được đối chiếu ở commit `37bc98a` chưa chứa danh sách gói; nhóm cần chốt API tra cứu gói phù hợp, không tự đặt endpoint hoặc thêm trường vào DTO của Lân.
- Khi triển khai: phối hợp Lân và người phụ trách nghiệp vụ gói để thống nhất hợp đồng, cập nhật đặc tả/wireframe, rồi thêm phần xem và kiểm thử. Cần kiểm tra các trường hợp chưa mua gói, có TIME cùng PT, gói UPCOMING/hết hạn, và truy cập hồ sơ ngoài quyền.

Ghi chú này chưa được gửi thành issue/comment trên GitHub và chưa xác nhận mốc triển khai; dùng làm đầu việc cần bàn giao khi nhóm chốt phần bổ sung.
