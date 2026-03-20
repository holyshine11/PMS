/**
 * 하우스키핑 모바일 - 휴무 관리 (캘린더 UI)
 */
var HkMobileDayOff = {

    propertyId: null,
    userId: null,
    currentYear: null,
    currentMonth: null,
    dayOffs: [],
    selectedDate: null,  // 'YYYY-MM-DD'

    init: function () {
        this.propertyId = (typeof HK_PROPERTY_ID !== 'undefined') ? HK_PROPERTY_ID : null;
        this.userId = (typeof HK_USER_ID !== 'undefined') ? HK_USER_ID : null;
        if (!this.propertyId) return;

        var now = new Date();
        this.currentYear = now.getFullYear();
        this.currentMonth = now.getMonth() + 1;
        this.bindEvents();
        this.loadData();
    },

    bindEvents: function () {
        var self = this;
        $('#btnPrevMonth').on('click', function () { self.changeMonth(-1); });
        $('#btnNextMonth').on('click', function () { self.changeMonth(1); });

        // 캘린더 셀 클릭
        $(document).on('click', '.cal-cell:not(.other-month)', function () {
            var date = $(this).data('date');
            if (!date) return;
            self.selectDate(date);
        });

        // 휴무 신청
        $(document).on('click', '#btnRequestDayOff', function () {
            self.createDayOff(self.selectedDate);
        });

        // 휴무 취소
        $(document).on('click', '#btnCancelDayOff', function () {
            var dayOffId = $(this).data('dayoff-id');
            if (confirm('휴무 신청을 취소하시겠습니까?')) {
                self.deleteDayOff(dayOffId);
            }
        });
    },

    changeMonth: function (delta) {
        this.currentMonth += delta;
        if (this.currentMonth > 12) { this.currentMonth = 1; this.currentYear++; }
        if (this.currentMonth < 1) { this.currentMonth = 12; this.currentYear--; }
        this.selectedDate = null;
        this.loadData();
    },

    loadData: function () {
        var self = this;
        $('#monthLabel').text(self.currentYear + '년 ' + self.currentMonth + '월');

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-dayoffs?year=' +
                self.currentYear + '&month=' + self.currentMonth,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.dayOffs = res.data || [];
                    self.renderCalendar();
                    self.renderColleagues();
                    if (self.selectedDate) {
                        self.showDateInfo(self.selectedDate);
                    } else {
                        $('#selectedDateInfo').hide();
                    }
                }
            }
        });
    },

    // === 캘린더 렌더 ===

    renderCalendar: function () {
        var self = this;
        var firstDay = new Date(self.currentYear, self.currentMonth - 1, 1).getDay(); // 1일의 요일 (0=일)
        var daysInMonth = new Date(self.currentYear, self.currentMonth, 0).getDate();
        var prevMonthDays = new Date(self.currentYear, self.currentMonth - 1, 0).getDate();

        var today = new Date();
        var todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');

        // 본인 휴무 맵: date → dayOff
        var myDayOffs = {};
        self.dayOffs.forEach(function (d) {
            if (String(d.housekeeperId) === String(self.userId)) {
                myDayOffs[d.dayOffDate] = d;
            }
        });

        var html = '';
        var totalCells = Math.ceil((firstDay + daysInMonth) / 7) * 7;

        for (var i = 0; i < totalCells; i++) {
            var day, dateStr, isOtherMonth = false;
            var dow = i % 7; // 0=일 ~ 6=토

            if (i < firstDay) {
                // 이전 달
                day = prevMonthDays - firstDay + 1 + i;
                isOtherMonth = true;
                dateStr = '';
            } else if (i - firstDay >= daysInMonth) {
                // 다음 달
                day = i - firstDay - daysInMonth + 1;
                isOtherMonth = true;
                dateStr = '';
            } else {
                // 이번 달
                day = i - firstDay + 1;
                dateStr = self.currentYear + '-' + String(self.currentMonth).padStart(2, '0') + '-' + String(day).padStart(2, '0');
            }

            var classes = ['cal-cell'];
            if (isOtherMonth) classes.push('other-month');
            if (dateStr === todayStr) classes.push('today');
            if (dateStr === self.selectedDate) classes.push('selected');
            if (dow === 0) classes.push('sun');
            if (dow === 6) classes.push('sat');

            // 휴무 도트
            var dotHtml = '';
            if (!isOtherMonth && myDayOffs[dateStr]) {
                var status = myDayOffs[dateStr].status.toLowerCase();
                dotHtml = '<span class="cal-dot ' + status + '"></span>';
            }

            html += '<div class="' + classes.join(' ') + '" data-date="' + dateStr + '">' +
                day + dotHtml + '</div>';
        }

        $('#calGrid').html(html);
    },

    // === 날짜 선택 ===

    selectDate: function (dateStr) {
        this.selectedDate = dateStr;
        // 선택 표시 업데이트
        $('.cal-cell').removeClass('selected');
        $('.cal-cell[data-date="' + dateStr + '"]').addClass('selected');
        this.showDateInfo(dateStr);
    },

    showDateInfo: function (dateStr) {
        var self = this;
        var parts = dateStr.split('-');
        var dayNames = ['일','월','화','수','목','금','토'];
        var dow = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2])).getDay();
        var label = parseInt(parts[1]) + '월 ' + parseInt(parts[2]) + '일 (' + dayNames[dow] + ')';

        $('#selectedDateLabel').text(label);

        // 본인의 해당 날짜 휴무 찾기
        var myDayOff = null;
        self.dayOffs.forEach(function (d) {
            if (String(d.housekeeperId) === String(self.userId) && d.dayOffDate === dateStr) {
                myDayOff = d;
            }
        });

        // 동료 휴무 목록
        var colleagues = [];
        self.dayOffs.forEach(function (d) {
            if (String(d.housekeeperId) !== String(self.userId) && d.dayOffDate === dateStr && d.status === 'APPROVED') {
                colleagues.push(d);
            }
        });

        var statusHtml = '';
        var actionHtml = '';

        if (myDayOff) {
            // 이미 휴무 등록됨
            var statusMap = {
                'APPROVED': '<span class="badge bg-success">승인 완료</span>',
                'PENDING': '<span class="badge bg-warning text-dark">승인 대기</span>',
                'REJECTED': '<span class="badge bg-danger">거절됨</span>'
            };
            statusHtml = statusMap[myDayOff.status] || myDayOff.status;

            if (myDayOff.status === 'PENDING') {
                actionHtml = '<button class="btn btn-outline-danger btn-mobile-sm w-100" id="btnCancelDayOff" data-dayoff-id="' + myDayOff.id + '">' +
                    '<i class="fas fa-times me-1"></i>휴무 신청 취소</button>';
            } else if (myDayOff.status === 'APPROVED') {
                actionHtml = '<div class="text-center text-muted" style="font-size:13px;"><i class="fas fa-check-circle text-success me-1"></i>휴무가 확정되었습니다.</div>';
            } else if (myDayOff.status === 'REJECTED') {
                actionHtml = '<div class="text-center text-muted" style="font-size:13px;"><i class="fas fa-info-circle me-1"></i>관리자가 거절한 요청입니다.</div>';
            }

            if (myDayOff.note) {
                actionHtml = '<div class="text-muted mb-2" style="font-size:13px;"><i class="fas fa-comment me-1"></i>' + HolaPms.escapeHtml(myDayOff.note) + '</div>' + actionHtml;
            }
        } else {
            // 미등록 → 신청 가능
            statusHtml = '<span class="text-muted" style="font-size:13px;">근무일</span>';

            // 과거 날짜 체크
            var today = new Date();
            today.setHours(0, 0, 0, 0);
            var selected = new Date(parts[0], parts[1] - 1, parts[2]);
            if (selected < today) {
                actionHtml = '<div class="text-center text-muted" style="font-size:13px;">지난 날짜는 신청할 수 없습니다.</div>';
            } else {
                actionHtml = '<button class="btn btn-primary btn-mobile-sm w-100" id="btnRequestDayOff">' +
                    '<i class="fas fa-calendar-minus me-1"></i>휴무 신청</button>';
            }
        }

        // 동료 휴무 표시
        if (colleagues.length > 0) {
            actionHtml += '<div class="mt-2" style="font-size:12px;color:#6c757d;">' +
                '<i class="fas fa-users me-1"></i>이 날 휴무 동료: ';
            colleagues.forEach(function (c, i) {
                if (i > 0) actionHtml += ', ';
                actionHtml += HolaPms.escapeHtml(c.userName);
            });
            actionHtml += '</div>';
        }

        $('#selectedStatusBadge').html(statusHtml);
        $('#selectedAction').html(actionHtml);
        $('#selectedDateInfo').show();
    },

    // === 동료 휴무 리스트 ===

    renderColleagues: function () {
        var self = this;
        // 이번 달 동료의 승인된 휴무일 집계
        var colleagues = {}; // userName → dates[]
        self.dayOffs.forEach(function (d) {
            if (String(d.housekeeperId) !== String(self.userId) && d.status === 'APPROVED') {
                if (!colleagues[d.userName]) colleagues[d.userName] = [];
                colleagues[d.userName].push(d.dayOffDate);
            }
        });

        var names = Object.keys(colleagues);
        if (names.length === 0) {
            $('#colleagueTitle, #colleagueList').hide();
            return;
        }

        $('#colleagueTitle').show();
        var html = '';
        names.forEach(function (name) {
            var dates = colleagues[name].sort();
            var daysText = dates.map(function (d) { return parseInt(d.split('-')[2]) + '일'; }).join(', ');
            html += '<div class="colleague-card">' +
                '<div>' +
                    '<span style="font-size:14px;font-weight:500;">' + HolaPms.escapeHtml(name) + '</span>' +
                    '<div style="font-size:12px;color:#6c757d;">' + daysText + '</div>' +
                '</div>' +
                '<span class="badge bg-secondary" style="font-size:11px;">' + dates.length + '일</span>' +
            '</div>';
        });
        $('#colleagueList').html(html).show();
    },

    // === API ===

    createDayOff: function (date) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-dayoffs',
            method: 'POST',
            data: JSON.stringify({ date: date }),
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '휴무 신청이 등록되었습니다.');
                    self.loadData();
                }
            }
        });
    },

    deleteDayOff: function (dayOffId) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-dayoffs/' + dayOffId,
            method: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '휴무 신청이 취소되었습니다.');
                    self.loadData();
                }
            }
        });
    }
};

$(function () {
    HkMobileDayOff.init();
});
