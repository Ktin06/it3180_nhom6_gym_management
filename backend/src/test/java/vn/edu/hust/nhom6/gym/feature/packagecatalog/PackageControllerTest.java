package vn.edu.hust.nhom6.gym.feature.packagecatalog;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.GlobalExceptionHandler;
import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.controller.PackageController;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.controller.PackageExceptionHandler;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.service.PackageCatalogService;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@WebMvcTest(PackageController.class)
@ContextConfiguration(classes = {PackageController.class, PackageExceptionHandler.class,
    GlobalExceptionHandler.class, SecurityConfig.class})
class PackageControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PackageCatalogService service;

    @BeforeEach
    void fixtures() {
        when(service.list(any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
    }

    @Test
    void defaultContract() throws Exception {
        mvc.perform(get("/api/v1/packages").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
        verify(service).list(any(), isNull(), eq(PackageStatus.ACTIVE), eq(0), eq(20));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,101", "page,text", "size,2147483648", "type,INVALID", "status,INVALID"})
    void rejectsInvalidQueryWithFieldErrors(String field, String value) throws Exception {
        mvc.perform(get("/api/v1/packages").param(field, value)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BRANCH_MANAGER"))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.errors." + field).isNotEmpty());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEMBER", "TRAINER"})
    void rejectsDeniedRole(String role) throws Exception {
        mvc.perform(get("/api/v1/packages").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void missingAndInvalidBearerAreUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/packages")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/packages").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
