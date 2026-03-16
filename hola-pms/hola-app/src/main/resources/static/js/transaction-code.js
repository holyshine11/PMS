/**
 * 트랜잭션 코드 관리 페이지
 * 좌측: 그룹 트리 (MAIN → SUB)
 * 우측: 코드 테이블 (DataTable)
 */
var TcPage = {
    table: null,
    selectedGroupId: null,
    groupTreeData: [],

    REVENUE_LABELS: {
        'LODGING': '숙박',
        'FOOD_BEVERAGE': '식음',
        'MISC': '기타',
        'TAX': '세금',
        'NON_REVENUE': '비매출'
    },

    CODE_TYPE_LABELS: {
        'CHARGE': '부과',
        'PAYMENT': '결제'
    },

    init: function() {
        this.initTable();
        this.bindEvents();
        this.reload();
    },

    initTable: function() {
        var self = this;
        this.table = $('#codeTable').DataTable($.extend({}, HolaPms.dataTableDefaults, {
            ajax: function(data, callback) {
                var propertyId = HolaPms.context.getPropertyId();
                if (!propertyId) {
                    $('#totalCount').text(0);
                    callback({ data: [] });
                    return;
                }

                var params = {};
                if (self.selectedGroupId) {
                    params.groupId = self.selectedGroupId;
                }
                var revenueCategory = $('#filterRevenueCategory').val();
                if (revenueCategory) {
                    params.revenueCategory = revenueCategory;
                }

                var queryString = $.param(params);
                var url = '/api/v1/properties/' + propertyId + '/transaction-codes';
                if (queryString) url += '?' + queryString;

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
                    data: null,
                    className: 'text-center',
                    render: function(data, type, row, meta) {
                        return meta.row + 1;
                    }
                },
                {
                    data: 'transactionCode',
                    className: 'text-center',
                    render: function(data) {
                        return '<span class="badge bg-dark">' + HolaPms.escapeHtml(data) + '</span>';
                    }
                },
                {
                    data: 'codeNameKo',
                    render: function(data, type, row) {
                        var name = HolaPms.escapeHtml(data);
                        if (row.codeNameEn) {
                            name += ' <small class="text-muted">(' + HolaPms.escapeHtml(row.codeNameEn) + ')</small>';
                        }
                        return name;
                    }
                },
                {
                    data: 'revenueCategory',
                    className: 'text-center',
                    render: function(data) {
                        return TcPage.REVENUE_LABELS[data] || data;
                    }
                },
                {
                    data: 'codeType',
                    className: 'text-center',
                    render: function(data) {
                        var cls = data === 'CHARGE' ? 'bg-primary' : 'bg-info';
                        return '<span class="badge ' + cls + '">' + (TcPage.CODE_TYPE_LABELS[data] || data) + '</span>';
                    }
                },
                {
                    data: 'useYn',
                    className: 'text-center',
                    render: HolaPms.renders.useYnBadge
                },
                {
                    data: null,
                    className: 'text-center',
                    render: function(data, type, row) {
                        return '<button class="btn btn-sm btn-outline-primary me-1" onclick="TcPage.editCode(' + row.id + ')">' +
                               '<i class="fas fa-edit"></i></button>' +
                               '<button class="btn btn-sm btn-outline-danger" onclick="TcPage.deleteCode(' + row.id + ')">' +
                               '<i class="fas fa-trash"></i></button>';
                    }
                }
            ]
        }));
    },

    bindEvents: function() {
        var self = this;

        $(document).on('hola:contextChange', function() {
            self.selectedGroupId = null;
            self.reload();
        });

        $('#filterRevenueCategory').on('change', function() {
            self.table.ajax.reload();
        });

        $('input[name="filterUseYn"]').on('change', function() {
            var val = $(this).val();
            if (val === 'true') {
                self.table.column(5).search('^사용$', true, false).draw();
            } else if (val === 'false') {
                self.table.column(5).search('미사용', true, false).draw();
            } else {
                self.table.column(5).search('').draw();
            }
        });

        // 그룹 유형 변경 시 상위 그룹 선택 토글
        $('#groupType').on('change', function() {
            if ($(this).val() === 'SUB') {
                $('#parentGroupRow').removeClass('d-none');
            } else {
                $('#parentGroupRow').addClass('d-none');
            }
        });
    },

    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#mainContent').addClass('d-none');
            return;
        }

        $('#contextAlert').addClass('d-none');
        $('#mainContent').removeClass('d-none');

        this.loadGroupTree();
        this.table.ajax.reload();
    },

    // ========== 그룹 트리 ==========

    loadGroupTree: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/transaction-code-groups',
            success: function(res) {
                self.groupTreeData = res.data || [];
                self.renderGroupTree();
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    renderGroupTree: function() {
        var self = this;
        var $tree = $('#groupTree');
        $tree.empty();

        if (this.groupTreeData.length === 0) {
            $tree.html('<div class="text-muted text-center py-5">등록된 그룹이 없습니다</div>');
            return;
        }

        var html = '<div class="list-group list-group-flush">';

        this.groupTreeData.forEach(function(main) {
            // MAIN 그룹 헤더
            var mainActive = self.selectedGroupId === main.id ? ' active' : '';
            html += '<a href="#" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center group-item' + mainActive + '"' +
                    ' data-group-id="' + main.id + '" data-group-type="MAIN">' +
                    '<span><i class="fas fa-folder me-2 text-warning"></i><strong>' + HolaPms.escapeHtml(main.groupNameKo) + '</strong></span>' +
                    '<span>' +
                    '<button class="btn btn-sm btn-link text-primary p-0 me-2" onclick="event.preventDefault(); TcPage.editGroup(' + main.id + ')" title="수정"><i class="fas fa-edit"></i></button>' +
                    '</span></a>';

            // SUB 그룹 아이템
            if (main.children && main.children.length > 0) {
                main.children.forEach(function(sub) {
                    var subActive = self.selectedGroupId === sub.id ? ' active' : '';
                    html += '<a href="#" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center ps-5 group-item' + subActive + '"' +
                            ' data-group-id="' + sub.id + '" data-group-type="SUB">' +
                            '<span><i class="fas fa-folder-open me-2 text-info"></i>' + HolaPms.escapeHtml(sub.groupNameKo) + '</span>' +
                            '<span>' +
                            '<button class="btn btn-sm btn-link text-primary p-0 me-2" onclick="event.preventDefault(); TcPage.editGroup(' + sub.id + ')" title="수정"><i class="fas fa-edit"></i></button>' +
                            '</span></a>';
                });
            }
        });

        html += '</div>';
        $tree.html(html);

        // 그룹 클릭 이벤트
        $tree.find('.group-item').on('click', function(e) {
            if ($(e.target).closest('button').length) return;
            e.preventDefault();
            var groupId = $(this).data('group-id');
            self.selectGroup(groupId, $(this).find('strong, span:first').text().trim());
        });
    },

    selectGroup: function(groupId, groupName) {
        if (this.selectedGroupId === groupId) {
            // 같은 그룹 클릭 시 선택 해제
            this.selectedGroupId = null;
            $('#selectedGroupLabel').text('');
            $('#filterRevenueCategory').val('');
        } else {
            this.selectedGroupId = groupId;
            $('#selectedGroupLabel').text('- ' + groupName);
        }
        this.renderGroupTree();
        this.table.ajax.reload();
    },

    // ========== 그룹 CRUD ==========

    openGroupModal: function(id) {
        $('#groupId').val('');
        $('#groupCode').val('').prop('disabled', false);
        $('#groupNameKo').val('');
        $('#groupNameEn').val('');
        $('#groupType').val('MAIN');
        $('#parentGroupId').val('');
        $('#parentGroupRow').addClass('d-none');
        $('#groupSortOrder').val(0);
        $('#groupModalTitle').text('그룹 등록');

        // 상위 그룹 셀렉트 옵션 갱신
        this.loadMainGroupOptions();

        HolaPms.modal.show('#groupModal');
    },

    loadMainGroupOptions: function() {
        var $sel = $('#parentGroupId');
        $sel.html('<option value="">선택</option>');
        this.groupTreeData.forEach(function(main) {
            $sel.append('<option value="' + main.id + '">' + HolaPms.escapeHtml(main.groupNameKo) + '</option>');
        });
    },

    editGroup: function(id) {
        var self = this;
        var group = this.findGroupById(id);
        if (!group) return;

        this.loadMainGroupOptions();

        $('#groupId').val(group.id);
        $('#groupCode').val(group.groupCode).prop('disabled', true);
        $('#groupNameKo').val(group.groupNameKo);
        $('#groupNameEn').val(group.groupNameEn || '');
        $('#groupType').val(group.groupType).prop('disabled', true);
        $('#groupSortOrder').val(group.sortOrder || 0);
        $('#groupModalTitle').text('그룹 수정');

        if (group.groupType === 'SUB') {
            $('#parentGroupRow').removeClass('d-none');
            // parentGroupId는 트리 데이터에서 직접 찾아야 함
            this.groupTreeData.forEach(function(main) {
                if (main.children) {
                    main.children.forEach(function(sub) {
                        if (sub.id === id) {
                            $('#parentGroupId').val(main.id);
                        }
                    });
                }
            });
        } else {
            $('#parentGroupRow').addClass('d-none');
        }

        HolaPms.modal.show('#groupModal');
    },

    findGroupById: function(id) {
        for (var i = 0; i < this.groupTreeData.length; i++) {
            var main = this.groupTreeData[i];
            if (main.id === id) return main;
            if (main.children) {
                for (var j = 0; j < main.children.length; j++) {
                    if (main.children[j].id === id) return main.children[j];
                }
            }
        }
        return null;
    },

    saveGroup: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        var id = $('#groupId').val();

        var groupNameKo = $.trim($('#groupNameKo').val());
        if (!groupNameKo) {
            HolaPms.alert('warning', '그룹명(한글)을 입력해주세요.');
            return;
        }

        if (id) {
            // 수정
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/transaction-code-groups/' + id,
                type: 'PUT',
                data: {
                    groupNameKo: groupNameKo,
                    groupNameEn: $.trim($('#groupNameEn').val()) || null,
                    sortOrder: parseInt($('#groupSortOrder').val()) || 0
                },
                success: function() {
                    HolaPms.modal.hide('#groupModal');
                    HolaPms.alert('success', '그룹이 수정되었습니다.');
                    self.loadGroupTree();
                }
            });
        } else {
            // 등록
            var groupCode = $.trim($('#groupCode').val());
            var groupType = $('#groupType').val();
            if (!groupCode) {
                HolaPms.alert('warning', '그룹 코드를 입력해주세요.');
                return;
            }

            var data = {
                groupCode: groupCode,
                groupNameKo: groupNameKo,
                groupNameEn: $.trim($('#groupNameEn').val()) || null,
                groupType: groupType,
                sortOrder: parseInt($('#groupSortOrder').val()) || 0
            };

            if (groupType === 'SUB') {
                var parentId = $('#parentGroupId').val();
                if (!parentId) {
                    HolaPms.alert('warning', '상위 그룹을 선택해주세요.');
                    return;
                }
                data.parentGroupId = parseInt(parentId);
            }

            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/transaction-code-groups',
                type: 'POST',
                data: data,
                success: function() {
                    HolaPms.modal.hide('#groupModal');
                    HolaPms.alert('success', '그룹이 등록되었습니다.');
                    self.loadGroupTree();
                }
            });
        }
    },

    // ========== 코드 CRUD ==========

    openCodeModal: function() {
        $('#codeId').val('');
        $('#transactionCode').val('').prop('disabled', false);
        $('#codeNameKo').val('');
        $('#codeNameEn').val('');
        $('#revenueCategory').val('LODGING');
        $('#codeType').val('CHARGE');
        $('#codeSortOrder').val(0);
        $('#codeModalTitle').text('트랜잭션 코드 등록');

        this.loadSubGroupOptions();

        // 현재 선택된 그룹이 있으면 자동 선택
        if (this.selectedGroupId) {
            $('#codeGroupId').val(this.selectedGroupId);
        }

        HolaPms.modal.show('#codeModal');
    },

    loadSubGroupOptions: function() {
        var $sel = $('#codeGroupId');
        $sel.html('<option value="">선택</option>');
        this.groupTreeData.forEach(function(main) {
            // SUB 그룹만 선택 가능
            if (main.children && main.children.length > 0) {
                var optGroup = $('<optgroup label="' + HolaPms.escapeHtml(main.groupNameKo) + '"></optgroup>');
                main.children.forEach(function(sub) {
                    optGroup.append('<option value="' + sub.id + '">' + HolaPms.escapeHtml(sub.groupNameKo) + '</option>');
                });
                $sel.append(optGroup);
            }
            // MAIN 그룹도 직접 코드를 가질 수 있도록
            $sel.append('<option value="' + main.id + '">[MAIN] ' + HolaPms.escapeHtml(main.groupNameKo) + '</option>');
        });
    },

    editCode: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/transaction-codes/' + id,
            success: function(res) {
                var code = res.data;
                self.loadSubGroupOptions();

                $('#codeId').val(code.id);
                $('#transactionCode').val(code.transactionCode).prop('disabled', true);
                $('#codeGroupId').val(code.transactionGroupId);
                $('#codeNameKo').val(code.codeNameKo);
                $('#codeNameEn').val(code.codeNameEn || '');
                $('#revenueCategory').val(code.revenueCategory);
                $('#codeType').val(code.codeType);
                $('#codeSortOrder').val(code.sortOrder || 0);
                $('#codeModalTitle').text('트랜잭션 코드 수정');

                HolaPms.modal.show('#codeModal');
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    saveCode: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        var id = $('#codeId').val();

        var codeNameKo = $.trim($('#codeNameKo').val());
        var groupId = $('#codeGroupId').val();

        if (!groupId) {
            HolaPms.alert('warning', '소속 그룹을 선택해주세요.');
            return;
        }
        if (!codeNameKo) {
            HolaPms.alert('warning', '코드명(한글)을 입력해주세요.');
            return;
        }

        if (id) {
            // 수정
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/transaction-codes/' + id,
                type: 'PUT',
                data: {
                    transactionGroupId: parseInt(groupId),
                    codeNameKo: codeNameKo,
                    codeNameEn: $.trim($('#codeNameEn').val()) || null,
                    revenueCategory: $('#revenueCategory').val(),
                    sortOrder: parseInt($('#codeSortOrder').val()) || 0
                },
                success: function() {
                    HolaPms.modal.hide('#codeModal');
                    HolaPms.alert('success', '트랜잭션 코드가 수정되었습니다.');
                    self.table.ajax.reload();
                }
            });
        } else {
            // 등록
            var transactionCode = $.trim($('#transactionCode').val());
            if (!transactionCode) {
                HolaPms.alert('warning', '트랜잭션 코드를 입력해주세요.');
                return;
            }

            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/transaction-codes',
                type: 'POST',
                data: {
                    transactionGroupId: parseInt(groupId),
                    transactionCode: transactionCode,
                    codeNameKo: codeNameKo,
                    codeNameEn: $.trim($('#codeNameEn').val()) || null,
                    revenueCategory: $('#revenueCategory').val(),
                    codeType: $('#codeType').val(),
                    sortOrder: parseInt($('#codeSortOrder').val()) || 0
                },
                success: function() {
                    HolaPms.modal.hide('#codeModal');
                    HolaPms.alert('success', '트랜잭션 코드가 등록되었습니다.');
                    self.table.ajax.reload();
                }
            });
        }
    },

    deleteCode: function(id) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();

        HolaPms.confirm('이 트랜잭션 코드를 삭제하시겠습니까?\n(비활성 상태만 삭제 가능)', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + propertyId + '/transaction-codes/' + id,
                type: 'DELETE',
                success: function() {
                    HolaPms.alert('success', '트랜잭션 코드가 삭제되었습니다.');
                    self.table.ajax.reload();
                }
            });
        });
    }
};

$(document).ready(function() {
    TcPage.init();
});
