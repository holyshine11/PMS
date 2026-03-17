/**
 * 대시보드 페이지
 * 역할별 분기: SUPER_ADMIN → 프로퍼티 랭킹, PROPERTY 선택 시 → KPI + 운영 + 차트
 */
var Dashboard = {
    pickupChart: null,
    isSuperAdmin: false,

    /**
     * 초기화
     */
    init: function() {
        var self = this;
        // 오늘 날짜 표시
        var today = new Date();
        var dateStr = today.getFullYear() + '년 ' + (today.getMonth() + 1) + '월 ' + today.getDate() + '일';
        var days = ['일', '월', '화', '수', '목', '금', '토'];
        dateStr += ' (' + days[today.getDay()] + ')';
        $('#dashboardDate').text(dateStr);

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
    },

    /**
     * 데이터 로드 분기
     */
    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        if (propertyId) {
            // 프로퍼티 선택됨 → KPI + 운영 + 차트
            $('#contextAlert').addClass('d-none');
            $('#propertyDashboard').removeClass('d-none');
            self.loadPropertyKpi(propertyId);
            self.loadOperation(propertyId);
            self.loadPickup(propertyId);
        } else {
            // 프로퍼티 미선택
            $('#propertyDashboard').addClass('d-none');
            // SUPER_ADMIN이면 랭킹 테이블이 보이므로 contextAlert 숨김
            if ($('#superAdminSummary').length > 0 && $('#superAdminSummary').is(':visible')) {
                self.loadAllKpis();
            } else {
                $('#contextAlert').removeClass('d-none');
            }
        }

        // SUPER_ADMIN이면 랭킹도 로드
        if ($('#rankingTable').length > 0) {
            self.loadAllKpis();
        }
    },

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

                    html += '<tr>'
                        + '<td class="ps-3">' + (i + 1) + '</td>'
                        + '<td>' + HolaPms.escapeHtml(d.propertyName || '-') + '</td>'
                        + '<td class="text-center">' + d.totalRooms + '</td>'
                        + '<td class="text-center">' + d.soldRooms + '</td>'
                        + '<td class="text-center"><span class="badge ' + Dashboard.getOccBadge(d.occupancyRate) + '">'
                        + (d.occupancyRate || 0).toFixed(1) + '%</span></td>'
                        + '<td class="text-end">' + Dashboard.formatCurrency(d.adr) + '</td>'
                        + '<td class="text-end">' + Dashboard.formatCurrency(d.revPar) + '</td>'
                        + '<td class="text-end pe-3">' + Dashboard.formatCurrency(d.totalRevenue) + '</td>'
                        + '</tr>';
                }

                if (data.length === 0) {
                    html = '<tr><td colspan="8" class="text-center py-4 text-muted">등록된 프로퍼티가 없습니다.</td></tr>';
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
     * 프로퍼티 KPI 로드
     */
    loadPropertyKpi: function(propertyId) {
        HolaPms.ajax({
            url: '/api/v1/dashboard/property/' + propertyId,
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var d = res.data;
                $('#kpiOcc').text((d.occupancyRate || 0).toFixed(1) + '%');
                $('#kpiSold').text(d.soldRooms);
                $('#kpiTotal').text(d.totalRooms);
                $('#kpiAdr').text(Dashboard.formatCurrency(d.adr));
                $('#kpiRevpar').text(Dashboard.formatCurrency(d.revPar));
                $('#kpiRevenue').text(Dashboard.formatCurrency(d.totalRevenue));
            }
        });
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

        self.pickupChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: '예약 객실 수',
                        data: counts,
                        backgroundColor: 'rgba(5, 130, 202, 0.7)',
                        borderColor: '#0582CA',
                        borderWidth: 1,
                        borderRadius: 4,
                        yAxisID: 'y',
                        order: 2
                    },
                    {
                        label: '매출 (원)',
                        data: revenues,
                        type: 'line',
                        borderColor: '#EF476F',
                        backgroundColor: 'rgba(239, 71, 111, 0.1)',
                        borderWidth: 2,
                        pointRadius: 4,
                        pointBackgroundColor: '#EF476F',
                        fill: true,
                        yAxisID: 'y1',
                        order: 1
                    }
                ]
            },
            options: {
                responsive: true,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: {
                        position: 'top',
                        labels: { font: { family: 'Pretendard' } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                if (context.dataset.yAxisID === 'y1') {
                                    return context.dataset.label + ': ' + Number(context.raw).toLocaleString() + '원';
                                }
                                return context.dataset.label + ': ' + context.raw + '실';
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        type: 'linear',
                        position: 'left',
                        beginAtZero: true,
                        ticks: { stepSize: 1, font: { family: 'Pretendard' } },
                        title: { display: true, text: '객실 수', font: { family: 'Pretendard' } }
                    },
                    y1: {
                        type: 'linear',
                        position: 'right',
                        beginAtZero: true,
                        grid: { drawOnChartArea: false },
                        ticks: {
                            font: { family: 'Pretendard' },
                            callback: function(value) {
                                return (value / 10000).toFixed(0) + '만';
                            }
                        },
                        title: { display: true, text: '매출', font: { family: 'Pretendard' } }
                    },
                    x: {
                        ticks: { font: { family: 'Pretendard' } }
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
