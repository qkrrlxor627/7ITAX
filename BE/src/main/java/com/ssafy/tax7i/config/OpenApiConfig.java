package com.ssafy.tax7i.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 설정. 우측 상단 Authorize에 accessToken만 넣으면 모든 요청에 Bearer 헤더가 붙는다.
 * 토큰은 POST /api/auth/simple-login(로컬·성능측정 환경)으로 받는다
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI tax7iOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("7iTAX API")
                        .description("개인사업자 세무 자동화 BE. 인증: POST /api/auth/simple-login → accessToken을 Authorize에 입력")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
