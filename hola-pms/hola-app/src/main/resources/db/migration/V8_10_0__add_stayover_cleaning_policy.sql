-- ============================================================
-- V8_10_0: 스테이오버 청소 정책 (HkConfig 확장 + HkCleaningPolicy 신규)
-- ============================================================

-- 1) hk_config 컬럼 추가 (프로퍼티 기본 정책)
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS stayover_frequency INTEGER DEFAULT 1;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS stayover_enabled BOOLEAN DEFAULT false;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS turndown_enabled BOOLEAN DEFAULT false;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS dnd_policy VARCHAR(30) DEFAULT 'SKIP';
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS dnd_max_skip_days INTEGER DEFAULT 3;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS daily_task_gen_time VARCHAR(5) DEFAULT '06:00';
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS od_transition_time VARCHAR(5) DEFAULT '05:00';

COMMENT ON COLUMN hk_config.stayover_frequency IS '1일 스테이오버 횟수 (기본 1)';
COMMENT ON COLUMN hk_config.stayover_enabled IS '스테이오버 자동 생성 활성화';
COMMENT ON COLUMN hk_config.turndown_enabled IS '턴다운 자동 생성 활성화';
COMMENT ON COLUMN hk_config.dnd_policy IS 'DND 정책: SKIP, RETRY_AFTERNOON, FORCE_AFTER_DAYS';
COMMENT ON COLUMN hk_config.dnd_max_skip_days IS 'FORCE_AFTER_DAYS 정책 시 강제 청소까지 최대 일수';
COMMENT ON COLUMN hk_config.daily_task_gen_time IS '일일 작업 자동 생성 시각 (HH:mm)';
COMMENT ON COLUMN hk_config.od_transition_time IS 'OC→OD 일괄 전환 시각 (HH:mm)';

-- 2) hk_cleaning_policy 테이블 (룸타입별 오버라이드)
CREATE TABLE IF NOT EXISTS hk_cleaning_policy (
    id                  BIGSERIAL PRIMARY KEY,
    property_id         BIGINT NOT NULL,
    room_type_id        BIGINT NOT NULL,

    -- 오버라이드 필드 (null = 프로퍼티 기본값 사용)
    stayover_enabled    BOOLEAN,
    stayover_frequency  INTEGER,
    turndown_enabled    BOOLEAN,
    stayover_credit     DECIMAL(3,1),
    turndown_credit     DECIMAL(3,1),
    stayover_priority   VARCHAR(10),
    dnd_policy          VARCHAR(30),
    dnd_max_skip_days   INTEGER,
    note                VARCHAR(500),

    -- BaseEntity 표준 필드
    use_yn              BOOLEAN DEFAULT true NOT NULL,
    sort_order          INTEGER DEFAULT 0,
    created_at          TIMESTAMP DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_at          TIMESTAMP DEFAULT NOW(),
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,

    CONSTRAINT uk_hk_cleaning_policy UNIQUE (property_id, room_type_id)
);

CREATE INDEX IF NOT EXISTS idx_hk_cleaning_policy_property
    ON hk_cleaning_policy (property_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE hk_cleaning_policy IS '룸타입별 청소 정책 오버라이드 (null 필드 = 프로퍼티 HkConfig 기본값 사용)';

-- 3) hk_task DND 추적 컬럼
ALTER TABLE hk_task ADD COLUMN IF NOT EXISTS dnd_skipped BOOLEAN DEFAULT false;
ALTER TABLE hk_task ADD COLUMN IF NOT EXISTS dnd_skip_count INTEGER DEFAULT 0;
ALTER TABLE hk_task ADD COLUMN IF NOT EXISTS scheduled_time VARCHAR(5);

COMMENT ON COLUMN hk_task.dnd_skipped IS 'DND 스킵 여부';
COMMENT ON COLUMN hk_task.dnd_skip_count IS 'DND 연속 스킵 횟수';
COMMENT ON COLUMN hk_task.scheduled_time IS '예정 청소 시간대 (HH:mm)';

-- 4) htl_room_number DND 추적 컬럼
ALTER TABLE htl_room_number ADD COLUMN IF NOT EXISTS dnd_since DATE;
ALTER TABLE htl_room_number ADD COLUMN IF NOT EXISTS consecutive_dnd_days INTEGER DEFAULT 0;

COMMENT ON COLUMN htl_room_number.dnd_since IS 'DND 시작 날짜';
COMMENT ON COLUMN htl_room_number.consecutive_dnd_days IS 'DND 연속 일수';
