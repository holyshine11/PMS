/**
 * 예약 관리 - 테이블뷰 (DataTable)
 */
var ReservationTableView = {
    propertyId: null,
    dataTable: null,

    /**
     * 초기화
     */
    init: function(propertyId) {
        this.propertyId = propertyId;
        var self = this;

        this.dataTable = $('#reservationTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: function(data, callback) {
                var pid = self.propertyId;
                if (!pid) {
                    callback({ data: [] });
                    return;
                }

                var params = self.buildQueryParams();
                var url = '/api/v1/properties/' + pid + '/reservations';
                if (params) {
                    url += '?' + params;
                }

                HolaPms.ajax({
                    url: url,
                    type: 'GET',
                    success: function(res) {
                        callback({ data: res.data || [] });
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
                    // 예약번호
                    data: 'masterReservationNo',
                    render: function(data, type, row) {
                        var id = row.id;
                        var display = HolaPms.escapeHtml(data || row.reservationNo || '-');
                        return '<a href="javascript:void(0)" class="text-primary" onclick="HolaPms.popup.openReservationDetail(' + id + ')">' + display + '</a>';
                    }
                },
                {
                    // 확인번호
                    data: 'confirmationNo',
                    render: HolaPms.renders.dashIfEmpty
                },
                {
                    // 상태
                    data: 'reservationStatus',
                    render: function(data) {
                        return ReservationTableView.getStatusBadge(data);
                    },
                    className: 'text-center'
                },
                {
                    // 예약자
                    data: 'guestNameKo',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    // 전화번호
                    data: 'phoneNumber',
                    render: HolaPms.renders.dashIfEmpty
                },
                {
                    // 체크인
                    data: 'masterCheckIn',
                    render: function(data) {
                        return data ? data.substring(0, 10) : '-';
                    },
                    className: 'text-center'
                },
                {
                    // 체크아웃
                    data: 'masterCheckOut',
                    render: function(data) {
                        return data ? data.substring(0, 10) : '-';
                    },
                    className: 'text-center'
                },
                {
                    // OTA 여부
                    data: 'isOtaManaged',
                    render: function(data) {
                        if (data === true || data === 'Y') {
                            return '<span class="badge bg-primary">Y</span>';
                        }
                        return '<span class="badge bg-secondary">N</span>';
                    },
                    className: 'text-center'
                },
                {
                    // 등록일시
                    data: 'createdAt',
                    render: function(data) {
                        if (!data) return '-';
                        return data.substring(0, 10);
                    },
                    className: 'text-center'
                },
                {
                    // 관리
                    data: null,
                    orderable: false,
                    render: function(data, type, row) {
                        var id = row.id;
                        return '<a href="javascript:void(0)" onclick="HolaPms.popup.openReservationDetail(' + id + ')" class="btn btn-sm btn-outline-primary">' +
                               '<i class="fas fa-eye"></i></a>';
                    },
                    className: 'text-center',
                    width: '80px'
                }
            ],
            order: [[0, 'desc']],
            dom: 'rtip'
        }));

        // 팝업 자식 창 메시지 수신 → DataTable 갱신
        HolaPms.popup.onChildMessage(function() {
            if (self.dataTable) self.dataTable.ajax.reload(null, false);
        });
    },

    /** 외부에서 전달된 검색 파라미터 (reload 시 갱신) */
    searchParams: {},

    /**
     * 검색 파라미터 조립
     */
    buildQueryParams: function() {
        var params = [];
        var p = this.searchParams || {};

        if (p.status) params.push('status=' + p.status);
        if (p.keyword) params.push('keyword=' + encodeURIComponent(p.keyword));
        if (p.checkInFrom) params.push('checkInFrom=' + p.checkInFrom);
        if (p.checkInTo) params.push('checkInTo=' + p.checkInTo);

        return params.join('&');
    },

    /**
     * 데이터 리로드
     */
    reload: function(params) {
        if (params) {
            this.searchParams = params;
        }
        if (this.dataTable) {
            this.dataTable.ajax.reload(null, true);
        }
    },

    /**
     * 상태별 배지 HTML
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
