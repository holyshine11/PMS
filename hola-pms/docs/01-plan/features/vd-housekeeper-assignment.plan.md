# VD 객실 하우스키퍼 담당자 지정 기능

## Executive Summary

| 항목 | 내용 |
|------|------|
| Feature | VD 객실 하우스키퍼 담당자 지정 |
| 작성일 | 2026-03-25 |
| 예상 수정 파일 | 6개 (Backend 4 + Frontend 2) |
| DB 변경 | 없음 |

### Value Delivered

| 관점 | 내용 |
|------|------|
| **Problem** | 객실현황에서 VD 상태 확인 후, HK 대시보드로 이동해 별도로 담당자를 배정해야 하는 2단계 워크플로우 |
| **Solution** | 객실 상태 변경 모달에서 VD일 때 하우스키퍼 담당자를 즉시 선택·배정하는 원스텝 통합 |
| **Function UX Effect** | 프론트데스크에서 상태 변경 + 담당자 배정을 한 화면에서 처리. HK 대시보드·모바일에 실시간 반영 |
| **Core Value** | 프론트데스크 운영 효율 향상, 청소 배정 리드타임 단축, VIP 객실 등 상황별 재량 배정 지원 |

---

## 1. 요구사항

### 1.1 핵심 기능
- 객실현황(Room Rack) 상태 변경 모달에서 **VD(Vacant Dirty) 상태일 때만** 하우스키퍼 담당자를 선택할 수 있는 드롭다운 표시
- 담당자 선택 시 HkTask 자동 생성 + 배정 (기존 활성 작업이 있으면 재배정만)
- 담당자 미선택 시 기존과 동일하게 상태만 변경

### 1.2 VD 조건 판정
- **Case A**: 현재 statusCode가 `VD`인 객실 → 모달 오픈 시 담당자 영역 표시
- **Case B**: 현재 VC/OOO/OOS인데 HK상태를 `DIRTY`로 변경 → hkStatusSelect change 이벤트로 담당자 영역 동적 표시
- **Case C**: 현재 VD인데 HK상태를 `CLEAN`으로 변경 → 담당자 영역 숨김 (청소 완료이므로)

### 1.3 담당자 배정 전략 (옵션 A: 수동 선택 + 추천)
- 해당 객실 층의 구역(Section) 담당자를 상단에 **추천** 표시
- 크레딧이 적은 순서로 정렬 (균등 배분 유도)
- 전체 가용 하우스키퍼도 하단에 표시
- 최종 선택은 프론트데스크 담당자의 재량

### 1.4 범위 외 (Out of Scope)
- 자동 배정 (autoAssign) 트리거 변경 — 기존 HK 대시보드에서 수행
- OD(Occupied Dirty) 담당자 지정 — 투숙중 객실은 현재 HK 대시보드 관할
- HkTask 상태 전이 변경 — 기존 PENDING→IN_PROGRESS→COMPLETED 흐름 유지

---

## 2. 기존 시스템 분석

### 2.1 현재 흐름 (AS-IS)
```
프론트데스크: 모달 → HK상태 변경 (CLEAN/DIRTY/OOO/OOS) + 메모
                    ↓
              RoomStatusService.updateRoomStatus()
                    ↓
              RoomNumber.updateHkStatus() — 끝 (담당자 배정 없음)

HK 대시보드: 별도 페이지에서 일일작업생성 → 자동배정 or 수동배정
```

### 2.2 변경 후 흐름 (TO-BE)
```
프론트데스크: 모달 → HK상태 변경 + (VD일 때) 담당자 선택
                    ↓
              RoomStatusApiController.updateStatus()
                    ├── RoomNumber.updateHkStatus()  ← 기존
                    └── (assigneeId 있으면)
                         ├── 활성 HkTask 존재? → 재배정 (assign)
                         └── 활성 HkTask 없음? → HkTask 생성 + 배정
                    ↓
              Room Rack 즉시 갱신 (loadRoomRack + loadSummary)
                    ↓
              HK 대시보드/모바일에 30초 이내 반영 (기존 폴링)
```

### 2.3 재사용 가능 자원

| 자원 | 용도 | 변경 필요 |
|------|------|----------|
| `GET /housekeeping/housekeepers` | 하우스키퍼 목록 조회 (이미 존재) | 없음 |
| `HkTask.assign(assignedTo, assignedBy)` | 작업 배정 엔티티 메서드 | 없음 |
| `existsActiveTaskByRoomNumberIdAndTaskDate()` | 중복 작업 체크 | 없음 |
| `HkConfig.defaultCheckoutCredit` | 작업 크레딧 기본값 | 없음 |
| Room Rack 카드의 `hkAssigneeName` 오버레이 | 배정 결과 즉시 표시 | 없음 |

---

## 3. 구현 계획

### Phase 1: Backend API 확장 (3개 파일)

#### 3.1 RoomStatusApiController — Request 확장
- `@RequestBody Map<String, String>` → DTO 변환 권장 (또는 Map에서 `assigneeId` 추가 추출)
- `assigneeId`가 String으로 오므로 Long 파싱

#### 3.2 RoomStatusService / RoomStatusServiceImpl — 오케스트레이션
- `updateRoomStatus()` 시그니처에 `Long assigneeId` 파라미터 추가
- 기존 로직 유지 + `assigneeId != null` 분기:
  1. `hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(roomNumberId, today)` 체크
  2. 활성 작업 있음 → 해당 작업에 `assign(assigneeId, currentUserId)` 재배정
  3. 활성 작업 없음 → HkTask 신규 생성 (CHECKOUT 타입, PENDING 상태) + `assign()`
- **RoomStatusServiceImpl에 HkTaskRepository, HkConfigRepository, AccessControlService 주입 추가**

#### 3.3 가용 하우스키퍼 API 추천 정보 보강 (선택)
- 기존 `GET /housekeeping/housekeepers`는 이름만 반환
- 층별 구역 매칭 + 현재 크레딧 정보를 포함하는 **경량 API 추가** 또는 기존 API 활용
- 초기 구현: 기존 API 그대로 사용 (이름만으로 충분), 추천 로직은 Phase 2에서

### Phase 2: Frontend 모달 확장 (2개 파일)

#### 3.4 room-rack.html — 모달 UI
- 기존 `<select id="hkStatusSelect">` 아래에 담당자 선택 영역 추가:
```html
<div id="hkAssigneeArea" class="mb-2 d-none">
    <label class="form-label text-muted">담당자 배정</label>
    <select class="form-select" id="hkAssigneeSelect">
        <option value="">미지정</option>
    </select>
</div>
```

#### 3.5 fd-room-rack-page.js — 동적 로직
- `openHkModal()`: 현재 statusCode 확인 → VD면 가용 하우스키퍼 조회 → 드롭다운 채움 + 영역 표시
- `hkStatusSelect` change 이벤트: DIRTY 선택 시 → foStatus가 VACANT이면 담당자 영역 표시, 아니면 숨김
- `saveHkStatus()`: `assigneeId` 포함하여 API 전송
- 하우스키퍼 목록 캐싱: `this.housekeepers` 배열에 저장, 모달 열 때마다 재조회하지 않고 `reload()` 시 갱신

---

## 4. 리스크 분석 및 대응

### 4.1 HkTask 중복 생성 (심각도: 높음)
- **시나리오**: 모달에서 HkTask 생성 후, HK 대시보드의 `generateDailyTasks()` 실행 시 동일 객실
- **대응**: `existsActiveTaskByRoomNumberIdAndTaskDate()` 이미 존재하며 양쪽 모두에서 체크 중. **이미 안전**
- **추가 검증**: 통합 테스트에서 모달 배정 → generateDailyTasks 순서로 호출하여 중복 방지 확인

### 4.2 동시 배정 충돌 (심각도: 중간)
- **시나리오**: 프론트데스크 A가 1201호 모달 열어놓은 상태에서 HK 대시보드의 autoAssign이 같은 객실 배정
- **대응**:
  - HkTask가 이미 있으면 reassign만 수행 (마지막 변경이 승리)
  - HkTask가 없어서 신규 생성하는 경우, `existsActiveTask` 체크 후 save → 극히 짧은 race window
  - **조치**: unique constraint 추가 불필요 (HkTask는 같은 객실에 여러 작업 허용 설계). 마지막 배정이 최종 결과로 충분

### 4.3 모듈 간 의존성 (심각도: 중간)
- **현재**: `RoomStatusServiceImpl`은 `RoomNumberRepository`만 의존
- **변경**: `HkTaskRepository`, `HkConfigRepository`, `AccessControlService` 추가 의존
- **판단**: 모두 **같은 hola-hotel 모듈** 내이므로 모듈 경계 위반 없음. 안전

### 4.4 SecurityConfig 권한 (심각도: 낮음)
- **현재**: `PUT /room-status/{roomNumberId}`는 JWT 기반 API 체인 (Order 2)
- **추가 호출**: `GET /housekeeping/housekeepers`도 동일 체인
- **판단**: 프론트데스크 담당자(`PROPERTY_ADMIN`)는 이미 `/api/v1/properties/*/housekeeping/**` 접근 가능. **변경 불필요**
- **검증**: SecurityConfig에서 HK API 경로의 permitAll/hasRole 확인

### 4.5 트랜잭션 원자성 (심각도: 중간)
- **시나리오**: `updateHkStatus()` 성공 후 `HkTask 생성`에서 예외 발생 → 상태만 변경되고 배정 실패
- **대응**: `RoomStatusServiceImpl.updateRoomStatus()`는 `@Transactional` — 전체 롤백 보장. **이미 안전**

### 4.6 빈 하우스키퍼 목록 (심각도: 낮음)
- **시나리오**: 프로퍼티에 하우스키퍼가 미등록 → 드롭다운 빈 상태
- **대응**: "미지정" 기본 옵션 항상 표시. 빈 목록이면 안내 메시지 표시 ("하우스키핑 > 인력관리에서 등록해주세요")

### 4.7 기존 HkTask와 assigneeId 배정 충돌 (심각도: 중간)
- **시나리오**: 이미 다른 하우스키퍼가 배정된 IN_PROGRESS 작업이 있는데, 모달에서 다른 사람 배정
- **대응**:
  - IN_PROGRESS/COMPLETED/INSPECTED 상태 작업은 재배정 차단
  - PENDING 상태만 재배정 허용
  - UI에서 "이미 청소 진행 중입니다 (담당: 김유리)" 안내 표시

---

## 5. 연동 페이지 영향 분석

| 페이지 | 영향 | 조치 |
|--------|------|------|
| **HK 대시보드** | 모달에서 생성한 HkTask 자동 표시 | 없음 (같은 테이블 조회) |
| **HK 작업 관리** | 작업 목록에 자동 표시 | 없음 |
| **HK 모바일** | 배정된 작업 표시 | 없음 |
| **일일작업 생성** | 중복 방지 로직 이미 존재 | 없음 (existsActiveTask 체크) |
| **자동 배정** | assignedTo != null 필터로 skip | 없음 |
| **Room Rack 카드** | hkAssigneeName 오버레이 | 없음 (갱신 시 자동 반영) |
| **예약 상세 팝업** | 무관 (투숙중 객실만) | 없음 |

---

## 6. 구현 순서 및 체크리스트

### Step 1: Backend — RoomStatusService 확장
- [ ] `updateRoomStatus()` 시그니처에 `assigneeId` 추가
- [ ] `assigneeId != null` 시 HkTask 조회/생성/배정 로직
- [ ] PENDING 상태만 재배정, IN_PROGRESS 이상은 차단

### Step 2: Backend — RoomStatusApiController 확장
- [ ] Request에서 `assigneeId` 추출 (Map → Long 파싱)
- [ ] Service 호출에 `assigneeId` 전달

### Step 3: Frontend — room-rack.html 모달 확장
- [ ] `hkAssigneeArea` div + select 추가 (d-none 기본)
- [ ] 진행중 작업 안내 영역 추가

### Step 4: Frontend — fd-room-rack-page.js 로직
- [ ] `openHkModal()`: VD 조건 시 하우스키퍼 조회 + 드롭다운
- [ ] `hkStatusSelect` change 이벤트: DIRTY/foStatus 조건부 표시
- [ ] `saveHkStatus()`: assigneeId 포함 전송
- [ ] 진행중 작업 감지 시 재배정 차단 + 안내

### Step 5: 검증
- [ ] VD 객실 → 모달 → 담당자 선택 → 저장 → Room Rack 카드에 배정자 표시 확인
- [ ] VC 객실 → DIRTY 변경 + 담당자 선택 → HkTask 생성 확인
- [ ] 이미 진행중 작업 있는 객실 → 재배정 차단 확인
- [ ] HK 대시보드에서 모달 배정 작업 표시 확인
- [ ] generateDailyTasks 후 중복 작업 미생성 확인
