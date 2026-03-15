// 공통 헬퍼 함수
const { expect } = require('@playwright/test');

/**
 * 호텔/프로퍼티 컨텍스트 선택
 * 사이드바 상단 셀렉터에서 첫 번째 호텔/프로퍼티 선택
 */
async function selectContext(page) {
  // 호텔 셀렉터 확인 후 첫 번째 옵션 선택
  const hotelSelector = page.locator('#hotelSelector, #contextHotelSelect, select[id*="hotel"]').first();
  if (await hotelSelector.isVisible({ timeout: 3000 }).catch(() => false)) {
    const options = await hotelSelector.locator('option').all();
    if (options.length > 1) {
      // 첫 번째 비-빈 옵션 선택
      await hotelSelector.selectOption({ index: 1 });
      await page.waitForTimeout(1000);
    }
  }

  // 프로퍼티 셀렉터 확인 후 첫 번째 옵션 선택
  const propertySelector = page.locator('#propertySelector, #contextPropertySelect, select[id*="property"]').first();
  if (await propertySelector.isVisible({ timeout: 3000 }).catch(() => false)) {
    const options = await propertySelector.locator('option').all();
    if (options.length > 1) {
      await propertySelector.selectOption({ index: 1 });
      await page.waitForTimeout(1000);
    }
  }
}

/**
 * 페이지 로드 후 기본 검증
 */
async function verifyPageLoad(page, url, titleText) {
  await page.goto(url);
  await page.waitForLoadState('networkidle');

  // HTTP 에러 없는지 확인
  const status = await page.evaluate(() => document.readyState);
  expect(status).toBe('complete');

  // 에러 페이지가 아닌지 확인
  const bodyText = await page.textContent('body');
  expect(bodyText).not.toContain('Whitelabel Error');
  expect(bodyText).not.toContain('500');
  expect(bodyText).not.toContain('404');

  // 타이틀 텍스트가 있으면 확인
  if (titleText) {
    await expect(page.locator('body')).toContainText(titleText);
  }
}

/**
 * DataTable이 로드되었는지 확인
 */
async function verifyDataTableLoaded(page, tableSelector = '.dataTable, table.table') {
  const table = page.locator(tableSelector).first();
  await expect(table).toBeVisible({ timeout: 10000 });
  return table;
}

/**
 * 콘솔 에러 수집
 */
function collectConsoleErrors(page) {
  const errors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });
  return errors;
}

/**
 * API 응답 검증
 */
async function verifyApiResponse(page, urlPattern) {
  const response = await page.waitForResponse(
    resp => resp.url().includes(urlPattern),
    { timeout: 10000 }
  );
  expect(response.status()).toBeLessThan(400);
  return response;
}

/**
 * 폼 필드 입력
 */
async function fillFormField(page, selector, value) {
  const field = page.locator(selector);
  await field.click();
  await field.fill(value);
}

/**
 * Toast/Alert 메시지 확인
 */
async function verifyToast(page, type = 'success') {
  const toast = page.locator(`.toast, .alert-${type}, .toast-${type}`).first();
  await expect(toast).toBeVisible({ timeout: 5000 });
}

module.exports = {
  selectContext,
  verifyPageLoad,
  verifyDataTableLoaded,
  collectConsoleErrors,
  verifyApiResponse,
  fillFormField,
  verifyToast,
};
