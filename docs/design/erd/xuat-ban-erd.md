# ERD và từ điển dữ liệu cho báo cáo

- **Người biên soạn:** Nguyễn Anh Tú (`natus26`) · [#15](https://github.com/Ktin06/it3180_nhom6_gym_management/issues/15) · WBS 1.3.11.
- **Người duyệt:** Phạm Gia Bảo (`GamBug`), theo phân công Sprint 1.
- **Cập nhật:** 05/11/2026.
- **Nguồn chuẩn:** [erd-v2.dbml](erd-v2.dbml), đủ 39 bảng thuộc 8 nhóm.

## 1. Sản phẩm và nguồn sinh

| Tệp | Cách dùng |
| --- | --- |
| [Từ điển dữ liệu](tu-dien-du-lieu.md) | Tra cứu đủ 422 cột: kiểu, NULL, mặc định, khóa, ý nghĩa và ghi chú; kèm 44 khai báo chỉ mục và 25 tập giá trị Enum |
| [ERD tổng quan PDF](hinh/erd-v2.pdf) | Hình vector, có thể phóng to hoặc chèn vào LaTeX bằng `\includegraphics` |
| [ERD tổng quan PNG](hinh/erd-v2.png) | Xem ảnh trực tiếp hoặc chèn vào trình soạn thảo báo cáo |
| [ERD tổng quan SVG](hinh/erd-v2.svg) | Bản vector để xem trong trình duyệt; muốn đổi cấu trúc thì sửa nguồn chuẩn và sinh lại |
| [Thông tin nguồn hình](hinh/nguon-hinh.json) | SHA-256 DBML chuẩn hóa LF, công cụ dựng, số bảng/cột/quan hệ và phạm vi từng hình |

**Bộ hình hiện tại được dựng cục bộ bằng bộ phân tích DBML chính thức và Graphviz**, không phải bản xuất từ dbdiagram.io. Trong lần thực hiện ngày 05/11, website tải được nhưng editor không render trong các phiên trình duyệt sạch, nên chưa xuất được bằng chức năng PDF/PNG của website. Mục 3 hướng dẫn thực hiện bước xuất dbdiagram.io còn lại của #15; bản dựng cục bộ vẫn dùng cùng DBML chuẩn để kiểm tra nội dung và chuẩn bị báo cáo.

Hình tổng quan chứa đủ **39 bảng, 422 cột và 89 quan hệ**. Vì sơ đồ lớn, khi trình bày ở khổ giấy nhỏ nên dùng các hình theo nhóm bên dưới thay vì thu toàn bộ sơ đồ vào một trang A4.

| Nhóm | Số bảng trong nhóm | PDF cho báo cáo | SVG để xem |
| --- | ---: | --- | --- |
| 1. Tài khoản và hệ thống | 6 | [nhom-1.pdf](hinh/nhom-1.pdf) | [nhom-1.svg](hinh/nhom-1.svg) |
| 2. Chi nhánh và cơ sở vật chất | 4 | [nhom-2.pdf](hinh/nhom-2.pdf) | [nhom-2.svg](hinh/nhom-2.svg) |
| 3. Hội viên và gói tập | 7 | [nhom-3.pdf](hinh/nhom-3.pdf) | [nhom-3.svg](hinh/nhom-3.svg) |
| 4. Check-in và lớp học | 5 | [nhom-4.pdf](hinh/nhom-4.pdf) | [nhom-4.svg](hinh/nhom-4.svg) |
| 5. Huấn luyện viên | 4 | [nhom-5.pdf](hinh/nhom-5.pdf) | [nhom-5.svg](hinh/nhom-5.svg) |
| 6. Bán hàng và thanh toán | 7 | [nhom-6.pdf](hinh/nhom-6.pdf) | [nhom-6.svg](hinh/nhom-6.svg) |
| 7. Nhân sự | 4 | [nhom-7.pdf](hinh/nhom-7.pdf) | [nhom-7.svg](hinh/nhom-7.svg) |
| 8. Gamification | 2 | [nhom-8.pdf](hinh/nhom-8.pdf) | [nhom-8.svg](hinh/nhom-8.svg) |

Các hình nhóm giữ đầy đủ cột của bảng trong nhóm và mọi FK xuất phát từ các bảng đó. Bảng cha ngoài nhóm được hiện màu xám với khóa chính và nhãn “tham chiếu ngoài nhóm”; không phải bản sao hay bảng mới của schema. Hợp của các hình nhóm chứa đủ 89 quan hệ; một hình nhóm không thể hiện mọi quan hệ đi vào nhóm từ bên ngoài.

## 2. Ký hiệu và giới hạn thể hiện

- `PK`: khóa chính; `FK`: khóa ngoại; `?` sau kiểu dữ liệu: cột cho phép NULL. Dấu tròn ở đường quan hệ là tùy chọn, vạch là một, chân quạ là nhiều.
- Đường quan hệ nối cột khóa của bảng cha tới cột FK của bảng con. Các liên kết 1–1 như `members.user_id` và `staff.user_id` được giữ theo DBML, gồm khả năng để trống của cột.
- Tên kiểu trong hình là kiểu khai báo DBML. Enum chỉ phục vụ thiết kế; khi triển khai dùng `varchar` kèm `CHECK` theo quy ước. Không suy ra độ dài `varchar` của Enum từ ảnh.
- Hình tập trung vào cột và quan hệ. Mặc định, UNIQUE, CHECK, chỉ mục có điều kiện, `NULLS NOT DISTINCT` và các ghi chú nghiệp vụ xem trong từ điển hoặc DBML. Các điều kiện ghi trong note phải được thực thi bằng migration/mã nghiệp vụ, không tự có hiệu lực chỉ vì đã vẽ.
- Các sản phẩm này không xác nhận migration hay dữ liệu triển khai đã khớp ERD; đó là bước kiểm thử schema riêng. Không thay đổi quy tắc hoặc sửa DBML trong nhiệm vụ xuất bản này.

## 3. Xuất trực tiếp bằng dbdiagram.io

1. Mở [dbdiagram.io](https://dbdiagram.io/d), tạo sơ đồ mới và dán toàn bộ nội dung `erd-v2.dbml` vào editor DBML.
2. Kiểm tra không có lỗi cú pháp; đối chiếu đủ 39 bảng và 89 quan hệ. Có thể kéo vị trí bảng để bố cục dễ đọc, giữ nguyên tên, cột, kiểu và liên kết.
3. Dùng **Export → PDF/PNG** theo [hướng dẫn chính thức](https://docs.dbdiagram.io/basic-editing-experience/). Xuất đủ sơ đồ; kiểm tra không mất bảng ở rìa và đọc được tên bảng/cột khi phóng to.
4. Lưu thành tệp riêng mang tiền tố `dbdiagram-erd-v2`, ghi ngày xuất và SHA-256 của DBML đã dùng, rồi bổ sung liên kết vào mục 1. Giữ rõ nguồn công cụ của từng hình; không đổi nhãn bản dựng cục bộ thành bản xuất website.

Nếu DBML chưa thay đổi, nội dung cột/quan hệ trong hai cách dựng phải giống nhau; bố cục và màu sắc có thể khác. Không đưa dữ liệu hội viên, tài khoản đăng nhập hoặc bí mật môi trường vào sơ đồ.

## 4. Sinh lại bản cục bộ và từ điển

Công cụ dùng [@dbml/core](https://dbml.dbdiagram.io/js-module/core/) để phân tích nguồn và [Viz.js/Graphviz](https://viz-js.com/api/) để bố trí hình. Các gói được cài trong thư mục tạm; không thêm dependency vào frontend hoặc backend.

Ví dụ PowerShell, chạy tại gốc repository:

```powershell
$erdTools = Join-Path $env:TEMP 'gym-erd-tools'
npm.cmd install --prefix $erdTools --no-save --no-package-lock @dbml/core@11.0.1 @viz-js/viz@3.31.0 playwright-core@1.64.1

# Sinh và kiểm tra từ điển.
node scripts/export-data-dictionary.cjs --dbml-core "$erdTools/node_modules/@dbml/core"
node scripts/export-data-dictionary.cjs --dbml-core "$erdTools/node_modules/@dbml/core" --check

# Chỉ sinh SVG và thông tin nguồn hình.
node scripts/export-erd.cjs --dbml-core "$erdTools/node_modules/@dbml/core" --viz "$erdTools/node_modules/@viz-js/viz"

# Sinh cả SVG, PDF và PNG bằng Chrome cài trên máy.
node scripts/export-erd.cjs --dbml-core "$erdTools/node_modules/@dbml/core" --viz "$erdTools/node_modules/@viz-js/viz" --playwright "$erdTools/node_modules/playwright-core" --chromium 'C:/Program Files/Google/Chrome/Application/chrome.exe'
```

Có thể thay đường dẫn Chrome bằng trình duyệt Chromium tương thích. Mỗi lần cập nhật nguồn cần xuất lại toàn bộ định dạng đang được sử dụng; lệnh chỉ sinh SVG không cập nhật các PDF/PNG đã có. Nếu công cụ báo thiếu diễn giải cột mới, bổ sung diễn giải và cập nhật ngày/lịch sử xuất bản trước khi đưa vào báo cáo.

## 5. Đưa vào báo cáo LaTeX

Từ thư mục `report/`, ví dụ chèn một hình nhóm với gói `graphicx`:

```latex
\begin{figure}[htbp]
  \centering
  \includegraphics[width=\textwidth]{../docs/design/erd/hinh/nhom-3.pdf}
  \caption{ERD nhóm hội viên và gói tập, sinh từ nguồn ERD v2.}
  \label{fig:erd-hoi-vien-goi-tap}
\end{figure}
```

Hình PDF giữ chữ vector khi phóng to; nhóm nhiều bảng có thể cần trang ngang hoặc chia phần trình bày. Từ điển Markdown là nguồn bảng phục vụ duyệt nội dung, chuyển cùng báo cáo theo mẫu LaTeX của nhóm; giữ nguyên tên, kiểu, ràng buộc và ghi chú khi chuyển.

## 6. Lịch sử thay đổi

| Ngày | Nội dung |
| --- | --- |
| 05/11/2026 | Xuất từ điển và bộ ERD cục bộ từ DBML; bổ sung hướng dẫn tái tạo, sử dụng trong LaTeX và bước xuất dbdiagram.io còn lại |
