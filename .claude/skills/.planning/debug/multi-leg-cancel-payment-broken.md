---
status: awaiting_human_verify
trigger: "예약 GMP260330-0046에서 Leg1(부킹엔진 PG결제)과 Leg2(관리자 직접 예약) 있는 예약의 취소/환불 처리가 전반적으로 망가져있음"
created: 2026-03-30T00:00:00+09:00
updated: 2026-03-30T00:00:00+09:00
---

## Current Focus

hypothesis: cancel() 메서드가 취소 후 grandTotal을 재계산하지 않아 잔액(remaining)이 잘못 표시됨. 또한 미결제 잔액(Leg2 미결제분)이 있는 상태에서 취소를 허용하는 검증 누락.
test: cancel() 코드에서 recalculatePayment() 호출 누락 확인 완료
expecting: 취소 후 grandTotal이 0이 되어야 하는데 798,600으로 남아있을 것
next_action: 근본 원인 확정 후 수정 적용

## Symptoms

expected: |
  - 취소 시 Leg1은 PG결제 취소(KICC API), Leg2는 VAN(카드) 또는 현금 결제 취소가 별도로 처리되어야 함
  - Leg별로 취소 내역이 결제 이력에 각각 표시되어야 함
  - 잔액이 남아있으면 취소 처리가 되면 안되거나, 잔액 결제 버튼이 있어야 함
actual: |
  - Leg1의 PG결제만 취소되고 Leg2는 아무런 설명 없이 취소 처리됨
  - 결제 이력에 1건의 환불만 나옴 (PG 환불 1건)
  - 잔액 145,200원이 남아있는데도 예약이 취소 완료 상태
  - 잔액이 있는데 결제하는 버튼도 없음
  - 총액 798,600원, 결제 653,400원, 환불 479,160원, 취소수수료 174,240원
errors: 에러 메시지는 없음. 정상적으로 취소 완료된 것처럼 보임
reproduction: |
  1. 부킹엔진으로 Leg1 예약+PG결제
  2. 관리자가 Leg2 예약 추가 (PG결제 없음)
  3. 예약 전체 취소 버튼 클릭
  4. Leg1만 PG 환불되고 Leg2는 환불 처리 없이 취소됨
started: 현재 발생 중

## Eliminated

- hypothesis: processRefundWithPg가 비-PG 결제 환불을 누락
  evidence: processRefundWithPg는 PG/비PG 분배 로직이 이미 구현되어 있음 (line 329-446). 문제는 refundAmount 자체가 잘못 계산됨
  timestamp: 2026-03-30T00:10:00+09:00

## Evidence

- timestamp: 2026-03-30T00:05:00+09:00
  checked: cancel() 메서드 (ReservationServiceImpl.java:545-604)
  found: cancel()은 모든 sub를 CANCELED로 변경하지만 recalculatePayment()를 호출하지 않음. grandTotal이 양 Leg 합산(798,600)으로 유지됨
  implication: 취소 후 remainingAmount = grandTotal(798,600) - totalPaid(653,400) = 145,200으로 잘못 표시

- timestamp: 2026-03-30T00:06:00+09:00
  checked: cancel() 메서드의 환불 금액 계산 (line 571)
  found: refundAmt = totalPaid(653,400) - cancelFee(174,240) = 479,160. 이는 PG결제액 기준이지 전체 예약 총액 기준이 아님
  implication: Leg2의 미결제 145,200원은 취소 수수료에 반영되지 않고 환불에도 반영되지 않음 — 결과적으로 잔액이 유령처럼 남음

- timestamp: 2026-03-30T00:07:00+09:00
  checked: processRefundWithPg() (ReservationPaymentServiceImpl.java:304-463)
  found: PG/비PG 분배 로직은 정상. pgRefundAmount=479,160, nonPgRefundAmount=0 (PG 결제액 내에서 환불 완료). Leg2 결제가 없으므로 비PG 환불도 0
  implication: 환불 거래는 PG 1건만 생성됨 — 정상적으로 보이지만, Leg2에 대한 환불 기록이 없는 것은 미결제였으므로 맞음

- timestamp: 2026-03-30T00:08:00+09:00
  checked: updatePaymentStatus() (ReservationPayment.java:134-161)
  found: paid(653,400) < grandTotal(798,600) → paymentStatus = "PARTIAL"
  implication: 취소 완료 후에도 결제 상태가 PARTIAL로 남음 — 취소된 예약인데 미결제 잔액이 있는 것처럼 보임

- timestamp: 2026-03-30T00:09:00+09:00
  checked: ReservationMapper.toPaymentSummaryResponse() (line 306)
  found: remainingAmount = grandTotal.subtract(totalPaid).max(BigDecimal.ZERO) = 798,600 - 653,400 = 145,200
  implication: 프론트엔드에 잔액 145,200원이 표시됨

- timestamp: 2026-03-30T00:10:00+09:00
  checked: 결제 버튼 표시 로직 (reservation-payment.js:296-301)
  found: remaining(145,200) > 0 && status !== 'OVERPAID' → 결제 버튼 표시됨
  implication: 실제로는 버튼이 보일 수 있지만, CANCELED 상태에서 결제 시도하면 서버에서 거부됨 (processPayment에서 CANCELED 상태 체크)

- timestamp: 2026-03-30T00:11:00+09:00
  checked: processCancel() (ReservationServiceImpl.java:1019-1057) — changeStatus() 경로
  found: cancel()과 동일한 패턴. recalculatePayment() 미호출
  implication: changeStatus()로 CANCELED 전환해도 같은 문제 발생

## Resolution

root_cause: |
  cancel()/processCancel()/processNoShow()에서 취소 후 recalculatePayment()를 호출하지 않아 grandTotal이
  원래 양 Leg 합산(798,600)으로 유지됨. 이로 인해:
  1. remainingAmount = grandTotal(798,600) - totalPaid(653,400) = 145,200으로 잘못 표시
  2. paymentStatus가 "PARTIAL"로 남아 취소 완료된 예약인데 미결제처럼 보임
  3. 결제 버튼이 CANCELED 상태에서도 표시될 수 있음 (결제 시도 시 서버 거부)
  4. updatePaymentStatus()가 cancelFeeAmount/refundAmount를 고려하지 않음
fix: |
  1. ReservationPayment에 updatePaymentStatusAfterCancel() 추가 — 환불 처리 후 "REFUNDED" 상태 설정
  2. cancel()/processCancel()/processNoShow()에서 취소 후 grandTotal 재계산 + REFUNDED 상태 설정
  3. 프론트엔드에서 REFUNDED 상태 라벨 추가, CANCELED/NO_SHOW 예약에서 결제 버튼 숨김
verification: |
  - 컴파일 성공 (gradlew compileJava BUILD SUCCESSFUL)
  - ReservationPaymentTest 전체 통과 (새 finalizeAfterCancel 테스트 2개 포함)
  - 기존 실패 테스트 2개는 이번 변경과 무관 (git stash 전에도 동일하게 실패)
files_changed:
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/entity/ReservationPayment.java
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js
  - hola-pms/hola-reservation/src/test/java/com/hola/reservation/entity/ReservationPaymentTest.java
