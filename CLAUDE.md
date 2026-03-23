# CLAUDE.md

## Build & Run

```bash
cd hola-pms
./gradlew build                    # 전체 빌드
./gradlew compileJava              # 컴파일만 (빠른 검증)
./gradlew :hola-app:bootRun        # 서버 실행 (http://localhost:8080)
./gradlew clean build              # 클린 빌드
./gradlew :hola-hotel:test --tests "com.hola.hotel.SomeTest"  # 단일 테스트
```

**환경**: Java 17, PostgreSQL 16 (`hola_pms` DB, `hola`/`hola1234`), Redis 7+, Profile: `local`, TZ: `Asia/Seoul`
**로그인**: http://localhost:8080/login (`admin` / `holapms1!`)

## 프로젝트 개요

올라(Hola) PMS — 국내 3~5성급 호텔 클라우드 PMS. Modular Monolith, Schema-per-Tenant(PostgreSQL).
**스택**: Java 17 + Spring Boot 3.2.5 + JPA + Flyway + Spring Security(JWT) + Thymeleaf + Bootstrap 5 + jQuery + DataTables

## 모듈 구조

```
hola-pms/
├── hola-common/       # M00: BaseEntity, HolaResponse, Security, JWT, 예외처리, 인증
├── hola-hotel/        # M01: 호텔/프로퍼티/회원/권한 + M04: 하우스키핑(Hk* 접두사)
├── hola-room/         # M02: 객실클래스/타입/서비스옵션 + 트랜잭션코드/재고
├── hola-rate/         # M03: 레이트코드/프로모션코드
├── hola-reservation/  # M07: 예약 + M10: 프론트데스크(FrontDesk*) + M08: 부킹엔진(booking/)
├── hola-app/          # 실행 모듈 (Thymeleaf, static, Flyway SQL)
└── e2e-tests/         # E2E 테스트
```

**의존성**: hola-app → hotel/room/rate/reservation → hola-common. reservation → room, rate도 참조.

## 핵심 규칙 (코드 작성 시 반드시 준수)

### BaseEntity & Soft Delete
- 모든 엔티티는 BaseEntity 상속: `id`, `createdAt/By`, `updatedAt/By`, `deletedAt`, `useYn`, `sortOrder`
- **물리 삭제 금지** → `softDelete()` 사용 (`deletedAt` + `useYn=false`)
- `@SQLRestriction("deleted_at IS NULL")` 자동 적용 → soft-deleted 레코드 자동 제외

### API 패턴
- 모든 응답: `HolaResponse.success(data)` / `HolaResponse.error(code, message)`
- 예외: `throw new HolaException(ErrorCode.XXX)` (ErrorCode enum 참조)
- RESTful: `/api/v1/{resource}`, 프로퍼티 소속: `/api/v1/properties/{propertyId}/{resource}`
- Mapper: 수동 `@Component` 매퍼 (MapStruct 미사용)

### Security (이중 필터 체인)
- `@Order(1)` API (`/api/**`): JWT, SessionCreationPolicy.NEVER
- `@Order(2)` Web (`/**`): 세션 기반, form login
- 역할: `SUPER_ADMIN`, `HOTEL_ADMIN`, `PROPERTY_ADMIN`
- 인가: `accessControlService.validatePropertyAccess(propertyId)` 사용
- JWT: Access 1h + Refresh 7d. 비밀번호: 10~20자, 5회 실패 시 잠금

### 코드 컨벤션
- 네이밍: `camelCase`(변수) / `PascalCase`(클래스) / `UPPER_SNAKE_CASE`(상수)
- DB: `snake_case` 테이블(접두사: `htl_`, `rm_`, `rt_`), `snake_case` 컬럼
- Git: `[HOLA-XXX] feat/fix/refactor: description` (영문). 코드 주석: 한글
- Flyway: `hola-app/src/main/resources/db/migration/`, `V{major}_{minor}_{patch}__{desc}.sql`, out-of-order 활성화
  - 버전대역: V1(호텔) V2(객실) V3(레이트) V4(예약) V5(테스트) V6(트랜잭션) V7(객실상태) V8(하우스키핑)

## UI 규칙 (Admin Frontend)

- DataTable: `$.extend({}, HolaPms.dataTableDefaults, {...})` — 개별 language 정의 금지
- 카드: `card border-0 shadow-sm`. 렌더러: `HolaPms.renders.*`
- 폼: Bootstrap grid (`row mb-3` + `col-sm-2 col-form-label`), `table table-bordered` 금지
- 버튼: `d-flex justify-content-between` — 왼쪽: 삭제(수정만), 오른쪽: 취소(`fa-arrow-left`)+저장
- **fw-bold**: 페이지 타이틀(`h4`), 섹션 헤더(`h6`)만 허용. 폼 라벨/데이터에 금지
- 컬러: #051923, #003554, #0582CA, #EF476F, #000/#FFF + gray. 폰트: Pretendard

## 기술적 함정 (반드시 숙지)

1. **JPQL null 파라미터 금지**: `(:param IS NULL OR column LIKE/=/ ...)` → bytea 캐스팅 에러. **해결**: Specification 또는 Java stream 필터링
2. **벌크 삭제**: 매핑 테이블에서 `deleteAllByXxx()` 금지 → `@Modifying @Query("DELETE FROM ...")` 필수
3. **orphanRemoval + JPQL DELETE 금지**: `collection.clear()` + `flush()` 방식만 사용
4. **SecurityConfig 순서**: 구체적 경로(`/api/v1/hotels/selector`)를 제너릭(`/api/v1/hotels/**`)보다 먼저
5. **HK 모바일 SecurityContext 오염**: HkMobileSessionFilter에서 try/finally 원본 복원 필수, `sessionFixation().none()`, accessControlService 우회 필수

## 개발 현황

| Phase | 상태 |
|-------|------|
| Phase 0~1: 기반+코어 (호텔/객실/레이트/예약/프론트데스크) | ✅ 완료 |
| Phase 2: 운영 (하우스키핑/부킹엔진/대시보드) | ✅ 대부분 완료 |
| Phase 3: 고도화 (채널/POS/정산) | 미착수 |

## 설계 자산

`설계/` 디렉토리: IA설계(`01.IA설계/`), 화면설계(`02.화면설계/` — 공통~시스템관리 7종), ERD(`99.기타자료/bwpms-논리.pdf`), 코드체계(`99.기타자료/코드관리_v0.1.xlsx`)

## 모델 라우팅 (서브에이전트)

| 모델 | 기준 |
|------|------|
| `haiku` | 단순 검색, 파일 읽기, 패턴 매칭 |
| `sonnet` | 반복 코드 생성, DTO/Entity 변환, 단순 CRUD |
| `opus` | 설계/계획, 비즈니스 로직, 디버깅, 코드 리뷰 (기본값) |
