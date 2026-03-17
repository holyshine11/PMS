# 대형 체인 호텔 PMS 벤치마킹 보고서

> 조사일: 2026-03-17 | 작성: Hola PMS 프로젝트

---

## 목차
1. [PMS별 벤치마킹 분석](#1-pms별-벤치마킹-분석)
2. [호텔 업계 주요 KPI 정리](#2-호텔-업계-주요-kpi-정리)
3. [멀티테넌시 대시보드 계층 구조 베스트 프랙티스](#3-멀티테넌시-대시보드-계층-구조-베스트-프랙티스)
4. [Hola PMS 적용 시사점](#4-hola-pms-적용-시사점)

---

## 1. PMS별 벤치마킹 분석

### 1.1 Oracle Opera Cloud PMS

**사용 호텔**: Marriott, Hilton, IHG, Hyatt 등 글로벌 체인 (전 세계 40,000+ 호텔)

#### 대시보드 구성
- **30개 이상의 사전 구성된 타일**: Front Desk, Revenue, Inventory, Housekeeping 영역별 제공
- **핵심 KPI 위젯**:
  - OCC% (점유율), ADR (평균객실단가), RevPAR (객실당수익)
  - 7일/30일 Pickup % (객실, 매출, ADR 기준)
  - 전월 대비, 전주 대비 비교 시각화
  - Room Revenue by Property (프로퍼티별 객실 매출)
- **300개 이상의 빌트인 리포트** + 커스텀 대시보드/리포트/시각화 생성 가능
- **타일 제어**: Chain / Property / User 레벨에서 유연하게 제어

#### 역할별 대시보드 차별화
| 역할 | 주요 뷰 |
|------|---------|
| **본사(Chain)** | 전체 프로퍼티 비교 대시보드, 체인 레벨 집계 리포트, 프로퍼티별 드릴다운 |
| **호텔 GM** | 부서별/호텔 전체 성과 분석, ADR/OCC/RevPAR 트렌드, 운영 현황 |
| **Revenue Manager** | Pickup 그리드, Rate Grid (1~4인 요금별), Block 관리, 매출 예측 |
| **프론트데스크** | Room Status (Clean/Dirty/Inspected/Pickup/OOO/OOS), 도착/출발 현황 |

#### 프론트데스크 화면
- **Room Grid/Room Diary**: 객실별 예약 현황을 그리드 형태로 시각화
  - Room Display Order 설정으로 표시 순서 커스터마이징
  - Pickup Grid: Room Type별/일자별 예약 수량 표시
- **체크인/체크아웃**: 자동 객실 배정(Auto Room Blocking), 원스트로크 체크인, Walk-in 빠른 체크인
- **객실 현황**: Clean, Dirty, Inspected, Pick Up, Out of Order, Out of Service 실시간 업데이트
- **Walk-in 처리**: Rapid Walk-in Check-in 기능으로 빠른 등록 처리

#### 실시간 알림/액션
- 체크아웃 시 하우스키핑 자동 알림
- VIP 도착 알림
- 객실 상태 변경 실시간 반영
- 유지보수 요청 자동 작업 지시

#### 모바일/태블릿 대응
- OPERAPalm: 모바일 디바이스에서 원격 체크인 지원
- 포터, 발렛, 하우스키핑 등 직원 모바일 액세스
- 디바이스 해상도에 적응하는 반응형 대시보드

#### 차별화 포인트
- 업계 최대 설치 기반 (40,000+ 호텔)으로 검증된 안정성
- MICROS 생태계와의 통합 (POS, F&B 등)
- 멀티 프로퍼티/체인 관리에 가장 강력한 계층 구조
- IDC MarketScape 2025 Leader 선정

> 참조:
> - [Oracle Opera Cloud PMS](https://www.oracle.com/hospitality/hotel-property-management/hotel-pms-software/)
> - [Opera Cloud Dashboards](https://docs.oracle.com/en/industries/hospitality/opera-reporting-analytics/ugrna/c_dashboards.htm)
> - [Opera Cloud Home Dashboard](https://docs.oracle.com/en/industries/hospitality/opera-cloud/24.4/ocsuh/c_home_dashboard.htm)

---

### 1.2 Mews PMS

**사용 호텔**: Choice Hotels International (글로벌 프랜차이즈), 유럽 부티크/체인 호텔 다수

#### 대시보드 구성
- **Multi-Property KPI Dashboard**: 여러 프로퍼티의 성과 데이터를 한 곳에서 조회
  - Total Operating Revenue (총 운영 매출)
  - Average Occupancy (평균 점유율)
  - Total Guests (총 투숙객)
  - Average Guest Spend (평균 투숙객 지출)
- **Mews Analytics**: 고급 재무 분석 + 대화형 대시보드로 실시간 메트릭 제공
- **ADR, RevPAR, OCC 벤치마킹** 지원

#### 역할별 대시보드 차별화
| 역할 | 주요 뷰 |
|------|---------|
| **본사(HQ)** | Multi-Property KPI Dashboard, 프로퍼티 간 벤치마킹 |
| **Revenue Manager** | 프로퍼티별 매출 극대화 분석, 크로스 프로퍼티 매출 비교 |
| **IT Lead** | 디지털 스택 관리, 통합 현황 |
| **프론트데스크** | Timeline 뷰 (도착/출발/재실), 예약 달력, 게스트 상세 윈도우 |

#### 프론트데스크 화면
- **Smart Timeline**: 인터랙티브 타임라인으로 도착/출발/재실 게스트를 직관적으로 확인
  - Smart Detail Window: 게스트 핵심 정보를 한 곳에서 조회
- **중앙집중 예약 달력**: 통합 예약 관리
- **실시간 가용성 확인**: 다양한 뷰에서 접근 가능

#### 실시간 알림/액션
- 예약/결제/하우스키핑/게스트 커뮤니케이션 통합 알림
- 서드파티 도구와 실시간 데이터 연동

#### 모바일/태블릿 대응
- 클라우드 네이티브: 인터넷 연결만 있으면 어디서든 접근
- 반응형 웹 기반

#### 차별화 포인트
- **클라우드 네이티브 아키텍처**: Day 1부터 클라우드로 설계 (레거시 마이그레이션 아님)
- **프론트데스크 효율성 24% 향상** (벤더 공식 수치)
- **신입 직원 수일 내 숙달** 가능한 직관적 UI
- Hotel Tech Report 2024, 2025 Best PMS 연속 수상
- Choice Hotels International 글로벌 프랜차이즈 채택 (2025)

> 참조:
> - [Mews PMS](https://www.mews.com/en-gb/property-management-system)
> - [Mews Multi-Property KPI Dashboard](https://community.mews.com/mews-business-intelligence-bi-88/introducing-the-new-multi-property-kpi-dashboard-2874)
> - [Mews Multi-Property Management](https://www.mews.com/en/products/multi-property-management)

---

### 1.3 Cloudbeds PMS

**사용 호텔**: 중소규모 호텔, 호스텔, B&B, 바케이션 렌탈 (전 세계 150개국)

#### 대시보드 구성
- **랜딩 페이지 = 대시보드**: 로그인 시 가장 먼저 보이는 화면
  - **Arrivals**: 당일 도착 총 객실 수
  - **Departures**: 당일 출발 객실 수 (체크아웃 잔여 파란색 숫자)
  - **In-house**: 재실 객실/투숙객 수 (1박 이상)
  - **Booked**: 예약 현황 개요
- **Forecast 섹션**: OCC%, ADR, 판매 객실 수 등
- **예약 검색바**: 게스트 이름으로 빠른 예약 검색

#### 프론트데스크 화면
- **드래그 앤 드롭 캘린더**: 예약을 시각적으로 이동/변경
  - 객실 타입 간 이동, 업그레이드, 요금 변경 시 알림 팝업
  - 제한: In-house 상태 예약은 드래그 앤 드롭 불가
- **빠른 예약 생성**: 캘린더에서 직접 예약 생성
- **룸 노트**: 예약에 메모 추가

#### 실시간 알림/액션
- **알림 설정 가능 이벤트**:
  - 새 예약 수신
  - 예약 수정
  - 예약 취소
  - 결제 성공/실패
- **Notifications Center**: 제품 업데이트, 필요 액션 등 안내

#### 모바일/태블릿 대응
- 반응형 웹 기반, 모바일 브라우저 접근 가능

#### 차별화 포인트
- **직관적 드래그 앤 드롭 UI**: 비전문가도 쉽게 사용
- **올인원 플랫폼**: PMS + Channel Manager + Booking Engine 통합
- **중소규모 특화**: 대형 PMS 대비 합리적 가격 + 빠른 도입
- Hotel Tech Report 2026 #1 Top-Rated PMS

> 참조:
> - [Cloudbeds Dashboard Guide](https://myfrontdesk.cloudbeds.com/hc/en-us/articles/115000400634-Dashboard-Everything-you-need-to-know)
> - [Cloudbeds PMS New Dashboard](https://myfrontdesk.cloudbeds.com/hc/en-us/articles/16873372577435-Cloudbeds-Release-Notes-Cloudbeds-PMS-New-Dashboard)
> - [Cloudbeds Notification Preferences](https://myfrontdesk.cloudbeds.com/hc/en-us/articles/115002546514-Cloudbeds-PMS-Notification-Preferences)

---

### 1.4 Protel PMS

**사용 호텔**: 유럽 대형 체인, 개별 호텔, 리조트 (14,000+ 호텔)

#### 대시보드 구성
- **커스터마이저블 대시보드**: 워크플로우에 맞게 필요한 데이터만 표시
- **Multi-Property 중앙 대시보드**: 전체 지점을 하나의 대시보드에서 관리
  - 예약, 매출, 운영 통합 뷰
  - 중앙집중 리포팅 및 컨트롤

#### 프론트데스크 화면
- 체크인/체크아웃, 프로필 조회, 예약 관리, 객실 변경
- 요금 등록, 인보이스 출력, 결제 처리
- 실시간 객실 상태 업데이트

#### 실시간 알림/액션
- 하우스키핑 ↔ 프론트데스크 실시간 객실 상태 동기화
- 청소 스케줄, 유지보수 추적 자동화

#### 모바일/태블릿 대응
- protel Air (클라우드 버전): 웹 기반 접근
- 모바일 최적화 제한적 (데스크톱 중심)

#### 차별화 포인트
- **1,200개+ 호스피탈리티 통합**: protel io 인터페이스를 통한 방대한 연동 생태계
- **유연한 호텔 유형 지원**: 개별, 체인, 리조트 등 다양한 유형
- **유럽 시장 강점**: GDPR 등 유럽 규제 완벽 대응

> 참조:
> - [Protel Cloud PMS](https://www.europrotel.com/en/protel-cloud-pms/)
> - [Protel Front Desk](https://www.protel.net/cloud-pms/front-desk/)

---

### 1.5 StayNTouch PMS

**사용 호텔**: Yotel, citizenM 등 모바일 퍼스트 브랜드 호텔

#### 대시보드 구성
- **Manager Dashboard 타일**:
  - ARRIVALS (도착)
  - MOBILE CHECK IN (모바일 체크인)
  - QUEUED (대기)
  - STAYOVERS (연박)
  - DEPARTURES (출발)
  - CHECKED OUT WITH BALANCE (잔액 있는 체크아웃)
  - LATE CHECK OUTS (늦은 체크아웃)
  - VIPS (VIP 투숙객)
- **운영 통계**:
  - Occupancy Statistics (점유율 통계)
  - Housekeeping Statistics (하우스키핑 통계)
  - ADR (평균객실단가)
  - Rate of the Day (당일 요금)
  - Upsells (업셀 현황)

#### 역할별 대시보드 차별화
| 역할 | 주요 뷰 |
|------|---------|
| **매니저** | 전체 운영 현황 대시보드 (도착/출발/점유율/ADR/업셀) |
| **프론트데스크** | 체크인/아웃 화면, Room Diary, 게스트 폴리오 |
| **하우스키핑** | 객실 상태 업데이트, 청소 태스크 |

#### 프론트데스크 화면
- **Dynamic Room Diary**: 직관적인 객실 일지
  - "Select-and-Click" + "Drag-and-Drop" 기능
  - 색상 코딩된 직관적 인터페이스
- **체크인/아웃 화면**: 직관적 원스크린 처리
- **객실 변경(Move Room)**: 별도 직관적 화면 제공

#### 실시간 알림/액션
- 모바일 체크인 요청 알림
- 업셀 오퍼 자동 발송 및 승인 알림
- VIP 도착 알림
- 잔액 있는 체크아웃 알림

#### 모바일/태블릿 대응
- **모바일 퍼스트 설계**: 핵심 차별화 포인트
  - 스마트폰/태블릿에서 전체 PMS 기능 사용
  - 로비, 레스토랑, 풀사이드 등 어디서든 체크인 처리
- **Guest Kiosk**: 태블릿 기반 셀프 체크인 키오스크
  - 브랜드 이미지에 맞게 완전 커스터마이징
  - 자동 객실 배정, 타임드 배정
  - 업셀/어메니티/얼리체크인 자동 오퍼
- **모바일 키**: 키리스 디지털 객실 접근

#### 차별화 포인트
- **모바일 퍼스트**: 업계 최초의 완전 모바일 PMS
- **교육 시간 2시간**: 프론트 직원 즉시 투입 가능
- **자동 업셀**: 부대수입 최대 18% 증가, ROI 240% 달성
- **Staff-Assisted 체크인 54% 감소**: 인건비 절감
- 2025 "Hotel PMS of the Year" 수상

> 참조:
> - [StayNTouch Cloud PMS](https://www.stayntouch.com/cloud-pms/)
> - [StayNTouch Front Desk](https://www.stayntouch.com/front-desk/)
> - [StayNTouch Guest Kiosk](https://www.stayntouch.com/guest-kiosk/)
> - [StayNTouch Dashboard](https://stayntouch.freshdesk.com/support/solutions/articles/24000077356-dashboard)

---

### 1.6 RoomRaccoon PMS

**사용 호텔**: 독립 호텔, 부티크 호텔, 게스트하우스 (유럽/남아프리카 중심)

#### 대시보드 구성
- **올인원 대시보드**: 모든 데이터를 단일 대시보드에서 조회
  - 예약 현황 (색상 코딩된 직관적 디자인)
  - 실시간 객실 상태
  - 성과 리포트 및 전년 대비(YoY) 비교
- **채널별 성과**: 200+ OTA/메타서치/GDS 실시간 동기화 현황

#### 프론트데스크 화면
- 색상 코딩된 예약 상태 표시
- 게스트 여정 전체 자동화 (확인 → 사전 도착 → 업셀 → 체크아웃 후 피드백)

#### 실시간 알림/액션
- 자동 게스트 커뮤니케이션 (이메일/메시지)
  - 예약 확인
  - 사전 도착 정보
  - 업셀 오퍼
  - 체크아웃 후 피드백 요청
- 하우스키핑 모듈: 청소 스케줄링, 유지보수 관리, 객실 상태 실시간 업데이트

#### 모바일/태블릿 대응
- 클라우드 기반, 모바일 접근 가능

#### 차별화 포인트
- **AI 기반 동적 가격 책정 (RaccoonRev Plus)**:
  - 시장 수요, 경쟁사 요금, 예약 트렌드, 지역 이벤트 분석
  - 365일 앞까지 스마트 자동 가격 설정
  - 업계에서 가장 진보된 AI 프라이싱 엔진 중 하나
- **자동 업셀링**: 게스트 여정 중 자동 업셀 오퍼
- **독립 호텔 특화**: 대형 PMS 대비 간결하고 사용하기 쉬운 인터페이스

> 참조:
> - [RoomRaccoon PMS](https://roomraccoon.com/platform/pms/)
> - [RoomRaccoon AI Pricing](https://roomraccoon.com/platform/ai-pricing-for-hotels/)
> - [RoomRaccoon Platform](https://roomraccoon.com/platform/)

---

### 1.7 국내 PMS: 산하정보기술(야놀자) 윙스 PMS + 기타

> 삼성SDS SHMS, LG CNS 호텔솔루션은 공개 정보가 매우 제한적입니다.
> 삼성은 LYNK HMS(객실 내 TV/IoT 관리)에 집중하고 있으며, LG CNS는 스마트 객실 서비스(IoT)에 집중합니다.
> 실질적인 국내 PMS 시장은 산하정보기술(야놀자 자회사)이 70% 이상 점유하고 있어 이를 중심으로 분석합니다.

#### 산하정보기술 윙스(Wings) PMS
- **시장 점유율**: 국내 PMS 시장 70% 이상 (1위)
- **아키텍처**: 클라우드 기반 관리 시스템
- **주요 기능**:
  - 원격 운영 관리
  - 호텔 규모/객실 수/부대시설에 따른 모듈 적용
  - CS 요청 실시간 대응
  - PMS + 스마트 키오스크 + RM(수익관리) + 객실제어 시스템 연동
- **프론트데스크**: 스마트 키오스크 통한 셀프 체크인/아웃, AI 안면인식 본인 인증
- **고객**: 국내 진출 일본 호텔 브랜드 전체 공급 (2025)

#### 삼성 LYNK HMS
- PMS가 아닌 **객실 내 디스플레이/IoT 관리 솔루션**
- 호텔 PMS와 연동하여 게스트 정보 동기화
- 원격 콘텐츠 관리, 게스트 콘텐츠 사용 분석
- LYNK Cloud: 글로벌 원격 프로퍼티 관리

#### LG CNS 스마트 호텔 서비스
- PMS가 아닌 **스마트 객실 IoT 플랫폼**
- 스마트폰 기반 객실키, 온도/조명/TV 제어
- 청소 요청 등 편의 서비스
- 스마트 결제 서비스, 에너지 절감 서비스

> 참조:
> - [산하정보기술 PMS](https://www.sanhait.co.kr/)
> - [야놀자 산하정보기술 일본 호텔 PMS 공급](https://www.newsis.com/view/NISX20251125_0003415320)
> - [Samsung LYNK Cloud](https://www.samsung.com/us/business/solutions/industries/hospitality/lynk-cloud/)

---

## 2. 호텔 업계 주요 KPI 정리

### 2.1 핵심 수익 지표

| KPI | 산출 공식 | 설명 |
|-----|-----------|------|
| **OCC% (Occupancy Rate)** | 판매객실 / 판매가능객실 x 100 | 객실 점유율. 가장 기본적인 운영 지표 |
| **ADR (Average Daily Rate)** | 총 객실매출 / 판매객실 수 | 평균 객실 단가. 가격 전략 효과 측정 |
| **RevPAR (Revenue Per Available Room)** | 총 객실매출 / 판매가능객실 = OCC% x ADR | 객실당 수익. OCC와 ADR을 통합한 핵심 성과 지표 |
| **TRevPAR (Total Revenue Per Available Room)** | 총 매출(F&B, 부대시설 포함) / 판매가능객실 | 객실 외 수익까지 포함한 총체적 성과 지표 |
| **GOPPAR (Gross Operating Profit Per Available Room)** | 총 영업이익 / 판매가능객실 | **비용까지 고려한 실질 수익성** 지표. 2025년 이후 RevPAR을 보완하는 핵심 KPI로 부상 |
| **ARPAR (Adjusted Revenue Per Available Room)** | (객실매출 - 변동비) / 판매가능객실 | 변동비 차감 후 순수 객실 수익 |
| **NRevPAR (Net Revenue Per Available Room)** | (객실매출 - OTA 수수료) / 판매가능객실 | 유통 채널 비용 차감 후 순수익 |

### 2.2 예약/수요 관리 지표

| KPI | 설명 |
|-----|------|
| **Pickup (예약 증감)** | 특정 기간 대비 새로운 예약 건수/금액 증감. 일별/주별/월별 추적 |
| **Booking Pace** | 특정 숙박일 기준으로 예약이 들어오는 속도. 전년 동기 대비 비교 |
| **Pace Report** | Booking Pace를 시각화한 리포트. 1년 전까지의 예약 추이를 그래프로 표시. Revenue Manager의 가격 조정 근거 |
| **Lead Time** | 예약일부터 체크인일까지의 기간. 채널별/세그먼트별 분석 |
| **Cancellation Rate** | 총 예약 대비 취소 비율. 채널별 분석 필수 |
| **No-show Rate** | 예약 후 미도착 비율. 오버부킹 정책의 근거 |
| **Wash Factor** | 블록 예약 대비 실제 사용 비율. 단체 예약 관리 핵심 |

### 2.3 유통/채널 관리 지표

| KPI | 설명 |
|-----|------|
| **Channel Mix** | 유통 채널별 예약 비율 (Direct, OTA, GDS, 전화, Walk-in 등). 직접 예약 비율 증가가 수익성 향상의 핵심 |
| **Segment Mix** | 시장 세그먼트별 예약 비율 (Business, Leisure, Group, Wholesale 등) |
| **Direct Booking Ratio** | 직접 예약(웹사이트, 전화) 비율. OTA 의존도 감소 = 수수료 절감 |
| **OTA Commission Rate** | OTA별 평균 수수료율. 채널별 NRevPAR 비교 근거 |
| **CPA (Cost Per Acquisition)** | 고객 1명 획득 비용. 채널별 마케팅 효율 측정 |

### 2.4 운영 효율 지표

| KPI | 설명 |
|-----|------|
| **Average Length of Stay (ALOS)** | 평균 투숙 기간 |
| **Guest Satisfaction Score** | 투숙객 만족도 점수 (설문, OTA 리뷰 기반) |
| **Online Review Score** | OTA 리뷰 평점 (Booking.com, Expedia 등) |
| **Staff-to-Room Ratio** | 객실당 직원 수. 인건비 효율성 측정 |
| **Energy Cost Per Occupied Room** | 점유 객실당 에너지 비용 |
| **Repeat Guest Rate** | 재방문 고객 비율. 로열티 프로그램 효과 측정 |
| **Upsell Conversion Rate** | 업셀 오퍼 대비 전환율 |

### 2.5 KPI 트렌드 (2025~2026)

1. **RevPAR → GOPPAR 전환**: 매출만이 아닌 수익성 중심 경영. 인건비/채널비용 등을 반영한 GOPPAR이 핵심 KPI로 부상
2. **TRevPAR 중시**: F&B, 스파, 부대시설 등 비객실 매출의 중요성 증가
3. **실시간 대시보드**: 일별/주별 정적 리포트에서 실시간 라이브 대시보드로 전환
4. **AI 기반 예측**: 과거 데이터 기반 예측에서 AI/ML 기반 수요 예측으로 진화
5. **채널 수익성 분석**: 단순 Channel Mix가 아닌 채널별 NRevPAR/GOPPAR 비교

> 참조:
> - [Hotel KPIs: The Ultimate Guide](https://hoteltechreport.com/news/hotel-kpis)
> - [15 Hotel Performance Metrics & KPIs 2026](https://www.priority-software.com/resources/hotel-performance-metrics/)
> - [Best Hotel KPIs in 2025: Beyond RevPAR](https://mappingmegan.com/best-hotel-kpis-in-2025-beyond-revpar-why-trevpar-and-goppar-tell-the-real-story/)
> - [Sigma Revenue KPI Dashboard](https://sigmarevenue.com/en/product/kpi-dashboard/)

---

## 3. 멀티테넌시 대시보드 계층 구조 베스트 프랙티스

### 3.1 계층 구조 모델

```
Level 0: Chain/Corporate HQ (본사)
  └─ Level 1: Brand/Hotel Group (브랜드/호텔그룹)
       └─ Level 2: Property (프로퍼티)
            └─ Level 3: Department/Front Desk (부서/프론트데스크)
```

### 3.2 레벨별 대시보드 설계

#### Level 0: 본사(Corporate) 대시보드

| 항목 | 내용 |
|------|------|
| **목적** | 전체 포트폴리오 성과 모니터링 및 전략 의사결정 |
| **주요 KPI** | RevPAR, GOPPAR, TRevPAR, OCC% (전체/브랜드별/지역별) |
| **핵심 뷰** | 프로퍼티 비교 랭킹 테이블, 지역별 히트맵, 트렌드 차트 |
| **기간** | MTD, QTD, YTD, YoY 비교 |
| **드릴다운** | 브랜드 → 프로퍼티 → 부서 레벨까지 |
| **알림** | 성과 이상 탐지 (목표 대비 미달 프로퍼티), 중요 이슈 에스컬레이션 |

**Opera Cloud 사례**: 300+ 빌트인 리포트, 체인 레벨 대시보드에서 프로퍼티별 드릴다운

#### Level 1: 호텔/브랜드 대시보드

| 항목 | 내용 |
|------|------|
| **목적** | 브랜드 산하 프로퍼티 비교 및 운영 최적화 |
| **주요 KPI** | ADR, RevPAR, OCC%, Channel Mix, Pickup Pace |
| **핵심 뷰** | 프로퍼티 간 벤치마킹, 주간/월간 Pace Report |
| **역할** | Hotel GM, Regional Manager, Revenue Manager |
| **기능** | 프로퍼티별 요금 전략 비교, 채널별 수익성 분석 |

**Mews 사례**: Multi-Property KPI Dashboard로 Total Operating Revenue, Average Occupancy, Total Guests, Average Guest Spend를 프로퍼티별 비교

**StayNTouch 사례**: Chain Dashboard에서 각 호텔의 객실 수, 현재 OCC%, 전일/익일 OCC%, BAR(Best Available Rate) 표시

#### Level 2: 프로퍼티 대시보드

| 항목 | 내용 |
|------|------|
| **목적** | 단일 프로퍼티 일일 운영 관리 |
| **주요 KPI** | 당일 OCC%, ADR, RevPAR, Arrivals, Departures, In-house |
| **핵심 뷰** | 7일 Pickup 차트, 객실 타입별 가용성, 하우스키핑 현황 |
| **역할** | Property Manager, Duty Manager |
| **기능** | 당일 운영 현황 + 향후 7~30일 예측 |

**Opera Cloud 사례**: 30+ 사전 구성 타일 (Front Desk, Revenue, Inventory, Housekeeping), 7일/30일 Pickup % 위젯

#### Level 3: 프론트데스크/운영 대시보드

| 항목 | 내용 |
|------|------|
| **목적** | 오늘의 실시간 운영 화면 |
| **주요 뷰** | Arrivals List, Departures List, In-house List, Room Rack/Diary |
| **실시간 정보** | 객실 상태(Clean/Dirty/OOO/OOS), VIP 도착, 잔액 체크아웃 |
| **액션** | 체크인/아웃, Walk-in 등록, 객실 변경, 요금 등록, 키 발급 |
| **역할** | Front Desk Agent, Night Auditor |

### 3.3 베스트 프랙티스 요약

1. **Role-Based Access Control (RBAC)**: 직원 역할에 따라 볼 수 있는 데이터와 기능 제한
   - 본사 임원: 모든 프로퍼티 재무 데이터
   - 지역 매니저: 담당 지역 프로퍼티만
   - 프론트 직원: 자기 프로퍼티 운영 데이터만

2. **계층적 드릴다운**: 상위 레벨에서 하위 레벨로 자연스러운 네비게이션
   - 체인 OCC% 클릭 → 프로퍼티별 OCC% → 객실타입별 OCC%

3. **표준화 → 비교**: 모든 프로퍼티에서 동일한 KPI 정의와 계산 방식 사용
   - 객실명, 객실타입, OCC% 산출 기준 통일

4. **실시간 + 이력**: 프론트데스크는 실시간, 경영진은 실시간 + 기간별 트렌드

5. **유연한 타일 구성**: 사용자가 대시보드 타일을 추가/제거/재배치 가능
   - Opera Cloud의 타일 시스템이 대표적

6. **알림 계층화**: 레벨별 알림 대상 차별화
   - Level 3: 체크아웃 알림, VIP 도착, 하우스키핑 완료
   - Level 2: 일일 마감 리포트, 오버부킹 경고
   - Level 1: 주간 성과 리포트, 목표 미달 알림
   - Level 0: 월간 포트폴리오 리포트, 이상 탐지

> 참조:
> - [Multi-Property Hotel PMS: Centralize Operations](https://blog.hotelogix.com/multi-property-hotel-pms/)
> - [PMS for Hotels and Multi-Property Control](https://www.hotelspeak.com/2025/11/pms-for-hotels-explained-and-the-path-to-multi-property-control/)
> - [StayNTouch Multi-Property Management](https://www.stayntouch.com/news/hotel-groups-benefit-from-streamlined-multi-property-management-with-stayntouch-pms-enhanced-chain-functionality/)
> - [Jonas Chorum Enterprise PMS](https://jonaschorum.com/your-hotel-property-management-system-should-do-more-for-your-portfolio-the-importance-of-a-multi-property-enterprise-pms/)
> - [WebRezPro Multi-Property PMS](https://webrezpro.com/multi-property-pms/)

---

## 4. Hola PMS 적용 시사점

### 4.1 대시보드 설계 권장사항

#### 1) 계층별 대시보드 구현 (4레벨)

```
SUPER_ADMIN (본사) → 전체 호텔/프로퍼티 비교 대시보드
HOTEL_ADMIN  (호텔) → 호텔 산하 프로퍼티 비교 대시보드
PROPERTY_ADMIN (프로퍼티) → 단일 프로퍼티 운영 대시보드
FRONT_DESK (프론트) → 오늘의 운영 화면 (향후 추가 역할)
```

#### 2) 대시보드 위젯 우선순위 (Phase별)

**Phase 1 (MVP)**:
- 당일 Arrivals / Departures / In-house 카운트 타일
- OCC%, ADR, RevPAR 핵심 3대 지표
- 7일 Pickup 차트
- 객실 상태 현황 (Clean/Dirty/OOO/OOS)

**Phase 2 (운영 고도화)**:
- Channel Mix / Segment Mix 파이차트
- 30일 Pace Report
- No-show Rate / Cancellation Rate
- 하우스키핑 진행률
- GOPPAR, TRevPAR

**Phase 3 (고도화)**:
- 프로퍼티 간 비교 랭킹 테이블
- YoY 비교 트렌드
- AI 기반 수요 예측
- 실시간 알림 센터

#### 3) 프론트데스크 화면 권장 구조

| 화면 | 설명 | 참고 PMS |
|------|------|----------|
| **Room Rack** | 층/호수별 객실 상태 그리드 (Clean/Dirty/OOO/OOS + 투숙객) | Opera, Protel |
| **Reservation Timeline** | 일자별 예약 타임라인 (드래그앤드롭) | Mews, Cloudbeds |
| **Today's Activity** | 당일 체크인/체크아웃 리스트 + 액션 버튼 | StayNTouch |
| **Quick Actions** | Walk-in, 빠른 체크인, 객실 변경, 요금 등록 | Opera, StayNTouch |

#### 4) 실시간 알림 이벤트 (권장 목록)

| 이벤트 | 대상 역할 | 긴급도 |
|--------|-----------|--------|
| VIP 도착 예정 | 프론트, 매니저 | 높음 |
| 체크아웃 완료 → 청소 요청 | 하우스키핑 | 높음 |
| 오버부킹 발생 | 매니저 | 긴급 |
| Walk-in 접수 | 프론트 | 보통 |
| 새 예약 수신 | 프론트, 매니저 | 보통 |
| 예약 취소 | 프론트, 매니저 | 보통 |
| No-show 발생 | 프론트, 매니저 | 보통 |
| 결제 실패 | 프론트, 매니저 | 높음 |
| 유지보수 요청 | 시설팀, 매니저 | 보통 |
| 잔액 있는 체크아웃 | 프론트, 매니저 | 높음 |
| 늦은 체크아웃 요청 | 프론트, 매니저 | 보통 |

### 4.2 기존 Hola PMS 구조와의 매핑

| Hola PMS 현재 구조 | 대시보드 적용 |
|---------------------|---------------|
| SUPER_ADMIN 역할 | Level 0/1 대시보드 (전체 호텔/프로퍼티 비교) |
| HOTEL_ADMIN 역할 | Level 1 대시보드 (호텔 산하 프로퍼티 비교) |
| PROPERTY_ADMIN 역할 | Level 2 대시보드 (단일 프로퍼티 운영) |
| HolaPms.context (호텔/프로퍼티 선택) | 대시보드 컨텍스트 자동 전환 |
| M07 예약 모듈 (예약/결제) | Arrivals/Departures/Revenue KPI 데이터 소스 |
| M02 객실 모듈 (객실타입/호수) | Room Rack / 가용성 데이터 소스 |
| M03 레이트 모듈 (레이트코드) | ADR/RevPAR 계산 데이터 소스 |

---

## PMS 비교 요약 매트릭스

| 기능 | Opera Cloud | Mews | Cloudbeds | Protel | StayNTouch | RoomRaccoon |
|------|:-----------:|:----:|:---------:|:------:|:----------:|:-----------:|
| **대상 시장** | 대형 체인 | 중대형 체인 | 중소규모 | 유럽 체인 | 모바일 퍼스트 | 독립 호텔 |
| **클라우드 네이티브** | O (마이그레이션) | O (Day 1) | O | O | O | O |
| **멀티 프로퍼티** | 최강 | 강함 | 제한적 | 강함 | 강함 | 제한적 |
| **Room Rack/Grid** | Room Grid | Timeline | Calendar | O | Room Diary | 색상 코딩 |
| **드래그앤드롭** | 제한적 | O | O | X | O | X |
| **모바일 PMS** | OPERAPalm | 반응형 | 반응형 | 제한적 | 최강 | 반응형 |
| **셀프 체크인** | 연동 | O | O | 연동 | Kiosk + Mobile | 연동 |
| **AI 가격** | 제한적 | X | X | X | X | RaccoonRev Plus |
| **자동 업셀** | X | O | X | X | 최강 | O |
| **통합 수** | 5,000+ | 1,000+ | 300+ | 1,200+ | 1,100+ | 200+ |
| **교육 시간** | 수주 | 수일 | 수일 | 수일~수주 | 2시간 | 수일 |
| **대표 가격** | $$$$ | $$$ | $$ | $$$ | $$$ | $$ |

---

*본 보고서는 공개된 웹 자료를 기반으로 작성되었으며, 각 PMS 벤더의 최신 업데이트에 따라 기능이 변경될 수 있습니다.*
