-- 부킹엔진 감사 로그 테이블
-- 외부 유입 예약의 전체 이력 추적 및 멱등성 관리
CREATE TABLE rsv_booking_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    master_reservation_id BIGINT,
    confirmation_no VARCHAR(10),
    event_type      VARCHAR(30) NOT NULL,
    channel         VARCHAR(20),
    request_payload TEXT,
    response_payload TEXT,
    client_ip       VARCHAR(45),
    user_agent      VARCHAR(500),
    idempotency_key VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 확인번호로 감사 로그 조회
CREATE INDEX idx_booking_audit_confirmation ON rsv_booking_audit_log(confirmation_no);

-- 멱등성 키로 중복 요청 확인
CREATE INDEX idx_booking_audit_idempotency ON rsv_booking_audit_log(idempotency_key);

-- 시간순 조회
CREATE INDEX idx_booking_audit_created_at ON rsv_booking_audit_log(created_at);
