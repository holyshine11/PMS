/**
 * 하우스키핑 설정 페이지 (일반 설정 + 구역 관리)
 */
var HkSettings = {

    propertyId: null,
    floors: [],
    housekeepers: [],
    sections: [],
    policies: [],

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () { self.reload(); });

        // 일반 설정 저장
        $('#btnSaveConfig').on('click', function () { self.saveConfig(); });

        // 구역 추가
        $('#btnAddSection').on('click', function () { self.openSectionModal(null); });

        // 구역 저장
        $('#btnSaveSection').on('click', function () { self.saveSection(); });

        // 구역 수정/삭제
        $(document).on('click', '.btn-edit-section', function () {
            self.openSectionModal($(this).data('id'));
        });
        $(document).on('click', '.btn-delete-section', function () {
            if (confirm('이 구역을 삭제하시겠습니까?')) {
                self.deleteSection($(this).data('id'));
            }
        });

        // 청소 정책
        $(document).on('click', '.btn-edit-policy', function () {
            self.openPolicyModal($(this).data('room-type-id'));
        });
        $('#btnSavePolicy').on('click', function () { self.savePolicy(); });
        $('#btnResetPolicy').on('click', function () { self.resetPolicy(); });
    },

    reload: function () {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#settingsTabs, #settingsContent').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#settingsTabs, #settingsContent').show();
        this.propertyId = propertyId;
        this.loadConfig();
        this.loadFloors();
        this.loadHousekeepers();
        this.loadSections();
        this.loadPolicies();
    },

    // === 일반 설정 ===

    loadConfig: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/config',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    var c = res.data;
                    $('#cfgInspectionRequired').prop('checked', c.inspectionRequired);
                    $('#cfgAutoCheckout').prop('checked', c.autoCreateCheckout);
                    $('#cfgAutoStayover').prop('checked', c.autoCreateStayover);
                    $('#cfgCheckoutCredit').val(c.defaultCheckoutCredit);
                    $('#cfgStayoverCredit').val(c.defaultStayoverCredit);
                    $('#cfgTurndownCredit').val(c.defaultTurndownCredit);
                    $('#cfgDeepCleanCredit').val(c.defaultDeepCleanCredit);
                    $('#cfgTouchUpCredit').val(c.defaultTouchUpCredit);
                    $('#cfgRushThreshold').val(c.rushThresholdMinutes);
                    $('#cfgStayoverEnabled').prop('checked', c.stayoverEnabled);
                    $('#cfgStayoverFrequency').val(c.stayoverFrequency || 1);
                    $('#cfgTurndownEnabled').prop('checked', c.turndownEnabled);
                    $('#cfgDndPolicy').val(c.dndPolicy || 'SKIP');
                    $('#cfgDndMaxSkipDays').val(c.dndMaxSkipDays || 3);
                    $('#cfgOdTransitionTime').val(c.odTransitionTime || '05:00');
                    $('#cfgDailyTaskGenTime').val(c.dailyTaskGenTime || '06:00');
                }
            }
        });
    },

    saveConfig: function () {
        var self = this;
        var data = {
            inspectionRequired: $('#cfgInspectionRequired').is(':checked'),
            autoCreateCheckout: $('#cfgAutoCheckout').is(':checked'),
            autoCreateStayover: $('#cfgAutoStayover').is(':checked'),
            defaultCheckoutCredit: parseFloat($('#cfgCheckoutCredit').val()) || 1.0,
            defaultStayoverCredit: parseFloat($('#cfgStayoverCredit').val()) || 0.5,
            defaultTurndownCredit: parseFloat($('#cfgTurndownCredit').val()) || 0.3,
            defaultDeepCleanCredit: parseFloat($('#cfgDeepCleanCredit').val()) || 2.0,
            defaultTouchUpCredit: parseFloat($('#cfgTouchUpCredit').val()) || 0.3,
            rushThresholdMinutes: parseInt($('#cfgRushThreshold').val()) || 120,
            stayoverEnabled: $('#cfgStayoverEnabled').is(':checked'),
            stayoverFrequency: parseInt($('#cfgStayoverFrequency').val()) || 1,
            turndownEnabled: $('#cfgTurndownEnabled').is(':checked'),
            dndPolicy: $('#cfgDndPolicy').val() || 'SKIP',
            dndMaxSkipDays: parseInt($('#cfgDndMaxSkipDays').val()) || 3,
            dailyTaskGenTime: $('#cfgDailyTaskGenTime').val() || '06:00',
            odTransitionTime: $('#cfgOdTransitionTime').val() || '05:00'
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/config',
            method: 'PUT',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) { HolaPms.alert('success', '설정이 저장되었습니다.'); }
            }
        });
    },

    // === 층/하우스키퍼 로드 ===

    loadFloors: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/floors',
            method: 'GET',
            success: function (res) {
                if (res.success) { self.floors = res.data || []; }
            }
        });
    },

    loadHousekeepers: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/housekeepers',
            method: 'GET',
            success: function (res) {
                if (res.success) { self.housekeepers = res.data || []; }
            }
        });
    },

    // === 구역 관리 ===

    loadSections: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/sections',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.sections = res.data || [];
                    self.renderSectionTable();
                }
            }
        });
    },

    renderSectionTable: function () {
        var $body = $('#sectionBody');
        $body.empty();

        if (this.sections.length === 0) {
            $body.append('<tr><td colspan="4" class="text-center text-muted py-3">등록된 구역이 없습니다.</td></tr>');
            return;
        }

        this.sections.forEach(function (s) {
            var floorNames = (s.floors || []).map(function (f) { return f.floorNumber || f.floorName; }).join(', ') || '-';
            var hkNames = (s.housekeepers || []).map(function (h) {
                return HolaPms.escapeHtml(h.userName) + (h.isPrimary ? '' : ' (부)');
            }).join(', ') || '-';

            $body.append(
                '<tr>' +
                '<td>' + HolaPms.escapeHtml(s.sectionName) + '</td>' +
                '<td class="text-center">' + floorNames + '</td>' +
                '<td class="text-center">' + hkNames + '</td>' +
                '<td class="text-center">' +
                    '<button class="btn btn-sm btn-outline-primary btn-edit-section me-1" data-id="' + s.id + '"><i class="fas fa-edit"></i></button>' +
                    '<button class="btn btn-sm btn-outline-danger btn-delete-section" data-id="' + s.id + '"><i class="fas fa-trash"></i></button>' +
                '</td>' +
                '</tr>'
            );
        });
    },

    openSectionModal: function (sectionId) {
        var self = this;
        var isEdit = sectionId != null;

        $('#sectionModalTitle').text(isEdit ? '구역 수정' : '구역 추가');
        $('#sectionEditId').val(sectionId || '');
        $('#sectionName').val('');
        $('#sectionCode').val('');
        $('#sectionMaxCredits').val('');

        // 층 체크박스 생성
        var floorHtml = '';
        self.floors.forEach(function (f) {
            floorHtml += '<div class="form-check">' +
                '<input class="form-check-input section-floor-check" type="checkbox" value="' + f.id + '" id="sf_' + f.id + '">' +
                '<label class="form-check-label" for="sf_' + f.id + '">' + HolaPms.escapeHtml(f.floorNumber || f.floorName || f.id) + '</label>' +
                '</div>';
        });
        $('#sectionFloorCheckboxes').html(floorHtml);

        // 하우스키퍼 체크박스 생성
        var hkHtml = '';
        self.housekeepers.forEach(function (hk) {
            hkHtml += '<div class="form-check">' +
                '<input class="form-check-input section-hk-check" type="checkbox" value="' + hk.userId + '" id="shk_' + hk.userId + '">' +
                '<label class="form-check-label" for="shk_' + hk.userId + '">' + HolaPms.escapeHtml(hk.userName) + '</label>' +
                '</div>';
        });
        $('#sectionHkCheckboxes').html(hkHtml);

        // 수정 모드: 기존 데이터 채우기
        if (isEdit) {
            var section = self.sections.find(function (s) { return s.id === sectionId; });
            if (section) {
                $('#sectionName').val(section.sectionName);
                $('#sectionCode').val(section.sectionCode || '');
                $('#sectionMaxCredits').val(section.maxCredits || '');
                (section.floors || []).forEach(function (f) {
                    $('#sf_' + f.id).prop('checked', true);
                });
                (section.housekeepers || []).forEach(function (h) {
                    $('#shk_' + h.id).prop('checked', true);
                });
            }
        }

        HolaPms.modal.show('#sectionModal');
    },

    saveSection: function () {
        var self = this;
        var sectionName = $('#sectionName').val().trim();
        if (!sectionName) {
            HolaPms.alert('warning', '구역 이름을 입력해주세요.');
            return;
        }

        var floorIds = [];
        $('.section-floor-check:checked').each(function () { floorIds.push(parseInt($(this).val())); });

        var housekeeperIds = [];
        $('.section-hk-check:checked').each(function () { housekeeperIds.push(parseInt($(this).val())); });

        var data = {
            sectionName: sectionName,
            sectionCode: HolaPms.form.val('#sectionCode'),
            maxCredits: $('#sectionMaxCredits').val() ? parseFloat($('#sectionMaxCredits').val()) : null,
            floorIds: floorIds,
            housekeeperIds: housekeeperIds
        };

        var editId = $('#sectionEditId').val();
        var url = '/api/v1/properties/' + self.propertyId + '/housekeeping/sections';
        var method = 'POST';

        if (editId) {
            url += '/' + editId;
            method = 'PUT';
        }

        HolaPms.ajax({
            url: url,
            method: method,
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#sectionModal');
                    HolaPms.alert('success', editId ? '구역이 수정되었습니다.' : '구역이 추가되었습니다.');
                    self.loadSections();
                }
            }
        });
    },

    deleteSection: function (sectionId) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeeping/sections/' + sectionId,
            method: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '구역이 삭제되었습니다.');
                    self.loadSections();
                }
            }
        });
    },

    // === 청소 정책 ===

    loadPolicies: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-cleaning-policies',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    self.policies = res.data || [];
                    self.renderPolicyTable();
                }
            }
        });
    },

    renderPolicyTable: function () {
        var $body = $('#policyBody');
        $body.empty();

        if (this.policies.length === 0) {
            $body.append('<tr><td colspan="6" class="text-center text-muted py-3">등록된 룸타입이 없습니다.</td></tr>');
            return;
        }

        this.policies.forEach(function (p) {
            var badge = p.overridden
                ? '<span class="badge bg-primary">오버라이드</span>'
                : '<span class="badge bg-secondary">기본값</span>';
            var stayover = p.overridden && p.stayoverEnabled != null
                ? (p.stayoverEnabled ? (p.stayoverFrequency || '-') + '회/일' : 'OFF')
                : '-';
            var turndown = p.overridden && p.turndownEnabled != null
                ? (p.turndownEnabled ? 'ON' : 'OFF')
                : '-';
            var dndMap = { 'SKIP': '스킵', 'RETRY_AFTERNOON': '오후 재시도', 'FORCE_AFTER_DAYS': 'N일 강제' };
            var dnd = p.overridden && p.dndPolicy ? (dndMap[p.dndPolicy] || p.dndPolicy) : '-';

            $body.append(
                '<tr>' +
                '<td>' + HolaPms.escapeHtml(p.roomTypeCode || '') +
                    ' <span class="text-muted small">(' + HolaPms.escapeHtml(p.roomTypeName || '') + ')</span></td>' +
                '<td class="text-center">' + stayover + '</td>' +
                '<td class="text-center">' + turndown + '</td>' +
                '<td class="text-center">' + dnd + '</td>' +
                '<td class="text-center">' + badge + '</td>' +
                '<td class="text-center">' +
                    '<button class="btn btn-sm btn-outline-primary btn-edit-policy" data-room-type-id="' + p.roomTypeId + '">' +
                        '<i class="fas fa-edit"></i>' +
                    '</button>' +
                '</td>' +
                '</tr>'
            );
        });
    },

    openPolicyModal: function (roomTypeId) {
        var self = this;
        var policy = self.policies.find(function (p) { return p.roomTypeId === roomTypeId; });
        if (!policy) return;

        $('#policyModalTitle').text(
            (policy.roomTypeCode || '') + ' 청소 정책' + (policy.overridden ? ' (오버라이드)' : '')
        );
        $('#policyRoomTypeId').val(roomTypeId);

        $('#polStayoverEnabled').val(policy.stayoverEnabled != null ? String(policy.stayoverEnabled) : '');
        $('#polStayoverFrequency').val(policy.stayoverFrequency || '');
        $('#polStayoverCredit').val(policy.stayoverCredit || '');
        $('#polStayoverPriority').val(policy.stayoverPriority || '');
        $('#polTurndownEnabled').val(policy.turndownEnabled != null ? String(policy.turndownEnabled) : '');
        $('#polTurndownCredit').val(policy.turndownCredit || '');
        $('#polDndPolicy').val(policy.dndPolicy || '');
        $('#polDndMaxSkipDays').val(policy.dndMaxSkipDays || '');
        $('#polNote').val(policy.note || '');

        if (policy.overridden) {
            $('#btnResetPolicy').removeClass('d-none');
        } else {
            $('#btnResetPolicy').addClass('d-none');
        }

        HolaPms.modal.show('#policyModal');
    },

    savePolicy: function () {
        var self = this;
        var roomTypeId = parseInt($('#policyRoomTypeId').val());

        var toNull = function (v) { return v === '' || v === undefined ? null : v; };
        var toBool = function (v) { return v === 'true' ? true : v === 'false' ? false : null; };

        var data = {
            roomTypeId: roomTypeId,
            stayoverEnabled: toBool($('#polStayoverEnabled').val()),
            stayoverFrequency: toNull($('#polStayoverFrequency').val()) ? parseInt($('#polStayoverFrequency').val()) : null,
            stayoverCredit: toNull($('#polStayoverCredit').val()) ? parseFloat($('#polStayoverCredit').val()) : null,
            stayoverPriority: toNull($('#polStayoverPriority').val()),
            turndownEnabled: toBool($('#polTurndownEnabled').val()),
            turndownCredit: toNull($('#polTurndownCredit').val()) ? parseFloat($('#polTurndownCredit').val()) : null,
            dndPolicy: toNull($('#polDndPolicy').val()),
            dndMaxSkipDays: toNull($('#polDndMaxSkipDays').val()) ? parseInt($('#polDndMaxSkipDays').val()) : null,
            note: toNull($('#polNote').val())
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-cleaning-policies',
            method: 'POST',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#policyModal');
                    HolaPms.alert('success', '정책이 저장되었습니다.');
                    self.loadPolicies();
                }
            }
        });
    },

    resetPolicy: function () {
        var self = this;
        var roomTypeId = parseInt($('#policyRoomTypeId').val());
        if (!confirm('이 룸타입의 오버라이드를 삭제하고 프로퍼티 기본값으로 복귀하시겠습니까?')) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-cleaning-policies/' + roomTypeId,
            method: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.modal.hide('#policyModal');
                    HolaPms.alert('success', '기본값으로 초기화되었습니다.');
                    self.loadPolicies();
                }
            }
        });
    }
};

$(function () {
    HkSettings.init();
});
