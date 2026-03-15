-- 부킹엔진 API Key 관리 테이블
CREATE TABLE rsv_booking_api_key (
    id              BIGSERIAL       PRIMARY KEY,
    vendor_id       VARCHAR(50)     NOT NULL UNIQUE,
    vendor_name     VARCHAR(100)    NOT NULL,
    api_key_hash    VARCHAR(200)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMP,
    description     VARCHAR(500),
    allowed_ips     VARCHAR(500),
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50)     NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(50)     NOT NULL DEFAULT 'SYSTEM',
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order      INTEGER         NOT NULL DEFAULT 0
);

COMMENT ON TABLE rsv_booking_api_key IS '부킹엔진 API Key 관리';
COMMENT ON COLUMN rsv_booking_api_key.vendor_id IS '벤더 식별자 (VENDOR-ID 헤더)';
COMMENT ON COLUMN rsv_booking_api_key.vendor_name IS '벤더명';
COMMENT ON COLUMN rsv_booking_api_key.api_key_hash IS 'API Key BCrypt 해시';
COMMENT ON COLUMN rsv_booking_api_key.active IS '활성 상태';
COMMENT ON COLUMN rsv_booking_api_key.expires_at IS '만료일시 (NULL이면 무기한)';
COMMENT ON COLUMN rsv_booking_api_key.allowed_ips IS '허용 IP 목록 (콤마 구분)';
COMMENT ON COLUMN rsv_booking_api_key.last_used_at IS '마지막 사용 일시';

-- 테스트용 벤더 등록 (API Key 평문: hola-booking-test-key-2026)
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO rsv_booking_api_key (vendor_id, vendor_name, api_key_hash, description, created_by)
VALUES ('TEST_VENDOR', '테스트 벤더', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '개발/테스트용 벤더 키', 'SYSTEM');
