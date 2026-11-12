package vn.edu.hust.nhom6.gym.feature.member.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import vn.edu.hust.nhom6.gym.feature.member.entity.Gender;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;

public record MemberDetailResponse(Long id, String memberCode, String fullName, String phone,
    String email, MemberStatus status, Long homeBranchId, Long userId, Gender gender,
    LocalDate dob, OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
