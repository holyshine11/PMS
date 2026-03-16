/**
 * 재고 관리 페이지
 */
var InvPage = {
    table: null,

    ITEM_TYPE_LABELS: {
        'EXTRA_BED': '엑스트라 베드',
        'CRIB': '유아용 침대',
        'ROLLAWAY': '롤어웨이 베드',
        'EQUIPMENT': '장비'
    },

    MGMT_TYPE_LABELS: {
        'INTERNAL': '자체관리',
        'EXTERNAL': '외부 ERP'
    },

    init: function() {
        this.initTable();
        this.bindEvents();
        this.reload();
    },

    initTable: function() {
        var self = this;
        this.table = $('#invTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: function(data, callback) {
                var propertyId = HolaPms.context.getPropertyId();
                if (!propertyId) {
                    $('#totalCount').text(0);
                    callback({ data: [] });
                    return;
                }

                var params = {};
                var mgmtType = $('#filterManagementType').val();
                if (mgmtType) params.managementType = mgmtType;

                var url = '/api/v1/properties/' + propertyId + '/inventory-items';
                var qs = $.param(params);
                if (qs) url += '?' + qs;

                $.ajax({
                    url: url,
                    success: function(res) {
                        if (res && res.data) {
                            $('#totalCount').text(res.data.length);
                            callback(res);
                        } else {
                            $('#totalCount').text(0);
                            callback({ data: [] });
                        }
                    },
                    error: function(xhr) {
                        HolaPms.handleAjaxError(xhr);
                        $('#totalCount').text(0);
                        callback({ data: [] });
                    }
                });
            },
            pageLength: 20,
            ordering: false,
            dom: 'rtip',
            columns: [
                {
                    data: null, className: 'text-center',
                    render: function(data, type, row, meta) { return meta.row + 1; }
                },
                {
                    data: 'itemCode', className: 'text-center',
                    render: function(data) {
                        return '<span class="badge bg-dark">' + HolaPms.escapeHtml(data) + '</span>';
                    }
                },
                {
                    data: 'itemNameKo',
                    render: function(data, type, row) {
                        var name = HolaPms.escapeHtml(data);
                        if (row.itemNameEn) name += ' <small class="text-muted">(' + HolaPms.escapeHtml(row.itemNameEn) + ')</small>';
                        return name;
                    }
                },
                {
                    data: 'itemType', className: 'text-center',
                    render: function(data) { return self.ITEM_TYPE_LABELS[data] || data; }
                },
                {
                    data: 'managementType', className: 'text-center',
                    render: function(data) {
                        var cls = data === 'INTERNAL' ? 'bg-primary' : 'bg-info';
                        return '<span class="badge ' + cls + '">' + (self.MGMT_TYPE_LABELS[data] || data) + '</span>';
                    }
                },
                {
                    data: 'totalQuantity', className: 'text-center',
                    render: function(data) { return '<span class="badge bg-secondary">' + (data || 0) + '</span>'; }
                },
                { data: 'useYn', className: 'text-center', render: HolaPms.renders.useYnBadge },
                {
                    data: null, className: 'text-center',
                    render: function(data, type, row) {
                        return '<button class="btn btn-sm btn-outline-primary me-1" onclick="InvPage.edit(' + row.id + ')"><i class="fas fa-edit"></i></button>' +
                               '<button class="btn btn-sm btn-outline-danger" onclick="InvPage.remove(' + row.id + ')"><i class="fas fa-trash"></i></button>';
                    }
                }
            ]
        }));
    },

    bindEvents: function() {
        var self = this;

        $(document).on('hola:contextChange', function() { self.reload(); });
        $('#filterManagementType').on('change', function() { self.table.ajax.reload(); });
        $('#filterItemType').on('change', function() {
            var val = $(this).val();
            if (val) {
                var label = self.ITEM_TYPE_LABELS[val] || val;
                self.table.column(3).search('^' + label + '$', true, false).draw();
            } else {
                self.table.column(3).search('').draw();
            }
        });

        $('#managementType').on('change', function() {
            if ($(this).val() === 'EXTERNAL') {
                $('#externalCodeRow').removeClass('d-none');
            } else {
                $('#externalCodeRow').addClass('d-none');
            }
        });
    },

    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#btnCreate').addClass('disabled');
            if (this.table) { this.table.clear().draw(); $('#totalCount').text(0); }
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#btnCreate').removeClass('disabled');
        this.table.ajax.reload();
    },

    openModal: function() {
        $('#itemId').val('');
        $('#itemCode').val('').prop('disabled', false);
        $('#itemNameKo').val('');
        $('#itemNameEn').val('');
        $('#itemType').val('EXTRA_BED');
        $('#managementType').val('INTERNAL');
        $('#externalSystemCode').val('');
        $('#externalCodeRow').addClass('d-none');
        $('#totalQuantity').val(0);
        $('#invModalTitle').text('아이템 등록');
        HolaPms.modal.show('#invModal');
    },

    edit: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/inventory-items/' + id,
            success: function(res) {
                var item = res.data;
                $('#itemId').val(item.id);
                $('#itemCode').val(item.itemCode).prop('disabled', true);
                $('#itemNameKo').val(item.itemNameKo);
                $('#itemNameEn').val(item.itemNameEn || '');
                $('#itemType').val(item.itemType);
                $('#managementType').val(item.managementType);
                $('#externalSystemCode').val(item.externalSystemCode || '');
                $('#totalQuantity').val(item.totalQuantity || 0);
                $('#invModalTitle').text('아이템 수정');

                if (item.managementType === 'EXTERNAL') {
                    $('#externalCodeRow').removeClass('d-none');
                } else {
                    $('#externalCodeRow').addClass('d-none');
                }

                HolaPms.modal.show('#invModal');
            },
            error: function(xhr) { HolaPms.handleAjaxError(xhr); }
        });
    },

    save: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        var id = $('#itemId').val();

        var itemNameKo = $.trim($('#itemNameKo').val());
        if (!itemNameKo) { HolaPms.alert('warning', '아이템명(한글)을 입력해주세요.'); return; }

        if (id) {
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/inventory-items/' + id,
                type: 'PUT',
                data: {
                    itemNameKo: itemNameKo,
                    itemNameEn: $.trim($('#itemNameEn').val()) || null,
                    itemType: $('#itemType').val(),
                    managementType: $('#managementType').val(),
                    externalSystemCode: $.trim($('#externalSystemCode').val()) || null,
                    totalQuantity: parseInt($('#totalQuantity').val()) || 0
                },
                success: function() {
                    HolaPms.modal.hide('#invModal');
                    HolaPms.alert('success', '아이템이 수정되었습니다.');
                    self.table.ajax.reload();
                }
            });
        } else {
            var itemCode = $.trim($('#itemCode').val());
            if (!itemCode) { HolaPms.alert('warning', '아이템 코드를 입력해주세요.'); return; }

            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/inventory-items',
                type: 'POST',
                data: {
                    itemCode: itemCode,
                    itemNameKo: itemNameKo,
                    itemNameEn: $.trim($('#itemNameEn').val()) || null,
                    itemType: $('#itemType').val(),
                    managementType: $('#managementType').val(),
                    externalSystemCode: $.trim($('#externalSystemCode').val()) || null,
                    totalQuantity: parseInt($('#totalQuantity').val()) || 0
                },
                success: function() {
                    HolaPms.modal.hide('#invModal');
                    HolaPms.alert('success', '아이템이 등록되었습니다.');
                    self.table.ajax.reload();
                }
            });
        }
    },

    remove: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        HolaPms.confirm('이 아이템을 삭제하시겠습니까?\n(비활성 상태만 삭제 가능)', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/inventory-items/' + id,
                type: 'DELETE',
                success: function() {
                    HolaPms.alert('success', '아이템이 삭제되었습니다.');
                    self.table.ajax.reload();
                }
            });
        });
    }
};

$(document).ready(function() {
    InvPage.init();
});
