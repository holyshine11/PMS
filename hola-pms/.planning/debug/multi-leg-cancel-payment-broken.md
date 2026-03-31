---
status: awaiting_human_verify
trigger: "Continue debugging multi-leg-cancel-payment-broken. Previous fix (finalizeAfterCancel zeroing grandTotal to 0) was incorrect."
created: 2026-03-30T00:00:00
updated: 2026-03-30T00:00:00
---

## Current Focus

hypothesis: Fix applied and self-verified. Awaiting human verification.
test: compileJava passes, ReservationPaymentTest passes, no new test failures introduced
expecting: User confirms cancel flow works correctly in real environment
next_action: Wait for user confirmation

## Symptoms

expected: After cancel, grandTotal should be preserved for audit; unpaid reservations should be blocked from cancelling
actual: finalizeAfterCancel() zeroes grandTotal to 0, losing original total amount for history/auditing
errors: grandTotal=0 after cancel (should retain original amount)
reproduction: Cancel any reservation with payment -> grandTotal becomes 0
started: Previous fix introduced finalizeAfterCancel

## Eliminated

- hypothesis: updatePaymentStatus() alone can handle cancel flow
  evidence: updatePaymentStatus compares totalPaid vs grandTotal - after cancel with refund, this comparison breaks because grandTotal still shows original amount while totalPaid stays the same but refund was processed. Need explicit REFUNDED status.
  timestamp: 2026-03-30

## Evidence

- timestamp: 2026-03-30
  checked: ReservationPayment.finalizeAfterCancel()
  found: Sets grandTotal=0 and paymentStatus="REFUNDED" - the grandTotal=0 destroys audit trail
  implication: Must remove this method, set REFUNDED directly without zeroing grandTotal

- timestamp: 2026-03-30
  checked: ReservationServiceImpl.java lines 587, 1051, 1088
  found: Three call sites of finalizeAfterCancel - cancel(), processCancel(), processNoShow()
  implication: All three need to be replaced with direct paymentStatus = "REFUNDED" setter

- timestamp: 2026-03-30
  checked: cancel() method validation logic
  found: Currently only validates cancelFee > totalPaid (CANCEL_FEE_UNPAID). Does NOT validate that full grandTotal is paid before allowing cancel.
  implication: Need to add unpaid balance validation before cancel processing

- timestamp: 2026-03-30
  checked: BookingServiceImpl.cancelBooking() at line 1135
  found: Guest self-cancel path calls payment.updatePaymentStatus() after cancel, which would compare totalPaid vs grandTotal and set PAID (not REFUNDED) since grandTotal is now preserved
  implication: Must also fix BookingServiceImpl to use setPaymentStatusRefunded()

- timestamp: 2026-03-30
  checked: updatePaymentStatus() vulnerability
  found: If any code path calls updatePaymentStatus() after cancel (e.g. recalculatePayment), the REFUNDED status would be overwritten to PAID/OVERPAID based on grandTotal vs totalPaid comparison
  implication: Added REFUNDED guard at the top of updatePaymentStatus() to protect against accidental overwrite

## Resolution

root_cause: finalizeAfterCancel() zeroes grandTotal to 0, destroying audit trail. Also missing validation to block cancel when unpaid balance exists. BookingServiceImpl cancel also used updatePaymentStatus() instead of explicit REFUNDED.
fix: |
  1) Replaced finalizeAfterCancel() with setPaymentStatusRefunded() in ReservationPayment.java (preserves grandTotal)
  2) Added unpaid balance validation (grandTotal > totalPaid -> block with CANCEL_UNPAID_BALANCE) in cancel() and processCancel()
  3) Added REFUNDED guard in updatePaymentStatus() to prevent accidental overwrite
  4) Fixed BookingServiceImpl guest self-cancel to use setPaymentStatusRefunded()
  5) Added CANCEL_UNPAID_BALANCE error code (HOLA-4084)
  6) Updated JS to show "총액 | 결제 | 환불 | 수수료" for REFUNDED status
  7) Updated tests to verify grandTotal preservation
verification: compileJava BUILD SUCCESSFUL, ReservationPaymentTest all pass. 2 pre-existing test failures unrelated to this change (confirmed failing at HEAD).
files_changed:
  - hola-common/src/main/java/com/hola/common/exception/ErrorCode.java
  - hola-reservation/src/main/java/com/hola/reservation/entity/ReservationPayment.java
  - hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java
  - hola-app/src/main/resources/static/js/reservation-payment.js
  - hola-reservation/src/test/java/com/hola/reservation/entity/ReservationPaymentTest.java
