-- hk_daily_attendance에 낙관적 잠금용 version 컬럼 추가
-- 동시 출퇴근 Race Condition 방지
ALTER TABLE hk_daily_attendance ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
