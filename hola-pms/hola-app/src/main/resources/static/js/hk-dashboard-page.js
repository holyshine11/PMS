/**
 * 하우스키핑 대시보드 페이지
 */
var HkDashboard = {

    propertyId: null,
    attendanceEntries: [],  // 출근부 전체 데이터 캐시

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;
        $(document).on('hola:contextChange', function () { self.reload(); });

        // 출근부
        $('#btnAttendance').on('click', function () { self.openAttendanceModal(); });
        $('#btnSaveAttendance').on('click', function () { self.saveAttendance(); });

        // 자동 배정
        $('#btnAutoAssign').on('click', function () {
            if (confirm('미배정 작업을 구역 기반으로 자동 배정하시겠습니까?')) {
                self.autoAssign();
            }
        });

        // 크레딧 재분배
        $('#btnRedistribute').on('click', function () {
            if (confirm('전체 작업의 크레딧을 균등하게 재분배하시겠습니까?')) {
                self.redistribute();
            }
        });
    },

    reload: function () {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#summaryRow, #progressArea, #assignmentCard, #housekeeperCard').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#summaryRow, #progressArea, #assignmentCard, #housekeeperCard').show();
        this.propertyId = propertyId;
        // 출근부 먼저 로드 → 완료 후 대시보드 로드 (병합 렌더링 위해)
        this.loadAttendanceThenDashboard();
    },

    /** 출근부 로드 후 대시보드 로드 (순차 실행) */
    loadAttendanceThenDashboard: function () {
        var self = this;
        var today = new Date().toISOString().split('T')[0];
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/attendance?date=' + today,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    var entries = res.data.entries || [];
                    self.attendanceEntries = entries;
                    self._updateAttendanceCounts(entries);
                }
                // 출근부 로드 완료 후 대시보드 로드
                self.loadDashboard();
            },
            error: function () {
                // 출근부 실패해도 대시보드는 로드
                self.loadDashboard();
            }
        });
    },

    /** 근태 상태별 카운트 갱신 */
    _updateAttendanceCounts: function (entries) {
        var counts = { WORKING: 0, LEFT: 0, DAY_OFF: 0, BEFORE_WORK: 0 };
        entries.forEach(function (e) {
            var st = e.attendanceStatus || 'BEFORE_WORK';
            // 토글 OFF(isAvailable=false)이면 휴무 처리
            if (!e.isAvailable) {
                st = 'DAY_OFF';
            }
            if (counts[st] != null) {
                counts[st]++;
            } else {
                counts['BEFORE_WORK']++;
            }
        });
        $('#attWorking').text(counts.WORKING);
        $('#attLeft').text(counts.LEFT);
        $('#attDayOff').text(counts.DAY_OFF);
        $('#attBeforeWork').text(counts.BEFORE_WORK);
    },

    loadDashboard: function () {
        var self = this;
        var today = new Date().toISOString().split('T')[0];
        $('#dashboardDate').text(today);

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/dashboard?date=' + today,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.renderSummary(res.data);
                    // 하우스키퍼 테이블은 출근부 데이터와 병합하여 렌더링
                    self.mergeAndRenderHousekeeperTable(res.data.housekeeperSummaries || []);
                }
            }
        });
    },

    renderSummary: function (data) {
        var total = data.totalTasks || 0;
        var pending = data.pendingTasks || 0;
        var inProgress = data.inProgressTasks || 0;
        var completed = data.completedTasks || 0;
        var inspected = data.inspectedTasks || 0;

        $('#totalCount').text(total);
        $('#pendingCount').text(pending);
        $('#inProgressCount').text(inProgress);
        $('#completedCount').text(completed);
        $('#inspectedCount').text(inspected);

        // 검수 완료율: "검수완료/전체" 형태
        if (total === 0) {
            $('#completionRate').text('-');
        } else {
            $('#completionRate').text(inspected + '/' + total);
        }

        // 다중 세그먼트 프로그레스 바
        if (total === 0) {
            $('#pgPending, #pgInProgress, #pgCompleted, #pgInspected').css('width', '0%').text('');
        } else {
            var pctPending = (pending / total * 100);
            var pctInProgress = (inProgress / total * 100);
            var pctCompleted = (completed / total * 100);
            var pctInspected = (inspected / total * 100);

            // 세그먼트 렌더: 너비 충분하면(>=8%) 건수 라벨 표시
            this._setSegment('#pgPending', pctPending, pending, '대기');
            this._setSegment('#pgInProgress', pctInProgress, inProgress, '진행');
            this._setSegment('#pgCompleted', pctCompleted, completed, '완료');
            this._setSegment('#pgInspected', pctInspected, inspected, '검수');
        }
    },

    /** 프로그레스 바 세그먼트 설정 헬퍼 */
    _setSegment: function (selector, pct, count, label) {
        var $el = $(selector);
        $el.css('width', pct + '%');
        // 너비 8% 이상이면 라벨 표시
        if (pct >= 8 && count > 0) {
            $el.text(label + ' ' + count);
        } else {
            $el.text('');
        }
    },

    /**
     * 하우스키퍼 테이블: 출근부 전체 인원 + 대시보드 작업 집계 병합
     * - 출근부에 있지만 작업이 없는 사람도 표시
     * - 정렬: 출근(WORKING) → 퇴근(LEFT) → 미출근(BEFORE_WORK) → 휴무(DAY_OFF)
     */
    mergeAndRenderHousekeeperTable: function (summaries) {
        var self = this;
        var entries = self.attendanceEntries || [];
        var $body = $('#housekeeperBody');
        $body.empty();

        // 대시보드 summaries를 userId 기준으로 맵핑
        var summaryMap = {};
        summaries.forEach(function (s) {
            summaryMap[s.userId] = s;
        });

        // 출근부 entries를 기반으로 병합 리스트 생성
        var merged = [];
        var seenUserIds = {};

        entries.forEach(function (e) {
            var s = summaryMap[e.housekeeperId] || {};
            seenUserIds[e.housekeeperId] = true;
            merged.push({
                userName: e.userName || s.userName || '-',
                sectionName: e.sectionName || '',
                attendanceStatus: e.attendanceStatus || 'BEFORE_WORK',
                clockInAt: e.clockInAt || s.clockInAt,
                clockOutAt: e.clockOutAt || s.clockOutAt,
                pendingCount: s.pendingCount || 0,
                inProgressCount: s.inProgressCount || 0,
                completedCount: s.completedCount || 0,
                totalCredits: s.totalCredits != null ? s.totalCredits : null,
                avgDurationMinutes: s.avgDurationMinutes != null ? s.avgDurationMinutes : null
            });
        });

        // 대시보드에는 있지만 출근부에 없는 사람 (예외 케이스)
        summaries.forEach(function (s) {
            if (!seenUserIds[s.userId]) {
                merged.push({
                    userName: s.userName || '-',
                    sectionName: '',
                    attendanceStatus: s.attendanceStatus || 'BEFORE_WORK',
                    clockInAt: s.clockInAt,
                    clockOutAt: s.clockOutAt,
                    pendingCount: s.pendingCount || 0,
                    inProgressCount: s.inProgressCount || 0,
                    completedCount: s.completedCount || 0,
                    totalCredits: s.totalCredits != null ? s.totalCredits : null,
                    avgDurationMinutes: s.avgDurationMinutes != null ? s.avgDurationMinutes : null
                });
            }
        });

        if (merged.length === 0) {
            $body.append('<tr><td colspan="8" class="text-center text-muted">배정된 하우스키퍼가 없습니다.</td></tr>');
            return;
        }

        // 정렬: WORKING → LEFT → BEFORE_WORK → DAY_OFF
        var statusOrder = { 'WORKING': 0, 'LEFT': 1, 'BEFORE_WORK': 2, 'DAY_OFF': 3 };
        merged.sort(function (a, b) {
            var oa = statusOrder[a.attendanceStatus] != null ? statusOrder[a.attendanceStatus] : 9;
            var ob = statusOrder[b.attendanceStatus] != null ? statusOrder[b.attendanceStatus] : 9;
            return oa - ob;
        });

        merged.forEach(function (m) {
            var avgMin = m.avgDurationMinutes != null ? Math.round(m.avgDurationMinutes) + '분' : '-';
            var credits = m.totalCredits != null ? m.totalCredits : '-';
            var attBadge = self.getAttendanceBadge(m);
            var sectionHtml = m.sectionName ? HolaPms.escapeHtml(m.sectionName) : '-';

            $body.append(
                '<tr>' +
                '<td class="text-center">' + HolaPms.escapeHtml(HolaPms.maskName(m.userName) || '-') + '</td>' +
                '<td class="text-center">' + sectionHtml + '</td>' +
                '<td class="text-center">' + attBadge + '</td>' +
                '<td class="text-center">' + m.pendingCount + '</td>' +
                '<td class="text-center">' + m.inProgressCount + '</td>' +
                '<td class="text-center">' + m.completedCount + '</td>' +
                '<td class="text-center">' + credits + '</td>' +
                '<td class="text-center">' + avgMin + '</td>' +
                '</tr>'
            );
        });
    },

    /** 근태 상태 뱃지 렌더 */
    getAttendanceBadge: function (s) {
        var status = s.attendanceStatus || 'BEFORE_WORK';
        var clockIn = s.clockInAt ? s.clockInAt.substring(11, 16) : '';
        var clockOut = s.clockOutAt ? s.clockOutAt.substring(11, 16) : '';

        switch (status) {
            case 'BEFORE_WORK':
                return '<span class="badge bg-warning text-dark" style="font-size:11px;">출근전</span>';
            case 'WORKING':
                return '<span class="badge bg-success" style="font-size:11px;">정상 ' + clockIn + '~</span>';
            case 'LEFT':
                return '<span class="badge bg-primary" style="font-size:11px;">퇴근 ' + clockIn + '~' + clockOut + '</span>';
            case 'DAY_OFF':
                return '<span class="badge bg-secondary" style="font-size:11px;">휴무</span>';
            default:
                return '<span class="badge bg-light text-dark" style="font-size:11px;">' + status + '</span>';
        }
    },

    /** 출근부 저장 후 전체 갱신용 */
    loadAttendanceSummary: function () {
        // 출근부 저장 후 호출 → 출근부 + 대시보드 모두 다시 로드
        this.loadAttendanceThenDashboard();
    },

    // === 출근부 모달 ===

    openAttendanceModal: function () {
        var self = this;
        var today = new Date().toISOString().split('T')[0];

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/attendance?date=' + today,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.renderAttendanceList(res.data);
                    HolaPms.modal.show('#attendanceModal');
                }
            }
        });
    },

    renderAttendanceList: function (data) {
        var self = this;
        var entries = data.entries || [];
        if (entries.length === 0) {
            $('#attendanceList').html('<p class="text-muted">등록된 하우스키퍼가 없습니다.</p>');
            return;
        }

        var html = '<div class="list-group list-group-flush">';
        entries.forEach(function (e) {
            var roleLabel = e.role === 'HOUSEKEEPING_SUPERVISOR' ?
                '<span class="badge bg-primary ms-1">감독자</span>' : '';
            var sectionLabel = e.sectionName ? '<small class="text-muted ms-1">(' + HolaPms.escapeHtml(e.sectionName) + ')</small>' : '';
            var checked = e.isAvailable ? ' checked' : '';
            var hkId = e.housekeeperId;

            // 모바일 출근 시간 뱃지 (B2)
            var timeBadge = '';
            var attStatus = e.attendanceStatus || 'BEFORE_WORK';
            var clockIn = e.clockInAt ? e.clockInAt.substring(11, 16) : '';
            var clockOut = e.clockOutAt ? e.clockOutAt.substring(11, 16) : '';
            if (attStatus === 'WORKING' && clockIn) {
                timeBadge = '<span class="badge bg-success ms-2">출근 ' + clockIn + '</span>';
            } else if (attStatus === 'LEFT' && clockIn) {
                timeBadge = '<span class="badge bg-primary ms-2">' + clockIn + '~' + clockOut + '</span>';
            }

            // 토글 OFF 시 휴무 뱃지 (B1)
            var dayOffBadge = !e.isAvailable ? '<span class="badge bg-secondary ms-2">휴무</span>' : '';

            html += '<div class="list-group-item d-flex align-items-center">' +
                '<div class="form-check form-switch me-3">' +
                    '<input class="form-check-input att-check" type="checkbox" data-hk-id="' + hkId + '"' + checked + '>' +
                '</div>' +
                '<div class="flex-grow-1">' +
                    HolaPms.escapeHtml(HolaPms.maskName(e.userName)) + roleLabel + sectionLabel + timeBadge +
                    '<span class="att-dayoff-badge" data-hk-id="' + hkId + '">' + dayOffBadge + '</span>' +
                '</div>' +
                '<select class="form-select form-select-sm att-shift" data-hk-id="' + hkId + '" style="width:90px;' + (e.isAvailable ? '' : 'display:none;') + '">' +
                    '<option value="DAY"' + (e.shiftType === 'DAY' ? ' selected' : '') + '>주간</option>' +
                    '<option value="EVENING"' + (e.shiftType === 'EVENING' ? ' selected' : '') + '>오후</option>' +
                    '<option value="NIGHT"' + (e.shiftType === 'NIGHT' ? ' selected' : '') + '>야간</option>' +
                '</select>' +
                '</div>';
        });
        html += '</div>';
        $('#attendanceList').html(html);

        // 토글 변경 시 휴무 뱃지/셀렉트 표시 전환
        $('#attendanceList').off('change', '.att-check').on('change', '.att-check', function () {
            var hkId = $(this).data('hk-id');
            var isOn = $(this).is(':checked');
            var $shift = $('.att-shift[data-hk-id="' + hkId + '"]');
            var $badge = $('.att-dayoff-badge[data-hk-id="' + hkId + '"]');
            if (isOn) {
                $shift.show();
                $badge.html('');
            } else {
                $shift.hide();
                $badge.html('<span class="badge bg-secondary ms-2">휴무</span>');
            }
        });
    },

    saveAttendance: function () {
        var self = this;
        var today = new Date().toISOString().split('T')[0];
        var entries = [];

        $('.att-check').each(function () {
            var hkId = parseInt($(this).data('hk-id'));
            var isAvailable = $(this).is(':checked');
            var shiftType = $('.att-shift[data-hk-id="' + hkId + '"]').val();
            entries.push({ housekeeperId: hkId, isAvailable: isAvailable, shiftType: shiftType });
        });

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/attendance',
            method: 'POST',
            data: JSON.stringify({ date: today, entries: entries }),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#attendanceModal');
                    HolaPms.alert('success', '출근부가 저장되었습니다.');
                    self.loadAttendanceSummary();
                }
            }
        });
    },

    // === 자동 배정 ===

    autoAssign: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/auto-assign',
            method: 'POST',
            data: JSON.stringify({}),
            success: function (res) {
                if (res.success) {
                    var count = res.data.assignedCount || 0;
                    HolaPms.alert('success', count + '건이 자동 배정되었습니다.');
                    self.loadDashboard();
                }
            }
        });
    },

    redistribute: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/task-sheets/redistribute',
            method: 'POST',
            data: JSON.stringify({}),
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '크레딧이 균등 재분배되었습니다.');
                    self.loadDashboard();
                }
            }
        });
    }
};

$(function () {
    HkDashboard.init();
});
