# Coding Conventions

**Analysis Date:** 2026-03-26

## Naming Patterns

**Java Classes:**
- Entity: `PascalCase` domain noun (e.g., `Hotel`, `MasterReservation`, `SubReservation`, `DailyCharge`)
- DTO Request: `{Entity}CreateRequest`, `{Entity}UpdateRequest`, `{Entity}StatusRequest` in `dto/request/`
- DTO Response: `{Entity}Response`, `{Entity}ListResponse`, `{Entity}DetailResponse` in `dto/response/`
- Service Interface: `{Entity}Service`
- Service Impl: `{Entity}ServiceImpl`
- Controller (API): `{Entity}ApiController` (`@RestController`)
- Controller (View): `{Entity}ViewController` (`@Controller`, Thymeleaf)
- Mapper: `{Entity}Mapper` (`@Component`, manual mapping)
- Repository: `{Entity}Repository` (`JpaRepository<Entity, Long>`)
- Housekeeping: `Hk*` prefix (e.g., `HkTask`, `HkTaskMapper`, `HkMobileApiController`)

**Java Variables/Methods:**
- `camelCase` for variables and methods
- `UPPER_SNAKE_CASE` for constants (e.g., `PROPERTY_ID`, `SUCCESS_CODE`)
- Boolean fields: `useYn`, `isOtaManaged`, `isDeleted()` (is/has prefix)
- ID references: `xxxId` (e.g., `propertyId`, `rateCodeId`, `hotelId`)

**Database:**
- Tables: `snake_case` with module prefix
  - `htl_` (hotel): `htl_hotel`, `htl_property`, `htl_floor`, `htl_room_number`, `htl_room_unavailable`
  - `rm_` (room): `rm_room_class`, `rm_room_type`, `rm_transaction_code`, `rm_transaction_code_group`
  - `rt_` (rate): `rt_rate_code`, `rt_day_use_rate`
  - `rsv_` (reservation): `rsv_master_reservation`, `rsv_sub_reservation`, `rsv_reservation_payment`
  - `fd_` (front desk): front desk tables
  - `hk_` (housekeeping): `hk_task`, `hk_staff`
- Columns: `snake_case` (e.g., `hotel_code`, `created_at`, `use_yn`, `deleted_at`)
- Sequences: `{table_name}_code_seq` (e.g., `htl_hotel_code_seq`) -> format `HTL00001`

**JavaScript Files:**
- `{domain}-page.js` for list pages (e.g., `hotel-admin-page.js`, `rate-code-page.js`)
- `{domain}-form.js` for form/detail pages (e.g., `hotel-form.js`, `room-class-form.js`)
- Special: `hola-common.js` (shared utilities), `booking.js` (booking engine)
- HK mobile: `hk-mobile-*.js` (e.g., `hk-mobile-tasks.js`)
- JS object name: `PascalCase` singleton matching file (e.g., `HotelForm`, `RateCodePage`)

**Thymeleaf Templates:**
- `{domain}/list.html`, `{domain}/form.html` pattern
- Layout: `layout/default.html` (Admin), `layout/booking.html` (Booking), `layout/mobile.html` (HK)
- Fragment slots: `layout:fragment="content"`, `layout:fragment="scripts"`

## Code Style

**Formatting:**
- No ESLint/Prettier for JavaScript
- Java: no explicit Checkstyle/SonarQube; standard Spring Boot conventions
- Encoding: UTF-8 (configured in `build.gradle`: `options.encoding = 'UTF-8'`)
- Parameter names preserved: `options.compilerArgs << '-parameters'` for `@RequestParam` binding
- Line length: ~120 characters observed

**Lombok Annotations (use consistently):**
- Entity: `@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder`
- DTO: `@Getter @NoArgsConstructor @AllArgsConstructor @Builder`
- Service: `@Slf4j @Service @RequiredArgsConstructor`
- Controller: `@RestController @RequiredArgsConstructor` (API) / `@Controller @RequiredArgsConstructor` (View)

## Import Organization

**Order (observed pattern):**
1. Project imports (`com.hola.*`)
2. Third-party libraries (`com.fasterxml.*`, `io.swagger.*`)
3. `jakarta.*`
4. `lombok.*`
5. `org.springframework.*`, `org.hibernate.*`
6. `java.*`

**No path aliases.** All imports use fully qualified package paths.

## API Response Pattern

**Standard response: `HolaResponse<T>`** - `hola-pms/hola-common/src/main/java/com/hola/common/dto/HolaResponse.java`

```java
// Success with data
return ResponseEntity.ok(HolaResponse.success(data));

// Success with pagination
return ResponseEntity.ok(HolaResponse.success(page.getContent(), PageInfo.from(page)));

// Success void
return ResponseEntity.ok(HolaResponse.success());

// Created (201)
return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));

// Error (auto via GlobalExceptionHandler)
throw new HolaException(ErrorCode.RESERVATION_NOT_FOUND);
```

**Response JSON shape:**
```json
{
  "success": true,
  "code": "HOLA-0000",
  "message": "성공",
  "data": { ... },
  "pagination": { "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 },
  "timestamp": "2026-03-26T10:00:00"
}
```

**Booking engine exception:** Uses `BookingResponse` with `$.result.RESULT_YN` / `$.result.data` format (different from standard `HolaResponse`).

## Error Handling

**Exception hierarchy:**
- `HolaException` - `hola-pms/hola-common/src/main/java/com/hola/common/exception/HolaException.java` (extends `RuntimeException`, carries `ErrorCode`)
- `GlobalExceptionHandler` - `hola-pms/hola-common/src/main/java/com/hola/common/exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`)

**ErrorCode enum** - `hola-pms/hola-common/src/main/java/com/hola/common/exception/ErrorCode.java`:
- Format: `HOLA-XXXX` (4-digit numeric)
- Each entry: `(code, message, httpStatus)` triple
- Code ranges:
  - `HOLA-0xxx`: Common (auth, validation, generic errors)
  - `HOLA-06xx`: Member management
  - `HOLA-07xx`: Role management
  - `HOLA-08xx`: Password
  - `HOLA-1xxx`: Hotel/Property (1000=hotel, 1010=property, 1020=floor, 1030=room number, 1040=market code)
  - `HOLA-2xxx`: Room
  - `HOLA-25xx`: Transaction codes
  - `HOLA-26xx`: Inventory
  - `HOLA-3xxx`: Rate
  - `HOLA-4xxx`: Reservation/Booking/Payment
  - `HOLA-5xxx`: Front Desk
  - `HOLA-8xxx`: Housekeeping

**Throwing pattern:**
```java
// Standard
throw new HolaException(ErrorCode.RESERVATION_NOT_FOUND);

// With custom message
throw new HolaException(ErrorCode.INVALID_INPUT, "커스텀 메시지");
```

**GlobalExceptionHandler mappings:**
| Exception | HTTP Status | Response Code |
|-----------|------------|---------------|
| `HolaException` | From `ErrorCode.httpStatus` | `ErrorCode.code` |
| `MethodArgumentNotValidException` | 400 | `HOLA-0002` (field errors joined) |
| `ObjectOptimisticLockingFailureException` | 409 | `HOLA-4027` |
| `DataIntegrityViolationException` | 409 | `HOLA-0004` |
| `MethodArgumentTypeMismatchException` | 400 | `HOLA-0002` |
| `NoResourceFoundException` | 404 | `HOLA-0004` |
| `Exception` (catch-all) | 500 | `HOLA-0001` |

## Entity Patterns

**BaseEntity** - `hola-pms/hola-common/src/main/java/com/hola/common/entity/BaseEntity.java`:

All entities MUST extend `BaseEntity`. Provides:
- `id` (Long, `@GeneratedValue(IDENTITY)`)
- `createdAt`, `updatedAt` (JPA Auditing `@CreatedDate`, `@LastModifiedDate`)
- `createdBy`, `updatedBy` (JPA Auditing `@CreatedBy`, `@LastModifiedBy`)
- `deletedAt` (soft delete timestamp, null = active)
- `useYn` (Boolean, default `true`)
- `sortOrder` (Integer, default `0`)
- Methods: `softDelete()`, `isDeleted()`, `activate()`, `deactivate()`, `changeSortOrder()`

**Entity annotation pattern:**
```java
@Entity
@Table(name = "htl_hotel")
@SQLRestriction("deleted_at IS NULL")   // ALWAYS add - auto-filters soft-deleted records
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hotel extends BaseEntity {
    @Column(name = "hotel_code", nullable = false, unique = true, length = 20)
    private String hotelCode;
    // ...
}
```

**Physical delete is FORBIDDEN.** Always use `entity.softDelete()`.

**Cross-module FK rule:**
```java
// CORRECT: ID reference only (no JPA relationship across modules)
@Column(name = "rate_code_id")
private Long rateCodeId;

// WRONG: @ManyToOne across module boundary (creates coupling)
// @ManyToOne private RateCode rateCode;  // DO NOT DO THIS
```

**Optimistic locking** for concurrent entities:
```java
@Version
private Long version;  // Used on MasterReservation, SubReservation, HkDailyAttendance
```

**Within-module relationships:**
```java
// Parent side
@OneToMany(mappedBy = "masterReservation", cascade = CascadeType.ALL, orphanRemoval = true)
private List<SubReservation> subReservations = new ArrayList<>();

// Child side
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "master_reservation_id", nullable = false)
private MasterReservation masterReservation;
```

**JPA critical settings:**
- `open-in-view: false` - lazy loading fails in views; must fetch in service layer
- `default_batch_fetch_size: 100` - IN clause batching for associations

## Mapper Pattern

**Manual mapping** with `@Component` (no MapStruct). Example: `hola-pms/hola-hotel/src/main/java/com/hola/hotel/mapper/HotelMapper.java`

```java
@Component
public class HotelMapper {
    // Request DTO -> Entity
    public Hotel toEntity(HotelCreateRequest request, String hotelCode) {
        return Hotel.builder()
                .hotelCode(hotelCode)
                .hotelName(request.getHotelName())
                // ... field-by-field
                .build();
    }

    // Entity -> Response DTO
    public HotelResponse toResponse(Hotel hotel) {
        return HotelResponse.builder()
                .id(hotel.getId())
                .hotelCode(hotel.getHotelCode())
                // ... field-by-field
                .build();
    }
}
```

**Conventions:**
- One mapper per module handles multiple entity types (e.g., `HotelMapper` handles Hotel, Property, Floor, RoomNumber, MarketCode, CancellationFee, etc.)
- Method names: `toEntity()`, `toResponse()`, `to{Specific}Response()` (e.g., `toReservationListResponse()`, `toPaymentSummaryResponse()`)
- Injected into ServiceImpl via constructor (`@RequiredArgsConstructor`)

## Service Layer Pattern

**Interface + Impl separation.** Example: `hola-pms/hola-hotel/src/main/java/com/hola/hotel/service/HotelServiceImpl.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Class-level: read-only by default
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Override
    public HotelResponse getHotel(Long id) {              // Inherits readOnly=true
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.HOTEL_NOT_FOUND));
        return hotelMapper.toResponse(hotel);
    }

    @Override
    @Transactional  // Override for write operations
    public HotelResponse createHotel(HotelCreateRequest request) {
        // 1. Validate
        if (hotelRepository.existsByHotelNameAndDeletedAtIsNull(request.getHotelName()))
            throw new HolaException(ErrorCode.HOTEL_NAME_DUPLICATE);
        // 2. Create & save
        Hotel hotel = hotelMapper.toEntity(request, generatedCode);
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }
}
```

**JPQL null parameter workaround** (critical trap):
```java
// WRONG: causes bytea casting error in PostgreSQL
// "WHERE (:name IS NULL OR h.hotelName LIKE %:name%)"

// CORRECT: Java conditional branching
boolean hasName = hotelName != null && !hotelName.isBlank();
if (hasName) {
    hotels = repository.findByNameContaining(hotelName, pageable);
} else {
    hotels = repository.findAll(pageable);
}
```

## Controller Pattern

**API Controller** (`@RestController`):
```java
@Tag(name = "프로퍼티 관리", description = "프로퍼티 CRUD API")
@RestController
@RequiredArgsConstructor
public class PropertyApiController {

    @Operation(summary = "프로퍼티 목록 조회")
    @GetMapping("/api/v1/hotels/{hotelId}/properties")
    public ResponseEntity<HolaResponse<List<PropertyResponse>>> getProperties(
            @PathVariable Long hotelId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PropertyResponse> page = propertyService.getProperties(hotelId, pageable);
        return ResponseEntity.ok(HolaResponse.success(page.getContent(), PageInfo.from(page)));
    }
}
```

**URL patterns:**
- Admin API: `/api/v1/{resource}` or `/api/v1/properties/{propertyId}/{resource}`
- Booking API: `/api/v1/booking/{resource}` (public, API-KEY auth)
- HK Mobile API: `/api/v1/properties/{propertyId}/hk-mobile/{resource}` (session auth)
- Swagger: `@Tag` on class, `@Operation` on methods

**View Controller** (`@Controller`):
```java
@Controller
@RequiredArgsConstructor
public class HotelViewController {
    @GetMapping("/admin/hotels")
    public String list() { return "hotel/list"; }
}
```

## DTO Pattern

**Request DTOs** - use Jakarta Validation with Korean messages:
```java
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationCreateRequest {
    @NotNull(message = "체크인 날짜는 필수입니다.")
    private LocalDate masterCheckIn;

    @NotBlank(message = "투숙객 이름은 필수입니다.")
    private String guestNameKo;

    @NotEmpty(message = "객실 레그는 최소 1개 이상 필요합니다.")
    @Valid
    private List<SubReservationRequest> subReservations;
}
```

**Response DTOs** - `@Builder`, no validation:
```java
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationListResponse {
    private Long id;
    private String masterReservationNo;
    private String confirmationNo;
    // ... flat fields only, no nested entities
}
```

## Repository Pattern

**Convention:**
```java
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    // Spring Data query methods
    Optional<Hotel> findByHotelCodeAndDeletedAtIsNull(String code);
    boolean existsByHotelNameAndDeletedAtIsNull(String name);
    Page<Hotel> findAllByHotelNameContaining(String name, Pageable pageable);

    // Native query for sequence
    @Query(value = "SELECT nextval('htl_hotel_code_seq')", nativeQuery = true)
    Long getNextHotelCodeSequence();
}
```

**Bulk delete trap:**
- FORBIDDEN: `repository.deleteAllByXxx()` (bypasses soft delete)
- REQUIRED: `@Modifying @Query("DELETE FROM ...")` for join table cleanup
- For orphaned collections: `collection.clear()` + `flush()`

## Frontend Patterns (HolaPms Namespace)

**`HolaPms` global object** - `hola-pms/hola-app/src/main/resources/static/js/hola-common.js`:

```javascript
// AJAX wrapper (always use this, not raw $.ajax)
HolaPms.ajax({
    url: '/api/v1/hotels/' + hotelId,
    type: 'GET',
    success: function(res) { /* res.data */ }
});

// Toast notifications
HolaPms.alert('success', '저장되었습니다.');    // auto-hide 1.5s
HolaPms.alert('error', '오류가 발생했습니다.'); // auto-hide 3s

// Redirect with flash
HolaPms.alertAndRedirect('/admin/hotels', 'success', '호텔이 생성되었습니다.');

// Context (hotel/property selector)
var propertyId = HolaPms.context.getPropertyId();
HolaPms.requireContext('property');

// PII masking (REQUIRED on list pages)
HolaPms.maskName('홍길동');       // 홍*동
HolaPms.maskPhone('01012345678'); // 010****5678

// DataTable defaults (ALWAYS extend, never define language separately)
$.extend({}, HolaPms.dataTableDefaults, {
    ajax: { url: '/api/v1/...' },
    columns: [...]
});

// Modal
HolaPms.modal.show('#myModal');
HolaPms.modal.hide('#myModal');
```

**JS page object pattern:**
```javascript
const HotelForm = {
    isEdit: false,
    hotelId: null,

    init: function() {
        this.hotelId = $('#hotelId').val() || null;
        this.isEdit = !!this.hotelId;
        this.bindEvents();
        this.load();
    },
    bindEvents: function() { /* event bindings */ },
    load: function() { /* data loading */ }
};

$(document).ready(function() {
    HotelForm.init();
});
```

**Property context pattern (CRITICAL - 3 bugs traced to violations):**
1. HTML: `<div id="contextAlert" class="alert alert-danger d-none">` must exist
2. `init()` ALWAYS runs `bindEvents()` + `reload()` unconditionally
3. `reload()` checks `HolaPms.context.getPropertyId()`, shows alert if missing
4. Listen `hola:contextChange` event -> `self.reload()`
5. NEVER conditionally call `init()` based on propertyId (causes `bindEvents()` skip -> listener never registered)

## UI Style Rules

**Card:** `card border-0 shadow-sm`
**Form:** Bootstrap grid (`row mb-3` + `col-sm-2 col-form-label`). `table table-bordered` FORBIDDEN for forms.
**Buttons:** `d-flex justify-content-between` - left: delete (edit only), right: cancel (`fa-arrow-left`) + save
**fw-bold:** Page title (`h4`), section header (`h6`) ONLY. FORBIDDEN on form labels/data.
**Colors:** #051923, #003554, #0582CA, #EF476F, #000/#FFF + gray. Font: Pretendard.
**Popup mode:** `?mode=popup` -> `body.popup-mode` class

## Logging

**Framework:** SLF4J via Lombok `@Slf4j`

```java
log.warn("비즈니스 예외: {} - {}", e.getErrorCode().getCode(), e.getMessage());
log.error("서버 오류: ", e);  // include stack trace
log.info("호텔 생성됨: {}", hotel.getHotelCode());
log.debug("정적 리소스 없음: {}", e.getResourcePath());
```

## Comments

**Language:** Korean for code comments, English for commit messages.

**Class-level:** brief Javadoc (`/** 호텔 마스터 엔티티 */`)
**Service interface:** Javadoc for each public method
**Section separators:**
```java
// ========== 예약 리스트 조회 ==========
```

## Git Conventions

**Commit:** `[HOLA-XXX] feat/fix/refactor: description` (English)
**Flyway:** `V{major}_{minor}_{patch}__{desc}.sql` at `hola-pms/hola-app/src/main/resources/db/migration/`
- Version bands: V1(Hotel) V2(Room) V3(Rate) V4(Reservation) V5(Test data) V6(Transaction) V7(Room status) V8(Housekeeping)

## Security Patterns

**4-tier SecurityFilterChain:**

| Order | Target | Auth |
|-------|--------|------|
| 0 | `/api/v1/booking/**` | `BookingApiKeyFilter` (API-KEY header, stateless) |
| 1 | `/api/v1/properties/*/hk-mobile/**` | Session-based (`IF_REQUIRED`), `sessionFixation().none()` |
| 2 | `/api/**` | JWT (`SessionCreationPolicy.NEVER`) |
| 3 | `/**` | Session-based form login |

**Authorization check in service:** `accessControlService.validatePropertyAccess(propertyId)`
**Roles:** `SUPER_ADMIN`, `HOTEL_ADMIN`, `PROPERTY_ADMIN`, `HOUSEKEEPING_SUPERVISOR`, `HOUSEKEEPER`

---

*Convention analysis: 2026-03-26*
