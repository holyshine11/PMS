# Technology Stack

**Analysis Date:** 2026-03-26

## Languages

**Primary:**
- Java 17 - 전체 백엔드 (497개 프로덕션 소스 파일)
  - 설정: `sourceCompatibility = JavaVersion.VERSION_17` (`hola-pms/build.gradle`)
  - 컴파일러 옵션: UTF-8 인코딩, `-parameters` 플래그 (Spring `@RequestParam` 파라미터 이름 유지)

**Secondary:**
- JavaScript (ES5/jQuery) - 프론트엔드 (70개 `.js` 파일)
  - 위치: `hola-pms/hola-app/src/main/resources/static/js/`
  - 프레임워크 없음 (순수 jQuery + 네임스페이스 패턴 `HolaPms.*`)
  - 공통 유틸: `hola-common.js` (633줄)
- HTML/Thymeleaf - 서버 사이드 렌더링 (140개 `.html` 템플릿)
- SQL - Flyway 마이그레이션 (96개 마이그레이션 파일)

## Runtime

**Environment:**
- JVM: Java 17 (Spring Boot 3.2.x 최소 요구사항)
- Timezone: `Asia/Seoul` (Jackson `spring.jackson.time-zone`, 전역)
- Profile: `local` (기본), `test` (TestContainers 기반)
- Port: 8080 (기본)

**Package Manager:**
- Gradle 8.7 (Wrapper)
  - Wrapper 설정: `hola-pms/gradle/wrapper/gradle-wrapper.properties`
  - Distribution: `gradle-8.7-bin.zip`
  - Lockfile: 없음 (Gradle version catalog 미사용)

**외부 런타임 의존성:**
- PostgreSQL 16 - 메인 RDBMS
- Redis 7+ - KICC PG 결제 임시 데이터 저장 + 결과 폴링

## Frameworks

**Core:**
- Spring Boot 3.2.5 - 메인 애플리케이션 프레임워크
  - 루트 빌드: `hola-pms/build.gradle`
  - BOM: `org.springframework.boot:spring-boot-dependencies:3.2.5`
  - Dependency Management Plugin: `io.spring.dependency-management:1.1.4`
- Spring Data JPA - 데이터 액세스 레이어
  - `open-in-view: false` — 뷰에서 지연 로딩 불가, 서비스에서 반드시 fetch
  - `default_batch_fetch_size: 100` — IN 쿼리 배치
  - `ddl-auto: none` — Flyway 전용 스키마 관리
- Spring Security 6 - 인증/인가
  - 4단 SecurityFilterChain: `@Order(0)` Booking API-KEY → `@Order(1)` HK Mobile Session → `@Order(2)` API/JWT → `@Order(3)` Web/Session
  - Password: `BCryptPasswordEncoder`
  - JWT: jjwt 0.12.5 (HS256, Access 1h + Refresh 7d)
  - 설정: `hola-pms/hola-common/src/main/java/com/hola/common/security/SecurityConfig.java`
- Spring Data Redis - KICC 결제 플로우 임시 데이터/결과 캐싱
  - `StringRedisTemplate` 사용: `KiccPaymentApiController.java`
  - Redis Key: `kicc:booking:{shopOrderNo}` (TTL 30분), `kicc:result:{shopOrderNo}` (TTL 10분)
- Spring Validation - Jakarta Bean Validation

**View:**
- Thymeleaf - 서버 사이드 HTML 렌더링
  - 의존성: `hola-pms/hola-common/build.gradle` (api scope로 전파)
  - Layout Dialect 3.3.0: 3종 레이아웃 파일
    - `templates/layout/default.html` — Admin 페이지
    - `templates/layout/booking.html` — 부킹엔진 (게스트용)
    - `templates/layout/mobile.html` — HK 모바일
  - Spring Security 6 통합: `thymeleaf-extras-springsecurity6` (sec:authorize 태그)

**Testing:**
- JUnit 5 - `spring-boot-starter-test` 포함
- Mockito - `@ExtendWith(MockitoExtension.class)` 패턴
- AssertJ - 어설션 라이브러리
- Spring MockMvc - 통합 테스트 (`@AutoConfigureMockMvc`)
- Spring Security Test - `@WithMockUser(roles = "SUPER_ADMIN")`
- TestContainers 1.19.7 - PostgreSQL 16-alpine 자동 프로비저닝
  - 의존성: `hola-pms/hola-app/build.gradle`
  - URL: `jdbc:tc:postgresql:16-alpine:///hola_pms_test`
- Playwright 1.58.2 - E2E 테스트 (Node.js)
  - 위치: `hola-pms/e2e-tests/package.json`

**Build/Dev:**
- Gradle 8.7 - 빌드 도구
- Flyway - 데이터베이스 마이그레이션 (Spring Boot 통합)
  - `out-of-order: true` — 버전 대역별 병렬 개발 지원
  - 96개 마이그레이션: V1(호텔) ~ V8(하우스키핑)
  - 위치: `hola-pms/hola-app/src/main/resources/db/migration/`
- SpringDoc OpenAPI 2.5.0 - Swagger UI (`/swagger-ui.html`)

## Key Dependencies

**Critical (hola-common에서 `api` scope로 전 모듈 전파):**

| 의존성 | 버전 | 용도 |
|--------|------|------|
| `spring-boot-starter-web` | 3.2.5 | REST API + MVC + 내장 Tomcat |
| `spring-boot-starter-data-jpa` | 3.2.5 | JPA/Hibernate ORM |
| `spring-boot-starter-security` | 3.2.5 | 인증/인가 |
| `spring-boot-starter-data-redis` | 3.2.5 | Redis 클라이언트 |
| `spring-boot-starter-validation` | 3.2.5 | Bean Validation |
| `spring-boot-starter-thymeleaf` | 3.2.5 | 서버 사이드 렌더링 |

**JWT:**
- `io.jsonwebtoken:jjwt-api:0.12.5` - JWT API (compile)
- `io.jsonwebtoken:jjwt-impl:0.12.5` - JWT 구현 (runtime)
- `io.jsonwebtoken:jjwt-jackson:0.12.5` - JWT Jackson 직렬화 (runtime)

**매핑/코드 생성:**
- `org.mapstruct:mapstruct:1.5.5.Final` - DTO/Entity 매핑 어노테이션 프로세서
  - 참고: 실제로는 수동 `XxxMapper(@Component)` 클래스 사용, MapStruct 자동 매핑 미사용
- `org.projectlombok:lombok` - 보일러플레이트 제거 (`compileOnly` + `annotationProcessor`, 전 모듈)

**Database:**
- `org.postgresql:postgresql` - PostgreSQL JDBC 드라이버 (runtime)
- `org.flywaydb:flyway-core` - DB 마이그레이션

**직렬화:**
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` - `java.time.*` Jackson 직렬화
  - 날짜 포맷: `yyyy-MM-dd'T'HH:mm:ss`
  - timestamps 비활성화: `write-dates-as-timestamps: false`

**문서화:**
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0` - Swagger UI + OpenAPI 3.0

**View 확장:**
- `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0` - 레이아웃 상속

## Frontend Libraries (CDN)

모든 프론트엔드 라이브러리는 CDN으로 로드됨. 번들링/빌드 도구(Webpack, Vite 등) 없음.

**Core (모든 Admin 페이지 — `layout/default.html`):**
- jQuery 3.7.1 — DOM 조작, AJAX (`code.jquery.com`)
- Bootstrap 5.3.3 — UI 프레임워크 CSS + JS Bundle (`cdn.jsdelivr.net`)
- Font Awesome 6.5.1 — 아이콘 (`cdnjs.cloudflare.com`)
- DataTables 1.13.8 — 테이블 + Bootstrap 5 테마 (`cdn.datatables.net`)

**페이지별:**
- Chart.js 4.4.1 — 대시보드 차트 (`templates/dashboard.html`에서만)
- Daum Postcode API v2 — 한국 주소 검색 (`templates/hotel/form.html`, `templates/property/form.html`)
  - URL: `//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js`

**Typography:**
- Pretendard v1.3.9 — 한글 웹폰트 (모든 레이아웃)

## Configuration

**환경 설정 파일 구조:**
- `hola-pms/hola-app/src/main/resources/application.yml` — 공통 설정 (프로파일 무관)
- `hola-pms/hola-app/src/main/resources/application-local.yml` — 로컬 개발 (DB/Redis/JWT/KICC)
- `hola-pms/hola-app/src/test/resources/application-test.yml` — 테스트 (TestContainers, Redis 비활성화)

**주요 외부 설정값:**
- `spring.datasource.*` — PostgreSQL 연결 정보
- `spring.data.redis.*` — Redis 연결 정보
- `jwt.secret` / `jwt.access-token-expiry` / `jwt.refresh-token-expiry` — JWT 설정
- `kicc.*` — KICC PG 설정 (`mallId`, `secretKey`, `apiDomain`, `returnBaseUrl`, `timeoutSeconds`)
- `hola.upload.path` — 파일 업로드 경로

**빌드 설정 파일:**
- `hola-pms/build.gradle` — 루트 (플러그인 + 공통 의존성 + Java 17)
- `hola-pms/settings.gradle` — 6개 서브모듈 선언
- `hola-pms/gradle/wrapper/gradle-wrapper.properties` — Gradle 8.7

**Database 설정:**
- HikariCP 커넥션 풀: max 10, min-idle 5, timeout 30s (`application-local.yml`)
- `open-in-view: false` — 뷰 레이어 지연 로딩 차단
- `hibernate.ddl-auto: none` — Flyway 전용
- `default_batch_fetch_size: 100` — IN 쿼리 배치

## Platform Requirements

**Development:**
- Java 17 JDK
- PostgreSQL 16 (로컬: DB `hola_pms`, User `hola`/`hola1234`)
- Redis 7+ (로컬: `localhost:6379`)
- Docker (테스트 시 TestContainers 필요)
- Gradle 8.7 (Wrapper 자동 다운로드)

**Production:**
- Java 17 JRE
- PostgreSQL 16 (schema-per-tenant 멀티테넌시)
- Redis 7+
- 파일 시스템: 업로드 디렉토리 (기본 `./uploads`)
- KICC PG 계정: 상점ID + 시크릿키 (환경변수 `KICC_SECRET_KEY`)
- 실행: 단일 Spring Boot JAR (내장 Tomcat)
- Artifact: `hola-app/build/libs/hola-app-0.0.1-SNAPSHOT.jar`

## Build Commands

```bash
cd hola-pms
./gradlew build                    # 전체 빌드 (컴파일 + 테스트 + JAR 패키징)
./gradlew compileJava              # 컴파일만 (빠른 검증)
./gradlew :hola-app:bootRun        # 서버 실행 (http://localhost:8080)
./gradlew clean build              # 클린 빌드
./gradlew test                     # 전체 테스트 (TestContainers PostgreSQL 자동)
./gradlew :hola-hotel:test --tests "com.hola.hotel.SomeTest"  # 단일 테스트
```

## Codebase Scale

| 항목 | 수량 |
|------|------|
| Java 프로덕션 파일 | 497개 |
| Java 테스트 파일 | 24개 |
| Thymeleaf 템플릿 | 140개 |
| JavaScript 파일 | 70개 |
| CSS 파일 | 2개 (`hola.css`, `booking.css`) |
| Flyway 마이그레이션 | 96개 |
| Gradle 모듈 | 6개 (common, hotel, room, rate, reservation, app) |

---

*Stack analysis: 2026-03-26*
