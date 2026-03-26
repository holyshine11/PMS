# External Integrations

**Analysis Date:** 2026-03-26

## APIs & External Services

### KICC 이지페이 PG (결제 게이트웨이)

**용도:** 부킹엔진 온라인 카드 결제 (거래등록 → 인증 → 승인 3단계 플로우)

**구현 파일:**
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccApiClient.java` — HTTP 클라이언트 (모든 API 호출 담당)
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccPaymentGateway.java` — `PaymentGateway` 인터페이스 구현
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccConfig.java` — RestTemplate Bean 설정
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccProperties.java` — `@ConfigurationProperties(prefix = "kicc")`
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccHmacUtils.java` — HMAC 서명 유틸
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/KiccPaymentApiController.java` — 결제 API 엔드포인트

**API 엔드포인트 (KICC 서버):**

| API | URL | 용도 |
|-----|-----|------|
| 거래등록 | `POST {apiDomain}/api/ep9/trades/webpay` | 결제창 URL 발급 |
| 결제승인 | `POST {apiDomain}/api/ep9/trades/approval` | 인증 후 최종 승인 |
| 빌키결제 | `POST {apiDomain}/api/trades/approval/batch` | 등록된 빌키로 재결제 |
| 빌키삭제 | `POST {apiDomain}/api/trades/removeBatchKey` | 빌키 삭제 |
| 취소/환불 | `POST {apiDomain}/api/trades/revise` | 전체/부분 취소 |
| 거래조회 | `POST {apiDomain}/api/trades/retrieveTransaction` | 거래 상태 확인 |

**설정값 (application-local.yml):**
- `kicc.mall-id`: 상점ID (테스트: `T5102001`)
- `kicc.secret-key`: 시크릿키 (환경변수 `KICC_SECRET_KEY`)
- `kicc.api-domain`: API 도메인 (테스트: `https://testpgapi.easypay.co.kr`)
- `kicc.return-base-url`: 인증 완료 후 리턴 URL (로컬: `http://localhost:8080`)
- `kicc.timeout-seconds`: API 타임아웃 (기본 30초)
- `kicc.billing-cert-type`: 빌키 카드인증 타입 (기본 `"0"`)

**HTTP 클라이언트:** `RestTemplate` (Spring Framework)
- 전용 Bean: `kiccRestTemplate` (`@Qualifier` 사용)
- Jackson ObjectMapper: `FAIL_ON_UNKNOWN_PROPERTIES = false` (하위 호환성)
- 프로파일 제한: `@Profile("!test")` — 테스트 환경에서 제외

**결제 플로우 (3단계 비동기):**
1. 프론트엔드 → `POST /api/v1/booking/payment/register` → 검증 + Redis 임시 저장 + KICC 거래등록 → 결제창 URL 반환
2. KICC 결제창 → 사용자 카드 인증 → `POST /api/v1/booking/payment/return` (returnUrl 콜백)
3. 서버에서 KICC 결제승인 API 호출 + 예약 생성 → 결과 Redis 저장

**에러 처리:**
- `ErrorCode.PG_REGISTER_FAILED` — 거래등록 실패
- `ErrorCode.PG_APPROVAL_FAILED` — 결제승인 실패
- `ErrorCode.PG_CANCEL_FAILED` — 취소/환불 실패
- `ErrorCode.PG_COMMUNICATION_ERROR` — API 통신 오류 (RestClientException)

### PaymentGateway 추상화

**인터페이스:** `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/gateway/PaymentGateway.java`

**메서드:**
- `authorize(PaymentRequest)` — 동기식 결제 승인 (Mock용)
- `cancel(String approvalNo)` — 승인 취소 (Mock용)
- `registerTransaction(RegisterRequest)` — PG 거래등록 (3단계 플로우)
- `approveAfterAuth(ApproveAfterAuthRequest)` — PG 인증 후 승인 (3단계 플로우)
- `cancelPayment(CancelPaymentRequest)` — PG 결제 취소

**구현체:**
- `KiccPaymentGateway` — KICC 이지페이 실 PG (`@Profile("!test")`)
- `MockPaymentGateway` — 테스트용 가상 승인 (항상 성공 반환)

### Daum Postcode API (주소 검색)

**용도:** 호텔/프로퍼티 등록 시 한국 주소 검색 (우편번호 + 도로명주소)

**사용 위치:**
- `hola-pms/hola-app/src/main/resources/templates/hotel/form.html` (호텔 등록/수정 폼)
- `hola-pms/hola-app/src/main/resources/templates/property/form.html` (프로퍼티 등록/수정 폼)

**스크립트:** `//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js`
- 클라이언트 사이드 전용 (서버 API 호출 없음)
- 인증 불필요 (무료 API)

## 내부 API (노출)

### Booking Engine API

**엔드포인트:** `/api/v1/booking/**`
**인증:** `BookingApiKeyFilter` — `X-API-KEY` 헤더 검증
**보안:** `BookingSecurityConfig` (`@Order(0)`, STATELESS, permitAll)

**주요 파일:**
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingSecurityConfig.java`
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingApiKeyFilter.java`
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/entity/BookingApiKey.java`
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingApiKeyService.java`

**API Key 저장:** `rsv_booking_api_key` 테이블, BCrypt 해시 저장

### Admin REST API

**엔드포인트:** `/api/v1/**` (booking 제외)
**인증:** JWT Bearer Token (`JwtAuthenticationFilter`)
**보안:** `SecurityConfig` (`@Order(2)`, SessionCreationPolicy.NEVER)
**응답 형식:** `HolaResponse.success(data)` / `HolaResponse.error(code, message)`

### HK Mobile API

**엔드포인트:** `/api/v1/properties/{propertyId}/hk-mobile/**`
**인증:** 세션 기반 (session attribute: `hkUserId`, `hkUserRole`)
**보안:** `SecurityConfig` (`@Order(1)`, SessionCreationPolicy.IF_REQUIRED)

## Data Storage

### PostgreSQL 16 (Primary Database)

**연결:**
- 로컬: `jdbc:postgresql://localhost:5432/hola_pms`
- 테스트: `jdbc:tc:postgresql:16-alpine:///hola_pms_test` (TestContainers)
- 드라이버: `org.postgresql.Driver`

**커넥션 풀:** HikariCP (Spring Boot 기본)
- `maximum-pool-size: 10`
- `minimum-idle: 5`
- `connection-timeout: 30000` (30초)

**멀티테넌시:** Schema-per-Tenant
- `TenantFilter` → `X-Tenant-ID` 헤더 → `TenantContext` (ThreadLocal)
- 파일: `hola-pms/hola-common/src/main/java/com/hola/common/tenant/TenantFilter.java`

**테이블 접두사:**

| 접두사 | 도메인 |
|--------|--------|
| `htl_` | 호텔/프로퍼티/회원 |
| `rm_` | 객실클래스/타입/서비스옵션 |
| `rt_` | 레이트코드/프로모션 |
| `rsv_` | 예약/부킹/결제 |
| `fd_` | 프론트데스크 |
| `hk_` | 하우스키핑 |

**스키마 관리:** Flyway
- 마이그레이션 위치: `hola-pms/hola-app/src/main/resources/db/migration/`
- 96개 마이그레이션 파일 (V1_0_0 ~ V8_9_0)
- `out-of-order: true`, `baseline-on-migrate: true`
- 테스트 환경: `flyway.target: 5.8.0` (V5_9_0+ 대용량 데이터 제외)

**ORM:** Spring Data JPA + Hibernate
- Soft Delete: `@SQLRestriction("deleted_at IS NULL")` 전 엔티티
- Optimistic Lock: `@Version` 사용 엔티티
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/entity/MasterReservation.java`
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/entity/SubReservation.java`
  - `hola-pms/hola-reservation/src/main/java/com/hola/reservation/entity/ReservationPayment.java`
  - `hola-pms/hola-hotel/src/main/java/com/hola/hotel/entity/HkDailyAttendance.java`

### Redis 7+ (Temporary Data Store)

**연결:**
- 로컬: `localhost:6379` (`application-local.yml`)
- 테스트: 비활성화 (`spring.data.redis.repositories.enabled: false`)

**용도 (현재):**
- KICC 결제 임시 데이터: `kicc:booking:{shopOrderNo}` (TTL 30분)
  - `BookingCreateRequest` + 검증 결과 + 주문번호 + 클라이언트 정보
- KICC 결제 결과 폴링: `kicc:result:{shopOrderNo}` (TTL 10분)
  - 모바일 리다이렉트 후 결과 확인용
- 클라이언트: `StringRedisTemplate` (JSON 직렬화/역직렬화)

**용도 (미사용/준비):**
- 세션 관리 (Spring Session Redis): 설정되어 있으나 활성 사용 미확인
- 쿼리 캐싱: `@Cacheable` 사용 없음

### 파일 스토리지 (Local Filesystem)

**서비스:** `hola-pms/hola-common/src/main/java/com/hola/common/service/FileUploadService.java`
**컨트롤러:** `hola-pms/hola-common/src/main/java/com/hola/common/controller/FileUploadController.java`
**정적 리소스 매핑:** `hola-pms/hola-common/src/main/java/com/hola/common/config/WebConfig.java`

**설정:**
- 경로: `hola.upload.path` (기본 `./uploads`)
- URL 매핑: `/uploads/**` → 로컬 파일시스템
- 허용 확장자: `pdf`, `jpg`, `jpeg`, `png`, `gif`, `svg`
- 최대 파일 크기: 10MB
- 파일명: UUID 기반 (`{uuid}.{ext}`)
- 하위 디렉토리: `biz-license/`, `logo/` 등

**제한:**
- 클라우드 스토리지 미연동 (S3, GCS, Azure Blob 없음)
- 경로 탈출 방지: `target.startsWith(rootLocation)` 검증

## Authentication & Identity

### JWT (Admin API)

**필터:** `JwtAuthenticationFilter` → `/api/**` 경로
**라이브러리:** JJWT 0.12.5
**알고리즘:** HS256 (HMAC SHA-256)
**토큰:**
- Access Token: 1시간 (3,600,000ms)
- Refresh Token: 7일 (604,800,000ms)
- Secret: 환경변수 또는 설정 (최소 256비트)

### Booking API Key

**필터:** `BookingApiKeyFilter` → `/api/v1/booking/**`
**인증:** `X-API-KEY` 헤더
**저장:** `rsv_booking_api_key` 테이블 (BCrypt 해시)
**서비스:** `BookingApiKeyService` (검증 + 로깅)

### 세션 기반 (Web + HK Mobile)

**Web:** Form Login (`POST /login`) → JSESSIONID 쿠키
**HK Mobile:** 별도 세션 인증 (`HkMobileSessionFilter`)
- Session 속성: `hkUserId`, `hkUserRole`
- SecurityContext 분리 (Admin 세션과 오염 방지)

### Multi-Tenancy

**메커니즘:** HTTP 헤더 기반 스키마 라우팅
- `TenantFilter` (최고 우선순위 필터) → `X-Tenant-ID` 헤더 읽기
- `TenantContext` (ThreadLocal) → 요청 종료 시 clear
- JPA MultiTenancyStrategy.SCHEMA → 테넌트별 PostgreSQL 스키마 격리

## Monitoring & Observability

**에러 처리:**
- `GlobalExceptionHandler` — 전역 예외 핸들러 (JSON ErrorCode + HTTP Status 매핑)
- `BookingExceptionHandler` — 부킹엔진 전용 예외 핸들러
- 위치: `hola-pms/hola-common/src/main/java/com/hola/common/exception/GlobalExceptionHandler.java`

**로깅:**
- SLF4J + Logback (Spring Boot 기본)
- 로컬: `com.hola=DEBUG`, `org.hibernate.SQL=DEBUG`, `BasicBinder=TRACE`
- 프로덕션: `root=INFO`, `com.hola=INFO`
- KICC 결제: 상세 로깅 (`[KICC]` 접두사, 거래번호/금액 로깅)

**헬스체크:**
- `/actuator/health` — Spring Boot Actuator (permitAll 설정됨)
- Swagger: `/swagger-ui.html`, `/v3/api-docs`

**외부 모니터링:** 미연동 (Sentry, DataDog, New Relic 등 없음)
**메트릭:** 미연동 (Micrometer/Prometheus 없음)

## CI/CD & Deployment

**Hosting:** 미결정 (AWS/Azure/On-premise TBD)
**CI Pipeline:** 미구축 (GitHub Actions/Jenkins 없음)
**Docker:** Dockerfile 미존재 (TestContainers만 테스트용 사용)
**Deployment Model:** 단일 Spring Boot JAR (내장 Tomcat)

**빌드:**
```bash
./gradlew clean build              # JAR 패키징
# Output: hola-app/build/libs/hola-app-0.0.1-SNAPSHOT.jar
```

## Webhooks & Callbacks

**Incoming:**
- `POST /api/v1/booking/payment/return` — KICC 결제 인증 완료 콜백
  - KICC가 브라우저를 통해 POST 호출 (리다이렉트)
  - 파라미터: `resCd`, `shopOrderNo`, `authorizationId`, `resMsg`
  - 성공 시: 결제 승인 + 예약 생성 + Thymeleaf 결과 페이지 반환

**Outgoing:**
- 없음 (알림/웹훅 발송 기능 미구현)

## Environment Configuration

**필수 환경 변수 (Production):**

| 변수 | 용도 | 비고 |
|------|------|------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://host:5432/hola_pms` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | Secret |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | 기본 6379 |
| `jwt.secret` | JWT 서명 키 | 최소 256비트 |
| `KICC_SECRET_KEY` | KICC PG 시크릿키 | Secret |
| `kicc.mall-id` | KICC 상점ID | 프로덕션용 발급 필요 |
| `kicc.api-domain` | KICC API 도메인 | 운영: `https://pgapi.easypay.co.kr` |
| `kicc.return-base-url` | 결제 리턴 베이스URL | 프로덕션 도메인 |
| `hola.upload.path` | 파일 업로드 경로 | 쓰기 권한 필요 |

**시크릿 위치:**
- 로컬: `application-local.yml` (개발용 하드코딩)
- 프로덕션: 환경 변수 또는 외부 설정 관리 (Spring Cloud Config 미구축)
- `.env` 파일: 커밋되지 않음

## 비활성/미구축 통합

| 통합 | 상태 | 비고 |
|------|------|------|
| 이메일 (SMTP) | 미구현 | `JavaMailSender` 미사용 |
| SMS/알림톡 | 미구현 | 카카오톡 알림 등 미연동 |
| 채널매니저 (OTA) | 미착수 | Phase 3 계획 |
| POS 연동 | 미착수 | Phase 3 계획 |
| 정산 시스템 | 미착수 | Phase 3 계획 |
| 클라우드 스토리지 | 미구현 | 로컬 파일시스템만 |
| 로그 수집 | 미구현 | ELK/CloudWatch 미연동 |
| APM/메트릭 | 미구현 | Sentry/DataDog 미연동 |
| 스케줄링 | 미구현 | `@Scheduled` 미사용 |
| 비동기 처리 | 미구현 | `@Async` 미사용 |
| 메시지 큐 | 미구현 | Kafka/RabbitMQ 미사용 |
| WebSocket | 미구현 | 실시간 알림 없음 |

---

*Integration audit: 2026-03-26*
