/**
 * 예약 관리 - 카드뷰 (칸반 스타일)
 * 4컬럼: 체크인 예정 | 투숙중 | 체크아웃 예정 | 취소/노쇼
 */
var ReservationCardView = {
    propertyId: null,
    data: [],

    /**
     * 초기화
     */
    init: function(propertyId) {
        this.propertyId = propertyId;
        this.load({
            date: $('#selectedDate').val(),
            status: '',
            keyword: ''
        });
    },

    /**
     * API 호출 후 데이터 로드
     */
    load: function(params) {
        var self = this;
        var pid = this.propertyId;
        if (!pid) return;

        var queryParams = [];
        if (params.date) queryParams.push('date=' + params.date);
        if (params.status) queryParams.push('status=' + params.status);
        if (params.keyword) queryParams.push('keyword=' + encodeURIComponent(params.keyword));

        var url = '/api/v1/properties/' + pid + '/reservations';
        if (queryParams.length > 0) {
            url += '?' + queryParams.join('&');
        }

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                self.data = res.data || [];
                self.render(self.data, params);
            },
            error: function(xhr) {
                // API 미구현 시 빈 데이터로 렌더링
                self.data = [];
                self.render(self.data, params);
            }
        });
    },

    /**
     * 칸반 스타일 4컬럼 렌더링
     */
    render: function(data, params) {
        var selectedDate = params && params.date ? params.date : '';

        // 데이터 분류
        var checkInList = [];    // 체크인 예정: 선택 날짜에 체크인인 RESERVED/CHECK_IN
        var inhouseList = [];    // 투숙중: INHOUSE
        var checkOutList = [];   // 체크아웃 예정: 선택 날짜에 체크아웃인 INHOUSE
        var cancelList = [];     // 취소/노쇼: CANCELED/NO_SHOW

        for (var i = 0; i < data.length; i++) {
            var r = data[i];
            var status = r.status || '';
            var checkIn = (r.checkInDate || '').substring(0, 10);
            var checkOut = (r.checkOutDate || '').substring(0, 10);

            if (status === 'CANCELED' || status === 'NO_SHOW') {
                cancelList.push(r);
            } else if (status === 'INHOUSE') {
                // INHOUSE이면서 선택 날짜가 체크아웃일이면 체크아웃 예정에도 표시
                if (selectedDate && checkOut === selectedDate) {
                    checkOutList.push(r);
                } else {
                    inhouseList.push(r);
                }
            } else if (status === 'RESERVED' || status === 'CHECK_IN') {
                if (selectedDate && checkIn === selectedDate) {
                    checkInList.push(r);
                } else {
                    checkInList.push(r);
                }
            } else if (status === 'CHECKED_OUT') {
                // 체크아웃 완료는 체크아웃 예정에 표시
                checkOutList.push(r);
            }
        }

        // 컬럼 정의
        var columns = [
            { title: '체크인 예정', color: '#0582CA', icon: 'fa-sign-in-alt', data: checkInList },
            { title: '투숙중', color: '#003554', icon: 'fa-bed', data: inhouseList },
            { title: '체크아웃 예정', color: '#051923', icon: 'fa-sign-out-alt', data: checkOutList },
            { title: '취소 / 노쇼', color: '#EF476F', icon: 'fa-times-circle', data: cancelList }
        ];

        var html = '<div class="row g-3">';

        for (var c = 0; c < columns.length; c++) {
            var col = columns[c];
            html += '<div class="col-md-3">';
            // 컬럼 헤더
            html += '<div class="d-flex align-items-center mb-2 px-1">';
            html += '<i class="fas ' + col.icon + ' me-2" style="color:' + col.color + '"></i>';
            html += '<span style="color:' + col.color + '; font-weight:600;">' + col.title + '</span>';
            html += '<span class="badge ms-auto" style="background-color:' + col.color + '">' + col.data.length + '</span>';
            html += '</div>';

            // 카드 목록 컨테이너
            html += '<div class="reservation-column" style="min-height:200px; background:#f8f9fa; border-radius:8px; padding:8px;">';

            if (col.data.length === 0) {
                html += '<div class="text-center text-muted py-4">';
                html += '<i class="fas fa-inbox mb-2" style="font-size:1.5rem;"></i>';
                html += '<div>해당 예약이 없습니다.</div>';
                html += '</div>';
            } else {
                for (var j = 0; j < col.data.length; j++) {
                    html += this.renderCard(col.data[j]);
                }
            }

            html += '</div>'; // .reservation-column
            html += '</div>'; // .col-md-3
        }

        html += '</div>'; // .row
        $('#cardViewContainer').html(html);
    },

    /**
     * 개별 카드 HTML 렌더링
     */
    renderCard: function(reservation) {
        var id = reservation.id || '';
        var badge = this.getStatusBadge(reservation.status);
        var guestName = HolaPms.escapeHtml(reservation.guestNameKo || reservation.guestName || '-');
        var roomInfo = '';
        if (reservation.roomTypeName) {
            roomInfo = HolaPms.escapeHtml(reservation.roomTypeName);
            if (reservation.roomNumber) {
                roomInfo += ' - ' + HolaPms.escapeHtml(reservation.roomNumber) + '호';
            }
        }
        var checkIn = (reservation.checkInDate || '').substring(5, 10);
        var checkOut = (reservation.checkOutDate || '').substring(5, 10);
        var dateRange = checkIn && checkOut ? checkIn + ' ~ ' + checkOut : '-';
        var reservationNo = HolaPms.escapeHtml(reservation.masterReservationNo || reservation.reservationNo || '-');

        var html = '';
        html += '<div class="card border-0 shadow-sm mb-2" style="cursor:pointer;" ';
        html += 'onclick="window.location.href=\'/admin/reservations/' + id + '\'">';
        html += '<div class="card-body py-2 px-3">';
        html += '<div class="d-flex justify-content-between align-items-center mb-1">';
        html += badge;
        html += '</div>';
        html += '<div style="font-size:0.95rem; font-weight:600;">' + guestName + '</div>';
        if (roomInfo) {
            html += '<div class="text-muted" style="font-size:0.82rem;">' + roomInfo + '</div>';
        }
        html += '<div class="text-muted" style="font-size:0.82rem;">';
        html += '<i class="fas fa-calendar-alt me-1"></i>' + dateRange;
        html += '</div>';
        html += '<div class="text-muted" style="font-size:0.78rem;">' + reservationNo + '</div>';
        html += '</div>'; // .card-body
        html += '</div>'; // .card

        return html;
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
        return '<span class="badge" style="background-color:' + info.bg + '; color:' + textColor + '; font-size:0.72rem;">' + info.label + '</span>';
    }
};
