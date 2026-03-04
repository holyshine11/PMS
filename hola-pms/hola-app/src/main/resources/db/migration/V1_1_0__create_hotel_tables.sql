-- =============================================
-- V1.1.0: 호텔/프로퍼티 관리 테이블 (M01)
-- htl_hotel, htl_property, htl_floor, htl_room_number, htl_market_code
-- =============================================

-- 호텔 마스터
CREATE TABLE htl_hotel (
    id              BIGSERIAL PRIMARY KEY,
    hotel_code      VARCHAR(20)  NOT NULL UNIQUE,
    hotel_name      VARCHAR(200) NOT NULL,
    hotel_type      VARCHAR(20),
    star_rating     VARCHAR(10),
    zip_code        VARCHAR(10),
    address         VARCHAR(500),
    address_detail  VARCHAR(500),
    phone           VARCHAR(20),
    fax             VARCHAR(20),
    email           VARCHAR(200),
    website         VARCHAR(300),
    description     TEXT,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP
);

-- 프로퍼티
CREATE TABLE htl_property (
    id              BIGSERIAL PRIMARY KEY,
    hotel_id        BIGINT       NOT NULL REFERENCES htl_hotel(id),
    property_code   VARCHAR(20)  NOT NULL,
    property_name   VARCHAR(200) NOT NULL,
    property_type   VARCHAR(20),
    check_in_time   VARCHAR(10)  DEFAULT '15:00',
    check_out_time  VARCHAR(10)  DEFAULT '11:00',
    total_rooms     INTEGER      DEFAULT 0,
    phone           VARCHAR(20),
    email           VARCHAR(200),
    description     TEXT,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (hotel_id, property_code)
);

-- 층
CREATE TABLE htl_floor (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    floor_number    INTEGER      NOT NULL,
    floor_name      VARCHAR(50),
    description     TEXT,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (property_id, floor_number)
);

-- 호수
CREATE TABLE htl_room_number (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    floor_id        BIGINT       REFERENCES htl_floor(id),
    room_number     VARCHAR(20)  NOT NULL,
    room_code       VARCHAR(20),
    description     TEXT,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (property_id, room_number)
);

-- 마켓코드
CREATE TABLE htl_market_code (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    market_code     VARCHAR(20)  NOT NULL,
    market_name     VARCHAR(200) NOT NULL,
    description     TEXT,
    sort_order      INTEGER      DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (property_id, market_code)
);

-- 인덱스
CREATE INDEX idx_htl_property_hotel ON htl_property(hotel_id);
CREATE INDEX idx_htl_floor_property ON htl_floor(property_id);
CREATE INDEX idx_htl_room_number_property ON htl_room_number(property_id);
CREATE INDEX idx_htl_room_number_floor ON htl_room_number(floor_id);
CREATE INDEX idx_htl_market_code_property ON htl_market_code(property_id);
