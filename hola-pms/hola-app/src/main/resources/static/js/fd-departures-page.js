/**
 * 프론트데스크 출발 페이지 + 체크아웃
 */
var FdDepartures = {
    propertyId: null,
    data: [],
    selectedIndex: -1,
    pollInterval: null,

    init: function() {
        var today = new Date();
        $('#todayLabel').text(today.getFullYear() + '-' + String(today.getMonth()+1).padStart(2,'0') + '-' + String(today.getDate()).padStart(2,'0'));
        this.bindEvents();
        this.reload();
    },

    bindEvents: function() {
        var self = this;
        $(document).on('hola:contextChange', function() { self.reload(); });
        $('#departureSearch').on('input', function() { self.renderTable(); });
        $('#coCompleteBtn').on('click', function() { self.doCheckOut(); });
    },

    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#summaryCards').hide();
            $('#departureTable').closest('.card').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#summaryCards').show();
        $('#departureTable').closest('.card').show();
        self.propertyId = propertyId;
        self.loadData();
        // 60초 자동 갱신
        if (self.pollInterval) clearInterval(self.pollInterval);
        self.pollInterval = setInterval(function() { self.loadData(); }, 60000);
    },

    loadData: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/front-desk/departures',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                self.data = res.data;

                // 요약 카드 업데이트
                var lateCount = self.data.filter(function(d) { return d.lateCheckOut; }).length;
                var balanceCount = self.data.filter(function(d) { return (d.balance || 0) > 0; }).length;
                $('#departureTotal').text(self.data.length);
                $('#lateCount').text(lateCount);
                $('#balanceWarningCount').text(balanceCount);

                self.renderTable();
            },
            error: function() {
                HolaPms.alert('danger', '출발 예정 목록을 불러오지 못했습니다.');
            }
        });
    },

    renderTable: function() {
        var self = this;
        var keyword = ($('#departureSearch').val() || '').trim().toLowerCase();
        var filtered = self.data;
        if (keyword) {
            filtered = self.data.filter(function(d) {
                return (d.guestNameKo || '').toLowerCase().indexOf(keyword) >= 0
                    || (d.roomNumber || '').toLowerCase().indexOf(keyword) >= 0
                    || (d.masterReservationNo || '').toLowerCase().indexOf(keyword) >= 0;
            });
        }
        $('#departureCount').text(filtered.length);

        if (filtered.length === 0) {
            $('#departureBody').html('<tr><td colspan="10" class="text-center py-4 text-muted">' + (keyword ? '검색 결과가 없습니다.' : '오늘 출발 예정 예약이 없습니다.') + '</td></tr>');
            return;
        }
        var html = '';
        for (var i = 0; i < filtered.length; i++) {
            var d = filtered[i];
            var idx = self.data.indexOf(d);
            var balanceClass = d.balance > 0 ? 'text-danger' : '';
            var lateBadge = d.lateCheckOut ? '<span class="badge bg-warning text-dark">Late</span>' : '-';
            html += '<tr>'
                + '<td class="ps-3"><a href="/admin/reservations/' + d.masterReservationId + '">' + HolaPms.escapeHtml(d.masterReservationNo) + '</a></td>'
                + '<td>' + HolaPms.escapeHtml(d.guestNameKo || '-') + '</td>'
                + '<td>' + HolaPms.escapeHtml(d.roomNumber || '-') + '</td>'
                + '<td>' + d.checkIn + '</td>'
                + '<td>' + d.checkOut + '</td>'
                + '<td class="text-center">' + lateBadge + '</td>'
                + '<td class="text-end">' + Number(d.totalAmount || 0).toLocaleString() + '</td>'
                + '<td class="text-end">' + Number(d.paidAmount || 0).toLocaleString() + '</td>'
                + '<td class="text-end ' + balanceClass + '">' + Number(d.balance || 0).toLocaleString() + '</td>'
                + '<td class="text-center pe-3">'
                + '<button class="btn btn-sm btn-danger" onclick="FdDepartures.openCheckOut(' + idx + ')">'
                + '<i class="fas fa-sign-out-alt"></i></button></td></tr>';
        }
        $('#departureBody').html(html);
    },

    openCheckOut: function(index) {
        var self = this;
        self.selectedIndex = index;
        var d = self.data[index];

        $('#coGuest').text(d.guestNameKo || '-');
        $('#coRoom').text(d.roomNumber || '-');
        $('#coTotal').text(Number(d.totalAmount || 0).toLocaleString() + '원');
        $('#coPaid').text(Number(d.paidAmount || 0).toLocaleString() + '원');

        var balance = d.balance || 0;
        $('#coBalance').text(Number(balance).toLocaleString() + '원')
            .toggleClass('text-danger', balance > 0);
        $('#coBalanceWarning').toggleClass('d-none', balance <= 0);
        // 결제 링크: 예약 상세 페이지로 이동 (결제 탭)
        $('#coPayLink').attr('href', '/admin/reservations/' + d.masterReservationId);

        HolaPms.modal.show('checkOutModal');
    },

    doCheckOut: function() {
        var self = this;
        var d = self.data[self.selectedIndex];

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations/' + d.masterReservationId + '/status',
            method: 'PUT',
            data: JSON.stringify({ newStatus: 'CHECKED_OUT' }),
            success: function(res) {
                if (res.success) {
                    HolaPms.modal.hide('checkOutModal');
                    HolaPms.alert('success', d.guestNameKo + ' 체크아웃 완료 (객실: ' + d.roomNumber + ')');
                    self.reload();
                }
            },
            error: function(xhr) {
                var msg = '체크아웃 처리에 실패했습니다.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    msg = xhr.responseJSON.message;
                }
                HolaPms.alert('danger', msg);
            }
        });
    }
};

$(document).ready(function() { FdDepartures.init(); });
