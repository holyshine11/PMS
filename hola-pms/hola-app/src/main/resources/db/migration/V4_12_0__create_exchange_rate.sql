-- 환율 테이블
CREATE TABLE rsv_exchange_rate (
    id              BIGSERIAL       PRIMARY KEY,
    currency_code   VARCHAR(3)      NOT NULL,
    rate_date       DATE            NOT NULL,
    rate_value      NUMERIC(18, 8)  NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50)     NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(50)     NOT NULL DEFAULT 'SYSTEM',
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    UNIQUE(currency_code, rate_date)
);

COMMENT ON TABLE rsv_exchange_rate IS '환율 테이블 (기준통화: KRW)';
COMMENT ON COLUMN rsv_exchange_rate.currency_code IS '통화 코드 (USD, JPY, CNY)';
COMMENT ON COLUMN rsv_exchange_rate.rate_date IS '적용 날짜';
COMMENT ON COLUMN rsv_exchange_rate.rate_value IS '1 KRW = ? 대상통화';

-- 기본 환율 데이터 (2026-03-15 기준 참고값)
INSERT INTO rsv_exchange_rate (currency_code, rate_date, rate_value, created_by) VALUES
('USD', '2026-03-01', 0.00069000, 'SYSTEM'),
('JPY', '2026-03-01', 0.10300000, 'SYSTEM'),
('CNY', '2026-03-01', 0.00500000, 'SYSTEM');
