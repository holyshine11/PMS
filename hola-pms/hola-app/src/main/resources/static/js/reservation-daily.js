/**
 * 일별 예약 목록 (캘린더 "+N건 더보기" 클릭 시 이동하는 페이지)
 */
var ReservationDaily = {
    propertyId: null,
    date: null,
    dataTable: null,

    /**
     * 초기화
     */
    init: function(propertyId, date) {
        this.propertyId = propertyId;
        this.date = date;
        var self = this;

        this.dataTable = $('#dailyTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: function(data, callback) {
                var pid = self.propertyId;
                if (!pid || !self.date) {
                    callback({ data: [] });
                    return;
                }

                // 캘린더 API로 해당 날짜 데이터 조회
                var url = '/api/v1/properties/' + pid + '/reservations/calendar'
                    + '?startDate=' + self.date + '&endDate=' + self.date;

                $.ajax({
                    url: url,
                    type: 'GET',
                    dataType: 'json',
                    success: function(res) {
                        var dayData = (res.data && res.data[self.date]) || [];
                        callback({ data: dayData });
                    },
                    error: function() {
                        callback({ data: [] });
                    }
                });
            },
            serverSide: false,
            pageLength: 20,
            columns: [
                {
                    data: 'masterReservationNo',
                    render: function(data, type, row) {
                        var display = HolaPms.escapeHtml(data || '-');
                        return '<a href="/admin/reservations/' + row.id + '" class="text-primary">' + display + '</a>';
                    }
                },
                {
                    data: 'reservationStatus',
                    render: function(data) {
                        return ReservationDaily.getStatusBadge(data);
                    },
                    className: 'text-center'
                },
                {
                    data: 'guestNameMasked',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    data: null,
                    render: function(data, type, row) {
                        var parts = [];
                        if (row.roomTypeName) parts.push(HolaPms.escapeHtml(row.roomTypeName));
                        if (row.roomInfo) parts.push(HolaPms.escapeHtml(row.roomInfo));
                        return parts.length > 0 ? parts.join(' / ') : '-';
                    }
                },
                {
                    data: 'masterCheckIn',
                    render: function(data) {
                        return data ? data.substring(0, 10) : '-';
                    },
                    className: 'text-center'
                },
                {
                    data: 'masterCheckOut',
                    render: function(data) {
                        return data ? data.substring(0, 10) : '-';
                    },
                    className: 'text-center'
                },
                {
                    data: null,
                    orderable: false,
                    render: function(data, type, row) {
                        return '<a href="/admin/reservations/' + row.id + '" class="btn btn-sm btn-outline-primary">' +
                               '<i class="fas fa-eye"></i></a>';
                    },
                    className: 'text-center',
                    width: '80px'
                }
            ],
            order: [[0, 'desc']],
            dom: 'rtip'
        }));
    },

    /**
     * 리로드
     */
    reload: function() {
        this.propertyId = HolaPms.context.getPropertyId();
        if (this.dataTable) {
            this.dataTable.ajax.reload(null, true);
        }
    },

    /**
     * 상태별 배지
     */
    getStatusBadge: function(status) {
        var map = {
            'RESERVED':    { label: '예약',       bg: '#0582CA' },
            'CHECK_IN':    { label: '체크인',     bg: '#17a2b8' },
            'INHOUSE':     { label: '투숙중',     bg: '#003554' },
            'CHECKED_OUT': { label: '체크아웃',   bg: '#6c757d' },
            'CANCELED':    { label: '취소',       bg: '#EF476F' },
            'NO_SHOW':     { label: '노쇼',       bg: '#ffc107', color: '#000' }
        };

        var info = map[status] || { label: status || '-', bg: '#6c757d' };
        var textColor = info.color || '#fff';
        return '<span class="badge" style="background-color:' + info.bg + '; color:' + textColor + '">' + info.label + '</span>';
    }
};
