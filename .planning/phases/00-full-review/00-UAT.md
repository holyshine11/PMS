---
status: complete
phase: 00-full-review
source: git log (commits 702fd5a..5c03669), 4 parallel code review agents, compile + unit tests
started: 2026-03-26T00:00:00+09:00
updated: 2026-03-26T01:00:00+09:00
---

## Current Test

[testing complete]

## Automated Verification Results

### Build & Test
- compileJava: PASS (all modules)
- Unit tests (hola-reservation, hola-hotel, hola-room, hola-common): ALL PASS
- Integration tests (hola-app): BLOCKED (Docker 미실행 - TestContainers PostgreSQL 필요)

---

## Issues Found

### CRITICAL

#### C-1. KICC 결제 승인 후 예약 생성 실패 시 PG 취소(롤백) 미구현
- **파일**: KiccPaymentApiController.java:152-187
- **증상**: `approveAfterAuth()` 성공(카드 결제 완료) 후 `createBookingWithPaymentResult()` 실패 시(재고 부족, DB 오류 등), catch 블록에서 로그만 남기고 **결제 취소를 하지 않음**
- **영향**: 고객 카드에서 돈은 빠져나가지만 예약은 생성되지 않음. 수동 환불 필요
- **복합 위험**: HIGH-1(cancelByPgCno HMAC 불일치)과 결합하면, 자동 취소 안전장치도 작동하지 않을 가능성 높음

---

### HIGH

#### H-1. cancelByPgCno() HMAC 생성 시 shopTransactionId 불일치
- **파일**: KiccPaymentGateway.java:225-242
- **증상**: HMAC 생성에 원본 `shopTransactionId`를 사용하지만, 취소 요청에는 새 UUID를 사용. KICC가 HMAC 검증 실패로 취소 거부 가능
- **영향**: 금액 불일치/HMAC 검증 실패 시 자동 취소가 작동하지 않아 결제만 남음

#### H-2. cancelByPgCno() 실패 시 무시(silent failure)
- **파일**: KiccPaymentGateway.java:240-242
- **증상**: 취소 API 호출 실패 시 로그만 남기고 리턴. 알림/재시도 메커니즘 없음
- **영향**: C-1 + H-1과 결합하면 모든 금액 불일치 건이 미환불 상태로 남음

#### H-3. postMessage origin 미검증
- **파일**: booking.js:792-793, payment-return.html:60,84
- **증상**: `window.addEventListener('message', ...)` 에서 `e.origin` 검증 없음. `postMessage(..., '*')` 로 모든 origin에 전송
- **영향**: 악의적 페이지가 결제 완료 위조 가능, 확인번호/propertyCode 유출 가능

#### H-4. HMAC 비교가 timing-safe 하지 않음
- **파일**: KiccHmacUtils.java:71
- **증상**: `String.equals()` 사용 (short-circuit). `MessageDigest.isEqual()` 사용해야 함
- **영향**: 타이밍 사이드채널 공격으로 HMAC 값 추론 가능 (실익은 제한적)

#### H-5. RoomStatusApiController 입력 검증 전무
- **파일**: RoomStatusApiController.java:29-46
- **증상**: `Map<String, String>` 사용, hkStatus/foStatus enum 검증 없음, assigneeId NumberFormatException 미처리, memo 길이 제한 없음
- **영향**: 잘못된 HK 상태값 저장 가능, 숫자 아닌 assigneeId 시 500 에러

#### H-6. RoomRackController getUserName() N+1 쿼리
- **파일**: RoomRackController.java:98-107, 201-205
- **증상**: HK 태스크 담당자명을 건별 조회. 30초 폴링 × 100객실 = 분당 수백 쿼리
- **영향**: 객실 수 증가 시 룸랙 로딩 속도 저하

#### H-7. cancelTask() 상태 가드 없음
- **파일**: HousekeepingServiceImpl.java:236-243
- **증상**: COMPLETED/INSPECTED 상태의 태스크도 취소 가능. 이미 CLEAN으로 변경된 객실 상태는 미복원
- **영향**: 감사 추적 무결성 훼손, 객실 상태 불일치

#### H-8. 대실(DayUse) 예약 수정 시 checkOut 미보정
- **파일**: ReservationServiceImpl.java:385, 404-407
- **증상**: `update()` 메서드에서 기존 대실 예약 수정 시 checkOut을 checkIn+1로 자동 보정하지 않음
- **영향**: 대실 예약의 checkout 날짜가 잘못 설정될 수 있음

#### H-9. 예약 목록 getList() 전체 조회 (페이징 없음)
- **파일**: ReservationServiceImpl.java:108-109
- **증상**: 프로퍼티 전체 예약을 DB에서 로드 후 Java에서 필터링
- **영향**: 예약 데이터 증가 시 메모리/성능 심각한 저하

#### H-10. getPaymentResult() 미인증 PII 노출
- **파일**: KiccPaymentApiController.java:195-211
- **증상**: `/api/v1/booking/payment/result` GET이 permitAll이며 rate limiting 없음. shopOrderNo만으로 투숙객명/금액/예약번호 등 PII 조회 가능
- **영향**: shopOrderNo 추측/유출 시 개인정보 노출

---

### MEDIUM

#### M-1. Redis TTL 만료 시 UX 혼란
- **파일**: KiccPaymentApiController.java:145-149
- **증상**: 결제 인증 완료 후 30분 초과 시 Redis 데이터 만료. KICC에서는 인증 성공이나 서버에서 "세션 만료" 표시

#### M-2. 에러 메시지 내부 정보 노출
- **파일**: KiccPaymentApiController.java:183
- **증상**: `e.getMessage()` 직접 사용자에게 표시 (SQL 에러 등 내부 정보 유출 가능)

#### M-3. 팝업 차단 시 이벤트 리스너 미해제
- **파일**: booking.js:848-852
- **증상**: 팝업 차단 시 `onKiccMessage` 리스너 미제거. 재클릭 시 중복 리스너 등록

#### M-4. postMessage 타임아웃 없음
- **파일**: booking.js:792-829
- **증상**: 결제 팝업 닫히거나 크래시 시 리스너/submitting 상태 영구 유지. 재시도 불가

#### M-5. 타임라인 캐시 키에 propertyId 미포함
- **파일**: reservation-timeline-view.js:68
- **증상**: 프로퍼티 전환 시 캐시 오염 가능 (현재는 loadToday에서 캐시 초기화로 완화)

#### M-6. HK 요약에 PICKUP/INSPECTED 상태 미집계
- **파일**: RoomStatusServiceImpl.java:115-124
- **증상**: PICKUP(검수 대기) 상태 객실이 요약 카드에서 누락

#### M-7. HK 태스크 배정 동시성 이슈
- **파일**: RoomStatusServiceImpl.java:77-112
- **증상**: 동시 배정 시 같은 객실에 중복 태스크 생성 가능 (@Version 없음)

#### M-8. DayUseTimeSlot 자정 교차 시 음수 duration
- **파일**: DayUseTimeSlot.java:36
- **증상**: startTime > endTime 구성 시 `durationHours()` 음수 반환

#### M-9. NoResourceFoundException 잘못된 에러코드
- **파일**: GlobalExceptionHandler.java:77
- **증상**: 404에 `HOLA-0004`(DUPLICATE_RESOURCE) 사용. `HOLA-0003`(RESOURCE_NOT_FOUND) 사용해야 함

#### M-10. HolaPms.ajax 401 시 로그인 리다이렉트 미구현
- **파일**: hola-common.js:66-86
- **증상**: 세션 만료 시 에러 토스트만 표시, 로그인 페이지 이동 안 됨

#### M-11. DayUse duration 파싱 regex 의존
- **파일**: reservation-form.js:1177-1181
- **증상**: 레이트코드명에서 `DU-(\d+)H` regex로 duration 추출. 네이밍 규칙 미준수 시 무시됨

#### M-12. reservation-form.js에서 대실 hidden field가 `<form>` 태그에 append되지만 form 태그 없음
- **파일**: reservation-form.js:621-624, reservation/form.html
- **증상**: `$('<input>').appendTo('form')` 실행되지만 form 태그 미존재. 대실 UX 경로(readonly checkout, info toast) 미작동
- **영향**: 대실 레이트코드 선택해도 프론트엔드 대실 특화 동작 안 됨 (서버에서 resolveStayType으로 보정)

#### M-13. BookingExceptionHandler에서 OptimisticLockException 미처리
- **파일**: BookingExceptionHandler.java
- **증상**: 동시 예약 충돌 시 "재시도" 대신 일반 서버 에러 표시

#### M-14. sanitizeGoodsName() UTF-8 잘림
- **파일**: KiccPaymentGateway.java:216-222
- **증상**: 50바이트 절단 시 한글 멀티바이트 문자 중간 절단 가능

---

### LOW (8건 요약)

| # | 파일 | 내용 |
|---|------|------|
| L-1 | booking.js:819 | 일부 실패 경로에서 sessionStorage 미정리 |
| L-2 | payment-return.html:80 | errorMessage innerHTML 사용 (XSS 방어층 약함) |
| L-3 | KiccPaymentGateway.java:124 | expectedAmount null 시 금액 검증 스킵 |
| L-4 | ReservationCalendarResponse | stayType/dayUse 시간 필드 없음 (타임라인 구분 불가) |
| L-5 | DayUseTimeSlot.java:17-21 | endTime이 property.dayUseEndTime 초과 검증 없음 |
| L-6 | PriceCalculationService.java:302 | roundingScale null 시 소수점 0 (비KRW 통화 문제) |
| L-7 | V4_15_0 migration | idx_rsv_master_email_lastname에 IF NOT EXISTS 누락 |
| L-8 | stay_type 컬럼 | 대실 쿼리용 인덱스 없음 |

---

## Severity Summary

| 등급 | 건수 | 핵심 |
|------|------|------|
| **CRITICAL** | 1 | 결제 승인 후 예약 실패 시 환불 미처리 |
| **HIGH** | 10 | PG 취소 HMAC 불일치, postMessage 보안, 입력 검증 부재, N+1 쿼리, 대실 수정 버그 |
| **MEDIUM** | 14 | 팝업 상태 관리, 캐시 오염, HK 상태 누락, 동시성, 에러코드 |
| **LOW** | 8 | 인덱스 누락, DTO 필드 누락, 마이그레이션 |

## Gaps

- truth: "결제 승인 후 예약 생성 실패 시 자동 환불 처리"
  status: failed
  reason: "catch 블록에서 paymentGateway.cancelPayment() 미호출. cancelByPgCno()도 HMAC 불일치로 작동 불가"
  severity: blocker
  test: C-1 + H-1 + H-2

- truth: "postMessage 통신의 origin 검증"
  status: failed
  reason: "booking.js에서 e.origin 미검증, payment-return.html에서 targetOrigin='*' 사용"
  severity: major
  test: H-3

- truth: "RoomStatus API 입력 검증"
  status: failed
  reason: "Map<String,String> raw 수신, enum/타입/길이 검증 전무"
  severity: major
  test: H-5

- truth: "RoomRack HK 담당자 조회 성능"
  status: failed
  reason: "getUserName() N+1 쿼리. 30초 폴링으로 누적"
  severity: major
  test: H-6

- truth: "예약 목록 조회 성능"
  status: failed
  reason: "전체 예약 로드 후 Java 필터링. 페이징/날짜 제한 없음"
  severity: major
  test: H-9
