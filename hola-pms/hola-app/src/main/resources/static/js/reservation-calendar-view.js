/**
 * 예약 관리 - 캘린더뷰 (2개월 표시, 월 단위 탐색)
 * 셀당 최대 10건 표시, 초과 시 "+N건 더보기" → 일별 예약 페이지 이동
 */
var ReservationCalendarView = {
    propertyId: null,
    currentYear: null,
    currentMonth: null,
    data: {},
    MAX_ITEMS_PER_CELL: 10,

    /**
     * 초기화
     */
    init: function(propertyId) {
        this.propertyId = propertyId;
        var today = new Date();
        this.currentYear = today.getFullYear();
        this.currentMonth = today.getMonth() + 1;
        this.bindEvents();
        this.load();
    },

    /**
     * 이벤트 바인딩
     */
    bindEvents: function() {
        var self = this;

        $('#calPrevBtn').off('click').on('click', function() {
            self.navigateMonth(-1);
        });

        $('#calNextBtn').off('click').on('click', function() {
            self.navigateMonth(1);
        });

        $('#calTodayBtn').off('click').on('click', function() {
            var today = new Date();
            self.currentYear = today.getFullYear();
            self.currentMonth = today.getMonth() + 1;
            self.load();
        });
    },

    /**
     * 월 이동
     */
    navigateMonth: function(offset) {
        this.currentMonth += offset;
        if (this.currentMonth > 12) {
            this.currentMonth = 1;
            this.currentYear++;
        } else if (this.currentMonth < 1) {
            this.currentMonth = 12;
            this.currentYear--;
        }
        this.load();
    },

    /**
     * 현재 표시 기간 계산 (2개월)
     */
    getDateRange: function() {
        var startDate = new Date(this.currentYear, this.currentMonth - 1, 1);
        var endMonth = this.currentMonth + 1;
        var endYear = this.currentYear;
        if (endMonth > 12) {
            endMonth = 1;
            endYear++;
        }
        // 다음 달의 마지막 날
        var endDate = new Date(endYear, endMonth, 0);

        return {
            startDate: this.formatDate(startDate),
            endDate: this.formatDate(endDate)
        };
    },

    /**
     * API 호출
     */
    load: function(params) {
        var self = this;
        if (!this.propertyId) return;

        var range = this.getDateRange();
        var queryParams = [
            'startDate=' + range.startDate,
            'endDate=' + range.endDate
        ];

        // 검색 파라미터
        if (params && params.status) queryParams.push('status=' + params.status);
        if (params && params.keyword) queryParams.push('keyword=' + encodeURIComponent(params.keyword));

        var url = '/api/v1/properties/' + this.propertyId + '/reservations/calendar?' + queryParams.join('&');

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                self.data = res.data || {};
                self.render();
            },
            error: function() {
                self.data = {};
                self.render();
            }
        });
    },

    /**
     * 캘린더 전체 렌더링 (2개월)
     */
    render: function() {
        var html = '';

        // 헤더: 월 이동 + 오늘
        html += '<div class="d-flex align-items-center mb-3">';
        html += '<button class="btn btn-outline-secondary btn-sm me-2" id="calPrevBtn"><i class="fas fa-chevron-left"></i></button>';
        html += '<h5 class="fw-bold mb-0">' + this.currentYear + '.' + this.padZero(this.currentMonth) + '</h5>';
        html += '<button class="btn btn-outline-secondary btn-sm ms-2" id="calNextBtn"><i class="fas fa-chevron-right"></i></button>';
        html += '<button class="btn btn-outline-primary btn-sm ms-3" id="calTodayBtn">오늘</button>';
        html += '</div>';

        // 첫 번째 달
        html += this.renderMonth(this.currentYear, this.currentMonth);

        // 두 번째 달
        var nextMonth = this.currentMonth + 1;
        var nextYear = this.currentYear;
        if (nextMonth > 12) { nextMonth = 1; nextYear++; }
        html += '<div class="mt-4"></div>';
        html += this.renderMonth(nextYear, nextMonth);

        $('#calendarViewContainer').html(html);
        this.bindEvents();
    },

    /**
     * 단일 월 렌더링
     */
    renderMonth: function(year, month) {
        var html = '';
        var today = new Date();
        var todayStr = this.formatDate(today);

        // 월 헤더
        html += '<div class="card border-0 shadow-sm">';
        html += '<div class="card-header bg-white border-bottom py-2">';
        html += '<h6 class="fw-bold mb-0">' + year + '년 ' + month + '월</h6>';
        html += '</div>';
        html += '<div class="card-body p-0">';

        // 요일 헤더
        var weekdays = ['일', '월', '화', '수', '목', '금', '토'];
        html += '<div class="row g-0 border-bottom">';
        for (var w = 0; w < 7; w++) {
            var dayColor = w === 0 ? 'color:#EF476F;' : (w === 6 ? 'color:#0582CA;' : '');
            html += '<div class="col text-center py-1" style="font-size:0.8rem; font-weight:600; ' + dayColor + '">' + weekdays[w] + '</div>';
        }
        html += '</div>';

        // 날짜 계산
        var firstDay = new Date(year, month - 1, 1);
        var lastDay = new Date(year, month, 0);
        var startDow = firstDay.getDay();
        var totalDays = lastDay.getDate();

        var day = 1;
        var weeks = Math.ceil((startDow + totalDays) / 7);

        for (var wk = 0; wk < weeks; wk++) {
            html += '<div class="row g-0">';
            for (var dow = 0; dow < 7; dow++) {
                var cellDay = wk * 7 + dow - startDow + 1;
                if (cellDay < 1 || cellDay > totalDays) {
                    html += '<div class="col calendar-cell" style="min-height:100px; border:1px solid #eee; background:#fafafa;"></div>';
                } else {
                    var dateStr = year + '-' + this.padZero(month) + '-' + this.padZero(cellDay);
                    var isToday = dateStr === todayStr;
                    var isWeekend = dow === 0 || dow === 6;
                    var cellStyle = 'min-height:100px; border:1px solid #eee;';
                    if (isToday) cellStyle += ' background:#e8f4fd;';

                    html += '<div class="col calendar-cell" style="' + cellStyle + '">';

                    // 날짜 숫자
                    var numStyle = 'font-size:0.78rem; font-weight:600;';
                    if (dow === 0) numStyle += ' color:#EF476F;';
                    else if (dow === 6) numStyle += ' color:#0582CA;';
                    if (isToday) numStyle += ' background:#0582CA; color:#fff; border-radius:50%; width:22px; height:22px; display:inline-flex; align-items:center; justify-content:center;';

                    html += '<div class="px-1 pt-1"><span style="' + numStyle + '">' + cellDay + '</span></div>';

                    // 예약 데이터
                    var cellData = this.data[dateStr] || [];
                    html += this.renderCellItems(cellData, dateStr);

                    html += '</div>';
                }
            }
            html += '</div>';
        }

        html += '</div>'; // card-body
        html += '</div>'; // card

        return html;
    },

    /**
     * 셀 내 예약 항목 렌더링 (최대 10건 + 더보기)
     */
    renderCellItems: function(items, dateStr) {
        if (!items || items.length === 0) return '';

        var html = '<div class="px-1 pb-1">';
        var showCount = Math.min(items.length, this.MAX_ITEMS_PER_CELL);

        for (var i = 0; i < showCount; i++) {
            var item = items[i];
            var statusColor = this.getStatusColor(item.reservationStatus);
            var name = HolaPms.escapeHtml(item.guestNameMasked || '-');
            var room = item.roomInfo ? HolaPms.escapeHtml(item.roomInfo) : '';

            html += '<div class="calendar-item mb-1" style="cursor:pointer; font-size:0.72rem; padding:1px 4px; border-radius:3px; ';
            html += 'background-color:' + statusColor.bg + '; color:' + statusColor.text + '; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" ';
            html += 'onclick="window.location.href=\'/admin/reservations/' + item.id + '\'" ';
            html += 'title="' + name + (room ? ' | ' + room : '') + '">';
            html += name;
            if (room) html += ' <span style="opacity:0.8;">' + room + '</span>';
            html += '</div>';
        }

        // +N건 더보기
        if (items.length > this.MAX_ITEMS_PER_CELL) {
            var moreCount = items.length - this.MAX_ITEMS_PER_CELL;
            html += '<a href="/admin/reservations/daily?date=' + dateStr + '" class="d-block text-center" ';
            html += 'style="font-size:0.7rem; color:#0582CA; text-decoration:none; cursor:pointer;">+' + moreCount + '건 더보기</a>';
        }

        html += '</div>';
        return html;
    },

    /**
     * 상태별 색상
     */
    getStatusColor: function(status) {
        var map = {
            'RESERVED':    { bg: '#d4edff', text: '#0582CA' },
            'CHECK_IN':    { bg: '#d1ecf1', text: '#0c5460' },
            'INHOUSE':     { bg: '#003554', text: '#ffffff' },
            'CHECKED_OUT': { bg: '#e2e3e5', text: '#383d41' },
            'CANCELED':    { bg: '#f8d7da', text: '#721c24' },
            'NO_SHOW':     { bg: '#fff3cd', text: '#856404' }
        };
        return map[status] || { bg: '#e2e3e5', text: '#383d41' };
    },

    /**
     * 현재 보고 있는 월 정보 반환 (뷰 전환 시 동기화용)
     */
    getCurrentMonthRange: function() {
        return {
            year: this.currentYear,
            month: this.currentMonth
        };
    },

    /**
     * 날짜 포맷 (YYYY-MM-DD)
     */
    formatDate: function(date) {
        var y = date.getFullYear();
        var m = this.padZero(date.getMonth() + 1);
        var d = this.padZero(date.getDate());
        return y + '-' + m + '-' + d;
    },

    /**
     * 숫자 2자리 패딩
     */
    padZero: function(n) {
        return n < 10 ? '0' + n : '' + n;
    }
};
