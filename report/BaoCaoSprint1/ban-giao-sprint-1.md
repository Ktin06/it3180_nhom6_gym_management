# Bàn giao Sprint 1 — Web quản lý chuỗi phòng tập gym

**Nhóm 6 · IT3180 · Giảng viên hướng dẫn: TS. Bùi Thị Mai Anh**
Sprint 1: 27/10 – 16/11/2026 · Demo: 17/11/2026 · Số liệu lấy từ GitHub ngày 16/11/2026, `main` tại `d68fdbd`.

Tài liệu này tóm tắt những gì nhóm làm được trong Sprint 1. Mỗi ý đều có liên kết tới bằng chứng trên repo. Slide trình bày: [SlideBaoCaoSprint1.tex](SlideBaoCaoSprint1.tex).

## Tóm tắt

- **Hoàn thành 35/37 issue (94%)** của milestone Sprint 1. Hai issue còn mở được chuyển sang Sprint 2 (mục 6).
- **Phân tích, thiết kế xong cho v1–v2 (F1–F10):** SRS, 31 user story, 10 đặc tả use case, ERD 39 bảng, sơ đồ UML, 13 wireframe, 11 ADR (file ghi lại quyết định kiến trúc kèm lý do).
- **Đã có sản phẩm chạy được:** database đủ 39 bảng; F1 (tài khoản, phân quyền), F2 (hồ sơ hội viên), F3 (danh mục gói tập) có cả API và giao diện.
- **Đã triển khai lên staging** (môi trường chạy thật trên Internet để thử trước khi bàn giao): Vercel – Render – Neon.
- **Quy trình làm việc chặt chẽ:** 43 pull request đã gộp. Mọi thay đổi phải qua review và CI (build, kiểm thử tự động) trước khi vào `main`.

## 1. Đề tài và mục tiêu Sprint 1

Hệ thống quản lý **chuỗi** phòng tập gym: 20 chức năng (F1–F20) chia 5 version, 6 vai trò (chủ chuỗi, quản lý chi nhánh, lễ tân, huấn luyện viên, hội viên, khách). Công nghệ: React + Ant Design, Spring Boot (Java 21), PostgreSQL. Phạm vi chi tiết ở [bản chốt phạm vi](../../docs/requirements/ban-chot-pham-vi.md).

| Version | Chức năng | Cam kết |
| --- | --- | --- |
| v1 | F1–F6: tài khoản, hội viên, gói tập, check-in, thanh toán tại quầy, dashboard | Bắt buộc |
| v2 | F7–F10: cổng hội viên, lớp nhóm, huấn luyện viên, thông báo | Bắt buộc |
| v3 | F11–F17: thiết bị, bán hàng/kho, nhân sự/lương, khuyến mãi, bảo lưu, báo cáo, nhật ký | Mục tiêu |
| v4 | F18–F19: đa chi nhánh, gamification | Cắt thứ hai nếu trễ |
| v5 | F20: nhận diện khuôn mặt tại quầy | Cắt đầu tiên nếu trễ |

**Mục tiêu Sprint 1:** phân tích yêu cầu F1–F10; thiết kế dữ liệu và giao diện; dựng khung dự án, quy trình, CI; làm sớm phần lõi v1 là F1, F2, F3; chạy được trên staging.

## 2. Kết quả so với mục tiêu

| Mục tiêu | Kết quả | Bằng chứng |
| --- | --- | --- |
| Phân tích yêu cầu | SRS tổng quan; 31 user story F1–F10 có tiêu chí chấp nhận Given/When/Then; 10 đặc tả use case theo một mẫu; bảng truy vết yêu cầu → use case → bảng dữ liệu | [SRS](../../docs/requirements/srs/), [user story](../../docs/requirements/user-stories-f1-f10.md), [bảng truy vết](../../docs/requirements/srs/bang-truy-vet-yeu-cau.md) |
| Thiết kế dữ liệu | ERD v2 gồm 39 bảng, 8 nhóm, cả nhóm duyệt ngày 02/11; từ điển dữ liệu 422 cột | [erd-v2.dbml](../../docs/design/erd/erd-v2.dbml) (bản chuẩn), [hình ERD](../../docs/design/erd/xuat-ban-erd.md), [từ điển](../../docs/design/erd/tu-dien-du-lieu.md) |
| Thiết kế UML, giao diện | 7 sơ đồ use case, 3 sơ đồ hoạt động, 13 wireframe, quy ước giao diện | [use case](../../docs/design/use-case/), [activity](../../docs/design/activity/), [wireframe](../../docs/design/wireframe/) |
| Khung dự án, quy trình | Backend chia theo chức năng, frontend chia layout theo vai trò; CI; bảo vệ nhánh `main`; 11 ADR | [quyết định kiến trúc](../../docs/decisions/), [CONTRIBUTING](../../CONTRIBUTING.md) |
| Phần lõi v1 | F1, F2, F3 có API và giao diện, đã gộp vào `main` | Mục 3 |
| Staging | Frontend, backend, database chạy trên Internet | Mục 3 |

## 3. Sản phẩm chạy được

| Phần | Đã làm | PR |
| --- | --- | --- |
| Database | 41 migration (file SQL đánh số, Flyway chạy lần lượt để dựng schema) tạo đủ 39 bảng và ràng buộc; 5 script dữ liệu mẫu cho môi trường local và staging | [#101](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/101), [#109](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/109) |
| F1 – tài khoản | Đăng ký, đăng nhập bằng email, làm mới phiên, đăng xuất; phân quyền theo vai trò ở backend; giao diện và menu riêng từng vai trò | [#87](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/87), [#91](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/91), [#105](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/105), [#80](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80), [#114](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/114) |
| F2 – hồ sơ hội viên | Thêm, sửa, xem, tìm kiếm, phân trang hồ sơ | API [#97](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/97), giao diện [#103](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/103) |
| F3 – danh mục gói | Thêm, sửa, ngừng bán gói tập | API [#107](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/107), giao diện [#103](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/103) |

**Staging:** [frontend trên Vercel](https://it3180-nhom6-gym.vercel.app) → [backend trên Render](https://it3180-nhom6-gym-management.onrender.com) → PostgreSQL trên Neon. Trình duyệt gọi `/api/v1` cùng tên miền với frontend, Vercel chuyển tiếp sang Render ([ADR 0008](../../docs/decisions/0008-goi-api-cung-domain.md)). Render bản miễn phí "ngủ" khi không có truy cập, nên lần gọi đầu có thể chậm khoảng một phút; đó không phải lỗi.

**Kịch bản demo (2–3 phút):**

1. Đăng nhập bằng tài khoản lễ tân.
2. Tìm, thêm hồ sơ hội viên (F2).
3. Xem, thêm gói trong danh mục gói tập (F3).
4. Đăng nhập bằng tài khoản hội viên: bị chặn khỏi trang quản lý (phân quyền F1).

## 4. Chất lượng và kiểm thử

- **CI** (GitHub Actions tự build và chạy kiểm thử mỗi khi mở hoặc cập nhật PR): đã chạy 189 lần. CI đỏ thì không gộp được PR.
- **Backend:** kiểm thử JUnit trong 24 lớp. Testcontainers dựng PostgreSQL thật để kiểm tra ràng buộc database, ví dụ không cho trùng email hay số điện thoại.
- **Frontend:** kiểm thử Vitest trong 19 file, kèm lint và build.
- **Kiểm thử API trên staging bằng Postman, ngày 12/11:**

| Chức năng | Số request | Số kiểm tra đạt |
| --- | --- | --- |
| F1 – tài khoản | 6 | 9/11 |
| F2 – hồ sơ hội viên | 20 | 25/26 |
| F3 – danh mục gói | 34 | 31/34 |

Các bước không đạt đều do dữ liệu thử trên staging, ví dụ tài khoản thử đã tồn tại hoặc thiếu chi nhánh thứ hai, không phải lỗi API. Nguồn: [issue #31 (staging và Postman)](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/31).

- **Thử đăng nhập qua HTTPS trên staging, ngày 12/11: 15/15 ca đạt.** Gồm đăng ký, đăng nhập, tải lại trang vẫn giữ phiên, đăng xuất, giao diện ở màn hình rộng 360 px. Cookie lưu phiên có `HttpOnly` và `Secure`, nên mã JavaScript không đọc được và cookie chỉ gửi qua HTTPS. Nguồn: [báo cáo trong PR #80](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/80#issuecomment-6828579075).

**Giới hạn:** chưa nghiệm thu toàn bộ yêu cầu phi chức năng (NF1–NF6, S1–S10). Kết quả Postman mới chạy một lần, chưa có người thứ hai chạy lại.

## 5. Cách nhóm làm việc

- **Agile, chia sprint.** Mỗi sprint là một milestone trên GitHub có hạn chót. Kế hoạch gồm 49 đầu việc WBS (bảng chia nhỏ công việc), tổng 136 story point (điểm ước lượng độ khó). Bảy thành viên mỗi người 15–16 điểm ([kế hoạch Sprint 1](../../docs/plans/sprint-1-tien-do.md)).
- **Mỗi việc là một issue** ghi rõ người làm, hạn, kết quả mong đợi. Bảng Kanban trên [GitHub Projects](https://github.com/users/Ktin06/projects/1) tự chuyển thẻ khi mở hoặc gộp PR.
- **Không ai đẩy thẳng lên `main`.** Quy trình: issue → nhánh → pull request → CI → review, Approve → squash merge (gom cả PR thành một commit). Trong Sprint 1 có 68 lượt review, 48 lượt Approve.
- **Quyết định được ghi lại.** Ví dụ: Phạm Gia Bảo nêu 14 câu hỏi nghiệp vụ khi đặc tả F8 (lớp nhóm). Trưởng nhóm chốt từng câu trong [PR #79](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/79), ghi thành [ADR 0010](../../docs/decisions/0010-danh-sach-cho-tu-dong-dua-vao-lop-ban-2.md), rồi đồng bộ tài liệu và migration ở PR #109.
- **Rút kinh nghiệm:** migration trễ hạn 04/11 làm API và giao diện phải chờ, nên nhóm dời hạn Sprint 1 từ 09/11 sang 16/11. Sang Sprint 2, mỗi người làm trọn một cụm chức năng (API → giao diện → kiểm thử) và họp giữa sprint để phát hiện trễ sớm ([biên bản tổng kết](../../docs/meetings/2026-11-10-tong-ket-sprint-1.md)).

## 6. Việc còn mở và Sprint 2

| Việc | Trạng thái | Xử lý |
| --- | --- | --- |
| [#32](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/32) — slide và gói bàn giao Sprint 1 | Đang hoàn tất trong PR #81 | Đóng sau buổi demo 17/11 |
| [#55](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/55) — nghiệm thu user story F3 | API và giao diện đã gộp, chưa nghiệm thu theo tiêu chí chấp nhận | Chuyển Sprint 2 |
| [#104](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/104) — lỗi refresh token dùng được 2 lần khi gửi đồng thời | Đang sửa ở [PR #116](https://github.com/Ktin06/it3180_nhom6_gym_management/pull/116) | Chuyển Sprint 2 |

**Sprint 2 (17/11 – 30/11, demo 01/12):** hoàn thiện v1 (F1–F6) và làm v2 (F7–F10). Đã có sẵn 32 issue trong milestone Sprint 2. Quyết định cho ảnh đại diện ([ADR 0011](../../docs/decisions/0011-anh-dai-dien-luu-trong-postgresql.md)) và danh sách chờ lớp nhóm (ADR 0010) đã chốt, nên làm được ngay. Phân công chi tiết ở [kế hoạch Sprint 2](../../docs/plans/sprint-2-ke-hoach.md).

## 7. Thành viên và phân công Sprint 1

| Thành viên | MSSV | Phụ trách | Điểm |
| --- | --- | --- | --- |
| Hoàng Kim Tiến (trưởng nhóm) | 202416367 | Khung dự án, ERD, quy trình; điều phối, duyệt PR | 27 |
| Bùi Trần Đức Việt | 202416402 | F1 backend: đăng nhập, phân quyền; migration nhóm 1–2; kiểm thử CSDL | 16 |
| Thái Hùng Lân | 202416261 | API F2, F3; đặc tả F3, F5; wireframe hóa đơn | 15 |
| Lê Phước Minh Quân | 202416324 | Đặc tả F4, F6; sơ đồ use case, hoạt động; migration nhóm 4 | 15 |
| Nguyễn Trọng Việt | 202416399 | Đặc tả F7, F9, F10; migration nhóm 5, 7; wireframe; kế hoạch Sprint 2 | 16 |
| Nguyễn Phú Trọng | 202400075 | Gộp schema 39 bảng, dữ liệu mẫu, triển khai staging, chạy Postman | 16 |
| Nguyễn Anh Tú | 202400078 | SRS, bảng truy vết, từ điển dữ liệu; giao diện F2, F3 | 15 |
| Phạm Gia Bảo | 202416425 | User story F1–F10; đặc tả F8; giao diện F1; slide bàn giao | 16 |

Trưởng nhóm có điểm cao hơn vì đã dựng khung dự án trước khi chia việc.

## 8. Biên dịch slide

Cần XeLaTeX (MiKTeX hoặc TeX Live) và font Segoe UI, Consolas (có sẵn trên Windows). Slide dùng theme HUST chung ở [report/mau-slide-hust/](../mau-slide-hust/), ảnh chụp màn hình ở [anh-slide/](anh-slide/).

```bash
cd report/BaoCaoSprint1
xelatex SlideBaoCaoSprint1.tex   # chạy 2 lần để cập nhật số trang
xelatex SlideBaoCaoSprint1.tex
```

File PDF và file phụ do LaTeX sinh ra không đưa lên Git ([report/.gitignore](../.gitignore)). PDF cuối do trưởng nhóm đính kèm lên GitHub Release.
