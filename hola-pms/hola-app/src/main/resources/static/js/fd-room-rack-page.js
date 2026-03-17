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

    bindEvents: function() {
        var self = this;
        $(document).on('hola:contextChange', function() { self.reload(); });
        $('#refreshBtn').on('click', function() { self.loadRoomRack(); });
        $('#rdSaveBtn').on('click', function() { self.saveHkStatus(); });
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
                self.renderGrid();
                var now = new Date();
                $('#lastRefresh').text(String(now.getHours()).padStart(2,'0') + ':' + String(now.getMinutes()).padStart(2,'0') + ':' + String(now.getSeconds()).padStart(2,'0') + ' 갱신');
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
            }
        });
    },

    renderGrid: function() {
        var self = this;
        var html = '';

        for (var i = 0; i < self.data.length; i++) {
            var floor = self.data[i];
            html += '<div class="card border-0 shadow-sm mb-3">'
                + '<div class="card-header bg-white border-0 py-2">'
                + '<h6 class="fw-bold mb-0">' + HolaPms.escapeHtml(floor.floorLabel) + '</h6>'
                + '</div>'
                + '<div class="card-body pt-0">'
                + '<div class="d-flex flex-wrap gap-2">';

            for (var j = 0; j < floor.rooms.length; j++) {
                var room = floor.rooms[j];
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

        HolaPms.modal.show('roomDetailModal');
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
                    HolaPms.modal.hide('roomDetailModal');
                    HolaPms.alert('success', '객실 상태 변경 완료');
                    self.loadRoomRack();
                    self.loadSummary();
                }
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
