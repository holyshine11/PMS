/**
 * 프로모션 코드 관리 - 등록/수정 폼
 */
var PromotionCodeForm = {
    isEditMode: false,
    codeChecked: false,
    allRateCodes: [],
    selectedRateCodeId: null,

    init: function() {
        var id = $('#promotionCodeId').val();
        this.isEditMode = !!id;

        // 호텔/프로퍼티명
        var hotelName = HolaPms.context.getHotelName();
        var propertyName = HolaPms.context.getPropertyName();
        if (hotelName && propertyName) {
            $('#hotelPropertyName').text(hotelName + ' > ' + propertyName);
        }

        // 컨텍스트 미선택 확인
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#btnSave').prop('disabled', true);
            return;
        }

        if (this.isEditMode) {
            this.loadData(id);
        }

        this.bindEvents();
    },

    bindEvents: function() {
        var self = this;

        // 프로모션 기간 날짜 범위 제한
        HolaPms.bindDateRange('#promotionStartDate', '#promotionEndDate');
        // 모달 내 레이트코드 검색 날짜 범위 제한
        HolaPms.bindDateRange('#modalSearchStartDate', '#modalSearchEndDate');

        // Down/Up sign 변경 시 필드 활성화/비활성화
        $('#downUpSign').on('change', function() {
            var hasSign = !!$(this).val();
            $('#downUpValue').prop('disabled', !hasSign);
            $('input[name="downUpUnit"]').prop('disabled', !hasSign);
            if (!hasSign) {
                $('#downUpValue').val('');
            }
        });
        // 초기 상태 설정
        $('#downUpSign').trigger('change');

        // 코드 변경 시 중복확인 리셋
        $('#promotionCode').on('input', function() {
            self.codeChecked = false;
            $('#codeCheckResult').html('');
        });

        // 컨텍스트 변경 시 호텔/프로퍼티명 갱신
        $(document).on('hola:contextChange', function() {
            var hotelName = HolaPms.context.getHotelName();
            var propertyName = HolaPms.context.getPropertyName();
            if (hotelName && propertyName) {
                $('#hotelPropertyName').text(hotelName + ' > ' + propertyName);
            }
            // 컨텍스트 미선택 시 경고
            var propertyId = HolaPms.context.getPropertyId();
            if (!propertyId) {
                $('#contextAlert').removeClass('d-none');
                $('#btnSave').prop('disabled', true);
            } else {
                $('#contextAlert').addClass('d-none');
                $('#btnSave').prop('disabled', false);
            }
        });
    },

    loadData: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/promotion-codes/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;

                // 레이트 코드
                self.selectedRateCodeId = data.rateCodeId;
                $('#rateCodeId').val(data.rateCodeId);
                $('#rateCodeName').val(data.rateCode || '');
                $('#btnRateCodeLookup').hide();
                $('#btnRateCodeChange').show();

                // 프로모션 코드 (수정모드에서는 읽기전용)
                $('#promotionCode').val(data.promotionCode).prop('readonly', true);
                $('#btnCheckDuplicate').hide();
                self.codeChecked = true;

                // 기간
                $('#promotionStartDate').val(data.promotionStartDate || '');
                $('#promotionEndDate').val(data.promotionEndDate || '');

                // 설명
                $('#descriptionKo').val(data.descriptionKo || '');
                $('#descriptionEn').val(data.descriptionEn || '');

                // 타입
                $('#promotionType').val(data.promotionType);

                // 사용여부
                $('input[name="useYn"][value="' + data.useYn + '"]').prop('checked', true);

                // 최종 수정일
                if (data.updatedAt) {
                    $('#updatedAt').text(data.updatedAt);
                    $('#updatedAtRow').show();
                }

                // Down/Up sale
                $('#downUpSign').val(data.downUpSign || '').trigger('change');
                if (data.downUpValue != null) $('#downUpValue').val(data.downUpValue);
                if (data.downUpUnit) $('input[name="downUpUnit"][value="' + data.downUpUnit + '"]').prop('checked', true);
                if (data.roundingDecimalPoint != null) $('#roundingDecimalPoint').val(data.roundingDecimalPoint);
                if (data.roundingMethod) $('#roundingMethod').val(data.roundingMethod);

                // 버튼 변경
                $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');
                // 삭제 버튼: 사용여부 N일때만
                if (data.useYn === false) {
                    $('#btnDelete').show();
                }
            }
        });
    },

    checkDuplicate: function() {
        var code = $('#promotionCode').val().trim();
        if (!code) {
            HolaPms.alert('warning', '프로모션 코드를 입력하세요.');
            return;
        }
        var propertyId = HolaPms.context.getPropertyId();
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/promotion-codes/check-code',
            type: 'GET',
            data: { promotionCode: code },
            success: function(res) {
                if (res.data.duplicate) {
                    $('#codeCheckResult').html('<span class="text-danger"><i class="fas fa-times-circle"></i> 이미 사용 중인 코드입니다.</span>');
                    self.codeChecked = false;
                } else {
                    $('#codeCheckResult').html('<span class="text-success"><i class="fas fa-check-circle"></i> 사용 가능한 코드입니다.</span>');
                    self.codeChecked = true;
                }
            }
        });
    },

    save: function() {
        // validation
        if (!this.selectedRateCodeId && !$('#rateCodeId').val()) {
            HolaPms.alert('warning', '레이트 코드를 선택하세요.');
            return;
        }
        if (!$('#promotionCode').val().trim()) {
            HolaPms.alert('warning', '프로모션 코드를 입력하세요.');
            return;
        }
        if (!this.isEditMode && !this.codeChecked) {
            HolaPms.alert('warning', '프로모션 코드 중복확인을 해주세요.');
            return;
        }
        if (!$('#promotionStartDate').val() || !$('#promotionEndDate').val()) {
            HolaPms.alert('warning', '프로모션 기간을 입력하세요.');
            return;
        }
        if ($('#promotionEndDate').val() < $('#promotionStartDate').val()) {
            HolaPms.alert('warning', '종료일은 시작일보다 이후여야 합니다.');
            return;
        }
        if (!$('#descriptionKo').val().trim()) {
            HolaPms.alert('warning', '국문 설명을 입력하세요.');
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        var downUpSign = $('#downUpSign').val();

        var requestData = {
            rateCodeId: this.selectedRateCodeId || parseInt($('#rateCodeId').val()),
            promotionCode: $('#promotionCode').val().trim(),
            promotionStartDate: $('#promotionStartDate').val(),
            promotionEndDate: $('#promotionEndDate').val(),
            descriptionKo: $('#descriptionKo').val().trim(),
            descriptionEn: $('#descriptionEn').val().trim() || null,
            promotionType: $('#promotionType').val(),
            useYn: $('input[name="useYn"]:checked').val() === 'true',
            downUpSign: downUpSign || null,
            downUpValue: downUpSign ? (function() {
                var v = $('#downUpValue').val();
                return v !== '' ? parseFloat(v) : null;
            })() : null,
            downUpUnit: downUpSign ? ($('input[name="downUpUnit"]:checked').val() || null) : null,
            roundingDecimalPoint: parseInt($('#roundingDecimalPoint').val()) || 0,
            roundingMethod: $('#roundingMethod').val() || null
        };

        var self = this;
        if (this.isEditMode) {
            var id = $('#promotionCodeId').val();
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/promotion-codes/' + id,
                type: 'PUT',
                data: requestData,
                success: function() {
                    HolaPms.alertAndRedirect('success', '저장되었습니다.', '/admin/promotion-codes');
                }
            });
        } else {
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/promotion-codes',
                type: 'POST',
                data: requestData,
                success: function(res) {
                    HolaPms.alertAndRedirect('success', '등록되었습니다.', '/admin/promotion-codes');
                }
            });
        }
    },

    remove: function() {
        if (!confirm('삭제하시겠습니까?')) return;
        var id = $('#promotionCodeId').val();
        var propertyId = HolaPms.context.getPropertyId();
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/promotion-codes/' + id,
            type: 'DELETE',
            success: function() {
                HolaPms.alertAndRedirect('success', '삭제되었습니다.', '/admin/promotion-codes');
            }
        });
    },

    // ===== 레이트 코드 조회 모달 =====
    openRateCodeModal: function() {
        this.loadRateCodes();
        HolaPms.modal.show('#rateCodeModal');
    },

    loadRateCodes: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/rate-codes',
            type: 'GET',
            success: function(res) {
                self.allRateCodes = res.data || [];
                self.renderRateCodeTable(self.allRateCodes);
            }
        });
    },

    renderRateCodeTable: function(data) {
        var tbody = $('#modalRateCodeTable tbody');
        tbody.empty();
        if (!data || data.length === 0) {
            tbody.append('<tr><td colspan="7" class="text-center text-muted">데이터가 없습니다.</td></tr>');
            return;
        }
        var selectedId = this.selectedRateCodeId || $('#rateCodeId').val();
        data.forEach(function(rc, idx) {
            var checked = (rc.id == selectedId) ? ' checked' : '';
            var useYnBadge = rc.useYn
                ? '<span class="badge bg-success">Y</span>'
                : '<span class="badge bg-secondary">N</span>';
            tbody.append(
                '<tr>' +
                '<td class="text-center">' + (idx + 1) + '</td>' +
                '<td>' + HolaPms.escapeHtml(rc.rateCode) + '</td>' +
                '<td class="text-center">' + (rc.saleStartDate || '-') + ' ~ ' + (rc.saleEndDate || '-') + '</td>' +
                '<td class="text-center">' + HolaPms.escapeHtml(rc.rateCategory || '-') + '</td>' +
                '<td class="text-center">' + HolaPms.escapeHtml(rc.currency || '-') + '</td>' +
                '<td class="text-center">' + useYnBadge + '</td>' +
                '<td class="text-center"><input type="radio" name="selectRateCode" value="' + rc.id + '" data-code="' + HolaPms.escapeHtml(rc.rateCode) + '"' + checked + '></td>' +
                '</tr>'
            );
        });
    },

    searchRateCodes: function() {
        var code = $('#modalSearchRateCode').val().trim().toLowerCase();
        var useYn = $('input[name="modalUseYn"]:checked').val();
        var startDate = $('#modalSearchStartDate').val();
        var endDate = $('#modalSearchEndDate').val();

        var filtered = this.allRateCodes.filter(function(rc) {
            if (code && rc.rateCode.toLowerCase().indexOf(code) === -1) return false;
            if (useYn === 'true' && rc.useYn !== true) return false;
            if (useYn === 'false' && rc.useYn !== false) return false;
            if (startDate && rc.saleEndDate && rc.saleEndDate < startDate) return false;
            if (endDate && rc.saleStartDate && rc.saleStartDate > endDate) return false;
            return true;
        });
        this.renderRateCodeTable(filtered);
    },

    resetRateCodeSearch: function() {
        $('#modalSearchRateCode').val('');
        $('#modalSearchStartDate').val('').removeAttr('max');
        $('#modalSearchEndDate').val('').removeAttr('min');
        $('#modalUseYnAll').prop('checked', true);
        this.renderRateCodeTable(this.allRateCodes);
    },

    applyRateCode: function() {
        var selected = $('input[name="selectRateCode"]:checked');
        if (!selected.length) {
            HolaPms.alert('warning', '레이트 코드를 선택하세요.');
            return;
        }
        this.selectedRateCodeId = parseInt(selected.val());
        var rateCodeName = selected.data('code');
        $('#rateCodeId').val(this.selectedRateCodeId);
        $('#rateCodeName').val(rateCodeName);
        // 모달 닫기 전 포커스를 모달 외부로 이동 (aria-hidden 경고 방지)
        $('#rateCodeName').focus();
        HolaPms.modal.hide('#rateCodeModal');
    }
};

$(document).ready(function() {
    PromotionCodeForm.init();
});
