package vn.edu.hust.nhom6.gym.feature.member.service.integration;

/** Hợp đồng tích hợp dịch vụ chi nhánh/nhân sự; F2 không sở hữu schema hay repository đó. */
public interface MemberBranchAccess {
    /** Tra cứu chi nhánh đang hoạt động theo mã CN01, không dùng ID cố định. */
    Long defaultBranchId();
    /** Nhân viên phải có chi nhánh hoạt động; không có thì từ chối. */
    Long staffBranchId(Long userId);
}
