package vn.edu.hust.nhom6.gym.feature.member.mapper;

import org.springframework.stereotype.Component;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;

@Component
public class MemberMapper {
    public MemberSummaryResponse summary(Member m, String email) {
        return new MemberSummaryResponse(m.getId(), m.getMemberCode(), m.getFullName(), m.getPhone(),
            email, m.getStatus(), m.getHomeBranchId());
    }

    public MemberDetailResponse detail(Member m, String email) {
        return new MemberDetailResponse(m.getId(), m.getMemberCode(), m.getFullName(), m.getPhone(),
            email, m.getStatus(), m.getHomeBranchId(), m.getUserId(), m.getGender(), m.getDob(),
            m.getCreatedAt(), m.getUpdatedAt());
    }
}
