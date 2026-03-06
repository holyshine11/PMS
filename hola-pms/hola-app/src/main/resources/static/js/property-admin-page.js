/**
 * 프로퍼티 관리자 목록 페이지 JS
 */
const PropertyAdminPage = {
    table: null,

    init: function() {
        this.table = $('#propertyAdminTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/properties/0/admins',
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
                    return '<a href="/admin/members/property-admins/' + parseInt(row.id, 10) + '/edit" class="text-decoration-none fw-bold">' + HolaPms.escapeHtml(data) + '</a>';
                  }
                },
                { data: 'userName' },
                { data: 'useYn', render: HolaPms.renders.useYnBadge, width: '80px' },
                { data: 'createdAt', render: HolaPms.renders.dashIfEmpty, width: '110px' }
            ],
            order: [[0, 'asc']]
        }));

        // 컨텍스트(호텔+프로퍼티) 변경 이벤트
        $(document).on('hola:contextChange', function() { PropertyAdminPage.reload(); });

        // 엔터키 검색
        $('#searchLoginId, #searchUserName').on('keyup', function(e) {
            if (e.key === 'Enter') PropertyAdminPage.search();
        });
        $('input[name="searchUseYn"]').on('change', function() {
            PropertyAdminPage.search();
        });

        // 페이지 사이즈 변경
        var self = this;
        $('#pageSizeSelect').on('change', function() {
            self.table.page.len(parseInt($(this).val())).draw();
        });

        this.reload();
    },

    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').show();
            this.table.clear().draw();
            HolaPms.requireContext('property');
            return;
        }
        $('#contextAlert').hide();

        // 호텔명/프로퍼티명 표시
        var hotelName = HolaPms.context.getHotelName ? HolaPms.context.getHotelName() : '';
        var propertyName = HolaPms.context.getPropertyName ? HolaPms.context.getPropertyName() : '';
        $('#searchHotelName').val(hotelName);
        $('#searchPropertyName').val(propertyName);

        var url = '/api/v1/properties/' + propertyId + '/admins';
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
    if ($('#propertyAdminTable').length) {
        PropertyAdminPage.init();
    }
});
