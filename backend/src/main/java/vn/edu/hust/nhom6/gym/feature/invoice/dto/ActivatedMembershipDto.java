package vn.edu.hust.nhom6.gym.feature.invoice.dto;

import java.time.LocalDate;

import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/** Gói vừa kích hoạt khi thu tiền (ACTIVE hoặc UPCOMING), một phần của {@link PaymentConfirmationDto}. */
public record ActivatedMembershipDto(Long membershipId, PackageType packageType, MembershipStatus status,
        LocalDate startDate, LocalDate endDate, Integer remainingSessions) {

    public static ActivatedMembershipDto of(Membership m) {
        return new ActivatedMembershipDto(m.getId(), m.getPackageType(), m.getStatus(), m.getStartDate(),
            m.getEndDate(), m.getRemainingSessions());
    }
}
