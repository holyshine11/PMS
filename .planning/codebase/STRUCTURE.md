# Codebase Structure

**Analysis Date:** 2026-02-28

## Directory Layout

```
hola-pms/
├── hola-common/              # M00: Shared foundation (security, tenant, auth, exceptions, DTOs)
│   └── src/main/java/com/hola/common/
│       ├── entity/BaseEntity.java
│       ├── dto/{HolaResponse.java, PageInfo.java}
│       ├── security/{SecurityConfig.java, JwtProvider.java, AccessControlService.java, ...}
│       ├── tenant/{TenantContext.java, TenantFilter.java, TenantIdentifierResolver.java, ...}
│       ├── auth/{entity/AdminUser.java, service/AuthService.java, ...}
│       ├── enums/ErrorCode.java
│       ├── exception/HolaException.java, RestExceptionHandler.java
│       ├── util/NameMaskingUtil.java
│       └── config/{JpaAuditingConfig.java, WebConfig.java, OpenApiConfig.java}
│
├── hola-hotel/               # M01: Hotel/Property/Member/Permission + M04: Housekeeping
│   └── src/main/java/com/hola/hotel/
│       ├── entity/{Hotel.java, Property.java, Floor.java, RoomNumber.java, ...}
│       ├── entity/{HkConfig.java, HkDayOff.java, HkSection.java, HkTask.java, ...}
│       ├── controller/
│       │   ├── HotelApiController.java         (REST: /api/v1/hotels)
│       │   ├── HotelViewController.java        (Web: /hotels)
│       │   ├── PropertyApiController.java      (REST: /api/v1/properties)
│       │   ├── HotelAdminApiController.java    (REST: /api/v1/hotel-admins)
│       │   ├── PropertyAdminApiController.java (REST: /api/v1/property-admins)
│       │   ├── HotelRoleApiController.java     (REST: /api/v1/hotel-roles)
│       │   ├── PropertyRoleApiController.java  (REST: /api/v1/property-roles)
│       │   ├── RoomNumberApiController.java    (REST: /api/v1/room-numbers)
│       │   ├── FloorApiController.java         (REST: /api/v1/floors)
│       │   ├── MarketCodeApiController.java    (REST: /api/v1/market-codes)
│       │   ├── HkMobileApiController.java      (REST: /api/v1/properties/*/hk-mobile/*, session-based)
│       │   ├── HousekeepingApiController.java  (REST: /api/v1/housekeeping/*, admin-only)
│       │   ├── HousekeeperApiController.java   (REST: /api/v1/housekeepers)
│       │   ├── RoomStatusApiController.java    (REST: /api/v1/room-status)
│       │   ├── ReservationChannelApiController.java (REST: /api/v1/reservation-channels)
│       │   ├── EarlyLateFeePolicyApiController.java (REST: /api/v1/early-late-policies)
│       │   └── [*ViewController.java]          (Web: Thymeleaf routes)
│       ├── service/
│       │   ├── HotelService.java / HotelServiceImpl.java
│       │   ├── PropertyService.java / PropertyServiceImpl.java
│       │   ├── HotelAdminService.java / HotelAdminServiceImpl.java
│       │   ├── PropertyAdminService.java / PropertyAdminServiceImpl.java
│       │   ├── HotelRoleService.java / HotelRoleServiceImpl.java
│       │   ├── PropertyRoleService.java / PropertyRoleServiceImpl.java
│       │   ├── RoomNumberService.java / RoomNumberServiceImpl.java
│       │   ├── FloorService.java / FloorServiceImpl.java
│       │   ├── MarketCodeService.java / MarketCodeServiceImpl.java
│       │   ├── HkAssignmentService.java / HkAssignmentServiceImpl.java
│       │   ├── HousekeepingService.java / HousekeepingServiceImpl.java
│       │   ├── HousekeeperService.java / HousekeeperServiceImpl.java
│       │   ├── RoomStatusService.java / RoomStatusServiceImpl.java
│       │   ├── RoomUnavailableService.java / RoomUnavailableServiceImpl.java
│       │   ├── EarlyLateFeePolicyService.java / EarlyLateFeePolicyServiceImpl.java
│       │   └── [other service pairs]
│       ├── repository/
│       │   ├── HotelRepository.java
│       │   ├── PropertyRepository.java
│       │   ├── FloorRepository.java
│       │   ├── RoomNumberRepository.java
│       │   ├── HkConfigRepository.java
│       │   ├── HkDayOffRepository.java
│       │   ├── HkTaskRepository.java
│       │   ├── RoomUnavailableRepository.java
│       │   └── [other repository pairs]
│       ├── mapper/
│       │   ├── HotelMapper.java
│       │   ├── PropertyMapper.java
│       │   └── [other mappers]
│       └── dto/
│           ├── request/{HotelCreateRequest.java, HotelUpdateRequest.java, ...}
│           └── response/{HotelResponse.java, PropertyResponse.java, RoleResponse.java, ...}
│
├── hola-room/                # M02: Room Class/Type/Service Option + Transaction Codes + Inventory
│   └── src/main/java/com/hola/room/
│       ├── entity/{RoomClass.java, RoomType.java, FreeServiceOption.java, PaidServiceOption.java, InventoryItem.java, ...}
│       ├── controller/
│       │   ├── RoomClassApiController.java
│       │   ├── RoomTypeApiController.java
│       │   ├── FreeServiceOptionApiController.java
│       │   ├── PaidServiceOptionApiController.java
│       │   ├── InventoryItemApiController.java
│       │   ├── TransactionCodeApiController.java
│       │   └── [*ViewController.java]
│       ├── service/
│       │   ├── RoomClassService.java / RoomClassServiceImpl.java
│       │   ├── RoomTypeService.java / RoomTypeServiceImpl.java
│       │   ├── FreeServiceOptionService.java / FreeServiceOptionServiceImpl.java
│       │   ├── PaidServiceOptionService.java / PaidServiceOptionServiceImpl.java
│       │   ├── InventoryItemService.java / InventoryItemServiceImpl.java
│       │   ├── TransactionCodeService.java / TransactionCodeServiceImpl.java
│       │   └── [other service pairs]
│       ├── repository/
│       │   ├── RoomClassRepository.java
│       │   ├── RoomTypeRepository.java
│       │   ├── FreeServiceOptionRepository.java
│       │   ├── PaidServiceOptionRepository.java
│       │   ├── InventoryItemRepository.java
│       │   ├── TransactionCodeRepository.java
│       │   └── [other repositories]
│       ├── mapper/
│       │   ├── RoomClassMapper.java
│       │   ├── RoomTypeMapper.java
│       │   └── [other mappers]
│       └── dto/
│           ├── request/{RoomClassCreateRequest.java, ...}
│           └── response/{RoomClassResponse.java, RoomTypeResponse.java, ...}
│
├── hola-rate/                # M03: Rate Code / Promotion Code
│   └── src/main/java/com/hola/rate/
│       ├── entity/{RateCode.java, PromotionCode.java}
│       ├── controller/
│       │   ├── RateCodeApiController.java
│       │   ├── RateCodeViewController.java
│       │   ├── PromotionCodeApiController.java
│       │   └── PromotionCodeViewController.java
│       ├── service/
│       │   ├── RateCodeService.java / RateCodeServiceImpl.java
│       │   ├── PromotionCodeService.java / PromotionCodeServiceImpl.java
│       │   └── [other service pairs]
│       ├── repository/
│       │   ├── RateCodeRepository.java
│       │   ├── PromotionCodeRepository.java
│       │   └── [other repositories]
│       ├── mapper/
│       │   ├── RateCodeMapper.java
│       │   └── PromotionCodeMapper.java
│       └── dto/
│           ├── request/{RateCodeCreateRequest.java, ...}
│           └── response/{RateCodeResponse.java, PromotionCodeResponse.java, ...}
│
├── hola-reservation/         # M07: Reservation + M10: Front Desk + M08: Booking Engine
│   └── src/main/java/com/hola/reservation/
│       ├── entity/
│       │   ├── Reservation.java
│       │   ├── MasterReservation.java / SubReservation.java
│       │   ├── ReservationPayment.java
│       │   ├── ReservationServiceItem.java
│       │   └── [other entities]
│       ├── controller/
│       │   ├── ReservationApiController.java  (REST: /api/v1/reservations)
│       │   ├── ReservationViewController.java (Web: /reservations)
│       │   ├── ReservationPaymentApiController.java (REST: /api/v1/reservation-payments)
│       │   ├── RoomRackController.java         (REST: /api/v1/room-rack/*)
│       │   ├── booking/BookingApiController.java (REST: /api/v1/booking/*, stateless API-KEY auth)
│       │   ├── booking/BookingViewController.java (Web: /booking/*)
│       │   └── [other controllers]
│       ├── service/
│       │   ├── ReservationService.java / ReservationServiceImpl.java
│       │   ├── ReservationPaymentService.java / ReservationPaymentServiceImpl.java
│       │   ├── booking/BookingService.java / BookingServiceImpl.java
│       │   ├── booking/PaymentGatewayService.java
│       │   └── [other service pairs]
│       ├── repository/
│       │   ├── ReservationRepository.java
│       │   ├── MasterReservationRepository.java
│       │   ├── SubReservationRepository.java
│       │   ├── ReservationPaymentRepository.java
│       │   └── [other repositories]
│       ├── mapper/
│       │   ├── ReservationMapper.java
│       │   └── [other mappers]
│       ├── booking/
│       │   ├── gateway/{PaymentGatewayAdapter, BookingGateway}
│       │   ├── security/BookingApiKeyFilter.java
│       │   ├── exception/
│       │   └── [booking-specific code]
│       └── dto/
│           ├── request/{ReservationCreateRequest.java, ...}
│           └── response/{ReservationResponse.java, ReservationPaymentResponse.java, ...}
│
├── hola-app/                 # Runtime module: boot JAR, Thymeleaf views, migrations, static assets
│   ├── src/main/java/com/hola/
│   │   ├── HolaPmsApplication.java     (Spring Boot entry point)
│   │   └── web/
│   │       ├── LoginController.java    (form login, /login, /logout, /my-profile)
│   │       ├── DashboardController.java (admin dashboard)
│   │       └── GlobalModelAdvice.java  (global @ModelAttribute)
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   │   ├── V1_*__*.sql             (M01 hotel tables)
│   │   │   ├── V2_*__*.sql             (M02 room tables)
│   │   │   ├── V3_*__*.sql             (M03 rate tables)
│   │   │   ├── V4_*__*.sql             (M07 reservation tables)
│   │   │   ├── V5_*__*.sql             (test data, flyway.target: 5.8.0)
│   │   │   ├── V6_*__*.sql             (M06 transaction code tables)
│   │   │   ├── V7_*__*.sql             (M09 room status tables)
│   │   │   ├── V8_*__*.sql             (M04 housekeeping tables)
│   │   │   └── V9_*__*.sql             (future phases)
│   │   ├── templates/
│   │   │   ├── layout/
│   │   │   │   ├── default.html        (Admin layout: 3-column with sidebar)
│   │   │   │   ├── booking.html        (Booking engine layout)
│   │   │   │   ├── mobile.html         (HK mobile layout)
│   │   │   │   └── fragments/
│   │   │   │       ├── header.html
│   │   │   │       ├── sidebar.html
│   │   │   │       ├── footer.html
│   │   │   │       └── [modals, common components]
│   │   │   ├── login.html              (login form)
│   │   │   ├── error/
│   │   │   │   ├── 403.html
│   │   │   │   ├── 404.html
│   │   │   │   └── 500.html
│   │   │   ├── bluewave-admin/        (system admin pages)
│   │   │   ├── hotel/                 (M01: hotel CRUD, market-code, floor, room-number)
│   │   │   ├── property/              (M01: property CRUD, fragments)
│   │   │   ├── hotel-admin/           (M01: hotel admin CRUD)
│   │   │   ├── property-admin/        (M01: property admin CRUD)
│   │   │   ├── hotel-role/            (M01: role CRUD + permission matrix)
│   │   │   ├── property-role/         (M01: property role CRUD)
│   │   │   ├── room-class/            (M02: room class CRUD)
│   │   │   ├── room-type/             (M02: room type CRUD)
│   │   │   ├── room-number/           (M02: room number CRUD, room-rack view)
│   │   │   ├── floor/                 (M02: floor CRUD)
│   │   │   ├── free-service-option/   (M02: free service option CRUD)
│   │   │   ├── paid-service-option/   (M02: paid service option CRUD)
│   │   │   ├── transaction-code/      (M06: TC CRUD)
│   │   │   ├── inventory-item/        (M02: inventory item CRUD)
│   │   │   ├── rate-code/             (M03: rate code CRUD)
│   │   │   ├── promotion-code/        (M03: promotion code CRUD)
│   │   │   ├── reservation-channel/   (M07: channel CRUD)
│   │   │   ├── reservation/           (M07: reservation CRUD, timeline/table views)
│   │   │   ├── front-desk/            (M10: operations, room-rack, room-unavailable)
│   │   │   ├── housekeeping/          (M04: admin dashboard, task sheet, attendance)
│   │   │   ├── mobile/housekeeping/   (M04: mobile task list, room status updates)
│   │   │   ├── booking/               (M08: booking engine forms)
│   │   │   └── my-profile/            (user profile management)
│   │   ├── static/
│   │   │   ├── css/
│   │   │   │   ├── hola.css           (global styles, DataTable customization)
│   │   │   │   └── [domain-specific CSS]
│   │   │   ├── js/
│   │   │   │   ├── hola-common.js     (HolaPms namespace: ajax, alert, context, validators)
│   │   │   │   ├── {domain}-page.js   (DataTable init, list view logic)
│   │   │   │   ├── {domain}-form.js   (form submission, detail view logic)
│   │   │   │   ├── booking.js         (booking engine JavaScript)
│   │   │   │   ├── dashboard-page.js  (admin dashboard charts/widgets)
│   │   │   │   ├── fd-room-rack-page.js (front desk room rack)
│   │   │   │   ├── hk-mobile-*.js     (HK mobile views)
│   │   │   │   └── [other domain-specific scripts]
│   │   │   └── images/
│   │   │       └── [logos, icons, static images]
│   │   ├── application.yml            (profiles: default, local, test, prod)
│   │   ├── application-local.yml      (local: PostgreSQL, Redis, Flyway)
│   │   └── application-test.yml       (test: TestContainers, flyway.target: 5.8.0)
│   └── build.gradle                   (depends on: hola-common, hola-hotel, hola-room, hola-rate, hola-reservation)
│
├── e2e-tests/                # Playwright E2E tests (JavaScript)
│   ├── tests/
│   │   ├── hotel.spec.ts
│   │   ├── reservation.spec.ts
│   │   ├── payment.spec.ts
│   │   └── [other specs]
│   ├── playwright.config.ts
│   └── package.json
│
├── docs/                     # Design assets & planning
│   ├── 01-plan/features/     (phase plans: transaction-charge-system.plan.md, dayuse-feature.md, etc.)
│   ├── 03-analysis/
│   └── 04-report/
│
├── settings.gradle           (subproject declarations: hola-common, hola-hotel, hola-room, hola-rate, hola-reservation, hola-app)
├── build.gradle              (root: Gradle 7.x, Java 17, Spring Boot 3.2.5)
└── gradlew / gradlew.bat     (Gradle wrapper)
```

## Directory Purposes

**hola-common:**
- Purpose: Shared infrastructure (base classes, security, tenant routing, exceptions, DTOs)
- Contains: BaseEntity, HolaResponse, ErrorCode, SecurityConfig, TenantContext, AuthService, utils
- Key files: `entity/BaseEntity.java`, `dto/HolaResponse.java`, `security/SecurityConfig.java`, `tenant/TenantContext.java`

**hola-hotel:**
- Purpose: Hotel master data (hotel/property/admin/roles) + housekeeping (M04: HK config, tasks, assignments)
- Contains: Hotel, Property, Floor, RoomNumber, HotelAdmin, PropertyAdmin, Role entities; HK service + mobile API
- Key files: `entity/{Hotel, Property, Floor}.java`, `controller/HotelApiController.java`, `service/HotelServiceImpl.java`

**hola-room:**
- Purpose: Room inventory (room class/type/service options) + transaction codes + inventory items
- Contains: RoomClass, RoomType, FreeServiceOption, PaidServiceOption, TransactionCode, InventoryItem entities
- Key files: `entity/{RoomClass, RoomType, PaidServiceOption}.java`, `controller/RoomTypeApiController.java`

**hola-rate:**
- Purpose: Rate management (rate codes, promotion codes with 12 types)
- Contains: RateCode, PromotionCode entities; rate lookup + promo calculation logic
- Key files: `entity/{RateCode, PromotionCode}.java`, `controller/RateCodeApiController.java`

**hola-reservation:**
- Purpose: Reservation lifecycle (create/pay/check-in/out) + front desk ops + booking engine
- Contains: Reservation, MasterReservation, SubReservation, ReservationPayment, ServiceItem entities; booking gateway
- Key files: `entity/Reservation.java`, `controller/ReservationApiController.java`, `booking/BookingApiController.java`

**hola-app:**
- Purpose: Runtime container; integrates all modules, serves Thymeleaf views + static assets, runs migrations
- Contains: HolaPmsApplication, LoginController, DashboardController, templates, static (css/js/images), Flyway migrations
- Key files: `HolaPmsApplication.java`, `templates/layout/default.html`, `static/js/hola-common.js`, `db/migration/V*_*_*__*.sql`

## Key File Locations

**Entry Points:**

- `hola-app/src/main/java/com/hola/HolaPmsApplication.java` — Spring Boot entry point (@SpringBootApplication)
- `hola-common/src/main/java/com/hola/common/security/SecurityConfig.java` — Auth chain configuration (4 filters)
- `hola-app/src/main/java/com/hola/web/LoginController.java` — Form login (/login, /logout, /my-profile routes)
- `hola-hotel/src/main/java/com/hola/hotel/controller/HotelApiController.java` — Hotel REST API (/api/v1/hotels)
- `hola-reservation/src/main/java/com/hola/reservation/booking/controller/BookingApiController.java` — Booking API (/api/v1/booking/*)

**Configuration:**

- `hola-common/src/main/java/com/hola/common/config/JpaAuditingConfig.java` — JPA @CreatedBy/@LastModifiedBy setup
- `hola-common/src/main/java/com/hola/common/config/WebConfig.java` — Web converter registration
- `hola-common/src/main/java/com/hola/common/config/OpenApiConfig.java` — Swagger UI title + info
- `hola-app/src/main/resources/application.yml` — Spring profiles, logging level, Flyway config
- `hola-app/src/main/resources/application-local.yml` — Local development (PostgreSQL, Redis, TZ: Asia/Seoul)

**Core Logic:**

- `hola-common/src/main/java/com/hola/common/entity/BaseEntity.java` — Soft delete foundation (softDelete(), @SQLRestriction)
- `hola-common/src/main/java/com/hola/common/tenant/TenantContext.java` — ThreadLocal tenant routing
- `hola-common/src/main/java/com/hola/common/security/AccessControlService.java` — Property-level authz checks
- `hola-hotel/src/main/java/com/hola/hotel/service/HotelServiceImpl.java` — Hotel CRUD business logic (JPQL null pattern avoidance)
- `hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` — Reservation lifecycle

**Testing:**

- `hola-app/src/test/java/com/hola/integration/` — Integration tests (BaseIntegrationTest, TestFixtures)
- `hola-app/src/test/java/com/hola/fixture/` — Test data builders
- `hola-hotel/src/test/java/com/hola/hotel/service/HotelServiceTest.java` — Unit tests (@ExtendWith(MockitoExtension))
- `hola-app/src/test/resources/application-test.yml` — TestContainers PostgreSQL config

**Frontend:**

- `hola-app/src/main/resources/templates/layout/default.html` — Admin layout (sidebar + main content)
- `hola-app/src/main/resources/templates/layout/mobile.html` — HK mobile layout
- `hola-app/src/main/resources/static/js/hola-common.js` — HolaPms namespace (ajax, alert, context, validators)
- `hola-app/src/main/resources/static/css/hola.css` — Global styles (colors, typography, DataTable customization)
- `hola-app/src/main/resources/templates/{domain}/{page}.html` — Domain-specific pages (e.g., `hotel/list.html`)

**Database:**

- `hola-app/src/main/resources/db/migration/V1_1_0__create_hotel_tables.sql` — M01 schema
- `hola-app/src/main/resources/db/migration/V2_1_0__create_room_tables.sql` — M02 schema
- `hola-app/src/main/resources/db/migration/V3_1_0__create_rate_tables.sql` — M03 schema
- `hola-app/src/main/resources/db/migration/V4_1_0__create_reservation_tables.sql` — M07 schema
- `hola-app/src/main/resources/db/migration/V8_*__*.sql` — M04 housekeeping tables

## Naming Conventions

**Files:**

- **Entities**: `{Domain}Entity.java` or just `{Domain}.java` (e.g., `Hotel.java`, `Property.java`, `RoomClass.java`)
- **Services**: Interface `{Domain}Service.java`, Implementation `{Domain}ServiceImpl.java` (e.g., `HotelService.java`, `HotelServiceImpl.java`)
- **Controllers**: `{Domain}ApiController.java` (REST), `{Domain}ViewController.java` (Web) (e.g., `HotelApiController.java`, `HotelViewController.java`)
- **Repositories**: `{Domain}Repository.java` (e.g., `HotelRepository.java`)
- **Mappers**: `{Domain}Mapper.java` (e.g., `HotelMapper.java`)
- **DTOs**: `{Domain}CreateRequest.java`, `{Domain}UpdateRequest.java`, `{Domain}Response.java` (e.g., `HotelCreateRequest.java`, `HotelResponse.java`)
- **Tests**: `{Domain}ServiceTest.java`, `{Domain}ControllerTest.java` (e.g., `HotelServiceTest.java`)
- **Templates**: `{domain}/{page}.html` (e.g., `hotel/list.html`, `reservation/detail.html`)
- **JavaScript**: `{domain}-page.js` (list view), `{domain}-form.js` (form/detail) (e.g., `hotel-page.js`, `hotel-form.js`)

**Directories:**

- **Domain packages**: `com.hola.{domain}` (e.g., `com.hola.hotel`, `com.hola.room`, `com.hola.reservation`)
- **Sublayers**: `entity`, `controller`, `service`, `repository`, `mapper`, `dto` (e.g., `com.hola.hotel.entity`, `com.hola.hotel.controller`)
- **DTO subpackages**: `dto.request`, `dto.response` (e.g., `com.hola.hotel.dto.request`)

## Where to Add New Code

**New Feature (Controller + Service + Repository):**
- Primary code: `{domain}/src/main/java/com/hola/{domain}/` (create controller, service interface + impl, repository, entity, DTOs)
- Tests: `{domain}/src/test/java/com/hola/{domain}/service/` (unit test with @Mock/@InjectMocks)
- Integration tests: `hola-app/src/test/java/com/hola/integration/{domain}/` (extends BaseIntegrationTest)
- Database: `hola-app/src/main/resources/db/migration/V{major}_{minor}_{patch}__{desc}.sql` (matches version band: V1 hotel, V2 room, V3 rate, V4 reservation, V8 housekeeping)

**New Entity:**
- Entity file: `{domain}/src/main/java/com/hola/{domain}/entity/{Entity}.java` (extends BaseEntity, @Entity, @SQLRestriction)
- Repository: `{domain}/src/main/java/com/hola/{domain}/repository/{Entity}Repository.java` (extends JpaRepository)
- Migration: `hola-app/src/main/resources/db/migration/V{band}_{sequence}__{desc}.sql` (CREATE TABLE with snake_case, audit columns, FK constraints)

**New API Endpoint:**
- Controller method: `{domain}/src/main/java/com/hola/{domain}/controller/{Domain}ApiController.java` (@RequestMapping, @GetMapping/@PostMapping/@PutMapping/@DeleteMapping)
- DTO: `{domain}/src/main/java/com/hola/{domain}/dto/request/{Name}Request.java` (or `/response/`)
- Service method: `{domain}/src/main/java/com/hola/{domain}/service/{Domain}ServiceImpl.java` (@Transactional or @Transactional(readOnly=true))
- Response: Always wrap in `HolaResponse.success(data)` or `HolaResponse.error(ErrorCode.XXX)`

**New Page (Thymeleaf + JavaScript):**
- Template: `hola-app/src/main/resources/templates/{domain}/{page}.html` (layout:fragment="content", extends `layout:default`)
- JavaScript: `hola-app/src/main/resources/static/js/{domain}-page.js` (init(), bindEvents(), reload() pattern, DataTable if list)
- CSS: Add rules to `hola-app/src/main/resources/static/css/hola.css` or inline in template (prefer global)
- Controller: `{domain}/src/main/java/com/hola/{domain}/controller/{Domain}ViewController.java` (@Controller, return "domain/page")

**New Test:**
- Unit: `{domain}/src/test/java/com/hola/{domain}/service/{Domain}ServiceTest.java` (@ExtendWith(MockitoExtension), @Mock/@InjectMocks, @Nested)
- Integration: `hola-app/src/test/java/com/hola/integration/{domain}/{Feature}Test.java` (extends BaseIntegrationTest, @Transactional, @WithMockUser)
- Fixtures: `hola-app/src/test/java/com/hola/fixture/` (builders or factory methods in TestFixtures class)

**New Migration:**
- File: `hola-app/src/main/resources/db/migration/V{major}_{minor}_{patch}__{description}.sql`
- Naming: Flyway version must match module band (V1 for hotel, V2 for room, V3 for rate, V4 for reservation, V8 for housekeeping)
- Content: CREATE TABLE / ALTER TABLE with snake_case naming (`htl_*` prefix for hotel, `rm_*` for room, `rt_*` for rate, `rsv_*` for reservation, `hk_*` for housekeeping)
- Constraints: NOT NULL for required, unique constraints, FK to `{prefix}_id`, include audit columns (created_at, updated_at, created_by, updated_by, deleted_at, use_yn, sort_order)

## Special Directories

**hola-app/src/main/resources/db/migration/:**
- Purpose: Flyway versioned SQL migrations (schema-per-tenant PostgreSQL)
- Generated: No (manual SQL scripts)
- Committed: Yes (critical for reproducible deployments)
- Naming: `V{major}_{minor}_{patch}__{description}.sql`, out-of-order enabled
- Version bands:
  - V1: Hotel entities (hotel, property, floor, room-number, market-code, admin users, roles)
  - V2: Room entities (room-class, room-type, service-options, inventory)
  - V3: Rate entities (rate-code, promotion-code)
  - V4: Reservation entities (reservation, payment, service-items)
  - V5: Test data (excluded from test via flyway.target: 5.8.0)
  - V6: Transaction code (not yet implemented)
  - V7: Room status (not yet implemented)
  - V8: Housekeeping entities (hk-config, hk-task, hk-section, hk-assignment, hk-day-off)

**hola-app/src/main/resources/templates/layout/fragments/:**
- Purpose: Reusable Thymeleaf fragments (header, sidebar, footer, modals, common components)
- Generated: No
- Committed: Yes
- Key fragments: `header.html` (top nav + tenant selector), `sidebar.html` (menu tree with sec:authorize), `footer.html`, error modals

**hola-app/src/main/resources/static/css/:**
- Purpose: Global CSS (colors, typography, DataTable customization, form/layout utilities)
- Generated: No
- Committed: Yes
- Key file: `hola.css` (centralized styles to avoid inline; uses Pretendard font, Bootstrap 5.3 grid, custom DataTable styling)

**hola-app/src/main/resources/static/js/:**
- Purpose: Client-side JavaScript (HolaPms namespace, domain-specific page logic, DataTable initialization)
- Generated: No
- Committed: Yes
- Key file: `hola-common.js` (HolaPms.ajax, HolaPms.alert, HolaPms.context, HolaPms.maskName/maskPhone, HolaPms.dataTableDefaults)
- Domain files: `{domain}-page.js` (list/overview), `{domain}-form.js` (CRUD forms), domain-specific: `booking.js`, `dashboard-page.js`, `hk-mobile-*.js`

**hola-app/src/test/:**
- Purpose: Integration + fixture tests (NOT unit tests; unit tests live in module subdirectories)
- Generated: No
- Committed: Yes
- Key files: `integration/BaseIntegrationTest.java` (TestContainers setup), `fixture/TestFixtures.java` (test data builders), `integration/{domain}/{Feature}Test.java`

**hola-app/build/generated/:**
- Purpose: Build artifacts (compiled classes, generated code)
- Generated: Yes (Gradle build output)
- Committed: No (.gitignore)

---

*Structure analysis: 2026-02-28*
