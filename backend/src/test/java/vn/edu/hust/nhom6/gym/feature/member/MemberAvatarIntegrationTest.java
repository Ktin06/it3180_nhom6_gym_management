package vn.edu.hust.nhom6.gym.feature.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/**
 * Kiểm thử tích hợp API ảnh đại diện hội viên (F2, S2-06.1, Issue #183, ADR 0011).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MemberTestConfiguration.class)
class MemberAvatarIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Long admin;
    private Long manager;
    private Long receptionist;
    private Long otherReceptionist;
    private Long memberUser;
    private Long otherMemberUser;
    private Long trainer;
    private Long branch;
    private Long otherBranch;
    private Long memberId;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE TABLE members, users, staff, refresh_tokens RESTART IDENTITY CASCADE");
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        jdbc.update("""
            INSERT INTO branches(code, name, open_time, close_time) VALUES ('CN02','Chi nhánh 2','06:00','22:00')
            ON CONFLICT(code) DO NOTHING
            """);
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN02'", Long.class);

        admin = insertUser("admin@example.com", "SUPER_ADMIN");
        manager = insertUser("manager@example.com", "BRANCH_MANAGER");
        receptionist = insertUser("reception@example.com", "RECEPTIONIST");
        otherReceptionist = insertUser("other.reception@example.com", "RECEPTIONIST");
        memberUser = insertUser("member@example.com", "MEMBER");
        otherMemberUser = insertUser("other.member@example.com", "MEMBER");
        trainer = insertUser("trainer@example.com", "TRAINER");

        insertStaff(manager, branch, "NV01");
        insertStaff(receptionist, branch, "NV02");
        insertStaff(otherReceptionist, otherBranch, "NV03");

        memberId = insertProfile("HV000901", "0912345601", branch, memberUser, "Nguyễn Văn An");
    }

    @Test
    @DisplayName("US-F2.4.2 KB1: lễ tân tải JPEG hoặc PNG trả 200 và lưu JPEG cạnh dài tối đa 256px, dung lượng tối đa 100KB")
    void uploadValidPngOrJpegResizesAndCompressesToJpegInDatabase() throws Exception {
        byte[] pngBytes = createImageBytes(600, 400, "png", true);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", pngBytes);

        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId)
                .file(file)
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM member_avatars WHERE member_id = ?", Integer.class, memberId);
        assertThat(count).isEqualTo(1);

        String contentType = jdbc.queryForObject(
            "SELECT content_type FROM member_avatars WHERE member_id = ?", String.class, memberId);
        Integer sizeBytes = jdbc.queryForObject(
            "SELECT size_bytes FROM member_avatars WHERE member_id = ?", Integer.class, memberId);
        Long uploadedBy = jdbc.queryForObject(
            "SELECT uploaded_by FROM member_avatars WHERE member_id = ?", Long.class, memberId);
        byte[] storedBytes = jdbc.queryForObject(
            "SELECT content FROM member_avatars WHERE member_id = ?", byte[].class, memberId);

        assertThat(contentType).isEqualTo("image/jpeg");
        assertThat(sizeBytes).isPositive().isLessThanOrEqualTo(102400);
        assertThat(storedBytes).hasSize(sizeBytes);
        assertThat(uploadedBy).isEqualTo(receptionist);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(storedBytes));
        assertThat(decoded).isNotNull();
        assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isEqualTo(256);
        assertThat(decoded.getWidth()).isEqualTo(256);
        assertThat(decoded.getHeight()).isEqualTo(171);
    }

    @Test
    @DisplayName("US-F2.4.2 KB2: tải ảnh lần hai ghi đè, vẫn một dòng trong member_avatars")
    void secondUploadOverwritesExistingRowAndUpdatesUploader() throws Exception {
        MockMultipartFile firstFile = new MockMultipartFile(
            "file", "first.jpg", "image/jpeg", createImageBytes(512, 512, "jpg", false));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId)
                .file(firstFile)
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk());

        Long firstAvatarId = jdbc.queryForObject(
            "SELECT id FROM member_avatars WHERE member_id = ?", Long.class, memberId);

        MockMultipartFile secondFile = new MockMultipartFile(
            "file", "second.png", "image/png", createImageBytes(120, 80, "png", false));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId)
                .file(secondFile)
                .with(identity(manager, UserRole.BRANCH_MANAGER)))
            .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM member_avatars WHERE member_id = ?", Integer.class, memberId);
        Long secondAvatarId = jdbc.queryForObject(
            "SELECT id FROM member_avatars WHERE member_id = ?", Long.class, memberId);
        Long uploadedBy = jdbc.queryForObject(
            "SELECT uploaded_by FROM member_avatars WHERE member_id = ?", Long.class, memberId);
        byte[] storedBytes = jdbc.queryForObject(
            "SELECT content FROM member_avatars WHERE member_id = ?", byte[].class, memberId);

        assertThat(count).isEqualTo(1);
        assertThat(secondAvatarId).isEqualTo(firstAvatarId);
        assertThat(uploadedBy).isEqualTo(manager);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(storedBytes));
        assertThat(decoded.getWidth()).isEqualTo(120);
        assertThat(decoded.getHeight()).isEqualTo(80);
    }

    @Test
    @DisplayName("US-F2.4.2 E1: file không phải ảnh, vượt 5MB hoặc vượt 8000px bị từ chối với mã 400")
    void rejectsNonImageOversizedOrOverDimensionFilesWith400() throws Exception {
        RequestPostProcessor auth = identity(receptionist, UserRole.RECEPTIONIST);

        // 1. File giả đuôi .jpg nhưng nội dung không phải ảnh
        MockMultipartFile fakeJpg = new MockMultipartFile(
            "file", "fake.jpg", "image/jpeg", "khong-phai-anh".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId).file(fakeJpg).with(auth))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        // 2. File ảnh BMP đổi đuôi thành .png (chỉ nhận JPEG hoặc PNG)
        ByteArrayOutputStream bmpOut = new ByteArrayOutputStream();
        BufferedImage bmpImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bmpImg, "bmp", bmpOut);
        MockMultipartFile disguisedBmp = new MockMultipartFile(
            "file", "disguised.png", "image/png", bmpOut.toByteArray());
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId).file(disguisedBmp).with(auth))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        // 3. File vượt 5 MB -> trả 400 kèm errors.file
        byte[] overFiveMb = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile tooLargeFile = new MockMultipartFile(
            "file", "huge.jpg", "image/jpeg", overFiveMb);
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId).file(tooLargeFile).with(auth))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.file").exists());

        // 4. Ảnh có một cạnh > 8.000 px (8001 x 1 px) -> chặn trước khi giải nén, trả 400
        MockMultipartFile tooWideFile = new MockMultipartFile(
            "file", "wide.png", "image/png", createImageBytes(8001, 1, "png", false));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId).file(tooWideFile).with(auth))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        Integer count = jdbc.queryForObject("SELECT count(*) FROM member_avatars", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("US-F2.4.2 S3: hội viên xem ảnh của mình trả 200, ảnh người khác hoặc chưa có ảnh trả 404")
    void memberCanReadOwnAvatarWhileMissingOrOtherMemberAvatarReturns404() throws Exception {
        // Chưa có ảnh -> 404
        mvc.perform(get("/api/v1/members/{id}/avatar", memberId)
                .with(identity(memberUser, UserRole.MEMBER)))
            .andExpect(status().isNotFound());

        // Lễ tân tải ảnh lên
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.jpg", "image/jpeg", createImageBytes(200, 200, "jpg", false));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId)
                .file(file)
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk());

        byte[] storedBytes = jdbc.queryForObject(
            "SELECT content FROM member_avatars WHERE member_id = ?", byte[].class, memberId);

        // Hội viên xem ảnh của chính mình -> 200 image/jpeg
        mvc.perform(get("/api/v1/members/{id}/avatar", memberId)
                .with(identity(memberUser, UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(content().bytes(storedBytes));

        // Hội viên khác xem ảnh -> 404
        mvc.perform(get("/api/v1/members/{id}/avatar", memberId)
                .with(identity(otherMemberUser, UserRole.MEMBER)))
            .andExpect(status().isNotFound());

        // Hội viên không tồn tại -> 404
        mvc.perform(get("/api/v1/members/{id}/avatar", 999999L)
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-F2.4.2 S3: nhân viên chi nhánh khác trả 404, MEMBER gọi PUT trả 403")
    void enforcesBranchAndRolePermissionsForAvatarEndpoints() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.jpg", "image/jpeg", createImageBytes(150, 150, "jpg", false));

        // MEMBER và TRAINER gọi PUT -> 403 Forbidden
        for (UserRole role : List.of(UserRole.MEMBER, UserRole.TRAINER)) {
            mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId)
                    .file(file)
                    .with(identity(memberUser, role)))
                .andExpect(status().isForbidden());
        }

        // Nhân viên chi nhánh khác gọi PUT -> 404 Not Found
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId)
                .file(file)
                .with(identity(otherReceptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound());

        // Lễ tân cùng chi nhánh tải ảnh lên -> 200 OK
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/members/{id}/avatar", memberId)
                .file(file)
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isOk());

        // Nhân viên chi nhánh khác gọi GET -> 404 Not Found
        mvc.perform(get("/api/v1/members/{id}/avatar", memberId)
                .with(identity(otherReceptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isNotFound());

        // Chủ chuỗi gọi GET -> 200 OK
        mvc.perform(get("/api/v1/members/{id}/avatar", memberId)
                .with(identity(admin, UserRole.SUPER_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG));

        // TRAINER gọi GET -> 403 Forbidden
        mvc.perform(get("/api/v1/members/{id}/avatar", memberId)
                .with(identity(trainer, UserRole.TRAINER)))
            .andExpect(status().isForbidden());
    }

    private static byte[] createImageBytes(int width, int height, String format, boolean withAlpha) throws IOException {
        int type = withAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(withAlpha ? new Color(30, 144, 255, 180) : new Color(30, 144, 255));
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private Long insertUser(String email, String role) {
        return jdbc.queryForObject(
            "INSERT INTO users(email,password_hash,role) VALUES (?, 'hash', ?) RETURNING id",
            Long.class, email, role);
    }

    private void insertStaff(Long userId, Long branchId, String code) {
        jdbc.update(
            "INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,?,'Nhân viên')",
            userId, branchId, code);
    }

    private Long insertProfile(String code, String phone, Long branchId, Long userId, String name) {
        return jdbc.queryForObject("""
            INSERT INTO members(member_code,phone,home_branch_id,user_id,full_name) VALUES (?,?,?,?,?) RETURNING id
            """, Long.class, code, phone, branchId, userId, name);
    }

    private RequestPostProcessor identity(Long userId, UserRole role) {
        return authentication(new UsernamePasswordAuthenticationToken(
            userId.toString(), "test-only", List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
