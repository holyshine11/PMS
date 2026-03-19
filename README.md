# Hola PMS - 로컬 개발 환경 설정 및 테스트 가이드

## 목차

- [1. 사전 요구사항](#1-사전-요구사항)
- [2. 데이터베이스 초기 설정](#2-데이터베이스-초기-설정)
- [3. 빠른 시작 (인수인계용)](#3-빠른-시작-인수인계용)
  - [3-1. Git 저장소 클론](#3-1-git-저장소-클론)
  - [3-2. 서버 빌드 및 실행](#3-2-서버-빌드-및-실행)
  - [3-3. 브라우저 접속 및 로그인](#3-3-브라우저-접속-및-로그인)
  - [3-4. GitHub 코드 반영](#3-4-github-코드-반영)
- [4. 웹 브라우저 테스트](#4-웹-브라우저-테스트)
- [5. API 테스트 (curl)](#5-api-테스트-curl)
- [6. E2E 통합 테스트 시나리오](#6-e2e-통합-테스트-시나리오)
- [7. DB 직접 확인](#7-db-직접-확인)
- [8. 트러블슈팅](#8-트러블슈팅)
- [9. 프로젝트 구조](#9-프로젝트-구조)
- [10. API 엔드포인트 전체 목록](#10-api-엔드포인트-전체-목록)
- [11. 부킹엔진 게스트 UI 테스트](#11-부킹엔진-게스트-ui-테스트)

---

## 1. 사전 요구사항

| 도구 | 버전 | 확인 명령어 |
|------|------|------------|
| Java (OpenJDK) | 17 | `java -version` |
| PostgreSQL | 16 | `psql --version` |
| Redis | 7+ | `redis-server --version` |
| Git | 2.x | `git --version` |

### 1-1. Homebrew로 설치 (미설치 시)

```bash
# Java 17
brew install openjdk@17

# ~/.zshrc에 추가
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# PostgreSQL 16
brew install postgresql@16
echo 'export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Redis
brew install redis
```

### 1-2. 서비스 시작

```bash
brew services start postgresql@16
brew services start redis

# 확인
pg_isready          # "accepting connections"
redis-cli ping      # "PONG"
```

---

## 2. 데이터베이스 초기 설정

```bash
# DB 생성
createdb hola_pms

# 사용자 생성 + 권한 부여
psql -d hola_pms -c "CREATE USER hola WITH PASSWORD 'hola1234';"
psql -d hola_pms -c "GRANT ALL PRIVILEGES ON DATABASE hola_pms TO hola;"
psql -d hola_pms -c "GRANT ALL ON SCHEMA public TO hola;"
psql -d hola_pms -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO hola;"
psql -d hola_pms -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO hola;"
```

### 확인

```bash
psql -U hola -d hola_pms -c "SELECT 1;"
# 정상이면 "1" 출력
```

---

## 3. 빠른 시작 (인수인계용)

> 사전 요구사항(1장)과 DB 설정(2장)이 완료된 상태에서,
> **Git 클론 → 빌드 → 서버 실행 → 브라우저 접속**까지 한 번에 따라하는 가이드입니다.

### 3-1. Git 저장소 클론

```bash
# 저장소 클론
git clone <repository-url>
cd PMS
```

### 3-2. 서버 빌드 및 실행

```bash
cd hola-pms

# 빌드 (최초 1회 또는 코드 변경 시)
./gradlew build

# 서버 실행 (로컬, http://localhost:8080)
./gradlew :hola-app:bootRun

# 다른 포트로 실행해야 할 경우
./gradlew :hola-app:bootRun --args='--server.port=9090'
```

**실행 성공 로그 (확인 포인트)**

```
Flyway: Successfully applied 2 migrations (V1.0.0, V1.1.0)
Tomcat started on port 8080
Started HolaPmsApplication in x.xxx seconds
```

> Flyway가 자동으로 테이블 생성 + 초기 데이터 삽입까지 처리합니다.
> 별도 SQL 실행 불필요.

### 3-3. 브라우저 접속 및 로그인

1. 브라우저에서 http://localhost:8080/login 접속
2. 로그인 정보 입력:
   - **아이디**: `admin`
   - **비밀번호**: `holapms1!`
3. 로그인 성공 시 대시보드(`/admin/dashboard`)로 이동

### 3-4. GitHub 코드 반영

```bash
cd hola-pms  # 또는 PMS 루트 디렉토리

# 1. 변경사항 확인
git status
git diff

# 2. 변경 파일 스테이징
git add <파일명>              # 개별 파일
git add .
git add hola-pms/hola-hotel/  # 모듈 단위
# git add -A                  # 전체 (주의: .env 등 민감파일 포함 여부 확인)

# 3. 커밋 (프로젝트 커밋 규칙: [HOLA-XXX] type: description)
git commit -m "[HOLA-001] feat: add hotel CRUD API and UI"

# 4. 원격 저장소에 푸시
git push origin main

# 5. (선택) 브랜치 작업 시
git checkout -b feature/HOLA-002-room-management
# ... 작업 후 ...
git push origin feature/HOLA-002-room-management
# GitHub에서 PR 생성
```

**커밋 메시지 규칙**

| 타입 | 설명 | 예시 |
|------|------|------|
| `feat` | 새 기능 | `[HOLA-001] feat: add hotel CRUD API` |
| `fix` | 버그 수정 | `[HOLA-002] fix: resolve login redirect issue` |
| `refactor` | 리팩토링 | `[HOLA-003] refactor: extract access control service` |
| `docs` | 문서 수정 | `[HOLA-004] docs: update README setup guide` |
| `chore` | 설정/빌드 | `[HOLA-005] chore: add flyway migration V2_0_0` |

**Git 브랜치 전략**

```
main ──────── production (안정 버전)
  └── develop ── staging
        ├── feature/HOLA-001-hotel-crud
        └── hotfix/HOLA-099-login-bug
```

> PR(Pull Request)은 최소 1인 리뷰 후 머지. 커밋 메시지는 영문으로 작성.

---

## 4. 웹 브라우저 테스트

### 4-1. 호텔 관리 화면

1. 좌측 메뉴에서 **호텔 관리 > 호텔 목록** 클릭 (또는 http://localhost:8080/admin/hotels)
2. 빈 테이블이 표시됨 (아직 데이터 없음)

### 4-2. 호텔 등록 테스트

1. 우측 상단 **호텔 등록** 버튼 클릭
2. 모달에 입력:
   - 호텔코드: `HOLA001`
   - 호텔명: `올라 호텔 서울`
   - 호텔유형: `호텔`
   - 등급: `5성급`
   - 전화번호: `02-1234-5678`
3. **저장** 클릭
4. 목록에 등록된 호텔 확인

### 4-3. 호텔 상세 > 프로퍼티 등록

1. 목록에서 **올라 호텔 서울** 링크 클릭 → 호텔 상세 페이지
2. **프로퍼티 등록** 버튼 클릭
3. 모달에 입력:
   - 프로퍼티코드: `MAIN`
   - 프로퍼티명: `메인타워`
   - 유형: `호텔`
   - 체크인: `15:00`
   - 체크아웃: `11:00`
   - 총 객실수: `200`
4. **저장** 클릭

### 4-4. 프로퍼티 상세 > 층/호수/마켓코드

1. 프로퍼티 목록에서 **메인타워** 링크 클릭 → 프로퍼티 상세 페이지
2. **층 관리** 탭 (기본 활성)에서:
   - **층 등록** 클릭 → 층 번호: `1`, 층 이름: `로비층` → 저장
   - **층 등록** 클릭 → 층 번호: `2`, 층 이름: `객실 2층` → 저장
3. **호수 관리** 탭 클릭 후:
   - **호수 등록** 클릭 → 호수: `101`, 객실코드: `STD101`, 층: `1층 (로비층)` → 저장
   - **호수 등록** 클릭 → 호수: `201`, 객실코드: `DLX201`, 층: `2층 (객실 2층)` → 저장
4. **마켓코드** 탭 클릭 후:
   - **마켓코드 등록** 클릭 → 마켓코드: `FIT`, 마켓코드명: `개인 여행객` → 저장
   - **마켓코드 등록** 클릭 → 마켓코드: `GRP`, 마켓코드명: `단체` → 저장

---

## 5. API 테스트 (curl)

API는 JWT 인증이 필요합니다. 먼저 토큰을 발급받고 사용합니다.

### 5-1. JWT 토큰 발급

```bash
# 로그인 → JWT 토큰 받기
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"admin","password":"holapms1!"}' | python3 -m json.tool
```

응답에서 `accessToken` 값을 복사합니다:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...(긴 토큰)",
    "refreshToken": "...",
    "userName": "시스템관리자",
    "role": "SUPER_ADMIN"
  }
}
```

### 5-2. 토큰을 환경변수에 저장

```bash
# 위에서 복사한 accessToken 값으로 교체
export TOKEN="eyJhbGciOi...(위에서 복사한 전체 토큰)"

# 또는 한 줄로 자동 추출
export TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"admin","password":"holapms1!"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
```

### 5-3. 호텔 CRUD API

```bash
# [CREATE] 호텔 등록
curl -s -X POST http://localhost:8080/api/v1/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "hotelCode": "HOLA002",
    "hotelName": "올라 리조트 제주",
    "hotelType": "RESORT",
    "starRating": "4STAR",
    "address": "제주시 연동",
    "phone": "064-123-4567"
  }' | python3 -m json.tool

# [READ] 호텔 목록 조회
curl -s http://localhost:8080/api/v1/hotels \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# [READ] 호텔 상세 조회 (id=1)
curl -s http://localhost:8080/api/v1/hotels/1 \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# [UPDATE] 호텔 수정
curl -s -X PUT http://localhost:8080/api/v1/hotels/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "hotelName": "올라 호텔 서울 (수정)",
    "hotelType": "HOTEL",
    "starRating": "5STAR",
    "phone": "02-9999-8888"
  }' | python3 -m json.tool

# [DELETE] 호텔 삭제 (Soft Delete)
curl -s -X DELETE http://localhost:8080/api/v1/hotels/2 \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### 5-4. 프로퍼티 CRUD API

```bash
# 프로퍼티 등록 (hotelId=1에 소속)
curl -s -X POST http://localhost:8080/api/v1/hotels/1/properties \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "propertyCode": "ANNEX",
    "propertyName": "별관",
    "propertyType": "HOTEL",
    "checkInTime": "15:00",
    "checkOutTime": "11:00",
    "totalRooms": 50
  }' | python3 -m json.tool

# 프로퍼티 목록 조회
curl -s http://localhost:8080/api/v1/hotels/1/properties \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### 5-5. 층/호수/마켓코드 CRUD API

```bash
# --- 층 ---
# 층 등록 (propertyId=1)
curl -s -X POST http://localhost:8080/api/v1/properties/1/floors \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"floorNumber": 3, "floorName": "객실 3층"}' | python3 -m json.tool

# 층 목록
curl -s http://localhost:8080/api/v1/properties/1/floors \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# --- 호수 ---
# 호수 등록 (propertyId=1, floorId=1)
curl -s -X POST http://localhost:8080/api/v1/properties/1/room-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"roomNumber": "301", "roomCode": "STD301", "floorId": 3}' | python3 -m json.tool

# 호수 목록
curl -s http://localhost:8080/api/v1/properties/1/room-numbers \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# --- 마켓코드 ---
# 마켓코드 등록 (propertyId=1)
curl -s -X POST http://localhost:8080/api/v1/properties/1/market-codes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"marketCode": "OTA", "marketName": "온라인 여행사"}' | python3 -m json.tool

# 마켓코드 목록
curl -s http://localhost:8080/api/v1/properties/1/market-codes \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## 6. E2E 통합 테스트 시나리오

아래 순서대로 실행하면 전체 M01 모듈을 검증할 수 있습니다.

```
[Step 1] 호텔 등록
  POST /api/v1/hotels  →  hotelCode: "TEST01", hotelName: "테스트호텔"

[Step 2] 프로퍼티 등록
  POST /api/v1/hotels/{hotelId}/properties  →  propertyCode: "MAIN", propertyName: "본관"

[Step 3] 층 등록 (2개)
  POST /api/v1/properties/{propertyId}/floors  →  floorNumber: 1
  POST /api/v1/properties/{propertyId}/floors  →  floorNumber: 2

[Step 4] 호수 등록 (3개)
  POST /api/v1/properties/{propertyId}/room-numbers  →  roomNumber: "101", floorId: {floor1Id}
  POST /api/v1/properties/{propertyId}/room-numbers  →  roomNumber: "102", floorId: {floor1Id}
  POST /api/v1/properties/{propertyId}/room-numbers  →  roomNumber: "201", floorId: {floor2Id}

[Step 5] 마켓코드 등록 (2개)
  POST /api/v1/properties/{propertyId}/market-codes  →  marketCode: "FIT"
  POST /api/v1/properties/{propertyId}/market-codes  →  marketCode: "COR"

[Step 6] 계층 조회 확인
  GET /api/v1/hotels/{hotelId}                  →  호텔 정보 + propertyCount: 1
  GET /api/v1/hotels/{hotelId}/properties       →  프로퍼티 목록 (floorCount, roomCount 확인)
  GET /api/v1/properties/{propertyId}/floors     →  층 목록 (roomCount 확인)

[Step 7] 삭제 제약 테스트
  DELETE /api/v1/properties/{propertyId}/floors/{floor1Id}
  → 하위 호수가 있으므로 삭제 실패 (HOLA-1040 에러) 확인

  DELETE /api/v1/hotels/{hotelId}
  → 하위 프로퍼티가 있으므로 삭제 실패 (HOLA-1002 에러) 확인

[Step 8] 정상 삭제 흐름
  DELETE /api/v1/properties/{propertyId}/room-numbers/{roomId}  →  호수 삭제 성공
  DELETE /api/v1/properties/{propertyId}/floors/{floorId}       →  호수 없는 층 삭제 성공
```

---

## 7. DB 직접 확인

```bash
# 테이블 목록 확인
psql -U hola -d hola_pms -c "\dt"

# 호텔 데이터 조회
psql -U hola -d hola_pms -c "SELECT id, hotel_code, hotel_name, use_yn, deleted_at FROM htl_hotel;"

# 프로퍼티 데이터 조회
psql -U hola -d hola_pms -c "SELECT id, property_code, property_name, hotel_id FROM htl_property;"

# 층 데이터 조회
psql -U hola -d hola_pms -c "SELECT id, floor_number, floor_name, property_id FROM htl_floor;"

# 호수 데이터 조회
psql -U hola -d hola_pms -c "SELECT id, room_number, room_code, floor_id FROM htl_room_number;"

# Soft Delete 확인 (삭제된 데이터는 deleted_at이 NOT NULL)
psql -U hola -d hola_pms -c "SELECT id, hotel_code, deleted_at FROM htl_hotel WHERE deleted_at IS NOT NULL;"

# Flyway 마이그레이션 이력
psql -U hola -d hola_pms -c "SELECT version, description, installed_on FROM flyway_schema_history;"
```

---

## 8. 트러블슈팅

### Java를 찾을 수 없는 경우

```bash
# JAVA_HOME 확인
echo $JAVA_HOME
# 비어있으면:
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export PATH="$JAVA_HOME/bin:$PATH"
```

### PostgreSQL 연결 실패

```bash
# 서비스 상태 확인
brew services list | grep postgresql

# 재시작
brew services restart postgresql@16

# hola 사용자로 접속 테스트
psql -U hola -d hola_pms -c "SELECT 1;"
```

### 포트 8080 충돌

```bash
# 8080 포트 사용 중인 프로세스 확인
lsof -i :8080

# 다른 포트로 실행
./gradlew :hola-app:bootRun --args='--server.port=9090'
```

### DB 초기화 (완전 리셋)

```bash
# DB 삭제 후 재생성
dropdb hola_pms
createdb hola_pms
psql -d hola_pms -c "GRANT ALL PRIVILEGES ON DATABASE hola_pms TO hola;"
psql -d hola_pms -c "GRANT ALL ON SCHEMA public TO hola;"
psql -d hola_pms -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO hola;"
psql -d hola_pms -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO hola;"

# 서버 재시작 → Flyway가 자동으로 테이블/데이터 재생성
./gradlew :hola-app:bootRun
```

---

## 9. 프로젝트 구조

```
hola-pms/
├── settings.gradle                # 멀티모듈 설정
├── build.gradle                   # 루트 빌드 (Java 17, Spring Boot 3.2.5)
├── gradlew                        # Gradle Wrapper
├── hola-common/                   # 공통 모듈
│   └── src/main/java/com/hola/common/
│       ├── entity/BaseEntity.java
│       ├── dto/HolaResponse.java
│       ├── exception/             # ErrorCode, GlobalExceptionHandler
│       ├── security/              # JWT, SecurityConfig
│       ├── tenant/                # 멀티테넌시 (Schema-per-Tenant)
│       └── auth/                  # 로그인 API
├── hola-hotel/                    # M01 호텔관리 모듈
│   └── src/main/java/com/hola/hotel/
│       ├── entity/                # Hotel, Property, Floor, RoomNumber, MarketCode
│       ├── repository/
│       ├── service/
│       ├── controller/            # REST API + Thymeleaf View
│       ├── dto/
│       └── mapper/
└── hola-app/                      # 실행 모듈
    └── src/main/
        ├── java/.../HolaPmsApplication.java
        └── resources/
            ├── application.yml
            ├── application-local.yml
            ├── db/migration/      # Flyway SQL
            ├── templates/         # Thymeleaf HTML
            │   ├── layout/        # 공통 레이아웃
            │   ├── login.html
            │   ├── dashboard.html
            │   ├── hotel/         # 호텔 목록/상세
            │   └── property/      # 프로퍼티 상세 (탭: 층/호수/마켓코드)
            └── static/
                ├── css/hola.css
                └── js/            # hotel.js, property.js, floor.js 등
```

---

## 10. API 엔드포인트 전체 목록

**Swagger UI URL**
- http://localhost:8080/swagger-ui.html

### 인증 (JWT)
| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) |

### 호텔
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/hotels` | 호텔 목록 |
| GET | `/api/v1/hotels/{id}` | 호텔 상세 |
| POST | `/api/v1/hotels` | 호텔 등록 |
| PUT | `/api/v1/hotels/{id}` | 호텔 수정 |
| DELETE | `/api/v1/hotels/{id}` | 호텔 삭제 |

### 프로퍼티
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/hotels/{hotelId}/properties` | 프로퍼티 목록 |
| GET | `/api/v1/hotels/{hotelId}/properties/{id}` | 프로퍼티 상세 |
| POST | `/api/v1/hotels/{hotelId}/properties` | 프로퍼티 등록 |
| PUT | `/api/v1/hotels/{hotelId}/properties/{id}` | 프로퍼티 수정 |
| DELETE | `/api/v1/hotels/{hotelId}/properties/{id}` | 프로퍼티 삭제 |

### 층
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/properties/{propertyId}/floors` | 층 목록 |
| GET | `/api/v1/properties/{propertyId}/floors/{id}` | 층 상세 |
| POST | `/api/v1/properties/{propertyId}/floors` | 층 등록 |
| PUT | `/api/v1/properties/{propertyId}/floors/{id}` | 층 수정 |
| DELETE | `/api/v1/properties/{propertyId}/floors/{id}` | 층 삭제 |

### 호수
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/properties/{propertyId}/room-numbers` | 호수 목록 |
| GET | `/api/v1/properties/{propertyId}/room-numbers/{id}` | 호수 상세 |
| POST | `/api/v1/properties/{propertyId}/room-numbers` | 호수 등록 |
| PUT | `/api/v1/properties/{propertyId}/room-numbers/{id}` | 호수 수정 |
| DELETE | `/api/v1/properties/{propertyId}/room-numbers/{id}` | 호수 삭제 |

### 마켓코드
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/properties/{propertyId}/market-codes` | 마켓코드 목록 |
| GET | `/api/v1/properties/{propertyId}/market-codes/{id}` | 마켓코드 상세 |
| POST | `/api/v1/properties/{propertyId}/market-codes` | 마켓코드 등록 |
| PUT | `/api/v1/properties/{propertyId}/market-codes/{id}` | 마켓코드 수정 |
| DELETE | `/api/v1/properties/{propertyId}/market-codes/{id}` | 마켓코드 삭제 |

### 부킹엔진 API (인증 불필요 - 게스트용)
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/booking/properties/{propertyCode}` | 프로퍼티 정보 |
| GET | `/api/v1/booking/properties/{propertyCode}/availability` | 가용 객실 검색 |
| POST | `/api/v1/booking/properties/{propertyCode}/price-check` | 요금 상세 조회 |
| POST | `/api/v1/booking/properties/{propertyCode}/reservations` | 예약 생성 |
| GET | `/api/v1/booking/confirmation/{confirmationNo}` | 예약 확인 |

### 웹 페이지 (Admin - Thymeleaf)
| URL | 설명 |
|-----|------|
| `/login` | 로그인 페이지 |
| `/admin/dashboard` | 대시보드 |
| `/admin/hotels` | 호텔 목록 |
| `/admin/hotels/{id}` | 호텔 상세 (프로퍼티 목록 포함) |
| `/admin/hotels/{hotelId}/properties/{id}` | 프로퍼티 상세 (층/호수/마켓코드 탭) |

### 웹 페이지 (게스트 - 부킹엔진, 로그인 불필요)
| URL | 설명 |
|-----|------|
| `/booking/{propertyCode}` | 예약 검색 (체크인/아웃, 인원) |
| `/booking/{propertyCode}/rooms` | 객실 선택 (가용 객실 + 요금) |
| `/booking/{propertyCode}/checkout` | 투숙객 정보 입력 + 결제 |
| `/booking/{propertyCode}/confirmation/{no}` | 예약 완료 확인 |

### Swagger UI (API 문서)

http://localhost:8080/swagger-ui.html

> 인증 필요 API는 Swagger UI 상단 Authorize 버튼에 JWT 토큰을 입력해야 합니다.
> 부킹엔진 API (`/api/v1/booking/**`)는 인증 없이 바로 테스트 가능합니다.

---

## 11. 부킹엔진 게스트 UI 테스트

### 접속 URL

| 프로퍼티 | URL | 비고 |
|----------|-----|------|
| 올라 그랜드 명동 | http://localhost:8080/booking/GMP | 5성급, 테스트 데이터 포함 |
| 올라 그랜드 서초 | http://localhost:8080/booking/GMS | 4성급 |
| 올라 그랜드 부산 | http://localhost:8080/booking/OBH

예약 조회 : http://localhost:8080/booking/GMP/confirmation/{확인번호}

### 테스트 플로우

```
Step 1. 검색 페이지 (/booking/GMP)
  - 호텔명/주소/체크인·아웃 시간 자동 표시 확인
  - 날짜 선택 → 숙박일수 자동 계산
  - "객실 검색" 클릭

Step 2. 객실 선택 (/booking/GMP/rooms?...)
  - 가용 객실 목록 + 요금 옵션 표시
  - 요금 클릭 → 일자별 상세 요금 펼침
  - 하단 바 "예약하기" 클릭

Step 3. 체크아웃 (/booking/GMP/checkout)
  - 투숙객 정보 입력 (성명: 테스트, 전화: 010-1234-5678, 이메일: test@test.com)
  - 결제 수단: 신용카드 또는 현장결제 (Mock 결제 - 아무 값 입력 가능)
  - 필수 약관 체크 → "예약 완료" 클릭

Step 4. 예약 확인 (/booking/GMP/confirmation/{no})
  - 확인번호, 객실, 결제 정보 표시
  - 새로고침 시 이메일 입력으로 재조회 가능
```

### PMS 예약 적재 확인

예약 완료 후 Admin 로그인 → 프로퍼티 선택 → 예약 목록에서 생성된 예약 확인

### 관리자 테스트 계정

| 아이디 | 이름 | 권한 | 소속호텔 |
|--------|------|------|----------|
| admin | 시스템관리자 | SUPER_ADMIN | - |
| hotel1admin | 김서울 | HOTEL_ADMIN | 올라서울호텔 |
| hotel2admin | 박부산 | HOTEL_ADMIN | 올라비치해운대 |
| prop1admin | 이명동 | PROPERTY_ADMIN | 명동 |
| prop2admin | 최서초 | PROPERTY_ADMIN | 서초 |
| prop3admin | 정해운 | PROPERTY_ADMIN | 해운대 |

> 전체 계정 비밀번호: `holapms1!`
