-- 프로퍼티/객실 이미지 관리 테이블
CREATE TABLE IF NOT EXISTS htl_property_image (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    image_type      VARCHAR(20)  NOT NULL,   -- PROPERTY, ROOM_TYPE, FACILITY, EXTERIOR
    reference_id    BIGINT,                   -- 객실타입 ID 등 (image_type에 따라)
    image_path      VARCHAR(500) NOT NULL,
    image_name      VARCHAR(200),
    alt_text        VARCHAR(300),
    sort_order      INT DEFAULT 0,
    use_yn          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_property_image_property ON htl_property_image(property_id);
CREATE INDEX idx_property_image_type ON htl_property_image(property_id, image_type);

COMMENT ON TABLE htl_property_image IS '프로퍼티/객실 이미지';
COMMENT ON COLUMN htl_property_image.image_type IS 'PROPERTY: 숙소, ROOM_TYPE: 객실, FACILITY: 시설, EXTERIOR: 외관';
COMMENT ON COLUMN htl_property_image.reference_id IS '객실타입 ID 등 (image_type=ROOM_TYPE일 때)';
