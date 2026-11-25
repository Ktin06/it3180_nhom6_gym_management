package vn.edu.hust.nhom6.gym.feature.member.repository;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberAvatar;

public interface MemberAvatarRepository extends JpaRepository<MemberAvatar, Long> {
    Optional<MemberAvatar> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    /** Chỉ đọc ID cho trang hiện tại, không nạp nội dung ảnh hay truy vấn từng hồ sơ. */
    @Query("SELECT a.memberId FROM MemberAvatar a WHERE a.memberId IN :memberIds")
    Set<Long> findMemberIdsWithAvatar(Set<Long> memberIds);
}
