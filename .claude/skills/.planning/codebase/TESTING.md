# Testing Patterns

**Analysis Date:** 2026-03-26

## Test Framework

**Runner:**
- JUnit 5 (Jupiter)
- Config: `tasks.named('test') { useJUnitPlatform() }` in `build.gradle`

**Assertion Library:**
- AssertJ (`import static org.assertj.core.api.Assertions.*`)
- Hamcrest for MockMvc JSON path assertions

**Mocking:**
- Mockito via `spring-boot-starter-test`
- `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`

**Run Commands:**
```bash
./gradlew test                                                    # Run all tests
./gradlew :hola-hotel:test                                        # Single module
./gradlew :hola-hotel:test --tests "com.hola.hotel.service.HotelServiceImplTest"  # Single class
./gradlew :hola-app:test                                          # Integration tests
./gradlew clean test                                              # Clean build + test
```

## Test File Organization

**Location:** Unit tests co-located in each module; integration tests in `hola-app`.

**Naming:**
- Unit test: `{Subject}Test.java` (e.g., `HotelServiceImplTest.java`, `ReservationMapperTest.java`)
- Integration test: `{Subject}IntegrationTest.java` (e.g., `BookingApiIntegrationTest.java`)

**Directory Structure:**
```
hola-common/src/test/java/com/hola/common/
├── dto/HolaResponseTest.java
├── exception/ErrorCodeTest.java
├── security/AccessControlServiceTest.java
└── util/NameMaskingUtilTest.java

hola-hotel/src/test/java/com/hola/hotel/
└── service/
    ├── HotelServiceImplTest.java
    └── PropertyServiceImplTest.java

hola-room/src/test/java/com/hola/room/
└── service/
    └── RoomTypeServiceImplTest.java

hola-reservation/src/test/java/com/hola/reservation/
├── booking/service/
│   ├── BookingServiceImplTest.java
│   └── CancellationPolicyServiceImplTest.java
├── entity/
│   └── ReservationPaymentTest.java
├── mapper/
│   └── ReservationMapperTest.java
└── service/
    ├── EarlyLateCheckServiceTest.java
    ├── PriceCalculationServiceTest.java
    ├── ReservationNumberGeneratorTest.java
    ├── ReservationPaymentServiceImplTest.java
    ├── ReservationServiceImplTest.java
    └── RoomAvailabilityServiceTest.java

hola-app/src/test/java/com/hola/
├── config/TestContainersConfig.java
├── fixture/TestFixtures.java
├── integration/
│   ├── booking/BookingApiIntegrationTest.java
│   ├── payment/PaymentApiIntegrationTest.java
│   ├── reservation/ReservationApiIntegrationTest.java
│   └── security/SecurityIntegrationTest.java
└── support/BaseIntegrationTest.java
```

## Test Counts by Module

**Total: 21 test files, ~256 test methods**

| Module | Test Files | Test Methods | Focus |
|--------|-----------|-------------|-------|
| `hola-common` | 4 | 20 | HolaResponse, ErrorCode integrity, AccessControl, NameMasking |
| `hola-hotel` | 2 | 15 | HotelService CRUD, PropertyService CRUD |
| `hola-room` | 1 | 5 | RoomTypeService |
| `hola-reservation` | 8 | 159 | ReservationService (41), BookingService (26), Payment (20), Pricing (17), CancelPolicy (14), EarlyLate (13), Availability (10), Mapper (6), PaymentEntity (9), NumberGen (8) |
| `hola-app` (integration) | 4 | 52 | ReservationApi (22), BookingApi (14), Security (10), PaymentApi (6) |
| **Total** | **21** | **~256** | |

## Unit Test Structure

**Standard pattern:** `@ExtendWith(MockitoExtension.class)` + `@Nested` + `@DisplayName`

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("HotelServiceImpl")
class HotelServiceImplTest {

    @InjectMocks
    private HotelServiceImpl hotelService;

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private HotelMapper hotelMapper;

    // Helper factory methods at class level
    private Hotel createHotel(String code, String name) {
        return Hotel.builder().hotelCode(code).hotelName(name).build();
    }

    @Nested
    @DisplayName("호텔 생성")
    class CreateHotel {

        @Test
        @DisplayName("자동 코드 생성 (HTL00001 형식)")
        void createHotel_autoCodeGeneration() {
            // given
            when(hotelRepository.existsByHotelNameAndDeletedAtIsNull("테스트 호텔"))
                .thenReturn(false);
            when(hotelRepository.getNextHotelCodeSequence()).thenReturn(1L);

            // when
            HotelResponse response = hotelService.createHotel(request);

            // then
            assertThat(response.getHotelCode()).isEqualTo("HTL00001");
            verify(hotelRepository).getNextHotelCodeSequence();
        }

        @Test
        @DisplayName("이름 중복 시 HOTEL_NAME_DUPLICATE")
        void createHotel_duplicateName_throws() {
            when(hotelRepository.existsByHotelNameAndDeletedAtIsNull("기존 호텔"))
                .thenReturn(true);

            assertThatThrownBy(() -> hotelService.createHotel(request))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.HOTEL_NAME_DUPLICATE);
        }
    }
}
```

**Key conventions:**
- `@DisplayName` on class AND every `@Test` method (Korean descriptions)
- `@Nested` inner classes group related test scenarios
- Helper/factory methods at bottom of test class (private)
- Constants for shared test IDs: `private static final Long PROPERTY_ID = 1L;`
- `@BeforeEach` for common setup (entity builders)

## Integration Test Structure

**Base class:** `hola-pms/hola-app/src/test/java/com/hola/support/BaseIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Transactional
@WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
}
```

**Two integration test approaches observed:**

**1. MockBean approach** (Reservation/Payment API tests) - mocks service layer:
```java
@DisplayName("예약 API 통합 테스트")
class ReservationApiIntegrationTest extends BaseIntegrationTest {

    @Autowired private ObjectMapper objectMapper;
    @MockBean private AccessControlService accessControlService;
    @MockBean private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        AdminUser mockAdmin = AdminUser.builder()
                .loginId("admin").role("SUPER_ADMIN").userName("관리자").password("encoded")
                .build();
        when(accessControlService.getCurrentUser()).thenReturn(mockAdmin);
        doNothing().when(accessControlService).validatePropertyAccess(anyLong());
    }

    @Nested
    @DisplayName("예약 등록 (POST /reservations)")
    class Create {
        @Test
        @DisplayName("유효한 요청으로 예약 등록 시 201 CREATED 반환")
        void create_validRequest_returns201() throws Exception {
            when(reservationService.create(eq(PROPERTY_ID), any()))
                    .thenReturn(/* response builder */);

            mockMvc.perform(post(BASE_URL, PROPERTY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reservationStatus").value("RESERVED"));
        }
    }
}
```

**2. Full-stack approach** (Booking API test) - uses real Flyway test data:
```java
@DisplayName("부킹엔진 API 통합 테스트")
class BookingApiIntegrationTest extends BaseIntegrationTest {

    @Autowired private PropertyRepository propertyRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @Test
    @DisplayName("존재하는 프로퍼티 코드로 조회 시 200 반환")
    void getPropertyInfo_existingCode_200() throws Exception {
        mockMvc.perform(get("/api/v1/booking/properties/{code}", "GMP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data.propertyCode").value("GMP"));
    }
}
```

**Security testing pattern:**
```java
@Test
@WithAnonymousUser  // Override default @WithMockUser
@DisplayName("미인증 사용자가 예약 API 접근 시 4xx 반환")
void unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/properties/1/reservations"))
            .andExpect(status().is4xxClientError());
}

@Test
@WithMockUser(username = "propadmin", roles = {"PROPERTY_ADMIN"})
@DisplayName("PROPERTY_ADMIN 역할로 접근 허용")
void propertyAdmin_allowed() throws Exception {
    mockMvc.perform(get("/api/v1/properties/1/reservations"))
            .andExpect(notAuthError());  // custom matcher: not 401/403
}
```

## Mocking Patterns

**Unit tests: `@Mock` + `@InjectMocks`**
```java
@Mock private MasterReservationRepository masterReservationRepository;
@Mock private ReservationMapper reservationMapper;
@InjectMocks private ReservationServiceImpl reservationService;
```

**Lenient stubs for optional setup:**
```java
@BeforeEach
void setUp() {
    // NPE 방지 - 모든 테스트에서 호출되지 않을 수 있음
    lenient().when(roomUnavailableRepository.findOverlapping(any(), any(), any()))
            .thenReturn(Collections.emptyList());
}
```

**Integration tests: `@MockBean` for auth bypass**
```java
@MockBean private AccessControlService accessControlService;
@MockBean private ReservationService reservationService;
```

**What to mock:**
- Repositories (DB I/O)
- External services (payment gateway, etc.)
- `AccessControlService` in integration tests (auth bypass)

**What NOT to mock:**
- Entity builders (use real instances)
- Mapper methods (test real transformations)
- Internal method calls within same service

## Fixtures and Factories

**`TestFixtures`** - `hola-pms/hola-app/src/test/java/com/hola/fixture/TestFixtures.java`

Static factory class with entity builders for common test data:

```java
public final class TestFixtures {
    private TestFixtures() {}

    // Hotel & Property
    public static Hotel createHotel() { ... }
    public static Property createProperty() { ... }
    public static Property createProperty(Hotel hotel) { ... }
    public static Property createPropertyNoTax(Hotel hotel) { ... }

    // Rate Pricing
    public static RatePricing createRatePricing(Long rateCodeId, LocalDate start, LocalDate end, BigDecimal price) { ... }
    public static RatePricing createWeekdayPricing(...) { ... }
    public static RatePricing createWeekendPricing(...) { ... }
    public static RatePricingPerson createPricingPerson(String type, int seq, BigDecimal price) { ... }

    // Reservation
    public static MasterReservation createMasterReservation(Property property) { ... }
    public static MasterReservation createMasterReservation(Property property, String status) { ... }
    public static MasterReservation createOtaMasterReservation(Property property) { ... }
    public static SubReservation createSubReservation(MasterReservation master) { ... }
    public static DailyCharge createDailyCharge(SubReservation sub, LocalDate date, BigDecimal price) { ... }

    // Payment
    public static ReservationPayment createPayment(MasterReservation master) { ... }
    public static PaymentTransaction createTransaction(Long masterId, int seq, String type, BigDecimal amount) { ... }
    public static PaymentAdjustment createAdjustment(Long masterId, int seq, String sign, BigDecimal amount) { ... }

    // Policy
    public static CancellationFee createDateCancellationFee(Property, int daysBefore, String feeType, BigDecimal amount) { ... }
    public static CancellationFee createNoShowCancellationFee(Property, String feeType, BigDecimal amount) { ... }
    public static EarlyLateFeePolicy createEarlyCheckInPolicy(Property, String timeFrom, String timeTo, ...) { ... }
    public static EarlyLateFeePolicy createLateCheckOutPolicy(Property, ...) { ... }

    // Service Item
    public static ReservationServiceItem createServiceItem(SubReservation sub, BigDecimal unitPrice, int qty) { ... }
}
```

**Inline helper methods** (per-test file, for simpler fixtures):
```java
private Hotel createHotel(String code, String name) {
    return Hotel.builder().hotelCode(code).hotelName(name).build();
}
```

## Test Data Management

**Flyway test profile** - `hola-pms/hola-app/src/test/resources/application-test.yml`:
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///hola_pms_test  # TestContainers auto-managed
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  flyway:
    locations: classpath:db/migration,classpath:db/testpatch
    target: 5.8.0        # Excludes V5_9_0+ (large test data) for speed
    out-of-order: true
  data:
    redis:
      repositories:
        enabled: false    # Redis disabled in tests
server:
  port: 0                 # Random port
```

**Test schema patches** - `hola-pms/hola-app/src/test/resources/db/testpatch/R__test_schema_patch.sql`:
- Applies DDL from V6+ through V8 migrations that are beyond `flyway.target: 5.8.0`
- Adds columns like `version` (optimistic locking), `stay_type` (dayuse), HK fields
- Repeatable migration (`R__` prefix) - reapplied on change

**Flyway test data (V5_0_0 ~ V5_8_0):**
- Properties: GMP (올라 그랜드 명동), GMS, OBH
- Rate codes, room types, test users for integration scenarios
- Admin user: `admin` / SUPER_ADMIN role

## E2E Tests

**Location:** `hola-pms/e2e-tests/`
**Framework:** Playwright 1.58.2 (Node.js CommonJS)
**Status:** Exists but NOT integrated into CI; manual execution only.

**Files:**
- `phase1-auth.js` - Login success/failure, sidebar navigation
- `phase2-hotel.js` - Hotel CRUD UI flows
- `phase3-room.js` - Room management
- `phase4-reservation.js` - Reservation workflow
- `phase4-ota-check.js` - OTA integration check

**Run (manual):**
```bash
cd hola-pms/e2e-tests
node phase1-auth.js      # Requires server running on localhost:8080
```

**Pattern:**
```javascript
const { chromium } = require('playwright');
const BASE_URL = 'http://localhost:8080';

(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await context.newPage();
    await page.goto(`${BASE_URL}/login`);
    await page.fill('#username', 'admin');
    await page.fill('#password', 'holapms1!');
    await page.click('#loginBtn');
    await page.waitForURL(/\/admin/);
    // ... assertions via URL/element checks
})();
```

## Test Coverage Gaps

### Services WITHOUT Tests (28 of 35 ServiceImpl files)

**hola-hotel (14 untested):**
- `FloorServiceImpl.java` - Floor CRUD
- `MarketCodeServiceImpl.java` - Market code CRUD
- `RoomNumberServiceImpl.java` - Room number CRUD
- `HotelAdminServiceImpl.java` - Hotel admin user management
- `PropertyAdminServiceImpl.java` - Property admin management
- `PropertyRoleServiceImpl.java` - Property role management
- `HotelRoleServiceImpl.java` - Hotel role management
- `PropertySettlementServiceImpl.java` - Settlement info management
- `CancellationFeeServiceImpl.java` - Cancellation fee rules
- `EarlyLateFeePolicyServiceImpl.java` - Early/late check fee policies
- `HousekeepingServiceImpl.java` - HK task management
- `HousekeeperServiceImpl.java` - HK staff management
- `HkAssignmentServiceImpl.java` - HK task assignments
- `RoomStatusServiceImpl.java` - Room FO/HK status management
- `RoomUnavailableServiceImpl.java` - OOO/OOS management
- `ReservationChannelServiceImpl.java` - Channel management

**hola-room (4 untested):**
- `RoomClassServiceImpl.java` - Room class CRUD
- `TransactionCodeServiceImpl.java` - Transaction code management
- `PaidServiceOptionServiceImpl.java` - Paid service options
- `FreeServiceOptionServiceImpl.java` - Free service options
- `InventoryServiceImpl.java` - Inventory management

**hola-rate (2 untested):**
- `RateCodeServiceImpl.java` - Rate code management (complex pricing logic)
- `PromotionCodeServiceImpl.java` - Promotion code management

**hola-reservation (3 untested):**
- `FrontDeskServiceImpl.java` - Front desk operations
- `RoomAssignServiceImpl.java` - Room assignment logic
- `RoomUpgradeServiceImpl.java` - Room upgrade logic
- `DashboardServiceImpl.java` - Dashboard aggregation

**hola-common (1 untested):**
- `BluewaveAdminServiceImpl.java` - Bluewave admin management

### Controllers WITHOUT Unit Tests (All 34 ApiControllers)

No controller has dedicated unit tests. Integration tests cover only 3 controllers:
- `ReservationApiController` (via `ReservationApiIntegrationTest`)
- `BookingApiController` (via `BookingApiIntegrationTest`)
- `ReservationPaymentApiController` (via `PaymentApiIntegrationTest`)

### Modules with NO Tests

- **hola-rate**: Only 1 file (`RoomTypeServiceImplTest`) actually in hola-room. Rate code service (complex pricing) has ZERO tests.

### High-Risk Untested Areas

| Area | Files | Risk |
|------|-------|------|
| Rate code pricing | `hola-rate/src/main/java/com/hola/rate/service/RateCodeServiceImpl.java` | Complex pricing rules, promotion application |
| Front desk operations | `hola-reservation/.../service/FrontDeskServiceImpl.java` | Check-in/out workflow, room status transitions |
| Room assignment | `hola-reservation/.../service/RoomAssignServiceImpl.java` | Overbooking detection, assignment logic |
| HK management | `hola-hotel/.../service/HousekeepingServiceImpl.java` | Task scheduling, status sync with rooms |
| Room status sync | `hola-hotel/.../service/RoomStatusServiceImpl.java` | FO/HK status coordination |

## Common Test Patterns

**Exception testing:**
```java
assertThatThrownBy(() -> hotelService.createHotel(request))
        .isInstanceOf(HolaException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.HOTEL_NAME_DUPLICATE);
```

**BigDecimal assertion:**
```java
assertThat(response.getGrandTotal()).isEqualByComparingTo("345000");
assertThat(response.getRemainingAmount()).isEqualByComparingTo("245000");
```

**MockMvc JSON assertion:**
```java
mockMvc.perform(get(BASE_URL, PROPERTY_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data", hasSize(0)));
```

**Entity domain logic testing (no mocks needed):**
```java
@Test
@DisplayName("grandTotal 0이하 -> PAID (결제 불필요)")
void updatePaymentStatus_zeroGrandTotal_paid() {
    ReservationPayment payment = createPayment(BigDecimal.ZERO, BigDecimal.ZERO);
    payment.updatePaymentStatus();
    assertThat(payment.getPaymentStatus()).isEqualTo("PAID");
}
```

**Verify interaction:**
```java
verify(reservationService).cancel(1L, PROPERTY_ID);
verify(hotelRepository).getNextHotelCodeSequence();
doNothing().when(accessControlService).validatePropertyAccess(anyLong());
```

## Best Practices (as observed)

1. **Always use `@DisplayName`** with Korean description on both class and method level
2. **Group with `@Nested`** by operation (Create, Update, Delete, GetList, etc.)
3. **Arrange-Act-Assert** pattern (sometimes given-when-then style)
4. **Test error paths** as thoroughly as happy paths (each ErrorCode scenario gets a test)
5. **Use `lenient()` stubs** in `@BeforeEach` for optional dependencies (prevents unnecessary strict mock warnings)
6. **Test service Javadoc comments** document test scope:
   ```java
   /**
    * ReservationServiceImpl 단위 테스트
    * 테스트 범위:
    * - CREATE (7): 정상 생성, 과거 날짜, 날짜 역전, ...
    * - UPDATE (5): 정상, CHECKED_OUT 불가, ...
    */
   ```
7. **Integration tests use `@MockBean`** for `AccessControlService` to bypass auth

---

*Testing analysis: 2026-03-26*
