/**
 * 예약 상세/수정 페이지
 */
var ReservationDetail = {
    // 객실 HK 상태 배지 렌더링 (중복 제거용)
    _roomStatusBadge: function(unavailableType, defaultLabel) {
        switch (unavailableType) {
            case 'OOO': return '<span class="badge bg-secondary">OOO</span>';
            case 'OOS': return '<span class="badge badge-oos">OOS</span>';
            case 'DIRTY': return '<span class="badge badge-dirty">DIRTY</span>';
            case 'PICKUP': return '<span class="badge badge-pickup">PICKUP</span>';
            default: return '<span class="badge bg-danger">' + (defaultLabel || '불가') + '</span>';
        }
    },

    propertyId: null,
    reservationId: null,
    reservationData: null,   // API 응답 전체 저장
    isOta: false,
    isReadonly: false,       // CHECKED_OUT / CANCELED / NO_SHOW
    isDirty: false,          // 미저장 변경 감지
    roomLegSeq: 0,           // 객실 레그 시퀀스
    currentLegSeq: null,     // 현재 모달 대상 레그 시퀀스

    // 유료 서비스 옵션 캐시 (roomTypeId별)
    paidServiceOptionsCache: {},

    // 모달 DataTable 인스턴스
    rateCodeTable: null,
    marketCodeTable: null,
    roomTypeTable: null,

    // 상태 배지 매핑 (HolaPms.reservationStatus 공통 참조)
    STATUS_BADGE: HolaPms.reservationStatus,

    // 상태 전이 매트릭스 (체크인 → 바로 INHOUSE, 업계 표준)
    STATUS_TRANSITIONS: {
        RESERVED: [
            { status: 'INHOUSE', label: '체크인', icon: 'fa-sign-in-alt', cls: 'btn-info' },
            { status: 'CANCELED', label: '취소', icon: 'fa-ban', cls: 'btn-danger' },
            { status: 'NO_SHOW', label: '노쇼', icon: 'fa-user-slash', cls: 'btn-warning' }
        ],
        CHECK_IN: [
            // 하위 호환: 기존 CHECK_IN 데이터 → INHOUSE 전이
            { status: 'INHOUSE', label: '투숙중', icon: 'fa-door-open', cls: 'btn-success' },
            { status: 'CANCELED', label: '취소', icon: 'fa-ban', cls: 'btn-danger' }
        ],
        INHOUSE: [
            { status: 'CHECKED_OUT', label: '체크아웃', icon: 'fa-sign-out-alt', cls: 'btn-secondary' }
        ]
    },

    /**
     * 초기화
     */
    init: function() {
        var self = this;

        // URL에서 reservationId 추출 (/admin/reservations/{id})
        var pathParts = window.location.pathname.split('/');
        self.reservationId = pathParts[pathParts.length - 1];

        self.bindEvents();

        // URL tab 파라미터로 탭 자동 활성화 (운영현황 등에서 특정 탭으로 이동 시)
        var urlParams = new URLSearchParams(window.location.search);
        var targetTab = urlParams.get('tab');
        var tabMap = { detail: '#tabDetail', payment: '#tabPayment', deposit: '#tabDeposit', history: '#tabHistory', etc: '#tabEtc' };
        var pendingTabSelector = targetTab ? tabMap[targetTab] : null;

        self.reload(function() {
            // 데이터 로드 완료 후 탭 전환 (콜백 기반, setTimeout 제거)
            if (pendingTabSelector) {
                var $tabLink = $('a[href="' + pendingTabSelector + '"]');
                if ($tabLink.length) {
                    var tab = new bootstrap.Tab($tabLink[0]);
                    tab.show();
                }
            }
        });
    },

    /**
     * 컨텍스트 기반 새로고침
     */
    reload: function(callback) {
        var self = this;
        self.propertyId = HolaPms.context.getPropertyId();
        if (!self.propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#formContainer').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        self.loadReservationChannels();
        self.loadPaidServiceOptions();
        self.loadData(callback);
    },

    /**
     * 예약 데이터 조회
     */
    loadData: function(callback) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId,
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    self.bindData(res.data);
                    self.loadChangeHistory();
                    self.clearDirty();
                }
                if (callback) callback();
            }
        });
    },

    /**
     * 예약채널 목록 로드
     */
    loadReservationChannels: function(selectedChannelId) {
        var self = this;
        if (!self.propertyId) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservation-channels',
            type: 'GET',
            success: function(res) {
                var $select = $('#reservationChannelId');
                $select.find('option:not(:first)').remove();
                var channels = (res.data || res || []);
                if (Array.isArray(channels)) {
                    channels.forEach(function(ch) {
                        $select.append('<option value="' + ch.id + '">' + HolaPms.escapeHtml(ch.channelName || ch.name) + '</option>');
                    });
                }
                // 콜백 기반 채널 선택 (setTimeout 제거)
                if (selectedChannelId) {
                    $select.val(selectedChannelId);
                }
            },
            error: function() {
                // 예약채널 API 미구현 시 무시
            }
        });
    },

    /**
     * 유료 서비스 옵션 목록 로드 (roomTypeId별 캐싱)
     */
    loadPaidServiceOptions: function(roomTypeId, callback) {
        var self = this;
        if (!self.propertyId) return;

        var cacheKey = roomTypeId || 'all';
        if (self.paidServiceOptionsCache[cacheKey]) {
            if (callback) callback(self.paidServiceOptionsCache[cacheKey]);
            return;
        }

        var url = '/api/v1/properties/' + self.propertyId + '/paid-service-options';
        if (roomTypeId) {
            url += '?roomTypeId=' + roomTypeId;
        }

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                var options = (res.data || res || []);
                if (Array.isArray(options)) {
                    options = options.filter(function(o) { return o.useYn !== false; });
                }
                self.paidServiceOptionsCache[cacheKey] = options;
                if (callback) callback(options);
            },
            error: function() {
                self.paidServiceOptionsCache[cacheKey] = [];
                if (callback) callback([]);
            }
        });
    },

    /**
     * 데이터 바인딩
     */
    bindData: function(data) {
        var self = this;
        self.reservationData = data;
        self.isOta = data.isOtaManaged === true;
        self.isReadonly = ['CHECKED_OUT', 'CANCELED', 'NO_SHOW'].indexOf(data.reservationStatus) !== -1;

        // 폼 컨테이너 표시
        $('#formContainer').show();
        $('#contextAlert').addClass('d-none');

        // 상태 배지
        var statusInfo = self.STATUS_BADGE[data.reservationStatus] || { label: data.reservationStatus, cls: 'bg-secondary' };
        $('#statusBadge').attr('class', 'badge ' + statusInfo.cls).text(statusInfo.label);

        // 예약번호 + 확인번호
        var noText = '';
        if (data.masterReservationNo) noText += '예약번호: ' + data.masterReservationNo;
        if (data.confirmationNo) noText += '  |  확인번호: ' + data.confirmationNo;
        $('#reservationNoDisplay').text(noText);

        // OTA / Readonly 경고
        if (self.isOta) $('#otaAlert').removeClass('d-none');
        if (self.isReadonly) $('#readonlyAlert').removeClass('d-none');

        var subs = data.subReservations || [];
        // 레이트코드 기반 stayType 우선, fallback으로 첫 번째 sub 확인
        self.isDayUse = data.stayType === 'DAY_USE'
            || (subs.length > 0 && subs[0].stayType === 'DAY_USE');

        // 프로퍼티 기본 체크인/체크아웃 시간
        self.propertyCheckInTime = data.propertyCheckInTime || '15:00';
        self.propertyCheckOutTime = data.propertyCheckOutTime || '11:00';

        // Dayuse 이용시간(시) — 기존 Leg에서 추출 (새 Leg 추가 시 동일 시간 적용)
        self.dayUseDurationHours = null;
        if (self.isDayUse) {
            var dayUseSub = subs.find(function(s) { return s.dayUseStartTime && s.dayUseEndTime; });
            if (dayUseSub) {
                var start = dayUseSub.dayUseStartTime.split(':');
                var end = dayUseSub.dayUseEndTime.split(':');
                self.dayUseDurationHours = parseInt(end[0]) - parseInt(start[0]);
            }
        }

        // ── Tab 1: 예약정보 ──
        $('#masterReservationNoDisplay').val(data.masterReservationNo || '');
        $('#confirmationNoDisplay').val(data.confirmationNo || '');
        $('#masterCheckIn').val(data.masterCheckIn || '');
        $('#masterCheckOut').val(self.isDayUse ? (data.masterCheckIn || '') : (data.masterCheckOut || ''));
        this.bindReservationDates('#masterCheckIn', '#masterCheckOut');

        if (data.rateCodeId) {
            $('#rateCodeId').val(data.rateCodeId);
            if (data.rateCodeName) {
                $('#rateCodeName').val(data.rateCodeName);
            } else {
                self.resolveCodeName('/api/v1/properties/' + self.propertyId + '/rate-codes', data.rateCodeId, '#rateCodeName', 'rateNameKo');
            }
        } else {
            $('#rateCodeId').val('');
            $('#rateCodeName').val('');
        }
        if (data.marketCodeId) {
            $('#marketCodeId').val(data.marketCodeId);
            if (data.marketCodeName) {
                $('#marketCodeName').val(data.marketCodeName);
            } else {
                self.resolveCodeName('/api/v1/market-codes', data.marketCodeId, '#marketCodeName', 'marketCodeName');
            }
        } else {
            $('#marketCodeId').val('');
            $('#marketCodeName').val('');
        }
        if (data.reservationChannelId) {
            // 채널 로드 완료 콜백에서 선택
            self.loadReservationChannels(data.reservationChannelId);
        }
        $('#promotionCode').val(data.promotionCode || '');

        // 예약자 정보
        $('#guestNameKo').val(data.guestNameKo || '');
        $('#guestLastNameEn').val(data.guestLastNameEn || '');
        $('#guestFirstNameEn').val(data.guestFirstNameEn || '');
        if (data.phoneCountryCode) $('#phoneCountryCode').val(data.phoneCountryCode);
        $('#phoneNumber').val(data.phoneNumber || '');
        $('#email').val(data.email || '');
        $('#birthDate').val(data.birthDate || '');
        if (data.gender) $('#gender').val(data.gender);
        $('#nationality').val(data.nationality || '');

        // OTA 정보
        $('#isOtaManaged').prop('checked', data.isOtaManaged === true);
        $('#otaReservationNo').val(data.otaReservationNo || '');

        // ── Tab 2: 서브 예약(레그) ──
        self.renderLegs(data.subReservations || []);

        // ── Tab 3: 예치금 ──
        self.bindDeposit(data.deposits || []);

        // ── Tab 4: 결제 정보 ──
        ReservationPayment.load(self.propertyId, self.reservationId, data);

        // ── Tab 5: 기타정보 ──
        $('#customerRequest').val(data.customerRequest || '');
        self.renderMemos(data.memos || []);

        // 상태별 필드 제어
        self.applyFieldControl();

        // 마스터 상태 변경은 Leg 카드에서 개별 처리 (하단 드롭다운 제거됨)
    },

    /**
     * 서브 예약(레그) 렌더링
     */
    renderLegs: function(legs) {
        var self = this;

        // 재렌더링 전 collapse 상태 저장 (Leg 접힘 + 서비스 접힘)
        var legCollapseState = {};
        var serviceCollapseState = {};
        $('.room-leg-card').each(function() {
            var seq = $(this).data('leg-seq');
            var $legBody = $('#roomLegBody_' + seq);
            var $svcCollapse = $('#serviceCollapse_' + seq);
            if ($legBody.length) legCollapseState[seq] = $legBody.hasClass('show');
            if ($svcCollapse.length) serviceCollapseState[seq] = $svcCollapse.hasClass('show');
        });

        $('#roomLegsContainer').empty();
        self.roomLegSeq = 0;

        if (!legs || legs.length === 0) {
            $('#roomLegsEmpty').show();
            return;
        }

        $('#roomLegsEmpty').hide();
        legs.forEach(function(leg) {
            self.addRoomLeg(leg);
        });

        // 저장된 collapse 상태 복원
        if (Object.keys(legCollapseState).length > 0) {
            $('.room-leg-card').each(function() {
                var seq = $(this).data('leg-seq');
                var $legBody = $('#roomLegBody_' + seq);
                var $header = $(this).find('.card-header[data-bs-toggle="collapse"]');
                if (legCollapseState.hasOwnProperty(seq)) {
                    if (legCollapseState[seq]) {
                        $legBody.addClass('show');
                        $header.removeClass('collapsed').attr('aria-expanded', 'true');
                    } else {
                        $legBody.removeClass('show');
                        $header.addClass('collapsed').attr('aria-expanded', 'false');
                    }
                }
                var $svcCollapse = $('#serviceCollapse_' + seq);
                var $svcToggle = $svcCollapse.closest('.card-body').find('[data-bs-target="#serviceCollapse_' + seq + '"]');
                if (serviceCollapseState.hasOwnProperty(seq)) {
                    if (serviceCollapseState[seq]) {
                        $svcCollapse.addClass('show');
                        $svcToggle.removeClass('collapsed');
                    } else {
                        $svcCollapse.removeClass('show');
                        $svcToggle.addClass('collapsed');
                    }
                }
            });
        }
    },

    /**
     * 객실 레그 카드 동적 추가 (기존 데이터 바인딩 포함)
     */
    addRoomLeg: function(legData) {
        var self = this;
        self.roomLegSeq++;
        var seq = self.roomLegSeq;

        // 기본값 설정
        var checkIn = '', checkOut = '', adults = 1, children = 0, earlyCheckIn = false, lateCheckOut = false;
        var roomTypeName = '', roomTypeId = '', floorId = '', roomNumberId = '', roomDisplay = '미배정';
        var legId = '', subNo = '';

        // 실제 체크인/아웃 시각 및 얼리/레이트 요금
        var actualCheckInTime = '', actualCheckOutTime = '';
        var earlyCheckInFee = 0, lateCheckOutFee = 0;
        var dayUseStartTime = '', dayUseEndTime = '';
        var legStayType = '';

        if (legData) {
            checkIn = legData.checkIn || '';
            checkOut = (legData.stayType === 'DAY_USE' && legData.checkIn) ? legData.checkIn : (legData.checkOut || '');
            adults = legData.adults || 1;
            children = legData.children || 0;
            earlyCheckIn = legData.earlyCheckIn === true;
            lateCheckOut = legData.lateCheckOut === true;
            roomTypeName = legData.roomTypeName || '';
            roomTypeId = legData.roomTypeId || '';
            floorId = legData.floorId || '';
            roomNumberId = legData.roomNumberId || '';
            legId = legData.id || '';
            subNo = legData.subReservationNo || '';
            actualCheckInTime = legData.actualCheckInTime || '';
            actualCheckOutTime = legData.actualCheckOutTime || '';
            earlyCheckInFee = legData.earlyCheckInFee || 0;
            lateCheckOutFee = legData.lateCheckOutFee || 0;
            dayUseStartTime = legData.dayUseStartTime || '';
            dayUseEndTime = legData.dayUseEndTime || '';
            legStayType = legData.stayType || '';
            if (legData.floorName && legData.roomNumber) {
                roomDisplay = legData.floorName + ' / ' + legData.roomNumber;
            } else if (legData.roomNumber) {
                roomDisplay = legData.roomNumber;
            }
        } else {
            // 신규 추가 시 마스터 날짜 기본값
            checkIn = $('#masterCheckIn').val() || '';
            checkOut = $('#masterCheckOut').val() || '';
        }

        // Leg별 상태 (편집 제어에 사용)
        var legStatus = legData ? (legData.roomReservationStatus || '') : '';
        // 종료 상태 Leg: 모든 편집 컨트롤 비활성화
        var legTerminated = ['CHECKED_OUT', 'CANCELED', 'NO_SHOW'].indexOf(legStatus) !== -1;

        // 상태에 따른 얼리/레이트 체크박스 활성화 제어 (Leg 상태 기준)
        // RESERVED: 둘 다 편집 가능 (사전 요청)
        // CHECK_IN/INHOUSE: 얼리 읽기전용 (자동 판정 완료), 레이트 편집 가능
        // CHECKED_OUT/CANCELED/NO_SHOW: 둘 다 읽기전용
        var status = legStatus || (self.reservationData ? self.reservationData.reservationStatus : '');
        var earlyDisabled = '';
        var lateDisabled = '';
        if (['CHECK_IN', 'INHOUSE'].indexOf(status) !== -1) {
            earlyDisabled = ' disabled';
        } else if (['CHECKED_OUT', 'CANCELED', 'NO_SHOW'].indexOf(status) !== -1) {
            earlyDisabled = ' disabled';
            lateDisabled = ' disabled';
        }

        var headerLabel = '객실 #' + seq;
        if (subNo) headerLabel += ' <span class="text-muted small ms-2">(' + HolaPms.escapeHtml(subNo) + ')</span>';

        // Leg별 상태 뱃지 + 액션 드롭다운 (허용 전이 전체 표시)
        var legStatusBadge = legStatus ? HolaPms.reservationStatus.styledBadge(legStatus) : '';
        var legActionBtn = '';
        if (legId && legStatus && self.STATUS_TRANSITIONS[legStatus]) {
            var transitions = self.STATUS_TRANSITIONS[legStatus];
            var btnAttr = 'data-leg-id="' + legId + '"';
            var items = '';
            transitions.forEach(function(t) {
                items += '<li><a class="dropdown-item leg-status-change" href="#" ' + btnAttr + ' data-new-status="' + t.status + '">'
                    + '<i class="fas ' + t.icon + ' me-2"></i>' + t.label + '</a></li>';
            });
            legActionBtn = '<div class="dropdown d-inline-block ms-2">'
                + '<button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">'
                + '<i class="fas fa-exchange-alt me-1"></i>상태 변경</button>'
                + '<ul class="dropdown-menu">' + items + '</ul></div>';
        }

        // 헤더 요약 정보 (접힌 상태에서도 보이는 객실타입/호수)
        var summaryParts = [];
        if (roomTypeName) summaryParts.push(HolaPms.escapeHtml(roomTypeName));
        if (roomDisplay && roomDisplay !== '미배정') summaryParts.push(HolaPms.escapeHtml(roomDisplay));
        var summaryInfo = summaryParts.length > 0
            ? '<span class="text-muted small ms-2 leg-summary">' + summaryParts.join(' | ') + '</span>'
            : '';

        var legHtml = ''
            + '<div class="card border shadow-sm mb-3 room-leg-card" id="roomLeg_' + seq + '" data-leg-seq="' + seq + '" data-leg-id="' + legId + '" data-leg-terminated="' + legTerminated + '">'
            + '  <div class="card-header d-flex justify-content-between align-items-center" style="cursor:pointer;" data-bs-toggle="collapse" data-bs-target="#roomLegBody_' + seq + '">'
            + '    <span>' + headerLabel + ' ' + legStatusBadge + summaryInfo + legActionBtn + '</span>'
            + '    <div class="d-flex align-items-center">'
            + (legId && !legTerminated && self.isUpgradeable() ? '    <button class="btn btn-outline-primary btn-sm me-1 upgrade-btn" data-leg-id="' + legId + '" data-room-type-id="' + roomTypeId + '" data-room-type-name="' + HolaPms.escapeHtml(roomTypeName) + '"><i class="fas fa-arrow-up me-1"></i>업그레이드</button>' : '')
            + (!legTerminated ? '    <button class="btn btn-outline-danger btn-sm remove-leg-btn me-2" data-leg="' + seq + '" data-leg-id="' + legId + '" aria-label="객실 삭제"><i class="fas fa-times"></i></button>' : '')
            + '    <i class="fas fa-chevron-down collapse-arrow"></i>'
            + '    </div>'
            + '  </div>'
            + '  <div class="collapse show" id="roomLegBody_' + seq + '">'
            + '  <div class="card-body">'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label required">객실타입</label>'
            + '      <div class="col-sm-4">'
            + '        <div class="input-group">'
            + '          <input type="text" class="form-control room-type-name" readonly placeholder="선택" value="' + HolaPms.escapeHtml(roomTypeName) + '">'
            + (!legTerminated ? '          <button class="btn btn-outline-secondary room-type-search-btn" type="button" aria-label="객실타입 검색"><i class="fas fa-search"></i></button>' : '')
            + (!legTerminated ? '          <button class="btn btn-outline-danger room-type-clear-btn" type="button" aria-label="객실타입 초기화"><i class="fas fa-times"></i></button>' : '')
            + '        </div>'
            + '        <input type="hidden" class="room-type-id" value="' + roomTypeId + '">'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label">층/호수</label>'
            + '      <div class="col-sm-4">'
            + '        <div class="input-group">'
            + '          <input type="text" class="form-control room-number-display" readonly placeholder="미배정" value="' + HolaPms.escapeHtml(roomDisplay === '미배정' ? '' : roomDisplay) + '">'
            + (!legTerminated ? '          <button class="btn btn-outline-secondary room-assign-btn" type="button" aria-label="객실 배정"><i class="fas fa-search"></i></button>' : '')
            + (!legTerminated ? '          <button class="btn btn-outline-danger room-assign-clear-btn" type="button" aria-label="객실배정 초기화"><i class="fas fa-times"></i></button>' : '')
            + '        </div>'
            + '        <input type="hidden" class="floor-id" value="' + floorId + '">'
            + '        <input type="hidden" class="room-number-id" value="' + roomNumberId + '">'
            + '      </div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">체크인</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-in" value="' + checkIn + '"' + (legTerminated ? ' disabled' : '') + '></div>'
            + '      <label class="col-sm-2 col-form-label">체크아웃</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-out" value="' + checkOut + '"' + (legTerminated ? ' disabled' : '') + '></div>'
            + '    </div>'
            + self.renderCheckTimeRow(legStayType, dayUseStartTime, dayUseEndTime)
            + self.renderActualTimeRow(actualCheckInTime, actualCheckOutTime, earlyCheckInFee, lateCheckOutFee)
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">성인</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-adults" value="' + adults + '" min="1" max="99"' + (legTerminated ? ' disabled' : '') + '></div>'
            + '      <label class="col-sm-2 col-form-label">아동</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-children" value="' + children + '" min="0" max="99"' + (legTerminated ? ' disabled' : '') + '></div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">얼리체크인</label>'
            + '      <div class="col-sm-2">'
            + '        <input type="hidden" class="leg-early-checkin" value="' + (earlyCheckIn ? 'true' : 'false') + '">'
            + '        <div class="btn-group btn-group-sm w-100" role="group">'
            + '          <button type="button" class="btn early-checkin-toggle' + (earlyCheckIn ? ' btn-primary' : ' btn-outline-secondary') + '"'
            + ' data-value="true"' + earlyDisabled + '>사용</button>'
            + '          <button type="button" class="btn early-checkin-toggle' + (!earlyCheckIn ? ' btn-outline-danger' : ' btn-outline-secondary') + '"'
            + ' data-value="false"' + earlyDisabled + '>미사용</button>'
            + '        </div>'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label">레이트체크아웃</label>'
            + '      <div class="col-sm-2">'
            + '        <input type="hidden" class="leg-late-checkout" value="' + (lateCheckOut ? 'true' : 'false') + '">'
            + '        <div class="btn-group btn-group-sm w-100" role="group">'
            + '          <button type="button" class="btn late-checkout-toggle' + (lateCheckOut ? ' btn-primary' : ' btn-outline-secondary') + '"'
            + ' data-value="true"' + lateDisabled + '>사용</button>'
            + '          <button type="button" class="btn late-checkout-toggle' + (!lateCheckOut ? ' btn-outline-danger' : ' btn-outline-secondary') + '"'
            + ' data-value="false"' + lateDisabled + '>미사용</button>'
            + '        </div>'
            + '      </div>'
            + '    </div>'
            + '    <div class="row mb-2 early-late-estimate d-none" id="earlyLateEstimate_' + seq + '" data-leg-id="' + legId + '"'
            + ' data-early-fee="' + earlyCheckInFee + '" data-late-fee="' + lateCheckOutFee + '">'
            + '      <div class="col-sm-10 offset-sm-2">'
            + '        <div class="small" id="earlyLateEstimateContent_' + seq + '"></div>'
            + '      </div>'
            + '    </div>'
            + '    <hr class="my-2">'
            + '    <div class="d-flex justify-content-between align-items-center mb-2">'
            + '      <span class="text-muted small collapsed" style="cursor:pointer;" data-bs-toggle="collapse" data-bs-target="#serviceCollapse_' + seq + '">'
            + '        <i class="fas fa-concierge-bell me-1"></i>유료 서비스'
            + '        <span class="service-count-badge" id="serviceCount_' + seq + '">' + (legData && legData.services && legData.services.length > 0 ? ' (' + legData.services.length + '건)' : '') + '</span>'
            + '        <i class="fas fa-chevron-down collapse-arrow ms-1" style="font-size:10px;"></i>'
            + '      </span>'
            + (!legTerminated && legId ? '      <button class="btn btn-outline-primary btn-sm add-service-btn" data-leg="' + seq + '" data-leg-id="' + legId + '" type="button">'
            + '        <i class="fas fa-plus me-1"></i>서비스 추가'
            + '      </button>' : '')
            + '    </div>'
            + '    <div class="collapse" id="serviceCollapse_' + seq + '">'
            + '    <div class="service-list" id="serviceList_' + seq + '">'
            + self.renderServiceItems(legData ? legData.services : [], seq, legId, legTerminated)
            + '    </div>'
            + '    </div>'
            + '    <div class="service-add-form d-none" id="serviceAddForm_' + seq + '">'
            + '      <div class="row g-2 align-items-end">'
            + '        <div class="col-12 col-md-5">'
            + '          <select class="form-select form-select-sm service-option-select">'
            + '            <option value="">서비스 선택</option>'
            + '          </select>'
            + '        </div>'
            + '        <div class="col-4 col-md-2">'
            + '          <input type="number" class="form-control form-control-sm service-qty" value="1" min="1" max="99">'
            + '        </div>'
            + '        <div class="col-8 col-md-2">'
            + '          <input type="date" class="form-control form-control-sm service-date"'
            + (legData && legData.checkIn ? ' min="' + legData.checkIn + '"' : '')
            + (legData && legData.checkOut ? ' max="' + (function(d){ var dt=new Date(d+'T00:00:00'); dt.setDate(dt.getDate()-1); return dt.getFullYear()+'-'+String(dt.getMonth()+1).padStart(2,'0')+'-'+String(dt.getDate()).padStart(2,'0'); })(legData.checkOut) + '"' : '')
            + '>'
            + '        </div>'
            + '        <div class="col-12 col-md-3 d-flex gap-1">'
            + '          <button class="btn btn-primary btn-sm confirm-add-service-btn" data-leg="' + seq + '" data-leg-id="' + legId + '">추가</button>'
            + '          <button class="btn btn-outline-secondary btn-sm cancel-add-service-btn" data-leg="' + seq + '">취소</button>'
            + '        </div>'
            + '      </div>'
            + '    </div>'
            + (legId ? '    <div class="upgrade-history mt-3" id="upgradeHistory_' + seq + '" data-leg-id="' + legId + '"></div>' : '')
            + '  </div>'
            + '  </div>'
            + '</div>';

        $('#roomLegsContainer').append(legHtml);

        // 서비스 옵션 드롭다운 구성
        this.populateServiceOptions(seq);
        this.updateRoomLegsEmpty();

        // 업그레이드 이력 로드
        if (legId) {
            this.loadUpgradeHistory(seq, legId);
        }

        // 얼리/레이트 사용 시 확정 요금 또는 선택 옵션 표시
        if (legId && (earlyCheckIn || lateCheckOut)) {
            this.showEarlyLateConfirmedFee($('#roomLegsContainer .room-leg-card').last());
        }
    },

    /**
     * 체크인/체크아웃 시간 정보 행 렌더링
     * - Dayuse: 이용시간 (시작~종료)
     * - Overnight: 프로퍼티 기본 체크인/체크아웃 시간
     */
    renderCheckTimeRow: function(stayType, dayUseStartTime, dayUseEndTime) {
        var self = this;
        var isDayUse = stayType === 'DAY_USE' || self.isDayUse;

        if (isDayUse) {
            var startDisplay = dayUseStartTime ? dayUseStartTime.substring(0, 5) : '-';
            var endDisplay = dayUseEndTime ? dayUseEndTime.substring(0, 5) : '-';
            return ''
                + '    <div class="row mb-3">'
                + '      <label class="col-sm-2 col-form-label text-muted small">이용시간</label>'
                + '      <div class="col-sm-10">'
                + '        <span class="form-control-plaintext">'
                + '<span class="badge" style="background-color:#0582CA;">' + startDisplay + ' ~ ' + endDisplay + '</span>'
                + '        </span>'
                + '      </div>'
                + '    </div>';
        }

        // Overnight: 프로퍼티 기본 시간 표시
        return ''
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label text-muted small">체크인 시간</label>'
            + '      <div class="col-sm-4">'
            + '        <span class="form-control-plaintext text-muted small">' + HolaPms.escapeHtml(self.propertyCheckInTime) + '</span>'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label text-muted small">체크아웃 시간</label>'
            + '      <div class="col-sm-4">'
            + '        <span class="form-control-plaintext text-muted small">' + HolaPms.escapeHtml(self.propertyCheckOutTime) + '</span>'
            + '      </div>'
            + '    </div>';
    },

    /**
     * 실제 체크인/아웃 시각 + 얼리/레이트 요금 행 렌더링
     */
    renderActualTimeRow: function(actualCheckInTime, actualCheckOutTime, earlyCheckInFee, lateCheckOutFee) {
        // 표시할 데이터가 없으면 빈 문자열 반환
        if (!actualCheckInTime && !actualCheckOutTime && !earlyCheckInFee && !lateCheckOutFee) {
            return '';
        }

        // 시각 포맷 (2026-03-08T13:00:45 → 2026-03-08 13:00:45)
        var formatTime = function(dt) {
            if (!dt) return '-';
            return dt.replace('T', ' ').substring(0, 19);
        };

        // 금액 포맷
        var formatFee = function(amount) {
            if (!amount || amount <= 0) return '';
            return Number(amount).toLocaleString('ko-KR');
        };

        var checkInDisplay = formatTime(actualCheckInTime);
        var checkOutDisplay = formatTime(actualCheckOutTime);

        // 얼리 체크인 요금 배지
        var earlyBadge = '';
        if (earlyCheckInFee > 0) {
            earlyBadge = ' <span class="badge bg-info ms-1">얼리 체크인 ₩' + formatFee(earlyCheckInFee) + '</span>';
        }

        // 레이트 체크아웃 요금 배지
        var lateBadge = '';
        if (lateCheckOutFee > 0) {
            lateBadge = ' <span class="badge bg-danger ms-1">레이트 체크아웃 ₩' + formatFee(lateCheckOutFee) + '</span>';
        }

        return ''
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label text-muted small">실제 체크인</label>'
            + '      <div class="col-sm-4">'
            + '        <span class="form-control-plaintext">' + HolaPms.escapeHtml(checkInDisplay) + earlyBadge + '</span>'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label text-muted small">실제 체크아웃</label>'
            + '      <div class="col-sm-4">'
            + '        <span class="form-control-plaintext">' + HolaPms.escapeHtml(checkOutDisplay) + lateBadge + '</span>'
            + '      </div>'
            + '    </div>';
    },

    /**
     * 객실 레그 빈 메시지 토글
     */
    updateRoomLegsEmpty: function() {
        if ($('.room-leg-card').length > 0) {
            $('#roomLegsEmpty').hide();
        } else {
            $('#roomLegsEmpty').show();
        }
    },

    /**
     * 서비스 항목 목록 렌더링
     */
    renderServiceItems: function(services, seq, legId, legTerminated) {
        if (!services || services.length === 0) {
            return '<div class="text-muted small text-center py-1">등록된 서비스가 없습니다.</div>';
        }

        var self = this;
        var html = '<table class="table table-sm table-borderless mb-0">';
        services.forEach(function(svc) {
            var name = svc.serviceName || (svc.serviceOptionId ? '서비스 #' + svc.serviceOptionId : '객실 업그레이드');
            var isRateIncluded = svc.serviceType === 'RATE_INCLUDED';
            var badgeHtml = isRateIncluded ? ' <span class="badge bg-info text-white">포함</span>' : '';
            var qtyPrice = svc.quantity + ' x ' + Number(svc.unitPrice || 0).toLocaleString('ko-KR') + '원';
            var taxStr = '세액 ' + Number(svc.tax || 0).toLocaleString('ko-KR') + '원';
            var totalStr = isRateIncluded ? '<span class="text-info">0원 (포함)</span>' : Number(svc.totalPrice || 0).toLocaleString('ko-KR') + '원';
            var dateStr = svc.serviceDate ? svc.serviceDate : '';

            var deleteBtn = '';
            if (!self.isReadonly && !legTerminated) {
                deleteBtn = '<button class="btn btn-outline-danger btn-sm py-0 px-1 remove-service-btn" '
                    + 'data-leg-id="' + legId + '" data-service-id="' + svc.id + '"'
                    + (isRateIncluded ? ' data-rate-included="true"' : '')
                    + ' aria-label="서비스 삭제: ' + HolaPms.escapeHtml(name) + '">'
                    + '<i class="fas fa-times"></i></button>';
            }

            html += '<tr>'
                + '<td class="small">' + HolaPms.escapeHtml(name) + badgeHtml + (dateStr ? ' <span class="text-muted">(' + dateStr + ')</span>' : '') + '</td>'
                + '<td class="small text-end text-muted">' + qtyPrice + '</td>'
                + '<td class="small text-end text-muted">' + taxStr + '</td>'
                + '<td class="small text-end">' + totalStr + '</td>'
                + '<td class="text-end" style="width:30px;">' + deleteBtn + '</td>'
                + '</tr>';
        });
        html += '</table>';
        return html;
    },

    /**
     * 업그레이드 이력 조회 및 렌더링
     */
    loadUpgradeHistory: function(seq, legId) {
        var self = this;
        var $container = $('#upgradeHistory_' + seq);
        if (!$container.length) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + legId + '/upgrade/history',
            type: 'GET',
            success: function(res) {
                var history = (res.data || []);
                if (history.length === 0) return;

                var typeBadge = function(type) {
                    if (type === 'COMPLIMENTARY') return '<span class="badge bg-success">무료</span>';
                    if (type === 'UPSELL') return '<span class="badge bg-info">업셀</span>';
                    return '<span class="badge bg-primary">유료</span>';
                };

                var html = '<hr class="my-2">';
                html += '<span class="text-muted small"><i class="fas fa-arrow-up me-1"></i>업그레이드 이력 (' + history.length + ')</span>';
                html += '<table class="table table-sm table-borderless mb-0 mt-1">';
                history.forEach(function(h) {
                    var diff = Number(h.priceDifference || 0).toLocaleString('ko-KR');
                    var dateStr = h.upgradedAt ? h.upgradedAt.replace('T', ' ').substring(0, 16) : '';
                    html += '<tr>'
                        + '<td class="small">' + typeBadge(h.upgradeType) + '</td>'
                        + '<td class="small">' + HolaPms.escapeHtml(h.fromRoomTypeName || '') + ' → ' + HolaPms.escapeHtml(h.toRoomTypeName || '') + '</td>'
                        + '<td class="small text-end">' + (h.priceDifference > 0 ? '+' : '') + diff + '원</td>'
                        + '<td class="small text-muted text-end">' + dateStr + '</td>'
                        + '</tr>';
                });
                html += '</table>';
                $container.html(html);
            },
            error: function() {
                // 업그레이드 이력 API 미구현 시 무시
            }
        });
    },

    /**
     * 서비스 옵션 드롭다운 구성
     */
    populateServiceOptions: function(seq) {
        var self = this;
        var $form = $('#serviceAddForm_' + seq);
        var $select = $form.find('.service-option-select');
        $select.find('option:not(:first)').remove();

        // 해당 레그의 roomTypeId 조회
        var $leg = $('#roomLeg_' + seq);
        var roomTypeId = $leg.find('.room-type-id').val() || null;

        // 로딩 표시
        $select.prop('disabled', true);
        $select.find('option:first').text('로딩중...');

        self.loadPaidServiceOptions(roomTypeId, function(options) {
            $select.prop('disabled', false);
            $select.find('option:first').text('서비스 선택');
            if (options && options.length > 0) {
                options.forEach(function(opt) {
                    var price = Number(opt.vatIncludedPrice || 0).toLocaleString('ko-KR');
                    $select.append('<option value="' + opt.id + '">'
                        + HolaPms.escapeHtml(opt.serviceNameKo) + ' (' + price + '원)'
                        + '</option>');
                });
            } else {
                $select.find('option:first').text('등록된 서비스가 없습니다');
            }
        });
    },

    /**
     * 유료 서비스 추가 API 호출
     */
    addServiceToLeg: function(legSeq, legId) {
        var self = this;

        // legId 미존재 시 카드 DOM에서 재추출
        if (!legId) {
            legId = $('#roomLeg_' + legSeq).data('leg-id');
        }
        if (!legId) {
            HolaPms.alert('warning', '객실을 먼저 저장한 후 서비스를 추가해주세요.');
            return;
        }

        var $form = $('#serviceAddForm_' + legSeq);
        var serviceOptionId = $form.find('.service-option-select').val();
        var quantity = parseInt($form.find('.service-qty').val()) || 1;
        var serviceDate = $form.find('.service-date').val() || null;

        if (!serviceOptionId) {
            HolaPms.alert('warning', '서비스를 선택해주세요.');
            return;
        }

        // 서비스 일자가 체크인~체크아웃 범위 내인지 검증
        if (serviceDate) {
            var $dateInput = $form.find('.service-date');
            var minDate = $dateInput.attr('min');
            var maxDate = $dateInput.attr('max');
            if (minDate && serviceDate < minDate) {
                HolaPms.alert('warning', '서비스 일자는 체크인일(' + minDate + ') 이후여야 합니다.');
                return;
            }
            if (maxDate && serviceDate > maxDate) {
                HolaPms.alert('warning', '서비스 일자는 체크아웃일(' + maxDate + ') 이내여야 합니다.');
                return;
            }
        }

        var requestData = {
            serviceOptionId: parseInt(serviceOptionId),
            quantity: quantity,
            serviceDate: serviceDate
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/legs/' + legId + '/services',
            type: 'POST',
            data: requestData,
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '서비스가 추가되었습니다.');
                    $form.addClass('d-none');
                    // 예약 데이터 재로드 (결제 탭 갱신 포함)
                    self.loadData();
                }
            }
        });
    },

    /**
     * 유료 서비스 삭제 API 호출
     */
    removeServiceFromLeg: function(legId, serviceId, isRateIncluded) {
        var self = this;
        if (!legId || !serviceId) {
            HolaPms.alert('error', '서비스 정보를 찾을 수 없습니다. 페이지를 새로고침해주세요.');
            return;
        }
        var msg = isRateIncluded
            ? '이 서비스는 레이트 플랜에 포함된 서비스입니다. 삭제하시겠습니까?'
            : '이 서비스를 삭제하시겠습니까?';

        HolaPms.confirm(msg, function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/legs/' + legId + '/services/' + serviceId,
                type: 'DELETE',
                success: function(res) {
                    if (res.success) {
                        HolaPms.alert('success', '서비스가 삭제되었습니다.');
                        // 예약 데이터 재로드 (결제 탭 갱신 포함)
                        self.loadData();
                    }
                }
            });
        });
    },

    /**
     * 예치금 바인딩
     */
    bindDeposit: function(deposits) {
        if (!deposits || deposits.length === 0) return;

        // 첫 번째 예치금 정보로 바인딩
        var dep = deposits[0];
        $('#depositId').val(dep.id || '');
        if (dep.depositMethod) {
            $('#depositMethod').val(dep.depositMethod);
            if (dep.depositMethod === 'CREDIT_CARD') {
                $('#creditCardSection').show();
                $('#cardCompany').val(dep.cardCompany || '');
                $('#cardNumber').val(dep.cardNumberMasked || '');
                $('#cardExpiryDate').val(dep.cardExpiryDate || '');
            }
        }
        if (dep.amount) $('#depositAmount').val(dep.amount);
    },

    /**
     * 상태별 필드 제어 (readonly/disabled)
     */
    applyFieldControl: function() {
        var self = this;

        // 삭제 버튼: SUPER_ADMIN + CHECKED_OUT일 때만 표시
        var status = self.reservationData ? self.reservationData.reservationStatus : '';
        if (status === 'CHECKED_OUT' && HolaPms.context.userRole === 'SUPER_ADMIN') {
            $('#deleteReservationBtn').removeClass('d-none');
        } else {
            $('#deleteReservationBtn').addClass('d-none');
        }

        if (self.isReadonly) {
            // VIEW 전용 모드: 모든 input/select/textarea disabled, 버튼 숨김
            $('#formContainer input, #formContainer select, #formContainer textarea').prop('disabled', true);
            $('#saveBtn, #addRoomBtn, #addMemoBtn, #vanCardPaymentBtn, #vanCashPaymentBtn, #cashPaymentBtn, #addAdjustmentBtn').hide();
            $('.leg-card-pay-btn, .leg-payment-buttons').hide();
            $('#statusChangeGroup').hide();
            $('.remove-leg-btn, .room-type-search-btn, .room-assign-btn, .room-type-clear-btn, .room-assign-clear-btn').hide();
            $('#rateCodeSearchBtn, #marketCodeSearchBtn, #rateCodeClearBtn, #marketCodeClearBtn').hide();
            $('.add-service-btn, .remove-service-btn').hide();
            return;
        }

        if (self.isOta) {
            // OTA 모드: OTA 제한 필드만 disabled
            var otaFields = [
                '#guestNameKo', '#guestLastNameEn', '#guestFirstNameEn',
                '#phoneCountryCode', '#phoneNumber', '#email', '#birthDate',
                '#gender', '#nationality',
                '#masterCheckIn', '#masterCheckOut',
                '#customerRequest'
            ];
            otaFields.forEach(function(sel) {
                $(sel).prop('disabled', true);
            });
            // OTA 모드: 서비스 추가/삭제는 허용 (호텔 부대시설 요금)
        }

        // CHECK_IN / INHOUSE: 일부 수정만 허용
        if (status === 'CHECK_IN' || status === 'INHOUSE') {
            // 마스터 체크인/체크아웃 비활성화
            $('#masterCheckIn').prop('disabled', true);
            $('#masterCheckOut').prop('disabled', true);
            // 서브 예약 날짜도 비활성화
            $('.leg-check-in, .leg-check-out').prop('disabled', true);
            // 예약자 기본정보 비활성화
            $('#guestNameKo').prop('disabled', true);
            $('#isOtaManaged').prop('disabled', true);
        }
    },

    /**
     * 상태 변경 메뉴 동적 구성
     */
    /**
     * 마스터 레벨 액션 메뉴 (전체 취소/노쇼 + 전체 일괄 버튼)
     * Leg별 체크인/투숙중/체크아웃은 Leg 카드에서 개별 처리
     */
    buildMasterActionMenu: function(currentStatus, activeLegCount) {
        var self = this;
        var $menu = $('#statusChangeMenu');
        $menu.empty();

        var items = [];

        // 전체 취소 (마스터가 종료 상태가 아닐 때)
        if (['RESERVED', 'CHECK_IN'].indexOf(currentStatus) !== -1) {
            items.push({ status: 'CANCELED', label: '전체 취소', icon: 'fa-ban', cls: 'btn-danger' });
        }
        // 전체 노쇼 (예약 상태일 때만)
        if (currentStatus === 'RESERVED') {
            items.push({ status: 'NO_SHOW', label: '전체 노쇼', icon: 'fa-user-slash', cls: 'btn-warning' });
        }

        if (items.length === 0) {
            $('#statusChangeGroup').hide();
            return;
        }

        $('#statusChangeGroup').show();
        items.forEach(function(t) {
            var $li = $('<li><a class="dropdown-item" href="javascript:void(0);">'
                + '<i class="fas ' + t.icon + ' me-1"></i>' + t.label
                + '</a></li>');
            $li.find('a').on('click', function() {
                self.confirmStatusChange(t.status, t.label);
            });
            $menu.append($li);
        });
    },

    /** @deprecated 기존 마스터 레벨 전이 메뉴 (Leg 독립 관리 전환 후 미사용) */
    buildStatusChangeMenu: function(currentStatus) {
        // Leg별 관리로 전환 — buildMasterActionMenu로 대체
        $('#statusChangeGroup').hide();
    },

    /**
     * 상태 변경 확인 모달
     * - CANCELED: 취소 수수료 미리보기 모달 표시 후 DELETE 엔드포인트 호출
     * - 기타 상태: 기존 확인 모달 → PUT /status 호출
     */
    confirmStatusChange: function(newStatus, label) {
        var self = this;

        // 취소/노쇼는 수수료 미리보기 모달로 분기
        if (newStatus === 'CANCELED') {
            self.showCancelPreview(false);
            return;
        }
        if (newStatus === 'NO_SHOW') {
            self.showCancelPreview(true);
            return;
        }

        // 체크아웃 시 결제 잔액 검증 → 잔액 있으면 결제정보 탭으로 이동
        if (newStatus === 'CHECKED_OUT') {
            var remaining = ReservationPayment.paymentData
                ? Number(ReservationPayment.paymentData.remainingAmount) || 0 : 0;
            if (remaining > 0) {
                HolaPms.alert('warning', '미결제 잔액이 있습니다. 결제정보 탭으로 이동합니다.');
                var $tabLink = $('a[href="#tabPayment"]');
                if ($tabLink.length) {
                    var tab = new bootstrap.Tab($tabLink[0]);
                    tab.show();
                }
                return;
            }
        }

        var statusInfo = self.STATUS_BADGE[newStatus] || { label: newStatus };
        $('#statusConfirmMessage').text('예약 상태를 "' + label + '"(으)로 변경하시겠습니까?');

        // 기존 이벤트 제거 후 바인딩
        $('#statusConfirmBtn').off('click').on('click', function() {
            HolaPms.modal.hide('#statusConfirmModal');
            self.changeStatus(newStatus);
        });

        HolaPms.modal.show('#statusConfirmModal');
    },

    /**
     * 취소/노쇼 수수료 미리보기 모달
     * @param isNoShow true이면 노쇼 정책 적용
     */
    showCancelPreview: function(isNoShow, legId) {
        var self = this;
        var url = '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/cancel-preview';
        var params = [];
        if (isNoShow) params.push('noShow=true');
        if (legId) params.push('subReservationId=' + legId);
        if (params.length > 0) url += '?' + params.join('&');

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    var d = res.data;
                    var fmt = function(v) { return Number(v || 0).toLocaleString('ko-KR') + '원'; };

                    // 모달 초기화 (이전 상태 잔류 방지)
                    $('#cpNonPgRefundRow').addClass('d-none');

                    $('#cancelPreviewTitle').text(isNoShow ? '노쇼 처리 확인' : '예약 취소 확인');
                    $('#cpReservationNo').text(d.subReservationNo || d.masterReservationNo);
                    $('#cpGuestName').text(d.guestNameKo);
                    $('#cpCheckInOut').text(d.checkIn + ' ~ ' + d.checkOut);
                    $('#cpFirstNight').text(fmt(d.firstNightTotal));
                    $('#cpPolicyDesc').text(d.policyDescription || '정책 미설정');
                    $('#cpCancelFeeAmt').text(fmt(d.cancelFeeAmount) + ' (' + (d.cancelFeePercent || 0) + '%)');
                    $('#cpTotalPaid').text(fmt(d.totalPaidAmount));
                    $('#cpRefundAmt').text(fmt(d.refundAmount));

                    // PG 결제 카드 정보 표시
                    if (d.pgPayment) {
                        var cardInfo = (d.pgIssuerName || '') + ' ' + (d.pgCardNo || '');
                        $('#cpPgCardInfo').text(cardInfo.trim() || '-');
                        $('#cpPgInfoRow').removeClass('d-none');
                    } else {
                        $('#cpPgInfoRow').addClass('d-none');
                    }

                    // 환불 분배 내역 표시
                    var $breakdownArea = $('#cpRefundBreakdowns');
                    $breakdownArea.empty();
                    if (d.refundBreakdowns && d.refundBreakdowns.length > 1) {
                        for (var i = 0; i < d.refundBreakdowns.length; i++) {
                            var bd = d.refundBreakdowns[i];
                            var methodLabel = bd.pgRefund
                                ? 'PG 환불' + (bd.cardInfo ? ' (' + bd.cardInfo + ')' : '')
                                : (bd.paymentMethod === 'CASH' ? '현금 환불' : '카드(VAN) 환불');
                            var badgeClass = bd.pgRefund ? 'text-primary' : 'text-danger';
                            $breakdownArea.append(
                                '<div class="d-flex justify-content-between small mt-1">' +
                                '<span class="' + badgeClass + '">' + methodLabel + '</span>' +
                                '<span class="fw-semibold">' + fmt(bd.refundAmount) + '</span></div>'
                            );
                        }
                        $breakdownArea.removeClass('d-none');
                    } else {
                        $breakdownArea.addClass('d-none');
                    }

                    // 노쇼: 체크인 날짜가 오늘 이후이면 처리 불가 (미도착이 확정되지 않음)
                    if (isNoShow && d.checkIn) {
                        var today = new Date();
                        today.setHours(0, 0, 0, 0);
                        var checkInDate = new Date(d.checkIn + 'T00:00:00');
                        if (checkInDate > today) {
                            $('#cpOutstandingRow').addClass('d-none');
                            $('#cpFeeAlert').addClass('d-none');
                            $('#cpRefundRow').addClass('d-none');
                            $('#cancelConfirmBtn').prop('disabled', true)
                                .html('<i class="fas fa-lock me-1"></i>체크인 전 노쇼 처리 불가');
                            HolaPms.modal.show('#cancelPreviewModal');
                            return;
                        }
                    }

                    // 미결제 잔액 표시 및 버튼 차단 (취소 시만, 노쇼는 제외)
                    var unpaidBalance = Number(d.unpaidBalance || 0);
                    if (!isNoShow && unpaidBalance > 0) {
                        $('#cpOutstandingRow').removeClass('d-none');
                        $('#cpOutstandingAmt').text(fmt(unpaidBalance));
                        $('#cpRefundRow').addClass('d-none');
                        $('#cpFeeAlert').removeClass('d-none')
                            .html('<i class="fas fa-exclamation-triangle me-1"></i>미결제 잔액 ' + fmt(unpaidBalance) + '을 먼저 결제해주세요. 결제정보 탭에서 결제 후 취소가 가능합니다.');
                        $('#cancelConfirmBtn').prop('disabled', true)
                            .html('<i class="fas fa-lock me-1"></i>미결제 잔액 결제 필요');
                        HolaPms.modal.show('#cancelPreviewModal');
                        return;
                    }

                    // 미결제 수수료 표시 및 버튼 차단
                    var outstanding = Number(d.outstandingCancelFee || 0);
                    if (outstanding > 0) {
                        $('#cpOutstandingRow').removeClass('d-none');
                        $('#cpOutstandingAmt').text(fmt(d.outstandingCancelFee));
                        $('#cpRefundRow').addClass('d-none');
                        $('#cpFeeAlert').removeClass('d-none')
                            .html('<i class="fas fa-exclamation-triangle me-1"></i>취소/노쇼 수수료가 미결제 상태입니다. 결제정보 탭에서 수수료를 결제한 후 처리해주세요.');
                        $('#cancelConfirmBtn').prop('disabled', true)
                            .html('<i class="fas fa-lock me-1"></i>수수료 결제 필요');
                    } else {
                        $('#cpOutstandingRow').addClass('d-none');
                        $('#cpRefundRow').removeClass('d-none');
                        $('#cpFeeAlert').addClass('d-none');

                        // 비-PG(현금/VAN) 환불 안내 표시
                        var nonPgAmt = Number(d.nonPgRefundAmount || 0);
                        if (nonPgAmt > 0) {
                            var nonPgMethodLabel = d.nonPgRefundMethod === 'CARD' ? '카드(VAN)' : '현금';
                            $('#cpNonPgRefundRow').removeClass('d-none');
                            $('#cpNonPgRefundInfo').html(
                                '<i class="fas fa-exclamation-circle text-warning me-1"></i>'
                                + nonPgMethodLabel + ' 환불 <strong>' + fmt(nonPgAmt) + '</strong>'
                                + ' -- 관리자 수동 환불 처리 필요'
                            );
                        } else {
                            $('#cpNonPgRefundRow').removeClass('d-none').addClass('d-none');
                        }

                        var btnLabel = isNoShow ? '노쇼 확인' : '취소 확인';
                        $('#cancelConfirmBtn').prop('disabled', false)
                            .html('<i class="fas fa-ban me-1"></i>' + btnLabel)
                            .off('click').on('click', function() {
                                // 비-PG 환불이 있으면 관리자 확인 alert 표시
                                var proceedWithCancel = function() {
                                    HolaPms.modal.hide('#cancelPreviewModal');
                                    if (legId) {
                                        var newStatus = isNoShow ? 'NO_SHOW' : 'CANCELED';
                                        self.changeStatus(newStatus, legId);
                                    } else if (isNoShow) {
                                        self.changeStatus('NO_SHOW');
                                    } else {
                                        self.executeCancel();
                                    }
                                };

                                if (nonPgAmt > 0) {
                                    var nonPgLabel = d.nonPgRefundMethod === 'CARD' ? '카드(VAN)' : '현금';
                                    var confirmMsg = nonPgLabel + ' 결제 ' + fmt(nonPgAmt)
                                        + ' 환불 처리가 필요합니다.\n환불 처리를 확인하셨습니까?';
                                    if (confirm(confirmMsg)) {
                                        proceedWithCancel();
                                    }
                                } else {
                                    proceedWithCancel();
                                }
                            });
                    }

                    HolaPms.modal.show('#cancelPreviewModal');
                }
            }
        });
    },

    /**
     * 예약 취소 실행 (DELETE 엔드포인트 - 수수료 계산 + REFUND 처리)
     */
    executeCancel: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId,
            type: 'DELETE',
            success: function(res) {
                if (res.success) {
                    HolaPms.popup.notifyParent('canceled', self.reservationId);
                    HolaPms.alert('success', '예약이 취소되었습니다.');
                    location.reload();
                }
            }
        });
    },

    /**
     * 상태 변경 API 호출
     */
    changeStatus: function(newStatus, subReservationId) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/status',
            type: 'PUT',
            data: { newStatus: newStatus, subReservationId: subReservationId || null },
            success: function(res) {
                if (res.success) {
                    HolaPms.popup.notifyParent('statusChanged', self.reservationId);
                    HolaPms.alert('success', '상태가 변경되었습니다.');
                    location.reload();
                }
            },
            error: function(xhr) {
                var res = xhr.responseJSON;
                var msg = res && res.message ? res.message : '처리 중 오류가 발생했습니다.';
                var code = res && res.code ? res.code : '';

                // 전제조건 에러: 해당 탭으로 자동 이동
                var codeTabMap = {
                    'HOLA-4029': '#tabPayment',  // 결제 잔액 → 결제정보 탭
                    'HOLA-5001': '#tabDetail',   // 객실 미배정 → 상세정보 탭
                    'HOLA-5002': '#tabDetail',   // 객실 미청소 → 상세정보 탭
                    'HOLA-5003': '#tabDetail',   // OOO 객실 → 상세정보 탭
                    'HOLA-5010': '#tabPayment'   // 미결제 잔액 → 결제정보 탭
                };
                var targetTab = codeTabMap[code];
                if (targetTab) {
                    HolaPms.alert('warning', msg);
                    var $tabLink = $('a[href="' + targetTab + '"]');
                    if ($tabLink.length) {
                        var tab = new bootstrap.Tab($tabLink[0]);
                        tab.show();
                    }
                } else {
                    HolaPms.alert('error', msg);
                }
            }
        });
    },

    /**
     * 이벤트 바인딩
     */
    bindEvents: function() {
        var self = this;

        // 프로퍼티 컨텍스트 변경
        $(document).on('hola:contextChange', function() {
            // 캐시 초기화
            self.allRoomTypes = null;
            self.assignData = null;
            self.reload();
        });

        // card-header 내 dropdown 버튼 클릭 시 collapse 토글 방지
        $(document).on('click', '.room-leg-card .card-header .dropdown-toggle', function(e) {
            e.stopPropagation();
        });

        // Leg별 상태 변경 (이벤트 위임)
        $(document).on('click', '.leg-status-change', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var legId = $(this).data('leg-id');
            var newStatus = $(this).data('new-status');
            var label = $(this).text().trim();

            // 취소/노쇼는 수수료 미리보기 모달로 분기 (결제 처리 포함 경로)
            if (newStatus === 'CANCELED') {
                self.showCancelPreview(false, legId);
                return;
            }
            if (newStatus === 'NO_SHOW') {
                self.showCancelPreview(true, legId);
                return;
            }

            // 체크아웃 시 Leg별 잔액 검증 (서버에서도 검증하지만, 사전 차단)
            if (newStatus === 'CHECKED_OUT') {
                var paymentData = (typeof ReservationPayment !== 'undefined') ? ReservationPayment.paymentData : null;
                if (paymentData && paymentData.legPayments) {
                    var legPayment = paymentData.legPayments.find(function(lp) { return lp.subReservationId == legId; });
                    var legRemaining = legPayment ? Number(legPayment.legRemaining) || 0 : -1;
                    if (legRemaining > 0) {
                        HolaPms.alert('warning', '해당 객실의 미결제 잔액 ' + legRemaining.toLocaleString() + '원이 있습니다. 결제정보 탭에서 잔액을 처리해주세요.');
                        var $tabLink = $('a[href="#tabPayment"]');
                        if ($tabLink.length) {
                            var tab = new bootstrap.Tab($tabLink[0]);
                            tab.show();
                        }
                        return;
                    }
                } else if (!paymentData) {
                    // 결제 데이터 미로드 시 경고 후 서버 검증에 위임
                    if (!confirm('결제 정보를 확인할 수 없습니다. 체크아웃을 진행하시겠습니까?')) return;
                }
            }

            if (!confirm(label + ' 처리하시겠습니까?')) return;
            self.changeStatus(newStatus, legId);
        });

        // 업그레이드 버튼 클릭
        $(document).on('click', '.upgrade-btn', function(e) {
            e.stopPropagation();
            var legId = $(this).data('leg-id');
            var roomTypeName = $(this).data('room-type-name');
            self.openUpgradeModal(legId, roomTypeName);
        });

        // 업그레이드 대상 객실타입 선택 시 미리보기
        $(document).on('change', '#upgradeRoomTypeId', function() {
            var toRoomTypeId = $(this).val();
            var legId = $('#upgradeLegId').val();
            if (toRoomTypeId && legId) {
                self.loadUpgradePreview(legId, toRoomTypeId);
            } else {
                $('#upgradePreviewArea').addClass('d-none');
            }
        });

        // 마스터 날짜 변경 시 단일 레그 날짜 자동 동기화
        $('#masterCheckIn').on('change', function() {
            var newVal = $(this).val();
            if (newVal && $('.room-leg-card').length === 1) {
                $('.leg-check-in').val(newVal);
            }
        });
        $('#masterCheckOut').on('change', function() {
            var newVal = $(this).val();
            if (newVal && $('.room-leg-card').length === 1) {
                $('.leg-check-out').val(newVal);
            }
        });

        // 레이트코드 검색/초기화 버튼
        $('#rateCodeSearchBtn').on('click', function() { self.openRateCodeModal(); });
        $('#rateCodeClearBtn').on('click', function() {
            $('#rateCodeId').val('');
            $('#rateCodeName').val('');
        });

        // 마켓코드 검색/초기화 버튼
        $('#marketCodeSearchBtn').on('click', function() { self.openMarketCodeModal(); });
        $('#marketCodeClearBtn').on('click', function() {
            $('#marketCodeId').val('');
            $('#marketCodeName').val('');
        });

        // 객실 추가 버튼
        $('#addRoomBtn').on('click', function() { self.addRoomLeg(null); });

        // 예치 방법 변경 → 카드 섹션 토글
        $('#depositMethod').on('change', function() {
            if ($(this).val() === 'CREDIT_CARD') {
                $('#creditCardSection').show();
            } else {
                $('#creditCardSection').hide();
            }
        });

        // 저장 버튼
        $('#saveBtn').on('click', function() { self.save(); });

        // 미저장 변경 감지: 저장 버튼이 필요한 폼 필드 변경 시 dirty 표시
        $(document).on('input change', '#tabReservation input, #tabReservation select, #tabReservation textarea, '
            + '.leg-check-in, .leg-check-out, .leg-adults, .leg-children, .room-type-name, .room-number-display, '
            + '#tabDeposit input, #tabDeposit select, #tabEtc textarea', function() {
            if (!self.isReadonly) self.markDirty();
        });

        // 창 닫기 시 미저장 경고
        $(window).on('beforeunload', function() {
            if (self.isDirty) return '저장하지 않은 변경사항이 있습니다.';
        });

        // 예약 삭제 버튼 (SUPER_ADMIN + CHECKED_OUT)
        $('#deleteReservationBtn').on('click', function() { self.deleteReservation(); });

        // 메모 등록 버튼
        $('#addMemoBtn').on('click', function() { self.addMemo(); });
        $('#newMemoContent').on('keypress', function(e) {
            if (e.which === 13) self.addMemo();
        });

        // 레이트코드 모달 검색
        $('#rateCodeSearchAction').on('click', function() { self.searchRateCode(); });
        $('#rateCodeSearchKeyword').on('keypress', function(e) { if (e.which === 13) self.searchRateCode(); });
        $('#rateCodeApplyBtn').on('click', function() { self.applyRateCode(); });

        // 마켓코드 모달 검색
        $('#marketCodeSearchAction').on('click', function() { self.searchMarketCode(); });
        $('#marketCodeSearchKeyword').on('keypress', function(e) { if (e.which === 13) self.searchMarketCode(); });
        $('#marketCodeApplyBtn').on('click', function() { self.applyMarketCode(); });

        // 객실타입 모달 검색 및 필터
        $('#roomTypeSearchAction').on('click', function() { self.filterRoomTypeList(); });
        $('#roomTypeSearchKeyword').on('keypress', function(e) { if (e.which === 13) self.filterRoomTypeList(); });
        $('#roomTypeApplyBtn').on('click', function() { self.applyRoomType(); });
        $('#showAllRoomTypes').on('change', function() { self.filterRoomTypeList(); });

        // 객실 배정 모달 탭 전환
        $(document).on('click', '#roomAssignModal [data-assign-tab]', function() {
            var tab = $(this).data('assign-tab');
            $('#roomAssignModal [data-assign-tab]').removeClass('active');
            $(this).addClass('active');
            self.renderAssignContent(tab);
        });
        // 객실 배정 적용
        $('#btnApplyRoomAssign').on('click', function() { self.applyRoomAssign(); });

        // 객실 레그 삭제 (이벤트 위임)
        $(document).on('click', '.remove-leg-btn', function(e) {
            e.stopPropagation();
            var $btn = $(this);
            var legSeq = $btn.data('leg');
            var legId = $btn.data('leg-id');

            // 마지막 활성 객실 삭제 방지 (최소 1개 필수, 종료 상태 레그 제외)
            var activeLegCount = $('.room-leg-card').filter(function() {
                var t = $(this).data('leg-terminated');
                return t !== true && t !== 'true';
            }).length;
            if (activeLegCount <= 1) {
                HolaPms.alert('warning', '마지막 객실은 삭제할 수 없습니다. 최소 1개의 객실이 필요합니다.');
                return;
            }

            if (legId) {
                // 서버에 존재하는 레그 → API 삭제
                HolaPms.confirm('이 객실을 삭제하시겠습니까?', function() {
                    $btn.prop('disabled', true);
                    HolaPms.ajax({
                        url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/legs/' + legId,
                        type: 'DELETE',
                        success: function(res) {
                            if (res.success) {
                                $('#roomLeg_' + legSeq).fadeOut(200, function() {
                                    $(this).remove();
                                    self.updateRoomLegsEmpty();
                                });
                                HolaPms.alert('success', '객실이 삭제되었습니다.');
                            } else {
                                $btn.prop('disabled', false);
                            }
                        },
                        error: function() {
                            $btn.prop('disabled', false);
                        }
                    });
                });
            } else {
                // 신규 추가 레그 → 단순 DOM 제거
                $('#roomLeg_' + legSeq).fadeOut(200, function() {
                    $(this).remove();
                    self.updateRoomLegsEmpty();
                });
            }
        });

        // 객실 레그 - 객실타입 검색 (이벤트 위임)
        $(document).on('click', '.room-type-search-btn', function() {
            var legSeq = $(this).closest('.room-leg-card').data('leg-seq');
            self.openRoomTypeModal(legSeq);
        });

        // 객실 레그 - 층/호수 검색 (이벤트 위임)
        $(document).on('click', '.room-assign-btn', function() {
            var legSeq = $(this).closest('.room-leg-card').data('leg-seq');
            self.openRoomAssignModal(legSeq);
        });

        // 객실타입 초기화 (이벤트 위임)
        $(document).on('click', '.room-type-clear-btn', function() {
            var $leg = $(this).closest('.room-leg-card');
            $leg.find('.room-type-id').val('');
            $leg.find('.room-type-name').val('');
            // 객실타입 초기화 → 서비스 드롭다운 전체 목록으로 갱신
            var legSeq = $leg.data('leg-seq');
            self.populateServiceOptions(legSeq);
        });

        // 층/호수 초기화 (이벤트 위임)
        $(document).on('click', '.room-assign-clear-btn', function() {
            var $leg = $(this).closest('.room-leg-card');
            $leg.find('.floor-id').val('');
            $leg.find('.room-number-id').val('');
            $leg.find('.room-number-display').val('');
        });

        // 얼리체크인 토글 버튼
        $(document).on('click', '.early-checkin-toggle:not([disabled])', function() {
            var $btn = $(this);
            var $group = $btn.closest('.btn-group');
            var val = $btn.data('value');
            var $legCard = $btn.closest('.room-leg-card');
            self.applyToggle($group, $btn, '.leg-early-checkin', val);
            if (val === true || val === 'true') {
                self.showTimeWindowOptions($legCard, 'EARLY_CHECKIN');
            } else {
                self.removeEarlyLateFee($legCard, 'EARLY_CHECKIN', function() {
                    self.showEarlyLateConfirmedFee($legCard);
                });
            }
            self.markDirty();
        });

        // 레이트체크아웃 토글 버튼
        $(document).on('click', '.late-checkout-toggle:not([disabled])', function() {
            var $btn = $(this);
            var $group = $btn.closest('.btn-group');
            var val = $btn.data('value');
            var $legCard = $btn.closest('.room-leg-card');
            self.applyToggle($group, $btn, '.leg-late-checkout', val);
            if (val === true || val === 'true') {
                self.showTimeWindowOptions($legCard, 'LATE_CHECKOUT');
            } else {
                self.removeEarlyLateFee($legCard, 'LATE_CHECKOUT', function() {
                    self.showEarlyLateConfirmedFee($legCard);
                });
            }
            self.markDirty();
        });

        // 얼리/레이트 시간대 선택 버튼 클릭 (이벤트 위임)
        $(document).on('click', '.early-late-select-btn', function() {
            var $btn = $(this);
            var $legCard = $btn.closest('.room-leg-card');
            var policyType = $btn.data('policy-type');
            var policyIndex = $btn.data('policy-index');
            self.registerEarlyLateFee($legCard, policyType, policyIndex);
        });

        // 유료 서비스 추가 폼 토글 (이벤트 위임)
        $(document).on('click', '.add-service-btn', function() {
            var legSeq = $(this).data('leg');
            var $form = $('#serviceAddForm_' + legSeq);
            $form.toggleClass('d-none');
            if (!$form.hasClass('d-none')) {
                // 드롭다운 재구성
                self.populateServiceOptions(legSeq);
                $form.find('.service-option-select').val('');
                $form.find('.service-qty').val(1);
                $form.find('.service-date').val('');
                // 서비스 날짜 범위를 현재 Leg의 체크인~체크아웃 당일까지 허용
                // (체크아웃 당일 조식 등 관리자 재량 서비스 추가 가능)
                var $card = $form.closest('.room-leg-card');
                var checkIn = $card.find('.leg-check-in').val();
                var checkOut = $card.find('.leg-check-out').val();
                var $dateInput = $form.find('.service-date');
                if (checkIn) $dateInput.attr('min', checkIn);
                if (checkOut) $dateInput.attr('max', checkOut);
            }
        });

        // 유료 서비스 추가 취소 (이벤트 위임)
        $(document).on('click', '.cancel-add-service-btn', function() {
            var legSeq = $(this).data('leg');
            $('#serviceAddForm_' + legSeq).addClass('d-none');
        });

        // 유료 서비스 추가 확인 (이벤트 위임)
        $(document).on('click', '.confirm-add-service-btn', function() {
            var legSeq = $(this).data('leg');
            var legId = $(this).data('leg-id');
            self.addServiceToLeg(legSeq, legId);
        });

        // 유료 서비스 삭제 (이벤트 위임)
        $(document).on('click', '.remove-service-btn', function() {
            var legId = $(this).data('leg-id');
            var serviceId = $(this).data('service-id');
            var isRateIncluded = $(this).data('rate-included') === true;
            self.removeServiceFromLeg(legId, serviceId, isRateIncluded);
        });

        // 카드결제 버튼 (글로벌 - 싱글레그): VAN 모듈 연동 전 안내
        // VAN 카드결제 버튼
        $('#vanCardPaymentBtn').on('click', function() {
            ReservationPayment.openVanCardPaymentModal();
        });

        // VAN 현금결제 버튼
        $('#vanCashPaymentBtn').on('click', function() {
            ReservationPayment.openVanCashPaymentModal();
        });

        // 인보이스 출력 버튼
        $('#printInvoiceBtn').on('click', function() {
            ReservationPayment.printInvoice();
        });

        // 수동 현금결제 버튼 (글로벌 - 싱글레그)
        $('#cashPaymentBtn').on('click', function() {
            ReservationPayment.openCashPaymentModal();
        });

        // Per-Leg 결제 버튼 (멀티레그 - 이벤트 위임)
        $(document).off('click.legPay').on('click.legPay', '.leg-card-pay-btn', function() {
            var $btn = $(this);
            var legContext = {
                subId: $btn.data('sub-id'),
                legIndex: $btn.data('leg-index'),
                legLabel: $btn.data('leg-label'),
                legTotal: $btn.data('leg-total'),
                legPaid: $btn.data('leg-paid'),
                legRemaining: $btn.data('leg-remaining')
            };
            var method = $btn.data('pay-method');

            if (method === 'card') {
                ReservationPayment.openVanCardPaymentModal(legContext);
            } else if (method === 'cash') {
                ReservationPayment.openVanCashPaymentModal(legContext);
            } else if (method === 'manual-cash') {
                ReservationPayment.openCashPaymentModal(legContext);
            }
        });

        // 현금결제 확인 버튼
        $('#cashPaymentConfirmBtn').on('click', function() {
            ReservationPayment.processCashPayment();
        });

        // 금액 조정 추가
        $('#addAdjustmentBtn').on('click', function() {
            ReservationPayment.showAdjustmentForm();
        });
    },

    // ═══════════════════════════════════════════
    // 레이트코드 모달
    // ═══════════════════════════════════════════

    openRateCodeModal: function() {
        var self = this;
        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }
        $('#rateCodeSearchKeyword').val('');

        if (self.rateCodeTable) {
            self.rateCodeTable.ajax.url('/api/v1/properties/' + self.propertyId + '/rate-codes').load();
        } else {
            self.rateCodeTable = $('#rateCodeSelectTable').DataTable({
                processing: true, serverSide: false, paging: true, pageLength: 10,
                ordering: false, searching: true, dom: 'rtip', info: false,
                language: HolaPms.dataTableLanguage,
                ajax: {
                    url: '/api/v1/properties/' + self.propertyId + '/rate-codes',
                    dataSrc: function(json) {
                        return (json.data || []).filter(function(item) { return item.useYn === true; });
                    },
                    error: function(xhr) { HolaPms.handleAjaxError(xhr); }
                },
                columns: [
                    { data: null, className: 'text-center', render: function(d, t, r, m) { return m.row + 1; } },
                    { data: 'rateCode', render: function(d) { return HolaPms.escapeHtml(d); } },
                    { data: 'rateNameKo', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    { data: 'rateCategory', className: 'text-center', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    {
                        data: null, className: 'text-center',
                        render: function(d) {
                            return HolaPms.escapeHtml((d.saleStartDate || '-') + ' ~ ' + (d.saleEndDate || '-'));
                        }
                    },
                    {
                        data: null, className: 'text-center',
                        render: function(d, t, row) {
                            return '<input type="radio" name="rateCodeSelect" value="' + row.id
                                + '" data-code="' + HolaPms.escapeHtml(row.rateCode || '')
                                + '" data-name="' + HolaPms.escapeHtml(row.rateNameKo || '')
                                + '" aria-label="선택: ' + HolaPms.escapeHtml(row.rateCode || '') + '">';
                        }
                    }
                ]
            });
        }
        HolaPms.modal.show('#rateCodeModal');
    },

    searchRateCode: function() {
        if (!this.rateCodeTable) return;
        this.rateCodeTable.search($.trim($('#rateCodeSearchKeyword').val())).draw();
    },

    applyRateCode: function() {
        var self = this;
        var selected = $('input[name="rateCodeSelect"]:checked');
        if (!selected.length) { HolaPms.alert('warning', '레이트코드를 선택해주세요.'); return; }

        var newRateCodeId = parseInt(selected.val());
        var origRateCodeId = self.reservationData ? self.reservationData.rateCodeId : null;

        // 동일 레이트코드 선택 시 바로 적용
        if (!origRateCodeId || newRateCodeId === origRateCodeId || !self.reservationId) {
            $('#rateCodeId').val(selected.val());
            $('#rateCodeName').val(selected.data('code') + ' - ' + selected.data('name'));
            HolaPms.modal.hide('#rateCodeModal');
            return;
        }

        // 변경된 레이트코드 → 차액 미리보기 API 호출
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                + '/rate-change-preview?newRateCodeId=' + newRateCodeId,
            type: 'GET',
            success: function(res) {
                if (!res.success || !res.data) {
                    // API 실패 시 단순 confirm
                    if (confirm('레이트코드를 변경하시겠습니까?')) {
                        $('#rateCodeId').val(selected.val());
                        $('#rateCodeName').val(selected.data('code') + ' - ' + selected.data('name'));
                    }
                    HolaPms.modal.hide('#rateCodeModal');
                    return;
                }

                var preview = res.data;
                var fmt = function(v) { return Number(v || 0).toLocaleString('ko-KR'); };
                var diffSign = preview.difference >= 0 ? '+' : '';

                var msg = '레이트코드 변경 요금 미리보기\n\n'
                    + '  현재: ' + (preview.currentRateCodeName || '-') + '\n'
                    + '  변경: ' + (preview.newRateCodeName || '-') + '\n\n'
                    + '  현재 요금: ₩' + fmt(preview.currentTotal) + '\n'
                    + '  변경 요금: ₩' + fmt(preview.newTotal)
                    + ' (' + diffSign + '₩' + fmt(preview.difference) + ')\n\n'
                    + '적용하시겠습니까?';

                if (confirm(msg)) {
                    $('#rateCodeId').val(selected.val());
                    $('#rateCodeName').val(selected.data('code') + ' - ' + selected.data('name'));
                }
                HolaPms.modal.hide('#rateCodeModal');
            },
            error: function() {
                // 미리보기 실패 시 사용자 확인 후 적용
                if (confirm('요금 미리보기를 불러올 수 없습니다.\n그래도 레이트코드를 변경하시겠습니까?')) {
                    $('#rateCodeId').val(selected.val());
                    $('#rateCodeName').val(selected.data('code') + ' - ' + selected.data('name'));
                }
                HolaPms.modal.hide('#rateCodeModal');
            }
        });
    },

    // ═══════════════════════════════════════════
    // 마켓코드 모달
    // ═══════════════════════════════════════════

    openMarketCodeModal: function() {
        var self = this;
        if (!self.propertyId) { HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.'); return; }
        $('#marketCodeSearchKeyword').val('');

        if (self.marketCodeTable) {
            self.marketCodeTable.ajax.url('/api/v1/properties/' + self.propertyId + '/market-codes').load();
        } else {
            self.marketCodeTable = $('#marketCodeSelectTable').DataTable({
                processing: true, serverSide: false, paging: true, pageLength: 10,
                ordering: false, searching: true, dom: 'rtip', info: false,
                language: HolaPms.dataTableLanguage,
                ajax: {
                    url: '/api/v1/properties/' + self.propertyId + '/market-codes',
                    dataSrc: function(json) {
                        return (json.data || []).filter(function(item) { return item.useYn === true; });
                    },
                    error: function(xhr) { HolaPms.handleAjaxError(xhr); }
                },
                columns: [
                    { data: null, className: 'text-center', render: function(d, t, r, m) { return m.row + 1; } },
                    { data: 'marketCode', render: function(d) { return HolaPms.escapeHtml(d); } },
                    { data: 'marketName', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    { data: 'descriptionKo', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    {
                        data: null, className: 'text-center',
                        render: function(d, t, row) {
                            return '<input type="radio" name="marketCodeSelect" value="' + row.id
                                + '" data-code="' + HolaPms.escapeHtml(row.marketCode || '')
                                + '" data-name="' + HolaPms.escapeHtml(row.marketName || '')
                                + '" aria-label="선택: ' + HolaPms.escapeHtml(row.marketCode || '') + '">';
                        }
                    }
                ]
            });
        }
        HolaPms.modal.show('#marketCodeModal');
    },

    searchMarketCode: function() {
        if (!this.marketCodeTable) return;
        this.marketCodeTable.search($.trim($('#marketCodeSearchKeyword').val())).draw();
    },

    applyMarketCode: function() {
        var selected = $('input[name="marketCodeSelect"]:checked');
        if (!selected.length) { HolaPms.alert('warning', '마켓코드를 선택해주세요.'); return; }
        $('#marketCodeId').val(selected.val());
        $('#marketCodeName').val(selected.data('code') + ' - ' + selected.data('name'));
        HolaPms.modal.hide('#marketCodeModal');
    },

    // ═══════════════════════════════════════════
    // 객실타입 모달
    // ═══════════════════════════════════════════

    openRoomTypeModal: function(legSeq) {
        var self = this;
        if (!self.propertyId) { HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.'); return; }
        self.currentLegSeq = legSeq;
        $('#roomTypeSearchKeyword').val('');
        $('#showAllRoomTypes').prop('checked', false);
        $('#roomTypeContent').html('<div class="text-center py-4 text-muted"><i class="fas fa-spinner fa-spin me-1"></i> 객실타입 정보를 불러오는 중...</div>');

        // 레이트코드에 매핑된 객실타입 ID 목록 조회
        var rateCodeId = $('#rateCodeId').val();
        self.rateCodeMappedRoomTypeIds = [];

        // 객실타입 전체 목록 조회
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-types',
            success: function(res) {
                self.allRoomTypes = (res.data || []).filter(function(item) { return item.useYn === true; });

                if (rateCodeId) {
                    // 레이트코드 상세에서 매핑된 roomTypeIds 조회
                    HolaPms.ajax({
                        url: '/api/v1/properties/' + self.propertyId + '/rate-codes/' + rateCodeId,
                        success: function(rateRes) {
                            self.rateCodeMappedRoomTypeIds = (rateRes.data && rateRes.data.roomTypeIds) || [];
                            self.filterRoomTypeList();
                        },
                        error: function() {
                            self.filterRoomTypeList();
                        }
                    });
                } else {
                    self.filterRoomTypeList();
                }
            },
            error: function() {
                $('#roomTypeContent').html('<div class="text-center py-4 text-danger">객실타입 정보를 불러오지 못했습니다.</div>');
            }
        });

        HolaPms.modal.show('#roomTypeModal');
    },

    /**
     * 객실타입 목록 필터링 및 렌더링
     */
    filterRoomTypeList: function() {
        var self = this;
        if (!self.allRoomTypes) return;

        var showAll = $('#showAllRoomTypes').is(':checked');
        var keyword = $.trim($('#roomTypeSearchKeyword').val()).toLowerCase();
        var mapped = self.rateCodeMappedRoomTypeIds || [];
        var hasMapped = mapped.length > 0;

        // 필터링
        var items = self.allRoomTypes.filter(function(rt) {
            // 키워드 필터
            if (keyword) {
                var match = (rt.roomTypeCode || '').toLowerCase().indexOf(keyword) >= 0
                    || (rt.roomClassName || '').toLowerCase().indexOf(keyword) >= 0
                    || (rt.description || '').toLowerCase().indexOf(keyword) >= 0;
                if (!match) return false;
            }
            // 레이트코드 매핑 필터 (전체 보기가 아닐 때)
            if (hasMapped && !showAll) {
                return mapped.indexOf(rt.id) >= 0;
            }
            return true;
        });

        if (items.length === 0) {
            $('#roomTypeContent').html('<div class="text-center py-4 text-muted">표시할 객실타입이 없습니다.</div>');
            return;
        }

        var html = '<div class="table-responsive">';
        html += '<table class="table table-hover mb-0" style="min-width:650px">';
        html += '<thead class="table-light"><tr>';
        html += '<th style="width:50px" class="text-center">NO</th>';
        html += '<th style="min-width:90px">객실타입</th>';
        html += '<th style="min-width:90px">객실 클래스</th>';
        html += '<th class="text-center" style="min-width:70px">객실크기</th>';
        html += '<th class="text-center" style="min-width:100px">최대인원</th>';
        html += '<th style="min-width:100px">설명</th>';
        if (hasMapped) html += '<th class="text-center" style="width:60px">매핑</th>';
        html += '<th class="text-center" style="width:60px">선택</th>';
        html += '</tr></thead><tbody>';

        items.forEach(function(rt, idx) {
            var isMapped = hasMapped && mapped.indexOf(rt.id) >= 0;
            html += '<tr' + (isMapped ? '' : ' class="table-light"') + '>';
            html += '<td class="text-center">' + (idx + 1) + '</td>';
            html += '<td>' + HolaPms.escapeHtml(rt.roomTypeCode) + '</td>';
            html += '<td>' + HolaPms.escapeHtml(rt.roomClassName || '-') + '</td>';
            html += '<td class="text-center">' + (rt.roomSize ? rt.roomSize + ' m\u00B2' : '-') + '</td>';
            html += '<td class="text-center">성인 ' + (rt.maxAdults || 0) + ' / 아동 ' + (rt.maxChildren || 0) + '</td>';
            html += '<td><small class="text-muted">' + HolaPms.escapeHtml(rt.description || '-') + '</small></td>';
            if (hasMapped) {
                html += '<td class="text-center">' + (isMapped ? '<span class="badge bg-primary">매핑</span>' : '-') + '</td>';
            }
            html += '<td class="text-center"><input type="radio" name="roomTypeSelect" value="' + rt.id
                + '" data-code="' + HolaPms.escapeHtml(rt.roomTypeCode || '')
                + '" aria-label="선택: ' + HolaPms.escapeHtml(rt.roomTypeCode || '') + '"></td>';
            html += '</tr>';
        });

        html += '</tbody></table>';
        html += '</div>';

        if (hasMapped && !showAll) {
            html += '<div class="text-center py-2"><small class="text-muted">레이트코드에 매핑된 객실타입만 표시 중. 전체 보려면 "전체 객실타입 보기"를 체크하세요.</small></div>';
        }

        $('#roomTypeContent').html(html);
    },

    applyRoomType: function() {
        var self = this;
        var selected = $('input[name="roomTypeSelect"]:checked');
        if (!selected.length) { HolaPms.alert('warning', '객실타입을 선택해주세요.'); return; }

        var $leg = $('#roomLeg_' + self.currentLegSeq);
        $leg.find('.room-type-id').val(selected.val());
        $leg.find('.room-type-name').val(selected.data('code'));
        HolaPms.modal.hide('#roomTypeModal');

        // 객실타입 변경 → 서비스 옵션 드롭다운 갱신
        self.populateServiceOptions(self.currentLegSeq);
    },

    // ═══════════════════════════════════════════
    // 객실 배정 모달
    // ═══════════════════════════════════════════

    openRoomAssignModal: function(legSeq) {
        var self = this;
        if (!self.propertyId) { HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.'); return; }
        self.currentLegSeq = legSeq;

        var legCard = $('#roomLeg_' + legSeq);
        var propertyId = self.propertyId;
        var roomTypeId = legCard.find('.room-type-id').val();
        var rateCodeId = $('#rateCodeId').val();
        var checkIn = legCard.find('.leg-check-in').val();
        var checkOut = legCard.find('.leg-check-out').val();
        var adults = legCard.find('.leg-adults').val() || 1;
        var children = legCard.find('.leg-children').val() || 0;
        var excludeSubId = legCard.data('leg-id') || '';

        // 필수 파라미터 검증
        if (!rateCodeId) {
            HolaPms.alert('warning', '레이트코드를 먼저 선택해주세요.');
            return;
        }
        if (!roomTypeId) {
            HolaPms.alert('warning', '객실타입을 먼저 선택해주세요.');
            return;
        }
        if (!checkIn || !checkOut) {
            HolaPms.alert('warning', '체크인/체크아웃 날짜를 입력해주세요.');
            return;
        }

        // 하위 호환용 값 유지
        self.assignCheckIn = checkIn || '';
        self.assignCheckOut = checkOut || '';
        self.assignExcludeSubId = excludeSubId;

        // 탭 초기화 (추천 탭 활성)
        $('#roomAssignModal [data-assign-tab]').removeClass('active');
        $('#roomAssignModal [data-assign-tab="recommended"]').addClass('active');

        HolaPms.modal.show('#roomAssignModal');
        self.loadAssignAvailability(propertyId, roomTypeId, rateCodeId, checkIn, checkOut, adults, children, excludeSubId);
    },

    loadRoomNumbers: function(floorId) {
        var self = this;
        var $list = $('#roomNumberList');
        $list.html('<p class="text-muted text-center py-3">로딩 중...</p>');

        if (!floorId) {
            $list.html('<p class="text-muted text-center py-3">층을 먼저 선택하세요</p>');
            return;
        }

        // 체크인/체크아웃이 있으면 가용성 API 호출
        var url = '/api/v1/properties/' + self.propertyId + '/floors/' + floorId + '/room-numbers';
        if (self.assignCheckIn && self.assignCheckOut) {
            url += '/availability?checkIn=' + self.assignCheckIn + '&checkOut=' + self.assignCheckOut;
            if (self.assignExcludeSubId) url += '&excludeSubId=' + self.assignExcludeSubId;
        }

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                var rooms = res.data || [];
                if (rooms.length === 0) {
                    $list.html('<p class="text-muted text-center py-3">해당 층에 등록된 호수가 없습니다.</p>');
                    return;
                }

                var hasAvailability = rooms[0].hasOwnProperty('available');

                var html = '<table class="table table-sm table-hover mb-0">'
                    + '<thead><tr>'
                    + '<th style="width:40px"></th>'
                    + '<th>호수</th>'
                    + '<th>설명</th>'
                    + (hasAvailability ? '<th>상태</th><th>선 예약 번호</th>' : '')
                    + '</tr></thead><tbody>';

                rooms.forEach(function(r) {
                    var available = hasAvailability ? r.available : true;
                    var rowClass = available ? '' : ' class="table-secondary text-muted"';
                    var disabled = available ? '' : ' disabled';
                    var roomLabel = HolaPms.escapeHtml(r.roomNumber);
                    var descLabel = r.descriptionKo ? HolaPms.escapeHtml(r.descriptionKo) : '-';

                    html += '<tr' + rowClass + '>'
                        + '<td><input type="radio" name="roomNumberRadio" value="' + r.id + '"'
                        + ' data-room-number="' + HolaPms.escapeHtml(r.roomNumber) + '"'
                        + ' data-desc="' + (r.descriptionKo ? HolaPms.escapeHtml(r.descriptionKo) : '') + '"'
                        + disabled + '></td>'
                        + '<td>' + roomLabel + '</td>'
                        + '<td>' + descLabel + '</td>';

                    if (hasAvailability) {
                        if (available) {
                            html += '<td><span class="badge bg-success">가용</span></td><td>-</td>';
                        } else {
                            var statusBadge = ReservationDetail._roomStatusBadge(r.unavailableType, '불가');
                            var conflictInfo = '';
                            if (r.conflictReservationNo) {
                                conflictInfo = HolaPms.escapeHtml(r.conflictReservationNo);
                                if (r.conflictGuestName) conflictInfo += ' / ' + HolaPms.escapeHtml(r.conflictGuestName);
                                if (r.conflictCheckIn && r.conflictCheckOut) conflictInfo += '<br><small>' + r.conflictCheckIn + ' ~ ' + r.conflictCheckOut + '</small>';
                            }
                            html += '<td>' + statusBadge + '</td>'
                                + '<td><small>' + (conflictInfo || '-') + '</small></td>';
                        }
                    }

                    html += '</tr>';
                });

                html += '</tbody></table>';
                $list.html(html);
            }
        });
    },

    /**
     * 객실 배정 가용성 데이터 로드
     */
    loadAssignAvailability: function(propertyId, roomTypeId, rateCodeId, checkIn, checkOut, adults, children, excludeSubId) {
        var self = this;
        var params = {
            roomTypeId: roomTypeId,
            rateCodeId: rateCodeId,
            checkIn: checkIn,
            checkOut: checkOut,
            adults: adults,
            children: children
        };
        if (excludeSubId) params.excludeSubId = excludeSubId;

        $('#assignContent').html('<div class="text-center py-5 text-muted"><i class="fas fa-spinner fa-spin me-1"></i> 객실 정보를 불러오는 중...</div>');

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-assign/availability',
            data: params,
            success: function(res) {
                if (!res.data) {
                    $('#assignContent').html('<div class="text-center py-5 text-muted">객실 정보가 없습니다.</div>');
                    return;
                }
                self.assignData = res.data;
                // 요금 요약 표시
                var d = res.data;
                var formatPrice = function(amount) {
                    return amount != null ? Number(amount).toLocaleString('ko-KR') : '-';
                };
                $('#assignTotalPrice').text(formatPrice(d.currentLegTotalPrice) + ' ' + (d.currency || ''));
                $('#assignAvgNightly').text(formatPrice(d.currentAvgNightly) + ' ' + (d.currency || ''));
                $('#assignNights').text(d.nights + '박');

                self.renderAssignContent('recommended');
            },
            error: function() {
                $('#assignContent').html('<div class="text-center py-5 text-danger">객실 정보를 불러오지 못했습니다.</div>');
            }
        });
    },

    /**
     * 객실 배정 아코디언 카드 렌더링
     * @param filter 'recommended' 추천만, 'all' 전체
     */
    renderAssignContent: function(filter) {
        var self = this;
        var data = self.assignData;
        if (!data || !data.roomTypeGroups) return;

        var groups = data.roomTypeGroups;
        if (filter === 'recommended') {
            groups = groups.filter(function(g) { return g.recommended; });
        }

        if (groups.length === 0) {
            $('#assignContent').html('<div class="text-center py-5 text-muted">표시할 객실이 없습니다.</div>');
            return;
        }

        var formatPrice = function(amount) {
            return amount != null ? Number(amount).toLocaleString('ko-KR') : '-';
        };

        var html = '<div class="accordion" id="assignAccordion">';

        groups.forEach(function(group, idx) {
            var collapseId = 'assignCollapse' + idx;
            var isExpanded = group.recommended;

            // 차이 뱃지
            var diffBadge = '';
            if (group.totalPrice != null && data.currentLegTotalPrice != null) {
                var diff = group.priceDiff;
                if (diff > 0) {
                    diffBadge = '<span class="badge bg-danger ms-2">+' + formatPrice(diff) + '</span>';
                } else if (diff < 0) {
                    diffBadge = '<span class="badge bg-success ms-2">' + formatPrice(diff) + '</span>';
                } else {
                    diffBadge = '<span class="badge bg-secondary ms-2">동일</span>';
                }
            }

            var priceInfo = group.totalPrice != null
                ? '<span class="text-muted ms-3">총 ' + formatPrice(group.totalPrice) + ' / 1박 ' + formatPrice(group.avgNightly) + '</span>'
                : '<span class="text-muted ms-3">요금 미설정</span>';

            // 객실 스펙 정보
            var specInfo = '';
            if (group.roomSize) specInfo += group.roomSize + 'm\u00B2';
            if (group.maxAdults || group.maxChildren) {
                if (specInfo) specInfo += ' / ';
                specInfo += '성인 ' + (group.maxAdults || 0) + '명';
                if (group.maxChildren > 0) specInfo += ' 아동 ' + group.maxChildren + '명';
            }

            html += '<div class="accordion-item">';
            html += '<h2 class="accordion-header">';
            html += '<button class="accordion-button' + (isExpanded ? '' : ' collapsed') + '" type="button" data-bs-toggle="collapse" data-bs-target="#' + collapseId + '">';
            html += '<div class="d-flex flex-wrap align-items-center gap-2 w-100 me-2">';
            html += '<span>' + HolaPms.escapeHtml(group.roomClassName) + ' &gt; ' + HolaPms.escapeHtml(group.roomTypeCode);
            if (group.roomTypeDescription) html += ' (' + HolaPms.escapeHtml(group.roomTypeDescription) + ')';
            html += '</span>';
            if (group.recommended) html += '<span class="badge bg-primary">추천</span>';
            if (specInfo) html += '<span class="badge bg-light text-dark border">' + specInfo + '</span>';
            html += '<span class="ms-auto"></span>';
            html += priceInfo + diffBadge;
            html += '</div>';
            html += '</button></h2>';

            html += '<div id="' + collapseId + '" class="accordion-collapse collapse' + (isExpanded ? ' show' : '') + '" data-bs-parent="#assignAccordion">';
            html += '<div class="accordion-body p-0">';

            // 일자별 요금 테이블
            if (group.dailyCharges && group.dailyCharges.length > 0) {
                html += '<div class="border-bottom px-3 py-2">';
                html += '<small class="text-muted d-block mb-1">일자별 요금</small>';
                html += '<div class="table-responsive">';
                html += '<table class="table table-sm table-hover mb-0" style="min-width:450px">';
                html += '<thead class="table-light"><tr><th class="text-center">일자</th><th class="text-center">공급가</th><th class="text-center">세금</th><th class="text-center">봉사료</th><th class="text-center">합계</th></tr></thead>';
                html += '<tbody>';
                group.dailyCharges.forEach(function(dc) {
                    html += '<tr>';
                    html += '<td class="text-center">' + dc.chargeDate + '</td>';
                    html += '<td class="text-center">' + formatPrice(dc.supplyPrice) + '</td>';
                    html += '<td class="text-center">' + formatPrice(dc.tax) + '</td>';
                    html += '<td class="text-center">' + formatPrice(dc.serviceCharge) + '</td>';
                    html += '<td class="text-center">' + formatPrice(dc.total) + '</td>';
                    html += '</tr>';
                });
                html += '</tbody></table>';
                html += '</div></div>';
            }

            // 층별 섹션
            if (group.floors && group.floors.length > 0) {
                group.floors.forEach(function(floor) {
                    html += '<div class="border-bottom">';
                    html += '<div class="bg-light px-3 py-2 d-flex justify-content-between align-items-center">';
                    html += '<span>' + HolaPms.escapeHtml(floor.floorName) + '</span>';
                    html += '<span class="badge bg-secondary">가용 ' + floor.availableRooms + '/' + floor.totalRooms + '</span>';
                    html += '</div>';

                    // 호수 테이블
                    html += '<div class="table-responsive">';
                    html += '<table class="table table-sm table-hover mb-0" style="min-width:500px">';
                    html += '<thead class="table-light"><tr><th style="width:40px" class="text-center"></th><th style="min-width:70px" class="text-center">호수</th><th style="min-width:100px" class="text-center">설명</th><th style="min-width:60px" class="text-center">상태</th><th style="min-width:150px" class="text-center">예약정보</th></tr></thead>';
                    html += '<tbody>';

                    floor.rooms.forEach(function(room) {
                        var disabled = !room.available ? ' disabled' : '';
                        var rowClass = !room.available ? ' class="table-secondary"' : '';

                        html += '<tr' + rowClass + '>';
                        html += '<td class="text-center"><input type="radio" name="roomAssignRadio" value="' + room.roomNumberId + '"';
                        html += ' data-floor-id="' + floor.floorId + '"';
                        html += ' data-room-number="' + HolaPms.escapeHtml(room.roomNumber) + '"';
                        html += ' data-floor-name="' + HolaPms.escapeHtml(floor.floorName) + '"';
                        html += ' aria-label="선택: ' + HolaPms.escapeHtml(floor.floorName) + ' ' + HolaPms.escapeHtml(room.roomNumber) + '"';
                        html += disabled + '></td>';
                        html += '<td class="text-center">' + HolaPms.escapeHtml(room.roomNumber) + '</td>';
                        html += '<td class="text-center">' + (room.descriptionKo ? HolaPms.escapeHtml(room.descriptionKo) : '-') + '</td>';
                        var badge = room.available
                            ? '<span class="badge bg-success">가용</span>'
                            : ReservationDetail._roomStatusBadge(room.unavailableType, '사용중');
                        html += '<td class="text-center">' + badge + '</td>';
                        html += '<td class="text-center">';
                        if (!room.available && room.conflictReservationNumber) {
                            html += '<small class="text-muted">';
                            html += HolaPms.escapeHtml(room.conflictReservationNumber) + ' / ' + HolaPms.escapeHtml(room.conflictGuestName || '');
                            html += '<br>' + (room.conflictCheckIn || '') + ' ~ ' + (room.conflictCheckOut || '');
                            html += '</small>';
                        } else {
                            html += '-';
                        }
                        html += '</td>';
                        html += '</tr>';
                    });

                    html += '</tbody></table>';
                    html += '</div>';
                    html += '</div>';
                });
            } else {
                html += '<div class="text-center py-3 text-muted">배정 가능한 층이 없습니다.</div>';
            }

            html += '</div></div></div>';
        });

        html += '</div>';
        $('#assignContent').html(html);
    },

    applyRoomAssign: function() {
        var self = this;
        var selected = $('input[name="roomAssignRadio"]:checked');
        if (selected.length === 0) {
            HolaPms.alert('warning', '객실을 선택해주세요.');
            return;
        }

        var roomNumberId = selected.val();
        var floorId = selected.data('floor-id');
        var roomNumber = selected.data('room-number');
        var floorName = selected.data('floor-name');

        var $leg = $('#roomLeg_' + self.currentLegSeq);
        $leg.find('.floor-id').val(floorId);
        $leg.find('.room-number-id').val(roomNumberId);

        // 표시 텍스트 업데이트
        $leg.find('.room-number-display').val(floorName + ' / ' + roomNumber);

        HolaPms.modal.hide('#roomAssignModal');
        HolaPms.alert('success', '객실이 배정되었습니다.');
    },

    // ═══════════════════════════════════════════
    // 폼 데이터 수집 & 저장
    // ═══════════════════════════════════════════

    /**
     * 전체 폼 데이터 수집
     */
    /** 날짜 문자열에 N일 더한 결과 반환 (yyyy-MM-dd) */
    addDays: function(dateStr, n) {
        var d = new Date(dateStr + 'T00:00:00');
        d.setDate(d.getDate() + n);
        return d.getFullYear() + '-'
            + String(d.getMonth() + 1).padStart(2, '0') + '-'
            + String(d.getDate()).padStart(2, '0');
    },

    collectFormData: function() {
        var self = this;
        // 서브 예약 (객실 레그) 수집
        var subReservations = [];
        $('.room-leg-card').each(function() {
            var $leg = $(this);
            // 종료 상태(취소/체크아웃/노쇼) 레그는 수집 제외
            if ($leg.data('leg-terminated') === true || $leg.data('leg-terminated') === 'true') return;
            var roomTypeId = HolaPms.form.intVal($leg.find('.room-type-id'));
            if (!roomTypeId) return; // 객실타입 미선택 레그 스킵

            var legCheckIn = HolaPms.form.val($leg.find('.leg-check-in'));
            var legCheckOut = HolaPms.form.val($leg.find('.leg-check-out'));
            // Dayuse: 서버 전송 시 체크아웃을 체크인+1일로 복원
            if (self.isDayUse && legCheckIn) {
                legCheckOut = self.addDays(legCheckIn, 1);
            }

            var legData = {
                roomTypeId: roomTypeId,
                floorId: HolaPms.form.intVal($leg.find('.floor-id')),
                roomNumberId: HolaPms.form.intVal($leg.find('.room-number-id')),
                checkIn: legCheckIn,
                checkOut: legCheckOut,
                adults: parseInt($leg.find('.leg-adults').val()) || 1,
                children: parseInt($leg.find('.leg-children').val()) || 0,
                earlyCheckIn: $leg.find('.leg-early-checkin').val() === 'true',
                lateCheckOut: $leg.find('.leg-late-checkout').val() === 'true',
                stayType: self.isDayUse ? 'DAY_USE' : 'OVERNIGHT',
                dayUseDurationHours: self.isDayUse ? self.dayUseDurationHours : null
            };

            // 기존 레그 ID가 있으면 포함
            var legId = $leg.data('leg-id');
            if (legId) legData.id = legId;

            subReservations.push(legData);
        });

        var masterCheckIn = HolaPms.form.val('#masterCheckIn');
        var masterCheckOut = HolaPms.form.val('#masterCheckOut');
        if (self.isDayUse && masterCheckIn) {
            masterCheckOut = self.addDays(masterCheckIn, 1);
        }

        return {
            // 예약 기본 정보
            masterCheckIn: masterCheckIn,
            masterCheckOut: masterCheckOut,
            rateCodeId: HolaPms.form.intVal('#rateCodeId'),
            marketCodeId: HolaPms.form.intVal('#marketCodeId'),
            reservationChannelId: HolaPms.form.intVal('#reservationChannelId'),
            promotionType: self.reservationData ? self.reservationData.promotionType : null,
            promotionCode: HolaPms.form.val('#promotionCode'),

            // 예약자 정보
            guestNameKo: HolaPms.form.val('#guestNameKo'),
            guestLastNameEn: HolaPms.form.val('#guestLastNameEn'),
            guestFirstNameEn: HolaPms.form.val('#guestFirstNameEn'),
            guestMiddleNameEn: self.reservationData ? self.reservationData.guestMiddleNameEn : null,
            phoneCountryCode: HolaPms.form.val('#phoneCountryCode'),
            phoneNumber: HolaPms.form.val('#phoneNumber'),
            email: HolaPms.form.val('#email'),
            birthDate: HolaPms.form.val('#birthDate'),
            gender: HolaPms.form.val('#gender'),
            nationality: HolaPms.form.val('#nationality'),

            // OTA 정보
            isOtaManaged: $('#isOtaManaged').is(':checked'),
            otaReservationNo: HolaPms.form.val('#otaReservationNo'),

            // 서브 예약 (객실 레그)
            subReservations: subReservations,

            // 기타정보
            customerRequest: HolaPms.form.val('#customerRequest')
        };
    },

    /**
     * 예약 전용 날짜 바인딩 (체크인 우선 → 체크아웃 자동 보정)
     */
    bindReservationDates: function(startSel, endSel) {
        var $start = $(startSel);
        var $end = $(endSel);

        function toDateStr(date) {
            var y = date.getFullYear();
            var m = ('0' + (date.getMonth() + 1)).slice(-2);
            var d = ('0' + date.getDate()).slice(-2);
            return y + '-' + m + '-' + d;
        }

        $start.on('change', function() {
            var startVal = $(this).val();
            if (!startVal) return;
            var nextDay = new Date(startVal + 'T00:00:00');
            nextDay.setDate(nextDay.getDate() + 1);
            var nextDayStr = toDateStr(nextDay);
            $end.attr('min', nextDayStr);
            if (!$end.val() || $end.val() <= startVal) {
                $end.val(nextDayStr);
            }
        });

        $end.on('change', function() {
            var endVal = $(this).val();
            if (!endVal) return;
            if ($start.val() && $start.val() >= endVal) {
                var prevDay = new Date(endVal + 'T00:00:00');
                prevDay.setDate(prevDay.getDate() - 1);
                $start.val(toDateStr(prevDay));
            }
        });

        if ($start.val()) {
            var initNext = new Date($start.val() + 'T00:00:00');
            initNext.setDate(initNext.getDate() + 1);
            $end.attr('min', toDateStr(initNext));
        }
    },

    /**
     * 유효성 검증
     */
    validate: function(data) {
        // 이전 검증 상태 초기화
        $('#formContainer [aria-invalid]').removeAttr('aria-invalid');

        var markInvalid = function(selector) { $(selector).attr('aria-invalid', 'true'); };

        if (!data.masterCheckIn) {
            HolaPms.alert('warning', '체크인 날짜를 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
            markInvalid('#masterCheckIn');
            $('#masterCheckIn').focus();
            return false;
        }
        if (!data.masterCheckOut) {
            HolaPms.alert('warning', '체크아웃 날짜를 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
            markInvalid('#masterCheckOut');
            $('#masterCheckOut').focus();
            return false;
        }
        if (!this.isDayUse && data.masterCheckIn >= data.masterCheckOut) {
            HolaPms.alert('warning', '체크아웃 날짜는 체크인 날짜 이후여야 합니다.');
            $('a[href="#tabReservation"]').tab('show');
            markInvalid('#masterCheckOut');
            $('#masterCheckOut').focus();
            return false;
        }
        if (!data.guestNameKo && !data.guestLastNameEn) {
            HolaPms.alert('warning', '예약자명(국문) 또는 영문 성을 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
            markInvalid('#guestNameKo');
            $('#guestNameKo').focus();
            return false;
        }
        if (data.subReservations.length === 0) {
            HolaPms.alert('warning', '객실을 1개 이상 추가해주세요.');
            $('a[href="#tabDetail"]').tab('show');
            return false;
        }

        // 각 서브 예약 검증
        for (var i = 0; i < data.subReservations.length; i++) {
            var sub = data.subReservations[i];
            if (!sub.checkIn || !sub.checkOut) {
                HolaPms.alert('warning', '객실 #' + (i + 1) + '의 체크인/체크아웃 날짜를 입력해주세요.');
                $('a[href="#tabDetail"]').tab('show');
                return false;
            }
            if (!this.isDayUse && sub.checkIn >= sub.checkOut) {
                HolaPms.alert('warning', '객실 #' + (i + 1) + '의 체크아웃은 체크인 이후여야 합니다.');
                $('a[href="#tabDetail"]').tab('show');
                return false;
            }
            // 객실타입 최대 수용 인원 검증
            if (sub.roomTypeId && this.allRoomTypes) {
                var rt = this.allRoomTypes.find(function(t) { return t.id === sub.roomTypeId; });
                if (rt) {
                    if ((sub.adults || 1) > (rt.maxAdults || 99)) {
                        HolaPms.alert('warning', '객실 #' + (i + 1) + '의 성인 수가 최대 수용 인원(' + rt.maxAdults + '명)을 초과합니다.');
                        $('a[href="#tabDetail"]').tab('show');
                        return false;
                    }
                    if ((sub.children || 0) > (rt.maxChildren || 99)) {
                        HolaPms.alert('warning', '객실 #' + (i + 1) + '의 아동 수가 최대 수용 인원(' + rt.maxChildren + '명)을 초과합니다.');
                        $('a[href="#tabDetail"]').tab('show');
                        return false;
                    }
                }
            }
        }

        return true;
    },

    /**
     * 코드 ID로 이름 조회 (레이트코드, 마켓코드)
     */
    resolveCodeName: function(baseUrl, id, targetSelector, nameField) {
        HolaPms.ajax({
            url: baseUrl,
            type: 'GET',
            success: function(res) {
                var items = res.data || res || [];
                if (Array.isArray(items)) {
                    var found = items.find(function(item) { return item.id === id; });
                    if (found) {
                        var name = found[nameField] || found.codeName || found.name || '';
                        // 레이트코드인 경우 '코드 - 이름' 형식
                        if (found.rateCode && nameField === 'rateNameKo') {
                            name = found.rateCode + ' - ' + name;
                        }
                        $(targetSelector).val(name);
                    }
                }
            },
            error: function() { /* 무시 */ }
        });
    },

    /**
     * 저장 (예약정보 + 서브예약 + 예치금 통합 저장)
     */
    save: function() {
        var self = this;

        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var data = self.collectFormData();
        if (!self.validate(data)) return;

        // 레이트코드 변경 감지 → 확인 다이얼로그
        var origRateCodeId = self.reservationData ? self.reservationData.rateCodeId : null;
        var newRateCodeId = data.rateCodeId || null;
        if (origRateCodeId && newRateCodeId && origRateCodeId !== newRateCodeId) {
            var origName = self.reservationData.rateCodeName || '(ID: ' + origRateCodeId + ')';
            var newName = $('#rateCodeName').val() || '(ID: ' + newRateCodeId + ')';
            if (!confirm('레이트코드가 변경되었습니다.\n\n'
                    + '  현재: ' + origName + '\n'
                    + '  변경: ' + newName + '\n\n'
                    + '레이트코드를 변경하면 모든 객실의 요금이 재계산됩니다.\n계속하시겠습니까?')) {
                return;
            }
        }

        // 1단계: 예약 정보 저장
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId,
            type: 'PUT',
            data: data,
            success: function(res) {
                if (res.success) {
                    // 2단계: 예치금 저장
                    self.saveDeposit(function() {
                        self.clearDirty();
                        HolaPms.popup.notifyParent('saved', self.reservationId);
                        HolaPms.alert('success', '예약이 수정되었습니다.');
                        self.loadData();
                    });
                }
            }
        });
    },

    /**
     * 예약 삭제 (SUPER_ADMIN 전용)
     */
    deleteReservation: function() {
        var self = this;

        if (!confirm('정말 삭제하시겠습니까?')) {
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/delete',
            type: 'DELETE',
            success: function(res) {
                if (res.success) {
                    HolaPms.popup.notifyParent('deleted', self.reservationId);
                    if (HolaPms.popup.isPopup()) {
                        HolaPms.alert('success', '예약이 삭제되었습니다.');
                        setTimeout(function() { window.close(); }, 800);
                    } else {
                        HolaPms.alertAndRedirect('success', '예약이 삭제되었습니다.', '/admin/reservations');
                    }
                }
            }
        });
    },

    /**
     * 예치금 저장 (POST 신규 / PUT 수정)
     */
    saveDeposit: function(callback) {
        var self = this;
        var depositMethod = HolaPms.form.val('#depositMethod');
        var amount = parseFloat($('#depositAmount').val()) || 0;

        // 예치 방법 미선택이고 금액 0이면 스킵
        if (!depositMethod && amount === 0) {
            if (callback) callback();
            return;
        }

        var depositData = {
            depositMethod: depositMethod,
            currency: 'KRW',
            amount: amount
        };

        // 카드 정보
        if (depositMethod === 'CREDIT_CARD') {
            depositData.cardCompany = HolaPms.form.val('#cardCompany');
            depositData.cardNumberEncrypted = HolaPms.form.val('#cardNumber');
            depositData.cardCvcEncrypted = HolaPms.form.val('#cardCvc');
            depositData.cardExpiryDate = HolaPms.form.val('#cardExpiryDate');
        }

        var depositId = HolaPms.form.val('#depositId');
        var url, method;
        if (depositId) {
            // 기존 예치금 수정
            url = '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/deposit/' + depositId;
            method = 'PUT';
        } else {
            // 신규 예치금 등록
            url = '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/deposit';
            method = 'POST';
        }

        HolaPms.ajax({
            url: url,
            type: method,
            data: depositData,
            success: function(res) {
                if (res.success && res.data) {
                    // 반환된 depositId 업데이트 (신규 등록 시)
                    $('#depositId').val(res.data.id || '');
                }
                if (callback) callback();
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
                if (callback) callback();
            }
        });
    },

    // ═══════════════════════════════════════════
    // 메모
    // ═══════════════════════════════════════════

    /**
     * 메모 등록
     */
    addMemo: function() {
        var self = this;
        var content = $.trim($('#newMemoContent').val());
        if (!content) {
            HolaPms.alert('warning', '메모 내용을 입력해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/memos',
            type: 'POST',
            data: { content: content },
            success: function(res) {
                if (res.success) {
                    $('#newMemoContent').val('');
                    HolaPms.alert('success', '메모가 등록되었습니다.');
                    self.loadMemos();
                }
            }
        });
    },

    /**
     * 메모 목록 조회
     */
    loadMemos: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/memos',
            type: 'GET',
            success: function(res) {
                if (res.success) {
                    self.renderMemos(res.data || []);
                }
            }
        });
    },

    /**
     * 메모 목록 렌더링 (최신순)
     */
    renderMemos: function(memos) {
        var $list = $('#memoList');
        $list.empty();

        if (!memos || memos.length === 0) {
            $list.html('<div class="text-center text-muted py-3"><i class="fas fa-comment-slash me-1"></i>등록된 메모가 없습니다.</div>');
            return;
        }

        // 최신순 정렬
        memos.sort(function(a, b) {
            return (b.createdAt || '').localeCompare(a.createdAt || '');
        });

        memos.forEach(function(memo) {
            var createdAt = memo.createdAt ? memo.createdAt.replace('T', ' ').substring(0, 16) : '';
            var createdBy = memo.createdBy || '';

            var html = ''
                + '<div class="border rounded p-2 mb-2">'
                + '  <div class="d-flex justify-content-between align-items-center mb-1">'
                + '    <span class="text-muted small">'
                + '      <i class="fas fa-user me-1"></i>' + HolaPms.escapeHtml(createdBy)
                + '    </span>'
                + '    <span class="text-muted small">' + HolaPms.escapeHtml(createdAt) + '</span>'
                + '  </div>'
                + '  <div>' + HolaPms.escapeHtml(memo.content || '') + '</div>'
                + '</div>';

            $list.append(html);
        });
    }
    // ========== 객실 업그레이드 ==========

    ,isUpgradeable: function() {
        if (!this.reservationData) return false;
        var status = this.reservationData.reservationStatus;
        return ['RESERVED', 'CHECK_IN', 'INHOUSE'].indexOf(status) !== -1;
    },

    openUpgradeModal: function(legId, currentRoomTypeName) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        $('#upgradeLegId').val(legId);
        $('#upgradeCurrentType').text(currentRoomTypeName || '-');
        $('#upgradePreviewArea').addClass('d-none');
        $('#upgradeRoomTypeId').html('<option value="">선택</option>');
        $('#upgradeType').val('PAID');
        $('#upgradeReason').val('');

        // 가능 객실타입 로드
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/reservations/' + legId + '/upgrade/available-types',
            success: function(res) {
                var types = res.data || [];
                types.forEach(function(t) {
                    var label = HolaPms.escapeHtml(t.roomTypeCode);
                    if (t.description) label += ' - ' + HolaPms.escapeHtml(t.description);
                    label += ' (최대 ' + t.maxAdults + '성인/' + t.maxChildren + '아동)';
                    $('#upgradeRoomTypeId').append('<option value="' + t.roomTypeId + '">' + label + '</option>');
                });
                HolaPms.modal.show('#upgradeModal');
            },
            error: function(xhr) { HolaPms.handleAjaxError(xhr); }
        });
    },

    /**
     * 토글 버튼 상태 적용
     */
    applyToggle: function($group, $btn, hiddenSelector, isOn) {
        $group.prev(hiddenSelector).val(String(isOn));
        $group.find('.btn').removeClass('btn-primary btn-outline-danger').addClass('btn-outline-secondary');
        if (isOn) {
            $btn.removeClass('btn-outline-secondary').addClass('btn-primary');
        } else {
            $btn.removeClass('btn-outline-secondary').addClass('btn-outline-danger');
        }
    },

    /**
     * 얼리/레이트 정책 존재 여부 확인
     */
    checkPolicyExists: function(policyType, callback) {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            callback(false);
            return;
        }
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/early-late-policies?policyType=' + policyType,
            type: 'GET',
            success: function(res) {
                callback(res.success && res.data && res.data.length > 0);
            },
            error: function() {
                callback(false);
            }
        });
    },

    /**
     * 확정 요금 배지 표시 — 이미 등록된 얼리/레이트 요금 표시
     */
    showEarlyLateConfirmedFee: function($legCard) {
        var seq = $legCard.data('leg-seq');
        var $area = $('#earlyLateEstimate_' + seq);
        var $content = $('#earlyLateEstimateContent_' + seq);

        var earlyOn = $legCard.find('.leg-early-checkin').val() === 'true';
        var lateOn = $legCard.find('.leg-late-checkout').val() === 'true';

        if (!earlyOn && !lateOn) {
            $area.addClass('d-none');
            return;
        }

        var earlyFee = Number($area.data('early-fee')) || 0;
        var lateFee = Number($area.data('late-fee')) || 0;
        var parts = [];

        if (earlyOn && earlyFee > 0) {
            parts.push('<span class="badge bg-info me-2"><i class="fas fa-check me-1"></i>얼리 체크인 ₩'
                + earlyFee.toLocaleString('ko-KR') + '</span>');
        } else if (earlyOn) {
            parts.push('<span class="text-muted"><i class="fas fa-clock me-1"></i>얼리 체크인 — 시간대 미선택</span>');
        }

        if (lateOn && lateFee > 0) {
            parts.push('<span class="badge bg-danger me-2"><i class="fas fa-check me-1"></i>레이트 체크아웃 ₩'
                + lateFee.toLocaleString('ko-KR') + '</span>');
        } else if (lateOn) {
            parts.push('<span class="text-muted"><i class="fas fa-clock me-1"></i>레이트 체크아웃 — 시간대 미선택</span>');
        }

        if (parts.length > 0) {
            $content.html(parts.join(' '));
            $area.removeClass('d-none');
        } else {
            $area.addClass('d-none');
        }
    },

    /**
     * 시간대 선택 옵션 표시 — 토글 ON 시 호출
     */
    showTimeWindowOptions: function($legCard, policyType) {
        var self = this;
        var legId = $legCard.data('leg-id');
        var seq = $legCard.data('leg-seq');
        var $area = $('#earlyLateEstimate_' + seq);
        var $content = $('#earlyLateEstimateContent_' + seq);
        var label = policyType === 'EARLY_CHECKIN' ? '얼리 체크인' : '레이트 체크아웃';

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                + '/legs/' + legId + '/early-late-estimate?policyType=' + policyType,
            type: 'GET',
            success: function(res) {
                if (!res.success || !res.data || res.data.length === 0) {
                    HolaPms.alert('warning', label + ' 요금 정책이 없습니다.');
                    return;
                }

                // 시간대가 1개면 자동 선택
                if (res.data.length === 1) {
                    self.registerEarlyLateFee($legCard, policyType, 0);
                    return;
                }

                // 시간대 선택 버튼 렌더
                var html = '<div class="mb-1"><i class="fas fa-clock text-primary me-1"></i><strong>' + label + '</strong> 시간대를 선택하세요:</div>';
                html += '<div class="d-flex flex-wrap gap-1">';
                res.data.forEach(function(e, idx) {
                    var feeLabel = e.feeType === 'PERCENT'
                        ? Number(e.feeValue) + '%'
                        : '₩' + Number(e.feeValue).toLocaleString('ko-KR');
                    html += '<button type="button" class="btn btn-outline-primary btn-sm early-late-select-btn"'
                        + ' data-policy-type="' + policyType + '" data-policy-index="' + idx + '">'
                        + e.timeFrom + '~' + e.timeTo
                        + ' <strong>₩' + Number(e.estimatedFee).toLocaleString('ko-KR') + '</strong>'
                        + ' <small class="text-muted">(' + feeLabel + ')</small>'
                        + '</button>';
                });
                html += '</div>';

                $content.html(html);
                $area.removeClass('d-none');
            }
        });
    },

    /**
     * 얼리/레이트 요금 등록 API 호출
     */
    registerEarlyLateFee: function($legCard, policyType, policyIndex) {
        var self = this;
        var legId = $legCard.data('leg-id');
        var seq = $legCard.data('leg-seq');
        var $area = $('#earlyLateEstimate_' + seq);
        var isEarly = policyType === 'EARLY_CHECKIN';
        var hiddenSelector = isEarly ? '.leg-early-checkin' : '.leg-late-checkout';
        var toggleClass = isEarly ? '.early-checkin-toggle' : '.late-checkout-toggle';

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                + '/legs/' + legId + '/early-late-fee',
            type: 'POST',
            data: JSON.stringify({ policyType: policyType, policyIndex: policyIndex }),
            success: function(res) {
                if (res.success) {
                    var fee = Number(res.data.fee);
                    // 토글 UI 업데이트
                    var $group = $legCard.find(toggleClass).first().closest('.btn-group');
                    var $onBtn = $group.find('[data-value="true"]');
                    self.applyToggle($group, $onBtn, hiddenSelector, true);

                    // 확정 요금 data 속성 업데이트
                    if (isEarly) {
                        $area.data('early-fee', fee);
                    } else {
                        $area.data('late-fee', fee);
                    }

                    // 확정 요금 표시
                    self.showEarlyLateConfirmedFee($legCard);

                    var label = isEarly ? '얼리 체크인' : '레이트 체크아웃';
                    HolaPms.alert('success', label + ' 요금 ₩' + fee.toLocaleString('ko-KR') + ' 등록 완료');
                }
            }
        });
    },

    /**
     * 얼리/레이트 요금 해제 API 호출
     */
    removeEarlyLateFee: function($legCard, policyType, callback) {
        var self = this;
        var legId = $legCard.data('leg-id');
        var seq = $legCard.data('leg-seq');
        var $area = $('#earlyLateEstimate_' + seq);
        var isEarly = policyType === 'EARLY_CHECKIN';

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                + '/legs/' + legId + '/early-late-fee?policyType=' + policyType,
            type: 'DELETE',
            success: function(res) {
                if (res.success) {
                    // data 속성 초기화
                    if (isEarly) {
                        $area.data('early-fee', 0);
                    } else {
                        $area.data('late-fee', 0);
                    }
                    if (callback) callback();
                }
            }
        });
    },

    loadUpgradePreview: function(legId, toRoomTypeId, selectedRateCodeId) {
        var propertyId = HolaPms.context.getPropertyId();
        var previewUrl = '/api/v1/properties/' + propertyId + '/reservations/' + legId
            + '/upgrade/preview?toRoomTypeId=' + toRoomTypeId;
        if (selectedRateCodeId) {
            previewUrl += '&selectedRateCodeId=' + selectedRateCodeId;
        }
        HolaPms.ajax({
            url: previewUrl,
            success: function(res) {
                var p = res.data;
                $('#previewFromType').text(p.fromRoomTypeName || '-');
                $('#previewToType').text(p.toRoomTypeName || '-');
                $('#previewCurrentTotal').text(Number(p.currentTotalCharge || 0).toLocaleString() + '원');
                $('#previewNewTotal').text(Number(p.newTotalCharge || 0).toLocaleString() + '원');
                var diff = Number(p.priceDifference || 0);
                var diffSign = diff >= 0 ? '+' : '';
                var diffClass = diff > 0 ? 'text-danger' : (diff < 0 ? 'text-primary' : '');
                $('#previewDiff').html('<span class="' + diffClass + '">' + diffSign + diff.toLocaleString() + '원</span>');
                $('#previewNights').text(p.remainingNights + '박 (' + (p.fromRoomTypeName || '') + ' → ' + (p.toRoomTypeName || '') + ')');

                // 일자별 상세
                var dailyHtml = '';
                if (p.dailyDiffs && p.dailyDiffs.length > 0) {
                    dailyHtml = '<table class="table table-sm table-bordered mt-2 mb-0"><thead class="table-light"><tr><th>일자</th><th class="text-end">현재</th><th class="text-end">변경후</th><th class="text-end">차액</th></tr></thead><tbody>';
                    p.dailyDiffs.forEach(function(d) {
                        var dd = Number(d.difference || 0);
                        var dc = dd > 0 ? 'text-danger' : (dd < 0 ? 'text-primary' : '');
                        dailyHtml += '<tr><td>' + d.chargeDate + '</td><td class="text-end">' + Number(d.currentCharge || 0).toLocaleString() + '</td><td class="text-end">' + Number(d.newCharge || 0).toLocaleString() + '</td><td class="text-end ' + dc + '">' + (dd >= 0 ? '+' : '') + dd.toLocaleString() + '</td></tr>';
                    });
                    dailyHtml += '</tbody></table>';
                }
                $('#previewDailyDiffs').html(dailyHtml);

                // 레이트코드 변경 경고 + 후보 선택
                if (p.rateCodeChanged && p.availableRateCodes && p.availableRateCodes.length > 1) {
                    var selectHtml = '<div class="alert alert-warning py-2 mt-2 mb-0">'
                        + '<i class="fas fa-exclamation-triangle me-1"></i>'
                        + '레이트코드가 변경됩니다: <strong>' + (p.currentRateCodeName || '') + '</strong> → '
                        + '<select id="upgradeRateCodeSelect" class="form-select form-select-sm d-inline-block" style="width:auto;">';
                    p.availableRateCodes.forEach(function(rc) {
                        var label = rc.rateCode + ' - ' + rc.rateNameKo;
                        if (rc.current) label += ' (현재)';
                        selectHtml += '<option value="' + rc.id + '"'
                            + (rc.recommended ? ' selected' : '')
                            + '>' + HolaPms.escapeHtml(label) + '</option>';
                    });
                    selectHtml += '</select></div>';
                    $('#previewRateCodeAlert').html(selectHtml).removeClass('d-none');

                    // 레이트코드 선택 변경 시 미리보기 재호출
                    $('#upgradeRateCodeSelect').off('change').on('change', function() {
                        var selectedRateCodeId = $(this).val();
                        var legId = $('#upgradeLegId').val();
                        var toRoomTypeId = $('#upgradeRoomTypeId').val();
                        self.loadUpgradePreview(legId, toRoomTypeId, selectedRateCodeId);
                    });
                } else if (p.rateCodeChanged) {
                    $('#previewRateCodeAlert').html(
                        '<div class="alert alert-warning py-2 mt-2 mb-0">' +
                        '<i class="fas fa-exclamation-triangle me-1"></i>' +
                        '레이트코드가 변경됩니다: <strong>' + (p.currentRateCodeName || '') +
                        '</strong> → <strong>' + (p.targetRateCodeName || '') + '</strong></div>'
                    ).removeClass('d-none');
                } else {
                    $('#previewRateCodeAlert').addClass('d-none').empty();
                }

                $('#upgradePreviewArea').removeClass('d-none');
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
                $('#upgradePreviewArea').addClass('d-none');
            }
        });
    },

    executeUpgrade: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        var legId = $('#upgradeLegId').val();
        var toRoomTypeId = $('#upgradeRoomTypeId').val();
        var upgradeType = $('#upgradeType').val();
        var reason = $.trim($('#upgradeReason').val());

        if (!toRoomTypeId) {
            HolaPms.alert('warning', '대상 객실타입을 선택해주세요.');
            return;
        }

        var upgradeTypeLabel = $('#upgradeType option:selected').text();
        HolaPms.confirm('객실 업그레이드를 실행하시겠습니까?\n유형: ' + upgradeTypeLabel, function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/reservations/' + legId + '/upgrade',
                type: 'POST',
                data: {
                    toRoomTypeId: parseInt(toRoomTypeId),
                    upgradeType: upgradeType,
                    reason: reason || null
                },
                success: function(res) {
                    HolaPms.modal.hide('#upgradeModal');
                    HolaPms.alert('success', '객실 업그레이드가 완료되었습니다.');
                    self.reload();
                }
            });
        });
    },

    // ─── 변경이력 ──────────────────────────

    loadChangeHistory: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/history',
            type: 'GET',
            success: function(res) {
                self.renderChangeHistory(res.data || []);
            },
            error: function() {
                $('#changeHistoryContainer').hide();
                $('#changeHistoryEmpty').show();
            }
        });
    },

    renderChangeHistory: function(items) {
        if (!items.length) {
            $('#changeHistoryContainer').hide();
            $('#changeHistoryEmpty').show();
            return;
        }
        $('#changeHistoryEmpty').hide();

        var categoryBadge = function(cat) {
            var map = {
                'STATUS': '<span class="badge bg-info">상태</span>',
                'ROOM': '<span class="badge bg-primary">객실</span>',
                'RATE': '<span class="badge bg-warning text-dark">요금</span>',
                'UPGRADE': '<span class="badge bg-success">업그레이드</span>',
                'SERVICE': '<span class="badge bg-secondary">서비스</span>',
                'PAYMENT': '<span class="badge bg-dark">결제</span>',
                'RESERVATION': '<span class="badge" style="background-color:#0582CA;">예약</span>'
            };
            return map[cat] || '<span class="badge bg-light text-dark">' + cat + '</span>';
        };

        var escapeHtml = function(str) {
            if (!str) return '';
            return $('<span>').text(str).html();
        };

        // 마스터/서브 동일 상태변경 중복 제거: 마스터(subReservationId=null)가 있으면 서브 건 숨김
        var masterKeys = {};
        items.forEach(function(item) {
            if (!item.subReservationId && item.changeCategory === 'STATUS') {
                var key = item.description + '|' + (item.createdAt || '').substring(0, 19);
                masterKeys[key] = true;
            }
        });
        var filtered = items.filter(function(item) {
            if (item.subReservationId && item.changeCategory === 'STATUS') {
                var key = item.description + '|' + (item.createdAt || '').substring(0, 19);
                return !masterKeys[key];
            }
            return true;
        });

        var html = '<div class="list-group list-group-flush">';
        filtered.forEach(function(item) {
            var ts = item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 19) : '';
            html += '<div class="list-group-item px-0 py-2">'
                + '<div class="d-flex justify-content-between align-items-start">'
                + '<div>' + categoryBadge(item.changeCategory)
                + ' <span class="ms-1">' + escapeHtml(item.description) + '</span></div>'
                + '<small class="text-muted text-nowrap ms-2">' + ts + '</small>'
                + '</div>'
                + '<small class="text-muted">' + escapeHtml(item.createdBy || '') + '</small>'
                + '</div>';
        });
        html += '</div>';
        $('#changeHistoryContainer').html(html).show();
    },

    // ─── 미저장 변경 감지 ──────────────────────────

    markDirty: function() {
        if (this.isDirty) return;
        this.isDirty = true;
        var $btn = $('#saveBtn');
        $btn.addClass('btn-save-dirty');
        $btn.html('<i class="fas fa-save me-1"></i> 저장');
    },

    clearDirty: function() {
        this.isDirty = false;
        var $btn = $('#saveBtn');
        $btn.removeClass('btn-save-dirty');
        $btn.html('<i class="fas fa-save me-1"></i> 저장');
    }
};

// 초기화
$(document).ready(function() {
    ReservationDetail.init();
});
