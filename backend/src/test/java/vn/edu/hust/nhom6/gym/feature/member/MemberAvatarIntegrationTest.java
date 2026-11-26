package vn.edu.hust.nhom6.gym.feature.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.edu.hust.nhom6.gym.DatabaseCleaner;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;

/** #183: API thật qua MockMvc và multipart qua HTTP/Tomcat, PostgreSQL riêng do Testcontainers tạo. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(MemberTestConfiguration.class)
class MemberAvatarIntegrationTest {
    private static final int FIVE_MIB = 5 * 1024 * 1024;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @LocalServerPort int port;
    Long owner, receptionist, manager, otherStaff, memberUser, otherMember, trainer, branch, otherBranch, memberId;

    @BeforeEach
    void fixtures() {
        DatabaseCleaner.truncate(jdbc, "members", "users", "staff", "refresh_tokens");
        jdbc.update("UPDATE branches SET status = 'ACTIVE' WHERE code = 'CN01'");
        jdbc.update("""
            INSERT INTO branches(code,name,open_time,close_time) VALUES ('CN02','Chi nhánh 2','06:00','22:00')
            ON CONFLICT(code) DO UPDATE SET status = 'ACTIVE'
            """);
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN02'", Long.class);
        owner = user("owner@example.com", "SUPER_ADMIN");
        receptionist = user("receptionist@example.com", "RECEPTIONIST");
        manager = user("manager@example.com", "BRANCH_MANAGER");
        otherStaff = user("other-staff@example.com", "RECEPTIONIST");
        memberUser = user("member@example.com", "MEMBER");
        otherMember = user("other-member@example.com", "MEMBER");
        trainer = user("trainer@example.com", "TRAINER");
        staff(receptionist, branch, "NV01");
        staff(manager, branch, "NV02");
        staff(otherStaff, otherBranch, "NV03");
        memberId = jdbc.queryForObject("""
            INSERT INTO members(member_code,phone,home_branch_id,user_id,full_name)
            VALUES ('HV000901','0912345601',?,?,'Hội viên kiểm thử') RETURNING id
            """, Long.class, branch, memberUser);
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpg"})
    @DisplayName("TC-F2.4.2-01a: JPEG và PNG được thu nhỏ đúng tỷ lệ, lưu JPEG và người tải")
    void validImagesAreResizedAndStored(String format) throws Exception {
        // Content-Type và đuôi file không quyết định định dạng: server đọc byte thực.
        upload(receptionist, UserRole.RECEPTIONIST, new MockMultipartFile(
            "file", "unknown.bin", "application/octet-stream", image(600, 400, format, Color.RED)))
            .andExpect(status().isOk()).andExpect(content().string(""));
        byte[] stored = stored();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored));
        assertThat(decoded.getWidth()).isEqualTo(256);
        assertThat(decoded.getHeight()).isEqualTo(171);
        assertThat(stored.length).isBetween(1, 102400);
        assertThat(stored[0] & 0xff).isEqualTo(0xff);
        assertThat(stored[1] & 0xff).isEqualTo(0xd8);
        assertThat(jdbc.queryForObject("SELECT content_type FROM member_avatars", String.class)).isEqualTo("image/jpeg");
        assertThat(jdbc.queryForObject("SELECT size_bytes FROM member_avatars", Integer.class)).isEqualTo(stored.length);
        assertThat(jdbc.queryForObject("SELECT uploaded_by FROM member_avatars", Long.class)).isEqualTo(receptionist);
        assertThat(jdbc.queryForObject("SELECT created_at IS NOT NULL AND updated_at IS NOT NULL FROM member_avatars",
            Boolean.class)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"120,80", "256,256", "80,120"})
    @DisplayName("TC-F2.4.2-01b: Ảnh nhỏ không phóng lớn và PNG trong suốt chuyển nền trắng")
    void smallTransparentImagesKeepDimensions(int width, int height) throws Exception {
        upload(receptionist, UserRole.RECEPTIONIST, file(image(width, height, "png", new Color(0, 0, 0, 0))))
            .andExpect(status().isOk());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored()));
        assertThat(decoded.getWidth()).isEqualTo(width);
        assertThat(decoded.getHeight()).isEqualTo(height);
        assertThat(new Color(decoded.getRGB(width / 2, height / 2))).isEqualTo(Color.WHITE);
    }

    @ParameterizedTest
    @CsvSource({"8000,1", "1,8000"})
    @DisplayName("TC-F2.4.2-01c: Chấp nhận đúng biên 8000 px và giữ cạnh nhỏ tối thiểu 1 px")
    void acceptsDimensionBoundary(int width, int height) throws Exception {
        upload(receptionist, UserRole.RECEPTIONIST, file(image(width, height, "png", Color.BLUE)))
            .andExpect(status().isOk());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored()));
        assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isEqualTo(256);
        assertThat(Math.min(decoded.getWidth(), decoded.getHeight())).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F2.4.2-01d: Chấp nhận file JPEG đúng giới hạn 5 MiB")
    void acceptsUploadSizeBoundary() throws Exception {
        byte[] padded = Arrays.copyOf(image(30, 30, "jpg", Color.RED), FIVE_MIB);
        upload(receptionist, UserRole.RECEPTIONIST, file(padded)).andExpect(status().isOk());
        assertThat(stored().length).isBetween(1, 102400);
    }

    @Test
    @DisplayName("TC-F2.4.2-02a: Tải ảnh lần hai giữ ID bản ghi và cập nhật nội dung, người tải")
    void overwritesSameRow() throws Exception {
        seedAvatar();
        Long originalId = jdbc.queryForObject("SELECT id FROM member_avatars", Long.class);
        byte[] original = stored();
        upload(manager, UserRole.BRANCH_MANAGER, file(image(90, 50, "png", Color.BLUE)))
            .andExpect(status().isOk());
        assertThat(avatarCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM member_avatars", Long.class)).isEqualTo(originalId);
        assertThat(jdbc.queryForObject("SELECT uploaded_by FROM member_avatars", Long.class)).isEqualTo(manager);
        assertThat(stored()).isNotEqualTo(original);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored()));
        assertThat(decoded.getWidth()).isEqualTo(90);
        assertThat(decoded.getHeight()).isEqualTo(50);
    }

    @Test
    @DisplayName("TC-F2.4.2-02b: Hai upload đầu tiên đồng thời vẫn chỉ có một bản ghi ảnh")
    void concurrentInitialUploadsAreSerialized() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        byte[] first = image(90, 50, "png", Color.RED);
        byte[] second = image(60, 40, "png", Color.BLUE);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Chờ upload quá hạn"); }
                return upload(receptionist, UserRole.RECEPTIONIST, file(first)).andReturn().getResponse().getStatus();
            });
            var b = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Chờ upload quá hạn"); }
                return upload(manager, UserRole.BRANCH_MANAGER, file(second)).andReturn().getResponse().getStatus();
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(b.get(20, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(avatarCount()).isEqualTo(1);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored()));
        Long uploader = jdbc.queryForObject("SELECT uploaded_by FROM member_avatars", Long.class);
        assertThat(decoded.getWidth()).isEqualTo(uploader.equals(manager) ? 60 : 90);
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "text", "bmp", "broken", "oversized", "wide", "tall"})
    @DisplayName("TC-F2.4.2-03a, TC-F2.4.2-02c: Từ chối file sai và giữ nguyên ảnh, người tải cũ")
    void invalidFilesDoNotReplaceExistingAvatar(String kind) throws Exception {
        seedAvatar();
        byte[] original = stored();
        byte[] invalid = switch (kind) {
            case "empty" -> new byte[0];
            case "text" -> "khong-phai-anh".getBytes(StandardCharsets.UTF_8);
            case "bmp" -> image(30, 30, "bmp", Color.RED);
            case "broken" -> new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
            case "oversized" -> new byte[FIVE_MIB + 1];
            case "wide" -> image(8001, 1, "png", Color.RED);
            case "tall" -> image(1, 8001, "png", Color.RED);
            default -> throw new IllegalArgumentException(kind);
        };
        upload(manager, UserRole.BRANCH_MANAGER, file(invalid)).andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400));
        assertThat(stored()).containsExactly(original);
        assertThat(avatarCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT uploaded_by FROM member_avatars", Long.class)).isEqualTo(receptionist);
    }

    @Test
    @DisplayName("TC-F2.4.2-03b: Giới hạn multipart thật của Tomcat trả 400 có errors.file")
    void servletRejectsOversizedMultipart() throws Exception {
        String boundary = "AvatarTestBoundary";
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"big.jpg\""
            + "\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        body.write(new byte[FIVE_MIB + 1]);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        String token = tokens.generateAccessToken(receptionist, "receptionist@example.com", UserRole.RECEPTIONIST);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path()))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("\"file\"");
        }
        assertThat(avatarCount()).isZero();
    }

    @Test
    @DisplayName("TC-F2.4.2-04a: Chủ chuỗi, nhân viên cùng chi nhánh và hội viên chủ hồ sơ xem đúng ảnh")
    void authorizedReadersReceiveStoredJpeg() throws Exception {
        seedAvatar();
        for (var actor : List.of(identity(owner, UserRole.SUPER_ADMIN), identity(manager, UserRole.BRANCH_MANAGER),
                identity(receptionist, UserRole.RECEPTIONIST), identity(memberUser, UserRole.MEMBER))) {
            mvc.perform(get(path()).with(actor)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG)).andExpect(content().bytes(stored()));
        }
    }

    @Test
    @DisplayName("TC-F2.4.2-04b: Hội viên khác và nhân viên khác chi nhánh nhận 404, TRAINER nhận 403")
    void otherReadersCannotSeeAvatar() throws Exception {
        seedAvatar();
        mvc.perform(get(path()).with(identity(otherMember, UserRole.MEMBER))).andExpect(status().isNotFound());
        mvc.perform(get(path()).with(identity(otherStaff, UserRole.RECEPTIONIST))).andExpect(status().isNotFound());
        mvc.perform(get(path()).with(identity(trainer, UserRole.TRAINER))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-F2.4.2-05a: Chủ chuỗi không có dòng staff vẫn đổi được ảnh của chi nhánh khác")
    void ownerMayUploadAcrossBranches() throws Exception {
        jdbc.update("UPDATE members SET home_branch_id = ? WHERE id = ?", otherBranch, memberId);
        upload(owner, UserRole.SUPER_ADMIN, file(image(30, 30, "jpg", Color.RED))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT uploaded_by FROM member_avatars", Long.class)).isEqualTo(owner);
    }

    @Test
    @DisplayName("TC-F2.4.2-05b: MEMBER, TRAINER và nhân viên ngoài chi nhánh không thay được ảnh")
    void deniedWritersDoNotMutateAvatar() throws Exception {
        seedAvatar();
        byte[] original = stored();
        MockMultipartFile next = file(image(30, 30, "jpg", Color.BLUE));
        upload(memberUser, UserRole.MEMBER, next).andExpect(status().isForbidden());
        upload(trainer, UserRole.TRAINER, next).andExpect(status().isForbidden());
        upload(otherStaff, UserRole.RECEPTIONIST, next).andExpect(status().isNotFound());
        Long unassigned = user("unassigned@example.com", "RECEPTIONIST");
        upload(unassigned, UserRole.RECEPTIONIST, next).andExpect(status().isForbidden());
        assertThat(stored()).containsExactly(original);
        assertThat(jdbc.queryForObject("SELECT uploaded_by FROM member_avatars", Long.class)).isEqualTo(receptionist);
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "branch"})
    @DisplayName("TC-F2.4.2-05c: Nhân viên hoặc chi nhánh ngừng hoạt động không được đọc hay đổi ảnh")
    void inactiveAssignmentsAreDenied(String kind) throws Exception {
        seedAvatar();
        byte[] original = stored();
        if (kind.equals("staff")) {
            jdbc.update("UPDATE staff SET status = 'INACTIVE' WHERE user_id = ?", receptionist);
        } else {
            jdbc.update("UPDATE branches SET status = 'INACTIVE' WHERE id = ?", branch);
        }
        mvc.perform(get(path()).with(identity(receptionist, UserRole.RECEPTIONIST))).andExpect(status().isForbidden());
        upload(receptionist, UserRole.RECEPTIONIST, file(image(30, 30, "jpg", Color.BLUE)))
            .andExpect(status().isForbidden());
        assertThat(stored()).containsExactly(original);
    }

    @Test
    @DisplayName("TC-F2.4.2-06a: Hồ sơ chưa có ảnh trả 404")
    void missingAvatarIsNotFound() throws Exception {
        mvc.perform(get(path()).with(identity(memberUser, UserRole.MEMBER))).andExpect(status().isNotFound());
        assertThat(avatarCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("TC-F2.4.2-06b: Hồ sơ không tồn tại hoặc xóa mềm trả 404 cho GET và PUT")
    void missingOrDeletedProfilesAreNotFound(boolean deleted) throws Exception {
        if (deleted) {
            jdbc.update("UPDATE members SET deleted_at = now() WHERE id = ?", memberId);
        } else {
            memberId = Long.MAX_VALUE;
        }
        mvc.perform(get(path()).with(identity(receptionist, UserRole.RECEPTIONIST))).andExpect(status().isNotFound());
        upload(receptionist, UserRole.RECEPTIONIST, file(image(30, 30, "jpg", Color.RED)))
            .andExpect(status().isNotFound());
        assertThat(avatarCount()).isZero();
    }

    @Test
    @DisplayName("TC-F2.4.2-07a: Chưa đăng nhập không gọi được GET hoặc PUT ảnh")
    void unauthenticatedRequestsAreDenied() throws Exception {
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
        mvc.perform(multipart(HttpMethod.PUT, path()).file(file(image(30, 30, "jpg", Color.RED))))
            .andExpect(status().isUnauthorized());
        assertThat(avatarCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    @DisplayName("TC-F2.4.2-07b: ID sai kiểu hoặc không dương trả 400 có errors.id")
    void invalidIdsAreRejected(String id) throws Exception {
        String url = "/api/v1/members/" + id + "/avatar";
        mvc.perform(get(url).with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.id").exists());
        mvc.perform(multipart(HttpMethod.PUT, url).file(file(image(30, 30, "jpg", Color.RED)))
                .with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.id").exists());
        assertThat(avatarCount()).isZero();
    }

    @Test
    @DisplayName("TC-F2.4.2-07c: PUT thiếu trường multipart file bị từ chối 400")
    void missingMultipartFileIsRejected() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, path()).with(identity(receptionist, UserRole.RECEPTIONIST)))
            .andExpect(status().isBadRequest());
        assertThat(avatarCount()).isZero();
    }

    @Test
    @DisplayName("TC-F2.4.2-08a: Hồ sơ vừa tạo trả hasAvatar=false để giao diện không gọi API ảnh")
    void newProfileHasNoAvatar() throws Exception {
        mvc.perform(post("/api/v1/members").with(identity(receptionist, UserRole.RECEPTIONIST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Hội viên mới","phone":"0912345602"}
                    """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.hasAvatar").value(false));
        assertThat(avatarCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("TC-F2.4.2-08b: Chi tiết, danh sách và cập nhật hồ sơ trả đúng hasAvatar của từng hội viên")
    void profileResponsesReflectAvatarPresence(boolean hasAvatar) throws Exception {
        if (hasAvatar) { seedAvatar(); }
        jdbc.update("""
            INSERT INTO members(member_code,phone,home_branch_id,full_name)
            VALUES ('HV000902','0912345602',?,'Hội viên chưa có ảnh')
            """, branch);
        var actor = identity(receptionist, UserRole.RECEPTIONIST);
        mvc.perform(get("/api/v1/members/" + memberId).with(actor))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hasAvatar").value(hasAvatar))
            .andExpect(jsonPath("$.content").doesNotExist());
        mvc.perform(get("/api/v1/members").with(actor))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].memberCode").value("HV000902"))
            .andExpect(jsonPath("$.content[0].hasAvatar").value(false))
            .andExpect(jsonPath("$.content[1].id").value(memberId))
            .andExpect(jsonPath("$.content[1].hasAvatar").value(hasAvatar))
            .andExpect(jsonPath("$.content[1].content").doesNotExist());
        mvc.perform(put("/api/v1/members/" + memberId).with(actor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Tên mới","phone":"0912345601","status":"ACTIVE"}
                    """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hasAvatar").value(hasAvatar));
        mvc.perform(get("/api/v1/members").param("page", "1").param("size", "2").with(actor))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        assertThat(avatarCount()).isEqualTo(hasAvatar ? 1 : 0);
    }

    private String path() { return "/api/v1/members/" + memberId + "/avatar"; }

    private ResultActions upload(Long userId, UserRole role, MockMultipartFile file) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, path()).file(file).with(identity(userId, role)));
    }

    private void seedAvatar() throws Exception {
        upload(receptionist, UserRole.RECEPTIONIST, file(image(50, 50, "jpg", Color.RED)))
            .andExpect(status().isOk());
    }

    private byte[] stored() {
        return jdbc.queryForObject("SELECT content FROM member_avatars WHERE member_id = ?", byte[].class, memberId);
    }

    private int avatarCount() { return jdbc.queryForObject("SELECT count(*) FROM member_avatars", Integer.class); }

    private MockMultipartFile file(byte[] bytes) { return new MockMultipartFile("file", "avatar.jpg", "image/jpeg", bytes); }

    private static byte[] image(int width, int height, String format, Color color) throws Exception {
        var image = new BufferedImage(width, height, format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        var bytes = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, bytes)).isTrue();
        image.flush();
        return bytes.toByteArray();
    }

    private Long user(String email, String role) {
        return jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES (?,'test-only',?) RETURNING id",
            Long.class, email, role);
    }

    private void staff(Long userId, Long branchId, String code) {
        jdbc.update("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,?,'Nhân viên')", userId, branchId, code);
    }

    private RequestPostProcessor identity(Long userId, UserRole role) {
        return authentication(new UsernamePasswordAuthenticationToken(userId.toString(), "test-only",
            List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
