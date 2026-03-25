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
    housekeepers: [], // 하우스키퍼 목록 캐시

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;
        $(document).on('hola:contextChange', function () {
            self.housekeepers = []; // 프로퍼티 변경 시 캐시 초기화
            self.reload();
        });
        $('#refreshBtn').on('click', function () { self.loadRoomRack(); });
        $('#hkSaveBtn').on('click', function () { self.saveHkStatus(); });

        // HK 상태 변경 시 담당자 영역 표시/숨김
        $('#hkStatusSelect').on('change', function () {
            self.toggleAssigneeArea();
        });

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
        self.loadHousekeepers();
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

    // 하우스키퍼 목록 조회 (프로퍼티 변경 시 + 최초 로드 시)
    loadHousekeepers: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/housekeepers',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.housekeepers = res.data || [];
                }
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

        // 고아 OC 카드에 경고 스타일 추가
        if (room.orphanOccupied) cssClass += ' room-card-orphan-flag';

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

        // 고아 OC: 투숙객 없이 OCCUPIED → 경고 표시
        if (room.orphanOccupied) {
            html += '<div class="room-card-orphan"><i class="fas fa-exclamation-triangle me-1"></i>투숙객 없음</div>';
            html += '<div class="room-card-status" style="font-size:0.55rem;">체크아웃 미처리</div>';
        } else if (isOccupied && room.guestName) {
            html += '<div class="room-card-guest">' + HolaPms.escapeHtml(room.guestName) + '</div>';
            if (room.checkOut) {
                html += '<div class="room-card-co"><i class="fas fa-sign-out-alt me-1"></i>' + room.checkOut + '</div>';
            }
        } else {
            // 빈방이면 상태코드 표시
            html += '<div class="room-card-status">' + room.statusCode + '</div>';
        }

        // HK 작업 오버레이 (진행중 작업만 표시, 완료/검수/취소된 작업은 숨김)
        var showHkOverlay = room.hkTaskStatus
            && room.hkTaskStatus !== 'CANCELLED'
            && room.hkTaskStatus !== 'INSPECTED'
            && room.hkTaskStatus !== 'COMPLETED';
        if (showHkOverlay) {
            var hkLabel = this.getHkTaskLabel(room.hkTaskStatus);
            html += '<div class="room-card-hk">' + hkLabel + '</div>';
            if (room.hkAssigneeName) {
                html += '<div class="room-card-assignee">' + HolaPms.escapeHtml(HolaPms.maskName(room.hkAssigneeName)) + '</div>';
            }
            if (room.hkTaskStartedAt) {
                html += '<div class="room-card-time">' + room.hkTaskStartedAt + '~</div>';
            }
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
        // 고아 OC → HK 모달 (상태 보정용)
        var room = this.findRoom(roomId);
        if (room && room.orphanOccupied) {
            this.openHkModal(roomId);
            return;
        }
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
        var self = this;
        var room = this.findRoom(roomId);
        if (!room) return;
        this.selectedRoomId = roomId;

        // 모달 기본 정보 설정
        $('#hkRoom').text(room.roomNumber);
        $('#hkCurrentStatus').html(this.getStatusBadge(room.statusCode));
        $('#hkRoomType').text(room.roomTypeName || '-');
        $('#hkStatusSelect').val(room.hkStatus);
        $('#hkMemoInput').val(room.hkMemo || '');
        $('#hkAssigneeSelect').val('');

        // 고아 OC 경고 표시
        if (room.orphanOccupied) {
            $('#hkOrphanWarning').removeClass('d-none');
        } else {
            $('#hkOrphanWarning').addClass('d-none');
        }

        // OOO/OOS 경고 표시
        var isOooOos = room.statusCode === 'OOO' || room.statusCode === 'OOS';
        var warningEl = $('#hkOooOosWarning');
        if (isOooOos) {
            $('#hkOooOosWarningText').text('이 객실에 ' + room.statusCode + ' 기록이 있습니다. 상태 변경 시 OOO/OOS 관리에서도 해제해주세요.');
            warningEl.removeClass('d-none');
        } else {
            warningEl.addClass('d-none');
        }

        // 진행중 작업 안내 초기화
        $('#hkInProgressAlert').addClass('d-none');

        // 진행중 작업 감지 (IN_PROGRESS/COMPLETED/INSPECTED)
        var hasActiveNonPending = room.hkTaskStatus &&
            room.hkTaskStatus !== 'CANCELLED' &&
            room.hkTaskStatus !== 'PENDING';
        if (hasActiveNonPending && room.hkAssigneeName) {
            var statusLabel = { 'IN_PROGRESS': '청소 진행중', 'COMPLETED': '청소 완료', 'PAUSED': '일시 중단', 'INSPECTED': '검수 완료' };
            $('#hkInProgressText').text(
                (statusLabel[room.hkTaskStatus] || room.hkTaskStatus) +
                ' (담당: ' + HolaPms.maskName(room.hkAssigneeName) + ')'
            );
            $('#hkInProgressAlert').removeClass('d-none');
        }

        // 담당자 영역: VD 조건 판정 + 드롭다운 채움
        self.toggleAssigneeArea(room);

        HolaPms.modal.show('#roomHkModal');
    },

    /**
     * 담당자 배정 영역 표시/숨김
     * VD 조건: (현재 VD이고 DIRTY 유지) 또는 (DIRTY로 변경 + foStatus VACANT)
     */
    toggleAssigneeArea: function (room) {
        var self = this;
        if (!room) {
            room = this.findRoom(this.selectedRoomId);
        }
        if (!room) return;

        var selectedHkStatus = $('#hkStatusSelect').val();
        var isVacant = room.foStatus === 'VACANT';
        var isDirty = selectedHkStatus === 'DIRTY';
        var showAssignee = isVacant && isDirty;

        // 진행중 작업이 있으면 담당자 배정 차단
        var hasActiveNonPending = room.hkTaskStatus &&
            room.hkTaskStatus !== 'CANCELLED' &&
            room.hkTaskStatus !== 'PENDING';
        if (hasActiveNonPending) {
            showAssignee = false;
        }

        if (showAssignee) {
            self.populateAssigneeSelect();
            $('#hkAssigneeArea').removeClass('d-none');
        } else {
            $('#hkAssigneeArea').addClass('d-none');
            $('#hkAssigneeSelect').val('');
        }
    },

    // 하우스키퍼 드롭다운 채우기 + 기존 배정자 선택
    populateAssigneeSelect: function () {
        var room = this.findRoom(this.selectedRoomId);
        var currentAssigneeId = room ? room.hkAssigneeId : null;
        var options = '<option value="">미지정</option>';

        if (this.housekeepers.length === 0) {
            $('#hkAssigneeEmpty').removeClass('d-none');
        } else {
            $('#hkAssigneeEmpty').addClass('d-none');
            for (var i = 0; i < this.housekeepers.length; i++) {
                var hk = this.housekeepers[i];
                options += '<option value="' + hk.userId + '">'
                    + HolaPms.escapeHtml(HolaPms.maskName(hk.userName))
                    + '</option>';
            }
        }

        $('#hkAssigneeSelect').html(options);

        // 기존 배정자가 있으면 선택
        if (currentAssigneeId) {
            $('#hkAssigneeSelect').val(String(currentAssigneeId));
        }
    },

    saveHkStatus: function () {
        var self = this;
        var hkStatus = $('#hkStatusSelect').val();
        var memo = $('#hkMemoInput').val().trim();
        var assigneeId = $('#hkAssigneeSelect').val() || '';

        var payload = { hkStatus: hkStatus, memo: memo };
        if (assigneeId) {
            payload.assigneeId = assigneeId;
        }
        // 고아 OC: 상태 변경 시 foStatus도 VACANT으로 자동 전환
        var room = this.findRoom(this.selectedRoomId);
        if (room && room.orphanOccupied) {
            payload.foStatus = 'VACANT';
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-status/' + self.selectedRoomId,
            method: 'PUT',
            data: JSON.stringify(payload),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#roomHkModal');
                    var msg = assigneeId ? '객실 상태 변경 + 담당자 배정 완료' : '객실 상태 변경 완료';
                    HolaPms.alert('success', msg);
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
            'VI': '<span class="badge room-card-vi">VI (빈방/검수완료)</span>',
            'VP': '<span class="badge room-card-vp">VP (빈방/간단정리)</span>',
            'OI': '<span class="badge room-card-oi">OI (사용중/검수완료)</span>',
            'OP': '<span class="badge room-card-op">OP (사용중/간단정리)</span>',
            'OOO': '<span class="badge room-card-ooo">OOO (사용불가)</span>',
            'OOS': '<span class="badge room-card-oos">OOS (일시중단)</span>'
        };
        return map[code] || '<span class="badge bg-secondary">' + code + '</span>';
    },

    getHkTaskLabel: function (status) {
        var map = {
            'PENDING': '<span class="badge hk-badge-pending">대기</span>',
            'IN_PROGRESS': '<span class="badge hk-badge-progress">청소중</span>',
            'PAUSED': '<span class="badge hk-badge-paused">중단</span>',
            'COMPLETED': '<span class="badge hk-badge-completed">완료</span>',
            'INSPECTED': '<span class="badge hk-badge-inspected">검수</span>'
        };
        return map[status] || '';
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
