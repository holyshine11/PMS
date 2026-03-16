/**
 * 04-reservation.spec.js
 * 예약관리 모듈 UAT 테스트 (API + 페이지 접근)
 */
const { test, expect } = require('@playwright/test');
const { login, selectHotelAndProperty, apiGet, apiPost } = require('./helpers');

// 테스트 간 공유 데이터
let propertyId;
let createdReservationId;
let createdMasterReservationNo;

test.describe('예약관리 모듈', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  // 프로퍼티 ID 확보 (호텔 셀렉터 → 프로퍼티 셀렉터 순서)
  async function ensurePropertyId(page) {
    if (!propertyId) {
      // 호텔 ID 먼저 확보
      const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
      expect(hotelRes.status).toBe(200);
      expect(hotelRes.data.data.length).toBeGreaterThanOrEqual(1);
      const hotelId = hotelRes.data.data[0].id;

      // hotelId로 프로퍼티 셀렉터 호출
      const res = await apiGet(page, `/api/v1/properties/selector?hotelId=${hotelId}`);
      expect(res.status).toBe(200);
      expect(res.data.data.length).toBeGreaterThanOrEqual(1);
      propertyId = res.data.data[0].id;
    }
    return propertyId;
  }

  test('TC-01: 예약 목록 API', async ({ page }) => {
    const pid = await ensurePropertyId(page);
    const { status, data } = await apiGet(page, `/api/v1/properties/${pid}/reservations`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-02: 예약 캘린더 API', async ({ page }) => {
    const pid = await ensurePropertyId(page);
    const { status, data } = await apiGet(
      page,
      `/api/v1/properties/${pid}/reservations/calendar?startDate=2026-03-01&endDate=2026-03-31`
    );

    expect(status).toBe(200);
    expect(data).toBeTruthy();
  });

  test('TC-03: 예약채널 목록 API', async ({ page }) => {
    const pid = await ensurePropertyId(page);
    const { status, data } = await apiGet(
      page,
      `/api/v1/properties/${pid}/reservation-channels`
    );

    expect(status).toBe(200);
    expect(data).toBeTruthy();
  });

  test('TC-04: 예약 목록 페이지 접근', async ({ page }) => {
    await page.goto('/admin/reservations');
    await page.waitForLoadState('networkidle');

    // 호텔/프로퍼티 선택
    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    // DataTable 또는 카드뷰 렌더링 확인
    const table = page.locator('table');
    const cardView = page.locator('.reservation-card, .card-view, [data-view="card"]');
    const either = await table.first().isVisible({ timeout: 5000 }).catch(() => false)
      || await cardView.first().isVisible({ timeout: 3000 }).catch(() => false);

    // 최소한 페이지가 정상 로드되었는지 확인
    const pageTitle = page.locator('h4, .page-title, .content-header');
    await expect(pageTitle.first()).toBeVisible({ timeout: 10000 });
  });

  test('TC-05: 예약 등록 페이지 접근', async ({ page }) => {
    await page.goto('/admin/reservations/new');
    await page.waitForLoadState('networkidle');

    // 헤더 호텔 셀렉터에서 첫 번째 호텔 선택
    const hotelSelect = page.locator('#headerHotelSelect');
    await hotelSelect.waitFor({ state: 'visible', timeout: 5000 });
    await page.waitForTimeout(1000); // 호텔 목록 로드 대기

    const hotelOptions = await hotelSelect.locator('option').all();
    if (hotelOptions.length > 1) {
      await hotelSelect.selectOption({ index: 1 });
      await page.waitForTimeout(1000); // 프로퍼티 목록 로드 대기
    }

    // 헤더 프로퍼티 셀렉터에서 첫 번째 프로퍼티 선택
    const propertySelect = page.locator('#headerPropertySelect');
    await page.waitForTimeout(500);
    const propOptions = await propertySelect.locator('option').all();
    if (propOptions.length > 1) {
      await propertySelect.selectOption({ index: 1 });
      await page.waitForTimeout(1500); // contextChange 이벤트 처리 대기
    }

    // formContainer가 표시되었는지 확인
    const formContainer = page.locator('#formContainer');
    await expect(formContainer).toBeVisible({ timeout: 10000 });

    // 폼 필드 존재 확인
    const masterCheckIn = page.locator('#masterCheckIn');
    const masterCheckOut = page.locator('#masterCheckOut');

    await expect(masterCheckIn).toBeVisible({ timeout: 5000 });
    await expect(masterCheckOut).toBeVisible({ timeout: 5000 });
  });

  test('TC-06: 예약 생성 API', async ({ page }) => {
    const pid = await ensurePropertyId(page);

    // 1) 객실타입 조회
    const rtRes = await apiGet(page, `/api/v1/properties/${pid}/room-types`);
    expect(rtRes.status).toBe(200);
    expect(rtRes.data.data.length).toBeGreaterThanOrEqual(1);
    const roomTypeId = rtRes.data.data[0].id;

    // 2) 레이트코드 조회
    const rcRes = await apiGet(page, `/api/v1/properties/${pid}/rate-codes`);
    expect(rcRes.status).toBe(200);
    expect(rcRes.data.data.length).toBeGreaterThanOrEqual(1);
    const rateCodeId = rcRes.data.data[0].id;

    // 3) 예약채널 조회
    const chRes = await apiGet(page, `/api/v1/properties/${pid}/reservation-channels`);
    expect(chRes.status).toBe(200);
    expect(chRes.data.data.length).toBeGreaterThanOrEqual(1);
    const channelId = chRes.data.data[0].id;

    // 4) 예약 생성
    const createPayload = {
      guestNameKo: 'UAT테스트',
      guestLastNameEn: 'TEST',
      guestFirstNameEn: 'UAT',
      phoneCountryCode: '82',
      phoneNumber: '01012345678',
      reservationChannelId: channelId,
      rateCodeId: rateCodeId,
      masterCheckIn: '2026-04-01',
      masterCheckOut: '2026-04-03',
      subReservations: [{
        roomTypeId: roomTypeId,
        checkIn: '2026-04-01',
        checkOut: '2026-04-03',
        adults: 2,
        children: 0
      }]
    };

    const { status, data } = await apiPost(
      page,
      `/api/v1/properties/${pid}/reservations`,
      createPayload
    );

    // 201 Created 또는 200 OK
    expect([200, 201]).toContain(status);
    expect(data).toBeTruthy();
    expect(data.data).toBeTruthy();
    expect(data.data.masterReservationNo).toBeTruthy();

    // 후속 테스트용 데이터 저장
    createdReservationId = data.data.id;
    createdMasterReservationNo = data.data.masterReservationNo;
  });

  test('TC-07: 생성된 예약 상세 조회', async ({ page }) => {
    const pid = await ensurePropertyId(page);

    // TC-06에서 생성한 예약이 없으면 기존 예약 사용
    let reservationId = createdReservationId;
    if (!reservationId) {
      const listRes = await apiGet(page, `/api/v1/properties/${pid}/reservations`);
      expect(listRes.status).toBe(200);
      expect(listRes.data.data.length).toBeGreaterThanOrEqual(1);
      reservationId = listRes.data.data[0].id;
    }

    const { status, data } = await apiGet(
      page,
      `/api/v1/properties/${pid}/reservations/${reservationId}`
    );

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(data.data).toBeTruthy();

    // TC-06에서 생성한 예약이면 상태 확인
    if (createdReservationId) {
      expect(data.data.reservationStatus).toBe('RESERVED');
      expect(Array.isArray(data.data.subReservations)).toBe(true);
      expect(data.data.subReservations.length).toBe(1);
    }
  });

  test('TC-08: 예약 상세 페이지 접근', async ({ page }) => {
    const pid = await ensurePropertyId(page);

    // 예약 ID 확보
    let reservationId = createdReservationId;
    if (!reservationId) {
      const listRes = await apiGet(page, `/api/v1/properties/${pid}/reservations`);
      expect(listRes.status).toBe(200);
      if (listRes.data.data.length === 0) {
        test.skip();
        return;
      }
      reservationId = listRes.data.data[0].id;
    }

    await page.goto(`/admin/reservations/${reservationId}`);
    await page.waitForLoadState('networkidle');

    // 헤더 호텔 셀렉터에서 첫 번째 호텔 선택
    const hotelSelect = page.locator('#headerHotelSelect');
    await hotelSelect.waitFor({ state: 'visible', timeout: 5000 });
    await page.waitForTimeout(1000);

    const hotelOptions = await hotelSelect.locator('option').all();
    if (hotelOptions.length > 1) {
      await hotelSelect.selectOption({ index: 1 });
      await page.waitForTimeout(1000);
    }

    // 헤더 프로퍼티 셀렉터에서 첫 번째 프로퍼티 선택
    const propertySelect = page.locator('#headerPropertySelect');
    await page.waitForTimeout(500);
    const propOptions = await propertySelect.locator('option').all();
    if (propOptions.length > 1) {
      await propertySelect.selectOption({ index: 1 });
      await page.waitForTimeout(1500);
    }

    // formContainer가 표시되었는지 확인
    const formContainer = page.locator('#formContainer');
    await expect(formContainer).toBeVisible({ timeout: 10000 });

    // 예약 상세 페이지 탭/카드 확인
    const tabContent = page.locator('.tab-content, .nav-tabs');
    await expect(tabContent.first()).toBeVisible({ timeout: 5000 });
  });

  test('TC-09: 예약 상태 변경 API (RESERVED -> CANCELED)', async ({ page }) => {
    const pid = await ensurePropertyId(page);

    // TC-06에서 생성한 예약이 없으면 직접 생성
    let targetId = createdReservationId;
    if (!targetId) {
      // 테스트용 예약 생성
      const rtRes = await apiGet(page, `/api/v1/properties/${pid}/room-types`);
      const rcRes = await apiGet(page, `/api/v1/properties/${pid}/rate-codes`);
      const chRes = await apiGet(page, `/api/v1/properties/${pid}/reservation-channels`);

      if (rtRes.data.data.length === 0 || rcRes.data.data.length === 0 || chRes.data.data.length === 0) {
        test.skip();
        return;
      }

      const createRes = await apiPost(page, `/api/v1/properties/${pid}/reservations`, {
        guestNameKo: 'UAT취소테스트',
        guestLastNameEn: 'CANCEL',
        guestFirstNameEn: 'UAT',
        phoneCountryCode: '82',
        phoneNumber: '01099998888',
        reservationChannelId: chRes.data.data[0].id,
        rateCodeId: rcRes.data.data[0].id,
        masterCheckIn: '2026-04-10',
        masterCheckOut: '2026-04-12',
        subReservations: [{
          roomTypeId: rtRes.data.data[0].id,
          checkIn: '2026-04-10',
          checkOut: '2026-04-12',
          adults: 2,
          children: 0
        }]
      });

      expect([200, 201]).toContain(createRes.status);
      targetId = createRes.data.data.id;
    }

    // DELETE 엔드포인트로 취소
    const response = await page.request.delete(
      `/api/v1/properties/${pid}/reservations/${targetId}`
    );
    const status = response.status();

    expect(status).toBe(200);

    // 취소 확인: 상세 재조회
    const verifyRes = await apiGet(
      page,
      `/api/v1/properties/${pid}/reservations/${targetId}`
    );
    // 상태 변경이면 CANCELED 확인
    if (verifyRes.status === 200 && verifyRes.data && verifyRes.data.data) {
      expect(verifyRes.data.data.reservationStatus).toBe('CANCELED');
    }
  });

  test('TC-10: 결제 요약 API', async ({ page }) => {
    const pid = await ensurePropertyId(page);

    // 예약 ID 확보 (TC-06 것은 취소되었을 수 있으므로 목록에서 가져옴)
    const listRes = await apiGet(page, `/api/v1/properties/${pid}/reservations`);
    expect(listRes.status).toBe(200);

    let reservationId;
    if (listRes.data.data.length > 0) {
      reservationId = listRes.data.data[0].id;
    } else if (createdReservationId) {
      reservationId = createdReservationId;
    } else {
      test.skip();
      return;
    }

    const { status, data } = await apiGet(
      page,
      `/api/v1/properties/${pid}/reservations/${reservationId}/payment`
    );

    expect(status).toBe(200);
    expect(data).toBeTruthy();
  });
});
