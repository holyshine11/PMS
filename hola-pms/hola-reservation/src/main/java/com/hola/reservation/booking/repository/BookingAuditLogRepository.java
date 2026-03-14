package com.hola.reservation.booking.repository;

import com.hola.reservation.booking.entity.BookingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingAuditLogRepository extends JpaRepository<BookingAuditLog, Long> {

    /**
     * 멱등성 키로 기존 처리 여부 확인
     */
    Optional<BookingAuditLog> findByIdempotencyKeyAndEventType(String idempotencyKey, String eventType);
}
