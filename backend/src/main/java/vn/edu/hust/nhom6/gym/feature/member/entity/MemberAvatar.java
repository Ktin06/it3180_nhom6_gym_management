package vn.edu.hust.nhom6.gym.feature.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/** Ảnh JPEG đã thu nhỏ; chi nhánh lấy qua hồ sơ, không lưu đường dẫn ảnh (ADR 0011). */
@Entity
@Table(name = "member_avatars")
@Getter
@Setter
public class MemberAvatar extends BaseEntity {
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType = "image/jpeg";

    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;
}
