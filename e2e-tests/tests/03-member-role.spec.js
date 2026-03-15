// Team 3: 회원관리 & 권한관리 E2E 테스트
const { test, expect } = require('@playwright/test');
const { verifyPageLoad, verifyDataTableLoaded, collectConsoleErrors } = require('./helpers');

// ============================================================
// 1. 블루웨이브 관리자 (Bluewave Admin)
// ============================================================
test.describe('블루웨이브 관리자', () => {

  test('리스트 페이지 로드 및 DataTable 표시', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/members/bluewave-admins', '블루웨이브 관리자 관리');

    // DataTable 렌더링 확인
    await verifyDataTableLoaded(page, '#bluewaveAdminTable');

    // 테이블 헤더 5컬럼 확인
    const headers = page.locator('#bluewaveAdminTable thead th');
    await expect(headers).toHaveCount(5);

    // 검색 영역 존재 확인
    await expect(page.locator('#searchLoginId')).toBeVisible();
    await expect(page.locator('#searchUserName')).toBeVisible();

    // 페이지 크기 선택기 확인
    await expect(page.locator('#pageSizeSelect')).toBeVisible();
    const pageSizeOptions = await page.locator('#pageSizeSelect option').allTextContents();
    expect(pageSizeOptions).toContain('10개씩 보기');
    expect(pageSizeOptions).toContain('20개씩 보기');

    // 등록 버튼 확인
    await expect(page.locator('a[href="/admin/members/bluewave-admins/new"]')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('등록 폼 - 필수 필드 및 UI 요소 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/members/bluewave-admins/new', '블루웨이브 관리자 등록');

    // 폼 존재 확인
    await expect(page.locator('#bluewaveAdminForm')).toBeVisible();

    // 필수 필드 라벨(required 클래스) 존재 확인
    const requiredLabels = page.locator('label.required');
    await expect(requiredLabels).not.toHaveCount(0);

    // 주요 입력 필드 확인
    await expect(page.locator('#loginId')).toBeVisible();
    await expect(page.locator('#userName')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#phone')).toBeVisible();
    await expect(page.locator('#roleName')).toBeVisible();

    // 아이디 중복확인 버튼
    await expect(page.locator('#btnCheckLoginId')).toBeVisible();

    // 계정상태 라디오 버튼 (Y/N)
    await expect(page.locator('input[name="useYn"][value="true"]')).toBeAttached();
    await expect(page.locator('input[name="useYn"][value="false"]')).toBeAttached();

    // 비밀번호 초기화 영역 - 등록 모드에서는 숨김 상태여야 함
    const resetPwdArea = page.locator('#resetPwdArea');
    await expect(resetPwdArea).not.toBeVisible();

    // 삭제 버튼 - 등록 모드에서는 숨김
    await expect(page.locator('#btnDelete')).not.toBeVisible();

    // 취소/저장 버튼 확인 (사이드바 nav-link와 구분하기 위해 .btn 클래스 지정)
    await expect(page.locator('a.btn[href="/admin/members/bluewave-admins"]')).toBeVisible();
    await expect(page.locator('button:has-text("등록")')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('수정 폼 - 첫 번째 항목 데이터 로드 확인', async ({ page }) => {
    // API로 블루웨이브 관리자 목록 조회
    const response = await page.request.get('/api/v1/bluewave-admins');
    const json = await response.json();

    if (!json.data || json.data.length === 0) {
      test.skip('블루웨이브 관리자 데이터 없음 - 테스트 스킵');
      return;
    }

    const firstAdmin = json.data[0];
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(
      page,
      `/admin/members/bluewave-admins/${firstAdmin.id}/edit`,
      '블루웨이브 관리자'
    );

    // JS 비동기 데이터 로드 대기
    await page.waitForTimeout(2000);

    // 아이디 필드에 값이 채워졌는지 확인
    const loginIdValue = await page.locator('#loginId').inputValue();
    expect(loginIdValue.length).toBeGreaterThan(0);

    // 비밀번호 초기화 버튼 표시 확인 (수정 모드)
    await expect(page.locator('#resetPwdArea')).toBeVisible();

    // 삭제 버튼 표시 확인 (수정 모드)
    await expect(page.locator('#btnDelete')).toBeVisible();

    // 수정 모드 저장 버튼 텍스트 확인 ("저장" 으로 표시 - JS에서 수정 모드도 "저장" 사용)
    const btnSaveText = await page.locator('#btnSaveText').textContent();
    expect(btnSaveText).toContain('저장');

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('비밀번호 초기화 버튼 - 수정 폼에서 존재 확인', async ({ page }) => {
    const response = await page.request.get('/api/v1/bluewave-admins');
    const json = await response.json();

    if (!json.data || json.data.length === 0) {
      test.skip('블루웨이브 관리자 데이터 없음 - 테스트 스킵');
      return;
    }

    const firstAdmin = json.data[0];
    await page.goto(`/admin/members/bluewave-admins/${firstAdmin.id}/edit`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // 비밀번호 초기화 버튼 존재 및 표시 확인
    const resetBtn = page.locator('button:has-text("비밀번호 초기화")');
    await expect(resetBtn).toBeVisible();
  });

});

// ============================================================
// 2. 호텔 관리자 (Hotel Admin)
// ============================================================
test.describe('호텔 관리자', () => {

  test('리스트 페이지 로드', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/members/hotel-admins', '호텔 관리자 관리');

    // DataTable 렌더링 확인
    await verifyDataTableLoaded(page, '#hotelAdminTable');

    // 테이블 헤더 7컬럼 확인
    const headers = page.locator('#hotelAdminTable thead th');
    await expect(headers).toHaveCount(7);

    // 호텔명 검색 필드 확인
    await expect(page.locator('#searchHotelName')).toBeVisible();
    await expect(page.locator('#searchLoginId')).toBeVisible();
    await expect(page.locator('#searchUserName')).toBeVisible();

    // contextAlert 존재 확인 (호텔 미선택 시 안내)
    await expect(page.locator('#contextAlert')).toBeAttached();

    // 등록 버튼 확인
    await expect(page.locator('a[href="/admin/members/hotel-admins/new"]')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('등록 폼 - 호텔 선택 연동 및 UI 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/members/hotel-admins/new', '호텔 관리자 등록');

    // 폼 존재 확인
    await expect(page.locator('#hotelAdminForm')).toBeVisible();

    // 프로퍼티 선택 영역 확인 (호텔 연동)
    await expect(page.locator('#propertyCheckboxArea')).toBeVisible();
    await expect(page.locator('#propertyAll')).toBeAttached(); // 전체 체크박스
    await expect(page.locator('#propertyList')).toBeVisible();

    // 초기 상태: 호텔 선택 전 안내 문구
    const propertyListText = await page.locator('#propertyList').textContent();
    expect(propertyListText).toContain('호텔을 선택하면 프로퍼티가 표시됩니다.');

    // 필수 입력 필드 확인
    await expect(page.locator('#loginId')).toBeVisible();
    await expect(page.locator('#userName')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#phone')).toBeVisible();

    // 호텔관리자 권한 셀렉트박스 확인
    await expect(page.locator('#roleId')).toBeVisible();

    // 비밀번호 초기화 영역 - 등록 모드에서 숨김
    await expect(page.locator('#resetPwdArea')).not.toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('수정 폼 - 데이터 로드 확인', async ({ page }) => {
    // 먼저 호텔 셀렉터 API로 첫 번째 호텔 ID 획득
    const hotelRes = await page.request.get('/api/v1/hotels/selector');
    const hotelJson = await hotelRes.json();

    if (!hotelJson.data || hotelJson.data.length === 0) {
      test.skip('호텔 데이터 없음 - 테스트 스킵');
      return;
    }

    const hotelId = hotelJson.data[0].id;

    // 해당 호텔의 관리자 목록 조회
    const adminRes = await page.request.get(`/api/v1/hotels/${hotelId}/admins`);
    const adminJson = await adminRes.json();

    if (!adminJson.data || adminJson.data.length === 0) {
      test.skip('호텔 관리자 데이터 없음 - 테스트 스킵');
      return;
    }

    const firstAdmin = adminJson.data[0];
    const consoleErrors = collectConsoleErrors(page);

    // sessionStorage에 hotelId를 먼저 세팅 (HotelAdminForm.init()이 컨텍스트에서 hotelId를 읽음)
    await page.goto('/admin/members/hotel-admins');
    await page.waitForLoadState('domcontentloaded');
    await page.evaluate((hid) => {
      sessionStorage.setItem('selectedHotelId', String(hid));
    }, hotelId);

    await verifyPageLoad(
      page,
      `/admin/members/hotel-admins/${firstAdmin.id}/edit`,
      '호텔 관리자'
    );

    // loadAdmin() API 응답 대기 (비동기 호출)
    await page.waitForFunction(() => {
      const val = document.querySelector('#loginId') ? document.querySelector('#loginId').value : '';
      return val.length > 0;
    }, { timeout: 10000 });

    // 아이디 필드에 값이 채워졌는지 확인
    const loginIdValue = await page.locator('#loginId').inputValue();
    expect(loginIdValue.length).toBeGreaterThan(0);

    // 비밀번호 초기화 버튼 표시 (수정 모드)
    await expect(page.locator('#resetPwdArea')).toBeVisible();

    // 삭제 버튼 표시 (수정 모드)
    await expect(page.locator('#btnDelete')).toBeVisible();

    // 저장 버튼 텍스트 확인 (수정 모드도 "저장" 표시)
    const btnSaveText = await page.locator('#btnSaveText').textContent();
    expect(btnSaveText).toContain('저장');

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

});

// ============================================================
// 3. 프로퍼티 관리자 (Property Admin)
// ============================================================
test.describe('프로퍼티 관리자', () => {

  test('리스트 페이지 로드', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/members/property-admins', '프로퍼티 관리자 관리');

    // DataTable 렌더링 확인
    await verifyDataTableLoaded(page, '#propertyAdminTable');

    // 테이블 헤더 5컬럼 확인
    const headers = page.locator('#propertyAdminTable thead th');
    await expect(headers).toHaveCount(5);

    // 호텔/프로퍼티 검색 필드 확인
    await expect(page.locator('#searchHotelName')).toBeVisible();
    await expect(page.locator('#searchPropertyName')).toBeVisible();
    await expect(page.locator('#searchLoginId')).toBeVisible();

    // contextAlert 존재 확인
    await expect(page.locator('#contextAlert')).toBeAttached();

    // 등록 버튼 확인
    await expect(page.locator('a[href="/admin/members/property-admins/new"]')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('등록 폼 - 호텔→프로퍼티 연동 및 UI 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/members/property-admins/new', '프로퍼티 관리자 등록');

    // 폼 존재 확인
    await expect(page.locator('#propertyAdminForm')).toBeVisible();

    // 호텔명 / 프로퍼티명 표시 필드 (readonly, 컨텍스트 연동)
    await expect(page.locator('#hotelName')).toBeAttached();
    await expect(page.locator('#propertyName')).toBeAttached();

    // 아이디 중복확인 버튼
    await expect(page.locator('#btnCheckLoginId')).toBeVisible();

    // 필수 필드 확인
    await expect(page.locator('#loginId')).toBeVisible();
    await expect(page.locator('#userName')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#phone')).toBeVisible();

    // 프로퍼티관리자 권한 셀렉트박스 확인
    await expect(page.locator('#roleId')).toBeVisible();

    // 비밀번호 초기화 영역 - 등록 모드에서 숨김
    await expect(page.locator('#resetPwdArea')).not.toBeVisible();

    // 계정유형 표시 확인
    const pageBody = await page.locator('body').textContent();
    expect(pageBody).toContain('프로퍼티 관리자');

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('수정 폼 - 데이터 로드 확인', async ({ page }) => {
    // 호텔 셀렉터로 첫 번째 호텔 ID 조회
    const hotelRes = await page.request.get('/api/v1/hotels/selector');
    const hotelJson = await hotelRes.json();

    if (!hotelJson.data || hotelJson.data.length === 0) {
      test.skip('호텔 데이터 없음 - 테스트 스킵');
      return;
    }

    const hotelId = hotelJson.data[0].id;

    // 프로퍼티 셀렉터로 첫 번째 프로퍼티 ID 조회
    const propRes = await page.request.get(`/api/v1/properties/selector?hotelId=${hotelId}`);
    const propJson = await propRes.json();

    if (!propJson.data || propJson.data.length === 0) {
      test.skip('프로퍼티 데이터 없음 - 테스트 스킵');
      return;
    }

    const propertyId = propJson.data[0].id;

    // 해당 프로퍼티의 관리자 목록 조회
    const adminRes = await page.request.get(`/api/v1/properties/${propertyId}/admins`);
    const adminJson = await adminRes.json();

    if (!adminJson.data || adminJson.data.length === 0) {
      test.skip('프로퍼티 관리자 데이터 없음 - 테스트 스킵');
      return;
    }

    const firstAdmin = adminJson.data[0];
    const consoleErrors = collectConsoleErrors(page);

    // sessionStorage에 hotelId, propertyId 세팅 (PropertyAdminForm.init()이 컨텍스트에서 읽음)
    await page.goto('/admin/members/property-admins');
    await page.waitForLoadState('domcontentloaded');
    await page.evaluate(({ hid, pid }) => {
      sessionStorage.setItem('selectedHotelId', String(hid));
      sessionStorage.setItem('selectedPropertyId', String(pid));
    }, { hid: hotelId, pid: propertyId });

    await verifyPageLoad(
      page,
      `/admin/members/property-admins/${firstAdmin.id}/edit`,
      '프로퍼티 관리자'
    );

    // loadAdmin() API 응답 대기
    await page.waitForFunction(() => {
      const val = document.querySelector('#loginId') ? document.querySelector('#loginId').value : '';
      return val.length > 0;
    }, { timeout: 10000 });

    // 아이디 필드에 값이 채워졌는지 확인
    const loginIdValue = await page.locator('#loginId').inputValue();
    expect(loginIdValue.length).toBeGreaterThan(0);

    // 비밀번호 초기화 버튼 표시 (수정 모드)
    await expect(page.locator('#resetPwdArea')).toBeVisible();

    // 삭제 버튼 표시 (수정 모드)
    await expect(page.locator('#btnDelete')).toBeVisible();

    // 저장 버튼 텍스트 확인 (수정 모드도 "저장" 표시)
    const btnSaveText = await page.locator('#btnSaveText').textContent();
    expect(btnSaveText).toContain('저장');

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

});

// ============================================================
// 4. 호텔 관리자 권한 (Hotel Admin Role)
// ============================================================
test.describe('호텔 관리자 권한', () => {

  test('리스트 페이지 로드', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/roles/hotel-admins', '호텔 관리자 권한 관리');

    // DataTable 렌더링 확인
    await verifyDataTableLoaded(page, '#roleTable');

    // 테이블 헤더 5컬럼 확인
    const headers = page.locator('#roleTable thead th');
    await expect(headers).toHaveCount(5);

    // 검색 영역 확인
    await expect(page.locator('#searchHotelId')).toBeVisible();
    await expect(page.locator('#searchRoleName')).toBeVisible();

    // 등록 버튼 확인
    await expect(page.locator('a[href="/admin/roles/hotel-admins/new"]')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('등록 폼 - 권한명 입력 + 메뉴트리(3-depth) 표시 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/roles/hotel-admins/new', '호텔 관리자 권한 등록');

    // 폼 존재 확인
    await expect(page.locator('#roleForm')).toBeVisible();

    // 권한명 + 중복확인 버튼
    await expect(page.locator('#roleName')).toBeVisible();
    await expect(page.locator('#btnCheckName')).toBeVisible();

    // 호텔 셀렉트박스 확인
    await expect(page.locator('#hotelId')).toBeVisible();

    // 사용여부 라디오
    await expect(page.locator('input[name="useYn"][value="true"]')).toBeAttached();

    // 메뉴트리 영역 확인
    await expect(page.locator('#menuTreeArea')).toBeVisible();
    await expect(page.locator('#menuSelectAll')).toBeAttached(); // 전체 선택 체크박스
    await expect(page.locator('#menuTree')).toBeVisible();

    // 메뉴트리 체크박스 렌더링 완료 대기 (API 응답 후 동적 렌더링)
    await page.waitForFunction(() => {
      return document.querySelectorAll('#menuTree input[type="checkbox"]').length > 0;
    }, { timeout: 10000 });

    // 메뉴트리에 체크박스가 렌더링되었는지 확인
    const menuCheckboxCount = await page.locator('#menuTree input[type="checkbox"]').count();
    expect(menuCheckboxCount).toBeGreaterThan(0);

    // 섹션 헤더 확인
    await expect(page.locator('h6:has-text("기본 정보")')).toBeVisible();
    await expect(page.locator('h6:has-text("화면 권한 설정")')).toBeVisible();

    // 취소/저장 버튼 (사이드바 nav-link와 구분하기 위해 .btn 클래스 지정)
    await expect(page.locator('a.btn[href="/admin/roles/hotel-admins"]')).toBeVisible();
    await expect(page.locator('button:has-text("등록")')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('메뉴트리 체크박스 전체선택 동작 확인', async ({ page }) => {
    await page.goto('/admin/roles/hotel-admins/new');
    await page.waitForLoadState('networkidle');
    // 메뉴트리 렌더링 대기
    await page.waitForFunction(() => {
      return document.querySelectorAll('#menuTree input[type="checkbox"]').length > 0;
    }, { timeout: 10000 });

    const menuCheckboxes = page.locator('#menuTree input[type="checkbox"]');
    const count = await menuCheckboxes.count();

    if (count === 0) {
      test.skip('메뉴 체크박스 없음 - 테스트 스킵');
      return;
    }

    // 전체선택 클릭
    await page.locator('#menuSelectAll').click();
    await page.waitForTimeout(500);

    // 전체선택 후 모든 체크박스가 체크됨
    const checkedCount = await page.locator('#menuTree input[type="checkbox"]:checked').count();
    expect(checkedCount).toBe(count);

    // 전체해제 클릭
    await page.locator('#menuSelectAll').click();
    await page.waitForTimeout(500);

    const checkedAfterDeselect = await page.locator('#menuTree input[type="checkbox"]:checked').count();
    expect(checkedAfterDeselect).toBe(0);
  });

  test('수정 폼 - 기존 권한 데이터 로드 확인', async ({ page }) => {
    // API로 호텔 관리자 권한 목록 조회
    const response = await page.request.get('/api/v1/hotel-admin-roles');
    const json = await response.json();

    if (!json.data || json.data.length === 0) {
      test.skip('호텔 관리자 권한 데이터 없음 - 테스트 스킵');
      return;
    }

    const firstRole = json.data[0];
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(
      page,
      `/admin/roles/hotel-admins/${firstRole.id}/edit`,
      '호텔 관리자 권한'
    );

    // loadRole() API 응답 대기 (loadHotels() → loadRole() 비동기 체인)
    await page.waitForFunction(() => {
      const el = document.querySelector('#roleName');
      return el && el.value.length > 0;
    }, { timeout: 10000 });

    // 권한명 필드에 값이 채워졌는지 확인
    const roleNameValue = await page.locator('#roleName').inputValue();
    expect(roleNameValue.length).toBeGreaterThan(0);

    // 삭제 버튼 표시 (수정 모드)
    await expect(page.locator('#btnDelete')).toBeVisible();

    // 저장 버튼 텍스트 "수정"
    const btnSaveText = await page.locator('#btnSaveText').textContent();
    expect(btnSaveText).toContain('저장');

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

});

// ============================================================
// 5. 프로퍼티 관리자 권한 (Property Admin Role)
// ============================================================
test.describe('프로퍼티 관리자 권한', () => {

  test('리스트 페이지 로드', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/roles/property-admins', '프로퍼티 관리자 권한 관리');

    // DataTable 렌더링 확인
    await verifyDataTableLoaded(page, '#roleTable');

    // 테이블 헤더 6컬럼 확인 (호텔명 + 프로퍼티명 포함)
    const headers = page.locator('#roleTable thead th');
    await expect(headers).toHaveCount(6);

    // 검색 영역 확인
    await expect(page.locator('#searchHotelId')).toBeVisible();
    await expect(page.locator('#searchRoleName')).toBeVisible();

    // 등록 버튼 확인
    await expect(page.locator('a[href="/admin/roles/property-admins/new"]')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('등록 폼 - 권한명 + 호텔/프로퍼티 연동 + 메뉴트리 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(page, '/admin/roles/property-admins/new', '프로퍼티 관리자 권한 등록');

    // 폼 존재 확인
    await expect(page.locator('#roleForm')).toBeVisible();

    // 권한명 + 중복확인
    await expect(page.locator('#roleName')).toBeVisible();
    await expect(page.locator('#btnCheckName')).toBeVisible();

    // 호텔 + 프로퍼티 셀렉트박스 확인 (연동 구조)
    await expect(page.locator('#hotelId')).toBeVisible();
    await expect(page.locator('#propertyId')).toBeVisible();

    // 초기 프로퍼티 셀렉트 안내 문구 확인
    const propertyDefaultOption = await page.locator('#propertyId option').first().textContent();
    expect(propertyDefaultOption).toContain('호텔을 먼저 선택하세요');

    // 사용여부 라디오
    await expect(page.locator('input[name="useYn"][value="true"]')).toBeAttached();

    // 메뉴트리 영역 확인
    await expect(page.locator('#menuTreeArea')).toBeVisible();
    await expect(page.locator('#menuSelectAll')).toBeAttached();
    await expect(page.locator('#menuTree')).toBeVisible();

    // 메뉴트리 체크박스 렌더링 완료 대기
    await page.waitForFunction(() => {
      return document.querySelectorAll('#menuTree input[type="checkbox"]').length > 0;
    }, { timeout: 10000 });
    const menuCheckboxCount = await page.locator('#menuTree input[type="checkbox"]').count();
    expect(menuCheckboxCount).toBeGreaterThan(0);

    // 섹션 헤더 확인
    await expect(page.locator('h6:has-text("기본 정보")')).toBeVisible();
    await expect(page.locator('h6:has-text("화면 권한 설정")')).toBeVisible();

    // 취소/저장 버튼 (사이드바 nav-link와 구분하기 위해 .btn 클래스 지정)
    await expect(page.locator('a.btn[href="/admin/roles/property-admins"]')).toBeVisible();
    await expect(page.locator('button:has-text("등록")')).toBeVisible();

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

  test('메뉴트리 체크박스 전체선택 동작 확인', async ({ page }) => {
    await page.goto('/admin/roles/property-admins/new');
    await page.waitForLoadState('networkidle');
    // 메뉴트리 렌더링 대기
    await page.waitForFunction(() => {
      return document.querySelectorAll('#menuTree input[type="checkbox"]').length > 0;
    }, { timeout: 10000 });

    const menuCheckboxes = page.locator('#menuTree input[type="checkbox"]');
    const count = await menuCheckboxes.count();

    if (count === 0) {
      test.skip('메뉴 체크박스 없음 - 테스트 스킵');
      return;
    }

    // 전체선택 클릭
    await page.locator('#menuSelectAll').click();
    await page.waitForTimeout(500);

    // 전체선택 후 모든 체크박스 체크됨
    const checkedCount = await page.locator('#menuTree input[type="checkbox"]:checked').count();
    expect(checkedCount).toBe(count);

    // 전체해제 클릭
    await page.locator('#menuSelectAll').click();
    await page.waitForTimeout(500);

    const checkedAfterDeselect = await page.locator('#menuTree input[type="checkbox"]:checked').count();
    expect(checkedAfterDeselect).toBe(0);
  });

  test('수정 폼 - 기존 권한 데이터 로드 확인', async ({ page }) => {
    // API로 프로퍼티 관리자 권한 목록 조회
    const response = await page.request.get('/api/v1/property-admin-roles');
    const json = await response.json();

    if (!json.data || json.data.length === 0) {
      test.skip('프로퍼티 관리자 권한 데이터 없음 - 테스트 스킵');
      return;
    }

    const firstRole = json.data[0];
    const consoleErrors = collectConsoleErrors(page);

    await verifyPageLoad(
      page,
      `/admin/roles/property-admins/${firstRole.id}/edit`,
      '프로퍼티 관리자 권한'
    );

    // loadRole() API 응답 대기
    await page.waitForFunction(() => {
      const el = document.querySelector('#roleName');
      return el && el.value.length > 0;
    }, { timeout: 10000 });

    // 권한명 필드에 값이 채워졌는지 확인
    const roleNameValue = await page.locator('#roleName').inputValue();
    expect(roleNameValue.length).toBeGreaterThan(0);

    // 삭제 버튼 표시 (수정 모드)
    await expect(page.locator('#btnDelete')).toBeVisible();

    // 저장 버튼 텍스트 "수정"
    const btnSaveText = await page.locator('#btnSaveText').textContent();
    expect(btnSaveText).toContain('저장');

    // 콘솔 에러 없어야 함
    expect(consoleErrors).toHaveLength(0);
  });

});
