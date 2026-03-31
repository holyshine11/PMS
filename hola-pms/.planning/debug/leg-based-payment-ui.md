---
status: awaiting_human_verify
trigger: "Restructure reservation payment tab UI from category-based to Leg-based layout"
created: 2026-03-30T00:00:00
updated: 2026-03-30T00:00:00
---

## Current Focus

hypothesis: UI restructuring task - replace category-based charge breakdown with Leg-based cards for multi-leg reservations
test: Visual verification after implementation
expecting: Multi-leg shows per-Leg cards with subtotals + payment buttons; single-leg keeps current layout
next_action: Implement HTML + JS changes

## Symptoms

expected: Leg-based layout showing per-Leg subtotals, per-Leg payment buttons, Opera PMS Billing style
actual: Category-based layout (room charges, services, service charges as separate toggleable sections)
errors: N/A (UI improvement, not a bug)
reproduction: Open any multi-leg reservation detail > payment tab
started: Always been category-based

## Evidence

- timestamp: 2026-03-30T00:00:00
  checked: detail.html lines 258-412 (payment tab section)
  found: Static HTML with chargeBreakdown container, 3 toggle sections (room/service/svcChg), earlyLate, adjustment, then summary/buttons/cancel/history
  implication: Need to replace chargeBreakdown inner content dynamically based on leg count

- timestamp: 2026-03-30T00:00:00
  checked: reservation-payment.js renderChargeBreakdown() lines 130-299
  found: Renders room charges per-leg within roomDetailContent, services mixed in serviceDetailContent, svcChg per-leg in svcChgDetailContent. Already has multi-leg awareness (Leg #N labels)
  implication: Refactoring needed to group by Leg instead of by category

- timestamp: 2026-03-30T00:00:00
  checked: reservation-detail.js cardPaymentBtn/cashPaymentBtn handlers (lines 1483-1501)
  found: cardPaymentBtn shows VAN info alert; cashPaymentBtn calls ReservationPayment.openCashPaymentModal(); Event binding uses IDs not classes
  implication: Need to change to class-based event delegation for per-Leg buttons

- timestamp: 2026-03-30T00:00:00
  checked: hola.css charge-related styles (lines 639-696)
  found: Existing styles for charge-detail-table, charge-toggle, charge-detail-wrap, charge-empty, row-subtotal
  implication: Can reuse existing styles; need new styles for Leg card wrapper

## Resolution

root_cause: UI design limitation - category-based grouping doesn't support per-Leg billing view
fix: |
  1. detail.html: Replaced static category-based chargeBreakdown sections with dynamic container.
     Moved earlyLate/adjustment to common area outside chargeBreakdown.
     Added cashPaymentModalTitle ID to cash payment modal for dynamic title.
  2. reservation-payment.js: Rewrote renderChargeBreakdown() to dispatch to renderMultiLegBreakdown()
     or renderSingleLegBreakdown() based on subs.length. Multi-leg renders per-Leg cards with
     header (Leg #N - RoomType + status badge + subtotal), body (room charges toggle, services,
     service charge), and per-Leg payment buttons. Single-leg preserves exact category-based layout.
     Updated bindSummary() to remove dead references. Updated renderPaymentStatus() to hide global
     buttons for multi-leg. Updated openCashPaymentModal() to accept legContext for per-Leg default
     amount and modal title.
  3. reservation-detail.js: Added event delegation for .leg-card-pay-btn (per-Leg payment buttons).
     Added .leg-card-pay-btn to readonly mode hide list.
  4. hola.css: Added leg-card, leg-card-header, leg-card-body, leg-section styles.
verification: pending human verification
files_changed:
  - hola-app/src/main/resources/templates/reservation/detail.html
  - hola-app/src/main/resources/static/js/reservation-payment.js
  - hola-app/src/main/resources/static/js/reservation-detail.js
  - hola-app/src/main/resources/static/css/hola.css
