package vn.edu.hust.nhom6.gym.feature.member.service;

import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;

/**
 * Controller F2 kiểm tra quyền endpoint và validate đầu vào trước khi gọi.
 * Service kiểm tra phạm vi chi nhánh, quyền sở hữu và các quy tắc nghiệp vụ.
 */
public interface MemberManagementService {
    MemberDetailResponse create(CurrentUser actor, CreateMemberRequest request);
    MemberDetailResponse update(CurrentUser actor, Long id, UpdateMemberRequest request);
    MemberDetailResponse detail(CurrentUser actor, Long id);
    PageResponse<MemberSummaryResponse> search(CurrentUser actor, String keyword, MemberStatus status, int page, int size);
}
