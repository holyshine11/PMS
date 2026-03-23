# Hola PMS API 계약 테스트 케이스

> 작성일: 2026-03-23 | 작성자: QC팀 | 버전: 2.0 (Dayuse 기능 추가 후 전체 검증)
> 테스트 환경: http://localhost:8080 | 인증: JWT Bearer Token (admin/holapms1!)
> 테스트 대상: Dayuse 기능 추가 + 리팩토링 사이드이펙트 점검

---

## 테스트 범례
- ▢ : 미테스트  ✅ : PASS  ❌ : FAIL  ⚠️ : 주의(동작하나 개선 필요)  ⏭ : SKIP

---

# Section 1: 인증 API (AUTH)

## API-AUTH-001 로그인 성공
- **엔드포인트**: POST /api/v1/auth/login
- **요청**: `{"loginId":"admin","password":"holapms1!"}`
- **기대결과**: HTTP 200, accessToken + refreshToken 반환
- **실제결과**: HTTP 200, success=True, accessToken 202자, refreshToken 정상
- **PASS 여부**: ✅

## API-AUTH-002 잘못된 비밀번호
- **엔드포인트**: POST /api/v1/auth/login
- **요청**: `{"loginId":"admin","password":"wrongpass"}`
- **기대결과**: HTTP 401, 에러 코드 반환
- **실제결과**: HTTP 401, success=False, code=HOLA-0005
- **PASS 여부**: ✅

## API-AUTH-003 존재하지 않는 계정
- **엔드포인트**: POST /api/v1/auth/login
- **요청**: `{"loginId":"nonexistent","password":"test1234!"}`
- **기대결과**: HTTP 401 (계정 존재 여부 미노출)
- **실제결과**: HTTP 401
- **PASS 여부**: ✅

## API-AUTH-004 빈 loginId
- **엔드포인트**: POST /api/v1/auth/login
- **요청**: `{"loginId":"","password":"test1234!"}`
- **기대결과**: HTTP 400 유효성 검증 오류
- **실제결과**: HTTP 400, success=False
- **PASS 여부**: ✅

## API-AUTH-005 body 없이 로그인
- **엔드포인트**: POST /api/v1/auth/login (빈 body)
- **기대결과**: HTTP 400
- **실제결과**: HTTP 500 (서버 내부 오류)
- **PASS 여부**: ⚠️ (500 대신 400 반환 권장, 기존 이슈)

---

# Section 2: 호텔/프로퍼티 기본 API

## API-HOTEL-001 호텔 목록 조회
- **엔드포인트**: GET /api/v1/hotels
- **기대결과**: HTTP 200, 호텔 배열 반환
- **실제결과**: HTTP 200, count=2 (올라 서울, 올라 부산)
- **PASS 여부**: ✅

## API-HOTEL-002 호텔 셀렉터
- **엔드포인트**: GET /api/v1/hotels/selector
- **기대결과**: HTTP 200, 권한 기반 호텔 목록
- **실제결과**: HTTP 200, count=2
- **PASS 여부**: ✅

## API-HOTEL-003 호텔 상세
- **엔드포인트**: GET /api/v1/hotels/{id}
- **실제결과**: ID=6 → HTTP 200 정상, ID=1 → HTTP 404 (데이터 없음 - 정상)
- **PASS 여부**: ✅

## API-PROP-001 프로퍼티 상세 - Dayuse 필드 포함 확인
- **엔드포인트**: GET /api/v1/properties/4
- **기대결과**: dayUseEnabled, dayUseStartTime, dayUseEndTime, dayUseDefaultHours 필드 포함
- **실제결과**: dayUseEnabled=True, dayUseStartTime=10:00, dayUseEndTime=20:00, dayUseDefaultHours=5
- **PASS 여부**: ✅

## API-PROP-002 인증 없이 접근
- **엔드포인트**: GET /api/v1/properties/4 (Authorization 헤더 없음)
- **기대결과**: HTTP 401
- **실제결과**: HTTP 401
- **PASS 여부**: ✅

---

# Section 3: Dayuse Rate API (신규)

## API-DAYUSE-001 Dayuse 요금 목록 조회
- **엔드포인트**: GET /api/v1/properties/4/rate-codes/77/dayuse-rates
- **기대결과**: HTTP 200, DayUseRate 배열 (sortOrder 순)
- **실제결과**: HTTP 200, count=1, id=4 (3시간 60,000원)
- **PASS 여부**: ✅

## API-DAYUSE-002 Dayuse 요금 등록 (정상)
- **엔드포인트**: POST /api/v1/properties/4/rate-codes/77/dayuse-rates
- **요청**: `{"durationHours":10,"supplyPrice":180000,"description":"API 테스트 10시간"}`
- **기대결과**: HTTP 201, 생성된 DayUseRate 반환
- **실제결과**: HTTP 201, id=15, 모든 필드 정상
- **PASS 여부**: ✅

## API-DAYUSE-003 Dayuse 요금 - 필수값 누락 (durationHours)
- **엔드포인트**: POST .../dayuse-rates
- **요청**: `{"supplyPrice":100000}` (durationHours 없음)
- **기대결과**: HTTP 400
- **실제결과**: HTTP 400, code=HOLA-0002
- **PASS 여부**: ✅

## API-DAYUSE-004 Dayuse 요금 - 필수값 누락 (supplyPrice)
- **요청**: `{"durationHours":5}` (supplyPrice 없음)
- **기대결과**: HTTP 400
- **실제결과**: HTTP 400
- **PASS 여부**: ✅

## API-DAYUSE-005 Dayuse 요금 - 음수 가격
- **요청**: `{"durationHours":3,"supplyPrice":-50000}`
- **기대결과**: HTTP 400 (@Min(0) 검증)
- **실제결과**: HTTP 400
- **PASS 여부**: ✅

## API-DAYUSE-006 Dayuse 요금 - 시간 0
- **요청**: `{"durationHours":0,"supplyPrice":50000}`
- **기대결과**: HTTP 400 (@Min(1) 검증)
- **실제결과**: HTTP 400
- **PASS 여부**: ✅

## API-DAYUSE-007 Dayuse 요금 삭제 (Soft Delete)
- **엔드포인트**: DELETE .../dayuse-rates/15
- **기대결과**: HTTP 200, soft delete (deletedAt 설정)
- **실제결과**: HTTP 200, success=True
- **PASS 여부**: ✅

## API-DAYUSE-008 삭제된 요금 목록 제외 확인
- **검증**: 삭제 후 목록 재조회 시 ID=15 미포함
- **실제결과**: 목록에 ID=4만 존재, ID=15 제외됨 (SQLRestriction 동작 확인)
- **PASS 여부**: ✅

## API-DAYUSE-009 존재하지 않는 요금 삭제
- **엔드포인트**: DELETE .../dayuse-rates/99999
- **기대결과**: HTTP 404 (존재하지 않는 리소스)
- **실제결과**: HTTP 200 (성공 응답)
- **PASS 여부**: ⚠️ 존재하지 않는 리소스 삭제 시 404 반환 권장

## API-DAYUSE-010 Overnight RC에 dayuse rate 조회
- **엔드포인트**: GET .../rate-codes/35/dayuse-rates (OVERNIGHT 레이트코드)
- **기대결과**: HTTP 200, 빈 배열
- **실제결과**: HTTP 200, count=0
- **PASS 여부**: ✅

---

# Section 4: Rate Code stayType 필드 (확장)

## API-RC-ST-001 레이트코드 생성 (stayType=DAY_USE)
- **엔드포인트**: POST /api/v1/properties/4/rate-codes
- **요청**: `{..., "stayType":"DAY_USE"}`
- **기대결과**: HTTP 201, stayType=DAY_USE
- **실제결과**: HTTP 201, stayType=DAY_USE ✅
- **PASS 여부**: ✅

## API-RC-ST-002 레이트코드 생성 (stayType=OVERNIGHT)
- **기대결과**: HTTP 201, stayType=OVERNIGHT
- **실제결과**: HTTP 201, stayType=OVERNIGHT
- **PASS 여부**: ✅

## API-RC-ST-003 레이트코드 생성 (stayType 생략 → 기본값 OVERNIGHT)
- **기대결과**: HTTP 201, stayType=OVERNIGHT (기본값)
- **실제결과**: HTTP 201, stayType=OVERNIGHT
- **PASS 여부**: ✅

## API-RC-ST-004 레이트코드 수정 (stayType OVERNIGHT→DAY_USE)
- **엔드포인트**: PUT /api/v1/properties/4/rate-codes/{id}
- **기대결과**: stayType 변경 반영
- **실제결과**: BEFORE=OVERNIGHT → AFTER=DAY_USE 정상 변경
- **PASS 여부**: ✅

## API-RC-ST-005 잘못된 stayType 값 (INVALID_TYPE)
- **요청**: `{..., "stayType":"INVALID_TYPE"}`
- **기대결과**: HTTP 400 (유효하지 않은 stayType)
- **실제결과**: HTTP 201 성공 (검증 없이 수락)
- **PASS 여부**: ❌ stayType 값 검증 누락 (OVERNIGHT/DAY_USE만 허용해야 함)

## API-RC-ST-006 레이트코드 목록 (stayType 필드 포함)
- **엔드포인트**: GET /api/v1/properties/4/rate-codes
- **기대결과**: 목록 응답에 stayType 필드 포함
- **실제결과**: count=18, DAY_USE 4개 / OVERNIGHT 14개, stayType 정상 반환
- **PASS 여부**: ✅

---

# Section 5: 예약 API (Dayuse 통합)

## API-RESV-001 예약 목록 조회
- **엔드포인트**: GET /api/v1/properties/4/reservations
- **기대결과**: HTTP 200, 예약 배열
- **실제결과**: HTTP 200, count=264, 응답 키: confirmationNo, reservationStatus, roomTypeName 등
- **PASS 여부**: ✅

## API-RESV-002 예약 상세 (stayType 필드)
- **엔드포인트**: GET /api/v1/properties/4/reservations/253
- **기대결과**: subReservations[].stayType 필드 포함
- **실제결과**: legs[0].stayType=OVERNIGHT, dayUseStartTime=null, dayUseEndTime=null
- **PASS 여부**: ✅

## API-RESV-003 Dayuse 예약 생성 (checkIn=checkOut 당일)
- **요청**: `{"masterCheckIn":"2026-04-15","masterCheckOut":"2026-04-15",...}`
- **기대결과**: HTTP 201 (dayuse는 당일 허용)
- **실제결과**: HTTP 400, HOLA-4013 "체크아웃은 체크인 이후여야 합니다."
- **PASS 여부**: ❌ **Dayuse 당일 체크인=체크아웃 지원 필요** (validateDates가 masterCheckIn/Out에도 적용)
- **우회방법**: masterCheckOut = masterCheckIn + 1 로 전송하면 정상 동작

## API-RESV-004 Dayuse 예약 생성 (checkOut=checkIn+1)
- **요청**: `{"masterCheckIn":"2026-04-15","masterCheckOut":"2026-04-16","rateCodeId":77,...,"stayType":"DAY_USE","dayUseDurationHours":3}`
- **기대결과**: HTTP 201, stayType=DAY_USE, dayUseStartTime/EndTime 설정
- **실제결과**: HTTP 201
  - stayType=DAY_USE ✅
  - dayUseStartTime=10:00 ✅ (프로퍼티 기본값)
  - dayUseEndTime=13:00 ✅ (10:00 + 3시간)
  - dailyCharge: supplyPrice=60,000 (DayUseRate 3시간 기준) ✅
  - tax=6,600 / serviceCharge=6,000 ✅
- **PASS 여부**: ✅ (checkOut 보정 방식으로 동작)

## API-RESV-005 Overnight 예약 생성 (사이드이펙트 없음 확인)
- **요청**: 일반 OVERNIGHT 예약 (rateCodeId=35, 2박)
- **기대결과**: HTTP 201, stayType=OVERNIGHT
- **참고**: adults=2 시 HOLA-4015 "최대 수용 인원 초과" → 정상 검증 동작
- **PASS 여부**: ✅ (검증 로직 사이드이펙트 없음)

## API-RESV-006 예약 캘린더
- **엔드포인트**: GET /api/v1/properties/4/reservations/calendar?startDate=2026-04-14&endDate=2026-04-17
- **기대결과**: 날짜별 예약 맵
- **실제결과**: HTTP 200, 4/15에 dayuse 예약 포함, stayType 필드 **미포함**
- **PASS 여부**: ⚠️ 캘린더 응답에 stayType 필드 누락 (dayuse/overnight 구분 불가)

## API-RESV-007 가용객실 조회
- **엔드포인트**: GET .../availability?roomTypeId=32&checkIn=2026-04-15&checkOut=2026-04-16
- **기대결과**: HTTP 200, availableCount
- **실제결과**: HTTP 200, availableCount=4
- **PASS 여부**: ✅

## API-RESV-008 예약 취소 (Dayuse)
- **엔드포인트**: DELETE /api/v1/properties/4/reservations/780
- **사전검증**: 취소 프리뷰 → cancellationFee=null, refundAmount=0
- **실제결과**: HTTP 200, 취소 성공
- **PASS 여부**: ✅

## API-RESV-009 예약 상태 변경 (Dayuse → CONFIRMED)
- **엔드포인트**: PUT .../reservations/779/status
- **요청**: `{"newStatus":"CONFIRMED"}`
- **실제결과**: HTTP 400, HOLA-4003 "해당 상태로 변경할 수 없습니다."
- **참고**: RESERVED→CONFIRMED 직접 전환은 비즈니스 규칙상 불가 (체크인 전 객실 배정 필요)
- **PASS 여부**: ✅ (정상 검증)

## API-RESV-010 예약 메모 CRUD
- **GET** .../reservations/779/memos → HTTP 200 ✅
- **POST** .../reservations/779/memos `{"content":"API 테스트 메모"}` → HTTP 201 ✅
- **PASS 여부**: ✅

---

# Section 6: 프론트데스크 API

## API-FD-001 서머리
- **엔드포인트**: GET /api/v1/properties/4/front-desk/summary
- **기대결과**: arrivals, inHouse, departures, checkedInToday, checkedOutToday
- **실제결과**: HTTP 200, arrivals=0, inHouse=9, departures=0, checkedInToday=2, checkedOutToday=3
- **PASS 여부**: ✅

## API-FD-002 인하우스 목록 (stayType 포함)
- **엔드포인트**: GET .../front-desk/in-house
- **기대결과**: stayType 필드 포함
- **실제결과**: HTTP 200, count=9, stayType=OVERNIGHT (모두 숙박), stayType 필드 정상 포함
- **PASS 여부**: ✅

## API-FD-003 전체 운영 목록 (stayType 포함)
- **엔드포인트**: GET .../front-desk/all
- **기대결과**: stayType 필드 포함
- **실제결과**: HTTP 200, count=15, stayType 필드 정상 포함
- **PASS 여부**: ✅

## API-FD-004 다른 프로퍼티 FD
- **프로퍼티 5, 6**: HTTP 200 정상
- **PASS 여부**: ✅

---

# Section 7: 대시보드 API

## API-DASH-001 대시보드 KPI
- **엔드포인트**: GET /api/v1/dashboard?propertyId=4
- **기대결과**: HTTP 200, KPI 데이터
- **실제결과**: HTTP 200, success=True
- **PASS 여부**: ✅

---

# 종합 결과

## 테스트 통계

| 구분 | PASS | FAIL | WARNING | 합계 |
|------|------|------|---------|------|
| AUTH API | 4 | 0 | 1 | 5 |
| 호텔/프로퍼티 | 4 | 0 | 0 | 4 |
| Dayuse Rate API | 9 | 0 | 1 | 10 |
| Rate Code stayType | 5 | 1 | 0 | 6 |
| 예약 API | 7 | 1 | 1 | 9 |
| 프론트데스크 API | 4 | 0 | 0 | 4 |
| 대시보드 API | 1 | 0 | 0 | 1 |
| **합계** | **34** | **2** | **3** | **39** |

## 발견 이슈 (수정 우선순위)

### ❌ FAIL (수정 필수)

| # | 이슈 | 심각도 | 설명 |
|---|------|--------|------|
| F-1 | Dayuse 예약 생성 시 당일 체크인=체크아웃 불가 | **HIGH** | `validateDates(masterCheckIn, masterCheckOut)`에서 `!checkOut.isAfter(checkIn)` 조건이 당일도 차단. Dayuse는 checkIn=checkOut 가능해야 함. **현재 우회**: masterCheckOut=checkIn+1 전송. `ReservationServiceImpl:251` |
| F-2 | stayType 값 검증 누락 | **MEDIUM** | RateCode 생성/수정 시 stayType에 "INVALID_TYPE" 등 임의 문자열 허용. OVERNIGHT/DAY_USE만 허용하는 enum 검증 필요. `RateCodeCreateRequest/UpdateRequest` |

### ⚠️ WARNING (개선 권장)

| # | 이슈 | 심각도 | 설명 |
|---|------|--------|------|
| W-1 | body 없는 로그인 요청 시 HTTP 500 | LOW | 빈 body POST 시 400 대신 500 반환. ExceptionHandler 보강 필요 |
| W-2 | 존재하지 않는 DayUseRate 삭제 시 HTTP 200 | LOW | ID=99999 삭제가 200 반환. 404 반환 권장 |
| W-3 | 캘린더 응답에 stayType 필드 누락 | MEDIUM | 캘린더 뷰에서 dayuse/overnight 구분 불가. UI에서 뱃지 표시 등에 필요 |

### Dayuse 사이드이펙트 점검 결과

| 영역 | 결과 | 상세 |
|------|------|------|
| Rate Code CRUD | ✅ 이상없음 | stayType 필드 추가, 기존 OVERNIGHT 동작 영향 없음 |
| DayUseRate CRUD | ✅ 정상 | 신규 3개 엔드포인트 모두 계약 준수 |
| 예약 생성 | ✅ 정상* | *masterCheckOut 보정 필요 (F-1) |
| 요금 계산 | ✅ 정상 | DayUseRate 기반 supplyPrice → tax/svc 정상 계산 |
| 프론트데스크 | ✅ 이상없음 | stayType 필드 정상 노출, 기존 로직 영향 없음 |
| 대시보드 | ✅ 이상없음 | KPI 정상 반환 |
| 가용객실 | ✅ 정상 | roomTypeId 기반 조회 정상 |
| 예약 취소 | ✅ 정상 | Dayuse 예약 취소 정상 |
