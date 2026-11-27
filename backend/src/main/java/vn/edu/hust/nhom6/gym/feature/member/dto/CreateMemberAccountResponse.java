package vn.edu.hust.nhom6.gym.feature.member.dto;

/** Kết quả tạo tài khoản đăng nhập cho hồ sơ hội viên có sẵn (F2, UC-F2.2; không chứa token). */
public record CreateMemberAccountResponse(
    Long memberId,
    Long userId,
    String email
) {
}
