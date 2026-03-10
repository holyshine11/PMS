/**
 * 얼리 체크인 / 레이트 체크아웃 정책 관리 페이지
 */
var EarlyLatePolicy = {
    propertyId: null,
    earlySeq: 0,    // 얼리 체크인 행 시퀀스
    lateSeq: 0,     // 레이트 체크아웃 행 시퀀스

    // 시간 옵션 생성 (06:00 ~ 23:00, 1시간 단위)
    TIME_OPTIONS: (function() {
        var opts = [];
        for (var h = 6; h <= 23; h++) {
            var hh = (h < 10 ? '0' : '') + h;
            opts.push(hh + ':00');
        }
        return opts;
    })(),

    /**
     * 초기화
     */
    init: function() {
        var self = this;
        self.bindEvents();
        self.reload();
    },

    /**
     * 컨텍스트 기반 새로고침
     */
    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#policyContainer').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#policyContainer').show();
        self.propertyId = propertyId;
        self.loadPolicies();
    },

    /**
     * 이벤트 바인딩
     */
    bindEvents: function() {
        var self = this;

        // 얼리 체크인 행 추가
        $('#btnAddEarlyRow').on('click', function() {
            self.addRow('EARLY_CHECKIN');
        });

        // 레이트 체크아웃 행 추가
        $('#btnAddLateRow').on('click', function() {
            self.addRow('LATE_CHECKOUT');
        });

        // 행 삭제 (이벤트 위임)
        $(document).on('click', '.policy-remove-btn', function() {
            $(this).closest('tr').fadeOut(200, function() {
                $(this).remove();
                self.updateRowNumbers('EARLY_CHECKIN');
                self.updateRowNumbers('LATE_CHECKOUT');
                self.updateEmptyMessage();
            });
        });

        // 저장 버튼
        $('#btnSavePolicies').on('click', function() {
            self.save();
        });

        // 프로퍼티 컨텍스트 변경
        $(document).on('hola:contextChange', function() {
            self.reload();
        });
    },

    /**
     * 시간 select 옵션 HTML 생성
     */
    buildTimeOptions: function(selectedValue) {
        var html = '<option value="">선택</option>';
        this.TIME_OPTIONS.forEach(function(t) {
            html += '<option value="' + t + '"' + (t === selectedValue ? ' selected' : '') + '>' + t + '</option>';
        });
        return html;
    },

    /**
     * 요금유형 select 옵션 HTML 생성
     */
    buildFeeTypeOptions: function(selectedValue) {
        return '<option value="PERCENT"' + (selectedValue === 'PERCENT' ? ' selected' : '') + '>비율 (%)</option>'
             + '<option value="FIXED"' + (selectedValue === 'FIXED' ? ' selected' : '') + '>고정금액 (원)</option>';
    },

    /**
     * 행 추가
     */
    addRow: function(policyType, data) {
        var self = this;
        var isEarly = (policyType === 'EARLY_CHECKIN');
        var seq = isEarly ? ++self.earlySeq : ++self.lateSeq;
        var tbodyId = isEarly ? '#earlyCheckinBody' : '#lateCheckoutBody';

        // 기본값
        var timeFrom = (data && data.timeFrom) || '';
        var timeTo = (data && data.timeTo) || '';
        var feeType = (data && data.feeType) || 'PERCENT';
        var feeValue = (data && data.feeValue != null) ? data.feeValue : '';
        var description = (data && data.description) || '';
        var policyId = (data && data.id) || '';

        var rowHtml = ''
            + '<tr class="policy-row" data-policy-type="' + policyType + '" data-policy-id="' + policyId + '">'
            + '  <td class="text-center row-number">' + seq + '</td>'
            + '  <td>'
            + '    <select class="form-select form-select-sm time-from">'
            +        self.buildTimeOptions(timeFrom)
            + '    </select>'
            + '  </td>'
            + '  <td>'
            + '    <select class="form-select form-select-sm time-to">'
            +        self.buildTimeOptions(timeTo)
            + '    </select>'
            + '  </td>'
            + '  <td>'
            + '    <select class="form-select form-select-sm fee-type">'
            +        self.buildFeeTypeOptions(feeType)
            + '    </select>'
            + '  </td>'
            + '  <td>'
            + '    <input type="number" class="form-control form-control-sm fee-value" '
            + '           value="' + feeValue + '" min="0" step="0.01" placeholder="' + (feeType === 'PERCENT' ? '0~100' : '금액') + '">'
            + '  </td>'
            + '  <td>'
            + '    <input type="text" class="form-control form-control-sm policy-description" '
            + '           value="' + HolaPms.escapeHtml(description) + '" maxlength="200" placeholder="설명 입력">'
            + '  </td>'
            + '  <td class="text-center">'
            + '    <button type="button" class="btn btn-outline-danger btn-sm policy-remove-btn">'
            + '      <i class="fas fa-trash"></i>'
            + '    </button>'
            + '  </td>'
            + '</tr>';

        $(tbodyId).append(rowHtml);
        self.updateEmptyMessage();
    },

    /**
     * 행 번호 갱신
     */
    updateRowNumbers: function(policyType) {
        var tbodyId = (policyType === 'EARLY_CHECKIN') ? '#earlyCheckinBody' : '#lateCheckoutBody';
        $(tbodyId + ' tr.policy-row').each(function(idx) {
            $(this).find('.row-number').text(idx + 1);
        });

        // 시퀀스 동기화
        if (policyType === 'EARLY_CHECKIN') {
            this.earlySeq = $('#earlyCheckinBody tr.policy-row').length;
        } else {
            this.lateSeq = $('#lateCheckoutBody tr.policy-row').length;
        }
    },

    /**
     * 빈 메시지 토글
     */
    updateEmptyMessage: function() {
        var earlyCount = $('#earlyCheckinBody tr.policy-row').length;
        var lateCount = $('#lateCheckoutBody tr.policy-row').length;

        $('#earlyCheckinEmpty').toggle(earlyCount === 0);
        $('#earlyCheckinTable').toggle(earlyCount > 0);
        $('#lateCheckoutEmpty').toggle(lateCount === 0);
        $('#lateCheckoutTable').toggle(lateCount > 0);
    },

    /**
     * 정책 조회
     */
    loadPolicies: function() {
        var self = this;
        if (!self.propertyId) return;

        // 기존 행 초기화
        $('#earlyCheckinBody').empty();
        $('#lateCheckoutBody').empty();
        self.earlySeq = 0;
        self.lateSeq = 0;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/early-late-policies',
            type: 'GET',
            success: function(res) {
                var policies = res.data || [];
                policies.forEach(function(p) {
                    self.addRow(p.policyType, p);
                });
                self.updateEmptyMessage();
            },
            error: function(xhr) {
                // API 미구현 시 빈 상태로 표시
                if (xhr.status === 404) {
                    self.updateEmptyMessage();
                } else {
                    HolaPms.handleAjaxError(xhr);
                }
            }
        });
    },

    /**
     * 폼 데이터 수집
     */
    collectData: function() {
        var policies = [];

        $('.policy-row').each(function(idx) {
            var $row = $(this);
            var timeFrom = $row.find('.time-from').val();
            var timeTo = $row.find('.time-to').val();
            var feeType = $row.find('.fee-type').val();
            var feeValue = $row.find('.fee-value').val();
            var description = $.trim($row.find('.policy-description').val());
            var policyType = $row.data('policy-type');
            var policyId = $row.data('policy-id');

            policies.push({
                id: policyId || null,
                policyType: policyType,
                timeFrom: timeFrom || null,
                timeTo: timeTo || null,
                feeType: feeType,
                feeValue: feeValue ? parseFloat(feeValue) : null,
                description: description || null,
                sortOrder: idx
            });
        });

        return policies;
    },

    /**
     * 유효성 검증
     */
    validate: function(policies) {
        for (var i = 0; i < policies.length; i++) {
            var p = policies[i];
            var label = (p.policyType === 'EARLY_CHECKIN' ? '얼리 체크인' : '레이트 체크아웃');

            if (!p.timeFrom) {
                HolaPms.alert('warning', label + ' ' + (i + 1) + '번째 행: 시작시간을 선택해주세요.');
                return false;
            }
            if (!p.timeTo) {
                HolaPms.alert('warning', label + ' ' + (i + 1) + '번째 행: 종료시간을 선택해주세요.');
                return false;
            }
            if (p.timeFrom >= p.timeTo) {
                HolaPms.alert('warning', label + ' ' + (i + 1) + '번째 행: 종료시간은 시작시간 이후여야 합니다.');
                return false;
            }
            if (p.feeValue == null || p.feeValue < 0) {
                HolaPms.alert('warning', label + ' ' + (i + 1) + '번째 행: 요금값을 입력해주세요.');
                return false;
            }
            if (p.feeType === 'PERCENT' && p.feeValue > 100) {
                HolaPms.alert('warning', label + ' ' + (i + 1) + '번째 행: 비율은 100%를 초과할 수 없습니다.');
                return false;
            }
        }
        return true;
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

        var policies = self.collectData();
        if (!self.validate(policies)) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/early-late-policies',
            type: 'POST',
            data: { policies: policies },
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '정책이 저장되었습니다.');
                    self.loadPolicies();
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    }
};

// 초기화
$(function() {
    EarlyLatePolicy.init();
});
