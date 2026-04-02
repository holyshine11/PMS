/**
 * 객실 그룹 관리 - 리스트 페이지
 */
var RoomClassPage = {
    table: null,

    init: function() {
        this.initTable();
        this.bindEvents();
        this.reload();
    },

    initTable: function() {
        this.table = $('#roomClassTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            processing: true,
            serverSide: false,
            ajax: function(data, callback) {
                var propertyId = HolaPms.context.getPropertyId();
                if (!propertyId) {
                    $('#totalCount').text(0);
                    callback({ data: [] });
                    return;
                }
                $.ajax({
                    url: '/api/v1/properties/' + propertyId + '/room-classes',
                    success: function(res) {
                        if (res && res.data) {
                            $('#totalCount').text(res.data.length);
                            callback(res);
                        } else {
                            $('#totalCount').text(0);
                            callback({ data: [] });
                        }
                    },
                    error: function(xhr) {
                        HolaPms.handleAjaxError(xhr);
                        $('#totalCount').text(0);
                        callback({ data: [] });
                    }
                });
            },
            ordering: false,
            dom: 'rtip',
            columns: [
                {
                    data: null,
                    className: 'text-center',
                    render: function(data, type, row, meta) {
                        return meta.row + 1;
                    }
                },
                {
                    data: 'roomClassCode',
                    render: function(data, type, row) {
                        return '<a href="/admin/room-classes/' + row.id + '" class="text-primary text-decoration-none fw-bold">'
                            + HolaPms.escapeHtml(data) + '</a>';
                    }
                },
                {
                    data: 'roomClassName',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    data: 'useYn',
                    className: 'text-center',
                    render: HolaPms.renders.useYnBadge
                },
                {
                    data: 'updatedAt',
                    className: 'text-center',
                    render: function(data) {
                        return HolaPms.formatDateTime(data);
                    }
                }
            ]
        }));
    },

    bindEvents: function() {
        var self = this;

        // 엔터 키 검색
        $('#searchClassName, #searchClassCode').on('keypress', function(e) {
            if (e.which === 13) self.search();
        });

        // 사용여부 라디오 변경 시 검색
        $('input[name="searchUseYn"]').on('change', function() {
            self.search();
        });

        // 페이지 사이즈 변경
        $('#pageSizeSelect').on('change', function() {
            self.table.page.len(parseInt($(this).val())).draw();
        });

        // 호텔/프로퍼티 컨텍스트 변경
        $(document).on('hola:contextChange', function() {
            self.reload();
        });
    },

    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#btnCreate').addClass('disabled');
            if (this.table) {
                this.table.clear().draw();
                $('#totalCount').text(0);
            }
            return;
        }

        $('#contextAlert').addClass('d-none');
        $('#btnCreate').removeClass('disabled');

        this.table.ajax.reload();
    },

    search: function() {
        var className = $.trim($('#searchClassName').val());
        var classCode = $.trim($('#searchClassCode').val());
        var useYn = $('input[name="searchUseYn"]:checked').val();

        // 클래스명 + 코드 합쳐서 전체 검색
        var keyword = '';
        if (className) keyword += className;
        if (classCode) keyword += (keyword ? ' ' : '') + classCode;
        this.table.search(keyword);

        // 사용여부 컬럼 필터 (렌더링된 배지 텍스트 "사용"/"미사용" 기준)
        if (useYn === 'true') {
            this.table.column(3).search('^사용$', true, false);
        } else if (useYn === 'false') {
            this.table.column(3).search('미사용', true, false);
        } else {
            this.table.column(3).search('');
        }

        this.table.draw();
    },

    reset: function() {
        $('#searchClassName').val('');
        $('#searchClassCode').val('');
        $('#useYnAll').prop('checked', true);
        this.table.search('').columns().search('').draw();
    }
};

$(document).ready(function() {
    RoomClassPage.init();
});
