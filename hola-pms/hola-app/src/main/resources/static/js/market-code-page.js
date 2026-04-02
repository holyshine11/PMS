/**
 * 마켓코드 관리 페이지 JS
 * - 설계서 Page 18-19 기준 리디자인
 */
const MarketCodePage = {
    table: null,
    editId: null,         // 수정 모드 시 ID
    duplicateChecked: false, // 중복확인 완료 여부

    init: function() {
        var self = this;

        // DataTable 초기화 (5컬럼: NO, 코드명, 설명(국문), 사용여부, 최종수정일)
        this.table = $('#marketCodeTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: {
                url: '/api/v1/properties/0/market-codes',
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
                    data: 'marketCode',
                    render: function(data, type, row) {
                        var id = parseInt(row.id, 10);
                        return '<a href="javascript:void(0)" class="text-primary fw-semibold" ' +
                               'onclick="MarketCodePage.openEditModal(' + id + ')">' +
                               HolaPms.escapeHtml(data) + '</a>';
                    }
                },
                {
                    data: 'marketName',
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
                        return '<button class="btn btn-outline-info btn-sm text-nowrap" onclick="MarketCodePage.openCopyModal(' + id + ')">' +
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

        // 프로퍼티 컨텍스트 변경 이벤트
        $(document).on('hola:contextChange', function() { self.reload(); });

        // 코드명 입력 시 중복확인 상태 리셋
        $('#mcCode').on('input', function() {
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
        this.table.ajax.url('/api/v1/properties/' + propertyId + '/market-codes').load();
    },

    /** 검색 실행 - DataTable 클라이언트 필터링 */
    search: function() {
        var keyword = $.trim($('#searchCode').val());
        var useYn = $('input[name="searchUseYn"]:checked').val();

        // 코드명/마켓코드명 전체 검색
        this.table.search(keyword);

        // 사용여부 컬럼(인덱스 3) 필터 - 렌더링된 뱃지 텍스트 기준 정규식 검색
        if (useYn === 'true') {
            this.table.column(3).search('^사용$', true, false);
        } else if (useYn === 'false') {
            this.table.column(3).search('미사용', true, false);
        } else {
            this.table.column(3).search('', true, false);
        }

        this.table.draw();
    },

    /** 검색 초기화 */
    resetSearch: function() {
        $('#searchCode').val('');
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

        $('#marketCodeModalTitle').text('마켓코드 등록');
        $('#marketCodeForm')[0].reset();
        $('#mcId').val('');
        $('#mcPropertyName').val(HolaPms.context.getPropertyName());
        $('#mcCode').prop('readonly', false);
        $('#mcUseYnY').prop('checked', true);
        $('#mcUpdatedAt').val('');

        // 중복확인 버튼 표시
        $('#btnCheckDuplicate').show();
        $('#duplicateResult').hide();

        // 버튼: 등록 모드
        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();

        HolaPms.modal.show('#marketCodeModal');
    },

    /** 수정/상세 모달 열기 */
    openEditModal: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        HolaPms.ajax({
            url: '/api/v1/properties/' + (propertyId || 0) + '/market-codes/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                self.editId = data.id;
                self.duplicateChecked = true; // 수정 시 중복확인 불필요

                $('#marketCodeModalTitle').text('마켓코드 수정');
                $('#mcId').val(data.id);
                $('#mcPropertyName').val(HolaPms.context.getPropertyName());
                $('#mcCode').val(data.marketCode).prop('readonly', true);
                $('#mcDescKo').val(data.marketName || '');
                $('#mcDescEn').val(data.descriptionEn || '');

                if (data.useYn === false) {
                    $('#mcUseYnN').prop('checked', true);
                } else {
                    $('#mcUseYnY').prop('checked', true);
                }

                // 최종 수정일 표시
                if (data.updatedAt) {
                    $('#mcUpdatedAt').val(HolaPms.formatDateTime(data.updatedAt));
                } else {
                    $('#mcUpdatedAt').val('');
                }

                // 중복확인 버튼 숨김 (수정 시 코드명 readonly)
                $('#btnCheckDuplicate').hide();
                $('#duplicateResult').hide();

                // 버튼: 저장 모드
                $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');

                $('#btnDelete').show();

                HolaPms.modal.show('#marketCodeModal');
            }
        });
    },

    /** 코드명 중복 확인 */
    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#mcCode').val());
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
            url: '/api/v1/properties/' + propertyId + '/market-codes/check-code',
            type: 'GET',
            data: { marketCode: code },
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

    /** 복사등록 모달 열기 */
    openCopyModal: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + propertyId + '/market-codes/' + id,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                self.editId = null;
                self.duplicateChecked = false;

                $('#marketCodeModalTitle').text('마켓코드 복사등록');
                $('#marketCodeForm')[0].reset();
                $('#mcId').val('');
                $('#mcPropertyName').val(HolaPms.context.getPropertyName());
                $('#mcCode').val(data.marketCode).prop('readonly', false);
                $('#mcDescKo').val(data.marketName || '');
                $('#mcDescEn').val(data.descriptionEn || '');
                $('#mcUseYnY').prop('checked', true);
                $('#mcUpdatedAt').val('');

                $('#btnCheckDuplicate').show();
                $('#duplicateResult').hide();

                $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
                $('#btnDelete').hide();

                HolaPms.modal.show('#marketCodeModal');
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

        var code = $.trim($('#mcCode').val());
        var descKo = $.trim($('#mcDescKo').val());

        // Validation
        if (!code) { HolaPms.alert('warning', '코드명을 입력해주세요.'); return; }
        if (code.length > 20) { HolaPms.alert('warning', '코드명은 최대 20자까지 입력 가능합니다.'); return; }
        if (!descKo) { HolaPms.alert('warning', '설명(국문)을 입력해주세요.'); return; }

        // 등록 시 중복확인 필수
        if (!isEdit && !this.duplicateChecked) {
            HolaPms.alert('warning', '코드명 중복확인을 해주세요.');
            return;
        }

        var data = {
            marketCode: code,
            marketName: descKo,
            descriptionEn: $.trim($('#mcDescEn').val()),
            useYn: $('input[name="mcUseYn"]:checked').val() === 'true'
        };

        var url = isEdit
            ? '/api/v1/properties/' + (propertyId || 0) + '/market-codes/' + this.editId
            : '/api/v1/properties/' + propertyId + '/market-codes';

        HolaPms.ajax({
            url: url,
            type: isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.modal.hide('#marketCodeModal');
                HolaPms.alert('success', isEdit ? '마켓코드가 수정되었습니다.' : '마켓코드가 등록되었습니다.');
                self.reload();
            }
        });
    },

    /** 삭제 (사용여부 N일 때만 가능) */
    remove: function() {
        var self = this;
        var id = this.editId;
        var propertyId = HolaPms.context.getPropertyId();

        if (!id) return;

        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + (propertyId || 0) + '/market-codes/' + id,
                type: 'DELETE',
                success: function() {
                    HolaPms.modal.hide('#marketCodeModal');
                    HolaPms.alert('success', '마켓코드가 삭제되었습니다.');
                    self.reload();
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#marketCodeTable').length) {
        MarketCodePage.init();
    }
});
