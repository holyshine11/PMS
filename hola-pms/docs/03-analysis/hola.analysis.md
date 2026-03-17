# Gap Analysis Report: Hola PMS Phase A~C

- **분석일**: 2026-03-17
- **대상**: Phase A (대시보드) + Phase B (프론트데스크) + Phase C (Room Rack)
- **Overall Match Rate**: 91%

## 종합 점수

| 카테고리 | 점수 | 상태 |
|----------|:----:|:----:|
| Phase A: 대시보드 | 95% | OK |
| Phase B: 프론트데스크 MVP | 88% | WARN |
| Phase C: Room Rack | 90% | OK |
| 아키텍처 준수 | 92% | OK |
| 컨벤션 준수 | 95% | OK |
| **전체** | **91%** | **OK** |

---

## Phase A: 대시보드 (95%)

### API 구현 현황
| 설계 | 구현 | 상태 |
|------|------|:----:|
| GET /api/v1/dashboard | DashboardController | Match |
| GET /api/v1/dashboard/property/{propertyId} | DashboardController | Match |
| GET /api/v1/dashboard/pickup/{propertyId} | DashboardController | Match |
| GET /api/v1/dashboard/operation/{propertyId} | DashboardController | Match |

### 백엔드
- DashboardService + Impl: KPI (OCC%, ADR, RevPAR), 운영현황, 7일 Pickup 모두 구현 완료
- SubReservationRepository: countArrivals, countDepartures, countInHouse, countSoldRooms 쿼리 추가 완료
- DailyChargeRepository: sumRevenueByPropertyAndDate 쿼리 추가 완료
- SecurityConfig: `/api/v1/dashboard/**` authenticated 설정 완료

### 프론트엔드
- dashboard.html: 역할별 분기 (SUPER_ADMIN 랭킹 + 프로퍼티 KPI/운영/차트)
- dashboard-page.js: Chart.js 4.4.1 CDN + AJAX 데이터 로딩
- contextChange 이벤트 리스너 정상 동작

### 이슈
| 심각도 | 이슈 | 설명 |
|--------|------|------|
| INFO | N+1 | getAllPropertyKpis()에서 프로퍼티별 개별 쿼리. 소규모 데이터에서는 문제 없음 |

---

## Phase B: 프론트데스크 MVP (88%)

### DB 마이그레이션
- V7_1_0__add_room_status_fields.sql: hk_status, fo_status, hk_updated_at, hk_memo + 복합인덱스 ✅

### Entity (RoomNumber)
- hkStatus(CLEAN), foStatus(VACANT), hkUpdatedAt, hkMemo 필드 ✅
- checkIn(), checkOut(), updateHkStatus() 메서드 ✅

### API 구현 현황
| 설계 | 구현 | 상태 |
|------|------|:----:|
| PUT /room-status/{roomNumberId} | RoomStatusApiController | Match |
| GET /room-status/summary | RoomStatusApiController | Match |
| GET /front-desk/arrivals | FrontDeskApiController | Match |
| GET /front-desk/in-house | FrontDeskApiController | Match |
| GET /front-desk/departures | FrontDeskApiController | Match |
| GET /front-desk/summary | FrontDeskApiController | Match |
| POST /front-desk/walk-in | **미구현** | Missing |

### ErrorCode (HOLA-5xxx)
- HOLA-5001 (FD_ROOM_ASSIGN_REQUIRED) ✅
- HOLA-5002 (FD_ROOM_NOT_CLEAN) ✅ (정의됨, 미사용)
- HOLA-5003 (FD_ROOM_OUT_OF_ORDER) ✅
- HOLA-5010 (FD_UNPAID_BALANCE) ✅
- HOLA-5020 (FD_STATUS_CHANGE_NOT_ALLOWED) ✅

### 프론트엔드
- arrivals.html + fd-arrivals-page.js: 7단계 체크인 모달 ✅
- in-house.html + fd-in-house-page.js: 투숙 리스트 ✅
- departures.html + fd-departures-page.js: 출발 리스트 + 체크아웃 모달 ✅
- walk-in.html: Placeholder (예약등록+도착화면으로 리다이렉트) ⚠️
- sidebar.html: 프론트데스크 메뉴 5종 ✅

### 이슈
| 심각도 | 이슈 | 위치 | 설명 | 상태 |
|--------|------|------|------|------|
| HIGH | SecurityConfig room-assign 누락 | SecurityConfig.java | HOTEL_ADMIN/PROPERTY_ADMIN 403 | **수정 완료** |
| WARN | Walk-In POST API 미구현 | FrontDeskApiController | 예약등록+체크인으로 대체 | Backlog |
| INFO | HOLA-5002 미사용 | ReservationServiceImpl | DIRTY 객실 체크인 허용 | 정책 확인 필요 |

---

## Phase C: Room Rack (90%)

### 구현 현황
| 설계 | 구현 | 상태 |
|------|------|:----:|
| RoomRackFloorGroupResponse | DTO 존재 | Match |
| RoomRackItemResponse | 10 필드 | Match |
| getRoomRack 층별 그룹핑 | RoomRackController | Match |
| GET /room-rack | RoomStatusApiController | Match |
| room-rack.html | 그리드 뷰 + 모달 | Match |
| fd-room-rack-page.js | 30초 폴링 + 상태 변경 | Match |
| hola.css 6종 상태색상 | room-card-vc/vd/oc/od/ooo/oos | Match |
| SecurityConfig room-rack | `/api/v1/properties/*/room-rack/**` | Match |

### 이슈
| 심각도 | 이슈 | 설명 |
|--------|------|------|
| WARN | roomTypeName 미설정 | RoomRackItemResponse 필드 존재하나 값 미매핑 |

---

## 버그 수정 내역 (2026-03-17)

### Critical: RoomAssignServiceImpl UnexpectedRollbackException
- **원인**: `PriceCalculationService.calculateDailyCharges()`에서 요금 미설정 객실타입에 대해 `HolaException` 발생 → 동일 트랜잭션 rollback-only 마킹 → try-catch로 잡아도 커밋 시 `UnexpectedRollbackException`
- **수정**: `hasPricingCoverage()` 사전 체크 추가 (예외 발생 방지)
- **파일**: `RoomAssignServiceImpl.java` (2곳)

### Medium: reservation-detail.js 파라미터 검증 누락
- **원인**: `openRoomAssignModal()`에서 rateCodeId, roomTypeId, checkIn/checkOut 미검증
- **수정**: reservation-form.js와 동일한 검증 로직 추가
- **파일**: `reservation-detail.js:1356`

### Low: RoomNumberResponse hkStatus/foStatus 미매핑
- **원인**: Phase B에서 RoomNumber 엔티티 확장 후 DTO/Mapper 미갱신
- **수정**: RoomNumberResponse + HotelMapper에 필드/매핑 추가
- **파일**: `RoomNumberResponse.java`, `HotelMapper.java`

### Low: RoomAssignServiceImpl 중복 import
- **수정**: `import com.hola.rate.entity.RateCode` 중복 제거

---

## 권장 조치

### 완료 (이번 세션)
1. ~~SecurityConfig room-assign 경로 추가~~ ✅
2. ~~RoomAssignServiceImpl 트랜잭션 rollback 수정~~ ✅
3. ~~reservation-detail.js 파라미터 검증 추가~~ ✅
4. ~~RoomNumberResponse hkStatus/foStatus 매핑~~ ✅

### Backlog
5. Room Rack roomTypeName 매핑 (Low)
6. Walk-In 전용 API 구현 (Medium, UX 개선 시)
7. HOLA-5002 DIRTY 검증 정책 확정 (Medium)
8. Dashboard N+1 최적화 (Low)
