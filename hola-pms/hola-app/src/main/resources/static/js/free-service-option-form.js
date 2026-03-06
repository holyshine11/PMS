/**
 * 무료 옵션 관리 - 등록/수정 폼 페이지
 */
var FreeServiceOptionForm = {
    editId: null,
    duplicateChecked: false,

    init: function() {
        this.editId = $('#freeServiceOptionId').val() || null;
        this.bindEvents();
        this.updateHotelPropertyName();

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

    setCreateMode: function() {
        $('#pageTitle').html('<i class="fas fa-gift me-2"></i>무료 옵션 관리');
        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();
        $('#serviceOptionCode').prop('readonly', false);
        $('#btnCheckDuplicate').show();
    },

    setEditMode: function() {
        $('#pageTitle').html('<i class="fas fa-gift me-2"></i>무료 옵션 관리');
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
            url: '/api/v1/properties/' + propertyId + '/free-service-options/' + self.editId,
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

                    // 최종 수정일시
                    var ts = d.updatedAt || d.createdAt;
                    $('#updatedAt').text(ts ? ts.replace('T', ' ').substring(0, 19) : '-');

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
            url: '/api/v1/properties/' + propertyId + '/free-service-options/check-code',
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

        // 신규 등록 시 중복확인 필수
        if (!self.editId && !self.duplicateChecked) {
            HolaPms.alert('warning', '서비스 옵션 코드 중복확인을 해주세요.');
            return;
        }

        var url, method, data;
        if (self.editId) {
            url = '/api/v1/properties/' + propertyId + '/free-service-options/' + self.editId;
            method = 'PUT';
            data = {
                serviceNameKo: serviceNameKo,
                serviceNameEn: serviceNameEn || null,
                serviceType: serviceType,
                applicableNights: applicableNights,
                quantity: quantity,
                quantityUnit: quantityUnit,
                useYn: useYn
            };
        } else {
            url = '/api/v1/properties/' + propertyId + '/free-service-options';
            method = 'POST';
            data = {
                serviceOptionCode: serviceOptionCode,
                serviceNameKo: serviceNameKo,
                serviceNameEn: serviceNameEn || null,
                serviceType: serviceType,
                applicableNights: applicableNights,
                quantity: quantity,
                quantityUnit: quantityUnit,
                useYn: useYn
            };
        }

        $.ajax({
            url: url,
            method: method,
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', self.editId ? '수정되었습니다.' : '등록되었습니다.');
                    window.location.href = '/admin/free-service-options';
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
            url: '/api/v1/properties/' + propertyId + '/free-service-options/' + self.editId,
            method: 'DELETE',
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '삭제되었습니다.');
                    window.location.href = '/admin/free-service-options';
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    }
};

$(document).ready(function() {
    FreeServiceOptionForm.init();
});
