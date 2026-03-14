package com.hola.reservation.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 부킹엔진 감사 로그
 * - 외부 유입 예약의 전체 이력 추적
 * - 멱등성 키 기반 중복 요청 방지
 */
@Entity
@Table(name = "rsv_booking_audit_log",
        indexes = {
                @Index(name = "idx_booking_audit_confirmation", columnList = "confirmation_no"),
                @Index(name = "idx_booking_audit_idempotency", columnList = "idempotency_key"),
                @Index(name = "idx_booking_audit_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_reservation_id")
    private Long masterReservationId;

    @Column(name = "confirmation_no", length = 10)
    private String confirmationNo;

    /** BOOKING_CREATED, PAYMENT_SUCCESS, PAYMENT_FAILED, BOOKING_EXPIRED */
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    /** WEBSITE, OTA 등 (예약 채널 타입) */
    @Column(name = "channel", length = 20)
    private String channel;

    /** 요청 JSON 스냅샷 */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /** 응답 JSON 스냅샷 */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** 중복 요청 방지 키 (UUID) */
    @Column(name = "idempotency_key", length = 50)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public BookingAuditLog(Long masterReservationId, String confirmationNo,
                           String eventType, String channel,
                           String requestPayload, String responsePayload,
                           String clientIp, String userAgent,
                           String idempotencyKey) {
        this.masterReservationId = masterReservationId;
        this.confirmationNo = confirmationNo;
        this.eventType = eventType;
        this.channel = channel;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.idempotencyKey = idempotencyKey;
    }
}
