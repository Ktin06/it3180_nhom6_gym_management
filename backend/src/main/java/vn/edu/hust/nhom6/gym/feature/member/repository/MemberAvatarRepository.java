package vn.edu.hust.nhom6.gym.feature.member.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberAvatar;

public interface MemberAvatarRepository extends JpaRepository<MemberAvatar, Long> {

    Optional<MemberAvatar> findByMemberId(Long memberId);
}
