/**
 * 호텔 목록 페이지 JS
 */
const HotelPage = {
    table: null,

    init: function() {
        this.table = $('#hotelTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/hotels',
                dataSrc: function(json) { return json.data || []; }
            },
            serverSide: false,
            columns: [
                { data: null, render: function(data, type, row, meta) { return meta.row + 1; }, width: '60px' },
                { data: 'hotelCode', width: '120px' },
                { data: 'hotelName',
                  render: function(data, type, row) {
                    return '<a href="/admin/hotels/' + parseInt(row.id, 10) + '/edit" class="text-decoration-none fw-bold">' + HolaPms.escapeHtml(data) + '</a>';
                  }
                },
                { data: 'useYn', render: HolaPms.renders.useYnBadge, width: '100px' },
                { data: 'updatedAt',
                  render: function(data) {
                    if (!data) return '-';
                    return data.substring(0, 10);
                  },
                  width: '120px'
                }
            ],
            order: [[1, 'asc']]
        }));

        // 검색 엔터키
        var self = this;
        $('#searchHotelName').on('keyup', function(e) {
            if (e.key === 'Enter') {
                self.search();
            }
        });

        // 사용여부 라디오 변경 시 자동 검색
        $('input[name="searchUseYn"]').on('change', function() {
            self.search();
        });
    },

    search: function() {
        var hotelName = $('#searchHotelName').val() || '';
        var useYn = $('input[name="searchUseYn"]:checked').val();

        var params = [];
        if (hotelName) params.push('hotelName=' + encodeURIComponent(hotelName));
        if (useYn !== '') params.push('useYn=' + useYn);

        var url = '/api/v1/hotels' + (params.length > 0 ? '?' + params.join('&') : '');
        this.table.ajax.url(url).load();
    },

    reset: function() {
        $('#searchHotelName').val('');
        $('#useYnAll').prop('checked', true);
        this.table.ajax.url('/api/v1/hotels').load();
    }
};

$(document).ready(function() {
    if ($('#hotelTable').length) {
        HotelPage.init();
    }
});
