-- 프로퍼티 이용약관 테이블
CREATE TABLE IF NOT EXISTS htl_property_terms (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    terms_type      VARCHAR(30)  NOT NULL,    -- BOOKING: 예약약관, PRIVACY: 개인정보, CANCELLATION: 취소환불, HOUSE_RULES: 이용규칙
    title_ko        VARCHAR(200) NOT NULL,
    title_en        VARCHAR(200),
    content_ko      TEXT NOT NULL,
    content_en      TEXT,
    version         VARCHAR(20)  DEFAULT '1.0',
    required        BOOLEAN      DEFAULT TRUE,
    sort_order      INT DEFAULT 0,
    use_yn          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_property_terms_property ON htl_property_terms(property_id);

COMMENT ON TABLE htl_property_terms IS '프로퍼티 이용약관';
COMMENT ON COLUMN htl_property_terms.terms_type IS 'BOOKING: 예약약관, PRIVACY: 개인정보, CANCELLATION: 취소환불, HOUSE_RULES: 이용규칙';
COMMENT ON COLUMN htl_property_terms.required IS '필수 동의 여부';
