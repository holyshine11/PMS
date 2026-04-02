/**
 * 레이트 관리 - 리스트 페이지
 */
var RateCodePage = {
    table: null,
    allData: [],

    init: function() {
        this.initTable();
        this.bindEvents();
        this.reload();
    },

    initTable: function() {
        var self = this;
        this.table = $('#rateCodeTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            processing: true,
            serverSide: false,
            ajax: function(data, callback) {
                var propertyId = HolaPms.context.getPropertyId();
                if (!propertyId) {
                    self.allData = [];
                    $('#totalCount').text(0);
                    callback({ data: [] });
                    return;
                }
                $.ajax({
                    url: '/api/v1/properties/' + propertyId + '/rate-codes',
                    success: function(res) {
                        if (res && res.data) {
                            self.allData = res.data;
                            $('#totalCount').text(res.data.length);
                            callback(res);
                        } else {
                            self.allData = [];
                            $('#totalCount').text(0);
                            callback({ data: [] });
                        }
                    },
                    error: function(xhr) {
                        HolaPms.handleAjaxError(xhr);
                        self.allData = [];
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
                    data: 'rateCode',
                    render: function(data, type, row) {
                        return '<a href="/admin/rate-codes/' + row.id + '" class="text-primary text-decoration-none fw-bold">'
                            + HolaPms.escapeHtml(data) + '</a>';
                    }
                },
                {
                    data: 'rateNameKo',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    data: 'stayType',
                    className: 'text-center',
                    render: function(data) {
                        if (data === 'DAY_USE') return '<span class="badge" style="background-color:#0582CA;">Dayuse</span>';
                        return '<span class="badge bg-secondary">숙박</span>';
                    }
                },
                {
                    data: 'marketCodeName',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    data: 'roomTypeCount',
                    className: 'text-center',
                    render: function(data) {
                        return data || 0;
                    }
                },
                {
                    data: 'currency',
                    className: 'text-center',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    data: null,
                    className: 'text-center',
                    render: function() {
                        return '-';
                    }
                },
                {
                    data: 'useYn',
                    className: 'text-center',
                    render: HolaPms.renders.useYnBadge
                },
                {
                    data: null,
                    className: 'text-center',
                    render: function(data, type, row) {
                        var start = row.saleStartDate || '-';
                        var end = row.saleEndDate || '-';
                        return start + ' ~ ' + end;
                    }
                }
            ]
        }));
    },

    bindEvents: function() {
        var self = this;

        // 검색 날짜 범위 제한
        HolaPms.bindDateRange('#searchStartDate', '#searchEndDate');

        // 카테고리 라디오 변경 시 검색
        $('input[name="searchCategory"]').on('change', function() {
            self.search();
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
        var category = $('input[name="searchCategory"]:checked').val();
        var useYn = $('input[name="searchUseYn"]:checked').val();

        // 카테고리 컬럼 필터 (rateCategory 값은 렌더링되지 않으므로 커스텀 필터 사용)
        var self = this;

        // DataTables 커스텀 필터
        $.fn.dataTable.ext.search.length = 0; // 기존 필터 초기화
        $.fn.dataTable.ext.search.push(function(settings, searchData, dataIndex) {
            var row = self.allData[dataIndex];
            if (!row) return true;

            // 카테고리 필터
            if (category && row.rateCategory !== category) return false;

            // 사용여부 필터
            if (useYn === 'true' && row.useYn !== true) return false;
            if (useYn === 'false' && row.useYn !== false) return false;

            // 판매기간 필터
            var searchStart = $('#searchStartDate').val();
            var searchEnd = $('#searchEndDate').val();
            if (searchStart && row.saleEndDate && row.saleEndDate < searchStart) return false;
            if (searchEnd && row.saleStartDate && row.saleStartDate > searchEnd) return false;

            return true;
        });

        this.table.draw();

        // 필터된 결과 수 업데이트
        $('#totalCount').text(this.table.rows({ search: 'applied' }).count());
    },

    reset: function() {
        $('#searchStartDate').val('').removeAttr('max');
        $('#searchEndDate').val('').removeAttr('min');
        $('#categoryAll').prop('checked', true);
        $('#useYnAll').prop('checked', true);
        $.fn.dataTable.ext.search.length = 0;
        this.table.search('').columns().search('').draw();
        $('#totalCount').text(this.allData.length);
    }
};

$(document).ready(function() {
    RateCodePage.init();
});
