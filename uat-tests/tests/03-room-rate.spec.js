/**
 * 03-room-rate.spec.js
 * 객실관리(M02) + 레이트관리(M03) + 트랜잭션/재고(신규) UAT 테스트
 */
const { test, expect } = require('@playwright/test');
const { login, selectHotelAndProperty, apiGet } = require('./helpers');

let propertyId;

test.describe('객실 & 레이트 관리 API/UI 테스트', () => {

  test.beforeAll(async ({ browser }) => {
    // 로그인 후 호텔 → 프로퍼티 순서로 ID 확보
    const page = await browser.newPage();
    await login(page);

    // 1) 호텔 목록 조회
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    expect(hotelRes.status).toBe(200);
    const hotels = hotelRes.data.data || hotelRes.data;
    expect(Array.isArray(hotels)).toBeTruthy();
    expect(hotels.length).toBeGreaterThan(0);
    const hotelId = hotels[0].id || hotels[0].hotelId;

    // 2) 호텔 ID로 프로퍼티 목록 조회
    const propRes = await apiGet(page, `/api/v1/properties/selector?hotelId=${hotelId}`);
    expect(propRes.status).toBe(200);
    const properties = propRes.data.data || propRes.data;
    expect(Array.isArray(properties)).toBeTruthy();
    expect(properties.length).toBeGreaterThan(0);
    propertyId = properties[0].id || properties[0].propertyId;
    expect(propertyId).toBeTruthy();

    await page.close();
  });

  // ===== TC-01: 객실 클래스 목록 API =====
  test('TC-01: 객실 클래스 목록 API', async ({ page }) => {
    await login(page);
    const res = await apiGet(page, `/api/v1/properties/${propertyId}/room-classes`);
    expect(res.status).toBe(200);
    expect(res.data.success).toBe(true);
    expect(Array.isArray(res.data.data)).toBeTruthy();
  });

  // ===== TC-02: 객실 타입 목록 API =====
  test('TC-02: 객실 타입 목록 API - roomTypeCode, description 필드 확인', async ({ page }) => {
    await login(page);
    const res = await apiGet(page, `/api/v1/properties/${propertyId}/room-types`);
    expect(res.status).toBe(200);
    expect(res.data.success).toBe(true);
    expect(Array.isArray(res.data.data)).toBeTruthy();

    // 데이터가 있으면 필수 필드 확인
    if (res.data.data.length > 0) {
      const item = res.data.data[0];
      expect(item).toHaveProperty('roomTypeCode');
      expect(item).toHaveProperty('description');
    }
  });

  // ===== TC-03: 무료 서비스 옵션 API =====
  test('TC-03: 무료 서비스 옵션 API', async ({ page }) => {
    await login(page);
    const res = await apiGet(page, `/api/v1/properties/${propertyId}/free-service-options`);
    expect(res.status).toBe(200);
    expect(res.data.success).toBe(true);
  });

  // ===== TC-04: 유료 서비스 옵션 API =====
  test('TC-04: 유료 서비스 옵션 API - transactionCodeId 필드 확인', async ({ page }) => {
    await login(page);
    const res = await apiGet(page, `/api/v1/properties/${propertyId}/paid-service-options`);
    expect(res.status).toBe(200);
    expect(res.data.success).toBe(true);
    expect(Array.isArray(res.data.data)).toBeTruthy();

    // 데이터가 있으면 Phase 2 확장 필드(transactionCodeId) 포함 여부 확인
    if (res.data.data.length > 0) {
      const item = res.data.data[0];
      const keys = Object.keys(item);
      expect(keys).toContain('transactionCodeId');
    }
  });

  // ===== TC-05: 레이트 코드 목록 API =====
  test('TC-05: 레이트 코드 목록 API - rateCode, rateNameKo, currency 필드 확인', async ({ page }) => {
    await login(page);
    const res = await apiGet(page, `/api/v1/properties/${propertyId}/rate-codes`);
    expect(res.status).toBe(200);
    expect(res.data.success).toBe(true);
    expect(Array.isArray(res.data.data)).toBeTruthy();

    if (res.data.data.length > 0) {
      const item = res.data.data[0];
      expect(item).toHaveProperty('rateCode');
      expect(item).toHaveProperty('rateNameKo');
      expect(item).toHaveProperty('currency');
    }
  });

  // ===== TC-06: 프로모션 코드 목록 API =====
  test('TC-06: 프로모션 코드 목록 API', async ({ page }) => {
    await login(page);
    const res = await apiGet(page, `/api/v1/properties/${propertyId}/promotion-codes`);
    expect(res.status).toBe(200);
    expect(res.data.success).toBe(true);
  });

  // ===== TC-07: 객실관리 페이지 접근 =====
  test('TC-07: 객실관리 페이지 접근 - DataTable 렌더링', async ({ page }) => {
    await login(page);
    await page.goto('/admin/room-types');
    await page.waitForLoadState('networkidle');

    // 프로퍼티 선택
    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    // DataTable 요소가 렌더링되었는지 확인
    const table = page.locator('table');
    await expect(table.first()).toBeVisible({ timeout: 10000 });
  });

  // ===== TC-08: 레이트관리 페이지 접근 =====
  test('TC-08: 레이트관리 페이지 접근 - DataTable 렌더링', async ({ page }) => {
    await login(page);
    await page.goto('/admin/rate-codes');
    await page.waitForLoadState('networkidle');

    // 프로퍼티 선택
    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    // DataTable 요소가 렌더링되었는지 확인
    const table = page.locator('table');
    await expect(table.first()).toBeVisible({ timeout: 10000 });
  });

  // ===== TC-09: 트랜잭션 코드 API (신규) =====
  test('TC-09: 트랜잭션 코드 그룹 및 코드 목록 API', async ({ page }) => {
    await login(page);

    // 트랜잭션 코드 그룹 조회
    const groupRes = await apiGet(page, `/api/v1/properties/${propertyId}/transaction-code-groups`);
    expect(groupRes.status).toBe(200);
    expect(groupRes.data.success).toBe(true);
    expect(Array.isArray(groupRes.data.data)).toBeTruthy();

    // 트랜잭션 코드 목록 조회
    const codeRes = await apiGet(page, `/api/v1/properties/${propertyId}/transaction-codes`);
    expect(codeRes.status).toBe(200);
    expect(codeRes.data.success).toBe(true);
    expect(Array.isArray(codeRes.data.data)).toBeTruthy();
  });

  // ===== TC-10: 재고 아이템 API (신규) =====
  test('TC-10: 재고 아이템 목록 API', async ({ page }) => {
    await login(page);
    const res = await apiGet(page, `/api/v1/properties/${propertyId}/inventory-items`);
    expect(res.status).toBe(200);
    expect(res.data.success).toBe(true);
    expect(Array.isArray(res.data.data)).toBeTruthy();
  });
});
