# Architecture

**Analysis Date:** 2026-03-26

## Pattern Overview

**Overall:** Modular Monolith (Schema-per-Tenant 멀티테넌시)

**Key Characteristics:**
- 6개 Gradle 서브모듈로 분리된 모듈형 모놀리스 (단일 Spring Boot 프로세스로 배포)
- Schema-per-Tenant 방식의 PostgreSQL 멀티테넌시 (`X-Tenant-ID` 헤더 → ThreadLocal → Hibernate `setSchema()`)
- 모듈 간 JPA 의존성 차단 원칙: 크로스 모듈 FK는 `@Column(name="xxx_id") Long xxxId` 사용
- 4단 Security Filter Chain: Booking API Key(@Order 0) → HK Mobile Session(@Order 1) → JWT API(@Order 2) → Session Web(@Order 3)
- Server-Side Rendering: Thymeleaf + jQuery + Bootstrap 5.3 (SPA 아님)

## Module Dependencies

**Gradle 빌드 의존성 그래프:**

```
hola-app ──→ hola-reservation ──→ hola-hotel ──→ hola-common
   │              │                                    ↑
   │              ├──→ hola-room ─────────────────────┘
   │              └──→ hola-rate ─────────────────────┘
   ├──→ hola-hotel
   ├──→ hola-room
   ├──→ hola-rate
   └──→ hola-common
```

**실제 코드 참조 방향 (import 기반):**
- `hola-reservation` → `hola-hotel`: Repository/Entity 직접 임포트 (PropertyRepository, RoomNumberRepository, FloorRepository, MarketCodeRepository, ReservationChannelRepository, RoomUnavailableRepository)
- `hola-reservation` → `hola-room`: Repository/Entity 직접 임포트 (RoomTypeRepository, RoomClassRepository, PaidServiceOptionRepository, FreeServiceOptionRepository, RoomTypeFloorRepository, RoomTypeFreeServiceRepository)
- `hola-reservation` → `hola-rate`: Repository/Entity/Service 직접 임포트 (RateCodeRepository, RateCodePaidServiceRepository, PromotionCodeRepository, RatePricingRepository, DayUseRateRepository, RateCodeService)
- `hola-common` → 없음 (최하위 모듈, 다른 모듈 참조 없음)
- `hola-hotel`, `hola-room`, `hola-rate` → `hola-common`만 참조

**크로스 모듈 접근 패턴:**
모듈 간 통신은 서비스 레이어 추상화를 거치지 않고, 다른 모듈의 Repository를 직접 주입받아 사용. 예:

```java
// hola-reservation/service/ReservationServiceImpl.java (line 7~23)
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
```

**예외 사항 (원칙 위반):**
- `MasterReservation` 엔티티가 `Property`를 `@ManyToOne(fetch = FetchType.LAZY)` 로 직접 참조 (`hola-reservation/src/main/java/com/hola/reservation/entity/MasterReservation.java` line 27)
- `BookingServiceImpl`이 `hola-rate`의 `RateCodeService` 인터페이스를 직접 호출 (`hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java` line 27)

## Layers

**Controller Layer:**
- Purpose: HTTP 요청 수신, 권한 검증 위임, 서비스 호출, 응답 반환
- Location: `hola-{module}/src/main/java/com/hola/{domain}/controller/`
- Contains: API 컨트롤러(`XxxApiController` @RestController), 뷰 컨트롤러(`XxxViewController` @Controller)
- Pattern: API와 View를 별도 클래스로 분리. API는 `HolaResponse` 래퍼 반환, View는 Thymeleaf 템플릿 경로 반환
- 모든 프로퍼티 소속 API에서 `accessControlService.validatePropertyAccess(propertyId)` 호출
- Depends on: Service 인터페이스, AccessControlService
- Used by: 클라이언트 (Browser, 외부 API)

**Service Layer:**
- Purpose: 비즈니스 로직, 트랜잭션 관리
- Location: `hola-{module}/src/main/java/com/hola/{domain}/service/`
- Contains: 인터페이스(`XxxService`), 구현체(`XxxServiceImpl`)
- Pattern: 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional` 오버라이드
- Depends on: Repository 인터페이스, Mapper, 다른 모듈의 Repository(크로스 모듈)
- Used by: Controller

**Repository Layer:**
- Purpose: 데이터 접근
- Location: `hola-{module}/src/main/java/com/hola/{domain}/repository/`
- Contains: `JpaRepository<Xxx, Long>` 인터페이스
- Pattern: Spring Data JPA 메서드 네이밍 쿼리 + 커스텀 `@Query` JPQL. 비관적 락 쿼리(`@Lock(PESSIMISTIC_WRITE)`) 사용
- Depends on: Entity
- Used by: Service (자기 모듈 + 크로스 모듈)

**Entity Layer:**
- Purpose: 도메인 모델, 비즈니스 규칙 캡슐화
- Location: `hola-{module}/src/main/java/com/hola/{domain}/entity/`
- Contains: JPA 엔티티, BaseEntity 상속
- Pattern: `@SQLRestriction("deleted_at IS NULL")` 자동 적용, Soft Delete (`softDelete()` 메서드)
- 동시성 제어: `MasterReservation`, `SubReservation` 등에 `@Version Long version` (낙관적 락)
- Depends on: BaseEntity (hola-common)
- Used by: Repository, Service, Mapper

**DTO Layer:**
- Purpose: 계층 간 데이터 전달
- Location: `hola-{module}/src/main/java/com/hola/{domain}/dto/request/`, `dto/response/`
- Contains: CreateRequest, UpdateRequest, Response (@Builder)
- Pattern: request/response 하위 패키지로 분리. Validation 어노테이션 사용
- Depends on: 없음 (순수 POJO)
- Used by: Controller, Service, Mapper

**Mapper Layer:**
- Purpose: Entity ↔ DTO 변환
- Location: `hola-{module}/src/main/java/com/hola/{domain}/mapper/`
- Contains: `@Component` 클래스, 수동 `toEntity()`/`toResponse()` 메서드
- Pattern: MapStruct 의존성은 있으나 실제 사용은 수동 변환
- Depends on: Entity, DTO
- Used by: Service

## Data Flow

**Admin 예약 생성 (일반):**

1. Browser → `POST /api/v1/properties/{propertyId}/reservations` (JSESSIONID 세션 쿠키)
2. `SecurityFilterChain @Order(2)`: JWT 필터 → 세션 기반 인증 (SessionCreationPolicy.NEVER)
3. `ReservationApiController`: `accessControlService.validatePropertyAccess(propertyId)` → `reservationService.create()`
4. `ReservationServiceImpl`: MasterReservation + SubReservation + DailyCharge 생성, RoomAvailabilityService 충돌 검사
5. `RoomAvailabilityService`: SubReservationRepository + RoomTypeFloorRepository(hola-room) 크로스 모듈 조회
6. DB 반영 후 `HolaResponse.success(data)` 반환

**부킹엔진 예약 (게스트, KICC PG):**

1. Browser → `POST /api/v1/booking/properties/{propertyCode}/reservations` (API-KEY 헤더)
2. `BookingSecurityConfig @Order(0)`: BookingApiKeyFilter → API 키 검증 (Stateless)
3. `BookingApiController` → `BookingServiceImpl`
4. Property, RateCode, RoomType 등 크로스 모듈 조회
5. PaymentGateway.registerTransaction() → KICC 거래등록 → 결제창 URL 반환
6. 게스트 결제창에서 인증 → KiccPaymentApiController로 리다이렉트
7. PaymentGateway.approveAfterAuth() → 최종 승인
8. MasterReservation + SubReservation + DailyCharge + ReservationPayment + PaymentTransaction 생성

**하우스키핑 모바일:**

1. Mobile Browser → `POST /api/v1/properties/{propertyId}/hk-mobile/tasks/{taskId}/status` (JSESSIONID)
2. `SecurityFilterChain @Order(1)`: HkMobileSessionFilter → 세션 attribute (`hkUserId`, `hkUserRole`)에서 SecurityContext 복원
3. `HkMobileApiController` (hola-hotel 모듈) → `HousekeepingService`
4. HkTask 상태 변경 + HkTaskLog 기록

**State Management:**
- Server: 세션 기반 (Spring Security SecurityContext) + JWT (API용)
- Client: `HolaPms.context` (sessionStorage) → hotelId/propertyId 관리, `hola:contextChange` 커스텀 이벤트로 페이지 간 동기화
- 멀티테넌시: ThreadLocal `TenantContext` (요청 단위)

## Key Abstractions

**BaseEntity:**
- Purpose: 전체 엔티티 공통 필드 (id, audit, soft delete)
- Location: `hola-common/src/main/java/com/hola/common/entity/BaseEntity.java`
- Pattern: `@MappedSuperclass` + JPA Auditing. `softDelete()`, `activate()`, `deactivate()`, `changeSortOrder()`

**HolaResponse<T>:**
- Purpose: 통일된 API 응답 포맷
- Location: `hola-common/src/main/java/com/hola/common/dto/HolaResponse.java`
- Pattern: Generic wrapper. `success(data)`, `success(data, pageInfo)`, `error(code, message)`. 성공코드: `HOLA-0000`

**ErrorCode:**
- Purpose: 전체 에러 코드 체계 (enum)
- Location: `hola-common/src/main/java/com/hola/common/exception/ErrorCode.java`
- Pattern: Enum with (code, message, HttpStatus). 모듈별 코드 대역: 0xxx 공통, 06xx 회원, 07xx 권한, 08xx 비밀번호, 1xxx 호텔, 2xxx 객실, 25xx TC, 26xx 재고, 3xxx 레이트, 4xxx 예약/부킹/결제, 5xxx 프론트데스크, 8xxx HK

**AccessControlService:**
- Purpose: 프로퍼티/호텔 접근 권한 검증 중앙화
- Location: `hola-common/src/main/java/com/hola/common/security/AccessControlService.java`
- Pattern: SUPER_ADMIN → 전체 허용, HOTEL_ADMIN/PROPERTY_ADMIN → AdminUserProperty 매핑 검증

**PaymentGateway:**
- Purpose: 결제 PG 추상화
- Location: `hola-reservation/src/main/java/com/hola/reservation/booking/gateway/PaymentGateway.java`
- Implementations:
  - `MockPaymentGateway` (`hola-reservation/src/main/java/com/hola/reservation/booking/gateway/MockPaymentGateway.java`) — 테스트용 동기식
  - `KiccPaymentGateway` (`hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccPaymentGateway.java`) — KICC 이지페이 3단계 비동기
- Pattern: Strategy pattern. `registerTransaction()` → `approveAfterAuth()` → `cancelPayment()`

**InventoryManagementStrategy:**
- Purpose: 재고 관리 전략 추상화
- Location: `hola-room/src/main/java/com/hola/room/service/inventory/InventoryManagementStrategy.java`
- Implementations:
  - `InternalInventoryStrategy` — 자체 DB 관리 (InventoryAvailability 테이블)
  - `ExternalInventoryStrategy` — 외부 ERP 연동 준비
- Pattern: Strategy pattern. `reserve()`, `release()`, `getAvailableCount()`

**RoomAvailabilityService:**
- Purpose: 객실 가용성 3계층 검증
- Location: `hola-reservation/src/main/java/com/hola/reservation/service/RoomAvailabilityService.java`
- L1: 호수(roomNumberId) 충돌 검사 — 동일 객실 기간 겹침 방지
- L2: 타입(roomTypeId)별 잔여 객실 수 — 총 등록 객실 vs 활성 예약
- L3: 오버부킹 경고 — L2 초과 시 경고 (관리자 판단 위임)
- Dayuse 시간 슬롯 충돌 필터링 지원
- 비관적 락(`findConflictsWithLock`) + 낙관적 락(`@Version`) 이중 동시성 보호

**PriceCalculationService:**
- Purpose: 요금 자동 계산 엔진
- Location: `hola-reservation/src/main/java/com/hola/reservation/service/PriceCalculationService.java`
- Pattern: RateCode → RatePricing → 요일별 요금표 → 인원 추가 요금 → 봉사료 → VAT(봉사료 포함) → DailyCharge 리스트 생성

## Entry Points

**Application Bootstrap:**
- Location: `hola-app/src/main/java/com/hola/HolaPmsApplication.java`
- Triggers: `./gradlew :hola-app:bootRun`
- Responsibilities: Spring Boot 자동 설정, `@SpringBootApplication` (base package: `com.hola`) → 전 모듈 컴포넌트 스캔

**Admin Web (세션 기반):**
- Location: `hola-app/src/main/java/com/hola/web/LoginController.java`
- Triggers: Browser GET `/login` → Form POST `/login`
- Responsibilities: `RoleBasedAuthSuccessHandler` 역할별 리다이렉트 (HOUSEKEEPER→모바일, 나머지→대시보드)

**REST API (JWT):**
- Location: 각 모듈 `*ApiController.java` (60+ 컨트롤러)
- Triggers: `POST /api/v1/auth/login` → JWT 토큰 발급 → `Authorization: Bearer {token}` 헤더
- Responsibilities: AJAX 호출 처리, `HolaResponse` 래퍼 반환

**Booking API (API Key):**
- Location: `hola-reservation/src/main/java/com/hola/reservation/booking/controller/BookingApiController.java`
- Triggers: 외부 요청 `API-KEY` 헤더
- Responsibilities: 게스트 예약 생성/조회/수정/취소, 결제 처리

**KICC PG Callback:**
- Location: `hola-reservation/src/main/java/com/hola/reservation/booking/controller/KiccPaymentApiController.java`
- Triggers: KICC 결제창 인증 완료 → 리다이렉트 (`/booking/payment-return`)
- Responsibilities: PG 인증 후 최종 결제 승인 처리

**HK Mobile:**
- Location: `hola-hotel/src/main/java/com/hola/hotel/controller/HkMobileApiController.java`, `HkMobileViewController.java`
- Triggers: Mobile browser `/m/housekeeping/login`
- Responsibilities: 하우스키퍼 로그인, 업무 조회/상태변경, 출퇴근

**Dashboard:**
- Location: `hola-app/src/main/java/com/hola/web/DashboardController.java`
- Triggers: `/` 또는 `/admin/dashboard`
- Responsibilities: 전체 KPI, 프로퍼티별 운영현황, 픽업 현황 (DashboardService → hola-reservation)

## Error Handling

**Strategy:** 3계층 예외 처리 (비즈니스 → 검증 → 시스템)

**Patterns:**
- 비즈니스 예외: `throw new HolaException(ErrorCode.XXX)` → `GlobalExceptionHandler` 에서 `HolaResponse.error()` 반환
- 입력값 검증: `@Valid` + `MethodArgumentNotValidException` → 필드 에러 메시지 조합
- 동시성 충돌: `ObjectOptimisticLockingFailureException` → HTTP 409 + "HOLA-4027"
- 데이터 무결성: `DataIntegrityViolationException` → HTTP 409 + "HOLA-0004"
- 시스템 오류: 최종 `Exception` catch → HTTP 500 + "HOLA-0001"
- 정적 리소스 미발견: `NoResourceFoundException` → HTTP 404 (로그 DEBUG)

**GlobalExceptionHandler:** `hola-common/src/main/java/com/hola/common/exception/GlobalExceptionHandler.java`
**BookingExceptionHandler:** `hola-reservation/src/main/java/com/hola/reservation/booking/exception/BookingExceptionHandler.java`

## Security Architecture

**4단 Filter Chain (Order 기준):**

| Order | Bean | Matcher | 인증 방식 | 세션 정책 |
|-------|------|---------|-----------|-----------|
| 0 | `bookingApiFilterChain` | `/api/v1/booking/**` | BookingApiKeyFilter (API-KEY 헤더) | STATELESS |
| 1 | `hkMobileApiFilterChain` | `/api/v1/properties/*/hk-mobile/**` | HkMobileSessionFilter (세션 attribute) | IF_REQUIRED, sessionFixation().none() |
| 2 | `apiFilterChain` | `/api/**` | JwtAuthenticationFilter | NEVER (세션 생성 안 함, 기존 세션은 허용) |
| 3 | `webFilterChain` | `/**` | Form Login (JSESSIONID) | 기본 (세션 생성) |

**SecurityConfig:** `hola-common/src/main/java/com/hola/common/security/SecurityConfig.java`
**BookingSecurityConfig:** `hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingSecurityConfig.java`

**HkMobileSessionFilter 핵심 주의점:**
- PMS Admin SecurityContext 오염 방지를 위해 try/finally로 원본 컨텍스트 백업/복원 필수
- `hkUserId`, `hkUserRole` 세션 attribute에서 별도 SecurityContext 생성
- `shouldNotFilter()`: `/hk-mobile/` 경로만 필터링
- Location: `hola-common/src/main/java/com/hola/common/security/HkMobileSessionFilter.java`

**역할 5종:** SUPER_ADMIN, HOTEL_ADMIN, PROPERTY_ADMIN, HOUSEKEEPING_SUPERVISOR, HOUSEKEEPER

**JWT:** Access Token 1h + Refresh Token 7d
- Location: `hola-common/src/main/java/com/hola/common/security/JwtProvider.java`

**URL 인가 규칙 경로 순서:**
- 구체적 경로 (`/api/v1/hotels/selector`)를 제너릭 (`/api/v1/hotels/**`)보다 **먼저** 배치 필수
- SecurityConfig의 `requestMatchers` 순서가 매칭 우선순위를 결정

## Multi-Tenancy

**방식:** Schema-per-Tenant (PostgreSQL 스키마 분리)

**구현 흐름:**
1. `TenantFilter` (Ordered.HIGHEST_PRECEDENCE): `X-Tenant-ID` 헤더 → `TenantContext.setTenantId()` (ThreadLocal)
2. `TenantIdentifierResolver`: Hibernate가 SQL 실행 시 `TenantContext.getTenantId()` 호출
3. `TenantConnectionProvider`: `connection.setSchema(tenantIdentifier)` → 스키마 전환
4. 요청 종료 시 `TenantFilter` finally 블록에서 `TenantContext.clear()` → ThreadLocal 정리

**관련 파일:**
- `hola-common/src/main/java/com/hola/common/tenant/TenantFilter.java`
- `hola-common/src/main/java/com/hola/common/tenant/TenantContext.java`
- `hola-common/src/main/java/com/hola/common/tenant/TenantConnectionProvider.java`
- `hola-common/src/main/java/com/hola/common/tenant/TenantIdentifierResolver.java`

## Cross-Cutting Concerns

**Logging:** SLF4J + Logback (`@Slf4j` Lombok). 프로파일별 레벨: local=DEBUG, prod=INFO.

**Validation:** Jakarta Validation (`@Valid`, `@NotBlank`, `@Size` 등) + `GlobalExceptionHandler`. 비즈니스 검증은 Service에서 `HolaException` throw.

**Authentication:** `AccessControlService` 를 통해 Controller에서 호출. `getCurrentUser()`, `validatePropertyAccess(propertyId)`, `validateHotelAccess(hotelId)`.

**Auditing:** JPA Auditing (`@CreatedDate`, `@CreatedBy`, `@LastModifiedDate`, `@LastModifiedBy`) → BaseEntity 자동 적용.
- Config: `hola-common/src/main/java/com/hola/common/config/JpaAuditingConfig.java`

**Soft Delete:** `BaseEntity.softDelete()` → `deletedAt` + `useYn=false`. `@SQLRestriction("deleted_at IS NULL")` 자동 필터링. **물리 삭제 금지 원칙.**

**Concurrency Control:**
- 낙관적 락: `@Version Long version` (MasterReservation, SubReservation 등)
- 비관적 락: `@Lock(PESSIMISTIC_WRITE)` + `@Query` JPQL (RoomAvailabilityService)
- 이중 보호: TOCTOU 레이스 컨디션 방지를 위해 비관적 락 → 낙관적 락 순차 적용

**File Upload:**
- `hola-common/src/main/java/com/hola/common/controller/FileUploadController.java`
- `hola-common/src/main/java/com/hola/common/service/FileUploadService.java`
- 로컬 파일시스템 (`./uploads/` 경로, `hola.upload.path` 설정)

## Change Impact Points

**BaseEntity 변경 시:**
- 전체 모듈의 모든 엔티티에 영향 (61+ 엔티티가 상속)
- Flyway 마이그레이션 필수

**SecurityConfig 변경 시:**
- 4개 Filter Chain 순서 중요 — @Order 값과 requestMatchers 순서 모두 확인 필요
- BookingSecurityConfig (@Order 0)이 별도 파일(`hola-reservation` 모듈)에 존재

**ErrorCode 추가 시:**
- `ErrorCode` enum에 새 코드 추가 → 프론트엔드 `hola-common.js` 에러 핸들링 매핑 확인

**RoomType/RateCode 엔티티 변경 시:**
- `hola-reservation` 모듈의 BookingServiceImpl(1,969줄), ReservationServiceImpl(1,857줄), PriceCalculationService에 연쇄 영향
- 크로스 모듈 Repository 직접 참조이므로 컴파일 타임에 즉시 발견됨

**Property 엔티티 변경 시:**
- MasterReservation `@ManyToOne` 직접 참조 → JPA 관계에 영향
- PriceCalculationService: 세금/봉사료율 (`taxRate`, `serviceChargeRate`) 사용
- 부킹엔진 전체: PropertyImage, PropertyTerms, PropertySettlement 등 연관

**SubReservation 상태 변경 로직:**
- RoomAvailabilityService 가용성 검사 (RELEASED_STATUSES 목록)
- FrontDeskServiceImpl 도착/인하우스/출발 조회
- DashboardServiceImpl KPI 계산
- HousekeepingServiceImpl 하우스키핑 태스크 자동 생성

---

*Architecture analysis: 2026-03-26*
