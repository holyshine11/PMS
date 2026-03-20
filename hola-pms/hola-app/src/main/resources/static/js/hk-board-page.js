/**
 * 하우스키핑 작업 보드 (칸반)
 */
var HkBoard = {

    propertyId: null,
    tasks: [],
    housekeepers: [],

    init: function () {
        $('#boardDate').val(new Date().toISOString().split('T')[0]);
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () {
            self.reload();
        });

        $('#btnBoardRefresh').on('click', function () {
            self.loadTasks();
        });

        $('#boardDate').on('change', function () {
            self.loadTasks();
        });

        $('#boardFilterAssigned').on('change', function () {
            self.renderBoard();
        });

        // 카드 액션 버튼
        $(document).on('click', '.kb-start', function (e) {
            e.stopPropagation();
            self.changeStatus($(this).data('id'), 'start');
        });
        $(document).on('click', '.kb-complete', function (e) {
            e.stopPropagation();
            self.changeStatus($(this).data('id'), 'complete');
        });
        $(document).on('click', '.kb-inspect', function (e) {
            e.stopPropagation();
            self.changeStatus($(this).data('id'), 'inspect');
        });

        // 인라인 담당자 변경
        $(document).on('change', '.kb-assign-select', function (e) {
            e.stopPropagation();
            var taskId = $(this).data('id');
            var assignedTo = $(this).val();
            self.assignTask(taskId, assignedTo);
        });
    },

    reload: function () {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#kanbanBoard').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#kanbanBoard').show();
        this.propertyId = propertyId;
        this.loadHousekeepers();
        this.loadTasks();
    },

    loadTasks: function () {
        var self = this;
        var date = $('#boardDate').val() || new Date().toISOString().split('T')[0];

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks?date=' + date,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.tasks = res.data || [];
                    self.renderBoard();
                }
            }
        });
    },

    renderBoard: function () {
        var self = this;
        var filterAssigned = $('#boardFilterAssigned').val();

        var filtered = this.tasks;
        if (filterAssigned) {
            filtered = this.tasks.filter(function (t) {
                return t.assignedTo && t.assignedTo.toString() === filterAssigned;
            });
        }

        var pending = filtered.filter(function (t) { return t.status === 'PENDING' || t.status === 'PAUSED'; });
        var progress = filtered.filter(function (t) { return t.status === 'IN_PROGRESS'; });
        var completed = filtered.filter(function (t) { return t.status === 'COMPLETED'; });
        var inspected = filtered.filter(function (t) { return t.status === 'INSPECTED'; });

        $('#countPending').text(pending.length);
        $('#countProgress').text(progress.length);
        $('#countCompleted').text(completed.length);
        $('#countInspected').text(inspected.length);

        self.renderColumn('#colPending', pending, '대기 중인 작업이 없습니다');
        self.renderColumn('#colProgress', progress, '진행 중인 작업이 없습니다');
        self.renderColumn('#colCompleted', completed, '완료된 작업이 없습니다');
        self.renderColumn('#colInspected', inspected, '검수 대기 없음');
    },

    renderColumn: function (selector, tasks, emptyMsg) {
        var self = this;
        var $col = $(selector);
        $col.empty();

        if (tasks.length === 0) {
            $col.html('<div class="text-center text-muted py-4" style="font-size:12px;">' +
                '<i class="fas fa-inbox mb-2" style="font-size:24px;display:block;opacity:0.3;"></i>' +
                emptyMsg + '</div>');
            return;
        }

        tasks.forEach(function (task) {
            $col.append(self.renderCard(task));
        });
    },

    renderCard: function (task) {
        var self = this;
        var priorityBadge = '';
        if (task.priority === 'RUSH') priorityBadge = '<span class="badge bg-danger ms-1">긴급</span>';
        else if (task.priority === 'HIGH') priorityBadge = '<span class="badge bg-warning text-dark ms-1">높음</span>';

        var typeMap = { 'CHECKOUT': '퇴실', 'STAYOVER': '체류', 'TURNDOWN': '턴다운', 'DEEP_CLEAN': '딥클린', 'TOUCH_UP': '간단' };
        var typeLabel = typeMap[task.taskType] || task.taskType;

        var actionBtn = '';
        switch (task.status) {
            case 'PENDING':
            case 'PAUSED':
                actionBtn = '<button class="btn btn-sm btn-outline-primary w-100 mt-2 kb-start" data-id="' + task.id + '"><i class="fas fa-play me-1"></i>시작</button>';
                break;
            case 'IN_PROGRESS':
                actionBtn = '<button class="btn btn-sm btn-outline-success w-100 mt-2 kb-complete" data-id="' + task.id + '"><i class="fas fa-check me-1"></i>완료</button>';
                break;
            case 'COMPLETED':
                actionBtn = '<button class="btn btn-sm btn-outline-info w-100 mt-2 kb-inspect" data-id="' + task.id + '"><i class="fas fa-clipboard-check me-1"></i>검수</button>';
                break;
        }

        var timeInfo = '';
        if (task.startedAt) {
            timeInfo = '<div style="font-size:11px;color:#6c757d;">' + task.startedAt.substring(11, 16) + '~';
            if (task.completedAt) timeInfo += task.completedAt.substring(11, 16);
            timeInfo += '</div>';
        }

        // 인라인 담당자 드롭다운 (INSPECTED, CANCELLED 제외)
        var assignSelect = '';
        if (task.status !== 'INSPECTED' && task.status !== 'CANCELLED') {
            assignSelect = '<div class="mt-1">' + self.buildAssignSelect(task.id, task.assignedTo) + '</div>';
        } else if (task.assignedToName) {
            assignSelect = '<div class="assignee mt-1"><i class="fas fa-user me-1"></i>' + HolaPms.escapeHtml(HolaPms.maskName(task.assignedToName)) + '</div>';
        }

        return '<div class="kanban-card">' +
            '<div class="d-flex justify-content-between align-items-start">' +
                '<span class="room-no">' + HolaPms.escapeHtml(task.roomNumber || '-') + '</span>' +
                '<span class="badge bg-secondary">' + typeLabel + '</span>' +
            '</div>' +
            '<div class="room-type">' + (task.roomTypeName || '') + priorityBadge + '</div>' +
            assignSelect +
            timeInfo +
            actionBtn +
            '</div>';
    },

    /**
     * 담당자 선택 드롭다운 HTML 생성
     */
    buildAssignSelect: function (taskId, currentAssignedTo) {
        var html = '<select class="form-select form-select-sm kb-assign-select" data-id="' + taskId + '" style="font-size:12px;padding:2px 24px 2px 6px;">';
        html += '<option value="">미배정</option>';
        this.housekeepers.forEach(function (hk) {
            var selected = (currentAssignedTo && currentAssignedTo.toString() === hk.userId.toString()) ? ' selected' : '';
            html += '<option value="' + hk.userId + '"' + selected + '>' + HolaPms.escapeHtml(HolaPms.maskName(hk.userName)) + '</option>';
        });
        html += '</select>';
        return html;
    },

    /**
     * 담당자 배정/해제 API 호출
     */
    assignTask: function (taskId, assignedTo) {
        var self = this;
        if (assignedTo) {
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId + '/assign',
                method: 'PUT',
                data: JSON.stringify({ assignedTo: parseInt(assignedTo) }),
                success: function (res) {
                    if (res.success) {
                        HolaPms.alert('success', '담당자가 배정되었습니다.');
                        // 로컬 데이터만 갱신 (보드 재렌더링 하지 않아 카드 위치 유지)
                        var task = self.tasks.find(function(t) { return t.id === taskId; });
                        if (task) task.assignedTo = parseInt(assignedTo);
                    }
                }
            });
        } else {
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId + '/unassign',
                method: 'PUT',
                success: function (res) {
                    if (res.success) {
                        HolaPms.alert('success', '배정이 해제되었습니다.');
                        var task = self.tasks.find(function(t) { return t.id === taskId; });
                        if (task) { task.assignedTo = null; task.assignedToName = null; }
                    }
                }
            });
        }
    },

    changeStatus: function (taskId, action) {
        var self = this;
        var url = '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId + '/' + action;
        HolaPms.ajax({
            url: url,
            method: 'PUT',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '상태가 변경되었습니다.');
                    self.loadTasks();
                }
            }
        });
    },

    loadHousekeepers: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/housekeepers',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.housekeepers = res.data || [];
                    var $sel = $('#boardFilterAssigned');
                    $sel.find('option:not(:first)').remove();
                    self.housekeepers.forEach(function (hk) {
                        $sel.append('<option value="' + hk.userId + '">' + HolaPms.escapeHtml(HolaPms.maskName(hk.userName)) + '</option>');
                    });
                }
            }
        });
    }
};

$(function () {
    HkBoard.init();
});
