package vn.edu.hust.nhom6.gym.feature.member.dto;

import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;

public record MemberSummaryResponse(Long id, String memberCode, String fullName, String phone,
    String email, MemberStatus status, Long homeBranchId) { }
