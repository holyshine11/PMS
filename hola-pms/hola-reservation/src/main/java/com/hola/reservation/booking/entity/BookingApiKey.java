package com.hola.reservation.booking.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 부킹엔진 API Key 관리
 * - 벤더별 API Key 발급/관리
 * - API-KEY + VENDOR-ID 헤더로 인증
 */
@Entity
@Table(name = "rsv_booking_api_key",
        uniqueConstraints = @UniqueConstraint(columnNames = "vendor_id"))
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BookingApiKey extends BaseEntity {

    /** 벤더 식별자 (헤더: VENDOR-ID) */
    @Column(name = "vendor_id", nullable = false, length = 50)
    private String vendorId;

    /** 벤더명 */
    @Column(name = "vendor_name", nullable = false, length = 100)
    private String vendorName;

    /** API Key (BCrypt 해시 저장) */
    @Column(name = "api_key_hash", nullable = false, length = 200)
    private String apiKeyHash;

    /** 활성 상태 */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** 만료일시 (null이면 무기한) */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 설명/메모 */
    @Column(name = "description", length = 500)
    private String description;

    /** 허용 IP 목록 (콤마 구분, null이면 전체 허용) */
    @Column(name = "allowed_ips", length = 500)
    private String allowedIps;

    /** 마지막 사용 일시 */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /** 활성 상태이고 만료되지 않았는지 확인 */
    public boolean isValid() {
        if (!Boolean.TRUE.equals(active)) return false;
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) return false;
        return true;
    }

    /** IP 허용 여부 확인 */
    public boolean isIpAllowed(String clientIp) {
        if (allowedIps == null || allowedIps.isBlank()) return true;
        String[] ips = allowedIps.split(",");
        for (String ip : ips) {
            if (ip.trim().equals(clientIp)) return true;
        }
        return false;
    }

    /** 마지막 사용 시간 갱신 */
    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
