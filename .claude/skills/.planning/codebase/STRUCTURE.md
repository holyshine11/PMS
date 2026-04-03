# Codebase Structure

**Analysis Date:** 2026-03-26

## Directory Layout

```
hola-pms/
├── hola-common/              # M00: 공통 모듈 (55 Java files)
├── hola-hotel/               # M01+M04: 호텔/프로퍼티/회원/권한/하우스키핑 (169 Java files)
├── hola-room/                # M02: 객실클래스/타입/서비스옵션/트랜잭션코드/재고 (81 Java files)
├── hola-rate/                # M03: 레이트코드/프로모션코드 (36 Java files)
├── hola-reservation/         # M07+M08+M10: 예약/부킹엔진/프론트데스크 (169 Java files)
├── hola-app/                 # 실행 모듈: 4 Main Java + 7 Test Java + 70 HTML + 60 JS + 96 SQL
├── e2e-tests/                # E2E 테스트 (Playwright/Puppeteer)
├── docs/                     # 프로젝트 문서
├── gradle/wrapper/           # Gradle Wrapper
├── build.gradle              # Root Gradle 빌드
├── settings.gradle           # 모듈 포함 설정
└── gradlew / gradlew.bat     # Gradle Wrapper 스크립트
```

## Module Breakdown

### hola-common (55 Java files)
**Purpose:** 전체 모듈 공통 기반 — BaseEntity, 보안, 인증, 멀티테넌시, 예외처리, API 응답

| Layer | Count | Key Files |
|-------|-------|-----------|
| Controller | 6 | `AuthApiController`, `BluewaveAdminApiController`, `BluewaveAdminViewController`, `MyProfileApiController`, `MyProfileViewController`, `FileUploadController` |
| Service | 6 | `AuthService`, `BluewaveAdminService`/`Impl`, `FileUploadService` |
| Entity | 6 | `AdminUser`, `AdminUserProperty`, `Menu`, `Role`, `RoleMenu` |
| Repository | 5 | `AdminUserRepository`, `AdminUserPropertyRepository`, `MenuRepository`, `RoleRepository`, `RoleMenuRepository` |
| DTO | 11 | `HolaResponse`, `PageInfo`, Login/BluwaveAdmin/MyProfile/Password DTOs |
| Security | 7 | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtProvider`, `HkMobileSessionFilter`, `AccessControlService`, `CustomUserDetailsService`, `RoleBasedAuthSuccessHandler` |
| Tenant | 4 | `TenantFilter`, `TenantContext`, `TenantConnectionProvider`, `TenantIdentifierResolver` |
| Config | 3 | `JpaAuditingConfig`, `OpenApiConfig`, `WebConfig` |
| Exception | 3 | `ErrorCode`, `GlobalExceptionHandler`, `HolaException` |
| Util | 1 | `NameMaskingUtil` |
| Enum | 1 | `StayType` |
| Test | 4 | `HolaResponseTest`, `ErrorCodeTest`, `AccessControlServiceTest`, `NameMaskingUtilTest` |

**Package Structure:**
```
com.hola.common/
├── auth/
│   ├── controller/       # AuthApi, BluewaveAdmin, MyProfile
│   ├── dto/              # Login, BluewaveAdmin, MyProfile, Password DTOs
│   ├── entity/           # AdminUser, AdminUserProperty, Menu, Role, RoleMenu
│   ├── repository/       # 5 repositories
│   └── service/          # AuthService, BluewaveAdminService/Impl
├── config/               # JpaAuditing, OpenApi, Web
├── controller/           # FileUploadController
├── dto/                  # HolaResponse, PageInfo
├── entity/               # BaseEntity
├── enums/                # StayType
├── exception/            # ErrorCode, GlobalExceptionHandler, HolaException
├── security/             # SecurityConfig, JWT*, HkMobile*, AccessControl*
├── service/              # FileUploadService
├── tenant/               # TenantFilter, TenantContext, TenantConnectionProvider, TenantIdentifierResolver
└── util/                 # NameMaskingUtil
```

### hola-hotel (169 Java files)
**Purpose:** 호텔/프로퍼티 관리, 회원/권한 관리, 하우스키핑

| Layer | Count | Key Files |
|-------|-------|-----------|
| Controller | 27 | Hotel(Api/View), Property(Api/View), HotelAdmin(Api/View), PropertyAdmin(Api/View), HotelRole(Api/View), PropertyRole(Api/View), MarketCode, Floor, RoomNumber, RoomStatus, RoomUnavailable, Housekeeping(Api/View), HkMobile(Api/View), Housekeeper, ReservationChannel(Api/View), EarlyLateFeePolicy(Api/View), SubModule |
| Service | 37 | Hotel, Property, PropertyAdmin, PropertySettlement, HotelAdmin, HotelRole, PropertyRole, MarketCode, Floor, RoomNumber, RoomStatus, RoomUnavailable, Housekeeping, HkAssignment, Housekeeper, CancellationFee, EarlyLateFeePolicy, ReservationChannel (각 Interface + Impl) |
| Entity | 22 | Hotel, Property, PropertyImage, PropertySettlement, PropertyTerms, Floor, RoomNumber, RoomUnavailable, MarketCode, CancellationFee, EarlyLateFeePolicy, ReservationChannel, HkConfig, HkTask, HkTaskSheet, HkTaskIssue, HkTaskLog, HkSection, HkSectionFloor, HkSectionHousekeeper, HkDailyAttendance, HkDayOff |
| Repository | 20 | 각 Entity에 대응하는 Repository |
| DTO Request | 30 | Hotel/Property/HotelAdmin/PropertyAdmin/Role/MarketCode/Floor/RoomNumber/RoomUnavailable/ReservationChannel/EarlyLateFeePolicy/CancellationFee/Hk* 관련 |
| DTO Response | 30 | 각 도메인별 Response + 리스트 Response |
| Mapper | 2 | `HotelMapper`, `HkTaskMapper` |
| Test | 2 | `HotelServiceImplTest`, `PropertyServiceImplTest` |

**핵심 서비스 파일 크기:**
- `HousekeepingServiceImpl.java`: 892줄 (하우스키핑 태스크/시트/이슈/대시보드/룸랙 매핑)

### hola-room (81 Java files)
**Purpose:** 객실 클래스/타입, 유무료 서비스 옵션, 트랜잭션 코드, 재고 관리

| Layer | Count | Key Files |
|-------|-------|-----------|
| Controller | 12 | RoomClass(Api/View), RoomType(Api/View), FreeServiceOption(Api/View), PaidServiceOption(Api/View), TransactionCode(Api/View), Inventory(Api/View) |
| Service | 14+3 | RoomClass, RoomType, FreeServiceOption, PaidServiceOption, TransactionCode, Inventory (각 Interface+Impl) + InventoryManagementStrategy, InternalInventoryStrategy, ExternalInventoryStrategy |
| Entity | 11 | RoomClass, RoomType, RoomTypeFloor, RoomTypeFreeService, RoomTypePaidService, FreeServiceOption, PaidServiceOption, TransactionCode, TransactionCodeGroup, InventoryItem, InventoryAvailability |
| Repository | 11 | 각 Entity에 대응 |
| DTO | 26 | Request/Response per domain |
| Mapper | 5 | RoomClassMapper, RoomTypeMapper, FreeServiceOptionMapper, PaidServiceOptionMapper, TransactionCodeMapper |
| Test | 1 | `RoomTypeServiceImplTest` |

### hola-rate (36 Java files)
**Purpose:** 레이트코드(요금 체계), 프로모션 코드

| Layer | Count | Key Files |
|-------|-------|-----------|
| Controller | 4 | RateCode(Api/View), PromotionCode(Api/View) |
| Service | 6 | RateCode(Interface+Impl), PromotionCode(Interface+Impl) |
| Entity | 7 | RateCode, RateCodeRoomType, RateCodePaidService, RatePricing, RatePricingPerson, PromotionCode, DayUseRate |
| Repository | 7 | 각 Entity에 대응 |
| DTO | 12 | RateCode/PromotionCode/DayUseRate/RatePricing Request/Response |
| Mapper | 2 | RateCodeMapper, PromotionCodeMapper |
| Test | 0 | (없음) |

### hola-reservation (169 Java files)
**Purpose:** 예약 관리, 부킹엔진, 프론트데스크, 결제(PG), 대시보드

| Layer | Count | Key Files |
|-------|-------|-----------|
| Controller | 11 | ReservationApi, ReservationView, FrontDeskApi, FrontDeskView, RoomAssignApi, RoomRack, RoomUpgradeApi, ReservationPaymentApi, BookingApi, BookingView, KiccPaymentApi |
| Service | 15+ | Reservation, FrontDesk, RoomAssign, RoomAvailability, RoomUpgrade, ReservationPayment, PriceCalculation, Dashboard, ReservationNumberGenerator, EarlyLateCheck, RateIncludedServiceHelper, Booking, BookingApiKey, CancellationPolicy, CardBinValidation, CurrencyConversion |
| Entity | 15 | MasterReservation, SubReservation, DailyCharge, ReservationGuest, ReservationMemo, ReservationDeposit, ReservationPayment, ReservationServiceItem, ReservationNoSeq, PaymentTransaction, PaymentAdjustment, RoomUpgradeHistory, BookingApiKey, BookingAuditLog, ExchangeRate |
| Repository | 15 | 각 Entity에 대응 |
| DTO | 76 | 가장 많은 DTO — Reservation/FrontDesk/Booking/Payment/Dashboard 관련 |
| Mapper | 1 | ReservationMapper |
| PG 관련 | 19 | gateway/ (PaymentGateway, MockPaymentGateway, Request/Result DTOs) + pg/kicc/ (KiccPaymentGateway, KiccApiClient, KiccConfig, KiccProperties, KiccHmacUtils, 10+ DTOs) |
| Security | 3 | BookingSecurityConfig, BookingApiKeyFilter, BookingApiKeyService |
| Test | 10 | BookingServiceImplTest, CancellationPolicyServiceImplTest, ReservationPaymentTest, ReservationMapperTest, EarlyLateCheckServiceTest, PriceCalculationServiceTest, ReservationNumberGeneratorTest, ReservationPaymentServiceImplTest, ReservationServiceImplTest, RoomAvailabilityServiceTest |

**핵심 서비스 파일 크기:**
- `BookingServiceImpl.java`: 1,969줄 (부킹엔진 전체 로직)
- `ReservationServiceImpl.java`: 1,857줄 (Admin 예약 관리 전체 로직)

**하위 패키지 구조:**
```
com.hola.reservation/
├── booking/                    # 부킹엔진 독립 패키지
│   ├── controller/             # BookingApiController, BookingViewController, KiccPaymentApiController
│   ├── dto/request/            # BookingCreateRequest, BookingModifyRequest, etc.
│   ├── dto/response/           # 25+ response DTOs
│   ├── entity/                 # BookingApiKey, BookingAuditLog, ExchangeRate
│   ├── exception/              # BookingExceptionHandler
│   ├── gateway/                # PaymentGateway interface, MockPaymentGateway, Request/Result DTOs
│   ├── pg/kicc/                # KICC PG 구현 (KiccPaymentGateway, KiccApiClient, KiccConfig, DTOs)
│   ├── repository/             # BookingApiKeyRepository, BookingAuditLogRepository, ExchangeRateRepository
│   ├── security/               # BookingSecurityConfig, BookingApiKeyFilter
│   └── service/                # BookingService/Impl, CancellationPolicy, CardBinValidation, Currency
├── controller/                 # Admin 예약/프론트데스크 컨트롤러
├── dto/request/                # Admin 예약 관련 Request DTOs
├── dto/response/               # Admin 예약/프론트데스크/대시보드 Response DTOs
├── entity/                     # 예약 도메인 엔티티
├── mapper/                     # ReservationMapper
├── repository/                 # 예약 도메인 Repository
├── service/                    # 예약/프론트데스크/대시보드/가격계산/가용성 서비스
└── vo/                         # DayUseTimeSlot (Value Object)
```

### hola-app (실행 모듈)
**Purpose:** Spring Boot 부트스트랩, Thymeleaf 뷰, 정적 리소스, DB 마이그레이션, 통합 테스트

| Category | Count | Location |
|----------|-------|----------|
| Main Java | 4 | `HolaPmsApplication`, `DashboardController`, `GlobalModelAdvice`, `LoginController` |
| Templates | 70 | `src/main/resources/templates/` (3 layouts + 4 fragments + 63 pages) |
| JS Files | 60 | `src/main/resources/static/js/` |
| CSS Files | 2 | `src/main/resources/static/css/` (hola.css, booking.css) |
| Migrations | 96 | `src/main/resources/db/migration/` |
| Test Java | 7 | `BaseIntegrationTest`, `TestContainersConfig`, `TestFixtures`, 4 integration tests |
| Config | 3 | `application.yml`, `application-local.yml`, `application-test.yml` |

## Static Resources Organization

### JavaScript (`hola-app/src/main/resources/static/js/`)

**공통:**
- `hola-common.js` — HolaPms 네임스페이스 (ajax, alert, modal, context, renders, dataTableDefaults)

**Admin 도메인별 (명명 규칙: `{domain}-page.js` 리스트, `{domain}-form.js` 폼):**
- 호텔: `hotel.js`, `hotel-form.js`, `property-list.js`, `property-form.js`
- 회원/권한: `hotel-admin-page.js`, `hotel-admin-form.js`, `property-admin-page.js`, `property-admin-form.js`, `hotel-role-page.js`, `hotel-role-form.js`, `property-role-page.js`, `property-role-form.js`, `bluewave-admin-page.js`, `bluewave-admin-form.js`, `my-profile-form.js`
- 객실: `room-class-page.js`, `room-class-form.js`, `room-type-page.js`, `room-type-form.js`, `room-number-page.js`, `floor-page.js`
- 서비스옵션: `free-service-option-page.js`, `free-service-option-form.js`, `paid-service-option-page.js`, `paid-service-option-form.js`
- 레이트: `rate-code-page.js`, `rate-code-form.js`, `promotion-code-page.js`, `promotion-code-form.js`
- 예약: `reservation-table-view.js`, `reservation-calendar-view.js`, `reservation-timeline-view.js`, `reservation-form.js`, `reservation-detail.js`, `reservation-daily.js`, `reservation-payment.js`
- 프론트데스크: `fd-operations-page.js`, `fd-room-rack-page.js`, `fd-room-unavailable-page.js`
- 하우스키핑(Admin): `hk-dashboard-page.js`, `hk-board-page.js`, `hk-tasks-page.js`, `hk-settings-page.js`, `hk-staff-page.js`, `hk-staff-form.js`, `hk-attendance-page.js`, `hk-dayoff-page.js`, `hk-history-page.js`
- 기타: `market-code-page.js`, `early-late-policy-page.js`, `reservation-channel-page.js`, `transaction-code.js`, `inventory-item.js`, `dashboard-page.js`

**부킹엔진:**
- `booking.js` — 게스트 예약 화면 전용

**HK 모바일:**
- `hk-mobile-tasks.js`, `hk-mobile-summary.js`, `hk-mobile-profile.js`, `hk-mobile-dayoff.js`

### CSS (`hola-app/src/main/resources/static/css/`)
- `hola.css` — Admin 전체 스타일
- `booking.css` — 부킹엔진 전용 스타일

### Templates (`hola-app/src/main/resources/templates/`)

**Layouts (3종):**
- `layout/default.html` — Admin 메인 레이아웃 (sidebar + header + content)
- `layout/booking.html` — 부킹엔진 게스트 레이아웃
- `layout/mobile.html` — HK 모바일 레이아웃

**Fragments:**
- `layout/fragments/header.html` — Admin 상단바
- `layout/fragments/sidebar.html` — Admin 사이드바 메뉴
- `layout/fragments/footer.html` — Admin 하단
- `layout/fragments/common-fields.html` — 공통 폼 필드

**Page Templates by Domain:**

| Domain | Templates | Path |
|--------|-----------|------|
| 로그인 | `login.html` | `templates/login.html` |
| 대시보드 | `dashboard.html` | `templates/dashboard.html` |
| 호텔 | `list.html`, `form.html` | `templates/hotel/` |
| 프로퍼티 | `list.html`, `form.html`, `early-late-policy.html` | `templates/property/` |
| 프로퍼티 프래그먼트 | (property 폼 내 탭 프래그먼트) | `templates/property/fragments/` |
| 호텔관리자 | `list.html`, `form.html` | `templates/hotel-admin/` |
| 프로퍼티관리자 | `list.html`, `form.html` | `templates/property-admin/` |
| 호텔권한 | `list.html`, `form.html` | `templates/hotel-role/` |
| 프로퍼티권한 | `list.html`, `form.html` | `templates/property-role/` |
| BW관리자 | `list.html`, `form.html` | `templates/bluewave-admin/` |
| 내프로필 | `form.html` | `templates/my-profile/` |
| 객실클래스 | `list.html`, `form.html` | `templates/room-class/` |
| 객실타입 | `list.html`, `form.html` | `templates/room-type/` |
| 층 | `list.html` | `templates/floor/` |
| 호수 | `list.html` | `templates/room-number/` |
| 무료서비스 | `list.html`, `form.html` | `templates/free-service-option/` |
| 유료서비스 | `list.html`, `form.html` | `templates/paid-service-option/` |
| TC그룹/코드 | `list.html` | `templates/transaction-code/` |
| 재고 | `list.html` | `templates/inventory-item/` |
| 마켓코드 | `list.html` | `templates/market-code/` |
| 예약채널 | `list.html` | `templates/reservation-channel/` |
| 레이트코드 | `list.html`, `form.html` | `templates/rate-code/` |
| 프로모션코드 | `list.html`, `form.html` | `templates/promotion-code/` |
| 예약 | `list.html`, `form.html`, `detail.html`, `daily.html` | `templates/reservation/` |
| 프론트데스크 | `operations.html`, `room-rack.html`, `room-unavailable.html` | `templates/front-desk/` |
| 하우스키핑(Admin) | `dashboard.html`, `board.html`, `tasks.html`, `settings.html`, `staff.html`, `staff-form.html`, `attendance.html`, `dayoff.html`, `history.html` | `templates/housekeeping/` |
| HK모바일 | `login.html`, `summary.html`, `tasks.html`, `profile.html`, `dayoff.html` | `templates/mobile/housekeeping/` |
| 부킹엔진 | `search.html`, `rooms.html`, `checkout.html`, `confirmation.html`, `cancellation.html`, `payment-return.html` | `templates/booking/` |
| 에러 | (에러 페이지) | `templates/error/` |

## Database Migrations (`hola-app/src/main/resources/db/migration/`)

**96개 SQL 파일, 버전 대역:**

| 대역 | 범위 | 도메인 | 파일 수 |
|------|------|--------|---------|
| V1 | V1_0_0 ~ V1_13_0 | 호텔/프로퍼티/회원/권한/메뉴 | 13 |
| V2 | V2_0_0 ~ V2_5_0 | 객실클래스/타입/서비스옵션 | 8 |
| V3 | V3_0_0 ~ V3_5_0 | 레이트코드/프로모션코드 | 6 |
| V4 | V4_0_0 ~ V4_20_0 | 예약/결제/부킹 | 21 |
| V5 | V5_0_0 ~ V5_18_0 | 테스트 데이터 | 19 |
| V6 | V6_1_0 ~ V6_9_0 | 트랜잭션코드/재고/업그레이드 | 9 |
| V7 | V7_1_0 ~ V7_2_0 | 객실상태/OOO-OOS | 2 |
| V8 | V8_1_0 ~ V8_9_0 | 하우스키핑 | 12 |

**최신 마이그레이션:**
- `V4_20_0__add_pg_transaction_fields.sql` — PG 결제 트랜잭션 필드 추가
- `V8_9_0__fix_orphan_inhouse_data.sql` — 하우스키핑 데이터 정리

**테스트 제외:** `application-test.yml`의 `flyway.target: 5.8.0` → V5_9_0+ 대용량 테스트 데이터 마이그레이션 제외

## Configuration Files

**Application Config:**
- `hola-app/src/main/resources/application.yml` — 공통 설정 (JPA, Flyway, Thymeleaf, Jackson, 파일 업로드)
- `hola-app/src/main/resources/application-local.yml` — 로컬 개발 설정 (DB 연결, Redis, JWT 시크릿, 로깅)
- `hola-app/src/test/resources/application-test.yml` — 테스트 설정 (TestContainers PostgreSQL, Flyway target)

**Build Config:**
- `hola-pms/build.gradle` — Root Gradle (Java 17, Spring Boot 3.2.5, Lombok, 전역 설정)
- `hola-pms/settings.gradle` — 6 서브모듈 포함
- `hola-pms/{module}/build.gradle` — 각 모듈별 의존성

**JPA 핵심 설정 (application.yml):**
- `open-in-view: false` — 뷰 렌더링 중 지연 로딩 불가
- `ddl-auto: none` — Flyway로만 스키마 관리
- `default_batch_fetch_size: 100` — IN 쿼리 배치 페칭

## Key File Locations

**Entry Points:**
- `hola-app/src/main/java/com/hola/HolaPmsApplication.java`: Spring Boot 메인 클래스
- `hola-app/src/main/java/com/hola/web/LoginController.java`: 로그인 페이지
- `hola-app/src/main/java/com/hola/web/DashboardController.java`: 대시보드 (View + API)

**Configuration:**
- `hola-common/src/main/java/com/hola/common/security/SecurityConfig.java`: Spring Security 4단 체인
- `hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingSecurityConfig.java`: 부킹 API 보안 (@Order 0)
- `hola-common/src/main/java/com/hola/common/config/JpaAuditingConfig.java`: JPA Auditing
- `hola-common/src/main/java/com/hola/common/config/OpenApiConfig.java`: Swagger 설정
- `hola-common/src/main/java/com/hola/common/config/WebConfig.java`: Web MVC 설정

**Core Infrastructure:**
- `hola-common/src/main/java/com/hola/common/entity/BaseEntity.java`: 전 엔티티 기반
- `hola-common/src/main/java/com/hola/common/dto/HolaResponse.java`: API 응답 래퍼
- `hola-common/src/main/java/com/hola/common/exception/ErrorCode.java`: 에러 코드 체계
- `hola-common/src/main/java/com/hola/common/exception/GlobalExceptionHandler.java`: 전역 예외 처리
- `hola-common/src/main/java/com/hola/common/security/AccessControlService.java`: 접근 권한 검증

**가장 복잡한 파일 (변경 주의):**
- `hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java` (1,969줄)
- `hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` (1,857줄)
- `hola-hotel/src/main/java/com/hola/hotel/service/HousekeepingServiceImpl.java` (892줄)
- `hola-common/src/main/java/com/hola/common/security/SecurityConfig.java` (247줄 — 인가 규칙 순서 중요)

**Testing:**
- `hola-app/src/test/java/com/hola/support/BaseIntegrationTest.java`: 통합 테스트 기반 클래스
- `hola-app/src/test/java/com/hola/config/TestContainersConfig.java`: TestContainers 설정
- `hola-app/src/test/java/com/hola/fixture/TestFixtures.java`: 테스트 픽스처 팩토리

**Frontend 공통:**
- `hola-app/src/main/resources/static/js/hola-common.js`: HolaPms 네임스페이스 (필수 로드)
- `hola-app/src/main/resources/static/css/hola.css`: Admin 전체 스타일
- `hola-app/src/main/resources/templates/layout/default.html`: Admin 기본 레이아웃

## Naming Conventions

**Java Files:**
- Controller: `{Domain}ApiController.java` (REST), `{Domain}ViewController.java` (Thymeleaf)
- Service: `{Domain}Service.java` (인터페이스), `{Domain}ServiceImpl.java` (구현)
- Entity: `{Domain}.java` (예: `Hotel.java`, `RoomType.java`)
- Repository: `{Domain}Repository.java`
- Mapper: `{Domain}Mapper.java`
- DTO Request: `{Domain}CreateRequest.java`, `{Domain}UpdateRequest.java`
- DTO Response: `{Domain}Response.java`, `{Domain}ListResponse.java`
- 하우스키핑: `Hk` 접두사 (예: `HkTask.java`, `HkTaskSheet.java`, `HkConfig.java`)
- 프론트데스크: `FrontDesk` 접두사 (예: `FrontDeskApiController.java`)

**JS Files:**
- 리스트 페이지: `{domain}-page.js` (예: `hotel-admin-page.js`)
- 폼/상세 페이지: `{domain}-form.js` (예: `hotel-admin-form.js`)
- HK 모바일: `hk-mobile-{function}.js` (예: `hk-mobile-tasks.js`)

**Templates:**
- 리스트: `{domain}/list.html`
- 폼: `{domain}/form.html`
- 상세: `{domain}/detail.html`

**DB Tables:**
- 접두사: `htl_` (호텔), `rm_` (객실), `rt_` (레이트), `rsv_` (예약), `fd_` (프론트데스크), `hk_` (하우스키핑)
- 시스템: `sys_` (메뉴, 권한)

**Flyway:**
- `V{major}_{minor}_{patch}__{description}.sql`
- 대역: V1(호텔) V2(객실) V3(레이트) V4(예약) V5(테스트데이터) V6(트랜잭션) V7(객실상태) V8(하우스키핑)

## Where to Add New Code

**새 도메인 기능 (기존 모듈):**
1. Entity: `hola-{module}/src/main/java/com/hola/{domain}/entity/NewEntity.java`
2. Repository: `hola-{module}/src/main/java/com/hola/{domain}/repository/NewEntityRepository.java`
3. DTO: `hola-{module}/src/main/java/com/hola/{domain}/dto/request/NewCreateRequest.java`, `dto/response/NewResponse.java`
4. Mapper: `hola-{module}/src/main/java/com/hola/{domain}/mapper/NewMapper.java`
5. Service: `hola-{module}/src/main/java/com/hola/{domain}/service/NewService.java`, `NewServiceImpl.java`
6. Controller: `hola-{module}/src/main/java/com/hola/{domain}/controller/NewApiController.java`, `NewViewController.java`
7. Template: `hola-app/src/main/resources/templates/{domain}/list.html`, `form.html`
8. JS: `hola-app/src/main/resources/static/js/{domain}-page.js`, `{domain}-form.js`
9. Flyway: `hola-app/src/main/resources/db/migration/V{n}_{m}_0__create_{table}.sql`
10. SecurityConfig: `hola-common/.../SecurityConfig.java` 에 URL 인가 추가 (구체적 경로 먼저!)
11. 메뉴: Flyway INSERT → `sys_menu` 테이블

**새 모듈 추가:**
1. `hola-pms/hola-{new}/build.gradle` 생성 (dependencies에 `hola-common` 추가)
2. `hola-pms/settings.gradle` 에 `include 'hola-{new}'` 추가
3. `hola-pms/hola-app/build.gradle` 에 `implementation project(':hola-{new}')` 추가
4. 패키지: `com.hola.{new}/` → `@SpringBootApplication(basePackages="com.hola")`가 자동 스캔

**새 유닛 테스트:**
- 각 도메인 모듈 `src/test/java/com/hola/{domain}/service/{Domain}ServiceImplTest.java`

**새 통합 테스트:**
- `hola-app/src/test/java/com/hola/integration/{domain}/{Domain}IntegrationTest.java`
- `BaseIntegrationTest` 상속 필수

**새 PG(결제) 연동:**
- `hola-reservation/src/main/java/com/hola/reservation/booking/pg/{provider}/` 패키지 생성
- `PaymentGateway` 인터페이스 구현
- `@Profile("!test")` 조건부 빈 등록

**새 Flyway 마이그레이션:**
- 도메인별 버전 대역 확인 후 다음 마이너 번호 사용
- 예: 하우스키핑 → `V8_10_0__description.sql`
- out-of-order 활성화되어 있으므로 순서 걱정 불필요

## Special Directories

**`hola-app/src/main/resources/db/migration/`:**
- Purpose: Flyway SQL 마이그레이션 파일 (96개)
- Generated: No (수동 작성)
- Committed: Yes

**`e2e-tests/`:**
- Purpose: E2E 자동화 테스트 (Node.js 기반)
- Contains: `phase1-auth.js`, `phase2-hotel.js`, `phase3-room.js`, `phase4-reservation.js`, `phase4-ota-check.js`
- Generated: No
- Committed: Yes (node_modules 제외)

**`docs/`:**
- Purpose: 프로젝트 문서 (기획, 분석, 리포트)
- Contains: `01-plan/features/`, `03-analysis/`, `04-report/`
- Generated: No
- Committed: Yes

**`build/` (각 모듈 내):**
- Purpose: Gradle 빌드 산출물
- Generated: Yes
- Committed: No (.gitignore)

**`uploads/` (런타임 생성):**
- Purpose: 파일 업로드 저장소 (프로퍼티 이미지 등)
- Generated: Yes (런타임)
- Committed: No

---

*Structure analysis: 2026-03-26*
