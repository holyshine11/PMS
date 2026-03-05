/**
 * 호텔 관리자 권한 목록 페이지 JS
 */
const HotelRolePage = {
    table: null,

    init: function() {
        this.loadHotels();

        this.table = $('#roleTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/hotel-admin-roles',
                dataSrc: function(json) { return json.data || []; }
            },
            serverSide: false,
            pageLength: 20,
            columns: [
                { data: null, render: function(data, type, row, meta) { return meta.row + 1; }, width: '50px' },
                { data: 'roleName',
                  render: function(data, type, row) {
                    return '<a href="/admin/roles/hotel-admins/' + parseInt(row.id, 10) + '/edit" class="text-decoration-none fw-bold">' + HolaPms.escapeHtml(data) + '</a>';
                  }
                },
                { data: 'hotelName', render: HolaPms.renders.dashIfEmpty },
                { data: 'useYn', render: HolaPms.renders.useYnBadge, width: '80px' },
                { data: 'updatedAt', render: HolaPms.renders.dashIfEmpty, width: '140px' }
            ],
            order: [[0, 'asc']]
        }));

        // 엔터키 검색
        $('#searchRoleName').on('keyup', function(e) {
            if (e.key === 'Enter') HotelRolePage.search();
        });
        // 사용여부 변경 시 자동 검색
        $('input[name="searchUseYn"]').on('change', function() {
            HotelRolePage.search();
        });
        // 호텔 변경 시 자동 검색
        $('#searchHotelId').on('change', function() {
            HotelRolePage.search();
        });
    },

    /** 호텔 목록 로드 (검색용 드롭다운) */
    loadHotels: function() {
        HolaPms.ajax({
            url: '/api/v1/hotels',
            type: 'GET',
            success: function(res) {
                var hotels = res.data || [];
                var $select = $('#searchHotelId');
                $select.find('option:not(:first)').remove();
                hotels.forEach(function(h) {
                    $select.append('<option value="' + h.id + '">' + HolaPms.escapeHtml(h.hotelName) + '</option>');
                });
            }
        });
    },

    search: function() {
        var url = '/api/v1/hotel-admin-roles';
        var params = this.buildSearchParams();
        if (params) url += '?' + params;
        this.table.ajax.url(url).load();
    },

    reset: function() {
        $('#searchHotelId').val('');
        $('#searchRoleName').val('');
        $('#useYnAll').prop('checked', true);
        this.search();
    },

    buildSearchParams: function() {
        var params = [];
        var hotelId = $('#searchHotelId').val();
        var roleName = $('#searchRoleName').val();
        var useYn = $('input[name="searchUseYn"]:checked').val();

        if (hotelId) params.push('hotelId=' + encodeURIComponent(hotelId));
        if (roleName) params.push('roleName=' + encodeURIComponent(roleName));
        if (useYn !== '' && useYn !== undefined) params.push('useYn=' + useYn);

        return params.join('&');
    }
};

$(document).ready(function() {
    if ($('#roleTable').length) {
        HotelRolePage.init();
    }
});
