---
status: awaiting_human_verify
trigger: "Bug 1: NO_SHOW allowed when check-in date hasn't passed. Bug 2: Retry PG refund shows success even when it fails."
created: 2026-03-30T00:00:00+09:00
updated: 2026-03-30T00:00:00+09:00
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: CONFIRMED both root causes.
test: N/A — root causes confirmed, applying fixes
expecting: N/A
next_action: Awaiting human verification of both fixes in test environment.

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: (1) NO_SHOW only allowed when check-in date has passed and guest didn't arrive. (2) Retry button shows failure message when PG refund fails.
actual: (1) NO_SHOW was allowed for reservation GMP260330-0008 possibly with future check-in date. (2) Retry always shows "PG환불이 완료되었습니다" regardless of actual result.
errors: No server errors — incorrect UI behavior only
reproduction: (1) Create reservation with future check-in date, attempt NO_SHOW from admin. (2) Have PG_REFUND_FAILED transaction, click retry (PG fails in test env).
started: Found during testing of newly implemented PG cancel/refund feature

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: Backend changeStatus() might have a date guard already
  evidence: Read the entire changeStatus() method (lines 610-720). STATUS_TRANSITIONS matrix only validates current status string. No date check anywhere in the NO_SHOW path. Backend gap confirmed.
  timestamp: 2026-03-30

- hypothesis: Frontend confirmStatusChange() might guard the NO_SHOW date before showing preview
  evidence: confirmStatusChange() (lines 932-969) for NO_SHOW immediately delegates to showCancelPreview(true) with zero date validation. No date check before or inside showCancelPreview.
  timestamp: 2026-03-30

- hypothesis: retryPgRefund() might throw an exception on PG failure
  evidence: Lines 436-449: try/catch wraps the PG call. On failure (result.isSuccess()==false) it only calls failedTxn.updateMemo(...) and logs. On exception it similarly only updates memo. Both paths fall through to line 451 which returns PaymentSummaryResponse wrapped in HolaResponse.success() at the controller level.
  timestamp: 2026-03-30

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-03-30
  checked: ReservationServiceImpl.STATUS_TRANSITIONS (line 92) + changeStatus() (lines 610-720)
  found: STATUS_TRANSITIONS only checks state machine transitions. changeStatus() for NO_SHOW path does: status transition validation → validateCancelFeePayment → applyStatusChange → processNoShow. Zero date-based guard anywhere.
  implication: Any RESERVED reservation can be set to NO_SHOW regardless of whether check-in date has passed or is in the future.

- timestamp: 2026-03-30
  checked: reservation-detail.js confirmStatusChange() line 940, showCancelPreview() lines 976-1037
  found: NO_SHOW triggers showCancelPreview(true). Preview modal shows checkIn date (d.checkIn from response) but never validates it against today before enabling the "노쇼 확인" button. The AdminCancelPreviewResponse already includes the checkIn field as a string.
  implication: Frontend has all the data it needs (d.checkIn is already in the preview response) to add a date guard — just needs a comparison before enabling the confirm button.

- timestamp: 2026-03-30
  checked: ReservationPaymentServiceImpl.retryPgRefund() lines 436-457
  found: try/catch swallows both failure cases (result.isSuccess()==false and exception). Both paths update the txn memo but do NOT change transactionStatus away from PG_REFUND_FAILED. Method always returns a PaymentSummaryResponse. Controller wraps in HolaResponse.success().
  implication: res.success is always true. Frontend if(res.success) unconditionally shows "PG환불이 완료되었습니다."

- timestamp: 2026-03-30
  checked: reservation-payment.js retry handler lines 476-500
  found: success callback at line 486 checks only if(res.success) → always true → always shows "PG 환불이 완료되었습니다." The response data contains the full transactions array including the updated txn. The txn's transactionStatus will be "COMPLETED" on success or still "PG_REFUND_FAILED" on failure (memo updated but status unchanged). Fix: check the actual status of the retried transaction in res.data.transactions.
  implication: Frontend can determine success/failure by finding the txnId in res.data.transactions and checking its transactionStatus.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: |
  Bug 1: No date-based guard for NO_SHOW. The STATUS_TRANSITIONS matrix only validates the current reservation status string. Neither the backend (changeStatus) nor the frontend (showCancelPreview) checks whether today >= masterCheckIn before allowing NO_SHOW. Fix is best applied at the frontend preview modal (data already available in d.checkIn) AND at the backend changeStatus() as a definitive server-side guard.

  Bug 2: retryPgRefund() in ReservationPaymentServiceImpl catches all PG failures and swallows them, always returning PaymentSummaryResponse. The frontend success callback checks only res.success (always true) instead of checking the actual transactionStatus of the retried transaction in the response data.

fix: |
  Bug 1 — Backend (ReservationServiceImpl.changeStatus): Add date guard before NO_SHOW is applied: if masterCheckIn is after today, throw HolaException(RESERVATION_STATUS_CHANGE_NOT_ALLOWED). Frontend (showCancelPreview): Add date validation before enabling the "노쇼 확인" button — if d.checkIn > today, show error alert and disable/hide button.

  Bug 2 — Frontend only (reservation-payment.js): In the retry success callback, find the txn with matching txnId in res.data.transactions, check its transactionStatus. If "COMPLETED" → show success. If still "PG_REFUND_FAILED" → show failure alert and re-enable retry button.

verification: Build verified (compileJava clean). Awaiting manual test in running server.
files_changed:
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-pms/hola-app/src/main/resources/static/js/reservation-detail.js
  - hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js
