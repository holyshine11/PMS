-- 통합 테스트용 스키마 패치
-- flyway.target=5.8.0 이후 DDL 마이그레이션을 테스트 DB에 적용

-- V5_18_0: 서비스 타입 컬럼 확장
ALTER TABLE rsv_reservation_service ALTER COLUMN service_type TYPE VARCHAR(20);

-- V6_1_0: 트랜잭션 코드 테이블 (V4_26_9 테스트 패치에서 생성 완료 — 여기서는 스킵)

-- V6_2_0: PaidServiceOption 확장
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS charge_type VARCHAR(20) DEFAULT 'PER_NIGHT';
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS transaction_code_id BIGINT;
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS inventory_item_id BIGINT;
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS posting_frequency VARCHAR(20);
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS package_scope VARCHAR(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS applicable_nights VARCHAR(20) NOT NULL DEFAULT 'ALL_NIGHTS';
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS sell_separately BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS quantity_unit VARCHAR(10) NOT NULL DEFAULT 'PER_ROOM';
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS admin_memo TEXT;
ALTER TABLE rm_paid_service_option ADD COLUMN IF NOT EXISTS vat_included_price NUMERIC(15,2) NOT NULL DEFAULT 0;

-- V6_5_0: ReservationServiceItem 확장
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS tc_code_id BIGINT;
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS charge_date DATE;
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS tax NUMERIC(15,2) DEFAULT 0;
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS service_charge NUMERIC(15,2) DEFAULT 0;
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS notes VARCHAR(500);

-- V6_8_0: 낙관적 잠금 version 컬럼
ALTER TABLE rsv_master_reservation ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rsv_sub_reservation ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- V6_9_0: service_option_id nullable
ALTER TABLE rsv_reservation_service ALTER COLUMN service_option_id DROP NOT NULL;

-- ReservationServiceItem 추가 컬럼 (엔티티에 정의된 컬럼)
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS posting_status VARCHAR(10);
ALTER TABLE rsv_reservation_service ADD COLUMN IF NOT EXISTS transaction_code_id BIGINT;

-- V7_1_0: 객실 상태 필드
ALTER TABLE htl_room_number ADD COLUMN IF NOT EXISTS room_status VARCHAR(20) NOT NULL DEFAULT 'VACANT';
ALTER TABLE htl_room_number ADD COLUMN IF NOT EXISTS fo_status VARCHAR(20) NOT NULL DEFAULT 'CLEAN';
ALTER TABLE htl_room_number ADD COLUMN IF NOT EXISTS hk_status VARCHAR(20) NOT NULL DEFAULT 'CLEAN';

-- V7_2_0: OOO/OOS 테이블
CREATE TABLE IF NOT EXISTS htl_room_unavailable (
    id BIGSERIAL PRIMARY KEY,
    room_number_id BIGINT NOT NULL REFERENCES htl_room_number(id),
    unavailable_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    use_yn BOOLEAN NOT NULL DEFAULT true,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- V4_19_0: Dayuse 지원
ALTER TABLE rsv_sub_reservation ADD COLUMN IF NOT EXISTS stay_type VARCHAR(20) NOT NULL DEFAULT 'OVERNIGHT';
ALTER TABLE rsv_sub_reservation ADD COLUMN IF NOT EXISTS day_use_start_time TIME;
ALTER TABLE rsv_sub_reservation ADD COLUMN IF NOT EXISTS day_use_end_time TIME;
ALTER TABLE rt_rate_code ADD COLUMN IF NOT EXISTS stay_type VARCHAR(20) NOT NULL DEFAULT 'OVERNIGHT';
ALTER TABLE htl_property ADD COLUMN IF NOT EXISTS day_use_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE htl_property ADD COLUMN IF NOT EXISTS day_use_start_time VARCHAR(10) DEFAULT '10:00';
ALTER TABLE htl_property ADD COLUMN IF NOT EXISTS day_use_end_time VARCHAR(10) DEFAULT '20:00';
ALTER TABLE htl_property ADD COLUMN IF NOT EXISTS day_use_default_hours INTEGER DEFAULT 5;

CREATE TABLE IF NOT EXISTS rt_day_use_rate (
    id BIGSERIAL PRIMARY KEY,
    rate_code_id BIGINT NOT NULL REFERENCES rt_rate_code(id),
    duration_hours INTEGER NOT NULL,
    supply_price NUMERIC(15,2) NOT NULL,
    description VARCHAR(200),
    sort_order INTEGER DEFAULT 0,
    use_yn BOOLEAN NOT NULL DEFAULT true,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- V4_16_0: 결제 필드 확장
ALTER TABLE rsv_reservation_payment ADD COLUMN IF NOT EXISTS total_early_late_fee NUMERIC(15,2) DEFAULT 0;

-- HK 테이블(V8_x)은 통합 테스트에서 불필요 — HK 마이그레이션(V8_1_0~V8_7_3)이 target 범위 밖이므로 스킵
