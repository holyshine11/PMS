var PromotionCodePage = {
    table: null,
    allData: [],
    // 프로모션 타입 라벨 매핑
    typeLabels: {
        'COMPANY': '기업 프로모션',
        'PROMOTION': '일반 프로모션',
        'OTA': 'OTA 프로모션',
        'PACKAGE': '패키지',
        'SEASONAL': '시즌',
        'EVENT': '이벤트',
        'EARLY_BIRD': '조기예약',
        'LAST_MINUTE': '직전예약',
        'MEMBER': '회원 전용',
        'GROUP': '단체',
        'LONG_STAY': '장기투숙',
        'GOVERNMENT': '관공서'
    },

    init: function() {
        this.initTable();
        this.bindEvents();
        this.reload();
    },

    initTable: function() {
        var self = this;
        this.table = $('#promotionCodeTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
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
                    url: '/api/v1/properties/' + propertyId + '/promotion-codes',
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
                    data: 'promotionCode',
                    render: function(data, type, row) {
                        return '<a href="/admin/promotion-codes/' + row.id + '" class="text-primary text-decoration-none fw-bold">'
                            + HolaPms.escapeHtml(data) + '</a>';
                    }
                },
                {
                    data: 'rateCode',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    data: 'promotionType',
                    className: 'text-center',
                    render: function(data) {
                        return HolaPms.escapeHtml(PromotionCodePage.typeLabels[data] || data || '-');
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
                        var start = row.promotionStartDate || '-';
                        var end = row.promotionEndDate || '-';
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
        $('#searchType').on('change', function() { self.search(); });
        $('input[name="searchUseYn"]').on('change', function() { self.search(); });
        $('#pageSizeSelect').on('change', function() {
            self.table.page.len(parseInt($(this).val())).draw();
        });
        $(document).on('hola:contextChange', function() { self.reload(); });
    },

    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#btnCreate').addClass('disabled');
            if (this.table) { this.table.clear().draw(); $('#totalCount').text(0); }
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#btnCreate').removeClass('disabled');
        this.table.ajax.reload();
    },

    search: function() {
        var code = $('#searchCode').val().trim().toLowerCase();
        var type = $('#searchType').val();
        var useYn = $('input[name="searchUseYn"]:checked').val();
        var self = this;

        $.fn.dataTable.ext.search.length = 0;
        $.fn.dataTable.ext.search.push(function(settings, searchData, dataIndex) {
            var row = self.allData[dataIndex];
            if (!row) return true;
            // 코드 필터
            if (code && row.promotionCode && row.promotionCode.toLowerCase().indexOf(code) === -1) return false;
            // 타입 필터
            if (type && row.promotionType !== type) return false;
            // 사용여부 필터
            if (useYn === 'true' && row.useYn !== true) return false;
            if (useYn === 'false' && row.useYn !== false) return false;
            // 기간 필터
            var searchStart = $('#searchStartDate').val();
            var searchEnd = $('#searchEndDate').val();
            if (searchStart && row.promotionEndDate && row.promotionEndDate < searchStart) return false;
            if (searchEnd && row.promotionStartDate && row.promotionStartDate > searchEnd) return false;
            return true;
        });
        this.table.draw();
        $('#totalCount').text(this.table.rows({ search: 'applied' }).count());
    },

    reset: function() {
        $('#searchCode').val('');
        $('#searchStartDate').val('').removeAttr('max');
        $('#searchEndDate').val('').removeAttr('min');
        $('#searchType').val('');
        $('#useYnAll').prop('checked', true);
        $.fn.dataTable.ext.search.length = 0;
        this.table.search('').columns().search('').draw();
        $('#totalCount').text(this.allData.length);
    }
};

$(document).ready(function() {
    PromotionCodePage.init();
});