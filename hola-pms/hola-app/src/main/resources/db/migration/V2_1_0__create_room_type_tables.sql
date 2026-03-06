-- 객실 타입 테이블
CREATE TABLE rm_room_type (
    id              BIGSERIAL    PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    room_class_id   BIGINT       NOT NULL REFERENCES rm_room_class(id),
    room_type_code  VARCHAR(50)  NOT NULL,
    room_type_name  VARCHAR(200) NOT NULL,
    description     TEXT,
    room_size       NUMERIC(10,2),
    features        TEXT,
    max_adults      INTEGER      NOT NULL DEFAULT 2,
    max_children    INTEGER      NOT NULL DEFAULT 0,
    extra_bed_yn    BOOLEAN      DEFAULT FALSE,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      DEFAULT TRUE,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    CONSTRAINT uk_rm_room_type_property_code UNIQUE (property_id, room_type_code)
);

CREATE INDEX idx_rm_room_type_property ON rm_room_type(property_id);
CREATE INDEX idx_rm_room_type_class ON rm_room_type(room_class_id);

-- 객실 타입 코드 시퀀스
CREATE SEQUENCE rm_room_type_code_seq START WITH 1 INCREMENT BY 1;

-- 객실 타입 - 층/호수 매핑 테이블
CREATE TABLE rm_room_type_floor (
    id              BIGSERIAL    PRIMARY KEY,
    room_type_id    BIGINT       NOT NULL REFERENCES rm_room_type(id),
    floor_id        BIGINT       NOT NULL,
    room_number_id  BIGINT       NOT NULL,
    CONSTRAINT uk_rm_room_type_floor UNIQUE (room_type_id, room_number_id)
);

CREATE INDEX idx_rm_room_type_floor_type ON rm_room_type_floor(room_type_id);
CREATE INDEX idx_rm_room_type_floor_floor ON rm_room_type_floor(floor_id);
