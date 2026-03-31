---
status: awaiting_human_verify
trigger: "Fix reservation cancellation refund flow to work like Opera PMS — PG auto-refund, cash/VAN manual refund with admin confirmation alert"
created: 2026-03-30T12:00:00
updated: 2026-03-30T12:30:00
---

## Current Focus

hypothesis: Fix applied and self-verified. All 5 changes implemented. compileJava passes. 2 pre-existing test failures (unrelated to this change).
test: compileJava BUILD SUCCESSFUL, manual code review of all changes
expecting: User confirms cancel flow works correctly in real environment
next_action: Wait for user confirmation

## Symptoms

expected: PG refunds auto-process; cash/VAN refunds show admin confirmation alert before proceeding; different status for manual vs auto refunds
actual: Cash refund transaction auto-created with COMPLETED status; no confirmation alert for cash/VAN; no distinction in history
errors: None (logic issue)
reproduction: Cancel reservation with PG+Cash payments -> both refund as COMPLETED without cash confirmation
started: Original implementation

## Eliminated

(none yet)

## Evidence

- timestamp: 2026-03-30
  checked: ReservationPaymentServiceImpl.processRefundWithPg() lines 425-446
  found: Non-PG refund creates transaction with transactionStatus="COMPLETED" — should be MANUAL_CONFIRMED
  implication: Backend change needed

- timestamp: 2026-03-30
  checked: AdminCancelPreviewResponse.java
  found: Has refundBreakdowns with pgRefund flag but no top-level pgRefundAmount/nonPgRefundAmount fields
  implication: Need to add summary fields for frontend to determine if confirmation needed

- timestamp: 2026-03-30
  checked: reservation-detail.js showCancelPreview() + executeCancel()
  found: No confirmation dialog for non-PG refunds before calling cancel API
  implication: Need to add confirmation step between preview and cancel execution

- timestamp: 2026-03-30
  checked: reservation-payment.js renderPaymentTransactions()
  found: Only handles PG_REFUND_FAILED status specially; no handling for MANUAL_CONFIRMED
  implication: Need to display MANUAL_CONFIRMED with different badge

## Resolution

root_cause: processRefundWithPg() marks non-PG refunds as COMPLETED without admin confirmation. No frontend alert for cash/VAN refund amounts. No MANUAL_CONFIRMED status distinction.
fix: |
  1) AdminCancelPreviewResponse.java: Added pgRefundAmount, nonPgRefundAmount, nonPgRefundMethod fields
  2) ReservationServiceImpl.getCancelPreview(): Calculates PG vs non-PG refund split and populates new fields
  3) ReservationPaymentServiceImpl.processRefundWithPg(): Changed non-PG refund transactionStatus from COMPLETED to MANUAL_CONFIRMED
  4) reservation-detail.js showCancelPreview(): Added non-PG refund warning in modal + confirm() dialog before proceeding
  5) reservation-payment.js renderPaymentTransactions(): Shows MANUAL_CONFIRMED badge (orange) and PG refund badge (blue) in status column
  6) reservation-payment.js renderCancelInfo(): Shows manual refund detail row for MANUAL_CONFIRMED transactions
  7) detail.html: Added cpNonPgRefundRow and cancelManualRefundRow HTML elements
verification: compileJava BUILD SUCCESSFUL. 2 pre-existing test failures (unrelated). Code review confirms all changes are consistent.
files_changed:
  - hola-reservation/src/main/java/com/hola/reservation/dto/response/AdminCancelPreviewResponse.java
  - hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-reservation/src/main/java/com/hola/reservation/service/ReservationPaymentServiceImpl.java
  - hola-app/src/main/resources/static/js/reservation-detail.js
  - hola-app/src/main/resources/static/js/reservation-payment.js
  - hola-app/src/main/resources/templates/reservation/detail.html
