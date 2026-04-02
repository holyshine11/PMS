/**
 * 층코드 관리 페이지 JS
 * - 설계서 기준 리디자인
 */
const FloorPage = {
    table: null,
    editId: null,
    duplicateChecked: false,

    init: function() {
        var self = this;

        // DataTable 초기화 (5컬럼: NO, 코드명, 설명(국문), 사용여부, 최종수정일)
        this.table = $('#floorTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/properties/0/floors',
                dataSrc: function(json) { return json.data || []; }
            },
            serverSide: false,
            pageLength: 20,
            columns: [
                {
                    data: null,
                    orderable: false,
                    render: function(data, type, row, meta) {
                        return meta.row + meta.settings._iDisplayStart + 1;
                    },
                    width: '60px',
                    className: 'text-center'
                },
                {
                    data: 'floorNumber',
                    render: function(data, type, row) {
                        var id = parseInt(row.id, 10);
                        return '<a href="javascript:void(0)" class="text-primary fw-semibold" ' +
                               'onclick="FloorPage.openEditModal(' + id + ')">' +
                               HolaPms.escapeHtml(data) + '</a>';
                    }
                },
                {
                    data: 'descriptionKo',
                    render: function(data) {
                        return data ? HolaPms.escapeHtml(data) : '-';
                    }
                },
                {
                    data: 'useYn',
                    render: HolaPms.renders.useYnBadge,
                    width: '90px',
                    className: 'text-center'
                },
                {
                    data: 'updatedAt',
                    render: function(data) {
                        return data ? data.substring(0, 10) : '-';
                    },
                    width: '130px',
                    className: 'text-center'
                },
                {
                    data: null,
                    orderable: false,
                    render: function(data, type, row) {
                        var id = parseInt(row.id, 10);
                        return '<button class="btn btn-outline-info btn-sm text-nowrap" onclick="FloorPage.openCopyModal(' + id + ')">' +
                               '<i class="fas fa-copy me-1"></i>복사등록</button>';
                    },
                    width: '100px',
                    className: 'text-center'
                }
            ],
            order: [[1, 'asc']],
            dom: 'rtip',
            drawCallback: function() {
                var info = this.api().page.info();
                $('#totalCount').text(info.recordsTotal);
            }
        }));

        // 검색 엔터 키
        $('#searchCode').on('keypress', function(e) {
            if (e.which === 13) { self.search(); }
        });

        // 사용여부 라디오 변경 시 자동 검색
        $('input[name="searchUseYn"]').on('change', function() {
            self.search();
        });

        // 페이지 사이즈 변경
        $('#pageSizeSelect').on('change', function() {
            self.table.page.len(parseInt($(this).val(), 10)).draw();
        });

        // 컨텍스트 변경 이벤트
        $(document).on('hola:contextChange', function() { self.reload(); });

        // 코드명 입력 시 중복확인 리셋
        $('#floorNumber').on('input', function() {
            self.duplicateChecked = false;
            $('#duplicateResult').hide();
        });

        this.reload();
    },

    /** 프로퍼티 변경 시 리로드 */
    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#btnCreateWrap').hide();
            this.table.clear().draw();
            HolaPms.requireContext('property');
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#btnCreateWrap').show();
        this.table.ajax.url('/api/v1/properties/' + propertyId + '/floors').load();
    },

    /** 검색 */
    search: function() {
        var keyword = $.trim($('#searchCode').val());
        var useYn = $('input[name="searchUseYn"]:checked').val();

        this.table.search(keyword);

        if (useYn === 'true') {
            this.table.column(3).search('true');
        } else if (useYn === 'false') {
            this.table.column(3).search('false');
        } else {
            this.table.column(3).search('');
        }

        this.table.draw();
    },

    /** 검색 초기화 */
    resetSearch: function() {
        $('#searchCode').val('');
        $('#useYnAll').prop('checked', true);
        this.table.search('').columns().search('').draw();
    },

    /** 등록 모달 */
    openCreateModal: function() {
        if (!HolaPms.context.getPropertyId()) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        this.editId = null;
        this.duplicateChecked = false;

        $('#floorModalTitle').text('층코드 등록');
        $('#floorForm')[0].reset();
        $('#floorId').val('');
        $('#flPropertyName').val(HolaPms.context.getPropertyName());
        $('#floorNumber').prop('readonly', false);
        $('#floorUseYnY').prop('checked', true);
        $('#flUpdatedAt').val('');

        $('#btnCheckDuplicate').show();
        $('#duplicateResult').hide();

        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();

        HolaPms.modal.show('#floorModal');
    },

    /** 수정/상세 모달 */
    openEditModal: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        HolaPms.ajax({
            url: '/api/v1/properties/' + (propertyId || 0) + '/floors/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                self.editId = data.id;
                self.duplicateChecked = true;

                $('#floorModalTitle').text('층코드 수정');
                $('#floorId').val(data.id);
                $('#flPropertyName').val(HolaPms.context.getPropertyName());
                $('#floorNumber').val(data.floorNumber).prop('readonly', true);
                $('#descriptionKo').val(data.descriptionKo || '');
                $('#descriptionEn').val(data.descriptionEn || '');

                if (data.useYn === false) {
                    $('#floorUseYnN').prop('checked', true);
                } else {
                    $('#floorUseYnY').prop('checked', true);
                }

                if (data.updatedAt) {
                    $('#flUpdatedAt').val(HolaPms.formatDateTime(data.updatedAt));
                } else {
                    $('#flUpdatedAt').val('');
                }

                $('#btnCheckDuplicate').hide();
                $('#duplicateResult').hide();

                $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');

                $('#btnDelete').show();

                HolaPms.modal.show('#floorModal');
            }
        });
    },

    /** 복사등록 모달 열기 */
    openCopyModal: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/floors/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                self.editId = null;
                self.duplicateChecked = false;

                $('#floorModalTitle').text('층코드 복사등록');
                $('#floorForm')[0].reset();
                $('#floorId').val('');
                $('#flPropertyName').val(HolaPms.context.getPropertyName());
                $('#floorNumber').val(data.floorNumber).prop('readonly', false);
                $('#descriptionKo').val(data.descriptionKo || '');
                $('#descriptionEn').val(data.descriptionEn || '');
                $('#floorUseYnY').prop('checked', true);
                $('#flUpdatedAt').val('');

                $('#btnCheckDuplicate').show();
                $('#duplicateResult').hide();

                $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
                $('#btnDelete').hide();

                HolaPms.modal.show('#floorModal');
            }
        });
    },

    /** 코드명 중복 확인 */
    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#floorNumber').val());
        var propertyId = HolaPms.context.getPropertyId();

        if (!code) {
            HolaPms.alert('warning', '코드명을 입력해주세요.');
            return;
        }
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/floors/check-code',
            type: 'GET',
            data: { floorNumber: code },
            success: function(res) {
                var $result = $('#duplicateResult');
                if (res.data.duplicate) {
                    $result.text('이미 사용 중인 코드명입니다.').removeClass('text-success').addClass('text-danger').show();
                    self.duplicateChecked = false;
                } else {
                    $result.text('사용 가능한 코드명입니다.').removeClass('text-danger').addClass('text-success').show();
                    self.duplicateChecked = true;
                }
            }
        });
    },

    /** 저장 */
    save: function() {
        var self = this;
        var isEdit = !!this.editId;
        var propertyId = HolaPms.context.getPropertyId();

        if (!propertyId && !isEdit) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var code = $.trim($('#floorNumber').val());
        var descKo = $.trim($('#descriptionKo').val());

        if (!code) { HolaPms.alert('warning', '코드명을 입력해주세요.'); return; }
        if (code.length > 20) { HolaPms.alert('warning', '코드명은 최대 20자까지 입력 가능합니다.'); return; }
        if (!descKo) { HolaPms.alert('warning', '설명(국문)을 입력해주세요.'); return; }

        if (!isEdit && !this.duplicateChecked) {
            HolaPms.alert('warning', '코드명 중복확인을 해주세요.');
            return;
        }

        var data = {
            floorNumber: code,
            floorName: code,
            descriptionKo: descKo,
            descriptionEn: $.trim($('#descriptionEn').val()),
            useYn: $('input[name="floorUseYn"]:checked').val() === 'true'
        };

        var url = isEdit
            ? '/api/v1/properties/' + (propertyId || 0) + '/floors/' + this.editId
            : '/api/v1/properties/' + propertyId + '/floors';

        HolaPms.ajax({
            url: url,
            type: isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.modal.hide('#floorModal');
                HolaPms.alert('success', isEdit ? '층코드가 수정되었습니다.' : '층코드가 등록되었습니다.');
                self.reload();
            }
        });
    },

    /** 삭제 (사용여부 N일 때만) */
    remove: function() {
        var self = this;
        var id = this.editId;
        var propertyId = HolaPms.context.getPropertyId();

        if (!id) return;

        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + (propertyId || 0) + '/floors/' + id,
                type: 'DELETE',
                success: function() {
                    HolaPms.modal.hide('#floorModal');
                    HolaPms.alert('success', '층코드가 삭제되었습니다.');
                    self.reload();
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#floorTable').length) {
        FloorPage.init();
    }
});
