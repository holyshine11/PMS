# Oracle Opera Cloud PMS - 대시보드 & 프론트데스크 상세 조사 보고서

> 조사일: 2026-02-28
> 출처: Oracle 공식 문서(docs.oracle.com Opera Cloud 26.1 User Help), Oracle 제품 페이지, 호텔 업계 자료 종합

---

## 1. Opera Cloud Home Dashboard (메인 대시보드)

### 1.1 대시보드 구조

Opera Cloud의 홈 대시보드는 **타일(Tile) 기반 인터페이스**로 구성된다.

- **Dashboard Page**: 복수의 대시보드 페이지를 생성하여 탭으로 전환 가능
- **Primary Page**: 사용자가 직접 생성/이름 변경/삭제 가능한 페이지
- **Extension Page**: 타일이 한 페이지를 초과할 때 시스템이 자동 생성하는 확장 페이지 (이름 변경/삭제 불가)
- Primary Page 삭제 시 연결된 모든 Extension Page도 함께 삭제됨

### 1.2 대시보드 페이지 구성 레벨

| 레벨 | 설명 |
|------|------|
| **Chain Level** | 체인 전체에 적용되는 표준 대시보드 |
| **Hub Level** | 허브(관리 거점) 단위 대시보드 |
| **Property Level** | 개별 프로퍼티(호텔) 단위 대시보드 |

### 1.3 역할별 페이지 구성 예시

대시보드 페이지를 역할별로 구분하여 구성할 수 있다:

- **Front Desk 페이지**: Arrivals, Departures, In-House 타일 배치
- **Housekeeping 페이지**: Queue Rooms, Task Sheet, Room Status, Room Maintenance 타일 배치
- **Sales 페이지**: Activities, Appointments, Block Overview, To Do List 타일 배치

### 1.4 전체 타일(위젯) 목록 - 32종

Oracle 공식 문서 기준 Opera Cloud에서 제공하는 대시보드 타일 전체 목록:

| # | 타일명 | 표시 데이터 | Dynamic Property 지원 |
|---|--------|------------|----------------------|
| 1 | **60 Minutes Activity** | 최근 60분간 체크인, 체크아웃, 객실 청소 완료 건수 | O |
| 2 | **(Sales) Activities** | 당일 영업 활동(Sales Activity) 건수. Activities OPERA Control 활성화 필요 | O |
| 3 | **Advance Check In** | 당일 사전 체크인 건수 (개별 예약 + 블록 예약). 현재/체크인완료/합계 3영역 표시 | O |
| 4 | **Appointments** | 선택한 영업 담당자 및 활동 유형별 당일 약속. S&E Management 에디션 필요 | O |
| 5 | **Arrivals** | 당일 도착 예정 건수, Queue 대기 예약 건수 표시 | O |
| 6 | **Block Overview** | 도착/재실 블록 개요. Opportunities 포함 옵션 | O |
| 7 | **Block Daily Revenue** | 활성 블록의 객실수, 케이터링 이벤트, 참석자수, 일자별 매출 | O |
| 8 | **Complimentary / House Use** | 당일 무료(Comp) 객실 및 하우스 유스(House Use) 객실 현황 | O |
| 9 | **Comp Routing** | Comp Routing 통계, 전기 상태, 요청 건수. 드릴다운 기능 | O |
| 10 | **Custom Content** | 사용자 정의 텍스트, 이미지, 웹 링크 표시 | X |
| 11 | **Daily Projections** | 일자별 예상 점유율 및 매출 수치 | O |
| 12 | **Departures** | 당일 예상/실제 출발 객실 수, 예정된 체크아웃 표시 | O |
| 13 | **Events** | 선택 일자의 케이터링 이벤트 요약. Opportunities 포함 옵션 | O |
| 14 | **External (System) Content** | 외부 시스템(Outbound External System)에서 수신한 데이터 표시 | X |
| 15 | **Image Gallery** | 평면도, 지도, 관광지 이미지 등 표시 | X |
| 16 | **In House (Occupied)** | 현재 재실(In-House) 객실(예약) 수 | O |
| 17 | **Max Available Rooms** | 당일 최대 판매 가능 객실 수 (비공제 객실 제외) | O |
| 18 | **Membership Reservation Activity** | 멤버십 타입별 도착/재실/출발/예상 매출 개요. 복수 프로퍼티 시 중앙 통화, 단일 프로퍼티 시 현지 통화 표시 | - |
| 19 | **OXI Interface Status** | OXI 다운로드/업로드 메시지 상태 | X |
| 20 | **Queue Reservations** | 대기 중인 도착 예약, 객실 타입별 가용 객실 수 및 상태 표시. Queue Room OPERA Control 필요 | O |
| 21 | **Reservation Activity** | 당일 도착/재실/출발 예약 세부 현황. 평균 체크인 시간 계산 포함 | O |
| 22 | **Reservation Revenue Summary** | 일자별 판매 객실 수 및 매출. 그래프 또는 테이블 형식 선택 가능 | X |
| 23 | **Reservation Statistics** | 당일 예약 통계 | O |
| 24 | **Reservations and Cancellations Today** | 당일 영업일 기준 예약 및 취소 건수. 사용자/프로퍼티 필터 옵션 | X |
| 25 | **Room Maintenance** | 해결/미해결 객실 유지보수 요청 현황 | O |
| 26 | **Room Status** | 객실 상태(Room Status)별, 프론트 오피스 상태(FO Status)별 객실 수. 객실/인원 불일치(Discrepancy) 및 Queue 객실 수 표시 | O |
| 27 | **Room Summary** | 판매 가능 객실, 전체(물리적) 객실, OOO 객실, OOS 객실 수 | O |
| 28 | **Rooms Availability Summary** | 일자별/객실 타입별 판매 객실 및 가용 객실 현황 | O |
| 29 | **Rooms Sold Summary** | 객실 타입별 판매 및 가용 현황 요약 | X |
| 30 | **Room Moves** | 보류 중(Pending) 및 완료된(Completed) 객실 이동 현황 | O |
| 31 | **Task Sheets** | 하우스키핑 태스크시트 합계: 객실 상태, 청소 완료율(%), 당일 메모, 상세 및 완료 상태 | O |
| 32 | **To Do List** | 당일 할 일 목록. S&E Management 에디션 필요 | O |
| 33 | **VIP Guests** | VIP 도착/출발 예약 스냅샷. VIP 코드별 필터링 가능. VIP OPERA Control 필요 | O |

### 1.5 타일 관리 기능

- **추가**: Dashboard 페이지 선택 → "Add New Tiles" → 타일 선택 후 수량 입력 → "Add to Dashboard"
- **설정**: 타일 위 기어(⚙️) 아이콘 클릭 → 타일별 설정 옵션 구성 → Save
- **삭제**: 타일 위 X 아이콘 클릭
- **재배치**: 타일을 클릭 홀드 → 드래그 앤 드롭 → 다른 타일 자동 재배치
- **복수 인스턴스**: Room Class OPERA Control 활성화 시 동일 타일을 다른 Room Class로 복수 추가 가능
- **복수 프로퍼티 선택**: Appointments, Activities, To Do List 타일은 복수 프로퍼티 선택 가능

### 1.6 Chain Level vs Property Level 타일 차이

Page Composer를 체인 레벨에서 프로퍼티 위치로 활성화할 때:
- **Current Property**: 현재 프로퍼티 위치에서만 타일 표시
- **Dynamic Property**: 체인 내 모든 프로퍼티 위치에서 타일 표시 (사용자 위치에 따라 프로퍼티 코드 자동 변경)
- Hub 위치에서 커스터마이징 시에는 이 옵션이 표시되지 않음

### 1.7 Advance Check In 타일 상세

3개 영역으로 구분되며 각 영역 클릭 시 사전 체크인 검색 화면으로 이동:

| 구분 | 내용 |
|------|------|
| **Current** | 현재 Advance Checked In 상태인 도착(Due In) 개별/블록 예약 수 |
| **Checked In** | 오늘 Advance Checked In으로 표시된 후 실제 체크인 완료된 개별/블록 예약 수 |
| **Total for Today** | 위 두 항목의 합계. Day Use 예약(당일 체크인/체크아웃) 포함 |

---

## 2. Opera Cloud Front Desk 화면

### 2.1 메뉴 구조 (Chapter 4: Front Desk)

Opera Cloud의 Front Desk 모듈은 다음과 같은 하위 섹션으로 구성된다:

```
Front Desk
├── Arrivals and Check In (도착 & 체크인)
│   ├── Pre Register Arrival Reservations (사전 등록)
│   ├── Creating a Walk In Reservation (워크인 예약)
│   ├── Managing Rooms on Hold (객실 보류 관리)
│   ├── Swapping or Shifting Assigned Rooms (객실 교환/이동)
│   ├── Managing Reservation Queue Status (대기 상태 관리)
│   ├── Managing eSignature Registration Cards (전자서명 등록카드)
│   ├── External Guest Notifications (외부 게스트 알림)
│   ├── Checking in Reservations (체크인 처리)
│   ├── Managing Welcome Offers (환영 오퍼 관리)
│   ├── About Advanced Folio Payment (사전결제)
│   └── Managing Vouchers (바우처 관리)
├── In House (재실)
│   ├── Reversing (Cancelling) Check-In (체크인 취소)
│   └── Moving an In House Reservation (재실 예약 객실 이동)
├── Departures and Checkout (출발 & 체크아웃)
│   ├── Using Quick Check-Out (빠른 체크아웃)
│   ├── Checking Out Reservations (체크아웃 처리)
│   ├── Scheduling a Checkout (체크아웃 예약)
│   ├── Checking Out Reservations Early (조기 체크아웃)
│   └── Reinstating Checked Out Reservations (체크아웃 복원)
├── Front Desk Workspace (프론트데스크 워크스페이스)
│   ├── Managing Guest Messages (게스트 메시지)
│   ├── Generating Batch Registration Cards (일괄 등록카드 생성)
│   ├── Managing Wake Up Calls (모닝콜 관리)
│   ├── Using Batch Room Assignment (일괄 객실 배정)
│   ├── Available Room Search (가용 객실 검색)
│   └── Key Packet Labels (키 패킷 라벨)
└── Reservation Upgrade - Powered by Nor1 PRiME (객실 업그레이드)
```

### 2.2 Arrivals 화면 (체크인 대기 리스트)

**접근 경로**: Front Desk → Arrivals (또는 대시보드 Arrivals 타일 클릭, Quick Launch)

#### 검색 기준
- 게스트명, 확인번호, 예약번호
- 객실 번호, 객실 타입
- 도착일
- 예약 상태 (Due In, Queue, Pre-Registered 등)
- VIP 코드
- 블록 코드

#### 표시 컬럼 (일반적 구성)
- 게스트명 (성/이름)
- 확인번호 (Confirmation No.)
- 객실 번호 (Room No.) - 배정된 경우
- 객실 타입 (Room Type)
- 도착일 / 출발일
- 숙박일수 (Nights)
- 레이트 코드 / 요금
- 예약 상태 (Reservation Status)
- VIP 코드
- 결제 방법 (Payment Method)
- 특별 요청 (Special Requests)

#### 필터 옵션
- "Skip Rooms Only": Discrepant Room OPERA Control 활성화 시, 불일치 예약 제외 가능
- 예약 유형별 필터 (개별/블록/그룹)
- 태블릿에서는 기본 카드 뷰(Card View)로 표시

#### 주요 액션 버튼
- **Check In**: 선택한 예약 체크인 시작
- **I Want To...**: 도착 예약 관련 다양한 액션 목록 표시
- **Search**: 검색 조건으로 예약 조회

### 2.3 Room Rack / Room Grid

Opera Cloud의 Room Rack은 **Inventory > Room Management** 섹션 내에 위치한다.

#### 구조
- **행(Row)**: 층(Floor)별 또는 객실 타입(Room Type)별 그룹핑
- **열(Column)**: 개별 객실 번호
- 각 셀은 해당 객실의 현재 상태를 시각적으로 표시

#### Room Status 색상 코드 체계

| 상태 | 약어 | 색상 (일반적) | 설명 |
|------|------|-------------|------|
| **Vacant Clean** | VC | 초록색 (Green) | 공실, 청소 완료 - 즉시 판매 가능 |
| **Vacant Clean Inspected** | VCI | 진한 초록색 (Dark Green) | 공실, 청소 완료 후 검사 완료 |
| **Vacant Dirty** | VD | 빨간색 (Red) | 공실, 청소 필요 |
| **Vacant Pickup** | VP | 노란색/주황색 (Yellow/Orange) | 공실, 픽업(정리) 필요 |
| **Occupied Clean** | OC | 파란색 (Blue) | 재실, 청소 완료 |
| **Occupied Clean Inspected** | OCI | 진한 파란색 (Dark Blue) | 재실, 청소 완료 후 검사 완료 |
| **Occupied Dirty** | OD | 자주색/보라색 (Purple/Magenta) | 재실, 청소 필요 |
| **Occupied Pickup** | OP | 연주황색 (Light Orange) | 재실, 픽업 필요 |
| **Out of Order** | OOO | 회색+빗금 (Gray/Striped) | 사용 불가 (수리/공사 등) - 재고에서 제외 |
| **Out of Service** | OOS | 연회색 (Light Gray) | 일시 서비스 중단 - 재고에는 포함되나 판매 제한 |

#### 프론트 오피스 상태 (FO Status)
- **Vacant**: 공실
- **Occupied**: 재실 (게스트 투숙 중)
- **Due Out**: 당일 체크아웃 예정
- **Due In**: 당일 도착 예정
- **Departed**: 체크아웃 완료

#### 클릭 시 액션
- 객실 클릭 → 객실 상세 정보 표시 (게스트 정보, 예약 정보, 하우스키핑 상태)
- 객실 상태 변경 가능 (Clean/Dirty/Inspected/Pickup 전환)
- 유지보수 요청 생성
- OOO/OOS 설정

### 2.4 Departures 화면 (체크아웃 리스트)

**접근 경로**: Front Desk → Departures (또는 대시보드 Departures 타일)

#### 표시 컬럼
- 게스트명
- 객실 번호
- 객실 타입
- 체크아웃 예정 시간
- 잔액 (Balance)
- 결제 방법
- 체크아웃 상태 (Due Out / Late Check-out / Scheduled)

#### 필터 옵션
- 당일 출발 예정 (Due Out Today)
- 지연 체크아웃 (Late Checkout)
- 예약된 체크아웃 (Scheduled Checkout)
- 불일치 객실 (Discrepant Rooms) 제외 옵션: "Skip Rooms Only"

#### 주요 액션
- **Check Out**: 선택 예약 체크아웃 시작
- **I Want To... → Check Out**: 체크아웃 실행
- **Scheduled Checkout**: 체크아웃 시간 예약
- **Quick Check-Out**: 빠른 체크아웃 처리

### 2.5 In-House 화면 (재실 게스트)

**접근 경로**: Front Desk → In House

#### 검색/필터
- 게스트명, 객실 번호, 객실 타입
- VIP 코드
- 그룹/블록 코드
- 멤버십 타입

#### 표시 컬럼
- 게스트명
- 객실 번호 / 객실 타입
- 도착일 / 출발 예정일
- 레이트 코드 / 요금
- 잔액
- VIP 레벨
- 특별 요청

#### 가능 액션
- **Room Move**: 다른 객실로 이동
- **Reverse Check-In**: 체크인 취소 (실수 체크인 시)
- **Early Check-Out**: 조기 체크아웃
- **Billing**: 빌링 화면 접근
- **I Want To...**: 추가 액션 메뉴

### 2.6 Queue Reservation (대기 예약)

Queue Rooms OPERA Control 활성화 시 사용 가능.

#### 동작 방식
- 도착 게스트의 배정 객실이 아직 준비되지 않은 경우 (Dirty/Pickup 상태), 예약을 Queue에 배치
- 하우스키핑에 해당 객실의 우선 청소 알림 발송
- 객실 상태가 Clean/Inspected로 변경되면 프론트에 알림
- 대시보드 Queue Reservations 타일에서 실시간 모니터링

#### Queue Reservations 타일 표시 항목
- 대기 중인 도착 예약 목록
- 객실 타입별 가용 객실 수
- 각 객실의 현재 하우스키핑 상태

---

## 3. 체크인/체크아웃 워크플로우

### 3.1 표준 체크인 프로세스 (15단계)

Oracle 공식 문서 기준의 상세 체크인 단계:

#### Step 1. 도착 예약 접근
- Front Desk → Arrivals 메뉴 접근
- 대시보드 Arrivals 타일 또는 Quick Launch에서도 접근 가능
- 태블릿: 카드 뷰에서 예약 카드 탭하여 시작

#### Step 2. 예약 검색 및 선택
- 검색 조건 입력 → Search 클릭
- 결과에서 예약 선택 → **Check In** 클릭

#### Step 3. 게스트 비즈니스 카드 확인
- 화면 상단에 게스트 비즈니스 카드 표시
- 게스트명 링크 클릭 시 프로필 편집 가능
- Room Upgrade 알림 표시 (해당 시)

#### Step 4. 객실 선택 및 배정
- **자동 배정**: Auto Assign Room at Check-In OPERA Control 활성화 시, 체크인 화면 접근 시 가장 추천되는 객실 자동 배정
  - 제외: 쉐어 예약, 백투백 예약, 컴포넌트 객실
- **수동 배정**:
  - Room Selection 패널에서 추천 객실 확인 (최대 10개, 청소 상태 높은 순)
  - 층, 특성(Feature), 흡연 선호도로 필터링
  - AI Room Search: Room Feature Suggestions 체크박스 (기본 선택)
  - 컴포넌트 스위트: 스위트 포함 여부 아이콘 표시
- **추가 액션**:
  - Accept Room and Mark as Do Not Move: 객실 고정 (잠금 표시)
  - Place Reservation on Queue: 객실 미준비 시 대기열 배치
  - Swap/Shift Rooms: 동일 도착일/객실타입 예약 간 교환

#### Step 5. 예약 개요 검토
- 객실 타입 링크 → 객실 타입 상세 및 이미지
- 레이트 코드 링크 → 레이트 코드 정보
- 요금 링크 → 요금 상세
- 잔액 링크 → 빌링 화면 (Pre-Stay Charges 확인)

#### Step 6. 패키지 & 아이템 확인
- 기존 패키지 및 아이템 검토
- 패키지 아이템/인벤토리 아이템 추가 가능

#### Step 7. 개인정보 보호 확인
- 게스트 프라이버시 옵션 검토
- Edit → 프라이버시 옵션 업데이트 → Save

#### Step 8. 신분증 확인
- 복수 신분증 정보 추가 가능
- 게스트 프로필에 저장
- Opera Cloud ID Document Scanning Cloud Service 연동 지원

#### Step 9. 멤버십 확인
- 적용 가능한 멤버십 확인 또는 선택
- Available Profile Memberships → Reservation Memberships로 이동
- 새 멤버십 추가 가능

#### Step 10. 결제 확인 및 수금
- 결제 방법 확인 또는 업데이트
- **최대 8개 빌링 윈도우** 지원
- **체크인 시 카드 승인**: 선택된 신용카드 유형에 대해 자동 승인 요청
- **백투백 예약 시**: 연결된 모든 예약에 대해 승인 필요
- **Confidential Billing Window**: 폴리오 윈도우 2~8에 기밀 체크박스 (인쇄 제한)

#### Step 10-1. 사전결제 규칙 (Advanced Folio Payment)
- 숙박비, 패키지, 고정요금, 사전 차지 전액 수금 요청
- Nights to Charge 조정 가능 (최소값 이상)
- **Advanced Folio Payment**: 실제 차지 미전기, 결제만 수금
- **Advanced Folio Posting**: 실제 차지를 Advance Bill로 전기
- 최소/전액 결제 완료 시에만 체크인 진행
- 결제된 숙박일수만큼 룸키 발급

#### Step 11. 동반 게스트 검증
- 동반 게스트 연령 기준(Threshold) 미달 시 체크인 차단
- 생년월일 및 관계(Relationship) 정보 필수 입력

#### Step 12. 등록카드 생성
- **Registration Card** 버튼 → 등록카드 인쇄
- **eSign Registration Card**: 태블릿에서 전자서명 등록카드 생성
- **일괄 등록카드**: Generate Registration Cards at Check-In = "Prompt" 시 자동 요청
- **백투백 예약**: 연결 예약 전체에 대한 등록카드 생성 안내

#### Step 13. 바우처 생성
- Vouchers OPERA Control 활성화 시
- 체크인 시작 시 인쇄 가능한 바우처 목록 표시

#### Step 14. 체크인 완료
- **Complete Check In** 클릭
- 시스템 최종 검증 수행
- **알림 처리**: 체크인 영역 알림 표시 (Alerts OPERA Control)
- **쉐어 예약 처리**:
  - 수동 모드: 쉐어 예약을 하나씩 순차 체크인
  - 자동 모드: 도착 쉐어 예약 목록 표시 → 선택 후 일괄 체크인 (실패 건은 수동 전환)
- **Queue 상태 확인**: 객실 미준비 시 Queue 배치 제안

#### Step 15. 선불카드 발급 (선택)
- Stored Value System(SVS) 인터페이스 설정 시
- Issue Prepaid Cards 클릭하여 선불카드 발급

### 3.2 Walk-In 체크인 프로세스

Walk-In은 사전 예약 없이 현장에서 바로 투숙하는 경우:

1. Front Desk → Arrivals → **Walk In** 버튼 클릭 (또는 I Want To... → Create Walk In)
2. 게스트 프로필 검색 또는 신규 생성
3. 객실 타입, 레이트 코드 선택
4. 숙박일수 및 출발일 설정
5. 객실 배정 (가용 객실에서 즉시 배정)
6. 결제 방법 입력
7. 바로 체크인 완료 (예약 생성 + 체크인이 한 프로세스로 처리)

### 3.3 체크아웃 프로세스

#### 접근
- Front Desk → Departures → 예약 검색 → 선택 → **Check Out** 클릭
- 대시보드 Departures 타일에서 직접 접근 가능

#### Cashier Check-out Workflow Preference에 따른 3가지 옵션

**Option A: Billing Workflow**
1. 추가 차지/리베이트 Post Charge로 검토/전기
2. Check Out 클릭하여 윈도우 잔액 정산

**Option B: Folio Settlement Workflow**
1. 각 빌링 윈도우에 결제 상세 입력 (라우팅 지시서에서 기본값)
2. **Settle and Send Folio** 클릭 → 결제 전기 + 폴리오 생성
3. 다른 빌링 윈도우에 대해 반복
4. 모든 윈도우 잔액 = 0 시:
   - **Checkout Now**: 즉시 체크아웃
   - **Schedule Checkout**: 지연 체크아웃 예약

**Option C: Post-Stay Charging**
1. I Want To → Post Charges → 추가 차지 전기
2. I Want To → Go to Billing → 빌링 화면 이동
3. 각 윈도우 결제 상세 입력
4. **Settle and Send Folio** (폴리오 포함) 또는 **Settle Folio** (결제만)
5. Checkout Now 클릭

#### 특수 시나리오
- **Day-Use 예약**: 빌링 화면 첫 접근 시 숙박비+세금 자동 전기
- **백투백 연결 예약**: 체크아웃 시 도착 예약 체크인 안내 (Scheduled/Mass/Auto 체크아웃에서는 미표시)

### 3.4 Quick Check-Out (빠른 체크아웃)

- 잔액이 0이거나 신용카드로 자동 정산 가능한 예약에 적용
- 빌링 검토 없이 바로 체크아웃 처리
- 폴리오는 이메일로 자동 발송 (설정 시)

### 3.5 Express Checkout

- 게스트가 직접 체크아웃할 수 있는 셀프 서비스 방식
- 객실 내 TV/키오스크/모바일 앱을 통해 폴리오 확인 후 승인
- 신용카드 자동 정산 → 키 비활성화

### 3.6 Group Check-In (그룹 체크인)

- 블록/그룹 예약에 대한 **Mass Check-In** 기능
- 여러 예약을 동시에 선택하여 일괄 체크인 처리
- 블록 코드로 검색 → 전체/부분 선택 → 일괄 체크인
- 결제 방법 누락/카드 거부 건은 수동 처리로 전환

### 3.7 Advance Check-In (사전 체크인)

당일 도착 예약에 대해 사전 체크인 플래그를 설정:

#### 적용 조건
- 당일 영업일(Business Date)의 도착(Due In) 예약
- 유효한 결제 방법 보유 필수
- 객실 배정 유무와 관계없이 가능

#### 적용 가능 예약 유형
- Walk-In 예약
- Pre-Registered 예약
- Queue 상태 예약

#### 자동 체크인 연동
- 객실 배정 + 객실 상태 매칭 시 자동 체크인 가능
- Mass Advance Check-In: 복수 예약 일괄 사전 체크인

---

## 4. Housekeeping Board (하우스키핑 보드)

### 4.1 접근 경로

Inventory → Room Management → Housekeeping Board

### 4.2 객실 상태 종류 (Room Status)

#### 하우스키핑 상태 (Housekeeping Status) - 6종

| 상태 | 코드 | 설명 |
|------|------|------|
| **Clean** | CL | 청소 완료 |
| **Dirty** | DI | 청소 필요 (체크아웃 후 또는 야간 감사 시 자동 전환) |
| **Inspected** | IN | 청소 완료 후 관리자 검사 완료 (가장 높은 등급) |
| **Pickup** | PU | 간단한 정리 필요 (터치업) |
| **Out of Order (OOO)** | OO | 사용 불가 - 유지보수/수리/리노베이션 필요. **재고에서 완전 제외** (판매 불가, 점유율 계산에서 제외) |
| **Out of Service (OOS)** | OS | 일시 서비스 중단 - 경미한 문제. **재고에는 포함**되나 판매 제한 (필요 시 오버라이드 가능) |

#### 프론트 오피스 상태 (Front Office Status / Occupancy Status) - 2종

| 상태 | 설명 |
|------|------|
| **Vacant** | 공실 (게스트 없음) |
| **Occupied** | 재실 (게스트 투숙 중) |

#### 결합 상태 (Combined Status) - 일반적으로 사용되는 조합

| 조합 상태 | 의미 | 판매 가능 |
|----------|------|----------|
| **Vacant Clean (VC)** | 공실 + 청소 완료 | O (즉시 판매) |
| **Vacant Clean Inspected (VCI)** | 공실 + 검사 완료 | O (최우선 판매) |
| **Vacant Dirty (VD)** | 공실 + 청소 필요 | X (청소 후 판매) |
| **Vacant Pickup (VP)** | 공실 + 정리 필요 | 조건부 |
| **Occupied Clean (OC)** | 재실 + 청소 완료 | - |
| **Occupied Dirty (OD)** | 재실 + 청소 필요 | - |
| **Occupied Clean Inspected (OCI)** | 재실 + 검사 완료 | - |
| **Occupied Pickup (OP)** | 재실 + 정리 필요 | - |

### 4.3 객실 상태 전이 플로우

```
[체크아웃] → Occupied Dirty → Vacant Dirty
                                    ↓
                              [하우스키핑 청소]
                                    ↓
                              Vacant Pickup (선택적)
                                    ↓
                              Vacant Clean
                                    ↓
                              [관리자 검사] (선택적)
                                    ↓
                              Vacant Clean Inspected
                                    ↓
                              [체크인] → Occupied Clean
                                    ↓
                              [야간 감사/EOD] → Occupied Dirty
                                    ↓
                              [하우스키핑 청소]
                                    ↓
                              Occupied Clean (또는 Inspected)
```

**야간 감사(Night Audit / End of Day) 시 자동 전환**:
- 모든 Occupied Clean → Occupied Dirty (Daily 서비스 기준)
- 설정에 따라 Vacant Clean → Vacant Dirty 전환 가능

### 4.4 Housekeeping Board 레이아웃

#### 필터링 옵션
- 층(Floor)별 필터
- 객실 타입별 필터
- 하우스키핑 상태별 필터 (Clean/Dirty/Inspected/Pickup)
- 프론트 오피스 상태별 필터 (Vacant/Occupied)
- OOO/OOS 필터
- 담당자(Attendant) 필터

#### 표시 정보
- 객실 번호
- 객실 타입
- 현재 HK 상태 (색상 코드)
- 현재 FO 상태
- 배정된 담당자(Attendant)
- 우선순위 (Queue 객실 등)
- 게스트 정보 (재실 시)
- 특별 지시사항

#### 가능 액션
- 개별/일괄 상태 변경 (Clean ↔ Dirty ↔ Inspected ↔ Pickup)
- OOO/OOS 설정/해제
- 담당자 배정/변경
- 유지보수 요청 생성
- Room Condition 설정

### 4.5 프론트데스크 ↔ 하우스키핑 연동

| 이벤트 | 프론트데스크 측 | 하우스키핑 측 |
|--------|---------------|-------------|
| 체크아웃 | 예약 상태 → Checked Out | 객실 상태 → Vacant Dirty |
| 객실 청소 완료 | Room Status 타일에 반영 | Clean 상태로 업데이트 |
| 관리자 검사 완료 | VCI로 표시 → 즉시 판매 가능 | Inspected 상태로 업데이트 |
| Queue 배치 | 게스트에게 대기 안내 | 해당 객실 우선 청소 알림 |
| OOO 설정 | 객실 판매 불가 처리 | 해당 객실 제외 |
| Room Discrepancy | 불일치 알림 (Person/Room) | 실제 객실 상황 보고 |

#### Room Discrepancy (객실 불일치)
- **Person Discrepancy**: 프론트 기록 인원과 하우스키핑 보고 인원 불일치
- **Room Discrepancy**: 프론트 기록 상태(Vacant/Occupied)와 하우스키핑 보고 상태 불일치
- Room Status 대시보드 타일에 불일치 건수 표시
- Room Management → Room Discrepancies에서 해결

### 4.6 Task Sheets (태스크 시트)

#### 구성
- **생성(Generation)**: 객실 목록, 배정 담당자, 청소 유형(일반/출발/VIP 등) 자동 생성
- **관리(Management)**: 진행 상황 추적, 상태 업데이트
- **리포트(Report)**: 완료 현황 보고서 출력
- **Task Sheet Companion**: 모바일 디바이스용 태스크시트 동반 앱

#### 대시보드 Task Sheets 타일 표시 항목
- 객실 상태별 합계
- 청소 완료율 (%)
- 당일 메모(Notes)
- 상세 정보 및 완료 상태

### 4.7 Attendant Console (담당자 콘솔)

- 담당자별 배정 객실 목록 관리
- 실시간 상태 업데이트
- 모바일 디바이스에서 접근 가능
- 객실별 청소 시작/완료 기록

### 4.8 Room Conditions (객실 조건)

OOO/OOS 외에 추가적인 객실 조건을 설정할 수 있는 시스템:
- 사용자 정의 조건 코드 생성
- 기간 설정 가능
- 객실 판매에 영향을 줄 수 있음

### 4.9 Floor Plans (평면도)

- 프로퍼티의 층별 평면도를 시각적으로 표시
- 각 객실의 현재 상태를 색상으로 표시
- 인터랙티브: 객실 클릭 → 상세 정보/상태 변경
- Property Site Plan Images도 별도 지원

---

## 5. Alert / Task / Traces 시스템

### 5.1 Reservation Alerts (예약 알림)

#### 알림 영역 (Area)
Opera Cloud의 알림은 표시 영역에 따라 구분된다:

| 영역 | 설명 |
|------|------|
| **Check-In** | 체크인 시점에 팝업 표시 |
| **Check-Out** | 체크아웃 시점에 팝업 표시 |
| **Reservation** | 예약 조회/수정 시 표시 |
| **In-House** | 재실 예약 조회 시 표시 |

#### 알림 유형
- **Global Alerts**: 전역 알림 (모든 관련 예약에 적용)
- **Reservation-specific Alerts**: 특정 예약에만 적용되는 알림
- **Popup Alerts**: 팝업으로 강제 표시되는 중요 알림
- **Display Alert 체크박스**: 알림 표시 여부 제어

#### 일반적 알림 예시
- VIP 게스트 도착 알림
- 특별 요청 알림 (알레르기, 장애인 편의 등)
- 미결제 잔액 경고
- 블랙리스트 게스트 경고
- 리턴 게스트(재방문) 알림
- 그룹/블록 특별 지시사항

### 5.2 Traces (트레이스)

트레이스는 예약에 연결된 **부서별 메모/지시사항** 시스템이다:

#### 구성 요소
- **Trace Date**: 트레이스가 활성화되는 일자
- **Trace Department**: 트레이스가 전달되는 부서 (Front Desk, Housekeeping, Room Service, Concierge 등)
- **Trace Text**: 지시사항 내용
- **Resolution**: 완료/처리 표시

#### 동작 방식
- 예약 생성/수정 시 트레이스 추가
- 해당 일자에 해당 부서 담당자에게 표시
- 처리 완료 시 Resolved 처리
- 미해결 트레이스는 목록에 계속 표시

#### 트레이스 예시
- "VIP 게스트, 룸에 환영 과일바구니 준비" (HK 부서, 도착일)
- "Late Checkout 14:00 승인됨" (FD 부서, 출발일)
- "Meeting Room A를 13:00에 세팅" (Banquet 부서, 행사일)

### 5.3 Notifications (알림)

상단 네비게이션 바의 알림 아이콘:
- 미읽은 알림 건수를 빨간 배지로 표시
- Notifications 태스크가 사용자 역할에 포함되어야 접근 가능
- "Mark All as Read" 옵션으로 일괄 읽음 처리

### 5.4 Guest Messages (게스트 메시지)

Front Desk Workspace 내 기능:
- 게스트 앞 메시지 등록/관리
- 메시지 수신 알림
- 메시지 전달 상태 추적

### 5.5 Wake-Up Calls (모닝콜)

Front Desk Workspace 내 기능:
- 모닝콜 시간 설정
- 반복 설정
- 완료/미응답 상태 추적

### 5.6 Service Requests (서비스 요청)

Miscellaneous 메뉴 내 기능:
- 게스트 서비스 요청 관리
- 부서별 배정
- 상태 추적 (Open/In Progress/Completed)

### 5.7 Track It / Log It

| 기능 | 설명 |
|------|------|
| **Track It** | 반복적인 이슈/요청 추적 (예: 객실 미니바 보충, 추가 타월 등) |
| **Log It** | 사건/이슈 기록 (예: 게스트 불만, 시설 파손 등) |

---

## 6. Role-Based Dashboard (역할 기반 대시보드)

### 6.1 역할 체계

Opera Cloud는 **Task-based Permission** 시스템을 사용한다:

| 역할 유형 | 설명 |
|----------|------|
| **Chain Administrator** | 체인 전체 관리. 모든 프로퍼티 접근 |
| **Hub Administrator** | 허브(지역 관리 거점) 단위 관리 |
| **Property Administrator** | 개별 프로퍼티 관리 |
| **Front Desk Agent** | 프론트데스크 업무 (체크인/체크아웃/In-House) |
| **Reservations Agent** | 예약 관리 업무 |
| **Housekeeping Supervisor** | 하우스키핑 관리 |
| **Housekeeping Attendant** | 하우스키핑 실무 (모바일) |
| **Revenue Manager** | 레이트/가격 관리 |
| **Sales Manager** | 영업/이벤트 관리 |
| **Night Auditor** | 야간 감사 (EOD 처리) |
| **Cashier** | 캐셔/결제 처리 |

### 6.2 역할별 메뉴 가시성

역할에 부여된 **Task(태스크)** 기반으로 메뉴가 표시된다:

| 역할 | 접근 가능 주요 메뉴 |
|------|------------------|
| **Front Desk Agent** | Front Desk (Arrivals, In-House, Departures), Bookings (Reservations 조회) |
| **Reservations Agent** | Bookings (Reservations, Blocks), Client Relations (Profiles) |
| **Housekeeping Supervisor** | Inventory (Room Management, Task Sheets) |
| **Revenue Manager** | Inventory (Restrictions, Availability), Bookings (Rate Management) |
| **Night Auditor** | Financials (End of Day, Cashiering), Front Desk, Inventory |
| **Chain Administrator** | 모든 메뉴 + Administration, Role Manager |

### 6.3 대시보드 타일 역할별 설정

- **Page Composer**를 통해 역할별 기본 대시보드 페이지 구성 가능
- Chain/Hub/Property 레벨에서 표준 대시보드 설정
- 개별 사용자는 **Edit Dashboard** 태스크 권한이 있어야 타일 관리 가능
- 타일 자체에 별도의 역할 제한은 없으나, 타일이 표시하는 데이터의 접근 권한이 역할에 따라 제한됨

### 6.4 Home Page Selection

사용자가 로그인 후 처음 보는 화면을 설정할 수 있다:
- **Dashboard**: 기본 대시보드 (타일 기반)
- **Reservations**: 예약 워크스페이스
- **Front Desk**: 프론트데스크 워크스페이스
- 역할/프로퍼티 설정에 따라 기본 홈 페이지가 달라질 수 있음

---

## 7. Reservation Calendar / Tape Chart / Availability

### 7.1 Bookings 모듈 구조

```
Bookings
├── Reservations (예약 관리)
├── Reservations Workspace (예약 워크스페이스)
├── Blocks (블록/그룹 예약)
└── Events (이벤트/연회)
```

### 7.2 Tape Chart (테이프 차트)

Opera Cloud의 Tape Chart는 시각적 예약 캘린더로 제공된다:

#### 레이아웃
- **Y축(행)**: 개별 객실 번호 (또는 객실 타입별 그룹핑)
- **X축(열)**: 날짜 (일자별 타임라인)
- **바(Bar)**: 각 예약이 수평 바로 표시되며, 체크인~체크아웃 기간을 시각적으로 나타냄

#### 색상 코드 (예약 상태별)
| 상태 | 색상 (일반적) | 설명 |
|------|-------------|------|
| **Reserved** | 노란색/골드 (Yellow/Gold) | 확정 예약 |
| **Due In (Arrival)** | 초록색 (Green) | 당일 도착 예정 |
| **Checked In / In-House** | 파란색 (Blue) | 체크인 완료, 투숙 중 |
| **Due Out (Departure)** | 주황색 (Orange) | 당일 출발 예정 |
| **Checked Out** | 회색 (Gray) | 체크아웃 완료 |
| **Cancelled** | 빨간색/취소선 (Red/Strikethrough) | 취소된 예약 |
| **No Show** | 빨간색 (Dark Red) | 미도착 |
| **Waitlisted** | 연보라색 (Light Purple) | 대기 예약 |

#### 인터랙션
- 예약 바 클릭 → 예약 상세 팝업
- 빈 셀 클릭 → 신규 예약 생성
- 드래그: 예약 기간 변경 또는 객실 이동 (설정에 따라)
- 줌 인/아웃: 일자 범위 조절 (1주/2주/1개월 등)

#### 필터
- 객실 타입별
- 층별
- 예약 상태별
- 날짜 범위

### 7.3 Property Availability (프로퍼티 가용성)

Inventory → Property Availability

#### Availability Overview (가용성 개요)

| 항목 | 설명 |
|------|------|
| **Total Rooms** | 전체 물리적 객실 수 |
| **OOO Rooms** | Out of Order 객실 수 |
| **OOS Rooms** | Out of Service 객실 수 |
| **Available Rooms** | 판매 가능 객실 수 |
| **Sold Rooms** | 판매된 객실 수 |
| **Remaining Availability** | 잔여 가용 객실 수 |
| **Occupancy %** | 점유율 |
| **ADR** | 평균 객실 단가 (Average Daily Rate) |
| **RevPAR** | 가용 객실당 매출 (Revenue Per Available Room) |

#### 일자별/객실 타입별 그리드
- 행: 객실 타입 (Standard, Deluxe, Suite 등)
- 열: 날짜
- 셀: 해당 날짜/객실 타입의 가용 객실 수

### 7.4 Sellable Availability (판매 가능 가용성)

Inventory → Sellable Availability
- OOO/OOS를 감안한 실제 판매 가능 객실 현황
- 채널별 판매 제한(Sell Limits) 반영
- 레이트 코드별 가용성

### 7.5 Rate Grid (레이트 그리드)

레이트 관리에서 제공하는 요금 캘린더:
- 행: 레이트 코드 / 객실 타입 조합
- 열: 날짜
- 셀: 해당 날짜의 판매 요금
- Restrictions (판매 제한) 표시: CTA(Close to Arrival), CTD(Close to Departure), MinLOS, MaxLOS

### 7.6 Restrictions (판매 제한)

Inventory → Restrictions

| 제한 유형 | 코드 | 설명 |
|----------|------|------|
| **Close to Arrival** | CTA | 해당 일자 도착 불가 |
| **Close to Departure** | CTD | 해당 일자 출발 불가 |
| **Closed** | CL | 해당 일자 판매 중단 |
| **Minimum Length of Stay** | MinLOS | 최소 숙박일수 |
| **Maximum Length of Stay** | MaxLOS | 최대 숙박일수 |
| **Minimum Stay Through** | MinST | 해당 일자를 포함하는 최소 숙박일수 |

---

## 8. Application Navigation (앱 네비게이션)

### 8.1 상단 네비게이션 바 구성

| 위치 | 요소 | 기능 |
|------|------|------|
| 좌상단 | **OPERA Cloud** 로고 | 홈페이지로 이동 |
| 좌상단 | **Primary Menu** | 주요 기능 메뉴 (역할 기반) |
| 중앙 | **Search Bar** | 메뉴 옵션 검색 + "Go" 버튼 |
| 우상단 | **Current Business Date** | 프로퍼티의 현재 영업일 표시 |
| 우상단 | **Current Location** | 현재 프로퍼티/허브 표시. 클릭하여 위치 변경/로그아웃 |
| 우상단 | **Quick Launch (F2)** | 빠른 기능 실행 |
| 우상단 | **Dashboard** 아이콘 | 대시보드 접근 |
| 우상단 | **Notifications** 아이콘 | 알림 (미읽은 건수 빨간 배지) |
| 우상단 | **Side Menu** 아이콘 | 사이드 메뉴 열기 |

### 8.2 Primary Menu (주 메뉴) 구조

역할 기반 접근 제어가 적용되는 주요 메뉴:

| 메뉴 | 하위 항목 |
|------|----------|
| **Client Relations** | Sales Activities, Membership (Loyalty), Profiles, Central Sales |
| **Bookings** | Reservations, Reservations Workspace, Blocks, Events |
| **Front Desk** | Arrivals and Check In, In House, Departures and Checkout, Front Desk Workspace |
| **Inventory** | Restrictions, Property Availability, Room Management, Sellable Availability, Task Sheets, Room Rotation |
| **Financials** | Accounts Receivables, Cashiering and Finance, Commission, Comp Accounting, End of Day |
| **Channel** | Channel Sell Limits, Channel Inventory |
| **Miscellaneous** | Property Brochure, Keys (Door Lock), Service Requests, Changes Log, Track It, Log It, Exports |

### 8.3 Side Menu (사이드 메뉴)

| 항목 | 기능 |
|------|------|
| **Administration** | 시스템 관리 |
| **Toolbox** | 도구 모음 |
| **Exchange** | 데이터 교환 |
| **Role Manager** | 역할 관리 |
| **Search** | 메뉴 옵션 검색 |
| **Help** | 사용자 가이드 실행 |
| **Settings** | 언어 선택, Page Composer 활성화 |
| **Identity Manager** | 사용자 프로필, 비밀번호, 보안 질문 관리 |
| **Credit Card Terminal** | 기본 EMV 결제 단말기 선택 |
| **Toggle Full Screen** | 전체 화면 전환 (기본: 전체 화면) |
| **About** | 애플리케이션 정보 (지원용) |
| **Performance Meter** | 성능 모니터링 도구 |

### 8.4 Quick Launch (빠른 실행)

- **단축키**: F2
- 자주 사용하는 기능에 빠르게 접근
- Quick Links 관리: 개인화된 바로가기 설정
- 검색 기반 실행: 기능명 입력하여 바로 이동

---

## 9. 요약: Hola PMS 대시보드/프론트데스크 설계 시 참고 사항

### 9.1 대시보드 설계 핵심 참고 포인트

1. **타일 기반 인터페이스**: 드래그 앤 드롭으로 재배치 가능한 위젯 방식
2. **역할별 페이지 분리**: Front Desk / Housekeeping / Sales 등 역할별 대시보드 페이지
3. **실시간 데이터**: 60분 활동, 도착/출발/재실 현황 실시간 반영
4. **드릴다운**: 타일 클릭 시 상세 화면으로 이동
5. **핵심 타일 우선순위**:
   - **P0 (필수)**: Arrivals, Departures, In House, Room Status, Room Summary, Reservation Activity
   - **P1 (중요)**: Queue Reservations, Task Sheets, Room Maintenance, Daily Projections, VIP Guests
   - **P2 (부가)**: 60 Minutes Activity, Block Overview, Reservation Statistics, Rooms Availability Summary

### 9.2 프론트데스크 설계 핵심 참고 포인트

1. **3 Main Views**: Arrivals (도착), In-House (재실), Departures (출발)
2. **체크인 15단계 워크플로우**: 단계별 패널 구성, 각 단계 확장/축소 가능
3. **Queue 시스템**: 객실 미준비 시 대기열 관리 + 하우스키핑 연동
4. **Room Rack**: 층별/타입별 시각적 객실 현황, 색상 코드 기반
5. **빠른 체크아웃/Express Checkout**: 잔액 0 또는 카드 자동 정산 시 간소화
6. **Walk-In**: 예약 없이 현장 투숙, 예약 생성 + 체크인 동시 처리

### 9.3 하우스키핑 설계 핵심 참고 포인트

1. **6종 객실 상태**: Clean, Dirty, Inspected, Pickup, OOO, OOS
2. **FO-HK 이중 상태**: Front Office 상태(Vacant/Occupied) x HK 상태 조합
3. **자동 상태 전환**: EOD 시 Clean → Dirty, 체크아웃 시 Occupied → Vacant Dirty
4. **Task Sheet**: 담당자별 객실 배정, 진행률 추적
5. **Room Discrepancy**: FO-HK 간 불일치 감지 및 해결
6. **모바일 지원**: Attendant Console 모바일 접근
