# Hola PMS Regression 테스트 — Dayuse + 리팩토링 사이드이펙트 점검

> 테스트일: 2026-03-23 | 테스터: QC팀 | 버전: 2.0
> 대상 커밋: 5f6bddb ~ 현재 unstaged (63개 파일 변경)

---

## 테스트 범례
- ✅ PASS  ❌ FAIL  ⚠️ WARNING

---

## 변경 범위 요약

| 영역 | 변경 파일 수 | 핵심 변경 |
|------|------------|----------|
| Entity | 5 | Property (dayuse 4필드), RateCode (stayType), SubReservation (stayType/dayuse시간), DayUseRate (신규), BookingApiKey |
| DTO | 11 | Request/Response에 stayType, dayuse 필드 추가 |
| Service | 5 | RateCodeServiceImpl, ReservationServiceImpl, DashboardServiceImpl, FrontDeskServiceImpl, BookingServiceImpl |
| Controller | 8 | RateCodeApiController (dayuse 3 EP 추가), 기타 Swagger 어노테이션 |
| Mapper | 3 | RateCodeMapper, ReservationMapper, HotelMapper |
| Frontend | 10 | rate-code-form/page.js, reservation-form.js, fd-operations.js, dashboard.js, hola-common.js |
| Template | 12 | rate-code/form.html, booking/rooms.html, dashboard.html 등 |
| DB | 1 | V4_19_0__add_dayuse_support.sql |

---

# Regression #1: Dayuse 기능 회귀

| # | 시나리오 | 결과 | 상세 |
|---|---------|------|------|
| R1-01 | DayUseRate DB 정합성 | ✅ | DU-3H(3h/60K), DU-5H(5h/80K), DU-6H(6h/95K), DU-8H(8h/120K) |
| R1-02 | stayType 필드 일관성 | ✅ | DAY_USE 4개, OVERNIGHT 14개, MISSING 0개 |
| R1-03 | Property dayuse 설정 | ✅ | P4: 10:00~20:00/5h, P5: 10:00~20:00/5h, P6: 09:00~21:00/6h |
| R1-04 | DayUseRate CRUD | ✅ | 생성(201)/조회(200)/삭제(200) 모두 정상 (API 테스트에서 검증) |
| R1-05 | Dayuse ErrorCode | ✅ | HOLA-4026, HOLA-4027 정상 정의 |

**Regression #1 결과: ✅ PASS**

---

# Regression #2: 예약 로직 회귀

| # | 시나리오 | 결과 | 상세 |
|---|---------|------|------|
| R2-01 | Overnight 예약 생성 | ✅ | HTTP 201, stayType=OVERNIGHT, 2박 dailyCharge 정상 |
| R2-02 | 예약 상세 조회 (legs) | ✅ | stayType=OVERNIGHT, dailyCharges 2건 정상 |
| R2-03 | 레그 추가 (서브예약) | ✅ | HTTP 201, 새 leg stayType=OVERNIGHT |
| R2-04 | 예약 수정 | ✅ | HTTP 200, guestName 변경 정상 |
| R2-05 | 예약 취소 | ✅ | HTTP 200 |
| R2-06 | Dayuse 예약 생성 (API) | ✅ | stayType=DAY_USE, dayUseStartTime/EndTime 정상 |
| R2-07 | Dayuse 예약 취소 | ✅ | HTTP 200, 정상 soft delete |
| R2-08 | 예약 메모 CRUD | ✅ | GET 200, POST 201 |

**Regression #2 결과: ✅ PASS (기존 예약 CRUD에 사이드이펙트 없음)**

---

# Regression #3: 요금 계산 회귀

| # | 시나리오 | 기대값 | 실제값 | 결과 |
|---|---------|--------|--------|------|
| R3-01 | Overnight 2박 (RACK, STD-S) | 일별 분리 계산 | 6/10: 150K+16.5K+15K=181.5K, 6/11: 동일 | ✅ |
| R3-02 | Dayuse 3시간 (DU-3H) | supply=60K, tax=6.6K, svc=6K | supply=60,000 tax=6,600 svc=6,000 total=72,600 | ✅ |
| R3-03 | 세금 계산 (supply→tax→svc) | tax=(supply+svc)*10% | 6,600 = (60,000+6,000)*10% | ✅ |
| R3-04 | Dayuse 5시간 (DU-5H) | supply=80K | supply=80,000 tax=8,800 svc=8,000 total=96,800 | ✅ |
| R3-05 | Overnight 요금 변동 없음 | 기존 RACK 요금 유지 | STD-S: 150K/일 → 변동 없음 | ✅ |

**Regression #3 결과: ✅ PASS (요금 계산 정합성 확인)**

---

# Regression #4: 예약 상태값 전반

| # | 시나리오 | 결과 | 상세 |
|---|---------|------|------|
| R4-01 | INHOUSE 카운트 | ✅ | 9건 모두 INHOUSE 상태 |
| R4-02 | FD Summary 정합 | ✅ | arrivals=0, inHouse=9, departures=0, checkedIn=2, checkedOut=3 |
| R4-03 | FD all 상태 분포 | ✅ | INHOUSE:9, CHECKED_OUT:3, NO_SHOW:2, CANCELED:1 = 총 15건 |
| R4-04 | stayType 분포 | ✅ | 오늘 운영: OVERNIGHT 15건 (dayuse 예약은 4/15이므로 미포함) |
| R4-05 | 상태 전이 규칙 | ✅ | RESERVED→INHOUSE: 객실 배정 필요 (HOLA-5001), INHOUSE→CHECKED_OUT: 결제 확인 (HOLA-4029) |

**Regression #4 결과: ✅ PASS (상태 관리 사이드이펙트 없음)**

---

# Regression #5: 리팩토링 점검

| # | 시나리오 | 결과 | 상세 |
|---|---------|------|------|
| R5-01 | 전체 컴파일 | ✅ | BUILD SUCCESSFUL (10 tasks UP-TO-DATE) |
| R5-02 | 대시보드 API (3 프로퍼티) | ✅ | P4/5/6 모두 success=True |
| R5-03 | PropertyResponse 필드 완전성 | ✅ | dayuse 4필드 포함 15개 필수 필드 모두 존재 |
| R5-04 | RateCode List→Detail 일관성 | ✅ | 목록/상세 모두 stayType 포함 |
| R5-05 | ReservationMapper dayuse 매핑 | ✅ | stayType, dayUseStartTime, dayUseEndTime 모두 매핑 |
| R5-06 | FD Response stayType 매핑 | ✅ | FrontDeskOperationResponse에 stayType 포함 |
| R5-07 | ErrorCode 추가 | ✅ | HOLA-4026, HOLA-4027 정상 정의 |
| R5-08 | Flyway 마이그레이션 | ✅ | V4_19_0 적용 완료 (서버 정상 구동) |

**Regression #5 결과: ✅ PASS**

---

# 종합 결과

## 테스트 통계

| Regression 영역 | 시나리오 수 | PASS | FAIL | WARNING |
|-----------------|-----------|------|------|---------|
| #1 Dayuse 기능 | 5 | 5 | 0 | 0 |
| #2 예약 로직 | 8 | 8 | 0 | 0 |
| #3 요금 계산 | 5 | 5 | 0 | 0 |
| #4 상태값 전반 | 5 | 5 | 0 | 0 |
| #5 리팩토링 점검 | 8 | 8 | 0 | 0 |
| **합계** | **31** | **31** | **0** | **0** |

## 사이드이펙트 판정: **없음**

63개 파일 변경에 대해 31개 회귀 시나리오 전량 PASS.
기존 기능 (Overnight 예약/요금/상태/FD/대시보드)에 영향 없음 확인.

## 이전 테스트 (API/Web Flow)에서 발견된 이슈 재확인

| # | 이슈 | 심각도 | 영역 | 설명 |
|---|------|--------|------|------|
| F-1 | Dayuse 당일 checkIn=checkOut 차단 | HIGH | ReservationServiceImpl:251 | `validateDates`에서 dayuse 예외 처리 필요 |
| F-2 | stayType 값 검증 누락 | MEDIUM | RateCodeCreate/UpdateRequest | enum 검증 추가 필요 |
| F-3 | 관리자 예약폼 dayuse 생성 불가 | HIGH | reservation-form.js:1126 | stayType/durationHours 미전송 |

## 잔여 리스크

| 리스크 | 영향도 | 대응 |
|--------|--------|------|
| 관리자 UI에서 Dayuse 예약 불가 | HIGH | reservation-form.js에 stayType/dayUseDurationHours 전송 로직 추가 필요 |
| stayType 검증 없음 | MEDIUM | 서버 사이드 enum 검증 추가 |
| 부킹엔진 Dayuse 선택 후 예약 완료 미검증 | MEDIUM | 부킹엔진 예약 완료 플로우 별도 테스트 필요 |
