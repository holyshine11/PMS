-- 예약 변경 이력 테이블
CREATE TABLE rsv_reservation_change_log (
    id                      BIGSERIAL       PRIMARY KEY,
    master_reservation_id   BIGINT          NOT NULL REFERENCES rsv_master_reservation(id),
    sub_reservation_id      BIGINT          REFERENCES rsv_sub_reservation(id),
    change_category         VARCHAR(30)     NOT NULL,
    change_type             VARCHAR(50)     NOT NULL,
    field_name              VARCHAR(50),
    old_value               VARCHAR(500),
    new_value               VARCHAR(500),
    description             VARCHAR(500),
    created_by              VARCHAR(50),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rsv_change_log_master ON rsv_reservation_change_log(master_reservation_id);
CREATE INDEX idx_rsv_change_log_created ON rsv_reservation_change_log(created_at);

COMMENT ON TABLE rsv_reservation_change_log IS '예약 변경 이력';
COMMENT ON COLUMN rsv_reservation_change_log.change_category IS 'STATUS, ROOM, RATE, UPGRADE, SERVICE, PAYMENT, RESERVATION';
COMMENT ON COLUMN rsv_reservation_change_log.change_type IS 'CREATE, UPDATE, STATUS_CHANGE, UPGRADE, ADD_LEG, REMOVE_LEG, ADD_SERVICE, REMOVE_SERVICE';
COMMENT ON COLUMN rsv_reservation_change_log.description IS '사람이 읽을 수 있는 변경 요약 (UI 직접 표시용)';

-- 기존 업그레이드 이력 backfill
INSERT INTO rsv_reservation_change_log
    (master_reservation_id, sub_reservation_id, change_category, change_type,
     old_value, new_value, description, created_by, created_at)
SELECT
    sr.master_reservation_id,
    ruh.sub_reservation_id,
    'UPGRADE',
    'UPGRADE',
    (SELECT rt.room_type_code FROM rm_room_type rt WHERE rt.id = ruh.from_room_type_id),
    (SELECT rt.room_type_code FROM rm_room_type rt WHERE rt.id = ruh.to_room_type_id),
    CONCAT('객실 업그레이드: ',
           (SELECT rt.room_type_code FROM rm_room_type rt WHERE rt.id = ruh.from_room_type_id),
           ' -> ',
           (SELECT rt.room_type_code FROM rm_room_type rt WHERE rt.id = ruh.to_room_type_id),
           CASE ruh.upgrade_type
               WHEN 'COMPLIMENTARY' THEN ' (무료)'
               WHEN 'UPSELL' THEN ' (업셀)'
               ELSE ' (유료)' END,
           CASE WHEN ruh.price_difference > 0
               THEN CONCAT(', +', ruh.price_difference, '원') ELSE '' END),
    ruh.created_by,
    ruh.created_at
FROM rsv_room_upgrade_history ruh
JOIN rsv_sub_reservation sr ON sr.id = ruh.sub_reservation_id;
