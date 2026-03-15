-- ============================================================
-- 취소 수수료 정책 테스트 데이터
-- 대상 프로퍼티: GMP, GMS, OBH (3개)
-- 정책: 한국 호텔 표준 취소 수수료 정책
--
-- 알고리즘: daysBefore ASC 정렬, remainingDays <= daysBefore 첫 매칭
--   - 당일(0일 전): 0<=1 → 80% (1박 요금의 80%)
--   - 2일 전:       2<=3 → 50% (1박 요금의 50%)
--   - 5일 전:       5<=7 → 0%  (무료 취소)
--   - 8일+ 전:      매칭 없음 → 무료 취소
--   - 노쇼:         100% (1박 요금 전액)
-- ============================================================

-- GMP 프로퍼티 취소 수수료 정책
INSERT INTO htl_cancellation_fee (property_id, checkin_basis, days_before, fee_amount, fee_type, sort_order, use_yn, created_at, created_by)
VALUES
    -- 체크인 1일 이내 취소: 1박 요금의 80%
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND deleted_at IS NULL), 'DATE', 1, 80, 'PERCENTAGE', 0, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    -- 체크인 3일 이내 취소: 1박 요금의 50%
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND deleted_at IS NULL), 'DATE', 3, 50, 'PERCENTAGE', 1, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    -- 체크인 7일 이내 취소: 무료 취소
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND deleted_at IS NULL), 'DATE', 7, 0, 'PERCENTAGE', 2, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    -- 노쇼: 1박 요금의 100%
    ((SELECT id FROM htl_property WHERE property_code = 'GMP' AND deleted_at IS NULL), 'NOSHOW', NULL, 100, 'PERCENTAGE', 3, true, CURRENT_TIMESTAMP, 'SYSTEM');

-- GMS 프로퍼티 취소 수수료 정책
INSERT INTO htl_cancellation_fee (property_id, checkin_basis, days_before, fee_amount, fee_type, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND deleted_at IS NULL), 'DATE', 1, 80, 'PERCENTAGE', 0, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND deleted_at IS NULL), 'DATE', 3, 50, 'PERCENTAGE', 1, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND deleted_at IS NULL), 'DATE', 7, 0, 'PERCENTAGE', 2, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS' AND deleted_at IS NULL), 'NOSHOW', NULL, 100, 'PERCENTAGE', 3, true, CURRENT_TIMESTAMP, 'SYSTEM');

-- OBH 프로퍼티 취소 수수료 정책
INSERT INTO htl_cancellation_fee (property_id, checkin_basis, days_before, fee_amount, fee_type, sort_order, use_yn, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND deleted_at IS NULL), 'DATE', 1, 80, 'PERCENTAGE', 0, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND deleted_at IS NULL), 'DATE', 3, 50, 'PERCENTAGE', 1, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND deleted_at IS NULL), 'DATE', 7, 0, 'PERCENTAGE', 2, true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ((SELECT id FROM htl_property WHERE property_code = 'OBH' AND deleted_at IS NULL), 'NOSHOW', NULL, 100, 'PERCENTAGE', 3, true, CURRENT_TIMESTAMP, 'SYSTEM');
