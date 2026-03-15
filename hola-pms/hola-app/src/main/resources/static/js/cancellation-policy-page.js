/**
 * 취소 정책 관리 페이지
 */
var CancellationPolicy = {
    propertyId: null,

    /** 프리셋 정의 */
    PRESETS: {
        standard: [
            { checkinBasis: 'DATE', daysBefore: 1, feeAmount: 80, feeType: 'PERCENTAGE' },
            { checkinBasis: 'DATE', daysBefore: 3, feeAmount: 50, feeType: 'PERCENTAGE' },
            { checkinBasis: 'DATE', daysBefore: 7, feeAmount: 0, feeType: 'PERCENTAGE' },
            { checkinBasis: 'NOSHOW', daysBefore: null, feeAmount: 100, feeType: 'PERCENTAGE' }
        ],
        strict: [
            { checkinBasis: 'DATE', daysBefore: 3, feeAmount: 100, feeType: 'PERCENTAGE' },
            { checkinBasis: 'DATE', daysBefore: 7, feeAmount: 80, feeType: 'PERCENTAGE' },
            { checkinBasis: 'DATE', daysBefore: 14, feeAmount: 50, feeType: 'PERCENTAGE' },
            { checkinBasis: 'NOSHOW', daysBefore: null, feeAmount: 100, feeType: 'PERCENTAGE' }
        ],
        flexible: [
            { checkinBasis: 'DATE', daysBefore: 1, feeAmount: 50, feeType: 'PERCENTAGE' },
            { checkinBasis: 'DATE', daysBefore: 3, feeAmount: 0, feeType: 'PERCENTAGE' },
            { checkinBasis: 'NOSHOW', daysBefore: null, feeAmount: 100, feeType: 'PERCENTAGE' }
        ]
    },

    /**
     * 초기화
     */
    init: function() {
        this.bindEvents();
        this.reload();
    },

    /**
     * 컨텍스트 기반 새로고침
     */
    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#policyContainer').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#policyContainer').show();
        this.propertyId = propertyId;
        this.loadPolicies();
    },

    /**
     * 이벤트 바인딩
     */
    bindEvents: function() {
        var self = this;

        // 행 추가
        $('#btnAddRow').on('click', function() {
            self.addRow();
            self.updateEmptyMessage();
        });

        // 행 삭제 (이벤트 위임)
        $(document).on('click', '.cancel-remove-btn', function() {
            var rowCount = $('#cancelFeeBody tr').length;
            if (rowCount <= 1) {
                HolaPms.alert('warning', '최소 1행은 유지해야 합니다.');
                return;
            }
            $(this).closest('tr').fadeOut(200, function() {
                $(this).remove();
                self.updateEmptyMessage();
                self.updatePolicySummary();
            });
        });

        // 프리셋 버튼
        $('#btnPresetStandard').on('click', function() { self.applyPreset('standard'); });
        $('#btnPresetStrict').on('click', function() { self.applyPreset('strict'); });
        $('#btnPresetFlexible').on('click', function() { self.applyPreset('flexible'); });

        // 저장 버튼
        $('#btnSavePolicies').on('click', function() { self.save(); });

        // 행 내 입력 변경 시 요약 갱신
        $(document).on('change', '#cancelFeeBody select, #cancelFeeBody input', function() {
            self.updatePolicySummary();
        });

        // 프로퍼티 컨텍스트 변경
        $(document).on('hola:contextChange', function() {
            self.reload();
        });
    },

    /**
     * 행 추가
     */
    addRow: function(data) {
        var basis = (data && data.checkinBasis) || '';
        var days = (data && data.daysBefore != null) ? data.daysBefore : '';
        var amount = (data && data.feeAmount != null) ? data.feeAmount : '';
        var type = (data && data.feeType) || 'PERCENTAGE';

        var daysDisabled = (basis === '' || basis === 'NOSHOW') ? 'disabled' : '';
        var label = (basis === 'NOSHOW') ? '시' : '일 전 취소 시';

        var html = '<tr>' +
            '<td>' +
                '<select class="form-select form-select-sm cancel-basis">' +
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
                '<button type="button" class="btn btn-outline-danger btn-sm cancel-remove-btn">' +
                    '<i class="fas fa-trash"></i>' +
                '</button>' +
            '</td>' +
            '</tr>';

        $('#cancelFeeBody').append(html);

        // 체크인 기준 변경 이벤트
        $('#cancelFeeBody tr:last .cancel-basis').on('change', function() {
            var $row = $(this).closest('tr');
            var val = $(this).val();
            var $days = $row.find('.cancel-days');
            var $label = $row.find('.cancel-label');

            if (val === '' || val === 'NOSHOW') {
                $days.val('').prop('disabled', true);
            } else {
                $days.prop('disabled', false);
            }
            $label.text(val === 'NOSHOW' ? '시' : '일 전 취소 시');
        });
    },

    /**
     * 빈 메시지 토글
     */
    updateEmptyMessage: function() {
        var count = $('#cancelFeeBody tr').length;
        $('#cancelFeeEmpty').toggle(count === 0);
        $('#cancelFeeTable').toggle(count > 0);
    },

    /**
     * 프리셋 적용
     */
    applyPreset: function(presetName) {
        var preset = this.PRESETS[presetName];
        if (!preset) return;

        $('#cancelFeeBody').empty();
        for (var i = 0; i < preset.length; i++) {
            this.addRow(preset[i]);
        }
        this.updateEmptyMessage();
        this.updatePolicySummary();
        HolaPms.alert('info', '프리셋이 적용되었습니다. 저장 버튼을 눌러 반영하세요.');
    },

    /**
     * 정책 요약 갱신
     */
    updatePolicySummary: function() {
        var rows = [];
        $('#cancelFeeBody tr').each(function() {
            var basis = $(this).find('.cancel-basis').val();
            var days = $(this).find('.cancel-days').val();
            var amount = $(this).find('.cancel-amount').val();
            var type = $(this).find('.cancel-type').val();
            if (!basis || amount === '') return;
            rows.push({ basis: basis, days: days ? parseInt(days) : null, amount: parseFloat(amount), type: type });
        });

        if (rows.length === 0) {
            $('#policySummary').hide();
            return;
        }

        var $list = $('#policySummaryList').empty();
        rows.forEach(function(r) {
            var text;
            var unit = r.type === 'PERCENTAGE' ? '%' : (r.type === 'FIXED_KRW' ? '원' : 'USD');
            if (r.basis === 'NOSHOW') {
                text = '노쇼 시: 1박 요금의 ' + r.amount + unit + ' 부과';
            } else if (r.amount === 0) {
                text = '체크인 ' + r.days + '일 전까지: 무료 취소';
            } else {
                text = '체크인 ' + r.days + '일 이내: 1박 요금의 ' + r.amount + unit + ' 부과';
            }
            $list.append('<li>' + text + '</li>');
        });
        $('#policySummary').show();
    },

    /**
     * 정책 조회
     */
    loadPolicies: function() {
        var self = this;
        if (!self.propertyId) return;

        $('#cancelFeeBody').empty();

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/cancellation-fees',
            type: 'GET',
            success: function(res) {
                var list = res.data || [];
                if (list.length === 0) {
                    self.addRow();
                    self.addRow();
                } else {
                    $.each(list, function(i, fee) {
                        self.addRow(fee);
                    });
                }
                self.updateEmptyMessage();
                self.updatePolicySummary();
            },
            error: function(xhr) {
                if (xhr.status === 404) {
                    self.addRow();
                    self.addRow();
                    self.updateEmptyMessage();
                }
            }
        });
    },

    /**
     * 저장
     */
    save: function() {
        var self = this;
        if (!self.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var fees = [];
        var valid = true;

        $('#cancelFeeBody tr').each(function() {
            var basis = $(this).find('.cancel-basis').val();
            if (!basis) return true; // 미선택 행 스킵

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
            url: '/api/v1/properties/' + self.propertyId + '/cancellation-fees',
            type: 'PUT',
            data: { fees: fees },
            success: function() {
                HolaPms.alert('success', '취소 정책이 저장되었습니다.');
                self.loadPolicies();
            }
        });
    }
};

// 초기화
$(function() {
    CancellationPolicy.init();
});
