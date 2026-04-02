/**
 * 예약채널 관리 페이지 JS
 * - 마켓코드 패턴 동일 구조
 */
const ReservationChannelPage = {
    table: null,
    editId: null,         // 수정 모드 시 ID
    duplicateChecked: false, // 중복확인 완료 여부

    init: function() {
        var self = this;

        // DataTable 초기화
        this.table = $('#reservationChannelTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/properties/0/reservation-channels',
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
                    data: 'channelCode',
                    render: function(data, type, row) {
                        var id = parseInt(row.id, 10);
                        return '<a href="javascript:void(0)" class="text-primary fw-semibold" ' +
                               'onclick="ReservationChannelPage.openEditModal(' + id + ')">' +
                               HolaPms.escapeHtml(data) + '</a>';
                    }
                },
                {
                    data: 'channelName',
                    render: function(data) {
                        return data ? HolaPms.escapeHtml(data) : '-';
                    }
                },
                {
                    data: 'channelType',
                    render: function(data) {
                        return data ? HolaPms.escapeHtml(data) : '-';
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
                    data: null,
                    orderable: false,
                    render: function(data, type, row) {
                        var id = parseInt(row.id, 10);
                        return '<button class="btn btn-outline-info btn-sm text-nowrap" onclick="ReservationChannelPage.openCopyModal(' + id + ')">' +
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

        // 검색 입력 필드 엔터 키
        $('#searchKeyword').on('keypress', function(e) {
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

        // 프로퍼티 컨텍스트 변경 이벤트
        $(document).on('hola:contextChange', function() { self.reload(); });

        // 채널코드 입력 시 중복확인 상태 리셋
        $('#rcCode').on('input', function() {
            self.duplicateChecked = false;
            $('#duplicateResult').hide();
        });

        this.reload();
    },

    /** 프로퍼티 변경 시 API URL 교체 후 리로드 */
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
        this.table.ajax.url('/api/v1/properties/' + propertyId + '/reservation-channels').load();
    },

    /** 검색 실행 - DataTable 클라이언트 필터링 */
    search: function() {
        var keyword = $.trim($('#searchKeyword').val());
        var useYn = $('input[name="searchUseYn"]:checked').val();

        // 채널코드/채널명 전체 검색
        this.table.search(keyword);

        // 사용여부 컬럼(인덱스 5) 필터 - 렌더링된 뱃지 텍스트 기준 정규식 검색
        if (useYn === 'true') {
            this.table.column(5).search('^사용$', true, false);
        } else if (useYn === 'false') {
            this.table.column(5).search('미사용', true, false);
        } else {
            this.table.column(5).search('', true, false);
        }

        this.table.draw();
    },

    /** 검색 초기화 */
    resetSearch: function() {
        $('#searchKeyword').val('');
        $('#useYnAll').prop('checked', true);
        this.table.search('').columns().search('').draw();
    },

    /** 등록 모달 열기 */
    openCreateModal: function() {
        if (!HolaPms.context.getPropertyId()) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        this.editId = null;
        this.duplicateChecked = false;

        $('#rcModalTitle').text('예약채널 등록');
        $('#rcForm')[0].reset();
        $('#rcId').val('');
        $('#rcPropertyName').val(HolaPms.context.getPropertyName());
        $('#rcCode').prop('readonly', false);
        $('#rcType').val('');
        $('#rcUseYnY').prop('checked', true);
        $('#rcUpdatedAt').val('');

        // 중복확인 버튼 표시
        $('#btnCheckDuplicate').show();
        $('#duplicateResult').hide();

        // 버튼: 등록 모드
        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();

        HolaPms.modal.show('#reservationChannelModal');
    },

    /** 수정/상세 모달 열기 */
    openEditModal: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        HolaPms.ajax({
            url: '/api/v1/properties/' + (propertyId || 0) + '/reservation-channels/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                self.editId = data.id;
                self.duplicateChecked = true; // 수정 시 중복확인 불필요

                $('#rcModalTitle').text('예약채널 수정');
                $('#rcId').val(data.id);
                $('#rcPropertyName').val(HolaPms.context.getPropertyName());
                $('#rcCode').val(data.channelCode).prop('readonly', true);
                $('#rcName').val(data.channelName || '');
                $('#rcType').val(data.channelType || '');
                $('#rcDescKo').val(data.descriptionKo || '');
                $('#rcDescEn').val(data.descriptionEn || '');

                if (data.useYn === false) {
                    $('#rcUseYnN').prop('checked', true);
                } else {
                    $('#rcUseYnY').prop('checked', true);
                }

                // 최종 수정일 표시
                if (data.updatedAt) {
                    $('#rcUpdatedAt').val(HolaPms.formatDateTime(data.updatedAt));
                } else {
                    $('#rcUpdatedAt').val('');
                }

                // 중복확인 버튼 숨김 (수정 시 코드 readonly)
                $('#btnCheckDuplicate').hide();
                $('#duplicateResult').hide();

                // 버튼: 저장 모드
                $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');
                $('#btnDelete').show();

                HolaPms.modal.show('#reservationChannelModal');
            }
        });
    },

    /** 채널코드 중복 확인 */
    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#rcCode').val());
        var propertyId = HolaPms.context.getPropertyId();

        if (!code) {
            HolaPms.alert('warning', '채널코드를 입력해주세요.');
            return;
        }

        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/reservation-channels/check-code',
            type: 'GET',
            data: { channelCode: code },
            success: function(res) {
                var $result = $('#duplicateResult');
                if (res.data.duplicate) {
                    $result.text('이미 사용 중인 채널코드입니다.').removeClass('text-success').addClass('text-danger').show();
                    self.duplicateChecked = false;
                } else {
                    $result.text('사용 가능한 채널코드입니다.').removeClass('text-danger').addClass('text-success').show();
                    self.duplicateChecked = true;
                }
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
            url: '/api/v1/properties/' + propertyId + '/reservation-channels/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                self.editId = null;
                self.duplicateChecked = false;

                $('#rcModalTitle').text('예약채널 복사등록');
                $('#rcForm')[0].reset();
                $('#rcId').val('');
                $('#rcPropertyName').val(HolaPms.context.getPropertyName());
                $('#rcCode').val(data.channelCode).prop('readonly', false);
                $('#rcName').val(data.channelName || '');
                $('#rcType').val(data.channelType || '');
                $('#rcDescKo').val(data.descriptionKo || '');
                $('#rcDescEn').val(data.descriptionEn || '');
                $('#rcUseYnY').prop('checked', true);
                $('#rcUpdatedAt').val('');

                $('#btnCheckDuplicate').show();
                $('#duplicateResult').hide();

                $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
                $('#btnDelete').hide();

                HolaPms.modal.show('#reservationChannelModal');
            }
        });
    },

    /** 저장 (등록/수정 분기) */
    save: function() {
        var self = this;
        var isEdit = !!this.editId;
        var propertyId = HolaPms.context.getPropertyId();

        if (!propertyId && !isEdit) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var code = $.trim($('#rcCode').val());
        var name = $.trim($('#rcName').val());
        var type = $('#rcType').val();

        // Validation
        if (!code) { HolaPms.alert('warning', '채널코드를 입력해주세요.'); return; }
        if (code.length > 20) { HolaPms.alert('warning', '채널코드는 최대 20자까지 입력 가능합니다.'); return; }
        if (!name) { HolaPms.alert('warning', '채널명을 입력해주세요.'); return; }
        if (!type) { HolaPms.alert('warning', '채널유형을 선택해주세요.'); return; }

        // 등록 시 중복확인 필수
        if (!isEdit && !this.duplicateChecked) {
            HolaPms.alert('warning', '채널코드 중복확인을 해주세요.');
            return;
        }

        var data = {
            channelCode: code,
            channelName: name,
            channelType: type,
            descriptionKo: $.trim($('#rcDescKo').val()),
            descriptionEn: $.trim($('#rcDescEn').val()),
            useYn: $('input[name="rcUseYn"]:checked').val() === 'true'
        };

        var url = isEdit
            ? '/api/v1/properties/' + (propertyId || 0) + '/reservation-channels/' + this.editId
            : '/api/v1/properties/' + propertyId + '/reservation-channels';

        HolaPms.ajax({
            url: url,
            type: isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.modal.hide('#reservationChannelModal');
                HolaPms.alert('success', isEdit ? '예약채널이 수정되었습니다.' : '예약채널이 등록되었습니다.');
                self.reload();
            }
        });
    },

    /** 삭제 */
    remove: function() {
        var self = this;
        var id = this.editId;
        var propertyId = HolaPms.context.getPropertyId();

        if (!id) return;

        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + (propertyId || 0) + '/reservation-channels/' + id,
                type: 'DELETE',
                success: function() {
                    HolaPms.modal.hide('#reservationChannelModal');
                    HolaPms.alert('success', '예약채널이 삭제되었습니다.');
                    self.reload();
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#reservationChannelTable').length) {
        ReservationChannelPage.init();
    }
});
