/**
 * 프론트데스크 운영현황 페이지
 * 상태 필터(예약목록 패턴) + 단일 DataTable + Quick Action + 예약 상세 모달
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
    arrivalsData: [],
    inHouseData: [],
    departuresData: [],
    activeFilter: '',      // 상태 필터 값 (빈 문자열=전체)
    specialFilter: null,   // 요약 카드 특수 필터: 'arrivals', 'departures'
    pollInterval: null,

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () {
            $('.modal.show').each(function () { HolaPms.modal.hide('#' + this.id); });
            self.reload();
        });

        $('#btnRefresh').on('click', function () { self.reload(); });

        // 상태 필터 버튼
        $('#statusFilterGroup').on('click', 'button[data-status]', function () {
            self.specialFilter = null; // 특수 필터 해제
            $('#statusFilterGroup button').removeClass('active').css('border-color', 'transparent').css('box-shadow', 'none');
            $(this).addClass('active').css('border-color', '#051923').css('box-shadow', '0 0 0 1px #051923');
            self.applyFilter();
        });

        // 요약 카드 클릭 → 특수 필터 (날짜 기반)
        $('#summaryCards .card[data-filter]').on('click', function () {
            var filter = $(this).data('filter');
            self.specialFilter = filter;
            // 상태 필터 비활성화 (특수 필터 활성 표시)
            $('#statusFilterGroup button').removeClass('active').css('border-color', 'transparent').css('box-shadow', 'none');
            self.applyFilter();
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

        // Quick Action 이벤트 — Leg 단위 상태 변경 (subReservationId 전달)
        $(document).on('click', '.btn-checkin', function (e) {
            e.preventDefault();
            self.changeStatus($(this).data('reservation-id'), 'CHECK_IN', '체크인 처리하시겠습니까?', $(this).data('sub-id'));
        });

        $(document).on('click', '.btn-inhouse', function (e) {
            e.preventDefault();
            self.changeStatus($(this).data('reservation-id'), 'INHOUSE', '투숙중으로 변경하시겠습니까?', $(this).data('sub-id'));
        });

        $(document).on('click', '.btn-checkout', function (e) {
            e.preventDefault();
            self.changeStatus($(this).data('reservation-id'), 'CHECKED_OUT', '체크아웃 처리하시겠습니까?', $(this).data('sub-id'));
        });

        $(document).on('click', '.btn-noshow', function (e) {
            e.preventDefault();
            self.changeStatus($(this).data('reservation-id'), 'NO_SHOW', '노쇼 처리하시겠습니까?', $(this).data('sub-id'));
        });

        $(document).on('click', '.btn-detail', function (e) {
            e.preventDefault();
            self.openReservationDetail($(this).data('reservation-id'));
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
            if (completed < 3) return;

            // 합치되 중복 제거
            var seen = {};
            self.allData = [];
            [].concat(self.arrivalsData, self.inHouseData, self.departuresData).forEach(function (item) {
                var key = item.subReservationId;
                if (!seen[key]) {
                    seen[key] = true;
                    self.allData.push(item);
                }
            });

            self.updateSummary();
            self.applyFilter();
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + pid + '/front-desk/arrivals',
            type: 'GET',
            success: function (res) { self.arrivalsData = res.data || []; checkDone(); },
            error: function () { self.arrivalsData = []; checkDone(); }
        });

        HolaPms.ajax({
            url: '/api/v1/properties/' + pid + '/front-desk/in-house',
            type: 'GET',
            success: function (res) { self.inHouseData = res.data || []; checkDone(); },
            error: function () { self.inHouseData = []; checkDone(); }
        });

        HolaPms.ajax({
            url: '/api/v1/properties/' + pid + '/front-desk/departures',
            type: 'GET',
            success: function (res) { self.departuresData = res.data || []; checkDone(); },
            error: function () { self.departuresData = []; checkDone(); }
        });
    },

    updateSummary: function () {
        $('#summaryArrivals').text(this.arrivalsData.length);
        $('#summaryInHouse').text(this.inHouseData.length);
        $('#summaryDepartures').text(this.departuresData.length);
        $('#summaryTotal').text(this.allData.length);
    },

    applyFilter: function () {
        var filtered;

        // 특수 필터 (요약 카드 클릭)
        if (this.specialFilter === 'arrivals') {
            filtered = this.arrivalsData.slice();
        } else if (this.specialFilter === 'departures') {
            filtered = this.departuresData.slice();
        } else if (this.specialFilter === 'inhouse') {
            filtered = this.inHouseData.slice();
        } else {
            // 상태 필터
            var status = $('#statusFilterGroup button.active').data('status') || '';
            filtered = this.allData.slice();
            if (status) {
                filtered = filtered.filter(function (d) {
                    return d.roomReservationStatus === status || d.reservationStatus === status;
                });
            }
        }

        // 키워드 필터
        var keyword = $.trim($('#keyword').val()).toLowerCase();
        if (keyword) {
            filtered = filtered.filter(function (d) {
                return (d.masterReservationNo && d.masterReservationNo.toLowerCase().indexOf(keyword) >= 0) ||
                       (d.guestNameKo && d.guestNameKo.toLowerCase().indexOf(keyword) >= 0) ||
                       (d.phoneNumber && d.phoneNumber.indexOf(keyword) >= 0) ||
                       (d.masterReservationNo && d.masterReservationNo.toLowerCase().indexOf(keyword) >= 0) ||
                       (d.roomNumber && d.roomNumber.indexOf(keyword) >= 0);
            });
        }

        if (this.dataTable) {
            this.dataTable.clear();
            this.dataTable.rows.add(filtered);
            this.dataTable.draw();
        } else {
            this.initTable(filtered);
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

        // 행 클릭 → 상세 모달 (액션 컬럼 대체)
        $('#operationTable tbody').on('click', 'tr', function() {
            var rowData = self.dataTable.row(this).data();
            if (rowData && rowData.reservationId) {
                self.openReservationDetail(rowData.reservationId);
            }
        });

        // 행 hover 커서
        $('#operationTable tbody').css('cursor', 'pointer');
    },

    // ========== 렌더러 ==========

    renderConfirmNo: function (data, type, row) {
        if (!data) return '-';
        var display = HolaPms.escapeHtml(data);
        // 멀티 Leg 구분: subReservationNo에서 Leg 번호 추출 (예: RES-001-01 → L1)
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

    /**
     * 액션 드롭다운 — 상태별 허용 전이만 표시
     * 예약 → 체크인, 노쇼
     * 체크인 → 투숙중
     * 투숙중 → 체크아웃
     */
    renderAction: function (data, type, row) {
        var status = row.roomReservationStatus;
        var rid = row.reservationId;
        var sid = row.subReservationId;
        var da = 'data-reservation-id="' + rid + '" data-sub-id="' + sid + '"';
        var items = '';

        if (status === 'RESERVED') {
            items += '<li><a class="dropdown-item btn-checkin" href="#" ' + da + '><i class="fas fa-key me-2 text-primary"></i>체크인</a></li>';
            if (!row.roomNumberId) {
                items += '<li><a class="dropdown-item btn-detail" href="#" data-reservation-id="' + rid + '"><i class="fas fa-door-open me-2 text-warning"></i>객실 배정</a></li>';
            }
            items += '<li><a class="dropdown-item btn-noshow" href="#" ' + da + '><i class="fas fa-user-times me-2 text-secondary"></i>노쇼</a></li>';
        } else if (status === 'CHECK_IN') {
            items += '<li><a class="dropdown-item btn-inhouse" href="#" ' + da + '><i class="fas fa-bed me-2" style="color:#003554;"></i>투숙중 처리</a></li>';
        } else if (status === 'INHOUSE') {
            items += '<li><a class="dropdown-item btn-checkout" href="#" ' + da + '><i class="fas fa-door-closed me-2" style="color:#EF476F;"></i>체크아웃</a></li>';
        }

        items += '<li><hr class="dropdown-divider"></li>';
        items += '<li><a class="dropdown-item btn-detail" href="#" data-reservation-id="' + rid + '"><i class="fas fa-info-circle me-2 text-secondary"></i>상세 보기</a></li>';

        return '<div class="dropdown">' +
               '<button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown" title="액션">' +
               '<i class="fas fa-ellipsis-v"></i></button>' +
               '<ul class="dropdown-menu dropdown-menu-end">' + items + '</ul></div>';
    },

    // ========== 상태 변경 ==========

    changeStatus: function (reservationId, newStatus, message, subReservationId) {
        if (!confirm(message)) return;
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
                    // 모달이 열려있으면 데이터 새로고침
                    if ($('#reservationDetailModal').hasClass('show')) {
                        self.openReservationDetail(reservationId);
                    }
                }
            },
            error: function (xhr) {
                var res = xhr.responseJSON;
                var msg = res && res.message ? res.message : '처리 중 오류가 발생했습니다.';
                var code = res && res.code ? res.code : '';

                // 전제조건 에러: 예약정보 수정 페이지로 이동 안내 (에러별 탭 지정)
                var codeTabMap = {
                    'HOLA-4029': 'payment',  // 결제 잔액 → 결제정보 탭
                    'HOLA-5001': 'detail',   // 객실 미배정 → 상세정보 탭
                    'HOLA-5003': 'detail',   // OOO 객실 → 상세정보 탭
                    'HOLA-5010': 'payment'   // 미결제 잔액 → 결제정보 탭
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

    // ========== 예약 상세 모달 ==========

    openReservationDetail: function (reservationId) {
        var self = this;
        var $body = $('#mdlBody');
        $body.html('<div class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></div>');
        $('#mdlBtnCheckin, #mdlBtnInhouse, #mdlBtnCheckout').addClass('d-none');
        // 새 창에서 프로퍼티 컨텍스트가 유지되도록 파라미터 전달
        var hotelId = HolaPms.context.getHotelId() || '';
        var propId = self.propertyId || '';
        $('#mdlFullDetailLink').attr('href', '/admin/reservations/' + reservationId + '?hotelId=' + hotelId + '&propertyId=' + propId);
        HolaPms.modal.show('#reservationDetailModal');

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + reservationId,
            type: 'GET',
            success: function (res) {
                if (!res.success || !res.data) {
                    $body.html('<div class="alert alert-warning">데이터를 불러올 수 없습니다.</div>');
                    return;
                }
                self.renderDetailModal(res.data);
            },
            error: function () {
                $body.html('<div class="alert alert-danger">조회 중 오류가 발생했습니다.</div>');
            }
        });
    },

    renderDetailModal: function (d) {
        var self = this;
        var status = d.reservationStatus;
        $('#mdlConfirmNo').text(d.masterReservationNo || d.masterReservationNo);

        // 마스터 레벨 액션 버튼 (전체 Leg 일괄, subReservationId=null)
        if (status === 'RESERVED') {
            $('#mdlBtnCheckin').removeClass('d-none').off('click').on('click', function () {
                HolaPms.modal.hide('#reservationDetailModal');
                self.changeStatus(d.id, 'CHECK_IN', '전체 객실 체크인 처리하시겠습니까?');
            });
        } else if (status === 'CHECK_IN') {
            $('#mdlBtnInhouse').removeClass('d-none').off('click').on('click', function () {
                HolaPms.modal.hide('#reservationDetailModal');
                self.changeStatus(d.id, 'INHOUSE', '전체 객실 투숙중으로 변경하시겠습니까?');
            });
        } else if (status === 'INHOUSE') {
            $('#mdlBtnCheckout').removeClass('d-none').off('click').on('click', function () {
                HolaPms.modal.hide('#reservationDetailModal');
                self.changeStatus(d.id, 'CHECKED_OUT', '전체 객실 체크아웃 처리하시겠습니까?');
            });
        }

        var statusBadge = FdOperations.renderStatusBadge(status);
        var subs = d.subReservations || [];
        var payment = d.payment || {};
        var grandTotal = payment.grandTotal || 0;
        var paidAmount = payment.totalPaidAmount || 0;
        var remaining = grandTotal - paidAmount;
        var remainStyle = remaining > 0 ? 'color:#EF476F;' : 'color:#28a745;';

        var html = '';
        // 예약 기본 정보
        html += '<div class="row mb-3">';
        html += '<div class="col-6"><small class="text-secondary">예약번호</small><div>' + HolaPms.escapeHtml(d.masterReservationNo || '') + '</div></div>';
        html += '<div class="col-6"><small class="text-secondary">상태</small><div>' + statusBadge + '</div></div>';
        html += '</div>';
        html += '<div class="row mb-3">';
        html += '<div class="col-4"><small class="text-secondary">예약자</small><div>' + HolaPms.escapeHtml(d.guestNameKo || '') + '</div></div>';
        html += '<div class="col-4"><small class="text-secondary">연락처</small><div>' + HolaPms.escapeHtml(d.phoneNumber || '-') + '</div></div>';
        html += '<div class="col-4"><small class="text-secondary">이메일</small><div>' + HolaPms.escapeHtml(d.email || '-') + '</div></div>';
        html += '</div>';

        // 객실(Leg) 목록 — 전체 표시
        html += '<hr>';
        html += '<h6 class="fw-bold mb-2"><i class="fas fa-bed me-1"></i>객실 목록 (' + subs.length + ')</h6>';

        if (subs.length === 0) {
            html += '<div class="text-muted small">등록된 객실이 없습니다.</div>';
        } else {
            html += '<div class="table-responsive"><table class="table table-sm table-bordered mb-0">';
            html += '<thead class="table-light"><tr>';
            html += '<th class="text-center">객실</th><th class="text-center">객실타입</th>';
            html += '<th class="text-center">체크인</th><th class="text-center">체크아웃</th>';
            html += '<th class="text-center">인원</th>';
            html += '<th class="text-center">상태</th>';
            html += '<th class="text-center">액션</th>';
            html += '</tr></thead><tbody>';

            subs.forEach(function (sub, idx) {
                var persons = (sub.adults || 0) + '명';
                if (sub.children > 0) persons += ' + 아동 ' + sub.children;
                var subStatus = FdOperations.renderStatusBadge(sub.roomReservationStatus);

                // Leg별 액션 버튼
                var legAction = '';
                var ss = sub.roomReservationStatus;
                var btnAttr = 'data-reservation-id="' + d.id + '" data-sub-id="' + sub.id + '"';
                if (ss === 'RESERVED') {
                    legAction = '<button class="btn btn-sm btn-primary btn-checkin" ' + btnAttr + '><i class="fas fa-key me-1"></i>체크인</button>';
                } else if (ss === 'CHECK_IN') {
                    legAction = '<button class="btn btn-sm btn-dark btn-inhouse" ' + btnAttr + '><i class="fas fa-bed me-1"></i>투숙중</button>';
                } else if (ss === 'INHOUSE') {
                    legAction = '<button class="btn btn-sm btn-checkout" ' + btnAttr + ' style="background:#EF476F;color:#fff;"><i class="fas fa-door-closed me-1"></i>체크아웃</button>';
                } else {
                    legAction = '-';
                }

                html += '<tr>';
                html += '<td class="text-center">' + (sub.roomNumber ? '<span class="badge bg-dark">' + HolaPms.escapeHtml(sub.roomNumber) + '</span>' : '<span class="badge bg-secondary">미배정</span>') + '</td>';
                html += '<td class="text-center">' + HolaPms.escapeHtml(sub.roomTypeName || '-') + '</td>';
                html += '<td class="text-center">' + (sub.checkIn || '-') + '</td>';
                html += '<td class="text-center">' + (sub.checkOut || '-') + '</td>';
                html += '<td class="text-center">' + persons + '</td>';
                html += '<td class="text-center">' + subStatus + '</td>';
                html += '<td class="text-center">' + legAction + '</td>';
                html += '</tr>';
            });

            html += '</tbody></table></div>';
        }

        // 결제 요약
        html += '<hr>';
        html += '<div class="row mb-2">';
        html += '<div class="col-4"><small class="text-secondary">총액</small><div>' + Number(grandTotal).toLocaleString('ko-KR') + '원</div></div>';
        html += '<div class="col-4"><small class="text-secondary">결제액</small><div>' + Number(paidAmount).toLocaleString('ko-KR') + '원</div></div>';
        html += '<div class="col-4"><small class="text-secondary">잔액</small><div style="' + remainStyle + '">' + Number(remaining).toLocaleString('ko-KR') + '원</div></div>';
        html += '</div>';

        if (d.customerRequest) {
            html += '<hr><small class="text-secondary">고객 요청</small>';
            html += '<div class="p-2 bg-light rounded mt-1">' + HolaPms.escapeHtml(d.customerRequest) + '</div>';
        }
        if (d.memos && d.memos.length > 0) {
            html += '<hr><small class="text-secondary">메모 (' + d.memos.length + ')</small>';
            d.memos.slice(0, 3).forEach(function (m) {
                html += '<div class="p-2 bg-light rounded mt-1 small">';
                html += '<span class="text-muted">' + (m.createdAt ? m.createdAt.substring(0, 16) : '') + ' ' + HolaPms.escapeHtml(m.createdBy || '') + '</span><br>';
                html += HolaPms.escapeHtml(m.content || '');
                html += '</div>';
            });
        }

        $('#mdlBody').html(html);
    },

    // ========== 폴링 ==========

    startPolling: function () {
        this.stopPolling();
        var self = this;
        this.pollInterval = setInterval(function () { self.loadAllData(); }, 30000);
    },

    stopPolling: function () {
        if (this.pollInterval) { clearInterval(this.pollInterval); this.pollInterval = null; }
    }
};

$(document).ready(function () {
    FdOperations.init();
});
