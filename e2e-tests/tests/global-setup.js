// 전역 로그인 설정 - 인증 상태를 auth.json에 저장
const { test, expect } = require('@playwright/test');

test('login and save auth state', async ({ page }) => {
  await page.goto('/login');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'holapms1!');
  await page.click('button[type="submit"]');

  // 로그인 성공 후 대시보드 또는 메인 페이지로 이동 확인
  await page.waitForURL('**/admin/dashboard', { timeout: 15000 });
  await expect(page).not.toHaveURL(/\/login/);

  // 인증 상태 저장
  await page.context().storageState({ path: 'auth.json' });
});
