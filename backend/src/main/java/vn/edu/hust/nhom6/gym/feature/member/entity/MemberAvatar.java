package vn.edu.hust.nhom6.gym.feature.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/**
 * Ảnh đại diện hội viên đã thu nhỏ (cạnh dài nhất tối đa 256 px) và nén JPEG tối đa 100 KB (ADR 0011).
 * Bảng con của {@code members}, lấy chi nhánh qua {@code members.home_branch_id}.
 */
@Entity
@Table(name = "member_avatars")
@Getter
@Setter
public class MemberAvatar extends BaseEntity {

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "content", nullable = false, columnDefinition = "bytea")
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType = "image/jpeg";

    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;
}
