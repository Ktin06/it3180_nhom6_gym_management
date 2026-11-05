package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Fallback;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberRegistrationService;

class F1MemberRegistrationAdapterTest {
    // Hợp đồng cục bộ chỉ để test cơ chế nối; bài IT riêng dùng chính interface F1 thật.
    public interface RegistrationContract {
        void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender);
    }

    @Test
    void delegatesAllArgumentsAndPropagatesConflict() {
        MemberRegistrationService target = mock(MemberRegistrationService.class);
        var adapter = adapter(target);
        RegistrationContract service = (RegistrationContract) adapter.getObject();
        LocalDate dob = LocalDate.of(2000, 5, 20);
        service.registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE");
        verify(target).registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE");
        doThrow(new ConflictException("Số điện thoại đã có hồ sơ"))
            .when(target).registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE");
        assertThatThrownBy(() -> service.registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE"))
            .isInstanceOf(ConflictException.class).hasMessageContaining("Số điện thoại");
        assertThat(adapter.getObjectType()).isEqualTo(RegistrationContract.class);
        assertThat(adapter.getObject()).isSameAs(service);
        assertThat(service.equals(service)).isTrue();
        assertThat(service.equals(new Object())).isFalse();
        assertThat(service.hashCode()).isEqualTo(System.identityHashCode(service));
    }

    @Test
    void ordinaryAdapterIsSelectedOverFallbackWithoutPrimary() {
        new ApplicationContextRunner().withUserConfiguration(SelectionConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            var selected = context.getBean(RegistrationContract.class);
            assertThat(selected).isSameAs(context.getBean("adapter"));
            assertThat(selected).isNotSameAs(context.getBean("fallback"));
            selected.registerMemberProfile(1L, "An", "0912345678", null, null);
            verify(context.getBean(MemberRegistrationService.class))
                .registerMemberProfile(1L, "An", "0912345678", null, null);
        });
    }

    @Test
    void absentF1DoesNotBreakStandaloneF2Context() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("vn.edu.hust.nhom6.gym.feature.auth"))
            .withUserConfiguration(F1MemberRegistrationAdapter.class)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean("f2MemberRegistrationAdapter");
            });
    }

    private F1MemberRegistrationAdapter adapter(MemberRegistrationService target) {
        @SuppressWarnings("unchecked")
        ObjectProvider<MemberRegistrationService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(target);
        return new F1MemberRegistrationAdapter(provider, RegistrationContract.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class SelectionConfiguration {
        @Bean
        MemberRegistrationService registrations() { return mock(MemberRegistrationService.class); }

        @Bean
        F1MemberRegistrationAdapter adapter(ObjectProvider<MemberRegistrationService> registrations) {
            return new F1MemberRegistrationAdapter(registrations, RegistrationContract.class);
        }

        @Bean
        @Fallback
        RegistrationContract fallback() { return mock(RegistrationContract.class); }
    }
}
