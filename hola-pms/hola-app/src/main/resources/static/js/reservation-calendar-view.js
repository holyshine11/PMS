/**
 * 예약 관리 - 캘린더뷰 (2개월 표시, 월 단위 탐색)
 * 예약 바가 셀 경계를 넘어 연속 표시 (Gantt 스타일 오버레이)
 * 셀당 최대 10건 표시, 초과 시 "+N" → 일별 예약 페이지 이동
 */
var ReservationCalendarView = {
    propertyId: null,
    currentYear: null,
    currentMonth: null,
    data: {},
    MAX_ITEMS_PER_CELL: 10,
    LANE_HEIGHT: 22,
    DATE_ROW_HEIGHT: 28,

    /**
     * 초기화
     */
    init: function(propertyId) {
        this.propertyId = propertyId;

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

        if (params && params.status) queryParams.push('status=' + params.status);
        if (params && params.keyword) queryParams.push('keyword=' + encodeURIComponent(params.keyword));

        var url = '/api/v1/properties/' + this.propertyId + '/reservations/calendar?' + queryParams.join('&');

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

        if (!params || !params.keyword) {
            self.load(params);
            return;
        }

        var listParams = 'keyword=' + encodeURIComponent(params.keyword);
        if (params.status) listParams += '&status=' + params.status;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations?' + listParams,
            type: 'GET',
            success: function(res) {
                var list = res.data || [];
                if (list.length > 0 && list[0].masterCheckIn) {
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
     * 단일 월 렌더링 (오버레이 기반 Gantt 스타일)
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
        var weeks = Math.ceil((startDow + totalDays) / 7);

        for (var wk = 0; wk < weeks; wk++) {
            // 주간 열 배열 (7열: null 또는 dateStr)
            var weekColumns = [];
            for (var wd = 0; wd < 7; wd++) {
                var cd = wk * 7 + wd - startDow + 1;
                if (cd >= 1 && cd <= totalDays) {
                    weekColumns.push(year + '-' + this.padZero(month) + '-' + this.padZero(cd));
                } else {
                    weekColumns.push(null);
                }
            }

            // 레이아웃 계산
            var layout = this.calculateWeekLayout(weekColumns);
            var visibleLanes = Math.min(layout.maxLane + 1, this.MAX_ITEMS_PER_CELL);
            var lanesHeight = visibleLanes * this.LANE_HEIGHT;
            var minCellHeight = this.DATE_ROW_HEIGHT + lanesHeight + 4;

            // 날짜별 숨김 항목 수
            var hiddenPerDate = {};
            for (var bi = 0; bi < layout.bars.length; bi++) {
                if (layout.bars[bi].lane >= visibleLanes) {
                    for (var hc = layout.bars[bi].startCol; hc <= layout.bars[bi].endCol; hc++) {
                        if (weekColumns[hc]) {
                            hiddenPerDate[weekColumns[hc]] = (hiddenPerDate[weekColumns[hc]] || 0) + 1;
                        }
                    }
                }
            }

            // 주간 래퍼 (position: relative → 오버레이 기준)
            html += '<div style="position: relative;">';

            // 날짜 셀 그리드 (숫자만 표시, 예약 바는 오버레이에서 렌더링)
            html += '<div class="row g-0">';
            for (var dow = 0; dow < 7; dow++) {
                if (weekColumns[dow] === null) {
                    html += '<div class="col calendar-cell" style="min-height:' + minCellHeight + 'px; border:1px solid #eee; background:#fafafa;"></div>';
                } else {
                    var dateStr = weekColumns[dow];
                    var cellDay = wk * 7 + dow - startDow + 1;
                    var isToday = dateStr === todayStr;
                    var cellStyle = 'min-height:' + minCellHeight + 'px; border:1px solid #eee;';
                    if (isToday) cellStyle += ' background:#e8f4fd;';

                    html += '<div class="col calendar-cell" style="' + cellStyle + '" data-date="' + dateStr + '" ondblclick="ReservationCalendarView.onCellDblClick(\'' + dateStr + '\')">';

                    // 날짜 숫자
                    var numStyle = 'font-size:0.78rem; font-weight:600;';
                    if (dow === 0) numStyle += ' color:#EF476F;';
                    else if (dow === 6) numStyle += ' color:#0582CA;';
                    if (isToday) numStyle += ' background:#0582CA; color:#fff; border-radius:50%; width:22px; height:22px; display:inline-flex; align-items:center; justify-content:center;';

                    html += '<div class="px-1 pt-1">';
                    html += '<span style="' + numStyle + '">' + cellDay + '</span>';

                    // +N 더보기 (숨김 항목 있으면 날짜 옆에 표시)
                    var hiddenCount = hiddenPerDate[dateStr] || 0;
                    if (hiddenCount > 0) {
                        html += ' <a href="/admin/reservations/daily?date=' + dateStr + '" ';
                        html += 'style="font-size:0.6rem; color:#0582CA; text-decoration:none; position:relative; z-index:2;">+' + hiddenCount + '</a>';
                    }
                    html += '</div>';

                    html += '</div>';
                }
            }
            html += '</div>';

            // 레인 오버레이 (예약 바 - 셀 경계를 넘어 연속 렌더링)
            if (visibleLanes > 0) {
                html += '<div style="position: absolute; top: ' + this.DATE_ROW_HEIGHT + 'px; left: 0; right: 0; z-index: 1; pointer-events: none;">';

                // 레인별 바 그룹화
                var laneBars = {};
                for (var bi = 0; bi < layout.bars.length; bi++) {
                    var bar = layout.bars[bi];
                    if (bar.lane >= visibleLanes) continue;
                    if (!laneBars[bar.lane]) laneBars[bar.lane] = [];
                    laneBars[bar.lane].push(bar);
                }

                for (var lane = 0; lane < visibleLanes; lane++) {
                    html += '<div style="position: relative; height: ' + (this.LANE_HEIGHT - 2) + 'px; margin-bottom: 2px;">';

                    var bars = laneBars[lane] || [];
                    for (var bi = 0; bi < bars.length; bi++) {
                        html += this.renderBar(bars[bi]);
                    }

                    html += '</div>';
                }

                html += '</div>';
            }

            html += '</div>'; // 주간 래퍼 끝
        }

        html += '</div>'; // card-body
        html += '</div>'; // card

        return html;
    },

    /**
     * 단일 예약 바 렌더링 (오버레이 내 절대 위치)
     */
    renderBar: function(bar) {
        var leftPct = (bar.startCol / 7 * 100).toFixed(4);
        var widthPct = (bar.spanCols / 7 * 100).toFixed(4);

        var statusColor = this.getStatusColor(bar.item.reservationStatus);
        var statusLabel = this.getStatusLabel(bar.item.reservationStatus);
        var name = HolaPms.escapeHtml(bar.item.guestNameMasked || '-');
        var resNo = bar.item.masterReservationNo ? HolaPms.escapeHtml(bar.item.masterReservationNo) : '';
        var room = bar.item.roomInfo ? HolaPms.escapeHtml(bar.item.roomInfo) : '';

        // 위치별 border-radius
        var radius;
        if (bar.isFirst && bar.isLast) radius = '3px';
        else if (bar.isFirst) radius = '3px 0 0 3px';
        else if (bar.isLast) radius = '0 3px 3px 0';
        else radius = '0';

        // 시작/끝 미세 여백 (셀 경계와 구분)
        var leftMargin = bar.isFirst ? 2 : 0;
        var rightMargin = bar.isLast ? 2 : 0;

        var tooltip = '[' + statusLabel + '] ' + resNo + ' ' + name + (room ? ' | ' + room : '');

        var html = '<div class="calendar-bar" style="position: absolute; pointer-events: auto; cursor: pointer; ';
        html += 'left: calc(' + leftPct + '% + ' + leftMargin + 'px); ';
        html += 'width: calc(' + widthPct + '% - ' + (leftMargin + rightMargin) + 'px); ';
        html += 'height: 100%; box-sizing: border-box; ';
        html += 'background-color: ' + statusColor.bg + '; color: ' + statusColor.text + '; ';
        html += 'border-radius: ' + radius + '; ';
        html += 'font-size: 0.7rem; line-height: ' + (this.LANE_HEIGHT - 2) + 'px; ';
        html += 'padding: 0 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" ';
        html += 'onclick="window.location.href=\'/admin/reservations/' + bar.item.id + '\'" ';
        html += 'title="' + tooltip + '">';

        if (bar.isFirst) {
            html += '<span style="font-weight:600;">[' + statusLabel + ']</span> ';
            if (bar.item.reservationStatus === 'RESERVED' && resNo) {
                html += '<span style="opacity:0.7;">' + resNo + '</span> ';
            }
            html += name;
            if (room) html += ' <span style="opacity:0.7;">' + room + '</span>';
        } else {
            html += '&nbsp;';
        }

        html += '</div>';
        return html;
    },

    /**
     * 주간 레이아웃 계산 (레인 할당 + 바 위치 정보)
     * @param weekColumns - 7개 요소 배열, 각 요소는 dateStr 또는 null
     * @returns { bars: [{item, lane, startCol, endCol, spanCols, isFirst, isLast}], maxLane: N }
     */
    calculateWeekLayout: function(weekColumns) {
        var self = this;
        var result = { bars: [], maxLane: -1 };

        // 날짜→열 매핑 및 고유 예약 수집
        var dateToCol = {};
        var reservationMap = {};

        for (var col = 0; col < 7; col++) {
            var dateStr = weekColumns[col];
            if (!dateStr) continue;
            dateToCol[dateStr] = col;

            var items = this.data[dateStr] || [];
            for (var j = 0; j < items.length; j++) {
                var item = items[j];
                if (!reservationMap[item.id]) {
                    reservationMap[item.id] = {
                        item: item,
                        startCol: col,
                        endCol: col
                    };
                } else {
                    reservationMap[item.id].endCol = col;
                }
            }
        }

        // 시작 열 → 체크인일 → ID 순 정렬
        var reservations = [];
        for (var id in reservationMap) {
            reservations.push(reservationMap[id]);
        }
        reservations.sort(function(a, b) {
            if (a.startCol !== b.startCol) return a.startCol - b.startCol;
            if (a.item.masterCheckIn < b.item.masterCheckIn) return -1;
            if (a.item.masterCheckIn > b.item.masterCheckIn) return 1;
            return a.item.id - b.item.id;
        });

        // 탐욕적 레인 할당 (1열 버퍼로 인접 예약 분리)
        var laneEnds = [];

        for (var i = 0; i < reservations.length; i++) {
            var r = reservations[i];
            var lane = -1;

            for (var l = 0; l < laneEnds.length; l++) {
                if (laneEnds[l] < r.startCol) {
                    lane = l;
                    break;
                }
            }
            if (lane === -1) lane = laneEnds.length;

            r.lane = lane;
            if (lane > result.maxLane) result.maxLane = lane;

            // endCol + 1 버퍼 → 인접 예약 같은 레인 방지
            var bufferedEnd = r.endCol + 1;
            if (lane >= laneEnds.length) {
                laneEnds.push(bufferedEnd);
            } else {
                laneEnds[lane] = bufferedEnd;
            }

            // 예약의 시작/끝 판정 (이 주 기준)
            var checkIn = r.item.masterCheckIn;
            var checkOut = r.item.masterCheckOut;

            result.bars.push({
                item: r.item,
                lane: lane,
                startCol: r.startCol,
                endCol: r.endCol,
                spanCols: r.endCol - r.startCol + 1,
                isFirst: dateToCol[checkIn] !== undefined,
                isLast: dateToCol[checkOut] !== undefined
            });
        }

        return result;
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
