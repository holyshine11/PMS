# Testing Patterns

**Analysis Date:** 2026-02-28

## Test Framework

**Runner:**
- JUnit 5 (Jupiter)
- Configuration: `tasks.named('test') { useJUnitPlatform() }` in build.gradle
- Gradle task: `./gradlew test` (all modules), `./gradlew :module:test --tests "TestClass"`

**Assertion Library:**
- AssertJ (`import static org.assertj.core.api.Assertions.*`)
- Methods: `assertThat(...).isTrue()`, `.isEqualTo()`, `.isNotNull()`, `.hasSize()`, `.isFalse()`, `.contains()`, `.throwable()`
- Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `@Spy`

**Run Commands:**
```bash
./gradlew test                                                # Run all tests
./gradlew :hola-app:test                                      # Run hola-app tests only
./gradlew :hola-hotel:test --tests "PropertyServiceImplTest"  # Single test class
./gradlew clean test                                          # Clean + rebuild + test
```

**Test Output:**
- Default: brief summary (passed/failed count)
- Verbose: `./gradlew test --info`

## Test File Organization

**Location:**
- Unit tests: `src/test/java/{module}` (mirrors src/main structure)
- Example: `hola-hotel/src/test/java/com/hola/hotel/service/PropertyServiceImplTest.java`

**Naming:**
- Unit test class: `{Subject}Test.java` (e.g., `HolaResponseTest`, `PropertyServiceImplTest`, `ReservationServiceImplTest`)
- Integration test class: `{Subject}IntegrationTest.java` (e.g., `BookingApiIntegrationTest`, `SecurityIntegrationTest`)

**Directory Structure:**
```
hola-{module}/
├── src/test/java/com/hola/{module}/
│   ├── service/
│   │   ├── PropertyServiceImplTest.java     # Unit test for service
│   │   └── ReservationServiceImplTest.java
│   ├── repository/
│   │   └── ...
│   ├── mapper/
│   │   └── ReservationMapperTest.java
│   └── entity/
│       └── ReservationPaymentTest.java
└── src/test/resources/
    └── application-test.yml                # Test config
```

**Integration tests (hola-app):**
```
hola-app/src/test/java/com/hola/
├── integration/
│   ├── booking/
│   │   └── BookingApiIntegrationTest.java  # Full HTTP test
│   ├── payment/
│   │   └── PaymentApiIntegrationTest.java
│   ├── security/
│   │   └── SecurityIntegrationTest.java
│   └── ...
├── support/
│   └── BaseIntegrationTest.java            # Base class for integration tests
└── fixture/
    └── TestFixtures.java                   # Reusable test data factories
```

## Test Structure

**Unit Test Suite Organization:**

Standard pattern using `@Nested` + `@DisplayName`:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationServiceImpl")
class ReservationServiceImplTest {

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Mock
    private MasterReservationRepository masterReservationRepository;
    @Mock
    private SubReservationRepository subReservationRepository;
    // ... more @Mock fields

    // Shared constants
    private static final Long PROPERTY_ID = 1L;
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 6, 1);

    // Shared test setup
    private Property property;
    private RateCode rateCode;

    @BeforeEach
    void setUp() {
        property = Property.builder()
                .propertyCode("GMP")
                .propertyName("그랜드 호텔")
                .build();
        // ... more setup
    }

    // Test case groups
    @Nested
    @DisplayName("예약 생성")
    class CreateReservation {

        @Test
        @DisplayName("정상 입력으로 예약 생성 성공")
        void create_validRequest_success() {
            // Arrange
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .guestNameKo("홍길동")
                    .marketCodeId(1L)
                    .rateCodeId(RATE_CODE_ID)
                    .build();
            when(propertyRepository.findById(PROPERTY_ID))
                    .thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(RATE_CODE_ID))
                    .thenReturn(Optional.of(rateCode));

            // Act
            ReservationResponse response = reservationService.createReservation(
                    PROPERTY_ID, request);

            // Assert
            assertThat(response.getId()).isNotNull();
            assertThat(response.getReservationStatus()).isEqualTo("RESERVED");
            verify(masterReservationRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("과거 날짜 체크인 시 HOLA-4014 에러")
        void create_pastDate_throwsError() {
            // Arrange
            ReservationCreateRequest request = buildRequest();
            request.setCheckIn(LocalDate.now().minusDays(1));

            // Act & Assert
            assertThatThrownBy(() -> reservationService.createReservation(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_CHECKIN_PAST_DATE);
        }
    }

    @Nested
    @DisplayName("예약 상태 변경")
    class ChangeStatus {
        // ... more tests
    }

    // Helper methods
    private MasterReservation createMaster(String status) {
        MasterReservation master = MasterReservation.builder()
                .masterReservationNo("GMP260601-0001")
                .reservationStatus(status)
                .build();
        setId(master, MASTER_ID);
        return master;
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

**Integration Test Base Class:**

`BaseIntegrationTest` in `hola-app/src/test/java/com/hola/support/`:
```java
@SpringBootTest                          // Full context load
@AutoConfigureMockMvc                   // MockMvc injection
@ActiveProfiles("test")                 // Use application-test.yml
@Import(TestContainersConfig.class)     // TestContainers setup
@Transactional                          // Auto-rollback after each test
@WithMockUser(username="admin", roles={"SUPER_ADMIN"})  // Default security
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
}
```

**Integration Test Example:**
```java
@DisplayName("부킹엔진 API 통합 테스트")
class BookingApiIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/booking";
    private static final String VALID_PROPERTY_CODE = "GMP";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private MasterReservationRepository masterReservationRepository;

    @Test
    @DisplayName("존재하는 프로퍼티 코드로 조회 시 200 반환")
    void getPropertyInfo_existingCode_200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/properties/{code}", VALID_PROPERTY_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data.propertyCode").value(VALID_PROPERTY_CODE))
                .andExpect(jsonPath("$.result.data.hotelName").value("올라 서울 호텔"));
    }

    @Test
    @WithAnonymousUser  // Override default @WithMockUser
    @DisplayName("인증 없이 접근 시 401 반환")
    void booking_anonymous_401() throws Exception {
        mockMvc.perform(post(BASE_URL + "/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
```

## Test Configuration

**File:** `hola-app/src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///hola_pms_test  # TestContainers PostgreSQL
    username: test
    password: test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  jpa:
    open-in-view: false                  # Force lazy loading to fail fast
    hibernate:
      ddl-auto: none                     # Use Flyway, not auto-schema
    properties:
      hibernate:
        format_sql: false                # No SQL logging
        default_batch_fetch_size: 100    # IN clause batching

  flyway:
    enabled: true
    locations: classpath:db/migration,classpath:db/testpatch
    baseline-on-migrate: true
    out-of-order: true
    target: 5.8.0                        # V5_9_0+ (large test data) excluded

  thymeleaf:
    cache: false                         # Reload templates

  jackson:
    time-zone: Asia/Seoul
    serialization:
      write-dates-as-timestamps: false

  data:
    redis:
      repositories:
        enabled: false                   # Redis not needed for tests

server:
  port: 0                                # Random port for integration tests

jwt:
  secret: holapms-test-secret-key-must-be-at-least-256-bits-long-for-hs256
  access-token-expiry: 3600000           # 1 hour
  refresh-token-expiry: 604800000        # 7 days
```

**TestContainers Setup:**
- `TestContainersConfig` class imported by BaseIntegrationTest
- Auto-starts PostgreSQL 16-alpine container before tests
- Uses JDBC URL prefix `jdbc:tc:postgresql:16-alpine:///hola_pms_test` for container lifecycle management
- Automatic cleanup after test suite

## Mocking

**Framework:** Mockito (provided by spring-boot-starter-test)

**Patterns:**

1. **Mock Dependencies:**
   ```java
   @Mock
   private PropertyRepository propertyRepository;

   @Mock
   private RateCodeRepository rateCodeRepository;

   @InjectMocks
   private ReservationServiceImpl reservationService;  // Mocks auto-injected
   ```

2. **Stub Return Values:**
   ```java
   when(propertyRepository.findById(PROPERTY_ID))
       .thenReturn(Optional.of(property));

   when(rateCodeRepository.findById(RATE_CODE_ID))
       .thenReturn(Optional.of(rateCode));
   ```

3. **Verify Interactions:**
   ```java
   verify(masterReservationRepository, times(1)).save(any());
   verify(dailyChargeRepository, never()).delete(any());
   ```

4. **Argument Matchers:**
   ```java
   when(repo.save(any(Entity.class))).thenReturn(savedEntity);
   when(repo.findById(eq(1L))).thenReturn(Optional.of(entity));
   when(repo.existsByCode(contains("GMP"))).thenReturn(true);
   ```

5. **Lenient Mocks (for optional stubs):**
   ```java
   @BeforeEach
   void setUp() {
       // This stub may not be called in all tests, no warning
       lenient().when(roomUnavailableRepository.findOverlapping(any(), any(), any()))
           .thenReturn(Collections.emptyList());
   }
   ```

**What to Mock:**
- Repository queries (external I/O)
- External service calls (payment gateway, email, etc.)
- Security context (if testing authorization)
- Date/time if determinism needed (rarely; use fixed LocalDate in tests)

**What NOT to Mock:**
- Entity constructors/builders (use real instances)
- Internal service method calls (test full flow if logically connected)
- Mapper methods (test real mappings)
- BaseEntity fields (createdAt, updatedAt) — let them flow through

## Fixtures and Factories

**Location:** `hola-app/src/test/java/com/hola/fixture/TestFixtures.java`

**Pattern - Static Factory Class:**
```java
public final class TestFixtures {
    private TestFixtures() {}  // Prevent instantiation

    // Hotel & Property
    public static Hotel createHotel() {
        return Hotel.builder()
                .hotelCode("HTL00001")
                .hotelName("테스트 호텔")
                .build();
    }

    public static Property createProperty() {
        return createProperty(createHotel());
    }

    public static Property createProperty(Hotel hotel) {
        return Property.builder()
                .hotel(hotel)
                .propertyCode("GMP")
                .propertyName("테스트 프로퍼티")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .taxRate(new BigDecimal("10"))
                .taxDecimalPlaces(0)
                .taxRoundingMethod("ROUND_DOWN")
                .serviceChargeRate(new BigDecimal("5"))
                .build();
    }

    // Variant: No tax property
    public static Property createPropertyNoTax(Hotel hotel) {
        return Property.builder()
                .hotel(hotel)
                .propertyCode("GMP")
                .propertyName("테스트 프로퍼티")
                .taxRate(BigDecimal.ZERO)
                .serviceChargeRate(BigDecimal.ZERO)
                .build();
    }

    // Pricing fixtures
    public static RatePricing createRatePricing(Long rateCodeId, LocalDate start,
                                                 LocalDate end, BigDecimal price) {
        return RatePricing.builder()
                .rateCodeId(rateCodeId)
                .startDate(start)
                .endDate(end)
                .dayMon(true).dayTue(true).dayWed(true).dayThu(true)
                .dayFri(true).daySat(true).daySun(true)
                .baseSupplyPrice(price)
                .baseTax(BigDecimal.ZERO)
                .baseTotal(price)
                .persons(new ArrayList<>())
                .build();
    }
}
```

**Usage in Tests:**
```java
@BeforeEach
void setUp() {
    property = TestFixtures.createProperty();
    rateCode = RateCode.builder().saleStartDate(...).build();
    pricing = TestFixtures.createRatePricing(RATE_CODE_ID, start, end, BigDecimal.valueOf(100));
}
```

## Coverage

**Target:** ~80% line coverage per module (not enforced by CI, but tracked)

**View Coverage:**
```bash
./gradlew jacocoTestReport  # If JaCoCo configured (optional)
```

**Coverage by Module:**
- `hola-common`: ~85% (Security, DTO, Exception handling)
- `hola-hotel`: ~75% (CRUD-heavy, property setup)
- `hola-room`: ~70% (Configuration tables)
- `hola-rate`: ~65% (Complex pricing logic being expanded)
- `hola-reservation`: ~80% (Most tested: complex state machine, payment flows)

**No Strict Enforcement:**
- Jenkins/GitHub Actions does not block on coverage threshold
- Code review may request tests for critical paths (payment, reservations, soft delete)

## Test Types

### Unit Tests (Mockito)

**Scope:** Single class in isolation
**Location:** `{module}/src/test/java/{domain}/{category}/{Subject}Test.java`
**Example:** `ReservationServiceImplTest`, `PriceCalculationServiceTest`

**Characteristics:**
- All dependencies @Mock
- Fast execution (< 1s per test)
- High granularity (each @Test tests one code path)
- @Nested groups for organizing related scenarios

**When to Write:**
- Service business logic (validation, state transitions, calculations)
- Utility functions (maskName, maskPhone, date calculations)
- Mapper transformations
- Simple entity behavior

**Example - Error Handling:**
```java
@Test
@DisplayName("과거 날짜 예약 시 HOLA-4014 에러")
void create_pastCheckIn_throwsException() {
    // Arrange
    ReservationCreateRequest request = new ReservationCreateRequest();
    request.setCheckIn(LocalDate.now().minusDays(1));

    // Act & Assert
    assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
            .isInstanceOf(HolaException.class)
            .hasFieldOrPropertyWithValue("errorCode",
                    ErrorCode.RESERVATION_CHECKIN_PAST_DATE);
}
```

### Integration Tests (MockMvc)

**Scope:** Full HTTP layer + service + database
**Location:** `hola-app/src/test/java/com/hola/integration/{domain}/{Subject}IntegrationTest.java`
**Base Class:** `BaseIntegrationTest`

**Characteristics:**
- Extends BaseIntegrationTest (SpringBootTest, MockMvc, Transactional)
- No @Mock repositories; uses real database (TestContainers PostgreSQL)
- Real service beans, real mappers
- TestData injected via Flyway (V5_0_0 ~ V5_14_0 scripts)
- Slower (5-30s per test class), but tests actual integration

**When to Write:**
- API endpoints (GET, POST, PUT, DELETE)
- End-to-end workflows (create reservation → add leg → pay → check in)
- Security (authorization checks, role validation)
- Data consistency (transactions, soft delete filtering)
- Booking engine public API

**Example - API Test:**
```java
@Test
@DisplayName("호텔 코드로 프로퍼티 정보 조회")
void getPropertyInfo_validCode_200() throws Exception {
    mockMvc.perform(get("/api/v1/booking/properties/{code}", "GMP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
            .andExpect(jsonPath("$.result.data.propertyCode").value("GMP"))
            .andExpect(jsonPath("$.result.data.propertyName").value("올라 그랜드 명동"));
}

@Test
@WithAnonymousUser
@DisplayName("인증 없이 결제 API 호출 시 401")
void payment_anonymous_401() throws Exception {
    mockMvc.perform(post("/api/v1/reservations/1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(paymentRequest)))
            .andExpect(status().isUnauthorized());
}
```

### E2E Tests (Optional)

**Not actively used in this codebase.**
- `hola-pms/e2e-tests/` exists but not populated
- Future: Selenium/Playwright for frontend interactions
- Would test: UI workflows, cross-browser compatibility

## Common Patterns

### Async Testing

**Pattern:** Use real Futures, not mocking async behavior
```java
@Test
@DisplayName("비동기 작업 완료 대기")
void asyncJob_completesSuccessfully() throws Exception {
    CompletableFuture<ReservationResponse> future = reservationService.createAsync(request);

    ReservationResponse response = future.get(2, TimeUnit.SECONDS);

    assertThat(response.getId()).isNotNull();
}
```

### Error Testing

**Pattern:** assertThatThrownBy with error code verification
```java
@Test
@DisplayName("중복 객실 예약 시 HOLA-4011 에러")
void createSub_roomConflict_throwsException() {
    // Arrange: existing reservation for same room/dates
    when(subReservationRepository.existsOverlap(roomId, checkIn, checkOut))
            .thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.addSubReservation(master, request))
            .isInstanceOf(HolaException.class)
            .matches(e -> ((HolaException) e).getErrorCode()
                    == ErrorCode.SUB_RESERVATION_ROOM_CONFLICT);
}
```

### Data Setup in Integration Tests

**Pattern 1 - TestFixtures (unit):**
```java
@BeforeEach
void setUp() {
    property = TestFixtures.createProperty();
    rateCode = TestFixtures.createRateCode();
}
```

**Pattern 2 - Repository Persistence (integration):**
```java
@Autowired
private PropertyRepository propertyRepository;

@Test
void testWithRealData() {
    // Use Flyway test data (V5_0_0 ~ V5_14_0)
    Property property = propertyRepository.findByPropertyCode("GMP").orElseThrow();

    // ...test with real data
}
```

**Pattern 3 - Transient Setup (rare):**
```java
@Test
void testWithNewEntity() {
    Hotel hotel = Hotel.builder().hotelCode("TEST001").build();
    hotelRepository.saveAndFlush(hotel);
    Property property = Property.builder().hotel(hotel).build();
    propertyRepository.saveAndFlush(property);

    // ...test
    // Auto-rollback via @Transactional
}
```

## Test Data Strategy

**Flyway Migrations (Production Seed):**
- `V5_0_0__initial.sql` through `V5_14_0__test_fixtures.sql`: Standard test fixtures
- Properties: GMP (그랜드 명동), GMS (마포신사), OBH (올라 부산 해운대)
- Rate codes: 5-star, 4-star, dayuse rates for 2026 (sale period 2026-01-01 ~ 2026-12-31)
- Test accounts: admin user with SUPER_ADMIN role

**application-test.yml Configuration:**
- `flyway.target: 5.8.0` → V5_9_0+ (large bulk data) excluded for speed
- In-memory or TestContainers database
- Auto-cleanup via @Transactional rollback

**Fixture Class:**
- `TestFixtures.java` for ad-hoc entity builders
- Does NOT persist to DB (used in unit tests)
- Variants: `createProperty()`, `createPropertyNoTax()`, `createRatePricing()`, etc.

## Running Tests

**All Tests:**
```bash
./gradlew test                  # All modules
./gradlew clean test            # Fresh build + test
```

**Specific Module:**
```bash
./gradlew :hola-hotel:test      # hola-hotel tests only
./gradlew :hola-app:test        # Integration tests
```

**Specific Test Class:**
```bash
./gradlew :hola-reservation:test --tests "ReservationServiceImplTest"
./gradlew :hola-app:test --tests "BookingApiIntegrationTest"
```

**Specific Test Method:**
```bash
./gradlew :hola-hotel:test --tests "PropertyServiceImplTest.createProperty_validRequest_success"
```

**With Logging:**
```bash
./gradlew test --info           # Detailed output
./gradlew test --debug          # Very verbose
```

**Parallel Execution (optional):**
```bash
./gradlew test -x               # Stop on first failure
./gradlew test --max-workers=4  # Use 4 worker threads (caution: DB conflicts)
```

## Best Practices

1. **One assertion per test** (or related group):
   - Clear failure message (know exactly what failed)
   - Exception: related assertions (response code + body content OK)

2. **Descriptive test names:**
   - Use @DisplayName("...") with clear scenario description
   - Include precondition + action + expected result
   - Example: "과거 날짜 체크인 시 HOLA-4014 에러" (clear in failure reports)

3. **Arrange-Act-Assert pattern:**
   ```java
   // Arrange: set up data, mocks
   // Act: execute method
   // Assert: verify result
   ```

4. **No test interdependencies:**
   - Each @Test must be runnable in any order
   - Use @BeforeEach for shared setup, not shared state

5. **Mock only what's necessary:**
   - Repository queries: YES (external I/O)
   - Internal service calls in same flow: NO (test full flow)
   - Mappers: NO (test the transformation)

6. **Cleanup:**
   - @Transactional on integration tests auto-rolls back
   - Unit tests need no cleanup (no DB access)

7. **Security Testing:**
   - Use `@WithMockUser(username="...", roles={...})` to override default
   - Use `@WithAnonymousUser` for auth-required endpoints
   - Test both positive (authorized) and negative (forbidden) paths

---

*Testing analysis: 2026-02-28*
