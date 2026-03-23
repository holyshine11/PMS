/**
 * 대시보드 페이지
 * 역할별 분기: SUPER_ADMIN → 프로퍼티 랭킹, PROPERTY 선택 시 → KPI + 운영 + 차트
 * 프로퍼티 미선택 시 → 웰컴 상태 (최근 프로퍼티 + 빠른 메뉴)
 */
var Dashboard = {
    pickupChart: null,
    isSuperAdmin: false,
    RECENT_PROPERTIES_KEY: 'hola_recent_properties',
    MAX_RECENT: 5,

    /**
     * 초기화
     */
    init: function() {
        var self = this;
        // 오늘 날짜 표시 (영업일 기준)
        var today = new Date();
        var yy = today.getFullYear();
        var mm = today.getMonth() + 1;
        var dd = today.getDate();
        var days = ['일', '월', '화', '수', '목', '금', '토'];
        var dayIdx = today.getDay();
        var dayName = days[dayIdx];
        var isWeekend = (dayIdx === 0 || dayIdx === 6);
        var dateHtml = '<i class="fas fa-calendar-day me-1"></i>'
            + yy + '년 ' + mm + '월 ' + dd + '일'
            + ' <span class="badge ' + (isWeekend ? 'bg-danger' : 'bg-secondary') + ' bg-opacity-75 ms-1" style="font-size:0.6875rem;font-weight:500;">'
            + dayName + '</span>';
        $('#dashboardDate').html(dateHtml);

        // Bootstrap 툴팁 초기화
        var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.forEach(function(el) { new bootstrap.Tooltip(el); });

        self.bindEvents();
        self.reload();
    },

    /**
     * 이벤트 바인딩
     */
    bindEvents: function() {
        var self = this;
        // 프로퍼티 컨텍스트 변경 시 새로고침
        $(document).on('hola:contextChange', function() {
            self.reload();
        });

        // 최근 프로퍼티 클릭 이벤트 (이벤트 위임)
        $(document).on('click', '.recent-property-item', function(e) {
            e.preventDefault();
            var hotelId = $(this).data('hotel-id');
            var propertyId = $(this).data('property-id');
            if (hotelId && propertyId) {
                self.selectPropertyFromWelcome(String(hotelId), String(propertyId));
            }
        });

        // 랭킹 테이블 행 클릭: 해당 프로퍼티 선택 (SUPER_ADMIN 드릴다운)
        $(document).on('click', '#rankingTable tbody tr[data-property-id]', function() {
            var propertyId = String($(this).data('property-id'));
            var hotelId = String($(this).data('hotel-id'));
            if (!propertyId || !hotelId) return;
            self.selectPropertyFromWelcome(hotelId, propertyId);
        });

        // 빠른 메뉴 클릭: 프로퍼티 미선택 시 최근 프로퍼티 자동 선택 후 이동
        $(document).on('click', '.quick-menu-card[data-require-property]', function(e) {
            if (HolaPms.context.getPropertyId()) return; // 이미 선택됨 → 그냥 이동

            e.preventDefault();
            var targetUrl = $(this).attr('href');
            var recents = self.getRecentProperties();

            if (recents.length > 0) {
                // sessionStorage에 직접 설정 후 즉시 이동 (대상 페이지에서 자동 복원)
                var r = recents[0];
                sessionStorage.setItem('selectedHotelId', String(r.hotelId));
                sessionStorage.setItem('selectedPropertyId', String(r.propertyId));
                window.location.href = targetUrl;
            } else {
                // 최근 프로퍼티 없음 → 프로퍼티 선택 안내
                HolaPms.alert('warning', '상단 헤더에서 프로퍼티를 먼저 선택해 주세요.');
            }
        });
    },

    /**
     * 데이터 로드 분기
     */
    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        if (propertyId) {
            // 프로퍼티 선택됨 → KPI + 운영 + 차트
            $('#welcomeState').addClass('d-none');
            $('#propertyDashboard').removeClass('d-none');
            self.saveRecentProperty();
            self.loadPropertyKpi(propertyId);
            self.loadOperation(propertyId);
            self.loadPickup(propertyId);
        } else {
            // 프로퍼티 미선택
            $('#propertyDashboard').addClass('d-none');
            // SUPER_ADMIN이면 랭킹 테이블이 보이므로 웰컴 숨김
            if ($('#superAdminSummary').length > 0 && $('#superAdminSummary').is(':visible')) {
                self.loadAllKpis();
            } else {
                self.showWelcomeState();
            }
        }

        // SUPER_ADMIN이면 랭킹도 로드
        if ($('#rankingTable').length > 0) {
            self.loadAllKpis();
        }
    },

    // ─── 웰컴 상태 ──────────────────────────────────────

    /**
     * 웰컴 상태 표시 (프로퍼티 미선택 시)
     */
    showWelcomeState: function() {
        var $welcome = $('#welcomeState');
        if ($welcome.length === 0) return;

        $welcome.removeClass('d-none');
        this.renderRecentProperties();
    },

    /**
     * 최근 사용 프로퍼티 목록 렌더링
     */
    renderRecentProperties: function() {
        var recents = this.getRecentProperties();
        var $section = $('#recentPropertiesSection');
        var $list = $('#recentPropertiesList');

        if (recents.length === 0) {
            $section.addClass('d-none');
            return;
        }

        $section.removeClass('d-none');
        var html = '';
        for (var i = 0; i < recents.length; i++) {
            var r = recents[i];
            html += '<a href="#" class="recent-property-item" '
                + 'data-hotel-id="' + HolaPms.escapeHtml(r.hotelId) + '" '
                + 'data-property-id="' + HolaPms.escapeHtml(r.propertyId) + '">'
                + '<div class="property-info">'
                + '<div class="property-icon"><i class="fas fa-building"></i></div>'
                + '<div>'
                + '<div class="property-name">' + HolaPms.escapeHtml(r.propertyName) + '</div>'
                + '<div class="property-hotel">' + HolaPms.escapeHtml(r.hotelName) + '</div>'
                + '</div>'
                + '</div>'
                + '<span class="arrow"><i class="fas fa-chevron-right"></i></span>'
                + '</a>';
        }
        $list.html(html);
    },

    /**
     * 웰컴 상태에서 프로퍼티 선택 시 헤더 드롭다운과 동기화
     */
    selectPropertyFromWelcome: function(hotelId, propertyId) {
        // 헤더 호텔 셀렉트 변경
        var $hotelSelect = $('#headerHotelSelect');
        $hotelSelect.val(hotelId);
        HolaPms.context.selectedHotelId = hotelId;
        sessionStorage.setItem('selectedHotelId', hotelId);

        // 프로퍼티 목록 로드 후 프로퍼티 선택
        HolaPms.ajax({
            url: '/api/v1/properties/selector?hotelId=' + hotelId,
            type: 'GET',
            success: function(res) {
                var $propSelect = $('#headerPropertySelect');
                $propSelect.find('option:not(:first)').remove();
                var properties = res.data || [];
                properties.forEach(function(p) {
                    $propSelect.append('<option value="' + p.id + '">' + HolaPms.escapeHtml(p.propertyName) + '</option>');
                });
                $propSelect.val(propertyId);
                HolaPms.context.selectedPropertyId = propertyId;
                sessionStorage.setItem('selectedPropertyId', propertyId);
                $(document).trigger('hola:contextChange', { hotelId: hotelId, propertyId: propertyId });
            }
        });
    },

    // ─── 최근 프로퍼티 저장/조회 (localStorage) ──────────

    /**
     * 현재 선택된 프로퍼티를 최근 사용 목록에 저장
     */
    saveRecentProperty: function() {
        var hotelId = HolaPms.context.getHotelId();
        var propertyId = HolaPms.context.getPropertyId();
        var hotelName = HolaPms.context.getHotelName();
        var propertyName = HolaPms.context.getPropertyName();

        if (!hotelId || !propertyId || !propertyName) return;

        var recents = this.getRecentProperties();

        // 중복 제거
        recents = recents.filter(function(r) {
            return r.propertyId !== propertyId;
        });

        // 맨 앞에 추가
        recents.unshift({
            hotelId: hotelId,
            propertyId: propertyId,
            hotelName: hotelName,
            propertyName: propertyName,
            timestamp: Date.now()
        });

        // 최대 개수 제한
        if (recents.length > this.MAX_RECENT) {
            recents = recents.slice(0, this.MAX_RECENT);
        }

        try {
            localStorage.setItem(this.RECENT_PROPERTIES_KEY, JSON.stringify(recents));
        } catch (e) { /* localStorage 비가용 시 무시 */ }
    },

    /**
     * 최근 사용 프로퍼티 목록 조회
     */
    getRecentProperties: function() {
        try {
            var stored = localStorage.getItem(this.RECENT_PROPERTIES_KEY);
            if (stored) return JSON.parse(stored);
        } catch (e) { /* 파싱 실패 시 빈 배열 */ }
        return [];
    },

    // ─── KPI / 운영현황 / 차트 ──────────────────────────

    /**
     * 전체 프로퍼티 KPI (SUPER_ADMIN 랭킹)
     */
    loadAllKpis: function() {
        HolaPms.ajax({
            url: '/api/v1/dashboard',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var data = res.data;
                var totalProps = data.length;
                var totalRooms = 0;
                var totalRevenue = 0;
                var totalOcc = 0;

                var html = '';
                for (var i = 0; i < data.length; i++) {
                    var d = data[i];
                    totalRooms += d.totalRooms;
                    totalRevenue += (d.totalRevenue || 0);
                    totalOcc += (d.occupancyRate || 0);
                    var occ = (d.occupancyRate || 0);
                    var barColor = occ < 30 ? '#EF476F' : '#0582CA';

                    html += '<tr style="cursor:pointer" data-property-id="' + d.propertyId + '" data-hotel-id="' + (d.hotelId || '') + '">'
                        + '<td class="ps-3">' + (i + 1) + '</td>'
                        + '<td>' + HolaPms.escapeHtml(d.propertyName || '-') + '</td>'
                        + '<td class="text-center">' + d.soldRooms + '/' + d.totalRooms + '</td>'
                        + '<td>'
                        + '<div class="d-flex align-items-center gap-2">'
                        + '<div class="occ-progress flex-grow-1"><div class="occ-progress-bar" style="width:' + Math.min(occ, 100) + '%;background-color:' + barColor + '"></div></div>'
                        + '<span class="fw-bold" style="min-width:48px;text-align:right;font-size:0.8125rem">' + occ.toFixed(1) + '%</span>'
                        + '</div></td>'
                        + '<td class="text-end">' + Dashboard.formatCurrency(d.adr) + '</td>'
                        + '<td class="text-end">' + Dashboard.formatCurrency(d.revPar) + '</td>'
                        + '<td class="text-end pe-3">' + Dashboard.formatCurrency(d.totalRevenue) + '</td>'
                        + '</tr>';
                }

                if (data.length === 0) {
                    html = '<tr><td colspan="7" class="text-center py-4 text-muted">등록된 프로퍼티가 없습니다.</td></tr>';
                }

                $('#rankingBody').html(html);
                $('#totalProperties').text(totalProps);
                $('#totalRoomsAll').text(totalRooms.toLocaleString());
                var avgOcc = totalProps > 0 ? (totalOcc / totalProps) : 0;
                $('#avgOccupancy').text(avgOcc.toFixed(1) + '%');
                $('#totalRevenueAll').text(Dashboard.formatCurrency(totalRevenue));
            }
        });
    },

    /**
     * 프로퍼티 KPI 로드 (전일 대비 트렌드 포함)
     */
    loadPropertyKpi: function(propertyId) {
        HolaPms.ajax({
            url: '/api/v1/dashboard/property/' + propertyId,
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var d = res.data;
                var occ = d.occupancyRate || 0;

                // 히어로 OCC
                $('#kpiOcc').text(Number(occ).toFixed(1) + '%');
                $('#kpiOccSub').text(d.soldRooms + ' / ' + d.totalRooms + ' 객실');
                // 프로그레스 바
                var $bar = $('#kpiOccBar');
                $bar.css('width', Math.min(occ, 100) + '%');
                if (occ < 30) $bar.addClass('low'); else $bar.removeClass('low');

                // 보조 KPI
                $('#kpiAdr').text(Dashboard.formatCurrency(d.adr));
                $('#kpiRevpar').text(Dashboard.formatCurrency(d.revPar));
                $('#kpiRevenue').text(Dashboard.formatCurrency(d.totalRevenue));

                // 전일 대비 트렌드
                Dashboard.renderTrend('#kpiOccTrend', occ, d.yesterdayOccupancyRate, '%');
                Dashboard.renderTrend('#kpiAdrTrend', d.adr, d.yesterdayAdr, '원');
                Dashboard.renderTrend('#kpiRevparTrend', d.revPar, d.yesterdayRevPar, '원');
                Dashboard.renderTrend('#kpiRevenueTrend', d.totalRevenue, d.yesterdayRevenue, '원');
            }
        });
    },

    /**
     * 전일 대비 트렌드 렌더링
     */
    renderTrend: function(selector, today, yesterday, unit) {
        var $el = $(selector);
        today = Number(today) || 0;
        yesterday = Number(yesterday) || 0;
        var diff = today - yesterday;

        if (diff > 0) {
            var display = unit === '%' ? '+' + diff.toFixed(1) + '%' : '+' + Number(diff).toLocaleString() + unit;
            $el.html('<i class="fas fa-arrow-up me-1"></i>' + display).attr('class', 'kpi-trend trend-up');
        } else if (diff < 0) {
            var display = unit === '%' ? diff.toFixed(1) + '%' : Number(diff).toLocaleString() + unit;
            $el.html('<i class="fas fa-arrow-down me-1"></i>' + display).attr('class', 'kpi-trend trend-down');
        } else {
            $el.html('<i class="fas fa-minus me-1"></i>0' + (unit === '%' ? '%' : unit)).attr('class', 'kpi-trend trend-flat');
        }
    },

    /**
     * 운영현황 로드
     */
    loadOperation: function(propertyId) {
        HolaPms.ajax({
            url: '/api/v1/dashboard/operation/' + propertyId,
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var d = res.data;
                $('#opArrivals').text(d.arrivals);
                $('#opInHouse').text(d.inHouse);
                $('#opDepartures').text(d.departures);
                $('#opCheckedIn').text(d.checkedInToday);
                $('#opCheckedOut').text(d.checkedOutToday);
            }
        });
    },

    /**
     * 7일 추이 차트 로드
     */
    loadPickup: function(propertyId) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/dashboard/pickup/' + propertyId,
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var pickups = res.data.dailyPickups;
                var labels = [];
                var counts = [];
                var revenues = [];

                for (var i = 0; i < pickups.length; i++) {
                    var p = pickups[i];
                    // MM/DD 형식
                    var parts = p.date.split('-');
                    labels.push(parseInt(parts[1]) + '/' + parseInt(parts[2]));
                    counts.push(p.reservationCount);
                    revenues.push(p.revenue || 0);
                }

                self.renderChart(labels, counts, revenues);
            }
        });
    },

    /**
     * Chart.js 차트 렌더링
     */
    renderChart: function(labels, counts, revenues) {
        var self = this;
        var ctx = document.getElementById('pickupChart');
        if (!ctx) return;

        // 기존 차트 제거
        if (self.pickupChart) {
            self.pickupChart.destroy();
        }

        var isMobile = window.innerWidth < 768;

        self.pickupChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: '예약 객실 수',
                        data: counts,
                        backgroundColor: '#003554',
                        hoverBackgroundColor: '#0582CA',
                        borderWidth: 0,
                        borderRadius: { topLeft: 100, topRight: 100, bottomLeft: 0, bottomRight: 0 },
                        borderSkipped: 'bottom',
                        barPercentage: 0.45,
                        categoryPercentage: 0.7,
                        yAxisID: 'y',
                        order: 2
                    },
                    {
                        label: '매출',
                        data: revenues,
                        type: 'line',
                        borderColor: '#EF476F',
                        backgroundColor: 'rgba(239, 71, 111, 0.05)',
                        borderWidth: 2,
                        pointRadius: isMobile ? 2 : 3,
                        pointHoverRadius: isMobile ? 4 : 5,
                        pointBackgroundColor: '#fff',
                        pointBorderColor: '#EF476F',
                        pointBorderWidth: 2,
                        fill: true,
                        tension: 0.3,
                        yAxisID: 'y1',
                        order: 1
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                aspectRatio: isMobile ? 1.5 : 2.5,
                layout: {
                    padding: { top: 4, bottom: 0, left: 0, right: 0 }
                },
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: {
                        position: 'top',
                        align: 'end',
                        labels: {
                            font: { family: 'Pretendard', size: 11, weight: '500' },
                            color: '#6c757d',
                            boxWidth: 12,
                            boxHeight: 12,
                            useBorderRadius: true,
                            borderRadius: 2,
                            padding: 16
                        }
                    },
                    tooltip: {
                        backgroundColor: '#051923',
                        titleFont: { family: 'Pretendard', size: 12, weight: '600' },
                        bodyFont: { family: 'Pretendard', size: 11 },
                        cornerRadius: 6,
                        padding: 10,
                        displayColors: true,
                        boxWidth: 8,
                        boxHeight: 8,
                        boxPadding: 4,
                        callbacks: {
                            label: function(context) {
                                if (context.dataset.yAxisID === 'y1') {
                                    return ' 매출: ' + Number(context.raw).toLocaleString() + '원';
                                }
                                return ' 객실: ' + context.raw + '실';
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        type: 'linear',
                        position: 'left',
                        beginAtZero: true,
                        border: { display: false },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.04)',
                            drawTicks: false
                        },
                        ticks: {
                            stepSize: 1,
                            font: { family: 'Pretendard', size: isMobile ? 10 : 11 },
                            color: '#adb5bd',
                            padding: 8
                        },
                        title: {
                            display: true,
                            text: '객실 수',
                            font: { family: 'Pretendard', size: 11, weight: '600' },
                            color: '#6c757d'
                        }
                    },
                    y1: {
                        type: 'linear',
                        position: 'right',
                        beginAtZero: true,
                        display: !isMobile,
                        border: { display: false },
                        grid: { drawOnChartArea: false, drawTicks: false },
                        ticks: {
                            font: { family: 'Pretendard', size: 11 },
                            color: '#adb5bd',
                            padding: 8,
                            callback: function(value) {
                                if (value === 0) return '0';
                                return (value / 10000).toFixed(0) + '만';
                            }
                        },
                        title: {
                            display: true,
                            text: '매출 (원)',
                            font: { family: 'Pretendard', size: 11, weight: '600' },
                            color: '#6c757d'
                        }
                    },
                    x: {
                        border: { display: false },
                        grid: { display: false },
                        ticks: {
                            font: { family: 'Pretendard', size: isMobile ? 10 : 11 },
                            color: '#6c757d',
                            padding: 4
                        }
                    }
                }
            }
        });
    },

    /**
     * 점유율에 따른 뱃지 색상
     */
    getOccBadge: function(rate) {
        if (rate >= 80) return 'bg-success';
        if (rate >= 50) return 'bg-primary';
        if (rate >= 30) return 'bg-warning text-dark';
        return 'bg-secondary';
    },

    /**
     * 통화 포맷 (원)
     */
    formatCurrency: function(amount) {
        if (amount === null || amount === undefined) return '0원';
        return Number(amount).toLocaleString() + '원';
    }
};

$(document).ready(function() {
    Dashboard.init();
});
