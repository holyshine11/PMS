const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
    const page = await context.newPage();

    // 로그인
    console.log('=== 1. 로그인 ===');
    await page.goto('http://localhost:8080/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'holapms1!');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin/dashboard');
    console.log('로그인 성공');

    // 초기 상태 확인 (펼침)
    console.log('\n=== 2. 초기 상태 (펼침) 확인 ===');
    const sidebar = page.locator('nav.sidebar.hola-sidebar');
    const mainContent = page.locator('main.main-content');
    const toggleBtn = page.locator('#sidebarToggle');

    const initialSidebarWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width);
    const initialMainMargin = await mainContent.evaluate(el => getComputedStyle(el).marginLeft);
    const bodyHasCollapsed = await page.evaluate(() => document.body.classList.contains('sidebar-collapsed'));

    console.log(`  사이드바 너비: ${initialSidebarWidth}px (기대: 250px)`);
    console.log(`  메인 콘텐츠 margin-left: ${initialMainMargin} (기대: 250px)`);
    console.log(`  sidebar-collapsed 클래스: ${bodyHasCollapsed} (기대: false)`);

    if (Math.abs(initialSidebarWidth - 250) > 5) {
        console.log('  ❌ 사이드바 초기 너비가 250px이 아닙니다!');
    } else {
        console.log('  ✅ 사이드바 초기 너비 정상');
    }

    // 메뉴 텍스트 표시 확인
    const menuTextVisible = await sidebar.locator('a.nav-link:has-text("대시보드")').isVisible();
    console.log(`  메뉴 텍스트 표시: ${menuTextVisible} (기대: true)`);

    // 사이드바 스크린샷 (펼침)
    await page.screenshot({ path: '/Users/Dev/PMS/uat-tests/sidebar-expanded.png', fullPage: false });
    console.log('  스크린샷 저장: sidebar-expanded.png');

    // 접힘 버튼 클릭
    console.log('\n=== 3. 접힘 버튼 클릭 후 상태 확인 ===');
    await toggleBtn.click();
    // transition 완료 대기
    await page.waitForTimeout(400);

    const collapsedSidebarWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width);
    const collapsedMainMargin = await mainContent.evaluate(el => getComputedStyle(el).marginLeft);
    const bodyHasCollapsedAfter = await page.evaluate(() => document.body.classList.contains('sidebar-collapsed'));

    console.log(`  사이드바 너비: ${collapsedSidebarWidth}px (기대: 60px)`);
    console.log(`  메인 콘텐츠 margin-left: ${collapsedMainMargin} (기대: 60px)`);
    console.log(`  sidebar-collapsed 클래스: ${bodyHasCollapsedAfter} (기대: true)`);

    if (Math.abs(collapsedSidebarWidth - 60) > 5) {
        console.log('  ❌ 사이드바 접힘 너비가 60px이 아닙니다!');
    } else {
        console.log('  ✅ 사이드바 접힘 너비 정상');
    }

    // 접힌 상태에서 아이콘만 표시되는지 확인
    const iconVisible = await sidebar.locator('.nav-link > i.fas.fa-tachometer-alt').isVisible();
    console.log(`  대시보드 아이콘 표시: ${iconVisible} (기대: true)`);

    // 접힌 상태 스크린샷
    await page.screenshot({ path: '/Users/Dev/PMS/uat-tests/sidebar-collapsed.png', fullPage: false });
    console.log('  스크린샷 저장: sidebar-collapsed.png');

    // 접힌 상태에서 콘텐츠 겹침 확인
    console.log('\n=== 4. 접힌 상태 콘텐츠 겹침 확인 ===');
    const sidebarRight = await sidebar.evaluate(el => el.getBoundingClientRect().right);
    const mainLeft = await mainContent.evaluate(el => el.getBoundingClientRect().left);
    console.log(`  사이드바 오른쪽: ${sidebarRight}px`);
    console.log(`  메인 콘텐츠 왼쪽: ${mainLeft}px`);

    if (mainLeft < sidebarRight) {
        console.log('  ❌ 콘텐츠가 사이드바와 겹칩니다!');
    } else {
        console.log('  ✅ 콘텐츠 겹침 없음');
    }

    // 호버 시 확장 확인
    console.log('\n=== 5. 접힌 상태에서 호버 시 확장 확인 ===');
    await sidebar.hover();
    await page.waitForTimeout(400);
    const hoverWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width);
    console.log(`  호버 시 사이드바 너비: ${hoverWidth}px (기대: 250px)`);

    if (Math.abs(hoverWidth - 250) > 5) {
        console.log('  ❌ 호버 시 사이드바가 250px로 확장되지 않습니다!');
    } else {
        console.log('  ✅ 호버 시 확장 정상');
    }

    // 호버 시 메뉴 텍스트 표시 확인
    const hoverMenuVisible = await sidebar.locator('a.nav-link:has-text("대시보드")').isVisible();
    console.log(`  호버 시 메뉴 텍스트 표시: ${hoverMenuVisible} (기대: true)`);

    // 호버 시 메인 콘텐츠 위치 (접힌 상태 유지)
    const hoverMainMargin = await mainContent.evaluate(el => getComputedStyle(el).marginLeft);
    console.log(`  호버 시 메인 콘텐츠 margin-left: ${hoverMainMargin} (기대: 60px - 오버레이)`);

    // 호버 스크린샷
    await page.screenshot({ path: '/Users/Dev/PMS/uat-tests/sidebar-hover.png', fullPage: false });
    console.log('  스크린샷 저장: sidebar-hover.png');

    // 마우스 밖으로 이동 (호버 해제)
    await page.mouse.move(800, 400);
    await page.waitForTimeout(400);

    // 다시 펼침 버튼 클릭
    console.log('\n=== 6. 다시 펼침 버튼 클릭 후 상태 확인 ===');
    await toggleBtn.click();
    await page.waitForTimeout(400);

    const expandedSidebarWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width);
    const expandedMainMargin = await mainContent.evaluate(el => getComputedStyle(el).marginLeft);
    const bodyCollapsedFinal = await page.evaluate(() => document.body.classList.contains('sidebar-collapsed'));

    console.log(`  사이드바 너비: ${expandedSidebarWidth}px (기대: 250px)`);
    console.log(`  메인 콘텐츠 margin-left: ${expandedMainMargin} (기대: 250px)`);
    console.log(`  sidebar-collapsed 클래스: ${bodyCollapsedFinal} (기대: false)`);

    if (Math.abs(expandedSidebarWidth - 250) > 5) {
        console.log('  ❌ 사이드바가 다시 250px로 펼쳐지지 않습니다!');
    } else {
        console.log('  ✅ 사이드바 다시 펼침 정상');
    }

    // 펼친 상태에서 겹침 확인
    const expandedSidebarRight = await sidebar.evaluate(el => el.getBoundingClientRect().right);
    const expandedMainLeft = await mainContent.evaluate(el => el.getBoundingClientRect().left);
    console.log(`  사이드바 오른쪽: ${expandedSidebarRight}px`);
    console.log(`  메인 콘텐츠 왼쪽: ${expandedMainLeft}px`);

    if (expandedMainLeft < expandedSidebarRight) {
        console.log('  ❌ 콘텐츠가 사이드바와 겹칩니다!');
    } else {
        console.log('  ✅ 콘텐츠 겹침 없음');
    }

    // 최종 스크린샷
    await page.screenshot({ path: '/Users/Dev/PMS/uat-tests/sidebar-re-expanded.png', fullPage: false });
    console.log('  스크린샷 저장: sidebar-re-expanded.png');

    // localStorage 상태 저장 확인
    console.log('\n=== 7. localStorage 상태 저장 확인 ===');
    const storedValue = await page.evaluate(() => localStorage.getItem('holaSidebarCollapsed'));
    console.log(`  holaSidebarCollapsed: ${storedValue} (기대: false - 펼친 상태)`);

    // 접힌 상태로 다시 전환 후 새로고침 시 상태 유지 확인
    console.log('\n=== 8. 페이지 새로고침 후 상태 유지 확인 ===');
    await toggleBtn.click();
    await page.waitForTimeout(400);
    const beforeReload = await page.evaluate(() => localStorage.getItem('holaSidebarCollapsed'));
    console.log(`  접힘 후 localStorage: ${beforeReload}`);

    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);

    const afterReloadCollapsed = await page.evaluate(() => document.body.classList.contains('sidebar-collapsed'));
    const afterReloadWidth = await sidebar.evaluate(el => el.getBoundingClientRect().width);
    console.log(`  새로고침 후 sidebar-collapsed: ${afterReloadCollapsed} (기대: true)`);
    console.log(`  새로고침 후 사이드바 너비: ${afterReloadWidth}px (기대: 60px)`);

    if (afterReloadCollapsed && Math.abs(afterReloadWidth - 60) <= 5) {
        console.log('  ✅ 새로고침 후 접힌 상태 유지 정상');
    } else {
        console.log('  ❌ 새로고침 후 접힌 상태가 유지되지 않습니다!');
    }

    // 정리: 다시 펼침으로 복원
    await toggleBtn.click();
    await page.waitForTimeout(300);

    console.log('\n=== 테스트 완료 ===');
    await browser.close();
})().catch(err => {
    console.error('테스트 실패:', err.message);
    process.exit(1);
});
