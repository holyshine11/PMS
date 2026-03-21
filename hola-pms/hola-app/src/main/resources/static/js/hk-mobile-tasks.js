/**
 * 하우스키핑 모바일 - 내 작업 리스트
 */
var HkMobile = {

    propertyId: null,
    tasks: [],
    activeFilter: 'ALL',

    init: function () {
        // 서버에서 전달한 프로퍼티 ID 사용 (Thymeleaf → JS 변수)
        this.propertyId = (typeof HK_PROPERTY_ID !== 'undefined') ? HK_PROPERTY_ID : null;
        if (!this.propertyId) {
            $('#taskList').html('<div class="text-center text-muted py-4">프로퍼티가 배정되지 않았습니다.</div>');
            return;
        }
        this.bindEvents();
        this.loadAttendanceStatus();
        this.loadTasks();
    },

    bindEvents: function () {
        var self = this;

        // 필터 탭
        $(document).on('click', '.filter-tab', function () {
            $('.filter-tab').removeClass('active btn-primary').addClass('btn-outline-secondary');
            $(this).removeClass('btn-outline-secondary').addClass('active btn-primary');
            self.activeFilter = $(this).data('filter');
            self.renderTasks();
        });

        // 작업 시작
        $(document).on('click', '.btn-task-start', function () {
            self.changeStatus($(this).data('id'), 'start', '청소를 시작합니다.');
        });

        // 작업 완료
        $(document).on('click', '.btn-task-complete', function () {
            self.changeStatus($(this).data('id'), 'complete', '청소를 완료합니다.');
        });

        // 작업 일시중단
        $(document).on('click', '.btn-task-pause', function () {
            self.changeStatus($(this).data('id'), 'pause', '작업을 중단합니다.');
        });

        // 이슈 등록 모달 열기
        $(document).on('click', '.btn-task-issue', function () {
            $('#issueTaskId').val($(this).data('id'));
            $('#issueType').val('MEMO');
            $('#issueDescription').val('');
            HolaPms.modal.show('#issueModal');
        });

        // 이슈 저장
        $('#btnSaveIssue').on('click', function () {
            self.saveIssue();
        });

        // 출퇴근 버튼
        $(document).on('click', '#btnClockIn', function () { self.clockIn(); });
        $(document).on('click', '#btnClockOut', function () { self.clockOut(); });

        // 이슈 기록 토글
        $(document).on('click', '.btn-task-issues-toggle', function () {
            var taskId = $(this).data('id');
            var $container = $('#issueList-' + taskId);
            if ($container.is(':visible')) {
                $container.slideUp(200);
            } else {
                self.loadTaskIssues(taskId);
                $container.slideDown(200);
            }
        });
    },

    loadTasks: function () {
        var self = this;
        var today = new Date().toISOString().split('T')[0];

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-tasks?date=' + today,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.tasks = res.data || [];
                    self.updateProgress();
                    self.renderTasks();
                }
            }
        });
    },

    updateProgress: function () {
        var total = this.tasks.length;
        var completed = this.tasks.filter(function (t) {
            return t.status === 'COMPLETED' || t.status === 'INSPECTED';
        }).length;
        var rate = total > 0 ? Math.round(completed * 100 / total) : 0;

        $('#totalCount').text(total);
        $('#completedCount').text(completed);
        $('#completionRate').text(rate + '%');
        $('#progressBar').css('width', rate + '%');
    },

    renderTasks: function () {
        var self = this;
        var $list = $('#taskList');
        $list.empty();

        var filtered = this.tasks;
        if (this.activeFilter !== 'ALL') {
            filtered = this.tasks.filter(function (t) { return t.status === self.activeFilter; });
        }

        if (filtered.length === 0) {
            var msg = this.tasks.length === 0
                ? '<div class="text-center py-5">' +
                  '<i class="fas fa-clipboard-check fa-3x text-muted mb-3" style="display:block;"></i>' +
                  '<div class="text-muted">오늘 배정된 작업이 없습니다.</div>' +
                  '<div class="text-muted" style="font-size:13px;">감독자가 작업을 배정하면 여기에 표시됩니다.</div></div>'
                : '<div class="text-center text-muted py-4">해당 상태의 작업이 없습니다.</div>';
            $list.html(msg);
            return;
        }

        // 우선순위 정렬: RUSH > HIGH > NORMAL > LOW
        var priorityOrder = { 'RUSH': 0, 'HIGH': 1, 'NORMAL': 2, 'LOW': 3 };
        filtered.sort(function (a, b) {
            return (priorityOrder[a.priority] || 2) - (priorityOrder[b.priority] || 2);
        });

        filtered.forEach(function (task) {
            $list.append(self.renderTaskCard(task));
        });
    },

    renderTaskCard: function (task) {
        var statusIcon = this.getStatusIcon(task.status);
        var typeLabel = this.getTaskTypeLabel(task.taskType);
        var actionBtn = this.getActionButton(task);
        var urgentHtml = '';

        if (task.nextCheckinAt) {
            urgentHtml = '<div class="urgent"><i class="fas fa-clock me-1"></i>' +
                task.nextCheckinAt.substring(11, 16) + ' 체크인</div>';
        }

        var timeInfo = '';
        if (task.status === 'IN_PROGRESS' && task.startedAt) {
            timeInfo = '<span class="room-info">진행중 ' + task.startedAt.substring(11, 16) + '~</span>';
        } else if ((task.status === 'COMPLETED' || task.status === 'INSPECTED') && task.startedAt && task.completedAt) {
            timeInfo = '<span class="room-info">완료 ' + task.startedAt.substring(11, 16) + '~' +
                task.completedAt.substring(11, 16) + ' (소요: ' + (task.durationMinutes || '-') + '분)</span>';
        }

        var issueBtn = '<button class="btn btn-sm btn-outline-secondary btn-task-issue" data-id="' + task.id + '">' +
            '<i class="fas fa-sticky-note me-1"></i>메모</button>';

        return '<div class="hk-task-card">' +
            '<div class="d-flex justify-content-between align-items-start">' +
                '<div>' +
                    '<span class="room-number">' + statusIcon + ' ' + HolaPms.escapeHtml(task.roomNumber || '-') + '호</span>' +
                    ' <span class="badge bg-secondary">' + typeLabel + '</span>' +
                    (task.priority === 'RUSH' ? ' <span class="badge bg-danger">긴급</span>' : '') +
                    (task.priority === 'HIGH' ? ' <span class="badge bg-warning text-dark">높음</span>' : '') +
                '</div>' +
                '<div>' + issueBtn + '</div>' +
            '</div>' +
            '<div class="room-info mt-1">' + (task.roomTypeName || '') + '</div>' +
            urgentHtml +
            timeInfo +
            (task.note ? '<div class="room-info mt-1"><i class="fas fa-comment-dots me-1 text-primary"></i>' + HolaPms.escapeHtml(task.note) + '</div>' : '') +
            '<div class="mt-1"><button class="btn btn-sm btn-link text-muted p-0 btn-task-issues-toggle" data-id="' + task.id + '" style="font-size:12px;text-decoration:none;">' +
                '<i class="fas fa-list-ul me-1"></i>이슈/메모 기록 보기</button></div>' +
            '<div id="issueList-' + task.id + '" style="display:none;" class="mt-1"></div>' +
            (actionBtn ? '<div class="mt-2">' + actionBtn + '</div>' : '') +
            '</div>';
    },

    getActionButton: function (task) {
        switch (task.status) {
            case 'PENDING':
                return '<button class="btn btn-primary btn-mobile-sm w-100 btn-task-start" data-id="' + task.id + '">' +
                    '<i class="fas fa-play me-2"></i>청소 시작</button>';
            case 'IN_PROGRESS':
                return '<div class="d-flex gap-2">' +
                    '<button class="btn btn-success btn-mobile-sm flex-grow-1 btn-task-complete" data-id="' + task.id + '">' +
                    '<i class="fas fa-check me-1"></i>완료</button>' +
                    '<button class="btn btn-outline-secondary btn-mobile-sm btn-task-pause" data-id="' + task.id + '">' +
                    '<i class="fas fa-pause"></i></button></div>';
            case 'PAUSED':
                return '<button class="btn btn-primary btn-mobile-sm w-100 btn-task-start" data-id="' + task.id + '">' +
                    '<i class="fas fa-play me-2"></i>재개</button>';
            default:
                return '';
        }
    },

    getStatusIcon: function (status) {
        var map = {
            'PENDING': '<i class="fas fa-circle text-warning" style="font-size:12px;"></i>',
            'IN_PROGRESS': '<i class="fas fa-circle text-primary" style="font-size:12px;"></i>',
            'PAUSED': '<i class="fas fa-circle text-secondary" style="font-size:12px;"></i>',
            'COMPLETED': '<i class="fas fa-check-circle text-success" style="font-size:12px;"></i>',
            'INSPECTED': '<i class="fas fa-check-double text-success" style="font-size:12px;"></i>'
        };
        return map[status] || '';
    },

    getTaskTypeLabel: function (type) {
        var map = { 'CHECKOUT': '퇴실청소', 'STAYOVER': '체류정리', 'TURNDOWN': '턴다운', 'DEEP_CLEAN': '딥클리닝', 'TOUCH_UP': '간단정리' };
        return map[type] || type;
    },

    _statusChanging: false,
    changeStatus: function (taskId, action, message) {
        var self = this;
        if (self._statusChanging) return;
        self._statusChanging = true;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-tasks/' + taskId + '/' + action,
            method: 'PUT',
            success: function (res) {
                self._statusChanging = false;
                if (res.success) {
                    HolaPms.alert('success', message || '상태가 변경되었습니다.');
                    self.loadTasks();
                }
            },
            error: function () { self._statusChanging = false; }
        });
    },

    ISSUE_TYPE_MAP: {
        'MEMO': '일반 메모', 'MAINTENANCE': '유지보수', 'SUPPLY_SHORT': '비품 부족',
        'LOST_FOUND': '분실물', 'DAMAGE': '파손/결함'
    },

    saveIssue: function () {
        var self = this;
        var taskId = $('#issueTaskId').val();
        var data = {
            issueType: $('#issueType').val(),
            description: $('#issueDescription').val()
        };
        if (!data.description) {
            HolaPms.alert('warning', '내용을 입력해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-tasks/' + taskId + '/issues',
            method: 'POST',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#issueModal');
                    HolaPms.alert('success', '이슈가 등록되었습니다.');
                    // 등록 후 해당 작업의 이슈 목록 자동 표시
                    self.loadTaskIssues(taskId);
                    $('#issueList-' + taskId).slideDown(200);
                }
            }
        });
    },

    loadTaskIssues: function (taskId) {
        var self = this;
        var $container = $('#issueList-' + taskId);
        $container.html('<div class="text-center text-muted py-1" style="font-size:12px;"><i class="fas fa-spinner fa-spin me-1"></i>로딩 중...</div>');

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-tasks/' + taskId + '/issues',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    var issues = res.data || [];
                    if (!issues.length) {
                        $container.html('<div class="text-muted py-1" style="font-size:12px;">등록된 기록이 없습니다.</div>');
                        return;
                    }
                    var html = '';
                    issues.forEach(function (issue) {
                        var typeLabel = self.ISSUE_TYPE_MAP[issue.issueType] || issue.issueType;
                        var time = issue.createdAt ? issue.createdAt.substring(11, 16) : '';
                        var resolvedMark = issue.resolved ? ' <i class="fas fa-check-circle text-success"></i>' : '';
                        var creatorName = issue.createdByName ? ' · ' + HolaPms.escapeHtml(issue.createdByName) : '';
                        html += '<div style="font-size:12px;padding:4px 0;border-bottom:1px solid #eee;">' +
                            '<span class="badge bg-secondary" style="font-size:10px;">' + typeLabel + '</span>' + resolvedMark +
                            ' <span class="text-muted">' + time + creatorName + '</span>' +
                            '<div style="margin-top:2px;">' + HolaPms.escapeHtml(issue.description) + '</div>' +
                        '</div>';
                    });
                    $container.html(html);
                }
            }
        });
    },

    // === 모바일 근태 ===

    loadAttendanceStatus: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-attendance',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.renderAttendanceButton(res.data);
                }
            },
            error: function () {
                // 출근부 미등록 시 버튼 숨김
                $('#attendanceBtnArea').empty();
            }
        });
    },

    renderAttendanceButton: function (data) {
        var $area = $('#attendanceBtnArea');
        var status = data.attendanceStatus || 'BEFORE_WORK';
        var html = '';

        switch (status) {
            case 'BEFORE_WORK':
                html = '<button id="btnClockIn" class="btn btn-sm btn-success" style="font-size:13px;padding:4px 12px;">' +
                    '<i class="fas fa-sign-in-alt me-1"></i>출근</button>';
                break;
            case 'WORKING':
                html = '<button id="btnClockOut" class="btn btn-sm btn-danger" style="font-size:13px;padding:4px 12px;">' +
                    '<i class="fas fa-sign-out-alt me-1"></i>퇴근</button>';
                break;
            case 'LEFT':
                html = '<span class="badge bg-secondary" style="font-size:12px;">퇴근완료</span>';
                break;
            case 'DAY_OFF':
                html = '<span class="badge bg-secondary" style="font-size:12px;">휴무</span>';
                break;
        }
        $area.html(html);
    },

    clockIn: function () {
        var self = this;
        if (!confirm('출근 처리하시겠습니까?')) return;
        // 더블클릭 방지
        $('#btnClockIn').prop('disabled', true);
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/clock-in',
            method: 'POST',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '출근 처리되었습니다.');
                    self.loadAttendanceStatus();
                }
            },
            error: function (xhr) {
                $('#btnClockIn').prop('disabled', false);
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '출근 처리에 실패했습니다.';
                HolaPms.alert('error', msg);
            }
        });
    },

    clockOut: function () {
        var self = this;
        if (!confirm('퇴근 처리하시겠습니까?')) return;
        // 더블클릭 방지
        $('#btnClockOut').prop('disabled', true);
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/clock-out',
            method: 'POST',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '퇴근 처리되었습니다.');
                    self.loadAttendanceStatus();
                }
            },
            error: function (xhr) {
                $('#btnClockOut').prop('disabled', false);
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '퇴근 처리에 실패했습니다.';
                HolaPms.alert('error', msg);
            }
        });
    }
};

$(function () {
    HkMobile.init();
});
