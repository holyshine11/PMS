/**
 * 블루웨이브 관리자 목록 페이지 JS
 */
const BluewaveAdminPage = {
    table: null,

    init: function() {
        this.table = $('#bluewaveAdminTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/bluewave-admins',
                dataSrc: function(json) { return json.data || []; }
            },
            serverSide: false,
            pageLength: 20,
            dom: 'rtip',
            drawCallback: function() {
                var info = this.api().page.info();
                $('#totalCount').text(info.recordsTotal);
            },
            columns: [
                { data: null, render: function(data, type, row, meta) { return meta.row + 1; }, width: '50px' },
                { data: 'loginId',
                  render: function(data, type, row) {
                    return '<a href="/admin/members/bluewave-admins/' + parseInt(row.id, 10) + '/edit" class="text-decoration-none fw-bold">' + HolaPms.escapeHtml(data) + '</a>';
                  }
                },
                { data: 'userName' },
                { data: 'useYn', render: HolaPms.renders.useYnBadge, width: '80px' },
                { data: 'createdAt', render: HolaPms.renders.dashIfEmpty, width: '110px' }
            ],
            order: [[0, 'asc']]
        }));

        // 엔터키 검색
        $('#searchLoginId, #searchUserName').on('keyup', function(e) {
            if (e.key === 'Enter') BluewaveAdminPage.search();
        });
        $('input[name="searchUseYn"]').on('change', function() {
            BluewaveAdminPage.search();
        });

        // 페이지 사이즈 변경
        var self = this;
        $('#pageSizeSelect').on('change', function() {
            self.table.page.len(parseInt($(this).val())).draw();
        });
        // DataTable ajax 초기화 시 자동 로드됨 — reload() 중복 호출 제거
    },

    reload: function() {
        var url = '/api/v1/bluewave-admins';
        var params = this.buildSearchParams();
        if (params) url += '?' + params;

        this.table.ajax.url(url).load();
    },

    search: function() {
        this.reload();
    },

    reset: function() {
        $('#searchLoginId').val('');
        $('#searchUserName').val('');
        $('#useYnAll').prop('checked', true);
        this.reload();
    },

    buildSearchParams: function() {
        var params = [];
        var loginId = $('#searchLoginId').val();
        var userName = $('#searchUserName').val();
        var useYn = $('input[name="searchUseYn"]:checked').val();

        if (loginId) params.push('loginId=' + encodeURIComponent(loginId));
        if (userName) params.push('userName=' + encodeURIComponent(userName));
        if (useYn !== '' && useYn !== undefined) params.push('useYn=' + useYn);

        return params.join('&');
    }
};

$(document).ready(function() {
    if ($('#bluewaveAdminTable').length) {
        BluewaveAdminPage.init();
    }
});
