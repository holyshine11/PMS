# Technology Stack

**Analysis Date:** 2026-02-28

## Languages

**Primary:**
- Java 17 - Core backend language, set as sourceCompatibility/targetCompatibility in all modules

## Runtime

**Environment:**
- Java Virtual Machine (JVM) via Spring Boot 3.2.5
- Port: 8080 (HTTP)
- Timezone: Asia/Seoul (configured in application.yml)
- Character encoding: UTF-8 (forced on servlet layer)

**Package Manager:**
- Gradle 8.x (inferred from build structure)
- Lockfile: Not present (Gradle wrapper manages versions)

## Frameworks

**Core:**
- Spring Boot 3.2.5 - Application framework and dependency management
- Spring Framework (transitive) - Core Spring Framework
- Spring Web (spring-boot-starter-web) - REST API and servlet support
- Spring Data JPA (spring-boot-starter-data-jpa) - ORM abstraction layer
- Spring Security 6 (spring-boot-starter-security) - Authentication and authorization
- Spring Data Redis (spring-boot-starter-data-redis) - Redis integration (configured but tests disable)

**Templating & View:**
- Thymeleaf 3 (spring-boot-starter-thymeleaf) - Server-side HTML template engine
- Thymeleaf Layout Dialect 3.3.0 - Master page/layout support
- Thymeleaf Spring Security 6 (thymeleaf-extras-springsecurity6) - sec:authorize tags

**Testing:**
- JUnit 5 (spring-boot-starter-test) - Test framework
- Mockito (spring-boot-starter-test) - Mocking library
- Spring Security Test (spring-security-test) - Authentication/authorization testing
- TestContainers 1.19.7 - Docker-based PostgreSQL for integration tests
- PostgreSQL 16-alpine (TestContainers image) - Test database isolation

**Build/Dev:**
- Gradle - Build automation tool
- Spring Boot Gradle Plugin - Spring Boot application packaging
- Spring Dependency Management Plugin - BOM-based dependency resolution
- Lombok - Annotation processing for boilerplate (getters/setters/constructors)
- JavaCompile with -parameters flag - Parameter name preservation for @RequestParam binding

## Key Dependencies

**Critical:**

- PostgreSQL 16 (org.postgresql:postgresql) - Primary relational database driver
- Spring Data JPA + Hibernate ORM - JPA provider for entity mapping and queries
- Flyway (org.flywaydb:flyway-core) - Database schema versioning and migration
  - Enabled with out-of-order migration support
  - Locations: classpath:db/migration
  - Baseline on migrate enabled
  - Critical: Schema-per-tenant multi-tenancy design

**Authentication & Authorization:**

- JJWT (io.jsonwebtoken) - JWT token generation/parsing
  - jjwt-api 0.12.5 - Core JWT API
  - jjwt-impl 0.12.5 - Implementation
  - jjwt-jackson 0.12.5 - JSON serialization support
  - HS256 algorithm (HMAC SHA-256)
  - Access token: 1 hour expiry
  - Refresh token: 7 days expiry
  - Secret: 256+ bits required (application-local.yml example)

**Data Serialization:**

- Jackson (com.fasterxml.jackson.datatype:jackson-datatype-jsr310) - JSON serialization
  - JSR310 support for java.time.* classes
  - Date format: yyyy-MM-dd'T'HH:mm:ss
  - Timezone: Asia/Seoul
  - Write dates as ISO-8601 strings (not timestamps)

**ORM & Validation:**

- Hibernate Validator (spring-boot-starter-validation) - Bean validation annotations
- MapStruct 1.5.5.Final - Type-safe DTO/Entity mapping
  - Annotation processor for compile-time code generation
  - Manual mappers preferred (no automatic mapping)

**API Documentation:**

- SpringDoc OpenAPI (springdoc-openapi-starter-webmvc-ui 2.5.0) - OpenAPI 3.0 Swagger UI
  - Accessible at http://localhost:8080/swagger-ui.html
  - Auto-generates OpenAPI spec from Controller annotations

## Configuration

**Environment Variables:**

Development (`application-local.yml`):
- `SPRING_PROFILES_ACTIVE=local` (default in application.yml)
- `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hola_pms`
- `SPRING_DATASOURCE_USERNAME=hola`
- `SPRING_DATASOURCE_PASSWORD=hola1234` (test credentials only)
- `SPRING_DATA_REDIS_HOST=localhost`
- `SPRING_DATA_REDIS_PORT=6379`
- `JWT_SECRET=holapms-local-secret-key-must-be-at-least-256-bits-long-for-hs256`
- `HOLA_UPLOAD_PATH=./uploads` (relative to working directory)
- Thymeleaf cache disabled for development
- Static resource cache disabled (period: 0, no-cache: true, no-store: true)
- Hibernate SQL logging enabled (DEBUG level)

Test (`application-test.yml`):
- `SPRING_DATASOURCE_URL=jdbc:tc:postgresql:16-alpine:///hola_pms_test`
- TestContainers PostgreSQL 16-alpine (auto-managed)
- Redis disabled via `spring.data.redis.repositories.enabled=false`
- Flyway target: V5_8_0 (excludes large test data migrations V5_9_0+)
- Server port: 0 (random port for parallel test runs)
- Log level: WARN (root), INFO (com.hola)

**Build:**

- `hola-pms/build.gradle` - Root build with:
  - Spring Boot 3.2.5 plugin
  - Dependency management BOM
  - Java 17 target
  - Lombok annotation processing
  - JUnit Platform (JUnit 5) task runner
  - UTF-8 encoding forced
  - Parameter names preserved via `-parameters` compiler arg

- Module builds (`hola-common/`, `hola-hotel/`, `hola-room/`, `hola-rate/`, `hola-reservation/`, `hola-app/`) extend root with module-specific dependencies

## Database Configuration

**Relational:**

- PostgreSQL 16 (local: 5432, production: TBD)
- Driver: org.postgresql.Driver
- Connection pool: HikariCP (default, configured in application-local.yml)
  - Maximum pool size: 10
  - Minimum idle: 5
  - Connection timeout: 30 seconds
- Schema-per-tenant design: Each hotel/property has isolated PostgreSQL schema
- Soft delete: All entities use `deletedAt` + `useYn` columns (no physical deletion)
- JPA configuration:
  - `open-in-view: false` - No lazy loading in view layer (service must fetch all needed data)
  - `hibernate.ddl-auto: none` - Flyway handles schema
  - `hibernate.default_batch_fetch_size: 100` - Batch IN queries to 100 items
  - SQL formatting: enabled (formatted output in logs)

**Caching:**

- Redis 7+ (localhost:6379 in local profile)
- Purpose: Session management, potential query caching (not yet utilized)
- Spring Data Redis Starter configured but not actively used in current Phase 0

**File Storage:**

- Local filesystem only
- Default path: `./uploads` (configurable via `hola.upload.path`)
- Subdirectories: `biz-license/`, `logo/`, etc.
- Allowed formats: pdf, jpg, jpeg, png, gif, svg
- Max file size: 10MB (multipart.max-file-size)
- Max request size: 10MB (multipart.max-request-size)

## Platform Requirements

**Development:**

- Java 17 JDK
- PostgreSQL 16 server (or use `./gradlew test` which auto-provisions TestContainers)
- Redis 7+ (optional for local dev, disabled in tests)
- Gradle 8.x or higher (or use wrapper)
- macOS/Linux/Windows with Bash support

**Production:**

- Java 17+ JVM
- PostgreSQL 16+ database (schema-per-tenant isolation)
- Redis 7+ (for session state, if multi-instance)
- Execution model: Single Spring Boot JAR with embedded Tomcat
- Output: bootJar via `./gradlew :hola-app:bootRun` or `./gradlew build` → `hola-app/build/libs/*.jar`

## API Contract

**REST:**

- Base path: `/api/v1`
- Request/Response: JSON (UTF-8)
- Pagination: Custom PageInfo object (not Spring Page)
- Response envelope: `HolaResponse` (success/error wrapper)
- Error responses: HTTP status code + ErrorCode (e.g., HOLA-0005, HOLA-1001) + message
- Multipart form: `/api/v1/files` endpoint for file uploads

**Swagger/OpenAPI:**

- UI: http://localhost:8080/swagger-ui.html
- Spec: http://localhost:8080/v3/api-docs
- Generated from `@RestController`, `@RequestMapping`, `@GetMapping`, etc.

## Logging

**Configuration:**

- Framework: SLF4J (via Logback, Spring Boot default)
- Root level: INFO
- Package `com.hola`: INFO (development), DEBUG (local profile), varies by test
- Hibernate SQL: DEBUG (local profile only)
- BasicBinder: TRACE (local profile only, shows parameter values)
- Format: Spring Boot default (timestamp, level, logger name, message)

## Java Compatibility

- Source: Java 17
- Target: Java 17
- Module system: Not used (classpath mode)
- Preview features: None

---

*Stack analysis: 2026-02-28*
