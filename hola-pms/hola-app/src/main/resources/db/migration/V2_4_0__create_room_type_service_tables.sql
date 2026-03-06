-- 객실 타입 - 무료 서비스 옵션 매핑 테이블
CREATE TABLE rm_room_type_free_service (
    id                      BIGSERIAL   PRIMARY KEY,
    room_type_id            BIGINT      NOT NULL REFERENCES rm_room_type(id),
    free_service_option_id  BIGINT      NOT NULL REFERENCES rm_free_service_option(id),
    quantity                INTEGER     NOT NULL DEFAULT 1,
    UNIQUE (room_type_id, free_service_option_id)
);

CREATE INDEX idx_rm_rt_free_svc_room_type ON rm_room_type_free_service(room_type_id);

COMMENT ON TABLE rm_room_type_free_service IS '객실 타입 - 무료 서비스 옵션 매핑';

-- 객실 타입 - 유료 서비스 옵션 매핑 테이블
CREATE TABLE rm_room_type_paid_service (
    id                      BIGSERIAL   PRIMARY KEY,
    room_type_id            BIGINT      NOT NULL REFERENCES rm_room_type(id),
    paid_service_option_id  BIGINT      NOT NULL REFERENCES rm_paid_service_option(id),
    quantity                INTEGER     NOT NULL DEFAULT 1,
    UNIQUE (room_type_id, paid_service_option_id)
);

CREATE INDEX idx_rm_rt_paid_svc_room_type ON rm_room_type_paid_service(room_type_id);

COMMENT ON TABLE rm_room_type_paid_service IS '객실 타입 - 유료 서비스 옵션 매핑';
