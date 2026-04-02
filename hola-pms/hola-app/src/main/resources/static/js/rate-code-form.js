/**
 * 레이트 관리 - 등록/수정 폼 페이지
 */
var RateCodeForm = {
    editId: null,
    duplicateChecked: false,
    selectedRoomTypes: [], // [{id, roomTypeCode, description}]

    // 옵션요금 데이터
    allPaidOptions: [],       // 프로퍼티의 전체 유료 옵션
    selectedOptionIds: new Set(), // 매핑된 옵션 ID Set

    init: function() {
        this.editId = $('#rateCodeId').val() || null;
        this.bindEvents();
        this.updateHotelPropertyName();
        this.loadTaxRate();

        // stayType 라디오 변경 시 Dayuse 탭 표시/숨김
        $('input[name="stayType"]').on('change', function() {
            RateCodeForm.toggleDayUseTab();
        });

        // Dayuse 요금 모달: 공급가 콤마 자동 포맷
        $('#dayUsePriceInput').on('input', function() {
            var raw = $(this).val().replace(/[^\d]/g, '');
            if (raw) {
                $(this).val(Number(raw).toLocaleString('ko-KR'));
            }
        });

        // Dayuse 요금 모달: Enter 키로 등록
        $('#dayUseRateModal').on('keydown', 'input', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                RateCodeForm.saveDayUseRate();
            }
        });

        if (this.editId) {
            this.loadData();
            this.loadPricingData();
            this.loadOptionPricingData();
        } else {
            this.setCreateMode();
        }
    },

    bindEvents: function() {
        var self = this;

        // 컨텍스트 변경 시 호텔/프로퍼티명 갱신
        $(document).on('hola:contextChange', function() {
            self.updateHotelPropertyName();
        });

        // 판매기간 날짜 범위 제한
        HolaPms.bindDateRange('#saleStartDate', '#saleEndDate');

        // 판매기간 변경 시 모든 요금행의 기간 캘린더 min/max 갱신
        $('#saleStartDate, #saleEndDate').on('change', function() {
            self.updatePricingDateLimits();
        });

        // 코드 입력 시 중복확인 리셋
        $('#rateCode').on('input', function() {
            self.duplicateChecked = false;
            $('#codeCheckResult').text('');
        });

        // 마켓코드 모달 엔터 검색
        $('#modalMarketCodeSearch').on('keypress', function(e) {
            if (e.which === 13) self.searchMarketCodes();
        });

        // 객실 타입 모달 엔터 검색
        $('#modalRoomTypeSearch').on('keypress', function(e) {
            if (e.which === 13) self.searchRoomTypes();
        });

        // 객실 전체 선택
        $('#roomTypeCheckAll').on('change', function() {
            $('#modalRoomTypeTable tbody input[type="checkbox"]').prop('checked', $(this).is(':checked'));
        });

        // 신규 등록 시 요금정보/옵션요금 탭 전환 차단
        $('#tab-pricing, #tab-option').on('show.bs.tab', function(e) {
            if (RateCodeForm.pricingTabBlocked) {
                e.preventDefault();
                HolaPms.alert('warning', '기본정보를 먼저 저장해주세요.');
            }
        });

        // 탭 전환 시 저장 버튼 텍스트 변경
        $('a[data-bs-toggle="tab"], button[data-bs-toggle="tab"]').on('shown.bs.tab', function(e) {
            var tabId = $(e.target).attr('id');
            if (tabId === 'tab-pricing') {
                $('#btnSave').html('<i class="fas fa-save me-1"></i>요금정보 저장');
            } else if (tabId === 'tab-option') {
                $('#btnSave').html('<i class="fas fa-save me-1"></i>옵션요금 저장');
            } else {
                $('#btnSave').html('<i class="fas fa-save me-1"></i>' + (RateCodeForm.editId ? '저장' : '등록'));
            }
        });

        // 요일 전체 선택 체크박스
        $(document).on('change', '.pricing-day-all', function() {
            var idx = $(this).data('idx');
            var checked = $(this).is(':checked');
            $('input.pricing-day[data-idx="' + idx + '"]').prop('checked', checked);
        });

        // 개별 요일 체크 해제 시 전체 선택 해제
        $(document).on('change', '.pricing-day', function() {
            var idx = $(this).data('idx');
            var allChecked = $('input.pricing-day[data-idx="' + idx + '"]').length ===
                             $('input.pricing-day[data-idx="' + idx + '"]:checked').length;
            $('#dayAll_' + idx).prop('checked', allChecked);
        });

        // 유료 옵션 모달 전체 선택 (현재 페이지만)
        $('#paidOptionCheckAll').on('change', function() {
            var checked = $(this).is(':checked');
            $('#modalPaidOptionTable tbody tr input.paid-option-check').prop('checked', checked);
        });

        // 유료 옵션 모달 검색
        $('#btnPaidOptionSearch').on('click', function() {
            self.filterPaidOptionTable();
        });
        $('#modalPaidOptionSearch').on('keypress', function(e) {
            if (e.which === 13) {
                e.preventDefault();
                self.filterPaidOptionTable();
            }
        });

        // 통화 토글 필터
        $('input[name="optionCurrencyFilter"]').on('change', function() {
            self.filterPaidOptionTable();
        });

    },

    updateHotelPropertyName: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#hotelPropertyName').text('-');
            return;
        }
        $('#contextAlert').addClass('d-none');

        var hotelName = $('#headerHotelSelect option:selected').text();
        var propertyName = $('#headerPropertySelect option:selected').text();
        if (hotelName && propertyName && hotelName !== '호텔 선택' && propertyName !== '프로퍼티 선택') {
            $('#hotelPropertyName').text(hotelName + ' > ' + propertyName);
        }
    },

    pricingTabBlocked: false,

    setCreateMode: function() {
        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();
        $('#rateCode').prop('readonly', false);
        $('#btnCheckDuplicate').show();
        // 신규 등록 시 요금정보/옵션요금 탭 비활성화
        this.pricingTabBlocked = true;
        $('#tab-pricing, #tab-option').css('opacity', '0.5').css('cursor', 'not-allowed');
    },

    setEditMode: function() {
        $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');
        $('#btnDelete').show();
        $('#rateCode').prop('readonly', true);
        $('#btnCheckDuplicate').hide();
        this.duplicateChecked = true;
        // 수정 모드에서 요금정보/옵션요금 탭 활성화
        this.pricingTabBlocked = false;
        $('#tab-pricing, #tab-option').css('opacity', '').css('cursor', '');
    },

    loadData: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId,
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    var d = res.data;
                    $('#rateCode').val(d.rateCode);
                    $('#rateNameKo').val(d.rateNameKo);
                    $('#rateNameEn').val(d.rateNameEn || '');
                    $('#saleStartDate').val(d.saleStartDate);
                    $('#saleEndDate').val(d.saleEndDate);
                    $('#marketCodeId').val(d.marketCodeId || '');
                    $('#marketCodeName').val(d.marketCodeName || '');
                    $('input[name="rateCategory"][value="' + d.rateCategory + '"]').prop('checked', true);
                    $('#currency').val(d.currency);
                    $('#minStayDays').val(d.minStayDays);
                    $('#maxStayDays').val(d.maxStayDays);
                    $('input[name="useYn"][value="' + d.useYn + '"]').prop('checked', true);
                    if (d.stayType) {
                        $('input[name="stayType"][value="' + d.stayType + '"]').prop('checked', true);
                    }
                    self.toggleDayUseTab();
                    if (d.stayType === 'DAY_USE') {
                        self.loadDayUseRates();
                    }

                    // 매핑된 객실 타입 로드
                    if (d.roomTypeIds && d.roomTypeIds.length > 0) {
                        self.loadRoomTypeDetails(propertyId, d.roomTypeIds);
                    }

                    self.setEditMode();
                }
            }
        });
    },

    loadRoomTypeDetails: function(propertyId, roomTypeIds) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-types',
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    self.selectedRoomTypes = res.data
                        .filter(function(rt) { return roomTypeIds.indexOf(rt.id) >= 0; })
                        .map(function(rt) {
                            return { id: rt.id, roomTypeCode: rt.roomTypeCode, description: rt.description || '-' };
                        });
                    self.renderRoomTypes();
                }
            }
        });
    },

    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#rateCode').val());
        if (!code) {
            HolaPms.alert('warning', '레이트 코드를 입력해주세요.');
            $('#rateCode').focus();
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/check-code',
            type: 'GET',
            data: { rateCode: code },
            success: function(res) {
                if (res.data.duplicate) {
                    $('#codeCheckResult').text('이미 사용 중인 코드입니다.').removeClass('text-primary').addClass('text-danger');
                    self.duplicateChecked = false;
                } else {
                    $('#codeCheckResult').text('사용 가능한 코드입니다.').removeClass('text-danger').addClass('text-primary');
                    self.duplicateChecked = true;
                }
            }
        });
    },

    save: function() {
        var self = this;

        // 요금정보 탭이 활성화된 경우 요금정보 저장
        if ($('#pane-pricing').hasClass('active')) {
            self.savePricing();
            return;
        }

        // 옵션요금 탭이 활성화된 경우 옵션요금 저장
        if ($('#pane-option').hasClass('active')) {
            self.saveOptionPricing();
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var rateCode = $.trim($('#rateCode').val());
        var rateNameKo = $.trim($('#rateNameKo').val());
        var rateNameEn = $.trim($('#rateNameEn').val());
        var saleStartDate = $('#saleStartDate').val();
        var saleEndDate = $('#saleEndDate').val();
        var marketCodeId = $('#marketCodeId').val() || null;
        var rateCategory = $('input[name="rateCategory"]:checked').val();
        var currency = $('#currency').val();
        var minStayDays = parseInt($('#minStayDays').val()) || 1;
        var maxStayDays = parseInt($('#maxStayDays').val()) || 365;
        var useYn = $('input[name="useYn"]:checked').val() === 'true';
        var stayType = $('input[name="stayType"]:checked').val() || 'OVERNIGHT';
        var roomTypeIds = self.selectedRoomTypes.map(function(rt) { return rt.id; });

        // 필수 검증
        if (!rateCode) {
            HolaPms.alert('warning', '레이트 코드를 입력해주세요.');
            $('#rateCode').focus();
            return;
        }
        if (!rateNameKo) {
            HolaPms.alert('warning', '레이트 코드 국문 설명을 입력해주세요.');
            $('#rateNameKo').focus();
            return;
        }
        if (!saleStartDate || !saleEndDate) {
            HolaPms.alert('warning', '판매기간을 입력해주세요.');
            return;
        }
        if (saleEndDate < saleStartDate) {
            HolaPms.alert('warning', '판매 종료일은 시작일보다 같거나 이후여야 합니다.');
            return;
        }
        if (maxStayDays < minStayDays) {
            HolaPms.alert('warning', '최대 숙박일수는 최소 숙박일수보다 크거나 같아야 합니다.');
            return;
        }

        // 신규 등록 시 중복확인 필수
        if (!self.editId && !self.duplicateChecked) {
            HolaPms.alert('warning', '레이트 코드 중복확인을 해주세요.');
            return;
        }

        var url, method, data;
        if (self.editId) {
            url = '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId;
            method = 'PUT';
            data = {
                rateNameKo: rateNameKo,
                rateNameEn: rateNameEn || null,
                rateCategory: rateCategory,
                marketCodeId: marketCodeId ? parseInt(marketCodeId) : null,
                currency: currency,
                saleStartDate: saleStartDate,
                saleEndDate: saleEndDate,
                minStayDays: minStayDays,
                maxStayDays: maxStayDays,
                stayType: stayType,
                useYn: useYn,
                roomTypeIds: roomTypeIds
            };
        } else {
            url = '/api/v1/properties/' + propertyId + '/rate-codes';
            method = 'POST';
            data = {
                rateCode: rateCode,
                rateNameKo: rateNameKo,
                rateNameEn: rateNameEn || null,
                rateCategory: rateCategory,
                marketCodeId: marketCodeId ? parseInt(marketCodeId) : null,
                currency: currency,
                saleStartDate: saleStartDate,
                saleEndDate: saleEndDate,
                minStayDays: minStayDays,
                maxStayDays: maxStayDays,
                stayType: stayType,
                useYn: useYn,
                roomTypeIds: roomTypeIds
            };
        }

        HolaPms.ajax({
            url: url,
            type: method,
            data: data,
            success: function(res) {
                if (res.success) {
                    var msg = self.editId ? '수정되었습니다.' : '등록되었습니다.';
                    if (!self.editId && res.data && res.data.id) {
                        // 신규 등록 후 수정 모드로 전환 (요금정보 탭 사용 가능)
                        HolaPms.alertAndRedirect('success', msg, '/admin/rate-codes/' + res.data.id);
                    } else {
                        HolaPms.alertAndRedirect('success', msg, '/admin/rate-codes');
                    }
                }
            }
        });
    },

    remove: function() {
        var self = this;
        if (!self.editId) return;

        if (!confirm('정말 삭제하시겠습니까?')) return;

        var propertyId = HolaPms.context.getPropertyId();
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId,
            type: 'DELETE',
            success: function(res) {
                if (res.success) {
                    HolaPms.alertAndRedirect('success', '삭제되었습니다.', '/admin/rate-codes');
                }
            }
        });
    },

    // ===== 마켓코드 모달 =====

    openMarketCodeModal: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }
        $('#modalMarketCodeSearch').val('');
        this.searchMarketCodes();
        HolaPms.modal.show('#marketCodeModal');
    },

    searchMarketCodes: function() {
        var propertyId = HolaPms.context.getPropertyId();
        var keyword = $.trim($('#modalMarketCodeSearch').val()).toLowerCase();

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/market-codes',
            type: 'GET',
            success: function(res) {
                var tbody = $('#modalMarketCodeTable tbody');
                tbody.empty();
                if (res.success && res.data) {
                    var filtered = res.data.filter(function(mc) {
                        if (!keyword) return true;
                        return (mc.marketCode || '').toLowerCase().indexOf(keyword) >= 0
                            || (mc.marketName || '').toLowerCase().indexOf(keyword) >= 0;
                    });
                    if (filtered.length === 0) {
                        tbody.append('<tr><td colspan="4" class="text-center text-muted">데이터가 없습니다.</td></tr>');
                        return;
                    }
                    filtered.forEach(function(mc, idx) {
                        tbody.append(
                            '<tr>' +
                            '<td class="text-center">' + (idx + 1) + '</td>' +
                            '<td>' + HolaPms.escapeHtml(mc.marketCode) + '</td>' +
                            '<td>' + HolaPms.escapeHtml(mc.marketName) + '</td>' +
                            '<td class="text-center"><button class="btn btn-sm btn-outline-primary" ' +
                            'onclick="RateCodeForm.selectMarketCode(' + mc.id + ',\'' +
                            HolaPms.escapeHtml(mc.marketCode) + ' - ' + HolaPms.escapeHtml(mc.marketName) + '\')">선택</button></td>' +
                            '</tr>'
                        );
                    });
                }
            }
        });
    },

    selectMarketCode: function(id, name) {
        $('#marketCodeId').val(id);
        $('#marketCodeName').val(name);
        bootstrap.Modal.getInstance(document.getElementById('marketCodeModal')).hide();
    },

    clearMarketCode: function() {
        $('#marketCodeId').val('');
        $('#marketCodeName').val('');
    },

    // ===== 객실 타입 모달 =====

    openRoomTypeModal: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }
        $('#modalRoomTypeSearch').val('');
        $('#roomTypeCheckAll').prop('checked', false);
        this.searchRoomTypes();
        HolaPms.modal.show('#roomTypeModal');
    },

    searchRoomTypes: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        var keyword = $.trim($('#modalRoomTypeSearch').val()).toLowerCase();
        var selectedIds = self.selectedRoomTypes.map(function(rt) { return rt.id; });

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-types',
            type: 'GET',
            success: function(res) {
                var tbody = $('#modalRoomTypeTable tbody');
                tbody.empty();
                if (res.success && res.data) {
                    var filtered = res.data.filter(function(rt) {
                        if (!keyword) return true;
                        return (rt.roomTypeCode || '').toLowerCase().indexOf(keyword) >= 0
                            || (rt.description || '').toLowerCase().indexOf(keyword) >= 0;
                    });
                    if (filtered.length === 0) {
                        tbody.append('<tr><td colspan="3" class="text-center text-muted">데이터가 없습니다.</td></tr>');
                        return;
                    }
                    filtered.forEach(function(rt) {
                        var checked = selectedIds.indexOf(rt.id) >= 0 ? ' checked' : '';
                        tbody.append(
                            '<tr>' +
                            '<td class="text-center"><input type="checkbox" class="room-type-check" ' +
                            'data-id="' + rt.id + '" data-code="' + HolaPms.escapeHtml(rt.roomTypeCode) + '" ' +
                            'data-desc="' + HolaPms.escapeHtml(rt.description || '-') + '"' + checked + '></td>' +
                            '<td>' + HolaPms.escapeHtml(rt.roomTypeCode) + '</td>' +
                            '<td>' + HolaPms.escapeHtml(rt.description || '-') + '</td>' +
                            '</tr>'
                        );
                    });
                }
            }
        });
    },

    applyRoomTypes: function() {
        var self = this;
        self.selectedRoomTypes = [];
        $('#modalRoomTypeTable tbody input.room-type-check:checked').each(function() {
            self.selectedRoomTypes.push({
                id: parseInt($(this).data('id')),
                roomTypeCode: $(this).data('code'),
                description: $(this).data('desc')
            });
        });
        self.renderRoomTypes();
        bootstrap.Modal.getInstance(document.getElementById('roomTypeModal')).hide();
    },

    renderRoomTypes: function() {
        var self = this;
        var tbody = $('#roomTypeTable tbody');
        tbody.empty();

        if (self.selectedRoomTypes.length === 0) {
            $('#roomTypeTable').hide();
            return;
        }

        $('#roomTypeTable').show();
        self.selectedRoomTypes.forEach(function(rt, idx) {
            tbody.append(
                '<tr>' +
                '<td class="text-center">' + (idx + 1) + '</td>' +
                '<td>' + HolaPms.escapeHtml(rt.roomTypeCode) + '</td>' +
                '<td>' + HolaPms.escapeHtml(rt.description) + '</td>' +
                '<td class="text-center"><button class="btn btn-sm btn-outline-danger" ' +
                'onclick="RateCodeForm.removeRoomType(' + rt.id + ')"><i class="fas fa-times"></i></button></td>' +
                '</tr>'
            );
        });
    },

    removeRoomType: function(id) {
        this.selectedRoomTypes = this.selectedRoomTypes.filter(function(rt) { return rt.id !== id; });
        this.renderRoomTypes();
    },

    // ===== 요금정보 탭 =====

    taxRate: 10, // 프로퍼티 세율 기본값 (%)
    pricingRowIndex: 0,

    loadTaxRate: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId,
            type: 'GET',
            success: function(res) {
                if (res.success && res.data && res.data.taxRate != null) {
                    self.taxRate = parseFloat(res.data.taxRate);
                }
            }
        });
    },

    loadPricingData: function() {
        var self = this;
        if (!self.editId) return;

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId + '/pricing',
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    var d = res.data;

                    // 요금 행 복원 (Down/Up sale 설정은 각 행 데이터에 포함)
                    if (d.pricingRows && d.pricingRows.length > 0) {
                        d.pricingRows.forEach(function(row) {
                            self.addPricingRow(row);
                        });
                    }
                }
            }
        });
    },

    addPricingRow: function(data) {
        var self = this;
        var idx = self.pricingRowIndex++;
        var currency = data ? data.currency : ($('#currency').val() || 'KRW');

        // 요금 설정 기간 기본값: 판매기간 또는 데이터값
        var saleStart = $('#saleStartDate').val() || '';
        var saleEnd = $('#saleEndDate').val() || '';
        var pricingStartDate = data ? (data.startDate || saleStart) : saleStart;
        var pricingEndDate = data ? (data.endDate || saleEnd) : saleEnd;

        var dayChecks = '';
        var days = [
            {key: 'Mon', label: 'MON'},
            {key: 'Tue', label: 'TUE'},
            {key: 'Wed', label: 'WED'},
            {key: 'Thu', label: 'THU'},
            {key: 'Fri', label: 'FRI'},
            {key: 'Sat', label: 'SAT'},
            {key: 'Sun', label: 'SUN'}
        ];

        var allChecked = true;
        days.forEach(function(d) {
            var checked = data ? data['day' + d.key] !== false : true;
            if (!checked) allChecked = false;
        });

        dayChecks += '<div class="form-check form-check-inline">' +
            '<input class="form-check-input pricing-day-all" type="checkbox" id="dayAll_' + idx + '" data-idx="' + idx + '"' +
            (allChecked ? ' checked' : '') + '>' +
            '<label class="form-check-label" for="dayAll_' + idx + '">전체</label></div>';

        days.forEach(function(d) {
            var checked = data ? data['day' + d.key] !== false : true;
            dayChecks += '<div class="form-check form-check-inline">' +
                '<input class="form-check-input pricing-day" type="checkbox" id="day' + d.key + '_' + idx + '" ' +
                'data-idx="' + idx + '" data-day="' + d.key + '"' + (checked ? ' checked' : '') + '>' +
                '<label class="form-check-label" for="day' + d.key + '_' + idx + '">' + d.label + '</label></div>';
        });

        // 기준금액
        var baseSupply = data ? (parseFloat(data.baseSupplyPrice) || 0) : 0;
        var baseTax = data ? (parseFloat(data.baseTax) || 0) : 0;
        var baseTotal = data ? (parseFloat(data.baseTotal) || 0) : 0;

        // Down/Up sale 데이터
        var downUpSign = data ? (data.downUpSign || null) : null;
        var downUpValue = data ? (data.downUpValue != null ? data.downUpValue : null) : null;
        var downUpUnit = data ? (data.downUpUnit || null) : null;
        var roundingDigits = data ? (data.roundingDigits != null ? data.roundingDigits : 0) : 0;
        var roundingMethod = data ? (data.roundingMethod || null) : null;

        // 인원별 추가 요금 데이터 맵 (person 데이터 빠르게 조회용)
        var personMap = {};
        if (data && data.persons) {
            data.persons.forEach(function(p) {
                personMap[p.personType + '_' + p.personSeq] = p;
            });
        }

        // 성인 추가 요금 행
        var adultRows = '';
        for (var i = 1; i <= 6; i++) {
            var label = i <= 5 ? '성인' + i : '엑스트라';
            var seq = i;
            var ap = personMap['ADULT_' + seq] || {};
            adultRows += self.buildPersonRow(idx, 'ADULT', seq, label, ap);
        }

        // 아동 추가 요금 행
        var childRows = '';
        for (var j = 1; j <= 6; j++) {
            var cLabel = j <= 5 ? '아동' + j : '엑스트라';
            var cSeq = j;
            var cp = personMap['CHILD_' + cSeq] || {};
            childRows += self.buildPersonRow(idx, 'CHILD', cSeq, cLabel, cp);
        }

        var collapseId = 'pricingCollapse_' + idx;

        var html =
            '<div class="card border mb-3 pricing-row" data-idx="' + idx + '">' +
            '<div class="card-body">' +
            '<div class="d-flex justify-content-between align-items-center mb-3">' +
            '<span class="card-section-toggle" data-bs-toggle="collapse" data-bs-target="#' + collapseId + '" aria-expanded="true">' +
            '<i class="fas fa-chevron-down section-arrow me-1"></i>' +
            '<span class="badge bg-primary">요금 #' + (idx + 1) + '</span></span>' +
            '<button type="button" class="btn btn-outline-danger btn-sm" onclick="RateCodeForm.removePricingRow(' + idx + ')">' +
            '<i class="fas fa-times"></i></button></div>' +

            '<div class="collapse show" id="' + collapseId + '">' +

            // 요금 설정 기간
            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label required">요금 설정 기간</label>' +
            '<div class="col-sm-10">' +
            '<div class="d-flex align-items-center">' +
            '<input type="date" class="form-control pricing-start-date" id="pricingStartDate_' + idx + '" ' +
            'data-idx="' + idx + '" value="' + pricingStartDate + '"' +
            (saleStart ? ' min="' + saleStart + '"' : '') +
            (saleEnd ? ' max="' + saleEnd + '"' : '') +
            ' style="max-width:200px;">' +
            '<span class="mx-2">~</span>' +
            '<input type="date" class="form-control pricing-end-date" id="pricingEndDate_' + idx + '" ' +
            'data-idx="' + idx + '" value="' + pricingEndDate + '"' +
            (saleStart ? ' min="' + saleStart + '"' : '') +
            (saleEnd ? ' max="' + saleEnd + '"' : '') +
            ' style="max-width:200px;">' +
            '</div></div></div>' +

            // 요일
            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label required">요일</label>' +
            '<div class="col-sm-10"><div class="mt-1">' + dayChecks + '</div></div></div>' +

            // 통화
            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label required">통화 구분</label>' +
            '<div class="col-sm-3">' +
            '<input type="text" class="form-control-plaintext" id="pricingCurrency_' + idx + '" value="' + currency + '" readonly>' +
            '</div></div>' +

            // 기준 금액
            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label required">기준 금액</label>' +
            '<div class="col-sm-8">' +
            '<div class="row">' +
            '<div class="col-sm-4"><label class="form-label">공급가</label>' +
            '<input type="number" class="form-control pricing-supply" id="baseSupply_' + idx + '" data-idx="' + idx + '" ' +
            'value="' + baseSupply + '" min="0" step="1" onchange="RateCodeForm.calcBaseTax(' + idx + ')"></div>' +
            '<div class="col-sm-4"><label class="form-label">TAX</label>' +
            '<input type="text" class="form-control" id="baseTax_' + idx + '" value="' + self.formatNumber(baseTax) + '" readonly></div>' +
            '<div class="col-sm-4"><label class="form-label">VAT 합계</label>' +
            '<input type="text" class="form-control" id="baseTotal_' + idx + '" value="' + self.formatNumber(baseTotal) + '" readonly></div>' +
            '</div></div></div>' +

            // 성인 추가 요금
            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label">성인 추가 요금</label>' +
            '<div class="col-sm-10">' +
            '<table class="table table-sm table-bordered mb-0"><thead class="table-light"><tr>' +
            '<th style="width:100px">구분</th><th>금액(공급가)</th><th>TAX</th><th>단가(VAT포함)</th></tr></thead>' +
            '<tbody>' + adultRows + '</tbody></table>' +
            '</div></div>' +

            // 아동 추가 요금
            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label">아동 추가 요금</label>' +
            '<div class="col-sm-10">' +
            '<table class="table table-sm table-bordered mb-0"><thead class="table-light"><tr>' +
            '<th style="width:100px">구분</th><th>금액(공급가)</th><th>TAX</th><th>단가(VAT포함)</th></tr></thead>' +
            '<tbody>' + childRows + '</tbody></table>' +
            '</div></div>' +

            // Down/Up sale 요금 설정 (요금 행 내부)
            '<hr class="my-3">' +
            '<h6 class="fw-bold mb-3"><i class="fas fa-square me-1 text-primary" style="font-size:8px;"></i> Down/Up sale 요금 설정</h6>' +
            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label">금액 설정</label>' +
            '<div class="col-sm-8">' +
            '<div class="d-flex align-items-center">' +
            '<select class="form-select me-2" id="downUpSign_' + idx + '" style="width:120px;">' +
            '<option value=""' + (!downUpSign ? ' selected' : '') + '>미사용</option>' +
            '<option value="-"' + (downUpSign === '-' ? ' selected' : '') + '>▼ Down</option>' +
            '<option value="+"' + (downUpSign === '+' ? ' selected' : '') + '>▲ Up</option>' +
            '</select>' +
            '<input type="number" class="form-control me-2" id="downUpValue_' + idx + '" placeholder="금액/비율" style="max-width:200px;" min="0" step="0.01"' +
            (downUpValue != null ? ' value="' + downUpValue + '"' : '') + '>' +
            '<div class="btn-group" role="group">' +
            '<input type="radio" class="btn-check" name="downUpUnit_' + idx + '" id="unitAmount_' + idx + '" value="AMOUNT"' +
            (downUpUnit !== 'PERCENT' ? ' checked' : '') + '>' +
            '<label class="btn btn-outline-primary btn-sm" for="unitAmount_' + idx + '">정액</label>' +
            '<input type="radio" class="btn-check" name="downUpUnit_' + idx + '" id="unitPercent_' + idx + '" value="PERCENT"' +
            (downUpUnit === 'PERCENT' ? ' checked' : '') + '>' +
            '<label class="btn btn-outline-primary btn-sm" for="unitPercent_' + idx + '">정률(%)</label>' +
            '</div></div>' +
            '<small class="text-muted mt-1 d-block">VAT 합계 금액 기준으로 처리됩니다.</small>' +
            '</div></div>' +

            '<div class="row mb-3">' +
            '<label class="col-sm-2 col-form-label">소수점 처리</label>' +
            '<div class="col-sm-8">' +
            '<div class="d-flex align-items-center">' +
            '<span class="me-2">소수점</span>' +
            '<input type="number" class="form-control me-2" id="roundingDigits_' + idx + '" value="' + roundingDigits + '" min="0" max="10" style="width:80px;">' +
            '<span class="me-2">자리 수</span>' +
            '<select class="form-select" id="roundingMethod_' + idx + '" style="width:140px;">' +
            '<option value="">선택</option>' +
            '<option value="CEIL"' + (roundingMethod === 'CEIL' ? ' selected' : '') + '>올림</option>' +
            '<option value="FLOOR"' + (roundingMethod === 'FLOOR' ? ' selected' : '') + '>내림</option>' +
            '<option value="HALF_UP"' + (roundingMethod === 'HALF_UP' ? ' selected' : '') + '>반올림</option>' +
            '<option value="HALF_DOWN"' + (roundingMethod === 'HALF_DOWN' ? ' selected' : '') + '>반내림</option>' +
            '<option value="TRUNCATE"' + (roundingMethod === 'TRUNCATE' ? ' selected' : '') + '>절사</option>' +
            '</select></div></div></div>' +

            '</div>' + // collapse 닫기
            '</div></div>';

        $('#pricingRowsContainer').append(html);
        $('#pricingEmptyMsg').hide();

        // 접기/펼치기 화살표 연동
        var $collapse = $('#' + collapseId);
        var $toggle = $collapse.closest('.pricing-row').find('.card-section-toggle');
        $collapse.on('hide.bs.collapse', function() { $toggle.addClass('collapsed'); });
        $collapse.on('show.bs.collapse', function() { $toggle.removeClass('collapsed'); });

        // 기간-요일 연동: 기간 변경 시 해당 기간 내 존재하는 요일만 활성화
        $('#pricingStartDate_' + idx + ', #pricingEndDate_' + idx).on('change', function() {
            self.updateDayCheckboxes(idx);
        });

        // 초기 로드 시 요일 상태 갱신
        if (pricingStartDate && pricingEndDate) {
            self.updateDayCheckboxes(idx);
        }
    },

    /**
     * 선택한 기간 내 존재하는 요일만 체크박스 활성화
     */
    updateDayCheckboxes: function(idx) {
        var startStr = $('#pricingStartDate_' + idx).val();
        var endStr = $('#pricingEndDate_' + idx).val();

        if (!startStr || !endStr) return;

        var start = new Date(startStr);
        var end = new Date(endStr);
        if (start > end) return;

        // 기간 내 존재하는 요일 수집 (0=Sun, 1=Mon, ... 6=Sat)
        var existingDays = new Set();
        var current = new Date(start);
        // 최대 7일만 확인하면 모든 요일 커버
        var maxDays = Math.min(Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1, 7);
        for (var i = 0; i < maxDays; i++) {
            existingDays.add(current.getDay());
            current.setDate(current.getDate() + 1);
        }
        // 7일 이상이면 모든 요일 존재
        if (Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1 >= 7) {
            for (var d = 0; d < 7; d++) existingDays.add(d);
        }

        var dayMap = {Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6, Sun: 0};
        var days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

        days.forEach(function(key) {
            var $cb = $('#day' + key + '_' + idx);
            if (existingDays.has(dayMap[key])) {
                $cb.prop('disabled', false);
            } else {
                $cb.prop('disabled', true).prop('checked', false);
            }
        });

        // 전체 체크박스 상태 갱신
        var allChecked = true;
        days.forEach(function(key) {
            var $cb = $('#day' + key + '_' + idx);
            if (!$cb.prop('disabled') && !$cb.prop('checked')) allChecked = false;
        });
        $('#dayAll_' + idx).prop('checked', allChecked);
    },

    /**
     * 판매기간 변경 시 모든 요금행의 기간 캘린더 min/max 갱신
     */
    updatePricingDateLimits: function() {
        var saleStart = $('#saleStartDate').val() || '';
        var saleEnd = $('#saleEndDate').val() || '';

        $('.pricing-start-date').attr('min', saleStart).attr('max', saleEnd);
        $('.pricing-end-date').attr('min', saleStart).attr('max', saleEnd);
    },

    buildPersonRow: function(idx, type, seq, label, data) {
        var supply = data.supplyPrice ? parseFloat(data.supplyPrice) : 0;
        var tax = data.tax ? parseFloat(data.tax) : 0;
        var total = data.totalPrice ? parseFloat(data.totalPrice) : 0;
        var key = type + '_' + seq + '_' + idx;
        return '<tr>' +
            '<td class="text-center align-middle">' + label + '</td>' +
            '<td><input type="number" class="form-control form-control-sm person-supply" ' +
            'id="ps_' + key + '" data-idx="' + idx + '" data-type="' + type + '" data-seq="' + seq + '" ' +
            'value="' + (supply || '') + '" min="0" step="1" onchange="RateCodeForm.calcPersonTax(\'' + key + '\')"></td>' +
            '<td><input type="text" class="form-control form-control-sm" id="pt_' + key + '" value="' + (supply > 0 ? this.formatNumber(tax) : '') + '" readonly></td>' +
            '<td><input type="text" class="form-control form-control-sm" id="pp_' + key + '" value="' + (supply > 0 ? this.formatNumber(total) : '') + '" readonly></td>' +
            '</tr>';
    },

    removePricingRow: function(idx) {
        $('.pricing-row[data-idx="' + idx + '"]').remove();
        if ($('.pricing-row').length === 0) {
            $('#pricingEmptyMsg').show();
        }
        // 번호 재정렬
        $('.pricing-row').each(function(i) {
            $(this).find('.badge').text('요금 #' + (i + 1));
        });
    },

    calcBaseTax: function(idx) {
        var supply = parseFloat($('#baseSupply_' + idx).val()) || 0;
        var tax = Math.round(supply * this.taxRate / 100);
        var total = supply + tax;
        $('#baseTax_' + idx).val(this.formatNumber(tax));
        $('#baseTotal_' + idx).val(this.formatNumber(total));
    },

    calcPersonTax: function(key) {
        var supply = parseFloat($('#ps_' + key).val()) || 0;
        if (supply <= 0) {
            $('#pt_' + key).val('');
            $('#pp_' + key).val('');
            return;
        }
        var tax = Math.round(supply * this.taxRate / 100);
        var total = supply + tax;
        $('#pt_' + key).val(this.formatNumber(tax));
        $('#pp_' + key).val(this.formatNumber(total));
    },

    formatNumber: function(num) {
        if (num == null || isNaN(num)) return '0';
        return Number(num).toLocaleString('ko-KR');
    },

    collectPricingData: function() {
        var self = this;
        var rows = [];

        $('.pricing-row').each(function() {
            var idx = $(this).data('idx');
            var days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
            var row = {
                startDate: $('#pricingStartDate_' + idx).val() || null,
                endDate: $('#pricingEndDate_' + idx).val() || null,
                currency: $('#pricingCurrency_' + idx).val(),
                baseSupplyPrice: parseFloat($('#baseSupply_' + idx).val()) || 0,
                baseTax: self.parseFormattedNumber($('#baseTax_' + idx).val()),
                baseTotal: self.parseFormattedNumber($('#baseTotal_' + idx).val()),
                // Down/Up sale 설정 (요금 행별)
                downUpSign: $('#downUpSign_' + idx).val() || null,
                downUpValue: (function() {
                    var v = $('#downUpValue_' + idx).val();
                    return v !== '' ? parseFloat(v) : null;
                })(),
                downUpUnit: $('#downUpSign_' + idx).val() ? ($('input[name="downUpUnit_' + idx + '"]:checked').val() || null) : null,
                roundingDecimalPoint: 0,
                roundingDigits: parseInt($('#roundingDigits_' + idx).val()) || 0,
                roundingMethod: $('#roundingMethod_' + idx).val() || null,
                persons: []
            };

            days.forEach(function(d) {
                row['day' + d] = $('#day' + d + '_' + idx).is(':checked');
            });

            // 인원별 추가 요금
            var types = ['ADULT', 'CHILD'];
            types.forEach(function(type) {
                for (var seq = 1; seq <= 6; seq++) {
                    var key = type + '_' + seq + '_' + idx;
                    var supply = parseFloat($('#ps_' + key).val()) || 0;
                    if (supply > 0) {
                        row.persons.push({
                            personType: type,
                            personSeq: seq,
                            supplyPrice: supply,
                            tax: self.parseFormattedNumber($('#pt_' + key).val()),
                            totalPrice: self.parseFormattedNumber($('#pp_' + key).val())
                        });
                    }
                }
            });

            rows.push(row);
        });

        return {
            pricingRows: rows
        };
    },

    /**
     * 요금 설정 기간 필수/범위 검증
     * @returns {string|null} 에러 메시지
     */
    checkPricingPeriod: function() {
        var saleStart = $('#saleStartDate').val();
        var saleEnd = $('#saleEndDate').val();
        var error = null;

        $('.pricing-row').each(function(rowIndex) {
            var idx = $(this).data('idx');
            var rowNum = rowIndex + 1;
            var start = $('#pricingStartDate_' + idx).val();
            var end = $('#pricingEndDate_' + idx).val();

            // 필수 검증
            if (!start || !end) {
                error = '요금 #' + rowNum + '의 요금 설정 기간을 입력해주세요.';
                return false;
            }
            // 역전 검증
            if (start > end) {
                error = '요금 #' + rowNum + '의 종료일은 시작일보다 같거나 이후여야 합니다.';
                return false;
            }
            // 판매기간 종속 검증
            if (saleStart && saleEnd) {
                if (start < saleStart || end > saleEnd) {
                    error = '요금 #' + rowNum + '의 기간이 판매기간(' + saleStart + ' ~ ' + saleEnd + ')을 벗어납니다.';
                    return false;
                }
            }
        });
        return error;
    },

    /**
     * 기간+요일 중복 검증 (기간이 겹치는 행 쌍에서만 요일 중복 검사)
     * @returns {string|null} 에러 메시지
     */
    checkPeriodOverlap: function() {
        var dayLabels = {Mon: '월', Tue: '화', Wed: '수', Thu: '목', Fri: '금', Sat: '토', Sun: '일'};
        var days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        var rows = [];

        $('.pricing-row').each(function(rowIndex) {
            var idx = $(this).data('idx');
            var start = $('#pricingStartDate_' + idx).val();
            var end = $('#pricingEndDate_' + idx).val();
            var checkedDays = [];
            days.forEach(function(d) {
                if ($('#day' + d + '_' + idx).is(':checked')) {
                    checkedDays.push(d);
                }
            });
            rows.push({num: rowIndex + 1, start: start, end: end, days: checkedDays});
        });

        // 모든 행 쌍 비교
        for (var i = 0; i < rows.length; i++) {
            for (var j = i + 1; j < rows.length; j++) {
                var a = rows[i];
                var b = rows[j];
                // 기간 중첩 확인
                if (a.start <= b.end && b.start <= a.end) {
                    // 기간이 겹치면 요일 중복 확인
                    for (var k = 0; k < a.days.length; k++) {
                        if (b.days.indexOf(a.days[k]) >= 0) {
                            return '요금 #' + a.num + '과(와) 요금 #' + b.num +
                                '의 기간이 겹치고 "' + dayLabels[a.days[k]] + '요일"이 중복됩니다.';
                        }
                    }
                }
            }
        }
        return null;
    },

    parseFormattedNumber: function(str) {
        if (!str) return 0;
        return parseFloat(String(str).replace(/,/g, '')) || 0;
    },

    savePricing: function() {
        var self = this;
        if (!self.editId) {
            HolaPms.alert('warning', '기본정보를 먼저 저장해주세요.');
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        // 요금 설정 기간 검증
        var periodError = self.checkPricingPeriod();
        if (periodError) {
            HolaPms.alert('warning', periodError);
            return;
        }

        // 기간+요일 중복 검증
        var overlapError = self.checkPeriodOverlap();
        if (overlapError) {
            HolaPms.alert('warning', overlapError);
            return;
        }

        var data = self.collectPricingData();

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId + '/pricing',
            type: 'POST',
            data: data,
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '요금정보가 저장되었습니다.');
                }
            }
        });
    },

    // ===== 옵션요금 탭 =====

    loadOptionPricingData: function() {
        var self = this;
        if (!self.editId) return;

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) return;

        // 2개 API 병렬 호출
        var optionsReq = HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/paid-service-options',
            type: 'GET'
        });
        var mappingReq = HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId + '/option-pricing',
            type: 'GET'
        });

        $.when(optionsReq, mappingReq).done(function(optionsRes, mappingRes) {
            if (optionsRes[0].success && optionsRes[0].data) {
                self.allPaidOptions = optionsRes[0].data;
            }
            if (mappingRes[0].success && mappingRes[0].data) {
                self.selectedOptionIds = new Set(mappingRes[0].data);
            }
            self.renderOptionSections();
        });
    },

    renderOptionSections: function() {
        var self = this;
        var container = $('#optionCategoryContainer');
        container.empty();

        var selected = self.allPaidOptions.filter(function(opt) {
            return self.selectedOptionIds.has(opt.id);
        });

        if (selected.length === 0) {
            $('#optionEmptyMsg').show();
            return;
        }
        $('#optionEmptyMsg').hide();

        var html = '<table class="table table-sm table-bordered mb-0">' +
            '<thead class="table-light"><tr>' +
            '<th style="width:50px">NO</th>' +
            '<th>옵션코드명</th>' +
            '<th>서비스명(국문)</th>' +
            '<th>서비스명(영문)</th>' +
            '<th class="text-end">통화</th>' +
            '<th class="text-end">수량</th>' +
            '<th class="text-end">VAT포함금액</th>' +
            '<th class="text-end">TAX</th>' +
            '<th class="text-end">단가</th>' +
            '<th style="width:50px">삭제</th>' +
            '</tr></thead><tbody>';

        selected.forEach(function(opt, idx) {
            html += '<tr>' +
                '<td class="text-center">' + (idx + 1) + '</td>' +
                '<td>' + HolaPms.escapeHtml(opt.serviceOptionCode || '-') + '</td>' +
                '<td>' + HolaPms.escapeHtml(opt.serviceNameKo || '-') + '</td>' +
                '<td>' + HolaPms.escapeHtml(opt.serviceNameEn || '-') + '</td>' +
                '<td class="text-end">' + HolaPms.escapeHtml(opt.currencyCode || '-') + '</td>' +
                '<td class="text-end">' + (opt.quantity != null ? opt.quantity : '-') + '</td>' +
                '<td class="text-end">' + self.formatNumber(opt.vatIncludedPrice) + '</td>' +
                '<td class="text-end">' + self.formatNumber(opt.taxAmount) + '</td>' +
                '<td class="text-end">' + self.formatNumber(opt.supplyPrice) + '</td>' +
                '<td class="text-center"><button class="btn btn-sm btn-outline-danger" ' +
                'onclick="RateCodeForm.removeOption(' + opt.id + ')"><i class="fas fa-times"></i></button></td>' +
                '</tr>';
        });

        html += '</tbody></table>';
        container.append(html);
    },

    paidOptionTable: null,

    openOptionModal: function() {
        var self = this;
        $('#paidOptionCheckAll').prop('checked', false);
        $('#modalPaidOptionSearch').val('');
        $('input[name="optionCurrencyFilter"][value=""]').prop('checked', true);

        // 기존 DataTable 파괴 후 재생성
        if (self.paidOptionTable) {
            self.paidOptionTable.destroy();
            self.paidOptionTable = null;
        }
        $('#modalPaidOptionTable tbody').empty();

        // 데이터 준비
        var tableData = self.allPaidOptions.map(function(opt) {
            var checked = self.selectedOptionIds.has(opt.id) ? ' checked' : '';
            return [
                '<input type="checkbox" class="paid-option-check" data-id="' + opt.id + '"' + checked + '>',
                HolaPms.escapeHtml(opt.serviceOptionCode || '-'),
                HolaPms.escapeHtml(opt.serviceNameKo || '-'),
                HolaPms.escapeHtml(opt.serviceNameEn || '-'),
                HolaPms.escapeHtml(opt.currencyCode || '-'),
                opt.quantity != null ? opt.quantity : '-',
                self.formatNumber(opt.vatIncludedPrice),
                self.formatNumber(opt.taxAmount),
                self.formatNumber(opt.supplyPrice)
            ];
        });

        self.paidOptionTable = $('#modalPaidOptionTable').DataTable({
            language: HolaPms.dataTableLanguage,
            data: tableData,
            dom: 'rtip',
            autoWidth: false,
            order: [[1, 'asc']],
            pageLength: 10,
            lengthMenu: [10, 20, 50, 100],
            columns: [
                { orderable: false, searchable: false, width: '40px', className: 'text-center' },
                { width: '15%' },
                { width: '15%' },
                { width: '15%' },
                { width: '60px', className: 'text-center' },
                { width: '60px', className: 'text-end', searchable: false },
                { width: '110px', className: 'text-end', searchable: false },
                { width: '90px', className: 'text-end', searchable: false },
                { width: '90px', className: 'text-end', searchable: false }
            ],
            drawCallback: function() {
                $('#paidOptionCheckAll').prop('checked', false);
            }
        });

        HolaPms.modal.show('#paidOptionModal');
    },

    filterPaidOptionTable: function() {
        var self = this;
        if (!self.paidOptionTable) return;

        var keyword = $('#modalPaidOptionSearch').val();
        var currency = $('input[name="optionCurrencyFilter"]:checked').val();

        // 텍스트 검색 (컬럼 1~3: 옵션코드명, 서비스명 국문/영문)
        self.paidOptionTable.search(keyword);

        // 통화 필터 (컬럼 4)
        self.paidOptionTable.column(4).search(currency ? '^' + currency + '$' : '', true, false);

        self.paidOptionTable.draw();
    },

    applyOptions: function() {
        var self = this;

        // DataTable 전체 페이지에서 체크된 항목 수집
        self.selectedOptionIds = new Set();
        if (self.paidOptionTable) {
            self.paidOptionTable.rows().every(function() {
                var $row = $(this.node());
                if ($row.find('input.paid-option-check').is(':checked')) {
                    self.selectedOptionIds.add(parseInt($row.find('input.paid-option-check').data('id')));
                }
            });
        }

        self.renderOptionSections();
        bootstrap.Modal.getInstance(document.getElementById('paidOptionModal')).hide();
    },

    removeOption: function(optionId) {
        this.selectedOptionIds.delete(optionId);
        this.renderOptionSections();
    },

    clearAllOptions: function() {
        if (!confirm('모든 유료 옵션 서비스를 삭제하시겠습니까?')) return;
        this.selectedOptionIds = new Set();
        this.renderOptionSections();
    },

    saveOptionPricing: function() {
        var self = this;
        if (!self.editId) {
            HolaPms.alert('warning', '기본정보를 먼저 저장해주세요.');
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var ids = Array.from(self.selectedOptionIds);

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId + '/option-pricing',
            type: 'POST',
            data: ids,
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '옵션요금이 저장되었습니다.');
                }
            }
        });
    },

    // ===== Dayuse 요금 관리 =====

    toggleDayUseTab: function() {
        var stayType = $('input[name="stayType"]:checked').val();
        if (stayType === 'DAY_USE') {
            $('#tab-dayuse-li').show();
            // Dayuse: 숙박일수 고정 1/1
            $('#minStayDays').val(1).prop('readonly', true);
            $('#maxStayDays').val(1).prop('readonly', true);
        } else {
            $('#tab-dayuse-li').hide();
            $('#minStayDays').prop('readonly', false);
            $('#maxStayDays').prop('readonly', false);
        }
    },

    loadDayUseRates: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId || !self.editId) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId + '/dayuse-rates',
            type: 'GET',
            success: function(res) {
                if (res.success) {
                    self.renderDayUseRates(res.data || []);
                }
            }
        });
    },

    renderDayUseRates: function(rates) {
        var $tbody = $('#dayUseRateTable tbody');
        $tbody.empty();
        if (rates.length === 0) {
            $('#dayUseRateEmpty').show();
            return;
        }
        $('#dayUseRateEmpty').hide();
        rates.forEach(function(r) {
            var row = '<tr data-id="' + r.id + '">'
                + '<td>' + r.durationHours + '시간</td>'
                + '<td>' + RateCodeForm.formatNumber(r.supplyPrice) + '원</td>'
                + '<td>' + HolaPms.escapeHtml(r.description || '-') + '</td>'
                + '<td><button type="button" class="btn btn-outline-danger btn-sm" '
                + 'onclick="RateCodeForm.deleteDayUseRate(' + r.id + ')"><i class="fas fa-trash"></i></button></td>'
                + '</tr>';
            $tbody.append(row);
        });
    },

    openDayUseRateModal: function() {
        if (!this.editId) {
            HolaPms.alert('warning', '레이트코드를 먼저 저장해주세요.');
            return;
        }
        // 입력 필드 초기화
        $('#dayUseHoursInput').val('').removeClass('is-invalid');
        $('#dayUsePriceInput').val('').removeClass('is-invalid');
        $('#dayUseDescInput').val('');
        $('#dayUseHoursError, #dayUsePriceError').text('');
        HolaPms.modal.show('#dayUseRateModal');
        // 첫 번째 입력 필드에 포커스
        setTimeout(function() { $('#dayUseHoursInput').focus(); }, 300);
    },

    saveDayUseRate: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        var valid = true;

        // 이용시간 검증
        var hoursVal = $('#dayUseHoursInput').val().trim();
        var hours = parseInt(hoursVal, 10);
        if (!hoursVal || isNaN(hours) || hours < 1 || hours > 24) {
            $('#dayUseHoursInput').addClass('is-invalid');
            $('#dayUseHoursError').text('1~24 사이의 정수를 입력해주세요.').show();
            valid = false;
        } else {
            $('#dayUseHoursInput').removeClass('is-invalid');
        }

        // 공급가 검증 (콤마 제거 후 숫자 파싱)
        var priceRaw = $('#dayUsePriceInput').val().replace(/,/g, '').trim();
        var price = parseFloat(priceRaw);
        if (!priceRaw || isNaN(price) || price < 0) {
            $('#dayUsePriceInput').addClass('is-invalid');
            $('#dayUsePriceError').text('0 이상의 금액을 입력해주세요.').show();
            valid = false;
        } else {
            $('#dayUsePriceInput').removeClass('is-invalid');
        }

        if (!valid) return;

        var desc = $('#dayUseDescInput').val().trim() || '';

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId + '/dayuse-rates',
            type: 'POST',
            data: { durationHours: hours, supplyPrice: price, description: desc },
            success: function(res) {
                if (res.success) {
                    HolaPms.modal.hide('#dayUseRateModal');
                    HolaPms.alert('success', 'Dayuse 요금이 등록되었습니다.');
                    self.loadDayUseRates();
                }
            }
        });
    },

    deleteDayUseRate: function(rateId) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        HolaPms.confirm('이 요금을 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/rate-codes/' + self.editId + '/dayuse-rates/' + rateId,
                type: 'DELETE',
                success: function(res) {
                    if (res.success) {
                        HolaPms.alert('success', '삭제되었습니다.');
                        self.loadDayUseRates();
                    }
                }
            });
        });
    }
};

$(document).ready(function() {
    RateCodeForm.init();
});
