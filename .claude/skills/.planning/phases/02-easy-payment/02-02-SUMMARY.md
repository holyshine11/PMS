---
phase: 02-easy-payment
plan: "02"
subsystem: booking-frontend
tags: [easy-pay, billkey, mobile, ux, frontend]
dependency_graph:
  requires: []
  provides: [email-guidance-message, mobile-pageshow-polling]
  affects: [booking-checkout-page]
tech_stack:
  added: []
  patterns: [pageshow-bfcache-handling, sessionStorage-signaling, polling-with-timeout]
key_files:
  modified:
    - hola-pms/hola-app/src/main/resources/static/js/booking.js
decisions:
  - "pageshow 이벤트로 bfcache 복원 및 일반 네비게이션 모두 포괄 처리"
  - "폴링 최대 횟수 10회(1.5초 간격)로 제한하여 무한 폴링 방지"
  - "타임아웃 시에도 loadEasyPayCards() 호출하여 최선의 UX 제공"
metrics:
  duration: "~10 minutes"
  completed: "2026-03-30T02:03:51Z"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 1
---

# Phase 02 Plan 02: booking.js Frontend Gap Patches Summary

Email-empty guidance message in renderEasyPayCards (D-01) and mobile pageshow-based billkey result polling after KICC redirect.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | renderEasyPayCards에 이메일 미입력 안내 메시지 추가 (D-01) | cba2037 | booking.js |
| 2 | 모바일 리다이렉트 복귀 시 pageshow 이벤트로 카드 목록 갱신 | 147b1c9 | booking.js |

## What Was Built

**Task 1 — 이메일 미입력 안내 메시지 (D-01)**

`renderEasyPayCards` 함수 상단에 이메일 체크 로직 추가. 이메일 미입력 상태에서 간편결제 탭을 선택하면 `#easyPayCards` 컨테이너에 Font Awesome 인포 아이콘과 함께 "이메일을 먼저 입력하면 등록된 카드를 확인할 수 있습니다." 안내 메시지를 표시하고 카드 추가 버튼은 숨김. `selectedCardId`를 null로 초기화하여 빈 상태를 명확히 표현.

**Task 2 — 모바일 pageshow 폴링**

`bindEvents` 함수 내 기존 `postMessage` 리스너 바로 뒤에 `pageshow` 이벤트 핸들러 추가. 모바일에서 KICC 빌키 등록 페이지로 이동 시 `sessionStorage`에 저장된 `hola_billkey_shopOrderNo`를 감지하여, 페이지 복귀 시 `/easy-pay/billkey-result` 엔드포인트를 최대 10회(1.5초 간격) 폴링. SUCCESS 시 카드 목록 갱신 + 성공 토스트, FAILED 시 에러 토스트, 타임아웃 시에도 카드 목록 갱신 시도.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Self-Check: PASSED

- [x] `booking.js` modified — `cba2037`, `147b1c9` commits exist
- [x] `이메일을 먼저 입력` string present in booking.js
- [x] `pageshow` event handler present in booking.js
- [x] `hola_billkey_shopOrderNo` present 3 times (set + get + remove)
- [x] `billkey-result` polling endpoint referenced
- [x] `pollBillkeyResult` function with `maxPoll` limit present
- [x] Existing PC postMessage flow unchanged
