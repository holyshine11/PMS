/**
 * 무료 옵션 관리 - 리스트 페이지
 */
var FreeServiceOptionPage = {
    table: null,

    /** 서비스 유형 라벨 맵 */
    SERVICE_TYPE_LABELS: {
        'BED': '베드',
        'VIEW': '뷰',
        'FLOOR': '층수/위치',
        'AMENITY': '어메니티',
        'BREAKFAST': '조식',
        'TRANSFER': '교통/셔틀',
        'PARKING': '주차',
        'INTERNET': '인터넷',
        'WELCOME': '웰컴 서비스',
        'EARLY_CHECKIN': '얼리 체크인',
        'LATE_CHECKOUT': '레이트 체크아웃',
        'LOUNGE': '라운지 이용',
        'POOL_FITNESS': '부대시설',
        'KIDS': '키즈 서비스',
        'PET': '반려동물',
        'SPECIAL_REQUEST': '특별 요청'
    },

    /** 적용 박수 라벨 맵 */
    APPLICABLE_NIGHTS_LABELS: {
        'FIRST_NIGHT_ONLY': '1박만',
        'ALL_NIGHTS': '모든 박수',
        'NOT_APPLICABLE': '해당 없음'
    },

    init: function() {
        this.initTable();
        this.bindEvents();
        this.reload();
    },

    initTable: function() {
        var self = this;
        this.table = $('#freeServiceOptionTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
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
                    url: '/api/v1/properties/' + propertyId + '/free-service-options',
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
                    data: 'serviceOptionCode',
                    render: function(data, type, row) {
                        return '<a href="/admin/free-service-options/' + row.id + '" class="text-primary text-decoration-none fw-bold">'
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
                    data: 'applicableNights',
                    render: function(data) {
                        return self.APPLICABLE_NIGHTS_LABELS[data] || data || '-';
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
            this.table.column(5).search('^사용$', true, false);
        } else if (useYn === 'false') {
            this.table.column(5).search('미사용', true, false);
        } else {
            this.table.column(5).search('');
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
    FreeServiceOptionPage.init();
});
