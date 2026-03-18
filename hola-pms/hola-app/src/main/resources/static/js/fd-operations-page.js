/**
 * 프론트데스크 운영현황 페이지
 * 상태 필터 + DataTable + Offcanvas Drawer (예약 상세 + 객실 배정 인라인)
 *
 * 상태 전이 매트릭스:
 *   예약(RESERVED) → 체크인(CHECK_IN), 취소(CANCELED), 노쇼(NO_SHOW)
 *   체크인(CHECK_IN) → 투숙중(INHOUSE), 취소(CANCELED)
 *   투숙중(INHOUSE) → 체크아웃(CHECKED_OUT)
 *   체크아웃/취소/노쇼 → (종료 상태)
 */
var FdOperations = {
    propertyId: null,
    dataTable: null,
    allData: [],
    specialFilter: null,
    summaryData: null,
    pollInterval: null,
    drawerInstance: null,       // Bootstrap Offcanvas 인스턴스
    currentReservationId: null, // Drawer에 표시 중인 예약 ID
    currentReservationData: null, // Drawer에 표시 중인 예약 데이터

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () {
            self.hideDrawer();
            self.reload();
        });

        $('#btnRefresh').on('click', function () { self.reload(); });

        // 상태 필터 버튼
        $('#statusFilterGroup').on('click', 'button[data-status]', function () {
            self.specialFilter = null;
            $('#statusFilterGroup button').removeClass('active').css('border-color', 'transparent').css('box-shadow', 'none');
            $(this).addClass('active').css('border-color', '#051923').css('box-shadow', '0 0 0 1px #051923');
            self.applyFilter();
        });

        // 요약 카드 클릭 → 서버 API 직접 호출 (summary 카운트와 일치 보장)
        $('#summaryCards .card[data-filter]').on('click', function () {
            var filter = $(this).data('filter');
            self.specialFilter = filter;
            $('#statusFilterGroup button').removeClass('active').css('border-color', 'transparent').css('box-shadow', 'none');
            self.loadSpecialFilter(filter);
        });

        // 검색
        $('#searchBtn').on('click', function () { self.applyFilter(); });
        $('#keyword').on('keypress', function (e) { if (e.which === 13) self.applyFilter(); });

        // 초기화
        $('#resetBtn').on('click', function () {
            self.specialFilter = null;
            $('#statusFilterGroup button').removeClass('active').css('border-color', 'transparent').css('box-shadow', 'none');
            var $all = $('#statusFilterGroup button[data-status=""]');
            $all.addClass('active').css('border-color', '#051923').css('box-shadow', '0 0 0 1px #051923');
            $('#keyword').val('');
            self.applyFilter();
        });

        // Quick Action — Leg 단위 상태 변경
        $(document).on('click', '.btn-checkin', function (e) {
            e.preventDefault(); e.stopPropagation();
            self.changeStatus($(this).data('reservation-id'), 'CHECK_IN', '체크인 처리하시겠습니까?', $(this).data('sub-id'));
        });
        $(document).on('click', '.btn-inhouse', function (e) {
            e.preventDefault(); e.stopPropagation();
            self.changeStatus($(this).data('reservation-id'), 'INHOUSE', '투숙중으로 변경하시겠습니까?', $(this).data('sub-id'));
        });
        $(document).on('click', '.btn-checkout', function (e) {
            e.preventDefault(); e.stopPropagation();
            self.changeStatus($(this).data('reservation-id'), 'CHECKED_OUT', '체크아웃 처리하시겠습니까?', $(this).data('sub-id'));
        });
        $(document).on('click', '.btn-noshow', function (e) {
            e.preventDefault(); e.stopPropagation();
            var rid = $(this).data('reservation-id');
            self.showCancelPreview(rid, true);
        });
        $(document).on('click', '.btn-cancel', function (e) {
            e.preventDefault(); e.stopPropagation();
            var rid = $(this).data('reservation-id');
            self.showCancelPreview(rid, false);
        });
        // 결제 버튼 — 준비중
        $(document).on('click', '.btn-payment-inline', function (e) {
            e.preventDefault(); e.stopPropagation();
            HolaPms.alert('info', '결제 기능은 준비중입니다.');
        });
        $(document).on('click', '.btn-detail', function (e) {
            e.preventDefault(); e.stopPropagation();
            self.openReservationDetail($(this).data('reservation-id'));
        });

        // 객실 배정 인라인 — 토글
        $(document).on('click', '.btn-assign-room', function (e) {
            e.preventDefault(); e.stopPropagation();
            var subId = $(this).data('sub-id');
            var $panel = $('#roomAssignPanel-' + subId);
            if ($panel.is(':visible')) {
                $panel.slideUp(200);
            } else {
                self.expandRoomAssign(subId);
            }
        });

        // 객실 배정 — 객실 선택
        $(document).on('click', '.room-assign-panel .room-option:not(.disabled)', function (e) {
            e.preventDefault();
            var $panel = $(this).closest('.room-assign-panel');
            $panel.find('.room-option').removeClass('selected');
            $(this).addClass('selected');
            $panel.find('.btn-confirm-assign').prop('disabled', false);
        });

        // 객실 배정 — 확정
        $(document).on('click', '.btn-confirm-assign', function (e) {
            e.preventDefault();
            var subId = $(this).data('sub-id');
            self.confirmRoomAssign(subId);
        });

        // 객실 배정 — 취소
        $(document).on('click', '.btn-cancel-assign', function (e) {
            e.preventDefault();
            var subId = $(this).data('sub-id');
            $('#roomAssignPanel-' + subId).slideUp(200);
        });
    },

    reload: function () {
        this.propertyId = HolaPms.context.getPropertyId();
        if (!this.propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#summaryCards, #filterCard, #tableCard').addClass('d-none');
            this.stopPolling();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#summaryCards, #filterCard, #tableCard').removeClass('d-none');
        this.loadAllData();
        this.startPolling();
    },

    // ========== 데이터 로드 ==========

    loadAllData: function () {
        var self = this;
        var pid = this.propertyId;
        var completed = 0;

        function checkDone() {
            completed++;
            if (completed < 2) return;

            self.updateSummary();
            // specialFilter가 활성이면 서버 API로 재조회, 아니면 allData 기반 필터
            if (self.specialFilter) {
                self.loadSpecialFilter(self.specialFilter);
            } else {
                self.applyFilter();
            }
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + pid + '/front-desk/all',
            type: 'GET',
            success: function (res) { self.allData = res.data || []; checkDone(); },
            error: function () { self.allData = []; checkDone(); }
        });

        HolaPms.ajax({
            url: '/api/v1/properties/' + pid + '/front-desk/summary',
            type: 'GET',
            success: function (res) {
                if (res.data) self.summaryData = res.data;
                checkDone();
            },
            error: function () { self.summaryData = null; checkDone(); }
        });
    },

    updateSummary: function () {
        if (this.summaryData) {
            $('#summaryArrivals').text(this.summaryData.arrivals || 0);
            $('#summaryInHouse').text(this.summaryData.inHouse || 0);
            $('#summaryDepartures').text(this.summaryData.departures || 0);
        }
        $('#summaryTotal').text(this.allData.length);
    },

    /**
     * 요약 카드 클릭 시 서버 API 직접 호출
     * allData(오늘 관련 건)와 summary(전체 카운트) 범위가 달라서
     * 카드 클릭 시에는 전용 API로 정확한 데이터를 로드해야 함
     */
    loadSpecialFilter: function (filter) {
        var self = this;
        var pid = this.propertyId;
        if (!pid) return;

        // "전체" 클릭 시 specialFilter 해제 → allData 표시
        if (filter === 'all') {
            self.specialFilter = null;
            self.renderTable(self.allData);
            return;
        }

        var endpointMap = {
            arrivals: '/api/v1/properties/' + pid + '/front-desk/arrivals',
            inhouse: '/api/v1/properties/' + pid + '/front-desk/in-house',
            departures: '/api/v1/properties/' + pid + '/front-desk/departures'
        };
        var url = endpointMap[filter];
        if (!url) { self.applyFilter(); return; }

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function (res) {
                var data = res.data || [];
                self.renderTable(data);
            },
            error: function () {
                self.renderTable([]);
            }
        });
    },

    applyFilter: function () {
        // specialFilter가 활성 상태이면 이미 loadSpecialFilter에서 처리됨
        if (this.specialFilter) return;

        var status = $('#statusFilterGroup button.active').data('status') || '';
        var filtered = this.allData.slice();
        if (status) {
            filtered = filtered.filter(function (d) {
                return d.roomReservationStatus === status || d.reservationStatus === status;
            });
        }

        var keyword = $.trim($('#keyword').val()).toLowerCase();
        if (keyword) {
            filtered = filtered.filter(function (d) {
                return (d.masterReservationNo && d.masterReservationNo.toLowerCase().indexOf(keyword) >= 0) ||
                       (d.guestNameKo && d.guestNameKo.toLowerCase().indexOf(keyword) >= 0) ||
                       (d.phoneNumber && d.phoneNumber.indexOf(keyword) >= 0) ||
                       (d.roomNumber && d.roomNumber.indexOf(keyword) >= 0);
            });
        }

        this.renderTable(filtered);
    },

    /**
     * DataTable에 데이터 렌더링 (applyFilter / loadSpecialFilter 공용)
     */
    renderTable: function (data) {
        if (this.dataTable) {
            this.dataTable.clear();
            this.dataTable.rows.add(data);
            this.dataTable.draw();
        } else {
            this.initTable(data);
        }
    },

    // ========== DataTable ==========

    initTable: function (data) {
        var self = this;
        this.dataTable = $('#operationTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            data: data,
            columns: [
                { data: 'masterReservationNo', render: this.renderConfirmNo, className: 'text-center' },
                { data: 'guestNameKo', render: HolaPms.renders.dashIfEmpty, className: 'text-center' },
                { data: 'roomTypeName', render: HolaPms.renders.dashIfEmpty, className: 'text-center' },
                { data: 'roomNumber', render: this.renderRoomNumber, className: 'text-center' },
                { data: null, render: this.renderPersons, className: 'text-center' },
                { data: 'checkIn', className: 'text-center' },
                { data: 'checkOut', className: 'text-center' },
                { data: 'nights', className: 'text-center' },
                { data: 'roomReservationStatus', render: this.renderStatusBadge, className: 'text-center' },
                { data: 'paymentStatus', render: this.renderPaymentBadge, className: 'text-center' }
            ],
            pageLength: 20,
            dom: 'rtip'
        }));

        // 행 클릭 → Drawer 열기
        $('#operationTable tbody').on('click', 'tr', function () {
            var rowData = self.dataTable.row(this).data();
            if (rowData && rowData.reservationId) {
                self.openReservationDetail(rowData.reservationId);
            }
        });

        $('#operationTable tbody').css('cursor', 'pointer');
    },

    // ========== 렌더러 ==========

    renderConfirmNo: function (data, type, row) {
        if (!data) return '-';
        var display = HolaPms.escapeHtml(data);
        if (row.subReservationNo) {
            var parts = row.subReservationNo.split('-');
            var legNum = parts.length > 0 ? parts[parts.length - 1] : '';
            if (legNum) {
                display += ' <span class="text-muted small">(L' + parseInt(legNum, 10) + ')</span>';
            }
        }
        return '<a href="#" class="text-decoration-none btn-detail" data-reservation-id="' + row.reservationId + '" style="color:#0582CA;">' +
               display + '</a>';
    },

    renderRoomNumber: function (data) {
        if (!data) return '<span class="badge bg-secondary">미배정</span>';
        return '<span class="badge bg-dark">' + HolaPms.escapeHtml(data) + '</span>';
    },

    renderPersons: function (data, type, row) {
        var text = (row.adults || 0) + '명';
        if (row.children > 0) text += ' + 아동 ' + row.children;
        return text;
    },

    renderPaymentBadge: function (data) {
        if (data === 'PAID') return '<span class="badge bg-success">완료</span>';
        if (data === 'UNPAID') return '<span class="badge bg-danger">미결</span>';
        return '<span class="badge bg-secondary">-</span>';
    },

    STATUS_MAP: {
        'RESERVED':    { label: '예약',     bg: '#0582CA' },
        'CHECK_IN':    { label: '체크인',   bg: '#17a2b8' },
        'INHOUSE':     { label: '투숙중',   bg: '#003554' },
        'CHECKED_OUT': { label: '체크아웃', bg: '#6c757d' },
        'CANCELED':    { label: '취소',     bg: '#EF476F' },
        'NO_SHOW':     { label: '노쇼',     bg: '#ffc107', color: '#000' }
    },

    renderStatusBadge: function (data) {
        var info = FdOperations.STATUS_MAP[data] || { label: data || '-', bg: '#6c757d' };
        var textColor = info.color || '#fff';
        return '<span class="badge" style="background-color:' + info.bg + '; color:' + textColor + '">' + info.label + '</span>';
    },

    // ========== 상태 변경 ==========

    changeStatus: function (reservationId, newStatus, message, subReservationId) {
        // message가 null이면 이미 확인 완료 (수수료 미리보기 모달에서 확인)
        if (message && !confirm(message)) return;
        var self = this;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + reservationId + '/status',
            type: 'PUT',
            data: { newStatus: newStatus, subReservationId: subReservationId || null },
            success: function (res) {
                if (res.success) {
                    var labelMap = { CHECK_IN: '체크인', INHOUSE: '투숙중', CHECKED_OUT: '체크아웃', NO_SHOW: '노쇼' };
                    HolaPms.alert('success', (labelMap[newStatus] || newStatus) + ' 처리가 완료되었습니다.');
                    self.reload();
                    // Drawer가 열려있으면 새로고침
                    if (self.isDrawerOpen()) {
                        self.openReservationDetail(reservationId);
                    }
                }
            },
            error: function (xhr) {
                var res = xhr.responseJSON;
                var msg = res && res.message ? res.message : '처리 중 오류가 발생했습니다.';
                var code = res && res.code ? res.code : '';

                var codeTabMap = {
                    'HOLA-4029': 'payment',
                    'HOLA-5001': 'detail',
                    'HOLA-5003': 'detail',
                    'HOLA-5010': 'payment'
                };
                if (codeTabMap[code]) {
                    if (confirm(msg + '\n\n예약정보 수정 페이지로 이동하시겠습니까?')) {
                        var hotelId = HolaPms.context.getHotelId() || '';
                        var propId = self.propertyId || '';
                        var tab = codeTabMap[code];
                        window.open('/admin/reservations/' + reservationId + '?hotelId=' + hotelId + '&propertyId=' + propId + '&tab=' + tab, '_blank');
                    }
                } else {
                    HolaPms.alert('error', msg);
                }
            }
        });
    },

    // ========== Drawer (Offcanvas) ==========

    showDrawer: function () {
        if (!this.drawerInstance) {
            this.drawerInstance = new bootstrap.Offcanvas(document.getElementById('reservationDrawer'));
        }
        this.drawerInstance.show();
    },

    hideDrawer: function () {
        if (this.drawerInstance) {
            this.drawerInstance.hide();
        }
    },

    isDrawerOpen: function () {
        return $('#reservationDrawer').hasClass('show') || $('#reservationDrawer').hasClass('showing');
    },

    openReservationDetail: function (reservationId) {
        var self = this;
        this.currentReservationId = reservationId;

        var $body = $('#drawerBody');
        $body.html('<div class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></div>');
        $('#drawerBtnCheckin, #drawerBtnInhouse, #drawerBtnCheckout').addClass('d-none');

        var hotelId = HolaPms.context.getHotelId() || '';
        var propId = self.propertyId || '';
        $('#drawerFullDetailLink').attr('href', '/admin/reservations/' + reservationId + '?hotelId=' + hotelId + '&propertyId=' + propId);
        this.showDrawer();

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + reservationId,
            type: 'GET',
            success: function (res) {
                if (!res.success || !res.data) {
                    $body.html('<div class="p-3"><div class="alert alert-warning mb-0">데이터를 불러올 수 없습니다.</div></div>');
                    return;
                }
                self.currentReservationData = res.data;
                self.renderDetailDrawer(res.data);
            },
            error: function () {
                $body.html('<div class="p-3"><div class="alert alert-danger mb-0">조회 중 오류가 발생했습니다.</div></div>');
            }
        });
    },

    renderDetailDrawer: function (d) {
        var self = this;
        var status = d.reservationStatus;
        $('#drawerConfirmNo').text(d.masterReservationNo || '');

        // 마스터 레벨 액션 버튼
        $('#drawerBtnCheckin, #drawerBtnInhouse, #drawerBtnCheckout, #drawerBtnCancel, #drawerBtnNoshow').addClass('d-none').off('click');
        if (status === 'RESERVED') {
            $('#drawerBtnCancel').removeClass('d-none').on('click', function () {
                self.showCancelPreview(d.id, false);
            });
            $('#drawerBtnNoshow').removeClass('d-none').on('click', function () {
                self.showCancelPreview(d.id, true);
            });
            $('#drawerBtnCheckin').removeClass('d-none').on('click', function () {
                self.changeStatus(d.id, 'CHECK_IN', '전체 객실 체크인 처리하시겠습니까?');
            });
        } else if (status === 'CHECK_IN') {
            $('#drawerBtnCancel').removeClass('d-none').on('click', function () {
                self.showCancelPreview(d.id, false);
            });
            $('#drawerBtnInhouse').removeClass('d-none').on('click', function () {
                self.changeStatus(d.id, 'INHOUSE', '전체 객실 투숙중으로 변경하시겠습니까?');
            });
        } else if (status === 'INHOUSE') {
            $('#drawerBtnCheckout').removeClass('d-none').on('click', function () {
                self.changeStatus(d.id, 'CHECKED_OUT', '전체 객실 체크아웃 처리하시겠습니까?');
            });
        }

        var statusBadge = FdOperations.renderStatusBadge(status);
        var subs = d.subReservations || [];
        var payment = d.payment || {};
        var grandTotal = payment.grandTotal || 0;
        var paidAmount = payment.totalPaidAmount || 0;
        var remaining = grandTotal - paidAmount;

        var html = '<div class="p-3">';

        // 예약 기본 정보
        html += '<div class="d-flex justify-content-between align-items-start mb-2">';
        html += '<div><small class="text-secondary">예약번호</small><div>' + HolaPms.escapeHtml(d.masterReservationNo || '') + '</div></div>';
        html += '<div class="text-end"><small class="text-secondary">상태</small><div>' + statusBadge + '</div></div>';
        html += '</div>';
        html += '<div class="d-flex gap-3 mb-3">';
        html += '<div><small class="text-secondary">예약자</small><div>' + HolaPms.escapeHtml(d.guestNameKo || '') + '</div></div>';
        html += '<div><small class="text-secondary">연락처</small><div>' + HolaPms.escapeHtml(d.phoneNumber || '-') + '</div></div>';
        html += '<div><small class="text-secondary">이메일</small><div>' + HolaPms.escapeHtml(d.email || '-') + '</div></div>';
        html += '</div>';

        // 객실(Leg) 카드 목록
        html += '<div class="border-top pt-3 mb-2">';
        html += '<h6 class="fw-bold mb-2"><i class="fas fa-bed me-1"></i>객실 목록 (' + subs.length + ')</h6>';
        html += '</div>';

        if (subs.length === 0) {
            html += '<div class="text-muted small mb-3">등록된 객실이 없습니다.</div>';
        } else {
            subs.forEach(function (sub, idx) {
                html += self.renderLegCard(d, sub, idx);
            });
        }

        // 결제 요약
        html += '<div class="border-top pt-3 mb-2">';
        html += '<h6 class="fw-bold mb-2"><i class="fas fa-credit-card me-1"></i>결제</h6>';
        html += '</div>';

        html += '<div class="drawer-payment-summary mb-3">';
        html += '<div class="pay-item"><div class="pay-label">총액</div><div class="pay-value">' + Number(grandTotal).toLocaleString('ko-KR') + '원</div></div>';
        html += '<div class="pay-item"><div class="pay-label">결제액</div><div class="pay-value">' + Number(paidAmount).toLocaleString('ko-KR') + '원</div></div>';
        html += '<div class="pay-item"><div class="pay-label">잔액</div><div class="pay-value" style="color:' + (remaining > 0 ? '#EF476F' : '#28a745') + ';">' + Number(remaining).toLocaleString('ko-KR') + '원</div></div>';
        html += '</div>';

        // 잔액 > 0이면 결제 버튼 (인라인, 준비중)
        if (remaining > 0) {
            html += '<div class="d-flex justify-content-end gap-2 mb-3">';
            html += '<button class="btn btn-sm btn-outline-primary btn-payment-inline"><i class="fas fa-credit-card me-1"></i>카드결제</button>';
            html += '<button class="btn btn-sm btn-outline-success btn-payment-inline"><i class="fas fa-money-bill me-1"></i>현금결제</button>';
            html += '</div>';
        }

        // 고객 요청
        if (d.customerRequest) {
            html += '<div class="border-top pt-3 mb-2"><small class="text-secondary">고객 요청</small>';
            html += '<div class="p-2 bg-light rounded mt-1 small">' + HolaPms.escapeHtml(d.customerRequest) + '</div></div>';
        }

        // 메모
        if (d.memos && d.memos.length > 0) {
            html += '<div class="border-top pt-3 mb-2"><small class="text-secondary">메모 (' + d.memos.length + ')</small></div>';
            d.memos.slice(0, 3).forEach(function (m) {
                html += '<div class="p-2 bg-light rounded mt-1 small">';
                html += '<span class="text-muted">' + (m.createdAt ? m.createdAt.substring(0, 16) : '') + ' ' + HolaPms.escapeHtml(m.createdBy || '') + '</span><br>';
                html += HolaPms.escapeHtml(m.content || '');
                html += '</div>';
            });
        }

        html += '</div>'; // .p-3 닫기
        $('#drawerBody').html(html);
    },

    /**
     * Leg 카드 렌더링 — 상태별 액션 + 미배정 시 객실 배정 영역
     */
    renderLegCard: function (reservation, sub, idx) {
        var persons = (sub.adults || 0) + '명';
        if (sub.children > 0) persons += ' + 아동 ' + sub.children;
        var ss = sub.roomReservationStatus;
        var statusBadge = FdOperations.renderStatusBadge(ss);
        var btnAttr = 'data-reservation-id="' + reservation.id + '" data-sub-id="' + sub.id + '"';
        var isUnassigned = !sub.roomNumberId;

        var html = '<div class="drawer-leg-card" id="legCard-' + sub.id + '">';

        // 헤더: Leg 번호 + 객실타입
        html += '<div class="leg-header">';
        html += '<span style="color:#003554;">Leg ' + (idx + 1) + ' · ' + HolaPms.escapeHtml(sub.roomTypeName || '-') + '</span>';
        html += statusBadge;
        html += '</div>';

        // 정보: 객실번호, 날짜, 인원
        html += '<div class="leg-info">';
        if (isUnassigned) {
            html += '<span class="badge bg-secondary">미배정</span>';
        } else {
            html += '<span class="badge bg-dark">' + HolaPms.escapeHtml(sub.roomNumber) + '</span>';
        }
        html += '<span>' + (sub.checkIn || '-') + ' ~ ' + (sub.checkOut || '-') + '</span>';
        if (sub.nights) html += '<span>' + sub.nights + '박</span>';
        html += '<span>' + persons + '</span>';
        html += '</div>';

        // 액션 버튼 영역
        html += '<div class="leg-actions">';
        // 미배정이면 객실 배정 버튼
        if (isUnassigned && (ss === 'RESERVED' || ss === 'CHECK_IN')) {
            html += '<button class="btn btn-sm btn-outline-primary btn-assign-room" data-sub-id="' + sub.id + '">';
            html += '<i class="fas fa-door-open me-1"></i>객실 배정</button>';
        }
        // 상태 전환 버튼
        if (ss === 'RESERVED') {
            html += '<button class="btn btn-sm btn-outline-secondary btn-cancel" ' + btnAttr + '><i class="fas fa-times me-1"></i>취소</button>';
            html += '<button class="btn btn-sm btn-outline-warning btn-noshow" ' + btnAttr + '><i class="fas fa-user-times me-1"></i>노쇼</button>';
            html += '<button class="btn btn-sm btn-primary btn-checkin" ' + btnAttr + '><i class="fas fa-key me-1"></i>체크인</button>';
        } else if (ss === 'CHECK_IN') {
            html += '<button class="btn btn-sm btn-outline-secondary btn-cancel" ' + btnAttr + '><i class="fas fa-times me-1"></i>취소</button>';
            html += '<button class="btn btn-sm btn-dark btn-inhouse" ' + btnAttr + '><i class="fas fa-bed me-1"></i>투숙중</button>';
        } else if (ss === 'INHOUSE') {
            html += '<button class="btn btn-sm btn-checkout" ' + btnAttr + ' style="background:#EF476F;color:#fff;"><i class="fas fa-door-closed me-1"></i>체크아웃</button>';
        }
        html += '</div>';

        // 객실 배정 인라인 패널 (숨김 상태)
        if (isUnassigned && (ss === 'RESERVED' || ss === 'CHECK_IN')) {
            html += '<div class="room-assign-panel" id="roomAssignPanel-' + sub.id + '" style="display:none;"></div>';
        }

        html += '</div>';
        return html;
    },

    // ========== 취소/노쇼 수수료 미리보기 ==========

    /**
     * 취소/노쇼 수수료 미리보기 모달 표시
     * 예약 상세 페이지(reservation-detail.js)의 showCancelPreview와 동일 정책
     * @param reservationId 예약 ID
     * @param isNoShow true이면 노쇼 정책 적용
     */
    showCancelPreview: function (reservationId, isNoShow) {
        var self = this;
        var url = '/api/v1/properties/' + self.propertyId + '/reservations/' + reservationId + '/cancel-preview';
        if (isNoShow) url += '?noShow=true';

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function (res) {
                if (res.success && res.data) {
                    var d = res.data;
                    var fmt = function (v) { return Number(v || 0).toLocaleString('ko-KR') + '원'; };

                    $('#fdCancelPreviewTitle').text(isNoShow ? '노쇼 처리 확인' : '예약 취소 확인');
                    $('#fdCpReservationNo').text(d.masterReservationNo);
                    $('#fdCpGuestName').text(d.guestNameKo);
                    $('#fdCpCheckInOut').text(d.checkIn + ' ~ ' + d.checkOut);
                    $('#fdCpFirstNight').text(fmt(d.firstNightTotal));
                    $('#fdCpPolicyDesc').text(d.policyDescription || '정책 미설정');
                    $('#fdCpCancelFeeAmt').text(fmt(d.cancelFeeAmount) + ' (' + (d.cancelFeePercent || 0) + '%)');
                    $('#fdCpTotalPaid').text(fmt(d.totalPaidAmount));
                    $('#fdCpRefundAmt').text(fmt(d.refundAmount));

                    var btnLabel = isNoShow ? '노쇼 확인' : '취소 확인';
                    $('#fdCancelConfirmBtn').html('<i class="fas fa-ban me-1"></i>' + btnLabel)
                        .off('click').on('click', function () {
                            HolaPms.modal.hide('#fdCancelPreviewModal');
                            if (isNoShow) {
                                // 노쇼: PUT /status
                                self.changeStatus(reservationId, 'NO_SHOW', null);
                            } else {
                                // 취소: DELETE (수수료 계산 + REFUND 처리)
                                self.executeCancel(reservationId);
                            }
                        });

                    HolaPms.modal.show('#fdCancelPreviewModal');
                }
            },
            error: function () {
                HolaPms.alert('error', '취소 정보를 조회할 수 없습니다.');
            }
        });
    },

    /**
     * 예약 취소 실행 (DELETE 엔드포인트 - 수수료 계산 + REFUND 처리)
     */
    executeCancel: function (reservationId) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + reservationId,
            type: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '예약이 취소되었습니다.');
                    self.reload();
                    if (self.isDrawerOpen()) {
                        self.openReservationDetail(reservationId);
                    }
                }
            },
            error: function (xhr) {
                var res = xhr.responseJSON;
                HolaPms.alert('error', res && res.message ? res.message : '취소 처리 중 오류가 발생했습니다.');
            }
        });
    },

    // ========== 객실 배정 인라인 (Step 2) ==========

    /**
     * 가용 객실 조회 후 인라인 패널에 표시
     */
    expandRoomAssign: function (subId) {
        var self = this;
        var d = this.currentReservationData;
        if (!d) return;

        // 해당 sub 찾기
        var sub = null;
        (d.subReservations || []).forEach(function (s) {
            if (s.id === subId) sub = s;
        });
        if (!sub) return;

        var $panel = $('#roomAssignPanel-' + subId);
        $panel.html('<div class="text-center py-2"><div class="spinner-border spinner-border-sm text-primary" role="status"></div> 가용 객실 조회 중...</div>');
        $panel.slideDown(200);

        // 가용 객실 API 호출
        var params = {
            roomTypeId: sub.roomTypeId,
            rateCodeId: d.rateCodeId || '',
            checkIn: sub.checkIn,
            checkOut: sub.checkOut,
            adults: sub.adults || 1,
            children: sub.children || 0,
            excludeSubId: sub.id
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-assign/availability',
            type: 'GET',
            data: params,
            success: function (res) {
                if (!res.success || !res.data) {
                    $panel.html('<div class="text-muted small py-2">가용 객실 데이터를 조회할 수 없습니다.</div>');
                    return;
                }
                self.renderRoomAssignPanel($panel, subId, sub, res.data);
            },
            error: function () {
                $panel.html('<div class="text-danger small py-2">가용 객실 조회 중 오류가 발생했습니다.</div>');
            }
        });
    },

    /**
     * 가용 객실 패널 렌더링 — 층별 그룹핑
     */
    renderRoomAssignPanel: function ($panel, subId, sub, availData) {
        var groups = availData.roomTypeGroups || [];

        // 현재 roomTypeId에 해당하는 그룹 찾기 (같은 타입 우선)
        var matchedGroup = null;
        groups.forEach(function (g) {
            if (g.roomTypeId === sub.roomTypeId) matchedGroup = g;
        });

        // 매칭되는 그룹이 없으면 첫 번째 사용
        if (!matchedGroup && groups.length > 0) matchedGroup = groups[0];

        if (!matchedGroup || !matchedGroup.floors || matchedGroup.floors.length === 0) {
            $panel.html('<div class="text-muted small py-2">가용 객실이 없습니다.</div>' +
                '<div class="text-end mt-2"><button class="btn btn-sm btn-outline-secondary btn-cancel-assign" data-sub-id="' + subId + '">닫기</button></div>');
            return;
        }

        var html = '<div class="small text-secondary mb-2"><i class="fas fa-door-open me-1"></i>가용 객실 선택</div>';

        matchedGroup.floors.forEach(function (floor) {
            if (!floor.rooms || floor.rooms.length === 0) return;
            html += '<div class="floor-group">';
            html += '<div class="floor-label"><i class="fas fa-building me-1"></i>' + HolaPms.escapeHtml(floor.floorName || '') + '</div>';
            html += '<div class="d-flex flex-wrap">';

            floor.rooms.forEach(function (room) {
                var cls = room.available ? '' : ' disabled';
                var dataAttr = room.available
                    ? 'data-room-number-id="' + room.roomNumberId + '" data-floor-id="' + floor.floorId + '" data-room-number="' + HolaPms.escapeHtml(room.roomNumber || '') + '"'
                    : '';
                html += '<span class="room-option' + cls + '" ' + dataAttr + '>' + HolaPms.escapeHtml(room.roomNumber || '') + '</span>';
            });

            html += '</div></div>';
        });

        // 버튼
        html += '<div class="d-flex justify-content-end gap-2 mt-2">';
        html += '<button class="btn btn-sm btn-outline-secondary btn-cancel-assign" data-sub-id="' + subId + '">취소</button>';
        html += '<button class="btn btn-sm btn-primary btn-confirm-assign" data-sub-id="' + subId + '" disabled><i class="fas fa-check me-1"></i>배정 완료</button>';
        html += '</div>';

        $panel.html(html);
    },

    /**
     * 객실 배정 확정 — PUT /legs/{legId}
     */
    confirmRoomAssign: function (subId) {
        var self = this;
        var d = this.currentReservationData;
        if (!d) return;

        var sub = null;
        (d.subReservations || []).forEach(function (s) {
            if (s.id === subId) sub = s;
        });
        if (!sub) return;

        var $panel = $('#roomAssignPanel-' + subId);
        var $selected = $panel.find('.room-option.selected');
        if ($selected.length === 0) {
            HolaPms.alert('warning', '객실을 선택해주세요.');
            return;
        }

        var roomNumberId = $selected.data('room-number-id');
        var floorId = $selected.data('floor-id');
        var roomNumber = $selected.data('room-number');

        // 확인
        if (!confirm(roomNumber + ' 객실을 배정하시겠습니까?')) return;

        // 배정 버튼 비활성화
        $panel.find('.btn-confirm-assign').prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-1"></i>처리 중...');

        // PUT /legs/{legId} — 기존 서브예약 데이터 유지하면서 객실만 변경
        var requestData = {
            id: sub.id,
            roomTypeId: sub.roomTypeId,
            floorId: floorId,
            roomNumberId: roomNumberId,
            adults: sub.adults || 1,
            children: sub.children || 0,
            checkIn: sub.checkIn,
            checkOut: sub.checkOut,
            earlyCheckIn: sub.earlyCheckIn || false,
            lateCheckOut: sub.lateCheckOut || false
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.id + '/legs/' + sub.id,
            type: 'PUT',
            data: requestData,
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', roomNumber + ' 객실 배정이 완료되었습니다.');
                    // Drawer + 테이블 동시 갱신
                    self.reload();
                    self.openReservationDetail(d.id);
                }
            },
            error: function (xhr) {
                var res = xhr.responseJSON;
                var msg = res && res.message ? res.message : '객실 배정 중 오류가 발생했습니다.';
                HolaPms.alert('error', msg);
                $panel.find('.btn-confirm-assign').prop('disabled', false).html('<i class="fas fa-check me-1"></i>배정 완료');
            }
        });
    },

    // ========== 폴링 ==========

    startPolling: function () {
        this.stopPolling();
        var self = this;
        this.pollInterval = setInterval(function () {
            // Drawer가 열려있으면 테이블만 갱신 (Drawer 깜빡임 방지)
            self.loadAllData();
        }, 30000);
    },

    stopPolling: function () {
        if (this.pollInterval) { clearInterval(this.pollInterval); this.pollInterval = null; }
    }
};

$(document).ready(function () {
    FdOperations.init();
});
