# 올라 PMS 중간 점검 보고서

> 작성일: 2026-03-18
> 목적: OPERA Cloud 벤치마킹 기반 프로젝트 방향성 점검 및 이정표 수립
> 원칙: 맹목적인 OPERA 추종 지양, 올라 PMS 고유 강점 강화

---

## Executive Summary

| 항목 | 현황 |
|------|------|
| 개발 기간 | ~3주 (2026-02-28 ~ 03-18) |
| 완료 모듈 | M01 호텔, M02 객실, M03 레이트, M07 예약, 회원/권한관리 |
| Java 파일 | 426개 |
| API 엔드포인트 | 100+ |
| HTML 템플릿 | 50+ |
| Flyway 마이그레이션 | 76개 (V7.1.0) |
| DB 테이블 | 50+ |
| **OPERA Cloud 대비 커버리지** | **핵심 운영 기능 ~45%, 전체 기능 ~25%** |

---

## 1. 현재 개발 항목 중 놓친 부분 점검

### 1.1 이미 잘 되어 있는 것 (Keep)

| 영역 | 올라 PMS 구현 | 평가 |
|------|-------------|------|
| **멀티테넌시** | Schema-per-Tenant | OPERA보다 진보적. OPERA는 Property Code 기반 필터링 |
| **예약 구조** | Master/Sub 예약 + DailyCharge | 업계 표준 충족. OPERA와 동등한 수준 |
| **가격 계산** | PriceCalculationService (레이트코드/프로모션 기반) | 핵심 비즈니스 로직 견고 |
| **결제 관리** | Payment + PaymentAdjustment + Deposit | 이력 관리까지 잘 설계됨 |
| **권한 체계** | 3단계 역할 + 3-depth 메뉴트리 | OPERA의 4단계보다 간결하지만 한국 시장에 적합 |
| **프로모션** | 12종 타입 체계 | OPERA보다 한국형 프로모션에 특화 (관공서, 장기투숙 등) |
| **Soft Delete** | 전 엔티티 @SQLRestriction | 데이터 보전 철저 |
| **감사 추적** | BaseEntity createdBy/updatedBy | 기본 감사 기능 충족 |
| **예약 캘린더** | 카드뷰 + 테이블뷰 전환 | OPERA의 Room Diary 대비 간결하지만 기능적 |
| **Room Rack** | 객실현황 시각화 | 프론트데스크 핵심 기능 구현 완료 |
| **부킹 엔진** | 공개 API + 외부 예약 지원 | OPERA에는 없는 자체 부킹 엔진 (강점) |

### 1.2 놓친 부분 — 심각도별 분류

#### 🔴 Critical (운영 불가능 수준 — Phase 1 내 해결 필수)

| # | 놓친 항목 | OPERA에서의 구현 | 올라 PMS 영향 | 제안 |
|---|---------|----------------|-------------|------|
| C1 | **체크인/체크아웃 워크플로우** | Arrivals → Room Assignment → Registration Card → Check In (4단계) | 현재 예약 상태만 변경 가능, 실제 체크인 프로세스 없음 | 프론트데스크 체크인/아웃 화면 구현 (이미 분석 완료) |
| C2 | **객실 상태 6단계 모델** | VC/VD/OC/OD/OOO/OOS | htl_room_status 테이블 존재하지만 하우스키핑 연동 미완성 | Room Status 상태 머신 + 자동 전이 로직 |
| C3 | **일마감(Night Audit/EOD)** | 캐셔 정산 + 자동 리포트 10종 생성 | 전무 | Phase 2 M09 핵심. 일마감 없이는 재무 데이터 신뢰 불가 |
| C4 | **Folio(투숙객 계정) 개념** | 예약별 다중 Folio (Window) 관리 | Payment/DailyCharge는 있으나 Folio 개념 부재 | Folio = 투숙 기간의 모든 차지를 모은 전표. 체크아웃 시 정산 단위 |

#### 🟡 Major (운영 품질 저하 — Phase 1~2 내 해결 권장)

| # | 놓친 항목 | OPERA에서의 구현 | 올라 PMS 영향 | 제안 |
|---|---------|----------------|-------------|------|
| M1 | **Guest Profile (고객 DB)** | Client Relations 전용 모듈, 통합 프로필 (게스트/기업/여행사) | 예약에 게스트 정보 내장, 독립 고객 DB 없음 | 재방문 고객 인식, VIP 관리, CRM 기반 부재 |
| M2 | **OOO/OOS 분리 관리** | 별도 메뉴, Reason Code + Return Status + 날짜 범위 | 없음 | 인벤토리 정확성에 직결. OOO는 가용재고 차감, OOS는 미차감 |
| M3 | **Group/Block 예약** | 전용 화면 (Group Rooms Control) | 없음 | 3~5성급 호텔의 단체 예약(컨퍼런스, 웨딩 등) 필수 기능 |
| M4 | **Reservation Trace** | 부서별 업무 전달 시스템 | 예약 메모만 존재 | VIP 특별 요청, 부서간 연계 (하우스키핑에 "꽃 배치" 등) |
| M5 | **미수금(AR) 관리** | Accounts Receivable 전용 섹션 | 없음 | 기업 계약, 여행사 후불, 단체 예약 결제 필수 |
| M6 | **확인서/등록카드 발행** | Confirmation Letters, Registration Cards 일괄 처리 | 없음 | 법적 요구사항 (숙박업법), 업무 효율 |
| M7 | **Changes Log (상세 감사)** | Group/Action Type/User/Date 기반 변경 이력 | BaseEntity 수준만 존재 | 감사 대응, 운영 이슈 추적에 필요 |

#### 🟢 Minor (편의성/경쟁력 — Phase 2~3에서 점진 도입)

| # | 놓친 항목 | OPERA에서의 구현 | 제안 |
|---|---------|----------------|------|
| m1 | Guest Message (전언) | 카드뷰, Delivered/Not Delivered 추적 | Phase 3 — 디지털 메시징으로 대체 가능 |
| m2 | Property Brochure (프로퍼티 종합 정보) | 6대분류 12탭 | M01 프로퍼티에 어메니티/교통편/시설 확장 |
| m3 | Sellable Availability (채널별 판매한도) | 바 차트 + Channel Sell Limits | Phase 3 채널관리 시 |
| m4 | Floor Plan (인터랙티브 평면도) | SVG 기반 시각화 | 차별화 요소이나 개발 비용 높음. Phase 3+ |
| m5 | Manage Restrictions (판매 제한) | 캘린더 뷰 Rate/Room 제한 | Phase 3 채널관리 시 CTA/CTD/Stop Sell |
| m6 | QR Code (서류 QR) | Stationery에 QR 삽입 | 셀프 체크인 등 디지털 전환 시 |
| m7 | 전화번호부 | 카테고리별 내/외선 관리 | 한국 호텔에서 실사용 빈도 낮음. 스킵 가능 |
| m8 | Shift Reports (교대 리포트) | 프론트/하우스키핑 교대 인수인계 | Phase 2 리포트 시스템 시 |

---

## 2. OPERA Cloud vs 올라 PMS — 설계 철학 차이 분석

### 2.1 아키텍처 철학

| 관점 | OPERA Cloud | 올라 PMS | 분석 |
|------|-----------|---------|------|
| **메뉴 구조** | 7개 최상위 + Workspace 하위 (기능 중심) | 모듈별 사이드바 (관리 대상 중심) | OPERA는 "무엇을 할 것인가"(동사), 올라는 "무엇을 관리할 것인가"(명사) |
| **검색 패턴** | Basic/Advanced 토글, 30개+ 필터 | 단순 검색 + DataTable 필터 | OPERA는 대규모 호텔(500실+) 기준, 올라는 중규모(100~300실)에 적합 |
| **뷰 패턴** | 4가지 뷰 모드 전환 (Table/List/Grid/Split) | 테이블 + 카드뷰 2모드 | 올라가 더 간결. 4모드는 오버엔지니어링일 수 있음 |
| **액션 패턴** | "I Want To..." 행별 드롭다운 | 상세 페이지 이동 후 액션 | OPERA는 리스트에서 바로 처리, 올라는 상세 진입 후 처리 |
| **프로퍼티 컨텍스트** | 모든 검색에 Property 필수 필드 | sessionStorage 기반 글로벌 컨텍스트 | 올라 방식이 UX 상 우월 (매번 프로퍼티 선택 불필요) |
| **멀티테넌시** | Property Code 필터링 (단일 스키마) | Schema-per-Tenant (스키마 분리) | 올라가 데이터 격리 면에서 우월 |
| **프론트엔드** | SPA (Oracle JET 기반) | SSR (Thymeleaf + jQuery) | OPERA가 UX 면에서 유리하나, 올라는 안정성/SEO에 강점 |

### 2.2 OPERA가 그렇게 한 이유 분석

#### (1) "I Want To..." 패턴이 있는 이유
- **호텔 프론트 데스크 = 시간 싸움**. 체크인 러시에 페이지 이동 최소화가 핵심
- 리스트에서 바로 "체크인", "객실배정", "폴리오 조회" 가능 → 클릭 1회 절약
- **올라 PMS 도입 검토**: DataTable 행에 드롭다운 액션 메뉴 추가 (reservation-table-view.js에 적용 가능)

#### (2) Folio(계정) 개념을 별도로 두는 이유
- 하나의 예약에 **여러 결제 주체**가 있을 수 있음 (본인 + 회사 + 여행사)
- Folio Window 1 = 숙박비(회사 부담), Window 2 = 미니바/기타(개인 부담)
- **올라 PMS 현황**: Payment 엔티티로 결제를 관리하지만, "누가 어느 비용을 부담하는가"의 분리가 없음
- **도입 우선순위**: Phase 2 정산 모듈에서 Folio 개념 도입 필요

#### (3) 캐셔 별도 인증이 있는 이유
- **재무 감사 대응**: "누가 이 결제를 처리했는가"에 대한 법적 추적 필요
- 교대 근무에서 같은 PC를 여러 직원이 사용 → 시스템 로그인과 캐셔를 분리
- **올라 PMS 도입 검토**: 한국 호텔에서는 POS 연동으로 해결하는 경우가 많음. MVP에서는 생략 가능하나, Phase 2에서 캐셔 세션 개념은 필요

#### (4) OOO/OOS를 분리하는 이유
- **OOO (Out of Order)**: 완전 판매 불가. 가용 재고에서 차감. 장기 (수리/리모델링)
- **OOS (Out of Service)**: 비선호하나 필요시 판매 가능. 가용 재고에서 미차감. 단기 (수도 고장 등)
- **재고 계산 공식**: `Available Rooms = Total - OOO - Occupied` (OOS는 차감 안 함)
- **올라 PMS 도입**: 필수. htl_room_status에 OOO/OOS 구분 + reason_code 추가

#### (5) Credits 기반 하우스키핑 업무 분배 이유
- 체크아웃 객실(풀청소) ≠ 재실 객실(간단청소). 단순 객실 수 분배는 불공정
- Credit 예시: 체크아웃 = 3 credit, 재실 = 1 credit, 스위트 = 5 credit
- **올라 PMS 도입 검토**: Phase D 하우스키핑에서 고려. MVP에서는 단순 객실 수 분배 후 v2에서 Credit 도입

---

## 3. 올라 PMS 고유 강점 분석

### 3.1 OPERA 대비 올라 PMS가 우월한 점

| # | 강점 | 상세 | OPERA와의 차이 |
|---|------|------|---------------|
| S1 | **Schema-per-Tenant 멀티테넌시** | 프로퍼티 간 완전한 데이터 격리 | OPERA는 Property Code 필터링 방식 → 데이터 유출 위험 존재 |
| S2 | **글로벌 프로퍼티 컨텍스트** | sessionStorage 기반, 한 번 선택하면 전 화면 적용 | OPERA는 매 화면마다 Property 선택 필요 (30개+ 검색 필드 중 하나) |
| S3 | **자체 부킹 엔진 API** | RESTful 공개 API + API Key 관리 + 감사 로그 | OPERA는 별도 OWS(Oracle Web Services) 구매 필요 |
| S4 | **한국형 프로모션 체계** | 관공서, 장기투숙, 조기예약 등 한국 시장 특화 12종 | OPERA는 글로벌 범용. 한국 특수 프로모션 커스터마이징 별도 필요 |
| S5 | **간결한 UI/UX** | Bootstrap 5 + DataTables, 학습 비용 낮음 | OPERA는 Oracle JET 기반, 복잡한 검색 패널 → 교육 비용 높음 |
| S6 | **Modular Monolith** | 모듈 간 명확한 의존성, 단일 배포 | OPERA는 마이크로서비스 → 운영 복잡도 높음 |
| S7 | **조기/지연 수수료 정책** | EarlyLateCheckService, 정책 엔티티 기반 | OPERA에서는 Admin에서 설정하지만 전용 관리 UI는 약함 |
| S8 | **환율 관리** | ExchangeRate 엔티티, 다통화 지원 기반 | OPERA도 지원하지만 올라는 한국 원화 기준으로 최적화 가능 |
| S9 | **Pretendard + 한글 최적화** | 한글 폰트/라벨/pageSizeSelect 완전 한국어 | OPERA는 다국어 지원하지만 한글 UX가 자연스럽지 않음 |
| S10 | **오픈 기술 스택** | Java 17 + Spring Boot + PostgreSQL (오픈소스) | OPERA는 Oracle DB + 독점 기술 → 벤더 종속 |

### 3.2 강화해야 할 올라 PMS만의 차별화 포인트

| # | 차별화 방향 | 상세 | 기대 효과 |
|---|-----------|------|----------|
| D1 | **모바일 퍼스트 프론트데스크** | 태블릿/모바일에서 체크인/아웃 처리 | OPERA의 데스크톱 중심 UI 대비 현장 운영 유연성 |
| D2 | **실시간 대시보드** | WebSocket 기반 실시간 객실 상태 업데이트 | OPERA는 "Refresh" 버튼 수동 갱신 |
| D3 | **한국 결제 연동** | PG사 직접 연동 (토스/이니시스/KG이니시스) | OPERA는 글로벌 게이트웨이 → 한국 PG 연동 복잡 |
| D4 | **한국 세무 연동** | 국세청 e-Tax 전자세금계산서 자동 발행 | OPERA는 Fiscal Payloads로 유럽/일본 특화, 한국 대응 약함 |
| D5 | **카카오/네이버 알림** | 예약 확인, 체크인 안내 카카오 알림톡/네이버 톡톡 | OPERA는 Email/SMS 중심 |
| D6 | **간편 셀프 체크인** | QR 기반 모바일 체크인 + 키오스크 연동 | OPERA도 지원하지만 별도 모듈 구매 필요 |

---

## 4. OPERA 벤치마킹 — 도입 검토 항목

### 4.1 즉시 도입 (Phase 1 진행 중인 작업에 반영)

| # | OPERA 기능 | 올라 PMS 적용 방안 | 개발 규모 | 비고 |
|---|-----------|------------------|----------|------|
| B1 | **Arrivals/Departures/InHouse 3분할** | 프론트데스크 하위 메뉴 3개 복원 + Room Rack | 소 | 이미 분석 완료, git에 삭제 상태 |
| B2 | **행별 Quick Action ("I Want To...")** | DataTable 행에 드롭다운 액션 추가 | 소 | `HolaPms.renders.actionButtons` 확장 |
| B3 | **OOO/OOS 분리** | htl_room_status에 type(OOO/OOS) + reason_code + return_status 추가 | 중 | 객실 가용성 계산에 반영 필수 |
| B4 | **ETA/ETD 필드** | SubReservation에 eta/etd 필드 추가 | 소 | 프론트데스크 도착 예정 시간 관리 |

### 4.2 단기 도입 (Phase 2, 2~4주 내)

| # | OPERA 기능 | 올라 PMS 적용 방안 | 개발 규모 |
|---|-----------|------------------|----------|
| B5 | **Guest Profile 독립 모듈** | 게스트/기업/여행사 통합 프로필 엔티티 | 대 |
| B6 | **Folio (투숙객 계정)** | 예약별 Folio 엔티티 + Window(결제 분리) | 대 |
| B7 | **일마감(EOD) 프로세스** | 야간 감사 + 자동 차지 게시 + 리포트 생성 | 대 |
| B8 | **하우스키핑 보드** | 카드뷰 객실 현황 + 청소 상태 관리 | 중 |
| B9 | **Task Sheet 생성** | 담당자별 객실 배분 (단순 개수 기반 우선) | 중 |
| B10 | **확인서/등록카드 발행** | PDF 생성 (iText/Thymeleaf PDF) | 중 |

### 4.3 중기 도입 (Phase 3, 1~2개월 내)

| # | OPERA 기능 | 올라 PMS 적용 방안 | 개발 규모 |
|---|-----------|------------------|----------|
| B11 | **Group/Block 예약** | BlockReservation 엔티티 + 그룹 할당 UI | 대 |
| B12 | **미수금(AR) 관리** | Account 엔티티 + Invoice/Payment 연동 | 대 |
| B13 | **판매 제한(Restrictions)** | RateRestriction + 캘린더 UI | 중 |
| B14 | **Sellable Availability** | 채널별 판매한도 관리 + 차트 | 중 |
| B15 | **Reservation Trace** | 부서별 업무 전달 + 완료 추적 | 중 |
| B16 | **리포트 시스템** | 정의 가능한 리포트 + PDF/Excel 생성 | 대 |

### 4.4 맹목적 OPERA 추종을 지양할 항목 (도입 불필요)

| # | OPERA 기능 | 지양 이유 |
|---|-----------|----------|
| X1 | **4가지 뷰 모드 전환** | 2모드(테이블+카드)로 충분. 과잉 UI |
| X2 | **30개+ Advanced Search 필터** | 한국 중규모 호텔에서 불필요. 핵심 5~10개 필터가 실용적 |
| X3 | **Floor Plan (인터랙티브 평면도)** | 개발 비용 대비 사용 빈도 낮음. 도심 호텔은 층 구조 단순 |
| X4 | **Site Plan (부지 배치도)** | 리조트 전용. 올라 PMS 타겟(도심 호텔)에 불필요 |
| X5 | **전화번호부/전화 오퍼레이터** | 한국 호텔은 내선 시스템이 별도. PMS에서 관리 불필요 |
| X6 | **캐셔 별도 인증** | 한국은 POS 연동이 일반적. MVP에서는 기존 인증으로 충분 |
| X7 | **Property Brochure 12탭** | 프로퍼티 기본 정보 + 이미지로 충분. 교통편/관광지는 외부 서비스 |
| X8 | **Trip Composer (예약 장바구니)** | 멀티세그먼트 예약은 한국 호텔에서 극히 드문 사용 케이스 |
| X9 | **Mass Cancellation (일괄 취소)** | 자연재해 등 극단 상황. 개별 취소 + 필터로 대체 가능 |
| X10 | **Sell Messages (마케팅 메시지)** | 한국 호텔에서 프론트에서 업셀하는 문화 약함. 카카오 알림이 더 효과적 |

---

## 5. 프로젝트 이정표 (Milestone) 재정립

### 5.1 수정된 Phase 계획

```
Phase 1: 핵심 운영 (현재 ~ +2주) ──────────────────────────────────
  ├── M10 프론트데스크 (체크인/아웃 워크플로우) ← 이번 주
  ├── 객실 상태 모델 보강 (OOO/OOS 분리, 6단계)
  ├── Guest Profile 기초 (재방문 인식 수준)
  └── ETA/ETD + Quick Action 패턴 도입

Phase D: 하우스키핑 (2026-03-23 주차) ─────────────────────────────
  ├── 하우스키핑 보드 (카드뷰)
  ├── Task Sheet 생성 (단순 객실 수 분배)
  ├── 청소 상태 업데이트 (프론트데스크 연동)
  └── Inspection 체크리스트

Phase 2: 재무/정산 (Phase D 이후 ~ +3주) ──────────────────────────
  ├── Folio 개념 도입 (결제 주체 분리)
  ├── 일마감(EOD) 프로세스
  ├── EOD 리포트 (매니저 리포트, 캐셔 요약, 게스트 원장)
  ├── 미수금(AR) 기초
  ├── 확인서/등록카드 PDF 발행
  └── 변경 이력 로그 (Audit Trail 강화)

Phase 3: 채널/고도화 (Phase 2 이후) ───────────────────────────────
  ├── 채널 매니저 연동 (판매 제한, 채널별 한도)
  ├── Group/Block 예약
  ├── 리포트 시스템 (정의 가능 리포트)
  ├── Guest Profile 확장 (CRM, VIP 등급)
  └── Reservation Trace (부서간 업무 전달)
```

### 5.2 의사결정 필요 사항

| # | 결정 사항 | 선택지 | 추천 | 근거 |
|---|---------|--------|------|------|
| D1 | **프론트데스크 3분할 복원 vs Room Rack 중심** | A: Arrivals/Departures/InHouse 3개 메뉴 복원 / B: Room Rack에 상태 필터 통합 | **B안** | 올라의 강점인 간결함 유지. Room Rack + 상태 필터로 OPERA의 3분할과 동일 기능을 더 적은 화면에서 제공 |
| D2 | **Guest Profile 범위** | A: 게스트만 / B: 게스트+기업+여행사 통합 | **A안** (Phase 1) | 기업/여행사는 AR 모듈과 함께 Phase 2에서. MVP는 게스트만 |
| D3 | **Folio 도입 시점** | A: Phase 1에서 기본 도입 / B: Phase 2에서 본격 도입 | **B안** | 현재 Payment 구조가 잘 작동 중. Folio는 AR/EOD와 함께 설계해야 일관성 유지 |
| D4 | **하우스키핑 Task Sheet 복잡도** | A: Credit 시스템 포함 / B: 단순 객실 수 분배 | **B안** (Phase D) | MVP는 단순 분배. Credit은 운영 데이터 축적 후 v2에서 도입 |
| D5 | **프론트엔드 기술** | A: Thymeleaf 유지 / B: React 전환 시작 | **A안** (당분간) | 현재 50+ 템플릿의 재작성 비용 과대. 기능 완성 후 UX 고도화 시 전환 검토 |

---

## 6. 종합 평가

### 6.1 현재 위치 (3주차)

```
올라 PMS 개발 진척률 (기능별)

호텔/프로퍼티 관리  ████████████████████░  95%  ← 거의 완료
객실 관리          ████████████████████░  95%  ← 거의 완료
레이트/프로모션     ████████████████████░  95%  ← 거의 완료
예약 관리          ██████████████████░░░  85%  ← Guest Profile/Group 미완
회원/권한 관리      ████████████████████░  95%  ← 거의 완료
프론트데스크        ████████░░░░░░░░░░░░  40%  ← Room Rack만. 체크인/아웃 미완
하우스키핑          ░░░░░░░░░░░░░░░░░░░░   0%  ← 미착수 (Phase D)
재무/정산           ░░░░░░░░░░░░░░░░░░░░   0%  ← 미착수 (Phase 2)
채널 관리           ░░░░░░░░░░░░░░░░░░░░   0%  ← 미착수 (Phase 3)
리포트              ████░░░░░░░░░░░░░░░░  20%  ← 대시보드 KPI만
```

### 6.2 핵심 메시지

1. **기초 공사는 탄탄하다**: 예약/객실/레이트의 데이터 모델과 비즈니스 로직이 업계 표준을 충족. 3주 만에 이 수준은 매우 빠른 진행.

2. **"관리"는 됐고, 이제 "운영"이 필요하다**: 현재까지는 데이터 CRUD(등록/수정/삭제/조회)에 집중. 이제 실제 호텔 운영 워크플로우(체크인 → 투숙 → 차지 → 체크아웃 → 정산)를 구현할 단계.

3. **OPERA를 따라갈 필요 없다**: OPERA는 500실+ 대형 체인 호텔 기준. 올라 PMS는 100~300실 한국 호텔에 최적화된 "간결하지만 빠짐없는" PMS를 목표로 해야 한다.

4. **다음 마일스톤의 핵심은 "숙박 라이프사이클"**: 예약 → 체크인 → 투숙(차지 발생) → 체크아웃(정산) → 일마감. 이 사이클이 돌아가면 MVP 완성.

### 6.3 우선순위 요약 (한 줄)

> **프론트데스크 체크인/아웃 → 하우스키핑 → Folio/EOD 순서로,**
> **"예약부터 정산까지 한 바퀴 도는 것"이 최우선 목표.**

---

## 부록: OPERA Cloud 모듈 vs 올라 PMS 매핑

| OPERA Cloud 모듈 | 하위 기능 | 올라 PMS 대응 | 구현 상태 |
|-----------------|---------|-------------|----------|
| **Client Relations** | Manage Profile | (없음 — Guest Profile 필요) | ⬜ |
| **Bookings** | Manage Reservation | M07 예약관리 | ✅ |
| | Look To Book | 예약 폼 + 가용성 조회 | ✅ |
| | Room Diary | 예약 캘린더뷰 | ✅ |
| | Group/Block | (없음) | ⬜ |
| | Confirmation Letters | (없음) | ⬜ |
| | Traces | 예약 메모 (기본만) | 🟡 |
| | House Posting | (없음) | ⬜ |
| **Front Desk** | Arrivals | (삭제 상태) | ⬜ |
| | Departures | (삭제 상태) | ⬜ |
| | In House | (삭제 상태) | ⬜ |
| | Walk In | 예약 폼 (기본) | 🟡 |
| | Room Assignment | 객실 배정 | ✅ |
| | Quick Check Out | (없음) | ⬜ |
| | Registration Cards | (없음) | ⬜ |
| | Guest Messages | (없음) | ⬜ |
| | Available Room Search | 가용 객실 조회 | ✅ |
| **Inventory** | Property Availability | 대시보드 KPI (부분) | 🟡 |
| | Housekeeping Board | (없음 — Phase D) | ⬜ |
| | OOO/OOS | htl_room_status (기본) | 🟡 |
| | Task Sheets | (없음 — Phase D) | ⬜ |
| | Manage Restrictions | (없음 — Phase 3) | ⬜ |
| | Floor Plan / Site Plan | (없음 — 스킵) | ❌ |
| **Financials** | Cashiering | Payment/Deposit (기본) | 🟡 |
| | Accounts Receivable | (없음 — Phase 2) | ⬜ |
| | End of Day | (없음 — Phase 2) | ⬜ |
| | Receipt History | (없음 — Phase 2) | ⬜ |
| **Miscellaneous** | Changes Log | BaseEntity 감사 (기본) | 🟡 |
| | Property Brochure | M01 프로퍼티 관리 (기본) | 🟡 |
| | Telephone | (스킵) | ❌ |
| **Reports** | Manage Reports | 대시보드 KPI (기본) | 🟡 |
| | Shift Reports | (없음 — Phase 2+) | ⬜ |
| | Manager's Report | (없음 — Phase 2) | ⬜ |

**범례**: ✅ 구현완료 | 🟡 부분구현 | ⬜ 미구현 | ❌ 의도적 스킵

---

*이 보고서는 개발 방향성 점검 목적이며, 구현 착수 전 충분한 협의를 거친 후 진행합니다.*
