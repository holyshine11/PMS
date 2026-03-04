/**
 * 프로퍼티 등록/수정 폼 JS (4탭 구조)
 */
const PropertyForm = {
    isEdit: false,
    propertyId: null,
    nameChecked: false,
    originalName: '',
    settlementLoaded: false,
    cancelFeeLoaded: false,

    init: function() {
        this.propertyId = $('#propertyId').val() || null;
        this.isEdit = !!this.propertyId;

        this.initHotelSelect();
        this.initFileUploadUI();
        this.initPhoneInputMask();
        this.initSettlement();
        this.initCancelFee();
        this.initTaxServiceCharge();

        if (this.isEdit) {
            $('#pageTitle').html('<i class="fas fa-city me-2"></i>프로퍼티 수정');
            $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');
            $('#btnDelete').show();
            // 수정 시 프로퍼티명 읽기전용 + 중복확인 버튼 숨김
            $('#propertyName').prop('readonly', true);
            $('#btnCheckName').hide();
            this.loadProperty();
        }

        // 프로퍼티명 변경 시 중복확인 초기화
        $('#propertyName').on('input', function() {
            PropertyForm.nameChecked = false;
            $('#nameCheckResult').hide();
        });
    },

    /** 호텔 셀렉트박스 초기화 (API에서 호텔 목록 로드) */
    initHotelSelect: function() {
        if (this.isEdit) {
            // 수정 시 호텔 셀렉트 숨기고 텍스트 표시 (loadProperty에서 처리)
            $('#hotelSelect').hide();
            return;
        }

        // 등록 시 호텔 목록 API 호출
        var contextHotelId = HolaPms.context.getHotelId();
        HolaPms.ajax({
            url: '/api/v1/hotels/selector',
            type: 'GET',
            success: function(res) {
                var hotels = res.data || [];
                var $select = $('#hotelSelect');
                $select.html('<option value="">호텔을 선택해주세요</option>');
                $.each(hotels, function(i, hotel) {
                    $select.append('<option value="' + hotel.id + '">' + hotel.hotelName + '</option>');
                });
                // 헤더에서 선택된 호텔이 있으면 자동 선택
                if (contextHotelId) {
                    $select.val(contextHotelId);
                }
            }
        });
    },

    /** 파일 업로드 UI 이벤트 바인딩 */
    initFileUploadUI: function() {
        // 사업자등록증 업로드
        $('#btnBizLicense').on('click', function() {
            $('#bizLicenseFile').trigger('click');
        });
        $('#bizLicenseFile').on('change', function() {
            var file = this.files[0];
            if (file) {
                PropertyForm.uploadFile(file, 'biz-license', function(data) {
                    $('#bizLicensePath').val(data.filePath);
                    $('#bizLicenseFileName').text(data.fileName);
                    PropertyForm.showBizLicenseDownload(data.filePath, data.fileName);
                });
            }
        });

        // 로고 업로드
        $('#btnLogo').on('click', function() {
            $('#logoFile').trigger('click');
        });
        $('#logoFile').on('change', function() {
            var file = this.files[0];
            if (file) {
                PropertyForm.uploadFile(file, 'logo', function(data) {
                    $('#logoPath').val(data.filePath);
                    $('#logoFileName').text(data.fileName);
                    PropertyForm.showLogoDownload(data.filePath, data.fileName);
                });
            }
        });
    },

    /** 파일 업로드 공통 */
    uploadFile: function(file, category, onSuccess) {
        var formData = new FormData();
        formData.append('file', file);
        formData.append('category', category);

        $.ajax({
            url: '/api/v1/files/upload',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            headers: {
                'Authorization': 'Bearer ' + (HolaPms.getToken ? HolaPms.getToken() : '')
            },
            success: function(res) {
                if (res.success && res.data) {
                    onSuccess(res.data);
                } else {
                    HolaPms.alert('error', '파일 업로드에 실패했습니다.');
                }
            },
            error: function() {
                HolaPms.alert('error', '파일 업로드에 실패했습니다.');
            }
        });
    },

    /** 사업자등록증 다운로드 링크 표시 */
    showBizLicenseDownload: function(filePath, fileName) {
        if (!filePath) {
            $('#bizLicenseDownload').hide();
            $('#bizLicenseFileName').show().text('파일을 선택해주세요');
            return;
        }
        var displayName = fileName || filePath.split('/').pop();
        $('#bizLicenseFileName').hide();
        $('#bizLicenseDownloadName').text(displayName);
        $('#bizLicenseDownload').attr('href', filePath).show();
    },

    /** 로고 다운로드 링크 표시 */
    showLogoDownload: function(filePath, fileName) {
        if (!filePath) {
            $('#logoDownload').hide();
            $('#logoFileName').show().text('이미지를 선택해주세요');
            return;
        }
        var displayName = fileName || filePath.split('/').pop();
        $('#logoFileName').hide();
        $('#logoDownloadName').text(displayName);
        $('#logoDownload').attr('href', filePath).show();
    },

    /** 전화번호 숫자만 입력 */
    initPhoneInputMask: function() {
        $('#phone1, #phone2, #phone3').on('input', function() {
            $(this).val($(this).val().replace(/[^0-9]/g, ''));
        });
        // 자동 포커스 이동
        $('#phone1').on('input', function() {
            if ($(this).val().length >= 3) $('#phone2').focus();
        });
        $('#phone2').on('input', function() {
            if ($(this).val().length >= 4) $('#phone3').focus();
        });
    },

    /** 프로퍼티 데이터 로드 (수정 시) */
    loadProperty: function() {
        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId,
            type: 'GET',
            success: function(res) {
                var d = res.data;

                // 호텔 정보 (수정 시 읽기전용)
                $('#hotelSelect').hide();
                var hotelLabel = (d.propertyCode || '') + ' | ' + (d.hotelName || '');
                $('#hotelReadonly').text(hotelLabel).show();

                // 프로퍼티명
                $('#propertyName').val(d.propertyName);
                PropertyForm.originalName = d.propertyName;

                // 시간대 파싱 (UTC+09:00 형태)
                PropertyForm.parseTimezone(d.timezone);

                // 대표자명
                $('#representativeName').val(d.representativeName || '');
                $('#representativeNameEn').val(d.representativeNameEn || '');
                $('#businessNumber').val(d.businessNumber || '');

                // 전화번호 3칸 분배
                PropertyForm.parsePhone(d.phone);
                $('#countryCode').val(d.countryCode || '+82');

                // 사용여부
                if (d.useYn === false) {
                    $('#useYnN').prop('checked', true);
                } else {
                    $('#useYnY').prop('checked', true);
                }

                // 주소
                $('#zipCode').val(d.zipCode || '');
                $('#address').val(d.address || '');
                $('#addressDetail').val(d.addressDetail || '');
                $('#addressEn').val(d.addressEn || '');
                $('#addressDetailEn').val(d.addressDetailEn || '');

                // 성급 라디오
                if (d.starRating) {
                    $('input[name="starRating"][value="' + d.starRating + '"]').prop('checked', true);
                }

                // 소개
                $('#introduction').val(d.introduction || '');

                // 파일 정보 표시 (다운로드 링크)
                if (d.bizLicensePath) {
                    $('#bizLicensePath').val(d.bizLicensePath);
                    var bizFileName = d.bizLicensePath.split('/').pop();
                    PropertyForm.showBizLicenseDownload(d.bizLicensePath, bizFileName);
                }
                if (d.logoPath) {
                    $('#logoPath').val(d.logoPath);
                    var logoFileName = d.logoPath.split('/').pop();
                    PropertyForm.showLogoDownload(d.logoPath, logoFileName);
                }

                // TAX/봉사료 정보
                PropertyForm.populateTaxServiceCharge(d);
            }
        });
    },

    /** 시간대 문자열 파싱 → 셀렉트박스 설정 */
    parseTimezone: function(tz) {
        if (!tz) return;
        // UTC+09:00, UTC-05:30 등 처리
        var match = tz.match(/^UTC([+-])(\d{2}):(\d{2})$/);
        if (match) {
            $('#tzSign').val(match[1]);
            $('#tzHour').val(match[2]);
            $('#tzMinute').val(match[3]);
        } else if (tz === 'Asia/Seoul') {
            // 기존 데이터 호환 (Asia/Seoul → UTC+09:00)
            $('#tzSign').val('+');
            $('#tzHour').val('09');
            $('#tzMinute').val('00');
        }
    },

    /** 시간대 셀렉트 → 문자열 조합 */
    getTimezoneString: function() {
        var sign = $('#tzSign').val();
        var hour = $('#tzHour').val();
        var minute = $('#tzMinute').val();
        return 'UTC' + sign + hour + ':' + minute;
    },

    /** 전화번호 문자열 → 3칸 분배 */
    parsePhone: function(phone) {
        if (!phone) return;
        var parts = phone.split('-');
        if (parts.length === 3) {
            $('#phone1').val(parts[0]);
            $('#phone2').val(parts[1]);
            $('#phone3').val(parts[2]);
        } else if (parts.length === 2) {
            $('#phone1').val(parts[0]);
            $('#phone2').val(parts[1]);
        } else {
            $('#phone1').val(phone);
        }
    },

    /** 3칸 → 전화번호 문자열 조합 */
    getPhoneString: function() {
        var p1 = $.trim($('#phone1').val());
        var p2 = $.trim($('#phone2').val());
        var p3 = $.trim($('#phone3').val());
        if (!p1 && !p2 && !p3) return '';
        var parts = [p1, p2, p3].filter(function(p) { return p; });
        return parts.join('-');
    },

    /** 프로퍼티명 중복확인 */
    checkName: function() {
        var name = $.trim($('#propertyName').val());
        if (!name) {
            HolaPms.alert('warning', '프로퍼티명을 입력해주세요.');
            return;
        }

        var hotelId = $('#hotelSelect').val();
        if (!hotelId) {
            HolaPms.alert('warning', '호텔을 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/hotels/' + hotelId + '/properties/check-name',
            type: 'GET',
            data: { propertyName: name },
            success: function(res) {
                var result = $('#nameCheckResult');
                if (res.data.duplicate) {
                    result.html('<span class="text-danger"><i class="fas fa-times-circle me-1"></i>이미 사용 중인 프로퍼티명입니다.</span>');
                    PropertyForm.nameChecked = false;
                } else {
                    result.html('<span class="text-success"><i class="fas fa-check-circle me-1"></i>사용 가능한 프로퍼티명입니다.</span>');
                    PropertyForm.nameChecked = true;
                }
                result.show();
            }
        });
    },

    /** 우편번호 조회 (다음 주소 API) */
    searchAddress: function() {
        if (typeof daum !== 'undefined' && daum.Postcode) {
            new daum.Postcode({
                oncomplete: function(data) {
                    $('#zipCode').val(data.zonecode);
                    $('#address').val(data.roadAddress || data.jibunAddress);
                    $('#addressDetail').val('').focus();
                    // 영문 주소 자동 채우기
                    if (data.engAddress) {
                        $('#addressDetailEn').val(data.engAddress);
                    }
                }
            }).open();
        } else {
            HolaPms.alert('info', '우편번호 검색 서비스를 로드 중입니다. 잠시 후 다시 시도해주세요.');
        }
    },

    /** 필수값 검증 */
    validate: function() {
        // 등록 시 호텔 선택 필수
        if (!this.isEdit && !$('#hotelSelect').val()) {
            HolaPms.alert('warning', '호텔을 선택해주세요.');
            return false;
        }

        var name = $.trim($('#propertyName').val());
        if (!name) { HolaPms.alert('warning', '프로퍼티명을 입력해주세요.'); return false; }

        // 등록 시 중복확인 필수
        if (!this.isEdit && !this.nameChecked) {
            HolaPms.alert('warning', '프로퍼티명 중복확인을 해주세요.');
            return false;
        }

        if (!$.trim($('#representativeName').val())) {
            HolaPms.alert('warning', '대표자명(국문)을 입력해주세요.');
            return false;
        }
        if (!$.trim($('#businessNumber').val())) {
            HolaPms.alert('warning', '사업자등록번호를 입력해주세요.');
            return false;
        }

        var phone = this.getPhoneString();
        if (!phone) {
            HolaPms.alert('warning', '전화번호를 입력해주세요.');
            return false;
        }

        // 주소 필수
        if (!$.trim($('#zipCode').val()) || !$.trim($('#address').val())) {
            HolaPms.alert('warning', '프로퍼티 주소를 입력해주세요. (우편번호 조회를 이용하세요)');
            return false;
        }

        if (!$('input[name="starRating"]:checked').val()) {
            HolaPms.alert('warning', '프로퍼티 성급을 선택해주세요.');
            return false;
        }

        return true;
    },

    /** 저장 */
    save: function() {
        if (!this.validate()) return;

        var hotelId = this.isEdit ? null : $('#hotelSelect').val();
        if (!this.isEdit && !hotelId) {
            HolaPms.alert('warning', '호텔을 먼저 선택해주세요.');
            return;
        }

        var data = {
            propertyName: $.trim($('#propertyName').val()),
            starRating: $('input[name="starRating"]:checked').val() || '',
            representativeName: $.trim($('#representativeName').val()),
            representativeNameEn: $.trim($('#representativeNameEn').val()),
            businessNumber: $.trim($('#businessNumber').val()),
            countryCode: $('#countryCode').val(),
            phone: this.getPhoneString(),
            timezone: this.getTimezoneString(),
            zipCode: $.trim($('#zipCode').val()),
            address: $.trim($('#address').val()),
            addressDetail: $.trim($('#addressDetail').val()),
            addressEn: $.trim($('#addressEn').val()),
            addressDetailEn: $.trim($('#addressDetailEn').val()),
            introduction: $.trim($('#introduction').val()),
            bizLicensePath: $('#bizLicensePath').val() || null,
            logoPath: $('#logoPath').val() || null,
            useYn: $('input[name="useYn"]:checked').val() === 'true'
        };

        var url, method;
        if (this.isEdit) {
            url = '/api/v1/properties/' + this.propertyId;
            method = 'PUT';
        } else {
            url = '/api/v1/hotels/' + hotelId + '/properties';
            method = 'POST';
        }

        HolaPms.ajax({
            url: url,
            type: method,
            data: data,
            success: function() {
                // 등록 시 헤더 컨텍스트를 선택한 호텔로 동기화 (리스트에서 바로 보이도록)
                if (!PropertyForm.isEdit && hotelId) {
                    HolaPms.context.onHotelChange(hotelId);
                }
                HolaPms.alert('success', PropertyForm.isEdit ? '프로퍼티가 수정되었습니다.' : '프로퍼티가 등록되었습니다.');
                setTimeout(function() { location.href = '/admin/properties'; }, 500);
            }
        });
    },

    /** 삭제 */
    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + PropertyForm.propertyId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alert('success', '프로퍼티가 삭제되었습니다.');
                    setTimeout(function() { location.href = '/admin/properties'; }, 500);
                }
            });
        });
    },

    // ─── 정산정보 ────────────────────────────

    /** 정산정보 초기화: 이벤트 바인딩 */
    initSettlement: function() {
        var self = this;

        // 정산정보 탭 클릭 시 lazy load
        $('button[data-bs-target="#pane-settlement"]').on('shown.bs.tab', function() {
            if (self.isEdit && !self.settlementLoaded) {
                self.loadSettlements();
            }
        });

        // 수정 모드에서만 저장 버튼 표시
        if (this.isEdit) {
            $('#settlementSaveArea').show();
        }

        // 계좌번호 입력 제한 (숫자, - 만 허용)
        $('#krAccountNumber').on('input', function() {
            $(this).val($(this).val().replace(/[^0-9\-]/g, ''));
        });

        // KR/US 입력 시 반대 탭 비활성화
        $('#pane-stl-kr').on('input change', 'input, select', function() {
            PropertyForm.toggleSettlementTabs();
        });
        $('#pane-stl-us').on('input change', 'input, select', function() {
            PropertyForm.toggleSettlementTabs();
        });

        // 정산일 '말일' 선택 시 '일' 라벨 숨김
        $('#krSettlementDay').on('change', function() {
            $('#krDayLabel').toggle($(this).val() !== 'LAST');
        });
        $('#usSettlementDay').on('change', function() {
            $('#usDayLabel').toggle($(this).val() !== 'LAST');
        });

        // 통장사본 파일 업로드
        $('#btnBankBook').on('click', function() {
            $('#bankBookFile').trigger('click');
        });
        $('#bankBookFile').on('change', function() {
            var file = this.files[0];
            if (file) {
                PropertyForm.uploadFile(file, 'bank-book', function(data) {
                    $('#krBankBookPath').val(data.filePath);
                    $('#bankBookFileName').hide();
                    $('#bankBookDownloadName').text(data.fileName);
                    $('#bankBookDownload').attr('href', data.filePath).show();
                });
            }
        });
    },

    /** KR 탭에 데이터가 있는지 확인 */
    hasKRData: function() {
        return !!($.trim($('#krAccountNumber').val()) || $('#krBankCode').val());
    },

    /** US 탭에 데이터가 있는지 확인 */
    hasUSData: function() {
        return !!($.trim($('#usAccountNumber').val()) || $.trim($('#usBankName').val())
            || $.trim($('#usAccountHolder').val()) || $.trim($('#usRoutingNumber').val())
            || $.trim($('#usSwiftCode').val()));
    },

    /** KR/US 상호 배타 토글: 한쪽에 데이터 있으면 반대쪽 비활성화 */
    toggleSettlementTabs: function() {
        var hasKR = this.hasKRData();
        var hasUS = this.hasUSData();

        // KR에 데이터 → US 비활성화
        $('#pane-stl-us input, #pane-stl-us select').prop('disabled', hasKR);
        if (hasKR) {
            if (!$('#usDisabledMsg').length) {
                $('#pane-stl-us').prepend(
                    '<div id="usDisabledMsg" class="alert alert-warning py-2 mb-3">' +
                    '<i class="fas fa-info-circle me-1"></i>KR(국내) 정산정보가 입력되어 US(해외)는 입력할 수 없습니다.</div>'
                );
            }
        } else {
            $('#usDisabledMsg').remove();
        }

        // US에 데이터 → KR 비활성화
        $('#pane-stl-kr input, #pane-stl-kr select').prop('disabled', hasUS);
        if (hasUS) {
            if (!$('#krDisabledMsg').length) {
                $('#pane-stl-kr').prepend(
                    '<div id="krDisabledMsg" class="alert alert-warning py-2 mb-3">' +
                    '<i class="fas fa-info-circle me-1"></i>US(해외) 정산정보가 입력되어 KR(국내)는 입력할 수 없습니다.</div>'
                );
            }
        } else {
            $('#krDisabledMsg').remove();
        }
    },

    /** 정산정보 로드 (GET API) */
    loadSettlements: function() {
        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/settlements',
            type: 'GET',
            success: function(res) {
                var list = res.data || [];
                $.each(list, function(i, s) {
                    if (s.countryType === 'KR') {
                        PropertyForm.populateKR(s);
                    } else if (s.countryType === 'US') {
                        PropertyForm.populateUS(s);
                    }
                });
                PropertyForm.settlementLoaded = true;
                PropertyForm.toggleSettlementTabs();
            }
        });
    },

    /** KR 데이터 세팅 */
    populateKR: function(s) {
        $('#krAccountNumber').val(s.accountNumber || '');
        $('#krBankCode').val(s.bankCode || '');
        if (s.settlementDay) {
            $('#krSettlementDay').val(s.settlementDay);
            $('#krDayLabel').toggle(s.settlementDay !== 'LAST');
        }
        if (s.bankBookPath) {
            $('#krBankBookPath').val(s.bankBookPath);
            var fileName = s.bankBookPath.split('/').pop();
            $('#bankBookFileName').hide();
            $('#bankBookDownloadName').text(fileName);
            $('#bankBookDownload').attr('href', s.bankBookPath).show();
        }
    },

    /** US 데이터 세팅 */
    populateUS: function(s) {
        $('#usAccountNumber').val(s.accountNumber || '');
        $('#usBankName').val(s.bankName || '');
        $('#usAccountHolder').val(s.accountHolder || '');
        $('#usRoutingNumber').val(s.routingNumber || '');
        $('#usSwiftCode').val(s.swiftCode || '');
        if (s.settlementDay) {
            $('#usSettlementDay').val(s.settlementDay);
            $('#usDayLabel').toggle(s.settlementDay !== 'LAST');
        }
    },

    // ─── 취소 수수료 ────────────────────────────

    /** 취소 수수료 초기화 */
    initCancelFee: function() {
        var self = this;

        // 취소 수수료 탭 클릭 시 lazy load
        $('button[data-bs-target="#pane-cancel"]').on('shown.bs.tab', function() {
            if (self.isEdit && !self.cancelFeeLoaded) {
                self.loadCancellationFees();
            } else if (!self.cancelFeeLoaded) {
                // 등록 모드: 디폴트 2행
                self.addCancelFeeRow();
                self.addCancelFeeRow();
                self.cancelFeeLoaded = true;
            }
        });

        // 수정 모드에서만 저장 버튼 표시
        if (this.isEdit) {
            $('#cancelFeeSaveArea').show();
        }
    },

    /** 취소 수수료 행 추가 */
    addCancelFeeRow: function(data) {
        var basis = (data && data.checkinBasis) || '';
        var days = (data && data.daysBefore != null) ? data.daysBefore : '';
        var amount = (data && data.feeAmount != null) ? data.feeAmount : '';
        var type = (data && data.feeType) || 'PERCENTAGE';

        var daysDisabled = (basis === '' || basis === 'NOSHOW') ? 'disabled' : '';
        var label = (basis === 'NOSHOW') ? '시' : '일 전 취소 시';

        var html = '<tr>' +
            '<td>' +
                '<select class="form-select form-select-sm" onchange="PropertyForm.onCheckinBasisChange(this)">' +
                    '<option value=""' + (basis === '' ? ' selected' : '') + '>선택</option>' +
                    '<option value="DATE"' + (basis === 'DATE' ? ' selected' : '') + '>일자</option>' +
                    '<option value="NOSHOW"' + (basis === 'NOSHOW' ? ' selected' : '') + '>노쇼</option>' +
                '</select>' +
            '</td>' +
            '<td><input type="text" class="form-control form-control-sm cancel-days" value="' + days + '" ' + daysDisabled +
                ' oninput="this.value=this.value.replace(/[^0-9]/g,\'\')"></td>' +
            '<td class="cancel-label text-muted">' + label + '</td>' +
            '<td><input type="text" class="form-control form-control-sm cancel-amount" value="' + amount + '"' +
                ' oninput="this.value=this.value.replace(/[^0-9.]/g,\'\')"></td>' +
            '<td>' +
                '<select class="form-select form-select-sm cancel-type">' +
                    '<option value="PERCENTAGE"' + (type === 'PERCENTAGE' ? ' selected' : '') + '>정율(%)</option>' +
                    '<option value="FIXED_KRW"' + (type === 'FIXED_KRW' ? ' selected' : '') + '>정액(KRW)</option>' +
                    '<option value="FIXED_USD"' + (type === 'FIXED_USD' ? ' selected' : '') + '>정액(USD)</option>' +
                '</select>' +
            '</td>' +
            '<td class="text-center">' +
                '<button type="button" class="btn btn-outline-danger btn-sm" onclick="PropertyForm.removeCancelFeeRow(this)">' +
                    '<i class="fas fa-minus"></i>' +
                '</button>' +
            '</td>' +
            '</tr>';

        $('#cancelFeeBody').append(html);
    },

    /** 취소 수수료 행 삭제 */
    removeCancelFeeRow: function(btn) {
        var rowCount = $('#cancelFeeBody tr').length;
        if (rowCount <= 1) {
            HolaPms.alert('warning', '최소 1행은 유지해야 합니다.');
            return;
        }
        $(btn).closest('tr').remove();
    },

    /** 체크인 기준 변경 이벤트 */
    onCheckinBasisChange: function(select) {
        var $row = $(select).closest('tr');
        var val = $(select).val();

        var $days = $row.find('.cancel-days');
        var $label = $row.find('.cancel-label');

        if (val === '' || val === 'NOSHOW') {
            $days.val('').prop('disabled', true);
        } else {
            $days.prop('disabled', false);
        }

        $label.text(val === 'NOSHOW' ? '시' : '일 전 취소 시');
    },

    /** 취소 수수료 로드 (GET API) */
    loadCancellationFees: function() {
        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/cancellation-fees',
            type: 'GET',
            success: function(res) {
                var list = res.data || [];
                $('#cancelFeeBody').empty();
                if (list.length === 0) {
                    // 데이터 없으면 디폴트 2행
                    PropertyForm.addCancelFeeRow();
                    PropertyForm.addCancelFeeRow();
                } else {
                    $.each(list, function(i, fee) {
                        PropertyForm.addCancelFeeRow(fee);
                    });
                }
                PropertyForm.cancelFeeLoaded = true;
            }
        });
    },

    /** 취소 수수료 저장 (PUT API) */
    saveCancellationFees: function() {
        if (!this.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 저장해주세요.');
            return;
        }

        var fees = [];
        var valid = true;

        $('#cancelFeeBody tr').each(function() {
            var basis = $(this).find('select:first').val();
            if (!basis) {
                // 체크인 기준 미선택 행은 스킵
                return true;
            }

            var days = $(this).find('.cancel-days').val();
            var amount = $(this).find('.cancel-amount').val();
            var type = $(this).find('.cancel-type').val();

            if (basis === 'DATE' && !days) {
                HolaPms.alert('warning', '일자 기준의 N일을 입력해주세요.');
                valid = false;
                return false;
            }

            if (!amount) {
                HolaPms.alert('warning', '수수료 금액을 입력해주세요.');
                valid = false;
                return false;
            }

            fees.push({
                checkinBasis: basis,
                daysBefore: basis === 'NOSHOW' ? null : (days ? parseInt(days) : null),
                feeAmount: parseFloat(amount),
                feeType: type
            });
        });

        if (!valid) return;

        if (fees.length === 0) {
            HolaPms.alert('warning', '저장할 취소 수수료 항목이 없습니다. 체크인 기준을 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/cancellation-fees',
            type: 'PUT',
            data: { fees: fees },
            success: function() {
                HolaPms.alert('success', '취소 수수료가 저장되었습니다.');
            }
        });
    },

    /** 정산정보 저장 (KR 또는 US 중 1건만 PUT) */
    saveSettlements: function() {
        if (!this.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 저장해주세요.');
            return;
        }

        var hasKR = this.hasKRData();
        var hasUS = this.hasUSData();

        // KR, US 동시 입력 차단
        if (hasKR && hasUS) {
            HolaPms.alert('warning', 'KR(국내)과 US(해외) 정산정보는 하나만 입력할 수 있습니다.');
            return;
        }

        if (!hasKR && !hasUS) {
            HolaPms.alert('info', '저장할 정산정보가 없습니다.');
            return;
        }

        var url = '/api/v1/properties/' + this.propertyId + '/settlements';

        if (hasKR) {
            var krBankCode = $('#krBankCode').val();
            var krBankName = $('#krBankCode option:selected').text();
            if (krBankCode === '') krBankName = '';

            HolaPms.ajax({
                url: url,
                type: 'PUT',
                data: {
                    countryType: 'KR',
                    accountNumber: $.trim($('#krAccountNumber').val()),
                    bankName: krBankName,
                    bankCode: krBankCode,
                    settlementDay: $('#krSettlementDay').val() || null,
                    bankBookPath: $('#krBankBookPath').val() || null
                },
                success: function() {
                    HolaPms.alert('success', '정산정보(KR)가 저장되었습니다.');
                }
            });
        }

        if (hasUS) {
            HolaPms.ajax({
                url: url,
                type: 'PUT',
                data: {
                    countryType: 'US',
                    accountNumber: $.trim($('#usAccountNumber').val()),
                    bankName: $.trim($('#usBankName').val()),
                    accountHolder: $.trim($('#usAccountHolder').val()),
                    routingNumber: $.trim($('#usRoutingNumber').val()),
                    swiftCode: $.trim($('#usSwiftCode').val()),
                    settlementDay: $('#usSettlementDay').val() || null
                },
                success: function() {
                    HolaPms.alert('success', '정산정보(US)가 저장되었습니다.');
                }
            });
        }
    },

    // ─── TAX/봉사료 ────────────────────────────

    /** TAX/봉사료 탭 초기화 */
    initTaxServiceCharge: function() {
        var self = this;

        // 탭 표시 시 저장 버튼 노출 (수정모드)
        $('button[data-bs-target="#pane-tax"]').on('shown.bs.tab', function() {
            if (self.isEdit) {
                $('#taxSaveArea').show();
            }
        });

        // 숫자 입력 제한 (비율 필드: 0~100, 소수 포함)
        $('#taxRate, #serviceChargeRate').on('input', function() {
            var val = parseFloat($(this).val());
            if (val < 0) $(this).val(0);
            if (val > 100) $(this).val(100);
        });

        // 소수점 자릿수 필드: 정수만
        $('#taxDecimalPlaces, #serviceChargeDecimalPlaces').on('input', function() {
            var val = parseInt($(this).val(), 10);
            if (isNaN(val) || val < 0) $(this).val(0);
            if (val > 10) $(this).val(10);
        });
    },

    /** TAX/봉사료 데이터 → 폼 채우기 */
    populateTaxServiceCharge: function(data) {
        if (data.taxRate != null) $('#taxRate').val(data.taxRate);
        if (data.taxDecimalPlaces != null) $('#taxDecimalPlaces').val(data.taxDecimalPlaces);
        if (data.taxRoundingMethod) $('#taxRoundingMethod').val(data.taxRoundingMethod);
        if (data.serviceChargeRate != null) $('#serviceChargeRate').val(data.serviceChargeRate);
        if (data.serviceChargeDecimalPlaces != null) $('#serviceChargeDecimalPlaces').val(data.serviceChargeDecimalPlaces);
        if (data.serviceChargeRoundingMethod) $('#serviceChargeRoundingMethod').val(data.serviceChargeRoundingMethod);
    },

    /** TAX/봉사료 저장 */
    saveTaxServiceCharge: function() {
        if (!this.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 저장해주세요.');
            return;
        }

        var taxRate = $('#taxRate').val();
        var serviceChargeRate = $('#serviceChargeRate').val();

        // 필수값 검증
        if (taxRate === '' || taxRate == null) {
            HolaPms.alert('warning', 'TAX 비율을 입력해주세요.');
            $('#taxRate').focus();
            return;
        }
        if (serviceChargeRate === '' || serviceChargeRate == null) {
            HolaPms.alert('warning', '봉사료 비율을 입력해주세요.');
            $('#serviceChargeRate').focus();
            return;
        }

        // 범위 검증
        if (parseFloat(taxRate) < 0 || parseFloat(taxRate) > 100) {
            HolaPms.alert('warning', 'TAX 비율은 0~100 사이 값을 입력해주세요.');
            $('#taxRate').focus();
            return;
        }
        if (parseFloat(serviceChargeRate) < 0 || parseFloat(serviceChargeRate) > 100) {
            HolaPms.alert('warning', '봉사료 비율은 0~100 사이 값을 입력해주세요.');
            $('#serviceChargeRate').focus();
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/tax-service-charge',
            type: 'PUT',
            data: {
                taxRate: parseFloat(taxRate),
                taxDecimalPlaces: parseInt($('#taxDecimalPlaces').val() || '0', 10),
                taxRoundingMethod: $('#taxRoundingMethod').val() || null,
                serviceChargeRate: parseFloat(serviceChargeRate),
                serviceChargeDecimalPlaces: parseInt($('#serviceChargeDecimalPlaces').val() || '0', 10),
                serviceChargeRoundingMethod: $('#serviceChargeRoundingMethod').val() || null
            },
            success: function() {
                HolaPms.alert('success', 'TAX/봉사료 정보가 저장되었습니다.');
            }
        });
    }
};

$(document).ready(function() {
    if ($('#propertyForm').length) {
        PropertyForm.init();
    }
});
