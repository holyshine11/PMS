# Codebase Concerns

**Analysis Date:** 2026-03-26

## Tech Debt

### God Class: BookingServiceImpl (1,969줄)

- Issue: 단일 서비스에 부킹엔진 전체 비즈니스 로직 집중. 프로퍼티 조회, 요금 계산, 가용성 검색, 예약 생성, 결제 처리, 취소, 수정, 확인 조회까지 모두 포함
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java`
- Impact: 변경 시 의도치 않은 사이드이펙트 발생 위험 높음. 테스트 작성 어려움. 1,969줄은 가독성 한계 초과
- Fix approach: 기능별 분리 — `BookingSearchService`, `BookingReservationService`, `BookingPaymentService`, `BookingLookupService` 등으로 위임 패턴 적용

### God Class: ReservationServiceImpl (1,857줄)

- Issue: 예약 CRUD, 상태 전이, 캘린더뷰, 타임라인, 서비스 항목 관리, 체크인/아웃 등 예약 도메인 전체가 단일 클래스
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java`
- Impact: BookingServiceImpl과 동일한 유지보수 위험
- Fix approach: `ReservationLifecycleService`(상태 전이), `ReservationCalendarService`(캘린더/타임라인), `ReservationServiceItemService`(유료 서비스) 분리

### ExternalInventoryStrategy — 미구현 스텁

- Issue: 외부 ERP 연동 전략이 TODO 상태. `getAvailableCount()`는 `Integer.MAX_VALUE` 반환
- Files: `hola-pms/hola-room/src/main/java/com/hola/room/service/inventory/ExternalInventoryStrategy.java` (라인 21-42)
- Impact: ERP 재고 관리 모드 선택 시 재고 검증 없이 무한 가용으로 처리됨
- Fix approach: ERP API 클라이언트 구현, 서킷브레이커 패턴 적용, 타임아웃 설정

### RoomRackController — 아키텍처 위반

- Issue: Controller가 Repository를 직접 주입받아 사용. 서비스 레이어를 거치지 않고 `SubReservationRepository`, `HkTaskRepository`, `RoomTypeFloorRepository`, `AdminUserRepository` 등 6개 Repository 직접 의존
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/controller/RoomRackController.java`
- Impact: 트랜잭션 경계 부재 (`@Transactional` 없음), 비즈니스 로직이 Controller에 분산, 테스트 어려움
- Fix approach: `RoomRackService` 추출하여 서비스 레이어로 이동. `@Transactional(readOnly = true)` 적용

---

## Security Considerations

### KICC postMessage Origin 미검증

- Risk: `booking.js`의 KICC 결제 결과 postMessage 리스너가 `e.origin` 검증 없이 메시지를 처리함. 악의적 iframe이나 팝업이 가짜 `KICC_PAYMENT_COMPLETE` 메시지를 전송하면 잘못된 결제 성공 페이지로 이동 가능
- Files:
  - `hola-pms/hola-app/src/main/resources/static/js/booking.js` (라인 792-793)
  - `hola-pms/hola-app/src/main/resources/templates/booking/payment-return.html` (라인 60, 66: `postMessage({...}, '*')`)
- Current mitigation: 서버 사이드에서 Redis 기반 결제 결과를 `/result` API로 재검증하므로 실제 예약 생성 위조는 불가. 다만 UI 레벨 오동작 가능
- Recommendations:
  1. `payment-return.html`에서 `postMessage(data, '*')` → `postMessage(data, window.location.origin)` 변경
  2. `booking.js` 리스너에 `if (e.origin !== window.location.origin) return;` 추가

### HMAC 검증 — Timing Attack 취약

- Risk: `KiccHmacUtils.verify()` 메서드가 `received.equals(expected)`로 비교. 일정 시간 차이를 이용한 timing attack에 이론적으로 취약
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccHmacUtils.java` (라인 71)
- Current mitigation: 테스트 환경(mallId 'T' 시작)에서는 HMAC 검증 실패 시 로그만 남기고 진행. 실제 공격 난이도는 매우 높음
- Recommendations: `MessageDigest.isEqual(received.getBytes(), expected.getBytes())` 사용으로 constant-time 비교 적용

### CSRF 전체 비활성화

- Risk: 모든 SecurityFilterChain에서 `.csrf(AbstractHttpConfigurer::disable)`. API(JWT/Stateless) 체인은 괜찮지만, 세션 기반 웹 체인(Order 3)도 CSRF 비활성화됨
- Files: `hola-pms/hola-common/src/main/java/com/hola/common/security/SecurityConfig.java` (라인 184)
- Current mitigation: Admin 페이지는 인증된 사용자만 접근. 단, CSRF가 없으면 로그인된 관리자의 브라우저에서 악의적 사이트가 Admin API 호출 가능
- Recommendations: 웹 체인(Order 3)에 CSRF 토큰 활성화 검토. Thymeleaf `_csrf` 자동 지원 가능

### Client IP 감지 — 프록시 미대응

- Risk: `httpRequest.getRemoteAddr()` 만 사용. 리버스 프록시(Nginx, LB) 뒤에 배포 시 모든 요청의 IP가 프록시 IP로 기록됨
- Files:
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/BookingApiController.java` (라인 186, 246)
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/KiccPaymentApiController.java` (라인 66)
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingApiKeyFilter.java` (라인 60)
- Recommendations: `X-Forwarded-For` / `X-Real-IP` 헤더를 우선 확인하는 유틸리티 메서드 도입. Spring Boot `server.forward-headers-strategy=native` 설정 검토

### Rate Limiting 부재

- Risk: 부킹엔진 API(`/api/v1/booking/**`)에 Rate Limiting 없음. 악의적 대량 요청 가능
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingSecurityConfig.java`
- Recommendations: Spring Cloud Gateway 또는 Bucket4j 기반 Rate Limiting 적용. 특히 예약 생성/결제 API에 IP당 분당 제한 필요

---

## Performance Bottlenecks

### Dashboard 전체 프로퍼티 KPI — N+1 x 7 쿼리

- Problem: `getAllPropertyKpis()`가 `propertyRepository.findAll()` 후 각 프로퍼티별 `getPropertyKpi()` 호출. `getPropertyKpi()` 내부에서 최소 7건의 DB 쿼리 실행 (countSoldRooms, sumRevenue, 전일 동일 등)
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/DashboardServiceImpl.java` (라인 157-168)
- Cause: 프로퍼티 수 N × 쿼리 7건 = 7N개 쿼리. 프로퍼티 10개면 70건
- Improvement path: 벌크 집계 쿼리 1~2회로 전체 KPI 조회 후 Java에서 매핑

### Dashboard Pickup — 예외 무시 루프

- Problem: `getPickup()`에서 7일간 반복하며 각 날짜별 2건씩 쿼리. 쿼리 실패 시 빈 catch 블록으로 에러 무시
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/DashboardServiceImpl.java` (라인 128-149)
- Cause: 날짜별 개별 쿼리 대신 기간 범위 집계 쿼리를 사용해야 함
- Improvement path: 기간 범위 집계 쿼리(`GROUP BY date`)로 통합, 빈 catch 제거하고 적절한 에러 핸들링

### BookingServiceImpl 요금 계산 루프 내 catch(Exception) 무시

- Problem: `getRatePlans()`과 `getRatePlansByRoomType()`에서 각 레이트코드별 요금 계산 시 `catch (Exception e) { // 요금 계산 실패 시 무시 }` 패턴. 루프 내 `rateCodeRepository.findById()` 개별 조회도 포함
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java` (라인 1593-1603, 1901-1908, 1911)
- Cause: 레이트코드 N개 × (calculateDailyCharges + findById) = 2N 쿼리. 에러 발생 시 디버깅 불가
- Improvement path: 레이트코드 벌크 조회, 요금 계산 실패 시 최소한 warn 로그 남기기

### 캘린더뷰 Java 필터링

- Problem: `getCalendarData()`에서 전체 예약을 DB에서 조회한 후 Java stream으로 상태/키워드 필터링
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` (라인 1526-1533)
- Cause: JPQL null 파라미터 제약 때문에 Specification 대신 전체 조회 후 필터링
- Improvement path: Spring Data JPA Specification 활용하여 DB 레벨 필터링

---

## Fragile Areas

### KICC 결제 승인 실패 후 보상 트랜잭션

- Files:
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/KiccPaymentApiController.java` (라인 152-187)
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccPaymentGateway.java` (라인 129-131, 225-243)
- Why fragile: KICC 결제 승인 성공 후 예약 생성 실패 시 결제만 완료되고 예약은 없는 상태 발생 가능. catch 블록에서 결제 자동 취소를 시도하지만 취소도 실패할 수 있음 (`cancelByPgCno` 내부에서 또 catch(Exception))
- Safe modification: 결제 승인 후 예약 생성을 하나의 트랜잭션으로 묶되, 실패 시 PG 취소를 별도 보상 트랜잭션으로 분리. 취소 실패 건은 DB에 기록하여 수동 환불 처리
- Test coverage: `hola-pms/hola-app/src/test/java/com/hola/integration/payment/PaymentApiIntegrationTest.java` 존재하나, PG 실패 시나리오 테스트 불명확

### 예약 상태 전이 — 하드코딩 상태 맵

- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` (라인 93-101)
- Why fragile: 상태 전이 규칙이 `Map<String, Set<String>>`으로 하드코딩. 새 상태 추가 시 여러 곳의 문자열 비교 수정 필요
- Safe modification: `ReservationStatus` enum 도입하여 상태 전이를 타입-세이프하게 관리
- Test coverage: `hola-pms/hola-reservation/src/test/java/com/hola/reservation/service/ReservationServiceImplTest.java` 존재

### HK 모바일 세션 필터 — SecurityContext 오염 방지

- Files: `hola-pms/hola-common/src/main/java/com/hola/common/security/HkMobileSessionFilter.java`
- Why fragile: try/finally 패턴으로 원본 SecurityContext를 복원하지만, 동일 브라우저에서 Admin과 모바일 동시 사용 시 세션 충돌 가능. `sessionFixation().none()` 설정으로 세션 고정 보호 비활성화됨
- Safe modification: CLAUDE.md에 이미 주의사항으로 기록됨. 변경 시 반드시 Admin + 모바일 동시 테스트 수행
- Test coverage: `hola-pms/hola-app/src/test/java/com/hola/integration/security/SecurityIntegrationTest.java` 존재하나 동시 접속 시나리오 미확인

---

## Module Coupling Issues

### reservation 모듈의 과도한 크로스 모듈 의존

- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` (라인 7-22)
- Issue: reservation 모듈이 hotel의 `Property`, `RoomNumber`, `Floor`, `RoomUnavailable` 엔티티와 `PropertyRepository`, `FloorRepository`, `RoomNumberRepository` 등 hotel/room 모듈 Repository를 직접 주입
- Impact: 모듈 간 JPA 의존성이 엔티티 레벨에서 발생. "크로스 모듈 FK는 Long ID만 사용" 원칙 부분 위반. `MasterReservation.property`는 `@ManyToOne`으로 hotel 엔티티 직접 참조
- Fix approach: 크로스 모듈 조회는 각 모듈의 Service 인터페이스를 통해 수행. 이미 일부 필드(`roomTypeId`, `floorId`)는 Long으로 분리되어 있으므로 나머지도 점진적 분리

### BookingServiceImpl — 15개 이상 의존성 주입

- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java` (라인 1-80)
- Issue: hotel, room, rate, reservation 4개 모듈의 Repository와 Service를 직접 주입받아 사용 (PropertyRepository, RoomTypeRepository, RateCodeRepository, RateCodeRoomTypeRepository 등)
- Impact: 단일 클래스가 전체 도메인 의존성 허브 역할. 변경 파급 효과 매우 큼

---

## Test Coverage Gaps

### 서비스 구현체 35개 중 단위테스트 8개

- What's not tested: 35개 ServiceImpl 중 단위테스트가 있는 것은 8개뿐
  - 테스트 있음: `BookingServiceImpl`, `CancellationPolicyServiceImpl`, `ReservationServiceImpl`, `ReservationPaymentServiceImpl`, `PriceCalculationService`, `RoomAvailabilityService`, `EarlyLateCheckService`, `PropertyServiceImpl`, `HotelServiceImpl`, `RoomTypeServiceImpl`
  - 테스트 없음: `HousekeepingServiceImpl`(892줄), `HkAssignmentServiceImpl`(722줄), `RateCodeServiceImpl`(533줄), `RoomAssignServiceImpl`(449줄), `DashboardServiceImpl`, `FrontDeskServiceImpl`, `RoomUpgradeServiceImpl`, `TransactionCodeServiceImpl`, `PropertyRoleServiceImpl` 등 27개
- Files: `hola-pms/*/src/test/java/` — 전체 22개 테스트 파일
- Risk: 하우스키핑, 프론트데스크, 대시보드 등 운영 핵심 기능의 리그레션 감지 불가
- Priority: High — 특히 `HousekeepingServiceImpl`, `RateCodeServiceImpl`, `FrontDeskServiceImpl`

### KICC PG 통합 — 실제 PG 시나리오 테스트 부재

- What's not tested: `KiccPaymentGateway`는 `@Profile("!test")`로 테스트 환경 제외. Mock PG(`MockPaymentGateway`)로 대체됨. 금액 불일치, HMAC 실패, 네트워크 타임아웃 등 PG 실패 시나리오 미테스트
- Files:
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccPaymentGateway.java`
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccApiClient.java`
- Risk: PG 결제 실패 시 보상 트랜잭션(취소) 로직 검증 안 됨
- Priority: High

### E2E 테스트 한정적

- What's not tested: `e2e-tests/` 디렉토리에 `phase4-reservation.js` 1개만 존재. 하우스키핑, 대시보드, 프론트데스크 E2E 없음
- Files: `hola-pms/e2e-tests/phase4-reservation.js`
- Risk: UI 플로우 리그레션 감지 불가
- Priority: Medium

---

## Missing Critical Features

### 스케줄러 부재

- Problem: `@Scheduled` 또는 `@Async` 어노테이션 사용 없음. 자동 No-Show 처리, 예약 만료, Redis 임시 데이터 정리, 일일 하우스키핑 작업 자동 생성 등 배치 작업이 없음
- Blocks: 운영 자동화. 현재 모든 상태 전이는 수동(API 호출)으로만 가능
- Fix approach: `@Scheduled` 기반 배치 작업 추가 — 매일 자정 No-Show 자동 처리, KICC 미완료 결제 정리, 하우스키핑 일일 작업 생성

### 감사 로그(Audit Log) 한정적

- Problem: `BookingAuditLog`는 부킹엔진 전용. Admin 측 예약 수정/취소, 객실 배정, 프론트데스크 체크인/아웃 등의 감사 로그 없음
- Blocks: 운영 추적성. 누가 언제 어떤 변경을 했는지 추적 불가
- Fix approach: `BaseEntity`의 `createdBy`/`updatedBy`는 있으나, 변경 내역(diff) 기록 없음. Spring Data Envers 또는 커스텀 AuditLog 도입 검토

---

## Frontend Concerns

### JavaScript 파일 크기 과대

- Problem: `reservation-detail.js`(2,477줄), `rate-code-form.js`(1,496줄), `reservation-form.js`(1,347줄), `booking.js`(1,287줄) 등 단일 파일 1,000줄 이상
- Files: `hola-pms/hola-app/src/main/resources/static/js/`
- Impact: 유지보수 어려움. 함수 간 의존 관계 파악 불가
- Fix approach: 페이지별 JS를 기능 단위로 분리하거나, 모듈 번들링(Vite 등) 도입 검토

### jQuery `.html()` XSS 위험 패턴

- Problem: 다수의 JS 파일에서 API 응답 데이터를 `$.html()`로 DOM에 주입. `booking.js`는 `escapeHtml()` 함수를 제공하고 일부 사용하지만, Admin 측 JS 파일들은 일관성 없음
- Files: `hola-pms/hola-app/src/main/resources/static/js/reservation-detail.js` 등 Admin JS 전반
- Impact: 게스트 이름, 메모 등 사용자 입력이 포함된 API 응답을 escaping 없이 `.html()`로 삽입 시 XSS 가능
- Fix approach: `HolaPms.escapeHtml()` 유틸리티를 `hola-common.js`에 추가하고, `.html()` 사용 시 반드시 escaping 적용 가이드라인 수립

---

## Recent Changes Requiring Attention

### KICC PG 결제 통합 (커밋: 5c03669)

- Status: 최근 커밋. 수정된 파일:
  - `hola-pms/hola-app/src/main/resources/static/js/booking.js`
  - `hola-pms/hola-app/src/main/resources/templates/booking/payment-return.html`
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/KiccPaymentApiController.java`
- Concerns:
  1. postMessage origin 미검증 (위 보안 항목 참조)
  2. 결제 승인 성공 → 예약 생성 실패 시 보상 처리 불완전
  3. `KiccPaymentApiController`가 `@Controller`인데 `@ResponseBody` 메서드와 뷰 반환 메서드 혼재 (일반적으로 `@RestController` + 별도 ViewController 분리 패턴)
  4. `payment-return.html`에서 Thymeleaf 인라인 JavaScript에 errorMessage를 직접 출력 — XSS 위험 (`th:inline="javascript"`는 자동 escaping하나, 라인 80에서 직접 문자열 연결)

### Roomrack 하우스키핑 매핑 (커밋: 4bd672e)

- Status: 최근 추가된 기능
- Files: `hola-pms/hola-reservation/src/main/java/com/hola/reservation/controller/RoomRackController.java`
- Concerns:
  1. 아키텍처 위반 — Controller에서 직접 Repository 접근 (위 Tech Debt 항목 참조)
  2. `getUserName(hkTask.getAssignedTo())` — 루프 내에서 개별 `adminUserRepository.findById()` 호출 (N+1 패턴)
  3. 개별 객실 상세 조회(`getRoomRackItem`)에서도 전체 프로퍼티의 `roomStatusService.getRoomRackItems(propertyId)` 호출 후 필터링 — 비효율

### Dayuse(대실) 추가 (커밋: 03dd787)

- Status: 비교적 최근 추가
- Concerns: dayuse 관련 코드가 기존 숙박 로직과 분기 처리(`if (sub.isDayUse())`)로 섞여 있음. `ReservationServiceImpl` 내 체크아웃 로직에서 dayuse 레이트 체크아웃 요금 면제 등 조건부 분기 증가

---

## Dependencies at Risk

### 멀티테넌시 — Schema-per-Tenant 미완성

- Risk: `TenantFilter`와 `TenantConnectionProvider`가 구현되어 있으나, 실제 멀티 스키마 운영은 확인 안 됨. 모든 Repository 쿼리가 propertyId 기반 필터링에 의존
- Impact: 테넌트 격리가 propertyId 체크에 의존하므로, propertyId 누락 시 다른 테넌트 데이터 접근 가능
- Migration plan: Schema-per-Tenant가 실제 운영에 필요할 때 Flyway 멀티 스키마 마이그레이션, TenantResolver 확장 필요

### Redis 의존 — 결제 플로우 핵심 경로

- Risk: KICC 결제 플로우에서 Redis가 임시 데이터 저장(`kicc:booking:`) 및 결제 결과 저장(`kicc:result:`)에 사용됨. Redis 장애 시 결제 플로우 전체 실패
- Impact: Redis 다운 시 결제 등록은 실패하고, 이미 진행 중인 결제의 콜백 처리도 실패
- Migration plan: Redis 고가용성 구성(Sentinel/Cluster) 또는 결제 임시 데이터를 DB 테이블로 대체 검토

---

*Concerns audit: 2026-03-26*
