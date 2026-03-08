/**
 * 호텔 관리자 권한 등록/수정 폼 JS
 */
const HotelRoleForm = {
    isEdit: false,
    roleId: null,
    nameChecked: false,
    originalRoleName: '',
    originalHotelId: null,

    init: function() {
        this.roleId = $('#roleId').val() || null;
        this.isEdit = !!this.roleId;

        // 호텔 드롭다운 로드
        this.loadHotels();

        // 메뉴 트리 로드
        this.loadMenuTree();

        if (this.isEdit) {
            $('#pageTitle').html('<i class="fas fa-shield-alt me-2"></i>호텔 관리자 권한 수정');
            $('#btnSaveText').text('저장');
            $('#btnDelete').show();
            $('#updatedAtRow').show();
            this.nameChecked = true;
        }

        // 권한명 변경 시 중복확인 초기화
        $('#roleName').on('input', function() {
            HotelRoleForm.nameChecked = false;
            $('#nameCheckResult').text('').removeClass('text-success text-danger');
        });
        // 호텔 변경 시 중복확인 초기화
        $('#hotelId').on('change', function() {
            HotelRoleForm.nameChecked = false;
            $('#nameCheckResult').text('').removeClass('text-success text-danger');
        });
    },

    /** 호텔 목록 로드 */
    loadHotels: function() {
        HolaPms.ajax({
            url: '/api/v1/hotels',
            type: 'GET',
            success: function(res) {
                var hotels = res.data || [];
                var $select = $('#hotelId');
                $select.find('option:not(:first)').remove();
                hotels.forEach(function(h) {
                    $select.append('<option value="' + h.id + '">' + HolaPms.escapeHtml(h.hotelName) + '</option>');
                });
                // 수정 모드일 때 데이터 로드
                if (HotelRoleForm.isEdit) {
                    HotelRoleForm.loadRole();
                }
            }
        });
    },

    /** 메뉴 트리 로드 */
    loadMenuTree: function() {
        HolaPms.ajax({
            url: '/api/v1/hotel-admin-roles/menu-tree',
            type: 'GET',
            success: function(res) {
                var tree = res.data || [];
                HotelRoleForm.renderMenuTree(tree);
            }
        });
    },

    /** 메뉴 트리 렌더링 (3-depth 지원) */
    renderMenuTree: function(tree) {
        var html = '';
        tree.forEach(function(depth1) {
            html += '<div class="menu-group mb-3">';
            // 1-depth: 볼드 체크박스 (섹션 헤더)
            html += '  <div class="form-check">';
            html += '    <input class="form-check-input menu-depth1" type="checkbox" value="' + depth1.id + '" id="menu_' + depth1.id + '" data-depth="1" onchange="HotelRoleForm.toggleNode(this)">';
            html += '    <label class="form-check-label fw-bold" for="menu_' + depth1.id + '">' + HolaPms.escapeHtml(depth1.menuName) + '</label>';
            html += '  </div>';

            if (depth1.children && depth1.children.length > 0) {
                depth1.children.forEach(function(depth2) {
                    // 2-depth: 들여쓰기 + 볼드 체크박스 (카테고리)
                    html += '  <div class="ms-4 mt-1">';
                    html += '    <div class="form-check">';
                    html += '      <input class="form-check-input menu-depth2" type="checkbox" value="' + depth2.id + '" id="menu_' + depth2.id + '" data-depth="2" data-parent="' + depth1.id + '" onchange="HotelRoleForm.toggleNode(this)">';
                    html += '      <label class="form-check-label fw-bold" for="menu_' + depth2.id + '">' + HolaPms.escapeHtml(depth2.menuName) + '</label>';
                    html += '    </div>';

                    if (depth2.children && depth2.children.length > 0) {
                        // 3-depth: 더 들여쓰기 + 일반 체크박스 (리프), inline 배치
                        html += '    <div class="ms-4 mt-1">';
                        depth2.children.forEach(function(depth3) {
                            html += '      <div class="form-check form-check-inline mb-1">';
                            html += '        <input class="form-check-input menu-depth3" type="checkbox" value="' + depth3.id + '" id="menu_' + depth3.id + '" data-depth="3" data-parent="' + depth2.id + '" data-root="' + depth1.id + '" onchange="HotelRoleForm.toggleNode(this)">';
                            html += '        <label class="form-check-label" for="menu_' + depth3.id + '">' + HolaPms.escapeHtml(depth3.menuName) + '</label>';
                            html += '      </div>';
                        });
                        html += '    </div>';
                    }

                    html += '  </div>';
                });
            }

            html += '</div>';
        });

        if (!tree.length) {
            html = '<span class="text-muted">등록된 메뉴가 없습니다.</span>';
        }
        $('#menuTree').html(html);
    },

    /** 노드 토글 → 상하위 연동 (3-depth 지원) */
    toggleNode: function(el) {
        var $el = $(el);
        var depth = parseInt($el.data('depth'), 10);
        var checked = $el.is(':checked');
        var nodeId = $el.val();

        if (depth === 1) {
            // 1-depth → 하위 2-depth, 3-depth 모두 연동
            $('input.menu-depth2[data-parent="' + nodeId + '"]').prop('checked', checked);
            $('input.menu-depth3[data-root="' + nodeId + '"]').prop('checked', checked);
        } else if (depth === 2) {
            var rootId = $el.data('parent');
            // 2-depth → 하위 3-depth 연동
            $('input.menu-depth3[data-parent="' + nodeId + '"]').prop('checked', checked);
            // 같은 1-depth 하위의 2-depth 전부 체크 → 1-depth 자동 체크
            this.syncParent('menu-depth2', rootId, 'menu-depth1');
        } else if (depth === 3) {
            var parentId = $el.data('parent');
            var rootId2 = $el.data('root');
            // 같은 2-depth 하위의 3-depth 전부 체크 → 2-depth 자동 체크
            this.syncParent('menu-depth3', parentId, 'menu-depth2');
            // 연쇄적으로 1-depth 갱신
            this.syncParent('menu-depth2', rootId2, 'menu-depth1');
        }

        this.updateAllCheck();
    },

    /** 하위 노드 체크 상태 기반으로 부모 자동 체크 */
    syncParent: function(childClass, parentId, parentClass) {
        var total = $('input.' + childClass + '[data-parent="' + parentId + '"]').length;
        var checked = $('input.' + childClass + '[data-parent="' + parentId + '"]:checked').length;
        $('input.' + parentClass + '[value="' + parentId + '"]').prop('checked', total > 0 && checked === total);
    },

    /** 전체선택 토글 */
    toggleAllMenus: function(el) {
        var checked = $(el).is(':checked');
        $('input.menu-depth1, input.menu-depth2, input.menu-depth3').prop('checked', checked);
    },

    /** 전체선택 체크박스 상태 갱신 */
    updateAllCheck: function() {
        var total = $('input.menu-depth1, input.menu-depth2, input.menu-depth3').length;
        var checked = $('input.menu-depth1:checked, input.menu-depth2:checked, input.menu-depth3:checked').length;
        $('#menuSelectAll').prop('checked', total > 0 && total === checked);
    },

    /** 수정 시 권한 데이터 로드 */
    loadRole: function() {
        HolaPms.ajax({
            url: '/api/v1/hotel-admin-roles/' + this.roleId,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                HotelRoleForm.originalRoleName = data.roleName;
                HotelRoleForm.originalHotelId = data.hotelId;

                $('#roleName').val(data.roleName);
                $('#hotelId').val(data.hotelId);
                // 수정 모드에서 호텔 변경 불가
                $('#hotelId').prop('disabled', true);
                $('#updatedAt').text(data.updatedAt || '-');

                if (data.useYn === false) {
                    $('#useYnN').prop('checked', true);
                } else {
                    $('#useYnY').prop('checked', true);
                }

                // 메뉴 체크 복원
                if (data.menuIds && data.menuIds.length > 0) {
                    HotelRoleForm.checkMenus(data.menuIds);
                }
            }
        });
    },

    /** 메뉴 체크 복원 (3-depth 지원) */
    checkMenus: function(menuIds) {
        // 메뉴 트리가 아직 로드 안 되었을 수 있으므로 재시도
        if ($('input.menu-depth1').length === 0 && $('input.menu-depth2').length === 0) {
            setTimeout(function() { HotelRoleForm.checkMenus(menuIds); }, 200);
            return;
        }
        $('input.menu-depth1, input.menu-depth2, input.menu-depth3').prop('checked', false);
        menuIds.forEach(function(id) {
            $('#menu_' + id).prop('checked', true);
        });
        // 2-depth 부모 체크상태 갱신 (3-depth 기준)
        $('input.menu-depth2').each(function() {
            var mid = $(this).val();
            var total = $('input.menu-depth3[data-parent="' + mid + '"]').length;
            if (total > 0) {
                var checked = $('input.menu-depth3[data-parent="' + mid + '"]:checked').length;
                $(this).prop('checked', checked === total);
            }
        });
        // 1-depth 부모 체크상태 갱신 (2-depth 기준)
        $('input.menu-depth1').each(function() {
            var mid = $(this).val();
            var total = $('input.menu-depth2[data-parent="' + mid + '"]').length;
            if (total > 0) {
                var checked = $('input.menu-depth2[data-parent="' + mid + '"]:checked').length;
                $(this).prop('checked', checked === total);
            }
        });
        this.updateAllCheck();
    },

    /** 권한명 중복확인 */
    checkName: function() {
        var roleName = $.trim($('#roleName').val());
        var hotelId = $('#hotelId').val();
        if (!roleName) {
            HolaPms.alert('warning', '권한명을 입력해주세요.');
            return;
        }
        if (!hotelId) {
            HolaPms.alert('warning', '호텔을 먼저 선택해주세요.');
            return;
        }

        var url = '/api/v1/hotel-admin-roles/check-name?hotelId=' + hotelId + '&roleName=' + encodeURIComponent(roleName);
        if (this.isEdit) {
            url += '&excludeId=' + this.roleId;
        }

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                if (res.data.duplicate) {
                    HotelRoleForm.nameChecked = false;
                    $('#nameCheckResult').text('이미 사용 중인 권한명입니다.').removeClass('text-success').addClass('text-danger');
                } else {
                    HotelRoleForm.nameChecked = true;
                    $('#nameCheckResult').text('사용 가능한 권한명입니다.').removeClass('text-danger').addClass('text-success');
                }
            }
        });
    },

    /** 저장 */
    save: function() {
        var roleName = $.trim($('#roleName').val());
        // disabled select는 val()이 안 되므로 prop('disabled') 임시 해제
        var $hotelSelect = $('#hotelId');
        var wasDisabled = $hotelSelect.prop('disabled');
        if (wasDisabled) $hotelSelect.prop('disabled', false);
        var hotelId = $hotelSelect.val();
        if (wasDisabled) $hotelSelect.prop('disabled', true);

        if (!roleName) {
            HolaPms.alert('warning', '권한명을 입력해주세요.');
            $('#roleName').focus();
            return;
        }
        if (!hotelId) {
            HolaPms.alert('warning', '호텔을 선택해주세요.');
            return;
        }

        // 수정 모드에서 권한명/호텔이 변경되지 않았으면 중복확인 스킵
        if (this.isEdit && roleName === this.originalRoleName) {
            this.nameChecked = true;
        }

        if (!this.nameChecked) {
            HolaPms.alert('warning', '권한명 중복확인을 해주세요.');
            return;
        }

        // 선택된 메뉴 수집 (leaf 노드만: children이 없는 최하위)
        var menuIds = [];
        // 3-depth leaf 수집
        $('input.menu-depth3:checked').each(function() {
            menuIds.push(parseInt($(this).val(), 10));
        });
        // children이 없는 2-depth도 leaf로 수집 (예: 예약관리)
        $('input.menu-depth2:checked').each(function() {
            var mid = $(this).val();
            if ($('input.menu-depth3[data-parent="' + mid + '"]').length === 0) {
                menuIds.push(parseInt(mid, 10));
            }
        });

        var data = {
            roleName: roleName,
            useYn: $('input[name="useYn"]:checked').val() === 'true',
            menuIds: menuIds
        };

        if (!this.isEdit) {
            data.hotelId = parseInt(hotelId, 10);
        }

        HolaPms.ajax({
            url: this.isEdit
                ? '/api/v1/hotel-admin-roles/' + this.roleId
                : '/api/v1/hotel-admin-roles',
            type: this.isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.alertAndRedirect('success', HotelRoleForm.isEdit ? '권한이 수정되었습니다.' : '권한이 등록되었습니다.', '/admin/roles/hotel-admins');
            }
        });
    },

    /** 삭제 */
    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/hotel-admin-roles/' + HotelRoleForm.roleId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alertAndRedirect('success', '권한이 삭제되었습니다.', '/admin/roles/hotel-admins');
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#roleForm').length) {
        HotelRoleForm.init();
    }
});
