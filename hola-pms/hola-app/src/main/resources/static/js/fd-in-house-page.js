/**
 * 프론트데스크 투숙 페이지
 */
var FdInHouse = {
    propertyId: null,

    init: function() {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function() {
        var self = this;
        $(document).on('hola:contextChange', function() { self.reload(); });
    },

    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#inHouseTable').closest('.card').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#inHouseTable').closest('.card').show();
        self.propertyId = propertyId;
        self.loadData();
    },

    loadData: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/front-desk/in-house',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var data = res.data;
                $('#inHouseCount').text(data.length);

                if (data.length === 0) {
                    $('#inHouseBody').html('<tr><td colspan="10" class="text-center py-4 text-muted">현재 투숙중인 예약이 없습니다.</td></tr>');
                    return;
                }

                var html = '';
                for (var i = 0; i < data.length; i++) {
                    var d = data[i];
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
        });
    }
};

$(document).ready(function() { FdInHouse.init(); });
