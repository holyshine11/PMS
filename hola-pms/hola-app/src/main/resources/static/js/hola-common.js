/**
 * Hola PMS 공통 JavaScript
 */
const HolaPms = {
    /**
     * HTML 이스케이프 (XSS 방지)
     */
    escapeHtml: function(str) {
        if (str == null) return '';
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(String(str)));
        return div.innerHTML;
    },

    /**
     * Ajax 요청 래퍼
     */
    ajax: function(options) {
        var defaults = {
            contentType: 'application/json',
            dataType: 'json',
            error: function(xhr) {
                var message = '서버 오류가 발생했습니다.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                HolaPms.alert('error', message);
            }
        };

        var settings = $.extend({}, defaults, options);
        var method = (settings.type || 'GET').toUpperCase();
        settings.type = method;
        if (settings.data && typeof settings.data === 'object' && method !== 'GET') {
            settings.data = JSON.stringify(settings.data);
        }
        return $.ajax(settings);
    },

    /**
     * 알림 메시지 (Bootstrap Toast) - XSS 방지 + 단일 컨테이너
     */
    alert: function(type, message) {
        var icons = {
            success: 'fa-check-circle text-success',
            error: 'fa-exclamation-circle text-danger',
            warning: 'fa-exclamation-triangle text-warning',
            info: 'fa-info-circle text-info'
        };

        // 단일 컨테이너 재사용
        var $container = $('#holaToastContainer');
        if ($container.length === 0) {
            $container = $('<div id="holaToastContainer" class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 9999;"></div>')
                .appendTo('body');
        }

        // 최대 3개까지만 표시
        var $existing = $container.find('.toast');
        if ($existing.length >= 3) {
            $existing.first().remove();
        }

        var safeMessage = HolaPms.escapeHtml(message);
        var $toast = $('<div class="toast show" role="alert">' +
            '<div class="toast-header">' +
            '<i class="fas ' + (icons[type] || icons.info) + ' me-2"></i>' +
            '<strong class="me-auto">알림</strong>' +
            '<button type="button" class="btn-close" data-bs-dismiss="toast"></button>' +
            '</div>' +
            '<div class="toast-body">' + safeMessage + '</div>' +
            '</div>').appendTo($container);

        setTimeout(function() { $toast.fadeOut(300, function() { $(this).remove(); }); }, 3000);
    },

    /**
     * 확인 다이얼로그
     */
    confirm: function(message, callback) {
        if (confirm(message)) {
            callback();
        }
    },

    /**
     * 모달 유틸리티 (인스턴스 재사용으로 메모리 누수 방지)
     */
    modal: {
        show: function(selector) {
            var el = typeof selector === 'string' ? document.querySelector(selector) : selector;
            var instance = bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el);
            instance.show();
            return instance;
        },
        hide: function(selector) {
            var el = typeof selector === 'string' ? document.querySelector(selector) : selector;
            var instance = bootstrap.Modal.getInstance(el);
            if (instance) instance.hide();
        }
    },

    /**
     * 폼 유틸리티
     */
    form: {
        /** 폼 값 가져오기 (빈 문자열 -> null) */
        val: function(selector) {
            var v = $(selector).val();
            return (v !== null && v !== undefined && v !== '') ? v : null;
        },
        /** 정수 변환 (빈값 -> null) */
        intVal: function(selector) {
            var v = $(selector).val();
            return v ? parseInt(v, 10) : null;
        }
    },

    /**
     * DataTable 공통 렌더러 (XSS 방지 적용)
     */
    renders: {
        /** null/빈값을 대시로 표시 */
        dashIfEmpty: function(data) {
            return data ? HolaPms.escapeHtml(data) : '-';
        },
        /** useYn 배지 */
        useYnBadge: function(data) {
            return data
                ? '<span class="badge bg-success">사용</span>'
                : '<span class="badge bg-secondary">미사용</span>';
        },
        /** 숫자 배지 */
        countBadge: function(bgClass) {
            bgClass = bgClass || 'bg-info';
            return function(data) {
                return '<span class="badge ' + bgClass + '">' + (data || 0) + '</span>';
            };
        },
        /** 수정/삭제 액션 버튼 */
        actionButtons: function(pageName) {
            return function(data, type, row) {
                var id = parseInt(row.id, 10);
                if (isNaN(id)) return '';
                return '<button class="btn btn-sm btn-outline-primary me-1" onclick="' + pageName + '.openEditModal(' + id + ')">' +
                       '<i class="fas fa-edit"></i></button>' +
                       '<button class="btn btn-sm btn-outline-danger" onclick="' + pageName + '.remove(' + id + ')">' +
                       '<i class="fas fa-trash"></i></button>';
            };
        }
    },

    /**
     * Debounce 유틸
     */
    debounce: function(func, wait) {
        var timeout;
        return function() {
            var context = this, args = arguments;
            clearTimeout(timeout);
            timeout = setTimeout(function() {
                func.apply(context, args);
            }, wait);
        };
    },

    /**
     * 호텔/프로퍼티 컨텍스트 (헤더 드롭다운 연동)
     */
    context: {
        selectedHotelId: null,
        selectedPropertyId: null,

        init: function() {
            // sessionStorage에서 복원
            this.selectedHotelId = sessionStorage.getItem('selectedHotelId') || null;
            this.selectedPropertyId = sessionStorage.getItem('selectedPropertyId') || null;
            this.loadHotels();
        },

        loadHotels: function() {
            var self = this;
            HolaPms.ajax({
                url: '/api/v1/hotels/selector',
                type: 'GET',
                success: function(res) {
                    var $select = $('#headerHotelSelect');
                    $select.find('option:not(:first)').remove();
                    var hotels = res.data || [];
                    hotels.forEach(function(h) {
                        $select.append('<option value="' + h.id + '">' + HolaPms.escapeHtml(h.hotelName) + '</option>');
                    });
                    if (self.selectedHotelId) {
                        $select.val(self.selectedHotelId);
                        self.loadProperties(self.selectedHotelId);
                    } else if (hotels.length === 1) {
                        // 호텔이 1개뿐이면 자동 선택
                        self.onHotelChange(hotels[0].id);
                        $select.val(hotels[0].id);
                    }
                }
            });
        },

        loadProperties: function(hotelId) {
            var self = this;
            var $select = $('#headerPropertySelect');
            $select.find('option:not(:first)').remove();
            if (!hotelId) return;

            HolaPms.ajax({
                url: '/api/v1/properties/selector?hotelId=' + hotelId,
                type: 'GET',
                success: function(res) {
                    var properties = res.data || [];
                    properties.forEach(function(p) {
                        $select.append('<option value="' + p.id + '">' + HolaPms.escapeHtml(p.propertyName) + '</option>');
                    });
                    if (self.selectedPropertyId) {
                        $select.val(self.selectedPropertyId);
                    }
                }
            });
        },

        onHotelChange: function(hotelId) {
            this.selectedHotelId = hotelId || null;
            this.selectedPropertyId = null;
            sessionStorage.setItem('selectedHotelId', hotelId || '');
            sessionStorage.removeItem('selectedPropertyId');
            $('#headerPropertySelect').val('');
            this.loadProperties(hotelId);
            // 컨텍스트 변경 이벤트 발행
            $(document).trigger('hola:contextChange', { hotelId: this.selectedHotelId, propertyId: null });
        },

        onPropertyChange: function(propertyId) {
            this.selectedPropertyId = propertyId || null;
            sessionStorage.setItem('selectedPropertyId', propertyId || '');
            // 컨텍스트 변경 이벤트 발행
            $(document).trigger('hola:contextChange', { hotelId: this.selectedHotelId, propertyId: this.selectedPropertyId });
        },

        getHotelId: function() { return this.selectedHotelId; },
        getPropertyId: function() { return this.selectedPropertyId; }
    },

    /**
     * DataTables 기본 설정
     */
    dataTableDefaults: {
        language: {
            lengthMenu: '_MENU_ 건씩 보기',
            zeroRecords: '데이터가 없습니다.',
            info: '_START_ ~ _END_ / 전체 _TOTAL_ 건',
            infoEmpty: '데이터가 없습니다.',
            infoFiltered: '(전체 _MAX_ 건 중 필터)',
            search: '검색:',
            paginate: {
                first: '처음',
                last: '마지막',
                next: '다음',
                previous: '이전'
            }
        },
        pageLength: 20,
        lengthMenu: [10, 20, 50, 100],
        responsive: true,
        order: [[0, 'desc']]
    }
};

// 헤더 호텔/프로퍼티 컨텍스트 초기화
$(document).ready(function() {
    if ($('#headerHotelSelect').length) {
        HolaPms.context.init();
    }
});
