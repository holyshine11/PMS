/**
 * 유료 옵션 관리 - 등록/수정 폼 페이지
 */
var PaidServiceOptionForm = {
    editId: null,
    duplicateChecked: false,

    init: function() {
        this.editId = $('#paidServiceOptionId').val() || null;
        this.bindEvents();
        this.updateHotelPropertyName();
        this.toggleVatFields();

        if (this.editId) {
            this.loadData();
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

        // 코드 입력 시 중복확인 리셋
        $('#serviceOptionCode').on('input', function() {
            self.duplicateChecked = false;
            $('#codeCheckResult').text('');
        });

        // 공급가 변경 시 TAX 자동계산
        $('#supplyPrice').on('input', function() {
            self.calculateTax();
        });

        // 세율 변경 시 TAX 자동계산
        $('#taxRate').on('input', function() {
            self.calculateTax();
        });

        // 통화 변경 시 모든 통화 라벨 갱신
        $('input[name="currencyCode"]').on('change', function() {
            var curr = $(this).val();
            var label = curr === 'USD' ? '$' : '원';
            $('#currencyLabel').text(label);
            $('.currency-unit').text(label);
        });

        // 부가세 포함여부 변경 시 TAX/VAT 필드 토글 + 재계산
        $('input[name="vatIncluded"]').on('change', function() {
            self.toggleVatFields();
            self.calculateTax();
        });
    },

    toggleVatFields: function() {
        // 부가세 포함여부와 무관하게 세율/TAX/VAT 항상 표시 및 입력 가능
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

    /** TAX 자동 계산 (입력한 세율 기준, 부가세 포함여부 무관하게 항상 계산) */
    calculateTax: function() {
        var supplyPrice = parseFloat($('#supplyPrice').val()) || 0;
        var taxRate = parseFloat($('#taxRate').val()) || 0;

        if (supplyPrice <= 0 || taxRate <= 0) {
            $('#taxAmount').val('0');
            $('#vatIncludedPrice').val(supplyPrice > 0 ? this.formatNumber(supplyPrice) : '0');
            return;
        }

        // TAX = 공급가 x (세율 / 100), 소수점 2자리 반올림
        var rawTax = supplyPrice * (taxRate / 100);
        var taxAmount = Math.round(rawTax * 100) / 100;
        var vatIncludedPrice = supplyPrice + taxAmount;

        $('#taxAmount').val(this.formatNumber(taxAmount));
        $('#vatIncludedPrice').val(this.formatNumber(vatIncludedPrice));
    },

    /** 숫자 포맷 (천단위 콤마) */
    formatNumber: function(num) {
        if (num == null) return '0';
        // 소수점이 있으면 소수점 유지, 없으면 정수
        var n = Number(num);
        if (n % 1 === 0) {
            return n.toLocaleString('ko-KR');
        }
        return n.toLocaleString('ko-KR', { minimumFractionDigits: 1, maximumFractionDigits: 2 });
    },

    /** 숫자 파싱 (콤마 제거) */
    parseNumber: function(str) {
        if (!str) return 0;
        return parseFloat(String(str).replace(/,/g, '')) || 0;
    },

    setCreateMode: function() {
        $('#pageTitle').html('<i class="fas fa-coins me-2"></i>유료 옵션 관리');
        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();
        $('#serviceOptionCode').prop('readonly', false);
        $('#btnCheckDuplicate').show();
    },

    setEditMode: function() {
        $('#pageTitle').html('<i class="fas fa-coins me-2"></i>유료 옵션 관리');
        $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');
        $('#btnDelete').show();
        $('#serviceOptionCode').prop('readonly', true);
        $('#btnCheckDuplicate').hide();
        $('#updatedAtRow').show();
        this.duplicateChecked = true;
    },

    loadData: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/paid-service-options/' + self.editId,
            method: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    var d = res.data;
                    $('#serviceOptionCode').val(d.serviceOptionCode);
                    $('#serviceNameKo').val(d.serviceNameKo);
                    $('#serviceNameEn').val(d.serviceNameEn || '');
                    $('#serviceType').val(d.serviceType);
                    $('input[name="useYn"][value="' + d.useYn + '"]').prop('checked', true);
                    $('input[name="applicableNights"][value="' + d.applicableNights + '"]').prop('checked', true);
                    $('#quantity').val(d.quantity);
                    $('#quantityUnit').val(d.quantityUnit);
                    $('#adminMemo').val(d.adminMemo || '');

                    // 가격정보
                    $('input[name="currencyCode"][value="' + d.currencyCode + '"]').prop('checked', true);
                    var currLabel = d.currencyCode === 'USD' ? '$' : '원';
                    $('#currencyLabel').text(currLabel);
                    $('.currency-unit').text(currLabel);

                    // 부가세 포함여부
                    var vatInc = d.vatIncluded != null ? d.vatIncluded : true;
                    $('input[name="vatIncluded"][value="' + vatInc + '"]').prop('checked', true);
                    self.toggleVatFields();

                    // 세율
                    $('#taxRate').val(d.taxRate || 0);

                    $('#supplyPrice').val(d.supplyPrice || 0);
                    $('#taxAmount').val(self.formatNumber(d.taxAmount || 0));
                    $('#vatIncludedPrice').val(self.formatNumber(d.vatIncludedPrice || 0));

                    // 최종 수정일시
                    var ts = d.updatedAt || d.createdAt;
                    $('#updatedAt').text(HolaPms.formatDateTime(ts));

                    self.setEditMode();
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#serviceOptionCode').val());
        if (!code) {
            HolaPms.alert('warning', '서비스 옵션 코드명을 입력해주세요.');
            $('#serviceOptionCode').focus();
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/paid-service-options/check-code',
            method: 'GET',
            data: { serviceOptionCode: code },
            success: function(res) {
                if (res.data.duplicate) {
                    $('#codeCheckResult').text('이미 사용 중인 코드입니다.').removeClass('text-primary').addClass('text-danger');
                    self.duplicateChecked = false;
                } else {
                    $('#codeCheckResult').text('사용 가능한 코드입니다.').removeClass('text-danger').addClass('text-primary');
                    self.duplicateChecked = true;
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    save: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var serviceOptionCode = $.trim($('#serviceOptionCode').val());
        var serviceNameKo = $.trim($('#serviceNameKo').val());
        var serviceNameEn = $.trim($('#serviceNameEn').val());
        var serviceType = $('#serviceType').val();
        var applicableNights = $('input[name="applicableNights"]:checked').val();
        var quantity = parseInt($('#quantity').val(), 10);
        var quantityUnit = $('#quantityUnit').val();
        var useYn = $('input[name="useYn"]:checked').val() === 'true';
        var currencyCode = $('input[name="currencyCode"]:checked').val();
        var vatIncluded = $('input[name="vatIncluded"]:checked').val() === 'true';
        var taxRate = parseFloat($('#taxRate').val()) || 0;
        var supplyPrice = parseFloat($('#supplyPrice').val()) || 0;
        var taxAmount = vatIncluded ? self.parseNumber($('#taxAmount').val()) : 0;
        var vatIncludedPrice = vatIncluded ? self.parseNumber($('#vatIncludedPrice').val()) : supplyPrice;
        var adminMemo = $.trim($('#adminMemo').val());

        // 필수 검증
        if (!serviceOptionCode) {
            HolaPms.alert('warning', '서비스 옵션 코드명을 입력해주세요.');
            $('#serviceOptionCode').focus();
            return;
        }
        if (!serviceNameKo) {
            HolaPms.alert('warning', '서비스명(국문)을 입력해주세요.');
            $('#serviceNameKo').focus();
            return;
        }
        if (!serviceType) {
            HolaPms.alert('warning', '서비스 옵션 유형을 선택해주세요.');
            $('#serviceType').focus();
            return;
        }
        if (!applicableNights) {
            HolaPms.alert('warning', '적용 박수를 선택해주세요.');
            return;
        }
        if (!quantity || quantity < 1) {
            HolaPms.alert('warning', '수량을 1 이상 입력해주세요.');
            $('#quantity').focus();
            return;
        }
        if (!quantityUnit) {
            HolaPms.alert('warning', '수량 단위를 선택해주세요.');
            return;
        }
        if (supplyPrice < 0) {
            HolaPms.alert('warning', '공급가는 0 이상이어야 합니다.');
            $('#supplyPrice').focus();
            return;
        }

        // 신규 등록 시 중복확인 필수
        if (!self.editId && !self.duplicateChecked) {
            HolaPms.alert('warning', '서비스 옵션 코드 중복확인을 해주세요.');
            return;
        }

        var data = {
            serviceNameKo: serviceNameKo,
            serviceNameEn: serviceNameEn || null,
            serviceType: serviceType,
            applicableNights: applicableNights,
            currencyCode: currencyCode,
            vatIncluded: vatIncluded,
            taxRate: taxRate,
            supplyPrice: supplyPrice,
            taxAmount: taxAmount,
            vatIncludedPrice: vatIncludedPrice,
            quantity: quantity,
            quantityUnit: quantityUnit,
            adminMemo: adminMemo || null,
            useYn: useYn
        };

        var url, method;
        if (self.editId) {
            url = '/api/v1/properties/' + propertyId + '/paid-service-options/' + self.editId;
            method = 'PUT';
        } else {
            url = '/api/v1/properties/' + propertyId + '/paid-service-options';
            method = 'POST';
            data.serviceOptionCode = serviceOptionCode;
        }

        $.ajax({
            url: url,
            method: method,
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(res) {
                if (res.success) {
                    HolaPms.alertAndRedirect('success', self.editId ? '수정되었습니다.' : '등록되었습니다.', '/admin/paid-service-options');
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    remove: function() {
        var self = this;
        if (!self.editId) return;

        if (!confirm('정말 삭제하시겠습니까?')) return;

        var propertyId = HolaPms.context.getPropertyId();
        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/paid-service-options/' + self.editId,
            method: 'DELETE',
            success: function(res) {
                if (res.success) {
                    HolaPms.alertAndRedirect('success', '삭제되었습니다.', '/admin/paid-service-options');
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    }
};

$(document).ready(function() {
    PaidServiceOptionForm.init();
});
