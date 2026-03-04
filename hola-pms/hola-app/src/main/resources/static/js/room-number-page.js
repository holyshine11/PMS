/**
 * 호수코드 관리 페이지 JS
 * - 설계서 Page 24-25 기준 리디자인
 */
const RoomNumberPage = {
    table: null,
    editId: null,
    duplicateChecked: false,

    init: function() {
        var self = this;

        // DataTable 초기화 (5컬럼: NO, 코드명, 설명(국문), 사용여부, 최종수정일)
        this.table = $('#roomNumberTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/properties/0/room-numbers',
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
                    data: 'roomNumber',
                    render: function(data, type, row) {
                        var id = parseInt(row.id, 10);
                        return '<a href="javascript:void(0)" class="text-primary fw-semibold" ' +
                               'onclick="RoomNumberPage.openEditModal(' + id + ')">' +
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
                }
            ],
            order: [[1, 'asc']],
            drawCallback: function(settings) {
                var api = new $.fn.dataTable.Api(settings);
                var info = api.page.info();
                $('#totalCount').text('검색 결과 총 ' + info.recordsDisplay + ' 개');
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
        $('#rnCode').on('input', function() {
            self.duplicateChecked = false;
            $('#duplicateResult').hide();
        });

        this.reload();
    },

    /** 헤더 프로퍼티 드롭다운에서 선택된 프로퍼티명 반환 */
    getPropertyName: function() {
        var $sel = $('#headerPropertySelect');
        return $sel.length && $sel.val() ? $sel.find('option:selected').text() : '';
    },

    /** 프로퍼티 변경 시 리로드 */
    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (propertyId) {
            $('#contextAlert').hide();
            $('#btnCreateWrap').show();
            this.table.ajax.url('/api/v1/properties/' + propertyId + '/room-numbers').load();
        } else {
            $('#contextAlert').show();
            $('#btnCreateWrap').hide();
            this.table.ajax.url('/api/v1/properties/0/room-numbers').load();
        }
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

        $('#roomNumberModalTitle').text('호수코드 등록');
        $('#roomNumberForm')[0].reset();
        $('#rnId').val('');
        $('#rnPropertyName').val(this.getPropertyName());
        $('#rnCode').prop('readonly', false);
        $('#rnUseYnY').prop('checked', true);
        $('#rnUpdatedAt').val('');

        $('#btnCheckDuplicate').show();
        $('#duplicateResult').hide();

        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();

        HolaPms.modal.show('#roomNumberModal');
    },

    /** 수정/상세 모달 */
    openEditModal: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        HolaPms.ajax({
            url: '/api/v1/properties/' + (propertyId || 0) + '/room-numbers/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                self.editId = data.id;
                self.duplicateChecked = true;

                $('#roomNumberModalTitle').text('호수코드 수정');
                $('#rnId').val(data.id);
                $('#rnPropertyName').val(self.getPropertyName());
                $('#rnCode').val(data.roomNumber).prop('readonly', true);
                $('#rnDescKo').val(data.descriptionKo || '');
                $('#rnDescEn').val(data.descriptionEn || '');

                if (data.useYn === false) {
                    $('#rnUseYnN').prop('checked', true);
                } else {
                    $('#rnUseYnY').prop('checked', true);
                }

                if (data.updatedAt) {
                    $('#rnUpdatedAt').val(data.updatedAt.replace('T', ' ').substring(0, 19));
                } else {
                    $('#rnUpdatedAt').val('');
                }

                $('#btnCheckDuplicate').hide();
                $('#duplicateResult').hide();

                $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');

                $('#btnDelete').show();

                HolaPms.modal.show('#roomNumberModal');
            }
        });
    },

    /** 코드명 중복 확인 */
    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#rnCode').val());
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
            url: '/api/v1/properties/' + propertyId + '/room-numbers/check-code',
            type: 'GET',
            data: { roomNumber: code },
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

        var code = $.trim($('#rnCode').val());
        var descKo = $.trim($('#rnDescKo').val());

        if (!code) { HolaPms.alert('warning', '코드명을 입력해주세요.'); return; }
        if (code.length > 20) { HolaPms.alert('warning', '코드명은 최대 20자까지 입력 가능합니다.'); return; }
        if (!descKo) { HolaPms.alert('warning', '설명(국문)을 입력해주세요.'); return; }

        if (!isEdit && !this.duplicateChecked) {
            HolaPms.alert('warning', '코드명 중복확인을 해주세요.');
            return;
        }

        var data = {
            roomNumber: code,
            descriptionKo: descKo,
            descriptionEn: $.trim($('#rnDescEn').val()),
            useYn: $('input[name="rnUseYn"]:checked').val() === 'true'
        };

        var url = isEdit
            ? '/api/v1/properties/' + (propertyId || 0) + '/room-numbers/' + this.editId
            : '/api/v1/properties/' + propertyId + '/room-numbers';

        HolaPms.ajax({
            url: url,
            type: isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.modal.hide('#roomNumberModal');
                HolaPms.alert('success', isEdit ? '호수코드가 수정되었습니다.' : '호수코드가 등록되었습니다.');
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
                url: '/api/v1/properties/' + (propertyId || 0) + '/room-numbers/' + id,
                type: 'DELETE',
                success: function() {
                    HolaPms.modal.hide('#roomNumberModal');
                    HolaPms.alert('success', '호수코드가 삭제되었습니다.');
                    self.reload();
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#roomNumberTable').length) {
        RoomNumberPage.init();
    }
});
