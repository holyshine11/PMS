# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
cd hola-pms
./gradlew build                    # 전체 빌드
./gradlew compileJava              # 컴파일만 (빠른 검증)
./gradlew :hola-app:bootRun        # 서버 실행 (http://localhost:8080)
./gradlew clean build              # 클린 빌드
./gradlew :hola-hotel:test --tests "com.hola.hotel.SomeTest"  # 단일 테스트
./gradlew test                     # 전체 테스트 (TestContainers PostgreSQL 자동 실행)
```

**환경**: Java 17, PostgreSQL 16 (`hola_pms` DB, `hola`/`hola1234`), Redis 7+, Profile: `local`, TZ: `Asia/Seoul`
**로그인**: http://localhost:8080/login (`admin` / `holapms1!`)
**Swagger**: http://localhost:8080/swagger-ui.html

## 프로젝트 개요

올라(Hola) PMS — 국내 3~5성급 호텔 클라우드 PMS. Modular Monolith, Schema-per-Tenant(PostgreSQL).
**스택**: Java 17 + Spring Boot 3.2.5 + JPA + Flyway + Spring Security(JWT) + Thymeleaf(Layout Dialect) + Bootstrap 5.3 + jQuery 3.7 + DataTables 1.13

## 모듈 구조

```
hola-pms/
├── hola-common/       # M00: BaseEntity, HolaResponse, Security, JWT, 예외처리, 인증, TenantFilter
├── hola-hotel/        # M01: 호텔/프로퍼티/회원/권한 + M04: 하우스키핑(Hk* 접두사)
├── hola-room/         # M02: 객실클래스/타입/서비스옵션 + 트랜잭션코드/재고
├── hola-rate/         # M03: 레이트코드/프로모션코드
├── hola-reservation/  # M07: 예약 + M10: 프론트데스크(FrontDesk*) + M08: 부킹엔진(booking/)
├── hola-app/          # 실행 모듈 (Thymeleaf, static, Flyway SQL)
└── e2e-tests/         # E2E 테스트
```

**의존성**: hola-app → hotel/room/rate/reservation → hola-common. reservation → room, rate도 참조.

## 계층 아키텍처 패턴

모든 도메인 모듈은 동일한 계층 구조를 따름:

```
Controller (API/View 분리)
├── XxxApiController      — @RestController, /api/v1/...
└── XxxViewController     — @Controller, Thymeleaf 뷰 반환
Service (인터페이스 + Impl)
├── XxxService            — 인터페이스
└── XxxServiceImpl        — @Transactional(readOnly=true) 클래스 수준, 쓰기만 @Transactional
Repository
└── XxxRepository         — JpaRepository<Xxx, Long>
Entity
└── Xxx extends BaseEntity — @SQLRestriction, 비즈니스 메서드 내부 캡슐화
DTO
├── dto/request/          — CreateRequest, UpdateRequest
└── dto/response/         — Response (@Builder)
Mapper
└── XxxMapper (@Component) — 수동 toEntity()/toResponse() 변환 (MapStruct 미사용)
```

**크로스 모듈 FK 원칙**: 다른 모듈 엔티티는 `@ManyToOne` 대신 `@Column(name="xxx_id") private Long xxxId`로 모듈 간 JPA 의존성 차단

## 핵심 규칙 (코드 작성 시 반드시 준수)

### BaseEntity & Soft Delete
- 모든 엔티티는 BaseEntity 상속: `id`, `createdAt/By`, `updatedAt/By`, `deletedAt`, `useYn`, `sortOrder`
- **물리 삭제 금지** → `softDelete()` 사용 (`deletedAt` + `useYn=false`)
- `@SQLRestriction("deleted_at IS NULL")` 자동 적용 → soft-deleted 레코드 자동 제외
- 동시성 제어가 필요한 엔티티는 `@Version Long version` 낙관적 락 사용 (MasterReservation, SubReservation 등)

### API 패턴
- 모든 응답: `HolaResponse.success(data)` / `HolaResponse.error(code, message)`
- 페이징: `HolaResponse.success(data, PageInfo.from(page))`
- 예외: `throw new HolaException(ErrorCode.XXX)` (ErrorCode enum 참조)
- RESTful: `/api/v1/{resource}`, 프로퍼티 소속: `/api/v1/properties/{propertyId}/{resource}`

### ErrorCode 코드 체계
- `HOLA-0xxx`: 공통, `HOLA-06xx`: 회원, `HOLA-07xx`: 권한, `HOLA-08xx`: 비밀번호
- `HOLA-1xxx`: 호텔, `HOLA-2xxx`: 객실, `HOLA-25xx`: TC, `HOLA-26xx`: 재고
- `HOLA-3xxx`: 레이트, `HOLA-4xxx`: 예약/부킹/결제, `HOLA-5xxx`: 프론트데스크, `HOLA-8xxx`: HK

### Security (4단 필터 체인)

| Order | 대상 | 인증 방식 |
|-------|------|-----------|
| 0 | `/api/v1/booking/**` | BookingApiKeyFilter (API-KEY 헤더, Stateless) |
| 1 | `/api/v1/properties/*/hk-mobile/**` | 세션 기반 (IF_REQUIRED), `sessionFixation().none()` |
| 2 | `/api/**` | JWT (SessionCreationPolicy.NEVER) |
| 3 | `/**` | 세션 기반 form login |

- 역할 5종: `SUPER_ADMIN`, `HOTEL_ADMIN`, `PROPERTY_ADMIN`, `HOUSEKEEPING_SUPERVISOR`, `HOUSEKEEPER`
- 인가: `accessControlService.validatePropertyAccess(propertyId)` 사용
- JWT: Access 1h + Refresh 7d. 비밀번호: 10~20자, 5회 실패 시 잠금
- HK 모바일: JWT 아닌 세션 기반. session attribute `hkUserId`, `hkUserRole`
- 멀티테넌시: `TenantFilter`가 `X-Tenant-ID` 헤더 → `TenantContext`(ThreadLocal) 설정

### 코드 컨벤션
- 네이밍: `camelCase`(변수) / `PascalCase`(클래스) / `UPPER_SNAKE_CASE`(상수)
- DB: `snake_case` 테이블(접두사: `htl_`, `rm_`, `rt_`, `rsv_`, `fd_`, `hk_`), `snake_case` 컬럼
- Git: `[HOLA-XXX] feat/fix/refactor: description` (영문). 코드 주석: 한글
- Flyway: `hola-app/src/main/resources/db/migration/`, `V{major}_{minor}_{patch}__{desc}.sql`, out-of-order 활성화
  - 버전대역: V1(호텔) V2(객실) V3(레이트) V4(예약/부킹/결제) V5(테스트데이터) V6(트랜잭션) V7(객실상태) V8(하우스키핑)
  - 최신: V8_10_0 (stayover cleaning policy). 다음 결제 마이그레이션은 V4_21_0 대역
- DB 시퀀스 자동 생성: `SELECT nextval('htl_hotel_code_seq')` → `HTL00001` 형식

## UI 규칙 (Admin Frontend)

### Thymeleaf 레이아웃
- 3종 레이아웃: `layout/default.html`(Admin), `layout/booking.html`(부킹엔진), `layout/mobile.html`(HK 모바일)
- Fragment 슬롯: `layout:fragment="content"`, `layout:fragment="scripts"`
- 팝업 모드: `?mode=popup` → `body.popup-mode` 클래스 추가

### HolaPms 네임스페이스 (hola-common.js)
- `HolaPms.ajax(options)` — jQuery AJAX 래퍼 (JSON 자동 직렬화)
- `HolaPms.alert(type, message)` — Bootstrap Toast (에러 3초, 성공 1.5초, 최대 3개)
- `HolaPms.alertAndRedirect(url, type, msg)` — sessionStorage Flash 메시지 + 리다이렉트
- `HolaPms.modal.show/hide(id)` — Bootstrap Modal 인스턴스 재사용
- `HolaPms.context` — sessionStorage 기반 hotelId/propertyId 관리, `hola:contextChange` 커스텀 이벤트
- `HolaPms.requireContext(type)` — 프로퍼티 미선택 시 toast 안내
- `HolaPms.maskName()`, `HolaPms.maskPhone()` — PII 마스킹 (리스트 페이지 필수)
- `HolaPms.reservationStatus` — 상태별 badge 스타일 중앙 관리
- `HolaPms.renders.*` — DataTable 렌더러 (dashIfEmpty, useYnBadge, countBadge, actionButtons)
- `HolaPms.dataTableDefaults` — 한글 언어팩 + pageSizeSelect(10/20/50/100)

### 프로퍼티 컨텍스트 페이지 필수 패턴
프로퍼티 의존 페이지는 반드시 이 패턴을 따를 것 (3회 버그 발생 이력):
1. HTML: `<div id="contextAlert" class="alert alert-danger d-none">` 배치
2. `init()`은 **무조건 호출** → `bindEvents()` + `reload()` 실행
3. `reload()`에서 `HolaPms.context.getPropertyId()` 체크 → 없으면 alert 표시
4. `hola:contextChange` 이벤트에서 `self.reload()` 호출
5. **금지**: `init()`을 propertyId 조건부로 호출하면 `bindEvents()`가 실행 안 됨 → `contextChange` 리스너 미등록

### UI 스타일 규칙
- DataTable: `$.extend({}, HolaPms.dataTableDefaults, {...})` — 개별 language 정의 금지
- 카드: `card border-0 shadow-sm`
- 폼: Bootstrap grid (`row mb-3` + `col-sm-2 col-form-label`), `table table-bordered` 금지
- 버튼: `d-flex justify-content-between` — 왼쪽: 삭제(수정만), 오른쪽: 취소(`fa-arrow-left`)+저장
- **fw-bold**: 페이지 타이틀(`h4`), 섹션 헤더(`h6`)만 허용. 폼 라벨/데이터에 금지
- 컬러: #051923, #003554, #0582CA, #EF476F, #000/#FFF + gray. 폰트: Pretendard
- JS 파일 명명: `{도메인}-page.js`(리스트), `{도메인}-form.js`(폼/상세)

## 테스트

### 단위 테스트 (각 도메인 모듈)
- `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` / `@Mock`
- `@Nested` + `@DisplayName`으로 구조화, AssertJ 사용

### 통합 테스트 (hola-app)
- `BaseIntegrationTest` 상속: `@SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("test") + @Transactional + @WithMockUser(roles=SUPER_ADMIN)`
- TestContainers PostgreSQL 16 자동 관리 (`jdbc:tc:postgresql:16-alpine:///hola_pms_test`)
- `TestFixtures` 클래스로 테스트 픽스처 생성
- `application-test.yml`의 `flyway.target: 5.8.0` → V5_9_0+ 대용량 테스트 데이터 마이그레이션 제외

## JPA 핵심 설정
- `open-in-view: false` — 뷰 렌더링 중 지연 로딩 불가, 서비스 레이어에서 필요 데이터 fetch 필수
- `default_batch_fetch_size: 100` — `@ManyToOne`/`@OneToMany` IN 쿼리 배치

## 기술적 함정 (반드시 숙지)

1. **JPQL null 파라미터 금지**: `(:param IS NULL OR column LIKE/=/ ...)` → bytea 캐스팅 에러. **해결**: Specification 또는 Java stream 필터링
2. **벌크 삭제**: 매핑 테이블에서 `deleteAllByXxx()` 금지 → `@Modifying @Query("DELETE FROM ...")` 필수
3. **orphanRemoval + JPQL DELETE 금지**: `collection.clear()` + `flush()` 방식만 사용
4. **SecurityConfig 순서**: 구체적 경로(`/api/v1/hotels/selector`)를 제너릭(`/api/v1/hotels/**`)보다 먼저
5. **HK 모바일 SecurityContext 오염**: HkMobileSessionFilter에서 try/finally 원본 복원 필수, `sessionFixation().none()`, accessControlService 우회 필수
6. **스케줄러 내부 호출 시 auth 우회**: `@Scheduled` 메서드는 SecurityContext 없이 실행됨. 내부 서비스 호출 시 `accessControlService.validatePropertyAccess()` 가 있으면 런타임 예외 발생. 스케줄러 경로용 내부 메서드 분리 필요

## 개발 현황

| 영역 | 상태 | 비고 |
|------|------|------|
| 기반+코어 (호텔/객실/레이트/예약/프론트데스크) | 완료 | |
| 하우스키핑 (관리자+모바일, 태스크·근태·휴무) | 완료 | |
| 투숙중 청소 관리 (Stayover/DND 정책) | 완료 | |
| 부킹엔진 + KICC PG 결제 | 완료 | |
| 간편결제 (빌키) | 컨텍스트 수집 완료 | `.planning/phases/02-easy-payment/` 참조 |
| 대시보드 | 완료 | N+1 성능 이슈 존재 |
| Dayuse(대실) | 완료 | |
| 타임라인 뷰 / Roomrack HK 매핑 | 완료 | |
| 고도화 (채널/POS/정산) | 미착수 | |

## 설계 자산

`설계/` 디렉토리: IA설계(`01.IA설계/`), 화면설계(`02.화면설계/` — 공통~시스템관리 7종), ERD(`99.기타자료/bwpms-논리.pdf`), 코드체계(`99.기타자료/코드관리_v0.1.xlsx`)

## 모델 라우팅 (서브에이전트)

| 모델 | 기준 |
|------|------|
| `haiku` | 단순 검색, 파일 읽기, 패턴 매칭 |
| `sonnet` | 반복 코드 생성, DTO/Entity 변환, 단순 CRUD |
| `opus` | 설계/계획, 비즈니스 로직, 디버깅, 코드 리뷰 (기본값) |
