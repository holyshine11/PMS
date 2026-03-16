/**
 * UAT 공통 헬퍼
 */

async function login(page, username = 'admin', password = 'holapms1!') {
  await page.goto('/login');
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin/dashboard', { timeout: 10000 });
}

async function selectHotelAndProperty(page) {
  // 호텔 셀렉터에서 첫 번째 호텔 선택
  const hotelSelect = page.locator('#hotelSelector, #hotelSelect, select[name="hotelId"]').first();
  if (await hotelSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
    const options = await hotelSelect.locator('option').all();
    if (options.length > 1) {
      await hotelSelect.selectOption({ index: 1 });
      await page.waitForTimeout(500);
    }
  }

  // 프로퍼티 셀렉터에서 첫 번째 프로퍼티 선택
  const propertySelect = page.locator('#propertySelector, #propertySelect, select[name="propertyId"]').first();
  if (await propertySelect.isVisible({ timeout: 3000 }).catch(() => false)) {
    await page.waitForTimeout(500);
    const options = await propertySelect.locator('option').all();
    if (options.length > 1) {
      await propertySelect.selectOption({ index: 1 });
      await page.waitForTimeout(500);
    }
  }
}

async function apiGet(page, url) {
  const response = await page.request.get(url);
  return { status: response.status(), data: await response.json().catch(() => null) };
}

async function apiPost(page, url, data) {
  const response = await page.request.post(url, { data });
  return { status: response.status(), data: await response.json().catch(() => null) };
}

module.exports = { login, selectHotelAndProperty, apiGet, apiPost };
