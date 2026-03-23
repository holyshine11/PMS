-- V4_18_0: VAT 계산 순서 변경 - (공급가+봉사료) 기반 세금 재계산
-- 변경 전: tax = supplyPrice * taxRate
-- 변경 후: tax = (supplyPrice + serviceCharge) * taxRate
-- 봉사료가 0인 예약은 결과 동일하므로 service_charge > 0 조건으로 필터링

-- 1. DailyCharge 세금/합계 재계산
UPDATE rsv_daily_charge dc
SET tax = ROUND(
        (dc.supply_price + dc.service_charge) * p.tax_rate / 100,
        COALESCE(p.tax_decimal_places, 0)
    ),
    total = dc.supply_price
        + dc.service_charge
        + ROUND(
            (dc.supply_price + dc.service_charge) * p.tax_rate / 100,
            COALESCE(p.tax_decimal_places, 0)
        ),
    updated_at = NOW()
FROM rsv_sub_reservation sr
JOIN rsv_master_reservation mr ON mr.id = sr.master_reservation_id
JOIN htl_property p ON p.id = mr.property_id
WHERE dc.sub_reservation_id = sr.id
  AND dc.service_charge > 0
  AND p.tax_rate > 0;

-- 2. ReservationPayment 합계 갱신 (DailyCharge 기반 재집계)
UPDATE rsv_reservation_payment rp
SET total_room_amount = sub_totals.total_room,
    total_service_charge_amount = sub_totals.total_svc_chg,
    grand_total = GREATEST(0,
        sub_totals.total_room
        + sub_totals.total_svc_chg
        + COALESCE(rp.total_service_amount, 0)
        + COALESCE(rp.total_adjustment_amount, 0)
        + COALESCE(rp.total_early_late_fee, 0)
    ),
    updated_at = NOW()
FROM (
    SELECT mr.id AS master_id,
           COALESCE(SUM(dc.supply_price + dc.tax), 0) AS total_room,
           COALESCE(SUM(dc.service_charge), 0) AS total_svc_chg
    FROM rsv_master_reservation mr
    JOIN rsv_sub_reservation sr ON sr.master_reservation_id = mr.id
        AND sr.deleted_at IS NULL
        AND sr.room_reservation_status != 'CANCELED'
    JOIN rsv_daily_charge dc ON dc.sub_reservation_id = sr.id
    GROUP BY mr.id
) sub_totals
WHERE rp.master_reservation_id = sub_totals.master_id
  AND rp.deleted_at IS NULL;
