-- 트랜잭션 코드 그룹 (Main → Sub 2단계 계층)
CREATE TABLE rm_transaction_code_group (
    id                  BIGSERIAL       PRIMARY KEY,
    property_id         BIGINT          NOT NULL REFERENCES htl_property(id),
    group_code          VARCHAR(20)     NOT NULL,
    group_name_ko       VARCHAR(100)    NOT NULL,
    group_name_en       VARCHAR(100),
    group_type          VARCHAR(10)     NOT NULL,      -- MAIN / SUB
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

CREATE INDEX idx_rm_tc_group_property ON rm_transaction_code_group(property_id);
CREATE INDEX idx_rm_tc_group_parent ON rm_transaction_code_group(parent_group_id);

COMMENT ON TABLE rm_transaction_code_group IS '트랜잭션 코드 그룹';
COMMENT ON COLUMN rm_transaction_code_group.group_code IS '그룹 코드 (LODGING, FOOD_BEVERAGE 등)';
COMMENT ON COLUMN rm_transaction_code_group.group_type IS 'MAIN: 대분류, SUB: 소분류';
COMMENT ON COLUMN rm_transaction_code_group.parent_group_id IS 'SUB 그룹의 상위 MAIN 그룹 ID';

-- 트랜잭션 코드 (부과 항목 회계 단위)
CREATE TABLE rm_transaction_code (
    id                      BIGSERIAL       PRIMARY KEY,
    property_id             BIGINT          NOT NULL REFERENCES htl_property(id),
    transaction_group_id    BIGINT          NOT NULL REFERENCES rm_transaction_code_group(id),
    transaction_code        VARCHAR(10)     NOT NULL,
    code_name_ko            VARCHAR(200)    NOT NULL,
    code_name_en            VARCHAR(200),
    revenue_category        VARCHAR(20)     NOT NULL,  -- LODGING, FOOD_BEVERAGE, MISC, TAX, NON_REVENUE
    code_type               VARCHAR(10)     NOT NULL DEFAULT 'CHARGE',  -- CHARGE / PAYMENT
    sort_order              INTEGER         NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    UNIQUE (property_id, transaction_code)
);

CREATE INDEX idx_rm_tc_property ON rm_transaction_code(property_id);
CREATE INDEX idx_rm_tc_group ON rm_transaction_code(transaction_group_id);
CREATE INDEX idx_rm_tc_category ON rm_transaction_code(revenue_category);

COMMENT ON TABLE rm_transaction_code IS '트랜잭션 코드';
COMMENT ON COLUMN rm_transaction_code.transaction_code IS '코드 번호 (예: 1000, 2000)';
COMMENT ON COLUMN rm_transaction_code.revenue_category IS '매출 분류 (LODGING/FOOD_BEVERAGE/MISC/TAX/NON_REVENUE)';
COMMENT ON COLUMN rm_transaction_code.code_type IS 'CHARGE: 부과, PAYMENT: 결제';
