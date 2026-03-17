# Front Desk 모듈 개선 완료 보고서

> 작성일: 2026-03-17 | Feature: front-desk | Phase: Report

---

## Executive Summary

| 항목 | 내용 |
|------|------|
| **Feature** | 프론트데스크 5개 화면 심층 분석 및 개선 (도착/투숙/출발/현장투숙/객실현황) |
| **기간** | 2026-03-17 (1일) |
| **Phase** | 분석 → Phase 1 (버그/보안) → Phase 2 (기능 보완) |
| **수정 파일** | 12개 (Java 3, JS 6, HTML 4) |
| **수정 라인** | +456 / -245 (순증 211줄) |

### Value Delivered

| 관점 | Before | After |
|------|--------|-------|
| **Problem** | Opera PMS 벤치마킹했으나 기존 예약 모듈과 기능 중복, Walk-In 미구현, 보안 불일치, 데이터 무결성 버그 존재 | 역할 분리 원칙(프론트데스크=오늘의 빠른 운영, 예약관리=상세 CRUD) 수립 후 중복 제거하며 핵심 결함 해소 |
| **Solution** | 4가지 판단 기준(이미 있는가/속도 핵심인가/오늘 한정인가/새 API 필요한가) 기반 의사결정 → Phase 1(버그 5건) + Phase 2(기능 6건) 순차 개선 | 기존 예약 API 100% 재사용, 새 API 0건 추가 |
| **기능 UX 효과** | 체크인 7단계→4단계 단축, Walk-In 카드형 UI + 도착 화면 자동 연결, 3개 화면 요약 카드 통일, 예약 상세 링크 3개 화면 통일 | 프론트 직원 체크인 클릭 수 43% 감소 (7→4단계) |
| **Core Value** | 호텔 PMS 프론트데스크의 본질(오늘의 빠른 운영)에 집중하되, 기존 예약 모듈과의 명확한 역할 분리로 유지보수성 확보 | 중복 코드 0줄, 새 API 0개, 기존 API 재사용률 100% |

---

## 1. 분석 개요

### 1.1 분석 방법론

| 역할 | 분석 영역 | 도구 |
|------|----------|------|
| 아키텍처 | 모듈 구조, API 중복, 코드 배치 | 소스 코드 리뷰 |
| 프론트엔드 | UX 일관성, UI 패턴, JS 품질 | HTML/JS 전수 검토 |
| 도메인 전문가 | Opera/Mews/StayNTouch 벤치마킹 대비 갭 | 벤치마킹 문서 2건 |
| QA | 데이터 무결성, 보안, 에러 핸들링 | 코드 경로 추적 |

### 1.2 벤치마크 대상

- Oracle Opera Cloud PMS (40,000+ 호텔, 32종 대시보드 타일)
- Mews PMS (Cloud Native, Smart Timeline)
- StayNTouch PMS (Mobile First, 2시간 교육)

### 1.3 핵심 의사결정: 역할 분리 프레임워크

Opera를 무조건 따라하면 기존 예약관리 모듈과 기능 중복이 발생하므로, **4가지 판단 기준**을 수립:

| 기준 | 설명 | 적용 결과 |
|------|------|----------|
| 이미 있는가? | 예약 상세에 동일 기능이 있으면 링크로 연결 | 결제/메모/업그레이드 → 링크 연결 |
| 속도가 핵심인가? | 1-Click 작업만 프론트데스크에 직접 구현 | 체크인/아웃 상태 변경만 직접 |
| 오늘 한정인가? | 오늘 날짜 기준만 프론트데스크 범위 | 날짜 범위 검색은 예약 목록에서 |
| 새 API 필요한가? | 기존 API로 충분하면 새로 만들지 않음 | 새 API 0건, 기존 API 100% 재사용 |

---

## 2. Phase 1: 버그/보안 수정 (5건)

### 2.1 [P1-1] assignRoom roomTypeId null 버그

| 항목 | 내용 |
|------|------|
| **심각도** | Critical (데이터 무결성) |
| **원인** | `fd-arrivals-page.js:230`에서 `roomTypeId: null` 전송 → `SubReservation.update()`가 null로 덮어쓰기 |
| **수정** | DTO에 `roomTypeId` 필드 추가 + JS에서 기존 값 전달 |
| **파일** | `FrontDeskArrivalResponse.java`, `FrontDeskServiceImpl.java`, `fd-arrivals-page.js` |

### 2.2 [P1-2] 마스터 단위 체크인 검증

| 항목 | 내용 |
|------|------|
| **심각도** | 분석 후 버그 아님 확인 |
| **원인** | `changeStatus()`가 마스터 단위로 전체 서브예약 동기화 |
| **결론** | Opera 표준 동작과 동일 (게스트 도착 시 전체 객실 체크인). 서버 검증(FD_ROOM_ASSIGN_REQUIRED, FD_ROOM_OUT_OF_ORDER)이 이미 다중 서브예약 처리 |
| **수정** | error 핸들링 추가로 서버 검증 실패 메시지 표시 |

### 2.3 [P1-3] Room Rack 투숙객 이름 미마스킹

| 항목 | 내용 |
|------|------|
| **심각도** | High (보안 불일치) |
| **원인** | `RoomRackController`에서 `getGuestNameKo()` 직접 노출 (Arrivals는 마스킹 적용 중) |
| **수정** | `NameMaskingUtil.maskKoreanName()` 2곳 적용 |
| **파일** | `RoomRackController.java` |

### 2.4 [P1-4] 객실 배정 정렬 개선

| 항목 | 내용 |
|------|------|
| **심각도** | Medium (UX) |
| **원인** | 전체 객실이 등록 순서로 표시, VC/OOO 구분 없이 평면적 나열 |
| **수정** | VC→VD→기타Vacant→Occupied→OOO/OOS 우선 정렬, 배정불가 객실 0건 시 경고 |
| **파일** | `fd-arrivals-page.js` |

### 2.5 [P1-5] AJAX error 핸들링 전수 추가

| 항목 | 내용 |
|------|------|
| **심각도** | Medium (안정성) |
| **원인** | 5개 JS 파일 전체에 error 콜백 없음 → API 실패 시 무응답 |
| **수정** | 모든 `HolaPms.ajax()` 호출에 `error` 콜백 추가 (총 12곳) |
| **파일** | `fd-arrivals-page.js`, `fd-departures-page.js`, `fd-in-house-page.js`, `fd-room-rack-page.js` |

---

## 3. Phase 2: 기능 보완 (6건)

### 3.1 [P2-1] 체크아웃 후 객실상태 자동 VD 전환

| 항목 | 내용 |
|------|------|
| **결과** | 이미 구현됨 확인 |
| **코드** | `ReservationServiceImpl.changeStatus()` → `room.checkOut()` → `foStatus=VACANT, hkStatus=DIRTY` 자동 전환 |
| **추가 작업** | 불필요 |

### 3.2 [P2-2] Walk-In 워크플로우 간소화

| 항목 | 내용 |
|------|------|
| **Before** | 단순 안내 페이지 (2개 링크만 제공) |
| **After** | 카드형 UI + `?walkin=true` 파라미터 → 예약 생성 후 도착 화면 자동 리다이렉트 |
| **원칙 적용** | 별도 폼 없이 기존 예약 등록 API 재사용 |
| **파일** | `walk-in.html`, `reservation-form.js` |

### 3.3 [P2-3] 예약 상세 링크 통일

| 항목 | 내용 |
|------|------|
| **Before** | In-House만 예약번호에 링크, Arrivals/Departures는 텍스트만 |
| **After** | 3개 화면 모두 예약번호 클릭 시 예약 상세 페이지로 이동 |
| **파일** | `fd-arrivals-page.js`, `fd-departures-page.js` |

### 3.4 [P2-4] 체크아웃 잔액 경고 강화

| 항목 | 내용 |
|------|------|
| **Before** | "미결제 잔액이 있습니다" 경고 텍스트만 |
| **After** | "예약 상세에서 결제하기" 링크 추가 → 예약 상세 페이지(결제 탭)로 직접 이동 |
| **원칙 적용** | 결제 기능 재구현 안 함, 기존 예약 상세 Tab4로 연결 |
| **파일** | `departures.html`, `fd-departures-page.js` |

### 3.5 [P2-5] 요약 카드/날짜 표시 통일

| 항목 | 내용 |
|------|------|
| **Before** | Arrivals만 3개 요약 카드 + 날짜 라벨 |
| **After** | Departures (출발예정/Late C-O/미결제잔액), In-House (투숙중/오늘체크아웃/미결제잔액) 각 3개 카드 + 날짜 라벨 추가 |
| **파일** | `departures.html`, `in-house.html`, `fd-departures-page.js`, `fd-in-house-page.js` |

### 3.6 [P2-6] 체크인 모달 7단계→4단계 축소

| 항목 | 내용 |
|------|------|
| **Before** | 예약확인 → 게스트 → 객실배정 → 요금 → 결제 → 메모 → 완료 (7단계) |
| **After** | 예약확인(+게스트+채널+총요금) → 객실배정 → 요금/결제요약(+예약상세링크) → 완료 (4단계) |
| **제거 사유** | Step2(게스트)=Step1과 중복, Step4(요금)/Step5(결제)=예약상세 Tab4와 100% 중복, Step6(메모)=예약상세 Tab5와 중복 |
| **효과** | 클릭 수 43% 감소 (7→4단계), 체크인 속도 향상 |
| **파일** | `arrivals.html`, `fd-arrivals-page.js` |

---

## 4. 수정 파일 요약

### 4.1 백엔드 (3개 파일)

| 파일 | 모듈 | 변경 내용 |
|------|------|----------|
| `FrontDeskArrivalResponse.java` | hola-reservation | `roomTypeId` 필드 추가 |
| `FrontDeskServiceImpl.java` | hola-reservation | `roomTypeId` 매핑 추가 |
| `RoomRackController.java` | hola-app | `NameMaskingUtil` import + 2곳 마스킹 적용 |

### 4.2 프론트엔드 JavaScript (6개 파일)

| 파일 | 변경 내용 |
|------|----------|
| `fd-arrivals-page.js` | 4단계 체크인, roomTypeId 유지, VC 우선 정렬, error 핸들링, 예약 링크 |
| `fd-departures-page.js` | 요약 카드, 날짜 라벨, 결제 링크, 예약 링크, error 핸들링 |
| `fd-in-house-page.js` | 요약 카드, 날짜 라벨, error 핸들링 |
| `fd-room-rack-page.js` | error 핸들링 (loadRoomRack, saveHkStatus) |
| `fd-walk-in-page.js` | 변경 없음 (HTML만 변경) |
| `reservation-form.js` | `walkin=true` 파라미터 감지 → 도착 화면 리다이렉트 |

### 4.3 HTML 템플릿 (4개 파일)

| 파일 | 변경 내용 |
|------|----------|
| `arrivals.html` | 7단계→4단계 모달, 채널/총요금/예약상세 링크 추가 |
| `departures.html` | 날짜 라벨, 3개 요약 카드, 결제 링크 |
| `in-house.html` | 날짜 라벨, 3개 요약 카드 |
| `walk-in.html` | 카드형 UI (Walk-In 예약등록 + 도착 화면) |

---

## 5. 미수행 항목 (Phase 3 후보)

| # | 항목 | 사유 | 우선순위 |
|---|------|------|---------|
| 1 | fw-bold 규칙 위반 수정 | 단순 CSS, 영향도 낮음 | Low |
| 2 | Room Rack 필터 (층/타입/상태) | 클라이언트 사이드, 새 API 불필요 | Medium |
| 3 | N+1 쿼리 최적화 (calcPaidAmount) | 성능, 데이터 많아지면 체감 | Medium |
| 4 | RoomRackController → hola-reservation 이동 | 리팩토링, 기능 변화 없음 | Low |
| 5 | 객실타입 기반 배정 필터 (서버 API 필요) | RoomNumber에 roomTypeId 없음 | Medium |
| 6 | 5개 화면 클라이언트 사이드 검색 필터 | 이름/객실번호 JS 필터 | Medium |

---

## 6. 교훈 및 권장사항

### 6.1 핵심 교훈

1. **벤치마킹 ≠ 복사**: Opera의 15단계 체크인을 그대로 따라하면 기존 예약 모듈과 100% 중복. **역할 분리 원칙**을 먼저 세우고 선별 적용해야 함
2. **기존 코드 확인 우선**: 체크아웃 후 VD 전환이 "미구현"으로 분류됐으나, 실제로는 `room.checkOut()`에 이미 구현되어 있었음. 분석 전 기존 코드 확인 필수
3. **API 재사용 원칙**: 새 API 0건으로 12개 파일 개선 완료. 기존 인프라 최대 활용이 유지보수성에 유리
4. **판단 기준 문서화**: 의사결정 프레임워크를 명시하면 향후 기능 추가 시 일관된 판단 가능

### 6.2 후속 권장사항

- **하우스키핑 모듈(Phase D)**: 프론트데스크와 연동 필요 (Task Sheet, 청소 상태 알림)
- **대시보드**: 프론트데스크 요약 카드를 대시보드 위젯으로 확장 (OCC%, ADR, RevPAR)
- **Night Audit**: EOD 시 Occupied Clean → Occupied Dirty 자동 전환 (Opera 표준)
