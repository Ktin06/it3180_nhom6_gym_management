package vn.edu.hust.nhom6.gym.feature.member.service;

import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;

public interface MemberManagementService {
    MemberDetailResponse create(MemberActor actor, CreateMemberRequest request);
    MemberDetailResponse update(MemberActor actor, Long id, UpdateMemberRequest request);
    MemberDetailResponse detail(MemberActor actor, Long id);
    PageResponse<MemberSummaryResponse> search(MemberActor actor, String keyword, MemberStatus status, int page, int size);
}
