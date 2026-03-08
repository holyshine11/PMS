-- 마스터 예약 테이블
CREATE TABLE rsv_master_reservation (
    id                      BIGSERIAL PRIMARY KEY,
    property_id             BIGINT       NOT NULL REFERENCES htl_property(id),
    master_reservation_no   VARCHAR(20)  NOT NULL,
    confirmation_no         VARCHAR(10)  NOT NULL,
    reservation_status      VARCHAR(20)  NOT NULL DEFAULT 'RESERVED',
    master_check_in         DATE         NOT NULL,
    master_check_out        DATE         NOT NULL,
    reservation_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 예약자 정보
    guest_name_ko           VARCHAR(100),
    guest_first_name_en     VARCHAR(50),
    guest_middle_name_en    VARCHAR(50),
    guest_last_name_en      VARCHAR(50),
    phone_country_code      VARCHAR(5),
    phone_number            VARCHAR(20),
    email                   VARCHAR(200),
    birth_date              DATE,
    gender                  VARCHAR(1),
    nationality             VARCHAR(5),

    -- 예약 기본 정보
    rate_code_id            BIGINT       REFERENCES rt_rate_code(id),
    market_code_id          BIGINT       REFERENCES htl_market_code(id),
    reservation_channel_id  BIGINT       REFERENCES htl_reservation_channel(id),
    promotion_type          VARCHAR(20),
    promotion_code          VARCHAR(50),
    ota_reservation_no      VARCHAR(100),
    is_ota_managed          BOOLEAN      NOT NULL DEFAULT FALSE,

    -- 기타
    customer_request        TEXT,
    sort_order              INTEGER      NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    UNIQUE (master_reservation_no),
    UNIQUE (confirmation_no)
);

CREATE INDEX idx_rsv_master_property ON rsv_master_reservation(property_id);
CREATE INDEX idx_rsv_master_status ON rsv_master_reservation(reservation_status);
CREATE INDEX idx_rsv_master_checkin ON rsv_master_reservation(master_check_in);
CREATE INDEX idx_rsv_master_checkout ON rsv_master_reservation(master_check_out);
CREATE INDEX idx_rsv_master_guest_name ON rsv_master_reservation(guest_name_ko);
CREATE INDEX idx_rsv_master_phone ON rsv_master_reservation(phone_number);

-- 일별 예약번호 시퀀스용 테이블 (propCode + date 기반)
CREATE TABLE rsv_reservation_no_seq (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT       NOT NULL REFERENCES htl_property(id),
    seq_date        DATE         NOT NULL,
    last_seq        INTEGER      NOT NULL DEFAULT 0,
    UNIQUE (property_id, seq_date)
);

COMMENT ON TABLE rsv_master_reservation IS '마스터 예약';
COMMENT ON COLUMN rsv_master_reservation.master_reservation_no IS '예약번호 ({propCode}{YYMMDD}-{seq})';
COMMENT ON COLUMN rsv_master_reservation.confirmation_no IS '확인번호 (8자리 영숫자)';
COMMENT ON COLUMN rsv_master_reservation.reservation_status IS '예약 상태 (RESERVED/CHECK_IN/INHOUSE/CHECKED_OUT/CANCELED/NO_SHOW)';
COMMENT ON COLUMN rsv_master_reservation.master_check_in IS '체크인 일자 (서브 중 최초)';
COMMENT ON COLUMN rsv_master_reservation.master_check_out IS '체크아웃 일자 (서브 중 최종)';
COMMENT ON COLUMN rsv_master_reservation.reservation_date IS '예약 등록 일시';
COMMENT ON COLUMN rsv_master_reservation.is_ota_managed IS 'OTA 수정제한 플래그';
COMMENT ON TABLE rsv_reservation_no_seq IS '예약번호 일별 시퀀스';
