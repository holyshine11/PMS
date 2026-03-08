-- 서브 예약 (객실 레그)
CREATE TABLE rsv_sub_reservation (
    id                      BIGSERIAL PRIMARY KEY,
    master_reservation_id   BIGINT       NOT NULL REFERENCES rsv_master_reservation(id),
    sub_reservation_no      VARCHAR(25)  NOT NULL,
    room_reservation_status VARCHAR(20)  NOT NULL DEFAULT 'RESERVED',
    room_type_id            BIGINT       NOT NULL REFERENCES rm_room_type(id),
    floor_id                BIGINT       REFERENCES htl_floor(id),
    room_number_id          BIGINT       REFERENCES htl_room_number(id),
    adults                  INTEGER      NOT NULL DEFAULT 1,
    children                INTEGER      NOT NULL DEFAULT 0,
    check_in                DATE         NOT NULL,
    check_out               DATE         NOT NULL,
    early_check_in          BOOLEAN      NOT NULL DEFAULT FALSE,
    late_check_out          BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order              INTEGER      NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    UNIQUE (sub_reservation_no)
);

CREATE INDEX idx_rsv_sub_master ON rsv_sub_reservation(master_reservation_id);
CREATE INDEX idx_rsv_sub_room_type ON rsv_sub_reservation(room_type_id);
CREATE INDEX idx_rsv_sub_room_number ON rsv_sub_reservation(room_number_id);
CREATE INDEX idx_rsv_sub_checkin ON rsv_sub_reservation(check_in);
CREATE INDEX idx_rsv_sub_checkout ON rsv_sub_reservation(check_out);
CREATE INDEX idx_rsv_sub_status ON rsv_sub_reservation(room_reservation_status);

-- 투숙객 정보
CREATE TABLE rsv_reservation_guest (
    id                      BIGSERIAL PRIMARY KEY,
    sub_reservation_id      BIGINT       NOT NULL REFERENCES rsv_sub_reservation(id) ON DELETE CASCADE,
    guest_seq               INTEGER      NOT NULL,
    guest_name_ko           VARCHAR(100),
    guest_first_name_en     VARCHAR(50),
    guest_middle_name_en    VARCHAR(50),
    guest_last_name_en      VARCHAR(50),
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    UNIQUE (sub_reservation_id, guest_seq)
);

CREATE INDEX idx_rsv_guest_sub ON rsv_reservation_guest(sub_reservation_id);

-- 일별 객실 요금
CREATE TABLE rsv_daily_charge (
    id                      BIGSERIAL PRIMARY KEY,
    sub_reservation_id      BIGINT       NOT NULL REFERENCES rsv_sub_reservation(id) ON DELETE CASCADE,
    charge_date             DATE         NOT NULL,
    supply_price            NUMERIC(15,2) NOT NULL DEFAULT 0,
    tax                     NUMERIC(15,2) NOT NULL DEFAULT 0,
    service_charge          NUMERIC(15,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    UNIQUE (sub_reservation_id, charge_date)
);

CREATE INDEX idx_rsv_daily_charge_sub ON rsv_daily_charge(sub_reservation_id);

COMMENT ON TABLE rsv_sub_reservation IS '서브 예약 (객실 레그)';
COMMENT ON COLUMN rsv_sub_reservation.sub_reservation_no IS '서브 예약번호 ({masterNo}-{legSeq})';
COMMENT ON COLUMN rsv_sub_reservation.room_reservation_status IS '객실 예약 상태';
COMMENT ON COLUMN rsv_sub_reservation.floor_id IS '배정 층 (배정 전 NULL)';
COMMENT ON COLUMN rsv_sub_reservation.room_number_id IS '배정 호수 (배정 전 NULL)';
COMMENT ON TABLE rsv_reservation_guest IS '투숙객 정보';
COMMENT ON TABLE rsv_daily_charge IS '일별 객실 요금';
