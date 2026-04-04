-- 테스트 전용: V4_27_0이 rm_transaction_code를 참조하므로 미리 생성
-- (운영에서는 V6_1_0에서 생성되지만, 테스트는 target=5.8.0이라 R__ 패치보다 V4_27_0이 먼저 실행됨)
-- 스키마는 V6_1_0 + JPA 엔티티 정의와 동일하게 유지

CREATE TABLE IF NOT EXISTS rm_transaction_code_group (
    id                  BIGSERIAL       PRIMARY KEY,
    property_id         BIGINT          NOT NULL REFERENCES htl_property(id),
    group_code          VARCHAR(20)     NOT NULL,
    group_name_ko       VARCHAR(100)    NOT NULL,
    group_name_en       VARCHAR(100),
    group_type          VARCHAR(10)     NOT NULL,
    parent_group_id     BIGINT          REFERENCES rm_transaction_code_group(id),
    sort_order          INTEGER         NOT NULL DEFAULT 0,
    use_yn              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,
    UNIQUE (property_id, group_code)
);

CREATE TABLE IF NOT EXISTS rm_transaction_code (
    id                      BIGSERIAL       PRIMARY KEY,
    property_id             BIGINT          NOT NULL REFERENCES htl_property(id),
    transaction_group_id    BIGINT          NOT NULL REFERENCES rm_transaction_code_group(id),
    transaction_code        VARCHAR(10)     NOT NULL,
    code_name_ko            VARCHAR(200)    NOT NULL,
    code_name_en            VARCHAR(200),
    revenue_category        VARCHAR(20)     NOT NULL,
    code_type               VARCHAR(10)     NOT NULL DEFAULT 'CHARGE',
    sort_order              INTEGER         NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    UNIQUE (property_id, transaction_code)
);

-- V4_27_0이 rsv_reservation_service.transaction_code_id도 참조
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS transaction_code_id BIGINT;

-- V4_27_0이 rsv_room_upgrade_history도 참조 (운영에서는 V6_4_0에서 생성)
CREATE TABLE IF NOT EXISTS rsv_room_upgrade_history (
    id                      BIGSERIAL       PRIMARY KEY,
    sub_reservation_id      BIGINT          NOT NULL REFERENCES rsv_sub_reservation(id),
    from_room_type_id       BIGINT          NOT NULL REFERENCES rm_room_type(id),
    to_room_type_id         BIGINT          NOT NULL REFERENCES rm_room_type(id),
    upgraded_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upgrade_type            VARCHAR(20)     NOT NULL,
    price_difference        NUMERIC(15,2),
    reason                  VARCHAR(500),
    created_by              VARCHAR(50),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
