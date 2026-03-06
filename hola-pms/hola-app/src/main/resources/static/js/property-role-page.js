/**
 * 프로퍼티 관리자 권한 목록 페이지 JS
 */
const PropertyRolePage = {
    table: null,
    userRole: null,
    userHotelId: null,

    init: function() {
        // 현재 사용자 역할 확인
        this.userRole = HolaPms.context.userRole || null;
        this.userHotelId = HolaPms.context.getHotelId ? HolaPms.context.getHotelId() : null;

        // HOTEL_ADMIN: 호텔 필터를 자기 호텔로 고정
        if (this.userRole !== 'SUPER_ADMIN' && this.userHotelId) {
            var $hotelSelect = $('#searchHotelId');
            $hotelSelect.find('option:not(:first)').remove();
            var hotelName = HolaPms.context.getHotelName ? HolaPms.context.getHotelName() : '';
            $hotelSelect.append('<option value="' + this.userHotelId + '">' + HolaPms.escapeHtml(hotelName) + '</option>');
            $hotelSelect.val(this.userHotelId);
            $hotelSelect.prop('disabled', true);
        } else {
            this.loadHotels();
        }

        // 초기 URL에 호텔 필터 적용
        var initialUrl = '/api/v1/property-admin-roles';
        if (this.userRole !== 'SUPER_ADMIN' && this.userHotelId) {
            initialUrl += '?hotelId=' + this.userHotelId;
        }

        this.table = $('#roleTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: initialUrl,
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
                { data: 'roleName',
                  render: function(data, type, row) {
                    return '<a href="/admin/roles/property-admins/' + parseInt(row.id, 10) + '/edit" class="text-decoration-none fw-bold">' + HolaPms.escapeHtml(data) + '</a>';
                  }
                },
                { data: 'hotelName', render: HolaPms.renders.dashIfEmpty },
                { data: 'propertyName', render: HolaPms.renders.dashIfEmpty },
                { data: 'useYn', render: HolaPms.renders.useYnBadge, width: '80px' },
                { data: 'updatedAt', render: HolaPms.renders.dashIfEmpty, width: '140px' }
            ],
            order: [[0, 'asc']]
        }));

        // 엔터키 검색
        $('#searchRoleName').on('keyup', function(e) {
            if (e.key === 'Enter') PropertyRolePage.search();
        });
        // 사용여부 변경 시 자동 검색
        $('input[name="searchUseYn"]').on('change', function() {
            PropertyRolePage.search();
        });
        // 호텔 변경 시 자동 검색
        $('#searchHotelId').on('change', function() {
            PropertyRolePage.search();
        });

        // 페이지 사이즈 변경
        var self = this;
        $('#pageSizeSelect').on('change', function() {
            self.table.page.len(parseInt($(this).val())).draw();
        });
    },

    /** 호텔 목록 로드 (SUPER_ADMIN 전용) */
    loadHotels: function() {
        HolaPms.ajax({
            url: '/api/v1/hotels/selector',
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
        var url = '/api/v1/property-admin-roles';
        var params = this.buildSearchParams();
        if (params) url += '?' + params;
        this.table.ajax.url(url).load();
    },

    reset: function() {
        // HOTEL_ADMIN: 호텔 필터 유지
        if (this.userRole === 'SUPER_ADMIN') {
            $('#searchHotelId').val('');
        }
        $('#searchRoleName').val('');
        $('#useYnAll').prop('checked', true);
        this.search();
    },

    buildSearchParams: function() {
        var params = [];
        var hotelId = $('#searchHotelId').val();
        var roleName = $('#searchRoleName').val();
        var useYn = $('input[name="searchUseYn"]:checked').val();

        // HOTEL_ADMIN: 항상 자기 호텔 ID 포함
        if (this.userRole !== 'SUPER_ADMIN' && this.userHotelId) {
            params.push('hotelId=' + this.userHotelId);
        } else if (hotelId) {
            params.push('hotelId=' + encodeURIComponent(hotelId));
        }
        if (roleName) params.push('roleName=' + encodeURIComponent(roleName));
        if (useYn !== '' && useYn !== undefined) params.push('useYn=' + useYn);

        return params.join('&');
    }
};

$(document).ready(function() {
    if ($('#roleTable').length) {
        PropertyRolePage.init();
    }
});
