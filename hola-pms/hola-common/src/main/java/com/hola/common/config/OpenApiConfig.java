package com.hola.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger UI) 설정
 * 접근: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI holaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hola PMS API")
                        .description("올라 PMS - 예약관리 및 부킹엔진 API")
                        .version("v1.0.0"));
    }
}
