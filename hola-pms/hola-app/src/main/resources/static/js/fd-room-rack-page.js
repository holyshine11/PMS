/**
 * Room Rack 페이지 - 객실 그리드 뷰 + 30초 자동 갱신
 */
var FdRoomRack = {
    propertyId: null,
    data: [],
    pollInterval: null,
    selectedRoomId: null,

    init: function() {
        this.bindEvents();
        this.reload();
    },

    activeStatuses: ['VC','VD','OC','OD','OOO','OOS'],

    bindEvents: function() {
        var self = this;
        $(document).on('hola:contextChange', function() { self.reload(); });
        $('#refreshBtn').on('click', function() { self.loadRoomRack(); });
        $('#rdSaveBtn').on('click', function() { self.saveHkStatus(); });

        // 상태 필터 토글
        $(document).on('click', '.filter-badge', function() {
            var status = $(this).data('status');
            var idx = self.activeStatuses.indexOf(status);
            if (idx >= 0) {
                self.activeStatuses.splice(idx, 1);
                $(this).css('opacity', '0.3');
            } else {
                self.activeStatuses.push(status);
                $(this).css('opacity', '1');
            }
            self.renderGrid();
        });

        // 층 필터
        $('#floorFilter').on('change', function() { self.renderGrid(); });
    },

    reload: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#legendBar').hide();
            $('#roomRackContainer').hide();
            self.stopPolling();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#legendBar').show();
        $('#roomRackContainer').show();
        self.propertyId = propertyId;
        self.loadRoomRack();
        self.loadSummary();
        self.startPolling();
    },

    loadRoomRack: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-rack',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                self.data = res.data;
                self.updateFloorFilter();
                self.renderGrid();
                var now = new Date();
                $('#lastRefresh').text(String(now.getHours()).padStart(2,'0') + ':' + String(now.getMinutes()).padStart(2,'0') + ':' + String(now.getSeconds()).padStart(2,'0') + ' 갱신');
            },
            error: function() {
                HolaPms.alert('error', '객실현황을 불러오지 못했습니다.');
            }
        });
    },

    loadSummary: function() {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-status/summary',
            method: 'GET',
            success: function(res) {
                if (!res.success) return;
                var d = res.data;
                $('#cntVC').text(d.VC || 0);
                $('#cntVD').text(d.VD || 0);
                $('#cntOC').text(d.OC || 0);
                $('#cntOD').text(d.OD || 0);
                $('#cntOOO').text(d.OOO || 0);
                $('#cntOOS').text(d.OOS || 0);
            },
            error: function() { /* 요약 실패는 무시 - 그리드가 주 데이터 */ }
        });
    },

    updateFloorFilter: function() {
        var self = this;
        var currentVal = $('#floorFilter').val();
        var options = '<option value="">전체 층</option>';
        for (var i = 0; i < self.data.length; i++) {
            var label = self.data[i].floorLabel;
            options += '<option value="' + HolaPms.escapeHtml(label) + '">' + HolaPms.escapeHtml(label) + '</option>';
        }
        $('#floorFilter').html(options).val(currentVal);
    },

    renderGrid: function() {
        var self = this;
        var floorFilter = $('#floorFilter').val();
        var html = '';

        for (var i = 0; i < self.data.length; i++) {
            var floor = self.data[i];

            // 층 필터
            if (floorFilter && floor.floorLabel !== floorFilter) continue;

            // 상태 필터 적용
            var filteredRooms = floor.rooms.filter(function(r) {
                return self.activeStatuses.indexOf(r.statusCode) >= 0;
            });

            if (filteredRooms.length === 0) continue;

            html += '<div class="card border-0 shadow-sm mb-3">'
                + '<div class="card-header bg-white border-0 py-2">'
                + '<h6 class="fw-bold mb-0">' + HolaPms.escapeHtml(floor.floorLabel) + ' <small class="text-muted">(' + filteredRooms.length + ')</small></h6>'
                + '</div>'
                + '<div class="card-body pt-0">'
                + '<div class="d-flex flex-wrap gap-2">';

            for (var j = 0; j < filteredRooms.length; j++) {
                var room = filteredRooms[j];
                var cssClass = 'room-card-' + room.statusCode.toLowerCase();
                var guestLine = room.guestName ? '<div class="room-card-guest">' + HolaPms.escapeHtml(room.guestName) + '</div>' : '';
                var checkOutLine = room.checkOut ? '<div class="room-card-co">' + room.checkOut + '</div>' : '';

                html += '<div class="room-card ' + cssClass + '" onclick="FdRoomRack.openDetail(' + room.roomNumberId + ')">'
                    + '<div class="room-card-number">' + HolaPms.escapeHtml(room.roomNumber) + '</div>'
                    + '<div class="room-card-status">' + room.statusCode + '</div>'
                    + guestLine + checkOutLine
                    + '</div>';
            }

            html += '</div></div></div>';
        }

        if (self.data.length === 0) {
            html = '<div class="text-center py-5 text-muted">등록된 객실이 없습니다.</div>';
        }

        $('#roomRackContainer').html(html);
    },

    openDetail: function(roomId) {
        var self = this;
        self.selectedRoomId = roomId;

        // 데이터에서 찾기
        var room = null;
        for (var i = 0; i < self.data.length; i++) {
            for (var j = 0; j < self.data[i].rooms.length; j++) {
                if (self.data[i].rooms[j].roomNumberId === roomId) {
                    room = self.data[i].rooms[j];
                    break;
                }
            }
            if (room) break;
        }

        if (!room) return;

        $('#rdRoom').text(room.roomNumber);
        $('#rdStatus').html(self.getStatusBadge(room.statusCode));
        $('#rdHkSelect').val(room.hkStatus);
        $('#rdHkMemo').val(room.hkMemo || '');

        if (room.guestName) {
            $('#rdGuestRow').show();
            $('#rdCheckOutRow').show();
            $('#rdGuest').text(room.guestName);
            $('#rdCheckOut').text(room.checkOut || '-');
        } else {
            $('#rdGuestRow').hide();
            $('#rdCheckOutRow').hide();
        }
        $('#rdMemo').text(room.hkMemo || '-');

        HolaPms.modal.show('#roomDetailModal');
    },

    saveHkStatus: function() {
        var self = this;
        var hkStatus = $('#rdHkSelect').val();
        var memo = $('#rdHkMemo').val().trim();

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-status/' + self.selectedRoomId,
            method: 'PUT',
            data: JSON.stringify({ hkStatus: hkStatus, memo: memo }),
            success: function(res) {
                if (res.success) {
                    HolaPms.modal.hide('#roomDetailModal');
                    HolaPms.alert('success', '객실 상태 변경 완료');
                    self.loadRoomRack();
                    self.loadSummary();
                }
            },
            error: function() {
                HolaPms.alert('error', '객실 상태 변경에 실패했습니다.');
            }
        });
    },

    getStatusBadge: function(code) {
        var map = {
            'VC': '<span class="badge room-card-vc">VC (빈방/청소완료)</span>',
            'VD': '<span class="badge room-card-vd">VD (빈방/청소필요)</span>',
            'OC': '<span class="badge room-card-oc">OC (사용중/청소완료)</span>',
            'OD': '<span class="badge room-card-od">OD (사용중/청소필요)</span>',
            'OOO': '<span class="badge room-card-ooo">OOO (사용불가)</span>',
            'OOS': '<span class="badge room-card-oos">OOS (일시중단)</span>'
        };
        return map[code] || '<span class="badge bg-secondary">' + code + '</span>';
    },

    startPolling: function() {
        var self = this;
        self.stopPolling();
        self.pollInterval = setInterval(function() {
            self.loadRoomRack();
            self.loadSummary();
        }, 30000);
    },

    stopPolling: function() {
        if (this.pollInterval) {
            clearInterval(this.pollInterval);
            this.pollInterval = null;
        }
    }
};

$(document).ready(function() { FdRoomRack.init(); });
