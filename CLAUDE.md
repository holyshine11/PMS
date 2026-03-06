# Hola PMS - 프로젝트 컨텍스트

## 프로젝트 개요
- **프로젝트명**: 올라(Hola) PMS
- **비전**: 국내 3~5성급 중견 호텔 대상 Oracle Opera Cloud PMS 한국형 대안
- **아키텍처**: Modular Monolith (Phase 4+ MSA 전환)
- **멀티테넌시**: Schema-per-Tenant (PostgreSQL)

## 기술 스택

| 레이어 | 기술 | 버전 |
|--------|------|------|
| Language | Java | 17 (LTS) |
| Framework | Spring Boot | 3.2.x |
| ORM | Spring Data JPA + QueryDSL | 5.0+ |
| Database | PostgreSQL | 16 |
| Cache | Redis | 7.x |
| Message Queue | RabbitMQ | 3.13+ |
| API Gateway | Spring Cloud Gateway | 4.x |
| Frontend | React 18 + Next.js | 14 (App Router) |
| UI Library | Ant Design | 5.x |
| Auth | Spring Security + JWT + OAuth2 | - |
| Cloud | AWS (ECS Fargate + RDS + ElastiCache) | - |
| CI/CD | GitHub Actions + Docker | - |
| Monitoring | Grafana + Prometheus + Loki | - |
| DB Migration | Flyway | - |

## 프로젝트 구조 (Gradle 멀티모듈)

```
hola-pms/
├── hola-common/          # M00: 공통 (코드, 유틸, 보안, 예외처리)
├── hola-hotel/           # M01: 호텔/프로퍼티/층/호수/마켓코드
├── hola-room/            # M02: 객실 클래스/타입/서비스옵션
├── hola-rate/            # M03: 레이트/프로모션
├── hola-housekeeping/    # M04: 하우스키핑/객실상태
├── hola-channel/         # M05: 채널매니저/OTA 연동
├── hola-pos/             # M06: POS 매출/부대시설
├── hola-reservation/     # M07: 예약/체크인/체크아웃
├── hola-booking/         # M08: 부킹엔진 API
├── hola-billing/         # M09: 정산/Night Audit
├── hola-frontdesk/       # M10: 프론트데스크/Room Rack
├── hola-ai/              # M11: AI 어시스턴트
├── hola-report/          # M12: 대시보드/KPI 리포팅
├── hola-notification/    # 알림 (카카오/이메일/SMS)
└── hola-gateway/         # API Gateway
```

## 패키지 구조 (모듈별)

```
com.hola.{module}/
├── controller/           # REST API 엔드포인트
│   └── {Entity}Controller.java
├── service/              # 비즈니스 로직
│   ├── {Entity}Service.java (인터페이스)
│   └── {Entity}ServiceImpl.java
├── repository/           # 데이터 접근
│   └── {Entity}Repository.java
├── dto/                  # 요청/응답 DTO
│   ├── request/
│   └── response/
├── entity/               # JPA 엔티티
│   └── {Entity}.java
├── mapper/               # Entity <-> DTO 변환 (MapStruct)
├── event/                # 도메인 이벤트 (발행/수신)
└── exception/            # 모듈별 예외
```

## 코드 컨벤션

### 네이밍
- 변수/필드: `camelCase`
- 클래스/인터페이스: `PascalCase`
- 상수: `UPPER_SNAKE_CASE`
- 패키지: `lowercase`
- DB 테이블: `snake_case` (접두사: `htl_`, `rm_`, `rsv_` 등)
- DB 컬럼: `snake_case`

### API 설계 규칙
- RESTful 리소스 중심: `/api/v1/{resource}`
- JWT Bearer Token + `X-Tenant-ID` 헤더 필수
- 통일된 JSON 응답: `HolaResponse<T>`
  ```json
  {
    "success": true,
    "code": "HOLA-0000",
    "message": "성공",
    "data": {},
    "pagination": { "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 },
    "timestamp": "2026-03-03T00:00:00Z"
  }
  ```
- 에러 코드: `HOLA-XXXX` 형식
- 페이징: `page`, `size`, `sort` 파라미터 표준
- HTTP 상태: 200(성공), 201(생성), 400(검증실패), 401(인증), 403(인가), 404(없음), 500(서버오류)

### 예외 처리
- 공통 예외: `HolaException` + `ErrorCode` enum
- `@RestControllerAdvice`로 전역 처리
- 에러 코드 체계:
  - `HOLA-0xxx`: 공통
  - `HOLA-1xxx`: 호텔/프로퍼티
  - `HOLA-2xxx`: 객실
  - `HOLA-3xxx`: 레이트
  - `HOLA-7xxx`: 예약

### 엔티티 공통
- `BaseEntity`: `createdAt`, `updatedAt`, `createdBy`, `updatedBy` (JPA Auditing)
- `@TenantRequired`: 멀티테넌트 스키마 자동 전환 어노테이션
- Soft Delete: `deletedAt` 필드 (물리 삭제 금지)
- `useYn` (Boolean): 사용 여부 플래그

### 테넌트 격리
- API Gateway에서 `X-Tenant-ID` 헤더로 테넌트 결정
- `TenantContext` (ThreadLocal) → 스키마 전환
- `hola_common` 스키마: 호텔/프로퍼티 마스터, 시스템 코드, 관리자
- `hola_{hotel}_{property}` 스키마: 프로퍼티별 운영 데이터

## 모듈 의존성 규칙

1. 각 모듈은 **자신의 도메인 테이블만** 직접 접근
2. 타 모듈 데이터 = **API 호출** 또는 **Event 수신**
3. 동기 호출: REST API (Internal)
4. 비동기: RabbitMQ Event

## 모듈별 API Base Path

| 모듈 | Base Path |
|------|-----------|
| M01 호텔관리 | `/api/v1/hotels`, `/api/v1/properties` |
| M02 객실관리 | `/api/v1/room-classes`, `/api/v1/room-types` |
| M03 레이트관리 | `/api/v1/rate-codes`, `/api/v1/promotions` |
| M07 예약관리 | `/api/v1/reservations` |
| M10 프론트데스크 | `/api/v1/front-desk` |

## 12대 모듈 우선순위

| 우선순위 | 모듈 |
|----------|------|
| **P0 (코어)** | M01 호텔관리, M02 객실관리, M03 레이트관리, M07 예약관리, M10 프론트데스크 |
| **P1 (운영)** | M04 하우스키핑, M05 채널매니저, M08 부킹엔진, M09 정산, M12 대시보드 |
| **P2 (고도화)** | M06 POS, M11 AI 어시스턴트 |

## 개발 Phase

| Phase | 기간 | 내용 |
|-------|------|------|
| Phase 0 | 2026.03~04 (8주) | 기반 구축 (skeleton, 인증, Admin UI, CI/CD) |
| Phase 1 | 2026.05~08 (16주) | 코어 PMS (M01, M02, M03, M07, M10) |
| Phase 2 | 2026.09~11 (12주) | 운영 모듈 (M04, M09, M12) |
| Phase 3 | 2026.12~2027.02 (12주) | 채널/부킹/POS (M05, M06, M08) |

## 개발 사이클 (Claude Code 통합)

```
[1턴] PM: 설계서 기반 기능 명세
[2턴] Claude: Backend 코드 생성 (Entity → Repository → Service → Controller)
[3턴] 개발자: 코드 리뷰 + 비즈니스 로직 보강
[4턴] Claude: 단위/통합 테스트 + Flyway 마이그레이션
[5턴] Claude: Frontend 페이지 생성 (Ant Design)
[6턴] 개발자: UI 리뷰 + API 연동 검증
```

> 핵심: **1턴 1기능**, 매 턴 결과 확인 후 다음 진행

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
| Opera 벤치마킹 | `설계/99.기타자료/조선_Opera_Configuration_GJJ_Base.pptx` |
| 부킹엔진 요구사항 | `설계/99.기타자료/부킹엔진_개발필요사항.txt` |

## 보안 규칙

- JWT: Access 1h + Refresh 7d
- 비밀번호: 10~20자, 영문+숫자+특수문자, 5회 실패 시 잠금
- 세션: Redis 1시간 타임아웃
- 개인정보: 이름(홍*동), 이메일(user****@), 전화(010-****-1234) 마스킹
- 전송: TLS 1.3, 저장: AES-256
- 감사 로그: 로그인/접근/다운로드/권한변경

## Git 전략

```
main ──── production
  └── develop ──── staging
        ├── feature/HOLA-001-hotel-crud
        └── hotfix/HOLA-099-login-bug
```

- 커밋: `[HOLA-XXX] feat/fix/refactor: description`
- PR: 최소 1인 리뷰, 2주 단위 릴리즈

## UI 공통 규칙 (Admin Frontend)

### 테이블 디자인
- Bootstrap 5 + DataTables 사용, 공통 CSS 기반 통일
- 테이블 클래스: `table table-hover mb-0`, 카드 래핑: `card border-0 shadow-sm`
- 헤더: `thead class="table-light"`
- DataTable 초기화: `$.extend({}, HolaPms.dataTableDefaults, {...})` 패턴 사용
- 개별 language 직접 정의 금지 → `HolaPms.dataTableDefaults` 또는 `HolaPms.dataTableLanguage` 사용

### 리스트 페이지 공통
- **pageSizeSelect**: 10, 20(기본값), 50, 100 — 한글 ("10개씩 보기", "20개씩 보기" 등)
  ```html
  <select id="pageSizeSelect" class="form-select form-select-sm d-inline-block" style="width: auto;">
      <option value="10">10개씩 보기</option>
      <option value="20" selected>20개씩 보기</option>
      <option value="50">50개씩 보기</option>
      <option value="100">100개씩 보기</option>
  </select>
  ```
- **검색 결과 카운트**: `검색 결과 총 <strong id="totalCount">0</strong> 개`
- **페이지네이션**: 한글 (이전/다음/처음/마지막) — `HolaPms.dataTableDefaults.language.paginate` 참조
- **검색 필드 라벨**: `form-label fw-bold` (리스트 검색 영역에서만 bold 허용)

### 폼 페이지 공통 (등록/수정/상세)
- **레이아웃**: Bootstrap grid (`row mb-3` + `col-sm-2 col-form-label`) 사용 필수
  - `table table-bordered` 레이아웃 사용 금지 (리스트 테이블만 table 사용)
  - 단일 필드: `col-sm-2` (label) + `col-sm-6~10` (input)
  - 2열 배치: `col-sm-2` + `col-sm-3` + `col-sm-2` + `col-sm-3`
  - 읽기전용 텍스트: `form-control-plaintext` 클래스
  - 필수 항목: label에 `required` 클래스 (CSS에서 `*` 표시)
- **fw-bold 사용 규칙**:
  - 허용: 페이지 타이틀(`h4`), 섹션 헤더(`h6`)
  - 금지: 폼 라벨(`label`), 데이터 표시 `span`, 읽기전용 `input`, 모달 내 검색 라벨
  - 별도 요청이 없으면 볼드 처리하지 않음
- **버튼 레이아웃**: `d-flex justify-content-between` — 왼쪽: 삭제(btn-outline-danger, 수정모드만), 오른쪽: 취소(btn-secondary)+저장(btn-primary)
- **취소 버튼 아이콘**: `fa-arrow-left`, **삭제 버튼**: 수정모드에서만 `.show()`

### DataTable Ajax 패턴
- 프로퍼티 의존 페이지: function 기반 ajax (propertyId 미선택 시 빈 배열 반환)
  ```javascript
  ajax: function(data, callback) {
      var propertyId = HolaPms.context.getPropertyId();
      if (!propertyId) { callback({ data: [] }); return; }
      $.ajax({ url: '/api/v1/...', success: function(res) { callback(res); } });
  }
  ```
- 프로퍼티 비의존 페이지: URL 기반 ajax (`{ url: '...', dataSrc: 'data' }`)

### 컬러 테마
- 5색: #051923, #003554, #0582CA, #EF476F, #000/#FFF + gray(secondary)
- 폰트: Pretendard (CDN)
