# External Integrations

**Analysis Date:** 2026-02-28

## APIs & External Services

**Booking Engine (Stateless API-KEY Auth):**
- Endpoint: `/api/v1/booking/**`
- Purpose: External channel partner creation/management of reservations
- Auth: BookingApiKeyFilter + API-KEY header validation (not JWT)
- Session creation: Stateless (SessionCreationPolicy = NEVER)
- Status: Implemented with MockPaymentGateway placeholder

**Payment Gateway (Mock):**
- Service: MockPaymentGateway (com.hola.reservation.booking.gateway.MockPaymentGateway)
- Purpose: Placeholder for KICC/PayPal/other payment processor
- Implementation: Always returns success
- Interface: `PaymentGateway` (authorize/cancel methods)
- Payment methods: Card-based (card bin validation prepared)
- Currency: Multi-currency support via CurrencyConversionService (exchange rate lookup)
- Status: Mock only, ready for production gateway substitution

**Card Bin Validation Service:**
- Service: CardBinValidationService (com.hola.reservation.booking.service.CardBinValidationService)
- Purpose: Validate credit card BIN (Bank Identification Number) for security
- Status: Prepared but not actively called (future integration)
- Related entity: `rsv_card_bin_validation_log` (audit table exists)

**Currency Conversion:**
- Service: CurrencyConversionService (com.hola.reservation.booking.service.CurrencyConversionService)
- Purpose: Multi-currency booking support with exchange rates
- Status: Prepared, lookup mechanism not specified (likely local rate table)

## Data Storage

**Databases:**

- **Primary Relational:**
  - PostgreSQL 16
  - Connection: `jdbc:postgresql://localhost:5432/hola_pms`
  - Client: Spring Data JPA + Hibernate ORM
  - Schema: Schema-per-tenant (separate schema per hotel)
  - Tables: Prefixed by domain module (htl_*, rm_*, rt_*, rsv_*, fd_*, hk_*)
  - Soft delete: All entities respect `deletedAt IS NULL` via @SQLRestriction

- **Test Database:**
  - PostgreSQL 16-alpine (TestContainers)
  - Auto-provisioned for `./gradlew test`
  - URL: `jdbc:tc:postgresql:16-alpine:///hola_pms_test`
  - Isolation: Complete schema reset between test runs

**Caching:**

- **Redis 7+**
  - Purpose: Session management (configured, not heavily used in Phase 0)
  - Connection: localhost:6379 (local), TBD (production)
  - Status: Starter dependency present, disabled in tests
  - Future use: Query caching, distributed sessions

**File Storage:**

- **Local Filesystem Only**
  - Path: `./uploads` (configurable)
  - Supported formats: pdf, jpg, jpeg, png, gif, svg
  - Subdirectories: biz-license/, logo/, etc. (managed by FileUploadService)
  - No cloud integration (S3/Azure Blob Storage/GCS) in current Phase

## Authentication & Identity

**Primary (Admin/Staff):**

- **Type:** JWT (JSON Web Tokens)
- **Mechanism:** Stateless Bearer token in Authorization header
- **Filter:** JwtAuthenticationFilter (applies to `/api/**` not `/api/v1/booking/**`)
- **Algorithm:** HS256 (HMAC SHA-256)
- **Secret:** Environment-provided (min 256 bits)
- **Tokens:**
  - Access: 1 hour expiry
  - Refresh: 7 days expiry
  - Claims: `sub` (username), `iat` (issued-at), `exp` (expiration), `role` (for access token)
- **Implementation:** JJWT library
- **Endpoint:** `POST /api/v1/auth/login` (returns access + refresh tokens)

**Housekeeping Mobile:**

- **Type:** Session-based
- **Mechanism:** Session attribute authentication (session ID in cookie)
- **Filter:** HkMobileSessionFilter (applies to `/api/v1/properties/*/hk-mobile/**`)
- **Session attributes:** `hkUserId`, `hkUserRole`
- **Session fixation:** Disabled (sessionFixation().none())
- **Purpose:** Separate from admin SessionContext to prevent SecurityContext pollution
- **Endpoint:** Form login to `/login` then HK-specific paths
- **Critical note:** Must restore original SecurityContext in filter finally block

**Form Login (Web UI):**

- **Type:** Session + password authentication
- **Mechanism:** POST /login → session cookie
- **Password encoder:** BCrypt (10 salt rounds)
- **Password policy:** 10-20 chars, 5 failed attempts locks account
- **Role hierarchy:** SUPER_ADMIN > HOTEL_ADMIN > PROPERTY_ADMIN > HOUSEKEEPING_SUPERVISOR > HOUSEKEEPER
- **Test credentials:** admin / holapms1! (Flyway-provided, BCrypt $2a$ hash)

**Multi-Tenancy (Tenant Isolation):**

- **Mechanism:** TenantFilter + TenantContext (ThreadLocal)
- **Header:** X-Tenant-ID (multi-schema routing)
- **Implementation:**
  - TenantFilter (com.hola.common.tenant.TenantFilter) - Sets ThreadLocal
  - TenantIdentifierResolver - JPA MultiTenancyStrategy.SCHEMA
  - TenantConnectionProvider - Selects schema based on tenant
- **Scope:** Per HTTP request, cleared after response
- **Purpose:** Enforce schema-per-tenant isolation (no cross-hotel data leakage)

## Monitoring & Observability

**Error Tracking:**

- Type: None (centralized exception handler)
- Mechanism: GlobalExceptionHandler (com.hola.common.exception.GlobalExceptionHandler)
- Output: JSON error responses with ErrorCode + message
- Status codes: Mapped error codes to HTTP status (e.g., 400 BAD_REQUEST, 409 CONFLICT)
- No external APM (DataDog, New Relic, Sentry) integrated

**Logs:**

- **Framework:** SLF4J + Logback (Spring Boot default)
- **Format:** Spring Boot standard (timestamp, level, logger, message)
- **Levels:**
  - Root: INFO
  - com.hola: INFO (prod), DEBUG (local)
  - org.hibernate.SQL: DEBUG (local only)
  - org.testcontainers: INFO (tests only)
- **Aggregation:** None (logs to stdout/stderr)
- **Future:** No log shipping configured (Elastic Stack/CloudWatch/GCP Logging TBD)

**Metrics:**

- Type: None (Spring Boot Actuator not configured)
- No health checks exposed
- No metrics endpoint

## CI/CD & Deployment

**Hosting:**

- Target: Not yet determined (AWS/Azure/On-premise TBD)
- Deployment model: Single Spring Boot JAR (embedded Tomcat)
- Build artifact: `hola-app/build/libs/*.jar`
- Port: 8080 (configurable via server.port)

**CI Pipeline:**

- Status: Not integrated in repository (no GitHub Actions/GitLab CI/Jenkins)
- Build command: `./gradlew clean build`
- Test command: `./gradlew test` (TestContainers provisions PostgreSQL 16)
- Manual: Developer runs locally before push

**Docker:**

- Dockerfile: Not present in repository (future requirement)
- TestContainers used for local/CI test isolation
- Runtime: Java 17+ JVM only

## Environment Configuration

**Required Environment Variables (Production):**

- `SPRING_DATASOURCE_URL` - PostgreSQL JDBC connection string
- `SPRING_DATASOURCE_USERNAME` - PostgreSQL user
- `SPRING_DATASOURCE_PASSWORD` - PostgreSQL password (secret)
- `SPRING_DATA_REDIS_HOST` - Redis host
- `SPRING_DATA_REDIS_PORT` - Redis port
- `JWT_SECRET` - Minimum 256-bit string for HS256 signing
- `JWT_ACCESS_TOKEN_EXPIRY` - Milliseconds (default 3600000 = 1h)
- `JWT_REFRESH_TOKEN_EXPIRY` - Milliseconds (default 604800000 = 7d)
- `HOLA_UPLOAD_PATH` - File upload directory (must be writable)
- `SERVER_PORT` - Tomcat port (default 8080)

**Secrets Location:**

- Local: `application-local.yml` (development only, hardcoded test values)
- Production: Environment variables or Spring Cloud Config (not yet implemented)
- No `.env` file committed (security best practice)
- JWT secret: Pass via environment, never hardcode in production

**Profile Activation:**

- Default: `spring.profiles.active=local` (in application.yml)
- Override: `-Dspring.profiles.active=prod` or environment variable
- Available profiles: `local`, `test` (implicit), `prod` (TBD)

## Webhooks & Callbacks

**Incoming:**

- `/api/v1/booking/**` - Booking engine channel integration (POST for reservations)
- `/api/v1/auth/login` - Authentication endpoint
- `/api/v1/files` - File upload endpoint
- No external webhook receivers configured

**Outgoing:**

- None currently implemented
- Future: Folio/EOD notification webhooks, housekeeping task status callbacks (Phase 3)

## API Key Management

**Booking API Keys:**

- Entity: BookingApiKey (com.hola.reservation.booking.entity.BookingApiKey)
- Table: `rsv_booking_api_key`
- Service: BookingApiKeyService (validate + log usage)
- Filter: BookingApiKeyFilter (intercepts `/api/v1/booking/**`)
- Hash: Stored as bcrypt hash in `api_key_hash` column (not plaintext)
- Validation: OncePerRequestFilter checks Authorization header value against stored hash
- Audit: Each request logged in application logs (not in separate table)

## Third-Party Libraries (Non-Framework)

**Code Generation/Annotation Processing:**

- Lombok 1.18+ - Reduce boilerplate (getters, setters, constructors, logging)

**Data Validation:**

- Hibernate Validator - Bean validation (javax.validation.constraints.*)

**DTO Mapping:**

- MapStruct 1.5.5 - Type-safe annotation-based DTO/Entity conversion

**Serialization:**

- Jackson (core, datatype-jsr310) - JSON serialization, LocalDate/LocalDateTime handling

**Database Migration:**

- Flyway - Schema versioning and migration (executed on startup if enabled)

**API Documentation:**

- SpringDoc OpenAPI 2.5.0 - Generates OpenAPI 3.0 spec from controller annotations

**Testing Isolation:**

- TestContainers 1.19.7 - Docker-based PostgreSQL 16-alpine provisioning for tests

**JWT:**

- JJWT 0.12.5 - JWT token generation/parsing with HS256 signing

---

*Integration audit: 2026-02-28*
