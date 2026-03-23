/**
 * 예약 등록 폼 페이지
 */
var ReservationForm = {
    propertyId: null,
    roomLegSeq: 0,          // 객실 레그 시퀀스
    currentLegSeq: null,     // 현재 모달 대상 레그 시퀀스

    // 유료 서비스 옵션 캐시 (roomTypeId별)
    serviceOptionsCache: {},

    // 모달 DataTable 인스턴스
    rateCodeTable: null,
    marketCodeTable: null,
    roomTypeTable: null,

    init: function() {
        var self = this;
        self.propertyId = HolaPms.context.getPropertyId();

        if (self.propertyId) {
            self.showForm();
            self.loadReservationChannels();
        } else {
            self.hideForm();
        }

        self.bindEvents();
        self.setDefaultDates();
    },

    /**
     * 프로퍼티 선택 상태에 따라 폼/알림 토글
     */
    showForm: function() {
        $('#contextAlert').addClass('d-none');
        $('#formContainer').show();
    },

    hideForm: function() {
        $('#contextAlert').removeClass('d-none');
        $('#formContainer').hide();
    },

    /**
     * 체크인/체크아웃 기본 날짜 설정
     * URL 파라미터 checkInDate가 있으면 해당 날짜 사용, 없으면 오늘/내일
     */
    setDefaultDates: function() {
        var params = new URLSearchParams(window.location.search);
        var checkInParam = params.get('checkInDate');

        var checkInDate, checkOutDate;
        if (checkInParam && /^\d{4}-\d{2}-\d{2}$/.test(checkInParam)) {
            checkInDate = new Date(checkInParam + 'T00:00:00');
            checkOutDate = new Date(checkInDate);
            checkOutDate.setDate(checkOutDate.getDate() + 1);
        } else {
            checkInDate = new Date();
            checkOutDate = new Date(checkInDate);
            checkOutDate.setDate(checkOutDate.getDate() + 1);
        }

        $('#masterCheckIn').val(this.formatDate(checkInDate));
        $('#masterCheckOut').val(this.formatDate(checkOutDate));

        // 예약 전용 날짜 바인딩 (체크인 우선 → 체크아웃 자동 보정)
        this.bindReservationDates('#masterCheckIn', '#masterCheckOut');
    },

    /**
     * 예약 전용 날짜 바인딩
     * - 체크인 변경 → 체크아웃을 체크인+1일로 자동 보정 + min 설정
     * - 체크아웃은 체크인에 max를 걸지 않음 (단방향: 체크인 먼저 자유 선택)
     */
    bindReservationDates: function(startSel, endSel) {
        var self = this;
        var $start = $(startSel);
        var $end = $(endSel);

        $start.on('change', function() {
            var startVal = $(this).val();
            if (!startVal) return;

            // 체크아웃 min = 체크인 다음날
            var nextDay = new Date(startVal + 'T00:00:00');
            nextDay.setDate(nextDay.getDate() + 1);
            var nextDayStr = self.formatDate(nextDay);
            $end.attr('min', nextDayStr);

            // 체크아웃이 체크인 이하이면 체크인+1일로 자동 보정
            if (!$end.val() || $end.val() <= startVal) {
                $end.val(nextDayStr);
            }
        });

        $end.on('change', function() {
            var endVal = $(this).val();
            if (!endVal) return;

            // 체크인이 체크아웃 이후이면 체크아웃-1일로 보정
            if ($start.val() && $start.val() >= endVal) {
                var prevDay = new Date(endVal + 'T00:00:00');
                prevDay.setDate(prevDay.getDate() - 1);
                $start.val(self.formatDate(prevDay));
            }
        });

        // 초기 min 설정: 체크아웃은 체크인 다음날부터 선택 가능
        if ($start.val()) {
            var initNext = new Date($start.val() + 'T00:00:00');
            initNext.setDate(initNext.getDate() + 1);
            $end.attr('min', self.formatDate(initNext));
        }
        // 체크인에는 max를 걸지 않음 → 미래 날짜 자유 선택
    },

    /**
     * Date 객체를 yyyy-MM-dd 형식으로 변환
     */
    formatDate: function(date) {
        var y = date.getFullYear();
        var m = ('0' + (date.getMonth() + 1)).slice(-2);
        var d = ('0' + date.getDate()).slice(-2);
        return y + '-' + m + '-' + d;
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
     * 이벤트 바인딩
     */
    bindEvents: function() {
        var self = this;

        // 프로퍼티 컨텍스트 변경
        $(document).on('hola:contextChange', function() {
            self.propertyId = HolaPms.context.getPropertyId();
            if (self.propertyId) {
                self.showForm();
                self.loadReservationChannels();
            } else {
                self.hideForm();
            }
        });

        // 레이트코드 검색 버튼
        $('#rateCodeSearchBtn').on('click', function() {
            self.openRateCodeModal();
        });

        // 마켓코드 검색 버튼
        $('#marketCodeSearchBtn').on('click', function() {
            self.openMarketCodeModal();
        });

        // 레이트코드 초기화
        $('#rateCodeClearBtn').on('click', function() {
            $('#rateCodeId').val('');
            $('#rateCodeName').val('');
        });

        // 마켓코드 초기화
        $('#marketCodeClearBtn').on('click', function() {
            $('#marketCodeId').val('');
            $('#marketCodeName').val('');
        });

        // 객실 추가 버튼
        $('#addRoomBtn').on('click', function() {
            self.addRoomLeg();
        });

        // 예치 방법 변경 → 카드 섹션 토글
        $('#depositMethod').on('change', function() {
            if ($(this).val() === 'CREDIT_CARD') {
                $('#creditCardSection').show();
            } else {
                $('#creditCardSection').hide();
            }
        });

        // 저장 버튼
        $('#saveBtn').on('click', function() {
            self.save();
        });

        // 레이트코드 모달 검색
        $('#rateCodeSearchAction').on('click', function() { self.searchRateCode(); });
        $('#rateCodeSearchKeyword').on('keypress', function(e) {
            if (e.which === 13) self.searchRateCode();
        });
        $('#rateCodeApplyBtn').on('click', function() { self.applyRateCode(); });

        // 마켓코드 모달 검색
        $('#marketCodeSearchAction').on('click', function() { self.searchMarketCode(); });
        $('#marketCodeSearchKeyword').on('keypress', function(e) {
            if (e.which === 13) self.searchMarketCode();
        });
        $('#marketCodeApplyBtn').on('click', function() { self.applyMarketCode(); });

        // 객실타입 모달 검색
        $('#roomTypeSearchAction').on('click', function() { self.filterRoomTypeList(); });
        $('#roomTypeSearchKeyword').on('keypress', function(e) {
            if (e.which === 13) self.filterRoomTypeList();
        });
        $('#roomTypeApplyBtn').on('click', function() { self.applyRoomType(); });
        $('#showAllRoomTypes').on('change', function() { self.filterRoomTypeList(); });

        // 객실 배정 모달 - 탭 전환
        $(document).on('click', '#roomAssignModal [data-assign-tab]', function() {
            var tab = $(this).data('assign-tab');
            $('#roomAssignModal [data-assign-tab]').removeClass('active');
            $(this).addClass('active');
            self.renderAssignContent(tab);
        });
        // 객실 배정 적용
        $('#btnApplyRoomAssign').on('click', function() { self.applyRoomAssign(); });

        // 객실 레그 삭제 (이벤트 위임)
        $(document).on('click', '.remove-leg-btn', function() {
            var legId = $(this).data('leg');
            $('#roomLeg_' + legId).fadeOut(200, function() {
                $(this).remove();
                self.updateRoomLegsEmpty();
            });
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

        // 얼리체크인 토글 버튼 (이벤트 위임)
        $(document).on('click', '.early-checkin-toggle', function() {
            var $group = $(this).closest('.btn-group');
            var val = $(this).data('value');
            $group.prev('.leg-early-checkin').val(String(val));
            $group.find('.btn').removeClass('btn-primary btn-outline-danger').addClass('btn-outline-secondary');
            if (val === true) {
                $(this).removeClass('btn-outline-secondary').addClass('btn-primary');
            } else {
                $(this).removeClass('btn-outline-secondary').addClass('btn-outline-danger');
            }
        });

        // 레이트체크아웃 토글 버튼 (이벤트 위임)
        $(document).on('click', '.late-checkout-toggle', function() {
            var $group = $(this).closest('.btn-group');
            var val = $(this).data('value');
            $group.prev('.leg-late-checkout').val(String(val));
            $group.find('.btn').removeClass('btn-primary btn-outline-danger').addClass('btn-outline-secondary');
            if (val === true) {
                $(this).removeClass('btn-outline-secondary').addClass('btn-primary');
            } else {
                $(this).removeClass('btn-outline-secondary').addClass('btn-outline-danger');
            }
        });

        // 서비스 추가 버튼 (이벤트 위임)
        $(document).on('click', '.add-service-selection-btn', function() {
            var legSeq = $(this).data('leg');
            self.addServiceRow(legSeq);
        });

        // 서비스 삭제 버튼 (이벤트 위임)
        $(document).on('click', '.remove-service-row-btn', function() {
            $(this).closest('.service-row').remove();
            var legSeq = $(this).data('leg');
            var $container = $('#serviceSelections_' + legSeq);
            if ($container.find('.service-row').length === 0) {
                $container.find('.service-empty-msg').show();
            }
        });
    },

    // ═══════════════════════════════════════════
    // 객실 레그 (Tab 2: 상세정보)
    // ═══════════════════════════════════════════

    /**
     * 객실 레그 카드 동적 추가
     */
    addRoomLeg: function() {
        var self = this;
        self.roomLegSeq++;
        var seq = self.roomLegSeq;

        // 마스터 체크인/체크아웃 기본값 세팅
        var masterCheckIn = $('#masterCheckIn').val() || '';
        var masterCheckOut = $('#masterCheckOut').val() || '';

        var legHtml = ''
            + '<div class="card border shadow-sm mb-3 room-leg-card" id="roomLeg_' + seq + '" data-leg-seq="' + seq + '">'
            + '  <div class="card-header d-flex justify-content-between align-items-center">'
            + '    <span>객실 #' + seq + '</span>'
            + '    <button class="btn btn-outline-danger btn-sm remove-leg-btn" data-leg="' + seq + '">'
            + '      <i class="fas fa-times"></i>'
            + '    </button>'
            + '  </div>'
            + '  <div class="card-body">'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label required">객실타입</label>'
            + '      <div class="col-sm-4">'
            + '        <div class="input-group">'
            + '          <input type="text" class="form-control room-type-name" readonly placeholder="선택">'
            + '          <button class="btn btn-outline-secondary room-type-search-btn" type="button"><i class="fas fa-search"></i></button>'
            + '          <button class="btn btn-outline-danger room-type-clear-btn" type="button"><i class="fas fa-times"></i></button>'
            + '        </div>'
            + '        <input type="hidden" class="room-type-id">'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label">층/호수</label>'
            + '      <div class="col-sm-4">'
            + '        <div class="input-group">'
            + '          <input type="text" class="form-control room-number-display" readonly placeholder="미배정">'
            + '          <button class="btn btn-outline-secondary room-assign-btn" type="button"><i class="fas fa-search"></i></button>'
            + '          <button class="btn btn-outline-danger room-assign-clear-btn" type="button"><i class="fas fa-times"></i></button>'
            + '        </div>'
            + '        <input type="hidden" class="floor-id">'
            + '        <input type="hidden" class="room-number-id">'
            + '      </div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">체크인</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-in" value="' + masterCheckIn + '" min="' + self.formatDate(new Date()) + '"></div>'
            + '      <label class="col-sm-2 col-form-label">체크아웃</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-out" value="' + masterCheckOut + '" min="' + self.formatDate(new Date()) + '"></div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">성인</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-adults" value="1" min="1" max="99"></div>'
            + '      <label class="col-sm-2 col-form-label">아동</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-children" value="0" min="0" max="99"></div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">얼리체크인</label>'
            + '      <div class="col-sm-2">'
            + '        <input type="hidden" class="leg-early-checkin" value="false">'
            + '        <div class="btn-group btn-group-sm w-100" role="group">'
            + '          <button type="button" class="btn btn-outline-secondary early-checkin-toggle" data-value="true">사용</button>'
            + '          <button type="button" class="btn btn-outline-danger early-checkin-toggle" data-value="false">미사용</button>'
            + '        </div>'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label">레이트체크아웃</label>'
            + '      <div class="col-sm-2">'
            + '        <input type="hidden" class="leg-late-checkout" value="false">'
            + '        <div class="btn-group btn-group-sm w-100" role="group">'
            + '          <button type="button" class="btn btn-outline-secondary late-checkout-toggle" data-value="true">사용</button>'
            + '          <button type="button" class="btn btn-outline-danger late-checkout-toggle" data-value="false">미사용</button>'
            + '        </div>'
            + '      </div>'
            + '    </div>'
            + '    <hr class="my-2">'
            + '    <div class="d-flex justify-content-between align-items-center mb-2">'
            + '      <span class="text-muted small"><i class="fas fa-concierge-bell me-1"></i>유료 서비스 (Add-on)</span>'
            + '      <button class="btn btn-outline-primary btn-sm add-service-selection-btn" data-leg="' + seq + '" type="button">'
            + '        <i class="fas fa-plus me-1"></i>서비스 추가'
            + '      </button>'
            + '    </div>'
            + '    <div class="service-selections" id="serviceSelections_' + seq + '">'
            + '      <div class="text-muted small text-center py-1 service-empty-msg">선택된 서비스가 없습니다.</div>'
            + '    </div>'
            + '  </div>'
            + '</div>';

        $('#roomLegsContainer').append(legHtml);
        self.updateRoomLegsEmpty();
    },

    /**
     * 객실 레그 빈 메시지 토글
     */
    /**
     * 서비스 선택 행 추가
     */
    addServiceRow: function(legSeq) {
        var self = this;
        var $leg = $('#roomLeg_' + legSeq);
        var roomTypeId = $leg.find('.room-type-id').val() || null;

        // 서비스 옵션 로드 (roomTypeId 기반 필터링)
        self.loadServiceOptions(roomTypeId, function(options) {
            var $container = $('#serviceSelections_' + legSeq);
            $container.find('.service-empty-msg').hide();

            var rowHtml = '<div class="row g-2 align-items-end mb-1 service-row">'
                + '  <div class="col-sm-6">'
                + '    <select class="form-select form-select-sm service-select">'
                + '      <option value="">서비스 선택</option>';

            if (options && options.length > 0) {
                options.forEach(function(opt) {
                    var price = Number(opt.vatIncludedPrice || 0).toLocaleString('ko-KR');
                    rowHtml += '<option value="' + opt.id + '">'
                        + HolaPms.escapeHtml(opt.serviceNameKo) + ' (' + price + '원)'
                        + '</option>';
                });
            }

            rowHtml += '    </select>'
                + '  </div>'
                + '  <div class="col-sm-3">'
                + '    <input type="number" class="form-control form-control-sm service-quantity" value="1" min="1" max="99">'
                + '  </div>'
                + '  <div class="col-sm-3">'
                + '    <button class="btn btn-outline-danger btn-sm w-100 remove-service-row-btn" data-leg="' + legSeq + '" type="button">'
                + '      <i class="fas fa-times"></i> 삭제'
                + '    </button>'
                + '  </div>'
                + '</div>';

            $container.append(rowHtml);
        });
    },

    /**
     * 유료 서비스 옵션 로드 (roomTypeId 기반 필터링, 캐싱)
     */
    loadServiceOptions: function(roomTypeId, callback) {
        var self = this;
        var cacheKey = roomTypeId || 'all';

        if (self.serviceOptionsCache[cacheKey]) {
            callback(self.serviceOptionsCache[cacheKey]);
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
                self.serviceOptionsCache[cacheKey] = options;
                callback(options);
            },
            error: function() {
                callback([]);
            }
        });
    },

    updateRoomLegsEmpty: function() {
        if ($('.room-leg-card').length > 0) {
            $('#roomLegsEmpty').hide();
        } else {
            $('#roomLegsEmpty').show();
        }
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

        // 체크인/체크아웃 날짜 기반 필터링 URL 구성
        var checkIn = $('#masterCheckIn').val();
        var checkOut = $('#masterCheckOut').val();
        var url = '/api/v1/properties/' + self.propertyId + '/rate-codes';
        if (checkIn && checkOut) {
            url += '?checkIn=' + checkIn + '&checkOut=' + checkOut;
        }

        $('#rateCodeSearchKeyword').val('');

        // DataTable 초기화 또는 리로드
        if (self.rateCodeTable) {
            self.rateCodeTable.ajax.url(url).load();
        } else {
            self.rateCodeTable = $('#rateCodeSelectTable').DataTable({
                processing: true,
                serverSide: false,
                paging: true,
                pageLength: 10,
                ordering: false,
                searching: true,
                dom: 'rtip',
                info: false,
                language: HolaPms.dataTableLanguage,
                ajax: {
                    url: url,
                    dataSrc: function(json) {
                        // checkIn/checkOut 파라미터가 있으면 서버에서 이미 필터링됨
                        return (json.data || []).filter(function(item) {
                            return item.useYn === true;
                        });
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
                            var start = d.saleStartDate || '-';
                            var end = d.saleEndDate || '-';
                            return HolaPms.escapeHtml(start + ' ~ ' + end);
                        }
                    },
                    {
                        data: null, className: 'text-center',
                        render: function(d, t, row) {
                            return '<input type="radio" name="rateCodeSelect" value="' + row.id
                                + '" data-code="' + HolaPms.escapeHtml(row.rateCode || '')
                                + '" data-name="' + HolaPms.escapeHtml(row.rateNameKo || '')
                                + '" data-stay-type="' + (row.stayType || 'OVERNIGHT') + '">';
                        }
                    }
                ]
            });
        }

        HolaPms.modal.show('#rateCodeModal');
    },

    searchRateCode: function() {
        if (!this.rateCodeTable) return;
        var keyword = $.trim($('#rateCodeSearchKeyword').val());
        this.rateCodeTable.search(keyword).draw();
    },

    applyRateCode: function() {
        var selected = $('input[name="rateCodeSelect"]:checked');
        if (!selected.length) {
            HolaPms.alert('warning', '레이트코드를 선택해주세요.');
            return;
        }
        $('#rateCodeId').val(selected.val());
        $('#rateCodeName').val(selected.data('code') + ' - ' + selected.data('name'));

        // Dayuse 레이트코드 선택 시 checkOut 자동 설정
        var stayType = selected.data('stay-type') || 'OVERNIGHT';
        // stayType을 히든 필드에 저장 (collectFormData에서 참조)
        if (!$('#rateCodeStayType').length) {
            $('<input type="hidden" id="rateCodeStayType">').appendTo('form');
        }
        $('#rateCodeStayType').val(stayType);
        if (stayType === 'DAY_USE') {
            var checkIn = $('#masterCheckIn').val();
            if (checkIn) {
                // checkOut = checkIn + 1일 (서버에서도 보정하지만 UX 일관성)
                var d = new Date(checkIn);
                d.setDate(d.getDate() + 1);
                var nextDay = d.toISOString().split('T')[0];
                $('#masterCheckOut').val(nextDay).prop('readonly', true);
                // 서브 예약 Leg의 체크아웃도 동기화
                $('.leg-check-out').val(nextDay).prop('readonly', true);
            }
            HolaPms.alert('info', 'Dayuse 레이트코드가 선택되었습니다. 체크아웃이 자동 설정됩니다.');
        } else {
            $('#masterCheckOut').prop('readonly', false);
            $('.leg-check-out').prop('readonly', false);
        }

        HolaPms.modal.hide('#rateCodeModal');
    },

    // ═══════════════════════════════════════════
    // 마켓코드 모달
    // ═══════════════════════════════════════════

    openMarketCodeModal: function() {
        var self = this;
        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $('#marketCodeSearchKeyword').val('');

        if (self.marketCodeTable) {
            self.marketCodeTable.ajax.url('/api/v1/properties/' + self.propertyId + '/market-codes').load();
        } else {
            self.marketCodeTable = $('#marketCodeSelectTable').DataTable({
                processing: true,
                serverSide: false,
                paging: true,
                pageLength: 10,
                ordering: false,
                searching: true,
                dom: 'rtip',
                info: false,
                language: HolaPms.dataTableLanguage,
                ajax: {
                    url: '/api/v1/properties/' + self.propertyId + '/market-codes',
                    dataSrc: function(json) {
                        return (json.data || []).filter(function(item) {
                            return item.useYn === true;
                        });
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
        var keyword = $.trim($('#marketCodeSearchKeyword').val());
        this.marketCodeTable.search(keyword).draw();
    },

    applyMarketCode: function() {
        var selected = $('input[name="marketCodeSelect"]:checked');
        if (!selected.length) {
            HolaPms.alert('warning', '마켓코드를 선택해주세요.');
            return;
        }
        $('#marketCodeId').val(selected.val());
        $('#marketCodeName').val(selected.data('code') + ' - ' + selected.data('name'));
        HolaPms.modal.hide('#marketCodeModal');
    },

    // ═══════════════════════════════════════════
    // 객실타입 모달
    // ═══════════════════════════════════════════

    openRoomTypeModal: function(legSeq) {
        var self = this;
        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }
        // 레이트코드 선택 필수
        var rateCodeId = $('#rateCodeId').val();
        if (!rateCodeId) {
            HolaPms.alert('warning', '레이트코드를 먼저 선택해주세요.');
            return;
        }

        self.currentLegSeq = legSeq;
        $('#roomTypeSearchKeyword').val('');
        $('#showAllRoomTypes').prop('checked', false);
        $('#roomTypeContent').html('<div class="text-center py-4 text-muted"><i class="fas fa-spinner fa-spin me-1"></i> 객실타입 정보를 불러오는 중...</div>');
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

        var items = self.allRoomTypes.filter(function(rt) {
            if (keyword) {
                var match = (rt.roomTypeCode || '').toLowerCase().indexOf(keyword) >= 0
                    || (rt.roomClassName || '').toLowerCase().indexOf(keyword) >= 0
                    || (rt.description || '').toLowerCase().indexOf(keyword) >= 0;
                if (!match) return false;
            }
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
        html += '<table class="table table-hover table-bordered mb-0" style="min-width:650px">';
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
                + '" data-code="' + HolaPms.escapeHtml(rt.roomTypeCode || '') + '"></td>';
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
        if (!selected.length) {
            HolaPms.alert('warning', '객실타입을 선택해주세요.');
            return;
        }

        var legSeq = self.currentLegSeq;
        var $leg = $('#roomLeg_' + legSeq);
        var oldRoomTypeId = $leg.find('.room-type-id').val();
        var newRoomTypeId = selected.val();

        $leg.find('.room-type-id').val(newRoomTypeId);
        $leg.find('.room-type-name').val(selected.data('code'));

        HolaPms.modal.hide('#roomTypeModal');

        // 객실타입 변경 시 기존 서비스 선택 초기화 + 캐시 갱신
        if (oldRoomTypeId !== newRoomTypeId) {
            var $container = $('#serviceSelections_' + legSeq);
            $container.find('.service-row').remove();
            $container.find('.service-empty-msg').show();
            // 새 roomTypeId 캐시 삭제 (다음 추가 시 새로 조회)
            delete self.serviceOptionsCache[newRoomTypeId];
        }
    },

    // ═══════════════════════════════════════════
    // 객실 배정 모달
    // ═══════════════════════════════════════════

    openRoomAssignModal: function(legSeq) {
        var self = this;
        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        self.currentLegSeq = legSeq;
        var $leg = $('#roomLeg_' + legSeq);
        var propertyId = self.propertyId;
        var roomTypeId = $leg.find('.room-type-id').val();
        var rateCodeId = $('#rateCodeId').val();
        var checkIn = $leg.find('.leg-check-in').val();
        var checkOut = $leg.find('.leg-check-out').val();
        var adults = $leg.find('.leg-adults').val() || 1;
        var children = $leg.find('.leg-children').val() || 0;

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

        // 탭 초기화 (추천 탭 활성)
        $('#roomAssignModal [data-assign-tab]').removeClass('active');
        $('#roomAssignModal [data-assign-tab="recommended"]').addClass('active');

        HolaPms.modal.show('#roomAssignModal');
        self.loadAssignAvailability(propertyId, roomTypeId, rateCodeId, checkIn, checkOut, adults, children, null);
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
                self.assignData = res.data;
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

            // 객실 스펙
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
                        html += disabled + '></td>';
                        html += '<td class="text-center">' + HolaPms.escapeHtml(room.roomNumber) + '</td>';
                        html += '<td class="text-center">' + (room.descriptionKo ? HolaPms.escapeHtml(room.descriptionKo) : '-') + '</td>';
                        var badge;
                        if (room.available) {
                            badge = '<span class="badge bg-success">가용</span>';
                        } else if (room.unavailableType === 'OOO') {
                            badge = '<span class="badge bg-secondary">OOO</span>';
                        } else if (room.unavailableType === 'OOS') {
                            badge = '<span class="badge" style="background:#e9ecef;color:#333">OOS</span>';
                        } else {
                            badge = '<span class="badge bg-danger">사용중</span>';
                        }
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
    collectFormData: function() {
        var self = this;

        // 레이트코드 stayType 확인 (Dayuse 여부)
        var rateCodeStayType = $('#rateCodeStayType').val() || 'OVERNIGHT';
        var isDayUse = rateCodeStayType === 'DAY_USE';

        // 서브 예약 (객실 레그) 수집
        var subReservations = [];
        $('.room-leg-card').each(function() {
            var $leg = $(this);
            var roomTypeId = HolaPms.form.intVal($leg.find('.room-type-id'));
            if (!roomTypeId) return; // 객실타입 미선택 레그 스킵

            // 선택 서비스 수집
            var services = [];
            $leg.find('.service-row').each(function() {
                var optId = parseInt($(this).find('.service-select').val());
                var qty = parseInt($(this).find('.service-quantity').val()) || 1;
                if (optId) {
                    services.push({ serviceOptionId: optId, quantity: qty });
                }
            });

            var legData = {
                roomTypeId: roomTypeId,
                floorId: HolaPms.form.intVal($leg.find('.floor-id')),
                roomNumberId: HolaPms.form.intVal($leg.find('.room-number-id')),
                checkIn: HolaPms.form.val($leg.find('.leg-check-in')),
                checkOut: HolaPms.form.val($leg.find('.leg-check-out')),
                adults: parseInt($leg.find('.leg-adults').val()) || 1,
                children: parseInt($leg.find('.leg-children').val()) || 0,
                earlyCheckIn: $leg.find('.leg-early-checkin').val() === 'true',
                lateCheckOut: $leg.find('.leg-late-checkout').val() === 'true',
                services: services.length > 0 ? services : null
            };

            // Dayuse 레이트코드인 경우 stayType과 이용시간 포함
            if (isDayUse) {
                legData.stayType = 'DAY_USE';
                // 레이트코드명에서 시간 추출 (예: DU-3H → 3, DU-5H → 5)
                var rcName = $('#rateCodeName').val() || '';
                var hoursMatch = rcName.match(/DU-(\d+)H/i);
                if (hoursMatch) {
                    legData.dayUseDurationHours = parseInt(hoursMatch[1]);
                }
            }

            subReservations.push(legData);
        });

        // 예치금 정보
        var deposit = null;
        var depositMethod = HolaPms.form.val('#depositMethod');
        if (depositMethod) {
            deposit = {
                depositMethod: depositMethod,
                depositAmount: parseInt($('#depositAmount').val()) || 0
            };

            if (depositMethod === 'CREDIT_CARD') {
                deposit.cardCompany = HolaPms.form.val('#cardCompany');
                deposit.cardNumber = HolaPms.form.val('#cardNumber');
                deposit.cardExpiryDate = HolaPms.form.val('#cardExpiryDate');
                deposit.cardCvc = HolaPms.form.val('#cardCvc');
            }
        }

        return {
            // 예약 기본 정보
            masterCheckIn: HolaPms.form.val('#masterCheckIn'),
            masterCheckOut: HolaPms.form.val('#masterCheckOut'),
            rateCodeId: HolaPms.form.intVal('#rateCodeId'),
            marketCodeId: HolaPms.form.intVal('#marketCodeId'),
            reservationChannelId: HolaPms.form.intVal('#reservationChannelId'),
            promotionCode: HolaPms.form.val('#promotionCode'),

            // 예약자 정보
            guestNameKo: HolaPms.form.val('#guestNameKo'),
            guestLastNameEn: HolaPms.form.val('#guestLastNameEn'),
            guestFirstNameEn: HolaPms.form.val('#guestFirstNameEn'),
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

            // 예치금 정보
            deposit: deposit,

            // 기타정보
            customerRequest: HolaPms.form.val('#customerRequest')
        };
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
            // 객실타입 최대 수용 인원 검증
            if (sub.roomTypeId && self.allRoomTypes) {
                var rt = self.allRoomTypes.find(function(t) { return t.id === sub.roomTypeId; });
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
     * 저장 (POST /api/v1/properties/{pid}/reservations)
     */
    save: function() {
        var self = this;

        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var data = self.collectFormData();
        if (!self.validate(data)) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations',
            type: 'POST',
            data: data,
            success: function(res) {
                if (res.success) {
                    // Walk-In 모드: 예약 목록으로 리다이렉트
                    var isWalkIn = new URLSearchParams(window.location.search).get('walkin') === 'true';
                    if (isWalkIn) {
                        HolaPms.alertAndRedirect('success', 'Walk-In 예약이 등록되었습니다.', '/admin/reservations');
                        return;
                    }
                    var reservationId = res.data ? (res.data.id || '') : '';
                    var redirectUrl = reservationId
                        ? '/admin/reservations/' + reservationId
                        : '/admin/reservations';
                    HolaPms.alertAndRedirect('success', '예약이 등록되었습니다.', redirectUrl);
                }
            }
        });
    }
};

// 초기화
$(document).ready(function() {
    ReservationForm.init();
});
