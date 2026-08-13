package com.stockdemy.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String JWT_SCHEME_NAME = "JWT";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
      // API 메타정보
      .info(new Info()
        .title("Stockdemy API")
        .description("AI 주식 일지 서비스 API 명세")
        .version("v0.0.1"))
      // JWT Bearer 토큰 인증 지원
      .components(new Components()
        .addSecuritySchemes(JWT_SCHEME_NAME, new SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")));
  }
}
