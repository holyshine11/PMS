-- 부킹엔진 예약 조회 성능 개선용 인덱스
-- findByEmailAndLastName: LOWER() 함수 사용 시 일반 인덱스 활용 불가하므로 expression index 필요

-- 이메일 + 영문 성(lastName) 복합 expression index
CREATE INDEX idx_rsv_master_email_lastname
    ON rsv_master_reservation(LOWER(email), LOWER(guest_last_name_en));

-- 캘린더/기간 조회 최적화 복합 인덱스 (property + 날짜 범위)
CREATE INDEX IF NOT EXISTS idx_rsv_master_property_dates
    ON rsv_master_reservation(property_id, master_check_in, master_check_out);
