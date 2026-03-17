/**
 * 프론트데스크 투숙 페이지
 */
var FdInHouse = {
    propertyId: null,
    data: [],
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
        $('#inHouseSearch').on('input', function() { self.renderTable(); });
    },

    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#summaryCards').hide();
            $('#inHouseTable').closest('.card').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#summaryCards').show();
        $('#inHouseTable').closest('.card').show();
        self.propertyId = propertyId;
        self.loadData();
        // 60초 자동 갱신
        if (self.pollInterval) clearInterval(self.pollInterval);
        self.pollInterval = setInterval(function() { self.loadData(); }, 60000);
    },

    loadData: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/front-desk/in-house',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                self.data = res.data;

                // 요약 카드 업데이트
                var todayStr = new Date().toISOString().substring(0, 10);
                var todayCheckout = self.data.filter(function(d) { return d.checkOut === todayStr; }).length;
                var balanceCount = self.data.filter(function(d) { return (d.balance || 0) > 0; }).length;
                $('#inHouseTotal').text(self.data.length);
                $('#todayCheckoutCount').text(todayCheckout);
                $('#balanceWarningCount').text(balanceCount);

                self.renderTable();
            },
            error: function() {
                HolaPms.alert('danger', '투숙중 목록을 불러오지 못했습니다.');
            }
        });
    },

    renderTable: function() {
        var self = this;
        var keyword = ($('#inHouseSearch').val() || '').trim().toLowerCase();
        var filtered = self.data;
        if (keyword) {
            filtered = self.data.filter(function(d) {
                return (d.guestNameKo || '').toLowerCase().indexOf(keyword) >= 0
                    || (d.roomNumber || '').toLowerCase().indexOf(keyword) >= 0
                    || (d.masterReservationNo || '').toLowerCase().indexOf(keyword) >= 0;
            });
        }
        $('#inHouseCount').text(filtered.length);

        if (filtered.length === 0) {
            $('#inHouseBody').html('<tr><td colspan="10" class="text-center py-4 text-muted">' + (keyword ? '검색 결과가 없습니다.' : '현재 투숙중인 예약이 없습니다.') + '</td></tr>');
            return;
        }

        var html = '';
        for (var i = 0; i < filtered.length; i++) {
            var d = filtered[i];
            var balanceClass = d.balance > 0 ? 'text-danger' : '';
            // 실제 체크인 시간 HH:mm 포맷
            var actualCi = '-';
            if (d.actualCheckInTime) {
                var t = d.actualCheckInTime;
                actualCi = t.length >= 16 ? t.substring(11, 16) : t;
            }
            html += '<tr>'
                + '<td class="ps-3"><a href="/admin/reservations/' + d.masterReservationId + '">' + HolaPms.escapeHtml(d.masterReservationNo) + '</a></td>'
                + '<td>' + HolaPms.escapeHtml(d.guestNameKo || '-') + '</td>'
                + '<td>' + HolaPms.escapeHtml(d.roomTypeName || '-') + '</td>'
                + '<td>' + HolaPms.escapeHtml(d.roomNumber || '-') + '</td>'
                + '<td>' + d.checkIn + '</td>'
                + '<td>' + actualCi + '</td>'
                + '<td>' + d.checkOut + '</td>'
                + '<td class="text-end">' + Number(d.totalAmount || 0).toLocaleString() + '</td>'
                + '<td class="text-end">' + Number(d.paidAmount || 0).toLocaleString() + '</td>'
                + '<td class="text-end pe-3 ' + balanceClass + '">' + Number(d.balance || 0).toLocaleString() + '</td>'
                + '</tr>';
        }
        $('#inHouseBody').html(html);
    }
};

$(document).ready(function() { FdInHouse.init(); });
