package vn.edu.hust.nhom6.gym.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Thông tin chung của tài liệu API (Swagger UI tại /swagger-ui/index.html). */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_JWT = "bearerJwt";

    @Bean
    OpenAPI gymOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("API Web quản lý chuỗi phòng tập gym")
                .description("IT3180 – Nhóm 6. Mọi API nghiệp vụ có tiền tố /api/v1.")
                .version("v1"))
            .components(new Components().addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
