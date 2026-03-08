-- 레이트 코드 - 유료 서비스 옵션 매핑 테이블
CREATE TABLE rt_rate_code_paid_service (
    id                      BIGSERIAL PRIMARY KEY,
    rate_code_id            BIGINT NOT NULL REFERENCES rt_rate_code(id) ON DELETE CASCADE,
    paid_service_option_id  BIGINT NOT NULL REFERENCES rm_paid_service_option(id),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (rate_code_id, paid_service_option_id)
);

CREATE INDEX idx_rt_rate_code_paid_service_rate ON rt_rate_code_paid_service(rate_code_id);
