package vn.edu.hust.nhom6.gym.feature.trainer.dto;

/**
 * Một học viên PT đang dạy (VD-26 vấn đề 1, SUA-5; GĐ-10 của S2-20.1). {@code viaPt}: có buổi PT chưa hủy
 * ({@code BOOKED}, {@code DONE}, {@code NO_SHOW}) với PT (luôn {@code true}). {@code viaClass}: luôn {@code false}
 * vì nhánh lớp nhóm đã bỏ; giữ lại trường này để tương thích giao diện S2-20.2 và sẽ bỏ khi đưa vào {@code main}.
 */
public record TrainerStudentDto(
        Long memberId,
        String memberCode,
        String fullName,
        Long homeBranchId,
        boolean viaPt,
        boolean viaClass) {
}
