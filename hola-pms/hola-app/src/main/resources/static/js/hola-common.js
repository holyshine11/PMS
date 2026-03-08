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
     * Ajax 에러 공통 핸들러
     */
    handleAjaxError: function(xhr) {
        if (xhr.status === 401) {
            HolaPms.alert('warning', '세션이 만료되었습니다. 다시 로그인해 주세요.');
            window.location.href = '/login';
            return;
        }
        if (xhr.status === 403) {
            HolaPms.alert('error', '접근 권한이 없습니다.');
            return;
        }
        var message = '서버 오류가 발생했습니다.';
        if (xhr.responseJSON && xhr.responseJSON.message) {
            message = xhr.responseJSON.message;
        }
        HolaPms.alert('error', message);
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
            success: 'fa-check-circle text-primary',
            error: 'fa-exclamation-circle text-danger',
            warning: 'fa-exclamation-triangle text-secondary',
            info: 'fa-info-circle text-primary'
        };

        // 단일 컨테이너 재사용
        var $container = $('#holaToastContainer');
        if ($container.length === 0) {
            $container = $('<div id="holaToastContainer" class="toast-container position-fixed top-0 start-50 translate-middle-x p-3" style="z-index: 9999;"></div>')
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

        setTimeout(function() { $toast.fadeOut(300, function() { $(this).remove(); }); }, 1000);
    },

    /**
     * 알림 메시지 표시 후 페이지 이동 (sessionStorage에 저장하여 이동 후 표시)
     */
    alertAndRedirect: function(type, message, url) {
        try {
            sessionStorage.setItem('holaFlashAlert', JSON.stringify({ type: type, message: message }));
        } catch (e) { /* sessionStorage 비가용 시 alert 생략 */ }
        window.location.href = url;
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
                ? '<span class="badge bg-primary">사용</span>'
                : '<span class="badge bg-secondary">미사용</span>';
        },
        /** 숫자 배지 */
        countBadge: function(bgClass) {
            bgClass = bgClass || 'bg-primary';
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
     * 날짜 범위 바인딩 (시작일/종료일 상호 제한)
     * - 시작일 선택 시 → 종료일 min 설정
     * - 종료일 선택 시 → 시작일 max 설정
     * @param {string} startSel - 시작일 셀렉터 (예: '#saleStartDate')
     * @param {string} endSel   - 종료일 셀렉터 (예: '#saleEndDate')
     */
    bindDateRange: function(startSel, endSel) {
        var $start = $(startSel);
        var $end = $(endSel);

        $start.on('change', function() {
            var startVal = $(this).val();
            $end.attr('min', startVal || '');
            // 종료일이 시작일보다 이전이면 자동 보정
            if (startVal && $end.val() && $end.val() < startVal) {
                $end.val(startVal);
            }
        });

        $end.on('change', function() {
            var endVal = $(this).val();
            $start.attr('max', endVal || '');
            // 시작일이 종료일보다 이후면 자동 보정
            if (endVal && $start.val() && $start.val() > endVal) {
                $start.val(endVal);
            }
        });

        // 이미 값이 있으면 초기 제한 설정
        if ($start.val()) $end.attr('min', $start.val());
        if ($end.val()) $start.attr('max', $end.val());
    },

    /**
     * 호텔/프로퍼티 컨텍스트 (헤더 드롭다운 연동)
     */
    context: {
        selectedHotelId: null,
        selectedPropertyId: null,
        userRole: null,

        init: function() {
            // 사용자 역할 감지
            if ($('#roleSuperAdmin').length) this.userRole = 'SUPER_ADMIN';
            else if ($('#roleHotelAdmin').length) this.userRole = 'HOTEL_ADMIN';
            else if ($('#rolePropertyAdmin').length) this.userRole = 'PROPERTY_ADMIN';

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
                    // 저장된 호텔 복원 시도 → 실패하면 stale이므로 클리어
                    if (self.selectedHotelId) {
                        $select.val(self.selectedHotelId);
                        if ($select.val() !== self.selectedHotelId) {
                            // stale: 이 사용자가 접근할 수 없는 호텔
                            self.selectedHotelId = null;
                            self.selectedPropertyId = null;
                            sessionStorage.removeItem('selectedHotelId');
                            sessionStorage.removeItem('selectedPropertyId');
                        }
                    }

                    if (self.selectedHotelId) {
                        self.loadProperties(self.selectedHotelId);
                    } else if (hotels.length === 1) {
                        // 호텔이 1개뿐이면 자동 선택
                        self.onHotelChange(hotels[0].id);
                        $select.val(hotels[0].id);
                    }
                    // HOTEL_ADMIN, PROPERTY_ADMIN: 호텔 드롭다운 비활성화
                    if (self.userRole !== 'SUPER_ADMIN' && self.userRole) {
                        $select.prop('disabled', true);
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

                    // 저장된 프로퍼티 복원 시도 → 실패하면 stale이므로 클리어
                    if (self.selectedPropertyId) {
                        $select.val(self.selectedPropertyId);
                        if ($select.val() !== self.selectedPropertyId) {
                            // stale: 이 사용자가 접근할 수 없는 프로퍼티
                            self.selectedPropertyId = null;
                            sessionStorage.removeItem('selectedPropertyId');
                        }
                    }

                    // 프로퍼티 미선택 + 1개뿐이면 자동 선택 (모든 역할 공통)
                    if (!self.selectedPropertyId && properties.length === 1) {
                        self.selectedPropertyId = String(properties[0].id);
                        sessionStorage.setItem('selectedPropertyId', self.selectedPropertyId);
                        $select.val(self.selectedPropertyId);
                    }

                    // PROPERTY_ADMIN: 프로퍼티 드롭다운 비활성화
                    if (self.userRole === 'PROPERTY_ADMIN') {
                        $select.prop('disabled', true);
                    }
                    // 초기 로드 완료 후 컨텍스트 변경 이벤트 발행
                    $(document).trigger('hola:contextChange', {
                        hotelId: self.selectedHotelId,
                        propertyId: self.selectedPropertyId
                    });
                }
            });
        },

        onHotelChange: function(hotelId) {
            this.selectedHotelId = hotelId || null;
            this.selectedPropertyId = null;
            sessionStorage.setItem('selectedHotelId', hotelId || '');
            sessionStorage.removeItem('selectedPropertyId');
            $('#headerPropertySelect').val('');
            // loadProperties 완료 시 contextChange 이벤트 발행 (중복 발행 방지)
            this.loadProperties(hotelId);
        },

        onPropertyChange: function(propertyId) {
            this.selectedPropertyId = propertyId || null;
            sessionStorage.setItem('selectedPropertyId', propertyId || '');
            // 컨텍스트 변경 이벤트 발행
            $(document).trigger('hola:contextChange', { hotelId: this.selectedHotelId, propertyId: this.selectedPropertyId });
        },

        getHotelId: function() { return this.selectedHotelId; },
        getPropertyId: function() { return this.selectedPropertyId; },
        getHotelName: function() {
            var $select = $('#headerHotelSelect');
            return ($select.length && $select.val()) ? $select.find('option:selected').text() : '';
        },
        getPropertyName: function() {
            var $select = $('#headerPropertySelect');
            return ($select.length && $select.val()) ? $select.find('option:selected').text() : '';
        }
    },

    /**
     * 호텔/프로퍼티 컨텍스트 필수 체크 (미선택 시 toast 안내)
     * @param {string} type - 'hotel' 또는 'property'
     * @returns {boolean} 유효하면 true
     */
    requireContext: function(type) {
        var hotelId = this.context.getHotelId();
        var propertyId = this.context.getPropertyId();

        if ((type === 'hotel' || type === 'property') && !hotelId) {
            this._showContextAlert('warning', '상단 헤더에서 호텔을 선택해 주세요.');
            return false;
        }
        if (type === 'property' && !propertyId) {
            this._showContextAlert('warning', '상단 헤더에서 프로퍼티를 선택해 주세요.');
            return false;
        }
        return true;
    },

    /** 컨텍스트 알림 중복 방지 (3초 내 동일 메시지 무시, 메시지별 관리) */
    _contextAlertTimers: {},
    _showContextAlert: function(type, message) {
        if (this._contextAlertTimers[message]) return;
        HolaPms.alert(type, message);
        var self = this;
        this._contextAlertTimers[message] = setTimeout(function() {
            delete self._contextAlertTimers[message];
        }, 3000);
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

// dataTableLanguage alias (개별 JS에서 참조용)
HolaPms.dataTableLanguage = HolaPms.dataTableDefaults.language;

// DataTable 에러 글로벌 핸들링 (401/403은 사용자에게 안내, 나머지는 콘솔)
$.fn.dataTable.ext.errMode = function(settings, techNote, message) {
    if (settings && settings.jqXHR) {
        var status = settings.jqXHR.status;
        if (status === 401) {
            HolaPms.alert('warning', '세션이 만료되었습니다. 다시 로그인해 주세요.');
            window.location.href = '/login';
            return;
        }
        if (status === 403) {
            HolaPms.alert('error', '접근 권한이 없습니다.');
            return;
        }
    }
    console.warn('DataTable:', message);
};

// 헤더 호텔/프로퍼티 컨텍스트 초기화 + flash alert 표시
$(document).ready(function() {
    if ($('#headerHotelSelect').length) {
        HolaPms.context.init();
    }

    // sessionStorage에 저장된 flash alert 표시
    var flash = sessionStorage.getItem('holaFlashAlert');
    if (flash) {
        sessionStorage.removeItem('holaFlashAlert');
        try {
            var data = JSON.parse(flash);
            HolaPms.alert(data.type, data.message);
        } catch (e) { /* 무시 */ }
    }
});
