-- =============================================
-- V8.1.0: 하우스키핑 설정 테이블
-- =============================================

CREATE TABLE hk_config (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL UNIQUE REFERENCES htl_property(id),

    -- 프로세스 설정
    inspection_required     BOOLEAN DEFAULT FALSE,      -- 검수 필수 여부
    auto_create_checkout    BOOLEAN DEFAULT TRUE,       -- 체크아웃 시 자동 작업 생성
    auto_create_stayover    BOOLEAN DEFAULT FALSE,      -- Stayover 자동 생성

    -- 크레딧 기본값
    default_checkout_credit DECIMAL(3,1) DEFAULT 1.0,
    default_stayover_credit DECIMAL(3,1) DEFAULT 0.5,
    default_turndown_credit DECIMAL(3,1) DEFAULT 0.3,
    default_deep_clean_credit DECIMAL(3,1) DEFAULT 2.0,

    -- Rush 판정 기준
    rush_threshold_minutes  INTEGER DEFAULT 120,        -- 체크인 N분 전이면 RUSH

    -- BaseEntity 공통
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

COMMENT ON TABLE hk_config IS '하우스키핑 프로퍼티별 설정';
COMMENT ON COLUMN hk_config.inspection_required IS '검수(Inspection) 필수 여부';
COMMENT ON COLUMN hk_config.auto_create_checkout IS '체크아웃 시 HK 작업 자동 생성 여부';
COMMENT ON COLUMN hk_config.auto_create_stayover IS 'Stayover HK 작업 자동 생성 여부';
COMMENT ON COLUMN hk_config.rush_threshold_minutes IS '체크인 N분 전이면 RUSH 우선순위 부여';
