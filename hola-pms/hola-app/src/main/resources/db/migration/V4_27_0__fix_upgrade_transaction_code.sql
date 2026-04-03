-- 업그레이드 차액 서비스 항목의 트랜잭션 코드 오류 수정
-- 기존: TC 1020 (조기 체크인 요금) → 수정: TC 1010 (업그레이드 차액)
-- 조건: service_type='PAID' + TC코드 '1020' + 해당 sub에 업그레이드 이력 존재

UPDATE rsv_reservation_service rs
SET transaction_code_id = correct_tc.id,
    updated_at = NOW()
FROM rm_transaction_code wrong_tc,
     rm_transaction_code correct_tc
WHERE rs.transaction_code_id = wrong_tc.id
  AND wrong_tc.transaction_code = '1020'
  AND correct_tc.property_id = wrong_tc.property_id
  AND correct_tc.transaction_code = '1010'
  AND correct_tc.deleted_at IS NULL
  AND rs.service_type = 'PAID'
  AND EXISTS (
      SELECT 1 FROM rsv_room_upgrade_history ruh
      WHERE ruh.sub_reservation_id = rs.sub_reservation_id
  );
