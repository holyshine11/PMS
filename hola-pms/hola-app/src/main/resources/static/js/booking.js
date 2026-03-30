/**
 * Hola Booking Engine — Premium Redesign
 * 인증 불필요, 공개 API (/api/v1/booking/*) 사용
 */
var HolaBooking = (function() {
    'use strict';

    var API_BASE = '/api/v1/booking';

    // ═══════════════════════════════════════
    // 공통 유틸
    // ═══════════════════════════════════════

    function formatDayUseLabel(hours) {
        return hours ? 'Dayuse ' + hours + '시간' : 'Dayuse';
    }

    function api(options) {
        var defaults = { contentType: 'application/json', dataType: 'json' };
        var settings = $.extend({}, defaults, options);
        return $.ajax(settings)
            .then(function(res) { return (res && res.result) ? res.result : res; })
            .fail(function(xhr) {
                var msg = '요청 처리 중 오류가 발생했습니다.';
                if (xhr.responseJSON) {
                    var r = xhr.responseJSON.result || xhr.responseJSON;
                    msg = r.RESULT_MESSAGE || r.message || msg;
                }
                showError(msg);
            });
    }

    function showError(message) {
        var $a = $('#errorAlert');
        if ($a.length) {
            $('#errorMessage').text(message);
            $a.removeClass('d-none');
            $('html, body').animate({ scrollTop: $a.offset().top - 100 }, 300);
        } else { alert(message); }
    }

    function hideError() { $('#errorAlert').addClass('d-none'); }

    function formatCurrency(amount, currency) {
        if (amount == null) return '-';
        var n = Number(amount);
        if (isNaN(n)) return '-';
        return (currency === 'USD' ? '$' : '₩') + n.toLocaleString('ko-KR');
    }

    function formatDate(s) { return s ? s.replace(/-/g, '.') : '-'; }

    function formatDateTime(s) {
        if (!s) return '-';
        // "2026-03-26T18:30:15" 또는 배열 [2026,3,26,18,30,15] 형태 처리
        var d;
        if (Array.isArray(s)) {
            d = new Date(s[0], s[1]-1, s[2], s[3]||0, s[4]||0, s[5]||0);
        } else {
            d = new Date(s);
        }
        if (isNaN(d.getTime())) return s;
        var pad = function(n) { return n < 10 ? '0'+n : n; };
        return d.getFullYear() + '.' + pad(d.getMonth()+1) + '.' + pad(d.getDate()) + ' '
             + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
    }

    function calcNights(ci, co) {
        var diff = Math.ceil((new Date(co) - new Date(ci)) / 86400000);
        return diff > 0 ? diff : 0;
    }

    function getQueryParam(name) { return new URLSearchParams(window.location.search).get(name); }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }

    function toDateStr(d) {
        return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
    }

    var WEEKDAYS_KO = ['일','월','화','수','목','금','토'];
    var WEEKDAYS_EN = ['SUN','MON','TUE','WED','THU','FRI','SAT'];

    function getDayOfWeekKo(dateStr) {
        return WEEKDAYS_KO[new Date(dateStr).getDay()];
    }

    function buildDailyPricesHtml(dailyPrices, currency) {
        if (!dailyPrices || dailyPrices.length === 0) return '';
        var html = '<table class="table table-sm table-borderless mb-0" style="font-size:0.75rem;">';
        html += '<thead><tr class="text-muted"><th>날짜</th><th class="text-end">객실료</th><th class="text-end">세금</th><th class="text-end">봉사료</th><th class="text-end">합계</th></tr></thead><tbody>';
        $.each(dailyPrices, function(i, dp) {
            html += '<tr><td>' + formatDate(dp.date) + '</td>'
                + '<td class="text-end">' + formatCurrency(dp.supplyPrice, currency) + '</td>'
                + '<td class="text-end">' + formatCurrency(dp.tax, currency) + '</td>'
                + '<td class="text-end">' + formatCurrency(dp.serviceCharge, currency) + '</td>'
                + '<td class="text-end" style="font-weight:600;">' + formatCurrency(dp.total, currency) + '</td></tr>';
        });
        html += '</tbody></table>';
        return html;
    }

    var STATUS_MAP = {
        'RESERVED':    { label: '예약확정', cls: 'bg-primary' },
        'CHECK_IN':    { label: '체크인',   cls: 'bg-success' },
        'INHOUSE':     { label: '투숙중',   cls: 'bg-info' },
        'CHECKED_OUT': { label: '체크아웃', cls: 'bg-secondary' },
        'CANCELED':    { label: '취소',     cls: 'bg-danger' },
        'NO_SHOW':     { label: '노쇼',     cls: 'bg-warning text-dark' }
    };

    var PAY_STATUS_MAP = {
        'PAID':     { label: '결제완료', cls: 'bg-success' },
        'UNPAID':   { label: '미결제',   cls: 'bg-warning text-dark' },
        'PARTIAL':  { label: '부분결제', cls: 'bg-info' },
        'OVERPAID': { label: '초과결제', cls: 'bg-danger' }
    };

    var METHOD_MAP = { 'CARD': '신용카드', 'CASH': '현장결제' };

    function statusBadge(status) {
        var s = STATUS_MAP[status] || { label: status, cls: 'bg-secondary' };
        return '<span class="badge ' + s.cls + '">' + s.label + '</span>';
    }

    function payStatusBadge(status) {
        var s = PAY_STATUS_MAP[status] || { label: status || '-', cls: 'bg-secondary' };
        return '<span class="badge ' + s.cls + '">' + s.label + '</span>';
    }

    // ═══════════════════════════════════════
    // 캘린더 컴포넌트 (jQuery, CDN 없음)
    // ═══════════════════════════════════════

    var Calendar = {
        container: null,
        baseMonth: null,     // Date: 좌측 달의 1일
        startDate: null,     // 'YYYY-MM-DD'
        endDate: null,       // 'YYYY-MM-DD'
        selecting: false,    // 첫 클릭 후 두 번째 대기 중
        onChange: null,      // callback(startDate, endDate)

        init: function(containerId, onChange) {
            this.container = $('#' + containerId);
            this.onChange = onChange;
            var today = new Date();
            this.baseMonth = new Date(today.getFullYear(), today.getMonth(), 1);

            // 기본 오늘~내일
            this.startDate = toDateStr(today);
            var tmr = new Date(today); tmr.setDate(tmr.getDate() + 1);
            this.endDate = toDateStr(tmr);

            this.render();
            this.bindEvents();
            if (this.onChange) this.onChange(this.startDate, this.endDate);
        },

        render: function() {
            var m1 = this.baseMonth;
            var m2 = new Date(m1.getFullYear(), m1.getMonth() + 1, 1);

            var html = '<div class="bk-calendar-header">'
                + '<button type="button" class="bk-calendar-nav" id="calPrev"><i class="fas fa-chevron-left"></i></button>'
                + '<div class="bk-calendar-months">'
                + '<span class="bk-calendar-month-title">' + this.monthTitle(m1) + '</span>'
                + '<span class="bk-calendar-month-title">' + this.monthTitle(m2) + '</span>'
                + '</div>'
                + '<button type="button" class="bk-calendar-nav" id="calNext"><i class="fas fa-chevron-right"></i></button>'
                + '</div>';

            html += '<div class="bk-calendar-body">';
            html += this.renderMonth(m1);
            html += this.renderMonth(m2);
            html += '</div>';

            html += this.renderSummary();
            this.container.html(html);

            // 이전 달 비활성화
            var today = new Date();
            var curMonth = new Date(today.getFullYear(), today.getMonth(), 1);
            $('#calPrev').prop('disabled', m1 <= curMonth);
        },

        monthTitle: function(d) {
            return d.getFullYear() + '.' + String(d.getMonth() + 1).padStart(2, '0');
        },

        renderMonth: function(monthDate) {
            var y = monthDate.getFullYear(), m = monthDate.getMonth();
            var firstDay = new Date(y, m, 1).getDay();
            var daysInMonth = new Date(y, m + 1, 0).getDate();
            var todayStr = toDateStr(new Date());
            var hasRange = this.startDate && this.endDate && this.startDate !== this.endDate;

            var html = '<div class="bk-calendar-grid">';
            // 요일 헤더 (영문)
            for (var w = 0; w < 7; w++) {
                html += '<div class="bk-calendar-weekday">' + WEEKDAYS_EN[w] + '</div>';
            }
            // 빈 칸
            for (var e = 0; e < firstDay; e++) {
                html += '<div class="bk-calendar-day empty"></div>';
            }
            // 날짜
            for (var d = 1; d <= daysInMonth; d++) {
                var dateStr = y + '-' + String(m+1).padStart(2,'0') + '-' + String(d).padStart(2,'0');
                var dow = new Date(y, m, d).getDay();
                var cellPos = (firstDay + d - 1) % 7; // 0=일 ~ 6=토
                var classes = ['bk-calendar-day'];

                if (dateStr < todayStr) classes.push('disabled');
                if (dateStr === todayStr) classes.push('today');
                if (dow === 0) classes.push('sunday');

                // 범위 표시
                var isStart = false, isEnd = false, isInRange = false;
                if (hasRange) {
                    if (dateStr === this.startDate) { classes.push('start', 'has-range'); isStart = true; }
                    if (dateStr === this.endDate) { classes.push('end', 'has-range'); isEnd = true; }
                    if (dateStr > this.startDate && dateStr < this.endDate) { classes.push('in-range'); isInRange = true; }
                } else if (this.startDate && dateStr === this.startDate) {
                    classes.push('start');
                }

                html += '<div class="' + classes.join(' ') + '" data-date="' + dateStr + '">' + d + '</div>';
            }
            html += '</div>';
            return html;
        },

        renderSummary: function() {
            if (!this.startDate) return '<div class="bk-calendar-summary"><span class="text-muted">날짜를 선택해주세요</span></div>';

            var ciText = formatDate(this.startDate) + ' (' + getDayOfWeekKo(this.startDate) + ')';
            var html = '<div class="bk-calendar-summary">';
            html += '<span>체크인 <span class="date-value">' + ciText + '</span></span>';

            if (this.endDate && this.endDate !== this.startDate) {
                var coText = formatDate(this.endDate) + ' (' + getDayOfWeekKo(this.endDate) + ')';
                var nights = calcNights(this.startDate, this.endDate);
                html += '<span>—</span>';
                html += '<span>체크아웃 <span class="date-value">' + coText + '</span></span>';
                html += '<span class="nights-badge">' + nights + '박 ' + (nights+1) + '일</span>';
            } else {
                html += '<span class="text-muted">체크아웃 날짜를 선택해주세요</span>';
            }
            html += '</div>';
            return html;
        },

        bindEvents: function() {
            var self = this;

            this.container.on('click', '#calPrev', function() {
                self.baseMonth = new Date(self.baseMonth.getFullYear(), self.baseMonth.getMonth() - 1, 1);
                self.render();
            });

            this.container.on('click', '#calNext', function() {
                self.baseMonth = new Date(self.baseMonth.getFullYear(), self.baseMonth.getMonth() + 1, 1);
                self.render();
            });

            this.container.on('click', '.bk-calendar-day:not(.disabled):not(.empty)', function() {
                var date = $(this).data('date');
                if (!date) return;

                if (!self.selecting) {
                    // 첫 클릭: 체크인
                    self.startDate = date;
                    self.endDate = null;
                    self.selecting = true;
                } else {
                    // 두 번째 클릭: 체크아웃
                    if (date <= self.startDate) {
                        // 체크인보다 이전/같은 날 → 새로운 체크인으로
                        self.startDate = date;
                        self.endDate = null;
                        return self.render();
                    }
                    var nights = calcNights(self.startDate, date);
                    if (nights > 30) {
                        showError('최대 30박까지 예약 가능합니다.');
                        return;
                    }
                    self.endDate = date;
                    self.selecting = false;
                }
                self.render();
                if (self.startDate && self.endDate && self.onChange) {
                    self.onChange(self.startDate, self.endDate);
                }
            });
        }
    };

    // ═══════════════════════════════════════
    // 검색 페이지 (search.html)
    // ═══════════════════════════════════════

    var SearchPage = {
        propertyCode: null,
        propertyInfo: null,
        adults: 2,
        children: 0,
        checkIn: null,
        checkOut: null,

        init: function() {
            this.propertyCode = $('#propertyCode').val();
            if (!this.propertyCode) return;

            this.loadPropertyInfo();
            this.initCalendar();
            this.bindEvents();
        },

        initCalendar: function() {
            var self = this;
            Calendar.init('calendarContainer', function(start, end) {
                self.checkIn = start;
                self.checkOut = end;
            });
        },

        loadPropertyInfo: function() {
            var self = this;
            api({ url: API_BASE + '/properties/' + this.propertyCode, method: 'GET' })
                .done(function(res) {
                    if (res.data) {
                        self.propertyInfo = res.data;
                        self.renderPropertyInfo(res.data);
                    }
                });
        },

        renderPropertyInfo: function(info) {
            $('#hotelName').text(info.hotelName || '');
            $('#propertyName').text(info.propertyName || '객실 예약');
            $('#propertyAddress').text((info.address || '') + (info.addressDetail ? ' ' + info.addressDetail : ''));
            document.title = (info.propertyName || 'Hola') + ' - 객실 예약';
        },

        bindEvents: function() {
            var self = this;

            // 성인 스테퍼
            $('#adultsDecrease').on('click', function() {
                if (self.adults > 1) { self.adults--; $('#adultsValue').text(self.adults); }
                self.updateStepperBtns();
            });
            $('#adultsIncrease').on('click', function() {
                if (self.adults < 6) { self.adults++; $('#adultsValue').text(self.adults); }
                self.updateStepperBtns();
            });

            // 아동 스테퍼
            $('#childrenDecrease').on('click', function() {
                if (self.children > 0) { self.children--; $('#childrenValue').text(self.children); }
                self.updateStepperBtns();
            });
            $('#childrenIncrease').on('click', function() {
                if (self.children < 4) { self.children++; $('#childrenValue').text(self.children); }
                self.updateStepperBtns();
            });

            // 검색
            $('#btnSearch').on('click', function() { self.doSearch(); });

            this.updateStepperBtns();
        },

        updateStepperBtns: function() {
            $('#adultsDecrease').prop('disabled', this.adults <= 1);
            $('#adultsIncrease').prop('disabled', this.adults >= 6);
            $('#childrenDecrease').prop('disabled', this.children <= 0);
            $('#childrenIncrease').prop('disabled', this.children >= 4);
        },

        doSearch: function() {
            hideError();
            if (!this.checkIn || !this.checkOut) {
                showError('체크인/체크아웃 날짜를 선택해주세요.');
                return;
            }
            window.location.href = '/booking/' + this.propertyCode + '/rooms'
                + '?checkIn=' + this.checkIn + '&checkOut=' + this.checkOut
                + '&adults=' + this.adults + '&children=' + this.children;
        }
    };

    // ═══════════════════════════════════════
    // 객실 선택 페이지 (rooms.html)
    // ═══════════════════════════════════════

    var RoomsPage = {
        propertyCode: null, checkIn: null, checkOut: null, adults: null, children: null,
        rooms: [], selectedRoom: null,

        init: function() {
            this.propertyCode = $('#propertyCode').val();
            this.checkIn = $('#checkIn').val();
            this.checkOut = $('#checkOut').val();
            this.adults = parseInt($('#adults').val()) || 2;
            this.children = parseInt($('#children').val()) || 0;
            if (!this.propertyCode || !this.checkIn || !this.checkOut) return;

            var nights = calcNights(this.checkIn, this.checkOut);
            $('#summaryNights').text(nights + '박');
            this.loadPropertyName();
            this.loadAvailability();
            this.bindEvents();
        },

        loadPropertyName: function() {
            api({ url: API_BASE + '/properties/' + this.propertyCode, method: 'GET' })
                .done(function(res) {
                    if (res.data) {
                        $('#summaryPropertyName').text(res.data.propertyName || '');
                        document.title = (res.data.propertyName || 'Hola') + ' - 객실 선택';
                    }
                });
        },

        loadAvailability: function() {
            var self = this;
            $('#roomsLoading').show();
            $('#roomsList').empty();
            $('#noRoomsMessage').hide();

            api({
                url: API_BASE + '/properties/' + this.propertyCode + '/availability',
                method: 'GET',
                data: { checkIn: this.checkIn, checkOut: this.checkOut, adults: this.adults, children: this.children }
            }).done(function(res) {
                $('#roomsLoading').hide();
                if (res.data && res.data.length > 0) {
                    self.rooms = res.data;
                    self.renderRooms(res.data);
                } else { $('#noRoomsMessage').show(); }
            }).fail(function() { $('#roomsLoading').hide(); $('#noRoomsMessage').show(); });
        },

        renderRooms: function(rooms) {
            var self = this;
            var $list = $('#roomsList').empty();
            $.each(rooms, function(idx, room) { $list.append(self.buildRoomCard(room, idx)); });
        },

        // 데모용 객실 이미지 (순환)
        DEMO_ROOM_IMAGES: [
            '/img/rooms/room-01.jpg', '/img/rooms/room-02.jpg', '/img/rooms/room-03.jpg',
            '/img/rooms/room-04.jpg', '/img/rooms/room-05.jpg', '/img/rooms/room-06.jpg',
            '/img/rooms/room-07.jpg', '/img/rooms/room-08.jpg', '/img/rooms/room-09.jpg'
        ],

        buildRoomCard: function(room, idx) {
            var nights = calcNights(this.checkIn, this.checkOut);

            // 이미지 영역 (데모: 순환 배정)
            var imgSrc = this.DEMO_ROOM_IMAGES[idx % this.DEMO_ROOM_IMAGES.length];
            var imageHtml = '<div class="room-card-image">'
                + '<img src="' + imgSrc + '" alt="' + escapeHtml(room.roomClassName) + '">';

            // 가용 수 뱃지
            var availClass = room.availableCount <= 3 ? 'low' : '';
            var availText = room.availableCount <= 3 ? '잔여 ' + room.availableCount + '실' : room.availableCount + '실 예약 가능';
            imageHtml += '<div class="room-avail-badge ' + availClass + '">' + availText + '</div></div>';

            // 메타 태그
            var metaHtml = '<div class="room-meta">';
            if (room.roomSize) metaHtml += '<span class="room-meta-tag"><i class="fas fa-expand-arrows-alt"></i> ' + room.roomSize + '㎡</span>';
            metaHtml += '<span class="room-meta-tag"><i class="fas fa-user"></i> 최대 ' + room.maxAdults + '명</span>';
            if (room.features) {
                $.each(room.features.split(','), function(i, f) {
                    var t = f.trim();
                    if (t) metaHtml += '<span class="room-meta-tag">' + escapeHtml(t) + '</span>';
                });
            }
            if (room.freeServices && room.freeServices.length > 0) {
                $.each(room.freeServices, function(i, svc) {
                    metaHtml += '<span class="room-meta-tag"><i class="fas fa-check"></i> ' + escapeHtml(svc.nameKo) + '</span>';
                });
            }
            metaHtml += '</div>';

            // 요금 옵션
            var rateHtml = '';
            if (room.rateOptions && room.rateOptions.length > 0) {
                rateHtml += '<div class="rate-options-title"><i class="fas fa-tag"></i> 요금 선택';
                if (nights > 0) rateHtml += ' <span class="text-muted">(' + nights + '박 기준)</span>';
                rateHtml += '</div>';

                $.each(room.rateOptions, function(ri, rate) {
                    var isDayUse = rate.stayType === 'DAY_USE';
                    var perNight = nights > 0 ? Math.round(rate.totalAmount / nights) : rate.totalAmount;

                    var svcHtml = '';
                    if (rate.includedServices && rate.includedServices.length > 0) {
                        $.each(rate.includedServices, function(si, svc) {
                            svcHtml += '<div class="rate-service-tag"><i class="fas fa-check"></i> ' + escapeHtml(svc.nameKo) + '</div>';
                        });
                    }

                    var priceUnit = isDayUse ? '' : '<div class="rate-price-unit">1박 평균 ' + formatCurrency(perNight, rate.currency) + '</div>';

                    rateHtml += '<div class="rate-option" data-room-idx="' + idx + '" data-rate-idx="' + ri + '"'
                        + ' data-room-type-id="' + room.roomTypeId + '"'
                        + ' data-room-type-code="' + escapeHtml(room.roomTypeCode) + '"'
                        + ' data-room-class-name="' + escapeHtml(room.roomClassName) + '"'
                        + ' data-rate-code-id="' + rate.rateCodeId + '"'
                        + ' data-rate-code="' + escapeHtml(rate.rateCode) + '"'
                        + ' data-rate-name="' + escapeHtml(rate.rateNameKo) + '"'
                        + ' data-total-amount="' + rate.totalAmount + '"'
                        + ' data-currency="' + (rate.currency || 'KRW') + '"'
                        + ' data-stay-type="' + (rate.stayType || 'OVERNIGHT') + '"'
                        + ' data-dayuse-hours="' + (rate.dayUseDurationHours || '') + '"'
                        + ' data-included-services="' + escapeHtml(JSON.stringify(rate.includedServices || [])) + '">'
                        + '<div class="d-flex justify-content-between align-items-center">'
                        + '<div>'
                        + '<div class="rate-name">' + escapeHtml(rate.rateNameKo) + '</div>'
                        + '<div class="rate-code">' + escapeHtml(rate.rateCode) + '</div>'
                        + svcHtml + '</div>'
                        + '<div class="text-end">'
                        + '<div class="rate-price">' + formatCurrency(rate.totalAmount, rate.currency) + '</div>'
                        + priceUnit + '</div></div>'
                        + '<div class="daily-prices-toggle" style="display:none;">' + buildDailyPricesHtml(rate.dailyPrices, rate.currency) + '</div>'
                        + '</div>';
                });
            } else {
                rateHtml = '<p class="text-muted small">적용 가능한 요금이 없습니다.</p>';
            }

            return '<div class="room-card">'
                + '<div class="room-card-layout">'
                + imageHtml
                + '<div class="room-card-body">'
                + '<div class="room-card-info">'
                + '<div class="room-name">' + escapeHtml(room.roomClassName) + '</div>'
                + metaHtml
                + (room.description ? '<div class="room-desc">' + escapeHtml(room.description) + '</div>' : '')
                + '</div>'
                + '<div class="room-card-rates">'
                + rateHtml
                + '</div>'
                + '</div>'
                + '</div>'
                + '</div>';
        },

        bindEvents: function() {
            var self = this;

            $(document).on('click', '.rate-option', function() {
                var $this = $(this);
                $('.rate-option').removeClass('selected');
                $this.addClass('selected');
                $('.daily-prices-toggle').hide();
                $this.find('.daily-prices-toggle').slideDown(200);

                var includedSvcs = [];
                try { includedSvcs = JSON.parse($this.attr('data-included-services') || '[]'); } catch(e) {}

                self.selectedRoom = {
                    roomTypeId: parseInt($this.data('room-type-id')),
                    roomTypeCode: $this.data('room-type-code'),
                    roomClassName: $this.data('room-class-name'),
                    rateCodeId: parseInt($this.data('rate-code-id')),
                    rateCode: $this.data('rate-code'),
                    rateNameKo: $this.data('rate-name'),
                    totalAmount: parseFloat($this.data('total-amount')),
                    currency: $this.data('currency') || 'KRW',
                    includedServices: includedSvcs,
                    stayType: $this.data('stay-type') || 'OVERNIGHT',
                    dayUseDurationHours: parseInt($this.data('dayuse-hours')) || null
                };
                self.updateBottomBar();
            });

            $('#btnProceedCheckout').on('click', function() { self.proceedToCheckout(); });
        },

        updateBottomBar: function() {
            if (!this.selectedRoom) { $('#bottomBar').hide(); return; }
            var r = this.selectedRoom;
            $('#selectedRoomSummary').text(r.roomClassName + ' · ' + r.rateNameKo);
            $('#selectedTotalAmount').text(formatCurrency(r.totalAmount, r.currency));
            $('#bottomBar').slideDown(200);
        },

        proceedToCheckout: function() {
            if (!this.selectedRoom) { showError('객실과 요금을 선택해주세요.'); return; }
            var bookingData = {
                propertyCode: this.propertyCode,
                checkIn: this.checkIn, checkOut: this.checkOut,
                adults: this.adults, children: this.children,
                stayType: this.selectedRoom.stayType || 'OVERNIGHT',
                dayUseDurationHours: this.selectedRoom.dayUseDurationHours,
                rooms: [{
                    roomTypeId: this.selectedRoom.roomTypeId,
                    roomTypeCode: this.selectedRoom.roomTypeCode,
                    roomClassName: this.selectedRoom.roomClassName,
                    rateCodeId: this.selectedRoom.rateCodeId,
                    rateCode: this.selectedRoom.rateCode,
                    rateNameKo: this.selectedRoom.rateNameKo,
                    totalAmount: this.selectedRoom.totalAmount,
                    currency: this.selectedRoom.currency,
                    includedServices: this.selectedRoom.includedServices || [],
                    stayType: this.selectedRoom.stayType || 'OVERNIGHT',
                    dayUseDurationHours: this.selectedRoom.dayUseDurationHours,
                    checkIn: this.checkIn, checkOut: this.checkOut,
                    adults: this.adults, children: this.children
                }]
            };
            sessionStorage.setItem('hola_booking', JSON.stringify(bookingData));
            window.location.href = '/booking/' + this.propertyCode + '/checkout';
        }
    };

    // ═══════════════════════════════════════
    // 체크아웃 페이지 (checkout.html)
    // ═══════════════════════════════════════

    var CheckoutPage = {
        propertyCode: null, bookingData: null, paymentMethod: 'CARD', submitting: false,

        init: function() {
            this.propertyCode = $('#propertyCode').val();
            var stored = sessionStorage.getItem('hola_booking');
            if (!stored) { $('#noDataAlert').removeClass('d-none'); return; }
            this.bookingData = JSON.parse(stored);
            if (this.bookingData.propertyCode !== this.propertyCode) { $('#noDataAlert').removeClass('d-none'); return; }
            this.renderSummary();
            this.verifyPrice();
            this.bindEvents();
            $('#checkoutContent').show();
        },

        verifyPrice: function() {
            var data = this.bookingData, room = data.rooms[0], self = this;
            api({
                url: API_BASE + '/properties/' + this.propertyCode + '/price-check',
                method: 'POST',
                data: JSON.stringify({ roomTypeId: room.roomTypeId, rateCodeId: room.rateCodeId, checkIn: data.checkIn, checkOut: data.checkOut, adults: data.adults, children: data.children })
            }).done(function(res) {
                if (res.data && res.data.grandTotal) {
                    var verified = Number(res.data.grandTotal), original = Number(room.totalAmount);
                    if (verified !== original) {
                        room.totalAmount = verified;
                        data.rooms[0] = room;
                        self.bookingData = data;
                        sessionStorage.setItem('hola_booking', JSON.stringify(data));
                        $('#summaryTotal').text(formatCurrency(verified, res.data.currency || room.currency));
                        showError('요금이 변동되었습니다. 변경된 금액을 확인해주세요: ' + formatCurrency(verified, res.data.currency));
                    }
                }
            });
        },

        renderSummary: function() {
            var data = this.bookingData, room = data.rooms[0];
            var nights = calcNights(data.checkIn, data.checkOut);
            var isDayUse = data.stayType === 'DAY_USE' || room.stayType === 'DAY_USE';
            var dayUseLabel = isDayUse ? formatDayUseLabel(data.dayUseDurationHours || room.dayUseDurationHours) : '';

            var inclHtml = '';
            if (room.includedServices && room.includedServices.length > 0) {
                inclHtml = '<div class="mt-2">';
                $.each(room.includedServices, function(i, svc) {
                    inclHtml += '<div style="font-size:0.8125rem;"><i class="fas fa-check" style="color:var(--bk-blue);margin-right:4px;"></i>' + escapeHtml(svc.nameKo) + '</div>';
                });
                inclHtml += '</div>';
            }

            $('#summaryRoomInfo').html(
                '<div style="font-weight:700;">' + escapeHtml(room.roomClassName) + '</div>'
                + '<div style="font-size:0.8125rem;color:var(--bk-gray-600);">' + (isDayUse ? dayUseLabel : escapeHtml(room.rateNameKo)) + '</div>'
                + inclHtml
            );

            $('#summaryCheckIn').text(formatDate(data.checkIn));
            if (isDayUse) {
                $('#summaryCheckOut').text(formatDate(data.checkIn));
                $('#summaryNightsLabel').text('이용');
                $('#summaryNights').text(dayUseLabel);
            } else {
                $('#summaryCheckOut').text(formatDate(data.checkOut));
                $('#summaryNightsLabel').text('숙박');
                $('#summaryNights').text(nights + '박 ' + (nights+1) + '일');
            }
            var g = '성인 ' + data.adults + '명';
            if (data.children > 0) g += ', 아동 ' + data.children + '명';
            $('#summaryGuests').text(g);
            $('#summaryTotal').text(formatCurrency(room.totalAmount, room.currency));
        },

        bindEvents: function() {
            var self = this;

            // 결제수단 탭 전환
            $('.payment-method-option').on('click', function() {
                $('.payment-method-option').removeClass('selected');
                $(this).addClass('selected');
                var method = $(this).data('method');
                self.paymentMethod = method;
                if (method === 'EASY_PAY') {
                    $('#easyPayPanel').slideDown(200);
                    self.loadEasyPayCards();
                } else {
                    $('#easyPayPanel').slideUp(200);
                }
            });

            // 간편결제 카드 추가 버튼 (동적 렌더링이므로 delegate)
            $(document).on('click', '#btnAddEasyCard', function() {
                self.registerBillkey();
            });

            // 간편결제 카드 선택
            $(document).on('click', '.easy-pay-card', function() {
                $('.easy-pay-card').removeClass('selected');
                $(this).addClass('selected');
                self.selectedCardId = $(this).data('card-id');
            });

            // 간편결제 카드 삭제
            $(document).on('click', '.easy-pay-card-delete', function(e) {
                e.stopPropagation();
                var cardId = $(this).closest('.easy-pay-card').data('card-id');
                self.deleteEasyPayCard(cardId);
            });

            // 이메일 입력 시 간편결제 카드 목록 갱신 (탭이 열려있을 때)
            $('#email').on('change', function() {
                if (self.paymentMethod === 'EASY_PAY') {
                    self.loadEasyPayCards();
                }
            });

            // 빌키 등록 완료 메시지 수신
            window.addEventListener('message', function(e) {
                if (e.origin !== window.location.origin) return;
                if (e.data && e.data.type === 'BILLKEY_REGISTER_COMPLETE') {
                    if (e.data.success) {
                        self.loadEasyPayCards();
                    }
                }
            });

            $('#agreeAll').on('change', function() {
                $('.agree-item').prop('checked', $(this).is(':checked'));
                self.updateSubmitButton();
            });

            $('.agree-item').on('change', function() {
                $('#agreeAll').prop('checked', $('.agree-item').length === $('.agree-item:checked').length);
                self.updateSubmitButton();
            });

            $('#guestNameKo, #phoneNumber, #email').on('input', function() { self.updateSubmitButton(); });
            $('#btnSubmitBooking').on('click', function() { self.submitBooking(); });
        },

        updateSubmitButton: function() {
            var valid = $('#guestNameKo').val().trim() && $('#phoneNumber').val().trim() && $('#email').val().trim();
            var terms = $('#agreeTerms').is(':checked') && $('#agreePrivacy').is(':checked');
            $('#btnSubmitBooking').prop('disabled', !(valid && terms));
        },

        submitBooking: function() {
            if (this.submitting) return;
            hideError();
            if (!$('#guestNameKo').val().trim()) { showError('성명(한글)을 입력해주세요.'); return; }
            if (!$('#phoneNumber').val().trim()) { showError('휴대전화를 입력해주세요.'); return; }
            if (!$('#email').val().trim()) { showError('이메일을 입력해주세요.'); return; }
            if (!$('#agreeTerms').is(':checked') || !$('#agreePrivacy').is(':checked')) { showError('필수 약관에 동의해주세요.'); return; }

            if (this.paymentMethod === 'EASY_PAY') {
                this.submitEasyPayBooking();
            } else {
                this.initiateKiccPayment();
            }
        },

        submitCashBooking: function() {
            var req = this.buildBookingRequest();
            this.submitting = true;
            var $btn = $('#btnSubmitBooking').prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span> 예약 처리 중...');
            var self = this;
            api({ url: API_BASE + '/properties/' + this.propertyCode + '/reservations', method: 'POST', data: JSON.stringify(req) })
                .done(function(res) {
                    if (res.data) {
                        sessionStorage.removeItem('hola_booking');
                        sessionStorage.setItem('hola_booking_confirmation', JSON.stringify(res.data));
                        window.location.href = '/booking/' + self.propertyCode + '/confirmation/' + res.data.confirmationNo;
                    }
                }).fail(function() {
                    self.submitting = false;
                    $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
                });
        },

        initiateKiccPayment: function() {
            var req = this.buildBookingRequest();
            this.submitting = true;
            var $btn = $('#btnSubmitBooking').prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span> 결제창 준비 중...');
            var self = this;

            window.addEventListener('message', function onKiccMessage(e) {
                if (e.origin !== window.location.origin) return;
                if (e.data && e.data.type === 'KICC_PAYMENT_COMPLETE') {
                    window.removeEventListener('message', onKiccMessage);
                    if (e.data.success) {
                        sessionStorage.removeItem('hola_booking');
                        var orderNo = e.data.shopOrderNo || sessionStorage.getItem('hola_kicc_shopOrderNo');
                        var rUrl = '/booking/' + self.propertyCode + '/confirmation/' + e.data.confirmationNo;
                        if (orderNo) {
                            $.ajax({ url: API_BASE + '/payment/result', method: 'GET', data: { shopOrderNo: orderNo } })
                                .done(function(res) {
                                    if (res.confirmation) sessionStorage.setItem('hola_booking_confirmation', JSON.stringify(res.confirmation));
                                    sessionStorage.removeItem('hola_kicc_shopOrderNo');
                                    window.location.href = rUrl;
                                }).fail(function() { sessionStorage.removeItem('hola_kicc_shopOrderNo'); window.location.href = rUrl; });
                        } else { window.location.href = rUrl; }
                    } else {
                        self.submitting = false;
                        $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
                        showError(e.data.errorMessage || '결제에 실패했습니다.');
                    }
                }
            });

            $.ajax({
                url: API_BASE + '/payment/register?propertyCode=' + this.propertyCode,
                method: 'POST', contentType: 'application/json', data: JSON.stringify(req)
            }).done(function(res) {
                if (res.success && res.authPageUrl) {
                    if (self.isMobile()) {
                        sessionStorage.setItem('hola_kicc_shopOrderNo', res.shopOrderNo);
                        window.location.href = res.authPageUrl;
                    } else {
                        var popup = window.open(res.authPageUrl, 'kiccPayment', 'width=720,height=680,scrollbars=yes,resizable=yes');
                        if (!popup || popup.closed) {
                            showError('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요.');
                            self.submitting = false;
                            $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
                        }
                    }
                } else {
                    showError('결제 준비에 실패했습니다.');
                    self.submitting = false;
                    $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
                }
            }).fail(function(xhr) {
                showError(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '결제 준비에 실패했습니다.');
                self.submitting = false;
                $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
            });
        },

        buildBookingRequest: function() {
            var room = this.bookingData.rooms[0];
            return {
                idempotencyKey: this.generateUUID(),
                guest: {
                    guestNameKo: $('#guestNameKo').val().trim(),
                    guestFirstNameEn: $('#guestFirstNameEn').val().trim() || null,
                    guestLastNameEn: $('#guestLastNameEn').val().trim() || null,
                    phoneCountryCode: $('#phoneCountryCode').val(),
                    phoneNumber: $('#phoneNumber').val().trim(),
                    email: $('#email').val().trim(),
                    nationality: $('#nationality').val()
                },
                rooms: [{ roomTypeId: room.roomTypeId, rateCodeId: room.rateCodeId, checkIn: this.bookingData.checkIn, checkOut: this.bookingData.checkOut, adults: this.bookingData.adults, children: this.bookingData.children }],
                payment: { method: this.paymentMethod },
                agreedTerms: true
            };
        },

        // ═══════════════════════════════════════
        // 간편결제 기능
        // ═══════════════════════════════════════

        selectedCardId: null,

        /** 등록된 간편결제 카드 목록 로드 */
        loadEasyPayCards: function() {
            var email = $('#email').val().trim();
            if (!email || !$('#guestNameKo').val().trim() || !$('#phoneNumber').val().trim()) {
                this.renderEasyPayCards([]);
                return;
            }
            var self = this;
            $.ajax({
                url: API_BASE + '/easy-pay/cards',
                method: 'GET',
                data: { email: email }
            }).done(function(res) {
                var cards = res.data || [];
                self.renderEasyPayCards(cards);
                // 카드 5개 이상이면 추가 버튼 숨김
                if (cards.length >= 5) {
                    $('#btnAddEasyCard').hide();
                } else {
                    $('#btnAddEasyCard').show();
                }
            }).fail(function() {
                $('#easyPayCards').html('<div class="text-danger small p-3">카드 목록을 불러올 수 없습니다.</div>');
            });
        },

        /** 카드 목록 렌더링 */
        renderEasyPayCards: function(cards) {
            var email = $('#email').val().trim();

            // D-01: 이메일 미입력 시 안내 메시지 표시
            if (!email) {
                $('#easyPayCards').html(
                    '<div class="text-muted small p-3 text-center">' +
                    '<i class="fas fa-info-circle me-1"></i>' +
                    '이메일을 먼저 입력하면 등록된 카드를 확인할 수 있습니다.</div>'
                );
                this.selectedCardId = null;
                return;
            }

            var html = '';
            var self = this;
            cards.forEach(function(card, idx) {
                var last4 = card.cardMaskNo ? card.cardMaskNo.slice(-4) : '****';
                var displayNo = '**** **** **** ' + last4;
                var issuer = card.issuerName || '카드';
                var selectedClass = idx === 0 ? ' selected' : '';
                html += '<div class="easy-pay-card' + selectedClass + '" data-card-id="' + card.id + '">' +
                    '<button class="easy-pay-card-delete" title="삭제">&times;</button>' +
                    '<div class="easy-pay-card-issuer">' + issuer + '</div>' +
                    '<div class="easy-pay-card-type">' + (card.cardType || '') + '</div>' +
                    '<div class="easy-pay-card-number">' + displayNo + '</div>' +
                    '</div>';
            });
            // 추가 버튼은 카드 목록 뒤에 (5개 미만일 때만)
            if (cards.length < 5) {
                html += '<div class="easy-pay-add" id="btnAddEasyCard">' +
                    '<div class="easy-pay-add-icon"><i class="fas fa-plus"></i></div>' +
                    '<span class="easy-pay-add-text">카드 추가</span></div>';
            }
            $('#easyPayCards').html(html);
            // 디폴트: 첫번째 카드 자동 선택
            if (cards.length > 0) {
                self.selectedCardId = cards[0].id;
            } else {
                self.selectedCardId = null;
            }
        },

        /** 필수 입력 항목 체크 — 미입력 필드 포커스 + 하이라이트 */
        validateGuestInfo: function() {
            var fields = [
                { id: '#guestNameKo', label: '성명(한글)' },
                { id: '#phoneNumber', label: '휴대전화' },
                { id: '#email', label: '이메일' }
            ];
            for (var i = 0; i < fields.length; i++) {
                var $f = $(fields[i].id);
                if (!$f.val().trim()) {
                    $f.focus().addClass('is-invalid');
                    setTimeout(function() { $('.is-invalid').removeClass('is-invalid'); }, 2500);
                    return false;
                }
            }
            return true;
        },

        /** 빌키 등록 (KICC 인증창 팝업) */
        registerBillkey: function() {
            if (!this.validateGuestInfo()) return;
            var email = $('#email').val().trim();
            var self = this;
            var reqData = {
                email: email,
                customerName: $('#guestNameKo').val().trim() || '',
                customerPhone: $('#phoneNumber').val().trim() || ''
            };

            $.ajax({
                url: API_BASE + '/easy-pay/register',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(reqData)
            }).done(function(res) {
                if (res.success && res.authPageUrl) {
                    if (self.isMobile()) {
                        sessionStorage.setItem('hola_billkey_shopOrderNo', res.shopOrderNo);
                        window.location.href = res.authPageUrl;
                    } else {
                        window.open(res.authPageUrl, 'kiccBillkey', 'width=720,height=680,scrollbars=yes,resizable=yes');
                    }
                } else {
                    showError('카드 등록 준비에 실패했습니다.');
                }
            }).fail(function(xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '카드 등록 준비에 실패했습니다.';
                showError(msg);
            });
        },

        /** 간편결제 카드 삭제 */
        deleteEasyPayCard: function(cardId) {
            if (!confirm('이 카드를 삭제하시겠습니까?')) return;
            var email = $('#email').val().trim();
            var self = this;
            $.ajax({
                url: API_BASE + '/easy-pay/cards/' + cardId + '?email=' + encodeURIComponent(email),
                method: 'DELETE'
            }).done(function() {
                self.loadEasyPayCards();
            }).fail(function(xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '카드 삭제에 실패했습니다.';
                showError(msg);
            });
        },

        /** 간편결제로 예약 (빌키 결제) */
        submitEasyPayBooking: function() {
            if (!this.selectedCardId) {
                showError('결제할 카드를 선택해주세요.');
                return;
            }
            var req = this.buildBookingRequest();
            this.submitting = true;
            var $btn = $('#btnSubmitBooking').prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span> 간편결제 처리 중...');
            var self = this;

            $.ajax({
                url: API_BASE + '/easy-pay/pay?propertyCode=' + this.propertyCode + '&cardId=' + this.selectedCardId,
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(req)
            }).done(function(res) {
                if (res.success && res.confirmationNo) {
                    sessionStorage.removeItem('hola_booking');
                    if (res.confirmation) {
                        sessionStorage.setItem('hola_booking_confirmation', JSON.stringify(res.confirmation));
                    }
                    window.location.href = '/booking/' + self.propertyCode + '/confirmation/' + res.confirmationNo;
                } else {
                    showError('결제 처리에 실패했습니다.');
                    self.submitting = false;
                    $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
                }
            }).fail(function(xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '간편결제에 실패했습니다.';
                showError(msg);
                self.submitting = false;
                $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
            });
        },

        isMobile: function() { return /Mobile|Android|iPhone|iPad/i.test(navigator.userAgent); },

        generateUUID: function() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random()*16|0; return (c==='x'?r:(r&0x3|0x8)).toString(16);
            });
        }
    };

    // ═══════════════════════════════════════
    // 예약 확인 페이지 (confirmation.html)
    // ═══════════════════════════════════════

    var ConfirmationPage = {
        propertyCode: null, confirmationNo: null,

        init: function() {
            this.propertyCode = $('#propertyCode').val();
            this.confirmationNo = $('#confirmationNo').val();
            if (!this.confirmationNo) { showError('예약 확인번호가 없습니다.'); return; }
            this.bindEvents();

            var stored = sessionStorage.getItem('hola_booking_confirmation');
            if (stored) {
                var data = JSON.parse(stored);
                if (data.confirmationNo === this.confirmationNo) {
                    sessionStorage.removeItem('hola_booking_confirmation');
                    this.renderConfirmation(data);
                    return;
                }
            }
            $('#verifySection').show();
        },

        bindEvents: function() {
            var self = this;
            $('#btnVerify').on('click', function() { self.verifyAndLoad(); });
            $('#verifyEmail').on('keypress', function(e) { if (e.which === 13) self.verifyAndLoad(); });
        },

        verifyAndLoad: function() {
            var email = $('#verifyEmail').val().trim();
            if (!email) { showError('이메일을 입력해주세요.'); return; }
            hideError();
            $('#verifySection').hide();
            $('#confirmLoading').show();
            var self = this;
            api({ url: API_BASE + '/confirmation/' + this.confirmationNo, method: 'GET', data: { email: email } })
                .done(function(res) { if (res.data) self.renderConfirmation(res.data); })
                .fail(function() { $('#confirmLoading').hide(); $('#verifySection').show(); });
        },

        renderConfirmation: function(data) {
            $('#confirmLoading').hide();
            $('#confirmNo').text(data.confirmationNo || '-');
            $('#cfmReservationNo').text(data.masterReservationNo || '-');
            $('#cfmGuestName').text(data.guestNameKo || '-');
            $('#cfmPropertyName').text(data.propertyName || '-');
            $('#cfmPropertyPhone').text(data.propertyPhone || '-');
            $('#cfmStatus').html(statusBadge(data.reservationStatus));

            // 객실 정보
            var rHtml = '';
            if (data.rooms && data.rooms.length > 0) {
                $.each(data.rooms, function(i, room) {
                    var isDU = room.stayType === 'DAY_USE';
                    var stay = isDU ? formatDayUseLabel(room.dayUseDurationHours) : room.nights + '박';
                    var dates = isDU ? formatDate(room.checkIn) : formatDate(room.checkIn) + ' ~ ' + formatDate(room.checkOut);
                    rHtml += '<div class="detail-row"><span class="detail-label">' + escapeHtml(room.roomTypeName) + '</span>'
                        + '<span class="detail-value">' + dates + ' (' + stay + ')</span></div>'
                        + '<div class="detail-row"><span class="detail-label">인원</span>'
                        + '<span class="detail-value">성인 ' + room.adults + '명' + (room.children > 0 ? ', 아동 ' + room.children + '명' : '') + '</span></div>'
                        + '<div class="detail-row"><span class="detail-label">객실 요금</span>'
                        + '<span class="detail-value">' + formatCurrency(room.roomTotal, data.currency) + '</span></div>';
                    if (i < data.rooms.length - 1) rHtml += '<hr class="my-2">';
                });
            }
            $('#cfmRooms').html(rHtml);

            $('#cfmTotalAmount').text(formatCurrency(data.totalAmount, data.currency));
            $('#cfmPaymentStatus').html(payStatusBadge(data.paymentStatus));
            $('#cfmPaymentMethod').text(METHOD_MAP[data.paymentMethod] || data.paymentMethod || '-');
            if (data.approvalNo) { $('#cfmApprovalNo').text(data.approvalNo); $('#cfmApprovalRow').show(); }
            if (data.paymentDate) { $('#cfmPaymentDate').text(formatDateTime(data.paymentDate)); $('#cfmPaymentDateRow').show(); }
            if (data.cardMaskNo) { $('#cfmCardMaskNo').text(data.cardMaskNo); $('#cfmCardMaskNoRow').show(); }
            $('#cfmCheckInTime').text(data.checkInTime || '-');
            $('#cfmCheckOutTime').text(data.checkOutTime || '-');

            if (data.cancellationPolicies && data.cancellationPolicies.length > 0) {
                var $list = $('#cfmPolicyList').empty();
                data.cancellationPolicies.forEach(function(p) { $list.append('<li class="mb-1">' + escapeHtml(p.description) + '</li>'); });
                $('#cfmPolicySection').show();
            }

            document.title = '예약 완료 - ' + (data.confirmationNo || 'Hola');
            $('#confirmContent').show();
        }
    };

    // ═══════════════════════════════════════
    // 예약 조회 페이지 (my-reservation.html)
    // ═══════════════════════════════════════

    var MyReservationPage = {
        confirmationNo: null,
        reservationData: null,

        init: function() {
            this.bindEvents();
        },

        bindEvents: function() {
            var self = this;

            $('#btnLookup').on('click', function() { self.lookup(); });
            $('#lookupConfirmNo').on('keypress', function(e) { if (e.which === 13) self.lookup(); });
            $('#btnBackToLookup').on('click', function() { self.showLookup(); });

            // 취소 관련
            $('#btnCancelReservation').on('click', function() { self.openCancelModal(); });
            $('#btnCancelModalClose').on('click', function() { self.closeCancelModal(); });
            $('#btnConfirmCancel').on('click', function() { self.confirmCancel(); });
            $('#cancelModal').on('click', function(e) { if (e.target === this) self.closeCancelModal(); });

            // 일별 요금 토글
            $('#chargesToggleBtn').on('click', function() {
                var $btn = $(this), $content = $('#chargesContent');
                if ($content.is(':visible')) { $content.slideUp(200); $btn.removeClass('open'); }
                else { $content.slideDown(200); $btn.addClass('open'); }
            });
        },

        showLookup: function() {
            $('#detailSection').hide();
            $('#lookupSection').show();
            hideError();
        },

        lookup: function() {
            var cn = $('#lookupConfirmNo').val().trim();
            if (!cn) { showError('예약번호를 입력해주세요.'); return; }
            hideError();
            this.confirmationNo = cn;

            $('#lookupSection').hide();
            $('#lookupLoading').show();

            var self = this;
            api({
                url: API_BASE + '/confirmation/' + encodeURIComponent(cn),
                method: 'GET'
            }).done(function(res) {
                $('#lookupLoading').hide();
                if (res.data) {
                    self.reservationData = res.data;
                    self.renderDetail(res.data);
                }
            }).fail(function() {
                $('#lookupLoading').hide();
                self.showLookup();
            });
        },

        renderDetail: function(data) {
            // 상태 바
            $('#dtlConfirmNo').text(data.confirmationNo || '-');
            $('#dtlStatus').html(statusBadge(data.reservationStatus));

            // 투숙객
            $('#dtlGuestName').text(data.guestNameKo || '-');
            $('#dtlGuestPhone').text(data.guestPhone || '-');
            $('#dtlGuestEmail').text(data.guestEmail || '-');

            // 숙박
            $('#dtlPropertyName').text(data.propertyName || '-');

            var roomsHtml = '';
            if (data.rooms && data.rooms.length > 0) {
                $.each(data.rooms, function(i, room) {
                    var isDU = room.stayType === 'DAY_USE';
                    var stay = isDU ? formatDayUseLabel(room.dayUseDurationHours) : room.nights + '박';
                    var dates = isDU ? formatDate(room.checkIn) : formatDate(room.checkIn) + ' ~ ' + formatDate(room.checkOut);

                    roomsHtml += '<div class="detail-row"><span class="detail-label">객실</span>'
                        + '<span class="detail-value">' + escapeHtml(room.roomTypeName) + '</span></div>'
                        + '<div class="detail-row"><span class="detail-label">기간</span>'
                        + '<span class="detail-value">' + dates + ' (' + stay + ')</span></div>'
                        + '<div class="detail-row"><span class="detail-label">인원</span>'
                        + '<span class="detail-value">성인 ' + room.adults + '명' + (room.children > 0 ? ', 아동 ' + room.children + '명' : '') + '</span></div>';

                    if (room.services && room.services.length > 0) {
                        $.each(room.services, function(si, svc) {
                            var isIncl = svc.serviceType === 'RATE_INCLUDED';
                            var priceText = isIncl ? '<span style="color:var(--bk-blue);">포함</span>' : formatCurrency(svc.totalPrice, data.currency);
                            roomsHtml += '<div class="detail-row" style="padding:2px 0;"><span class="detail-label" style="font-size:0.8125rem;">'
                                + (isIncl ? '<span class="badge bg-info text-white me-1" style="font-size:0.6rem;">포함</span>' : '')
                                + escapeHtml(svc.serviceName) + '</span>'
                                + '<span class="detail-value" style="font-size:0.8125rem;">' + priceText + '</span></div>';
                        });
                    }
                    if (i < data.rooms.length - 1) roomsHtml += '<hr class="my-2">';
                });
            }
            $('#dtlRooms').html(roomsHtml);

            // 결제
            $('#dtlTotalAmount').text(formatCurrency(data.totalAmount, data.currency));
            $('#dtlPaymentStatus').html(payStatusBadge(data.paymentStatus));
            $('#dtlPaymentMethod').text(METHOD_MAP[data.paymentMethod] || data.paymentMethod || '-');
            if (data.approvalNo) { $('#dtlApprovalNo').text(data.approvalNo); $('#dtlApprovalRow').show(); }

            // 취소 정책
            if (data.cancellationPolicies && data.cancellationPolicies.length > 0) {
                var $list = $('#dtlPolicyList').empty();
                data.cancellationPolicies.forEach(function(p) { $list.append('<li class="mb-1">' + escapeHtml(p.description) + '</li>'); });
                $('#dtlPolicySection').show();
            }

            // 취소 가능 여부 (RESERVED 상태일 때만)
            if (data.reservationStatus === 'RESERVED') {
                $('#btnCancelReservation').show();
            } else {
                $('#btnCancelReservation').hide();
            }

            // 일별 요금 (첫 번째 객실)
            if (data.rooms && data.rooms.length > 0 && data.rooms[0].dailyCharges && data.rooms[0].dailyCharges.length > 0) {
                $('#chargesContent').html(buildDailyPricesHtml(data.rooms[0].dailyCharges, data.currency));
                $('#dtlChargesSection').show();
            }

            document.title = '예약 조회 - ' + (data.confirmationNo || 'Hola');
            $('#detailSection').show();
        },

        openCancelModal: function() {
            var self = this;
            $('#cancelPreview').hide();
            $('#cancelComplete').hide();
            $('#cancelLoading').show();
            $('#cancelActions').show();
            $('#btnConfirmCancel').show().prop('disabled', false);
            $('#cancelModal').addClass('show');

            // 취소 수수료 조회
            api({
                url: API_BASE + '/reservations/' + encodeURIComponent(this.confirmationNo) + '/cancel-fee',
                method: 'GET'
            }).done(function(res) {
                $('#cancelLoading').hide();
                if (res.data) {
                    var d = res.data;
                    $('#cancelFirstNight').text(formatCurrency(d.firstNightAmount));
                    $('#cancelFeePercent').text(d.cancelFeePercent + '%');
                    $('#cancelFeeAmount').text(formatCurrency(d.cancelFeeAmount));
                    $('#cancelTotalPaid').text(formatCurrency(d.totalPaidAmount));
                    $('#cancelRefund').text(formatCurrency(d.refundAmount));
                    $('#cancelPolicyDesc').text(d.policyDescription || '');
                    $('#cancelPreview').show();
                }
            }).fail(function() {
                $('#cancelLoading').hide();
                self.closeCancelModal();
            });
        },

        closeCancelModal: function() {
            $('#cancelModal').removeClass('show');
        },

        confirmCancel: function() {
            var self = this;
            $('#btnConfirmCancel').prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span> 취소 처리 중...');

            api({
                url: API_BASE + '/reservations/' + encodeURIComponent(this.confirmationNo) + '/cancel',
                method: 'POST',
                data: JSON.stringify({})
            }).done(function(res) {
                if (res.data) {
                    $('#cancelPreview').hide();
                    $('#cancelCompleteRefund').text(formatCurrency(res.data.refundAmount));
                    $('#cancelComplete').show();
                    $('#btnConfirmCancel').hide();
                    $('#btnCancelModalClose').text('닫기');

                    // 상세 화면 상태 업데이트
                    $('#dtlStatus').html(statusBadge('CANCELED'));
                    $('#btnCancelReservation').hide();
                }
            }).fail(function() {
                $('#btnConfirmCancel').prop('disabled', false).html('예약 취소 확인');
            });
        }
    };

    // ═══════════════════════════════════════
    // 글로벌: HOLA 로고 클릭 → 예약 검색 페이지
    // ═══════════════════════════════════════

    function goHome() {
        // URL에서 propertyCode 추출 시도
        var match = window.location.pathname.match(/\/booking\/([^\/]+)/);
        if (match && match[1] !== 'my-reservation') {
            window.location.href = '/booking/' + match[1];
        } else {
            // propertyCode 없으면 sessionStorage에서 시도
            var stored = sessionStorage.getItem('hola_booking');
            if (stored) {
                try {
                    var data = JSON.parse(stored);
                    if (data.propertyCode) {
                        window.location.href = '/booking/' + data.propertyCode;
                        return;
                    }
                } catch(e) {}
            }
            window.location.href = '/booking/my-reservation';
        }
    }

    // ═══════════════════════════════════════
    // 공개 API
    // ═══════════════════════════════════════

    return {
        SearchPage: SearchPage,
        RoomsPage: RoomsPage,
        CheckoutPage: CheckoutPage,
        ConfirmationPage: ConfirmationPage,
        MyReservationPage: MyReservationPage,
        goHome: goHome,
        api: api,
        showError: showError,
        hideError: hideError,
        formatCurrency: formatCurrency,
        formatDate: formatDate,
        calcNights: calcNights,
        escapeHtml: escapeHtml
    };

})();
