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

        // sessionStorage에 저장된 월이 있으면 복원 (뒤로가기 대응)
        var savedYear = sessionStorage.getItem('cal_year');
        var savedMonth = sessionStorage.getItem('cal_month');
        if (savedYear && savedMonth) {
            this.currentYear = parseInt(savedYear);
            this.currentMonth = parseInt(savedMonth);
        } else {
            var today = new Date();
            this.currentYear = today.getFullYear();
            this.currentMonth = today.getMonth() + 1;
        }

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

        // 현재 월 상태를 sessionStorage에 보존 (뒤로가기 복원용)
        sessionStorage.setItem('cal_year', this.currentYear);
        sessionStorage.setItem('cal_month', this.currentMonth);

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
     * 키워드 검색 시 해당 예약 월로 자동 이동 후 로드
     */
    searchAndNavigate: function(params) {
        var self = this;
        if (!self.propertyId) return;

        // 키워드가 없으면 현재 월에서 그냥 로드
        if (!params || !params.keyword) {
            self.load(params);
            return;
        }

        // 리스트 API로 먼저 해당 예약의 체크인 월을 조회
        var listParams = 'keyword=' + encodeURIComponent(params.keyword);
        if (params.status) listParams += '&status=' + params.status;

        $.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations?' + listParams,
            method: 'GET',
            success: function(res) {
                var list = res.data || [];
                if (list.length > 0 && list[0].masterCheckIn) {
                    // 첫 번째 검색 결과의 체크인 월로 캘린더 이동
                    var parts = list[0].masterCheckIn.split('-');
                    self.currentYear = parseInt(parts[0]);
                    self.currentMonth = parseInt(parts[1]);
                }
                self.load(params);
            },
            error: function() {
                self.load(params);
            }
        });
    },

    /**
     * 캘린더 전체 렌더링 (2개월)
     */
    render: function() {
        var html = '';

        // 헤더: 월 이동 + 오늘 + 상태 범례
        html += '<div class="d-flex align-items-center justify-content-between mb-3">';
        html += '<div class="d-flex align-items-center">';
        html += '<button class="btn btn-outline-secondary btn-sm me-2" id="calPrevBtn"><i class="fas fa-chevron-left"></i></button>';
        html += '<h5 class="fw-bold mb-0">' + this.currentYear + '.' + this.padZero(this.currentMonth) + '</h5>';
        html += '<button class="btn btn-outline-secondary btn-sm ms-2" id="calNextBtn"><i class="fas fa-chevron-right"></i></button>';
        html += '<button class="btn btn-outline-primary btn-sm ms-3" id="calTodayBtn">오늘</button>';
        html += '</div>';
        // 상태 컬러 범례
        html += '<div class="d-flex align-items-center gap-2 flex-wrap" style="font-size:0.75rem;">';
        var legendItems = [
            { label: '예약', status: 'RESERVED' },
            { label: '체크인', status: 'CHECK_IN' },
            { label: '투숙중', status: 'INHOUSE' },
            { label: '체크아웃', status: 'CHECKED_OUT' },
            { label: '취소', status: 'CANCELED' },
            { label: '노쇼', status: 'NO_SHOW' }
        ];
        for (var li = 0; li < legendItems.length; li++) {
            var lc = this.getStatusColor(legendItems[li].status);
            html += '<span class="d-inline-flex align-items-center">';
            html += '<span style="display:inline-block; width:12px; height:12px; border-radius:2px; background:' + lc.bg + '; border:1px solid #ccc; margin-right:3px;"></span>';
            html += legendItems[li].label;
            html += '</span>';
        }
        html += '</div>';
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

                    html += '<div class="col calendar-cell" style="' + cellStyle + '" data-date="' + dateStr + '" ondblclick="ReservationCalendarView.onCellDblClick(\'' + dateStr + '\')">';

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
     * 셀 내 예약 항목 렌더링 (연속 바 + 상태/예약번호 포함)
     */
    renderCellItems: function(items, dateStr) {
        if (!items || items.length === 0) return '';

        // ID 순 정렬 (일자별 일관된 순서 보장 → 연속 바 위치 유지)
        var sorted = items.slice().sort(function(a, b) { return a.id - b.id; });
        var showCount = Math.min(sorted.length, this.MAX_ITEMS_PER_CELL);

        var html = '<div class="pb-1">';

        for (var i = 0; i < showCount; i++) {
            var item = sorted[i];
            var pos = this.getBarPosition(item, dateStr);
            var statusColor = this.getStatusColor(item.reservationStatus);
            var statusLabel = this.getStatusLabel(item.reservationStatus);
            var name = HolaPms.escapeHtml(item.guestNameMasked || '-');
            var room = item.roomInfo ? HolaPms.escapeHtml(item.roomInfo) : '';
            var resNo = item.masterReservationNo ? HolaPms.escapeHtml(item.masterReservationNo) : '';

            // 위치별 border-radius (연속 바 효과)
            var radius;
            if (pos === 'single') radius = '3px';
            else if (pos === 'start') radius = '3px 0 0 3px';
            else if (pos === 'end') radius = '0 3px 3px 0';
            else radius = '0';

            var tooltip = '[' + statusLabel + '] ' + resNo + ' ' + name + (room ? ' | ' + room : '');

            html += '<div class="calendar-item mb-1" style="cursor:pointer; font-size:0.7rem; padding:1px 4px; ';
            html += 'border-radius:' + radius + '; ';
            html += 'background-color:' + statusColor.bg + '; color:' + statusColor.text + '; ';
            html += 'white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" ';
            html += 'onclick="window.location.href=\'/admin/reservations/' + item.id + '\'" ';
            html += 'title="' + tooltip + '">';

            if (pos === 'start' || pos === 'single') {
                // 시작일/1박: [상태] + 이름 + 호수 (예약 상태만 예약번호 추가)
                html += '<span style="font-weight:600;">[' + statusLabel + ']</span> ';
                if (item.reservationStatus === 'RESERVED' && resNo) {
                    html += '<span style="opacity:0.7;">' + resNo + '</span> ';
                }
                html += name;
                if (room) html += ' <span style="opacity:0.7;">' + room + '</span>';
            } else {
                // 중간/끝일: [상태] + 이름 (바 연속성 유지)
                html += '<span style="font-weight:600;">[' + statusLabel + ']</span> ';
                html += name;
            }

            html += '</div>';
        }

        // +N건 더보기
        if (sorted.length > this.MAX_ITEMS_PER_CELL) {
            var moreCount = sorted.length - this.MAX_ITEMS_PER_CELL;
            html += '<a href="/admin/reservations/daily?date=' + dateStr + '" class="d-block text-center" ';
            html += 'style="font-size:0.7rem; color:#0582CA; text-decoration:none; cursor:pointer;">+' + moreCount + '건 더보기</a>';
        }

        html += '</div>';
        return html;
    },

    /**
     * 예약 바 위치 판정 (start/middle/end/single)
     */
    getBarPosition: function(item, dateStr) {
        var checkIn = item.masterCheckIn;
        // 체크아웃 전날이 마지막 숙박일
        var coDate = new Date(item.masterCheckOut + 'T00:00:00');
        coDate.setDate(coDate.getDate() - 1);
        var lastNight = this.formatDate(coDate);

        var isStart = (dateStr === checkIn);
        var isEnd = (dateStr === lastNight);

        if (isStart && isEnd) return 'single';
        if (isStart) return 'start';
        if (isEnd) return 'end';
        return 'middle';
    },

    /**
     * 상태 라벨
     */
    getStatusLabel: function(status) {
        var labels = {
            'RESERVED': '예약',
            'CHECK_IN': '체크인',
            'INHOUSE': '투숙중',
            'CHECKED_OUT': '체크아웃',
            'CANCELED': '취소',
            'NO_SHOW': '노쇼'
        };
        return labels[status] || status;
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
     * 빈 날짜 셀 더블클릭 → 신규 예약 등록 (해당 일자)
     */
    onCellDblClick: function(dateStr) {
        window.location.href = '/admin/reservations/new?checkInDate=' + dateStr;
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
