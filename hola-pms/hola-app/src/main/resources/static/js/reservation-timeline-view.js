/**
 * 예약 관리 - Room Timeline (Tape Chart) 뷰
 * 90일 와이드 버퍼: 자유로운 좌우 패닝, 엣지 도달 극히 드묾
 * 객실 컬럼 sticky 고정 + 상단 헤더 sticky
 */
var ReservationTimelineView = {
    propertyId: null,
    viewDate: null,       // 로드된 90일 윈도우의 시작일
    data: null,
    collapsedFloors: {},
    unassignedCollapsed: true,

    DAYS: 120,            // 한 번에 로드하는 총 일수
    BUFFER: 30,           // viewDate 기준 과거 버퍼
    SHIFT: 30,            // Prev/Next 및 엣지 이동 단위
    ROW_H: 48,
    FLOOR_H: 36,
    HDR_H: 56,
    ROOM_W: 152,

    _drag: { active: false, startX: 0, scrollLeft: 0, moved: false },
    _onDocMouseMove: null,
    _onDocMouseUp: null,
    _loading: false,
    _pendingScrollLeft: undefined,
    _pendingScrollDays: undefined,   // day offset 기반 스크롤 (render 시 정확한 dayWidth로 변환)
    _pendingScrollTop: undefined,
    _cache: {},
    _currentParams: null,   // 현재 적용된 필터 파라미터 (status, keyword)

    /* ─── Public API ──────────────────────────── */

    init: function(propertyId) {
        this.propertyId = propertyId;
        this.collapsedFloors = {};
        this.unassignedCollapsed = true;
        this._currentParams = null;
        this.loadToday();
    },

    /**
     * 오늘 기준으로 리셋 후 로드 (뷰 전환, 프로퍼티 변경, 팝업 복귀 등)
     */
    loadToday: function(params) {
        this._cache = {};
        var today = this.getToday();
        this.viewDate = this.addDays(today, -this.BUFFER);
        // day offset으로 저장 → render() 시 정확한 dayWidth로 변환 (hidden 컨테이너 버그 방지)
        this._pendingScrollDays = this.BUFFER - 3;
        this._pendingScrollLeft = undefined;
        this.load(params);
    },

    load: function(params) {
        var self = this;
        if (!this.propertyId) return;

        // params 전달 시 저장, 미전달 시 이전 필터 유지
        if (params !== undefined) {
            this._currentParams = params;
        } else {
            params = this._currentParams;
        }

        var startStr = this.formatDate(this.viewDate);
        var endDate = this.addDays(this.viewDate, this.DAYS);
        var endStr = this.formatDate(endDate);
        var cacheKey = startStr + '~' + endStr;

        // 세션에 현재 보이는 날짜 저장 (viewDate + BUFFER = 실제 화면의 시작일)
        var visibleDate = this.addDays(this.viewDate, this.BUFFER);
        sessionStorage.setItem('tl_view', this.formatDate(visibleDate));

        // 스크롤 타겟을 클로저로 캡처 (AJAX 이중 호출 race condition 방지)
        var capturedScrollDays = this._pendingScrollDays;
        var capturedScrollLeft = this._pendingScrollLeft;
        this._pendingScrollDays = undefined;
        this._pendingScrollLeft = undefined;

        var hasFilter = params && (params.status || params.keyword);
        if (!hasFilter && this._cache[cacheKey]) {
            this.data = this._cache[cacheKey];
            this._pendingScrollDays = capturedScrollDays;
            this._pendingScrollLeft = capturedScrollLeft;
            this.render();
            return;
        }

        this._loading = true;
        this.showLoading(true);

        var q = ['startDate=' + startStr, 'endDate=' + endStr];
        if (params && params.status) q.push('status=' + params.status);
        if (params && params.keyword) q.push('keyword=' + encodeURIComponent(params.keyword));

        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/reservations/timeline?' + q.join('&'),
            type: 'GET',
            success: function(res) {
                self._loading = false;
                self.data = res.data || { rooms: [], unassigned: [] };
                if (!hasFilter) self._cache[cacheKey] = self.data;
                self._pendingScrollDays = capturedScrollDays;
                self._pendingScrollLeft = capturedScrollLeft;
                self.render();
            },
            error: function() {
                self._loading = false;
                self.data = { rooms: [], unassigned: [] };
                self._pendingScrollDays = capturedScrollDays;
                self._pendingScrollLeft = capturedScrollLeft;
                self.render();
            }
        });
    },

    searchAndNavigate: function(p) {
        var self = this;
        if (!self.propertyId) return;
        if (!p || !p.keyword) { self.load(p); return; }
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/reservations?keyword=' + encodeURIComponent(p.keyword) + (p.status ? '&status=' + p.status : ''),
            type: 'GET',
            success: function(r) {
                var l = r.data || [];
                if (l.length > 0 && l[0].masterCheckIn) {
                    var target = self.parseDate(l[0].masterCheckIn);
                    self.viewDate = self.addDays(target, -self.BUFFER);
                    self._pendingScrollDays = self.BUFFER;
                    self._pendingScrollLeft = undefined;
                }
                self.load(p);
            },
            error: function() { self.load(p); }
        });
    },

    getCurrentRange: function() {
        return { viewDate: this.viewDate, days: this.DAYS };
    },

    calcDayWidth: function() {
        var $c = $('#timelineViewContainer');
        var cw = $c.width() || window.innerWidth - 280;
        var avail = cw - this.ROOM_W - 2;
        var dw = Math.ceil(avail / 22);
        return Math.max(52, dw);
    },

    showLoading: function(show) {
        var el = document.getElementById('tlLoading');
        if (el) el.style.display = show ? 'block' : 'none';
    },

    /* ─── Rendering ──────────────────────────── */

    render: function() {
        var dayWidth = this.calcDayWidth();
        var z = { dayWidth: dayWidth, days: this.DAYS };
        var dates = [];
        for (var i = 0; i < z.days; i++) dates.push(this.addDays(this.viewDate, i));
        var gridW = dates.length * z.dayWidth;
        var totalW = this.ROOM_W + gridW;
        var h = '';

        h += this.buildToolbar();
        h += '<div class="tl-card" style="position:relative;">';
        h += '<div class="tl-loading-indicator" id="tlLoading" style="display:none;"></div>';
        h += '<div class="tl-viewport" id="tlViewport">';
        h += '<div class="tl-canvas" style="width:' + totalW + 'px;">';
        h += this.buildHeader(dates, z);
        h += this.buildUnassigned(dates, z, gridW);

        var groups = this.groupByFloor(this.data.rooms || []);
        for (var fi = 0; fi < groups.length; fi++) {
            h += this.buildFloorGroup(groups[fi], dates, z, gridW);
        }

        if ((!this.data.rooms || this.data.rooms.length === 0) &&
            (!this.data.unassigned || this.data.unassigned.length === 0)) {
            h += '<div class="tl-empty"><i class="fas fa-calendar-alt"></i><span>등록된 객실이 없습니다</span></div>';
        }

        h += '</div></div></div>';
        h += '<div class="tl-tooltip" id="tlTooltip"></div>';

        var $container = $('#timelineViewContainer');
        $container[0].innerHTML = h;
        $container.closest('main').css('min-width', '0');

        // 스크롤 위치 복원 (day offset 우선, pixel 보조)
        var vp = document.getElementById('tlViewport');
        if (vp && this._pendingScrollDays !== undefined) {
            vp.scrollLeft = Math.max(0, Math.min(this._pendingScrollDays * dayWidth, vp.scrollWidth - vp.clientWidth));
            this._pendingScrollDays = undefined;
            this._pendingScrollLeft = undefined;
        } else if (vp && this._pendingScrollLeft !== undefined) {
            vp.scrollLeft = Math.max(0, Math.min(this._pendingScrollLeft, vp.scrollWidth - vp.clientWidth));
            this._pendingScrollLeft = undefined;
        }
        if (vp && this._pendingScrollTop !== undefined) {
            vp.scrollTop = this._pendingScrollTop;
            this._pendingScrollTop = undefined;
        }

        this.bindEvents();
        this.bindGridInteractions();

        // 초기 기간 레이블 갱신
        this.updatePeriodLabel();
    },

    buildToolbar: function() {
        var h = '<div class="tl-toolbar">';
        h += '<div class="tl-nav">';
        h += '<button class="tl-nav-btn" id="tlPrevBtn" title="이전 30일" aria-label="이전 30일"><i class="fas fa-chevron-left"></i></button>';
        h += '<button class="tl-nav-btn tl-nav-today" id="tlTodayBtn">오늘</button>';
        h += '<button class="tl-nav-btn" id="tlNextBtn" title="다음 30일" aria-label="다음 30일"><i class="fas fa-chevron-right"></i></button>';
        h += '<span class="tl-period" id="tlPeriod"></span>';
        h += '</div>';
        h += '<div class="tl-controls">';
        h += '<div class="tl-btn-group">';
        h += '<button class="tl-ctrl-btn" id="tlExpandAll" title="전체 펼치기" aria-label="전체 펼치기"><i class="fas fa-expand-alt"></i></button>';
        h += '<button class="tl-ctrl-btn" id="tlCollapseAll" title="전체 접기" aria-label="전체 접기"><i class="fas fa-compress-alt"></i></button>';
        h += '</div>';
        h += '<div class="tl-legend">';
        var leg = [['예약','RESERVED'],['투숙중','INHOUSE'],['체크아웃','CHECKED_OUT'],['취소','CANCELED'],['노쇼','NO_SHOW']];
        for (var i = 0; i < leg.length; i++) {
            var sc = this.getStatusColor(leg[i][1]);
            h += '<span class="tl-legend-item"><span class="tl-legend-dot" style="background:' + sc.bg + ';border-color:' + sc.border + ';"></span>' + leg[i][0] + '</span>';
        }
        h += '</div></div></div>';
        return h;
    },

    buildHeader: function(dates, z) {
        var months = this.groupDatesByMonth(dates);
        var h = '<div class="tl-month-row">';
        h += '<div class="tl-corner tl-corner--month"></div>';
        for (var mi = 0; mi < months.length; mi++) {
            var m = months[mi];
            h += '<div class="tl-month-span" style="width:' + (m.count * z.dayWidth) + 'px;">';
            h += m.year + '년 ' + m.month + '월</div>';
        }
        h += '</div>';

        h += '<div class="tl-header-row" style="height:' + this.HDR_H + 'px;">';
        h += '<div class="tl-corner"><span>객실</span></div>';
        for (var i = 0; i < dates.length; i++) {
            var d = dates[i];
            var cls = 'tl-date-cell';
            if (this.isToday(d)) cls += ' tl-date-today';
            if (d.getDay() === 0) cls += ' tl-date-sun';
            if (d.getDay() === 6) cls += ' tl-date-sat';
            h += '<div class="' + cls + '" style="width:' + z.dayWidth + 'px;">';
            h += '<span class="tl-day-num' + (this.isToday(d) ? ' tl-today-circle' : '') + '">' + d.getDate() + '</span>';
            h += '<span class="tl-day-name">' + this.getDayLabel(d) + '</span>';
            h += '</div>';
        }
        h += '</div>';
        return h;
    },

    groupDatesByMonth: function(dates) {
        var result = [];
        var cur = null;
        for (var i = 0; i < dates.length; i++) {
            var d = dates[i];
            var key = d.getFullYear() + '-' + (d.getMonth() + 1);
            if (!cur || cur.key !== key) {
                cur = { key: key, year: d.getFullYear(), month: d.getMonth() + 1, count: 0 };
                result.push(cur);
            }
            cur.count++;
        }
        return result;
    },

    LANE_H: 32,          // 미배정 레인 1개 높이

    /**
     * 미배정 예약을 객실타입별 타임라인 행으로 렌더링
     */
    buildUnassigned: function(dates, z, gridW) {
        var list = this.data.unassigned || [];
        if (list.length === 0) return '';
        var isC = this.unassignedCollapsed;
        var h = '';

        // 객실타입별 그룹핑
        var groups = {}, order = [];
        for (var i = 0; i < list.length; i++) {
            var typeName = list[i].roomTypeName || '미정';
            if (!groups[typeName]) { groups[typeName] = []; order.push(typeName); }
            groups[typeName].push(list[i]);
        }

        // 층 헤더
        h += '<div class="tl-floor-row" data-floor="__unassigned__">';
        h += '<div class="tl-floor-cell tl-floor-cell--warn">';
        h += '<i class="fas fa-chevron-down tl-chevron' + (isC ? ' tl-chevron-collapsed' : '') + '"></i>';
        h += '<span class="tl-floor-name tl-floor-name--warn">미배정</span>';
        h += '<span class="tl-floor-badge tl-floor-badge--warn">' + list.length + '</span>';
        h += '</div><div class="tl-floor-line"></div></div>';

        // 객실타입별 타임라인 행
        for (var gi = 0; gi < order.length; gi++) {
            var typeName = order[gi];
            var typeList = groups[typeName];
            var totalLanes = this.calculateLanes(typeList);
            var rowH = Math.max(this.ROW_H, totalLanes * this.LANE_H);

            h += '<div class="tl-data-row tl-data-row--unassigned" data-floor="__unassigned__"';
            h += ' style="height:' + rowH + 'px;';
            if (isC) h += 'display:none;';
            h += '">';

            // 객실타입 셀
            h += '<div class="tl-room-cell tl-room-cell--unassigned">';
            h += '<span class="tl-room-num tl-room-num--unassigned">' + HolaPms.escapeHtml(typeName) + '</span>';
            h += '<span class="tl-room-type">' + typeList.length + '건</span>';
            h += '</div>';

            // 타임라인 그리드
            h += '<div class="tl-timeline">';
            for (var di = 0; di < dates.length; di++) {
                var d = dates[di], x = di * z.dayWidth;
                var cls = 'tl-gcell';
                if (this.isToday(d)) cls += ' tl-gcell-today';
                else if (d.getDay() === 0 || d.getDay() === 6) cls += ' tl-gcell-weekend';
                h += '<div class="' + cls + '" style="left:' + x + 'px;width:' + z.dayWidth + 'px;"></div>';
            }
            var tI = this.daysBetween(this.viewDate, this.getToday());
            if (tI >= 0 && tI < dates.length) {
                h += '<div class="tl-today-line" style="left:' + (tI * z.dayWidth) + 'px;"></div>';
            }
            for (var ri = 0; ri < typeList.length; ri++) {
                h += this.renderLanedBar(typeList[ri], dates, z, typeList[ri]._lane);
            }
            h += '</div></div>';
        }

        return h;
    },

    /**
     * 겹치는 예약의 레인 배치 계산 (greedy 알고리즘)
     * 각 예약에 _lane 프로퍼티 부여, 총 레인 수 반환
     */
    calculateLanes: function(reservations) {
        var sorted = reservations.slice().sort(function(a, b) {
            return (a.masterCheckIn || '').localeCompare(b.masterCheckIn || '');
        });
        var laneEnds = [];
        for (var i = 0; i < sorted.length; i++) {
            var rsv = sorted[i];
            var rsvEnd = (rsv.stayType === 'DAY_USE') ? rsv.masterCheckIn : rsv.masterCheckOut;
            var placed = false;
            for (var li = 0; li < laneEnds.length; li++) {
                if (rsv.masterCheckIn >= laneEnds[li]) {
                    laneEnds[li] = rsvEnd;
                    rsv._lane = li;
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                rsv._lane = laneEnds.length;
                laneEnds.push(rsvEnd);
            }
        }
        return Math.max(1, laneEnds.length);
    },

    /**
     * 레인 기반 예약 바 렌더링 (미배정 타임라인 전용)
     */
    renderLanedBar: function(rsv, dates, z, lane) {
        return this.renderBar(rsv, dates, z, lane);
    },

    buildFloorGroup: function(group, dates, z, gridW) {
        var fk = group.floorName;
        var esc = HolaPms.escapeHtml(fk);
        var isC = this.collapsedFloors[fk] === true;
        var h = '';

        h += '<div class="tl-floor-row" data-floor="' + esc + '">';
        h += '<div class="tl-floor-cell">';
        h += '<i class="fas fa-chevron-down tl-chevron' + (isC ? ' tl-chevron-collapsed' : '') + '"></i>';
        h += '<span class="tl-floor-name">' + esc + '</span>';
        h += '<span class="tl-floor-badge">' + group.rooms.length + '실</span>';
        h += '</div><div class="tl-floor-line"></div></div>';

        for (var ri = 0; ri < group.rooms.length; ri++) {
            h += this.buildRoomRow(group.rooms[ri], dates, z, fk, isC);
        }
        return h;
    },

    buildRoomRow: function(room, dates, z, floorKey, hidden) {
        var h = '<div class="tl-data-row" data-floor="' + HolaPms.escapeHtml(floorKey) + '" data-room-id="' + room.roomId + '"';
        if (hidden) h += ' style="display:none;"';
        h += '>';

        h += '<div class="tl-room-cell">';
        h += '<span class="tl-room-num">' + HolaPms.escapeHtml(room.roomNumber) + '</span>';
        h += '<span class="tl-room-type">' + HolaPms.escapeHtml(room.roomTypeName || '') + '</span>';
        h += '</div>';

        h += '<div class="tl-timeline">';
        for (var di = 0; di < dates.length; di++) {
            var d = dates[di], x = di * z.dayWidth;
            var cls = 'tl-gcell';
            if (this.isToday(d)) cls += ' tl-gcell-today';
            else if (d.getDay() === 0 || d.getDay() === 6) cls += ' tl-gcell-weekend';
            h += '<div class="' + cls + '" style="left:' + x + 'px;width:' + z.dayWidth + 'px;"></div>';
        }

        var tI = this.daysBetween(this.viewDate, this.getToday());
        if (tI >= 0 && tI < dates.length) {
            h += '<div class="tl-today-line" style="left:' + (tI * z.dayWidth) + 'px;"></div>';
        }

        if (room.reservations) {
            for (var bi = 0; bi < room.reservations.length; bi++) {
                h += this.renderBar(room.reservations[bi], dates, z);
            }
        }

        h += '</div></div>';
        return h;
    },

    /**
     * 예약 바 렌더링 (배정 객실 + 미배정 레인 공용)
     * @param lane - null이면 배정 객실(top/height 생략), 숫자면 미배정 레인 위치 적용
     */
    renderBar: function(rsv, dates, z, lane) {
        var viewStart = this.formatDate(this.viewDate);
        var viewEnd = this.formatDate(this.addDays(this.viewDate, dates.length));
        var ci = rsv.masterCheckIn;
        var co = (rsv.stayType === 'DAY_USE') ? ci : rsv.masterCheckOut;
        if (ci >= viewEnd || co < viewStart) return '';

        var bs = ci < viewStart ? viewStart : ci;
        var be = co > viewEnd ? viewEnd : co;
        var sIdx = this.daysBetween(this.viewDate, this.parseDate(bs));
        var eIdx = this.daysBetween(this.viewDate, this.parseDate(be));
        // 체크아웃 당일 컬럼 포함 (호텔 테이프차트 표준)
        if (co <= viewEnd) eIdx += 1;
        if (sIdx < 0) sIdx = 0;
        if (eIdx > dates.length) eIdx = dates.length;
        if (eIdx <= sIdx) return '';

        var left = sIdx * z.dayWidth + 2;
        var width = (eIdx - sIdx) * z.dayWidth - 4;
        var span = eIdx - sIdx;
        var name = HolaPms.escapeHtml(rsv.guestNameMasked || '-');
        var resNo = rsv.masterReservationNo ? HolaPms.escapeHtml(rsv.masterReservationNo) : '';
        var statusLabel = this.getStatusLabel(rsv.reservationStatus);
        var statusClass = rsv.reservationStatus.toLowerCase().replace(/_/g, '-');

        var isFirst = ci >= viewStart, isLast = co <= viewEnd;
        var rad = '6px';
        if (isFirst && !isLast) rad = '6px 2px 2px 6px';
        else if (!isFirst && isLast) rad = '2px 6px 6px 2px';
        else if (!isFirst && !isLast) rad = '2px';

        var style = 'left:' + left + 'px;width:' + width + 'px;border-radius:' + rad + ';';
        if (lane != null) {
            style += 'top:' + (lane * this.LANE_H + 4) + 'px;height:' + (this.LANE_H - 8) + 'px;';
        }

        var h = '<div class="tl-bar tl-bar--' + statusClass + '" ';
        h += 'style="' + style + '" ';
        h += 'data-status-label="' + HolaPms.escapeHtml(statusLabel) + '" ';
        h += 'data-guest="' + name + '" data-resno="' + resNo + '" ';
        h += 'data-period="' + HolaPms.escapeHtml(ci + ' → ' + co) + '" ';
        h += 'onclick="HolaPms.popup.openReservationDetail(' + rsv.id + ')">';

        if (span >= 2 || z.dayWidth >= 80) {
            h += '<span class="tl-bar-icon">' + this.getStatusIcon(rsv.reservationStatus) + '</span>';
            h += '<span class="tl-bar-text">' + name + '</span>';
            if (span >= 3 && z.dayWidth >= 54) h += '<span class="tl-bar-sub">' + resNo + '</span>';
        } else {
            h += '<span class="tl-bar-icon">' + this.getStatusIcon(rsv.reservationStatus) + '</span>';
        }

        h += '</div>';
        return h;
    },

    /* ─── Events ──────────────────────────────── */

    bindEvents: function() {
        var self = this;
        // Prev/Next: 30일 단위 이동 (90일 버퍼 새로 로드)
        $('#tlPrevBtn').off('click').on('click', function() {
            var vp = document.getElementById('tlViewport');
            self._pendingScrollTop = vp ? vp.scrollTop : 0;
            self.viewDate = self.addDays(self.viewDate, -self.SHIFT);
            self._pendingScrollLeft = self.SHIFT * self.calcDayWidth() + (vp ? vp.scrollLeft : 0);
            self.load();
        });
        $('#tlNextBtn').off('click').on('click', function() {
            var vp = document.getElementById('tlViewport');
            self._pendingScrollTop = vp ? vp.scrollTop : 0;
            self.viewDate = self.addDays(self.viewDate, self.SHIFT);
            self._pendingScrollLeft = Math.max(0, (vp ? vp.scrollLeft : 0) - self.SHIFT * self.calcDayWidth());
            self.load();
        });
        $('#tlTodayBtn').off('click').on('click', function() {
            self.viewDate = self.addDays(self.getToday(), -self.BUFFER);
            self._pendingScrollDays = self.BUFFER;
            self._pendingScrollLeft = undefined;
            self.load();
        });
        $('#tlExpandAll').off('click').on('click', function() { self.toggleAll(false); });
        $('#tlCollapseAll').off('click').on('click', function() { self.toggleAll(true); });
        // 팝업 메시지 리스너: 중복 등록 방지 + 스크롤 위치 보존 + 캐시 클리어
        if (!self._popupListenerBound) {
            HolaPms.popup.onChildMessage(function() {
                var vp = document.getElementById('tlViewport');
                if (vp) {
                    self._pendingScrollLeft = vp.scrollLeft;
                    self._pendingScrollTop = vp.scrollTop;
                }
                self._cache = {};
                self.load();
            });
            self._popupListenerBound = true;
        }
        $(window).off('resize.tl').on('resize.tl', function() {
            if (self._resizeTimer) clearTimeout(self._resizeTimer);
            self._resizeTimer = setTimeout(function() {
                var vp = document.getElementById('tlViewport');
                if (vp) {
                    self._pendingScrollLeft = vp.scrollLeft;
                    self._pendingScrollTop = vp.scrollTop;
                }
                self.render();
            }, 200);
        });
    },

    bindGridInteractions: function() {
        var self = this;
        var viewport = document.getElementById('tlViewport');
        var tooltip = document.getElementById('tlTooltip');
        if (!viewport) return;

        // 층 접기/펼치기
        $(viewport).on('click', '.tl-floor-row', function() {
            var f = $(this).data('floor');
            if (f === undefined || f === '') return;
            self.toggleFloor(String(f));
        });

        // 좌우 드래그 (상하는 네이티브 스크롤)
        viewport.addEventListener('mousedown', function(e) {
            if (e.target.closest('.tl-bar, .tl-floor-row, button, a')) return;
            self._drag.active = true;
            self._drag.moved = false;
            self._drag.startX = e.pageX;
            self._drag.scrollLeft = viewport.scrollLeft;
            viewport.style.cursor = 'grabbing';
            viewport.style.userSelect = 'none';
            e.preventDefault();
        });

        if (this._onDocMouseMove) document.removeEventListener('mousemove', this._onDocMouseMove);
        if (this._onDocMouseUp) document.removeEventListener('mouseup', this._onDocMouseUp);

        this._onDocMouseMove = function(e) {
            if (!self._drag.active) return;
            var dx = e.pageX - self._drag.startX;
            if (Math.abs(dx) > 3) self._drag.moved = true;
            requestAnimationFrame(function() {
                viewport.scrollLeft = self._drag.scrollLeft - dx;
            });
        };
        this._onDocMouseUp = function() {
            if (self._drag.active) {
                self._drag.active = false;
                viewport.style.cursor = '';
                viewport.style.userSelect = '';
            }
        };
        document.addEventListener('mousemove', this._onDocMouseMove);
        document.addEventListener('mouseup', this._onDocMouseUp);

        // 스크롤 시 기간 레이블 실시간 갱신 + 엣지 안전망
        viewport.addEventListener('scroll', function() {
            self.updatePeriodLabel();
            // 엣지 안전망 (67일 스크롤 소진 시에만 트리거 - 극히 드묾)
            if (self._edgeTimer) clearTimeout(self._edgeTimer);
            self._edgeTimer = setTimeout(function() {
                if (self._loading || self._drag.active) return;
                var maxSL = viewport.scrollWidth - viewport.clientWidth;
                var dw = self.calcDayWidth();
                if (maxSL > 0 && viewport.scrollLeft >= maxSL - dw) {
                    self._pendingScrollTop = viewport.scrollTop;
                    self.viewDate = self.addDays(self.viewDate, self.SHIFT);
                    self._pendingScrollLeft = viewport.scrollLeft - (self.SHIFT * dw);
                    self.load();
                } else if (viewport.scrollLeft <= dw) {
                    self._pendingScrollTop = viewport.scrollTop;
                    self.viewDate = self.addDays(self.viewDate, -self.SHIFT);
                    self._pendingScrollLeft = viewport.scrollLeft + (self.SHIFT * dw);
                    self.load();
                }
            }, 500);
        });

        // 커스텀 툴팁
        $(viewport).on('mouseenter', '.tl-bar', function(e) {
            if (!tooltip) return;
            var $b = $(this);
            var lines = [$b.data('status-label'), $b.data('guest')];
            if ($b.data('resno')) lines.push($b.data('resno'));
            lines.push($b.data('period'));
            tooltip.textContent = lines.join('\n');
            tooltip.classList.add('visible');
            self.positionTooltip(e, tooltip);
        }).on('mousemove', '.tl-bar', function(e) {
            if (tooltip) self.positionTooltip(e, tooltip);
        }).on('mouseleave', '.tl-bar', function() {
            if (tooltip) tooltip.classList.remove('visible');
        });
    },

    positionTooltip: function(e, el) {
        var x = e.clientX + 14, y = e.clientY - 12;
        if (x + 280 > window.innerWidth) x = e.clientX - 280;
        if (y < 8) y = e.clientY + 20;
        el.style.left = x + 'px';
        el.style.top = y + 'px';
    },

    /* ─── 기간 레이블 동적 갱신 ──────────────── */

    updatePeriodLabel: function() {
        var vp = document.getElementById('tlViewport');
        var el = document.getElementById('tlPeriod');
        if (!vp || !el) return;

        var dw = this.calcDayWidth();
        var firstDay = Math.round(vp.scrollLeft / dw);
        var visibleDays = Math.round((vp.clientWidth - this.ROOM_W) / dw);
        var start = this.addDays(this.viewDate, firstDay);
        var end = this.addDays(start, visibleDays - 1);

        // 세션에 현재 보이는 시작일 저장
        sessionStorage.setItem('tl_view', this.formatDate(start));

        el.textContent = this.formatPeriodRange(start, end);
    },

    formatPeriodRange: function(sv, se) {
        if (sv.getFullYear() === se.getFullYear()) {
            if (sv.getMonth() === se.getMonth()) {
                return sv.getFullYear() + '년 ' + (sv.getMonth() + 1) + '월 ' + sv.getDate() + '일 – ' + se.getDate() + '일';
            }
            return sv.getFullYear() + '년 ' + (sv.getMonth() + 1) + '.' + sv.getDate() + ' – ' + (se.getMonth() + 1) + '.' + se.getDate();
        }
        return this.formatDate(sv) + ' – ' + this.formatDate(se);
    },

    shiftView: function(days) {
        this.viewDate = this.addDays(this.viewDate, days);
        this.load();
    },

    /* ─── DOM 기반 접기/펼치기 (re-render 없음) ─── */

    toggleFloor: function(floorKey) {
        if (floorKey === '__unassigned__') {
            this.unassignedCollapsed = !this.unassignedCollapsed;
            var show = !this.unassignedCollapsed;
            $('.tl-data-row--unassigned').toggle(show);
        } else {
            this.collapsedFloors[floorKey] = !this.collapsedFloors[floorKey];
            var show = !this.collapsedFloors[floorKey];
            $('.tl-data-row').each(function() {
                if (String($(this).data('floor')) === floorKey) $(this).toggle(show);
            });
        }
        $('.tl-floor-row').each(function() {
            if (String($(this).data('floor')) === floorKey) {
                $(this).find('.tl-chevron').toggleClass('tl-chevron-collapsed');
            }
        });
    },

    toggleAll: function(collapse) {
        if (!this.data || !this.data.rooms) return;
        var groups = this.groupByFloor(this.data.rooms);
        for (var i = 0; i < groups.length; i++) this.collapsedFloors[groups[i].floorName] = collapse;
        this.unassignedCollapsed = collapse;

        $('.tl-data-row').toggle(!collapse);
        if (collapse) {
            $('.tl-chevron').addClass('tl-chevron-collapsed');
        } else {
            $('.tl-chevron').removeClass('tl-chevron-collapsed');
        }
    },

    /* ─── Utilities ───────────────────────────── */

    groupByFloor: function(rooms) {
        var g = {}, o = [];
        for (var i = 0; i < rooms.length; i++) {
            var f = rooms[i].floorName || '기타';
            if (!g[f]) { g[f] = []; o.push(f); }
            g[f].push(rooms[i]);
        }
        return o.map(function(f) { return { floorName: f, rooms: g[f] }; });
    },

    getStatusColor: function(s) {
        return HolaPms.reservationStatus.viewColor(s);
    },

    getStatusIcon: function(s) {
        var map = {
            'RESERVED': 'fa-clock', 'CHECK_IN': 'fa-sign-in-alt', 'INHOUSE': 'fa-bed',
            'CHECKED_OUT': 'fa-sign-out-alt', 'CANCELED': 'fa-times-circle', 'NO_SHOW': 'fa-ban',
            'DAY_USE': 'fa-sun', 'DAY_OFF': 'fa-moon'
        };
        return map[s] ? '<i class="fas ' + map[s] + '"></i>' : '';
    },

    getStatusLabel: function(s) {
        var map = {
            'RESERVED': '예약', 'CHECK_IN': '체크인', 'INHOUSE': '투숙중',
            'CHECKED_OUT': '체크아웃', 'CANCELED': '취소', 'NO_SHOW': '노쇼',
            'DAY_USE': '대실', 'DAY_OFF': '정비'
        };
        return map[s] || s;
    },

    getToday: function() { var n = new Date(); return new Date(n.getFullYear(), n.getMonth(), n.getDate()); },
    isToday: function(d) { var t = this.getToday(); return d.getFullYear() === t.getFullYear() && d.getMonth() === t.getMonth() && d.getDate() === t.getDate(); },
    addDays: function(d, n) { var r = new Date(d.getTime()); r.setDate(r.getDate() + n); return r; },
    daysBetween: function(a, b) { return Math.floor((b.getTime() - a.getTime()) / 86400000); },
    parseDate: function(s) { if (s instanceof Date) return s; var p = String(s).split('-'); return new Date(+p[0], +p[1] - 1, +p[2]); },
    formatDate: function(d) { return d.getFullYear() + '-' + this.pad(d.getMonth() + 1) + '-' + this.pad(d.getDate()); },
    pad: function(n) { return n < 10 ? '0' + n : '' + n; },
    getDayLabel: function(d) { return ['일', '월', '화', '수', '목', '금', '토'][d.getDay()]; }
};
