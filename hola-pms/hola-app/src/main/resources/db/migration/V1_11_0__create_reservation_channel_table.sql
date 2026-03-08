-- 예약채널 테이블
CREATE TABLE htl_reservation_channel (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    channel_code    VARCHAR(20)  NOT NULL,
    channel_name    VARCHAR(200) NOT NULL,
    channel_type    VARCHAR(20)  NOT NULL DEFAULT 'WALK_IN',
    description_ko  TEXT,
    description_en  TEXT,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (property_id, channel_code)
);

CREATE INDEX idx_htl_reservation_channel_property ON htl_reservation_channel(property_id);

COMMENT ON TABLE htl_reservation_channel IS '예약채널';
COMMENT ON COLUMN htl_reservation_channel.property_id IS '프로퍼티 ID';
COMMENT ON COLUMN htl_reservation_channel.channel_code IS '채널 코드';
COMMENT ON COLUMN htl_reservation_channel.channel_name IS '채널명';
COMMENT ON COLUMN htl_reservation_channel.channel_type IS '채널 유형 (WALK_IN/PHONE/EMAIL/OTA/B2B/WEBSITE)';
COMMENT ON COLUMN htl_reservation_channel.description_ko IS '설명 (국문)';
COMMENT ON COLUMN htl_reservation_channel.description_en IS '설명 (영문)';
