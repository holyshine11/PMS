-- 기존 PaymentTransaction의 sub_reservation_id NULL 데이터 보정
-- V4_23_0에서 PG PAYMENT만 처리했으므로, 나머지 거래도 귀속시킴

-- 1) 싱글레그 예약: 모든 NULL 거래를 유일한 sub_reservation에 귀속
UPDATE rsv_payment_transaction pt
SET sub_reservation_id = (
    SELECT sr.id
    FROM rsv_sub_reservation sr
    WHERE sr.master_reservation_id = pt.master_reservation_id
    AND sr.deleted_at IS NULL
)
WHERE pt.sub_reservation_id IS NULL
AND (SELECT COUNT(*) FROM rsv_sub_reservation sr
     WHERE sr.master_reservation_id = pt.master_reservation_id
     AND sr.deleted_at IS NULL) = 1;

-- 2) 멀티레그 예약 — PAYMENT 거래: 첫 번째 활성(비취소) Leg에 귀속
UPDATE rsv_payment_transaction pt
SET sub_reservation_id = (
    SELECT MIN(sr.id)
    FROM rsv_sub_reservation sr
    WHERE sr.master_reservation_id = pt.master_reservation_id
    AND sr.deleted_at IS NULL
    AND sr.room_reservation_status NOT IN ('CANCELED', 'NO_SHOW')
)
WHERE pt.sub_reservation_id IS NULL
AND pt.transaction_type = 'PAYMENT'
AND (SELECT COUNT(*) FROM rsv_sub_reservation sr
     WHERE sr.master_reservation_id = pt.master_reservation_id
     AND sr.deleted_at IS NULL) > 1;

-- 3) 멀티레그 예약 — REFUND 거래: 첫 번째 취소/노쇼 Leg에 귀속
UPDATE rsv_payment_transaction pt
SET sub_reservation_id = (
    SELECT MIN(sr.id)
    FROM rsv_sub_reservation sr
    WHERE sr.master_reservation_id = pt.master_reservation_id
    AND sr.deleted_at IS NULL
    AND sr.room_reservation_status IN ('CANCELED', 'NO_SHOW')
)
WHERE pt.sub_reservation_id IS NULL
AND pt.transaction_type = 'REFUND'
AND (SELECT COUNT(*) FROM rsv_sub_reservation sr
     WHERE sr.master_reservation_id = pt.master_reservation_id
     AND sr.deleted_at IS NULL) > 1;

-- 4) 위 조건에도 매칭 안 된 잔여 NULL 거래: 첫 번째 sub에 귀속 (방어)
UPDATE rsv_payment_transaction pt
SET sub_reservation_id = (
    SELECT MIN(sr.id)
    FROM rsv_sub_reservation sr
    WHERE sr.master_reservation_id = pt.master_reservation_id
    AND sr.deleted_at IS NULL
)
WHERE pt.sub_reservation_id IS NULL
AND EXISTS (
    SELECT 1 FROM rsv_sub_reservation sr
    WHERE sr.master_reservation_id = pt.master_reservation_id
    AND sr.deleted_at IS NULL
);
