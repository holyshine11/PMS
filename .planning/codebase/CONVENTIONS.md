# Coding Conventions

**Analysis Date:** 2026-02-28

## Naming Patterns

**Files:**
- Package structure: `com.hola.{module}.{layer}` (entity, repository, service, controller, dto, mapper)
- Entity files: `{DomainName}Entity` (e.g., `Hotel.java`, `Property.java`)
- DTO files:
  - Request: `{Domain}Request.java` or `{Domain}CreateRequest.java`, `{Domain}UpdateRequest.java`
  - Response: `{Domain}Response.java` or `{Domain}ListResponse.java`
- Service interface: `{Domain}Service.java`
- Service implementation: `{Domain}ServiceImpl.java`
- Mapper: `{Domain}Mapper.java` (@Component, manual toEntity/toResponse)
- Repository: `{Domain}Repository.java` (extends JpaRepository)
- Controller (API): `{Domain}ApiController.java` (@RestController, /api/v1/...)
- Controller (View): `{Domain}ViewController.java` (@Controller, Thymeleaf)

**Classes:**
- PascalCase: `HolaException`, `ErrorCode`, `BaseEntity`, `Hotel`, `Property`
- Enum constants: UPPER_SNAKE_CASE (e.g., `RESERVED`, `CHECKED_IN`, `DAY_USE`)

**Variables & Fields:**
- camelCase: `hotelId`, `propertyName`, `masterReservationNo`, `checkInTime`
- Private fields in entities: use Lombok @Getter (no manual getters)
- Constants: UPPER_SNAKE_CASE (e.g., `PROPERTY_ID = 1L`, `CHECK_IN = LocalDate.of(2026, 6, 1)`)

**Database:**
- Table names: snake_case with prefixes: `htl_` (hotel), `rm_` (room), `rt_` (rate), `rsv_` (reservation), `fd_` (front desk), `hk_` (housekeeping)
- Column names: snake_case (e.g., `hotel_id`, `hotel_code`, `property_name`, `check_in_time`)
- Primary key: `id` (auto-generated IDENTITY)
- Foreign key reference columns (cross-module): use `@Column(name="xxx_id")` private Long xxxId instead of @ManyToOne to avoid JPA cross-module coupling

## Code Style

**Formatting:**
- No enforced formatter (ESLint/Prettier for Java not used)
- Encoding: UTF-8 (configured in build.gradle: `tasks.withType(JavaCompile) { options.encoding = 'UTF-8' }`)
- JVM parameter compilation: enabled (`options.compilerArgs << '-parameters'` in build.gradle) to preserve method parameter names for @RequestParam
- Line length: project uses ~120 character lines (observed in code samples)

**Linting:**
- No SonarQube/Checkstyle active
- IDE: VSCode default formatting
- Maven/Gradle: Java 17 target (projects configured in build.gradle with `JavaVersion.VERSION_17`)

## Import Organization

**Order (standard Java/Spring pattern):**
1. Package declaration
2. Blank line
3. Jakarta/Java imports (jakarta.*, java.*)
4. Third-party imports (org.springframework.*, org.hibernate.*, etc.)
5. Blank line
6. Project imports (com.hola.*)

**Example from `Hotel.java`:**
```java
package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
```

**Path Aliases:**
- No explicit aliases used; all imports are full paths
- Modules import from each other using full package paths: `com.hola.{module}.*`

## Error Handling

**Exception Pattern:**
- All business exceptions: `throw new HolaException(ErrorCode.XXX)` or `throw new HolaException(ErrorCode.XXX, "custom message")`
- Location: `com.hola.common.exception.HolaException` (extends RuntimeException, @Getter with errorCode field)

**ErrorCode Enum:**
- Location: `com.hola.common.exception.ErrorCode` (each constant has code String, message String, HttpStatus)
- Code system:
  - `HOLA-0xxx`: Common (0001=Internal Error, 0002=Invalid Input, 0003=Not Found, etc.)
  - `HOLA-06xx`: Member management (0600=Admin Not Found, 0601=Login ID Duplicate)
  - `HOLA-07xx`: Authorization (0700=Role Not Found, 0701=Role Name Duplicate)
  - `HOLA-08xx`: Password (0800=Mismatch, 0802=Invalid Format)
  - `HOLA-1xxx`: Hotel/Property (1000=Hotel Not Found, 1010=Property Not Found)
  - `HOLA-2xxx`: Room (2000=Room Class Not Found, 2010=Room Type Not Found)
  - `HOLA-25xx`: Transaction Code (2500=TC Group Not Found, 2510=TC Not Found)
  - `HOLA-26xx`: Inventory (2600=Inventory Item Not Found, 2610=Not Available)
  - `HOLA-3xxx`: Rate (3000=Rate Code Not Found, 3001=Duplicate)
  - `HOLA-35xx`: Promotion (3500=Promotion Code Not Found)
  - `HOLA-4xxx`: Reservation (4000=Reservation Not Found, 4001=Duplicate, 4020=Payment Already Completed)
  - `HOLA-401x`: Sub Reservation (4010=Sub Reservation Not Found, 4011=Room Conflict)
  - `HOLA-407x`: Booking Engine (4070=Property Not Found, 4071=No Availability)
  - `HOLA-409x`: Booking Auth (4090=API-KEY Required, 4091=Invalid API Key)
  - `HOLA-5xxx`: Front Desk (5001=Room Assign Required, 5002=Room Not Clean)
  - `HOLA-8xxx`: Housekeeping (8000=HK Task Not Found, 8060=Already Clocked In)

**Global Exception Handler:**
- Location: `com.hola.common.exception.GlobalExceptionHandler` (@RestControllerAdvice)
- Catches HolaException and returns HolaResponse.error(code, message) with HttpStatus from ErrorCode

## DTO Patterns

**Request DTOs:**
- Location: `src/main/java/{module}/dto/request/`
- Annotations: @Getter, @NoArgsConstructor, @AllArgsConstructor, @Builder
- Validation: @NotNull, @Min, @Max, @NotBlank with message fields (Korean messages)
- Example: `SubReservationRequest` contains roomTypeId, adults, checkIn, checkOut, guests list, services list
- No @Setter; use builder pattern

**Response DTOs:**
- Location: `src/main/java/{module}/dto/response/`
- Annotations: @Getter, @NoArgsConstructor, @AllArgsConstructor, @Builder
- Fields: match entity fields (id, code, name, createdAt, updatedAt, useYn, etc.)
- No nested entities; use primitive types or IDs
- Example: `HotelResponse` has id, hotelCode, hotelName, phone, email, address, useYn, createdAt, updatedAt
- Builder pattern used for construction

**Mapper Pattern:**
- Location: `com.hola.{module}.mapper.{Domain}Mapper` (@Component)
- No MapStruct; manual toEntity(request, ...) and toResponse(entity) methods
- Purpose: Entity <-> DTO conversion, isolating controller from entity structure
- Example from `HotelMapper`:
  ```java
  public Hotel toEntity(HotelCreateRequest request, String hotelCode) {
      return Hotel.builder().hotelCode(hotelCode)...build();
  }
  public HotelResponse toResponse(Hotel hotel) {
      return HotelResponse.builder().id(hotel.getId())...build();
  }
  ```

## Entity Patterns

**Base Entity:**
- Location: `com.hola.common.entity.BaseEntity` (abstract MappedSuperclass)
- All entities extend BaseEntity
- Inherited fields:
  - `id` (Long, @GeneratedValue IDENTITY, @Id)
  - `createdAt` (LocalDateTime, @CreatedDate, immutable)
  - `updatedAt` (LocalDateTime, @LastModifiedDate)
  - `createdBy` (String, @CreatedBy, immutable)
  - `updatedBy` (String, @LastModifiedBy)
  - `deletedAt` (LocalDateTime, null=active, set on soft delete)
  - `useYn` (Boolean, default true, false on soft delete or deactivate)
  - `sortOrder` (Integer, default 0)
- Methods:
  - `softDelete()`: sets deletedAt=now, useYn=false
  - `isDeleted()`: checks deletedAt != null
  - `activate()`: sets useYn=true
  - `deactivate()`: sets useYn=false
  - `changeSortOrder(Integer)`: updates sortOrder
- Auditing: JPA @EntityListeners(AuditingEntityListener.class), createdBy/updatedBy populated via Principal

**Entity Annotations:**
- `@Entity`, `@Table(name="...")`, `@SQLRestriction("deleted_at IS NULL")` (soft delete auto-filter)
- `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@Builder` (Lombok)
- `@Version` (Long, optional): optimistic locking for concurrent updates (used on MasterReservation, SubReservation, HkDailyAttendance)

**Relationship Patterns:**
- **One-to-Many:** `@OneToMany(mappedBy="parent", fetch=FetchType.LAZY)` with `orphanRemoval=true` if needed
- **Many-to-One (same module):** `@ManyToOne(fetch=FetchType.LAZY)`, `@JoinColumn(name="parent_id", nullable=false)`
- **Cross-module FK:** Use `@Column(name="xxx_id") private Long xxxId` instead of @ManyToOne to avoid circular dependencies
- Fetch strategy: **LAZY by default** (open-in-view: false enforced in application-test.yml, so all loads must be eager in service layer)

**Business Methods in Entity:**
- Encapsulate state changes inside entity
- Example from `Hotel.java`: `update(...)` method for field updates
- Example from `MarketCode`: soft delete handled in service but uses entity's `softDelete()` method

## Service Layer

**Service Interface & Implementation:**
- Location: `src/main/java/{module}/service/`
- Interface: `{Domain}Service.java` (public operations)
- Implementation: `{Domain}ServiceImpl.java` (@Service, @RequiredArgsConstructor, @Transactional)
- Transaction strategy:
  - Class-level: `@Transactional(readOnly=true)` (default for reads)
  - Method-level: `@Transactional` (overrides, for writes: create/update/delete)
  - Example from `MarketCodeServiceImpl`:
    ```java
    @Service
    @Transactional(readOnly=true)
    public class MarketCodeServiceImpl {
        @Transactional
        public MarketCodeResponse createMarketCode(...) { ... }
        public MarketCodeResponse getMarketCode(...) { ... }  // inherits readOnly=true
    }
    ```

**Service Methods:**
- Single responsibility: one domain concern per method
- Validation: check all preconditions first, throw HolaException early
- Null coalescing: use Optional.orElseThrow(() -> new HolaException(...))
- Logging: @Slf4j, log.info() for business events (creation, deletion), log.error() for exceptions
- Return type: Response DTO or List<Response> or void (for side effects)

**Common Pattern:**
```java
public ResponseDto create(CreateRequest request) {
    // 1. Validate/fetch dependent entities
    Entity parent = findById(request.getParentId());

    // 2. Check duplicates/constraints
    if (repository.exists(...)) throw new HolaException(ErrorCode.DUPLICATE);

    // 3. Create entity (use builder)
    Entity entity = Entity.builder().field(request.getField()).build();

    // 4. Apply optional fields
    if (request.getField() != null) entity.update(...);

    // 5. Persist
    Entity saved = repository.save(entity);

    // 6. Log
    log.info("생성됨: {}", saved.getName());

    // 7. Return response
    return mapper.toResponse(saved);
}
```

## Repository Layer

**Repository Interface:**
- Location: `src/main/java/{module}/repository/`
- Extends `JpaRepository<Entity, Long>` (auto-provides save, delete, findById, findAll)
- Custom finder methods: follow Spring Data naming convention
- Soft delete: explicitly query `DeletedAtIsNull` (e.g., `findByIdAndDeletedAtIsNull(Long id)`)
- Example from `AdminUserRepository`:
  ```java
  Optional<AdminUser> findByLoginIdAndDeletedAtIsNull(String loginId);
  boolean existsByLoginIdAndDeletedAtIsNull(String loginId);
  List<AdminUser> findByHotelIdAndAccountTypeAndDeletedAtIsNullOrderByCreatedAtDesc(...);
  ```
- @SQLRestriction on entity auto-filters soft-deleted records in most queries
- Native queries: use `@Query(value="...", nativeQuery=true)` sparingly (e.g., sequence retrieval)

**Bulk Delete Pattern:**
- **FORBIDDEN:** `repository.deleteAllByXxx()` (will not trigger orphanRemoval or soft delete logic)
- **REQUIRED:** Use `@Modifying @Query("UPDATE Entity SET deletedAt=NOW(), useYn=false WHERE ...")` for soft delete
- For orphaned collections: `collection.clear()` + `flush()`

## Controller Patterns

**API Controller:**
- Location: `src/main/java/{module}/controller/{Domain}ApiController`
- Annotation: `@RestController`, `@RequestMapping("/api/v1/...")`
- Tag: `@Tag(name="...", description="...")` for Swagger/OpenAPI
- Methods: `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping`
- Request body validation: `@Valid @RequestBody CreateRequest`
- Response: Always `ResponseEntity<HolaResponse<T>>` or `HolaResponse<T>`
- Example from `AuthApiController`:
  ```java
  @PostMapping("/login")
  public ResponseEntity<HolaResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
      LoginResponse response = authService.login(request);
      return ResponseEntity.ok(HolaResponse.success(response));
  }
  ```

**Response DTO:**
- All endpoints return: `HolaResponse.success(data)` (code: HOLA-0000), `HolaResponse.success(data, pageInfo)` (paginated), or `HolaResponse.error(code, message)` (errors via GlobalExceptionHandler)
- Paginated responses: wrap List in PageInfo using `PageInfo.from(page)`

**View Controller:**
- Location: `src/main/java/{module}/controller/{Domain}ViewController`
- Annotation: `@Controller`, `@RequestMapping("/..."`
- Return type: String (Thymeleaf template name)
- Data: `model.addAttribute(key, value)`
- Pattern: list view, detail view, form view (GET for show, POST for save)

## Frontend JavaScript Conventions

**File Naming:**
- Pattern: `{domain}-page.js` (list/table pages), `{domain}-form.js` (form/detail pages)
- Examples: `hotel-page.js`, `property-form.js`, `dashboard-page.js`, `hk-mobile-tasks.js`

**Module Pattern (Object Namespace):**
- Encapsulate all code in a single object/namespace to avoid global scope pollution
- Example from `dashboard-page.js`:
  ```javascript
  var Dashboard = {
      pickupChart: null,
      isSuperAdmin: false,
      MAX_RECENT: 5,

      init: function() { ... },
      bindEvents: function() { ... },
      reload: function() { ... }
  };
  ```
- Initialize on document ready: `$(document).ready(function() { Dashboard.init(); });`

**AJAX Pattern:**
- Use `HolaPms.ajax(options)` wrapper (auto JSON serialization, error handling)
- Options: `url`, `type`, `data` (object, auto-stringified), `success`, `error`
- Example:
  ```javascript
  HolaPms.ajax({
      url: '/api/v1/properties/' + propertyId,
      type: 'GET',
      success: function(response) { ... },
      error: function(xhr) { HolaPms.handleAjaxError(xhr); }
  });
  ```

**Alert/Modal Handling:**
- Alerts: `HolaPms.alert(type, message)` (type: success, error, warning, info) — auto-hides after 1.5s (success) or 3s (error)
- Redirects: `HolaPms.alertAndRedirect(type, message, url)` — flash alert via sessionStorage
- Modals: `HolaPms.modal.show(selector)`, `HolaPms.modal.hide(selector)` (reuses Bootstrap Modal instance)

**PII Masking:**
- Names in tables: `HolaPms.maskName(name)` — (1자: 그대로, 2자: 김*, 3자: 김*수, 4자+: 김**수)
- Phones in tables: `HolaPms.maskPhone(phone)` — (010-1234-5678 → 010-****-5678)
- **Rule:** All list/table pages MUST mask customer names and phone numbers (no exceptions, enforced policy since feedback-pii-masking.md)

**DataTable Conventions:**
- Use `$.extend({}, HolaPms.dataTableDefaults, {...})` for configuration
- Never define individual `language` object; defaults include Korean language pack
- Column definitions use `HolaPms.renders.*` helpers (dashIfEmpty, useYnBadge, countBadge, actionButtons)
- Page size select: 10/20(default)/50/100 in Korean (predefined in HolaPms.dataTableDefaults)

**Property Context Pattern (CRITICAL):**
- All property-dependent pages MUST follow strict initialization pattern (3 bugs traced to this):
  1. HTML: Include alert div: `<div id="contextAlert" class="alert alert-danger d-none">...</div>`
  2. JS: **Always call `init()` unconditionally** (NOT conditionally on propertyId check)
  3. JS: Inside `init()`, call `bindEvents()` then `reload()`
  4. JS: In `reload()`, check `HolaPms.context.getPropertyId()` — if null, show alert; else load data
  5. JS: Register event listener in `bindEvents()`: `$(document).on('hola:contextChange', function() { self.reload(); })`
  - **DO NOT:** Check propertyId before calling init() — this causes bindEvents() to skip and contextChange listener to never register
  - Examples: `early-late-policy-page.js`, `reservation-detail.js`, dashboard property branches

**Context Management:**
- Global: `HolaPms.context.getHotelId()`, `HolaPms.context.getPropertyId()` (sessionStorage-backed)
- Event: `hola:contextChange` fires when user selects hotel/property in header
- Header selector stores context, triggers event for all pages to reload

## CSS Conventions

**Design System:**
- Location: `hola-pms/hola-app/src/main/resources/static/css/hola.css`
- Color palette (CSS variables):
  - `--bs-primary`: #0582CA (bright blue, buttons, links)
  - `--bs-danger`: #EF476F (pink-red, errors, destructive actions)
  - `--bs-dark`: #051923 (navy, sidebar)
  - `--hola-header-bg`: #003554 (darker blue, header)
  - `--hola-text-primary`: #212529 (text)
  - `--hola-text-secondary`: #6c757d (secondary text)
  - `--hola-border`: #e9ecef (borders)
  - `--hola-shadow-sm`, `-md`, `-lg`: depth shadows

**Bootstrap Customizations:**
- Overrides in :root CSS variables
- Font: Pretendard (Google CDN + fallbacks)
- Button sizing: .btn-sm for compact UI (0.75rem font, 4px 10px padding)
- Card default: `card border-0 shadow-sm` (no border, subtle shadow)

**Typography Rules:**
- **fw-bold (font-weight-bold):** Allowed ONLY on h4 (page title), h6 (section headers). Forbidden on labels, data spans, modal titles
- Form labels: use Bootstrap .col-form-label without fw-bold
- DataTable pagination: info on left (0 margin), pagination centered

**Responsive Design:**
- Bootstrap 5.3 grid (col-sm-2, col-lg-3, etc.)
- Mobile-first: base styles then @media min-width breakpoints
- Popup mode: HTML param `?mode=popup` → body gets `.popup-mode` class for modal-only display

## Comments

**When to Comment:**
- Class/method header: JSDoc-style comments for public APIs
- Complex logic: explain "why" not "what" (e.g., "Soft delete required for audit trail, not physical removal")
- Business rules: document constraints (e.g., "Max 5 recent properties to prevent storage bloat")
- Language: Korean comments preferred (per CLAUDE.md), English for commit messages

**JSDoc/Documentation:**
- Location: class-level /** */ blocks with @param, @return, @deprecated as needed
- Example from error handler: `/** 비즈니스 예외 */` for HolaException class
- Service methods: brief Javadoc for public operations

**Inline Comments:**
- Use sparingly; prefer clear variable/method names
- Explain non-obvious algorithm steps
- Link to JIRA/issue numbers if applicable

## Version Control & Build

**Git Commit Messages:**
- Format: `[HOLA-XXX] feat/fix/refactor: description` (English, issue key optional)
- Examples: `[HOLA-100] feat: add dayuse checkout`, `fix: soft delete on cascade`, `refactor: extract price calculator`

**Build Configuration:**
- Build tool: Gradle (root build.gradle + subproject build.gradle files)
- Java version: Java 17 (sourceCompatibility = VERSION_17, targetCompatibility = VERSION_17)
- Encoding: UTF-8 (enforced in build.gradle)
- Parameter preservation: enabled for @RequestParam binding (options.compilerArgs << '-parameters')
- Test runner: JUnit 5 (useJUnitPlatform() in test task)
- Lombok: compileOnly, testCompileOnly with annotationProcessor

## Code Quality Auto-Checks (Pre-Commit)

Before committing, verify:
1. **Service layer pattern:** All domain methods in @Transactional(readOnly=true) class + method-level @Transactional on writes
2. **Type hints:** All public method parameters + returns typed (no Object, use specific DTO/Entity)
3. **No hardcoded env vars:** Check for passwords, API keys, URLs not in application.yml
4. **Soft delete usage:** All delete endpoints call softDelete(), not physical delete()
5. **DTO in/out:** Controllers accept/return DTO, not Entity (except internal service calls)
6. **PII in logs:** Never log names/phones directly; use masking utilities if needed

---

*Convention analysis: 2026-02-28*
