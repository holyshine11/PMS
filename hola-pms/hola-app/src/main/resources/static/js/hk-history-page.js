/**
 * 하우스키핑 이력 조회 페이지
 */
var HkHistory = {

    propertyId: null,
    dataTable: null,

    ISSUE_TYPE_MAP: {
        'MEMO': '일반 메모', 'MAINTENANCE': '유지보수', 'SUPPLY_SHORT': '비품 부족',
        'LOST_FOUND': '분실물', 'DAMAGE': '파손/결함'
    },

    init: function () {
        // 기본 기간: 최근 7일
        var today = new Date().toISOString().split('T')[0];
        var weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0];
        $('#filterFrom').val(weekAgo);
        $('#filterTo').val(today);

        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;
        $(document).on('hola:contextChange', function () { self.reload(); });
        $('#btnSearch').on('click', function () { self.loadHistory(); });

        // 비고 클릭 → 메모/이슈 상세 모달
        $(document).on('click', '.btn-hist-memo', function () {
            var taskId = $(this).data('id');
            var note = $(this).data('note') || '';
            self.showMemoDetail(taskId, note);
        });
    },

    reload: function () {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#filterCard, #statsRow, #historyTableCard').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#filterCard, #statsRow, #historyTableCard').show();
        this.propertyId = propertyId;
        this.loadHousekeepers();
        this.loadHistory();
    },

    _loadSeq: 0,

    loadHistory: function () {
        var self = this;
        var seq = ++self._loadSeq;
        var params = {
            from: $('#filterFrom').val(),
            to: $('#filterTo').val()
        };
        var assignedTo = $('#filterAssignedTo').val();
        if (assignedTo) params.assignedTo = assignedTo;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/history?' + $.param(params),
            method: 'GET',
            success: function (res) {
                if (seq !== self._loadSeq) return;
                if (res.success) {
                    var data = res.data || [];
                    self.updateStats(data);
                    self.renderTable(data);
                }
            }
        });
    },

    updateStats: function (data) {
        var total = data.length;
        var completed = data.filter(function (t) { return t.status === 'COMPLETED' || t.status === 'INSPECTED'; }).length;
        var cancelled = data.filter(function (t) { return t.status === 'CANCELLED'; }).length;
        var durations = data.filter(function (t) { return t.durationMinutes; }).map(function (t) { return t.durationMinutes; });
        var avgTime = durations.length > 0 ? Math.round(durations.reduce(function (a, b) { return a + b; }, 0) / durations.length) : 0;
        var rate = total > 0 ? Math.round(completed * 100 / total) : 0;

        $('#statTotal').text(total);
        $('#statCompleted').text(completed);
        $('#statCancelled').text(cancelled);
        $('#statAvgTime').text(avgTime > 0 ? avgTime + '분' : '-');
        $('#statRate').text(rate + '%');
    },

    renderTable: function (data) {
        var self = this;
        if (self.dataTable) {
            self.dataTable.destroy();
            self.dataTable = null;
        }
        $('#historyTable').html(
            '<thead class="table-light"><tr>' +
            '<th class="text-center">날짜</th>' +
            '<th class="text-center">호수</th>' +
            '<th class="text-center">유형</th>' +
            '<th class="text-center">상태</th>' +
            '<th class="text-center">담당자</th>' +
            '<th class="text-center">시작</th>' +
            '<th class="text-center">완료</th>' +
            '<th class="text-center">소요(분)</th>' +
            '<th class="text-center">크레딧</th>' +
            '<th class="text-center">비고</th>' +
            '</tr></thead>'
        );

        self.dataTable = $('#historyTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            data: data,
            columns: [
                { data: 'taskDate', className: 'text-center' },
                { data: 'roomNumber', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'taskType', className: 'text-center', render: function (v) {
                    var m = { 'CHECKOUT': '퇴실청소', 'STAYOVER': '체류정리', 'TURNDOWN': '턴다운', 'DEEP_CLEAN': '딥클리닝', 'TOUCH_UP': '간단정리' };
                    return m[v] || v;
                }},
                { data: 'status', className: 'text-center', render: function (v) {
                    var m = { 'PENDING': '<span class="badge bg-warning text-dark">대기</span>', 'IN_PROGRESS': '<span class="badge bg-primary">진행</span>',
                        'COMPLETED': '<span class="badge bg-success">완료</span>', 'INSPECTED': '<span class="badge" style="background:#20c997;">검수</span>',
                        'CANCELLED': '<span class="badge bg-danger">취소</span>' };
                    return m[v] || v;
                }},
                { data: 'assignedToName', className: 'text-center', render: function(val) { return val ? HolaPms.escapeHtml(HolaPms.maskName(val)) : '-'; } },
                { data: 'startedAt', className: 'text-center', render: function (v) { return v ? v.substring(11, 16) : '-'; } },
                { data: 'completedAt', className: 'text-center', render: function (v) { return v ? v.substring(11, 16) : '-'; } },
                { data: 'durationMinutes', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'credit', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'note', className: 'text-center', orderable: false, render: function (val, type, row) {
                    return '<button class="btn btn-sm btn-outline-secondary btn-hist-memo" data-id="' + row.id + '" data-note="' + HolaPms.escapeHtml(val || '') + '" title="메모/이슈 보기">' +
                        '<i class="fas fa-comment-dots' + (val ? ' text-primary' : '') + '"></i></button>';
                }}
            ],
            order: [[0, 'desc']],
            pageLength: 20
        }));
    },

    // === 메모/이슈 상세 모달 ===

    showMemoDetail: function (taskId, note) {
        var self = this;
        if (note) {
            $('#histMemoNoteContent').html('<div class="alert alert-light mb-0"><i class="fas fa-comment-dots me-2 text-primary"></i>' + HolaPms.escapeHtml(note) + '</div>');
        } else {
            $('#histMemoNoteContent').html('<span class="text-muted">작업 메모가 없습니다.</span>');
        }
        $('#histMemoIssueList').html('<div class="text-center text-muted py-2"><i class="fas fa-spinner fa-spin me-1"></i>로딩 중...</div>');
        HolaPms.modal.show('#histMemoDetailModal');

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/tasks/' + taskId + '/issues',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    var issues = res.data || [];
                    if (!issues.length) {
                        $('#histMemoIssueList').html('<span class="text-muted">등록된 이슈/메모가 없습니다.</span>');
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
                    $('#histMemoIssueList').html(html);
                }
            },
            error: function () {
                $('#histMemoIssueList').html('<span class="text-muted">이슈를 불러올 수 없습니다.</span>');
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
                    var $sel = $('#filterAssignedTo');
                    $sel.find('option:not(:first)').remove();
                    (res.data || []).forEach(function (hk) {
                        $sel.append('<option value="' + hk.userId + '">' + HolaPms.escapeHtml(HolaPms.maskName(hk.userName)) + '</option>');
                    });
                }
            }
        });
    }
};

$(function () {
    HkHistory.init();
});
