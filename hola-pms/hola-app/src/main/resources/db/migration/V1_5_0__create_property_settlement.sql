-- 프로퍼티 정산정보 테이블
CREATE TABLE htl_property_settlement (
    id                BIGSERIAL PRIMARY KEY,
    property_id       BIGINT       NOT NULL REFERENCES htl_property(id),
    country_type      VARCHAR(2)   NOT NULL,          -- 'KR' / 'US'
    account_number    VARCHAR(50),                     -- 계좌번호
    bank_name         VARCHAR(100),                    -- 은행명
    bank_code         VARCHAR(20),                     -- KR: 은행코드
    account_holder    VARCHAR(100),                    -- US: 예금주
    routing_number    VARCHAR(50),                     -- US: Routing Number
    swift_code        VARCHAR(50),                     -- US: SWIFT CODE
    settlement_day    VARCHAR(10),                     -- '1','5','10','15','20','25','LAST'
    bank_book_path    VARCHAR(500),                    -- KR: 통장사본 파일경로
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    use_yn            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    deleted_at        TIMESTAMP,
    UNIQUE (property_id, country_type)
);

CREATE INDEX idx_htl_property_settlement_property ON htl_property_settlement(property_id);
