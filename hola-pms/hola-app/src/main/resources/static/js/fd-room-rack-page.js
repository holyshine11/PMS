/**
 * Room Rack 페이지 - 객실 그리드 뷰 + 예약 상세 팝업 + HK 변경
 * 30초 자동 갱신
 */
var FdRoomRack = {
    propertyId: null,
    data: [],
    pollInterval: null,
    selectedRoomId: null,
    activeFilter: 'ALL',

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;
        $(document).on('hola:contextChange', function () {
            self.reload();
        });
        $('#refreshBtn').on('click', function () { self.loadRoomRack(); });
        $('#hkSaveBtn').on('click', function () { self.saveHkStatus(); });

        // 요약 카드 클릭 → 상태 필터
        $('#summaryRow').on('click', '.room-summary-card', function () {
            var status = $(this).data('status');
            self.activeFilter = status;
            $('#summaryRow .room-summary-card').removeClass('active');
            $(this).addClass('active');
            self.renderGrid();
        });

        // 층 필터
        $('#floorFilter').on('change', function () { self.renderGrid(); });

        // 팝업 자식 창 메시지 수신 → 데이터 갱신
        HolaPms.popup.onChildMessage(function() {
            self.loadRoomRack();
            self.loadSummary();
        });
    },

    reload: function () {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#summaryRow, #filterBar, #roomRackContainer').hide();
            self.stopPolling();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#summaryRow, #filterBar, #roomRackContainer').show();
        self.propertyId = propertyId;
        self.loadRoomRack();
        self.loadSummary();
        self.startPolling();
    },

    loadRoomRack: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-rack',
            method: 'GET',
            success: function (res) {
                if (!res.success) return;
                self.data = res.data;
                self.updateFloorFilter();
                self.renderGrid();
                var now = new Date();
                var ts = String(now.getHours()).padStart(2, '0') + ':' +
                    String(now.getMinutes()).padStart(2, '0') + ':' +
                    String(now.getSeconds()).padStart(2, '0');
                $('#lastRefresh').text(ts + ' 갱신');
            },
            error: function () {
                HolaPms.alert('error', '객실현황을 불러오지 못했습니다.');
            }
        });
    },

    loadSummary: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-status/summary',
            method: 'GET',
            success: function (res) {
                if (!res.success) return;
                var d = res.data;
                var vc = d.VC || 0, vd = d.VD || 0, oc = d.OC || 0;
                var od = d.OD || 0, ooo = d.OOO || 0, oos = d.OOS || 0;
                $('#cntVC').text(vc);
                $('#cntVD').text(vd);
                $('#cntOC').text(oc);
                $('#cntOD').text(od);
                $('#cntOOO').text(ooo);
                $('#cntOOS').text(oos);
                $('#cntTotal').text(vc + vd + oc + od + ooo + oos);
            }
        });
    },

    updateFloorFilter: function () {
        var currentVal = $('#floorFilter').val();
        var options = '<option value="">전체 층</option>';
        for (var i = 0; i < this.data.length; i++) {
            var label = this.data[i].floorLabel;
            options += '<option value="' + HolaPms.escapeHtml(label) + '">' + HolaPms.escapeHtml(label) + '</option>';
        }
        $('#floorFilter').html(options).val(currentVal);
    },

    // ========== 그리드 렌더링 ==========

    renderGrid: function () {
        var self = this;
        var floorFilter = $('#floorFilter').val();
        var html = '';

        for (var i = 0; i < self.data.length; i++) {
            var floor = self.data[i];
            if (floorFilter && floor.floorLabel !== floorFilter) continue;

            // 상태 필터 적용
            var filteredRooms = floor.rooms;
            if (self.activeFilter !== 'ALL') {
                filteredRooms = floor.rooms.filter(function (r) {
                    return r.statusCode === self.activeFilter;
                });
            }
            if (filteredRooms.length === 0) continue;

            html += '<div class="card border-0 shadow-sm mb-3">'
                + '<div class="card-header bg-white border-0 py-2">'
                + '<h6 class="fw-bold mb-0">' + HolaPms.escapeHtml(floor.floorLabel)
                + ' <small class="text-muted">(' + filteredRooms.length + ')</small></h6>'
                + '</div>'
                + '<div class="card-body pt-0">'
                + '<div class="d-flex flex-wrap gap-2">';

            for (var j = 0; j < filteredRooms.length; j++) {
                html += self.renderRoomCard(filteredRooms[j]);
            }

            html += '</div></div></div>';
        }

        if (self.data.length === 0) {
            html = '<div class="text-center py-5 text-muted"><i class="fas fa-bed fa-2x mb-2"></i><div>등록된 객실이 없습니다.</div></div>';
        }

        $('#roomRackContainer').html(html);
    },

    renderRoomCard: function (room) {
        var cssClass = 'room-card-' + room.statusCode.toLowerCase();
        var isOccupied = room.foStatus === 'OCCUPIED';

        var html = '<div class="room-card ' + cssClass + '" data-room-id="' + room.roomNumberId + '"'
            + ' data-occupied="' + isOccupied + '"'
            + (isOccupied && room.reservationId ? ' data-reservation-id="' + room.reservationId + '"' : '')
            + '>';

        // 호수번호
        html += '<div class="room-card-number">' + HolaPms.escapeHtml(room.roomNumber) + '</div>';

        // 객실타입
        if (room.roomTypeName) {
            html += '<div class="room-card-type">' + HolaPms.escapeHtml(room.roomTypeName) + '</div>';
        }

        // 투숙중이면 투숙객 + 체크아웃일
        if (isOccupied && room.guestName) {
            html += '<div class="room-card-guest">' + HolaPms.escapeHtml(room.guestName) + '</div>';
            if (room.checkOut) {
                html += '<div class="room-card-co"><i class="fas fa-sign-out-alt me-1"></i>' + room.checkOut + '</div>';
            }
        } else {
            // 빈방이면 상태코드 표시
            html += '<div class="room-card-status">' + room.statusCode + '</div>';
        }

        // HK 메모 있으면 표시
        if (room.hkMemo) {
            html += '<div class="room-card-memo" title="' + HolaPms.escapeHtml(room.hkMemo) + '"><i class="fas fa-sticky-note"></i></div>';
        }

        html += '</div>';
        return html;
    },

    // ========== 카드 클릭 핸들러 ==========

    handleCardClick: function (roomId, isOccupied, reservationId) {
        if (isOccupied && reservationId) {
            // 투숙중 → 예약 상세 팝업
            HolaPms.popup.openReservationDetail(reservationId);
        } else {
            // 빈방/OOO/OOS → HK 상태 변경 모달
            this.openHkModal(roomId);
        }
    },

    // ========== HK 상태 변경 모달 ==========

    openHkModal: function (roomId) {
        var room = this.findRoom(roomId);
        if (!room) return;
        this.selectedRoomId = roomId;

        $('#hkRoom').text(room.roomNumber);
        $('#hkCurrentStatus').html(this.getStatusBadge(room.statusCode));
        $('#hkRoomType').text(room.roomTypeName || '-');
        $('#hkStatusSelect').val(room.hkStatus);
        $('#hkMemoInput').val(room.hkMemo || '');

        HolaPms.modal.show('#roomHkModal');
    },

    saveHkStatus: function () {
        var self = this;
        var hkStatus = $('#hkStatusSelect').val();
        var memo = $('#hkMemoInput').val().trim();

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-status/' + self.selectedRoomId,
            method: 'PUT',
            data: JSON.stringify({ hkStatus: hkStatus, memo: memo }),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#roomHkModal');
                    HolaPms.alert('success', '객실 상태 변경 완료');
                    self.loadRoomRack();
                    self.loadSummary();
                }
            },
            error: function () {
                HolaPms.alert('error', '객실 상태 변경에 실패했습니다.');
            }
        });
    },


    // ========== 유틸리티 ==========

    getStatusBadge: function (code) {
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

    findRoom: function (roomId) {
        for (var i = 0; i < this.data.length; i++) {
            for (var j = 0; j < this.data[i].rooms.length; j++) {
                if (this.data[i].rooms[j].roomNumberId === roomId) {
                    return this.data[i].rooms[j];
                }
            }
        }
        return null;
    },

    startPolling: function () {
        var self = this;
        self.stopPolling();
        self.pollInterval = setInterval(function () {
            self.loadRoomRack();
            self.loadSummary();
        }, 30000);
    },

    stopPolling: function () {
        if (this.pollInterval) {
            clearInterval(this.pollInterval);
            this.pollInterval = null;
        }
    }
};

$(document).ready(function () {
    FdRoomRack.init();

    // 카드 클릭 이벤트 (동적 요소)
    $(document).on('click', '.room-card', function () {
        var roomId = $(this).data('room-id');
        var isOccupied = $(this).data('occupied') === true;
        var reservationId = $(this).data('reservation-id');
        FdRoomRack.handleCardClick(roomId, isOccupied, reservationId);
    });
});
