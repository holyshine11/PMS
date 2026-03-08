-- 레이트 코드 테이블
CREATE TABLE rt_rate_code (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    rate_code       VARCHAR(50)  NOT NULL,
    rate_name_ko    VARCHAR(200) NOT NULL,
    rate_name_en    VARCHAR(200),
    rate_category   VARCHAR(20)  NOT NULL DEFAULT 'ROOM_ONLY',
    market_code_id  BIGINT       REFERENCES htl_market_code(id),
    currency        VARCHAR(10)  NOT NULL DEFAULT 'KRW',
    sale_start_date DATE         NOT NULL,
    sale_end_date   DATE         NOT NULL,
    min_stay_days   INTEGER      NOT NULL DEFAULT 1,
    max_stay_days   INTEGER      NOT NULL DEFAULT 365,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    UNIQUE (property_id, rate_code)
);

CREATE INDEX idx_rt_rate_code_property ON rt_rate_code(property_id);

COMMENT ON TABLE rt_rate_code IS '레이트 코드';
COMMENT ON COLUMN rt_rate_code.property_id IS '프로퍼티 ID';
COMMENT ON COLUMN rt_rate_code.rate_code IS '레이트 코드';
COMMENT ON COLUMN rt_rate_code.rate_name_ko IS '레이트 코드 국문명';
COMMENT ON COLUMN rt_rate_code.rate_name_en IS '레이트 코드 영문명';
COMMENT ON COLUMN rt_rate_code.rate_category IS '레이트 카테고리 (ROOM_ONLY/PACKAGE)';
COMMENT ON COLUMN rt_rate_code.market_code_id IS '마켓코드 ID';
COMMENT ON COLUMN rt_rate_code.currency IS '통화 (KRW/USD)';
COMMENT ON COLUMN rt_rate_code.sale_start_date IS '판매 시작일';
COMMENT ON COLUMN rt_rate_code.sale_end_date IS '판매 종료일';
COMMENT ON COLUMN rt_rate_code.min_stay_days IS '최소 숙박일수';
COMMENT ON COLUMN rt_rate_code.max_stay_days IS '최대 숙박일수';

-- 레이트 코드 - 객실 타입 매핑 테이블
CREATE TABLE rt_rate_code_room_type (
    id              BIGSERIAL PRIMARY KEY,
    rate_code_id    BIGINT    NOT NULL REFERENCES rt_rate_code(id) ON DELETE CASCADE,
    room_type_id    BIGINT    NOT NULL REFERENCES rm_room_type(id),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (rate_code_id, room_type_id)
);

CREATE INDEX idx_rt_rate_code_room_type_rate ON rt_rate_code_room_type(rate_code_id);

COMMENT ON TABLE rt_rate_code_room_type IS '레이트 코드 - 객실 타입 매핑';
COMMENT ON COLUMN rt_rate_code_room_type.rate_code_id IS '레이트 코드 ID';
COMMENT ON COLUMN rt_rate_code_room_type.room_type_id IS '객실 타입 ID';
