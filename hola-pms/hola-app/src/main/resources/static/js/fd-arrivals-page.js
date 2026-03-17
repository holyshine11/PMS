/**
 * 프론트데스크 도착 페이지 + 7단계 체크인
 */
var FdArrivals = {
    propertyId: null,
    data: [],
    currentStep: 0,
    totalSteps: 7,
    selectedReservation: null,

    init: function() {
        var self = this;
        var today = new Date();
        $('#todayLabel').text(today.getFullYear() + '-' + String(today.getMonth()+1).padStart(2,'0') + '-' + String(today.getDate()).padStart(2,'0'));
        self.bindEvents();
        self.reload();
    },

    bindEvents: function() {
        var self = this;
        $(document).on('hola:contextChange', function() { self.reload(); });

        // 체크인 모달 네비게이션
        $('#ciPrevBtn').on('click', function() { self.prevStep(); });
        $('#ciNextBtn').on('click', function() { self.nextStep(); });
        $('#ciCompleteBtn').on('click', function() { self.doCheckIn(); });

        // 메모 추가
        $('#ciAddMemoBtn').on('click', function() { self.addMemo(); });
        $('#ciNewMemo').on('keypress', function(e) {
            if (e.which === 13) self.addMemo();
        });
    },

    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#summaryCards').hide();
            $('#arrivalTable').closest('.card').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#summaryCards').show();
        $('#arrivalTable').closest('.card').show();
        self.propertyId = propertyId;
        self.loadArrivals();
    },

    loadArrivals: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/front-desk/arrivals',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                self.data = res.data;
                self.renderTable();
                self.updateSummary();
            }
        });
    },

    renderTable: function() {
        var self = this;
        var html = '';
        if (self.data.length === 0) {
            html = '<tr><td colspan="11" class="text-center py-4 text-muted">오늘 도착 예정 예약이 없습니다.</td></tr>';
        } else {
            for (var i = 0; i < self.data.length; i++) {
                var d = self.data[i];
                var hkBadge = self.getHkBadge(d.hkStatus);
                var roomDisplay = d.roomNumber ? HolaPms.escapeHtml(d.roomNumber) : '<span class="text-danger">미배정</span>';

                html += '<tr>'
                    + '<td class="ps-3">' + HolaPms.escapeHtml(d.masterReservationNo) + '</td>'
                    + '<td>' + HolaPms.escapeHtml(d.confirmationNo) + '</td>'
                    + '<td>' + HolaPms.escapeHtml(d.guestNameKo || '-') + '</td>'
                    + '<td>' + HolaPms.escapeHtml(d.roomTypeName || '-') + '</td>'
                    + '<td>' + roomDisplay + '</td>'
                    + '<td>' + hkBadge + '</td>'
                    + '<td class="text-center">' + d.adults + (d.children > 0 ? '+' + d.children : '') + '</td>'
                    + '<td>' + d.checkIn + '</td>'
                    + '<td>' + d.checkOut + '</td>'
                    + '<td class="text-end">' + (d.totalAmount ? Number(d.totalAmount).toLocaleString() : '0') + '</td>'
                    + '<td class="text-center pe-3">'
                    + '<button class="btn btn-sm btn-primary" onclick="FdArrivals.openCheckIn(' + i + ')">'
                    + '<i class="fas fa-sign-in-alt"></i></button>'
                    + '</td></tr>';
            }
        }
        $('#arrivalBody').html(html);
    },

    updateSummary: function() {
        var self = this;
        var total = self.data.length;
        var unassigned = self.data.filter(function(d) { return !d.roomNumber; }).length;
        var hkWarn = self.data.filter(function(d) { return d.hkStatus && d.hkStatus !== 'CLEAN'; }).length;
        $('#arrivalCount').text(total);
        $('#unassignedCount').text(unassigned);
        $('#hkWarningCount').text(hkWarn);
    },

    getHkBadge: function(status) {
        if (!status) return '<span class="badge bg-secondary">-</span>';
        var map = {
            'CLEAN': '<span class="badge bg-success">CLEAN</span>',
            'DIRTY': '<span class="badge bg-danger">DIRTY</span>',
            'OOO': '<span class="badge bg-dark">OOO</span>',
            'OOS': '<span class="badge bg-secondary">OOS</span>'
        };
        return map[status] || '<span class="badge bg-secondary">' + HolaPms.escapeHtml(status) + '</span>';
    },

    // === 7단계 체크인 ===

    openCheckIn: function(index) {
        var self = this;
        self.selectedReservation = self.data[index];
        self.currentStep = 0;
        self.showStep(0);

        var d = self.selectedReservation;
        // Step 1 데이터 채우기
        $('#ciReservationNo').text(d.masterReservationNo);
        $('#ciConfirmationNo').text(d.confirmationNo);
        $('#ciGuestName').text(d.guestNameKo || '-');
        $('#ciPhone').text(d.phoneNumber || '-');
        $('#ciRoomType').text(d.roomTypeName || '-');
        $('#ciDates').text(d.checkIn + ' ~ ' + d.checkOut);
        $('#ciGuests').text('성인 ' + d.adults + '명' + (d.children > 0 ? ', 아동 ' + d.children + '명' : ''));

        HolaPms.modal.show('checkInModal');
    },

    showStep: function(step) {
        var self = this;
        self.currentStep = step;

        // 탭 활성화
        $('#checkInSteps .nav-link').removeClass('active');
        $('#checkInSteps .nav-link').eq(step).addClass('active');

        // 패널 표시
        var tabs = ['#step1','#step2','#step3','#step4','#step5','#step6','#step7'];
        for (var i = 0; i < tabs.length; i++) {
            $(tabs[i]).toggleClass('show active', i === step);
        }

        // 이전/다음 버튼 상태
        $('#ciPrevBtn').prop('disabled', step === 0);
        $('#ciNextBtn').toggleClass('d-none', step === self.totalSteps - 1);
        $('#ciCompleteBtn').toggleClass('d-none', step !== self.totalSteps - 1);

        // 각 스텝별 데이터 로드
        if (step === 1) self.loadGuestInfo();
        if (step === 2) self.loadRoomList();
        if (step === 3) self.loadChargeInfo();
        if (step === 4) self.loadPaymentInfo();
        if (step === 5) self.loadMemos();
        if (step === 6) self.updateFinalSummary();
    },

    prevStep: function() {
        var self = this;
        if (self.currentStep > 0) self.showStep(self.currentStep - 1);
    },

    nextStep: function() {
        var self = this;
        if (self.currentStep < self.totalSteps - 1) self.showStep(self.currentStep + 1);
    },

    loadGuestInfo: function() {
        var self = this;
        var d = self.selectedReservation;
        var html = '<div class="row mb-2"><div class="col-sm-3 text-muted">이름(한)</div><div class="col-sm-9">' + HolaPms.escapeHtml(d.guestNameKo || '-') + '</div></div>'
            + '<div class="row mb-2"><div class="col-sm-3 text-muted">연락처</div><div class="col-sm-9">' + HolaPms.escapeHtml(d.phoneNumber || '-') + '</div></div>'
            + '<div class="row mb-2"><div class="col-sm-3 text-muted">채널</div><div class="col-sm-9">' + HolaPms.escapeHtml(d.channelName || '-') + '</div></div>';
        $('#ciGuestInfo').html(html);
    },

    loadRoomList: function() {
        var self = this;
        var d = self.selectedReservation;
        $('#ciCurrentRoom').text(d.roomNumber || '미배정');

        // VC 객실 로드 (기존 room-assign API 재사용)
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-numbers',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var rooms = res.data;
                var html = '<table class="table table-sm table-hover mb-0">'
                    + '<thead><tr><th>호수</th><th>HK</th><th>FO</th><th></th></tr></thead><tbody>';

                for (var i = 0; i < rooms.length; i++) {
                    var r = rooms[i];
                    var isVC = r.hkStatus === 'CLEAN' && r.foStatus === 'VACANT';
                    var isOOO = r.hkStatus === 'OOO';
                    var rowClass = isOOO ? 'table-secondary' : (isVC ? '' : 'text-muted');
                    var btnDisabled = (isOOO || r.foStatus === 'OCCUPIED') ? 'disabled' : '';
                    var recommended = isVC ? ' <span class="badge bg-success">추천</span>' : '';

                    html += '<tr class="' + rowClass + '">'
                        + '<td>' + HolaPms.escapeHtml(r.roomNumber) + recommended + '</td>'
                        + '<td>' + self.getHkBadge(r.hkStatus) + '</td>'
                        + '<td>' + (r.foStatus === 'OCCUPIED' ? '<span class="badge bg-primary">OC</span>' : '<span class="badge bg-secondary">VC</span>') + '</td>'
                        + '<td><button class="btn btn-sm btn-outline-primary" ' + btnDisabled
                        + ' onclick="FdArrivals.assignRoom(' + r.id + ', \'' + HolaPms.escapeHtml(r.roomNumber) + '\')">'
                        + '배정</button></td></tr>';
                }
                html += '</tbody></table>';
                $('#ciRoomList').html(html);
            }
        });
    },

    assignRoom: function(roomId, roomNumber) {
        var self = this;
        var d = self.selectedReservation;
        // 기존 예약 수정 API로 객실 배정 (서브 예약 수정)
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.masterReservationId + '/legs/' + d.subReservationId,
            method: 'PUT',
            data: JSON.stringify({
                roomTypeId: null, // 기존 유지
                floorId: null,
                roomNumberId: roomId,
                adults: d.adults,
                children: d.children,
                checkIn: d.checkIn,
                checkOut: d.checkOut,
                earlyCheckIn: false,
                lateCheckOut: false
            }),
            success: function(res) {
                if (res.success) {
                    d.roomNumberId = roomId;
                    d.roomNumber = roomNumber;
                    $('#ciCurrentRoom').text(roomNumber).addClass('text-primary');
                    HolaPms.alert('success', '객실 ' + roomNumber + ' 배정 완료');
                }
            }
        });
    },

    loadChargeInfo: function() {
        var self = this;
        var d = self.selectedReservation;
        // 예약 상세에서 요금 정보 가져오기
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.masterReservationId,
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var detail = res.data;
                var html = '<table class="table table-sm mb-0">';
                if (detail.subReservations && detail.subReservations.length > 0) {
                    var sub = detail.subReservations[0];
                    if (sub.dailyCharges) {
                        html += '<thead><tr><th>날짜</th><th class="text-end">공급가</th><th class="text-end">세금</th><th class="text-end">합계</th></tr></thead><tbody>';
                        for (var i = 0; i < sub.dailyCharges.length; i++) {
                            var c = sub.dailyCharges[i];
                            html += '<tr><td>' + c.chargeDate + '</td>'
                                + '<td class="text-end">' + Number(c.supplyPrice || 0).toLocaleString() + '</td>'
                                + '<td class="text-end">' + Number(c.tax || 0).toLocaleString() + '</td>'
                                + '<td class="text-end">' + Number(c.total || 0).toLocaleString() + '</td></tr>';
                        }
                    }
                }
                html += '</tbody></table>';
                html += '<div class="text-end mt-2 fw-bold">총액: ' + Number(d.totalAmount || 0).toLocaleString() + '원</div>';
                $('#ciChargeInfo').html(html);
            }
        });
    },

    loadPaymentInfo: function() {
        var self = this;
        var d = self.selectedReservation;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.masterReservationId,
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var detail = res.data;
                var ps = detail.paymentSummary || {};
                var html = '<div class="row mb-2"><div class="col-6 text-muted">총 요금</div><div class="col-6 text-end">' + Number(ps.totalCharge || 0).toLocaleString() + '원</div></div>'
                    + '<div class="row mb-2"><div class="col-6 text-muted">결제 금액</div><div class="col-6 text-end">' + Number(ps.totalPaid || 0).toLocaleString() + '원</div></div>'
                    + '<hr>'
                    + '<div class="row mb-2"><div class="col-6 fw-bold">잔액</div><div class="col-6 text-end fw-bold ' + ((ps.balance || 0) > 0 ? 'text-danger' : 'text-primary') + '">'
                    + Number(ps.balance || 0).toLocaleString() + '원</div></div>';
                $('#ciPaymentInfo').html(html);
            }
        });
    },

    loadMemos: function() {
        var self = this;
        var d = self.selectedReservation;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.masterReservationId + '/memos',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var memos = res.data;
                if (memos.length === 0) {
                    $('#ciMemoList').html('<p class="text-muted">등록된 메모가 없습니다.</p>');
                } else {
                    var html = '';
                    for (var i = 0; i < memos.length; i++) {
                        html += '<div class="border-bottom py-2"><small class="text-muted">' + memos[i].createdAt + ' - ' + HolaPms.escapeHtml(memos[i].createdBy || '') + '</small>'
                            + '<div>' + HolaPms.escapeHtml(memos[i].content) + '</div></div>';
                    }
                    $('#ciMemoList').html(html);
                }
            }
        });
    },

    addMemo: function() {
        var self = this;
        var content = $('#ciNewMemo').val().trim();
        if (!content) return;
        var d = self.selectedReservation;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.masterReservationId + '/memos',
            method: 'POST',
            data: JSON.stringify({ content: content }),
            success: function(res) {
                if (res.success) {
                    $('#ciNewMemo').val('');
                    self.loadMemos();
                    HolaPms.alert('success', '메모 추가 완료');
                }
            }
        });
    },

    updateFinalSummary: function() {
        var self = this;
        var d = self.selectedReservation;
        $('#ciFinalGuest').text(d.guestNameKo || '-');
        $('#ciFinalRoom').text(d.roomNumber || '미배정');
        $('#ciFinalCheckOut').text(d.checkOut);
    },

    doCheckIn: function() {
        var self = this;
        var d = self.selectedReservation;

        if (!d.roomNumberId && !d.roomNumber) {
            HolaPms.alert('danger', '객실을 먼저 배정해주세요.');
            self.showStep(2);
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.masterReservationId + '/status',
            method: 'PUT',
            data: JSON.stringify({ newStatus: 'CHECK_IN' }),
            success: function(res) {
                if (res.success) {
                    HolaPms.modal.hide('checkInModal');
                    HolaPms.alert('success', d.guestNameKo + ' 체크인 완료 (객실: ' + d.roomNumber + ')');
                    self.reload();
                }
            }
        });
    }
};

$(document).ready(function() {
    FdArrivals.init();
});
