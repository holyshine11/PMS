/**
 * Hola Booking Engine - 게스트 예약 플로우 JS
 * 인증 불필요, 공개 API (/api/v1/booking/*) 사용
 */
var HolaBooking = (function() {
    'use strict';

    var API_BASE = '/api/v1/booking';

    // ─── 공통 유틸 ───

    /** API 호출 래퍼 (BookingResponse 래핑 해제) */
    function api(options) {
        var defaults = {
            contentType: 'application/json',
            dataType: 'json'
        };
        var settings = $.extend({}, defaults, options);

        // .then()으로 unwrap하여 .done() 체인에서도 해제된 데이터 수신
        // BookingResponse {result: {RESULT_YN, data}} → {RESULT_YN, data}
        return $.ajax(settings)
            .then(function(res) {
                return (res && res.result) ? res.result : res;
            })
            .fail(function(xhr) {
                var msg = '요청 처리 중 오류가 발생했습니다.';
                if (xhr.responseJSON) {
                    var result = xhr.responseJSON.result || xhr.responseJSON;
                    msg = result.RESULT_MESSAGE || result.message || msg;
                }
                showError(msg);
            });
    }

    /** 에러 표시 */
    function showError(message) {
        var $alert = $('#errorAlert');
        if ($alert.length) {
            $('#errorMessage').text(message);
            $alert.removeClass('d-none');
            $('html, body').animate({ scrollTop: $alert.offset().top - 100 }, 300);
        } else {
            alert(message);
        }
    }

    /** 에러 숨김 */
    function hideError() {
        $('#errorAlert').addClass('d-none');
    }

    /** 금액 포맷 (₩123,456) */
    function formatCurrency(amount, currency) {
        if (amount == null) return '-';
        var num = Number(amount);
        if (isNaN(num)) return '-';
        var prefix = (currency === 'USD') ? '$' : '₩';
        return prefix + num.toLocaleString('ko-KR');
    }

    /** 날짜 포맷 (YYYY-MM-DD → YYYY.MM.DD) */
    function formatDate(dateStr) {
        if (!dateStr) return '-';
        return dateStr.replace(/-/g, '.');
    }

    /** 숙박일수 계산 */
    function calcNights(checkIn, checkOut) {
        var d1 = new Date(checkIn);
        var d2 = new Date(checkOut);
        var diff = Math.ceil((d2 - d1) / (1000 * 60 * 60 * 24));
        return diff > 0 ? diff : 0;
    }

    /** 스피너 표시/숨김 */
    function showSpinner(containerId) {
        var $el = $('#' + containerId);
        $el.html('<div class="booking-spinner"><div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div></div>');
        $el.show();
    }

    /** URL 쿼리 파라미터 읽기 */
    function getQueryParam(name) {
        var params = new URLSearchParams(window.location.search);
        return params.get(name);
    }

    // ─── 검색 페이지 (search.html) ───

    var SearchPage = {
        propertyCode: null,
        propertyInfo: null,

        init: function() {
            this.propertyCode = $('#propertyCode').val();
            if (!this.propertyCode) return;

            this.loadPropertyInfo();
            this.bindEvents();
            this.setDefaultDates();
        },

        /** 프로퍼티 기본정보 로드 */
        loadPropertyInfo: function() {
            var self = this;
            api({
                url: API_BASE + '/properties/' + this.propertyCode,
                method: 'GET'
            }).done(function(res) {
                if (res.data) {
                    self.propertyInfo = res.data;
                    self.renderPropertyInfo(res.data);
                }
            }).fail(function() {
                showError('프로퍼티 정보를 불러올 수 없습니다. 올바른 URL인지 확인해주세요.');
            });
        },

        /** 프로퍼티 정보 화면 렌더링 */
        renderPropertyInfo: function(info) {
            $('#hotelName').text(info.hotelName || '');
            $('#propertyName').text(info.propertyName || '객실 예약');
            $('#propertyAddress').text(
                (info.address || '') + (info.addressDetail ? ' ' + info.addressDetail : '')
            );

            // 상세 정보 카드
            $('#infoCheckIn').text(info.checkInTime || '-');
            $('#infoCheckOut').text(info.checkOutTime || '-');
            $('#infoPhone').text(info.phone || '-');
            $('#infoEmail').text(info.email || '-');
            $('#propertyInfoSection').show();

            // 페이지 타이틀 변경
            document.title = (info.propertyName || 'Hola') + ' - 객실 예약';
        },

        /** 기본 날짜 설정 (오늘 ~ 내일) */
        setDefaultDates: function() {
            var today = new Date();
            var tomorrow = new Date(today);
            tomorrow.setDate(tomorrow.getDate() + 1);

            var todayStr = this.toDateString(today);
            var tomorrowStr = this.toDateString(tomorrow);

            $('#checkIn').attr('min', todayStr).val(todayStr);
            $('#checkOut').attr('min', tomorrowStr).val(tomorrowStr);

            this.updateNightsInfo();
        },

        toDateString: function(date) {
            var y = date.getFullYear();
            var m = String(date.getMonth() + 1).padStart(2, '0');
            var d = String(date.getDate()).padStart(2, '0');
            return y + '-' + m + '-' + d;
        },

        /** 이벤트 바인딩 */
        bindEvents: function() {
            var self = this;

            // 체크인 날짜 변경 → 체크아웃 min 업데이트
            $('#checkIn').on('change', function() {
                var checkIn = $(this).val();
                if (checkIn) {
                    var nextDay = new Date(checkIn);
                    nextDay.setDate(nextDay.getDate() + 1);
                    var minCheckOut = self.toDateString(nextDay);
                    $('#checkOut').attr('min', minCheckOut);

                    // 체크아웃이 체크인보다 이전이면 자동 조정
                    if ($('#checkOut').val() <= checkIn) {
                        $('#checkOut').val(minCheckOut);
                    }
                }
                self.updateNightsInfo();
            });

            $('#checkOut').on('change', function() {
                self.updateNightsInfo();
            });

            // 검색 폼 제출
            $('#searchForm').on('submit', function(e) {
                e.preventDefault();
                hideError();
                self.doSearch();
            });
        },

        /** 숙박일수 표시 업데이트 */
        updateNightsInfo: function() {
            var checkIn = $('#checkIn').val();
            var checkOut = $('#checkOut').val();
            if (checkIn && checkOut) {
                var nights = calcNights(checkIn, checkOut);
                if (nights > 0) {
                    $('#nightsCount').text(nights);
                    $('#nightsCountPlus').text(nights + 1);
                    $('#nightsInfo').show();
                } else {
                    $('#nightsInfo').hide();
                }
            }
        },

        /** 객실 검색 → rooms 페이지로 이동 */
        doSearch: function() {
            var checkIn = $('#checkIn').val();
            var checkOut = $('#checkOut').val();
            var adults = $('#adults').val();
            var children = $('#children').val();

            // 유효성 검증
            if (!checkIn || !checkOut) {
                showError('체크인/체크아웃 날짜를 선택해주세요.');
                return;
            }

            var today = this.toDateString(new Date());
            if (checkIn < today) {
                showError('체크인 날짜는 오늘 이후여야 합니다.');
                return;
            }

            if (checkOut <= checkIn) {
                showError('체크아웃 날짜는 체크인 이후여야 합니다.');
                return;
            }

            var nights = calcNights(checkIn, checkOut);
            if (nights > 30) {
                showError('최대 30박까지 예약 가능합니다.');
                return;
            }

            // rooms 페이지로 이동
            var url = '/booking/' + this.propertyCode + '/rooms'
                + '?checkIn=' + checkIn
                + '&checkOut=' + checkOut
                + '&adults=' + adults
                + '&children=' + children;

            window.location.href = url;
        }
    };

    // ─── 객실 선택 페이지 (rooms.html) ───

    var RoomsPage = {
        propertyCode: null,
        checkIn: null,
        checkOut: null,
        adults: null,
        children: null,
        rooms: [],          // API 응답 객실 목록
        selectedRoom: null, // 선택된 객실 { roomTypeId, roomTypeCode, roomClassName, rateCodeId, rateCode, rateNameKo, totalAmount, currency }

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

        /** 프로퍼티 이름 로드 */
        loadPropertyName: function() {
            api({
                url: API_BASE + '/properties/' + this.propertyCode,
                method: 'GET'
            }).done(function(res) {
                if (res.data) {
                    $('#summaryPropertyName').text(res.data.propertyName || '');
                    document.title = (res.data.propertyName || 'Hola') + ' - 객실 선택';
                }
            });
        },

        /** 가용 객실 검색 API 호출 */
        loadAvailability: function() {
            var self = this;
            $('#roomsLoading').show();
            $('#roomsList').empty();
            $('#noRoomsMessage').hide();

            api({
                url: API_BASE + '/properties/' + this.propertyCode + '/availability',
                method: 'GET',
                data: {
                    checkIn: this.checkIn,
                    checkOut: this.checkOut,
                    adults: this.adults,
                    children: this.children
                }
            }).done(function(res) {
                $('#roomsLoading').hide();
                if (res.data && res.data.length > 0) {
                    self.rooms = res.data;
                    self.renderRooms(res.data);
                } else {
                    $('#noRoomsMessage').show();
                }
            }).fail(function() {
                $('#roomsLoading').hide();
                $('#noRoomsMessage').show();
            });
        },

        /** 객실 카드 렌더링 */
        renderRooms: function(rooms) {
            var self = this;
            var $list = $('#roomsList');
            $list.empty();

            $.each(rooms, function(idx, room) {
                var html = self.buildRoomCard(room, idx);
                $list.append(html);
            });
        },

        /** 개별 객실 카드 HTML 생성 */
        buildRoomCard: function(room, idx) {
            var nights = calcNights(this.checkIn, this.checkOut);

            // 무료 서비스 태그
            var servicesHtml = '';
            if (room.freeServices && room.freeServices.length > 0) {
                $.each(room.freeServices, function(i, svc) {
                    servicesHtml += '<span class="room-feature">' + escapeHtml(svc.nameKo) + '</span>';
                });
            }

            // 객실 특징 태그
            var featuresHtml = '';
            if (room.features) {
                var featureList = room.features.split(',');
                $.each(featureList, function(i, f) {
                    var trimmed = f.trim();
                    if (trimmed) {
                        featuresHtml += '<span class="room-feature">' + escapeHtml(trimmed) + '</span>';
                    }
                });
            }

            // 가용 객실 수 표시
            var availClass = room.availableCount <= 3 ? 'low' : '';
            var availText = room.availableCount <= 3
                ? '잔여 ' + room.availableCount + '실'
                : room.availableCount + '실 예약 가능';

            // 요금 옵션 렌더링
            var rateOptionsHtml = '';
            if (room.rateOptions && room.rateOptions.length > 0) {
                $.each(room.rateOptions, function(ri, rate) {
                    var perNight = nights > 0 ? Math.round(rate.totalAmount / nights) : rate.totalAmount;
                    rateOptionsHtml += ''
                        + '<div class="rate-option" data-room-idx="' + idx + '" data-rate-idx="' + ri + '"'
                        + '     data-room-type-id="' + room.roomTypeId + '"'
                        + '     data-room-type-code="' + escapeHtml(room.roomTypeCode) + '"'
                        + '     data-room-class-name="' + escapeHtml(room.roomClassName) + '"'
                        + '     data-rate-code-id="' + rate.rateCodeId + '"'
                        + '     data-rate-code="' + escapeHtml(rate.rateCode) + '"'
                        + '     data-rate-name="' + escapeHtml(rate.rateNameKo) + '"'
                        + '     data-total-amount="' + rate.totalAmount + '"'
                        + '     data-currency="' + (rate.currency || 'KRW') + '">'
                        + '  <div class="d-flex justify-content-between align-items-center">'
                        + '    <div>'
                        + '      <div class="rate-name">' + escapeHtml(rate.rateNameKo) + '</div>'
                        + '      <div class="text-muted" style="font-size:0.75rem;">' + escapeHtml(rate.rateCode) + '</div>'
                        + '    </div>'
                        + '    <div class="text-end">'
                        + '      <div class="rate-price">' + formatCurrency(rate.totalAmount, rate.currency) + '</div>'
                        + '      <div class="rate-price-unit">1박 평균 ' + formatCurrency(perNight, rate.currency) + '</div>'
                        + '    </div>'
                        + '  </div>'
                        + '  <div class="daily-prices-toggle mt-2" style="display:none;">'
                        + buildDailyPricesHtml(rate.dailyPrices, rate.currency)
                        + '  </div>'
                        + '</div>';
                });
            } else {
                rateOptionsHtml = '<p class="text-muted small">적용 가능한 요금이 없습니다.</p>';
            }

            var roomSizeText = room.roomSize ? room.roomSize + '㎡' : '';
            var capacityText = '최대 성인 ' + room.maxAdults + '명';
            if (room.maxChildren > 0) {
                capacityText += ' / 아동 ' + room.maxChildren + '명';
            }

            return ''
                + '<div class="room-card mb-4">'
                + '  <div class="room-card-header d-flex justify-content-between align-items-center">'
                + '    <div>'
                + '      <span class="room-name">' + escapeHtml(room.roomClassName) + '</span>'
                + '      <span class="room-size ms-2">' + roomSizeText + '</span>'
                + '    </div>'
                + '    <span class="room-availability ' + availClass + '">'
                + '      <i class="fas fa-door-open me-1"></i>' + availText
                + '    </span>'
                + '  </div>'
                + '  <div class="room-card-body">'
                + '    <div class="row">'
                + '      <div class="col-md-5 mb-3 mb-md-0">'
                + '        <p class="text-muted small mb-2">' + escapeHtml(room.description || '') + '</p>'
                + '        <p class="small mb-2"><i class="fas fa-users me-1 text-primary"></i>' + capacityText + '</p>'
                + '        <div class="mb-2">' + featuresHtml + servicesHtml + '</div>'
                + '      </div>'
                + '      <div class="col-md-7">'
                + '        <p class="small text-muted mb-2">'
                + '          <i class="fas fa-tag me-1"></i>요금 선택 <span class="text-muted">(' + nights + '박 기준)</span>'
                + '        </p>'
                + '        ' + rateOptionsHtml
                + '      </div>'
                + '    </div>'
                + '  </div>'
                + '</div>';
        },

        /** 이벤트 바인딩 */
        bindEvents: function() {
            var self = this;

            // 요금 옵션 선택
            $(document).on('click', '.rate-option', function() {
                var $this = $(this);

                // 기존 선택 해제
                $('.rate-option').removeClass('selected');
                $this.addClass('selected');

                // 일자별 요금 토글
                $('.daily-prices-toggle').hide();
                $this.find('.daily-prices-toggle').slideDown(200);

                // 선택 정보 저장
                self.selectedRoom = {
                    roomTypeId: parseInt($this.data('room-type-id')),
                    roomTypeCode: $this.data('room-type-code'),
                    roomClassName: $this.data('room-class-name'),
                    rateCodeId: parseInt($this.data('rate-code-id')),
                    rateCode: $this.data('rate-code'),
                    rateNameKo: $this.data('rate-name'),
                    totalAmount: parseFloat($this.data('total-amount')),
                    currency: $this.data('currency') || 'KRW'
                };

                self.updateBottomBar();
            });

            // 예약하기 버튼
            $('#btnProceedCheckout').on('click', function() {
                self.proceedToCheckout();
            });
        },

        /** 하단 바 업데이트 */
        updateBottomBar: function() {
            if (!this.selectedRoom) {
                $('#bottomBar').hide();
                return;
            }

            var r = this.selectedRoom;
            $('#selectedRoomSummary').text(r.roomClassName + ' (' + r.rateNameKo + ')');
            $('#selectedTotalAmount').text(formatCurrency(r.totalAmount, r.currency));
            $('#bottomBar').slideDown(200);
        },

        /** 체크아웃 페이지로 이동 */
        proceedToCheckout: function() {
            if (!this.selectedRoom) {
                showError('객실과 요금을 선택해주세요.');
                return;
            }

            // sessionStorage에 선택 정보 저장
            var bookingData = {
                propertyCode: this.propertyCode,
                checkIn: this.checkIn,
                checkOut: this.checkOut,
                adults: this.adults,
                children: this.children,
                rooms: [{
                    roomTypeId: this.selectedRoom.roomTypeId,
                    roomTypeCode: this.selectedRoom.roomTypeCode,
                    roomClassName: this.selectedRoom.roomClassName,
                    rateCodeId: this.selectedRoom.rateCodeId,
                    rateCode: this.selectedRoom.rateCode,
                    rateNameKo: this.selectedRoom.rateNameKo,
                    totalAmount: this.selectedRoom.totalAmount,
                    currency: this.selectedRoom.currency,
                    checkIn: this.checkIn,
                    checkOut: this.checkOut,
                    adults: this.adults,
                    children: this.children
                }]
            };

            sessionStorage.setItem('hola_booking', JSON.stringify(bookingData));

            window.location.href = '/booking/' + this.propertyCode + '/checkout';
        }
    };

    // ─── 체크아웃 페이지 (checkout.html) ───

    var CheckoutPage = {
        propertyCode: null,
        bookingData: null,     // sessionStorage에서 로드
        paymentMethod: 'CARD',
        submitting: false,

        init: function() {
            this.propertyCode = $('#propertyCode').val();

            // sessionStorage에서 예약 데이터 로드
            var stored = sessionStorage.getItem('hola_booking');
            if (!stored) {
                $('#noDataAlert').removeClass('d-none');
                return;
            }

            this.bookingData = JSON.parse(stored);

            // propertyCode 불일치 검증
            if (this.bookingData.propertyCode !== this.propertyCode) {
                $('#noDataAlert').removeClass('d-none');
                return;
            }

            this.renderSummary();
            this.verifyPrice();
            this.bindEvents();
            $('#checkoutContent').show();
        },

        /** price-check API로 최종 가격 확인 */
        verifyPrice: function() {
            var data = this.bookingData;
            var room = data.rooms[0];
            var self = this;

            api({
                url: API_BASE + '/properties/' + this.propertyCode + '/price-check',
                method: 'POST',
                data: JSON.stringify({
                    roomTypeId: room.roomTypeId,
                    rateCodeId: room.rateCodeId,
                    checkIn: data.checkIn,
                    checkOut: data.checkOut,
                    adults: data.adults,
                    children: data.children
                })
            }).done(function(res) {
                if (res.data && res.data.grandTotal) {
                    var verifiedTotal = Number(res.data.grandTotal);
                    var originalTotal = Number(room.totalAmount);
                    // 가격 변동 시 업데이트
                    if (verifiedTotal !== originalTotal) {
                        room.totalAmount = verifiedTotal;
                        data.rooms[0] = room;
                        self.bookingData = data;
                        sessionStorage.setItem('hola_booking', JSON.stringify(data));
                        $('#summaryTotal').text(formatCurrency(verifiedTotal, res.data.currency || room.currency));
                        showError('요금이 변동되었습니다. 변경된 금액을 확인해주세요: ' + formatCurrency(verifiedTotal, res.data.currency));
                    }
                }
            });
            // price-check 실패 시 기존 금액으로 진행 (비차단)
        },

        /** 예약 요약 렌더링 */
        renderSummary: function() {
            var data = this.bookingData;
            var room = data.rooms[0];
            var nights = calcNights(data.checkIn, data.checkOut);

            // 객실 정보
            $('#summaryRoomInfo').html(
                '<div class="p-2 rounded" style="background:rgba(5,130,202,0.05);">'
                + '<div class="fw-bold">' + escapeHtml(room.roomClassName) + '</div>'
                + '<div class="small text-muted">' + escapeHtml(room.rateNameKo) + '</div>'
                + '</div>'
            );

            $('#summaryCheckIn').text(formatDate(data.checkIn));
            $('#summaryCheckOut').text(formatDate(data.checkOut));
            $('#summaryNights').text(nights + '박 ' + (nights + 1) + '일');

            var guestsText = '성인 ' + data.adults + '명';
            if (data.children > 0) guestsText += ', 아동 ' + data.children + '명';
            $('#summaryGuests').text(guestsText);

            $('#summaryTotal').text(formatCurrency(room.totalAmount, room.currency));
        },

        /** 이벤트 바인딩 */
        bindEvents: function() {
            var self = this;

            // 결제 수단 선택
            $('.payment-method-option').on('click', function() {
                $('.payment-method-option').removeClass('selected');
                $(this).addClass('selected');
                self.paymentMethod = $(this).data('method');

                if (self.paymentMethod === 'CARD') {
                    $('#cardFields').slideDown(200);
                } else {
                    $('#cardFields').slideUp(200);
                }
            });

            // 전체 동의
            $('#agreeAll').on('change', function() {
                var checked = $(this).is(':checked');
                $('.agree-item').prop('checked', checked);
                self.updateSubmitButton();
            });

            // 개별 동의 → 전체 동의 연동
            $('.agree-item').on('change', function() {
                var allChecked = $('.agree-item:required').length === $('.agree-item:required:checked').length;
                // agreeAll은 선택 포함 전체
                var totalAll = $('.agree-item').length === $('.agree-item:checked').length;
                $('#agreeAll').prop('checked', totalAll);
                self.updateSubmitButton();
            });

            // 필수 입력 필드 변경 감지
            $('#guestNameKo, #phoneNumber, #email').on('input', function() {
                self.updateSubmitButton();
            });

            // 예약 완료 버튼
            $('#btnSubmitBooking').on('click', function() {
                self.submitBooking();
            });
        },

        /** 예약 버튼 활성화 조건 확인 */
        updateSubmitButton: function() {
            var guestValid = $('#guestNameKo').val().trim() !== ''
                && $('#phoneNumber').val().trim() !== ''
                && $('#email').val().trim() !== '';
            var termsValid = $('#agreeTerms').is(':checked') && $('#agreePrivacy').is(':checked');

            $('#btnSubmitBooking').prop('disabled', !(guestValid && termsValid));
        },

        /** 예약 생성 API 호출 */
        submitBooking: function() {
            if (this.submitting) return;
            hideError();

            // 최종 유효성 검증
            var guestNameKo = $('#guestNameKo').val().trim();
            var phoneNumber = $('#phoneNumber').val().trim();
            var email = $('#email').val().trim();

            if (!guestNameKo) { showError('성명(한글)을 입력해주세요.'); return; }
            if (!phoneNumber) { showError('휴대전화를 입력해주세요.'); return; }
            if (!email) { showError('이메일을 입력해주세요.'); return; }
            if (!$('#agreeTerms').is(':checked') || !$('#agreePrivacy').is(':checked')) {
                showError('필수 약관에 동의해주세요.');
                return;
            }

            var data = this.bookingData;
            var room = data.rooms[0];

            // BookingCreateRequest 생성
            var requestBody = {
                idempotencyKey: this.generateUUID(),
                guest: {
                    guestNameKo: guestNameKo,
                    guestFirstNameEn: $('#guestFirstNameEn').val().trim() || null,
                    guestLastNameEn: $('#guestLastNameEn').val().trim() || null,
                    phoneCountryCode: $('#phoneCountryCode').val(),
                    phoneNumber: phoneNumber,
                    email: email,
                    nationality: $('#nationality').val()
                },
                rooms: [{
                    roomTypeId: room.roomTypeId,
                    rateCodeId: room.rateCodeId,
                    checkIn: data.checkIn,
                    checkOut: data.checkOut,
                    adults: data.adults,
                    children: data.children
                }],
                payment: {
                    method: this.paymentMethod,
                    cardNumber: this.paymentMethod === 'CARD' ? ($('#cardNumber').val() || null) : null,
                    expiryDate: this.paymentMethod === 'CARD' ? ($('#expiryDate').val() || null) : null,
                    cvv: this.paymentMethod === 'CARD' ? ($('#cvv').val() || null) : null
                },
                agreedTerms: true
            };

            this.submitting = true;
            var $btn = $('#btnSubmitBooking');
            $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span> 예약 처리 중...');

            var self = this;
            api({
                url: API_BASE + '/properties/' + this.propertyCode + '/reservations',
                method: 'POST',
                data: JSON.stringify(requestBody)
            }).done(function(res) {
                if (res.data) {
                    // 예약 성공 → 확인 페이지 이동
                    sessionStorage.removeItem('hola_booking');
                    sessionStorage.setItem('hola_booking_confirmation', JSON.stringify(res.data));
                    window.location.href = '/booking/' + self.propertyCode
                        + '/confirmation/' + res.data.confirmationNo;
                }
            }).fail(function() {
                self.submitting = false;
                $btn.prop('disabled', false).html('<i class="fas fa-lock me-1"></i> 예약 완료');
            });
        },

        /** UUID v4 생성 */
        generateUUID: function() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random() * 16 | 0;
                var v = c === 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
        }
    };

    // ─── 예약 확인 페이지 (confirmation.html) ───

    var ConfirmationPage = {
        propertyCode: null,
        confirmationNo: null,

        init: function() {
            this.propertyCode = $('#propertyCode').val();
            this.confirmationNo = $('#confirmationNo').val();

            if (!this.confirmationNo) {
                showError('예약 확인번호가 없습니다.');
                return;
            }

            this.bindEvents();

            // sessionStorage에서 방금 완료된 예약 데이터 로드
            var stored = sessionStorage.getItem('hola_booking_confirmation');
            if (stored) {
                var data = JSON.parse(stored);
                if (data.confirmationNo === this.confirmationNo) {
                    sessionStorage.removeItem('hola_booking_confirmation');
                    this.renderConfirmation(data);
                    return;
                }
            }

            // sessionStorage에 없으면 이메일 인증 폼 표시
            $('#verifySection').show();
        },

        /** 이벤트 바인딩 */
        bindEvents: function() {
            var self = this;

            $('#btnVerify').on('click', function() {
                self.verifyAndLoad();
            });

            // 엔터키로 조회
            $('#verifyEmail').on('keypress', function(e) {
                if (e.which === 13) self.verifyAndLoad();
            });
        },

        /** 이메일 인증 후 예약 조회 */
        verifyAndLoad: function() {
            var email = $('#verifyEmail').val().trim();
            if (!email) {
                showError('이메일을 입력해주세요.');
                return;
            }
            hideError();
            $('#verifySection').hide();
            $('#confirmLoading').show();
            this.loadConfirmation(email);
        },

        /** API로 예약 확인 정보 로드 (email 파라미터 포함) */
        loadConfirmation: function(email) {
            var self = this;
            api({
                url: API_BASE + '/confirmation/' + this.confirmationNo,
                method: 'GET',
                data: { email: email }
            }).done(function(res) {
                if (res.data) {
                    self.renderConfirmation(res.data);
                }
            }).fail(function() {
                $('#confirmLoading').hide();
                $('#verifySection').show();
            });
        },

        /** 확인 정보 렌더링 */
        renderConfirmation: function(data) {
            $('#confirmLoading').hide();

            // 확인번호
            $('#confirmNo').text(data.confirmationNo || '-');

            // 예약 정보
            $('#cfmReservationNo').text(data.masterReservationNo || '-');
            $('#cfmGuestName').text(data.guestNameKo || '-');
            $('#cfmPropertyName').text(data.propertyName || '-');
            $('#cfmPropertyAddress').text(data.propertyAddress || '-');
            $('#cfmPropertyPhone').text(data.propertyPhone || '-');

            // 예약 상태 뱃지
            var statusMap = {
                'RESERVED': { label: '예약확정', cls: 'bg-primary' },
                'CHECK_IN': { label: '체크인', cls: 'bg-success' },
                'INHOUSE': { label: '투숙중', cls: 'bg-info' },
                'CHECKED_OUT': { label: '체크아웃', cls: 'bg-secondary' },
                'CANCELED': { label: '취소', cls: 'bg-danger' },
                'NO_SHOW': { label: '노쇼', cls: 'bg-warning text-dark' }
            };
            var status = statusMap[data.reservationStatus] || { label: data.reservationStatus, cls: 'bg-secondary' };
            $('#cfmStatus').html('<span class="badge ' + status.cls + '">' + status.label + '</span>');

            // 객실 정보
            var roomsHtml = '';
            if (data.rooms && data.rooms.length > 0) {
                $.each(data.rooms, function(i, room) {
                    roomsHtml += '<div class="confirm-detail-row">'
                        + '<span class="confirm-label">' + escapeHtml(room.roomTypeName) + '</span>'
                        + '<span class="confirm-value">'
                        + formatDate(room.checkIn) + ' ~ ' + formatDate(room.checkOut)
                        + ' (' + room.nights + '박)'
                        + '</span>'
                        + '</div>'
                        + '<div class="confirm-detail-row">'
                        + '<span class="confirm-label">인원</span>'
                        + '<span class="confirm-value">성인 ' + room.adults + '명'
                        + (room.children > 0 ? ', 아동 ' + room.children + '명' : '')
                        + '</span>'
                        + '</div>'
                        + '<div class="confirm-detail-row">'
                        + '<span class="confirm-label">객실 요금</span>'
                        + '<span class="confirm-value">' + formatCurrency(room.roomTotal, data.currency) + '</span>'
                        + '</div>';
                    if (i < data.rooms.length - 1) {
                        roomsHtml += '<hr class="my-2">';
                    }
                });
            }
            $('#cfmRooms').html(roomsHtml);

            // 결제 정보
            $('#cfmTotalAmount').text(formatCurrency(data.totalAmount, data.currency));

            var payStatusMap = {
                'PAID': { label: '결제완료', cls: 'bg-success' },
                'UNPAID': { label: '미결제', cls: 'bg-warning text-dark' },
                'PARTIAL': { label: '부분결제', cls: 'bg-info' },
                'OVERPAID': { label: '초과결제', cls: 'bg-danger' }
            };
            var payStatus = payStatusMap[data.paymentStatus] || { label: data.paymentStatus || '-', cls: 'bg-secondary' };
            $('#cfmPaymentStatus').html('<span class="badge ' + payStatus.cls + '">' + payStatus.label + '</span>');

            var methodMap = { 'CARD': '신용카드', 'CASH': '현장결제' };
            $('#cfmPaymentMethod').text(methodMap[data.paymentMethod] || data.paymentMethod || '-');

            if (data.approvalNo) {
                $('#cfmApprovalNo').text(data.approvalNo);
                $('#cfmApprovalRow').show();
            }

            // 체크인/아웃 시간
            $('#cfmCheckInTime').text(data.checkInTime || '-');
            $('#cfmCheckOutTime').text(data.checkOutTime || '-');

            // 취소 정책
            if (data.cancellationPolicies && data.cancellationPolicies.length > 0) {
                var $list = $('#cfmPolicyList').empty();
                data.cancellationPolicies.forEach(function(p) {
                    $list.append('<li class="mb-1">' + escapeHtml(p.description) + '</li>');
                });
                $('#cfmPolicySection').show();
            }

            // 페이지 타이틀
            document.title = '예약 완료 - ' + (data.confirmationNo || 'Hola');

            $('#confirmContent').show();
        }
    };

    // ─── 내부 헬퍼 ───

    /** XSS 방지 */
    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    /** 일자별 요금 테이블 HTML */
    function buildDailyPricesHtml(dailyPrices, currency) {
        if (!dailyPrices || dailyPrices.length === 0) return '';

        var html = '<table class="table table-sm table-borderless mb-0" style="font-size:0.8125rem;">';
        html += '<thead><tr class="text-muted"><th>날짜</th><th class="text-end">객실료</th><th class="text-end">세금</th><th class="text-end">봉사료</th><th class="text-end">합계</th></tr></thead>';
        html += '<tbody>';

        $.each(dailyPrices, function(i, dp) {
            html += '<tr>'
                + '<td>' + formatDate(dp.date) + '</td>'
                + '<td class="text-end">' + formatCurrency(dp.supplyPrice, currency) + '</td>'
                + '<td class="text-end">' + formatCurrency(dp.tax, currency) + '</td>'
                + '<td class="text-end">' + formatCurrency(dp.serviceCharge, currency) + '</td>'
                + '<td class="text-end fw-bold">' + formatCurrency(dp.total, currency) + '</td>'
                + '</tr>';
        });

        html += '</tbody></table>';
        return html;
    }

    // ─── 예약 취소 페이지 ───

    var CancellationPage = {
        propertyCode: null,
        confirmationNo: null,
        email: null,

        init: function() {
            this.propertyCode = $('#propertyCode').val();
            this.confirmationNo = $('#confirmationNo').val();

            if (!this.confirmationNo) {
                showError('예약 확인번호가 없습니다.');
                return;
            }

            this.bindEvents();
        },

        bindEvents: function() {
            var self = this;

            $('#btnVerify').on('click', function() {
                self.loadPreview();
            });

            $('#verifyEmail').on('keypress', function(e) {
                if (e.which === 13) self.loadPreview();
            });

            $('#btnBack').on('click', function() {
                history.back();
            });

            $('#btnConfirmCancel').on('click', function() {
                self.confirmCancel();
            });
        },

        /** 취소 수수료 미리보기 로드 */
        loadPreview: function() {
            var email = $('#verifyEmail').val().trim();
            if (!email) {
                showError('이메일을 입력해주세요.');
                return;
            }
            this.email = email;
            hideError();

            var self = this;
            $('#confirmLoading').show();

            api({
                url: API_BASE + '/reservations/' + this.confirmationNo + '/cancel-fee?email=' + encodeURIComponent(email),
                method: 'GET'
            }).done(function(res) {
                $('#confirmLoading').hide();
                var data = res.data;
                self.renderPreview(data);
            }).fail(function() {
                $('#confirmLoading').hide();
            });
        },

        /** 미리보기 렌더링 */
        renderPreview: function(data) {
            $('#verifySection').hide();
            $('#previewSection').show();

            $('#pvConfirmNo').text(escapeHtml(data.confirmationNo));
            $('#pvGuestName').text(escapeHtml(data.guestNameKo));
            $('#pvCheckIn').text(formatDate(data.checkIn));
            $('#pvCheckOut').text(formatDate(data.checkOut));
            $('#pvStatus').html('<span class="badge bg-primary">' + escapeHtml(data.reservationStatus) + '</span>');
            $('#pvFirstNight').text(formatCurrency(data.firstNightAmount));
            $('#pvFeePercent').text(data.cancelFeePercent + '%');
            $('#pvCancelFee').text(formatCurrency(data.cancelFeeAmount));
            $('#pvTotalPaid').text(formatCurrency(data.totalPaidAmount));
            $('#pvRefund').text(formatCurrency(data.refundAmount));
            $('#pvPolicyDesc').text(data.policyDescription || '');
        },

        /** 취소 확인 */
        confirmCancel: function() {
            if (!confirm('정말 예약을 취소하시겠습니까?\n취소 후에는 되돌릴 수 없습니다.')) {
                return;
            }

            var self = this;
            $('#confirmLoading').show();
            $('#btnConfirmCancel').prop('disabled', true);

            api({
                url: API_BASE + '/reservations/' + this.confirmationNo + '/cancel',
                method: 'POST',
                data: JSON.stringify({ email: this.email })
            }).done(function(res) {
                $('#confirmLoading').hide();
                var data = res.data;
                self.renderComplete(data);
            }).fail(function() {
                $('#confirmLoading').hide();
                $('#btnConfirmCancel').prop('disabled', false);
            });
        },

        /** 취소 완료 렌더링 */
        renderComplete: function(data) {
            $('#previewSection').hide();
            $('#completeSection').show();

            $('#cpConfirmNo').text(escapeHtml(data.confirmationNo));
            $('#cpCancelFee').text(formatCurrency(data.cancelFeeAmount));
            $('#cpRefund').text(formatCurrency(data.refundAmount));
        }
    };

    // ─── 공개 API ───

    return {
        SearchPage: SearchPage,
        RoomsPage: RoomsPage,
        CheckoutPage: CheckoutPage,
        ConfirmationPage: ConfirmationPage,
        CancellationPage: CancellationPage,
        api: api,
        showError: showError,
        hideError: hideError,
        formatCurrency: formatCurrency,
        formatDate: formatDate,
        calcNights: calcNights,
        showSpinner: showSpinner,
        getQueryParam: getQueryParam,
        escapeHtml: escapeHtml
    };

})();
