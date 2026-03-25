/**
 * 예약 결제 정보 관리 모듈
 */
var ReservationPayment = {
    propertyId: null,
    reservationId: null,
    reservationData: null,
    paymentData: null,
    isOta: false,

    /**
     * 결제 정보 로드
     */
    load: function(propertyId, reservationId, reservationData) {
        this.propertyId = propertyId;
        this.reservationId = reservationId;
        this.reservationData = reservationData || null;
        this.isOta = reservationData && reservationData.isOtaManaged === true;
        this.bindToggleEvents();
        this.loadPaymentSummary();
    },

    /**
     * 세부 내역 토글 이벤트
     */
    bindToggleEvents: function() {
        $(document).off('click.chargeToggle').on('click.chargeToggle', '.charge-toggle', function() {
            var target = $(this).data('target');
            var $target = $(target);
            var $icon = $(this).find('.toggle-icon');

            if ($target.is(':visible')) {
                $target.slideUp(200);
                $icon.removeClass('fa-caret-down').addClass('fa-caret-right');
            } else {
                $target.slideDown(200);
                $icon.removeClass('fa-caret-right').addClass('fa-caret-down');
            }
        });

        // 결제 이력 토글
        $(document).off('click.historyToggle').on('click.historyToggle', '#paymentHistoryToggle', function() {
            var $collapse = $('#paymentHistoryCollapse');
            var $icon = $('#paymentHistoryToggleIcon');

            if ($collapse.hasClass('show')) {
                $collapse.removeClass('show');
                $icon.removeClass('fa-caret-up').addClass('fa-caret-down');
            } else {
                $collapse.addClass('show');
                $icon.removeClass('fa-caret-down').addClass('fa-caret-up');
            }
        });
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
                    self.paymentData = res.data;
                    self.bindSummary(res.data);
                    self.renderChargeBreakdown();
                    self.renderAdjustments(res.data.adjustments || []);
                    self.renderPaymentTransactions(res.data.transactions || []);
                }
            },
            error: function() {
                // 결제 정보 미존재 시 세부 내역만 렌더링
                self.renderChargeBreakdown();
            }
        });
    },

    /**
     * 결제 요약 바인딩
     */
    bindSummary: function(data) {
        $('#totalRoomAmount').text(this.formatCurrency(data.totalRoomAmount));
        $('#totalServiceAmount').text(this.formatCurrency(data.totalServiceAmount));
        $('#totalServiceChargeAmount').text(this.formatCurrency(data.totalServiceChargeAmount));
        $('#totalEarlyLateFee').text(this.formatCurrency(data.totalEarlyLateFee));
        $('#totalAdjustmentAmount').text(this.formatCurrency(data.totalAdjustmentAmount));
        $('#grandTotal').text(this.formatCurrency(data.grandTotal));

        // 결제 누적액/잔액 표시
        var grandTotal = Number(data.grandTotal) || 0;
        var totalPaid = Number(data.totalPaidAmount) || 0;
        var remaining = Number(data.remainingAmount) || 0;

        var displayHtml = '총액 <strong>' + this.formatCurrency(grandTotal) + '</strong>';
        if (totalPaid > 0) {
            displayHtml += ' &nbsp;|&nbsp; 결제 <strong>' + this.formatCurrency(totalPaid) + '</strong>';
            if (remaining < 0) {
                displayHtml += ' &nbsp;|&nbsp; <strong class="text-warning">환불필요 ' + this.formatCurrency(Math.abs(remaining)) + '</strong>';
            } else {
                displayHtml += ' &nbsp;|&nbsp; 잔액 <strong class="text-danger">' + this.formatCurrency(remaining) + '</strong>';
            }
        }
        $('#paidAmountDisplay').html(displayHtml);

        // 결제 상태 배지
        this.renderPaymentStatus(data.paymentStatus);

        // 취소/환불 정보 섹션
        this.renderCancelInfo(data);
    },

    /**
     * 요금 세부 내역 렌더링 (reservationData 기반)
     */
    renderChargeBreakdown: function() {
        var self = this;
        var data = self.reservationData;
        if (!data || !data.subReservations) return;

        var subs = data.subReservations;
        var totalSupply = 0, totalTax = 0, totalSvcChg = 0;

        // ── 1. 객실 요금 세부 (일별 공급가 + 세액) ──
        var roomHtml = '';
        subs.forEach(function(sub, idx) {
            if (sub.roomReservationStatus === 'CANCELED') return;
            var charges = sub.dailyCharges || [];
            if (charges.length === 0) return;

            var label = sub.roomTypeName || ('객실 #' + (idx + 1));
            if (subs.length > 1) label = 'Leg #' + (idx + 1) + ' - ' + label;

            roomHtml += '<div class="charge-detail-wrap">';
            if (subs.length > 1) {
                roomHtml += '<div class="text-muted small mb-1">' + HolaPms.escapeHtml(label) + '</div>';
            }
            roomHtml += '<table class="table table-sm charge-detail-table">';
            roomHtml += '<thead><tr>'
                + '<th class="col-date">날짜</th>'
                + '<th class="col-amount">공급가</th>'
                + '<th class="col-amount">세액</th>'
                + '<th class="col-amount">소계</th>'
                + '</tr></thead><tbody>';

            var subSupply = 0, subTax = 0;
            charges.forEach(function(c) {
                var sp = Number(c.supplyPrice) || 0;
                var tx = Number(c.tax) || 0;
                var sc = Number(c.serviceCharge) || 0;
                subSupply += sp;
                subTax += tx;
                totalSvcChg += sc;

                roomHtml += '<tr>'
                    + '<td class="col-date">' + c.chargeDate + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(sp) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(tx) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(sp + tx) + '</td>'
                    + '</tr>';
            });

            // 멀티 레그: 소계
            if (subs.length > 1) {
                roomHtml += '<tr class="row-subtotal">'
                    + '<td class="col-date">소계</td>'
                    + '<td class="col-amount">' + self.formatCurrency(subSupply) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(subTax) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(subSupply + subTax) + '</td>'
                    + '</tr>';
            }

            roomHtml += '</tbody></table></div>';
            totalSupply += subSupply;
            totalTax += subTax;
        });

        $('#roomDetailContent').html(roomHtml || '<div class="charge-empty">객실 요금 내역이 없습니다.</div>');

        // ── 2. 유료 서비스 요금 세부 ──
        var svcHtml = '';
        var paidServices = [];
        subs.forEach(function(sub) {
            if (sub.roomReservationStatus === 'CANCELED') return;
            (sub.services || []).forEach(function(svc) {
                if (Number(svc.totalPrice) > 0) {
                    paidServices.push(svc);
                }
            });
        });

        if (paidServices.length > 0) {
            svcHtml += '<div class="charge-detail-wrap">';
            svcHtml += '<table class="table table-sm charge-detail-table">';
            svcHtml += '<thead><tr>'
                + '<th class="col-label">항목</th>'
                + '<th class="col-qty">수량</th>'
                + '<th class="col-amount">단가</th>'
                + '<th class="col-amount">세액</th>'
                + '<th class="col-amount">합계</th>'
                + '</tr></thead><tbody>';

            paidServices.forEach(function(s) {
                var typeLabel = s.serviceName || (s.serviceOptionId ? '서비스 #' + s.serviceOptionId : '객실 업그레이드');
                if (s.serviceDate) typeLabel += ' (' + s.serviceDate + ')';

                var unitP = Number(s.unitPrice) || 0;
                var qty = s.quantity || 1;
                var sTax = Number(s.tax) || 0;
                var sTotal = Number(s.totalPrice) || 0;
                var sSupply = unitP * qty;

                totalSupply += sSupply;
                totalTax += sTax;

                svcHtml += '<tr>'
                    + '<td class="col-label">' + HolaPms.escapeHtml(typeLabel) + '</td>'
                    + '<td class="col-qty">' + qty + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(unitP) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(sTax) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(sTotal) + '</td>'
                    + '</tr>';
            });

            svcHtml += '</tbody></table></div>';
        }

        $('#serviceDetailContent').html(svcHtml || '<div class="charge-empty">유료 서비스 내역이 없습니다.</div>');

        // ── 3. 봉사료 세부 (일별) ──
        var chgHtml = '';
        subs.forEach(function(sub, idx) {
            if (sub.roomReservationStatus === 'CANCELED') return;
            var charges = sub.dailyCharges || [];
            var hasChg = charges.some(function(c) { return Number(c.serviceCharge) > 0; });
            if (!hasChg) return;

            var label = sub.roomTypeName || ('객실 #' + (idx + 1));
            if (subs.length > 1) label = 'Leg #' + (idx + 1) + ' - ' + label;

            chgHtml += '<div class="charge-detail-wrap">';
            if (subs.length > 1) {
                chgHtml += '<div class="text-muted small mb-1">' + HolaPms.escapeHtml(label) + '</div>';
            }
            chgHtml += '<table class="table table-sm charge-detail-table">';
            chgHtml += '<thead><tr><th class="col-date">날짜</th><th class="col-amount">봉사료</th></tr></thead><tbody>';

            var subSvcChg = 0;
            charges.forEach(function(c) {
                var sc = Number(c.serviceCharge) || 0;
                if (sc > 0) {
                    subSvcChg += sc;
                    chgHtml += '<tr><td class="col-date">' + c.chargeDate + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(sc) + '</td></tr>';
                }
            });

            if (subs.length > 1) {
                chgHtml += '<tr class="row-subtotal"><td class="col-date">소계</td>'
                    + '<td class="col-amount">' + self.formatCurrency(subSvcChg) + '</td></tr>';
            }

            chgHtml += '</tbody></table></div>';
        });

        $('#svcChgDetailContent').html(chgHtml || '<div class="charge-empty">봉사료 내역이 없습니다.</div>');

        // ── 공급가/세액/봉사료 소계 바인딩 ──
        $('#totalSupplyPrice').text(self.formatCurrency(totalSupply));
        $('#totalTaxAmount').text(self.formatCurrency(totalTax));
        $('#totalSvcChgSubtotal').text(self.formatCurrency(totalSvcChg));
    },

    /**
     * 결제 상태 배지 렌더링
     */
    renderPaymentStatus: function(status) {
        var $badge = $('#paymentStatusBadge');
        $badge.empty();

        var statusMap = {
            UNPAID: { label: '미결제', cls: 'bg-danger' },
            PARTIAL: { label: '부분결제', cls: 'bg-info' },
            PAID: { label: '결제완료', cls: 'bg-success' },
            OVERPAID: { label: '초과결제(환불필요)', cls: 'bg-warning text-dark' },
            REFUNDED: { label: '환불완료', cls: 'bg-secondary' }
        };

        var info = statusMap[status] || { label: status || '미결제', cls: 'bg-secondary' };
        $badge.html('<span class="badge ' + info.cls + '">' + HolaPms.escapeHtml(info.label) + '</span>');

        // 잔액이 0 이하이면 결제 버튼 숨김 (PAID, OVERPAID, grandTotal<=0)
        var remaining = this.paymentData ? Number(this.paymentData.remainingAmount) || 0 : 0;
        if (status === 'PAID' || status === 'OVERPAID' || remaining <= 0) {
            $('#paymentButtonGroup').hide();
        } else {
            $('#paymentButtonGroup').show();
        }
    },

    /**
     * 현금결제 모달 열기
     */
    openCashPaymentModal: function() {
        var self = this;

        // 모달 열기 전 최신 결제 정보 재조회
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment',
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    self.paymentData = res.data;
                    self.bindSummary(res.data);
                    self.renderAdjustments(res.data.adjustments || []);
                    self.renderPaymentTransactions(res.data.transactions || []);

                    var grandTotal = Number(res.data.grandTotal) || 0;
                    var totalPaid = Number(res.data.totalPaidAmount) || 0;
                    var remaining = Number(res.data.remainingAmount) || 0;

                    $('#cashGrandTotalDisplay').val(self.formatCurrency(grandTotal));
                    $('#cashPaidDisplay').val(self.formatCurrency(totalPaid));
                    $('#cashRemainingDisplay').val(self.formatCurrency(remaining));
                    $('#cashPaymentAmount').val(remaining > 0 ? Math.floor(remaining) : '');
                    $('#cashPaymentMemo').val('');

                    HolaPms.modal.show('#cashPaymentModal');
                }
            }
        });
    },

    /**
     * 현금 결제 처리
     */
    processCashPayment: function() {
        var self = this;
        var amount = parseInt($('#cashPaymentAmount').val());
        var memo = $.trim($('#cashPaymentMemo').val());

        if (!amount || amount <= 0) {
            HolaPms.alert('warning', '결제 금액을 입력해주세요.');
            return;
        }

        var requestData = {
            paymentMethod: 'CASH',
            amount: amount,
            memo: memo || null
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment/transactions',
            type: 'POST',
            data: requestData,
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '현금 결제가 처리되었습니다.');
                    HolaPms.modal.hide('#cashPaymentModal');
                    if (res.data) {
                        self.paymentData = res.data;
                        self.bindSummary(res.data);
                        self.renderAdjustments(res.data.adjustments || []);
                        self.renderPaymentTransactions(res.data.transactions || []);
                    }
                }
            }
        });
    },

    /**
     * 결제 거래 이력 렌더링
     */
    renderPaymentTransactions: function(transactions) {
        var self = this;
        var $content = $('#paymentHistoryContent');
        var $collapse = $('#paymentHistoryCollapse');
        var $icon = $('#paymentHistoryToggleIcon');

        if (!transactions || transactions.length === 0) {
            $content.html('<p class="text-center text-muted py-3">결제 이력이 없습니다.</p>');
            $collapse.removeClass('show');
            $icon.removeClass('fa-caret-up').addClass('fa-caret-down');
            return;
        }

        // 이력 있으면 기본 펼침
        $collapse.addClass('show');
        $icon.removeClass('fa-caret-down').addClass('fa-caret-up');

        var methodLabels = {
            CARD: '카드',
            CASH: '현금'
        };

        var typeLabels = {
            PAYMENT: '결제',
            REFUND: '환불',
            CANCEL_FEE: '취소수수료'
        };

        var typeStyles = {
            REFUND: 'text-primary',
            CANCEL_FEE: 'text-danger'
        };

        // PG 결제 정보가 하나라도 있는지 확인
        var hasPgInfo = transactions.some(function(t) { return t.pgProvider && t.pgProvider !== 'MOCK'; });

        var html = '<table class="table table-bordered table-sm mb-0 align-middle">'
            + '<thead class="table-light">'
            + '<tr>'
            + '  <th style="width:50px" class="text-center">NO</th>'
            + '  <th style="width:80px" class="text-center">유형</th>'
            + '  <th style="width:80px" class="text-center">결제수단</th>'
            + '  <th style="width:120px" class="text-end">금액</th>';
        if (hasPgInfo) {
            html += '  <th style="width:100px" class="text-center">PG승인번호</th>'
                + '  <th style="width:100px" class="text-center">카드사</th>'
                + '  <th style="width:60px" class="text-center">할부</th>';
        }
        html += '  <th class="text-center">메모</th>'
            + '  <th style="width:80px" class="text-center">처리자</th>'
            + '  <th style="width:180px" class="text-center">처리일시</th>'
            + '</tr>'
            + '</thead><tbody>';

        transactions.forEach(function(txn, idx) {
            var methodLabel = methodLabels[txn.paymentMethod] || HolaPms.escapeHtml(txn.paymentMethod);
            var createdAt = txn.createdAt ? txn.createdAt.replace('T', ' ').substring(0, 19) : '-';
            var typeLabel = typeLabels[txn.transactionType] || HolaPms.escapeHtml(txn.transactionType || '결제');
            var typeStyle = typeStyles[txn.transactionType] || '';

            html += '<tr>'
                + '<td class="text-center">' + (idx + 1) + '</td>'
                + '<td class="text-center ' + typeStyle + '">' + typeLabel + '</td>'
                + '<td class="text-center">' + methodLabel + '</td>'
                + '<td class="text-end">' + self.formatCurrency(txn.amount) + '</td>';
            if (hasPgInfo) {
                var installment = txn.pgInstallmentMonth ? (txn.pgInstallmentMonth === 0 ? '일시불' : txn.pgInstallmentMonth + '개월') : '-';
                html += '<td class="text-center">' + HolaPms.escapeHtml(txn.pgApprovalNo || txn.approvalNo || '-') + '</td>'
                    + '<td class="text-center">' + HolaPms.escapeHtml(txn.pgIssuerName || '-') + '</td>'
                    + '<td class="text-center">' + installment + '</td>';
            }
            html += '<td class="text-center">' + HolaPms.escapeHtml(txn.memo || '-') + '</td>'
                + '<td class="text-center">' + HolaPms.escapeHtml(txn.createdBy || '-') + '</td>'
                + '<td class="text-center text-nowrap">' + HolaPms.escapeHtml(createdAt) + '</td>'
                + '</tr>';
        });

        html += '</tbody></table>';
        $content.html(html);
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

        var html = '<table class="table table-bordered table-sm align-middle">'
            + '<thead class="table-light">'
            + '<tr>'
            + '  <th style="width:50px" class="text-center">NO</th>'
            + '  <th style="width:60px" class="text-center">구분</th>'
            + '  <th style="width:120px" class="text-end">공급가</th>'
            + '  <th style="width:120px" class="text-end">세금</th>'
            + '  <th style="width:120px" class="text-end">합계</th>'
            + '  <th class="text-center">사유</th>'
            + '  <th style="width:80px" class="text-center">작성자</th>'
            + '  <th style="width:180px" class="text-center">일시</th>'
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
                + '<td class="text-center">' + HolaPms.escapeHtml(adj.comment || '-') + '</td>'
                + '<td class="text-center">' + HolaPms.escapeHtml(adj.createdBy || '-') + '</td>'
                + '<td class="text-center text-nowrap">' + HolaPms.escapeHtml(createdAt) + '</td>'
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
     * 취소/환불 정보 섹션 렌더링
     */
    renderCancelInfo: function(data) {
        var cancelFee = Number(data.cancelFeeAmount) || 0;
        var refund = Number(data.refundAmount) || 0;

        if (cancelFee <= 0 && refund <= 0) {
            $('#cancelInfoSection').hide();
            return;
        }

        var totalPaid = Number(data.totalPaidAmount) || 0;

        // REFUND 거래의 memo에서 정책 설명 추출
        var policyDesc = '-';
        var transactions = data.transactions || [];
        for (var i = 0; i < transactions.length; i++) {
            var txn = transactions[i];
            if (txn.transactionType === 'REFUND' && txn.memo) {
                policyDesc = txn.memo;
                break;
            }
        }

        $('#cancelPolicyDesc').text(policyDesc);
        $('#cancelFeeDisplay').text(this.formatCurrency(cancelFee));
        $('#cancelTotalPaid').text(this.formatCurrency(totalPaid));
        $('#cancelRefundAmt').text(this.formatCurrency(refund));
        $('#cancelInfoSection').show();
    },

    /**
     * 통화 포맷 (콤마 + 원)
     */
    formatCurrency: function(amount) {
        if (amount === null || amount === undefined) return '0원';
        return Number(amount).toLocaleString() + '원';
    },

    /**
     * 인보이스 출력 (새 창)
     */
    printInvoice: function() {
        var self = this;
        var resData = self.reservationData;
        var payData = self.paymentData;

        if (!resData) {
            HolaPms.alert('warning', '예약 정보를 먼저 로드해주세요.');
            return;
        }

        // 프로퍼티 정보 조회 후 인보이스 생성
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId,
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    self.generateInvoice(res.data, resData, payData);
                }
            },
            error: function() {
                // 프로퍼티 정보 없어도 인보이스 출력 시도
                self.generateInvoice(null, resData, payData);
            }
        });
    },

    /**
     * 인보이스 HTML 생성 + 새 창 열기
     */
    generateInvoice: function(property, resData, payData) {
        var self = this;
        var fmt = function(amt) {
            if (amt === null || amt === undefined) return '0';
            return Number(amt).toLocaleString();
        };

        // 프로퍼티 정보
        var hotelName = (property && property.propertyName) || 'Hotel';
        var hotelAddr = '';
        if (property) {
            hotelAddr = (property.address || '') + (property.addressDetail ? ' ' + property.addressDetail : '');
        }
        var hotelPhone = (property && property.phone) || '';
        var hotelEmail = (property && property.email) || '';
        var bizNumber = (property && property.businessNumber) || '';
        var logoPath = (property && property.logoPath) || '';
        var repName = (property && property.representativeName) || '';

        // 예약 정보
        var reservationNo = resData.masterReservationNo || '-';
        var confirmNo = resData.confirmationNo || '-';
        var guestName = resData.guestNameKo || '-';
        var guestEmail = resData.email || '';
        var guestPhone = '';
        if (resData.phoneCountryCode && resData.phoneNumber) {
            guestPhone = resData.phoneCountryCode + ' ' + resData.phoneNumber;
        } else if (resData.phoneNumber) {
            guestPhone = resData.phoneNumber;
        }
        var checkIn = resData.masterCheckIn || '-';
        var checkOut = resData.masterCheckOut || '-';

        // 숙박일수
        var nights = 0;
        if (resData.masterCheckIn && resData.masterCheckOut) {
            var d1 = new Date(resData.masterCheckIn);
            var d2 = new Date(resData.masterCheckOut);
            nights = Math.round((d2 - d1) / (1000 * 60 * 60 * 24));
        }

        // 객실 정보
        var subs = resData.subReservations || [];
        var roomTypeNames = [];
        subs.forEach(function(sub) {
            if (sub.roomReservationStatus !== 'CANCELED' && sub.roomTypeName) {
                roomTypeNames.push(sub.roomTypeName);
            }
        });

        // 발행일
        var today = new Date();
        var issueDate = today.getFullYear() + '-'
            + String(today.getMonth() + 1).padStart(2, '0') + '-'
            + String(today.getDate()).padStart(2, '0');

        // ── 요금 명세 상세 테이블 ──
        var chargeRows = '';
        var totalSupply = 0, totalTax = 0, totalSvcChg = 0;

        // 1. 객실 요금 (일별)
        subs.forEach(function(sub, idx) {
            if (sub.roomReservationStatus === 'CANCELED') return;
            var charges = sub.dailyCharges || [];
            if (charges.length === 0) return;

            var label = sub.roomTypeName || ('객실 #' + (idx + 1));
            if (subs.length > 1) label = 'Leg #' + (idx + 1) + ' - ' + label;

            chargeRows += '<tr class="section-header"><td colspan="4">' + HolaPms.escapeHtml(label) + '</td></tr>';

            charges.forEach(function(c) {
                var sp = Number(c.supplyPrice) || 0;
                var tx = Number(c.tax) || 0;
                var sc = Number(c.serviceCharge) || 0;
                totalSupply += sp;
                totalTax += tx;
                totalSvcChg += sc;

                chargeRows += '<tr>'
                    + '<td class="ps-4">' + c.chargeDate + '</td>'
                    + '<td class="text-end">' + fmt(sp) + '</td>'
                    + '<td class="text-end">' + fmt(tx) + '</td>'
                    + '<td class="text-end">' + fmt(sp + tx) + '</td>'
                    + '</tr>';
            });
        });

        // 2. 유료 서비스
        var paidServices = [];
        subs.forEach(function(sub) {
            if (sub.roomReservationStatus === 'CANCELED') return;
            (sub.services || []).forEach(function(svc) {
                if (Number(svc.totalPrice) > 0) paidServices.push(svc);
            });
        });

        if (paidServices.length > 0) {
            chargeRows += '<tr class="section-header"><td colspan="4">유료 서비스</td></tr>';
            paidServices.forEach(function(s) {
                var sLabel = s.serviceName || (s.serviceOptionId ? '서비스 #' + s.serviceOptionId : '객실 업그레이드');
                if (s.serviceDate) sLabel += ' (' + s.serviceDate + ')';
                var unitP = Number(s.unitPrice) || 0;
                var qty = s.quantity || 1;
                var sTax = Number(s.tax) || 0;
                var sTotal = Number(s.totalPrice) || 0;
                totalSupply += (unitP * qty);
                totalTax += sTax;

                chargeRows += '<tr>'
                    + '<td class="ps-4">' + HolaPms.escapeHtml(sLabel) + ' x' + qty + '</td>'
                    + '<td class="text-end">' + fmt(unitP * qty) + '</td>'
                    + '<td class="text-end">' + fmt(sTax) + '</td>'
                    + '<td class="text-end">' + fmt(sTotal) + '</td>'
                    + '</tr>';
            });
        }

        // 3. 봉사료
        if (totalSvcChg > 0) {
            chargeRows += '<tr class="section-header"><td colspan="4">봉사료</td></tr>';
            chargeRows += '<tr>'
                + '<td class="ps-4">봉사료 합계</td>'
                + '<td class="text-end" colspan="2"></td>'
                + '<td class="text-end">' + fmt(totalSvcChg) + '</td>'
                + '</tr>';
        }

        // 4. 얼리체크인/레이트체크아웃
        var earlyLateFee = payData ? Number(payData.totalEarlyLateFee) || 0 : 0;
        if (earlyLateFee > 0) {
            chargeRows += '<tr>'
                + '<td>얼리체크인/레이트체크아웃</td>'
                + '<td class="text-end" colspan="2"></td>'
                + '<td class="text-end">' + fmt(earlyLateFee) + '</td>'
                + '</tr>';
        }

        // 5. 조정 금액
        var adjAmount = payData ? Number(payData.totalAdjustmentAmount) || 0 : 0;
        if (adjAmount !== 0) {
            chargeRows += '<tr>'
                + '<td>금액 조정</td>'
                + '<td class="text-end" colspan="2"></td>'
                + '<td class="text-end">' + fmt(adjAmount) + '</td>'
                + '</tr>';
        }

        // 합계 정보
        var grandTotal = payData ? Number(payData.grandTotal) || 0 : 0;
        var totalPaid = payData ? Number(payData.totalPaidAmount) || 0 : 0;
        var remaining = payData ? Number(payData.remainingAmount) || 0 : 0;

        // ── 결제 이력 테이블 ──
        var txnRows = '';
        var transactions = (payData && payData.transactions) || [];
        var methodLabels = { CARD: '카드', CASH: '현금' };
        var typeLabels = { PAYMENT: '결제', REFUND: '환불', CANCEL_FEE: '취소수수료' };

        if (transactions.length > 0) {
            transactions.forEach(function(txn, idx) {
                var createdAt = txn.createdAt ? txn.createdAt.replace('T', ' ').substring(0, 16) : '-';
                txnRows += '<tr>'
                    + '<td class="text-center">' + (idx + 1) + '</td>'
                    + '<td class="text-center">' + (typeLabels[txn.transactionType] || txn.transactionType) + '</td>'
                    + '<td class="text-center">' + (methodLabels[txn.paymentMethod] || txn.paymentMethod) + '</td>'
                    + '<td class="text-end">' + fmt(txn.amount) + '원</td>'
                    + '<td class="text-center">' + HolaPms.escapeHtml(createdAt) + '</td>'
                    + '</tr>';
            });
        }

        // 로고 이미지 (절대 경로)
        var logoImg = '';
        if (logoPath) {
            logoImg = '<img src="' + window.location.origin + logoPath + '" alt="Hotel Logo" style="max-height:60px; max-width:200px; object-fit:contain;">';
        }

        // ── 인보이스 HTML ──
        var html = '<!DOCTYPE html>'
            + '<html lang="ko"><head><meta charset="UTF-8">'
            + '<title>Invoice - ' + HolaPms.escapeHtml(reservationNo) + '</title>'
            + '<style>'
            + '  @import url("https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.8/dist/web/static/pretendard.css");'
            + '  * { margin: 0; padding: 0; box-sizing: border-box; }'
            + '  body { font-family: "Pretendard", -apple-system, sans-serif; font-size: 12px; color: #333; padding: 40px; line-height: 1.5; }'
            + '  .invoice-container { max-width: 800px; margin: 0 auto; }'

            // 헤더
            + '  .invoice-header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 3px solid #051923; padding-bottom: 20px; margin-bottom: 24px; }'
            + '  .hotel-info { flex: 1; }'
            + '  .hotel-name { font-size: 22px; font-weight: 700; color: #051923; margin-bottom: 4px; }'
            + '  .hotel-detail { font-size: 11px; color: #666; line-height: 1.6; }'
            + '  .invoice-title-area { text-align: right; }'
            + '  .invoice-title { font-size: 28px; font-weight: 800; color: #003554; letter-spacing: 2px; }'
            + '  .invoice-logo { margin-bottom: 8px; text-align: right; }'

            // 정보 그리드
            + '  .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }'
            + '  .info-box { background: #f8f9fa; border-radius: 6px; padding: 16px; }'
            + '  .info-box-title { font-size: 11px; font-weight: 700; color: #003554; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; border-bottom: 1px solid #dee2e6; padding-bottom: 6px; }'
            + '  .info-row { display: flex; justify-content: space-between; margin-bottom: 3px; }'
            + '  .info-label { color: #888; font-size: 11px; }'
            + '  .info-value { font-weight: 500; font-size: 11px; }'

            // 테이블
            + '  .charge-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }'
            + '  .charge-table th { background: #051923; color: #fff; padding: 8px 12px; font-size: 11px; font-weight: 600; }'
            + '  .charge-table td { padding: 6px 12px; border-bottom: 1px solid #eee; font-size: 11px; }'
            + '  .charge-table .section-header td { background: #f0f4f8; font-weight: 600; color: #003554; font-size: 11px; padding: 6px 12px; }'
            + '  .charge-table .ps-4 { padding-left: 28px; }'
            + '  .text-end { text-align: right; }'
            + '  .text-center { text-align: center; }'

            // 합계 영역
            + '  .summary-area { display: flex; justify-content: flex-end; margin-bottom: 24px; }'
            + '  .summary-box { width: 320px; }'
            + '  .summary-row { display: flex; justify-content: space-between; padding: 5px 0; font-size: 12px; }'
            + '  .summary-row.subtotal { border-top: 1px solid #dee2e6; margin-top: 4px; padding-top: 8px; }'
            + '  .summary-row.grand-total { background: #051923; color: #fff; padding: 10px 16px; border-radius: 4px; font-size: 14px; font-weight: 700; margin-top: 8px; }'
            + '  .summary-row.paid { color: #0582CA; font-weight: 600; }'
            + '  .summary-row.balance { color: #EF476F; font-weight: 700; font-size: 13px; }'

            // 결제 이력
            + '  .section-title { font-size: 13px; font-weight: 700; color: #003554; margin: 20px 0 10px; padding-bottom: 6px; border-bottom: 2px solid #003554; }'
            + '  .txn-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }'
            + '  .txn-table th { background: #f0f4f8; padding: 6px 10px; font-size: 11px; font-weight: 600; color: #003554; border-bottom: 1px solid #dee2e6; }'
            + '  .txn-table td { padding: 6px 10px; font-size: 11px; border-bottom: 1px solid #eee; }'

            // 서명
            + '  .signature-area { margin-top: 40px; display: flex; justify-content: space-between; }'
            + '  .sig-box { width: 45%; text-align: center; }'
            + '  .sig-line { border-top: 1px solid #333; margin-top: 60px; padding-top: 8px; font-size: 11px; color: #666; }'

            // 푸터
            + '  .invoice-footer { margin-top: 40px; text-align: center; font-size: 10px; color: #aaa; border-top: 1px solid #eee; padding-top: 16px; }'

            // 프린트
            + '  @media print { body { padding: 20px; } .no-print { display: none !important; } @page { margin: 15mm; } }'
            + '  .print-btn { position: fixed; top: 20px; right: 20px; padding: 10px 24px; background: #003554; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; font-family: "Pretendard", sans-serif; }'
            + '  .print-btn:hover { background: #051923; }'
            + '</style>'
            + '</head><body>'
            + '<button class="print-btn no-print" onclick="window.print()"><i class="fas"></i>프린트</button>'
            + '<div class="invoice-container">'

            // 헤더
            + '<div class="invoice-header">'
            + '  <div class="hotel-info">'
            + '    <div class="hotel-name">' + HolaPms.escapeHtml(hotelName) + '</div>'
            + '    <div class="hotel-detail">'
            + (hotelAddr ? HolaPms.escapeHtml(hotelAddr) + '<br>' : '')
            + (hotelPhone ? 'TEL: ' + HolaPms.escapeHtml(hotelPhone) : '')
            + (hotelEmail ? ' &nbsp;|&nbsp; EMAIL: ' + HolaPms.escapeHtml(hotelEmail) : '')
            + (bizNumber ? '<br>사업자등록번호: ' + HolaPms.escapeHtml(bizNumber) : '')
            + (repName ? ' &nbsp;|&nbsp; 대표: ' + HolaPms.escapeHtml(repName) : '')
            + '    </div>'
            + '  </div>'
            + '  <div class="invoice-title-area">'
            + (logoImg ? '<div class="invoice-logo">' + logoImg + '</div>' : '')
            + '    <div class="invoice-title">INVOICE</div>'
            + '  </div>'
            + '</div>'

            // 정보 그리드
            + '<div class="info-grid">'
            + '  <div class="info-box">'
            + '    <div class="info-box-title">Guest Information</div>'
            + '    <div class="info-row"><span class="info-label">성명</span><span class="info-value">' + HolaPms.escapeHtml(guestName) + '</span></div>'
            + (guestPhone ? '<div class="info-row"><span class="info-label">연락처</span><span class="info-value">' + HolaPms.escapeHtml(guestPhone) + '</span></div>' : '')
            + (guestEmail ? '<div class="info-row"><span class="info-label">이메일</span><span class="info-value">' + HolaPms.escapeHtml(guestEmail) + '</span></div>' : '')
            + '  </div>'
            + '  <div class="info-box">'
            + '    <div class="info-box-title">Reservation Details</div>'
            + '    <div class="info-row"><span class="info-label">예약번호</span><span class="info-value">' + HolaPms.escapeHtml(reservationNo) + '</span></div>'
            + '    <div class="info-row"><span class="info-label">확인번호</span><span class="info-value">' + HolaPms.escapeHtml(confirmNo) + '</span></div>'
            + '    <div class="info-row"><span class="info-label">체크인</span><span class="info-value">' + checkIn + '</span></div>'
            + '    <div class="info-row"><span class="info-label">체크아웃</span><span class="info-value">' + checkOut + '</span></div>'
            + '    <div class="info-row"><span class="info-label">숙박일수</span><span class="info-value">' + nights + '박</span></div>'
            + '    <div class="info-row"><span class="info-label">객실타입</span><span class="info-value">' + HolaPms.escapeHtml(roomTypeNames.join(', ') || '-') + '</span></div>'
            + '    <div class="info-row"><span class="info-label">발행일</span><span class="info-value">' + issueDate + '</span></div>'
            + '  </div>'
            + '</div>'

            // 요금 명세 테이블
            + '<div class="section-title">Charge Details / 요금 명세</div>'
            + '<table class="charge-table">'
            + '<thead><tr><th style="text-align:left">항목</th><th style="text-align:right">공급가</th><th style="text-align:right">세액</th><th style="text-align:right">소계</th></tr></thead>'
            + '<tbody>'
            + chargeRows
            + '</tbody></table>'

            // 합계 영역
            + '<div class="summary-area">'
            + '  <div class="summary-box">'
            + '    <div class="summary-row subtotal"><span>공급가 합계</span><span>' + fmt(totalSupply) + '원</span></div>'
            + '    <div class="summary-row"><span>세액 합계</span><span>' + fmt(totalTax) + '원</span></div>'
            + (totalSvcChg > 0 ? '<div class="summary-row"><span>봉사료 합계</span><span>' + fmt(totalSvcChg) + '원</span></div>' : '')
            + '    <div class="summary-row grand-total"><span>총 청구 금액</span><span>' + fmt(grandTotal) + '원</span></div>'
            + (totalPaid > 0 ? '<div class="summary-row paid"><span>결제 완료</span><span>' + fmt(totalPaid) + '원</span></div>' : '')
            + (remaining > 0 ? '<div class="summary-row balance"><span>잔액</span><span>' + fmt(remaining) + '원</span></div>' : '')
            + '  </div>'
            + '</div>';

        // 결제 이력
        if (txnRows) {
            html += '<div class="section-title">Payment History / 결제 이력</div>'
                + '<table class="txn-table">'
                + '<thead><tr><th>NO</th><th>유형</th><th>수단</th><th style="text-align:right">금액</th><th>일시</th></tr></thead>'
                + '<tbody>' + txnRows + '</tbody></table>';
        }

        // 서명 영역
        html += '<div class="signature-area">'
            + '  <div class="sig-box">'
            + '    <div class="sig-line">Guest Signature / 투숙객 서명</div>'
            + '  </div>'
            + '  <div class="sig-box">'
            + '    <div class="sig-line">Hotel Authorized / 호텔 담당자</div>'
            + '  </div>'
            + '</div>';

        // 푸터
        html += '<div class="invoice-footer">'
            + HolaPms.escapeHtml(hotelName)
            + (hotelAddr ? ' | ' + HolaPms.escapeHtml(hotelAddr) : '')
            + (hotelPhone ? ' | TEL ' + HolaPms.escapeHtml(hotelPhone) : '')
            + '<br>Thank you for staying with us.'
            + '</div>';

        html += '</div></body></html>';

        // 새 창 열기
        var win = window.open('', '_blank', 'width=900,height=700');
        if (win) {
            win.document.write(html);
            win.document.close();
        } else {
            HolaPms.alert('warning', '팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요.');
        }
    },

};
