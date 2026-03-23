/**
 * 프론트데스크 운영현황 페이지
 * 상태 필터 + DataTable + 예약 상세 팝업
 *
 * 상태 전이 매트릭스:
 *   예약(RESERVED) → 체크인(INHOUSE), 취소(CANCELED), 노쇼(NO_SHOW)
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
    ACTIVE_STATUSES: ['RESERVED', 'CHECK_IN', 'INHOUSE'],

    init: function () {
        // URL 파라미터로 초기 필터 설정 (대시보드에서 클릭 시)
        var urlParams = new URLSearchParams(window.location.search);
        var initFilter = urlParams.get('filter');
        if (initFilter && ['arrivals', 'inhouse', 'departures'].indexOf(initFilter) >= 0) {
            this.specialFilter = initFilter;
        }

        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () {
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
            self.changeStatus($(this).data('reservation-id'), 'INHOUSE', '체크인 처리하시겠습니까?', $(this).data('sub-id'), $(this).data('current-status'));
        });
        $(document).on('click', '.btn-inhouse', function (e) {
            e.preventDefault(); e.stopPropagation();
            self.changeStatus($(this).data('reservation-id'), 'INHOUSE', '투숙중으로 변경하시겠습니까?', $(this).data('sub-id'), $(this).data('current-status'));
        });
        $(document).on('click', '.btn-checkout', function (e) {
            e.preventDefault(); e.stopPropagation();
            self.changeStatus($(this).data('reservation-id'), 'CHECKED_OUT', '체크아웃 처리하시겠습니까?', $(this).data('sub-id'), $(this).data('current-status'));
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
        $(document).on('click', '.btn-detail', function (e) {
            e.preventDefault(); e.stopPropagation();
            HolaPms.popup.openReservationDetail($(this).data('reservation-id'));
        });

        // 팝업 자식 창 메시지 수신 → 데이터 갱신
        HolaPms.popup.onChildMessage(function() {
            self.reload();
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
            $('#summaryCheckedIn').text(this.summaryData.checkedInToday || 0);
            $('#summaryCheckedOut').text(this.summaryData.checkedOutToday || 0);
        }
        // "오늘 전체"는 allData 전체 건수 (오늘 관련 모든 예약)
        $('#summaryTotal').text(this.allData.length);
    },

    /**
     * 활성 상태(RESERVED, CHECK_IN, INHOUSE) 데이터만 필터
     */
    getActiveData: function () {
        var active = this.ACTIVE_STATUSES;
        return this.allData.filter(function (d) {
            var s = d.roomReservationStatus || d.reservationStatus || '';
            return active.indexOf(s) >= 0;
        });
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

        // "전체" 클릭 시 오늘 관련 전체 표시
        if (filter === 'all') {
            self.specialFilter = 'all';
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
        var filtered;
        if (status) {
            // 특정 상태 필터 선택 시 → 전체 데이터에서 해당 상태만
            filtered = this.allData.filter(function (d) {
                return d.roomReservationStatus === status || d.reservationStatus === status;
            });
        } else {
            // "전체" 필터 (기본) → 오늘 관련 전체
            filtered = this.allData.slice();
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
                { data: 'guestNameKo', render: function(data) { return HolaPms.escapeHtml(HolaPms.maskName(data) || '-'); }, className: 'text-center' },
                { data: 'roomTypeName', render: HolaPms.renders.dashIfEmpty, className: 'text-center' },
                { data: 'roomNumber', render: this.renderRoomNumber, className: 'text-center' },
                { data: null, render: this.renderPersons, className: 'text-center' },
                { data: 'checkIn', className: 'text-center' },
                { data: 'checkOut', className: 'text-center' },
                { data: 'nights', className: 'text-center', render: function(data, type, row) {
                    if (row.stayType === 'DAY_USE') {
                        return '<span class="badge" style="background-color:#0582CA;">Dayuse</span>';
                    }
                    return data + '박';
                }},
                { data: 'roomReservationStatus', render: this.renderStatusBadge, className: 'text-center' },
                { data: 'paymentStatus', render: this.renderPaymentBadge, className: 'text-center' }
            ],
            pageLength: 20,
            dom: 'rtip'
        }));

        // 행 클릭 → 예약 상세 팝업 열기
        $('#operationTable tbody').on('click', 'tr', function (e) {
            // 액션 버튼 클릭 시 행 클릭 무시
            if ($(e.target).closest('button, a').length) return;
            var rowData = self.dataTable.row(this).data();
            if (rowData && rowData.reservationId) {
                HolaPms.popup.openReservationDetail(rowData.reservationId);
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

    // 상태 전환 매트릭스 (체크인 → 바로 INHOUSE)
    STATUS_TRANSITIONS: {
        'RESERVED':    ['INHOUSE', 'CANCELED', 'NO_SHOW'],
        'CHECK_IN':    ['INHOUSE', 'CANCELED'],  // 하위 호환
        'INHOUSE':     ['CHECKED_OUT'],
        'CHECKED_OUT': [],
        'CANCELED':    [],
        'NO_SHOW':     []
    },

    renderStatusBadge: function (data) {
        var info = FdOperations.STATUS_MAP[data] || { label: data || '-', bg: '#6c757d' };
        var textColor = info.color || '#fff';
        return '<span class="badge" style="background-color:' + info.bg + '; color:' + textColor + '">' + info.label + '</span>';
    },

    // ========== 상태 변경 ==========

    changeStatus: function (reservationId, newStatus, message, subReservationId, currentStatus) {
        // 프론트엔드 상태 전환 검증
        if (currentStatus && FdOperations.STATUS_TRANSITIONS[currentStatus]) {
            if (FdOperations.STATUS_TRANSITIONS[currentStatus].indexOf(newStatus) === -1) {
                HolaPms.alert('warning', currentStatus + ' 상태에서 ' + newStatus + '(으)로 변경할 수 없습니다.');
                return;
            }
        }
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
                        HolaPms.popup.openReservationDetail(reservationId, { tab: tab });
                    }
                } else {
                    HolaPms.alert('error', msg);
                }
            }
        });
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
                }
            },
            error: function (xhr) {
                var res = xhr.responseJSON;
                HolaPms.alert('error', res && res.message ? res.message : '취소 처리 중 오류가 발생했습니다.');
            }
        });
    },

    // ========== 폴링 ==========

    startPolling: function () {
        this.stopPolling();
        var self = this;
        this.pollInterval = setInterval(function () {
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
