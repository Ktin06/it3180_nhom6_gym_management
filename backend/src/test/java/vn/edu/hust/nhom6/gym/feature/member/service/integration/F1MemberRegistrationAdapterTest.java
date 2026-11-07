package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.feature.auth.service.DefaultMemberService;
import vn.edu.hust.nhom6.gym.feature.auth.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberRegistrationService;

class F1MemberRegistrationAdapterTest {
    @Test
    void delegatesAllArgumentsAndPropagatesConflict() {
        MemberRegistrationService target = mock(MemberRegistrationService.class);
        MemberService service = new F1MemberRegistrationAdapter(target);
        LocalDate dob = LocalDate.of(2000, 5, 20);
        service.registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE");
        verify(target).registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE");
        var failure = new ConflictException("Số điện thoại đã có hồ sơ");
        doThrow(failure)
            .when(target).registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE");
        assertThatThrownBy(() -> service.registerMemberProfile(15L, "Nguyễn An", "0912345678", dob, "MALE"))
            .isSameAs(failure);
    }

    @Test
    void ordinaryAdapterIsSelectedOverFallbackWithoutPrimary() {
        new ApplicationContextRunner().withUserConfiguration(SelectionConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            MemberService selected = context.getBean(MemberService.class);
            assertThat(selected).isSameAs(context.getBean(F1MemberRegistrationAdapter.class));
            assertThat(selected).isNotSameAs(context.getBean(DefaultMemberService.class));
            selected.registerMemberProfile(1L, "An", "0912345678", null, null);
            verify(context.getBean(MemberRegistrationService.class))
                .registerMemberProfile(1L, "An", "0912345678", null, null);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({F1MemberRegistrationAdapter.class, DefaultMemberService.class})
    static class SelectionConfiguration {
        @Bean
        MemberRegistrationService registrations() { return mock(MemberRegistrationService.class); }
    }
}
