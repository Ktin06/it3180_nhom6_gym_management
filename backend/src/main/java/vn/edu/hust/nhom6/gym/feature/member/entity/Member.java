package vn.edu.hust.nhom6.gym.feature.member.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/** Hồ sơ hội viên; chỉ lưu ID liên kết, truy cập chức năng khác qua service. */
@Entity
@Table(name = "members")
@Getter
@Setter
public class Member extends BaseEntity {
    @Column(name = "user_id") private Long userId;
    @Column(name = "home_branch_id", nullable = false) private Long homeBranchId;
    @Column(name = "member_code", nullable = false, length = 20) private String memberCode;
    @Column(name = "full_name", nullable = false) private String fullName;
    @Enumerated(EnumType.STRING)
    @Column(length = 10) private Gender gender;
    private LocalDate dob;
    @Column(nullable = false, length = 20) private String phone;
    @Column(name = "avatar_url", length = 500) private String avatarUrl;
    @Column(name = "face_consent_at") private OffsetDateTime faceConsentAt;
    @Column(name = "booking_locked_until") private OffsetDateTime bookingLockedUntil;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private MemberStatus status = MemberStatus.ACTIVE;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;
    @Column(name = "created_by") private Long createdBy;
}
