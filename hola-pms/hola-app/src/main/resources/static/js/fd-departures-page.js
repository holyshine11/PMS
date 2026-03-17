/**
 * 프론트데스크 출발 페이지 + 체크아웃
 */
var FdDepartures = {
    propertyId: null,
    data: [],
    selectedIndex: -1,

    init: function() {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function() {
        var self = this;
        $(document).on('hola:contextChange', function() { self.reload(); });
        $('#coCompleteBtn').on('click', function() { self.doCheckOut(); });
    },

    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#departureTable').closest('.card').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#departureTable').closest('.card').show();
        self.propertyId = propertyId;
        self.loadData();
    },

    loadData: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/front-desk/departures',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                self.data = res.data;
                $('#departureCount').text(self.data.length);

                if (self.data.length === 0) {
                    $('#departureBody').html('<tr><td colspan="10" class="text-center py-4 text-muted">오늘 출발 예정 예약이 없습니다.</td></tr>');
                    return;
                }

                var html = '';
                for (var i = 0; i < self.data.length; i++) {
                    var d = self.data[i];
                    var balanceClass = d.balance > 0 ? 'text-danger' : '';
                    var lateBadge = d.lateCheckOut ? '<span class="badge bg-warning text-dark">Late</span>' : '-';
                    html += '<tr>'
                        + '<td class="ps-3">' + HolaPms.escapeHtml(d.masterReservationNo) + '</td>'
                        + '<td>' + HolaPms.escapeHtml(d.guestNameKo || '-') + '</td>'
                        + '<td>' + HolaPms.escapeHtml(d.roomNumber || '-') + '</td>'
                        + '<td>' + d.checkIn + '</td>'
                        + '<td>' + d.checkOut + '</td>'
                        + '<td class="text-center">' + lateBadge + '</td>'
                        + '<td class="text-end">' + Number(d.totalAmount || 0).toLocaleString() + '</td>'
                        + '<td class="text-end">' + Number(d.paidAmount || 0).toLocaleString() + '</td>'
                        + '<td class="text-end ' + balanceClass + '">' + Number(d.balance || 0).toLocaleString() + '</td>'
                        + '<td class="text-center pe-3">'
                        + '<button class="btn btn-sm btn-danger" onclick="FdDepartures.openCheckOut(' + i + ')">'
                        + '<i class="fas fa-sign-out-alt"></i></button></td></tr>';
                }
                $('#departureBody').html(html);
            }
        });
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
            }
        });
    }
};

$(document).ready(function() { FdDepartures.init(); });
