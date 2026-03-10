-- 얼리 체크인 / 레이트 체크아웃 요금 정책 테이블
CREATE TABLE htl_early_late_fee_policy (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    policy_type     VARCHAR(20) NOT NULL,       -- EARLY_CHECKIN, LATE_CHECKOUT
    time_from       VARCHAR(10) NOT NULL,       -- 시작시간 (HH:mm)
    time_to         VARCHAR(10) NOT NULL,       -- 종료시간 (HH:mm)
    fee_type        VARCHAR(10) NOT NULL,       -- PERCENT, FIXED
    fee_value       NUMERIC(15, 2) NOT NULL,    -- 비율(%) 또는 고정금액
    description     VARCHAR(200),               -- 설명
    sort_order      INTEGER DEFAULT 0,
    use_yn          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_early_late_policy_property ON htl_early_late_fee_policy(property_id, policy_type);

COMMENT ON TABLE htl_early_late_fee_policy IS '얼리체크인/레이트체크아웃 요금 정책';
COMMENT ON COLUMN htl_early_late_fee_policy.policy_type IS 'EARLY_CHECKIN: 얼리 체크인, LATE_CHECKOUT: 레이트 체크아웃';
COMMENT ON COLUMN htl_early_late_fee_policy.fee_type IS 'PERCENT: 비율(%), FIXED: 고정금액';
