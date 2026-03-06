-- 객실 클래스(그룹) 테이블
CREATE TABLE rm_room_class (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    room_class_code VARCHAR(50)  NOT NULL,
    room_class_name VARCHAR(200) NOT NULL,
    description     TEXT,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (property_id, room_class_code)
);

CREATE INDEX idx_rm_room_class_property ON rm_room_class(property_id);

-- 객실 클래스 코드 시퀀스
CREATE SEQUENCE rm_room_class_code_seq START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE rm_room_class IS '객실 클래스(그룹)';
COMMENT ON COLUMN rm_room_class.property_id IS '프로퍼티 ID';
COMMENT ON COLUMN rm_room_class.room_class_code IS '객실 클래스 코드';
COMMENT ON COLUMN rm_room_class.room_class_name IS '객실 클래스명';
COMMENT ON COLUMN rm_room_class.description IS '설명';
