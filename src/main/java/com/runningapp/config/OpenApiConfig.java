package com.runningapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정
 *
 * /swagger-ui.html 에서 API 문서 확인
 * Bearer JWT 인증 스킴 추가 → Swagger UI에서 "Authorize" 버튼으로 토큰 입력 가능
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Running App API")
                        .description("나이키 러닝 앱 스타일 - 러닝 활동 저장, 챌린지/플랜 추천 백엔드 API\n\n" +
                                "**인증 방법**: 1) 회원가입 또는 로그인 → 2) accessToken 복사 → 3) Swagger 상단 'Authorize' 클릭 → Bearer {토큰} 입력")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
