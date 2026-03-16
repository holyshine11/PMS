-- 재고 아이템 마스터
CREATE TABLE rm_inventory_item (
    id                      BIGSERIAL       PRIMARY KEY,
    property_id             BIGINT          NOT NULL REFERENCES htl_property(id),
    item_code               VARCHAR(30)     NOT NULL,
    item_name_ko            VARCHAR(200)    NOT NULL,
    item_name_en            VARCHAR(200),
    item_type               VARCHAR(20)     NOT NULL,       -- EXTRA_BED, CRIB, ROLLAWAY, EQUIPMENT
    management_type         VARCHAR(10)     NOT NULL DEFAULT 'INTERNAL',  -- INTERNAL / EXTERNAL
    external_system_code    VARCHAR(50),                     -- SAP 등 외부 시스템 코드
    total_quantity          INTEGER         NOT NULL DEFAULT 0,
    sort_order              INTEGER         NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    UNIQUE (property_id, item_code)
);

CREATE INDEX idx_rm_inv_item_property ON rm_inventory_item(property_id);

COMMENT ON TABLE rm_inventory_item IS '재고 아이템 마스터';
COMMENT ON COLUMN rm_inventory_item.item_type IS 'EXTRA_BED/CRIB/ROLLAWAY/EQUIPMENT';
COMMENT ON COLUMN rm_inventory_item.management_type IS 'INTERNAL: 자체관리, EXTERNAL: 외부 ERP 연동';
COMMENT ON COLUMN rm_inventory_item.external_system_code IS '외부 시스템(SAP 등) 아이템 식별 코드';

-- 일자별 재고 가용성
CREATE TABLE rm_inventory_availability (
    id                  BIGSERIAL       PRIMARY KEY,
    inventory_item_id   BIGINT          NOT NULL REFERENCES rm_inventory_item(id),
    availability_date   DATE            NOT NULL,
    available_count     INTEGER         NOT NULL DEFAULT 0,
    reserved_count      INTEGER         NOT NULL DEFAULT 0,
    UNIQUE (inventory_item_id, availability_date)
);

CREATE INDEX idx_rm_inv_avail_item_date ON rm_inventory_availability(inventory_item_id, availability_date);

COMMENT ON TABLE rm_inventory_availability IS '일자별 재고 가용성';
COMMENT ON COLUMN rm_inventory_availability.available_count IS '총 가용 수량';
COMMENT ON COLUMN rm_inventory_availability.reserved_count IS '예약된 수량';

-- PaidServiceOption에 재고 연결 필드 추가
ALTER TABLE rm_paid_service_option
    ADD COLUMN inventory_item_id BIGINT REFERENCES rm_inventory_item(id);

COMMENT ON COLUMN rm_paid_service_option.inventory_item_id IS '연결된 재고 아이템 ID';
