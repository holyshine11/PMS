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

# 테스트
./gradlew test

# 단일 모듈 테스트
./gradlew :hola-hotel:test

# 클린 빌드
./gradlew clean build
```

**로컬 환경 요구사항**: Java 17, PostgreSQL 16 (`hola_pms` DB, user: `hola`/`hola1234`), Redis 7+

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
├── hola-rate/            # M03: 레이트 코드/요금
└── hola-app/             # 실행 모듈 (bootJar, Thymeleaf 템플릿, static, Flyway SQL)
```

### 모듈 의존성 방향

```
hola-app → hola-hotel → hola-common
         → hola-room  → hola-common
         → hola-rate  → hola-common
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
- **JPA Auditing**: `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`

### 예외 처리

```java
throw new HolaException(ErrorCode.HOTEL_NOT_FOUND);  // ErrorCode enum 사용
```
에러 코드 체계: `HOLA-0xxx` 공통, `HOLA-1xxx` 호텔, `HOLA-2xxx` 객실, `HOLA-3xxx` 레이트, `HOLA-06xx` 회원, `HOLA-07xx` 권한

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

### DataTable Ajax 패턴 (Frontend)

프로퍼티 의존 페이지에서는 function 기반 ajax:
```javascript
ajax: function(data, callback) {
    var propertyId = HolaPms.context.getPropertyId();
    if (!propertyId) { callback({ data: [] }); return; }
    $.ajax({ url: '/api/v1/...', success: function(res) { callback(res); } });
}
```

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
- 모듈별 버전 대역: V1.x.x (호텔/공통), V2.x.x (객실), V3.x.x (레이트)

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

## 모듈별 API Base Path

| 모듈 | Base Path |
|------|-----------|
| M01 호텔관리 | `/api/v1/hotels`, `/api/v1/properties` |
| M02 객실관리 | `/api/v1/properties/{id}/room-classes`, `room-types`, `free-service-options`, `paid-service-options` |
| M03 레이트관리 | `/api/v1/properties/{id}/rate-codes` |

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
