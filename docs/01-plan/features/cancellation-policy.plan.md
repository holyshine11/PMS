---
feature: cancellation-policy
phase: plan
createdAt: 2026-03-14
level: Dynamic
module: hola-reservation (M07) + hola-hotel (M01)
---

# Plan: 취소 정책 엔진 (Cancellation Policy Engine)

## 1. 개요

### 1.1 목적
예약 취소 시 체크인까지 남은 일수 기반으로 취소 수수료를 자동 계산하고,
게스트가 부킹엔진에서 직접 취소할 수 있는 셀프서비스 기능을 구현한다.

### 1.2 배경
- **산화정보통신 API 참조**: 다단계 취소 레벨 (`cncl_lev_code`), 일수 기반 차등 수수료 (`cncl_pot_code: D1/D2`), 시간 기반 마감 (`cncl_appl_time`)
- **현재 상태**: `CancellationFee` 엔티티(M01)는 Admin CRUD만 존재하고, 부킹엔진/예약 취소 시 실제 수수료 계산에 활용되지 않음
- **Admin 취소**: `ReservationServiceImpl.cancel()`에서 상태만 CANCELED로 변경, 수수료 계산 없음

### 1.3 범위

| 포함 | 제외 |
|------|------|
| 취소 수수료 자동 계산 서비스 | PG사 실결제 환불 (MockGateway 유지) |
| Admin 예약 취소 시 수수료 반영 | 노쇼 정책 (별도 feature) |
| 게스트 부킹엔진 자가 취소 API + UI | 게스트 My Page 전체 (별도 feature) |
| 취소 수수료 미리보기 API | 이메일/SMS 취소 알림 |
| PaymentTransaction REFUND 타입 | Deposit Rule 엔진 (별도 feature) |
| ReservationPayment 취소/환불 필드 추가 | |

---

## 2. 현재 구현 분석

### 2.1 기존 엔티티 (활용 가능)

**CancellationFee** (`hola-hotel` M01)
```
테이블: htl_cancellation_fee
필드: property_id, checkin_basis(DATE|NOSHOW), days_before, fee_amount, fee_type(PERCENTAGE|FIXED_KRW|FIXED_USD)
API: GET/PUT /api/v1/properties/{id}/cancellation-fees (Admin 벌크 CRUD)
서비스: CancellationFeeService (조회/저장)
```

**MasterReservation** (`hola-reservation` M07)
```
상태: RESERVED → CANCELED (cancel() 메서드)
허용: RESERVED, CHECK_IN 상태에서만 취소 가능
```

**ReservationPayment** (`hola-reservation` M07)
```
현재 필드: totalRoomAmount, totalServiceAmount, totalServiceChargeAmount,
           totalAdjustmentAmount, totalEarlyLateFee, grandTotal, totalPaidAmount
결제 상태: UNPAID, PARTIAL, PAID, OVERPAID
누락 필드: cancelFeeAmount, refundAmount
```

**PaymentTransaction** (`hola-reservation` M07)
```
현재 타입: CARD, CASH (paymentMethod)
현재 상태: COMPLETED (transactionStatus)
누락: REFUND 타입, CANCEL_FEE 용도 구분
```

### 2.2 Gap 요약

| 항목 | 현재 | 목표 |
|------|------|------|
| 취소 수수료 계산 | 없음 | CancellationFee 기반 자동 계산 |
| Admin 취소 플로우 | 상태만 변경 | 수수료 계산 → 확인 → 취소 + 결제 조정 |
| 게스트 자가 취소 | 없음 | 수수료 미리보기 → 확인 → 취소 |
| 환불 추적 | PaymentAdjustment로 수동 | cancelFeeAmount + refundAmount 자동 |
| PaymentTransaction | 결제만 기록 | REFUND 거래도 기록 |

---

## 3. 구현 계획

### 3.1 백엔드 구현 항목

#### STEP 1: 취소 수수료 계산 서비스 (핵심)
- **CancellationPolicyService** 생성 (`hola-reservation/booking/service/`)
  - `calculateCancelFee(Long propertyId, LocalDate checkInDate)` → `CancelFeeResult`
    - 체크인까지 남은 일수 계산
    - `CancellationFeeRepository`에서 property의 정책 조회
    - `daysBefore` 기준으로 매칭되는 정책 찾기 (가장 가까운 상위 매칭)
    - 수수료 계산: PERCENTAGE → 1박 요금의 N%, FIXED_KRW → 정액
  - `getCancelFeePreview(String confirmationNo, String email)` → `CancelFeePreviewResponse`
    - 인증 + 수수료 미리보기

#### STEP 2: ReservationPayment 확장
- **Flyway 마이그레이션**: `V4_11_0__add_cancel_refund_fields.sql`
  - `rsv_reservation_payment`에 컬럼 추가:
    - `cancel_fee_amount NUMERIC(15,2) DEFAULT 0`
    - `refund_amount NUMERIC(15,2) DEFAULT 0`
- **ReservationPayment 엔티티** 필드 추가
- **recalculatePayment()** 로직 업데이트

#### STEP 3: PaymentTransaction 확장
- `transactionStatus`에 `REFUNDED` 상태 추가
- `transactionType` 컬럼 추가 (PAYMENT, REFUND, CANCEL_FEE) — 또는 `paymentMethod`에 REFUND 추가
- Flyway: `V4_12_0__add_transaction_type.sql`

#### STEP 4: Admin 취소 플로우 개선
- `ReservationServiceImpl.cancel()` 수정:
  1. CancellationPolicyService로 수수료 계산
  2. ReservationPayment에 cancelFeeAmount 반영
  3. refundAmount 계산 (totalPaidAmount - cancelFeeAmount)
  4. PaymentTransaction에 REFUND 기록
  5. 상태 변경 CANCELED

#### STEP 5: 게스트 부킹엔진 취소 API
- **BookingApiController** 엔드포인트 추가:
  - `GET /api/v1/booking/reservations/{confirmationNo}/cancel-fee?email=` — 수수료 미리보기
  - `POST /api/v1/booking/reservations/{confirmationNo}/cancel` — 자가 취소 실행
- **BookingService** 메서드 추가:
  - `getCancelFeePreview(confirmationNo, email)` — 인증 + 수수료 계산
  - `cancelBooking(confirmationNo, email)` — 인증 + 취소 실행

#### STEP 6: BookingAuditLog 이벤트 추가
- `BOOKING_CANCELED` 이벤트 타입 추가
- 취소 시 감사 로그 기록 (수수료 금액 포함)

### 3.2 프론트엔드 구현 항목

#### STEP 7: 게스트 취소 UI
- **cancellation.html** 템플릿 생성 (`templates/booking/`)
  - 이메일 인증 → 예약 정보 표시 → 취소 수수료 안내 → 최종 확인 → 취소 완료
- **BookingViewController** 라우트 추가:
  - `GET /booking/{propertyCode}/cancel/{confirmationNo}` — 취소 페이지
- **booking.js** `CancellationPage` 클래스 추가
- **SecurityConfig** permitAll 추가: `/booking/**/cancel/**`

#### STEP 8: Admin 취소 UI 개선 (선택)
- 예약 상세 페이지에서 취소 시 수수료 확인 모달 표시
- reservation-detail.js 수정

### 3.3 구현 순서 (의존성 기반)

```
STEP 1 (계산 서비스) → STEP 2 (Payment 확장) → STEP 3 (Transaction 확장)
         ↓
STEP 4 (Admin 취소 개선) → STEP 8 (Admin UI, 선택)
         ↓
STEP 5 (게스트 API) → STEP 6 (감사 로그) → STEP 7 (게스트 UI)
```

---

## 4. 데이터 모델 변경

### 4.1 신규 테이블: 없음
- `htl_cancellation_fee`는 이미 존재, 재활용

### 4.2 기존 테이블 변경

**rsv_reservation_payment** (ALTER)
```sql
ALTER TABLE rsv_reservation_payment
  ADD COLUMN cancel_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
  ADD COLUMN refund_amount NUMERIC(15,2) NOT NULL DEFAULT 0;
```

**rsv_payment_transaction** (ALTER)
```sql
ALTER TABLE rsv_payment_transaction
  ADD COLUMN transaction_type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT';
-- PAYMENT: 결제, REFUND: 환불, CANCEL_FEE: 취소수수료
```

### 4.3 CancellationFee 매칭 로직 (핵심 알고리즘)

```
입력: propertyId, checkInDate
로직:
  1. remainingDays = checkInDate - today
  2. policies = CancellationFeeRepo.findByPropertyId(propertyId)
                  .filter(checkinBasis == "DATE")
                  .sortBy(daysBefore DESC)
  3. matchedPolicy = policies.find(p -> remainingDays <= p.daysBefore)
     - 없으면 → 수수료 0 (무료 취소)
  4. fee 계산:
     - PERCENTAGE → firstNightSupplyPrice × (feeAmount / 100)
     - FIXED_KRW → feeAmount 그대로
     - FIXED_USD → feeAmount × 환율 (현재 미지원, KRW만)

예시:
  정책: [30일전 0%, 7일전 50%, 당일 100%]
  체크인 3월20일, 오늘 3월18일 → 남은 2일 → 당일(0일) 정책 매칭 → 100%
  체크인 3월20일, 오늘 3월10일 → 남은 10일 → 7일전 정책 매칭 → 50%
  체크인 3월20일, 오늘 2월15일 → 남은 33일 → 30일전 정책 매칭 → 0%
```

---

## 5. API 설계

### 5.1 게스트 부킹엔진 API (Public, No Auth)

```
GET  /api/v1/booking/reservations/{confirmationNo}/cancel-fee?email={email}
→ 200: { cancelFeeAmount, cancelFeePercent, roomAmount, refundAmount, policyDescription }
→ 404: 예약 없음 또는 이메일 불일치
→ 400: 취소 불가 상태 (CANCELED, CHECKED_OUT 등)

POST /api/v1/booking/reservations/{confirmationNo}/cancel
Body: { "email": "guest@example.com" }
→ 200: { confirmationNo, status: "CANCELED", cancelFeeAmount, refundAmount }
→ 404: 예약 없음 또는 이메일 불일치
→ 400: 취소 불가 상태
```

### 5.2 Admin API (기존 확장)

```
DELETE /api/v1/properties/{propertyId}/reservations/{id}
→ 기존: 상태만 CANCELED
→ 변경: 수수료 계산 + ReservationPayment 업데이트 + REFUND 기록
→ 응답에 cancelFeeAmount, refundAmount 추가
```

---

## 6. 보안 고려사항

| 항목 | 대응 |
|------|------|
| 게스트 인증 | confirmationNo + email 이중 검증 (기존 확인 페이지와 동일) |
| 중복 취소 방지 | 이미 CANCELED 상태면 400 반환 |
| XSS 방지 | escapeHtml() 기존 패턴 적용 |
| 취소 남용 | BookingAuditLog에 clientIp + userAgent 기록 |
| SecurityConfig | `/api/v1/booking/**` 이미 permitAll, 추가 설정 불필요 |

---

## 7. 영향 범위 분석

### 7.1 수정 대상 파일

| 모듈 | 파일 | 변경 내용 |
|------|------|-----------|
| hola-reservation | `entity/ReservationPayment.java` | cancelFeeAmount, refundAmount 필드 추가 |
| hola-reservation | `entity/PaymentTransaction.java` | transactionType 필드 추가 |
| hola-reservation | `service/ReservationPaymentServiceImpl.java` | recalculate 로직 수정 |
| hola-reservation | `service/ReservationServiceImpl.java` | cancel() 수수료 연동 |
| hola-reservation | `booking/service/BookingServiceImpl.java` | cancelBooking(), getCancelFeePreview() 추가 |
| hola-reservation | `booking/controller/BookingApiController.java` | 취소 엔드포인트 2개 추가 |
| hola-reservation | `dto/response/PaymentSummaryResponse.java` | 취소/환불 필드 추가 |
| hola-reservation | `mapper/ReservationMapper.java` | 매핑 업데이트 |
| hola-app | `db/migration/V4_11_0__*.sql` | Payment 컬럼 추가 |
| hola-app | `db/migration/V4_12_0__*.sql` | Transaction 타입 추가 |
| hola-app | `templates/booking/cancellation.html` | 게스트 취소 UI |
| hola-app | `static/js/booking.js` | CancellationPage 클래스 |
| hola-common | `exception/ErrorCode.java` | 취소 관련 에러코드 추가 |

### 7.2 신규 생성 파일

| 모듈 | 파일 | 용도 |
|------|------|------|
| hola-reservation | `booking/service/CancellationPolicyService.java` | 취소 수수료 계산 서비스 |
| hola-reservation | `booking/dto/CancelFeePreviewResponse.java` | 수수료 미리보기 응답 |
| hola-reservation | `booking/dto/CancelBookingRequest.java` | 취소 요청 DTO |
| hola-reservation | `booking/dto/CancelBookingResponse.java` | 취소 결과 응답 DTO |

### 7.3 영향 없는 모듈
- hola-hotel: CancellationFee 읽기 전용 참조 (수정 없음)
- hola-room: 영향 없음
- hola-rate: 영향 없음

---

## 8. 테스트 시나리오

| # | 시나리오 | 기대 결과 |
|---|---------|-----------|
| T1 | 30일 전 취소 (무료 구간) | 수수료 0원, 전액 환불 |
| T2 | 7일 전 취소 (50% 구간) | 1박 요금의 50% 수수료 |
| T3 | 당일 취소 (100% 구간) | 1박 요금 전액 수수료 |
| T4 | 정책 미설정 프로퍼티 | 수수료 0원 (무료 취소) |
| T5 | 이미 취소된 예약 재취소 | 400 에러 |
| T6 | CHECKED_OUT 예약 취소 | 400 에러 |
| T7 | 이메일 불일치 | 404 에러 |
| T8 | 결제 완료 후 취소 | 수수료 차감 후 환불금액 계산 |
| T9 | 미결제 예약 취소 | 수수료만 부과, 환불 0 |
| T10 | Admin 취소 시 수수료 표시 | PaymentSummary에 반영 |

---

## 9. 리스크 & 완화

| 리스크 | 영향 | 완화 |
|--------|------|------|
| CancellationFee 정책 미설정 시 | 수수료 계산 불가 | 정책 없으면 무료 취소로 처리 |
| 실결제 환불 미지원 | 환불 금액만 기록, 실제 환불 X | MockGateway.cancel() 활용, 향후 PG 연동 시 확장 |
| 시간 기반 마감 미구현 | 산화정보통신의 cncl_appl_time | 현 단계에서는 일수 기반만 구현, 시간은 v2에서 |
| 다단계 취소 레벨 미구현 | 산화정보통신의 cncl_lev_code | 현재 CancellationFee가 단일 레벨, 충분 |

---

## 10. 완료 조건 (Definition of Done)

- [ ] CancellationPolicyService 수수료 계산 정상 동작
- [ ] ReservationPayment에 cancelFeeAmount, refundAmount 반영
- [ ] PaymentTransaction에 REFUND 거래 기록
- [ ] Admin 취소 시 수수료 자동 계산 + 결제 조정
- [ ] 게스트 수수료 미리보기 API 정상 응답
- [ ] 게스트 자가 취소 API 정상 동작
- [ ] 게스트 취소 UI (cancellation.html) 정상 렌더링
- [ ] BookingAuditLog에 BOOKING_CANCELED 기록
- [ ] 컴파일 성공 (`./gradlew compileJava`)
- [ ] 10개 테스트 시나리오 수동 검증
