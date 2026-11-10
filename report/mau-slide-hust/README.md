# Mẫu slide Beamer HUST

Mẫu slide LaTeX Beamer theo nhận diện Đại học Bách khoa Hà Nội, dùng cho các buổi bàn giao, demo và bảo vệ của nhóm.

## Thành phần

| File | Vai trò |
| --- | --- |
| `beamerthemeHUST.sty` | Theme: màu, bố cục, trang bìa, trang chuyển mục. **Giữ nguyên tên file** vì `\usepackage{beamerthemeHUST}` tìm đúng tên này. |
| `slide-mau.tex` | File ví dụ: cấu hình lề, trang bìa, mục lục và các kiểu slide. Copy file này làm điểm bắt đầu. |
| `images/` | Ảnh nền của theme, 4 bộ: màu `red` hoặc `blue`, tỉ lệ `169` (16:9) hoặc `43` (4:3). Đây là file nguồn của mẫu, không phải file xuất. |

## Cách dùng

1. Copy `slide-mau.tex` sang chỗ làm slide (ví dụ `report/slide-sprint-1.tex`).
2. Trong file vừa copy, sửa đường dẫn để LaTeX tìm thấy theme và ảnh, ví dụ khi file nằm ở `report/`:

   ```latex
   \usepackage[red, 169]{mau-slide-hust/beamerthemeHUST}
   \graphicspath{{mau-slide-hust/images/}}
   ```

   Hoặc đặt file `.tex` ngay trong thư mục này thì không cần sửa gì.
3. Đổi màu, tỉ lệ ở dòng `\usepackage[<màu>, <tỉ lệ>]{beamerthemeHUST}`. Đổi tỉ lệ thì đổi cả `aspectratio=` trong `\documentclass`.
4. Thay tiêu đề, tác giả, đơn vị (đang là nội dung ví dụ) bằng thông tin của nhóm.
5. Biên dịch bằng XeLaTeX, pdfLaTeX hoặc Tectonic.

## Quy ước

Chỉ đưa file nguồn (`.tex`, `.sty`, ảnh dùng trong slide) lên Git. Không đưa file PDF đã xuất; bản PDF nộp cuối cùng đính kèm vào Release trên GitHub.
