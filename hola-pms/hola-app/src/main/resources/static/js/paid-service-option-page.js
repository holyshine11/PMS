/**
 * 유료 옵션 관리 - 리스트 페이지
 */
var PaidServiceOptionPage = {
    table: null,

    /** 서비스 유형 라벨 맵 */
    SERVICE_TYPE_LABELS: {
        'ROOM_UPGRADE': '객실 업그레이드',
        'BED_EXTRA': '추가 침대',
        'BREAKFAST_PAID': '유료 조식',
        'MEAL': '식사 패키지',
        'MINIBAR': '미니바',
        'ROOM_SERVICE': '룸서비스',
        'AMENITY_PREMIUM': '유료 어메니티',
        'SPA_WELLNESS': '스파/웰니스',
        'LAUNDRY': '세탁 서비스',
        'TRANSFER_PAID': '유료 교통',
        'PARKING_PAID': '유료 주차',
        'TOUR_ACTIVITY': '투어/액티비티',
        'CHILDCARE': '육아 서비스',
        'BUSINESS': '비즈니스 서비스',
        'DECORATION': '객실 데코레이션',
        'PHOTO': '촬영 서비스',
        'INTERNET_PAID': '유료 인터넷',
        'SAFE_LOCKER': '금고/보관',
        'MINI_KITCHEN': '주방 서비스',
        'GIFT_PACKAGE': '기프트 패키지'
    },

    init: function() {
        this.initTable();
        this.bindEvents();
        this.reload();
    },

    initTable: function() {
        var self = this;
        this.table = $('#paidServiceOptionTable').DataTable({
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
                    url: '/api/v1/properties/' + propertyId + '/paid-service-options',
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
            pageLength: 20,
            ordering: false,
            dom: 'rtip',
            language: HolaPms.dataTableLanguage,
            columns: [
                {
                    data: null,
                    className: 'text-center',
                    render: function(data, type, row, meta) {
                        return meta.row + 1;
                    }
                },
                {
                    data: 'serviceOptionCode',
                    render: function(data, type, row) {
                        return '<a href="/admin/paid-service-options/' + row.id + '" class="text-primary text-decoration-none fw-bold">'
                            + HolaPms.escapeHtml(data) + '</a>';
                    }
                },
                {
                    data: 'serviceNameKo',
                    render: function(data) {
                        return HolaPms.escapeHtml(data || '-');
                    }
                },
                {
                    data: 'serviceType',
                    render: function(data) {
                        return self.SERVICE_TYPE_LABELS[data] || data || '-';
                    }
                },
                {
                    data: 'currencyCode',
                    className: 'text-center',
                    render: function(data) {
                        return data || '-';
                    }
                },
                {
                    data: 'vatIncludedPrice',
                    className: 'text-end',
                    render: function(data, type, row) {
                        if (data == null) return '-';
                        var formatted = Number(data).toLocaleString('ko-KR');
                        return formatted;
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
                        return data ? data.replace('T', ' ').substring(0, 19) : '-';
                    }
                }
            ]
        });
    },

    bindEvents: function() {
        var self = this;

        // 엔터 키 검색
        $('#searchCode, #searchName').on('keypress', function(e) {
            if (e.which === 13) self.search();
        });

        // 사용여부 라디오 변경 시 검색
        $('input[name="searchUseYn"]').on('change', function() {
            self.search();
        });

        // 서비스 유형 변경 시 검색
        $('#searchServiceType').on('change', function() {
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
        var code = $.trim($('#searchCode').val());
        var name = $.trim($('#searchName').val());
        var serviceType = $('#searchServiceType').val();
        var useYn = $('input[name="searchUseYn"]:checked').val();

        // 코드명 + 서비스명 합쳐서 전체 검색
        var keyword = '';
        if (code) keyword += code;
        if (name) keyword += (keyword ? ' ' : '') + name;
        this.table.search(keyword);

        // 서비스 유형 필터 (렌더링된 한글 라벨 기준)
        if (serviceType) {
            var typeLabel = this.SERVICE_TYPE_LABELS[serviceType] || '';
            this.table.column(3).search(typeLabel ? ('^' + typeLabel + '$') : '', true, false);
        } else {
            this.table.column(3).search('');
        }

        // 사용여부 컬럼 필터
        if (useYn === 'true') {
            this.table.column(6).search('^사용$', true, false);
        } else if (useYn === 'false') {
            this.table.column(6).search('미사용', true, false);
        } else {
            this.table.column(6).search('');
        }

        this.table.draw();
    },

    reset: function() {
        $('#searchCode').val('');
        $('#searchName').val('');
        $('#searchServiceType').val('');
        $('#useYnAll').prop('checked', true);
        this.table.search('').columns().search('').draw();
    }
};

$(document).ready(function() {
    PaidServiceOptionPage.init();
});
