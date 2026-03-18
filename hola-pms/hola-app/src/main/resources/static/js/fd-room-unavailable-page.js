/**
 * OOO/OOS 관리 페이지
 */
var FdRoomUnavailable = {
    propertyId: null,
    table: null,
    rooms: [],

    init: function () {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () {
            self.reload();
        });

        $('#filterType').on('change', function () {
            self.loadList();
        });

        $('#btnNew').on('click', function () {
            self.openForm(null);
        });

        $('#btnSave').on('click', function () {
            self.save();
        });

        $(document).on('click', '.btn-edit', function () {
            self.openForm($(this).data('id'));
        });

        $(document).on('click', '.btn-release', function () {
            var id = $(this).data('id');
            if (!confirm('해제하시겠습니까? 객실 상태가 복귀됩니다.')) return;
            self.deleteItem(id);
        });

        HolaPms.bindDateRange('#formFromDate', '#formThroughDate');
    },

    reload: function () {
        this.propertyId = HolaPms.context.getPropertyId();

        if (!this.propertyId) {
            $('#contextAlert').removeClass('d-none');
            return;
        }

        $('#contextAlert').addClass('d-none');
        this.loadRooms();
        this.loadList();
    },

    loadRooms: function () {
        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/room-numbers',
            type: 'GET',
            success: function (res) {
                if (res.success) {
                    FdRoomUnavailable.rooms = res.data || [];
                    var $select = $('#formRoom').empty();
                    $select.append('<option value="">선택</option>');
                    FdRoomUnavailable.rooms.forEach(function (r) {
                        $select.append('<option value="' + r.id + '">' + HolaPms.escapeHtml(r.roomNumber) + '</option>');
                    });
                }
            }
        });
    },

    loadList: function () {
        var self = this;
        var type = $('#filterType').val();
        var url = '/api/v1/properties/' + this.propertyId + '/room-unavailable';
        if (type) url += '?type=' + type;

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function (res) {
                if (!res.success) return;

                if (self.table) {
                    self.table.clear();
                    self.table.rows.add(res.data || []);
                    self.table.draw();
                } else {
                    self.table = $('#tableUnavailable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
                        data: res.data || [],
                        columns: [
                            { data: 'unavailableType', render: self.renderTypeBadge },
                            { data: 'roomNumber', render: HolaPms.renders.dashIfEmpty },
                            { data: 'reasonCode', render: self.renderReasonCode },
                            { data: 'reasonDetail', render: self.renderReasonDetail },
                            { data: 'fromDate' },
                            { data: 'throughDate' },
                            { data: 'returnStatus', render: self.renderReturnStatus },
                            { data: 'createdAt', render: self.renderDate },
                            { data: null, render: self.renderActions }
                        ],
                        order: [[4, 'desc']],
                        pageLength: 20
                    }));
                }
            }
        });
    },

    // ========== 렌더러 ==========

    REASON_MAP: {
        MAINTENANCE: '보수', RENOVATION: '리모델링', DAMAGE: '손상',
        PLUMBING: '배관', ELECTRICAL: '전기',
        SHOWROOM: '전시', VIP_HOLD: 'VIP 대기', STAFF_USE: '직원사용',
        DEEP_CLEAN: '특별청소', INSPECTION: '점검'
    },

    renderTypeBadge: function (data) {
        if (data === 'OOO') return '<span class="badge" style="background-color:#EF476F;">OOO</span>';
        if (data === 'OOS') return '<span class="badge bg-warning text-dark">OOS</span>';
        return data || '-';
    },

    renderReasonCode: function (data) {
        return FdRoomUnavailable.REASON_MAP[data] || data || '-';
    },

    renderReasonDetail: function (data) {
        if (!data) return '-';
        var escaped = HolaPms.escapeHtml(data);
        return escaped.length > 30 ? '<span title="' + escaped + '">' + escaped.substring(0, 30) + '...</span>' : escaped;
    },

    renderReturnStatus: function (data) {
        if (data === 'CLEAN') return '<span class="badge bg-success">Clean</span>';
        return '<span class="badge bg-danger">Dirty</span>';
    },

    renderDate: function (data) {
        if (!data) return '-';
        return data.substring(0, 10);
    },

    renderActions: function (data, type, row) {
        return '<div class="btn-group btn-group-sm">' +
            '<button class="btn btn-outline-secondary btn-edit" data-id="' + row.id + '" title="수정"><i class="fas fa-edit"></i></button>' +
            '<button class="btn btn-outline-danger btn-release" data-id="' + row.id + '" title="해제"><i class="fas fa-unlock"></i></button>' +
            '</div>';
    },

    // ========== 폼 ==========

    openForm: function (id) {
        var self = this;
        $('#formId').val('');
        $('#formType').val('OOO');
        $('#formRoom').val('');
        $('#formReasonCode').val('');
        $('#formReasonDetail').val('');
        $('#formFromDate').val('');
        $('#formThroughDate').val('');
        $('#formReturnStatus').val('DIRTY');

        if (id) {
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/room-unavailable/' + id,
                type: 'GET',
                success: function (res) {
                    if (res.success && res.data) {
                        var d = res.data;
                        $('#formId').val(d.id);
                        $('#formType').val(d.unavailableType);
                        $('#formRoom').val(d.roomNumberId);
                        $('#formReasonCode').val(d.reasonCode || '');
                        $('#formReasonDetail').val(d.reasonDetail || '');
                        $('#formFromDate').val(d.fromDate);
                        $('#formThroughDate').val(d.throughDate);
                        $('#formReturnStatus').val(d.returnStatus || 'DIRTY');
                        $('#formModalTitle').text('OOO/OOS 수정');
                        $('#formRoom').prop('disabled', true);
                        $('#formType').prop('disabled', true);
                        HolaPms.modal.show('#formModal');
                    }
                }
            });
        } else {
            $('#formModalTitle').text('OOO/OOS 등록');
            $('#formRoom').prop('disabled', false);
            $('#formType').prop('disabled', false);
            HolaPms.modal.show('#formModal');
        }
    },

    save: function () {
        var self = this;
        var id = $('#formId').val();
        var data = {
            roomNumberId: HolaPms.form.intVal('#formRoom'),
            unavailableType: $('#formType').val(),
            reasonCode: HolaPms.form.val('#formReasonCode'),
            reasonDetail: HolaPms.form.val('#formReasonDetail'),
            fromDate: HolaPms.form.val('#formFromDate'),
            throughDate: HolaPms.form.val('#formThroughDate'),
            returnStatus: $('#formReturnStatus').val()
        };

        if (!data.roomNumberId || !data.fromDate || !data.throughDate) {
            HolaPms.alert('warning', '필수 항목을 입력해주세요.');
            return;
        }

        var method = id ? 'PUT' : 'POST';
        var url = '/api/v1/properties/' + self.propertyId + '/room-unavailable';
        if (id) url += '/' + id;

        HolaPms.ajax({
            url: url,
            type: method,
            data: data,
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', id ? '수정되었습니다.' : '등록되었습니다.');
                    HolaPms.modal.hide('#formModal');
                    self.loadList();
                }
            },
            error: function (xhr) {
                var res = xhr.responseJSON;
                HolaPms.alert('error', res && res.message ? res.message : '저장 중 오류가 발생했습니다.');
            }
        });
    },

    deleteItem: function (id) {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/room-unavailable/' + id,
            type: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '해제되었습니다. 객실 상태가 복귀됩니다.');
                    self.loadList();
                }
            },
            error: function (xhr) {
                var res = xhr.responseJSON;
                HolaPms.alert('error', res && res.message ? res.message : '해제 중 오류가 발생했습니다.');
            }
        });
    }
};

$(document).ready(function () {
    FdRoomUnavailable.init();
});
