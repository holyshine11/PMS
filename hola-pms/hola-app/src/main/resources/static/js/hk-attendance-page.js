/**
 * 하우스키핑 근태관리 캘린더 페이지
 */
var HkAttendance = {

    propertyId: null,
    currentYear: null,
    currentMonth: null,

    init: function () {
        var now = new Date();
        this.currentYear = now.getFullYear();
        this.currentMonth = now.getMonth() + 1;
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;
        $(document).on('hola:contextChange', function () { self.reload(); });

        $('#btnPrevMonth').on('click', function () { self.changeMonth(-1); });
        $('#btnNextMonth').on('click', function () { self.changeMonth(1); });
        $('#btnToday').on('click', function () {
            var now = new Date();
            self.currentYear = now.getFullYear();
            self.currentMonth = now.getMonth() + 1;
            self.loadCalendar();
        });

        // 셀 클릭 → 편집 모달
        $(document).on('click', '.att-cell[data-att-id]', function () {
            var attId = $(this).data('att-id');
            var status = $(this).data('status') || 'BEFORE_WORK';
            var clockIn = $(this).data('clock-in') || '';
            var clockOut = $(this).data('clock-out') || '';

            if (!attId) return;  // 미등록 셀은 편집 불가

            $('#editAttId').val(attId);
            $('#editAttStatus').val(status);
            $('#editClockIn').val(clockIn);
            $('#editClockOut').val(clockOut);
            HolaPms.modal.show('#editAttModal');
        });

        // 저장
        $('#btnSaveAtt').on('click', function () { self.saveAttendance(); });
    },

    reload: function () {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#calendarCard, #legendRow').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#calendarCard, #legendRow').show();
        this.propertyId = propertyId;
        this.loadCalendar();
    },

    changeMonth: function (delta) {
        this.currentMonth += delta;
        if (this.currentMonth > 12) { this.currentMonth = 1; this.currentYear++; }
        if (this.currentMonth < 1) { this.currentMonth = 12; this.currentYear--; }
        this.loadCalendar();
    },

    loadCalendar: function () {
        var self = this;
        $('#monthLabel').text(self.currentYear + '년 ' + self.currentMonth + '월');

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/attendance/monthly?year=' +
                self.currentYear + '&month=' + self.currentMonth,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.renderCalendar(res.data);
                }
            }
        });
    },

    renderCalendar: function (data) {
        var daysInMonth = data.daysInMonth;
        var today = new Date();
        var isCurrentMonth = (today.getFullYear() === data.year && today.getMonth() + 1 === data.month);
        var todayDay = isCurrentMonth ? today.getDate() : -1;

        // thead: 요일명 포함 (C2)
        var dayNames = ['일','월','화','수','목','금','토'];
        var headHtml = '<tr><th class="text-center" style="min-width:80px;position:sticky;left:0;background:#f8f9fa;z-index:1;">이름</th>';
        for (var d = 1; d <= daysInMonth; d++) {
            var dow = new Date(data.year, data.month - 1, d).getDay();
            var dayClass = dow === 0 ? 'text-danger' : dow === 6 ? 'text-primary' : '';
            var todayMark = d === todayDay ? 'background:#e3f2fd;' : '';
            headHtml += '<th class="text-center ' + dayClass + '" style="min-width:36px;' + todayMark + '">' +
                d + '<br><span style="font-size:9px;font-weight:400;">' + dayNames[dow] + '</span></th>';
        }
        // 합계 컬럼 헤더 (C3)
        headHtml += '<th class="text-center" style="min-width:40px;background:#f8f9fa;">출근</th>';
        headHtml += '<th class="text-center" style="min-width:40px;background:#f8f9fa;">휴무</th>';
        headHtml += '<th class="text-center" style="min-width:40px;background:#f8f9fa;">미등록</th>';
        headHtml += '</tr>';
        $('#calendarHead').html(headHtml);

        // tbody
        var bodyHtml = '';
        var rows = data.rows || [];
        // colspan: 이름 + 일수 + 합계 3컬럼
        if (rows.length === 0) {
            bodyHtml = '<tr><td colspan="' + (daysInMonth + 4) + '" class="text-center text-muted py-3">등록된 하우스키퍼가 없습니다.</td></tr>';
        } else {
            rows.forEach(function (row) {
                bodyHtml += '<tr>';
                bodyHtml += '<td class="text-center" style="position:sticky;left:0;background:#fff;z-index:1;white-space:nowrap;">' +
                    HolaPms.escapeHtml(HolaPms.maskName(row.userName)) + '</td>';

                // 행별 통계 카운트 (C3)
                var cntWork = 0, cntOff = 0, cntNone = 0;

                (row.days || []).forEach(function (cell) {
                    var bg = '';
                    var symbol = '';
                    var tooltip = '';
                    var attId = cell.attendanceId || '';
                    var clockIn = cell.clockInAt ? cell.clockInAt.substring(11, 16) : '';
                    var clockOut = cell.clockOutAt ? cell.clockOutAt.substring(11, 16) : '';

                    // 상태별 스타일 + tooltip (C4)
                    switch (cell.attendanceStatus) {
                        case 'WORKING':
                            bg = '#d4edda'; symbol = '<span style="color:#155724;">●</span>';
                            tooltip = '출근 ' + clockIn;
                            cntWork++;
                            break;
                        case 'LEFT':
                            bg = '#cce5ff'; symbol = '<span style="color:#004085;">●</span>';
                            tooltip = '출근 ' + clockIn + ' ~ 퇴근 ' + clockOut;
                            cntWork++;
                            break;
                        case 'DAY_OFF':
                            bg = '#e2e3e5'; symbol = '<span style="color:#383d41;">—</span>';
                            tooltip = '휴무';
                            cntOff++;
                            break;
                        case 'BEFORE_WORK':
                            bg = '#fff3cd'; symbol = '<span style="color:#856404;">○</span>';
                            tooltip = '출근전 (미출근)';
                            cntNone++;
                            break;
                        default:
                            bg = '#fff'; symbol = '';
                            tooltip = '미등록';
                            cntNone++;
                    }

                    var todayBorder = cell.day === todayDay ? 'border:2px solid #0582CA;' : '';
                    var cursor = attId ? 'cursor:pointer;' : '';

                    bodyHtml += '<td class="text-center att-cell" ' +
                        'data-att-id="' + attId + '" ' +
                        'data-status="' + (cell.attendanceStatus || '') + '" ' +
                        'data-clock-in="' + clockIn + '" ' +
                        'data-clock-out="' + clockOut + '" ' +
                        'style="background:' + bg + ';' + todayBorder + cursor + '" ' +
                        'title="' + HolaPms.escapeHtml(tooltip) + '">' +
                        symbol + '</td>';
                });

                // 합계 컬럼 (C3)
                bodyHtml += '<td class="text-center" style="background:#f8f9fa;"><span style="color:#155724;">' + cntWork + '</span></td>';
                bodyHtml += '<td class="text-center" style="background:#f8f9fa;"><span style="color:#383d41;">' + cntOff + '</span></td>';
                bodyHtml += '<td class="text-center" style="background:#f8f9fa;"><span style="color:#6c757d;">' + cntNone + '</span></td>';
                bodyHtml += '</tr>';
            });
        }
        $('#calendarBody').html(bodyHtml);
    },

    saveAttendance: function () {
        var self = this;
        var attId = $('#editAttId').val();
        if (!attId) return;

        var data = {
            attendanceStatus: $('#editAttStatus').val(),
            clockInAt: $('#editClockIn').val() || null,
            clockOutAt: $('#editClockOut').val() || null
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/attendance/' + attId,
            method: 'PUT',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#editAttModal');
                    HolaPms.alert('success', '근태가 수정되었습니다.');
                    self.loadCalendar();
                }
            }
        });
    }
};

$(function () {
    HkAttendance.init();
});
