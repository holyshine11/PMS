# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
cd hola-pms

# 전체 빌드
./gradlew build

# 컴파일만 (빠른 검증)
./gradlew compileJava

# 서버 실행 (로컬, http://localhost:8080)
./gradlew :hola-app:bootRun

# 다른 포트로 실행
./gradlew :hola-app:bootRun --args='--server.port=9090'

# 테스트 (현재 테스트 코드 없음 - JUnit5 + Spring Boot Test 설정만 존재)
./gradlew test

# 단일 모듈 테스트
./gradlew :hola-hotel:test

# 단일 테스트 클래스 실행
./gradlew :hola-hotel:test --tests "com.hola.hotel.SomeTest"

# 클린 빌드
./gradlew clean build
```

**Active Profile**: `local` (application.yml에서 기본 설정). 타임존: `Asia/Seoul`

**로컬 환경 요구사항**: Java 17, PostgreSQL 16 (`hola_pms` DB, user: `hola`/`hola1234`), Redis 7+
**DB Pool**: HikariCP max 10, min idle 5, timeout 30s. **파일 업로드**: multipart max 10MB

**로그인**: http://localhost:8080/login (`admin` / `holapms1!`)

## 프로젝트 개요

- **프로젝트명**: 올라(Hola) PMS - 국내 3~5성급 호텔 대상 클라우드 PMS
- **아키텍처**: Modular Monolith (Gradle 멀티모듈)
- **멀티테넌시**: Schema-per-Tenant (PostgreSQL)

## 기술 스택 (현재 구현)

| 레이어 | 기술 | 버전 |
|--------|------|------|
| Language | Java | 17 (LTS) |
| Framework | Spring Boot | 3.2.5 |
| ORM | Spring Data JPA | - |
| Database | PostgreSQL | 16 |
| Cache | Redis | 7.x |
| DB Migration | Flyway | - |
| Auth | Spring Security + JWT | - |
| Frontend (Admin) | **Thymeleaf + Bootstrap 5 + jQuery + DataTables** | - |
| CSS | 커스텀 hola.css + Pretendard 폰트 | - |

> **주의**: 기술 스택 계획서에는 React 18 + Next.js 14 + Ant Design 5가 명시되어 있으나,
> 현재 Admin UI는 Thymeleaf SSR + Bootstrap 5 + jQuery + DataTables로 구현됨.

## 아키텍처 & 모듈 구조

```
hola-pms/
├── build.gradle          # 루트: Java 17, Spring Boot 3.2.5, Lombok, JUnit5
├── settings.gradle       # 모듈 등록
├── hola-common/          # M00: 공통 (BaseEntity, HolaResponse, Security, JWT, 예외처리, 인증)
├── hola-hotel/           # M01: 호텔/프로퍼티/층/호수/마켓코드/회원관리/권한관리
├── hola-room/            # M02: 객실 클래스/타입/서비스옵션(무료/유료)
├── hola-rate/            # M03: 레이트 코드/프로모션 코드
├── hola-reservation/     # M07: 예약관리 (마스터/서브예약, 결제, 가격계산)
└── hola-app/             # 실행 모듈 (bootJar, Thymeleaf 템플릿, static, Flyway SQL)
```

### 모듈 의존성 방향

```
hola-app → hola-hotel       → hola-common
         → hola-room         → hola-common
         → hola-rate         → hola-common
         → hola-reservation  → hola-common, hola-room, hola-rate
```

- **hola-common**: `java-library` 플러그인, `api` 스코프로 전이 의존성 제공
- **hola-app**: `org.springframework.boot` 플러그인, bootJar 생성. Thymeleaf 템플릿/static/Flyway SQL 포함
- **도메인 모듈** (hotel, room, rate): Entity, Repository, Service, Controller, DTO, Mapper 포함

### 패키지 구조 (모듈별)

```
com.hola.{module}/
├── controller/           # REST API (@RestController) + View (@Controller)
├── service/              # 인터페이스 + Impl 분리
├── repository/           # Spring Data JPA
├── dto/request/          # 요청 DTO
├── dto/response/         # 응답 DTO
├── entity/               # JPA 엔티티
└── mapper/               # 수동 @Component 매퍼 (Entity <-> DTO 변환)
```

### Frontend 파일 위치 (모두 hola-app 내)

- 템플릿: `hola-app/src/main/resources/templates/{feature}/list.html`, `form.html`
- JS: `hola-app/src/main/resources/static/js/{feature}-page.js` (리스트), `{feature}-form.js` (폼)
- 공통 JS: `hola-app/src/main/resources/static/js/hola-common.js` (`HolaPms` 네임스페이스)
- CSS: `hola-app/src/main/resources/static/css/hola.css`
- 레이아웃: `templates/layout/default.html` (Thymeleaf Layout Dialect)

## 핵심 코드 패턴

### API 응답 형식

모든 REST API는 `HolaResponse<T>`로 통일:
```java
HolaResponse.success(data)          // 데이터 응답
HolaResponse.success()              // 빈 성공 응답
HolaResponse.error(code, message)   // 에러 응답
```

### 엔티티 공통 (BaseEntity)

모든 엔티티가 상속. 필드: `id` (IDENTITY), `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedAt`, `useYn`, `sortOrder`
- **Soft Delete**: `softDelete()` → `deletedAt` 설정 + `useYn = false`. 물리 삭제 금지
- **JPA Auditing**: `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` (auditorAware는 로그인 사용자 loginId, 미인증 시 "SYSTEM")
- **자동 필터링**: 모든 엔티티에 `@SQLRestriction("deleted_at IS NULL")` 적용 → 조회 시 soft-deleted 레코드 자동 제외

### 예외 처리

```java
throw new HolaException(ErrorCode.HOTEL_NOT_FOUND);  // ErrorCode enum 사용
```
에러 코드 체계: `HOLA-0xxx` 공통, `HOLA-1xxx` 호텔, `HOLA-2xxx` 객실, `HOLA-3xxx` 레이트, `HOLA-35xx` 프로모션, `HOLA-4xxx` 예약, `HOLA-06xx` 회원, `HOLA-07xx` 권한

### Mapper 패턴

MapStruct 의존성은 있으나, 실제로는 **수동 `@Component` 매퍼** 사용:
```java
@Component
public class HotelMapper {
    public Hotel toEntity(HotelCreateRequest request, ...) { ... }
    public HotelResponse toResponse(Hotel hotel) { ... }
}
```

### Security 구조 (이중 필터 체인)

- `@Order(1)` API 체인 (`/api/**`): JWT 인증, SessionCreationPolicy.NEVER
- `@Order(2)` Web 체인 (`/**`): 세션 기반, form login → `/login`, 성공 → `/admin/dashboard`
- 역할: `SUPER_ADMIN`, `HOTEL_ADMIN`, `PROPERTY_ADMIN`
- 컨텍스트: SUPER_ADMIN은 호텔/프로퍼티 모두 선택 가능, HOTEL_ADMIN은 프로퍼티만 선택, PROPERTY_ADMIN은 자동 고정
- CSRF: 전역 비활성화, Frame Options: sameOrigin

### 인가 패턴 (AccessControlService)

컨트롤러에서 `AccessControlService`를 주입받아 리소스 접근 권한 검증:
```java
accessControlService.validatePropertyAccess(propertyId);  // SUPER_ADMIN은 bypass, 나머지는 매핑 확인
accessControlService.validateHotelAccess(hotelId);
```
- `getCurrentUser()`: SecurityContext에서 AdminUser 조회
- 계정 잠금: 로그인 5회 실패 시 accountLocked = true

### DataTable Ajax 패턴 (Frontend)

프로퍼티 의존 페이지에서는 function 기반 ajax:
```javascript
ajax: function(data, callback) {
    var propertyId = HolaPms.context.getPropertyId();
    if (!propertyId) { callback({ data: [] }); return; }
    $.ajax({ url: '/api/v1/...', success: function(res) { callback(res); } });
}
```

### HolaPms 네임스페이스 주요 API (hola-common.js)

- `HolaPms.ajax(options)`: $.ajax 래퍼 (JSON 기본)
- `HolaPms.alert(type, message)`: Bootstrap Toast (최대 3개, 1초 자동닫힘)
- `HolaPms.alertAndRedirect(type, message, url)`: sessionStorage 플래시 알림 후 리다이렉트
- `HolaPms.context`: 호텔/프로퍼티 선택 컨텍스트 (sessionStorage 기반, `hola:contextChange` 이벤트 발행)
- `HolaPms.requireContext(type)`: 호텔/프로퍼티 선택 필수 검증 (미선택 시 alert 표시)
- `HolaPms.renders.*`: DataTable 렌더러 (dashIfEmpty, useYnBadge, countBadge, actionButtons). XSS 방지를 위해 `escapeHtml()` 적용
- `HolaPms.reservationStatus`: 예약 상태 매핑 (RESERVED, CHECK_IN, INHOUSE, CHECKED_OUT, CANCELED, NO_SHOW) + 뱃지 스타일
- `HolaPms.modal.show/hide()`: Bootstrap Modal 인스턴스 재사용 + 포커스 관리
- `HolaPms.form.val(selector)`: 빈 문자열 → null 변환
- `HolaPms.form.intVal(selector)`: 정수 변환
- `HolaPms.bindDateRange(startSel, endSel)`: 날짜 범위 min/max 연동

## 코드 컨벤션

### 네이밍
- 변수/필드: `camelCase`, 클래스: `PascalCase`, 상수: `UPPER_SNAKE_CASE`
- DB 테이블: `snake_case` (접두사: `htl_`, `rm_`, `rt_` 등), DB 컬럼: `snake_case`

### API 설계
- RESTful: `/api/v1/{resource}` (프로퍼티 소속: `/api/v1/properties/{propertyId}/{resource}`)
- JWT Bearer Token 인증
- 페이징: `page`, `size`, `sort` 파라미터 표준
- HTTP: 200(성공), 201(생성), 400(검증실패), 401(인증), 403(인가), 404(없음), 500(서버오류)

### Git
- 커밋: `[HOLA-XXX] feat/fix/refactor: description` (영문)
- 코드 주석: 한글

### Flyway 마이그레이션
- 위치: `hola-app/src/main/resources/db/migration/`
- 네이밍: `V{major}_{minor}_{patch}__{description}.sql` (예: `V2_1_0__create_room_type_tables.sql`)
- 모듈별 버전 대역: V1.x.x (호텔/공통), V2.x.x (객실), V3.x.x (레이트), V4.x.x (예약), V5.x.x (테스트 데이터)
- 최신 버전: V5.13.0
- Flyway 설정: out-of-order 마이그레이션 활성화

## UI 공통 규칙 (Admin Frontend)

### 테이블/리스트
- Bootstrap 5 + DataTables, 카드 래핑: `card border-0 shadow-sm`
- DataTable 초기화: `$.extend({}, HolaPms.dataTableDefaults, {...})`
- 개별 language 정의 금지 → `HolaPms.dataTableDefaults` 사용
- pageSizeSelect: 10, 20(기본), 50, 100 한글 ("10개씩 보기" 등)
- 렌더러: `HolaPms.renders.dashIfEmpty`, `HolaPms.renders.useYnBadge`, `HolaPms.renders.actionButtons`

### 폼 (등록/수정/상세)
- Bootstrap grid 필수 (`row mb-3` + `col-sm-2 col-form-label`), `table table-bordered` 금지
- 필수 항목: label에 `required` 클래스
- 버튼: `d-flex justify-content-between` — 왼쪽: 삭제(수정모드만), 오른쪽: 취소+저장
- 취소 아이콘: `fa-arrow-left`
- **fw-bold 제한**: 페이지 타이틀(`h4`), 섹션 헤더(`h6`)만 허용. 폼 라벨/데이터 span에는 금지

### 컬러 테마
- 5색: #051923, #003554, #0582CA, #EF476F, #000/#FFF + gray(secondary)
- 폰트: Pretendard (CDN)

## 알려진 기술적 함정

### JPQL null 조건부 LIKE 금지
PostgreSQL + Hibernate 6에서 `(:param IS NULL OR column LIKE ...)` 패턴 사용 시 bytea 캐스팅 에러 발생.
→ **해결**: Java stream 필터링 또는 Specification 사용

### 벌크 삭제 시 @Modifying @Query 필수
매핑 테이블(N:M)에서 `deleteAllByXxx()` 파생 삭제 후 `saveAll()` 시 flush 순서 충돌로 500 에러.
→ **해결**: 반드시 `@Modifying @Query("DELETE FROM ...")` 사용

### SecurityConfig 매칭 순서
`requestMatchers`는 구체적 경로를 제너릭 경로보다 먼저 선언해야 함.
(예: `/api/v1/hotels/selector`를 `/api/v1/hotels/**`보다 먼저)

### 예약 모듈 아키텍처 (M07)

- **Master/Sub 예약 구조**: 단일 마스터 예약에 복수 서브예약 (객실타입 + 날짜 범위 별)
- **일자별 요금**: DailyCharge 엔티티로 각 일자/객실/요금 조합 분리 관리
- **결제 조정**: PaymentAdjustment 엔티티로 결제 수정 이력 관리
- **가격 계산**: PriceCalculationService에서 레이트코드/프로모션 기반 계산
- **객실 가용성**: RoomAvailabilityService에서 재고 확인
- **예약번호 생성**: ReservationNumberGenerator (시퀀스 기반)
- **조기/지연 수수료**: EarlyLateCheckService에서 별도 정책 엔티티 기반 계산

### 공통 유틸리티

- **FileUploadService**: 로컬 파일 업로드 (UUID 네이밍, 경로 탐색 방지). 허용 확장자: pdf, jpg, jpeg, png, gif, svg. 최대 10MB
- **NameMaskingUtil**: 한국어 이름 마스킹 (2자: 김*, 3자: 김*수, 4자+: 김**수)

## 모듈별 API Base Path

| 모듈 | Base Path |
|------|-----------|
| M01 호텔관리 | `/api/v1/hotels`, `/api/v1/properties` |
| M02 객실관리 | `/api/v1/properties/{id}/room-classes`, `room-types`, `free-service-options`, `paid-service-options` |
| M03 레이트관리 | `/api/v1/properties/{id}/rate-codes`, `promotion-codes` |
| M07 예약관리 | `/api/v1/properties/{id}/reservations`, `reservation-channels`, `reservations/calendar` |
| 공통 선택자 | `/api/v1/hotels/selector`, `/api/v1/properties/selector` |
| 객실 가용성 | `/api/v1/properties/{id}/room-numbers/availability` |
| 인증 | `/api/v1/auth/login` |

## 설계 자산 참조

| 문서 | 경로 |
|------|------|
| IA v1.0 | `설계/01.IA설계/올라 PMS 구축_IA_V1.0.xlsx` |
| 공통 모듈 설계 | `설계/02.화면설계/01_올라 PMS_V1.0_공통.pptx` |
| 호텔관리 설계 | `설계/02.화면설계/02_올라 PMS_V1.0_호텔관리.pptx` |
| 회원관리 설계 | `설계/02.화면설계/03_올라 PMS_V1.0_회원관리.pptx` |
| 권한관리 설계 | `설계/02.화면설계/04_올라 PMS_V1.0_권한관리.pptx` |
| 객실관리 설계 | `설계/02.화면설계/05_올라 PMS_V1.0_객실관리.pptx` |
| 예약관리 설계 | `설계/02.화면설계/06_올라 PMS_V1.0_예약관리.pptx` |
| 시스템관리 설계 | `설계/02.화면설계/07_올라 PMS_V1.0_시스템 관리.pptx` |
| 코드체계 | `설계/99.기타자료/코드관리_v0.1.xlsx` |
| 논리 ERD | `설계/99.기타자료/bwpms-논리.pdf` |

## 개발 Phase & 모듈 우선순위

| Phase | 모듈 |
|-------|------|
| Phase 0 (현재) | 기반 구축: skeleton, 인증, Admin UI |
| Phase 1 | **P0 코어**: M01 호텔, M02 객실, M03 레이트, M07 예약, M10 프론트데스크 |
| Phase 2 | **P1 운영**: M04 하우스키핑, M09 정산, M12 대시보드 |
| Phase 3 | **P2 고도화**: M05 채널, M06 POS, M08 부킹엔진 |

## 보안 규칙

- JWT: Access 1h + Refresh 7d
- 비밀번호: 10~20자, 영문+숫자+특수문자, 5회 실패 시 잠금
- Soft Delete 필수 (물리 삭제 금지)
- 감사 로그: BaseEntity의 `createdBy`/`updatedBy` 자동 기록

## 모델 라우팅 규칙 (토큰 효율화)

Agent 도구로 서브에이전트를 생성할 때 반드시 작업 복잡도에 따라 `model` 파라미터를 지정한다.
품질이 중요한 작업은 반드시 opus를 사용하고, 단순 작업만 하위 모델에 위임한다.

| 모델 | 용도 | 예시 |
|------|------|------|
| `haiku` | 파일 읽기, 단순 검색, 패턴 매칭 | Explore 에이전트 (quick), Glob/Grep 래핑, 파일 내용 요약 |
| `sonnet` | 단순 반복 코드 생성, 포맷팅, 리팩토링 | 테스트 코드 반복 생성, DTO/Entity 변환, 단순 CRUD 구현 |
| `opus` | 복잡한 설계/계획, 핵심 비즈니스 로직, 아키텍처 결정 | Plan 에이전트, 복잡한 서비스 구현, 디버깅, 코드 리뷰 |

### 라우팅 기준
- **기본값은 opus** (명시하지 않으면 opus 사용)
- haiku: 결과의 정확성이 덜 중요하고, 단순 조회/검색만 필요한 경우
- sonnet: 패턴이 명확하고 반복적인 코드 생성, 기존 코드 참고하여 유사 코드 작성
- opus: 판단이 필요한 모든 작업 (설계, 구현, 리뷰, 디버깅, 계획)
