# Front Desk Action Drawer

## Executive Summary

| 항목 | 내용 |
|------|------|
| Feature | 프론트데스크 Offcanvas Action Drawer |
| 작성일 | 2026-03-18 |
| 예상 범위 | Step 1~2 (Step 3 결제는 VAN 연동 시 추후) |

### Value Delivered

| 관점 | 내용 |
|------|------|
| **Problem** | 프론트데스크에서 객실 배정·결제 처리 시 상세 페이지로 이동해야 하는 UX 단절. 모달은 배경 컨텍스트를 가리고, 복잡한 액션을 담기에 공간이 부족함 |
| **Solution** | Bootstrap 5 Offcanvas를 활용한 우측 슬라이드 패널(Drawer)로 전환. 테이블 컨텍스트를 유지하면서 상태 전환·객실 배정을 인라인 처리 |
| **Function UX Effect** | 프론트데스크 직원의 체크인~체크아웃 워크플로우가 한 화면에서 완결. 페이지 이동 0회 (기존 평균 2~3회) |
| **Core Value** | 프론트데스크 업무 효율 향상. OPERA Cloud 등 주요 PMS의 Side Panel UX 패턴 적용 |

---

## 1. 배경 및 목적

### 1.1 현재 상태
- 프론트데스크 운영현황 페이지에서 예약 행 클릭 → **Bootstrap Modal**(modal-lg)로 예약 상세 표시
- 모달에서 가능한 액션: 상태 전환(체크인/투숙중/체크아웃) — Leg 단위 + 마스터 일괄
- 모달에서 **불가능한 액션**: 객실 배정, 결제 처리 → 상세 페이지(`/admin/reservations/{id}`)로 이동 필요
- 모달이 열리면 배경 테이블이 가려져 컨텍스트 상실

### 1.2 문제점
1. **페이지 이동 빈번**: 체크인 시 객실 미배정이면 → 상세 페이지 Tab2 → 배정 → 돌아와서 체크인 (3단계)
2. **컨텍스트 단절**: 모달이 배경을 가려 다른 예약 상태를 동시에 확인 불가
3. **이중 관리**: 모달 렌더링 코드(`renderDetailModal`)와 상세 페이지가 동일 데이터를 별도 렌더링

### 1.3 목표
- 프론트데스크 일상 업무(체크인/아웃, 객실 배정)를 **한 화면에서 완결**
- Bootstrap 5 Offcanvas로 테이블 컨텍스트를 유지하면서 Quick Action 제공
- 기존 백엔드 API 100% 재사용 (백엔드 변경 0)

---

## 2. 범위

### 2.1 Step 1 — Modal → Offcanvas 전환 (이번 구현)
- 기존 `#reservationDetailModal` (Bootstrap Modal) → `#reservationDrawer` (Bootstrap Offcanvas) 전환
- 기존 모달의 모든 기능 이전: 예약 기본 정보, Leg 목록, 상태 전환, 결제 요약, 고객 요청, 메모
- Offcanvas 위치: **우측 (offcanvas-end)**, 너비: **500px**
- 테이블 행 클릭 → Offcanvas 열기 (기존 모달 대신)
- Offcanvas 내 상태 변경 후 → 테이블 자동 갱신 (기존 패턴 유지)
- "예약정보 수정" 링크 유지 (새 창 열기)
- 잔액 > 0인 경우 **결제 딥링크 버튼** 추가 (상세페이지 `tab=payment`로 이동)

### 2.2 Step 2 — 객실 배정 인라인 (이번 구현)
- 미배정 Leg에 "객실 배정" 버튼 표시
- 클릭 시 Leg 카드 내부에서 **인라인 확장**하여 가용 객실 리스트 표시
- 가용 객실 API 재사용: `GET /api/v1/properties/{pid}/room-assign/availability`
- 층별 객실 리스트: `GET /api/v1/properties/{pid}/floors/{floorId}/room-numbers/availability`
- 객실 선택 후 배정 완료: `PUT /api/v1/properties/{pid}/reservations/{id}/legs/{legId}`
- 배정 완료 시 Drawer 내용 + 테이블 동시 갱신

### 2.3 Step 3 — 결제 인라인 (추후, VAN 연동 시)
- Drawer 내 결제 섹션에서 카드/현금 결제 직접 처리
- VAN API 연동 완료 후 구현
- **이번 범위에서 제외** — 잔액 딥링크 버튼으로 대체

---

## 3. 기술 설계

### 3.1 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `templates/front-desk/operations.html` | Modal HTML → Offcanvas HTML 교체 |
| `static/js/fd-operations-page.js` | `openReservationDetail()`, `renderDetailModal()` 수정 + 객실 배정 로직 추가 |
| `static/css/hola.css` | Offcanvas 커스텀 스타일 (너비, 스크롤) |

### 3.2 HTML 구조 변경

**Before (Modal)**:
```html
<div class="modal fade" id="reservationDetailModal">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">...</div>
  </div>
</div>
```

**After (Offcanvas)**:
```html
<div class="offcanvas offcanvas-end" id="reservationDrawer" tabindex="-1" style="width:500px;">
  <div class="offcanvas-header">
    <h6 class="offcanvas-title fw-bold">
      예약 상세 <span id="drawerConfirmNo" style="color:#0582CA;"></span>
    </h6>
    <button type="button" class="btn-close" data-bs-dismiss="offcanvas"></button>
  </div>
  <div class="offcanvas-body" id="drawerBody">
    <!-- 동적 렌더링 -->
  </div>
  <div class="offcanvas-footer border-top p-3" id="drawerFooter">
    <!-- 액션 버튼 -->
  </div>
</div>
```

### 3.3 JS 주요 변경

#### openReservationDetail() 수정
```javascript
// Before: HolaPms.modal.show('#reservationDetailModal');
// After:
var drawer = bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('reservationDrawer'));
drawer.show();
```

#### renderDetailDrawer() — 기존 renderDetailModal() 대체
- 렌더링 로직 동일, 타겟 DOM만 변경 (`#mdlBody` → `#drawerBody`)
- Leg 카드 구조로 변경 (테이블 → 카드, Drawer 너비에 적합)
- 미배정 Leg에 "객실 배정" 확장 영역 추가

#### 객실 배정 인라인 (Step 2 추가)
```javascript
// 미배정 Leg 클릭 시 확장
expandRoomAssign: function(legIndex, subReservation) {
    // 1. 가용 객실 API 호출
    // 2. 층별 그룹핑 → 라디오 선택 UI 렌더링
    // 3. 선택 후 PUT /legs/{legId} 호출
    // 4. Drawer + 테이블 갱신
}
```

### 3.4 사용 API (전부 기존 API, 신규 없음)

| 용도 | Method | Endpoint | 변경 |
|------|--------|----------|------|
| 예약 상세 조회 | GET | `/api/v1/properties/{pid}/reservations/{id}` | 없음 |
| 상태 변경 | PUT | `/api/v1/properties/{pid}/reservations/{id}/status` | 없음 |
| 가용 객실 조회 | GET | `/api/v1/properties/{pid}/room-assign/availability` | 없음 |
| 층별 객실 상세 | GET | `/api/v1/properties/{pid}/floors/{fid}/room-numbers/availability` | 없음 |
| Leg 수정 (배정) | PUT | `/api/v1/properties/{pid}/reservations/{id}/legs/{legId}` | 없음 |

---

## 4. UI/UX 설계

### 4.1 Drawer 레이아웃 (Step 1)

```
┌─────────────────────────────────────┐
│ 예약 상세  GMP260317-0002      [×]  │ ← offcanvas-header
├─────────────────────────────────────┤
│                                     │
│ 예약번호: GMP260317-0002   투숙중   │
│ 예약자: 박성준이                    │
│ 연락처: -          이메일: -        │
│                                     │
│ ─── 🏨 객실 목록 (2) ───           │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ Leg 1 · STD-S                   │ │
│ │ 1301  3/17~3/24 (7박)  1명     │ │
│ │ 투숙중     [🚪 체크아웃]       │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ Leg 2 · DLX-T                   │ │
│ │ 1501  3/17~3/18 (1박)  1명     │ │
│ │ 체크아웃   ✓ 완료               │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ─── 💰 결제 ───                    │
│ 총액: 1,740,000원                   │
│ 결제: 1,380,000원                   │
│ 잔액: 360,000원  [💳 결제하기 →]   │
│                                     │
│ ─── 고객 요청 ───                  │
│ (내용)                              │
│                                     │
├─────────────────────────────────────┤
│ [📝 예약정보 수정]          [닫기]  │ ← offcanvas-footer
└─────────────────────────────────────┘
```

### 4.2 객실 배정 인라인 확장 (Step 2)

미배정 Leg의 "객실 배정" 버튼 클릭 시:

```
│ ┌─────────────────────────────────┐ │
│ │ Leg 1 · STD-S                   │ │
│ │ 미배정  3/17~3/24 (7박)  1명   │ │
│ │ 예약                            │ │
│ │                                 │ │
│ │ ▼ 가용 객실 선택                │ │ ← 토글 확장
│ │ ┌─────────────────────────────┐ │ │
│ │ │ 13F                         │ │ │
│ │ │ ○ 1301 (Clean)             │ │ │
│ │ │ ○ 1302 (Clean)             │ │ │
│ │ │ ● 1303 — 사용불가          │ │ │
│ │ ├─────────────────────────────┤ │ │
│ │ │ 14F                         │ │ │
│ │ │ ○ 1401 (Clean)             │ │ │
│ │ │ ○ 1402 (Clean)             │ │ │
│ │ └─────────────────────────────┘ │ │
│ │          [배정 완료] [취소]     │ │
│ │                                 │ │
│ │ [✅ 체크인]                     │ │
│ └─────────────────────────────────┘ │
```

### 4.3 상호작용 흐름

```
[테이블 행 클릭]
    ↓
[Drawer 열기] → 예약 상세 API 호출
    ↓
[Drawer 렌더링] → Leg 카드 + 결제 요약 + 액션 버튼
    ↓
┌──────────────────┬──────────────────┬──────────────────┐
│ 상태 전환        │ 객실 배정        │ 결제             │
│ Leg 버튼 클릭    │ "배정" 버튼 클릭 │ "결제하기" 클릭  │
│ → confirm()      │ → 인라인 확장    │ → 상세 페이지    │
│ → PUT /status    │ → 가용 객실 로드 │   tab=payment    │
│ → Drawer 갱신    │ → 선택 + 배정    │   새 창 열기     │
│ → 테이블 갱신    │ → PUT /legs/{id} │                  │
│                  │ → Drawer 갱신    │                  │
│                  │ → 테이블 갱신    │                  │
└──────────────────┴──────────────────┴──────────────────┘
```

---

## 5. 구현 순서

### Step 1: Modal → Offcanvas 전환

| # | 작업 | 파일 |
|---|------|------|
| 1-1 | `operations.html`에서 Modal HTML → Offcanvas HTML 교체 | operations.html |
| 1-2 | `hola.css`에 Offcanvas 커스텀 스타일 추가 (너비 500px, body 스크롤) | hola.css |
| 1-3 | `fd-operations-page.js`에서 Modal 관련 코드 → Offcanvas로 전환 | fd-operations-page.js |
| 1-4 | `renderDetailModal()` → `renderDetailDrawer()` 리팩토링 (테이블→카드 레이아웃) | fd-operations-page.js |
| 1-5 | 잔액 > 0일 때 "결제하기" 딥링크 버튼 추가 | fd-operations-page.js |
| 1-6 | 상태 변경 후 Drawer 자동 갱신 로직 수정 | fd-operations-page.js |
| 1-7 | 컴파일 + 동작 검증 | - |

### Step 2: 객실 배정 인라인

| # | 작업 | 파일 |
|---|------|------|
| 2-1 | 미배정 Leg에 "객실 배정" 버튼 추가 (renderDetailDrawer 내) | fd-operations-page.js |
| 2-2 | `expandRoomAssign()` 구현 — 가용 객실 API 호출 + 층별 렌더링 | fd-operations-page.js |
| 2-3 | 객실 선택 + `confirmRoomAssign()` 구현 — PUT /legs/{legId} 호출 | fd-operations-page.js |
| 2-4 | 배정 완료 시 Drawer + 테이블 동시 갱신 | fd-operations-page.js |
| 2-5 | 가용 객실 없는 경우 안내 메시지 처리 | fd-operations-page.js |
| 2-6 | E2E 검증 (미배정 → 배정 → 체크인 → 투숙중 → 체크아웃 전체 흐름) | - |

---

## 6. 리스크 및 완화

| 리스크 | 영향 | 완화 |
|--------|------|------|
| Offcanvas 내부 스크롤 이슈 | 긴 예약(다수 Leg) 시 스크롤 필요 | `offcanvas-body` overflow-y: auto (기본 지원) |
| 객실 배정 인라인 확장 시 높이 증가 | Drawer가 길어짐 | 스크롤로 자연 처리, 확장 시 해당 영역으로 자동 스크롤 |
| 모바일/태블릿 화면 | 500px Drawer가 화면 대부분 차지 | 프론트데스크는 데스크톱 전용 환경, 모바일 대응 불필요 |
| 기존 Quick Action 이벤트 바인딩 | Drawer 내부 버튼에도 이벤트 위임 필요 | 현재 `$(document).on('click', '.btn-checkin', ...)` 패턴이라 자동 작동 |
| 폴링 중 Drawer 열려있는 경우 | 30초 갱신 시 Drawer 내용 깜빡임 | Drawer 열린 상태에서는 테이블만 갱신, Drawer는 수동 갱신 |

---

## 7. 제외 사항 (이번 범위 아님)

- **결제 인라인 처리** (Step 3): VAN API 연동 후 진행
- **예약 정보 수정**: 기존 상세 페이지에서만 처리 (Drawer에서 링크 제공)
- **예약자 정보 수정**: 상세 페이지 Tab1 전용
- **일별 요금 조정**: 상세 페이지 Tab4 전용
- **메모 작성/수정**: 상세 페이지 전용 (Drawer는 조회만)
