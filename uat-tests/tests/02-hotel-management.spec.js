/**
 * 02-hotel-management.spec.js
 * 호텔관리 모듈 UAT 테스트 (API + 페이지 접근)
 */
const { test, expect } = require('@playwright/test');
const { login, selectHotelAndProperty, apiGet } = require('./helpers');

test.describe('호텔관리 모듈', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('TC-01: 호텔 목록 API 조회', async ({ page }) => {
    const { status, data } = await apiGet(page, '/api/v1/hotels');

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-02: 호텔 셀렉터 API', async ({ page }) => {
    const { status, data } = await apiGet(page, '/api/v1/hotels/selector');

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
    expect(data.data.length).toBeGreaterThanOrEqual(1);
  });

  test('TC-03: 프로퍼티 목록 API', async ({ page }) => {
    // 호텔 셀렉터에서 hotelId 확보
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    expect(hotelRes.status).toBe(200);
    expect(hotelRes.data.data.length).toBeGreaterThanOrEqual(1);

    const hotelId = hotelRes.data.data[0].id;

    // hotelId를 전달하여 프로퍼티 셀렉터 호출
    const { status, data } = await apiGet(page, `/api/v1/properties/selector?hotelId=${hotelId}`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
    expect(data.data.length).toBeGreaterThanOrEqual(1);
  });

  test('TC-04: 호텔관리 페이지 접근', async ({ page }) => {
    await page.goto('/admin/hotels');
    await page.waitForLoadState('networkidle');

    // DataTable 존재 확인
    const table = page.locator('table');
    await expect(table.first()).toBeVisible({ timeout: 10000 });

    // DataTable 데이터 로드 확인 (tbody에 행이 존재)
    const rows = page.locator('table tbody tr');
    await expect(rows.first()).toBeVisible({ timeout: 10000 });
  });

  test('TC-05: 프로퍼티 관리 페이지 접근', async ({ page }) => {
    await page.goto('/admin/properties');
    await page.waitForLoadState('networkidle');

    // 호텔/프로퍼티 선택
    await selectHotelAndProperty(page);

    // DataTable 존재 확인
    const table = page.locator('table');
    await expect(table.first()).toBeVisible({ timeout: 10000 });

    // DataTable 데이터 로드 대기 (빈 행 메시지 또는 데이터 행)
    const dataRow = page.locator('table tbody tr');
    await expect(dataRow.first()).toBeVisible({ timeout: 10000 });
  });

  test('TC-06: 층 관리 API', async ({ page }) => {
    // 호텔 ID → 프로퍼티 ID 확보
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    expect(hotelRes.status).toBe(200);
    const hotelId = hotelRes.data.data[0].id;

    const propRes = await apiGet(page, `/api/v1/properties/selector?hotelId=${hotelId}`);
    expect(propRes.status).toBe(200);
    expect(propRes.data.data.length).toBeGreaterThanOrEqual(1);

    const propertyId = propRes.data.data[0].id;

    // 층 목록 조회
    const { status, data } = await apiGet(page, `/api/v1/properties/${propertyId}/floors`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-07: 호수 관리 API', async ({ page }) => {
    // 호텔 ID → 프로퍼티 ID 확보
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    const hotelId = hotelRes.data.data[0].id;

    const propRes = await apiGet(page, `/api/v1/properties/selector?hotelId=${hotelId}`);
    const propertyId = propRes.data.data[0].id;

    // 호수(객실번호) 목록 조회
    const { status, data } = await apiGet(
      page,
      `/api/v1/properties/${propertyId}/room-numbers`
    );

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-08: 마켓코드 API', async ({ page }) => {
    // 호텔 ID → 프로퍼티 ID 확보
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    const hotelId = hotelRes.data.data[0].id;

    const propRes = await apiGet(page, `/api/v1/properties/selector?hotelId=${hotelId}`);
    const propertyId = propRes.data.data[0].id;

    // 마켓코드 조회 (propertyId 기반 경로)
    const { status, data } = await apiGet(page, `/api/v1/properties/${propertyId}/market-codes`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
  });
});
