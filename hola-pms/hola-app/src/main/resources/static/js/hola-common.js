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
     * 이름 마스킹 (리스트 전용) — 서버 NameMaskingUtil.maskKoreanName()과 동일 로직
     * 1자: 그대로, 2자: 김*, 3자: 김*수, 4자+: 김**수
     */
    maskName: function(name) {
        if (!name || !name.trim()) return name;
        var s = name.trim();
        var len = s.length;
        if (len === 1) return s;
        if (len === 2) return s.charAt(0) + '*';
        if (len === 3) return s.charAt(0) + '*' + s.charAt(len - 1);
        return s.charAt(0) + '**' + s.charAt(len - 1);
    },

    /**
     * ISO 날짜/시간 문자열을 'YYYY-MM-DD HH:mm:ss' 형식으로 변환
     * @param {string} isoString - ISO 형식 문자열 (예: '2024-01-01T12:30:45')
     * @returns {string} 포맷된 문자열 또는 빈값 시 '-'
     */
    formatDateTime: function(isoString) {
        if (!isoString) return '-';
        return String(isoString).replace('T', ' ').substring(0, 19);
    },

    /**
     * 숫자를 한국식 천단위 콤마 형식으로 변환
     * @param {number|string} amount - 금액
     * @returns {string} 포맷된 문자열 (null/undefined/0 → '0')
     */
    formatCurrency: function(amount) {
        if (amount == null || amount === '') return '0';
        var n = Number(amount);
        if (isNaN(n)) return '0';
        return n.toLocaleString('ko-KR');
    },

    maskPhone: function(phone) {
        if (!phone) return phone;
        var s = phone.replace(/[^0-9-]/g, '');
        // 010-1234-5678 → 010-****-5678
        if (s.indexOf('-') >= 0) {
            var parts = s.split('-');
            if (parts.length === 3) return parts[0] + '-****-' + parts[2];
            if (parts.length === 2) return parts[0] + '-****';
        }
        // 01012345678 → 010****5678
        if (s.length >= 8) return s.substring(0, 3) + '****' + s.substring(s.length - 4);
        return s;
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
        // CSRF 토큰 자동 첨부 (GET 이외의 요청)
        if (method !== 'GET') {
            var csrfToken = $('meta[name="_csrf"]').attr('content');
            var csrfHeader = $('meta[name="_csrf_header"]').attr('content');
            if (csrfToken && csrfHeader) {
                settings.headers = settings.headers || {};
                settings.headers[csrfHeader] = csrfToken;
            }
        }
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

        var duration = (type === 'error' || type === 'danger') ? 3000 : 1500;
        setTimeout(function() { $toast.fadeOut(300, function() { $(this).remove(); }); }, duration);
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
            // 모달 내부 focus 해제 → aria-hidden 경고 방지
            if (el && el.contains(document.activeElement)) {
                document.activeElement.blur();
            }
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

            // URL 파라미터 우선 (새 창에서 프로퍼티 컨텍스트 전달용)
            var urlParams = new URLSearchParams(window.location.search);
            var urlHotelId = urlParams.get('hotelId');
            var urlPropertyId = urlParams.get('propertyId');

            if (urlHotelId && urlPropertyId) {
                this.selectedHotelId = urlHotelId;
                this.selectedPropertyId = urlPropertyId;
                sessionStorage.setItem('selectedHotelId', urlHotelId);
                sessionStorage.setItem('selectedPropertyId', urlPropertyId);
            } else {
                // sessionStorage에서 복원
                this.selectedHotelId = sessionStorage.getItem('selectedHotelId') || null;
                this.selectedPropertyId = sessionStorage.getItem('selectedPropertyId') || null;
            }
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
            this.selectedHotelId = hotelId ? String(hotelId) : null;
            this.selectedPropertyId = null;
            sessionStorage.setItem('selectedHotelId', hotelId || '');
            sessionStorage.removeItem('selectedPropertyId');
            $('#headerPropertySelect').val('');
            // loadProperties 완료 시 contextChange 이벤트 발행 (중복 발행 방지)
            this.loadProperties(hotelId);
        },

        onPropertyChange: function(propertyId) {
            this.selectedPropertyId = propertyId ? String(propertyId) : null;
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
     * 예약 상태 매핑 (공통 정의 - 모든 예약 관련 JS에서 참조)
     */
    reservationStatus: {
        RESERVED:    { label: '예약',     cls: 'bg-primary',          bg: '#0582CA', color: '#fff', softBg: 'rgba(5,130,202,0.1)',    softColor: '#0572b0' },
        CHECK_IN:    { label: '체크인',   cls: 'bg-info',             bg: '#17a2b8', color: '#fff', softBg: 'rgba(23,162,184,0.12)',  softColor: '#0c5460' },
        INHOUSE:     { label: '투숙중',   cls: 'bg-success',          bg: '#003554', color: '#fff', softBg: 'rgba(0,53,84,0.12)',     softColor: '#003554' },
        CHECKED_OUT: { label: '체크아웃', cls: 'bg-secondary',        bg: '#6c757d', color: '#fff', softBg: 'rgba(108,117,125,0.08)', softColor: '#6c757d' },
        CANCELED:    { label: '취소',     cls: 'bg-danger',           bg: '#EF476F', color: '#fff', softBg: 'rgba(239,71,111,0.08)',  softColor: '#d03058' },
        NO_SHOW:     { label: '노쇼',     cls: 'bg-warning text-dark', bg: '#ffc107', color: '#000', softBg: 'rgba(255,193,7,0.15)',  softColor: '#856404' },

        /** Soft 배지 HTML 반환 (테이블/리스트용) */
        badge: function(status) {
            var info = this[status] || { label: status || '-', softBg: 'rgba(108,117,125,0.08)', softColor: '#6c757d' };
            return '<span class="badge" style="background-color:' + info.softBg + '; color:' + info.softColor + '">' + info.label + '</span>';
        },
        /** Solid 배지 HTML 반환 (캘린더/일별 뷰용) */
        styledBadge: function(status) {
            var info = this[status] || { label: status || '-', bg: '#6c757d', color: '#fff' };
            return '<span class="badge" style="background-color:' + info.bg + '; color:' + (info.color || '#fff') + '">' + info.label + '</span>';
        },
        /** 상태 정보 객체 반환 */
        get: function(status) {
            return this[status] || { label: status || '-', cls: 'bg-secondary', bg: '#6c757d', color: '#fff', softBg: 'rgba(108,117,125,0.08)', softColor: '#6c757d' };
        },
        /** 캘린더/타임라인용 상태 색상 (연한 배경 + 진한 텍스트 + 테두리) */
        _viewColors: {
            'RESERVED':    { bg: '#dbeafe', text: '#1e40af', border: '#93c5fd' },
            'CHECK_IN':    { bg: '#d1fae5', text: '#065f46', border: '#6ee7b7' },
            'INHOUSE':     { bg: '#003554', text: '#ffffff', border: '#002940' },
            'CHECKED_OUT': { bg: '#f3f4f6', text: '#6b7280', border: '#d1d5db' },
            'CANCELED':    { bg: '#fef2f2', text: '#991b1b', border: '#fecaca' },
            'NO_SHOW':     { bg: '#fefce8', text: '#854d0e', border: '#fde68a' },
            'DAY_USE':     { bg: '#f5f3ff', text: '#6d28d9', border: '#c4b5fd' },
            'DAY_OFF':     { bg: '#ecfdf5', text: '#047857', border: '#6ee7b7' }
        },
        viewColor: function(status) {
            return this._viewColors[status] || { bg: '#f3f4f6', text: '#6b7280', border: '#d1d5db' };
        }
    },

    /**
     * 사이드바 접힘/펼침
     */
    sidebar: {
        toggle: function() {
            document.body.classList.toggle('sidebar-collapsed');
            try {
                localStorage.setItem('holaSidebarCollapsed',
                    document.body.classList.contains('sidebar-collapsed'));
            } catch (e) { /* localStorage 비가용 시 무시 */ }
        },
        init: function() {
            try {
                if (localStorage.getItem('holaSidebarCollapsed') === 'true') {
                    document.body.classList.add('sidebar-collapsed');
                }
            } catch (e) { /* 무시 */ }
            var toggleBtn = document.getElementById('sidebarToggle');
            if (toggleBtn) {
                toggleBtn.addEventListener('click', HolaPms.sidebar.toggle);
            }
        }
    },

    /**
     * 팝업 테이블 행 클릭 → 라디오/체크박스 선택 (공통)
     * 모달 내 table-hover 테이블의 tbody tr 클릭 시 해당 행의 radio/checkbox 선택
     */
    initModalRowClick: function() {
        $(document).on('click', '.modal .table-hover tbody tr', function(e) {
            // radio/checkbox 직접 클릭한 경우 무시 (이중 토글 방지)
            if ($(e.target).is('input[type="radio"], input[type="checkbox"]')) return;
            // disabled 행 무시
            if ($(this).hasClass('table-secondary')) return;

            var $radio = $(this).find('input[type="radio"]');
            var $checkbox = $(this).find('input[type="checkbox"]');

            if ($radio.length && !$radio.prop('disabled')) {
                $radio.prop('checked', true).trigger('change');
                // 같은 테이블 내 모든 행에서 선택 스타일 제거 후 현재 행에 적용
                $(this).closest('tbody').find('tr').removeClass('row-selected');
                $(this).addClass('row-selected');
            } else if ($checkbox.length && !$checkbox.prop('disabled')) {
                $checkbox.prop('checked', !$checkbox.prop('checked')).trigger('change');
                $(this).toggleClass('row-selected', $checkbox.prop('checked'));
            }
        });

        // radio change 이벤트로 행 하이라이트 동기화
        $(document).on('change', '.modal .table-hover input[type="radio"]', function() {
            var $tbody = $(this).closest('tbody');
            $tbody.find('tr').removeClass('row-selected');
            $(this).closest('tr').addClass('row-selected');
        });
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

// DataTable 에러 글로벌 핸들링 (모바일 등 DataTables 미로드 환경 대비 가드)
if ($.fn.dataTable) $.fn.dataTable.ext.errMode = function(settings, techNote, message) {
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

// ── 팝업 유틸리티 ──
HolaPms.popup = {
    isPopup: function() {
        return new URLSearchParams(window.location.search).get('mode') === 'popup';
    },
    openReservationDetail: function(reservationId, opts) {
        opts = opts || {};
        var hotelId = HolaPms.context.getHotelId() || '';
        var propertyId = HolaPms.context.getPropertyId() || '';
        var url = '/admin/reservations/' + reservationId
            + '?mode=popup&hotelId=' + hotelId + '&propertyId=' + propertyId;
        if (opts.tab) url += '&tab=' + opts.tab;
        var windowName = 'holaReservation_' + reservationId;
        var features = 'width=1200,height=800,scrollbars=yes,resizable=yes';
        var left = (screen.width - 1200) / 2;
        var top = (screen.height - 800) / 2;
        features += ',left=' + left + ',top=' + top;
        var win = window.open(url, windowName, features);
        if (!win) {
            HolaPms.alert('warning', '팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요.');
        } else {
            win.focus();
        }
        return win;
    },
    notifyParent: function(action, reservationId) {
        if (window.opener && !window.opener.closed) {
            window.opener.postMessage(
                { type: 'holaReservation', action: action, reservationId: Number(reservationId) },
                window.location.origin
            );
        }
    },
    onChildMessage: function(callback) {
        window.addEventListener('message', function(event) {
            if (event.origin !== window.location.origin) return;
            if (event.data && event.data.type === 'holaReservation') {
                callback(event.data);
            }
        });
    }
};

// 헤더 호텔/프로퍼티 컨텍스트 초기화 + flash alert 표시
$(document).ready(function() {
    // 사이드바 접힘/펼침 초기화
    HolaPms.sidebar.init();

    // 팝업 행 클릭 선택 초기화
    HolaPms.initModalRowClick();

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
