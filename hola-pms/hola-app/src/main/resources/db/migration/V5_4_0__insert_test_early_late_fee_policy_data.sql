-- =============================================
-- V5.4.0: 얼리 체크인 / 레이트 체크아웃 정책 테스트 데이터
-- GMP(올라 그랜드 명동), GMS(올라 그랜드 서초) 프로퍼티 대상
-- =============================================

-- 기존 정책 정리
DELETE FROM htl_early_late_fee_policy;

-- =============================================
-- GMP (올라 그랜드 명동) - 얼리 체크인 정책
-- 기준 체크인: 15:00
-- =============================================
INSERT INTO htl_early_late_fee_policy (property_id, policy_type, time_from, time_to, fee_type, fee_value, description, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EARLY_CHECKIN', '06:00', '09:00', 'PERCENT', 100.00, '새벽 얼리체크인 (1박 추가)', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EARLY_CHECKIN', '09:00', '12:00', 'PERCENT', 50.00, '오전 얼리체크인 (50%)', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'EARLY_CHECKIN', '12:00', '15:00', 'FIXED', 30000.00, '오후 얼리체크인 (3만원)', 3, NOW(), 'admin');

-- =============================================
-- GMP (올라 그랜드 명동) - 레이트 체크아웃 정책
-- 기준 체크아웃: 11:00
-- =============================================
INSERT INTO htl_early_late_fee_policy (property_id, policy_type, time_from, time_to, fee_type, fee_value, description, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'LATE_CHECKOUT', '11:00', '13:00', 'FIXED', 30000.00, '오후 1시까지 (3만원)', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'LATE_CHECKOUT', '13:00', '15:00', 'PERCENT', 50.00, '오후 3시까지 (50%)', 2, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMP'), 'LATE_CHECKOUT', '15:00', '18:00', 'PERCENT', 100.00, '오후 6시까지 (1박 추가)', 3, NOW(), 'admin');

-- =============================================
-- GMS (올라 그랜드 서초) - 얼리 체크인 정책
-- 기준 체크인: 15:00
-- =============================================
INSERT INTO htl_early_late_fee_policy (property_id, policy_type, time_from, time_to, fee_type, fee_value, description, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'EARLY_CHECKIN', '08:00', '12:00', 'PERCENT', 50.00, '오전 얼리체크인 (50%)', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'EARLY_CHECKIN', '12:00', '15:00', 'FIXED', 20000.00, '오후 얼리체크인 (2만원)', 2, NOW(), 'admin');

-- =============================================
-- GMS (올라 그랜드 서초) - 레이트 체크아웃 정책
-- 기준 체크아웃: 11:00
-- =============================================
INSERT INTO htl_early_late_fee_policy (property_id, policy_type, time_from, time_to, fee_type, fee_value, description, sort_order, created_at, created_by)
VALUES
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'LATE_CHECKOUT', '11:00', '14:00', 'FIXED', 25000.00, '오후 2시까지 (2.5만원)', 1, NOW(), 'admin'),
    ((SELECT id FROM htl_property WHERE property_code = 'GMS'), 'LATE_CHECKOUT', '14:00', '18:00', 'PERCENT', 100.00, '오후 6시까지 (1박 추가)', 2, NOW(), 'admin');
