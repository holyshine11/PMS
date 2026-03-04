-- 취소 수수료 정책 테이블
CREATE TABLE htl_cancellation_fee (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    checkin_basis   VARCHAR(10)  NOT NULL,
    days_before     INTEGER,
    fee_amount      NUMERIC(15,2) NOT NULL DEFAULT 0,
    fee_type        VARCHAR(20)  NOT NULL DEFAULT 'PERCENTAGE',
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    use_yn          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_htl_cancellation_fee_property ON htl_cancellation_fee(property_id);
