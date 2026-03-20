-- =============================================
-- V8.5.0: 하우스키핑 구역(Section) 및 일일 출근부
-- =============================================

-- 1. 구역 정의
CREATE TABLE hk_section (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),

    section_name    VARCHAR(50) NOT NULL,       -- "3~5층", "스위트동" 등
    section_code    VARCHAR(20),                -- 선택적 코드
    max_credits     DECIMAL(5,1),               -- 구역별 1인 크레딧 상한 (선택)

    -- BaseEntity 공통
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_hk_section ON hk_section(property_id, section_name) WHERE deleted_at IS NULL;
CREATE INDEX idx_hk_section_property ON hk_section(property_id);

COMMENT ON TABLE hk_section IS '하우스키핑 구역 (층 그룹)';
COMMENT ON COLUMN hk_section.section_name IS '구역 이름 (예: 3~5층, 스위트동)';
COMMENT ON COLUMN hk_section.max_credits IS '구역별 1인 일일 크레딧 상한';

-- 2. 구역-층 매핑 (N:M)
CREATE TABLE hk_section_floor (
    id              BIGSERIAL PRIMARY KEY,
    section_id      BIGINT NOT NULL REFERENCES hk_section(id) ON DELETE CASCADE,
    floor_id        BIGINT NOT NULL REFERENCES htl_floor(id),

    UNIQUE (section_id, floor_id)
);

CREATE INDEX idx_hk_section_floor_section ON hk_section_floor(section_id);
CREATE INDEX idx_hk_section_floor_floor ON hk_section_floor(floor_id);

COMMENT ON TABLE hk_section_floor IS '구역-층 매핑';

-- 3. 구역 기본 담당자 (N:M)
CREATE TABLE hk_section_housekeeper (
    id              BIGSERIAL PRIMARY KEY,
    section_id      BIGINT NOT NULL REFERENCES hk_section(id) ON DELETE CASCADE,
    housekeeper_id  BIGINT NOT NULL REFERENCES sys_admin_user(id),
    is_primary      BOOLEAN DEFAULT TRUE,       -- 주담당 여부

    UNIQUE (section_id, housekeeper_id)
);

CREATE INDEX idx_hk_section_hk_section ON hk_section_housekeeper(section_id);
CREATE INDEX idx_hk_section_hk_hk ON hk_section_housekeeper(housekeeper_id);

COMMENT ON TABLE hk_section_housekeeper IS '구역 기본 담당자';
COMMENT ON COLUMN hk_section_housekeeper.is_primary IS '주담당 여부 (true=주, false=부)';

-- 4. 일일 출근부
CREATE TABLE hk_daily_attendance (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    attendance_date DATE NOT NULL,
    housekeeper_id  BIGINT NOT NULL REFERENCES sys_admin_user(id),
    is_available    BOOLEAN DEFAULT TRUE,       -- 가용 여부
    shift_type      VARCHAR(10) DEFAULT 'DAY',  -- DAY, EVENING, NIGHT
    note            VARCHAR(200),               -- 비고 (반차, 조퇴 등)

    -- BaseEntity 공통
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),

    UNIQUE (property_id, attendance_date, housekeeper_id)
);

CREATE INDEX idx_hk_attendance_property_date ON hk_daily_attendance(property_id, attendance_date);

COMMENT ON TABLE hk_daily_attendance IS '하우스키핑 일일 출근부';
COMMENT ON COLUMN hk_daily_attendance.shift_type IS '근무 시간대: DAY, EVENING, NIGHT';
COMMENT ON COLUMN hk_daily_attendance.is_available IS '오늘 가용 여부 (false=결근/휴무)';
