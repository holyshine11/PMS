/**
 * 예약 결제 정보 관리 모듈
 */
var ReservationPayment = {
    propertyId: null,
    reservationId: null,

    /**
     * 결제 정보 로드
     */
    load: function(propertyId, reservationId) {
        this.propertyId = propertyId;
        this.reservationId = reservationId;
        this.loadPaymentSummary();
    },

    /**
     * 결제 요약 조회 및 바인딩
     */
    loadPaymentSummary: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment',
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    self.bindSummary(res.data);
                    self.renderAdjustments(res.data.adjustments || []);
                }
            },
            error: function() {
                // 결제 정보 미존재 시 기본값 유지
            }
        });
    },

    /**
     * 결제 요약 테이블 바인딩
     */
    bindSummary: function(data) {
        $('#totalRoomAmount').text(this.formatCurrency(data.totalRoomAmount));
        $('#totalServiceAmount').text(this.formatCurrency(data.totalServiceAmount));
        $('#totalServiceChargeAmount').text(this.formatCurrency(data.totalServiceChargeAmount));
        $('#totalAdjustmentAmount').text(this.formatCurrency(data.totalAdjustmentAmount));
        $('#grandTotal').text(this.formatCurrency(data.grandTotal));

        // 결제 상태 배지
        this.renderPaymentStatus(data.paymentStatus);
    },

    /**
     * 결제 상태 배지 렌더링
     */
    renderPaymentStatus: function(status) {
        var $badge = $('#paymentStatusBadge');
        $badge.empty();

        var statusMap = {
            UNPAID: { label: '미결제', cls: 'bg-warning text-dark' },
            PARTIAL: { label: '부분결제', cls: 'bg-info' },
            PAID: { label: '결제완료', cls: 'bg-success' },
            REFUNDED: { label: '환불', cls: 'bg-danger' }
        };

        var info = statusMap[status] || { label: status || '미결제', cls: 'bg-secondary' };
        $badge.html('<span class="badge ' + info.cls + '">' + HolaPms.escapeHtml(info.label) + '</span>');

        // 결제완료 시 결제 버튼 숨김
        if (status === 'PAID') {
            $('#processPaymentBtn').hide();
        }
    },

    /**
     * 결제 처리 (더미)
     */
    processPayment: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment/process',
            type: 'PUT',
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '결제가 처리되었습니다.');
                    if (res.data) {
                        self.bindSummary(res.data);
                        self.renderAdjustments(res.data.adjustments || []);
                    }
                }
            }
        });
    },

    /**
     * 금액 조정 목록 렌더링
     */
    renderAdjustments: function(adjustments) {
        var self = this;
        var $list = $('#adjustmentList');
        $list.empty();

        if (!adjustments || adjustments.length === 0) {
            $list.html('<div class="text-center text-muted py-3 border rounded"><i class="fas fa-calculator me-1"></i>등록된 조정 내역이 없습니다.</div>');
            return;
        }

        var html = '<table class="table table-bordered table-sm">'
            + '<thead class="table-light">'
            + '<tr>'
            + '  <th style="width:50px" class="text-center">NO</th>'
            + '  <th style="width:60px" class="text-center">구분</th>'
            + '  <th class="text-end">공급가</th>'
            + '  <th class="text-end">세금</th>'
            + '  <th class="text-end">합계</th>'
            + '  <th>사유</th>'
            + '  <th>작성자</th>'
            + '  <th>일시</th>'
            + '</tr>'
            + '</thead><tbody>';

        adjustments.forEach(function(adj, idx) {
            var signLabel = adj.adjustmentSign === '+' ? '<span class="text-success">+</span>' : '<span class="text-danger">-</span>';
            var createdAt = adj.createdAt ? adj.createdAt.replace('T', ' ').substring(0, 16) : '';

            html += '<tr>'
                + '<td class="text-center">' + (idx + 1) + '</td>'
                + '<td class="text-center">' + signLabel + '</td>'
                + '<td class="text-end">' + self.formatCurrency(adj.supplyPrice) + '</td>'
                + '<td class="text-end">' + self.formatCurrency(adj.tax) + '</td>'
                + '<td class="text-end">' + self.formatCurrency(adj.totalAmount) + '</td>'
                + '<td>' + HolaPms.escapeHtml(adj.comment || '-') + '</td>'
                + '<td>' + HolaPms.escapeHtml(adj.createdBy || '-') + '</td>'
                + '<td>' + HolaPms.escapeHtml(createdAt) + '</td>'
                + '</tr>';
        });

        html += '</tbody></table>';
        $list.html(html);
    },

    /**
     * 조정 추가 인라인 폼 표시
     */
    showAdjustmentForm: function() {
        var self = this;

        // 이미 표시 중이면 무시
        if ($('#adjustmentFormInline').length > 0) return;

        var formHtml = ''
            + '<div id="adjustmentFormInline" class="border rounded p-3 mt-2 mb-2">'
            + '  <div class="row g-2 align-items-end">'
            + '    <div class="col-md-2">'
            + '      <label class="form-label small">구분</label>'
            + '      <select class="form-select form-select-sm" id="adjSign">'
            + '        <option value="+">증액 (+)</option>'
            + '        <option value="-">감액 (-)</option>'
            + '      </select>'
            + '    </div>'
            + '    <div class="col-md-2">'
            + '      <label class="form-label small">공급가</label>'
            + '      <input type="number" class="form-control form-control-sm" id="adjSupplyPrice" value="0" min="0">'
            + '    </div>'
            + '    <div class="col-md-2">'
            + '      <label class="form-label small">세금</label>'
            + '      <input type="number" class="form-control form-control-sm" id="adjTax" value="0" min="0">'
            + '    </div>'
            + '    <div class="col-md-3">'
            + '      <label class="form-label small">사유</label>'
            + '      <input type="text" class="form-control form-control-sm" id="adjComment" placeholder="조정 사유">'
            + '    </div>'
            + '    <div class="col-md-3">'
            + '      <button class="btn btn-primary btn-sm me-1" id="adjSaveBtn"><i class="fas fa-check me-1"></i>등록</button>'
            + '      <button class="btn btn-secondary btn-sm" id="adjCancelBtn"><i class="fas fa-times me-1"></i>취소</button>'
            + '    </div>'
            + '  </div>'
            + '</div>';

        $('#addAdjustmentBtn').before(formHtml);

        // 등록 버튼
        $('#adjSaveBtn').on('click', function() {
            self.addAdjustment();
        });

        // 취소 버튼
        $('#adjCancelBtn').on('click', function() {
            $('#adjustmentFormInline').remove();
        });
    },

    /**
     * 금액 조정 등록
     */
    addAdjustment: function() {
        var self = this;
        var supplyPrice = parseInt($('#adjSupplyPrice').val()) || 0;
        var tax = parseInt($('#adjTax').val()) || 0;
        var totalAmount = supplyPrice + tax;
        var comment = $.trim($('#adjComment').val());

        if (totalAmount <= 0) {
            HolaPms.alert('warning', '조정 금액을 입력해주세요.');
            return;
        }
        if (!comment) {
            HolaPms.alert('warning', '조정 사유를 입력해주세요.');
            return;
        }

        var data = {
            adjustmentSign: $('#adjSign').val(),
            supplyPrice: supplyPrice,
            tax: tax,
            totalAmount: totalAmount,
            comment: comment
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment/adjustments',
            type: 'POST',
            data: data,
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '금액 조정이 등록되었습니다.');
                    $('#adjustmentFormInline').remove();
                    self.loadPaymentSummary();
                }
            }
        });
    },

    /**
     * 통화 포맷 (콤마 + 원)
     */
    formatCurrency: function(amount) {
        if (amount === null || amount === undefined) return '0원';
        return Number(amount).toLocaleString() + '원';
    }
};
