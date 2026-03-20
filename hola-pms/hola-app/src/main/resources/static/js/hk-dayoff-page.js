/**
 * 하우스키핑 휴무일 관리 페이지
 */
var HkDayOff = {

    propertyId: null,
    currentYear: null,
    currentMonth: null,
    housekeepers: [],  // 전체 하우스키퍼 목록
    dayOffs: [],       // 월별 휴무일 데이터

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
            self.loadData();
        });

        // 빈 셀 클릭 → 등록 모달
        $(document).on('click', '.dayoff-cell-empty', function () {
            var hkId = $(this).data('hk-id');
            var date = $(this).data('date');
            var hkName = $(this).data('hk-name');
            self.openCreateModal(hkId, date, hkName);
        });

        // 기존 휴무 셀 클릭 → 관리 모달
        $(document).on('click', '.dayoff-cell-filled', function () {
            var dayOffId = $(this).data('dayoff-id');
            var hkName = $(this).data('hk-name');
            var date = $(this).data('date');
            var status = $(this).data('status');
            var note = $(this).data('note') || '';
            self.openManageModal(dayOffId, hkName, date, status, note);
        });

        $('#btnSaveDayOff').on('click', function () { self.saveDayOff(); });
        $('#btnDeleteDayOff').on('click', function () { self.deleteDayOff(); });
        $('#btnApproveDayOff').on('click', function () { self.approveDayOff(); });
        $('#btnRejectDayOff').on('click', function () { self.rejectDayOff(); });
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
        this.loadData();
    },

    changeMonth: function (delta) {
        this.currentMonth += delta;
        if (this.currentMonth > 12) { this.currentMonth = 1; this.currentYear++; }
        if (this.currentMonth < 1) { this.currentMonth = 12; this.currentYear--; }
        this.loadData();
    },

    loadData: function () {
        var self = this;
        $('#monthLabel').text(self.currentYear + '년 ' + self.currentMonth + '월');

        // 하우스키퍼 목록 + 휴무일 병렬 로드
        var loaded = 0;
        var tryRender = function () { if (++loaded === 2) self.renderCalendar(); };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/housekeepers',
            method: 'GET',
            success: function (res) {
                if (res.success) self.housekeepers = res.data || [];
                tryRender();
            }
        });

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/day-offs?year=' +
                self.currentYear + '&month=' + self.currentMonth,
            method: 'GET',
            success: function (res) {
                if (res.success) self.dayOffs = res.data || [];
                tryRender();
            }
        });
    },

    renderCalendar: function () {
        var self = this;
        var ym = new Date(self.currentYear, self.currentMonth - 1, 1);
        var daysInMonth = new Date(self.currentYear, self.currentMonth, 0).getDate();
        var today = new Date();
        var isCurrentMonth = (today.getFullYear() === self.currentYear && today.getMonth() + 1 === self.currentMonth);
        var todayDay = isCurrentMonth ? today.getDate() : -1;
        var dayNames = ['일','월','화','수','목','금','토'];

        // 휴무일 맵: hkId_date → dayOff
        var dayOffMap = {};
        self.dayOffs.forEach(function (d) {
            dayOffMap[d.housekeeperId + '_' + d.dayOffDate] = d;
        });

        // thead
        var headHtml = '<tr><th class="text-center" style="min-width:80px;position:sticky;left:0;background:#f8f9fa;z-index:1;">이름</th>';
        for (var d = 1; d <= daysInMonth; d++) {
            var dow = new Date(self.currentYear, self.currentMonth - 1, d).getDay();
            var dayClass = dow === 0 ? 'text-danger' : dow === 6 ? 'text-primary' : '';
            var todayMark = d === todayDay ? 'background:#e3f2fd;' : '';
            headHtml += '<th class="text-center ' + dayClass + '" style="min-width:36px;' + todayMark + '">' +
                d + '<br><span style="font-size:9px;font-weight:400;">' + dayNames[dow] + '</span></th>';
        }
        headHtml += '</tr>';
        $('#dayOffHead').html(headHtml);

        // tbody
        var bodyHtml = '';
        if (self.housekeepers.length === 0) {
            bodyHtml = '<tr><td colspan="' + (daysInMonth + 1) + '" class="text-center text-muted py-3">등록된 하우스키퍼가 없습니다.</td></tr>';
        } else {
            self.housekeepers.forEach(function (hk) {
                var maskedName = HolaPms.maskName(hk.userName);
                bodyHtml += '<tr>';
                bodyHtml += '<td class="text-center" style="position:sticky;left:0;background:#fff;z-index:1;white-space:nowrap;">' +
                    HolaPms.escapeHtml(maskedName) + '</td>';

                for (var d = 1; d <= daysInMonth; d++) {
                    var dateStr = self.currentYear + '-' + String(self.currentMonth).padStart(2, '0') + '-' + String(d).padStart(2, '0');
                    var key = hk.userId + '_' + dateStr;
                    var dayOff = dayOffMap[key];
                    var todayBorder = d === todayDay ? 'border:2px solid #0582CA;' : '';

                    if (dayOff) {
                        // 휴무일 있음
                        var bg = '', symbol = '', title = '';
                        switch (dayOff.status) {
                            case 'APPROVED':
                                bg = '#d4edda'; symbol = '✓'; title = '승인됨';
                                break;
                            case 'PENDING':
                                bg = '#fff3cd'; symbol = '?'; title = '승인 대기';
                                break;
                            case 'REJECTED':
                                bg = '#f8d7da'; symbol = '✗'; title = '거절됨';
                                break;
                        }
                        if (dayOff.note) title += ' — ' + dayOff.note;

                        bodyHtml += '<td class="text-center dayoff-cell-filled" ' +
                            'data-dayoff-id="' + dayOff.id + '" ' +
                            'data-hk-name="' + HolaPms.escapeHtml(maskedName) + '" ' +
                            'data-date="' + dateStr + '" ' +
                            'data-status="' + dayOff.status + '" ' +
                            'data-note="' + HolaPms.escapeHtml(dayOff.note || '') + '" ' +
                            'style="background:' + bg + ';cursor:pointer;' + todayBorder + '" ' +
                            'title="' + HolaPms.escapeHtml(title) + '">' + symbol + '</td>';
                    } else {
                        // 빈 셀
                        bodyHtml += '<td class="text-center dayoff-cell-empty" ' +
                            'data-hk-id="' + hk.userId + '" ' +
                            'data-date="' + dateStr + '" ' +
                            'data-hk-name="' + HolaPms.escapeHtml(maskedName) + '" ' +
                            'style="cursor:pointer;' + todayBorder + '" ' +
                            'title="클릭하여 휴무 등록"></td>';
                    }
                }
                bodyHtml += '</tr>';
            });
        }
        $('#dayOffBody').html(bodyHtml);
    },

    // === 등록 모달 ===

    openCreateModal: function (hkId, date, hkName) {
        this._currentDayOffId = null;
        $('#modalHkId').val(hkId);
        $('#modalDate').val(date);
        $('#modalHkName').text(hkName);
        $('#modalDateLabel').text(date);
        $('#modalNote').val('');
        $('#dayOffModalTitle').text('휴무일 등록');
        $('#modalNoteGroup').show();
        $('#modalExistingInfo').addClass('d-none');
        $('#modalCreateBtns').show();
        $('#modalManageBtns').addClass('d-none');
        HolaPms.modal.show('#dayOffModal');
    },

    openManageModal: function (dayOffId, hkName, date, status, note) {
        this._currentDayOffId = dayOffId;
        $('#modalHkName').text(hkName);
        $('#modalDateLabel').text(date);
        $('#dayOffModalTitle').text('휴무일 관리');
        $('#modalNoteGroup').hide();
        $('#modalExistingInfo').removeClass('d-none');

        // 상태 뱃지
        var statusMap = { 'APPROVED': '<span class="badge bg-success">승인</span>',
                          'PENDING': '<span class="badge bg-warning text-dark">대기</span>',
                          'REJECTED': '<span class="badge bg-danger">거절</span>' };
        $('#modalStatus').html(statusMap[status] || status);
        $('#modalExistingNote').text(note ? '사유: ' + note : '');

        // 버튼 표시
        $('#modalCreateBtns').hide();
        $('#modalManageBtns').removeClass('d-none');
        if (status === 'PENDING') {
            $('#btnApproveDayOff, #btnRejectDayOff').removeClass('d-none');
        } else {
            $('#btnApproveDayOff, #btnRejectDayOff').addClass('d-none');
        }

        HolaPms.modal.show('#dayOffModal');
    },

    saveDayOff: function () {
        var self = this;
        var data = {
            housekeeperId: $('#modalHkId').val(),
            date: $('#modalDate').val(),
            note: HolaPms.form.val('#modalNote')
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/day-offs',
            method: 'POST',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#dayOffModal');
                    HolaPms.alert('success', '휴무일이 등록되었습니다.');
                    self.loadData();
                }
            }
        });
    },

    deleteDayOff: function () {
        var self = this;
        if (!confirm('이 휴무일을 삭제하시겠습니까?')) return;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/day-offs/' + self._currentDayOffId,
            method: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#dayOffModal');
                    HolaPms.alert('success', '휴무일이 삭제되었습니다.');
                    self.loadData();
                }
            }
        });
    },

    approveDayOff: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/day-offs/' + self._currentDayOffId + '/approve',
            method: 'PUT',
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#dayOffModal');
                    HolaPms.alert('success', '휴무일이 승인되었습니다.');
                    self.loadData();
                }
            }
        });
    },

    rejectDayOff: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/day-offs/' + self._currentDayOffId + '/reject',
            method: 'PUT',
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#dayOffModal');
                    HolaPms.alert('success', '휴무일이 거절되었습니다.');
                    self.loadData();
                }
            }
        });
    }
};

$(function () {
    HkDayOff.init();
});
