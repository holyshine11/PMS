# Phase 1: Stayover Cleaning Management - Context

**Gathered:** 2026-03-26
**Status:** Ready for planning

<domain>
## Phase Boundary

투숙 중 객실(Occupied Room)의 청소 관리 기능을 구현한다.
100개+ 다양한 호텔 프로퍼티를 지원하기 위해, 프로퍼티별 기본 정책 + 룸타입별 오버라이드 구조로 설계한다.

**포함 범위:**
- HkCleaningPolicy 엔티티/테이블 (룸타입별 청소 정책 오버라이드)
- HkConfig 확장 (프로퍼티 레벨 기본 정책 필드 추가)
- 정책 해석 엔진 (기본값 + 오버라이드 조합)
- OC→OD 일일 전환 메커니즘
- 스테이오버 작업 자동 생성 스케줄러
- DND/청소거절 워크플로우
- 관리자 UI (HK 설정 페이지 확장)

**제외 범위:**
- 턴다운 서비스 자동화 (별도 페이즈)
- Night Audit 전체 구현 (이 페이즈에서는 HK 관련 부분만)
- 고객 대면 앱 (부킹엔진 연동 등)

</domain>

<decisions>
## Implementation Decisions

### D-01: 설정 위치
하우스키핑 모듈 내에서 관리한다 (프로퍼티 관리 X).
이유: 설정의 주 소비자가 HK 수퍼바이저이며, 운영 중 수시 조정 필요.

### D-02: 구조 패턴 — "기본값 + 오버라이드"
- `HkConfig` = 프로퍼티 레벨 기본값 (전체 객실에 적용)
- `HkCleaningPolicy` = 룸타입별 오버라이드 (설정된 룸타입만 기본값 대신 적용)
- 오버라이드가 없는 룸타입 → 자동으로 프로퍼티 기본값 적용

### D-03: 성급 기준 미사용
호텔 성급으로 정책을 구분하지 않음. 모든 차이는 프로퍼티별 설정으로 대응.

### D-04: 관리자 UI 사용성
- 기본 설정 탭: 프로퍼티 전체 기본 정책
- 룸타입별 설정 탭: 테이블 형태, "기본값 사용" 뱃지가 기본, 오버라이드 필요한 것만 편집
- 직관적으로 "어디가 특별한지" 한눈에 파악 가능해야 함

### Claude's Discretion
- DB 마이그레이션 버전 번호 (V8 대역 내)
- 스케줄러 구현 방식 (Spring @Scheduled vs 배치)
- DND 상세 구현 패턴

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `HkConfig` 엔티티: 프로퍼티별 설정 기반, 확장 대상
- `HousekeepingServiceImpl.generateDailyTasks()`: VD→CHECKOUT, OD→STAYOVER 로직 이미 존재
- `RoomNumber.updateHkStatus()`: hkStatus 변경 메서드
- `RoomStatusService.calcStatusCode()`: 상태 코드 계산 유틸
- `HkAssignmentServiceImpl.autoAssign()`: 자동 배정 로직 (generateDailyTasks 호출 포함)
- `hk-settings-page.js`: 기존 설정 UI (확장 대상)

### Established Patterns
- 모든 엔티티: BaseEntity 상속, @SQLRestriction, softDelete
- 크로스 모듈: Long xxxId 참조 (JPA FK 미사용)
- 서비스: Interface + Impl, @Transactional(readOnly=true) 클래스 수준
- UI: HolaPms 네임스페이스, Bootstrap 5.3, jQuery 3.7

### Integration Points
- `HousekeepingServiceImpl` — 정책 기반 태스크 생성 로직 주입 지점
- `hk-settings-page.js` + `housekeeping/settings.html` — UI 확장 지점
- `RoomNumber` 엔티티 — OC→OD 일일 전환 대상
- `HkConfig` — 프로퍼티 기본값 필드 추가 대상
- `RoomNumberRepository` — OC 상태 객실 조회 쿼리 추가

### Cross-Module Dependencies
- RoomType은 hola-room 모듈에 있음. HkCleaningPolicy에서 roomTypeId를 Long으로 참조
- RoomNumber→RoomType 연결은 rm_room_type_floor 경유 (네이티브 SQL)
- 예약 모듈에서 checkout 시 housekeepingService.createTaskOnCheckout() 호출

</code_context>

<specifics>
## Specific Ideas

- 100개+ 다양한 호텔을 위한 설정 기반 엔진 (하드코딩 금지)
- 관리자가 직관적으로 파악할 수 있는 UI 사용성 우선
- "기본값 사용" 뱃지로 오버라이드 여부 즉시 확인
</specifics>

<deferred>
## Deferred Ideas

- 턴다운 서비스 자동 스케줄링
- Night Audit 전체 구현
- 고객 앱에서 청소 요청/거절
- 청소 품질 평가 시스템
- 크레딧 자동 정산
</deferred>

---

*Phase: 01-stayover-cleaning*
*Context gathered: 2026-03-26*
