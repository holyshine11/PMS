-- 예약 테이블 복합 인덱스 추가 (쿼리 성능 최적화)

-- 서브예약: 호수별 가용성 조회 최적화
CREATE INDEX IF NOT EXISTS idx_rsv_sub_room_availability
    ON rsv_sub_reservation(room_number_id, check_in, check_out, room_reservation_status);

-- 서브예약: 객실타입별 가용성 조회 최적화
CREATE INDEX IF NOT EXISTS idx_rsv_sub_roomtype_availability
    ON rsv_sub_reservation(room_type_id, check_in, check_out, room_reservation_status);

-- 마스터예약: 캘린더뷰/리스트 날짜 범위 조회 최적화
CREATE INDEX IF NOT EXISTS idx_rsv_master_property_dates
    ON rsv_master_reservation(property_id, master_check_in, master_check_out);

-- 마스터예약: 리스트 상태+날짜 필터 최적화
CREATE INDEX IF NOT EXISTS idx_rsv_master_property_status_checkin
    ON rsv_master_reservation(property_id, reservation_status, master_check_in);
