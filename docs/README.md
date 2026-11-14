# Tài liệu dự án

Mục lục thư mục `docs/`. Khi các tài liệu mâu thuẫn nhau, thứ tự ưu tiên là: bản chốt phạm vi (nghiệp vụ) → `erd-v2.dbml` (dữ liệu) → ADR (quyết định) → các tài liệu khác.

## Yêu cầu – [requirements/](requirements/)

| Tài liệu | Nội dung |
| --- | --- |
| [ban-chot-pham-vi.md](requirements/ban-chot-pham-vi.md) | **Bản chuẩn** về phạm vi, 20 chức năng, 5 version, quy tắc nghiệp vụ, yêu cầu phi chức năng |
| [boi-canh-de-tai.md](requirements/boi-canh-de-tai.md) | Tóm tắt đề tài, thành viên, các quyết định chính |
| [srs/srs-tong-quan.md](requirements/srs/srs-tong-quan.md) | SRS tổng quan: bối cảnh và quy trình hiện tại, phạm vi, 6 vai trò, NF1–NF6, S1–S10 và nguồn tham khảo |
| [srs/](requirements/srs/) | Đặc tả use case chi tiết theo chức năng và mẫu dùng chung |
| [Bảng truy vết yêu cầu](requirements/srs/bang-truy-vet-yeu-cau.md) | Liên kết F1–F20, use case và 39 bảng ERD; ghi nhận nguồn nháp và các điểm cần đồng bộ |

## Thiết kế – [design/](design/)

| Tài liệu | Nội dung |
| --- | --- |
| [erd/erd-v2.dbml](design/erd/erd-v2.dbml) | **Bản chuẩn** của ERD (40 bảng, 8 nhóm); dán vào dbdiagram.io để xem sơ đồ |
| [erd/thiet-ke-du-lieu.md](design/erd/thiet-ke-du-lieu.md) | Giải thích thiết kế dữ liệu, quy ước, ràng buộc cần kiểm thử |
| [erd/tu-dien-du-lieu.md](design/erd/tu-dien-du-lieu.md) | Từ điển đủ 39 bảng, 422 cột, khóa, chỉ mục và ghi chú từ DBML |
| [erd/xuat-ban-erd.md](design/erd/xuat-ban-erd.md) | Bản xuất dbdiagram.io PDF/PNG và hình theo nhóm; cách tái tạo và đưa vào báo cáo LaTeX |
| [use-case/](design/use-case/) | Sơ đồ use case (sẽ bổ sung) |
| [activity/](design/activity/) | Sơ đồ hoạt động (sẽ bổ sung) |
| [wireframe/](design/wireframe/) | Wireframe màn hình (sẽ bổ sung) |

Sơ đồ UML và wireframe vẽ bằng draw.io (trong VS Code dùng tiện ích *Draw.io Integration*), lưu dạng `.drawio.svg` trong thư mục tương ứng, tên file chữ thường không dấu nối gạch ngang (ví dụ `wireframe/check-in-tai-quay.drawio.svg`). File này vừa mở ra sửa được bằng draw.io, vừa xem ngay trên GitHub và chèn được vào báo cáo LaTeX.

## Quyết định kiến trúc (ADR) – [decisions/](decisions/)

| Số | Quyết định |
| --- | --- |
| [0000](decisions/0000-mau-adr.md) | Mẫu ADR |
| [0001](decisions/0001-cong-nghe.md) | Công nghệ: React + Spring Boot + PostgreSQL + Flyway, kèm phiên bản |
| [0002](decisions/0002-phat-trien-tang-dan-5-version.md) | Phát triển tăng dần theo 5 version |
| [0003](decisions/0003-danh-sach-cho-tu-dong-dua-vao-lop.md) | Danh sách chờ tự động đưa vào lớp |
| [0004](decisions/0004-thanh-toan-chi-tai-quay.md) | Thanh toán chỉ tại quầy |
| [0005](decisions/0005-erd-v2.md) | Thiết kế dữ liệu ERD bản 2 |
| [0006](decisions/0006-git-workflow.md) | Quy trình Git: main + nhánh tính năng, squash merge |
| [0007](decisions/0007-cau-truc-ma-nguon.md) | Cấu trúc mã nguồn: đóng gói theo chức năng |
| [0008](decisions/0008-goi-api-cung-domain.md) | Gọi API cùng domain qua proxy của Vercel (cookie refresh token) |
| [0009](decisions/0009-bo-so-do-phan-tich-thiet-ke.md) | Bộ 8 loại sơ đồ UML cho phân tích và thiết kế, không dùng DFD |

## Khác

| Thư mục | Nội dung |
| --- | --- |
| [meetings/](meetings/) | Biên bản họp ([mẫu](meetings/0000-mau-bien-ban.md)) |
| [plans/](plans/) | Tiến độ sprint so với kế hoạch ([Sprint 1](plans/sprint-1-tien-do.md)) |
| [testing/](testing/) | Kế hoạch và báo cáo kiểm thử (sẽ bổ sung) |
| [guides/cai-dat-moi-truong.md](guides/cai-dat-moi-truong.md) | Hướng dẫn cài đặt môi trường cho Windows và macOS |
| [guides/quy-tac-ma-nguon.md](guides/quy-tac-ma-nguon.md) | Quy tắc mã nguồn bắt buộc, quy tắc nghiệp vụ dễ làm sai, quy trình làm một chức năng |

Báo cáo LaTeX nằm ở thư mục [`report/`](../report/) (sẽ bổ sung).
