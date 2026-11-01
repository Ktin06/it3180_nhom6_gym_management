#!/usr/bin/env bash
# Tạo (hoặc cập nhật) bộ nhãn GitHub cho repo. Chạy một lần sau khi tạo repo:
#   gh auth login
#   bash scripts/create-labels.sh                      # repo của thư mục hiện tại
#   bash scripts/create-labels.sh Ktin06/it3180_nhom6_gym_management
# --force: nhãn đã có thì cập nhật màu và mô tả, không báo lỗi.
set -euo pipefail

REPO_ARGS=()
if [ "${1:-}" != "" ]; then
  REPO_ARGS=(--repo "$1")
fi

label() {
  # Cú pháp ${a[@]+...} để chạy được cả với bash 3.2 mặc định của macOS khi mảng rỗng.
  gh label create "$1" --color "$2" --description "$3" --force ${REPO_ARGS[@]+"${REPO_ARGS[@]}"}
}

# ----- Chức năng F1–F20 (theo bản chốt phạm vi) -----
label "F1"  "1d76db" "Tài khoản và phân quyền (v1)"
label "F2"  "1d76db" "Hồ sơ hội viên (v1)"
label "F3"  "1d76db" "Gói tập và gia hạn (v1)"
label "F4"  "1d76db" "Check-in tại quầy (v1)"
label "F5"  "1d76db" "Thanh toán tại quầy (v1)"
label "F6"  "1d76db" "Dashboard cơ bản (v1)"
label "F7"  "0e8a16" "Cổng hội viên (v2)"
label "F8"  "0e8a16" "Lớp học nhóm (v2)"
label "F9"  "0e8a16" "Huấn luyện viên (PT) (v2)"
label "F10" "0e8a16" "Thông báo (v2)"
label "F11" "5319e7" "Thiết bị (v3)"
label "F12" "5319e7" "Bán hàng tại quầy (POS) và kho (v3)"
label "F13" "5319e7" "Nhân sự và lương (v3)"
label "F14" "5319e7" "Khuyến mãi (v3)"
label "F15" "5319e7" "Bảo lưu và chuyển nhượng gói (v3)"
label "F16" "5319e7" "Báo cáo nâng cao (v3)"
label "F17" "5319e7" "Nhật ký hoạt động (audit log) (v3)"
label "F18" "fbca04" "Đa chi nhánh (v4)"
label "F19" "fbca04" "Gamification (v4)"
label "F20" "c5def5" "Check-in bằng nhận diện khuôn mặt (v5)"

# ----- Loại việc -----
label "type:feature" "a2eeef" "Chức năng mới"
label "type:bug"     "d73a4a" "Lỗi"
label "type:docs"    "0075ca" "Tài liệu"
label "type:test"    "bfd4f2" "Kiểm thử"
label "type:chore"   "ededed" "Cấu hình, công cụ, việc lặt vặt"

# ----- Độ ưu tiên -----
label "priority:high"   "b60205" "Ưu tiên cao: chặn demo, sai tiền, lộ dữ liệu"
label "priority:medium" "fbca04" "Ưu tiên trung bình"
label "priority:low"    "0e8a16" "Ưu tiên thấp"

# ----- Version -----
label "v1" "1d76db" "Vận hành cơ bản tại quầy (F1–F6) – bắt buộc"
label "v2" "0e8a16" "Hội viên, lớp học và huấn luyện viên (F7–F10) – bắt buộc"
label "v3" "5319e7" "Vận hành nâng cao (F11–F17) – mục tiêu"
label "v4" "fbca04" "Chuỗi phòng tập (F18–F19) – mục tiêu, cắt thứ hai nếu trễ"
label "v5" "c5def5" "Nhận diện khuôn mặt (F20) – cắt đầu tiên nếu trễ"

# ----- Trạng thái -----
label "blocked" "000000" "Đang bị chặn, chờ việc khác hoặc chờ quyết định"

echo "Đã tạo/cập nhật xong nhãn."
