/**
 * 예약 등록 폼 페이지
 */
var ReservationForm = {
    propertyId: null,
    roomLegSeq: 0,          // 객실 레그 시퀀스
    currentLegSeq: null,     // 현재 모달 대상 레그 시퀀스

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

        // 날짜 범위 바인딩
        HolaPms.bindDateRange('#masterCheckIn', '#masterCheckOut');
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

        $.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservation-channels',
            method: 'GET',
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
        $('#roomTypeSearchAction').on('click', function() { self.searchRoomType(); });
        $('#roomTypeSearchKeyword').on('keypress', function(e) {
            if (e.which === 13) self.searchRoomType();
        });
        $('#roomTypeApplyBtn').on('click', function() { self.applyRoomType(); });

        // 층/호수 모달 - 층 변경 시 호수 로드
        $('#assignFloorSelect').on('change', function() {
            self.loadRoomNumbers($(this).val());
        });
        $('#roomAssignApplyBtn').on('click', function() { self.applyRoomAssign(); });

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
    },

    // ═══════════════════════════════════════════
    // 객실 레그 (Tab 2: 상세정보)
    // ═══════════════════════════════════════════

    /**
     * 객실 레그 카드 동적 추가
     */
    addRoomLeg: function() {
        this.roomLegSeq++;
        var seq = this.roomLegSeq;

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
            + '        </div>'
            + '        <input type="hidden" class="room-type-id">'
            + '      </div>'
            + '      <label class="col-sm-2 col-form-label">층/호수</label>'
            + '      <div class="col-sm-4">'
            + '        <div class="input-group">'
            + '          <input type="text" class="form-control room-number-display" readonly placeholder="미배정">'
            + '          <button class="btn btn-outline-secondary room-assign-btn" type="button"><i class="fas fa-search"></i></button>'
            + '        </div>'
            + '        <input type="hidden" class="floor-id">'
            + '        <input type="hidden" class="room-number-id">'
            + '      </div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">체크인</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-in" value="' + masterCheckIn + '"></div>'
            + '      <label class="col-sm-2 col-form-label">체크아웃</label>'
            + '      <div class="col-sm-4"><input type="date" class="form-control leg-check-out" value="' + masterCheckOut + '"></div>'
            + '    </div>'
            + '    <div class="row mb-3">'
            + '      <label class="col-sm-2 col-form-label">성인</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-adults" value="1" min="1" max="99"></div>'
            + '      <label class="col-sm-2 col-form-label">아동</label>'
            + '      <div class="col-sm-2"><input type="number" class="form-control leg-children" value="0" min="0" max="99"></div>'
            + '      <label class="col-sm-2 col-form-label">얼리체크인</label>'
            + '      <div class="col-sm-2">'
            + '        <div class="form-check mt-2">'
            + '          <input class="form-check-input leg-early-checkin" type="checkbox">'
            + '        </div>'
            + '      </div>'
            + '    </div>'
            + '  </div>'
            + '</div>';

        $('#roomLegsContainer').append(legHtml);
        this.updateRoomLegsEmpty();
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

        // DataTable 초기화 또는 리로드
        if (self.rateCodeTable) {
            self.rateCodeTable.ajax.url('/api/v1/properties/' + self.propertyId + '/rate-codes').load();
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
                    url: '/api/v1/properties/' + self.propertyId + '/rate-codes',
                    dataSrc: function(json) {
                        // useYn=true만 필터링
                        return (json.data || []).filter(function(item) {
                            return item.useYn === true;
                        });
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
                    { data: 'marketCodeName', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    { data: 'description', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    {
                        data: null, className: 'text-center',
                        render: function(d, t, row) {
                            return '<input type="radio" name="marketCodeSelect" value="' + row.id
                                + '" data-code="' + HolaPms.escapeHtml(row.marketCode || '')
                                + '" data-name="' + HolaPms.escapeHtml(row.marketCodeName || '') + '">';
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

        self.currentLegSeq = legSeq;
        $('#roomTypeSearchKeyword').val('');

        if (self.roomTypeTable) {
            self.roomTypeTable.ajax.url('/api/v1/properties/' + self.propertyId + '/room-types').load();
        } else {
            self.roomTypeTable = $('#roomTypeSelectTable').DataTable({
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
                    url: '/api/v1/properties/' + self.propertyId + '/room-types',
                    dataSrc: function(json) {
                        return (json.data || []).filter(function(item) {
                            return item.useYn === true;
                        });
                    },
                    error: function(xhr) { HolaPms.handleAjaxError(xhr); }
                },
                columns: [
                    { data: null, className: 'text-center', render: function(d, t, r, m) { return m.row + 1; } },
                    { data: 'roomTypeCode', render: function(d) { return HolaPms.escapeHtml(d); } },
                    { data: 'roomClassName', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    {
                        data: 'roomSize', className: 'text-center',
                        render: function(d) { return d ? d + ' m&sup2;' : '-'; }
                    },
                    {
                        data: null, className: 'text-center',
                        render: function(d) {
                            var adults = d.maxAdults || 0;
                            var children = d.maxChildren || 0;
                            return '성인 ' + adults + ' / 아동 ' + children;
                        }
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
        var keyword = $.trim($('#roomTypeSearchKeyword').val());
        this.roomTypeTable.search(keyword).draw();
    },

    applyRoomType: function() {
        var self = this;
        var selected = $('input[name="roomTypeSelect"]:checked');
        if (!selected.length) {
            HolaPms.alert('warning', '객실타입을 선택해주세요.');
            return;
        }

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
        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        self.currentLegSeq = legSeq;
        var $select = $('#assignFloorSelect');
        $select.find('option:not(:first)').remove();
        $('#assignRoomNumberSelect').html('<option value="">층을 먼저 선택하세요</option>');

        // 층 목록 로드
        $.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/floors',
            method: 'GET',
            success: function(res) {
                var floors = res.data || [];
                floors.forEach(function(f) {
                    $select.append('<option value="' + f.id + '">'
                        + HolaPms.escapeHtml(f.floorNumber + (f.floorName ? ' | ' + f.floorName : ''))
                        + '</option>');
                });
            },
            error: function(xhr) { HolaPms.handleAjaxError(xhr); }
        });

        HolaPms.modal.show('#roomAssignModal');
    },

    /**
     * 선택된 층의 호수 목록 로드
     */
    loadRoomNumbers: function(floorId) {
        var self = this;
        var $select = $('#assignRoomNumberSelect');
        $select.html('<option value="">선택</option>');

        if (!floorId) return;

        $.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/floors/' + floorId + '/room-numbers',
            method: 'GET',
            success: function(res) {
                var rooms = res.data || [];
                rooms.forEach(function(r) {
                    $select.append('<option value="' + r.id + '">'
                        + HolaPms.escapeHtml(r.roomNumber + (r.descriptionKo ? ' | ' + r.descriptionKo : ''))
                        + '</option>');
                });
            },
            error: function(xhr) { HolaPms.handleAjaxError(xhr); }
        });
    },

    applyRoomAssign: function() {
        var self = this;
        var floorId = $('#assignFloorSelect').val();
        var roomNumberId = $('#assignRoomNumberSelect').val();

        if (!floorId || !roomNumberId) {
            HolaPms.alert('warning', '층과 호수를 모두 선택해주세요.');
            return;
        }

        var floorText = $('#assignFloorSelect option:selected').text();
        var roomText = $('#assignRoomNumberSelect option:selected').text();

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

            subReservations.push({
                roomTypeId: roomTypeId,
                floorId: HolaPms.form.intVal($leg.find('.floor-id')),
                roomNumberId: HolaPms.form.intVal($leg.find('.room-number-id')),
                checkInDate: HolaPms.form.val($leg.find('.leg-check-in')),
                checkOutDate: HolaPms.form.val($leg.find('.leg-check-out')),
                adults: parseInt($leg.find('.leg-adults').val()) || 1,
                children: parseInt($leg.find('.leg-children').val()) || 0,
                earlyCheckIn: $leg.find('.leg-early-checkin').is(':checked')
            });
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
            checkInDate: HolaPms.form.val('#masterCheckIn'),
            checkOutDate: HolaPms.form.val('#masterCheckOut'),
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
        if (!data.checkInDate) {
            HolaPms.alert('warning', '체크인 날짜를 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
            $('#masterCheckIn').focus();
            return false;
        }
        if (!data.checkOutDate) {
            HolaPms.alert('warning', '체크아웃 날짜를 입력해주세요.');
            $('a[href="#tabReservation"]').tab('show');
            $('#masterCheckOut').focus();
            return false;
        }
        if (data.checkInDate >= data.checkOutDate) {
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
            if (!sub.checkInDate || !sub.checkOutDate) {
                HolaPms.alert('warning', '객실 #' + (i + 1) + '의 체크인/체크아웃 날짜를 입력해주세요.');
                $('a[href="#tabDetail"]').tab('show');
                return false;
            }
            if (sub.checkInDate >= sub.checkOutDate) {
                HolaPms.alert('warning', '객실 #' + (i + 1) + '의 체크아웃은 체크인 이후여야 합니다.');
                $('a[href="#tabDetail"]').tab('show');
                return false;
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

        $.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(res) {
                if (res.success) {
                    var reservationId = res.data ? (res.data.id || '') : '';
                    var redirectUrl = reservationId
                        ? '/admin/reservations/' + reservationId
                        : '/admin/reservations';
                    HolaPms.alertAndRedirect('success', '예약이 등록되었습니다.', redirectUrl);
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    }
};

// 초기화
$(document).ready(function() {
    ReservationForm.init();
});
