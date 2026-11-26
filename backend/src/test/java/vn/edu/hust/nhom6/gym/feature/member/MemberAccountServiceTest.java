package vn.edu.hust.nhom6.gym.feature.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionOperations;

import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.PasswordResetService;
import vn.edu.hust.nhom6.gym.feature.member.dto.CreateMemberAccountRequest;
import vn.edu.hust.nhom6.gym.feature.member.dto.CreateMemberAccountResponse;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberAccountService;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;

/**
 * Kiểm thử đơn vị cho {@link MemberAccountService} (F2, UC-F2.2, S2-36).
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MemberAccountServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private EmailSender emailSender;

    private MemberAccountService memberAccountService;
    private final CurrentUser receptionist = new CurrentUser(10L, UserRole.RECEPTIONIST);

    @BeforeEach
    void setUp() {
        memberAccountService = new MemberAccountService(
            memberService,
            passwordResetService,
            emailSender,
            TransactionOperations.withoutTransaction(),
            "http://localhost:5173/");
    }

    @Test
    @DisplayName("TC-F2.2.1-01e: phát token hạn 72 giờ và gửi email tiêu đề Kích hoạt tài khoản chứa liên kết đặt mật khẩu")
    void createAccountIssues72HourTokenAndSendsActivationEmail() {
        var linked = new CreateMemberAccountResponse(5L, 77L, "member@example.com");
        when(memberService.linkAccount(receptionist, 5L, "Member@Example.com")).thenReturn(linked);
        when(passwordResetService.issue(77L, Duration.ofHours(72))).thenReturn("raw-secret-token-72h");

        CreateMemberAccountResponse response = memberAccountService.createAccount(
            receptionist, 5L, new CreateMemberAccountRequest("Member@Example.com"));

        assertThat(response).isEqualTo(linked);
        verify(passwordResetService).issue(77L, Duration.ofHours(72));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(
            eq("member@example.com"),
            eq("Kích hoạt tài khoản"),
            bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
            .contains("http://localhost:5173/dat-lai-mat-khau?token=raw-secret-token-72h");
    }

    @Test
    @DisplayName("TC-F2.2.1-06a: gửi email lỗi vẫn trả 200 và ghi log ERROR không chứa token")
    void emailFailureDoesNotFailAccountCreationAndDoesNotLogToken(CapturedOutput output) {
        var linked = new CreateMemberAccountResponse(9L, 88L, "member9@example.com");
        when(memberService.linkAccount(receptionist, 9L, "member9@example.com")).thenReturn(linked);
        when(passwordResetService.issue(88L, Duration.ofHours(72))).thenReturn("super-secret-raw-token-xyz");
        doThrow(new RuntimeException("Brevo trả lại nội dung thư có liên kết ?token=super-secret-raw-token-xyz"))
            .when(emailSender).send(eq("member9@example.com"), eq("Kích hoạt tài khoản"), anyString());

        CreateMemberAccountResponse response = memberAccountService.createAccount(
            receptionist, 9L, new CreateMemberAccountRequest("member9@example.com"));

        assertThat(response).isEqualTo(linked);
        assertThat(output.getOut()).contains("ERROR");
        assertThat(output.getOut()).contains("88");
        assertThat(output.getOut()).doesNotContain("super-secret-raw-token-xyz");
    }
}
