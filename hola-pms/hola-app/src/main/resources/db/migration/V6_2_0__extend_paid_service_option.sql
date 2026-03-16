-- PaidServiceOption 확장 (PackageCode 역할 흡수)
ALTER TABLE rm_paid_service_option
    ADD COLUMN transaction_code_id  BIGINT REFERENCES rm_transaction_code(id),
    ADD COLUMN posting_frequency    VARCHAR(20),
    ADD COLUMN package_scope        VARCHAR(20) NOT NULL DEFAULT 'PROPERTY_WIDE',
    ADD COLUMN sell_separately      BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_rm_paid_svc_tc ON rm_paid_service_option(transaction_code_id);

COMMENT ON COLUMN rm_paid_service_option.transaction_code_id IS '트랜잭션 코드 FK (점진 매핑)';
COMMENT ON COLUMN rm_paid_service_option.posting_frequency IS 'PER_NIGHT/PER_STAY/ONE_TIME (applicableNights 대체)';
COMMENT ON COLUMN rm_paid_service_option.package_scope IS 'PROPERTY_WIDE: 전체/ROOM_TYPE_SPECIFIC: 객실타입 한정';
COMMENT ON COLUMN rm_paid_service_option.sell_separately IS '개별 판매 가능 여부';

-- RoomTypePaidService 확장 (가격/가용성 오버라이드)
ALTER TABLE rm_room_type_paid_service
    ADD COLUMN override_price   NUMERIC(15,2),
    ADD COLUMN max_quantity     INTEGER,
    ADD COLUMN available        BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN rm_room_type_paid_service.override_price IS '객실타입별 가격 오버라이드 (NULL이면 기본가)';
COMMENT ON COLUMN rm_room_type_paid_service.max_quantity IS '객실타입별 최대 수량 (NULL이면 무제한)';
COMMENT ON COLUMN rm_room_type_paid_service.available IS '해당 객실타입 가용 여부';

-- 기존 applicableNights → postingFrequency 데이터 매핑
UPDATE rm_paid_service_option
SET posting_frequency = CASE
    WHEN applicable_nights = 'ALL_NIGHTS'       THEN 'PER_NIGHT'
    WHEN applicable_nights = 'FIRST_NIGHT_ONLY' THEN 'ONE_TIME'
    WHEN applicable_nights = 'NOT_APPLICABLE'   THEN 'ONE_TIME'
    ELSE 'ONE_TIME'
END
WHERE posting_frequency IS NULL;
