/**
 * 하우스키퍼 담당자 관리 - 리스트 페이지
 */
var HkStaffPage = {

    propertyId: null,
    dataTable: null,
    _loadSeq: 0,

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;
        $(document).on('hola:contextChange', function () { self.reload(); });
    },

    reload: function () {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#staffTableCard').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#staffTableCard').show();
        this.propertyId = propertyId;
        this.loadList();
    },

    loadList: function () {
        var self = this;
        var seq = ++self._loadSeq;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeepers',
            method: 'GET',
            success: function (res) {
                // 이전 요청 응답 무시 (최신 요청만 처리)
                if (seq !== self._loadSeq) return;
                if (res.success) {
                    self.renderTable(res.data || []);
                }
            }
        });
    },

    renderTable: function (data) {
        // 기존 DataTable 제거
        if (this.dataTable) {
            this.dataTable.destroy();
            this.dataTable = null;
        }
        // thead 보장을 위해 테이블 HTML 재구성
        $('#staffTable').html(
            '<thead class="table-light"><tr>' +
            '<th class="text-center">아이디</th>' +
            '<th class="text-center">담당자명</th>' +
            '<th class="text-center">역할</th>' +
            '<th class="text-center">구역</th>' +
            '<th class="text-center">부서</th>' +
            '<th class="text-center">연락처</th>' +
            '<th class="text-center">상태</th>' +
            '<th class="text-center">등록일</th>' +
            '</tr></thead>'
        );

        this.dataTable = $('#staffTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            data: data,
            columns: [
                {
                    data: 'loginId',
                    className: 'text-center',
                    render: function (val, type, row) {
                        return '<a href="/admin/housekeeping/staff/' + row.id + '/edit" class="text-decoration-none">'
                            + HolaPms.escapeHtml(val) + '</a>';
                    }
                },
                { data: 'userName', className: 'text-center', render: function(val) { return val ? HolaPms.escapeHtml(HolaPms.maskName(val)) : '-'; } },
                {
                    data: 'roleLabel',
                    className: 'text-center',
                    render: function (val, type, row) {
                        if (row.role === 'HOUSEKEEPING_SUPERVISOR') {
                            return '<span class="badge bg-primary">감독자</span>';
                        }
                        return '<span class="badge bg-secondary">청소 담당</span>';
                    }
                },
                { data: 'sectionName', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'department', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'phone', className: 'text-center', render: HolaPms.renders.dashIfEmpty },
                { data: 'useYn', className: 'text-center', render: HolaPms.renders.useYnBadge },
                {
                    data: 'createdAt',
                    className: 'text-center',
                    render: function (val) { return val ? val.substring(0, 10) : '-'; }
                }
            ],
            order: [[6, 'desc'], [2, 'asc']],
            pageLength: 20
        }));
    }
};

$(function () {
    HkStaffPage.init();
});
