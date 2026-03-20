-- 하우스키핑 휴무일 관리 테이블
CREATE TABLE hk_day_off (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL,
    housekeeper_id BIGINT NOT NULL,
    day_off_date DATE NOT NULL,
    day_off_type VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note VARCHAR(200),
    approved_by VARCHAR(50),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),

    CONSTRAINT uk_hk_day_off UNIQUE (property_id, housekeeper_id, day_off_date)
);

COMMENT ON TABLE hk_day_off IS '하우스키핑 휴무일 관리';
COMMENT ON COLUMN hk_day_off.day_off_type IS '휴무 유형 (REGULAR: 정기, REQUESTED: 요청, APPROVED: 관리자 직접등록)';
COMMENT ON COLUMN hk_day_off.status IS '상태 (PENDING: 대기, APPROVED: 승인, REJECTED: 거절)';
COMMENT ON COLUMN hk_day_off.approved_by IS '승인/거절 처리자 loginId';
