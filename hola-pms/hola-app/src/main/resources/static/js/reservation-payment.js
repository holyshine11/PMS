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
        // 요금 명세 공통 영역 바인딩
        $('#totalEarlyLateFee').text(this.formatCurrency(data.totalEarlyLateFee));
        $('#totalAdjustmentAmount').text(this.formatCurrency(data.totalAdjustmentAmount));

        // 결제 요약 카드 바인딩
        var grandTotal = Number(data.grandTotal) || 0;
        var totalPaid = Number(data.totalPaidAmount) || 0;
        var remaining = Number(data.remainingAmount) || 0;
        var cancelFee = Number(data.cancelFeeAmount) || 0;
        var refund = Number(data.refundAmount) || 0;

        $('#summaryGrandTotal').text(this.formatCurrency(grandTotal));
        $('#summaryPaidAmount').text(this.formatCurrency(totalPaid));

        // 잔액 색상 분기
        var $remaining = $('#summaryRemainingAmount');
        var $remainingBox = $('#summaryRemainingBox');
        $remainingBox.removeClass('ps-remaining-due ps-remaining-refund ps-remaining-clear');
        if (remaining < 0) {
            $remaining.text('환불필요 ' + this.formatCurrency(Math.abs(remaining)));
            $remainingBox.addClass('ps-remaining-refund');
        } else if (remaining > 0) {
            $remaining.text(this.formatCurrency(remaining));
            $remainingBox.addClass('ps-remaining-due');
        } else {
            $remaining.text('0원');
            $remainingBox.addClass('ps-remaining-clear');
        }

        // 환불 상태 시 추가 정보 행 표시
        if (data.paymentStatus === 'REFUNDED') {
            $('#summaryRefundAmount').text(this.formatCurrency(refund));
            $('#summaryCancelFee').text(this.formatCurrency(cancelFee));
            $('#refundSummaryRow').removeClass('d-none');
        } else {
            $('#refundSummaryRow').addClass('d-none');
        }

        // 결제 상태 배지
        this.renderPaymentStatus(data.paymentStatus);

        // 취소/환불 정보 섹션
        this.renderCancelInfo(data);
    },

    /**
     * 요금 세부 내역 렌더링 (reservationData 기반)
     * - 멀티레그(subs.length > 1): Leg별 카드 레이아웃
     * - 싱글레그(subs.length == 1): 기존 카테고리형 레이아웃
     */
    renderChargeBreakdown: function() {
        var self = this;
        var data = self.reservationData;
        if (!data || !data.subReservations) return;

        var subs = data.subReservations;
        var isMultiLeg = subs.length > 1;

        if (isMultiLeg) {
            this.renderMultiLegBreakdown(subs);
        } else {
            this.renderSingleLegBreakdown(subs);
        }
    },

    /**
     * Leg별 상태 배지 HTML
     */
    getLegStatusBadge: function(status) {
        var map = {
            RESERVED: { label: '예약', cls: 'bg-primary' },
            CONFIRMED: { label: '확정', cls: 'bg-info' },
            CHECK_IN: { label: '체크인', cls: 'bg-success' },
            CHECKED_OUT: { label: '체크아웃', cls: 'bg-secondary' },
            CANCELED: { label: '취소됨', cls: 'bg-secondary' },
            NO_SHOW: { label: '노쇼', cls: 'bg-danger' }
        };
        var info = map[status] || { label: status || '-', cls: 'bg-secondary' };
        return '<span class="badge ' + info.cls + ' ms-2" style="font-size:0.7rem;">' + HolaPms.escapeHtml(info.label) + '</span>';
    },

    /**
     * 멀티레그 Leg별 카드 렌더링
     */
    renderMultiLegBreakdown: function(subs) {
        var self = this;
        var totalSupply = 0, totalTax = 0, totalSvcChg = 0;
        var html = '';

        // 결제 상태 정보
        var rsvStatus = self.reservationData ? self.reservationData.reservationStatus : '';
        var isCanceledOrNoShow = rsvStatus === 'CANCELED' || rsvStatus === 'NO_SHOW';
        var paymentStatus = self.paymentData ? self.paymentData.paymentStatus : '';
        var isPaymentComplete = paymentStatus === 'OVERPAID' || paymentStatus === 'REFUNDED';

        // Leg별 결제 현황 데이터 (API에서 반환)
        var legPayments = (self.paymentData && self.paymentData.legPayments) || [];
        var legPaymentMap = {};
        legPayments.forEach(function(lp) {
            legPaymentMap[lp.subReservationId] = lp;
        });

        subs.forEach(function(sub, idx) {
            var isCanceled = sub.roomReservationStatus === 'CANCELED';
            var legNum = idx + 1;
            var label = sub.roomTypeName || ('객실 #' + legNum);
            var legLabel = 'Leg #' + legNum + ' - ' + label;
            var rowClass = isCanceled ? ' class="text-decoration-line-through text-muted"' : '';

            // ── Leg 소계 계산 ──
            var legRoomSupply = 0, legRoomTax = 0, legSvcChg = 0, legServiceTotal = 0;
            (sub.dailyCharges || []).forEach(function(c) {
                legRoomSupply += Number(c.supplyPrice) || 0;
                legRoomTax += Number(c.tax) || 0;
                legSvcChg += Number(c.serviceCharge) || 0;
            });
            (sub.services || []).forEach(function(svc) {
                if (Number(svc.totalPrice) > 0) {
                    legServiceTotal += Number(svc.totalPrice) || 0;
                }
            });
            var legTotal = legRoomSupply + legRoomTax + legSvcChg + legServiceTotal;

            // Per-Leg 결제 현황 (API legPayments 데이터 사용)
            var legPmtInfo = legPaymentMap[sub.id] || {};
            var legPaid = Number(legPmtInfo.legPaid) || 0;
            var legRefunded = Number(legPmtInfo.legRefunded) || 0;
            var legRemaining = legPmtInfo.legRemaining != null ? Number(legPmtInfo.legRemaining) : legTotal;

            if (!isCanceled) {
                totalSupply += legRoomSupply;
                totalTax += legRoomTax;
                totalSvcChg += legSvcChg;
                // 서비스 supply/tax 합산
                (sub.services || []).forEach(function(svc) {
                    if (Number(svc.totalPrice) > 0) {
                        var unitP = Number(svc.unitPrice) || 0;
                        var qty = svc.quantity || 1;
                        totalSupply += unitP * qty;
                        totalTax += Number(svc.tax) || 0;
                    }
                });
            }

            // ── Leg 카드 시작 ──
            html += '<div class="leg-card' + (isCanceled ? ' leg-card-canceled' : '') + '" data-sub-id="' + sub.id + '">';

            // 카드 헤더: Leg # - RoomType [상태배지]    합계: XXX원
            html += '<div class="leg-card-header">';
            html += '<div class="d-flex align-items-center">';
            html += '<span class="leg-card-title">' + HolaPms.escapeHtml(legLabel) + '</span>';
            html += self.getLegStatusBadge(sub.roomReservationStatus);
            html += '</div>';
            html += '<span class="leg-card-total' + (isCanceled ? ' text-decoration-line-through text-muted' : '') + '">';
            html += '합계: ' + self.formatCurrency(legTotal);
            // Per-Leg 결제 상태 표시
            if (!isCanceled && legPaid > 0) {
                var netLegPaid = legPaid - legRefunded;
                if (netLegPaid >= legTotal) {
                    html += ' <span class="badge bg-success ms-1" style="font-size:0.65rem;">결제완료</span>';
                } else if (netLegPaid > 0) {
                    html += ' <span class="badge bg-info ms-1" style="font-size:0.65rem;">부분결제</span>';
                }
            }
            html += '</span>';
            html += '</div>';

            // 카드 바디
            html += '<div class="leg-card-body">';

            // ── 객실 요금 ──
            var charges = sub.dailyCharges || [];
            if (charges.length > 0) {
                var roomAmount = legRoomSupply + legRoomTax;
                html += '<div class="leg-section">';
                html += '<div class="d-flex justify-content-between align-items-center charge-toggle" data-target="#legRoomDetail' + idx + '" style="cursor:pointer">';
                html += '<span><i class="fas fa-caret-right me-1 toggle-icon"></i>객실 요금</span>';
                html += '<span' + (isCanceled ? ' class="text-decoration-line-through text-muted"' : '') + '>' + self.formatCurrency(roomAmount) + '</span>';
                html += '</div>';
                html += '<div id="legRoomDetail' + idx + '" style="display:none" class="mt-2">';
                html += '<div class="charge-detail-wrap">';
                html += '<table class="table table-sm charge-detail-table">';
                html += '<thead><tr>'
                    + '<th class="col-date">날짜</th>'
                    + '<th class="col-amount">공급가</th>'
                    + '<th class="col-amount">세액</th>'
                    + '<th class="col-amount">소계</th>'
                    + '</tr></thead><tbody>';

                charges.forEach(function(c) {
                    var sp = Number(c.supplyPrice) || 0;
                    var tx = Number(c.tax) || 0;
                    html += '<tr' + rowClass + '>'
                        + '<td class="col-date">' + c.chargeDate + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(sp) + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(tx) + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(sp + tx) + '</td>'
                        + '</tr>';
                });

                html += '</tbody></table></div></div></div>';
            }

            // ── 유료 서비스 ──
            var legServices = (sub.services || []).filter(function(svc) { return Number(svc.totalPrice) > 0; });
            html += '<div class="leg-section">';
            html += '<div class="d-flex justify-content-between align-items-center">';
            html += '<span class="ps-3">유료 서비스</span>';
            html += '<span' + (isCanceled ? ' class="text-decoration-line-through text-muted"' : '') + '>' + self.formatCurrency(legServiceTotal) + '</span>';
            html += '</div>';

            if (legServices.length > 0) {
                html += '<div class="charge-detail-wrap">';
                html += '<table class="table table-sm charge-detail-table">';
                html += '<thead><tr>'
                    + '<th class="col-label">항목</th>'
                    + '<th class="col-qty">수량</th>'
                    + '<th class="col-amount">단가</th>'
                    + '<th class="col-amount">세액</th>'
                    + '<th class="col-amount">합계</th>'
                    + '</tr></thead><tbody>';

                legServices.forEach(function(svc) {
                    var typeLabel = svc.serviceName || (svc.serviceOptionId ? '서비스 #' + svc.serviceOptionId : '객실 업그레이드');
                    if (svc.serviceDate) typeLabel += ' (' + svc.serviceDate + ')';
                    var unitP = Number(svc.unitPrice) || 0;
                    var qty = svc.quantity || 1;
                    var sTax = Number(svc.tax) || 0;
                    var sTotal = Number(svc.totalPrice) || 0;

                    html += '<tr' + rowClass + '>'
                        + '<td class="col-label">' + HolaPms.escapeHtml(typeLabel) + '</td>'
                        + '<td class="col-qty">' + qty + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(unitP) + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(sTax) + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(sTotal) + '</td>'
                        + '</tr>';
                });

                html += '</tbody></table></div>';
            }
            html += '</div>';

            // ── 봉사료 ──
            html += '<div class="leg-section">';
            html += '<div class="d-flex justify-content-between align-items-center">';
            html += '<span class="ps-3">봉사료</span>';
            html += '<span' + (isCanceled ? ' class="text-decoration-line-through text-muted"' : '') + '>' + self.formatCurrency(legSvcChg) + '</span>';
            html += '</div>';
            html += '</div>';

            // ── Per-Leg 결제 버튼 (해당 Leg의 잔액 > 0, 취소/노쇼/완납 아닌 경우, Leg 미취소) ──
            var showLegPayButton = !isCanceled && !isCanceledOrNoShow && !isPaymentComplete
                && !self.isOta && legRemaining > 0;
            if (showLegPayButton) {
                html += '<div class="leg-payment-buttons mt-2 pt-2 border-top">';
                // 결제/잔액 요약 라인
                if (legPaid > 0) {
                    html += '<div class="mb-1 small text-muted">';
                    html += '결제: ' + self.formatCurrency(legPaid - legRefunded) + ' / 잔액: ' + self.formatCurrency(legRemaining);
                    html += '</div>';
                }
                html += '<button class="btn btn-primary btn-sm me-1 leg-card-pay-btn" '
                    + 'data-sub-id="' + sub.id + '" data-leg-index="' + legNum + '" '
                    + 'data-leg-label="' + HolaPms.escapeHtml(legLabel) + '" data-leg-total="' + legTotal + '" '
                    + 'data-leg-paid="' + (legPaid - legRefunded) + '" data-leg-remaining="' + legRemaining + '" '
                    + 'data-pay-method="card">'
                    + '<i class="fas fa-credit-card me-1"></i>카드결제</button>';
                html += '<button class="btn btn-success btn-sm leg-card-pay-btn" '
                    + 'data-sub-id="' + sub.id + '" data-leg-index="' + legNum + '" '
                    + 'data-leg-label="' + HolaPms.escapeHtml(legLabel) + '" data-leg-total="' + legTotal + '" '
                    + 'data-leg-paid="' + (legPaid - legRefunded) + '" data-leg-remaining="' + legRemaining + '" '
                    + 'data-pay-method="cash">'
                    + '<i class="fas fa-money-bill-wave me-1"></i>현금결제</button>';
                html += '</div>';
            }

            html += '</div>'; // leg-card-body 끝
            html += '</div>'; // leg-card 끝
        });

        $('#chargeBreakdown').html(html);

        // 멀티레그일 때 글로벌 결제 버튼 숨김
        $('#paymentButtonGroup').addClass('d-none');

        // ── 공급가/세액/봉사료 소계 바인딩 ──
        $('#totalSupplyPrice').text(self.formatCurrency(totalSupply));
        $('#totalTaxAmount').text(self.formatCurrency(totalTax));
        $('#totalSvcChgSubtotal').text(self.formatCurrency(totalSvcChg));
    },

    /**
     * 싱글레그 카테고리형 렌더링 (기존 로직 유지)
     */
    renderSingleLegBreakdown: function(subs) {
        var self = this;
        var sub = subs[0];
        var totalSupply = 0, totalTax = 0, totalSvcChg = 0;
        var html = '';

        // ── 1. 객실 요금 ──
        var charges = sub.dailyCharges || [];
        var roomHtml = '';
        if (charges.length > 0) {
            roomHtml += '<div class="charge-detail-wrap">';
            roomHtml += '<table class="table table-sm charge-detail-table">';
            roomHtml += '<thead><tr>'
                + '<th class="col-date">날짜</th>'
                + '<th class="col-amount">공급가</th>'
                + '<th class="col-amount">세액</th>'
                + '<th class="col-amount">소계</th>'
                + '</tr></thead><tbody>';

            charges.forEach(function(c) {
                var sp = Number(c.supplyPrice) || 0;
                var tx = Number(c.tax) || 0;
                var sc = Number(c.serviceCharge) || 0;
                totalSupply += sp;
                totalTax += tx;
                totalSvcChg += sc;

                roomHtml += '<tr>'
                    + '<td class="col-date">' + c.chargeDate + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(sp) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(tx) + '</td>'
                    + '<td class="col-amount">' + self.formatCurrency(sp + tx) + '</td>'
                    + '</tr>';
            });

            roomHtml += '</tbody></table></div>';
        }
        var totalRoomAmount = 0;
        charges.forEach(function(c) {
            totalRoomAmount += (Number(c.supplyPrice) || 0) + (Number(c.tax) || 0);
        });

        // ── 2. 유료 서비스 ──
        var svcHtml = '';
        var paidServices = (sub.services || []).filter(function(svc) { return Number(svc.totalPrice) > 0; });
        var totalServiceAmount = 0;

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

            paidServices.forEach(function(svc) {
                var typeLabel = svc.serviceName || (svc.serviceOptionId ? '서비스 #' + svc.serviceOptionId : '객실 업그레이드');
                if (svc.serviceDate) typeLabel += ' (' + svc.serviceDate + ')';
                var unitP = Number(svc.unitPrice) || 0;
                var qty = svc.quantity || 1;
                var sTax = Number(svc.tax) || 0;
                var sTotal = Number(svc.totalPrice) || 0;

                totalSupply += unitP * qty;
                totalTax += sTax;
                totalServiceAmount += sTotal;

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

        // ── 3. 봉사료 ──
        var chgHtml = '';
        var hasChg = charges.some(function(c) { return Number(c.serviceCharge) > 0; });
        if (hasChg) {
            chgHtml += '<div class="charge-detail-wrap">';
            chgHtml += '<table class="table table-sm charge-detail-table">';
            chgHtml += '<thead><tr><th class="col-date">날짜</th><th class="col-amount">봉사료</th></tr></thead><tbody>';

            charges.forEach(function(c) {
                var sc = Number(c.serviceCharge) || 0;
                if (sc > 0) {
                    chgHtml += '<tr><td class="col-date">' + c.chargeDate + '</td>'
                        + '<td class="col-amount">' + self.formatCurrency(sc) + '</td></tr>';
                }
            });

            chgHtml += '</tbody></table></div>';
        }

        // ── HTML 조합: 카테고리형 토글 ──
        var breakdownHtml = '';

        // 객실 요금 (기본 펼침)
        breakdownHtml += '<div class="border-bottom py-2">';
        breakdownHtml += '<div class="d-flex justify-content-between align-items-center charge-toggle" data-target="#roomDetail" style="cursor:pointer">';
        breakdownHtml += '<span><i class="fas fa-caret-down me-1 toggle-icon"></i>객실 요금</span>';
        breakdownHtml += '<span>' + self.formatCurrency(totalRoomAmount) + '</span>';
        breakdownHtml += '</div>';
        breakdownHtml += '<div id="roomDetail" class="mt-2">';
        breakdownHtml += roomHtml || '<div class="charge-empty">객실 요금 내역이 없습니다.</div>';
        breakdownHtml += '</div></div>';

        // 유료 서비스 (기본 펼침)
        breakdownHtml += '<div class="border-bottom py-2">';
        breakdownHtml += '<div class="d-flex justify-content-between align-items-center charge-toggle" data-target="#serviceDetail" style="cursor:pointer">';
        breakdownHtml += '<span><i class="fas fa-caret-down me-1 toggle-icon"></i>유료 서비스 요금</span>';
        breakdownHtml += '<span>' + self.formatCurrency(totalServiceAmount) + '</span>';
        breakdownHtml += '</div>';
        breakdownHtml += '<div id="serviceDetail" class="mt-2">';
        breakdownHtml += svcHtml || '<div class="charge-empty">유료 서비스 내역이 없습니다.</div>';
        breakdownHtml += '</div></div>';

        // 봉사료 (기본 펼침)
        breakdownHtml += '<div class="border-bottom py-2">';
        breakdownHtml += '<div class="d-flex justify-content-between align-items-center charge-toggle" data-target="#svcChgDetail" style="cursor:pointer">';
        breakdownHtml += '<span><i class="fas fa-caret-down me-1 toggle-icon"></i>봉사료</span>';
        breakdownHtml += '<span>' + self.formatCurrency(totalSvcChg) + '</span>';
        breakdownHtml += '</div>';
        breakdownHtml += '<div id="svcChgDetail" class="mt-2">';
        breakdownHtml += chgHtml || '<div class="charge-empty">봉사료 내역이 없습니다.</div>';
        breakdownHtml += '</div></div>';

        $('#chargeBreakdown').html(breakdownHtml);

        // 싱글레그: 결제 버튼 표시 여부는 renderPaymentStatus에서 결정
        // (여기서 show() 하면 PAID/OVERPAID 등에서도 버튼이 보이는 버그 발생)
        if (this.paymentData) {
            this.renderPaymentStatus(this.paymentData.paymentStatus);
        }

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

        // 결제 버튼 표시 조건
        var remaining = this.paymentData ? Number(this.paymentData.remainingAmount) || 0 : 0;
        var rsvStatus = this.reservationData ? this.reservationData.reservationStatus : '';
        var isCanceledOrNoShow = rsvStatus === 'CANCELED' || rsvStatus === 'NO_SHOW';
        var subs = this.reservationData ? this.reservationData.subReservations || [] : [];
        var isMultiLeg = subs.length > 1;

        var hideBtn = isMultiLeg || status === 'OVERPAID' || status === 'REFUNDED'
            || isCanceledOrNoShow || remaining <= 0;
        $('#paymentButtonGroup').toggleClass('d-none', hideBtn);
    },

    /**
     * 현금결제 모달 열기
     * @param {Object} [legContext] - Leg 컨텍스트 (멀티레그에서 per-Leg 결제 시)
     * @param {number} legContext.subId - SubReservation ID
     * @param {number} legContext.legIndex - Leg 번호 (1-based)
     * @param {string} legContext.legLabel - Leg 라벨 (e.g. "Leg #1 - RYL-K")
     * @param {number} legContext.legTotal - Leg 합계 금액
     */
    openCashPaymentModal: function(legContext) {
        var self = this;
        // Leg 컨텍스트 저장 (결제 처리 시 memo에 반영)
        self._currentLegContext = legContext || null;

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

                    // Leg 컨텍스트가 있으면 Leg 단위 금액 표시
                    if (legContext) {
                        $('#cashPaymentModalTitle').text(legContext.legLabel + ' 현금결제');
                        var legTotal = Number(legContext.legTotal) || 0;
                        var legPaid = Number(legContext.legPaid) || 0;
                        var legRemaining = legContext.legRemaining != null ? Number(legContext.legRemaining) : legTotal;

                        $('#cashGrandTotalDisplay').val(self.formatCurrency(legTotal));
                        $('#cashPaidDisplay').val(self.formatCurrency(legPaid));
                        $('#cashRemainingDisplay').val(self.formatCurrency(legRemaining));
                        $('#cashPaymentAmount').val(legRemaining > 0 ? Math.floor(legRemaining) : '');
                        $('#cashPaymentMemo').val(legContext.legLabel);
                    } else {
                        $('#cashPaymentModalTitle').text('현금 결제');
                        $('#cashGrandTotalDisplay').val(self.formatCurrency(grandTotal));
                        $('#cashPaidDisplay').val(self.formatCurrency(totalPaid));
                        $('#cashRemainingDisplay').val(self.formatCurrency(remaining));
                        $('#cashPaymentAmount').val(remaining > 0 ? Math.floor(remaining) : '');
                        $('#cashPaymentMemo').val('');
                    }

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
            memo: memo || null,
            subReservationId: (self._currentLegContext && self._currentLegContext.subId) ? self._currentLegContext.subId : null
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
                        self.renderChargeBreakdown(); // Leg별 결제 버튼 재렌더링
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

        // 건수 배지 업데이트
        $('#paymentHistoryCount').text(transactions ? transactions.length : 0);

        if (!transactions || transactions.length === 0) {
            $content.html('<p class="text-center text-muted py-3">결제 이력이 없습니다.</p>');
            $collapse.removeClass('show');
            $icon.removeClass('fa-caret-up').addClass('fa-caret-down');
            return;
        }

        // 이력 있으면 기본 펼침
        $collapse.addClass('show');
        $icon.removeClass('fa-caret-down').addClass('fa-caret-up');

        var methodLabels = { CARD: '카드', CASH: '현금' };
        var typeLabels = { PAYMENT: '결제', REFUND: '환불', CANCEL_FEE: '취소수수료' };
        var typeStyles = { REFUND: 'text-primary', CANCEL_FEE: 'text-danger' };

        // PG 환불 실패 건 존재 여부
        var hasFailedRefund = transactions.some(function(t) { return t.transactionStatus === 'PG_REFUND_FAILED'; });
        // 멀티레그 여부
        var subs = self.reservationData ? self.reservationData.subReservations || [] : [];
        var isMultiLeg = subs.length > 1;
        var subLabelMap = {};
        if (isMultiLeg) {
            subs.forEach(function(sub, idx) {
                subLabelMap[sub.id] = 'Leg #' + (idx + 1) + ' - ' + (sub.roomTypeName || '객실 #' + (idx + 1));
            });
        }

        // 핵심 5컬럼 + 멀티레그 시 객실 컬럼
        var colCount = isMultiLeg ? 6 : 5;
        var html = '<table class="table table-sm mb-0 align-middle txn-history-table">'
            + '<thead><tr>'
            + '<th class="text-center" style="width:40px">NO</th>'
            + '<th class="text-center" style="width:70px">유형</th>';
        if (isMultiLeg) html += '<th class="text-center">객실</th>';
        html += '<th class="text-center" style="width:70px">수단</th>'
            + '<th class="text-end" style="width:110px">금액</th>'
            + '<th class="text-center" style="width:140px">일시</th>'
            + '</tr></thead><tbody>';

        transactions.forEach(function(txn, idx) {
            var methodLabel = methodLabels[txn.paymentMethod] || HolaPms.escapeHtml(txn.paymentMethod);
            var createdAt = txn.createdAt ? txn.createdAt.replace('T', ' ').substring(0, 16) : '-';
            var typeLabel = typeLabels[txn.transactionType] || HolaPms.escapeHtml(txn.transactionType || '결제');
            var typeStyle = typeStyles[txn.transactionType] || '';

            // 상태 배지
            var statusHtml = '';
            if (txn.transactionStatus === 'PG_REFUND_FAILED') {
                statusHtml = ' <span class="badge bg-danger">실패</span>';
            } else if (txn.transactionStatus === 'MANUAL_CONFIRMED') {
                statusHtml = ' <span class="badge badge-manual-refund">수동확인</span>';
            }

            // PG 상세 서브행 판단 (메인행 클래스 결정에 필요)
            var hasPg = txn.pgCno || txn.pgApprovalNo || txn.pgIssuerName;
            var hasMemo = txn.memo;
            var hasDetailRow = hasPg || hasMemo || txn.transactionStatus === 'PG_REFUND_FAILED';

            // 메인 행
            html += '<tr class="txn-main-row' + (hasDetailRow ? ' has-detail' : '') + '">'
                + '<td class="text-center">' + (idx + 1) + '</td>'
                + '<td class="text-center ' + typeStyle + '">' + typeLabel + statusHtml + '</td>';
            if (isMultiLeg) {
                var legLabel = txn.subReservationId ? (subLabelMap[txn.subReservationId] || '-') : '-';
                html += '<td class="small">' + HolaPms.escapeHtml(legLabel) + '</td>';
            }
            html += '<td class="text-center">' + methodLabel + '</td>'
                + '<td class="text-end fw-medium">' + self.formatCurrency(txn.amount) + '</td>'
                + '<td class="text-center text-nowrap small">' + HolaPms.escapeHtml(createdAt) + '</td>'
                + '</tr>';
            if (hasDetailRow) {
                html += '<tr class="txn-detail-row"><td></td><td colspan="' + (colCount - 1) + '">';
                var details = [];
                if (txn.pgIssuerName) details.push(HolaPms.escapeHtml(txn.pgIssuerName));
                if (txn.pgCardNo) details.push(HolaPms.escapeHtml(txn.pgCardNo));
                if (txn.pgApprovalNo || txn.approvalNo) details.push('승인 ' + HolaPms.escapeHtml(txn.pgApprovalNo || txn.approvalNo));
                if (txn.pgInstallmentMonth != null) {
                    details.push(txn.pgInstallmentMonth === 0 ? '일시불' : txn.pgInstallmentMonth + '개월');
                }
                if (details.length > 0) {
                    html += '<span class="text-muted">' + details.join(' · ') + '</span>';
                }
                if (hasMemo) {
                    html += (details.length > 0 ? '<br>' : '') + '<span class="text-muted">메모: ' + HolaPms.escapeHtml(txn.memo) + '</span>';
                }
                if (txn.createdBy) {
                    html += (details.length > 0 || hasMemo ? ' · ' : '') + '<span class="text-muted">처리자: ' + HolaPms.escapeHtml(txn.createdBy) + '</span>';
                }
                // PG 환불 실패 시 재시도 버튼
                if (txn.transactionStatus === 'PG_REFUND_FAILED') {
                    html += '<br><button class="btn btn-warning btn-sm mt-1 retry-refund-btn" data-txn-id="' + txn.id + '">'
                        + '<i class="fas fa-redo me-1"></i>PG 환불 재시도</button>';
                }
                html += '</td></tr>';
            }
        });

        html += '</tbody></table>';
        $content.html(html);

        // PG 환불 재시도 버튼 이벤트
        if (hasFailedRefund) {
            $content.off('click', '.retry-refund-btn').on('click', '.retry-refund-btn', function() {
                var txnId = $(this).data('txn-id');
                var $btn = $(this);
                $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm"></span>');

                HolaPms.ajax({
                    url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                        + '/payment/transactions/' + txnId + '/retry-refund',
                    type: 'POST',
                    success: function(res) {
                        if (res.success && res.data) {
                            var updatedTxn = (res.data.transactions || []).find(function(t) { return t.id === txnId; });
                            var refundSucceeded = updatedTxn && updatedTxn.transactionStatus === 'COMPLETED';
                            self.bindSummary(res.data);
                            self.renderPaymentTransactions(res.data.transactions || []);
                            self.renderCancelInfo(res.data);
                            HolaPms.alert(refundSucceeded ? 'success' : 'error',
                                refundSucceeded ? 'PG 환불이 완료되었습니다.' : 'PG 환불 재시도에 실패했습니다.');
                        }
                    },
                    error: function() {
                        $btn.prop('disabled', false).html('<i class="fas fa-redo me-1"></i>PG 환불 재시도');
                        HolaPms.alert('error', 'PG 환불 재시도에 실패했습니다.');
                    }
                });
            });
        }
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

        var html = '<table class="table table-sm mb-0 align-middle txn-history-table">'
            + '<thead><tr>'
            + '<th class="text-center" style="width:40px">NO</th>'
            + '<th class="text-center" style="width:50px">구분</th>'
            + '<th class="text-end" style="width:100px">공급가</th>'
            + '<th class="text-end" style="width:100px">세금</th>'
            + '<th class="text-end" style="width:100px">합계</th>'
            + '<th>사유</th>'
            + '<th class="text-center" style="width:70px">작성자</th>'
            + '<th class="text-center" style="width:130px">일시</th>'
            + '</tr></thead><tbody>';

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
        var self = this;
        var cancelFee = Number(data.cancelFeeAmount) || 0;
        var refund = Number(data.refundAmount) || 0;
        var transactions = data.transactions || [];

        // 환불/수수료 거래가 하나도 없으면 숨김
        var hasRefundActivity = transactions.some(function(t) {
            return t.transactionType === 'REFUND' || t.transactionType === 'CANCEL_FEE';
        });
        if (!hasRefundActivity && cancelFee <= 0 && refund <= 0) {
            $('#cancelInfoSection').addClass('d-none');
            return;
        }

        var totalPaid = Number(data.totalPaidAmount) || 0;
        var adjustTotal = Number(data.totalAdjustmentAmount) || 0;
        var fmt = function(v) { return self.formatCurrency(v); };
        var esc = HolaPms.escapeHtml;

        // Leg 라벨 매핑
        var subs = self.reservationData ? self.reservationData.subReservations || [] : [];
        var subLabelMap = {};
        var subStatusMap = {};
        subs.forEach(function(sub, idx) {
            subLabelMap[sub.id] = 'Leg #' + (idx + 1) + ' - ' + (sub.roomTypeName || '');
            subStatusMap[sub.id] = sub.roomReservationStatus;
        });

        var methodLabels = { CARD: '카드', CASH: '현금', TRANSFER: '계좌이체' };

        // ── 전체 요약 카드 ──
        var html = '<div class="bg-light rounded p-3 mb-3">';
        html += '<div class="row mb-2"><div class="col-sm-3 text-muted">총 결제액</div><div class="col-sm-9">' + fmt(totalPaid) + '</div></div>';
        html += '<div class="row mb-2"><div class="col-sm-3 text-muted">총 취소 수수료</div><div class="col-sm-9 text-danger">' + fmt(cancelFee) + '</div></div>';
        html += '<div class="row mb-2"><div class="col-sm-3 text-muted">총 환불 금액</div><div class="col-sm-9 text-primary">' + fmt(refund) + '</div></div>';
        if (adjustTotal !== 0) {
            html += '<div class="row mb-2"><div class="col-sm-3 text-muted">조정 금액</div><div class="col-sm-9">'
                + (adjustTotal > 0 ? '+' : '') + fmt(adjustTotal) + '</div></div>';
        }
        html += '</div>';

        // ── 거래를 Leg별로 그룹핑 ──
        var legGroups = {};     // subReservationId → { payments:[], refunds:[], cancelFees:[] }
        var unassigned = { payments: [], refunds: [], cancelFees: [] };

        transactions.forEach(function(txn) {
            var key = txn.subReservationId;
            var target;
            if (key && subLabelMap[key]) {
                if (!legGroups[key]) legGroups[key] = { payments: [], refunds: [], cancelFees: [] };
                target = legGroups[key];
            } else {
                target = unassigned;
            }
            if (txn.transactionType === 'PAYMENT') target.payments.push(txn);
            else if (txn.transactionType === 'REFUND') target.refunds.push(txn);
            else if (txn.transactionType === 'CANCEL_FEE') target.cancelFees.push(txn);
        });

        // 취소/환불 활동이 있는 Leg만 표시
        var renderTxnDetail = function(txn, typeLabel) {
            var method = methodLabels[txn.paymentMethod] || esc(txn.paymentMethod || '-');
            var line = '<span class="me-2">' + typeLabel + '</span>';
            if (txn.pgCno) {
                // PG 거래
                line += method + '(PG) <strong>' + fmt(txn.amount) + '</strong>';
                if (txn.pgIssuerName || txn.pgCardNo) {
                    line += ' - ' + esc(txn.pgIssuerName || '') + ' ' + esc(txn.pgCardNo || '');
                }
                if (txn.pgApprovalNo) line += ' (승인번호: ' + esc(txn.pgApprovalNo) + ')';
                if (txn.transactionStatus === 'PG_REFUND_FAILED') {
                    line += ' <span class="badge bg-danger ms-1">PG환불실패</span>';
                }
            } else if (txn.transactionStatus === 'MANUAL_CONFIRMED') {
                // 수동 환불 확인 완료
                line += method + ' <strong>' + fmt(txn.amount) + '</strong>';
                line += ' <span class="badge badge-manual-refund">수동환불(확인)</span>';
            } else {
                // 현금/VAN 등
                line += method + ' <strong>' + fmt(txn.amount) + '</strong>';
            }
            return line;
        };

        var renderLegCard = function(subId, group, label) {
            if (group.refunds.length === 0 && group.cancelFees.length === 0) return '';

            var status = subId ? subStatusMap[subId] : null;
            var statusBadge = '';
            if (status) statusBadge = ' ' + HolaPms.reservationStatus.styledBadge(status);

            var card = '<div class="border rounded p-3 mb-2">';
            card += '<div class="mb-2"><strong>' + esc(label) + '</strong>' + statusBadge + '</div>';

            // 정책 설명 (REFUND memo에서 추출)
            var policyDesc = '';
            group.refunds.forEach(function(r) {
                if (r.memo && !policyDesc) policyDesc = r.memo;
            });
            if (policyDesc) {
                card += '<div class="row mb-1"><div class="col-sm-3 text-muted small">적용 정책</div><div class="col-sm-9 small">' + esc(policyDesc) + '</div></div>';
            }

            // 수수료
            group.cancelFees.forEach(function(txn) {
                card += '<div class="row mb-1"><div class="col-sm-3 text-muted small">취소 수수료</div><div class="col-sm-9 small text-danger">'
                    + renderTxnDetail(txn, '') + '</div></div>';
            });

            // 결제 내역
            group.payments.forEach(function(txn) {
                card += '<div class="row mb-1"><div class="col-sm-3 text-muted small">결제</div><div class="col-sm-9 small">'
                    + renderTxnDetail(txn, '') + '</div></div>';
            });

            // 환불 내역
            group.refunds.forEach(function(txn) {
                card += '<div class="row mb-1"><div class="col-sm-3 text-muted small">환불</div><div class="col-sm-9 small text-primary">'
                    + renderTxnDetail(txn, '') + '</div></div>';
            });

            card += '</div>';
            return card;
        };

        // Leg 순서대로 렌더링
        subs.forEach(function(sub) {
            if (legGroups[sub.id]) {
                html += renderLegCard(sub.id, legGroups[sub.id], subLabelMap[sub.id]);
            }
        });

        // 공통 환불 (특정 객실에 귀속되지 않은 거래)
        if (unassigned.refunds.length > 0 || unassigned.cancelFees.length > 0) {
            html += renderLegCard(null, unassigned, '공통 환불');
        }

        // 조정 내역 상세 (있는 경우)
        var adjustments = data.adjustments || [];
        if (adjustments.length > 0) {
            html += '<div class="border rounded p-3 mb-2">';
            html += '<div class="mb-2"><strong>조정 내역</strong></div>';
            adjustments.forEach(function(adj) {
                var adjAmt = Number(adj.totalAmount) || 0;
                var sign = adj.adjustmentSign === '+' ? '+' : '-';
                html += '<div class="row mb-1"><div class="col-sm-3 text-muted small">'
                    + sign + '</div>'
                    + '<div class="col-sm-5 small">' + esc(adj.comment || '-') + '</div>'
                    + '<div class="col-sm-4 small text-end">' + fmt(adjAmt) + '</div></div>';
            });
            html += '</div>';
        }

        $('#cancelInfoContent').html(html);
        $('#cancelInfoSection').removeClass('d-none');
    },

    /**
     * 통화 포맷 (콤마 + 원)
     */
    formatCurrency: function(amount) {
        if (amount === null || amount === undefined) return '0원';
        return Number(amount).toLocaleString() + '원';
    },

    /**
     * 인보이스 출력 (새 창 — Thymeleaf 템플릿)
     */
    printInvoice: function() {
        if (!this.reservationId || !this.propertyId) {
            HolaPms.alert('warning', '예약 정보를 먼저 로드해주세요.');
            return;
        }
        var url = '/admin/reservations/' + this.reservationId + '/invoice?propertyId=' + this.propertyId;
        var win = window.open(url, '_blank', 'width=900,height=700');
        if (!win) {
            HolaPms.alert('warning', '팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요.');
        }
    }
};
