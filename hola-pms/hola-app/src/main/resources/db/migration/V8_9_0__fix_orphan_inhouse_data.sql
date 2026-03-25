-- V8_9_0: 고아 INHOUSE 데이터 보정
-- 원인: 체크아웃 미처리로 인해 SubReservation이 INHOUSE, RoomNumber가 OCCUPIED 상태로 잔류
-- 대상: checkOut < today인 INHOUSE + checkIn > today인 INHOUSE (테스트 데이터 오류)

-- 1. 과거 체크아웃인 INHOUSE → CHECKED_OUT (실제 체크아웃 시간: 체크아웃일 15:00)
UPDATE rsv_sub_reservation
SET room_reservation_status = 'CHECKED_OUT',
    actual_check_out_time = (check_out::timestamp + interval '15 hours'),
    updated_at = NOW()
WHERE room_reservation_status IN ('INHOUSE', 'CHECK_IN')
  AND check_out < CURRENT_DATE
  AND deleted_at IS NULL;

-- 2. 미래 체크인인 INHOUSE → RESERVED (테스트 데이터 시드 오류 복원)
UPDATE rsv_sub_reservation
SET room_reservation_status = 'RESERVED',
    updated_at = NOW()
WHERE room_reservation_status IN ('INHOUSE', 'CHECK_IN')
  AND check_in > CURRENT_DATE
  AND deleted_at IS NULL;

-- 3. MasterReservation 상태 도출 보정
-- 3-1. 활성 Sub 전부 CHECKED_OUT이면 마스터도 CHECKED_OUT (deriveMasterStatus 로직)
UPDATE rsv_master_reservation mr
SET reservation_status = 'CHECKED_OUT',
    updated_at = NOW()
WHERE mr.deleted_at IS NULL
  AND mr.reservation_status IN ('INHOUSE', 'CHECK_IN')
  AND NOT EXISTS (
      SELECT 1 FROM rsv_sub_reservation s
      WHERE s.master_reservation_id = mr.id
        AND s.deleted_at IS NULL
        AND s.room_reservation_status NOT IN ('CHECKED_OUT', 'CANCELED', 'NO_SHOW')
  );

-- 3-2. 활성 Sub 중 RESERVED만 있으면 마스터도 RESERVED
UPDATE rsv_master_reservation mr
SET reservation_status = 'RESERVED',
    updated_at = NOW()
WHERE mr.deleted_at IS NULL
  AND mr.reservation_status = 'INHOUSE'
  AND EXISTS (
      SELECT 1 FROM rsv_sub_reservation s
      WHERE s.master_reservation_id = mr.id
        AND s.deleted_at IS NULL
        AND s.room_reservation_status = 'RESERVED'
  )
  AND NOT EXISTS (
      SELECT 1 FROM rsv_sub_reservation s
      WHERE s.master_reservation_id = mr.id
        AND s.deleted_at IS NULL
        AND s.room_reservation_status IN ('INHOUSE', 'CHECK_IN')
  );

-- 4. 고아 RoomNumber: OCCUPIED인데 활성 INHOUSE Sub 없으면 → VACANT (hkStatus 유지)
UPDATE htl_room_number
SET fo_status = 'VACANT',
    updated_at = NOW()
WHERE fo_status = 'OCCUPIED'
  AND deleted_at IS NULL
  AND id NOT IN (
      SELECT room_number_id FROM rsv_sub_reservation
      WHERE room_reservation_status IN ('CHECK_IN', 'INHOUSE')
        AND deleted_at IS NULL
        AND room_number_id IS NOT NULL
  );
