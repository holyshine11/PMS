# Architecture

**Analysis Date:** 2026-02-28

## Pattern Overview

**Overall:** Modular Monolith with Schema-per-Tenant

**Key Characteristics:**
- 5 domain modules + 1 runtime module, all exposing REST APIs + Thymeleaf views
- Layered architecture (Controller → Service → Repository → Entity) applied uniformly across all modules
- Cross-module dependencies via Spring dependency injection; **no JPA associations between modules** (primary keys stored as `Long` columns only)
- Tenant isolation via PostgreSQL schema-per-tenant model with `TenantContext` ThreadLocal routing
- Multi-auth: JWT (REST API), session (Admin form login), HK Mobile session (isolated from Admin)
- Soft delete on all entities via `BaseEntity.softDelete()` + `@SQLRestriction("deleted_at IS NULL")`

## Layers

**Controller (API + View Separation):**
- Purpose: HTTP entry point, request parsing, response marshalling
- Location: `{module}/src/main/java/com/hola/{domain}/controller/`
- Contains: `XxxApiController` (@RestController, `/api/v1/...`) and `XxxViewController` (@Controller, Thymeleaf routes)
- Depends on: Service, DTOs, HolaResponse
- Used by: HTTP clients, browsers

**Service (Interface + Implementation):**
- Purpose: Business logic, domain state transitions, transaction boundaries
- Location: `{module}/src/main/java/com/hola/{domain}/service/`
- Contains: `XxxService` (interface) + `XxxServiceImpl` (@Service, @Transactional(readOnly=true) class-level)
- Depends on: Repository, Entity, Mapper, other Services (via dependency injection), ErrorCode enum
- Used by: Controllers, other Services
- Pattern: Write methods marked `@Transactional` (no readOnly), read-only class-level annotation overridden per method

**Repository (Data Access):**
- Purpose: Entity persistence, query abstraction
- Location: `{module}/src/main/java/com/hola/{domain}/repository/`
- Contains: `XxxRepository extends JpaRepository<Xxx, Long>`
- Depends on: Entities, Spring Data JPA
- Used by: Services
- Note: Custom queries use `@Query` + `@Modifying` for bulk operations; `deleteAllByXxx()` is forbidden without `@Modifying`

**Entity (Domain Model):**
- Purpose: Persistence model, business invariants
- Location: `{module}/src/main/java/com/hola/{domain}/entity/`
- Contains: `Xxx extends BaseEntity` (@Entity, @Table, @SQLRestriction, business methods)
- Depends on: BaseEntity, JPA, Hibernate annotations, other entities (LAZY loaded)
- Used by: Repository, Service (via query results)
- Pattern: All entities inherit `id`, `createdAt/By`, `updatedAt/By`, `deletedAt`, `useYn`, `sortOrder`; business methods encapsulate state changes

**DTO (Data Transfer Objects):**
- Purpose: Decouple API contract from persistence model
- Location: `{module}/src/main/java/com/hola/{domain}/dto/request/` + `.../dto/response/`
- Contains: `CreateRequest`, `UpdateRequest` (request), `Response` (response, @Builder)
- Depends on: Validation annotations (@NotBlank, @Valid, etc.)
- Used by: Controllers, Mappers
- Pattern: Request DTOs with validation constraints; response DTOs with @Builder for flexible construction

**Mapper (Manual Conversion):**
- Purpose: Entity ↔ DTO bidirectional transformation
- Location: `{module}/src/main/java/com/hola/{domain}/mapper/`
- Contains: `XxxMapper` (@Component, manual `toEntity()` / `toResponse()` methods)
- Depends on: Entities, DTOs
- Used by: Services
- Pattern: MapStruct declared but **manual mapping preferred** for explicit control and cross-module DTO assembly

**hola-common (M00 - Foundation):**
- Purpose: Shared base classes, security, tenant routing, exception handling, utility
- Location: `hola-common/src/main/java/com/hola/common/`
- Contains:
  - `entity/BaseEntity.java` — JPA MappedSuperclass with audit fields, soft delete methods
  - `dto/{HolaResponse, PageInfo}` — unified API response envelope, pagination metadata
  - `security/{SecurityConfig, JwtProvider, AccessControlService, JwtAuthenticationFilter, HkMobileSessionFilter, RoleBasedAuthSuccessHandler}` — 4-chain auth, JWT, tenant access validation
  - `tenant/{TenantContext, TenantFilter, TenantConnectionProvider, TenantIdentifierResolver}` — schema-per-tenant routing
  - `auth/{entity/AdminUser, service/AuthService}` — user/password management, multi-tenant admin isolation
  - `config/{JpaAuditingConfig, WebConfig, OpenApiConfig}` — Flyway, JPA auditing, Swagger init
  - `enums/ErrorCode.java` — centralized error codes (HOLA-XXXX format)
  - `exception/HolaException.java` — runtime exception wrapper
  - `util/NameMaskingUtil.java` — PII masking
- Depends on: Spring Security, Spring Data JPA, Spring Data Redis, JWT, Thymeleaf, Flyway, Swagger
- Used by: All domain modules (api dependency)

## Data Flow

**Admin Web (Form Login + Thymeleaf):**

1. **User Request** → `GET /login` browser load
2. **Controller** → `LoginController.loginForm()` returns `login.html` template
3. **Form Submit** → POST with `username` + `password` + CSRF token
4. **Spring Security** → `UsernamePasswordAuthenticationFilter` → `AuthService.loadUserByUsername()` (AdminUser lookup)
5. **Password Verify** → `PasswordEncoder.matches()`
6. **Session Created** → `HttpSession` set with `AdminUser` principal
7. **Redirect** → `RoleBasedAuthSuccessHandler.onAuthenticationSuccess()` routes by role (SUPER_ADMIN → `/admin`, HOTEL_ADMIN → `/hotel-admin`, etc.)
8. **Page Request** → `HotelViewController.hotelList()` (e.g.)
9. **Service → Repository** → `hotelRepository.findAll(pageable)` returns `Page<Hotel>`
10. **Mapper** → `hotelMapper.toResponse(hotel)` builds `List<HotelResponse>`
11. **Thymeleaf Render** → `layout/default.html` + `hotel/list.html` fragment
12. **JavaScript** → `hotel-page.js` hydrates DataTable, event bindings
13. **Response** → HTML + toast notifications (HolaPms.alert)

**REST API (JWT + Stateless):**

1. **API Request** → `POST /api/v1/hotels` with `Authorization: Bearer {JWT}`
2. **JwtAuthenticationFilter** → extracts token, calls `JwtProvider.validateToken(token)`
3. **Claims Validated** → extract `sub` (loginId), `hotelIds`, `role`
4. **SecurityContext** → set `UsernamePasswordAuthenticationToken` with roles
5. **Controller** → `HotelApiController.createHotel(HotelCreateRequest)` receives deserialized JSON
6. **Validation** → `@Valid` trigger Jakarta constraints (@NotBlank, etc.)
7. **Service** → `HotelServiceImpl.createHotel()` business logic (duplicate check, code generation)
8. **Repository** → `hotelRepository.save(hotel)` writes to `public.htl_hotel` (or tenant schema)
9. **Response** → `HolaResponse.success(hotelResponse)` JSON envelope
10. **Status** → `200 OK` + JSON body with `success:true`, `code:HOLA-0000`, `data:{...}`

**HK Mobile (Isolated Session):**

1. **HK User Login** → `POST /api/v1/properties/{propertyId}/hk-mobile/login`
2. **HkMobileSessionFilter** (pre-auth) → attempts session lookup, saves original `SecurityContext`
3. **Controller** → `HkMobileApiController.login()` validates password → creates new `HttpSession`
4. **Session Attrs** → `hkUserId`, `hkUserRole` stored in session (NOT `SecurityContext`)
5. **HkMobileSessionFilter** (post-auth) → **restores original SecurityContext** in finally block (prevents Admin/Mobile cross-contamination)
6. **Subsequent Requests** → `HkMobileSessionFilter` loads `hkUserId` from session, bypasses `AccessControlService` (mobile has different authz model)
7. **Response** → API returns `HolaResponse.success(data)` same as Admin API
8. **Mobile UI** → Uses same layout + Bootstrap but `layout/mobile.html` variant for responsive design

**Tenant Data Routing:**

1. **Request Entry** → `TenantFilter` (SecurityContextHolderFilter.AFTER) reads `X-Tenant-ID` header or defaults to `public` schema
2. **TenantContext.setTenantId()** → stores tenant ID in ThreadLocal
3. **JPA Query** → `TenantIdentifierResolver.resolveCurrentTenantIdentifier()` used by Hibernate to set `SET search_path = {tenantSchema}`
4. **Data Isolation** → all queries auto-filtered to tenant schema (e.g., `SELECT * FROM tenant_schema.htl_hotel`)
5. **Response** → data scoped to tenant schema only
6. **TenantFilter finally block** → `TenantContext.clear()` removes ThreadLocal (prevents leakage to next request)

**State Management:**

- **Entities**: Mutable business objects with `@Transactional` method scope; JPA dirty checking auto-persists changes
- **Services**: Orchestrate entity state via repositories; transactions bound at service method
- **Controllers**: Stateless; extract current user from `SecurityContextHolder.getContext().getAuthentication()` or session attributes
- **Client (Admin)**: sessionStorage + `HolaPms.context` manages current hotel/property selection; broadcasts `hola:contextChange` custom event
- **Client (Mobile)**: sessionStorage stores task state; server authoritative on persistence

## Key Abstractions

**BaseEntity (Foundational):**
- Purpose: Standardize audit fields + soft delete on all domain entities
- Examples: `Hotel`, `Property`, `Room`, `Reservation`, `RoomNumber`, etc.
- Pattern: `@MappedSuperclass` with `@EntityListeners(AuditingEntityListener.class)`; `softDelete()` method sets `deletedAt` + `useYn=false`; `@SQLRestriction` auto-filters soft-deleted rows on queries

**HolaResponse\<T\> (API Contract):**
- Purpose: Unified response envelope for REST APIs (success/error/pagination)
- Examples: `HolaResponse.success(hotelList, pageInfo)`, `HolaResponse.error(ErrorCode.HOLA_0001, message)`
- Pattern: Static factory methods; `@JsonInclude(NON_NULL)` omits null fields; always includes `success`, `code`, `message`, `timestamp`

**ErrorCode (Domain Errors):**
- Purpose: Centralized error taxonomy (HOLA-XXXX format)
- Examples: `HOTEL_NAME_DUPLICATE = "HOLA-1001"`, `PROPERTY_NOT_FOUND = "HOLA-1002"`, `INVALID_JWT = "HOLA-0005"`
- Pattern: Enum with code + message; service throws `HolaException(ErrorCode.XXX)` which is auto-caught by `RestExceptionHandler` → `HolaResponse.error(code, message)`

**AccessControlService (Authz):**
- Purpose: Property-level access enforcement (multi-tenant + role-based)
- Examples: `accessControlService.validatePropertyAccess(propertyId)` throws `HolaException(PROPERTY_ACCESS_DENIED)` if current user lacks access
- Pattern: Retrieves current user from `SecurityContextHolder`; checks `adminUser.getHotels()/getProperties()` against requested resource; used in service layer pre-condition checks

**TenantContext (Isolation):**
- Purpose: ThreadLocal routing for schema-per-tenant PostgreSQL
- Examples: `TenantContext.setTenantId("hotel_001")` → subsequent JPA queries run in `hotel_001` schema
- Pattern: `TenantFilter` on every request sets tenant; `TenantIdentifierResolver` used by Hibernate; finally block clears ThreadLocal

## Entry Points

**Admin Web:**
- Location: `hola-app/src/main/java/com/hola/web/LoginController.java`
- Triggers: Browser GET `/login` → form, POST → session creation
- Responsibilities: Login form rendering, password validation, session initialization, role-based redirect

**REST APIs:**
- Location: `{module}/src/main/java/com/hola/{domain}/controller/{Domain}ApiController.java` (e.g., `HotelApiController`)
- Triggers: POST/GET/PUT/DELETE to `/api/v1/{resource}`
- Responsibilities: Request parsing (@RequestBody, @PathVariable), validation (@Valid), service delegation, response marshalling

**Booking Engine:**
- Location: `hola-reservation/src/main/java/com/hola/reservation/booking/controller/BookingApiController.java`
- Triggers: External POST to `/api/v1/booking/...` with `API-KEY` header (stateless, no tenant context)
- Responsibilities: Book-on-behalf payment processing, OTA channel integration, no admin session required

**HK Mobile Dashboard:**
- Location: `hola-hotel/src/main/java/com/hola/hotel/controller/HkMobileApiController.java`
- Triggers: Mobile POST `/api/v1/properties/{propertyId}/hk-mobile/login` → session creation
- Responsibilities: Housekeeping task assignment, room status updates, attendance tracking (isolation from Admin)

**Thymeleaf Views:**
- Location: `hola-app/src/main/resources/templates/{domain}/{page}.html`
- Triggers: Controller returns `String viewName` (e.g., `"hotel/list"`)
- Responsibilities: Fragment rendering via `layout:fragment`, JavaScript hydration, form submission

## Error Handling

**Strategy:** Centralized exception translation via `@RestControllerAdvice` + `RestExceptionHandler`

**Patterns:**

- **Business Errors** → `throw new HolaException(ErrorCode.HOTEL_NOT_FOUND)` in service → caught by handler → `HolaResponse.error(code, message)` with `success:false`
- **Validation Errors** → `@Valid` + Jakarta constraints on DTOs → `MethodArgumentNotValidException` → handler extracts field errors → `HolaResponse.error(HOLA-0001, field + error message)`
- **Authorization Errors** → `AccessControlService.validatePropertyAccess()` throws `HolaException(PROPERTY_ACCESS_DENIED)` → handler → `HolaResponse.error()` with 403 HTTP status
- **JWT Errors** → `JwtProvider.validateToken()` throws `ExpiredJwtException` / `SignatureException` → `JwtAuthenticationFilter` catches → `401 Unauthorized` JSON response
- **Session Expiry (Admin)** → Spring Security intercepts unauthenticated request → `AuthenticationEntryPoint` → redirect to `/login` with referrer
- **Session Expiry (HK Mobile)** → `HkMobileSessionFilter` detects missing session → `401 Unauthorized` JSON response (mobile app handles redirect)

## Cross-Cutting Concerns

**Logging:** `@Slf4j` on services + controllers; structured logs via `log.info()/warn()/error()` with context (e.g., "호텔 생성: HotelName (HotelCode)")

**Validation:**
- Request layer: Jakarta `@NotBlank`, `@NotNull`, `@Min`, `@Max` on DTO fields
- Service layer: Manual checks for business rules (e.g., duplicate checks, state transitions)
- Database layer: Unique constraints on `htl_hotel.hotel_code`, NOT NULL on required columns

**Authentication:**
- Admin: Spring Security default `UsernamePasswordAuthenticationFilter` → `AuthService.loadUserByUsername()` → session
- API: `JwtAuthenticationFilter` → `JwtProvider.validateToken()` → `SecurityContext`
- Mobile: `HkMobileSessionFilter` → session attributes (`hkUserId`, `hkUserRole`) outside SecurityContext

**Multitenant Access:**
- Header injection: `X-Tenant-ID` passed by client or inferred from `AdminUser.hotelId` (SUPER_ADMIN defaults to `public`)
- Runtime routing: `TenantFilter` sets `TenantContext`; `TenantIdentifierResolver` configures Hibernate
- Row-level security: Each entity query scoped to tenant schema automatically

**Transaction Boundaries:**
- Class-level `@Transactional(readOnly=true)` on all `*ServiceImpl`
- Write methods override with `@Transactional` (no readOnly)
- Exception: `orphanRemoval=true` collections must use `collection.clear() + flush()` not JPQL DELETE

**PII Masking:**
- Server-side: `NameMaskingUtil.maskName()`, `maskPhone()` in service response builders
- Client-side: `HolaPms.maskName()`, `HolaPms.maskPhone()` in JavaScript DataTable renderers (redundant safety)

---

*Architecture analysis: 2026-02-28*
