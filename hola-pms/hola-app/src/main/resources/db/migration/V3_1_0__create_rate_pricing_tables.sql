-- 요금 정보 테이블
CREATE TABLE rt_rate_pricing (
    id                  BIGSERIAL PRIMARY KEY,
    rate_code_id        BIGINT       NOT NULL REFERENCES rt_rate_code(id) ON DELETE CASCADE,
    day_mon             BOOLEAN      NOT NULL DEFAULT TRUE,
    day_tue             BOOLEAN      NOT NULL DEFAULT TRUE,
    day_wed             BOOLEAN      NOT NULL DEFAULT TRUE,
    day_thu             BOOLEAN      NOT NULL DEFAULT TRUE,
    day_fri             BOOLEAN      NOT NULL DEFAULT TRUE,
    day_sat             BOOLEAN      NOT NULL DEFAULT TRUE,
    day_sun             BOOLEAN      NOT NULL DEFAULT TRUE,
    currency            VARCHAR(10)  NOT NULL DEFAULT 'KRW',
    base_supply_price   NUMERIC(15,2) NOT NULL DEFAULT 0,
    base_tax            NUMERIC(15,2) NOT NULL DEFAULT 0,
    base_total          NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP
);

CREATE INDEX idx_rt_rate_pricing_rate_code ON rt_rate_pricing(rate_code_id);

-- 인원별 추가 요금 테이블
CREATE TABLE rt_rate_pricing_person (
    id                  BIGSERIAL PRIMARY KEY,
    rate_pricing_id     BIGINT       NOT NULL REFERENCES rt_rate_pricing(id) ON DELETE CASCADE,
    person_type         VARCHAR(10)  NOT NULL,
    person_seq          INTEGER      NOT NULL,
    supply_price        NUMERIC(15,2) NOT NULL DEFAULT 0,
    tax                 NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_price         NUMERIC(15,2) NOT NULL DEFAULT 0,
    UNIQUE (rate_pricing_id, person_type, person_seq)
);

CREATE INDEX idx_rt_rate_pricing_person_pricing ON rt_rate_pricing_person(rate_pricing_id);

-- 레이트 코드 테이블에 Down/Up sale 컬럼 추가
ALTER TABLE rt_rate_code ADD COLUMN down_up_sign VARCHAR(1);
ALTER TABLE rt_rate_code ADD COLUMN down_up_value NUMERIC(15,2);
ALTER TABLE rt_rate_code ADD COLUMN down_up_unit VARCHAR(10);
ALTER TABLE rt_rate_code ADD COLUMN rounding_decimal_point INTEGER DEFAULT 0;
ALTER TABLE rt_rate_code ADD COLUMN rounding_digits INTEGER DEFAULT 0;
ALTER TABLE rt_rate_code ADD COLUMN rounding_method VARCHAR(20);
