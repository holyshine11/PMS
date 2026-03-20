/**
 * 하우스키핑 작업 관리 페이지
 */
var HkTasks = {

    propertyId: null,
    dataTable: null,
    housekeepers: [],

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () {
            self.reload();
        });

        // 오늘 날짜 기본값
        $('#filterDate').val(new Date().toISOString().split('T')[0]);

        // 조회 버튼
        $('#btnSearch').on('click', function () {
            self.loadTasks();
        });

        // 작업 생성 모달 열기
        $('#btnCreateTask').on('click', function () {
            self.openCreateModal();
        });

        // 작업 저장
        $('#btnSaveTask').on('click', function () {
            self.saveTask();
        });

        // 비고 컬럼 클릭 → 메모/이슈 상세 모달
        $(document).on('click', '.btn-hk-memo', function () {
            var taskId = $(this).data('id');
            var note = $(this).data('note') || '';
            self.showMemoDetail(taskId, note);
        });

        // 테이블 액션 버튼 이벤트 위임
        $(document).on('click', '.btn-hk-start', function () {
            self.changeStatus($(this).data('id'), 'start');
        });
        $(document).on('click', '.btn-hk-complete', function () {
            self.changeStatus($(this).data('id'), 'complete');
        });
        $(document).on('click', '.btn-hk-inspect', function () {
            self.changeStatus($(this).data('id'), 'inspect');
        });
        $(document).on('click', '.btn-hk-cancel', function () {
            if (confirm('이 작업을 취소하시겠습니까?')) {
                self.cancelTask($(this).data('id'));
            }
        });

        // 인라인 담당자 변경
        $(document).on('change', '.hk-inline-assign', function () {
            var taskId = $(this).data('id');
            var assignedTo = $(this).val();
            self.assignSingle(taskId, assignedTo);
        });

        // 체크박스: 전체 선택
        $(document).on('change', '#hkCheckAll', function () {
            var checked = $(this).is(':checked');
            $('.hk-check-item').prop('checked', checked);
            self.updateBatchToolbar();
        });

        // 체크박스: 개별 선택
        $(document).on('change', '.hk-check-item', function () {
            self.updateBatchToolbar();
        });

        // 일괄 배정 실행
        $('#btnBatchAssign').on('click', function () {
            self.batchAssign();
        });

        // 선택 해제
        $('#btnBatchCancel').on('click', function () {
            $('.hk-check-item, #hkCheckAll').prop('checked', false);
            self.updateBatchToolbar();
        });
    },

    reload: function () {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#filterCard, #taskTableCard, #batchToolbar').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#filterCard, #taskTableCard').show();
        this.propertyId = propertyId;
        this.loadHousekeepers();
        this.loadRooms();
        this.loadTasks();
    },

    _loadSeq: 0,

    loadTasks: function () {
        var self = this;
        var seq = ++self._loadSeq;
        var params = {
            date: $('#filterDate').val() || new Date().toISOString().split('T')[0]
        };
        var status = $('#filterStatus').val();
        if (status) params.status = status;
        var taskType = $('#filterTaskType').val();
        if (taskType) params.taskType = taskType;
        var assignedTo = $('#filterAssignedTo').val();
        if (assignedTo) params.assignedTo = assignedTo;

        var queryStr = $.param(params);

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks?' + queryStr,
            method: 'GET',
            success: function (res) {
                if (seq !== self._loadSeq) return;
                if (res.success) {
                    self.renderTable(res.data || []);
                }
            }
        });
    },

    renderTable: function (data) {
        var self = this;
        // 기존 DataTable 제거
        if (self.dataTable) {
            self.dataTable.destroy();
            self.dataTable = null;
        }
        // thead 보장을 위해 테이블 HTML 재구성
        $('#taskTable').html(
            '<thead class="table-light"><tr>' +
            '<th class="text-center" style="width:36px;"><input type="checkbox" id="hkCheckAll"></th>' +
            '<th class="text-center">호수</th>' +
            '<th class="text-center">유형</th>' +
            '<th class="text-center">상태</th>' +
            '<th class="text-center">우선순위</th>' +
            '<th class="text-center">담당자</th>' +
            '<th class="text-center">시작</th>' +
            '<th class="text-center">완료</th>' +
            '<th class="text-center">소요(분)</th>' +
            '<th class="text-center">크레딧</th>' +
            '<th class="text-center">비고</th>' +
            '<th class="text-center">관리</th>' +
            '</tr></thead>'
        );

        self.dataTable = $('#taskTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            data: data,
            columns: [
                { data: null, className: 'text-center', orderable: false, render: function (data) {
                    // INSPECTED/CANCELLED은 체크박스 제외
                    if (data.status === 'INSPECTED' || data.status === 'CANCELLED') return '';
                    return '<input type="checkbox" class="hk-check-item" value="' + data.id + '">';
                }},
                { data: 'roomNumber', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'taskType', className: 'text-center', render: function (val) { return self.getTaskTypeBadge(val); } },
                { data: 'status', className: 'text-center', render: function (val) { return self.getStatusBadge(val); } },
                { data: 'priority', className: 'text-center', render: function (val) { return self.getPriorityBadge(val); } },
                { data: null, className: 'text-center', orderable: false, render: function (data) {
                    return self.renderAssignCell(data);
                }},
                { data: 'startedAt', className: 'text-center', render: function (val) { return val ? val.substring(11, 16) : '-'; } },
                { data: 'completedAt', className: 'text-center', render: function (val) { return val ? val.substring(11, 16) : '-'; } },
                { data: 'durationMinutes', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'credit', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'note', className: 'text-center', orderable: false, render: function (val, type, row) {
                    return '<button class="btn btn-sm btn-outline-secondary btn-hk-memo" data-id="' + row.id + '" data-note="' + HolaPms.escapeHtml(val || '') + '" title="메모/이슈 보기">' +
                        '<i class="fas fa-comment-dots' + (val ? ' text-primary' : '') + '"></i></button>';
                }},
                { data: null, className: 'text-center', orderable: false, render: function (data) { return self.renderActions(data); } }
            ],
            order: [[4, 'asc'], [1, 'asc']],
            pageLength: 20
        }));

        // 일괄 도구 초기화
        $('#hkCheckAll').prop('checked', false);
        self.updateBatchToolbar();
    },

    /**
     * 담당자 컬럼 렌더: 인라인 드롭다운 (INSPECTED/CANCELLED 제외)
     */
    renderAssignCell: function (data) {
        if (data.status === 'INSPECTED' || data.status === 'CANCELLED') {
            return data.assignedToName ? HolaPms.escapeHtml(HolaPms.maskName(data.assignedToName)) : '-';
        }
        var html = '<select class="form-select form-select-sm hk-inline-assign" data-id="' + data.id + '" style="font-size:12px;min-width:90px;padding:2px 24px 2px 6px;">';
        html += '<option value="">미배정</option>';
        this.housekeepers.forEach(function (hk) {
            var selected = (data.assignedTo && data.assignedTo.toString() === hk.userId.toString()) ? ' selected' : '';
            html += '<option value="' + hk.userId + '"' + selected + '>' + HolaPms.escapeHtml(HolaPms.maskName(hk.userName)) + '</option>';
        });
        html += '</select>';
        return html;
    },

    /**
     * 개별 담당자 배정/해제
     */
    assignSingle: function (taskId, assignedTo) {
        var self = this;
        if (assignedTo) {
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId + '/assign',
                method: 'PUT',
                data: JSON.stringify({ assignedTo: parseInt(assignedTo) }),
                success: function (res) {
                    if (res.success) {
                        HolaPms.alert('success', '담당자가 배정되었습니다.');
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
                    }
                }
            });
        }
    },

    /**
     * 일괄 배정 툴바 표시/숨김
     */
    updateBatchToolbar: function () {
        var checked = $('.hk-check-item:checked');
        if (checked.length > 0) {
            $('#batchToolbar').removeClass('d-none').show();
            $('#batchCount').text(checked.length);
        } else {
            $('#batchToolbar').addClass('d-none');
        }
    },

    /**
     * 일괄 배정 실행
     */
    batchAssign: function () {
        var self = this;
        var taskIds = [];
        $('.hk-check-item:checked').each(function () {
            taskIds.push(parseInt($(this).val()));
        });

        if (taskIds.length === 0) {
            HolaPms.alert('warning', '작업을 선택해주세요.');
            return;
        }

        var assignedTo = $('#batchAssignTo').val();
        var data = {
            taskIds: taskIds,
            assignedTo: assignedTo ? parseInt(assignedTo) : null
        };

        var msg = assignedTo ? '선택한 ' + taskIds.length + '건을 배정하시겠습니까?' :
                               '선택한 ' + taskIds.length + '건의 배정을 해제하시겠습니까?';
        if (!confirm(msg)) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/batch-assign',
            method: 'PUT',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', taskIds.length + '건이 처리되었습니다.');
                    self.loadTasks();
                }
            }
        });
    },

    renderActions: function (data) {
        var html = '<div class="btn-group btn-group-sm">';
        switch (data.status) {
            case 'PENDING':
                html += '<button class="btn btn-outline-primary btn-hk-start" data-id="' + data.id + '" title="시작"><i class="fas fa-play"></i></button>';
                html += '<button class="btn btn-outline-danger btn-hk-cancel" data-id="' + data.id + '" title="취소"><i class="fas fa-times"></i></button>';
                break;
            case 'IN_PROGRESS':
                html += '<button class="btn btn-outline-success btn-hk-complete" data-id="' + data.id + '" title="완료"><i class="fas fa-check"></i></button>';
                break;
            case 'COMPLETED':
                html += '<button class="btn btn-outline-info btn-hk-inspect" data-id="' + data.id + '" title="검수"><i class="fas fa-clipboard-check"></i></button>';
                break;
        }
        html += '</div>';
        return html;
    },

    changeStatus: function (taskId, action) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId + '/' + action,
            method: 'PUT',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '상태가 변경되었습니다.');
                    self.loadTasks();
                }
            }
        });
    },

    cancelTask: function (taskId) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId,
            method: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '작업이 취소되었습니다.');
                    self.loadTasks();
                }
            }
        });
    },

    // === 작업 생성 ===

    openCreateModal: function () {
        $('#createTaskType').val('CHECKOUT');
        $('#createPriority').val('NORMAL');
        $('#createAssignedTo').val('');
        $('#createNote').val('');
        $('#createRoomNumberId').val('');
        HolaPms.modal.show('#createTaskModal');
    },

    saveTask: function () {
        var self = this;
        var roomNumberId = $('#createRoomNumberId').val();
        if (!roomNumberId) {
            HolaPms.alert('warning', '객실을 선택해주세요.');
            return;
        }

        var data = {
            roomNumberId: parseInt(roomNumberId),
            taskType: $('#createTaskType').val(),
            priority: $('#createPriority').val(),
            assignedTo: $('#createAssignedTo').val() ? parseInt($('#createAssignedTo').val()) : null,
            note: HolaPms.form.val('#createNote')
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks',
            method: 'POST',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#createTaskModal');
                    HolaPms.alert('success', '작업이 생성되었습니다.');
                    self.loadTasks();
                }
            }
        });
    },

    // === 하우스키퍼/객실 드롭다운 로드 ===

    loadHousekeepers: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/housekeepers',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.housekeepers = res.data || [];
                    var $filter = $('#filterAssignedTo');
                    var $create = $('#createAssignedTo');
                    var $batch = $('#batchAssignTo');
                    $filter.find('option:not(:first)').remove();
                    $create.find('option:not(:first)').remove();
                    $batch.find('option:not(:first)').remove();
                    self.housekeepers.forEach(function (hk) {
                        var opt = '<option value="' + hk.userId + '">' + HolaPms.escapeHtml(HolaPms.maskName(hk.userName)) + '</option>';
                        $filter.append(opt);
                        $create.append(opt);
                        $batch.append(opt);
                    });
                }
            }
        });
    },

    loadRooms: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-numbers',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    var list = res.data || [];
                    var $select = $('#createRoomNumberId');
                    $select.find('option:not(:first)').remove();
                    list.forEach(function (room) {
                        $select.append('<option value="' + room.id + '">' + HolaPms.escapeHtml(room.roomNumber) + '</option>');
                    });
                }
            }
        });
    },

    // === 뱃지 렌더 ===

    getStatusBadge: function (status) {
        var map = {
            'PENDING': '<span class="badge bg-warning text-dark">대기</span>',
            'IN_PROGRESS': '<span class="badge bg-primary">진행중</span>',
            'PAUSED': '<span class="badge bg-secondary">중단</span>',
            'COMPLETED': '<span class="badge bg-success">완료</span>',
            'INSPECTED': '<span class="badge" style="background:#20c997;">검수완료</span>',
            'CANCELLED': '<span class="badge bg-danger">취소</span>'
        };
        return map[status] || '<span class="badge bg-secondary">' + status + '</span>';
    },

    getTaskTypeBadge: function (type) {
        var map = {
            'CHECKOUT': '<span class="badge bg-danger" title="퇴실 후 전체 청소">퇴실청소</span>',
            'STAYOVER': '<span class="badge bg-info" title="투숙 중 객실 정리">체류정리</span>',
            'TURNDOWN': '<span class="badge bg-secondary" title="저녁 턴다운 서비스">턴다운</span>',
            'DEEP_CLEAN': '<span class="badge bg-dark" title="정기 딥클리닝 (침구·카펫 등)">딥클리닝</span>',
            'TOUCH_UP': '<span class="badge bg-light text-dark" title="빠른 간단 정리">간단정리</span>'
        };
        return map[type] || type;
    },

    // === 메모/이슈 상세 모달 ===

    ISSUE_TYPE_MAP: {
        'MEMO': '일반 메모', 'MAINTENANCE': '유지보수', 'SUPPLY_SHORT': '비품 부족',
        'LOST_FOUND': '분실물', 'DAMAGE': '파손/결함'
    },

    showMemoDetail: function (taskId, note) {
        var self = this;
        // 메모 표시
        if (note) {
            $('#memoNoteContent').html('<div class="alert alert-light mb-0"><i class="fas fa-comment-dots me-2 text-primary"></i>' + HolaPms.escapeHtml(note) + '</div>');
        } else {
            $('#memoNoteContent').html('<span class="text-muted">작업 메모가 없습니다.</span>');
        }
        // 이슈 로딩
        $('#memoIssueList').html('<div class="text-center text-muted py-2"><i class="fas fa-spinner fa-spin me-1"></i>로딩 중...</div>');
        HolaPms.modal.show('#memoDetailModal');

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId + '/issues',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.renderIssueList(res.data || []);
                }
            },
            error: function () {
                $('#memoIssueList').html('<span class="text-muted">이슈를 불러올 수 없습니다.</span>');
            }
        });
    },

    renderIssueList: function (issues) {
        var self = this;
        if (!issues || issues.length === 0) {
            $('#memoIssueList').html('<span class="text-muted">등록된 이슈/메모가 없습니다.</span>');
            return;
        }
        var html = '<div class="list-group list-group-flush">';
        issues.forEach(function (issue) {
            var typeLabel = self.ISSUE_TYPE_MAP[issue.issueType] || issue.issueType;
            var time = issue.createdAt ? issue.createdAt.substring(0, 16).replace('T', ' ') : '';
            var resolvedBadge = issue.resolved ? '<span class="badge bg-success ms-1">해결</span>' : '';
            html += '<div class="list-group-item px-0">' +
                '<div class="d-flex justify-content-between align-items-center">' +
                    '<span class="badge bg-secondary">' + typeLabel + '</span>' + resolvedBadge +
                    '<small class="text-muted">' + time + (issue.createdByName ? ' · ' + HolaPms.escapeHtml(issue.createdByName) : '') + '</small>' +
                '</div>' +
                '<div class="mt-1" style="font-size:14px;">' + HolaPms.escapeHtml(issue.description) + '</div>' +
            '</div>';
        });
        html += '</div>';
        $('#memoIssueList').html(html);
    },

    getPriorityBadge: function (priority) {
        var map = {
            'RUSH': '<span class="badge bg-danger">긴급</span>',
            'HIGH': '<span class="badge bg-warning text-dark">높음</span>',
            'NORMAL': '<span class="text-muted">보통</span>',
            'LOW': '<span class="text-muted">낮음</span>'
        };
        return map[priority] || priority;
    }
};

$(function () {
    HkTasks.init();
});
