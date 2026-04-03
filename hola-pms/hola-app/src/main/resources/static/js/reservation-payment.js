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
                var legBtnData = 'data-sub-id="' + sub.id + '" data-leg-index="' + legNum + '" '
                    + 'data-leg-label="' + HolaPms.escapeHtml(legLabel) + '" data-leg-total="' + legTotal + '" '
                    + 'data-leg-paid="' + (legPaid - legRefunded) + '" data-leg-remaining="' + legRemaining + '"';
                html += '<button class="btn btn-primary btn-sm me-1 leg-card-pay-btn" '
                    + legBtnData + ' data-pay-method="card">'
                    + '<i class="fas fa-credit-card me-1"></i>카드결제</button>';
                html += '<button class="btn btn-success btn-sm me-1 leg-card-pay-btn" '
                    + legBtnData + ' data-pay-method="cash">'
                    + '<i class="fas fa-money-bill-wave me-1"></i>현금결제</button>';
                html += '<button class="btn btn-outline-secondary btn-sm leg-card-pay-btn" '
                    + legBtnData + ' data-pay-method="manual-cash">'
                    + '<i class="fas fa-coins me-1"></i>수동 현금</button>';
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

        var html = '<table class="table table-sm mb-0 align-middle txn-history-table">'
            + '<thead><tr>'
            + '<th class="text-center" style="width:40px">NO</th>'
            + '<th class="text-center" style="width:100px">유형</th>';
        if (isMultiLeg) html += '<th class="text-center" style="width:120px">객실</th>';
        html += '<th class="text-center" style="width:90px">수단</th>'
            + '<th>내용</th>'
            + '<th class="text-end" style="width:110px">금액</th>'
            + '<th class="text-center" style="width:140px">일시</th>'
            + '</tr></thead><tbody>';

        transactions.forEach(function(txn, idx) {
            var methodLabel = methodLabels[txn.paymentMethod] || HolaPms.escapeHtml(txn.paymentMethod);
            // PG/VAN 채널 구분 표시
            if (txn.paymentMethod === 'CARD') {
                if (txn.paymentChannel === 'VAN') {
                    methodLabel = '카드(VAN)';
                } else if (txn.pgCno || txn.pgApprovalNo || txn.pgIssuerName || txn.paymentChannel === 'PG') {
                    methodLabel = '카드(PG)';
                }
            } else if (txn.paymentMethod === 'CASH') {
                if (txn.paymentChannel === 'VAN') {
                    methodLabel = '현금(VAN)';
                }
            }
            var createdAt = txn.createdAt ? txn.createdAt.replace('T', ' ').substring(0, 16) : '-';
            var typeLabel = typeLabels[txn.transactionType] || HolaPms.escapeHtml(txn.transactionType || '결제');
            var typeStyle = typeStyles[txn.transactionType] || '';

            // 상태 배지 (유형 오른쪽 인라인)
            var statusHtml = '';
            if (txn.transactionStatus === 'PG_REFUND_FAILED') {
                statusHtml = ' <span class="badge bg-danger ms-1" style="font-size:0.65rem">실패</span>';
            } else if (txn.transactionStatus === 'MANUAL_CONFIRMED') {
                statusHtml = ' <span class="badge badge-manual-refund ms-1" style="font-size:0.65rem">수동확인</span>';
            }

            var hasPg = txn.pgCno || txn.pgApprovalNo || txn.pgIssuerName;
            var hasVan = txn.paymentChannel === 'VAN';
            var hasMemo = txn.memo;

            // ── 내용 컬럼 조립 ──
            var contentParts = [];

            // 거래 상세 (카드사·번호·승인번호 등)
            var details = [];
            if (hasVan && txn.transactionType !== 'REFUND') {
                if (txn.vanIssuerName) details.push(HolaPms.escapeHtml(txn.vanIssuerName));
                if (txn.vanPan) details.push(HolaPms.escapeHtml(txn.vanPan));
                if (txn.vanAuthCode) details.push('승인 ' + HolaPms.escapeHtml(txn.vanAuthCode));
                if (txn.vanAcquirerName) details.push(HolaPms.escapeHtml(txn.vanAcquirerName));
            } else if (hasVan && txn.transactionType === 'REFUND') {
                if (txn.vanIssuerName) details.push(HolaPms.escapeHtml(txn.vanIssuerName));
                if (txn.vanAuthCode) details.push('취소승인 ' + HolaPms.escapeHtml(txn.vanAuthCode));
            } else if (hasPg) {
                if (txn.pgIssuerName) details.push(HolaPms.escapeHtml(txn.pgIssuerName));
                if (txn.pgCardNo) details.push(HolaPms.escapeHtml(txn.pgCardNo));
                if (txn.pgApprovalNo || txn.approvalNo) details.push('승인 ' + HolaPms.escapeHtml(txn.pgApprovalNo || txn.approvalNo));
                if (txn.paymentMethod === 'CARD' && txn.pgInstallmentMonth != null) {
                    details.push(txn.pgInstallmentMonth === 0 ? '일시불' : txn.pgInstallmentMonth + '개월');
                }
            }
            if (details.length > 0) {
                contentParts.push(details.join(' · '));
            }

            // 메모 · 처리자
            var metaParts = [];
            if (hasMemo) metaParts.push(HolaPms.escapeHtml(txn.memo));
            if (txn.createdBy) metaParts.push('처리자: ' + HolaPms.escapeHtml(txn.createdBy));
            if (metaParts.length > 0) {
                contentParts.push(metaParts.join(' · '));
            }

            var contentHtml = '<span class="text-muted">' + contentParts.join('<br>') + '</span>';

            // 액션 버튼
            if (txn.transactionStatus === 'PG_REFUND_FAILED') {
                contentHtml += '<br><button class="btn btn-warning btn-sm mt-1 retry-refund-btn" data-txn-id="' + txn.id + '">'
                    + '<i class="fas fa-redo me-1"></i>PG 환불 재시도</button>';
            }
            if (txn.cancelable) {
                contentHtml += '<br><button class="btn btn-outline-danger btn-sm mt-1 van-cancel-btn" data-txn-id="' + txn.id + '">'
                    + '<i class="fas fa-undo me-1"></i>VAN 취소</button>';
            }

            // 단일 행
            html += '<tr>'
                + '<td class="text-center">' + (idx + 1) + '</td>'
                + '<td class="text-center text-nowrap ' + typeStyle + '">' + typeLabel + statusHtml + '</td>';
            if (isMultiLeg) {
                var legLabel = txn.subReservationId ? (subLabelMap[txn.subReservationId] || '-') : '-';
                html += '<td class="small">' + HolaPms.escapeHtml(legLabel) + '</td>';
            }
            html += '<td class="text-center text-nowrap">' + methodLabel + '</td>'
                + '<td class="txn-content-cell">' + contentHtml + '</td>'
                + '<td class="text-end fw-medium text-nowrap">' + self.formatCurrency(txn.amount) + '</td>'
                + '<td class="text-center text-nowrap small">' + HolaPms.escapeHtml(createdAt) + '</td>'
                + '</tr>';
        });

        html += '</tbody></table>';
        $content.html(html);

        // VAN 취소 버튼 이벤트
        $content.off('click', '.van-cancel-btn').on('click', '.van-cancel-btn', function() {
            var txnId = $(this).data('txn-id');
            self.processVanCancel(txnId);
        });

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
            // 채널 구분 라벨 (PG/VAN)
            var channelSuffix = '';
            if (txn.pgCno || txn.pgApprovalNo || txn.paymentChannel === 'PG') {
                channelSuffix = '(PG)';
            } else if (txn.paymentChannel === 'VAN') {
                channelSuffix = '(VAN)';
            }
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
                line += method + channelSuffix + ' <strong>' + fmt(txn.amount) + '</strong>';
                line += ' <span class="badge badge-manual-refund">수동환불(확인)</span>';
            } else if (txn.paymentChannel === 'VAN') {
                // VAN 거래
                line += method + '(VAN) <strong>' + fmt(txn.amount) + '</strong>';
                if (txn.vanIssuerName) line += ' - ' + esc(txn.vanIssuerName);
                if (txn.vanAuthCode) line += ' (승인번호: ' + esc(txn.vanAuthCode) + ')';
            } else {
                // 수동 현금 등
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
    },

    // === VAN 결제 관련 ===

    /** 캐시된 워크스테이션 목록 */
    _workstations: null,

    /**
     * 워크스테이션 목록 로드
     */
    loadWorkstations: function(callback) {
        var self = this;
        if (self._workstations) {
            callback(self._workstations);
            return;
        }
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/workstations',
            type: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    self._workstations = res.data;
                    callback(res.data);
                } else {
                    callback([]);
                }
            },
            error: function() {
                callback([]);
            }
        });
    },

    /**
     * 선택된 워크스테이션 가져오기 (localStorage 기반)
     */
    getSelectedWorkstation: function(workstations) {
        if (!workstations || workstations.length === 0) return null;
        if (workstations.length === 1) return workstations[0];

        var savedId = localStorage.getItem('hola_workstationId');
        if (savedId) {
            var found = workstations.find(function(ws) { return ws.id == savedId; });
            if (found) return found;
        }
        return null;
    },

    /**
     * VAN 카드결제 모달 열기
     */
    openVanCardPaymentModal: function(legContext) {
        var self = this;
        self._currentLegContext = legContext || null;
        self._vanPaymentType = 'CARD';

        self.loadWorkstations(function(workstations) {
            if (workstations.length === 0) {
                HolaPms.alert('error', '등록된 워크스테이션이 없습니다. 관리자에게 문의하세요.');
                return;
            }

            // 최신 결제 정보 재조회
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment',
                type: 'GET',
                success: function(res) {
                    if (res.success && res.data) {
                        self.paymentData = res.data;

                        var remaining = legContext
                            ? (legContext.legRemaining != null ? Number(legContext.legRemaining) : Number(legContext.legTotal))
                            : Number(res.data.remainingAmount) || 0;

                        if (remaining <= 0) {
                            HolaPms.alert('warning', '결제할 잔액이 없습니다.');
                            return;
                        }

                        // 모달 구성
                        self._buildVanPaymentModal({
                            title: legContext ? legContext.legLabel + ' VAN 카드결제' : 'VAN 카드결제',
                            remaining: remaining,
                            workstations: workstations,
                            showCashReceipt: false
                        });
                    }
                }
            });
        });
    },

    /**
     * VAN 현금결제 모달 열기
     */
    openVanCashPaymentModal: function(legContext) {
        var self = this;
        self._currentLegContext = legContext || null;
        self._vanPaymentType = 'CASH';

        self.loadWorkstations(function(workstations) {
            if (workstations.length === 0) {
                HolaPms.alert('error', '등록된 워크스테이션이 없습니다. 관리자에게 문의하세요.');
                return;
            }

            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment',
                type: 'GET',
                success: function(res) {
                    if (res.success && res.data) {
                        self.paymentData = res.data;

                        var remaining = legContext
                            ? (legContext.legRemaining != null ? Number(legContext.legRemaining) : Number(legContext.legTotal))
                            : Number(res.data.remainingAmount) || 0;

                        if (remaining <= 0) {
                            HolaPms.alert('warning', '결제할 잔액이 없습니다.');
                            return;
                        }

                        self._buildVanPaymentModal({
                            title: legContext ? legContext.legLabel + ' VAN 현금결제' : 'VAN 현금결제',
                            remaining: remaining,
                            workstations: workstations,
                            showCashReceipt: true
                        });
                    }
                }
            });
        });
    },

    /**
     * VAN 결제 모달 생성 (카드/현금 공통)
     */
    _buildVanPaymentModal: function(opts) {
        var self = this;
        var selectedWs = self.getSelectedWorkstation(opts.workstations);

        // 기존 모달 제거
        $('#vanPaymentModal').remove();

        var wsSelectHtml = '';
        if (opts.workstations.length > 1) {
            wsSelectHtml = '<div class="mb-3"><label class="form-label small">워크스테이션</label>'
                + '<select class="form-select form-select-sm" id="vanWsSelect">';
            opts.workstations.forEach(function(ws) {
                var selected = selectedWs && selectedWs.id === ws.id ? ' selected' : '';
                wsSelectHtml += '<option value="' + ws.id + '"' + selected + '>'
                    + HolaPms.escapeHtml(ws.wsName || ws.wsNo) + '</option>';
            });
            wsSelectHtml += '</select></div>';
        }

        var cashReceiptHtml = '';
        if (opts.showCashReceipt) {
            cashReceiptHtml = '<div class="mb-3">'
                + '<div class="form-check form-switch">'
                + '<input class="form-check-input" type="checkbox" id="vanCashReceiptToggle">'
                + '<label class="form-check-label" for="vanCashReceiptToggle">현금영수증 발행</label>'
                + '</div>'
                + '</div>'
                + '<div id="vanCashReceiptFields" class="d-none">'
                + '<div class="mb-3"><label class="form-label small">식별번호 (전화번호 또는 사업자번호)</label>'
                + '<input type="text" class="form-control form-control-sm" id="vanCashReceiptNo" '
                + 'placeholder="010-0000-0000">'
                + '</div></div>';
        }

        var modalHtml = '<div class="modal fade" id="vanPaymentModal" tabindex="-1">'
            + '<div class="modal-dialog"><div class="modal-content">'
            + '<div class="modal-header"><h6 class="modal-title">' + HolaPms.escapeHtml(opts.title) + '</h6>'
            + '<button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>'
            + '<div class="modal-body">'
            + wsSelectHtml
            + '<div class="mb-3"><label class="form-label small">결제 금액</label>'
            + '<input type="number" class="form-control form-control-sm" id="vanPaymentAmount" '
            + 'value="' + Math.floor(opts.remaining) + '"></div>'
            + cashReceiptHtml
            + '<div class="mb-3"><label class="form-label small">메모</label>'
            + '<input type="text" class="form-control form-control-sm" id="vanPaymentMemo"></div>'
            + '</div>'
            + '<div class="modal-footer">'
            + '<button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">'
            + '<i class="fas fa-times me-1"></i>취소</button>'
            + '<button type="button" class="btn btn-primary btn-sm" id="vanPaymentConfirmBtn">'
            + '<i class="fas fa-credit-card me-1"></i>결제</button>'
            + '</div></div></div></div>';

        $('body').append(modalHtml);

        // 현금영수증 토글 이벤트
        if (opts.showCashReceipt) {
            $('#vanCashReceiptToggle').on('change', function() {
                $('#vanCashReceiptFields').toggleClass('d-none', !this.checked);
            });
        }

        // 워크스테이션 선택 저장
        if (opts.workstations.length > 1) {
            $('#vanWsSelect').on('change', function() {
                localStorage.setItem('hola_workstationId', $(this).val());
            });
        }

        // 결제 확인 버튼
        $('#vanPaymentConfirmBtn').on('click', function() {
            self._processVanPayment(opts.workstations);
        });

        HolaPms.modal.show('#vanPaymentModal');
    },

    /**
     * VAN 결제 처리 (KPSP 직접 호출 → 백엔드 저장)
     */
    _processVanPayment: function(workstations) {
        var self = this;
        var amount = parseInt($('#vanPaymentAmount').val());
        var memo = $.trim($('#vanPaymentMemo').val());

        if (!amount || amount <= 0) {
            HolaPms.alert('warning', '결제 금액을 입력해주세요.');
            return;
        }

        // 현금영수증 검증
        var issueCashReceipt = self._vanPaymentType === 'CASH' && $('#vanCashReceiptToggle').is(':checked');
        var cashReceiptNo = '';
        if (issueCashReceipt) {
            cashReceiptNo = $.trim($('#vanCashReceiptNo').val());
            if (!cashReceiptNo) {
                HolaPms.alert('warning', '현금영수증 식별번호를 입력해주세요.');
                return;
            }
        }

        // 워크스테이션 결정
        var ws;
        if (workstations.length === 1) {
            ws = workstations[0];
        } else {
            var wsId = $('#vanWsSelect').val();
            ws = workstations.find(function(w) { return w.id == wsId; });
        }
        if (!ws) {
            HolaPms.alert('error', '워크스테이션을 선택해주세요.');
            return;
        }

        // 버튼 비활성화
        var $btn = $('#vanPaymentConfirmBtn');
        $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span>처리 중...');
        // 모달 닫기 방지
        $('#vanPaymentModal').find('.btn-close, [data-bs-dismiss]').prop('disabled', true);

        // 1. 시퀀스 번호 발급
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                + '/payment/next-van-sequence?workstationId=' + ws.id,
            type: 'GET',
            success: function(seqRes) {
                if (!seqRes.success) {
                    self._resetVanPaymentButton($btn);
                    HolaPms.alert('error', '시퀀스 번호 발급에 실패했습니다.');
                    return;
                }
                var sequenceNo = seqRes.data;

                // 2. PMS 백엔드 프록시를 통해 KPSP 호출 (CORS 우회)
                var proxyEndpoint = self._vanPaymentType === 'CARD' ? '/sales/card' : '/sales/cash';
                var proxyUrl = '/api/v1/properties/' + self.propertyId + '/van/proxy' + proxyEndpoint
                    + '?workstationId=' + ws.id;
                var now = new Date();
                var transDateTime = now.toISOString().split('.')[0] + '+09:00';

                var kpspBody = {
                    sequenceNo: sequenceNo,
                    transType: 'SALE',
                    transAmount: amount,
                    taxAmount: Math.floor(amount / 11),
                    transCurrency: 'KRW',
                    transDateTime: transDateTime,
                    siteId: 'HOLA',
                    wsNo: ws.wsNo,
                    operator: 'frontdesk'
                };

                // 현금영수증 시 guestNo에 전화번호
                if (issueCashReceipt && cashReceiptNo) {
                    kpspBody.guestNo = cashReceiptNo;
                }

                // 예약 정보 추가 (선택적 enrichment)
                if (self.reservationData) {
                    var subs = self.reservationData.subReservations || [];
                    if (subs.length > 0) {
                        kpspBody.roomNo = subs[0].roomNumber || '';
                        kpspBody.roomType = 1;
                    }
                    kpspBody.guestName = self.reservationData.guestName || '';
                    kpspBody.checkInDate = self.reservationData.checkInDate || '';
                    kpspBody.checkOutDate = self.reservationData.checkOutDate || '';
                }

                if (self._vanPaymentType === 'CARD') {
                    $btn.html('<span class="spinner-border spinner-border-sm me-1"></span>단말기에서 카드를 읽는 중...');
                } else {
                    $btn.html('<span class="spinner-border spinner-border-sm me-1"></span>현금영수증 처리 중...');
                }

                HolaPms.ajax({
                    url: proxyUrl,
                    type: 'POST',
                    data: kpspBody,
                    timeout: 65000,
                    success: function(kpspResult) {
                        if (!kpspResult.success || kpspResult.respCode !== '0000') {
                            self._resetVanPaymentButton($btn);
                            var errMsg = kpspResult.respMessage || kpspResult.respText || '결제가 거절되었습니다.';
                            HolaPms.alert('error', errMsg);
                            return;
                        }

                        // 3. PMS 백엔드에 결과 저장
                        var requestData = {
                            paymentMethod: self._vanPaymentType,
                            amount: amount,
                            memo: memo || null,
                            subReservationId: (self._currentLegContext && self._currentLegContext.subId) || null,
                            paymentChannel: 'VAN',
                            workstationId: ws.id,
                            vanResult: kpspResult
                        };

                        HolaPms.ajax({
                            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId + '/payment/transactions',
                            type: 'POST',
                            data: requestData,
                            success: function(res) {
                                if (res.success) {
                                    HolaPms.alert('success', 'VAN ' + (self._vanPaymentType === 'CARD' ? '카드' : '현금') + ' 결제가 완료되었습니다.');
                                    HolaPms.modal.hide('#vanPaymentModal');
                                    if (res.data) {
                                        self.paymentData = res.data;
                                        self.bindSummary(res.data);
                                        self.renderChargeBreakdown();
                                        self.renderAdjustments(res.data.adjustments || []);
                                        self.renderPaymentTransactions(res.data.transactions || []);
                                    }
                                }
                            },
                            error: function() {
                                localStorage.setItem('hola_van_pending_' + sequenceNo, JSON.stringify(requestData));
                                self._resetVanPaymentButton($btn);
                                HolaPms.alert('error', '결제는 승인되었으나 저장에 실패했습니다. 페이지를 새로고침하고 다시 시도해주세요.');
                            }
                        });
                    },
                    error: function() {
                        self._resetVanPaymentButton($btn);
                        HolaPms.alert('error', '단말기에 연결할 수 없습니다. KPSP 서비스 실행 상태를 확인하세요.');
                    }
                });
            },
            error: function() {
                self._resetVanPaymentButton($btn);
                HolaPms.alert('error', '시퀀스 번호 발급에 실패했습니다.');
            }
        });
    },

    /**
     * VAN 결제 버튼 원복
     */
    _resetVanPaymentButton: function($btn) {
        $btn.prop('disabled', false).html('<i class="fas fa-credit-card me-1"></i>결제');
        $('#vanPaymentModal').find('.btn-close, [data-bs-dismiss]').prop('disabled', false);
    },

    /**
     * VAN 취소 처리
     */
    processVanCancel: function(txnId) {
        var self = this;

        if (!confirm('이 거래를 취소하시겠습니까? 카드사/현금영수증 취소가 함께 진행됩니다.')) {
            return;
        }

        // 1. 취소 정보 조회
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                + '/payment/transactions/' + txnId + '/van-cancel-info',
            type: 'GET',
            success: function(infoRes) {
                if (!infoRes.success || !infoRes.data) {
                    HolaPms.alert('error', '취소 정보 조회에 실패했습니다.');
                    return;
                }
                var info = infoRes.data;

                // KPSP 취소 프록시 호출
                var isCard = info.paymentMethod === 'CARD';
                var proxyEndpoint = isCard ? '/refund/card' : '/refund/cash';
                var proxyUrl = '/api/v1/properties/' + self.propertyId + '/van/proxy' + proxyEndpoint
                    + '?workstationId=' + (info.workstationId || txnId);
                var cancelTransType = isCard ? 'I1' : 'B1';

                var now = new Date();
                var transDateTime = now.toISOString().split('.')[0] + '+09:00';

                var cancelBody = {
                    sequenceNo: info.sequenceNo,
                    transType: cancelTransType,
                    transAmount: Number(info.amount),
                    taxAmount: Math.floor(Number(info.amount) / 11),
                    transCurrency: 'KRW',
                    transDateTime: transDateTime,
                    siteId: 'HOLA',
                    wsNo: info.wsNo || 'ADMIN',
                    operator: 'frontdesk'
                };

                // VAN 취소 결과를 PMS에 저장하는 공통 함수
                var saveVanCancelResult = function(result) {
                    HolaPms.ajax({
                        url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                            + '/payment/transactions/' + txnId + '/van-cancel',
                        type: 'POST',
                        data: result,
                        success: function(res) {
                            if (res.success) {
                                HolaPms.alert('success', 'VAN 결제가 취소되었습니다.');
                                if (res.data) {
                                    self.paymentData = res.data;
                                    self.bindSummary(res.data);
                                    self.renderChargeBreakdown();
                                    self.renderAdjustments(res.data.adjustments || []);
                                    self.renderPaymentTransactions(res.data.transactions || []);
                                    self.renderCancelInfo(res.data);
                                }
                            }
                        },
                        error: function() {
                            localStorage.setItem('hola_van_cancel_pending_' + txnId, JSON.stringify(result));
                            HolaPms.alert('error', '취소는 완료되었으나 시스템 저장에 실패했습니다. 관리자에게 문의하세요.');
                        }
                    });
                };

                // 수동 확인 처리 함수
                var processManualConfirm = function() {
                    if (!confirm('VAN 단말기 영수증/이력에서 취소 완료를 확인하셨습니까?\n확인 후 수동으로 환불 처리합니다.')) {
                        return;
                    }
                    // 수동 확인용 payload (respCode=0000 대신 MANUAL_CONFIRMED 상태 전달)
                    var manualPayload = {
                        respCode: 'MANUAL',
                        authCode: info.authCode || '',
                        rrn: info.rrn || '',
                        pan: '',
                        sequenceNo: info.sequenceNo || ''
                    };
                    HolaPms.ajax({
                        url: '/api/v1/properties/' + self.propertyId + '/reservations/' + self.reservationId
                            + '/payment/transactions/' + txnId + '/van-cancel-manual',
                        type: 'POST',
                        data: manualPayload,
                        success: function(res) {
                            if (res.success) {
                                HolaPms.alert('success', 'VAN 취소가 수동 확인 처리되었습니다.');
                                if (res.data) {
                                    self.paymentData = res.data;
                                    self.bindSummary(res.data);
                                    self.renderChargeBreakdown();
                                    self.renderAdjustments(res.data.adjustments || []);
                                    self.renderPaymentTransactions(res.data.transactions || []);
                                    self.renderCancelInfo(res.data);
                                }
                            }
                        }
                    });
                };

                // 2. PMS 프록시를 통해 KPSP 취소 호출
                HolaPms.ajax({
                    url: proxyUrl,
                    type: 'POST',
                    data: cancelBody,
                    timeout: 65000,
                    success: function(cancelResult) {
                        if (!cancelResult.success || cancelResult.respCode !== '0000') {
                            // 취소 실패 — VAN측에서 이미 취소되었을 수 있으므로 수동 확인 옵션 제공
                            var errMsg = cancelResult.respMessage || cancelResult.respText || '취소 응답 실패';
                            var confirmManual = confirm(
                                'VAN 취소 응답: ' + errMsg + '\n\n'
                                + 'VAN 단말기에서 이미 취소가 완료되었을 수 있습니다.\n'
                                + '단말기 영수증/이력을 확인 후 수동 처리하시겠습니까?\n\n'
                                + '[확인] 수동 취소 처리  [취소] 취소 중단'
                            );
                            if (confirmManual) {
                                processManualConfirm();
                            }
                            return;
                        }

                        // 3. 정상 취소 — PMS 백엔드에 결과 저장
                        saveVanCancelResult(cancelResult);
                    },
                    error: function() {
                        var confirmManual = confirm(
                            '단말기에 연결할 수 없습니다.\n\n'
                            + '이미 취소가 완료된 상태라면 수동으로 처리할 수 있습니다.\n'
                            + '단말기 영수증/이력을 확인 후 수동 처리하시겠습니까?\n\n'
                            + '[확인] 수동 취소 처리  [취소] 취소 중단'
                        );
                        if (confirmManual) {
                            processManualConfirm();
                        }
                    }
                });
            }
        });
    }
};
