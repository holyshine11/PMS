-- 당일예약 마감시간(Booking Cutoff) 설정
-- 컬럼이 이미 존재하면(이전 실행 잔여) 기본값만 변경, 없으면 추가
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='htl_property' AND column_name='same_day_booking_enabled') THEN
        ALTER TABLE htl_property ADD COLUMN same_day_booking_enabled BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='htl_property' AND column_name='same_day_cutoff_time') THEN
        ALTER TABLE htl_property ADD COLUMN same_day_cutoff_time INTEGER NOT NULL DEFAULT 1080;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='htl_property' AND column_name='walk_in_override') THEN
        ALTER TABLE htl_property ADD COLUMN walk_in_override BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
END $$;

-- 기존 시간 단위(0-23) 데이터를 분 단위로 보정 (이전 버전에서 마이그레이션된 경우)
UPDATE htl_property SET same_day_cutoff_time = same_day_cutoff_time * 60
WHERE same_day_cutoff_time BETWEEN 0 AND 23;

-- 기본값 1080분(18:00)으로 설정
ALTER TABLE htl_property ALTER COLUMN same_day_cutoff_time SET DEFAULT 1080;

COMMENT ON COLUMN htl_property.same_day_booking_enabled IS '당일 예약 허용 여부';
COMMENT ON COLUMN htl_property.same_day_cutoff_time IS '당일 예약 마감시간 (분 단위, 0-1410, 30분 간격)';
COMMENT ON COLUMN htl_property.walk_in_override IS '어드민 PMS 워크인 마감 면제 여부';
