-- =============================================
-- V8.2.0: 하우스키핑 작업 관련 테이블
-- =============================================

-- 1. 작업 시트
CREATE TABLE hk_task_sheet (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),

    sheet_name      VARCHAR(100) NOT NULL,
    sheet_date      DATE NOT NULL,
    assigned_to     BIGINT REFERENCES sys_admin_user(id),

    total_rooms     INTEGER DEFAULT 0,
    total_credits   DECIMAL(5,1) DEFAULT 0,
    completed_rooms INTEGER DEFAULT 0,

    -- BaseEntity 공통
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

CREATE INDEX idx_hk_task_sheet_property_date ON hk_task_sheet(property_id, sheet_date);

COMMENT ON TABLE hk_task_sheet IS '하우스키핑 작업 시트 (하우스키퍼별 일일 작업 묶음)';

-- 2. HK 작업
CREATE TABLE hk_task (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    room_number_id  BIGINT NOT NULL REFERENCES htl_room_number(id),
    task_sheet_id   BIGINT REFERENCES hk_task_sheet(id),

    -- 작업 정보
    task_type       VARCHAR(20) NOT NULL,       -- CHECKOUT, STAYOVER, TURNDOWN, DEEP_CLEAN, TOUCH_UP
    task_date       DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, PAUSED, COMPLETED, INSPECTED, CANCELLED
    priority        VARCHAR(10) DEFAULT 'NORMAL',            -- RUSH, HIGH, NORMAL, LOW
    credit          DECIMAL(3,1) DEFAULT 1.0,

    -- 배정
    assigned_to     BIGINT REFERENCES sys_admin_user(id),
    assigned_by     BIGINT REFERENCES sys_admin_user(id),
    assigned_at     TIMESTAMP,

    -- 시간 추적
    started_at      TIMESTAMP,
    paused_at       TIMESTAMP,
    completed_at    TIMESTAMP,
    inspected_at    TIMESTAMP,
    inspected_by    BIGINT REFERENCES sys_admin_user(id),
    estimated_end   TIMESTAMP,
    duration_minutes INTEGER,

    -- 예약 연계
    reservation_id  BIGINT,
    next_checkin_at TIMESTAMP,

    -- 메모
    note            VARCHAR(500),

    -- BaseEntity 공통
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

CREATE INDEX idx_hk_task_property_date ON hk_task(property_id, task_date);
CREATE INDEX idx_hk_task_assigned ON hk_task(assigned_to, task_date, status);
CREATE INDEX idx_hk_task_status ON hk_task(property_id, status, task_date);
CREATE INDEX idx_hk_task_room ON hk_task(room_number_id, task_date);

COMMENT ON TABLE hk_task IS '하우스키핑 작업';
COMMENT ON COLUMN hk_task.task_type IS '작업유형: CHECKOUT, STAYOVER, TURNDOWN, DEEP_CLEAN, TOUCH_UP';
COMMENT ON COLUMN hk_task.status IS '작업상태: PENDING, IN_PROGRESS, PAUSED, COMPLETED, INSPECTED, CANCELLED';
COMMENT ON COLUMN hk_task.priority IS '우선순위: RUSH, HIGH, NORMAL, LOW';

-- 3. 작업 상태 변경 이력
CREATE TABLE hk_task_log (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES hk_task(id),

    from_status     VARCHAR(20),
    to_status       VARCHAR(20) NOT NULL,
    changed_by      VARCHAR(50) NOT NULL,
    changed_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    note            VARCHAR(500)
);

CREATE INDEX idx_hk_task_log_task ON hk_task_log(task_id);

COMMENT ON TABLE hk_task_log IS '하우스키핑 작업 상태 변경 이력';

-- 4. 작업 이슈/메모
CREATE TABLE hk_task_issue (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT REFERENCES hk_task(id),
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    room_number_id  BIGINT NOT NULL REFERENCES htl_room_number(id),

    issue_type      VARCHAR(20) NOT NULL,       -- MEMO, MAINTENANCE, SUPPLY_SHORT, LOST_FOUND, DAMAGE
    description     VARCHAR(1000) NOT NULL,
    image_path      VARCHAR(500),
    resolved        BOOLEAN DEFAULT FALSE,
    resolved_at     TIMESTAMP,
    resolved_by     VARCHAR(50),

    -- BaseEntity 공통
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

CREATE INDEX idx_hk_task_issue_property ON hk_task_issue(property_id, issue_type);
CREATE INDEX idx_hk_task_issue_room ON hk_task_issue(room_number_id);

COMMENT ON TABLE hk_task_issue IS '하우스키핑 작업 이슈/메모';
COMMENT ON COLUMN hk_task_issue.issue_type IS '이슈유형: MEMO, MAINTENANCE, SUPPLY_SHORT, LOST_FOUND, DAMAGE';
