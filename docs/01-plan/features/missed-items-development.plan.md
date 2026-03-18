# 놓친 항목 단계별 개발 계획서

> 작성일: 2026-03-18
> 기반: 중간 점검 보고서 (mid-project-review-2026-03-18.md)
> 원칙: Critical → Major → Minor 순서, 의존성 기반 그룹핑

---

## Executive Summary

| 항목 | Feature | 기간 | 핵심 산출물 |
|------|---------|------|-----------|
| Step 1 | 프론트데스크 운영 강화 | ~3일 | 체크인/아웃 워크플로우 UI, OOO/OOS 관리 |
| Step 2 | 고객 프로필 기초 | ~2일 | Guest Profile 독립 엔티티 + 예약 연동 |
| Step 3 | 하우스키핑 | ~3일 | HK 보드, Task Sheet, 프론트데스크 연동 |
| Step 4 | 예약 운영 확장 | ~3일 | Trace, 확인서/등록카드 PDF, Group 예약 기초 |
| Step 5 | 재무/정산 기초 | ~4일 | Folio, EOD 프로세스, AR 기초 |
| Step 6 | 감사/리포트 | ~2일 | Changes Log, Manager Report |
| Step 7 | Minor 항목 | 선택적 | Property 확장, Restrictions 등 |

---

## 현재 상태 재평가 (코드 분석 결과)

중간 점검에서 "놓침"으로 분류했으나, 코드 분석 결과 **이미 구현되어 있는 항목**:

| 항목 | 점검 시 판단 | 실제 상태 | 남은 작업 |
|------|-----------|----------|----------|
| C1 체크인/아웃 | "워크플로우 없음" | `ReservationServiceImpl.changeStatus()`에 CHECK_IN/CHECKED_OUT 완전 구현 (조기/지연 수수료 포함) | **UI만 필요** — 프론트데스크 전용 화면 |
| C2 객실 상태 6단계 | "미완성" | `RoomNumber`에 hkStatus(CLEAN/DIRTY/OOO/OOS) + foStatus(VACANT/OCCUPIED) + checkIn()/checkOut() 구현 완료 | **OOO/OOS 관리 UI** — 사유코드, 날짜범위, 반환상태 |
| M2 OOO/OOS 분리 | "없음" | Room Rack에서 OOO/OOS 상태 표시 + 필터 가능 | **전용 관리 화면** — OOO/OOS 등록/해제 |

→ **기반 코드가 탄탄하여 UI 중심 개발로 빠르게 진행 가능**

---

## Step 1: 프론트데스크 운영 강화

### 포함 항목
- **C1**: 체크인/체크아웃 워크플로우 UI
- **C2 보강**: OOO/OOS 관리 화면 (사유코드, 날짜범위)
- **M2**: OOO/OOS 분리 관리 전용 UI
- **벤치마킹 B2**: "I Want To..." Quick Action 패턴
- **벤치마킹 B4**: ETA/ETD 필드

### 1.1 프론트데스크 대시보드 (Arrivals/Departures/InHouse 통합)

**설계 방향**: OPERA의 3분할 메뉴 대신, Room Rack 페이지에 **탭 기반 통합 뷰** 구현

```
프론트데스크 (Front Desk)
├── 객실현황 (Room Rack) ← 기존 유지
└── 운영현황 (Operations) ← 신규
    ├── [도착] 탭 — 오늘 체크인 예정 예약 목록
    ├── [투숙중] 탭 — 현재 In-House 예약 목록
    └── [출발] 탭 — 오늘 체크아웃 예정 예약 목록
```

**백엔드 변경**:

| 파일 | 변경 | 상세 |
|------|------|------|
| `FrontDeskApiController.java` (신규) | API 3개 | GET arrivals, inHouse, departures |
| `FrontDeskService.java` (신규) | 서비스 | 기존 ReservationRepository 활용, 상태+날짜 필터 |
| `FrontDeskResponse.java` (신규) | DTO | 예약번호, 게스트명, 객실, 상태, ETA/ETD, 결제현황 |

**프론트엔드 변경**:

| 파일 | 변경 | 상세 |
|------|------|------|
| `front-desk/operations.html` (신규) | 탭 뷰 | 도착/투숙/출발 3탭 + DataTable |
| `fd-operations-page.js` (신규) | JS | 탭 전환, Quick Action 드롭다운, 체크인/아웃 처리 |
| `sidebar.html` | 수정 | 프론트데스크 > 운영현황 메뉴 추가 |

**Quick Action 드롭다운 (벤치마킹 B2)**:
```
각 예약 행의 드롭다운 메뉴:
├── 체크인 (도착 탭에서)
├── 체크아웃 (투숙/출발 탭에서)
├── 객실 배정 (미배정 시)
├── 예약 상세 보기
├── 메모 추가
└── 객실 변경
```

구현: `HolaPms.renders.quickActions(type)` — actionButtons 확장

### 1.2 ETA/ETD 필드 추가 (벤치마킹 B4)

**DB 마이그레이션**: `V4_17_0__add_eta_etd_fields.sql`
```sql
ALTER TABLE rsv_sub_reservation ADD COLUMN eta TIME;        -- 예상 도착 시간
ALTER TABLE rsv_sub_reservation ADD COLUMN etd TIME;        -- 예상 출발 시간
```

**엔티티/DTO 수정**:
- `SubReservation.java`: `eta`, `etd` (LocalTime) 필드 추가
- `ReservationCreateRequest.java`: eta, etd 추가
- `FrontDeskResponse.java`: eta, etd 포함

### 1.3 OOO/OOS 관리 화면

**설계 방향**: OOO/OOS를 Room Rack에서 간단히 설정하는 것(현재)에 더해, **전용 관리 화면**으로 날짜 범위 + 사유 관리

**DB 마이그레이션**: `V7_2_0__create_room_ooo_oos_table.sql`
```sql
CREATE TABLE htl_room_unavailable (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    room_number_id  BIGINT NOT NULL REFERENCES htl_room_number(id),
    unavailable_type VARCHAR(10) NOT NULL,   -- OOO, OOS
    reason_code     VARCHAR(20),              -- MAINTENANCE, RENOVATION, SHOWROOM 등
    reason_detail   VARCHAR(500),
    from_date       DATE NOT NULL,
    through_date    DATE NOT NULL,
    return_status   VARCHAR(20) DEFAULT 'DIRTY',  -- OOO/OOS 해제 시 복귀 상태
    -- audit
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);
```

**사유 코드 (reason_code) 초기 목록**:
- OOO: MAINTENANCE(보수), RENOVATION(리모델링), DAMAGE(손상), PLUMBING(배관), ELECTRICAL(전기)
- OOS: SHOWROOM(전시), VIP_HOLD(VIP 대기), STAFF_USE(직원사용), DEEP_CLEAN(특별청소), INSPECTION(점검)

**백엔드**: `RoomUnavailableController`, `RoomUnavailableService`, `RoomUnavailable` 엔티티
**프론트엔드**: `front-desk/room-unavailable.html`, `fd-room-unavailable-page.js`
**사이드바**: 프론트데스크 > OOO/OOS 관리

**Room Rack 연동**:
- OOO/OOS 객실 카드에 사유 + 기간 표시
- `htl_room_unavailable`에 현재 유효한 레코드가 있으면 `RoomNumber.hkStatus`를 자동 동기화

### Step 1 산출물 요약

| 구분 | 신규 | 수정 |
|------|------|------|
| Java | FrontDeskApiController, FrontDeskService(I+Impl), FrontDeskResponse, RoomUnavailable(Entity+Repo+Controller+Service+DTO) = **~10개** | SubReservation(+eta/etd), ReservationCreateRequest = **2개** |
| HTML | operations.html, room-unavailable.html = **2개** | sidebar.html = **1개** |
| JS | fd-operations-page.js, fd-room-unavailable-page.js = **2개** | hola-common.js(+quickActions) = **1개** |
| SQL | V4_17_0, V7_2_0 = **2개** | - |
| **합계** | **~16개 신규** | **~4개 수정** |

---

## Step 2: 고객 프로필 기초

### 포함 항목
- **M1**: Guest Profile 독립 모듈 (게스트 전용, Phase 1 범위)

### 설계 방향

현재: 게스트 정보가 `MasterReservation`에 직접 내장 (guestNameKo, email, phone 등)
목표: `GuestProfile` 독립 엔티티 → 예약 시 연결, 재방문 인식

**핵심 원칙**: 기존 MasterReservation의 게스트 필드는 유지 (하위 호환). GuestProfile은 **선택적 연결**.

### DB 마이그레이션: `V1_14_0__create_guest_profile.sql`

```sql
CREATE TABLE htl_guest_profile (
    id                  BIGSERIAL PRIMARY KEY,
    property_id         BIGINT NOT NULL REFERENCES htl_property(id),
    -- 이름
    guest_name_ko       VARCHAR(100),
    guest_first_name_en VARCHAR(50),
    guest_middle_name_en VARCHAR(50),
    guest_last_name_en  VARCHAR(50),
    -- 연락처
    phone_country_code  VARCHAR(5),
    phone_number        VARCHAR(20),
    email               VARCHAR(200),
    -- 개인정보
    birth_date          DATE,
    gender              VARCHAR(1),
    nationality         VARCHAR(5),
    -- 신분증
    id_type             VARCHAR(20),       -- PASSPORT, NATIONAL_ID, DRIVER_LICENSE
    id_number           VARCHAR(100),
    id_expiry_date      DATE,
    -- VIP/멤버십
    vip_code            VARCHAR(10),
    membership_number   VARCHAR(50),
    membership_level    VARCHAR(20),
    -- 선호도
    preferred_room_type VARCHAR(50),
    preferred_floor     VARCHAR(10),
    special_requests    TEXT,
    -- 통계
    total_stays         INTEGER DEFAULT 0,
    total_nights        INTEGER DEFAULT 0,
    last_stay_date      DATE,
    -- audit
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,
    use_yn              BOOLEAN DEFAULT TRUE,
    sort_order          INTEGER DEFAULT 0
);

-- 예약과 연결
ALTER TABLE rsv_master_reservation
    ADD COLUMN guest_profile_id BIGINT REFERENCES htl_guest_profile(id);
```

**모듈 위치**: `hola-hotel` (고객은 호텔 도메인)

**백엔드**:

| 파일 | 설명 |
|------|------|
| `GuestProfile.java` | 엔티티 |
| `GuestProfileRepository.java` | 리포지토리 |
| `GuestProfileService.java` / `Impl` | 서비스 (CRUD + 중복 검색 + 통계 업데이트) |
| `GuestProfileApiController.java` | REST API |
| `GuestProfileViewController.java` | 뷰 컨트롤러 |
| `GuestProfileMapper.java` | 매퍼 |
| `GuestProfileCreateRequest.java` | 요청 DTO |
| `GuestProfileResponse.java` | 응답 DTO |

**프론트엔드**:

| 파일 | 설명 |
|------|------|
| `guest-profile/list.html` | 고객 목록 (검색: 이름/전화/이메일) |
| `guest-profile/form.html` | 고객 등록/수정 폼 + 투숙 이력 탭 |
| `guest-profile-page.js` | 리스트 JS |
| `guest-profile-form.js` | 폼 JS |

**예약 연동**:
- 예약 폼에서 게스트 이름 입력 시 → 기존 프로필 자동검색 (전화번호/이메일 기준)
- 매칭되면 `guest_profile_id` 연결 + 프로필 정보 자동 채움
- 매칭 안 되면 체크인 시 자동 프로필 생성 (옵션)

**사이드바**: 회원관리 하위 → 고객 관리 메뉴 추가

### Step 2 산출물 요약

| 구분 | 신규 | 수정 |
|------|------|------|
| Java | Entity+Repo+Service(I+Impl)+Controller(2)+Mapper+DTO(2) = **~10개** | MasterReservation(+guestProfileId) = **1개** |
| HTML | list.html, form.html = **2개** | sidebar.html, reservation-form.html = **2개** |
| JS | page.js, form.js = **2개** | reservation-form.js(+프로필검색) = **1개** |
| SQL | V1_14_0 = **1개** | - |
| **합계** | **~15개 신규** | **~4개 수정** |

---

## Step 3: 하우스키핑 (Phase D)

### 포함 항목
- 하우스키핑 보드 (카드뷰 — OPERA 벤치마킹)
- Task Sheet 생성 (단순 객실 수 분배)
- 청소 상태 업데이트 + 프론트데스크 연동
- Inspection 체크리스트

> 기존 계획서 참조: `project-phase-d-deferred.md`
> 별도 상세 Plan 작성 예정 — 여기서는 개요만

### 핵심 엔티티

```
htl_housekeeping_task       -- 청소 작업 단위
htl_housekeeping_assignment -- 담당자 배정 (Task Sheet)
htl_housekeeper             -- 하우스키퍼 (담당자)
htl_inspection_checklist    -- 점검 항목
htl_inspection_result       -- 점검 결과
```

### 의존성
- Step 1의 OOO/OOS 관리가 선행되어야 HK 보드에서 정확한 객실 상태 표시 가능
- Room Rack의 hkStatus 업데이트 로직 재사용

### 예상 규모
- Java: ~20개 (Entity 5 + Repo 5 + Service 3 + Controller 3 + DTO 4)
- HTML: 3개 (보드, Task Sheet, 점검)
- JS: 3개
- SQL: 2~3개

---

## Step 4: 예약 운영 확장

### 포함 항목
- **M4**: Reservation Trace (부서별 업무 전달)
- **M6**: 확인서/등록카드 PDF 발행
- **M3**: Group/Block 예약 기초

### 4.1 Reservation Trace (부서별 업무 전달)

**현재**: `ReservationMemo` (단순 텍스트 메모, append-only)
**목표**: 부서 지정 + 완료 추적이 가능한 Trace 시스템

**DB 마이그레이션**: `V4_18_0__create_reservation_trace.sql`
```sql
CREATE TABLE rsv_reservation_trace (
    id                      BIGSERIAL PRIMARY KEY,
    master_reservation_id   BIGINT NOT NULL REFERENCES rsv_master_reservation(id),
    department_code         VARCHAR(20) NOT NULL,  -- FRONT_DESK, HOUSEKEEPING, CONCIERGE, F_AND_B, MAINTENANCE
    trace_date              DATE NOT NULL,          -- 실행 예정일
    description             TEXT NOT NULL,
    is_completed            BOOLEAN DEFAULT FALSE,
    completed_at            TIMESTAMP,
    completed_by            VARCHAR(50),
    -- audit
    created_at              TIMESTAMP DEFAULT NOW(),
    created_by              VARCHAR(50)
);
```

**부서 코드**: FRONT_DESK, HOUSEKEEPING, CONCIERGE, F_AND_B, MAINTENANCE, BELL_SERVICE, ENGINEERING

**구현**: 예약 상세 화면에 "Trace" 탭 추가
- Trace 등록: 부서 선택 + 날짜 + 내용
- Trace 조회: 부서별/완료여부 필터
- 프론트데스크 Operations에서 당일 미완료 Trace 알림 표시

### 4.2 확인서/등록카드 PDF 발행

**기술 선택**: Thymeleaf PDF 렌더링 (Flying Saucer / OpenHTMLToPDF)
- 별도 템플릿 엔진 불필요 — 기존 Thymeleaf 인프라 재사용

**템플릿 2종**:

| 문서 | 내용 | 출력 시점 |
|------|------|----------|
| **예약 확인서** (Confirmation Letter) | 예약번호, 게스트명, 체크인/아웃, 객실타입, 요금, 결제현황, 호텔정보 | 예약 완료 후 |
| **투숙 등록카드** (Registration Card) | 게스트 정보, 서명란, 약관 동의, 동반투숙객, 특별요청 | 체크인 시 |

**구현**:
- `PdfService.java`: Thymeleaf → HTML → PDF 변환
- `DocumentApiController.java`: GET /api/v1/.../reservations/{id}/confirmation-pdf, /registration-card-pdf
- 예약 상세 화면에 "확인서 출력", "등록카드 출력" 버튼 추가

**의존성**: OpenHTMLToPDF 라이브러리 추가 (build.gradle)

### 4.3 Group/Block 예약 기초

**범위 (Phase 1 최소)**: 단체 예약을 하나의 블록으로 묶는 기능

**DB 마이그레이션**: `V4_19_0__create_reservation_block.sql`
```sql
CREATE TABLE rsv_reservation_block (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES htl_property(id),
    block_code      VARCHAR(20) NOT NULL,
    block_name      VARCHAR(200) NOT NULL,
    contact_name    VARCHAR(100),
    contact_phone   VARCHAR(20),
    contact_email   VARCHAR(200),
    arrival_date    DATE NOT NULL,
    departure_date  DATE NOT NULL,
    total_rooms     INTEGER DEFAULT 0,
    cutoff_date     DATE,                     -- 블록 해제 기한
    status          VARCHAR(20) DEFAULT 'TENTATIVE',  -- TENTATIVE, DEFINITE, CANCELLED
    notes           TEXT,
    -- audit
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    use_yn          BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0
);

-- 예약-블록 연결
ALTER TABLE rsv_master_reservation
    ADD COLUMN block_id BIGINT REFERENCES rsv_reservation_block(id);
```

**MVP 기능**:
- 블록 등록/수정/삭제 (CRUD)
- 예약 생성 시 블록 선택 (선택적)
- 블록별 예약 목록 조회
- 컷오프 날짜 이후 미사용 객실 자동 해제 (배치/수동)

### Step 4 산출물 요약

| 구분 | Trace | PDF | Block | 합계 |
|------|-------|-----|-------|------|
| Java | 4개 | 3개 | 8개 | **~15개** |
| HTML | 0 (탭 추가) | 2 (PDF 템플릿) | 2 | **4개** |
| JS | 0 (기존 확장) | 0 | 2 | **2개** |
| SQL | 1개 | 0 | 1개 | **2개** |

---

## Step 5: 재무/정산 기초

### 포함 항목
- **C4**: Folio (투숙객 계정)
- **C3**: 일마감 (EOD) 프로세스
- **M5**: 미수금 (AR) 기초

> ⚠️ 이 Step은 가장 규모가 크고 복잡. 별도 상세 Plan 필요.
> 여기서는 아키텍처 방향성만 정의.

### 5.1 Folio (투숙객 계정)

**현재**: `ReservationPayment` (예약당 1개, 총액 관리)
**목표**: 예약당 N개 Folio (결제 주체별 분리)

```sql
CREATE TABLE rsv_folio (
    id                      BIGSERIAL PRIMARY KEY,
    master_reservation_id   BIGINT NOT NULL REFERENCES rsv_master_reservation(id),
    folio_number            VARCHAR(20) NOT NULL,
    folio_window            INTEGER DEFAULT 1,    -- Window 1, 2, 3...
    folio_type              VARCHAR(20) DEFAULT 'GUEST',  -- GUEST, COMPANY, TRAVEL_AGENT
    bill_to_name            VARCHAR(200),
    status                  VARCHAR(20) DEFAULT 'OPEN',   -- OPEN, CLOSED, TRANSFERRED
    total_charges           NUMERIC(15,2) DEFAULT 0,
    total_payments          NUMERIC(15,2) DEFAULT 0,
    balance                 NUMERIC(15,2) DEFAULT 0,
    -- audit ...
);

CREATE TABLE rsv_folio_transaction (
    id              BIGSERIAL PRIMARY KEY,
    folio_id        BIGINT NOT NULL REFERENCES rsv_folio(id),
    transaction_date TIMESTAMP NOT NULL,
    transaction_code_id BIGINT REFERENCES rm_transaction_code(id),
    description     VARCHAR(500),
    amount          NUMERIC(15,2) NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,  -- DEBIT(차변), CREDIT(대변)
    reference       VARCHAR(100),
    -- audit ...
);
```

**핵심 변경**:
- 기존 DailyCharge → Folio에 DEBIT으로 게시
- 기존 PaymentTransaction → Folio에 CREDIT으로 게시
- 체크아웃 시 Folio balance = 0 검증 (기존 remaining 검증 대체)

### 5.2 일마감 (EOD/Night Audit)

**프로세스**:
```
EOD 실행 전 체크리스트:
  ├── 모든 Arrivals 처리 (체크인 or 노쇼) ← 경고
  ├── 모든 Departures 처리 (체크아웃 완료) ← 경고
  ├── 모든 Open Folio 정산 확인 ← 차단
  └── 모든 캐셔 닫힘 확인 ← 차단 (캐셔 도입 시)

EOD 자동 처리:
  ├── 1. 투숙중(INHOUSE) 객실 → 당일 객실 요금 Folio에 자동 게시
  ├── 2. 객실 상태 일괄 업데이트 (OC → OD: 투숙 객실 Dirty 전환)
  ├── 3. Business Date 증가 (오늘 → 내일)
  └── 4. EOD 리포트 자동 생성

자동 생성 리포트 (MVP 3종):
  ├── Manager Flash Report (당일 KPI 요약)
  ├── Guest Ledger (투숙객 원장)
  └── Cashier Summary (결제 요약)
```

### 5.3 미수금 (AR) 기초

```sql
CREATE TABLE fin_ar_account (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL,
    account_type    VARCHAR(20) NOT NULL,   -- COMPANY, TRAVEL_AGENT, GROUP, GOVERNMENT
    account_name    VARCHAR(200) NOT NULL,
    account_number  VARCHAR(20) UNIQUE,
    credit_limit    NUMERIC(15,2),
    balance         NUMERIC(15,2) DEFAULT 0,
    payment_terms   INTEGER DEFAULT 30,      -- 결제 기한 (일)
    -- audit ...
);
```

**MVP 기능**: 계정 CRUD + Folio에서 AR로 이전 (Transfer to AR) + 잔액 조회

### Step 5 예상 규모
- Java: ~30개
- HTML: 5~6개
- JS: 5~6개
- SQL: 5~6개
- **별도 상세 Plan 문서 필요**

---

## Step 6: 감사/리포트

### 포함 항목
- **M7**: Changes Log (상세 감사 추적)
- **리포트 시스템 기초**

### 6.1 Changes Log

**설계**: JPA EntityListener 기반 자동 변경 이력 수집

```sql
CREATE TABLE sys_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT,
    entity_type     VARCHAR(50) NOT NULL,    -- RESERVATION, ROOM_NUMBER, GUEST_PROFILE 등
    entity_id       BIGINT NOT NULL,
    action_type     VARCHAR(20) NOT NULL,    -- CREATE, UPDATE, DELETE
    field_name      VARCHAR(100),            -- 변경된 필드명
    old_value       TEXT,
    new_value       TEXT,
    changed_by      VARCHAR(50) NOT NULL,
    changed_at      TIMESTAMP DEFAULT NOW(),
    ip_address      VARCHAR(45)
);
```

**구현**:
- `AuditLogListener.java` (@EntityListener) — 핵심 엔티티에 적용
- `AuditLogService.java` — 로그 조회 API
- 시스템관리 > 변경 이력 메뉴

### 6.2 리포트 시스템

**MVP 접근**: PDF 리포트 생성 (Step 4의 PdfService 재사용)
- Manager Report: 대시보드 KPI를 PDF로 출력
- 예약 리포트: 기간별 예약 목록/통계
- 매출 리포트: 기간별 매출 집계

### Step 6 예상 규모
- Java: ~8개
- HTML: 2개
- JS: 2개
- SQL: 1개

---

## Step 7: Minor 항목 (선택적)

| # | 항목 | 구현 여부 | 근거 |
|---|------|----------|------|
| m1 | Guest Message | **스킵** | 카카오 알림톡으로 대체 (Phase 3) |
| m2 | Property Brochure 확장 | **일부** | 어메니티/시설 정보 추가 (Step 2 Guest Profile과 함께) |
| m3 | Sellable Availability | **Phase 3** | 채널 관리와 함께 |
| m4 | Floor Plan | **스킵** | 개발 비용 대비 효과 낮음 |
| m5 | Manage Restrictions | **Phase 3** | 채널 관리와 함께 (CTA/CTD/Stop Sell) |
| m6 | QR Code | **Phase 3** | 셀프 체크인과 함께 |
| m7 | 전화번호부 | **스킵** | 한국 호텔 사용 빈도 없음 |
| m8 | Shift Reports | **Step 5** | EOD 리포트에 포함 |

---

## 의존성 다이어그램

```
Step 1: 프론트데스크 강화 ──────────────────┐
  (체크인/아웃 UI, OOO/OOS, Quick Action)    │
                                             │
Step 2: 고객 프로필 ────────────────────┐    │
  (Guest Profile, 예약 연동)            │    │
                                        ↓    ↓
Step 3: 하우스키핑 ◄──── Step 1 필요 (객실 상태 모델)
  (HK 보드, Task Sheet, Inspection)

Step 4: 예약 운영 확장 ◄── Step 2 필요 (프로필 → 등록카드)
  (Trace, PDF, Block)

Step 5: 재무/정산 ◄──── Step 1, 4 필요 (체크인/아웃 + Trace)
  (Folio, EOD, AR)

Step 6: 감사/리포트 ◄── Step 5 이후 (EOD 리포트 포함)
  (Changes Log, Reports)
```

**병렬 가능**: Step 1 + Step 2 (독립적)
**순차 필수**: Step 1 → Step 3, Step 2 → Step 4, Step 4 → Step 5 → Step 6

---

## 실행 순서 제안

| 주차 | Step | 핵심 산출물 | 마일스톤 |
|------|------|-----------|---------|
| **W1** (03-18~) | Step 1 + 2 병렬 | 프론트데스크 Operations + Guest Profile | "체크인/아웃이 UI에서 된다" |
| **W2** (03-23~) | Step 3 | 하우스키핑 보드 + Task Sheet | "청소 배정이 된다" |
| **W3** (03-30~) | Step 4 | Trace + PDF + Block 기초 | "확인서/등록카드 출력 된다" |
| **W4~5** (04-06~) | Step 5 | Folio + EOD | "일마감이 돌아간다" |
| **W6** (04-20~) | Step 6 | Changes Log + Reports | "감사/리포트 완성" |

→ **6주 후 MVP 완성 목표**: 예약 → 체크인 → 투숙(차지) → 체크아웃(정산) → 일마감 사이클

---

## 의사결정 필요 사항

| # | 결정 사항 | 제안 | 대안 |
|---|---------|------|------|
| **P1** | 프론트데스크 구조: 3분할 메뉴 vs 탭 통합 | **탭 통합** (Room Rack + Operations 2개 메뉴) | 3분할 복원 (OPERA 방식) |
| **P2** | Guest Profile 모듈 위치 | **hola-hotel** (고객은 호텔 도메인) | 별도 hola-guest 모듈 신설 |
| **P3** | PDF 라이브러리 | **OpenHTMLToPDF** (Thymeleaf 호환) | iText (유료 라이선스), JasperReports (복잡) |
| **P4** | Folio 도입 범위 | **Step 5에서 본격 도입** (기존 Payment 유지하다가 전환) | Step 1에서 바로 도입 (리스크 큼) |
| **P5** | Changes Log 범위 | **핵심 엔티티만** (Reservation, GuestProfile, RoomNumber) | 전 엔티티 (성능 이슈 가능) |

---

*이 계획서는 협의 후 확정. 각 Step 착수 전 상세 설계 진행.*
