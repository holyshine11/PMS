---
status: awaiting_human_verify
trigger: "온라인 결제(부킹엔진) 예약건에서 룸 업그레이드/유료옵션 추가 후 추가 결제가 불가능하게 차단됨. 정책 변경이 필요함."
created: 2026-03-30T00:00:00+09:00
updated: 2026-03-30T00:01:00+09:00
---

## Current Focus

hypothesis: CONFIRMED - updateLeg()에서 paymentService.recalculatePayment() 호출 누락 + processPayment()에서 stale paymentStatus 선체크 문제
test: 완료 - 코드 트레이스로 확인
expecting: 수정 후 룸 업그레이드 후에도 결제 버튼이 표시되고 추가 결제가 가능해야 함
next_action: updateLeg()에 recalculatePayment 추가 + processPayment()의 stale 체크 순서 개선 + UI에서 추가 결제 버튼 로직 개선

## Symptoms

expected: 온라인 예약(부킹엔진)건도 룸 업그레이드나 유료옵션 추가 시 추가 결제(현금/카드 VAN)가 가능해야 함
actual: "온라인 결제 주문건이라 추가 결제가 불가능합니다" 와 같은 메시지가 나타남
errors: 추가 결제 차단 - 온라인 결제 주문건에 대한 정책 제한
reproduction: 1. 부킹엔진으로 온라인 결제 예약 생성 (GMP260330-0016) 2. 프론트데스크에서 룸 업그레이드 3. 유료 서비스옵션 추가 4. 추가 결제 시도 → 차단됨
started: 현재 정책으로 설계된 것으로 보임 (버그가 아닌 정책 변경 요청)

## Eliminated

(none yet)

## Eliminated

- hypothesis: processPayment()가 PG 결제 여부를 체크하여 추가 결제를 차단
  evidence: processPayment()에 PG/온라인 여부 체크 로직 없음. paymentStatus(PAID/OVERPAID) 체크만 있음
  timestamp: 2026-03-30T00:01:00+09:00

- hypothesis: reservation-payment.js에 isOta/pgPayment 기반 차단 로직 존재
  evidence: reservation-payment.js에서 isOta는 로드되지만 결제 버튼 표시 제어에 사용 안 됨. 버튼은 paymentStatus와 remaining으로만 제어됨
  timestamp: 2026-03-30T00:01:00+09:00

## Evidence

- timestamp: 2026-03-30T00:01:00+09:00
  checked: ReservationServiceImpl.java - updateLeg() 메서드 전체
  found: updateLeg()은 recalculateDailyCharges()를 호출하지만 paymentService.recalculatePayment()는 호출하지 않음. addService()는 recalculatePayment() 호출함.
  implication: 룸 업그레이드 후 ReservationPayment.grandTotal이 stale 상태 유지. paymentStatus=PAID 그대로.

- timestamp: 2026-03-30T00:01:00+09:00
  checked: ReservationPaymentServiceImpl.processPayment() 라인 94
  found: paymentStatus 체크(PAID/OVERPAID → 예외)가 recalculateAmounts() 호출 이전에 발생
  implication: updateLeg() 후 grandTotal은 증가했지만 paymentStatus=PAID인 채로 processPayment() 호출 시 RESERVATION_PAYMENT_ALREADY_COMPLETED 예외 발생

- timestamp: 2026-03-30T00:01:00+09:00
  checked: reservation-payment.js - renderPaymentStatus()
  found: paymentStatus=PAID이거나 remaining<=0이면 paymentButtonGroup.hide(). grandTotal stale → remaining=0 → 버튼 숨김
  implication: UI에서도 결제 버튼이 숨겨짐

- timestamp: 2026-03-30T00:01:00+09:00
  checked: ReservationMapper.toPaymentSummaryResponse() - remainingAmount
  found: remainingAmount = grandTotal.subtract(totalPaid).max(ZERO). grandTotal stale이면 0 반환.
  implication: getPaymentSummary API가 remaining=0 반환 → UI 버튼 숨김

## Resolution

root_cause: |
  두 가지 복합 원인:
  1. ReservationServiceImpl.updateLeg()에서 recalculateDailyCharges()는 호출하지만 paymentService.recalculatePayment()를 호출하지 않음.
     → 룸 업그레이드 후 ReservationPayment.grandTotal이 stale(이전 금액) 유지, paymentStatus=PAID 그대로
  2. ReservationPaymentServiceImpl.processPayment()에서 paymentStatus 체크(PAID → 예외)가 recalculateAmounts() 이전에 실행됨.
     → stale paymentStatus=PAID이면 추가 결제 시도 시 RESERVATION_PAYMENT_ALREADY_COMPLETED 예외 발생

  결과: 룸 업그레이드 후 UI에서 결제 버튼이 숨겨지고(remaining=0), 백엔드에서도 차단됨.
  서비스 추가(addService)는 정상 작동 - recalculatePayment() 호출 있음.

fix: |
  1. ReservationServiceImpl.updateLeg() 끝에 paymentService.recalculatePayment(master.getId()) 추가
  2. ReservationPaymentServiceImpl.processPayment()에서 PAID 체크를 recalculateAmounts() 이후로 이동하여 최신 grandTotal 기준으로 판단
  3. reservation-payment.js - renderPaymentStatus()에서 PAID 상태여도 remaining > 0이면 버튼 표시 (방어적 처리)
  Note: OTA 예약은 updateLeg() 자체가 RESERVATION_OTA_EDIT_RESTRICTED로 차단되므로 별도 처리 불필요

verification: |
  - ./gradlew :hola-reservation:test --tests "com.hola.reservation.service.ReservationPaymentServiceImplTest" → BUILD SUCCESSFUL
  - ./gradlew :hola-reservation:test --tests "com.hola.reservation.service.ReservationServiceImplTest" → 1 pre-existing failure (NO_SHOW 체크인 전 처리 불가 - 수정과 무관)
  - ./gradlew :hola-reservation:compileJava → BUILD SUCCESSFUL
files_changed:
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationPaymentServiceImpl.java
  - hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js
  - hola-pms/hola-reservation/src/test/java/com/hola/reservation/service/ReservationPaymentServiceImplTest.java
