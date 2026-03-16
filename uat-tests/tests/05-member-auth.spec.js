/**
 * 05-member-auth.spec.js
 * 회원관리 + 권한관리 모듈 UAT 테스트 (API + 페이지 접근)
 */
const { test, expect } = require('@playwright/test');
const { login, selectHotelAndProperty, apiGet } = require('./helpers');

let hotelId;
let propertyId;

test.describe('회원관리 + 권한관리 모듈', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  // ── API 테스트 ──

  test('TC-01: 블루웨이브 관리자 목록 API', async ({ page }) => {
    const { status, data } = await apiGet(page, '/api/v1/bluewave-admins');

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-02: 호텔 관리자 목록 API', async ({ page }) => {
    // 호텔 ID 확보
    const selectorRes = await apiGet(page, '/api/v1/hotels/selector');
    expect(selectorRes.status).toBe(200);
    expect(selectorRes.data.data.length).toBeGreaterThanOrEqual(1);
    hotelId = selectorRes.data.data[0].id;

    const { status, data } = await apiGet(page, `/api/v1/hotels/${hotelId}/admins`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-03: 프로퍼티 관리자 목록 API', async ({ page }) => {
    // 호텔 ID 확보 후 프로퍼티 ID 확보 (hotelId 필수)
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    expect(hotelRes.status).toBe(200);
    const hId = hotelRes.data.data[0].id;

    const selectorRes = await apiGet(page, `/api/v1/properties/selector?hotelId=${hId}`);
    expect(selectorRes.status).toBe(200);
    expect(selectorRes.data.data.length).toBeGreaterThanOrEqual(1);
    propertyId = selectorRes.data.data[0].id;

    const { status, data } = await apiGet(page, `/api/v1/properties/${propertyId}/admins`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-04: 호텔 관리자 권한(역할) 목록 API', async ({ page }) => {
    // hotelId 필수 파라미터
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    const hId = hotelRes.data.data[0].id;

    const { status, data } = await apiGet(page, `/api/v1/hotel-admin-roles/selector?hotelId=${hId}`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('TC-05: 프로퍼티 관리자 권한 목록 API', async ({ page }) => {
    // hotelId 필수 파라미터
    const hotelRes = await apiGet(page, '/api/v1/hotels/selector');
    const hId = hotelRes.data.data[0].id;

    const { status, data } = await apiGet(page, `/api/v1/property-admin-roles/selector?hotelId=${hId}`);

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(Array.isArray(data.data)).toBe(true);
  });

  // ── 페이지 접근 테스트 ──

  test('TC-06: 블루웨이브 관리자 관리 페이지', async ({ page }) => {
    await page.goto('/admin/members/bluewave-admins');
    await page.waitForLoadState('networkidle');

    // DataTable 렌더링 확인
    const table = page.locator('table');
    await expect(table.first()).toBeVisible({ timeout: 10000 });

    // DataTable 데이터 행 존재 확인
    const rows = page.locator('table tbody tr');
    await expect(rows.first()).toBeVisible({ timeout: 10000 });
  });

  test('TC-07: 호텔 관리자 관리 페이지', async ({ page }) => {
    await page.goto('/admin/members/hotel-admins');
    await page.waitForLoadState('networkidle');

    // 호텔/프로퍼티 선택
    await selectHotelAndProperty(page);

    // DataTable 렌더링 확인
    const table = page.locator('table');
    await expect(table.first()).toBeVisible({ timeout: 10000 });

    // DataTable 데이터 행 또는 빈 메시지 확인
    const rows = page.locator('table tbody tr');
    await expect(rows.first()).toBeVisible({ timeout: 10000 });
  });

  test('TC-08: 프로퍼티 관리자 관리 페이지', async ({ page }) => {
    await page.goto('/admin/members/property-admins');
    await page.waitForLoadState('networkidle');

    // 호텔/프로퍼티 선택
    await selectHotelAndProperty(page);

    // DataTable 렌더링 확인
    const table = page.locator('table');
    await expect(table.first()).toBeVisible({ timeout: 10000 });

    // DataTable 데이터 행 또는 빈 메시지 확인
    const rows = page.locator('table tbody tr');
    await expect(rows.first()).toBeVisible({ timeout: 10000 });
  });

  test('TC-09: 호텔 권한관리 페이지', async ({ page }) => {
    await page.goto('/admin/roles/hotel-admins');
    await page.waitForLoadState('networkidle');

    // 역할 트리 또는 역할 목록 표시 확인
    // 트리 컨테이너 또는 테이블 중 하나가 보이면 OK
    const treeOrTable = page.locator('.role-tree, .tree-container, #roleTree, table, .card').first();
    await expect(treeOrTable).toBeVisible({ timeout: 10000 });
  });

  test('TC-10: 내 프로필 API', async ({ page }) => {
    const { status, data } = await apiGet(page, '/api/v1/my-profile');

    expect(status).toBe(200);
    expect(data).toBeTruthy();
    expect(data.data).toBeTruthy();
    expect(data.data.loginId).toBeTruthy();
    expect(data.data.userName).toBeTruthy();
  });
});
