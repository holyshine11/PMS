package com.hola.reservation.booking.pg.kicc;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * KICC 이지페이 PG 설정
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "kicc")
public class KiccProperties {

    /** KICC 상점ID (테스트: T5102001) */
    @NotBlank
    private String mallId;

    /** KICC 시크릿키 (환경변수로 주입) */
    @NotBlank
    private String secretKey;

    /** KICC API 도메인 */
    @NotBlank
    private String apiDomain;

    /** 인증 완료 후 리턴 베이스 URL */
    @NotBlank
    private String returnBaseUrl;

    /** API 타임아웃 (초) */
    private int timeoutSeconds = 30;

    /** 빌키 카드인증 타입 (0: 번호+유효기간+생년월일+비밀번호) */
    private String billingCertType = "0";
}
