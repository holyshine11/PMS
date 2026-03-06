/**
 * 프로퍼티 목록 페이지 JS
 */
const PropertyListPage = {
    table: null,

    init: function() {
        this.table = $('#propertyTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/hotels/0/properties',
                dataSrc: function(json) { return json.data || []; }
            },
            serverSide: false,
            dom: 'rtip',
            drawCallback: function() {
                var info = this.api().page.info();
                $('#totalCount').text(info.recordsTotal);
            },
            columns: [
                { data: null, render: function(data, type, row, meta) { return meta.row + 1; }, width: '50px' },
                { data: 'propertyCode', width: '130px' },
                { data: 'propertyName',
                  render: function(data, type, row) {
                    return '<a href="/admin/properties/' + parseInt(row.id, 10) + '/edit" class="text-decoration-none fw-bold">' + HolaPms.escapeHtml(data) + '</a>';
                  }
                },
                { data: 'hotelName', render: HolaPms.renders.dashIfEmpty },
                { data: 'useYn', render: HolaPms.renders.useYnBadge, width: '80px' },
                { data: 'updatedAt',
                  render: function(data) { return data ? data.substring(0, 10) : '-'; },
                  width: '110px'
                },
                { data: null,
                  orderable: false,
                  render: function(data, type, row) {
                    var id = parseInt(row.id, 10);
                    return '<button class="btn btn-outline-info btn-sm text-nowrap" onclick="PropertyListPage.copyRegister(' + id + ')">' +
                           '<i class="fas fa-copy me-1"></i>복사등록</button>';
                  },
                  width: '100px',
                  className: 'text-center'
                }
            ],
            order: [[1, 'asc']]
        }));

        $(document).on('hola:contextChange', function() { PropertyListPage.reload(); });

        $('#searchPropertyName').on('keyup', function(e) {
            if (e.key === 'Enter') PropertyListPage.search();
        });
        $('input[name="searchUseYn"]').on('change', function() {
            PropertyListPage.search();
        });

        // 페이지 사이즈 변경
        var self = this;
        $('#pageSizeSelect').on('change', function() {
            self.table.page.len(parseInt($(this).val())).draw();
        });

        this.reload();
    },

    reload: function() {
        var hotelId = HolaPms.context.getHotelId();
        if (!hotelId) {
            $('#contextAlert').show();
            this.table.clear().draw();
            HolaPms.requireContext('hotel');
            return;
        }
        $('#contextAlert').hide();
        this.table.ajax.url('/api/v1/hotels/' + hotelId + '/properties').load();
    },

    search: function() {
        // 클라이언트 사이드 검색
        var name = $('#searchPropertyName').val();
        var useYn = $('input[name="searchUseYn"]:checked').val();
        this.table.search(name).draw();
    },

    copyRegister: function(id) {
        location.href = '/admin/properties/new?copyFrom=' + id;
    },

    reset: function() {
        $('#searchPropertyName').val('');
        $('#useYnAll').prop('checked', true);
        this.table.search('').draw();
    }
};

$(document).ready(function() {
    if ($('#propertyTable').length) {
        PropertyListPage.init();
    }
});
