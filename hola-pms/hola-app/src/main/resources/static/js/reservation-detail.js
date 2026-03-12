/**
 * 예약 상세/수정 페이지
 */
var ReservationDetail = {
    propertyId: null,
    reservationId: null,
    reservationData: null,   // API 응답 전체 저장
    isOta: false,
    isReadonly: false,       // CHECKED_OUT / CANCELED / NO_SHOW
    roomLegSeq: 0,           // 객실 레그 시퀀스
    currentLegSeq: null,     // 현재 모달 대상 레그 시퀀스

    // 유료 서비스 옵션 캐시
    paidServiceOptions: null,

    // 모달 DataTable 인스턴스
    rateCodeTable: null,
    marketCodeTable: null,
    roomTypeTable: null,

    // 상태 배지 매핑 (HolaPms.reservationStatus 공통 참조)
    STATUS_BADGE: HolaPms.reservationStatus,

    // 상태 전이 매트릭스
    STATUS_TRANSITIONS: {
        RESERVED: [
            { status: 'CHECK_IN', label: '체크인', icon: 'fa-sign-in-alt', cls: 'btn-info' },
            { status: 'CANCELED', label: '취소', icon: 'fa-ban', cls: 'btn-danger' },
            { status: 'NO_SHOW', label: '노쇼', icon: 'fa-user-slash', cls: 'btn-warning' }
        ],
        CHECK_IN: [
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
        self.reload();
    },

    /**
     * 컨텍스트 기반 새로고침
     */
    reload: function() {
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
        self.loadData();
    },

    /**
     * 예약 데이터 조회
     */
    loadData: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId,
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    self.bindData(res.data);
                }
            }
        });
    },

    /**
     * 예약채널 목록 로드
     */
    loadReservationChannels: function() {
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
            },
            error: function() {
                // 예약채널 API 미구현 시 무시
            }
        });
    },

    /**
     * 유료 서비스 옵션 목록 로드 (캐싱)
     */
    loadPaidServiceOptions: function() {
        var self = this;
        if (!self.propertyId) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/paid-service-options',
            type: 'GET',
            success: function(res) {
                var options = (res.data || res || []);
                if (Array.isArray(options)) {
                    self.paidServiceOptions = options.filter(function(o) { return o.useYn !== false; });
                }
            },
            error: function() {
                self.paidServiceOptions = [];
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

        // ── Tab 1: 예약정보 ──
        $('#masterReservationNoDisplay').val(data.masterReservationNo || '');
        $('#confirmationNoDisplay').val(data.confirmationNo || '');
        $('#masterCheckIn').val(data.masterCheckIn || '');
        $('#masterCheckOut').val(data.masterCheckOut || '');
        this.bindReservationDates('#masterCheckIn', '#masterCheckOut');

        if (data.rateCodeId) {
            $('#rateCodeId').val(data.rateCodeId);
            if (data.rateCodeName) {
                $('#rateCodeName').val(data.rateCodeName);
            } else {
                self.resolveCodeName('/api/v1/properties/' + self.propertyId + '/rate-codes', data.rateCodeId, '#rateCodeName', 'rateCodeName');
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
            // 채널 로드 후 선택 (약간의 지연 적용)
            setTimeout(function() { $('#reservationChannelId').val(data.reservationChannelId); }, 300);
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

        // 상태 변경 메뉴 구성
        self.buildStatusChangeMenu(data.reservationStatus);
    },

    /**
     * 서브 예약(레그) 렌더링
     */
    renderLegs: function(legs) {
        var self = this;
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

        if (legData) {
            checkIn = legData.checkIn || '';
            checkOut = legData.checkOut || '';
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

        // 상태에 따른 얼리/레이트 체크박스 활성화 제어
        // RESERVED/CONFIRMED: 둘 다 편집 가능 (사전 요청)
        // CHECK_IN/INHOUSE: 얼리 읽기전용 (자동 판정 완료), 레이트 편집 가능
        // CHECKED_OUT/CANCELED/NO_SHOW: 둘 다 읽기전용
        var status = self.reservationData ? self.reservationData.reservationStatus : '';
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

        var legHtml = ''
            + '<div class="card border shadow-sm mb-3 room-leg-card" id="roomLeg_' + seq + '" data-leg-seq="' + seq + '" data-leg-id="' + legId + '">'
            + '  <div class="card-header d-flex justify-content-between align-items-center">'
            + '    <span>' + headerLabel + '</span>'
            + '    <button class="btn btn-outline-danger btn-sm remove-leg-btn" data-leg="' + seq + '" data-leg-id="' + legId + '">'
            + '      <i class="fas fa-times"></i>'
            + '    </button>'
            + '  </div>'
            + '  <div class="card-body">'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label required">객실타입</label>'
            + '      <div class="col-sm-4">'
            + '        <div class="input-group">'
            + '          <input type="text" class="form-control room-type-name" readonly placeholder="선택" value="' + HolaPms.escapeHtml(roomTypeName) + '">'
            + '          <button class="btn btn-outline-secondary room-type-search-btn" type="button"><i class="fas fa-search"></i></button>'
            + '          <button class="btn btn-outline-danger room-type-clear-btn" type="button"><i class="fas fa-times"></i></button>'
            + '        </div>'
            + '        <input type="hidden" class="room-type-id" value="' + roomTypeId + '">'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label">층/호수</label>'
            + '      <div class="col-sm-4">'
            + '        <div class="input-group">'
            + '          <input type="text" class="form-control room-number-display" readonly placeholder="미배정" value="' + HolaPms.escapeHtml(roomDisplay === '미배정' ? '' : roomDisplay) + '">'
            + '          <button class="btn btn-outline-secondary room-assign-btn" type="button"><i class="fas fa-search"></i></button>'
            + '          <button class="btn btn-outline-danger room-assign-clear-btn" type="button"><i class="fas fa-times"></i></button>'
            + '        </div>'
            + '        <input type="hidden" class="floor-id" value="' + floorId + '">'
            + '        <input type="hidden" class="room-number-id" value="' + roomNumberId + '">'
            + '      </div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">체크인</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-in" value="' + checkIn + '"></div>'
            + '      <label class="col-sm-2 col-form-label">체크아웃</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-out" value="' + checkOut + '"></div>'
            + '    </div>'
            + self.renderActualTimeRow(actualCheckInTime, actualCheckOutTime, earlyCheckInFee, lateCheckOutFee)
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">성인</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-adults" value="' + adults + '" min="1" max="99"></div>'
            + '      <label class="col-sm-2 col-form-label">아동</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-children" value="' + children + '" min="0" max="99"></div>'
            + '      <label class="col-sm-2 col-form-label">얼리체크인</label>'
            + '      <div class="col-sm-1">'
            + '        <div class="form-check mt-2">'
            + '          <input class="form-check-input leg-early-checkin" type="checkbox"' + (earlyCheckIn ? ' checked' : '') + earlyDisabled + '>'
            + '        </div>'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label">레이트체크아웃</label>'
            + '      <div class="col-sm-1">'
            + '        <div class="form-check mt-2">'
            + '          <input class="form-check-input leg-late-checkout" type="checkbox"' + (lateCheckOut ? ' checked' : '') + lateDisabled + '>'
            + '        </div>'
            + '      </div>'
            + '    </div>'
            + '    <hr class="my-2">'
            + '    <div class="d-flex justify-content-between align-items-center mb-2">'
            + '      <span class="text-muted small"><i class="fas fa-concierge-bell me-1"></i>유료 서비스</span>'
            + '      <button class="btn btn-outline-primary btn-sm add-service-btn" data-leg="' + seq + '" data-leg-id="' + legId + '" type="button">'
            + '        <i class="fas fa-plus me-1"></i>서비스 추가'
            + '      </button>'
            + '    </div>'
            + '    <div class="service-list" id="serviceList_' + seq + '">'
            + self.renderServiceItems(legData ? legData.services : [], seq, legId)
            + '    </div>'
            + '    <div class="service-add-form d-none" id="serviceAddForm_' + seq + '">'
            + '      <div class="row g-2 align-items-end">'
            + '        <div class="col-sm-5">'
            + '          <select class="form-select form-select-sm service-option-select">'
            + '            <option value="">서비스 선택</option>'
            + '          </select>'
            + '        </div>'
            + '        <div class="col-sm-2">'
            + '          <input type="number" class="form-control form-control-sm service-qty" value="1" min="1" max="99">'
            + '        </div>'
            + '        <div class="col-sm-3">'
            + '          <input type="date" class="form-control form-control-sm service-date">'
            + '        </div>'
            + '        <div class="col-sm-2">'
            + '          <button class="btn btn-primary btn-sm w-100 confirm-add-service-btn" data-leg="' + seq + '" data-leg-id="' + legId + '">추가</button>'
            + '        </div>'
            + '      </div>'
            + '    </div>'
            + '  </div>'
            + '</div>';

        $('#roomLegsContainer').append(legHtml);

        // 서비스 옵션 드롭다운 구성
        this.populateServiceOptions(seq);
        this.updateRoomLegsEmpty();
    },

    /**
     * 실제 체크인/아웃 시각 + 얼리/레이트 요금 행 렌더링
     */
    renderActualTimeRow: function(actualCheckInTime, actualCheckOutTime, earlyCheckInFee, lateCheckOutFee) {
        // 표시할 데이터가 없으면 빈 문자열 반환
        if (!actualCheckInTime && !actualCheckOutTime && !earlyCheckInFee && !lateCheckOutFee) {
            return '';
        }

        // 시각 포맷 (2026-03-08T13:00:00 → 2026-03-08 13:00)
        var formatTime = function(dt) {
            if (!dt) return '-';
            return dt.replace('T', ' ').substring(0, 16);
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
            lateBadge = ' <span class="badge ms-1" style="background-color:#EF476F;">레이트 체크아웃 ₩' + formatFee(lateCheckOutFee) + '</span>';
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
    renderServiceItems: function(services, seq, legId) {
        if (!services || services.length === 0) {
            return '<div class="text-muted small text-center py-1">등록된 서비스가 없습니다.</div>';
        }

        var self = this;
        var html = '<table class="table table-sm table-borderless mb-0">';
        services.forEach(function(svc) {
            var name = svc.serviceName || ('서비스 #' + svc.serviceOptionId);
            var qtyPrice = svc.quantity + ' x ' + Number(svc.unitPrice || 0).toLocaleString('ko-KR') + '원';
            var taxStr = '세액 ' + Number(svc.tax || 0).toLocaleString('ko-KR') + '원';
            var totalStr = Number(svc.totalPrice || 0).toLocaleString('ko-KR') + '원';
            var dateStr = svc.serviceDate ? svc.serviceDate : '';

            var deleteBtn = '';
            if (!self.isReadonly && !self.isOta) {
                deleteBtn = '<button class="btn btn-outline-danger btn-sm py-0 px-1 remove-service-btn" '
                    + 'data-leg-id="' + legId + '" data-service-id="' + svc.id + '">'
                    + '<i class="fas fa-times"></i></button>';
            }

            html += '<tr>'
                + '<td class="small">' + HolaPms.escapeHtml(name) + (dateStr ? ' <span class="text-muted">(' + dateStr + ')</span>' : '') + '</td>'
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
     * 서비스 옵션 드롭다운 구성
     */
    populateServiceOptions: function(seq) {
        var self = this;
        var $select = $('#serviceAddForm_' + seq + ' .service-option-select');
        $select.find('option:not(:first)').remove();

        if (self.paidServiceOptions && self.paidServiceOptions.length > 0) {
            self.paidServiceOptions.forEach(function(opt) {
                var price = Number(opt.vatIncludedPrice || 0).toLocaleString('ko-KR');
                $select.append('<option value="' + opt.id + '">'
                    + HolaPms.escapeHtml(opt.serviceNameKo) + ' (' + price + '원)'
                    + '</option>');
            });
        }
    },

    /**
     * 유료 서비스 추가 API 호출
     */
    addServiceToLeg: function(legSeq, legId) {
        var self = this;
        var $form = $('#serviceAddForm_' + legSeq);
        var serviceOptionId = $form.find('.service-option-select').val();
        var quantity = parseInt($form.find('.service-qty').val()) || 1;
        var serviceDate = $form.find('.service-date').val() || null;

        if (!serviceOptionId) {
            HolaPms.alert('warning', '서비스를 선택해주세요.');
            return;
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
    removeServiceFromLeg: function(legId, serviceId) {
        var self = this;

        HolaPms.confirm('이 서비스를 삭제하시겠습니까?', function() {
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
            $('#saveBtn, #addRoomBtn, #addMemoBtn, #processPaymentBtn, #addAdjustmentBtn').hide();
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
            // OTA 모드: 서비스 추가/삭제 비활성화
            $('.add-service-btn, .remove-service-btn').hide();
        }

        // CHECK_IN / INHOUSE: 일부 수정만 허용
        var status = self.reservationData ? self.reservationData.reservationStatus : '';
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
    buildStatusChangeMenu: function(currentStatus) {
        var self = this;
        var $menu = $('#statusChangeMenu');
        $menu.empty();

        var transitions = self.STATUS_TRANSITIONS[currentStatus];
        if (!transitions || transitions.length === 0) {
            $('#statusChangeGroup').hide();
            return;
        }

        $('#statusChangeGroup').show();
        transitions.forEach(function(t) {
            var $li = $('<li><a class="dropdown-item" href="javascript:void(0);">'
                + '<i class="fas ' + t.icon + ' me-1"></i>' + t.label
                + '</a></li>');
            $li.find('a').on('click', function() {
                self.confirmStatusChange(t.status, t.label);
            });
            $menu.append($li);
        });
    },

    /**
     * 상태 변경 확인 모달
     */
    confirmStatusChange: function(newStatus, label) {
        var self = this;
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
     * 상태 변경 API 호출
     */
    changeStatus: function(newStatus) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/status',
            type: 'PUT',
            data: { newStatus: newStatus },
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '상태가 변경되었습니다.');
                    setTimeout(function() { location.reload(); }, 500);
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
            self.reload();
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

        // 객실타입 모달 검색
        $('#roomTypeSearchAction').on('click', function() { self.searchRoomType(); });
        $('#roomTypeSearchKeyword').on('keypress', function(e) { if (e.which === 13) self.searchRoomType(); });
        $('#roomTypeApplyBtn').on('click', function() { self.applyRoomType(); });

        // 층/호수 모달
        $('#assignFloorSelect').on('change', function() { self.loadRoomNumbers($(this).val()); });
        $('#roomAssignApplyBtn').on('click', function() { self.applyRoomAssign(); });

        // 객실 레그 삭제 (이벤트 위임)
        $(document).on('click', '.remove-leg-btn', function() {
            var $btn = $(this);
            var legSeq = $btn.data('leg');
            var legId = $btn.data('leg-id');

            if (legId) {
                // 서버에 존재하는 레그 → API 삭제
                HolaPms.confirm('이 객실을 삭제하시겠습니까?', function() {
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
                            }
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
        });

        // 층/호수 초기화 (이벤트 위임)
        $(document).on('click', '.room-assign-clear-btn', function() {
            var $leg = $(this).closest('.room-leg-card');
            $leg.find('.floor-id').val('');
            $leg.find('.room-number-id').val('');
            $leg.find('.room-number-display').val('');
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
            }
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
            self.removeServiceFromLeg(legId, serviceId);
        });

        // 결제 처리 버튼
        $('#processPaymentBtn').on('click', function() {
            HolaPms.modal.show('#paymentConfirmModal');
        });
        $('#paymentConfirmBtn').on('click', function() {
            HolaPms.modal.hide('#paymentConfirmModal');
            ReservationPayment.processPayment();
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
                    { data: 'rateCodeName', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
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
                                + '" data-name="' + HolaPms.escapeHtml(row.rateCodeName || '') + '">';
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
        var selected = $('input[name="rateCodeSelect"]:checked');
        if (!selected.length) { HolaPms.alert('warning', '레이트코드를 선택해주세요.'); return; }
        $('#rateCodeId').val(selected.val());
        $('#rateCodeName').val(selected.data('code') + ' - ' + selected.data('name'));
        HolaPms.modal.hide('#rateCodeModal');
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
                                + '" data-name="' + HolaPms.escapeHtml(row.marketName || '') + '">';
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

        if (self.roomTypeTable) {
            self.roomTypeTable.ajax.url('/api/v1/properties/' + self.propertyId + '/room-types').load();
        } else {
            self.roomTypeTable = $('#roomTypeSelectTable').DataTable({
                processing: true, serverSide: false, paging: true, pageLength: 10,
                ordering: false, searching: true, dom: 'rtip', info: false,
                language: HolaPms.dataTableLanguage,
                ajax: {
                    url: '/api/v1/properties/' + self.propertyId + '/room-types',
                    dataSrc: function(json) {
                        return (json.data || []).filter(function(item) { return item.useYn === true; });
                    },
                    error: function(xhr) { HolaPms.handleAjaxError(xhr); }
                },
                columns: [
                    { data: null, className: 'text-center', render: function(d, t, r, m) { return m.row + 1; } },
                    { data: 'roomTypeCode', render: function(d) { return HolaPms.escapeHtml(d); } },
                    { data: 'roomClassName', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    { data: 'roomSize', className: 'text-center', render: function(d) { return d ? d + ' m&sup2;' : '-'; } },
                    {
                        data: null, className: 'text-center',
                        render: function(d) { return '성인 ' + (d.maxAdults || 0) + ' / 아동 ' + (d.maxChildren || 0); }
                    },
                    {
                        data: null, className: 'text-center',
                        render: function(d, t, row) {
                            return '<input type="radio" name="roomTypeSelect" value="' + row.id
                                + '" data-code="' + HolaPms.escapeHtml(row.roomTypeCode || '') + '">';
                        }
                    }
                ]
            });
        }
        HolaPms.modal.show('#roomTypeModal');
    },

    searchRoomType: function() {
        if (!this.roomTypeTable) return;
        this.roomTypeTable.search($.trim($('#roomTypeSearchKeyword').val())).draw();
    },

    applyRoomType: function() {
        var self = this;
        var selected = $('input[name="roomTypeSelect"]:checked');
        if (!selected.length) { HolaPms.alert('warning', '객실타입을 선택해주세요.'); return; }

        var $leg = $('#roomLeg_' + self.currentLegSeq);
        $leg.find('.room-type-id').val(selected.val());
        $leg.find('.room-type-name').val(selected.data('code'));
        HolaPms.modal.hide('#roomTypeModal');
    },

    // ═══════════════════════════════════════════
    // 층/호수 선택 모달
    // ═══════════════════════════════════════════

    openRoomAssignModal: function(legSeq) {
        var self = this;
        if (!self.propertyId) { HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.'); return; }
        self.currentLegSeq = legSeq;

        // 현재 레그의 체크인/체크아웃, 서브예약 ID 가져오기
        var $leg = $('#roomLeg_' + legSeq);
        self.assignCheckIn = $leg.find('.leg-check-in').val() || '';
        self.assignCheckOut = $leg.find('.leg-check-out').val() || '';
        self.assignExcludeSubId = $leg.data('leg-id') || '';

        var $select = $('#assignFloorSelect');
        $select.find('option:not(:first)').remove();
        $('#roomNumberList').html('<p class="text-muted text-center py-3">층을 먼저 선택하세요</p>');

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/floors',
            type: 'GET',
            success: function(res) {
                var floors = res.data || [];
                floors.forEach(function(f) {
                    $select.append('<option value="' + f.id + '">'
                        + HolaPms.escapeHtml(f.floorNumber + (f.floorName ? ' | ' + f.floorName : ''))
                        + '</option>');
                });
            }
        });

        HolaPms.modal.show('#roomAssignModal');
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
                            var conflictInfo = '';
                            if (r.conflictReservationNo) {
                                conflictInfo = HolaPms.escapeHtml(r.conflictReservationNo);
                                if (r.conflictGuestName) conflictInfo += ' / ' + HolaPms.escapeHtml(r.conflictGuestName);
                                if (r.conflictCheckIn && r.conflictCheckOut) conflictInfo += '<br><small>' + r.conflictCheckIn + ' ~ ' + r.conflictCheckOut + '</small>';
                            }
                            html += '<td><span class="badge bg-danger">불가</span></td>'
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

    applyRoomAssign: function() {
        var self = this;
        var floorId = $('#assignFloorSelect').val();
        var $selected = $('input[name="roomNumberRadio"]:checked');

        if (!floorId || $selected.length === 0) {
            HolaPms.alert('warning', '층과 호수를 모두 선택해주세요.');
            return;
        }

        var roomNumberId = $selected.val();
        var floorText = $('#assignFloorSelect option:selected').text();
        var roomText = $selected.data('room-number') + ($selected.data('desc') ? ' | ' + $selected.data('desc') : '');

        var $leg = $('#roomLeg_' + self.currentLegSeq);
        $leg.find('.floor-id').val(floorId);
        $leg.find('.room-number-id').val(roomNumberId);
        $leg.find('.room-number-display').val(floorText + ' / ' + roomText);
        HolaPms.modal.hide('#roomAssignModal');
    },

    // ═══════════════════════════════════════════
    // 폼 데이터 수집 & 저장
    // ═══════════════════════════════════════════

    /**
     * 전체 폼 데이터 수집
     */
    collectFormData: function() {
        var self = this;
        // 서브 예약 (객실 레그) 수집
        var subReservations = [];
        $('.room-leg-card').each(function() {
            var $leg = $(this);
            var roomTypeId = HolaPms.form.intVal($leg.find('.room-type-id'));
            if (!roomTypeId) return; // 객실타입 미선택 레그 스킵

            var legData = {
                roomTypeId: roomTypeId,
                floorId: HolaPms.form.intVal($leg.find('.floor-id')),
                roomNumberId: HolaPms.form.intVal($leg.find('.room-number-id')),
                checkIn: HolaPms.form.val($leg.find('.leg-check-in')),
                checkOut: HolaPms.form.val($leg.find('.leg-check-out')),
                adults: parseInt($leg.find('.leg-adults').val()) || 1,
                children: parseInt($leg.find('.leg-children').val()) || 0,
                earlyCheckIn: $leg.find('.leg-early-checkin').is(':checked'),
                lateCheckOut: $leg.find('.leg-late-checkout').is(':checked')
            };

            // 기존 레그 ID가 있으면 포함
            var legId = $leg.data('leg-id');
            if (legId) legData.id = legId;

            subReservations.push(legData);
        });

        return {
            // 예약 기본 정보
            masterCheckIn: HolaPms.form.val('#masterCheckIn'),
            masterCheckOut: HolaPms.form.val('#masterCheckOut'),
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
        if (!data.masterCheckIn) {
            HolaPms.alert('warning', '체크인 날짜를 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
            $('#masterCheckIn').focus();
            return false;
        }
        if (!data.masterCheckOut) {
            HolaPms.alert('warning', '체크아웃 날짜를 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
            $('#masterCheckOut').focus();
            return false;
        }
        if (data.masterCheckIn >= data.masterCheckOut) {
            HolaPms.alert('warning', '체크아웃 날짜는 체크인 날짜 이후여야 합니다.');
            $('a[href="#tabReservation"]').tab('show');
            $('#masterCheckOut').focus();
            return false;
        }
        if (!data.guestNameKo && !data.guestLastNameEn) {
            HolaPms.alert('warning', '예약자명(국문) 또는 영문 성을 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
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
            if (sub.checkIn >= sub.checkOut) {
                HolaPms.alert('warning', '객실 #' + (i + 1) + '의 체크아웃은 체크인 이후여야 합니다.');
                $('a[href="#tabDetail"]').tab('show');
                return false;
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

        // 1단계: 예약 정보 저장
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId,
            type: 'PUT',
            data: data,
            success: function(res) {
                if (res.success) {
                    // 2단계: 예치금 저장
                    self.saveDeposit(function() {
                        HolaPms.alert('success', '예약이 수정되었습니다.');
                        setTimeout(function() { self.loadData(); }, 500);
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
                    HolaPms.alertAndRedirect('success', '예약이 삭제되었습니다.', '/admin/reservations');
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
};

// 초기화
$(document).ready(function() {
    ReservationDetail.init();
});
