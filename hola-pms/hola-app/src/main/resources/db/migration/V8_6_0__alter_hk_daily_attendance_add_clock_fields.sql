-- 하우스키핑 일일 출근부: 모바일 출퇴근 필드 추가
ALTER TABLE hk_daily_attendance ADD COLUMN clock_in_at TIMESTAMP;
ALTER TABLE hk_daily_attendance ADD COLUMN clock_out_at TIMESTAMP;
ALTER TABLE hk_daily_attendance ADD COLUMN attendance_status VARCHAR(20) DEFAULT 'BEFORE_WORK';

COMMENT ON COLUMN hk_daily_attendance.clock_in_at IS '출근 시각';
COMMENT ON COLUMN hk_daily_attendance.clock_out_at IS '퇴근 시각';
COMMENT ON COLUMN hk_daily_attendance.attendance_status IS '근태 상태 (BEFORE_WORK/WORKING/LEFT/DAY_OFF)';
