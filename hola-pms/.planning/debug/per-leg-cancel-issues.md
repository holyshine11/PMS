---
status: awaiting_human_verify
trigger: "Per-leg (SubReservation) cancellation issues: no payment validation, cancelled legs hidden in UI, grandTotal recalculation"
created: 2026-03-30T12:00:00+09:00
updated: 2026-03-30T12:00:00+09:00
---

## Current Focus

hypothesis: CONFIRMED and FIXED — Three issues addressed: (1) validateUnpaidBalance added to changeStatus, (2) CANCELED legs shown with strikethrough, (3) cancel preview displays unpaid balance warning
test: compileJava BUILD SUCCESSFUL, ReservationPaymentTest passes, 2 pre-existing test failures unchanged
expecting: User confirms per-leg cancel blocks unpaid legs and cancelled legs are visible in payment display
next_action: Await human verification

## Symptoms

expected: |
  1. Per-leg cancel of unpaid leg should be blocked with "미결제 잔액" error
  2. Cancelled legs should remain visible in payment display (with visual distinction)
  3. Payment summary should clearly show what happened
actual: |
  1. GMP260330-0050-02 (unpaid 363,000원) was cancelled without payment validation
  2. After cancelling Leg 02, payment display only shows Leg 01 data
  3. grandTotal recalculated to 968,000 excluding cancelled leg's 363,000
errors: No error thrown for unpaid leg cancellation
reproduction: Cancel unpaid leg in multi-leg reservation
started: Per-leg cancel path was recently added

## Eliminated

## Evidence

- timestamp: 2026-03-30T12:05:00+09:00
  checked: changeStatus() lines 725-854 in ReservationServiceImpl.java
  found: Per-leg CANCELED at line 752-758 calls validateCancelFeePayment() (cancel FEE check only). processCancel() at line 843 only runs when ALL legs are cancelled (derivedStatus=CANCELED). processPartialLegCancelRefund() at line 848 handles overpayment refund but never blocks unpaid cancel.
  implication: No unpaid balance validation exists for per-leg cancel path

- timestamp: 2026-03-30T12:06:00+09:00
  checked: reservation-payment.js renderChargeBreakdown() lines 140-280
  found: Five forEach loops skip CANCELED legs with `if (sub.roomReservationStatus === 'CANCELED') return;` at lines 141, 198, 247, 831, 860
  implication: Cancelled leg charges are completely invisible in payment display

- timestamp: 2026-03-30T12:07:00+09:00
  checked: recalculateAmounts() lines 577-633 in ReservationPaymentServiceImpl.java
  found: Lines 585, 603 skip CANCELED sub-reservations. grandTotal excludes cancelled legs. This is CORRECT behavior for remaining charges.
  implication: grandTotal recalculation is correct; the issue is only in UI visibility

- timestamp: 2026-03-30T12:08:00+09:00
  checked: getCancelPreview() lines 436-541 in ReservationServiceImpl.java
  found: Shows outstandingCancelFee (cancelFee - totalPaid) but NOT unpaidBalance (grandTotal - totalPaid). Cancel preview does not surface unpaid charges info.
  implication: Need to add unpaidBalance and grandTotal to preview response

- timestamp: 2026-03-30T12:09:00+09:00
  checked: AdminCancelPreviewResponse.java
  found: No grandTotal or unpaidBalance field exists
  implication: DTO needs new fields for unpaid balance display

## Resolution

root_cause: |
  Three related defects in per-leg (SubReservation) cancellation:
  1. changeStatus() per-leg cancel path only validated cancel FEE payment (validateCancelFeePayment),
     but NOT unpaid balance (grandTotal > totalPaid). cancel() had this check, changeStatus() did not.
  2. reservation-payment.js filtered out CANCELED legs entirely with `if (sub.roomReservationStatus === 'CANCELED') return;`
     in 5+ forEach loops, making cancelled leg charges completely invisible.
  3. Cancel preview API (getCancelPreview) returned outstandingCancelFee but NOT unpaidBalance/grandTotal,
     so the modal could not display or block on unpaid charges.
  Additionally: processCancel() and processNoShow() used updatePaymentStatus() instead of setPaymentStatusRefunded(),
  and cancel() had duplicated inline unpaid balance check instead of using shared method.
fix: |
  1. Created validateUnpaidBalance() shared method — checks grandTotal > totalPaid, throws CANCEL_UNPAID_BALANCE
  2. Added validateUnpaidBalance() call to: cancel(), changeStatus() per-leg cancel, changeStatus() bulk cancel
  3. Added grandTotal and unpaidBalance fields to AdminCancelPreviewResponse DTO
  4. Populated grandTotal/unpaidBalance in getCancelPreview() service method
  5. reservation-detail.js: Added unpaid balance warning in cancel preview modal (before cancel fee check),
     blocks confirm button with "미결제 잔액 결제 필요" message
  6. reservation-payment.js renderChargeBreakdown(): Changed from skip-CANCELED to show-with-strikethrough —
     cancelled legs rendered with opacity-50, text-decoration-line-through, "취소됨" badge, but NOT counted in totals
  7. reservation-payment.js receipt view: Same treatment for print/receipt rendering
  8. processCancel() and processNoShow() now use setPaymentStatusRefunded() instead of updatePaymentStatus()
  9. Refactored cancel() to use shared validateUnpaidBalance() instead of inline duplicate
verification: |
  - compileJava BUILD SUCCESSFUL
  - ReservationPaymentTest all pass
  - 175 tests total, 2 pre-existing failures (confirmed same failures on HEAD before changes)
  - Pre-existing failures: CHECK_IN cancel test (outdated assumption) and NO_SHOW test (future date check-in)
files_changed:
  - hola-reservation/src/main/java/com/hola/reservation/dto/response/AdminCancelPreviewResponse.java
  - hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-app/src/main/resources/static/js/reservation-payment.js
  - hola-app/src/main/resources/static/js/reservation-detail.js
