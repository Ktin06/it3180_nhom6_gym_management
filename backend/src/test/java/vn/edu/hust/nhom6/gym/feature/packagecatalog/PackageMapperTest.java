package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.GymPackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.mapper.PackageMapper;
import static org.assertj.core.api.Assertions.assertThat;

class PackageMapperTest {
    @Test
    void detailUsesUtcEvenWhenAuditWasCreatedInVietnamTimezone() {
        var item = GymPackage.create(1L, "UTC", "Gói UTC", PackageType.TIME, 30, null, 0, null);
        var audit = OffsetDateTime.parse("2026-11-09T09:00:00+07:00");
        ReflectionTestUtils.setField(item, "createdAt", audit);
        ReflectionTestUtils.setField(item, "updatedAt", audit);
        var response = PackageMapper.detail(item);
        assertThat(response.createdAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(response.updatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(response.createdAt().toInstant()).isEqualTo(audit.toInstant());
        item.deactivate();
        var stopped = PackageMapper.deactivation(item);
        assertThat(stopped.updatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(stopped.updatedAt().toInstant()).isEqualTo(audit.toInstant());
    }
}
